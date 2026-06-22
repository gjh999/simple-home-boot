# eGovFrame 5.0 프로젝트 — Claude 컨텍스트

## 프로젝트 개요

- **프레임워크**: 전자정부표준프레임워크(eGovFrame) 5.0 + Spring Boot 3.x + Thymeleaf
- **Java**: 17 (JDK 17)
- **DB**: HSQL(개발, 내장 EmbeddedDB) / PostgreSQL / MySQL / Oracle / Altibase / Tibero / CUBRID
- **서버 포트**: 8080, context-path: /
- **패키지 루트**: `egovframework`
- **빌드 도구**: Maven 3.9.9 (eGovCI-5.0.0-Windows-64bit 내장)

## 빌드·실행 환경 (실제 경로)

```
JDK 17  : C:\eGovCI-5.0.0-Windows-64bit\bin\jdk-17.0.17+10
Maven   : C:\eGovCI-5.0.0-Windows-64bit\bin\apache-maven-3.9.9\bin\mvn.cmd
```

### bash(Bash 도구)에서 실행
```bash
export JAVA_HOME="/c/eGovCI-5.0.0-Windows-64bit/bin/jdk-17.0.17+10"
export PATH="$JAVA_HOME/bin:$PATH"
cd "/c/eGovFrame/workspace-egov/simple-home-boot"

# 컴파일 검증
/c/eGovCI-5.0.0-Windows-64bit/bin/apache-maven-3.9.9/bin/mvn clean compile -q

# 서버 실행
/c/eGovCI-5.0.0-Windows-64bit/bin/apache-maven-3.9.9/bin/mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="-Xmx512m -Xms256m -Dfile.encoding=UTF-8"
```

### 포트 충돌 시 프로세스 종료 (PowerShell)
```powershell
$connections = Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue
foreach ($conn in $connections) {
    Stop-Process -Id $conn.OwningProcess -Force -ErrorAction SilentlyContinue
}
```

## 테스트 계정

| 계정 | ID | 비밀번호 | 권한 |
|------|-----|---------|------|
| 관리자 | `admin` | `1` | ROLE_ADMIN |
| 일반 사용자 | `user` | `user` | ROLE_USER |

> 비밀번호는 이중 SHA-256 해시(`egov-project` 스킬 §6-6 참조)로 저장됨

## 디렉터리 구조

```
src/main/java/egovframework/
  com/          # 공통(인증·파일·설정·보안·JWT)
    cmm/        # 공통 VO·서비스·유틸
    config/     # Spring 설정 클래스
    jwt/        # JWT 유틸·필터
    security/   # SecurityConfig, WebMvcConfig
  let/          # 업무 기능
    cop/bbs/    # 게시판
    cop/smt/sim/# 일정관리
    cop/com/    # 게시판 사용정보
    main/       # 메인
    uat/uia/    # 로그인
    uat/esm/    # 사이트 관리
    uss/umt/    # 회원관리
    utl/        # 유틸

src/main/resources/
  egovframework/mapper/   # MyBatis XML (DB별 분리)
  egovframework/message/  # 다국어 메시지
  egovframework/validator/# 검증 XML
  db/shtdb.sql            # HSQL 초기화 DDL+DML
  static/                 # 정적 리소스 (CSS/JS/이미지)
    css/bootstrap.min.css
    css/bootstrap-icons.min.css
    css/fonts/            # Bootstrap Icons 폰트
    css/krds.css
    js/bootstrap.bundle.min.js
    js/krds.js
    js/common.js
  templates/              # Thymeleaf 템플릿
    layouts/default.html  # 기본 레이아웃
    layouts/login.html    # 로그인 레이아웃
    fragments/            # header, nav, footer, pagination
    let/{모듈}/           # 기능별 화면

DATABASE/                 # DBMS별 DDL+DML (배포용)
  all_sht_ddl_{dbms}.sql
  all_sht_data_{dbms}.sql

Docs/                     # 참조 문서
  단어규칙.md              # 공공데이터 공통표준 단어 (3284개)
  도메인규칙.md            # 공통표준 도메인 (129개)
  용어규칙.md              # 공통표준 용어 (대용량)
  db-schema-guide.md      # DB 스키마 가이드
```

## 아키텍처 패턴

### 레이어 구조
```
Controller (@Controller) → Service (interface) → ServiceImpl → DAO (MyBatis Mapper)
REST Controller (@RestController, 기존 유지) → 동일 Service 레이어 공유
```

### 컨트롤러 명명 규칙
- MVC: `Egov{기능명}Controller` (`@Controller`, Thymeleaf 화면)
- REST: `Egov{기능명}ApiController` (`@RestController`, JSON API)

### URL 패턴
| 기능 | MVC (Thymeleaf) | REST API |
|------|-----------------|----------|
| 로그인 | `GET/POST /login` | `POST /auth/login-jwt` |
| 게시물 목록 | `GET /bbs/{bbsId}/list` | `GET /board` |
| 게시물 상세 | `GET /bbs/{bbsId}/{nttId}/detail` | `GET /board/{bbsId}/{nttId}` |
| 게시물 등록 | `GET/POST /bbs/{bbsId}/write` | `POST /board` |
| 일정 목록 | `GET /schedule` | `GET /schedule/daily` |
| 회원 목록 | `GET /member/list` | `GET /etc/member/list` |

## DB 명명 규칙

### 테이블명
- **접두어**: `TB_` (모든 업무 테이블 공통)
- **스네이크 케이스** 대문자 (예: `TB_BBS_MASTER`, `TB_EMPLYR_INFO`)
- 구 명명규칙(`LETTN`, `LETTC`, `LETTH`) → 전면 폐기

### 테이블명 매핑표 (구 → 신)
| 구 테이블명 | 신 테이블명 | 설명 |
|-------------|-------------|------|
| LETTCCMMNCLCODE | TB_CMMN_CL_CODE | 공통코드 대분류 |
| LETTCCMMNCODE | TB_CMMN_CODE | 공통코드 중분류 |
| LETTCCMMNDETAILCODE | TB_CMMN_DETAIL_CODE | 공통코드 소분류 |
| LETTNAUTHORINFO | TB_AUTHOR_INFO | 권한 |
| LETTNAUTHORGROUPINFO | TB_AUTHOR_GROUP_INFO | 권한그룹 |
| LETTNORGNZTINFO | TB_ORGNZT_INFO | 조직 |
| LETTNEMPLYRINFO | TB_EMPLYR_INFO | 사용자/직원 |
| LETTHEMPLYRINFOCHANGEDTLS | TB_EMPLYR_INFO_CHNG_DTLS | 사용자 변경이력 |
| LETTNEMPLYRSCRTYESTBS | TB_EMPLYR_SCRTY_ESTBS | 사용자-권한 매핑 |
| LETTNGNRLMBER | TB_GNRL_MBER | 일반회원 |
| LETTNENTRPRSMBER | TB_ENTRPRS_MBER | 기업회원 |
| LETTNBBSMASTER | TB_BBS_MASTER | 게시판 마스터 |
| LETTNBBSMASTEROPTN | TB_BBS_MASTER_OPTN | 게시판 옵션 |
| LETTNBBSUSE | TB_BBS_USE | 게시판 사용대상 |
| LETTNBBS | TB_BBS | 게시물 |
| LETTNFILE | TB_FILE | 첨부파일 헤더 |
| LETTNFILEDETAIL | TB_FILE_DETAIL | 첨부파일 상세 |
| LETTNSCHDULINFO | TB_SCHDUL_INFO | 일정 |
| LETTNCMMNTY | TB_CMMNTY | 커뮤니티 |
| LETTNCMMNTYUSER | TB_CMMNTY_USER | 커뮤니티 사용자 |
| LETTNCLUB | TB_CLUB | 동호회 |
| LETTNCLUBUSER | TB_CLUB_USER | 동호회 사용자 |
| LETTNSTPLATINFO | TB_STPLAT_INFO | 이용약관 |
| LETTNDIARYINFO | TB_DIARY_INFO | 일정 다이어리 |
| COMVNUSERMASTER (VIEW) | VW_USER_MASTER | 사용자 통합 뷰 |

### 감사 컬럼 (Audit Columns — 모든 테이블 필수, 전 테이블 적용 완료)
| 컬럼명 | 타입 | 설명 |
|--------|------|------|
| `FRST_REGIST_PNTTM` | TIMESTAMP | 등록일시 |
| `FRST_REGISTER_ID` | VARCHAR(20) | 등록자ID |
| `LAST_UPDT_PNTTM` | TIMESTAMP | 수정일시 |
| `LAST_UPDUSR_ID` | VARCHAR(20) | 수정자ID |

### 주요 약어 매핑 (단어규칙)
| 한국어 | 영문약어 | 한국어 | 영문약어 |
|--------|----------|--------|----------|
| 사용자/직원 | EMPLYR | 파일 | FILE |
| 게시판 | BBS | 코드 | CODE |
| 게시물 | NTT | 여부 | AT |
| 조직 | ORGNZT | 시각 | PNTTM |
| 권한 | AUTHOR | 명칭 | NM |
| 일정 | SCHDUL | 설명 | DC |
| 회원 | MBER | 아이디 | ID |

## 코딩 표준

### Java
- **Lombok** 사용: `@RequiredArgsConstructor`, `@Slf4j`
- 생성자 주입 방식 (필드 주입 금지)
- 서비스는 반드시 인터페이스 + Impl 분리
- DAO는 `EgovAbstractMapper` 상속 또는 `@Mapper` 애노테이션

### VO/DTO 구분
- `*VO`: 영속 도메인 객체 (DB 컬럼과 매핑)
- `*RequestDTO`: 요청 파라미터 수신
- `*ResponseDTO`: 응답 데이터 전달
- 페이징: `ComDefaultVO` 상속 (searchKeyword, searchCondition, pageIndex 등)

### MyBatis
- SQL XML 파일: `EgovXxx_SQL_{db타입}.xml` 형태로 DB별 분리
- resultMap 사용 필수 (camelCase ↔ SNAKE_CASE 자동 매핑: `mapper-config.xml`)

### Thymeleaf 템플릿
- 레이아웃: `templates/layouts/default.html` (Thymeleaf Layout Dialect)
- 프래그먼트: `templates/fragments/` (header, nav, footer, pagination)
- 페이지 경로: `templates/let/{모듈}/`

### KRDS 디자인 표준 (디지털정부 표준 디자인시스템, https://www.krds.go.kr)
- **Bootstrap 프레임워크 미사용**. 공식 **KRDS HTML Component Kit**(`static/krds/`)으로 전환.
  - `bootstrap.min.css`·`bootstrap.bundle.min.js` 삭제. (Bootstrap Icons `bi-*` 폰트는 아이콘용으로만 유지)
- **로드 순서**(레이아웃): `bootstrap-icons` → `/krds/resources/cdn/krds.min.css`(공식, 1,835 토큰) →
  `common.css` → `krds-compat.css` → `krds.css`. **JS**: `krds-compat.js` → `krds.js` → `common.js`.
- **`krds-compat.css`**: Bootstrap이 제공하던 그리드(`row/col-*`)·유틸리티·컴포넌트 구조(btn/card/table/form/
  breadcrumb/pagination/navbar/dropdown)를 KRDS 토큰으로 재구현 + `@font-face`(Pretendard GOV).
- **`krds-compat.js`**: `data-bs-*`(드롭다운·모바일 collapse·알림 닫기) 동작을 바닐라 JS로 대체.
- 정적 리소스: 로컬만 사용(CDN 금지). krds.min.css의 원격 아이콘은 `/krds/resources/img/`로 로컬화.
  `SecurityConfig`/`WebMvcConfig`에 `/krds/**` 허용 필수.
- 폰트: **Pretendard GOV**(`/krds/resources/fonts/PretendardGOV-*.subset.woff2`).
- 접근성(KWCAG): skip-nav, `:focus-visible` 포커스 링, 명도대비. 상세·MD 가이드라인: `Docs/krds-적용-가이드.md`,
  `Docs/krds-uiux-가이드라인(2025.08).md`, `Docs/krds-uiux-자체검증-체크리스트.md`.

### KRDS 전환 룰 (화면 작업 시 필수 — 스킬 `krds-conversion` 참조)
- **시각 클래스만 교체. 로직(`th:*`/`sec:*`/`layout:*`)·`name`/`id`/`action`/URL은 절대 변경 금지.**
- **컴포넌트는 KRDS 네이티브, 그리드·유틸·GNB는 호환 레이어 유지**(KRDS는 그리드/유틸 미제공 → `krds-compat.css` 전면 제거 금지).
- 클래스 매핑(요지): `card→krds-panel(head/body)`, `egov-table→krds-table-wrap>table.tbl`(colgroup/caption/`scope`),
  정의표`table.tbl.col`(`th[scope=row]`), `btn btn-primary→krds-btn primary`, `btn-outline-primary→secondary`,
  `btn-outline-secondary/secondary→tertiary`, `btn-outline-danger→danger`, `btn-sm/lg→small/large`,
  폼 `mb-3→form-group`(여러개 `fieldset`), `form-label→form-tit>label`, `form-control→krds-input`(`form-conts`),
  `form-select→krds-form-select`, `form-text→p.form-hint`, `text-danger *→frm-rq *`, `badge bg-*→krds-badge bg-*`(`bg-secondary→bg-gray`).
  `invalid-feedback` 삭제 + `novalidate` 제거(native validation), `alert-dismissible fade show` 제거.
- **그대로 둘 것**: `container-fluid/row/col-*/d-flex/justify-*/gap-*/여백/text-*/small/w-100/form-check/input-group`. 아이콘 `bi-*` 유지(`me-1` 제거).
- 토큰 10px↔root 16px 차이는 `krds.css` `!important` 정규화로 해결(root는 16px 유지). 삭제 버튼은 보조 클래스 `.krds-btn.danger`.
- 폰트: Pretendard GOV woff2 **preload** + `@font-face{font-display:optional}`(클릭 후 글꼴 교체 깜빡임 제거).
- 공개 페이지(랜딩/`/krds-sample`)는 `layouts/public.html`(공통 헤더/푸터) decorate, 업무 페이지는 `layouts/default.html`.
- 작업 후 **자체검증 전수검사**: `.claude/skills/krds-conversion/scripts/krds-verify.sh`(HTTP200/오류0/레거시0/KRDS존재) +
  체크리스트 요약(`Docs/krds-uiux-자체검증-체크리스트-요약.md`) P/F/E/N/A 판정. 요약 가이드: `Docs/krds-uiux-가이드라인(2025.08)-요약.md`.

### 다국어(i18n) 룰 — 신규 기능·화면 추가/변경 시 **반드시 확인** (스킬 `egov-component` §8)
- 사용자 노출 텍스트(라벨·버튼·placeholder·title·alt·검증 메시지)는 **하드코딩 금지** → Thymeleaf `th:text="#{key}"`.
- 메시지는 `egovframework/message/message-ui_{ko,en}.properties`에 **ko·en 동일 키**로 추가(프레임워크 공통은 `message/com/message-common_{ko,en}`). 언어전환 `/cmm/lang(lang='ko'|'en')`.
- **변경/추가 작업마다**: ① 노출 문구 전부 메시지 키 처리 ② ko/en 양쪽 값 존재(키 집합 일치) ③ 누락/한글 fallback 0 — 확인.
- 기존 하드코딩 화면도 **건드리면 함께 i18n 전환**. 검증: `comm -3`로 ko/en 키 diff, 변경 html에서 `#{` 없는 한글 스캔.

## 보안

- **인증**: JWT 쿠키 방식 (`ACCESS_TOKEN` HttpOnly 쿠키)
- **CSRF**: 비활성화 (`csrf(AbstractHttpConfigurer::disable)`)
- **세션**: STATELESS (`SessionCreationPolicy.STATELESS`)
- **로그인**: `POST /login` (파라미터: `id`, `password`)
- **XSS**: HTMLTagFilter 적용
- **파일 업로드**: 확장자 화이트리스트 검사

### 접근 권한
| URL 패턴 | 필요 권한 |
|----------|---------|
| `/`, `/login`, `/css/**`, `/js/**` | 누구나 |
| `/bbs/{bbsId}/**`, `/schedule/**` | 로그인 필요 |
| `/bbs/master/**`, `/bbs/use/**`, `/member/**`, `/admin/**` | ROLE_ADMIN |

## 주요 설정 파일

| 파일 | 설명 |
|------|------|
| `src/main/resources/application.properties` | 메인 설정 (DB타입: hsql, 포트: 8080) |
| `src/main/resources/application-dev.properties` | 개발 환경 오버라이드 |
| `src/main/resources/db/shtdb.sql` | HSQL 초기화 DDL+DML |
| `EgovConfigAppDatasource.java` | 내장 HSQLDB 설정 (별도 서버 불필요) |
| `SecurityConfig.java` | Spring Security 설정 |

## 작업 규칙

### 자율 작업 원칙
1. 모든 진행 상태·설명·요약은 **한글**로 출력
2. 모든 단계를 묻지 않고 처음부터 끝까지 **완전 자율**로 진행
3. 막히는 부분은 합리적인 판단으로 결정하여 계속 진행
4. 완료 후에는 **결과만 요약**해서 보고

### 표준 참조 순서 (신규 작업 시)
1. `CLAUDE.md` → 전체 구조 파악
2. `Docs/단어규칙.md` → 영문약어 결정
3. `Docs/도메인규칙.md` → 타입·길이 결정
4. `Docs/용어규칙.md` → 컬럼명 확인
5. `egov-project` 스킬(`.claude/skills/egov-project/SKILL.md`) → 구현 절차·검증 체크리스트
   - 화면 KRDS 전환은 `krds-conversion`, 신규 컴포넌트 스캐폴딩은 `egov-component` 스킬 사용

## 구현 현황 및 DB 변경 이력

### 구현된 기능 (2026-06-02 완료)
| 기능 | MVC 컨트롤러 | Thymeleaf 화면 | URL |
|------|-------------|--------------|-----|
| 로그인/로그아웃 | EgovLoginController | loginView.html | `/login` |
| 메인 | EgovMainController | mainView.html | `/` |
| 게시판 CRUD | EgovBBSManageController | bbs/*.html | `/bbs/{bbsId}/**` |
| 게시판 마스터 | EgovBBSAttributeManageController | bbs/master/*.html | `/bbs/master/**` |
| 게시판 사용정보 | EgovBBSUseInfoManageController | bbs/use/*.html | `/bbs/use/**` |
| 일정관리 | EgovIndvdlSchdulManageController | schedule*.html | `/schedule/**` |
| 회원관리 | EgovMberManageController | member*.html | `/member/**` |
| 비밀번호 변경 | EgovSiteManagerController | adminPassword.html | `/admin/password` |

### DB 스키마 변경 이력
| 날짜 | 변경 내용 |
|------|-----------|
| 2026-06-02 | LET 특성 제거, 테이블명 TB_ 스네이크케이스 전면 개편 |
| 2026-06-02 | 감사 컬럼(4개) 전 테이블 적용 |
| 2026-06-02 | PostgreSQL DDL/DML 추가 |
| 2026-06-02 | 매퍼 XML 60개 파일 테이블명 일괄 치환 |
| 2026-06-02 | admin 비밀번호 이중해시 수정 (pw: 1) |
| 2026-06-16 | 매퍼 잔존 구명칭 6종 → TB_ 전환(커뮤니티/동호회/약관/다이어리). 매퍼 28파일 치환 + 전 7종 DBMS(HSQL·PostgreSQL·MySQL·Oracle·Altibase·Tibero·CUBRID) DDL에 TB_CMMNTY·TB_CMMNTY_USER·TB_CLUB·TB_CLUB_USER·TB_STPLAT_INFO·TB_DIARY_INFO 신설(감사컬럼 포함). 컬럼명은 기존부터 규칙 준수 |
| 2026-06-22 | DDL 가독성: 6종 DBMS DDL 전 컬럼에 인라인 한글주석(`-- 한글명`) 부여(각 285개). 시드 INSERT 전건 컬럼명 명시(postgresql 32·HSQL 44 보완 → 전 7파일 100%). 비파괴적(주석/컬럼명만). 컬럼 한글명 사전: `Docs/db-컬럼-한글명-매핑.md`. HSQL 재기동 무오류로 정합 검증 |

## 코드베이스 구축 작업 체크리스트

### 0️⃣ 환경 준비
- [x] 서버 구동 확인 (포트 8080, JDK 17, Maven 3.9.9, 내장 HSQLDB)
- [x] CLAUDE.md 작업 규칙 갱신
- [x] 표준 문서 학습 (Docs 폴더 — 단어/도메인/용어 규칙 CLAUDE.md에 반영)

### 1️⃣ 표준 학습 및 정의
- [x] `Docs/단어규칙.md` (3284개 표준단어) 학습 및 주요 약어 CLAUDE.md 반영
- [x] `Docs/도메인규칙.md` (129개 표준도메인) 학습
- [x] `Docs/용어규칙.md` (표준용어) 학습
- [x] 테이블명: LET 특성 제거 → TB_ 스네이크케이스 전면 적용
- [x] 감사 컬럼 4개 전 테이블 추가 완료

### 2️⃣ 기능 구현
- [x] 기존 REST API 컨트롤러 분석
- [x] 각 컨트롤러마다 Thymeleaf MVC 버전 구현 (8개 기능)
- [x] KRDS 디자인 시스템 로컬 CSS/JS 적용
- [x] Bootstrap Icons 로컬 파일 추가
- [x] 모든 템플릿에 레이아웃 + 프래그먼트 적용

### 3️⃣ 점검 및 구동
- [x] DB 테이블명/컬럼명 규칙 전수 검증 (TB_ 적용, 감사컬럼 확인)
- [x] Maven 빌드 성공 확인 (컴파일 오류 없음)
- [x] 서버 구동 및 전체 URL HTTP 200 동작 검증 (13개 URL)
- [x] 로그인 기능 검증 (admin/1, user/user)

### 4️⃣ 스킬 및 구조화
- [x] SKILL.md 작성 (컨텍스트 엔지니어링 + 하네스 엔지니어링)
- [x] 재사용 가능한 절차 정의 (비밀번호 해시, 빌드 경로, 템플릿 주의사항)

## DBMS별 SQL 변환 규칙

| DBMS | 기본 타입 | 시퀀스 | 현재 날짜 | 비고 |
|------|---------|--------|----------|------|
| HSQL | VARCHAR/INT/TIMESTAMP | IDS 테이블 | NOW() | 내장 메모리 DB, 개발용 |
| PostgreSQL | VARCHAR/INTEGER/TIMESTAMP | CREATE SEQUENCE | NOW() | 프로덕션용 |
| MySQL | VARCHAR/INT/DATETIME | AUTO_INCREMENT | NOW() | - |
| Oracle | VARCHAR2/NUMBER/DATE | CREATE SEQUENCE | SYSDATE | - |
