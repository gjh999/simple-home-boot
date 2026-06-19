# DB 명명 매핑 (구 → 신) — 테이블·컬럼

> - 목적 : eGov 레거시 명명(`LETT*`/`COMVN*`)에서 표준 `TB_`/`VW_` 명명으로의 전환 내역을 한곳에 정리하여,
>   기여자가 구 코드/문서와 현행 스키마를 대조할 수 있도록 합니다.
> - 대상 : `src/main/resources/db/shtdb.sql`(개발용 HSQL 시드), `DATABASE/all_sht_{ddl,data}_{dbms}.sql`(운영 배포용 6종 DBMS).
> - 컬럼 한글 논리명(상세) : [db-schema-guide.md](db-schema-guide.md) 참조. 표준 단어/도메인/용어 : `단어규칙.md`·`도메인규칙.md`·`용어규칙.md`.
> - **컬럼 변경 정책** : 컬럼명은 처음부터 공공데이터 공통표준 용어를 준수했으므로 **물리 컬럼명 변경은 없습니다**.
>   이번 전환에서 바뀐 것은 **테이블명(접두어·구조)** 이며, 일부 테이블은 약어 축약이 적용되었습니다(예: `LETTHEMPLYRINFOCHANGEDTLS → TB_EMPLYR_INFO_CHNG_DTLS`).

---

## 1. 명명 규칙 변경 요약

| 구분 | 구(舊) | 신(新) |
|------|--------|--------|
| 테이블 접두어 | `LETTN`(일반)·`LETTC`(코드)·`LETTH`(이력) | `TB_` 단일 접두어 |
| 표기 | 연속 대문자(언더스코어 없음) | **SNAKE_CASE 대문자** (`TB_BBS_MASTER`) |
| 뷰 | `COMVN*` | `VW_` 접두어 (`VW_USER_MASTER`) |
| 감사 컬럼 | 일부 누락 | 전 테이블 4종 필수 적용 |

**감사 컬럼 4종(전 테이블 공통)**

| 컬럼 | 타입 | 한글 논리명 |
|------|------|------------|
| `FRST_REGIST_PNTTM` | TIMESTAMP | 최초등록시각 |
| `FRST_REGISTER_ID` | VARCHAR(20) | 최초등록자ID |
| `LAST_UPDT_PNTTM` | TIMESTAMP | 최종수정시각 |
| `LAST_UPDUSR_ID` | VARCHAR(20) | 최종수정자ID |

---

## 2. 테이블명 매핑 (구 → 신)

| 구 테이블명 | 신 테이블명 | 한글명 | PK |
|-------------|-------------|--------|-----|
| LETTCCMMNCLCODE | TB_CMMN_CL_CODE | 공통코드 대분류 | CL_CODE |
| LETTCCMMNCODE | TB_CMMN_CODE | 공통코드 중분류 | CODE_ID |
| LETTCCMMNDETAILCODE | TB_CMMN_DETAIL_CODE | 공통코드 소분류 | (CODE_ID, CODE) |
| LETTNAUTHORINFO | TB_AUTHOR_INFO | 권한 | AUTHOR_CODE |
| LETTNAUTHORGROUPINFO | TB_AUTHOR_GROUP_INFO | 권한그룹 | GROUP_ID |
| LETTNORGNZTINFO | TB_ORGNZT_INFO | 조직 | ORGNZT_ID |
| LETTNEMPLYRINFO | TB_EMPLYR_INFO | 사용자/직원 | EMPLYR_ID |
| LETTHEMPLYRINFOCHANGEDTLS | TB_EMPLYR_INFO_CHNG_DTLS | 사용자 변경이력 | (EMPLYR_ID, CHANGE_DE) |
| LETTNEMPLYRSCRTYESTBS | TB_EMPLYR_SCRTY_ESTBS | 사용자-권한 매핑 | SCRTY_DTRMN_TRGET_ID |
| LETTNGNRLMBER | TB_GNRL_MBER | 일반회원 | MBER_ID |
| LETTNENTRPRSMBER | TB_ENTRPRS_MBER | 기업회원 | ENTRPRS_MBER_ID |
| LETTNBBSMASTER | TB_BBS_MASTER | 게시판 마스터 | BBS_ID |
| LETTNBBSMASTEROPTN | TB_BBS_MASTER_OPTN | 게시판 옵션 | BBS_ID |
| LETTNBBSUSE | TB_BBS_USE | 게시판 사용대상 | (BBS_ID, TRGET_ID) |
| LETTNBBS | TB_BBS | 게시물 | (NTT_ID, BBS_ID) |
| LETTNFILE | TB_FILE | 첨부파일 묶음 헤더 | ATCH_FILE_ID |
| LETTNFILEDETAIL | TB_FILE_DETAIL | 첨부파일 상세 | (ATCH_FILE_ID, FILE_SN) |
| LETTNSCHDULINFO | TB_SCHDUL_INFO | 일정 | SCHDUL_ID |
| LETTNCMMNTY | TB_CMMNTY | 커뮤니티 | CMMNTY_ID |
| LETTNCMMNTYUSER | TB_CMMNTY_USER | 커뮤니티 사용자 | ESNTL_ID |
| LETTNCLUB | TB_CLUB | 동호회 | CLB_ID |
| LETTNCLUBUSER | TB_CLUB_USER | 동호회 사용자 | ESNTL_ID |
| LETTNSTPLATINFO | TB_STPLAT_INFO | 이용약관 | USE_STPLAT_ID |
| LETTNDIARYINFO | TB_DIARY_INFO | 일정 다이어리 | DIARY_ID |
| COMVNUSERMASTER (VIEW) | VW_USER_MASTER | 사용자 통합 뷰 | - |
| (내장 시퀀스) | IDS | 내장샘플 시퀀스 | TABLE_NAME |
| (범용 시퀀스) | COMTECOPSEQ | 범용 시퀀스 | TABLE_NAME |

> 비고: `IDS`/`COMTECOPSEQ`는 eGov 표준 내장 시퀀스 테이블로 명칭을 유지합니다.

---

## 3. 컬럼 사전 (물리명 → 한글 논리명)

> 컬럼명은 전환 전후 동일(표준 용어 준수)합니다. 아래는 시드 데이터에 등장하는 주요 물리 컬럼의 한글 논리명입니다.
> 테이블별 전체 컬럼 정의는 [db-schema-guide.md](db-schema-guide.md)를 참조하세요.

### 공통/감사
| 물리명 | 한글명 | 물리명 | 한글명 |
|--------|--------|--------|--------|
| USE_AT | 사용여부(Y/N) | ESNTL_ID | 고유ID |
| FRST_REGIST_PNTTM | 최초등록시각 | FRST_REGISTER_ID | 최초등록자ID |
| LAST_UPDT_PNTTM | 최종수정시각 | LAST_UPDUSR_ID | 최종수정자ID |
| REGIST_SE_CODE | 등록구분코드 | TABLE_NAME | 대상 테이블명 |

### 코드
| 물리명 | 한글명 | 물리명 | 한글명 |
|--------|--------|--------|--------|
| CL_CODE | 분류코드 | CL_CODE_NM | 분류코드명 |
| CODE_ID | 코드ID | CODE_ID_NM | 코드ID명 |
| CODE | 상세코드 | CODE_NM | 상세코드명 |
| *_DC | (코드)설명 | NEXT_ID | 다음 ID 값 |

### 권한·조직·사용자·회원
| 물리명 | 한글명 | 물리명 | 한글명 |
|--------|--------|--------|--------|
| AUTHOR_CODE | 권한코드 | AUTHOR_NM | 권한명 |
| GROUP_ID | 권한그룹ID | GROUP_NM | 그룹명 |
| ORGNZT_ID | 조직ID | ORGNZT_NM | 조직명 |
| EMPLYR_ID | 사용자ID | USER_NM | 사용자명 |
| MBER_ID | 회원ID | MBER_NM | 회원명 |
| ENTRPRS_MBER_ID | 기업회원ID | CMPNY_NM | 회사명 |
| PASSWORD | 비밀번호(해시) | IHIDNUM | 주민등록번호 |
| SCRTY_DTRMN_TRGET_ID | 보안결정대상ID | MBER_TY_CODE | 회원유형코드 |
| ZIP | 우편번호 | ADRES / HOUSE_ADRES | 주소/자택주소 |
| MBTLNUM | 휴대전화 | EMAIL_ADRES | 이메일 |
| SEXDSTN_CODE | 성별코드 | SBSCRB_DE | 가입일시 |
| BIZRNO | 사업자등록번호 | JURIRNO | 법인등록번호 |

### 게시판·파일·일정
| 물리명 | 한글명 | 물리명 | 한글명 |
|--------|--------|--------|--------|
| BBS_ID | 게시판ID | BBS_NM | 게시판명 |
| BBS_TY_CODE | 게시판 유형코드 | BBS_ATTRB_CODE | 게시판 속성코드 |
| NTT_ID | 게시물ID | NTT_SJ | 제목 |
| NTT_CN | 내용 | NTT_NO | 게시물 번호 |
| RDCNT | 조회수 | ANSWER_AT | 답변글 여부(Y/N) |
| ATCH_FILE_ID | 첨부파일ID(묶음) | FILE_SN | 파일 일련번호 |
| STRE_FILE_NM | 저장파일명 | ORIGNL_FILE_NM | 원파일명 |
| FILE_EXTSN | 파일확장자 | FILE_SIZE | 파일크기(Byte) |
| SCHDUL_ID | 일정ID | SCHDUL_NM | 일정명 |
| SCHDUL_BEGINDE | 시작일시 | SCHDUL_ENDDE | 종료일시 |

---

## 4. 적용 위치

- **개발(HSQL 시드)** : `src/main/resources/db/shtdb.sql` — 각 `CREATE` 문에 테이블 한글명+구 테이블명, 컬럼별 한글 논리명을 `--` 주석으로 병기. `INSERT` 그룹에는 테이블 한글명·컬럼 순서 주석 추가.
- **운영(배포용)** : `DATABASE/all_sht_ddl_{dbms}.sql`, `DATABASE/all_sht_data_{dbms}.sql` (PostgreSQL·MySQL·Oracle·Altibase·Tibero·CUBRID).
- **매퍼** : `src/main/resources/egovframework/mapper/**` (DB별 분리, 테이블명 `TB_*`/`VW_*` 사용).

## 변경 이력
| 날짜 | 변경 내용 |
|------|-----------|
| 2026-06-19 | 구→신 테이블/컬럼 매핑 문서 신설. shtdb.sql CREATE/INSERT에 한글 논리명 주석 병기. |
