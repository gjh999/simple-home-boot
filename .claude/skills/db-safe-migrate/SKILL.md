---
name: db-safe-migrate
description: 테이블/컬럼 삭제(DROP TABLE, DROP COLUMN, TRUNCATE 등 비가역 파괴)를 포함한 DB 변경 전에, 현재 DB를 복구 가능한 SQL 덤프 파일로 먼저 백업하는 절차. DROP/파괴적 마이그레이션을 시작하기 직전에 반드시 사용.
---

# db-safe-migrate — 파괴적 DB 변경 전 복구 백업

## 언제 사용하나 (필수)
다음 중 하나라도 포함하는 DB 작업을 **시작하기 전에 반드시** 이 스킬로 백업을 먼저 만든다:
- `DROP TABLE` / `DROP COLUMN`
- `TRUNCATE`
- 데이터 손실 가능 변경: 컬럼 타입 축소·재정의, `DELETE FROM`(대량), 컬럼 rename 후 기존 컬럼 제거 등

추가만 하는 변경(`ADD COLUMN`, `CREATE TABLE`, `ADD COLUMN IF NOT EXISTS`, 주석/컬럼명 명시)은 비파괴적이라 백업 불필요.
판단이 애매하면 **백업하고 진행**한다.

## 이 프로젝트(eGovFrame)의 DB 구성
- **개발**: 내장 **HSQLDB**(별도 서버 없음). 초기화 시드 = `src/main/resources/db/shtdb.sql`, 설정 = `EgovConfigAppDatasource.java`.
  - 내장/메모리 DB라 보통 파괴적 마이그레이션 대상이 아니지만(재기동 시 시드로 재생성), 운영 데이터가 쌓인 파일모드면 동일하게 백업한다.
- **운영(6종 DBMS)**: PostgreSQL·MySQL·Oracle·Altibase·Tibero·CUBRID. DDL/DML = `DATABASE/all_sht_{ddl,data}_{dbms}.sql`.
- 접속정보 위치: `application.properties`(`spring.datasource.*`) · `application-*.properties` · `Globals.DbType`(MyBatis 매퍼 DBMS 분기). **운영 시크릿은 환경변수로 주입하고 저장소엔 placeholder만 둔다.**

## 절차

### 0) 접속정보 확보
대상 DB의 JDBC URL/계정/비번을 설정 파일에서 찾는다(`application*.properties` 의 `spring.datasource.url/username/password`).
예) 운영 PostgreSQL: `jdbc:postgresql://<HOST>:<PORT>/<DB>`, 계정/비번은 설정값 사용(여기 문서엔 절대 실제 값 적지 않음).

### 1) 백업 파일 경로 결정
타임스탬프 포함 파일명으로 프로젝트의 `.dbbackup/`(없으면 생성)에 저장한다.
`Date.now()`/`new Date()` 가 막혀 있으므로 셸에서 타임스탬프를 만든다:
```bash
TS=$(date +%Y%m%d_%H%M%S)         # Bash
OUT=".dbbackup/backup_<dbname>_${TS}.sql"
mkdir -p .dbbackup
```

### 2) 덤프 생성 — 네이티브 도구 우선, 없으면 JDBC 폴백
**(A) 네이티브 덤프 도구가 있으면(최선):**
```bash
# PostgreSQL:  pg_dump "postgresql://<USER>:<PASS>@<HOST>:<PORT>/<DB>" -f "$OUT"
# MySQL:       mysqldump -h <HOST> -P <PORT> -u <USER> -p<PASS> <DB> > "$OUT"
# Oracle/Tibero: expdp 또는 SQL Developer 카트, CUBRID: cubrid unloaddb, Altibase: aexport
```

**(B) 네이티브 도구가 없으면(이 환경 기본) — 동봉된 `DbDump.java`(JDBC 폴백) 사용:**
스킬 폴더의 `DbDump.java` 를 프로젝트로 복사해 컴파일·실행한다. JDBC 드라이버 jar 는 보통 `~/.m2` 에 있다(아래는 PostgreSQL 예; 다른 DBMS는 해당 드라이버 jar로 교체).
```bash
SKILL_DIR=".claude/skills/db-safe-migrate"
PGJAR=$(find "$HOME/.m2" -name "postgresql-*.jar" 2>/dev/null | grep -v sources | head -1)
# Windows java 는 클래스패스 구분자 ';' + Windows 경로 필요 → 변환
PGJAR_WIN=$(cygpath -w "$PGJAR" 2>/dev/null || echo "$PGJAR")
JAVA_HOME="/d/eGovCI-5.0.0-Windows-64bit/bin/jdk-17.0.17+10"   # 프로젝트의 JDK 경로로 조정
cp "$SKILL_DIR/DbDump.java" .
"$JAVA_HOME/bin/javac" -encoding UTF-8 DbDump.java
"$JAVA_HOME/bin/java" -Dfile.encoding=UTF-8 -cp ".;$PGJAR_WIN" DbDump \
  "jdbc:postgresql://HOST:PORT/DB" USER PASS "$OUT"
```

### 3) 백업 검증 (이게 통과해야 다음 단계로)
```bash
test -s "$OUT" && echo "size ok"                 # 0바이트 아님
grep -c "^CREATE TABLE" "$OUT"                   # 테이블 수 > 0
grep -c "^INSERT INTO"  "$OUT"                    # 데이터 행 수 확인
tail -1 "$OUT"                                    # 'COMMIT;' 로 끝나는지(JDBC 폴백)
```
- 0바이트거나 CREATE TABLE 이 0이면 **백업 실패 → DROP 작업 중단**하고 원인 해결.
- 사용자에게 백업 파일 경로와 테이블/행 수를 보고한 뒤 파괴적 변경을 진행한다.

### 4) 이제 파괴적 변경 진행
백업이 검증된 뒤에만 DROP/파괴 작업을 실행한다.

## 복구 방법 (필요 시)
- 네이티브 덤프 산출물: `psql "<libpq url>" -f "$OUT"`(PG) / `mysql <DB> < "$OUT"`(MySQL) 등으로 빈 DB에 적용.
- JDBC 폴백 산출물: 동일하게 스크립트 실행. **빈 스키마에 적용** 권장(폴백 덤프는 FK/시퀀스 미포함 — 데이터·PK 복구용). 정밀 복구가 필요하면 평소 네이티브 백업을 병행할 것.

## 주의
- 백업은 운영 DB에 읽기만 한다(파괴적 작업 없음). 안전.
- 큰 테이블은 덤프 파일이 커질 수 있다(허용).
- `.dbbackup/` 는 비밀/대용량일 수 있으니 **`.gitignore` 에 추가하고 커밋하지 않는다**(본 저장소는 이미 제외됨).
