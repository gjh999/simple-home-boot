# simple-home-boot 전용 스킬 (SKILL.md)

> 이 파일은 Claude Code가 본 프로젝트에서 반복 작업을 재현 가능한 절차로 수행하기 위한
> **컨텍스트 엔지니어링** 및 **하네스 엔지니어링** 가이드입니다.

---

## 1. 표준 참조 순서 (신규 작업 시)

```
① CLAUDE.md        → 전체 구조·아키텍처 파악
② Docs/단어규칙.md   → 영문약어 결정 (3284개 공통표준단어)
③ Docs/도메인규칙.md → 타입·길이 결정 (129개 공통표준도메인)
④ Docs/용어규칙.md  → 컬럼명·테이블명 확인
⑤ SKILL.md         → 본 파일(구현 절차·검증 체크리스트)
```

규칙 파일에 존재하지 않는 단어/용어/도메인은 해당 파일 하단에 **직접 추가** 후 사용한다.

---

## 2. DB 스키마 표준

### 2-1. 테이블 명명 규칙
```
TB_ + 기능명  (대문자 스네이크케이스)
예) TB_BBS, TB_BBS_MASTER, TB_EMPLYR_INFO
```

### 2-2. 감사 컬럼 (Audit Columns — 모든 테이블 필수)
```sql
FRST_REGIST_PNTTM  TIMESTAMP    -- 등록일시
FRST_REGISTER_ID   VARCHAR(20)  -- 등록자ID
LAST_UPDT_PNTTM    TIMESTAMP    -- 수정일시
LAST_UPDUSR_ID     VARCHAR(20)  -- 수정자ID
```

### 2-3. DBMS별 문법 차이
| 구분 | HSQL | PostgreSQL | MySQL | Oracle |
|------|------|-----------|-------|--------|
| 자동증가 | IDENTITY / IDS 테이블 | SERIAL / SEQUENCE | AUTO_INCREMENT | SEQUENCE |
| 현재시각 | NOW() | NOW() | NOW() | SYSDATE |
| 문자열 연결 | \|\| | \|\| | CONCAT() | \|\| |
| CLOB | LONGVARCHAR | TEXT | MEDIUMTEXT | CLOB |
| 날짜형식 | TIMESTAMP | TIMESTAMP | DATETIME | DATE |
| LIMIT | LIMIT n OFFSET m | LIMIT n OFFSET m | LIMIT n OFFSET m | ROWNUM / FETCH |
| IF NULL | IFNULL() | COALESCE() | IFNULL() | NVL() |
| 날짜포맷 | YEAR/MONTH/DAY 함수 | TO_CHAR | DATE_FORMAT | TO_CHAR |

### 2-4. 신규 테이블 생성 체크리스트
- [ ] TB_ 접두어 + 스네이크케이스 명명
- [ ] PK 정의
- [ ] 감사 컬럼 4개 포함
- [ ] FK 제약조건 명명 (FK_대상_참조)
- [ ] shtdb.sql 업데이트
- [ ] DATABASE/all_sht_ddl_*.sql 전 DBMS 파일 업데이트
- [ ] Docs/db-schema-guide.md 업데이트

### 2-5. 테이블명 매핑표 (구 LET* → 신 TB_*)
| 구 테이블명 | 신 테이블명 |
|-------------|-------------|
| LETTCCMMNCLCODE | TB_CMMN_CL_CODE |
| LETTCCMMNDETAILCODE | TB_CMMN_DETAIL_CODE |
| LETTCCMMNCODE | TB_CMMN_CODE |
| LETTHEMPLYRINFOCHANGEDTLS | TB_EMPLYR_INFO_CHNG_DTLS |
| LETTNAUTHORGROUPINFO | TB_AUTHOR_GROUP_INFO |
| LETTNAUTHORINFO | TB_AUTHOR_INFO |
| LETTNBBSMASTEROPTN | TB_BBS_MASTER_OPTN |
| LETTNBBSMASTER | TB_BBS_MASTER |
| LETTNBBSUSE | TB_BBS_USE |
| LETTNBBS | TB_BBS |
| LETTNEMPLYRSCRTYESTBS | TB_EMPLYR_SCRTY_ESTBS |
| LETTNEMPLYRINFO | TB_EMPLYR_INFO |
| LETTNENTRPRSMBER | TB_ENTRPRS_MBER |
| LETTNFILEDETAIL | TB_FILE_DETAIL |
| LETTNFILE | TB_FILE |
| LETTNGNRLMBER | TB_GNRL_MBER |
| LETTNORGNZTINFO | TB_ORGNZT_INFO |
| LETTNSCHDULINFO | TB_SCHDUL_INFO |
| COMVNUSERMASTER (VIEW) | VW_USER_MASTER |

---

## 3. 매퍼 XML 작성 표준

### 3-1. 파일 위치
```
src/main/resources/egovframework/mapper/let/{모듈}/Egov{기능}_{SQL}_{db}.xml
```

### 3-2. DB별 파일 목록 (6종)
`hsql`, `mysql`, `oracle`, `tibero`, `cubrid`, `altibase`

### 3-3. HSQL 전용 주의사항
```xml
<!-- IFNULL 사용 (Oracle NVL 사용 금지) -->
SELECT IFNULL(MAX(NTT_ID), 0) + 1

<!-- 날짜 포맷: TO_CHAR 미지원 → 날짜 함수 사용 -->
YEAR(FRST_REGIST_PNTTM) || '-' || MONTH(FRST_REGIST_PNTTM) || '-' || DAYOFMONTH(FRST_REGIST_PNTTM)

<!-- 페이징 -->
LIMIT #{recordCountPerPage} OFFSET #{firstIndex}

<!-- 현재시각 -->
NOW()
```

---

## 4. Java 코딩 표준

### 4-1. 패키지 구조
```
egovframework/
  com/cmm/     → 공통 VO, 서비스, 유틸
  com/config/  → Spring 설정 (EgovConfigApp*.java)
  com/jwt/     → JWT 유틸·필터
  com/security/→ SecurityConfig, WebMvcConfig
  let/{모듈}/
    controller/       → @Controller (MVC) 또는 @RestController (API)
    domain/model/     → *VO, 도메인 객체
    dto/request/      → *RequestDTO
    dto/response/     → *ResponseDTO
    service/          → 인터페이스
    service/impl/     → *Impl
    domain/repository/→ DAO (@Mapper)
```

### 4-2. 클래스 명명
```java
@Controller
public class Egov{기능명}Controller { }      // MVC (Thymeleaf)

@RestController
public class Egov{기능명}ApiController { }   // REST API (JSON)

public interface Egov{기능명}Service { }
@Service
public class Egov{기능명}ServiceImpl implements Egov{기능명}Service { }

@Mapper
public interface {기능명}DAO { }
```

### 4-3. 필수 어노테이션
```java
@RequiredArgsConstructor  // 생성자 주입 (필드 주입 금지)
@Slf4j                    // 로깅
```

---

## 5. Thymeleaf / KRDS 화면 표준

### 5-1. 템플릿 위치
```
src/main/resources/templates/
  layouts/default.html      → 기본 레이아웃
  fragments/header.html     → 헤더
  fragments/nav.html        → 네비게이션
  fragments/footer.html     → 푸터
  fragments/pagination.html → 페이지네이션
  let/{모듈}/{기능}/{화면}.html
```

### 5-2. KRDS 클래스 사용 원칙
- 로컬 파일 사용 (CDN 사용 금지)
  - CSS: `/static/css/krds.css`
  - JS: `/static/js/krds.js`
- Bootstrap 5 기반 `krds-*` 클래스 적용
- 버튼: `krds-btn`, 폼: `krds-form-control`, 테이블: `krds-table`

### 5-3. URL 패턴
| 기능 | MVC URL | REST API URL |
|------|---------|--------------|
| 로그인 | GET/POST /login | POST /auth/login-jwt |
| 게시물 목록 | GET /bbs/{bbsId}/list | GET /board |
| 게시물 상세 | GET /bbs/{bbsId}/{nttId} | GET /board/{bbsId}/{nttId} |
| 게시물 등록 | GET/POST /bbs/{bbsId}/write | POST /board |
| 일정 | GET /schedule | GET /schedule/daily |
| 회원 목록 | GET /member/list | GET /etc/member/list |

---

## 6. 빌드 및 실행 절차

### 6-1. 환경 변수 (eGovCI-5.0.0-Windows-64bit 기준)
```
JAVA_HOME = C:\eGovFrame\eGovCI-5.0.0-Windows-64bit\bin\jdk-17.0.17+10
Maven     = C:\eGovFrame\eGovCI-5.0.0-Windows-64bit\bin\apache-maven-3.9.9\bin\mvn.cmd
```

### 6-2. bash(Bash 도구)에서 빌드·실행
```bash
export JAVA_HOME="/c/eGovFrame/eGovCI-5.0.0-Windows-64bit/bin/jdk-17.0.17+10"
export PATH="$JAVA_HOME/bin:$PATH"
cd "/c/eGovFrame/workspace-egov/simple-home-boot"
MVN="/c/eGovFrame/eGovCI-5.0.0-Windows-64bit/bin/apache-maven-3.9.9/bin/mvn"

# 컴파일 검증
$MVN clean compile -q

# 서버 실행 (백그라운드)
$MVN spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xmx512m -Xms256m -Dfile.encoding=UTF-8" \
  > /tmp/spring-boot.log 2>&1 &

# 시작 확인
grep "Started EgovBoot" /tmp/spring-boot.log
```

### 6-3. 포트 충돌 시 프로세스 종료 (PowerShell)
```powershell
$connections = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
foreach ($conn in $connections) {
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
}
```

### 6-4. 빌드 전 체크리스트
- [ ] 매퍼 XML 테이블명이 TB_ 규칙을 따르는지 확인
- [ ] shtdb.sql 문법 오류 없는지 확인
- [ ] application.properties `Globals.DbType=hsql` 확인
- [ ] 감사 컬럼 INSERT/UPDATE 구문에 포함되었는지 확인

### 6-5. DB 정보
- **내장 HSQLDB** 사용: `EmbeddedDatabaseType.HSQL`로 별도 서버 불필요
- 초기 데이터: `src/main/resources/db/shtdb.sql`
- 테스트 계정: `admin` / `admin` (ID/비밀번호 동일)

### 6-6. 비밀번호 해시 알고리즘
```
저장 형식 = 이중 SHA-256 해시 (Base64)
- 1차 해시 = Base64(SHA256(id + password))   ← 폼 전송 후 서버에서 적용
- 2차 해시 = Base64(SHA256(id + 1차해시))    ← DB 저장 형식
신규 비밀번호 생성:  EgovFileScrty.encryptPasswordTwice(plaintext, id)
```

**PowerShell로 해시 계산:**
```powershell
$sha256 = [System.Security.Cryptography.SHA256]::Create()
function Get-EgovPwd($pw, $id) {
    $h1 = [Convert]::ToBase64String($sha256.ComputeHash([Text.Encoding]::UTF8.GetBytes($id + $pw)))
    return [Convert]::ToBase64String($sha256.ComputeHash([Text.Encoding]::UTF8.GetBytes($id + $h1)))
}
Get-EgovPwd "admin" "admin"  # → 3hoZ/BiR1c8t8cVoMBxUxK1aNlljp6bb/JS2tpqaO5Y=
Get-EgovPwd "user" "user"   # → rTozi2u1iWZ8AikX9o06EtgDVxXtwNV7Eb600ai5Amk=
```

---

## 7. 테이블명 일괄 치환 스크립트 (PowerShell)

신규 DBMS 추가 또는 추가 파일에서 테이블명 치환이 필요할 때 사용한다.

```powershell
$targetPath = "대상_디렉터리_경로"
$replacements = [ordered]@{
    'LETTCCMMNCLCODE'           = 'TB_CMMN_CL_CODE'
    'LETTCCMMNDETAILCODE'       = 'TB_CMMN_DETAIL_CODE'
    'LETTCCMMNCODE'             = 'TB_CMMN_CODE'
    'LETTHEMPLYRINFOCHANGEDTLS' = 'TB_EMPLYR_INFO_CHNG_DTLS'
    'LETTNAUTHORGROUPINFO'      = 'TB_AUTHOR_GROUP_INFO'
    'LETTNAUTHORINFO'           = 'TB_AUTHOR_INFO'
    'LETTNBBSMASTEROPTN'        = 'TB_BBS_MASTER_OPTN'
    'LETTNBBSMASTER'            = 'TB_BBS_MASTER'
    'LETTNBBSUSE'               = 'TB_BBS_USE'
    'LETTNBBS'                  = 'TB_BBS'
    'LETTNEMPLYRSCRTYESTBS'     = 'TB_EMPLYR_SCRTY_ESTBS'
    'LETTNEMPLYRINFO'           = 'TB_EMPLYR_INFO'
    'LETTNENTRPRSMBER'          = 'TB_ENTRPRS_MBER'
    'LETTNFILEDETAIL'           = 'TB_FILE_DETAIL'
    'LETTNFILE'                 = 'TB_FILE'
    'LETTNGNRLMBER'             = 'TB_GNRL_MBER'
    'LETTNORGNZTINFO'           = 'TB_ORGNZT_INFO'
    'LETTNSCHDULINFO'           = 'TB_SCHDUL_INFO'
    'COMVNUSERMASTER'           = 'VW_USER_MASTER'
}
Get-ChildItem -Path $targetPath -Recurse -Include "*.xml","*.sql" | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $original = $content
    foreach ($old in $replacements.Keys) { $content = $content -replace $old, $replacements[$old] }
    if ($content -ne $original) {
        Set-Content $_.FullName -Value $content -Encoding UTF8 -NoNewline
        Write-Host "수정: $($_.Name)"
    }
}
```

---

## 8. Thymeleaf 템플릿 개발 시 주의사항

### 8-1. VO/DTO 필드명 매핑 (자주 틀리는 항목)
| 잘못된 표현 | 올바른 표현 | VO/DTO |
|-------------|-------------|--------|
| `searchVO.searchWrd` | `searchVO.searchKeyword` | `ComDefaultVO`, `UserDefaultVO` |
| `searchVO.searchCnd` | `searchVO.searchCondition` | `ComDefaultVO`, `UserDefaultVO` |
| `item.frstRegistPnttm` | `item.frstRegisterPnttm` | `BbsDetailResponseDTO` 상속 계열 |
| `item.rdcnt` | `item.inqireCo` | `BbsDetailResponseDTO` |
| `result.item.*` | `result.boardVO.*` | `BbsManageDetailResponseDTO` |

### 8-2. 인증 방식
- **JWT 쿠키 방식**: `ACCESS_TOKEN` HttpOnly 쿠키
- CSRF 비활성화: `csrf(AbstractHttpConfigurer::disable)`
- 세션 비활성화: `SessionCreationPolicy.STATELESS`
- 로그인 URL: `POST /login` (파라미터: `id`, `password`)

### 8-3. 접근 권한
| URL 패턴 | 필요 권한 |
|----------|---------|
| `/`, `/login`, `/error`, `/css/**`, `/js/**` | 누구나 |
| `/bbs/{bbsId}/list`, `/bbs/{bbsId}/{nttId}/detail` | 로그인 필요 |
| `/bbs/master/**`, `/bbs/use/**`, `/member/**` | ROLE_ADMIN |
| `/schedule/**` | 로그인 필요 |
| `/admin/**` | ROLE_ADMIN |

### 8-4. Bootstrap Icons 로컬 적용
```html
<!-- layouts/default.html에 포함됨 -->
<link rel="stylesheet" th:href="@{/css/bootstrap-icons.min.css}">
<!-- 폰트 파일 위치: /static/css/fonts/bootstrap-icons.woff2 -->
```

---

## 9. 변경 이력

| 날짜 | 작업 |
|------|------|
| 2026-06-02 | SKILL.md 최초 작성 |
| 2026-06-02 | TB_ 테이블 명명규칙 전면 적용 |
| 2026-06-02 | 감사 컬럼 전 테이블 추가 완료 |
| 2026-06-02 | PostgreSQL DDL/DML 추가 |
| 2026-06-02 | Bootstrap Icons 로컬 파일 추가 (bootstrap-icons.min.css + woff2) |
| 2026-06-02 | 비밀번호 이중해시 알고리즘 반영 (admin/user 테스트 계정 수정) |
| 2026-06-02 | Thymeleaf 필드명 오류 수정 (frstRegistPnttm→frstRegisterPnttm, searchWrd→searchKeyword 등) |
| 2026-06-02 | 전체 화면 HTTP 200 동작 확인 완료 |
