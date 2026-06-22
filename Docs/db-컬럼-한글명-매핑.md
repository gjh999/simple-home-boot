# DB 컬럼 한글명 매핑 & DDL/DML 주석 적용 가이드

> - 목적 : 6종 운영 DBMS DDL과 HSQL 시드의 **컬럼 한글 논리명**을 한곳에 정리하고, 이번에 적용한
>   **DDL 컬럼 한글주석**·**INSERT 컬럼명 명시** 작업 내역을 기록한다.
> - 대상 : `DATABASE/all_sht_{ddl,data}_{altibase,cubrid,mysql,oracle,postgresql,tibero}.sql`,
>   `src/main/resources/db/shtdb.sql`(개발용 HSQL 시드).
> - 관련 : 테이블 매핑 [db-name-mapping.md](db-name-mapping.md) · 스키마 상세 [db-schema-guide.md](db-schema-guide.md).
>   표준 단어/도메인/용어 : `단어규칙.md`·`도메인규칙.md`·`용어규칙.md`.

## 1. 적용 내역(이번 작업)

### 1-1. DDL 컬럼 한글주석
- 모든 `CREATE TABLE`의 **각 컬럼 정의 끝에 인라인 한글주석(`-- 한글명`)** 을 부여.
  - 예: `CL_CODE  CHAR(3)  NOT NULL,  -- 분류코드`
- 적용: 6종 DBMS DDL(각 271개 컬럼) + HSQL `shtdb.sql`(기존부터 보유).
- **인라인 `--` 주석**을 채택한 이유: 6종 DBMS + HSQL에서 **동일 문법으로 안전**하게 동작(실행 영향 없음)하고,
  소스에서 바로 읽혀 기여자 가독성이 높다. (DB 메타데이터가 필요하면 `COMMENT ON COLUMN`으로 확장 가능)

### 1-2. INSERT 컬럼명 명시
- 모든 시드 `INSERT`에 **대상 컬럼명을 명시**(`INSERT INTO T (col, ...) VALUES (...)`).
  - altibase·cubrid·mysql·oracle·tibero : 기존 완비(58/58).
  - **postgresql** : 코드 3테이블(`TB_CMMN_CL_CODE`/`TB_CMMN_CODE`/`TB_CMMN_DETAIL_CODE`) 32건 보완 → 54/54.
  - **HSQL `shtdb.sql`** : IDS·COMTECOPSEQ·코드 3테이블·`TB_BBS_MASTER`·`TB_BBS_USE` 44건 보완 → 59/59.
- 컬럼 순서는 각 DDL 정의 순서를 따르며, **HSQL 재기동 시 초기화 무오류**로 컬럼·값 정합을 검증함.

### 1-3. 비파괴성
- 본 작업은 **주석 추가 / INSERT에 컬럼명 명시**만 수행 → `DROP`·`TRUNCATE`·컬럼 삭제 없음(비파괴적). 백업 불요.

## 2. 컬럼 한글명 사전 (물리명 → 한글 논리명)

> 컬럼 물리명은 공공데이터 공통표준 용어를 준수(접미 규칙: `_AT`=여부, `_NM`=명, `_DC`=설명,
> `_CODE`=코드, `_ID`=아이디, `_PNTTM`=시각, `_DE`=일자, `_CN`=내용, `_SE`=구분). 감사 컬럼 4종은 전 테이블 공통.

| 컬럼(물리명) | 한글 논리명 |
|---|---|
| `ADRES` | 주소 |
| `ANSWER_AT` | 답변글 여부(Y/N) |
| `ANSWER_LC` | 답변 계층(레벨) |
| `APPLCNT_EMAIL_ADRES` | 신청자 이메일 |
| `APPLCNT_IHIDNUM` | 신청자 주민등록번호 |
| `APPLCNT_NM` | 신청자명 |
| `AREA_NO` | 지역번호 |
| `ATCH_FILE_ID` | 첨부파일 묶음ID |
| `ATCH_POSBL_FILE_NUMBER` | 첨부가능 파일 수 |
| `ATCH_POSBL_FILE_SIZE` | 첨부가능 총 용량(단위: 시스템 정의) |
| `AUTHOR_CODE` | 권한코드 |
| `AUTHOR_CREAT_DE` | 생성일자 |
| `AUTHOR_DC` | 권한 설명 |
| `AUTHOR_NM` | 권한명 |
| `BBS_ATTRB_CODE` | 게시판 속성코드 |
| `BBS_ID` | 게시판ID |
| `BBS_INTRCN` | 게시판 소개 |
| `BBS_NM` | 게시판명 |
| `BBS_TY_CODE` | 게시판 유형코드 |
| `BIZRNO` | 사업자등록번호 |
| `BRTHDY` | 생년월일 |
| `CHANGE_DE` | 변경일자 |
| `CLB_ID` | 동호회ID |
| `CLB_NM` | 동호회명 |
| `CL_CODE` | 분류코드 |
| `CL_CODE_DC` | 분류코드 설명 |
| `CL_CODE_NM` | 분류코드명 |
| `CMMNTY_ID` | 커뮤니티ID |
| `CMMNTY_NM` | 커뮤니티명 |
| `CMPNY_NM` | 회사명 |
| `CODE` | 상세코드 |
| `CODE_DC` | 상세코드 설명 |
| `CODE_ID` | 코드ID |
| `CODE_ID_DC` | 코드ID 설명 |
| `CODE_ID_NM` | 코드ID명 |
| `CODE_NM` | 상세코드명 |
| `CREAT_DT` | 생성시각 |
| `CRTFC_DN_VALUE` | 인증DN |
| `CXFC` | 대표자명 |
| `DETAIL_ADRES` | 상세주소 |
| `DIARY_CN` | 다이어리 내용 |
| `DIARY_ID` | 다이어리ID |
| `EMAIL_ADRES` | 이메일 |
| `EMPLYR_ID` | 사용자ID |
| `EMPLYR_STTUS_CODE` | 사용자상태코드 |
| `EMPL_NO` | 사번 |
| `END_TELNO` | 전화 끝번호 |
| `ENTRPRS_END_TELNO` | 전화 끝번호 |
| `ENTRPRS_MBER_ID` | 기업회원ID |
| `ENTRPRS_MBER_PASSWORD` | 비밀번호(해시) |
| `ENTRPRS_MBER_PASSWORD_CNSR` | 비밀번호 정답 |
| `ENTRPRS_MBER_PASSWORD_HINT` | 비밀번호 힌트 |
| `ENTRPRS_MBER_STTUS` | 회원상태코드 |
| `ENTRPRS_MIDDLE_TELNO` | 전화 중간번호 |
| `ENTRPRS_SE_CODE` | 기업구분코드 |
| `ESNTL_ID` | 고유ID |
| `FILE_ATCH_POSBL_AT` | 파일첨부 가능(Y/N) |
| `FILE_CN` | 파일 내용/비고 |
| `FILE_EXTSN` | 파일확장자 |
| `FILE_SIZE` | 파일크기(Byte) |
| `FILE_SN` | 파일 일련번호 |
| `FILE_STRE_COURS` | 파일 저장경로 |
| `FRST_REGISTER_ID` | 최초등록자ID |
| `FRST_REGIST_PNTTM` | 최초등록시각 |
| `FXNUM` | 팩스 |
| `GROUP_CREAT_DE` | 생성일자 |
| `GROUP_DC` | 그룹 설명 |
| `GROUP_ID` | 그룹ID |
| `GROUP_NM` | 그룹명 |
| `HOUSE_ADRES` | 자택주소 |
| `HOUSE_END_TELNO` | 자택전화 끝번호 |
| `HOUSE_MIDDLE_TELNO` | 자택전화 중간번호 |
| `IHIDNUM` | 주민등록번호 |
| `INDUTY_CODE` | 업종코드 |
| `INFO_PROVD_AGRE_CN` | 정보제공 동의 내용 |
| `JURIRNO` | 법인등록번호 |
| `LAST_UPDT_PNTTM` | 최종수정시각 |
| `LAST_UPDUSR_ID` | 최종수정자ID |
| `MBER_EMAIL_ADRES` | 이메일주소 |
| `MBER_FXNUM` | 팩스번호 |
| `MBER_ID` | 회원ID |
| `MBER_NM` | 회원명 |
| `MBER_STTUS` | 회원상태코드 |
| `MBER_TY_CODE` | 회원유형코드 |
| `MBTLNUM` | 휴대전화 |
| `MIDDLE_TELNO` | 전화 중간번호 |
| `MNGR_AT` | 관리자 여부(Y/N) |
| `NEXT_ID` | 다음 ID 값 |
| `NTCE_BGNDE` | 공지 시작일시 |
| `NTCE_ENDDE` | 공지 종료일시 |
| `NTCR_ID` | 게시자ID |
| `NTCR_NM` | 게시자명 |
| `NTT_CN` | 내용 |
| `NTT_ID` | 게시물ID |
| `NTT_NO` | 게시물 번호(정렬/표시용) |
| `NTT_SJ` | 제목 |
| `OFCPS_NM` | 직책명 |
| `OFFM_TELNO` | 사무실전화 |
| `OPRTR_AT` | 운영자 여부(Y/N) |
| `ORGNZT_DC` | 조직 설명 |
| `ORGNZT_ID` | 조직ID |
| `ORGNZT_NM` | 조직명 |
| `ORIGNL_FILE_NM` | 원파일명 |
| `PARNTSCTT_NO` | 부모글 번호 |
| `PASSWORD` | 비밀번호(해시) |
| `PASSWORD_CNSR` | 비밀번호 정답 |
| `PASSWORD_HINT` | 비밀번호 힌트 |
| `PSTINST_CODE` | 소속기관코드 |
| `RDCNT` | 조회수 |
| `REGIST_SE_CODE` | 등록구분코드 |
| `REPLY_POSBL_AT` | 답글 가능(Y/N) |
| `REPTIT_SE_CODE` | 반복구분코드 |
| `SBSCRB_DE` | 가입일시 |
| `SCHDUL_BEGINDE` | 시작일시 |
| `SCHDUL_CHARGER_ID` | 담당자ID |
| `SCHDUL_CN` | 일정 내용 |
| `SCHDUL_DEPT_ID` | 부서ID |
| `SCHDUL_ENDDE` | 종료일시 |
| `SCHDUL_ID` | 일정ID |
| `SCHDUL_IPCR_CODE` | 중요도코드 |
| `SCHDUL_KND_CODE` | 일정종류코드 |
| `SCHDUL_NM` | 일정명 |
| `SCHDUL_PLACE` | 장소 |
| `SCHDUL_SE` | 일정구분코드 |
| `SCRTY_DTRMN_TRGET_ID` | 보안결정대상ID(보통 사용자ID) |
| `SEXDSTN_CODE` | 성별코드 |
| `SORT_ORDR` | 정렬순서 |
| `STRE_FILE_NM` | 저장파일명 |
| `STSFDG_AT` | 만족도 조사 사용(Y/N) |
| `TABLE_NAME` | 대상 테이블명 |
| `TMPLAT_ID` | 템플릿ID |
| `TRGET_ID` | 사용대상ID |
| `USER_NM` | 사용자명 |
| `USE_AT` | 사용여부(Y/N) |
| `USE_STPLAT_CN` | 이용약관 내용 |
| `USE_STPLAT_ID` | 이용약관ID |
| `USE_STPLAT_NM` | 이용약관명 |
| `ZIP` | 우편번호 |
