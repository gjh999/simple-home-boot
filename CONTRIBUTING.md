# 기여 가이드 (Contributing)

`simple-home-boot` (전자정부표준프레임워크 5.0 기반 Spring Boot + Thymeleaf 심플 홈페이지)에
관심을 가져 주셔서 감사합니다. 본 문서는 **코드 기여 절차**를 안내합니다.

> 이 저장소를 활용한 **커뮤니티 프로젝트 등재**(Awesome eGovFrame) 절차는
> [Docs/CONTRIBUTING.md](Docs/CONTRIBUTING.md) 와 [Docs/CONTRIBUTE_README.md](Docs/CONTRIBUTE_README.md) 를 참고하세요.

## 시작하기

```bash
# 1) 본인 계정으로 포크 후 클론
git clone https://github.com/<your-account>/simple-home-boot.git
cd simple-home-boot

# 2) 빌드·구동 (내장 HSQLDB로 별도 DB 없이 실행)
mvn clean package
mvn spring-boot:run        # http://localhost:8080
```

- 요구사항: JDK 17, Maven 3.9+
- 테스트 계정: 관리자 `admin`/`1`, 사용자 `user`/`user`

## 기여 절차

1. **이슈 확인/생성** — 버그·기능은 먼저 [Issues](../../issues)에 등록(`.github/ISSUE_TEMPLATE` 템플릿 사용).
2. **브랜치 생성** — `feature/<요약>`, `fix/<요약>`, `docs/<요약>` 규칙.
3. **개발·검증** — `mvn clean package` 통과 확인. 화면 변경은 로컬 구동으로 스모크 확인.
4. **커밋** — 명확한 메시지(제목 + 본문). 한 커밋은 한 주제로.
5. **PR 생성** — `.github/pull_request_template.md` 양식에 맞춰 작성하고 관련 이슈를 연결.

## 코딩 규칙

- 컨트롤러: MVC=`Egov{기능}Controller`, REST=`Egov{기능}ApiController`.
- 서비스: 인터페이스 + `*ServiceImpl` 분리, 생성자 주입(`@RequiredArgsConstructor`).
- MyBatis: `EgovXxx_SQL_{dbtype}.xml` (DBMS별 분리), `resultMap` 사용.
- 테이블: `TB_` 접두 + 대문자 스네이크케이스, 감사 컬럼 4종 필수.
- 정적 리소스는 로컬 보관(CDN 금지). 상세 규칙은 [CLAUDE.md](CLAUDE.md) 참조.

## 🔐 보안 — 커밋 전 필수 점검

공개 저장소이므로 **한 번 푸시된 비밀은 회수가 사실상 불가능**합니다. 커밋 전 반드시 확인하세요.

- **절대 커밋 금지**: 실제 API 키·토큰(`ghp_`, `AKIA…`, `sk-…`, `ntn_…`), 비밀번호·DB 접속정보,
  JWT 서명키·OAuth secret, 인증서/키 파일(`*.pem`, `*.key`, `*.p12`, `*.jks`), `.env`,
  개인정보(주민등록번호, 실명+연락처, 개인 이메일), 로컬 절대경로의 사용자명.
- **운영 시크릿은 환경변수로만**: `EGOV_JWT_SECRET`(32자+), `EGOV_CRYPTO_KEY`(16자+).
  소스/`application.properties`에는 개발용 placeholder만 두며, `prod` 프로파일은
  기본 placeholder 사용 시 `ProductionSecretsGuard`가 **기동을 차단**합니다.
- **점검 방법**: 커밋 직전 `git diff --cached` 로 추가 내용을 확인하고,
  의심 패턴을 검색하세요. 빌드 산출물(`target/`), 로그(`*.log`), 업로드물(`files/`),
  IDE/로컬 설정은 `.gitignore`로 제외되어 있습니다.
- 실수로 비밀을 커밋·푸시했다면 **즉시 해당 키를 폐기·교체(rotate)** 하고 관리자에게 알리세요.

## 행동 강령

모든 기여자는 [Contributor Covenant](https://www.contributor-covenant.org/ko/version/2/1/code_of_conduct/)
행동 강령을 따릅니다. 위반 신고: egovframesupport@gmail.com 또는 GitHub Issue.

## 라이선스

기여하신 코드는 본 저장소의 [Apache License 2.0](LICENSE) 하에 배포됩니다.
