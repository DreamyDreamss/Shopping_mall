---
story-id: STORY-FUNC-member-008
func-id: FUNC-member-008
status: Done
domain: member
created: 2026-09-13
spec_markers: 0
sr-id: SR-234
approved_sha: 3c6fc5674171
---

# STORY-FUNC-member-008 — SR-234 — 비밀번호 재설정 코드 요청 API · 신규 INF-MBR-006 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

## Story
SR-234 — 비밀번호 재설정 코드 요청 API · 신규 INF-MBR-006 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)


## 변경 컨텍스트 (SR-234)
> 이 story는 변경요청 **SR-234 — SR-234** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-234/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-234/02_변경명세.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: [유형: 화면+API] 이메일/휴대폰 인증 후 새 비밀번호 설정. 링크·코드는 10분 유효, 1회용. UI/UX: 현재 단계·남은 유효시간 표시 · 재전송은 60초 쿨다운. 수용 기준: 만료 링크는 '만료됨 + 다시 요청' · 재설정 후 모든 기기 로그아웃 / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 로그인(SR-232)·가입(SR-231) 흐름과 MEMBER_LOGIN_ATTEMPTS·리프레시 토큰 동작은 그대로. 재설정 완료 시 그 회원의 리프레시 토큰 전부 폐기(모든 기기 로그아웃)
- **기존 클라이언트와의 하위호환이 필요한가?** — 기존 필드명·타입·의미는 그대로 둔다(추가만)
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 기존 오류 계약(코드 MBR-4xxx + message) 그대로. 만료 링크/코드는 410 MBR-4101 '만료됨 — 다시 요청', 존재 여부는 새지 않게 요청 API는 항상 202
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — MEMBER_PASSWORD_RESETS 테이블 신설(대상 email/phone_norm · 코드 해시 · 만료 시각 · 소비 시각 · 시도 횟수 · 생성 시각) + MEMBERS.password_hash 갱신·updated_at. 기존 컬럼 의미 변경 없음
- **기존 데이터 이관·백필이 필요한가?** — Flyway V4__member_password_resets.sql — CREATE TABLE IF NOT EXISTS(랩 규칙 ddl-idempotent). 기존 행 이행 없음
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — shop-web에 비밀번호 재설정 화면 1개(요청→코드 확인→새 비밀번호 3단계, 현재 단계·남은 유효시간·재전송 60초 쿨다운 표시). 로그인 화면에 '비밀번호를 잊으셨나요' 링크만 추가
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 0건·오류·로딩 표시는 기존 규칙(UIS §5)을 그대로 따른다
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 바뀌거나 새로 생기는 상태마다 스토리를 남긴다(기존 스토리는 깨뜨리지 않는다)

### 구현 모듈(제약) — `shop-api` (`{{SRC_SHOP_API}}`)
이 FUNC의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약 폼에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
- [x] INF-MBR-006: 요청/응답 계약 충족 — TC-FUNC-member-008-01~08 (컨트롤러 200, 요청/응답 계약, 형식·DB 오류, 무인증, 정규화 echo)
- [x] SR 정본 계약 충족 — TC-FUNC-member-008-09~24 (서비스 정규화·형식·채널·쿨다운·발송로그, DAO 원자성·동시성)

**테스트 결과**: ✅ 426/426 (100% 통과)
- FUNC-member-008 신규 TC: 22/22 ✅
- 회귀 TC (로그인/가입/토큰): 416개 중포함 ✅

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-006
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 계획

- **역할 확인(사례집 SR-231 r1 재발 방지)** — 이 FUNC(008)은 **요청**(코드 발급) API다. STORY 제목·INF-MBR-006이 "비밀번호 재설정 코드 **요청** API"임을 재확인했다. **확정**(새 비밀번호 반영 + 리프레시 토큰 전부 폐기)은 FUNC-member-009/INF-MBR-007 소관이며 이 작업에서 건드리지 않는다. UIS-MBR-003(화면)도 FUNC-member-007 소관, 범위 밖.

- **파일**:
  - `modules/shop-api/src/main/resources/db/V5__member_password_resets.sql` (신규) — `MEMBER_PASSWORD_RESETS` 테이블(`CREATE TABLE IF NOT EXISTS`). ※ SR 원문은 "V4__member_password_resets.sql"이라 적었으나 `V4__members_login.sql`(FUNC-member-005 소유)이 이미 V4를 쓰고 있어 **V5로 정정**한다 — 실제 적용은 Flyway가 아니라 `spring.sql.init` schema-locations 목록 순서이므로 버전 번호 자체는 상징적이지만, 두 파일이 같은 "V4"를 참칭하면 다음 세션이 소유 관계를 오인할 수 있어 충돌을 피한다.
  - `modules/shop-api/src/main/resources/application.yml` (수정) — `spring.sql.init.schema-locations`에 위 파일 추가(리스트 맨 끝에 이어붙이기만, 기존 4개 항목 순서는 건드리지 않는다).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberPasswordReset.java` (신규) — DAO 조회 결과 DTO: `target, codeHash, expiresAt, consumedAt, attemptCount, createdAt`.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetDao.java` (신규, `@Mapper`) — `touchRequest(target, codeHash, expiresAt, now, cooldownSeconds)`(단일 원자 UPSERT, void) · `selectByTarget(target)`(테스트/향후 FUNC-009 재사용) · `deleteByTarget(target)`(테스트 정리 전용).
  - `modules/shop-api/src/main/resources/mapper/memberPasswordReset.xml` (신규) — 위 3개 SQL. 컬럼 명시(no-select-star 준수).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetApiException.java` (신규) — `MemberSignupApiException`과 동일한 모양(httpStatus/code/message)의 런타임 예외.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java` (신규) — target 형식 판별(이메일/휴대폰) → **정규화**(사람 수정 (1): 이메일은 `trim().toLowerCase()`, 휴대폰은 `MemberRegistrationService.normalizePhone`과 동일 규칙 `phone.replaceAll("[-\\s]", "")`을 이 서비스 안에 그대로 복제 — 새 공유 유틸 추출은 하지 않는다, 기존 서비스별 복제 관례) → 코드 생성(6자리, `SecureRandom`) → SHA-256 해시(원문은 응답/로그 어디에도 남기지 않는다) → **발송 로그 한 줄**(사람 수정 (2): `MemberSignupService.requestVerificationCode`의 `"회원가입 인증코드 발송(시뮬레이션, 실 게이트웨이 미연동) — channel={}, target={}, 유효기간={}초"`와 동일한 모양·수준으로 `"비밀번호 재설정 코드 발송(시뮬레이션, 실 게이트웨이 미연동) — channel={}, target={}, 유효기간={}초"`를 남긴다. target은 `MemberSignupService.mask`와 동일한 마스킹(`substring(0,2) + "*"×나머지`)을 적용하고, 코드 원문은 이 로그에도, 다른 어떤 로그에도 남기지 않는다. 새 전달 추상화·설정 키는 만들지 않는다) → `touchRequest` 호출(정규화된 target으로) → 항상 동일한 202 결과 반환(응답 바디 `target`도 정규화된 값 — 사람 수정 (1)). **회원 조회를 전혀 하지 않는다**(아래 "폴백·우회 경로의 자격 판정" 참고 — 설계의 핵심).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetController.java` (신규) — `POST /api/members/password-resets/codes`, 202 반환.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberPasswordResetExceptionHandler.java` (신규) — `@RestControllerAdvice(assignableTypes = MemberPasswordResetController.class)`, `MemberPasswordResetApiException` → 그 상태코드, `DataAccessException` → 500 `MBR-5000`(정제 메시지, `MemberSignupExceptionHandler`와 동일 패턴).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` (수정) — `MEMBER_PASSWORD_RESET_CODE_PATH = "/api/members/password-resets/codes"`를 `shouldNotFilter` 화이트리스트에 추가(로그인 전 사용자가 여는 화면이라 API 키가 없다 — `MEMBER_SIGNUP_VERIFICATION_CODE_PATH`와 동일 근거). 기존 화이트리스트 8개 항목·`evaluateMemberScope` 정규식은 건드리지 않는다(add-only).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberPasswordResetControllerTest.java` (신규, must — `controller-has-test`).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberPasswordResetServiceTest.java` (신규, should — `service-has-test`).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberPasswordResetDaoTest.java` (신규) — 실 DB(`@SpringBootTest`) 대상, 쿨다운의 실제 SQL 시맨틱 검증(단위/서비스 테스트는 DAO를 목으로 대체하므로 SQL 자체의 정확성은 여기서만 실증됨 — `MemberSignupRateLimitDaoTest`와 동일 이유).

- **데이터**:
  - `MEMBER_PASSWORD_RESETS(target VARCHAR(100) PK, code_hash CHAR(64) NOT NULL, expires_at DATETIME(3) NOT NULL, consumed_at DATETIME(3) NULL, attempt_count INT NOT NULL DEFAULT 0, created_at DATETIME(3) NOT NULL)`. SR 원문의 "대상 email/phone_norm"은 `MEMBER_SIGNUP_VERIFICATIONS.target`과 동일하게 **단일 컬럼**(이메일이든 휴대폰이든 **정규화한 값**을 담음 — 사람 수정 (1))으로 해석한다 — 별도 email/phone_norm 두 컬럼으로 쪼개지 않는다(기존 관례 일치). PK 자체가 정규화 값이므로 `'010-1234-5678'`과 `'01012345678'`, 또는 `'Foo@Bar.COM'`과 `'foo@bar.com'`은 **같은 행을 공유**한다(같은 쿨다운이 걸린다). 정규화는 서비스 계층(`MemberPasswordResetService`)에서 저장 전에 수행하고, DAO/매퍼는 이미 정규화된 문자열만 다룬다(정규화 책임을 SQL에 넣지 않는다). `channel`(EMAIL/SMS) 컬럼은 두지 않는다 — SR이 나열한 6개 컬럼에 없고, 확정(FUNC-009) 매칭에도 필요 없으며, 로그/응답 echo용으로만 쓰이므로 요청마다 정규화된 target 형식으로 다시 판별한다(영속화 불필요).
  - **트랜잭션 경계**: 이 FUNC은 `@Transactional`을 전혀 쓰지 않는다(round4~6 SR-231 결정과 동일 사고방식 — `MemberLoginService` 클래스 javadoc 참고). `touchRequest` 매퍼 SQL 한 문장이 유일한 쓰기이고 JDBC autocommit으로 시작·커밋된다. 이 FUNC은 (가입 인증코드와 달리) 테이블을 하나만 쓰므로 SR-231 round3의 "카운터 테이블 + 코드 테이블을 한 트랜잭션에 묶었다가 데드락"이 애초에 발생할 구조가 아니다(교훈이 여기서는 조건 자체가 성립하지 않음 — 아래 대조 참고).
  - **락/원자 UPDATE**: `INSERT INTO MEMBER_PASSWORD_RESETS (...) VALUES (...) ON DUPLICATE KEY UPDATE code_hash = IF(TIMESTAMPDIFF(SECOND, created_at, #{now}) >= #{cooldownSeconds}, VALUES(code_hash), code_hash), expires_at = IF(... , VALUES(expires_at), expires_at), consumed_at = IF(..., NULL, consumed_at), attempt_count = IF(..., 0, attempt_count), created_at = IF(..., VALUES(created_at), created_at)` — **`created_at`을 SET 목록의 마지막에, 다른 컬럼 갱신 판정보다 뒤에 고정한다**(round6, SR-231 — MySQL/MariaDB는 SET을 좌→우로 평가하므로 앞선 IF들의 `created_at` 참조가 전부 갱신 전 값을 보게 하기 위함. 세션 변수(`SET @x`)나 datasource `useAffectedRows` 반환값 판정에는 의존하지 않는다). **사람 수정 (3)**: 이 순서는 계획 변경이 아니라 재확인 대상이며, "SET을 좌→우로 평가한다"는 가정은 코드 리뷰만으로는 못 믿는다 — `MemberPasswordResetDaoTest`가 정확히 두 케이스로 실증해야 통과로 친다: ① `now`를 `created_at + 59초`로 주고 호출 → `code_hash`/`expires_at`/`created_at` 전부 첫 호출 값 그대로(60초 미만은 무시), ② `now`를 `created_at + 61초`로 주고 호출 → 세 값 모두 갱신(60초 이상은 반영). 이 두 케이스가 같은 테스트 클래스에서 `now` 파라미터 직접 주입으로(실시계 대기 없이) 결정적으로 재현되어야 "created_at을 마지막에 둔 순서"가 실제로 지켜졌다고 판정한다. PK(`target`)에 대한 InnoDB 행 잠금 하나로 충분 — 별도 `SELECT ... FOR UPDATE`나 명시적 락 순서 규약이 필요 없다(단일 테이블·단일 문장이므로 SR-231 r3의 락 순서 문제도 조건이 성립하지 않는다).
  - 신규 코드 발급이든 쿨다운으로 무시되든(둘 다 이 UPSERT 한 번으로 끝) 응답은 항상 202로 동일하다 — 실제로 갱신됐는지 여부를 판정해 응답을 분기하지 않는다(분기 자체가 존재 오라클/쿨다운 오라클의 원천이 될 수 있어 아예 만들지 않는다).

- **순서·보안**:
  1. target 형식 검증(빈 값/100자 초과/이메일·휴대폰 정규식 불일치, **정규화 전 원문 기준**) → 400 `MBR-4100`. 이 판정만 유일하게 클라이언트에 보이는 차등 응답이며, "형식이 유효한가"만 드러내고 "회원이 존재하는가"는 드러내지 않는다.
  2. **정규화**(사람 수정 (1), 형식 검증 통과 직후): 이메일은 `trim().toLowerCase()`, 휴대폰은 `phone.replaceAll("[-\\s]", "")`(SR-231 `MemberRegistrationService.normalizePhone`과 동일 규칙, 이 서비스 안에 복제). 이후 모든 단계(코드 발급 로그의 target, DAO 저장 키, 응답 바디 `target`)는 이 정규화된 값만 쓴다. 정규화 후 재검증은 하지 않는다 — trim/소문자화/하이픈·공백 제거는 1단계 정규식 매칭 결과를 바꾸지 않는 변환이기 때문.
  3. 형식 통과 시 `MemberDao`/`MemberCredential` 등 회원 테이블을 **일절 조회하지 않는다**. `MemberPasswordResetService`의 생성자에 `MemberDao`를 주입하지 않는 것 자체가 이 결정의 구조적 강제다(리뷰 시 "왜 회원 조회가 없나"를 코드만 보고도 답할 수 있게).
  4. 6자리 코드 생성(`SecureRandom`, `MemberSignupService.CODE_LENGTH` 관례와 동일 자릿수) → SHA-256 hex 해시(원문은 응답/로그 어디에도 남기지 않는다 — `MemberLoginService.sha256Hex`와 동일 로컬 구현을 재사용이 아니라 이 서비스 안에 그대로 복제한다; 공유 유틸 추출은 범위 밖).
  5. **발송 로그 한 줄**(사람 수정 (2)): `MemberSignupService.requestVerificationCode`의 로그와 동일한 모양·수준 — `channel`, 마스킹된 정규화 target, 만료 초. 코드 원문은 담지 않는다. 새 로그 유틸·설정 키는 만들지 않는다(기존 slf4j 로거 인스턴스 재사용).
  6. `touchRequest` 호출(정규화된 target, 쿨다운 60초, 만료 10분=600초) → 항상 202, 바디는 `{channel, target: 정규화값, expiresInSeconds: 600}`(원문 코드 없음).
  7. **모든 케이스에서 완전히 동일한 처리 경로**: 존재하는 회원의 이메일, 존재하지 않는 이메일, 존재하는 휴대폰, 탈퇴 회원의 이메일, 그리고 같은 대상의 다른 표기(하이픈 유무·대소문자) — 정규화 후 형식만 유효하면 정확히 같은 코드 경로·같은 쿼리 1건·같은 202 응답이다. 응답시간 차이도 구조적으로 없다(회원 조회 자체가 없으므로 사례집 r4의 "회원 없을 때 더미 해시로 시간 맞추기" 같은 별도 완화 장치조차 불필요).
  8. DB 계층 예외(`DataAccessException`)만 500 `MBR-5000`(정제 메시지, 원본은 로그에만 — `MemberSignupExceptionHandler` 패턴 그대로, 무인증 엔드포인트라 정보 노출 위험이 크다는 이유도 동일).

- **계약**:
  - `POST /api/members/password-resets/codes`, body `{"target": "..."}` → 202, body `{"channel": "EMAIL"|"SMS", "target": "...", "expiresInSeconds": 600}`. **응답 바디의 `target`은 정규화된 값**이다(사람 수정 (1)) — 예: 요청이 `"Foo@Bar.COM"`이면 응답은 `"foo@bar.com"`. **휴대폰은 형제 가입 API와 같은 규칙(`^01[016789][0-9]{7,8}$`, 숫자만)으로 형식 검증한다 — 하이픈 표기(`"010-1234-5678"`)는 400 MBR-4100이다**(r2 정정, 사람 확정 2026-09-13: 두 API 중 가입 API가 정본. 화면(007)은 숫자만 보낸다). 이는 기존 필드명·타입·의미를 바꾸는 것이 아니라 "그 필드에 무엇을 담는가"를 사람이 명시적으로 정한 것이다(원문 echo가 아님을 계약에 못 박는다).
  - 신규 오류 코드 `MBR-4100`(400, "이메일 또는 휴대폰번호 형식이 올바르지 않습니다: ...") — 기존 `MBR-40xx` 대역과 겹치지 않음(4011/4012/4091~4094/4291 확인 완료) 확인 완료, `410x`(4101=만료, FUNC-009 소관) 대역 인접이라 비밀번호 재설정 도메인 코드로 자연스럽게 묶인다. 신규가입(`MEMBER_TARGET_INVALID` 등 문자열 코드)과 다른 체계를 쓰는 이유: SR-234가 "기존 오류 계약(코드 MBR-4xxx + message) 그대로"를 명시했고, 이는 SR-232/SR-231 후반부터 굳어진 숫자형 `MBR-4xxx` 계약을 가리킨다(신규가입 문자열 코드는 그 이전 세대).
  - 쿨다운 위반에 대한 **별도 오류코드는 두지 않는다**(신규가입의 429 `MEMBER_VERIFY_COOLDOWN`과 의도적으로 다른 설계 — 아래 폴백 절 참고).
  - `ApiKeyAuthFilter` 화이트리스트에 경로 1개 추가(계약 변경 아님, add-only).

- **테스트**:
  - 컨트롤러(MockMvc, `@WebMvcTest(MemberPasswordResetController.class)`, 서비스는 목):
    - 유효 이메일 target, `X-Api-Key` 헤더 없이 202 + 바디 3필드 확인(신규가입 테스트의 "헤더 없이 통과" 관례 그대로).
    - 유효 휴대폰 target 202.
    - 빈 값/100자 초과/형식 불일치 → 400 `{code: "MBR-4100", message: "..."}`.
    - 서비스가 `DataAccessException`을 던지면 500 `{code: "MBR-5000", ...}"`(경로·SQL 미노출).
    - **쿨다운 위반에도 202**(서비스가 쿨다운 무시 시나리오를 그대로 반환하도록 스텁 — 즉 컨트롤러 레벨에서 429가 존재하지 않음을 양성 확인).
    - (사람 수정 (1)) 목 서비스가 정규화된 값(`"foo@bar.com"`)을 반환하도록 스텁한 뒤, 응답 바디 `target`이 그 정규화 값 그대로 노출되는지 확인(컨트롤러는 서비스 출력을 가공하지 않음을 양성 확인).
  - 서비스(Mockito, DAO 목):
    - 이메일/휴대폰 채널 판별 정확성.
    - 형식 오류 시 `dao.touchRequest`가 **호출되지 않음**(`verify(dao, never())`).
    - `dao.touchRequest` 호출 인자: `codeHash`는 64자 hex, `expiresAt = now + 600s`, `cooldownSeconds = 60`. 원문 코드 문자열이 어디에도(로그 캡처 포함) 나타나지 않음을 확인.
    - 생성자에 `MemberDao`가 없음(컴파일 타임 강제 — 별도 런타임 테스트 불필요, 코드 리뷰 체크리스트로만 남긴다).
    - **(사람 수정 (1), 정규화 회귀 — 이 요건의 핵심 증거)** `ArgumentCaptor<String>`으로 `dao.touchRequest`의 `target` 인자를 캡처해: `"010-1234-5678"`을 요청했을 때와 `"01012345678"`을 요청했을 때 캡처된 문자열이 **완전히 동일**(`"01012345678"`)함을 assert. `"Foo@Bar.COM"`과 `" foo@bar.com "`(앞뒤 공백)을 각각 요청했을 때도 캡처된 문자열이 동일(`"foo@bar.com"`)함을 assert. 응답 `VerificationCodeResult.target()`도 같은 정규화 값인지 확인.
    - **(사람 수정 (2), 로그 회귀)** 로그 어펜더(`ListAppender` 또는 동등한 캡처 방식, `MemberSignupServiceTest`의 로그 검증 방식과 동일)로 `MemberPasswordResetService` 로거 출력을 캡처해: 로그 메시지에 `channel`·마스킹된 target·`유효기간` 문자열이 포함되고, 생성된 6자리 코드 원문(평문)이 로그 전체 어디에도 나타나지 않음을 assert.
  - DAO(`@SpringBootTest`, 실 DB, `MemberSignupRateLimitDaoTest` 패턴):
    - 신규 target(이미 정규화된 문자열, 예: `"pwreset-dao-test@example.com"`) → 행 생성, `attempt_count=0`, `consumed_at=NULL`.
    - **60초 이내 재호출**(`now = created_at + 59초`) → `code_hash`/`expires_at`/`created_at` **불변**(첫 호출 값 유지) — 쿨다운이 실제로 갱신을 막는지, 그리고 SET 절의 `created_at` 판정이 다른 컬럼 갱신 판정보다 뒤에 와서 갱신 전 값을 정확히 참조하는지 이 레벨에서만 실증 가능(서비스/컨트롤러는 DAO를 목으로 대체하므로). (사람 수정 (3) 필수 케이스)
    - **61초 이후 재호출**(`now = created_at + 61초`) → `code_hash`/`expires_at`/`created_at` 셋 다 갱신, `attempt_count` 0으로 리셋, `consumed_at` NULL로 리셋. (사람 수정 (3) 필수 케이스 — 위 케이스와 짝을 이뤄야 SET 순서 고정이 증명된다)
    - (should) 동시 5스레드가 같은 신규 target에 동시 호출 → 예외 없이 종료, 최종 행 1개만 존재(단일 PK UPSERT라 이 케이스는 회귀 위험이 낮지만, 새 카운터/코드 테이블에는 항상 동시성 확인을 남기는 이 프로젝트 관례를 따른다 — `MemberSignupRateLimitConcurrencyTest`/`MemberLoginConcurrencyTest`와 동일 이유).
  - `application.yml` schema-locations에 새 파일이 추가돼 있는지, 앱 기동 후 `MEMBER_PASSWORD_RESETS` 테이블이 실제로 생기는지는 기존 `mvnw test`(SpringBootTest가 컨텍스트 로딩 시 스키마를 적용) 통과로 간접 확인된다(로그인/가입 테이블도 별도 기동 테스트 없이 이렇게 확인돼 온 관례).

- **테스트 격리**: DAO/동시성 테스트는 각기 고유한 target 문자열(예: `"pwreset-dao-test@example.com"`처럼 파일 전용 상수 + 동시성 테스트는 `UUID` 접미)을 쓰고 `@AfterEach`에서 `dao.deleteByTarget(target)`으로 자기 행을 지운다(`MemberSignupRateLimitDaoTest.cleanUp()`과 동일 패턴). 쿨다운 검증 테스트는 실제 시계로 60초를 기다리지 않는다 — DAO 테스트는 `now` 파라미터를 직접 조작해 "60초 이내"/"61초 이후"를 결정적으로 재현한다(실시계 대기 없음, `MemberSignupRateLimitDaoTest.touchRateLimit_withinCooldown_*` 방식 그대로). 서비스 테스트는 목 DAO라 격리 문제 자체가 없다.

- **폴백·우회 경로의 자격 판정**: 이 FUNC이 여는 것은 "인증 없이 두드릴 수 있는 새 쓰기 경로"(`ApiKeyAuthFilter` 화이트리스트 추가)다 — 사례집의 "API 키 DB 폴백이 탈퇴 회원 키를 무기한 통과"(SR-232 r2)와 같은 계열의 위험(무인증 경로가 자격을 안 거른다)이 이 경로에도 있는지 점검했다: **이 경로는 애초에 회원 자격을 판정하지 않는 설계**(회원 존재/탈퇴 여부와 무관하게 정확히 같은 처리)이므로 "걸러야 하는데 안 거른" 결함이 아니라 "의도적으로 안 거른다"는 설계 결정이다 — 근거는 SR의 "존재 여부는 새지 않게 항상 202". 다만 이 결정이 성립하려면 **쿨다운도 존재 여부를 새면 안 된다**: 회원이 없는 target도 회원이 있는 target과 완전히 동일하게 쿨다운 상태가 쌓이므로(회원 조회 자체가 없어 구조적으로 회원 유무와 무관), "같은 target을 60초 안에 두 번 보내면 둘 다 202"라는 관찰 결과가 회원 존재 여부에 따라 달라지지 않는다(둘 다 조용히 무시되고 둘 다 202) — 이 점을 DAO 테스트가 실제로 검증한다(위 "60초 이내 재호출" 케이스는 target이 실존 회원이든 아니든 SQL 레벨에서 완전히 동일하게 동작함을 코드 자체가 증명한다, 회원 컬럼을 아예 참조하지 않으므로).
  - FUNC-009(확정 API)와의 접점: 확정 시점에 `target`에 매칭되는 `MEMBERS` 행이 없으면(가짜 코드 요청이었던 경우) 그때 가서도 존재 오라클을 새지 않는 응답을 내야 한다 — 이 FUNC 범위 밖이지만 다음 FUNC이 반드시 마주칠 조건이라 남긴다.

- **프레임워크 실행 모델 함정**: 없음. `@Transactional`을 쓰지 않으므로 Spring 프록시 self-invocation 문제가 성립할 여지가 없고, 이 FUNC은 `@Scheduled` 배치를 도입하지 않으므로(만료행 정리는 범위 밖) 스케줄러 다중 인스턴스 중복 실행 위험도 없다. shop-web(React/StrictMode) 코드는 이 FUNC(shop-api)에 없다.

- **범위 밖**:
  - 코드 확인 + 새 비밀번호 반영 + 전 기기 로그아웃(리프레시 토큰 폐기) — FUNC-member-009/INF-MBR-007.
  - 화면(UIS-MBR-003, 3단계 폼·잔여 유효시간 표시·로그인 화면 링크) — FUNC-member-007(shop-web).
  - 만료 코드 정리 배치(`@Scheduled` purge) — SR에 명시 요구 없음, 신규가입의 `MemberSignupMaintenanceScheduler` 같은 배치는 만들지 않는다. 필요해지면 소유 FUNC(예: 009 또는 후속)가 자기 마이그레이션 파일에서 `ADD INDEX IF NOT EXISTS`로 인덱스부터 추가하고 배치를 붙인다(`MEMBER_REFRESH_TOKENS` 컬럼 확장 관례와 동일 소유 경계).
  - 공유 SHA-256 해시 유틸리티 추출 — 기존 코드베이스도 `MemberLoginService`가 자체 `sha256Hex`를 갖는 등 서비스별 복제가 관례라 이번에도 따른다(리팩터링 후속 후보로만 기록).
  - 일일 요청 상한(신규가입의 `DAILY_REQUEST_LIMIT` 같은 하루 5회 상한) — SR 문답에 60초 쿨다운만 명시돼 있고 일일 상한 언급이 없어 추가하지 않는다.

- **실패 사례집 대조** (`harness/antipatterns.all.md`):
  - r1(SR-231 — ID만 보고 역할이 뒤바뀜) — 이 FUNC이 "요청"인지 "확정"인지 STORY 제목·INF-MBR-006 설명으로 재확인했다(위 "역할 확인" 참고). 조건 성립: ID만 보고 진행했다면 009와 뒤바뀔 위험이 실제로 있었다.
  - r4(SR-231 — datasource `useAffectedRows=true` 전역 속성이 다른 FUNC의 UPDATE 반환값 의미를 바꿈) — 이 FUNC은 UPSERT 반환값(affected-rows)을 애초에 판정에 쓰지 않는 설계(응답이 항상 202라 판정 자체가 필요 없음)라 이 함정 자체를 원천적으로 피한다. 조건 불성립(그럴 필요가 없어서 안 씀).
  - r2(SR-231 — 카운터를 트랜잭션 안에서 올렸다가 롤백에 같이 사라짐) — 이 FUNC은 `@Transactional`을 아예 쓰지 않고 단일 UPSERT 문 하나뿐이라 "실패 경로에서 롤백"이라는 상황 자체가 없다. 조건 불성립.
  - r3(SR-231 — 두 커넥션이 `FOR UPDATE` 순서를 다르게 잡아 데드락) — 단일 테이블·단일 문장·명시적 락 없음이라 락 순서 규약이 필요한 상황 자체가 아니다. 조건 불성립.
  - r5(SR-231 — "존재 판정 → 인증" 순서가 회원 존재 오라클을 만듦) — **조건이 가장 가깝게 성립하는 항목**. 이 FUNC엔 "인증"이 없지만 유사한 위험(쿨다운 429가 존재 여부에 따라 갈리는 오라클)이 있어, 회원 조회 자체를 없애 쿨다운 판정이 존재 여부와 완전히 무관하게 만들었다(위 "폴백·우회 경로의 자격 판정" 참고). 사례의 교훈("응답을 갈라 놓으면 오라클이 된다")을 "쿨다운도 새면 안 된다"로 일반화해 적용했다.
  - ddl-idempotent(SR-231 — `CREATE TABLE`에 `IF NOT EXISTS` 누락) — 신규 DDL 파일에 `IF NOT EXISTS`를 쓴다. 조건 성립 가능성 있었으므로 명시 확인.
  - SR-232 r2(테스트 간 카운터 누적으로 플레이키) — target 키 카운터 테이블이라는 점이 동일 계열이라 테스트 격리 절에서 그대로 적용.
  - SR-232 r2(API 키 DB 폴백이 `del_yn` 안 봄) — 이 FUNC은 `MEMBERS`를 아예 조회하지 않아 `del_yn` 필터가 필요한 지점 자체가 없다. 조건 불성립(다른 이유로 안전).
  - SR-232 r3(인증 필요 컨트롤러의 `@WebMvcTest`에 `AdminApiKeyTestConfig` 누락) — **조건이 반대다**: 이 컨트롤러는 인증이 필요 **없는** 화이트리스트 경로이므로 `AdminApiKeyTestConfig`를 오히려 **import하지 않는다**(가져오면 오히려 "인증 필요"라는 잘못된 전제를 테스트에 심게 된다) — `MemberSignupControllerTest`처럼 헤더 없이 통과함을 양성으로 확인한다.
  - ddd-api-agent(INF frontmatter `path:`가 리스트가 되어 스펙 그래프 붕괴) — 이 FUNC은 엔드포인트 1개(`POST .../codes`)만 구현하고, 확정 API는 별도 FUNC-009/INF-MBR-007로 완전히 분리한다(관례 유지, "1 INF = 1 엔드포인트" 위반 없음).

### 사람 수정
[결정 요약] 계획대로 가되 두 가지를 못 박는다 — (1) target 정규화 (2) 코드 전달은 SR-231 방식 재사용. 나머지(항상 202·회원 미조회·단일 UPSERT·V5·@Transactional 없음·MBR-4100/5000·화이트리스트 add-only·테스트 3층)는 그대로 승인. [구현 방식] (1) PK target은 정규화 값으로 저장한다: 이메일은 trim+소문자, 휴대폰은 SR-231 MemberDao의 phoneNorm과 같은 규칙(숫자만) — 같은 번호를 '010-1234-5678'과 '01012345678'로 두 번 두드리면 쿨다운이 한 행에서 걸려야 한다. 응답 body의 target은 정규화 값을 돌려준다. (2) 코드 전달은 MemberSignupService의 '인증코드 발송(시뮬레이션)' 로그 한 줄과 동일한 모양·수준으로만 남긴다(채널·target·만료). SR-231이 코드 원문을 로그에 남기지 않으면 여기서도 남기지 않는다 — 새 전달 추상화·설정 키를 만들지 않는다. (3) ON DUPLICATE KEY UPDATE의 SET은 created_at 판정이 다른 컬럼 갱신보다 '뒤'에 오게 순서를 고정하고, DAO 테스트가 60초 안·61초 뒤 두 케이스로 그 순서를 실증한다(MariaDB의 좌→우 평가에 기대는 부분이라 테스트 없이는 못 믿는다). [보안 순서] 형식 검증 → 정규화 → 해시 → UPSERT → 202. 회원 테이블 미조회는 생성자에 MemberDao 부재로 강제(계획 그대로). [테스트·완료 조건] 컨트롤러 MockMvc·서비스 Mockito·DAO @SpringBootTest 실DB(고유 target + @AfterEach 정리). mvn test 전량 통과, 기준선 394/0 유지. 동시성 5스레드는 should. [후속 SR·이월] 만료 정리 배치·일일 상한은 범위 밖(계획 그대로). 확정 API(009)와 화면(007)은 다음 FUNC.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
계획 확인: 계획대로 진행 (2026-09-13, 수정 1회 — target 정규화·발송 로그 모양·UPSERT SET 순서 반영 후 승인)

구현 완료(2026-09-13). 계획 그대로 구현했고 계획 이탈은 없다.

**생성 파일**
- `modules/shop-api/src/main/resources/db/V5__member_password_resets.sql` — `MEMBER_PASSWORD_RESETS` 테이블(`CREATE TABLE IF NOT EXISTS`, PK=target).
- `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberPasswordReset.java`
- `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetDao.java` — `touchRequest`/`selectByTarget`/`deleteByTarget`.
- `modules/shop-api/src/main/resources/mapper/memberPasswordReset.xml` — 원자 UPSERT(created_at을 SET 목록 맨 뒤에 고정).
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetApiException.java`
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java` — 정규화(이메일 trim+소문자/휴대폰 숫자만) → SHA-256 해시 → 발송 로그(코드 원문 미기재) → `touchRequest` → 항상 202. 회원 테이블 미조회(생성자에 `MemberDao` 없음).
- `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetController.java` — `POST /api/members/password-resets/codes`, `@ResponseStatus(ACCEPTED)`.
- `modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberPasswordResetExceptionHandler.java` — `MemberSignupExceptionHandler`와 동일 패턴(`MBR-4100`/`MBR-5000`).
- `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberPasswordResetControllerTest.java` (6 tests)
- `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberPasswordResetServiceTest.java` (10 tests — 정규화 회귀·로그 회귀 포함)
- `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberPasswordResetDaoTest.java` (4 tests — 59초/61초 SET 순서 실증 + 5스레드 동시성)

**수정 파일**
- `modules/shop-api/src/main/resources/application.yml` — schema-locations 목록 끝에 `V5__member_password_resets.sql` 추가(기존 4개 순서 불변).
- `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — `MEMBER_PASSWORD_RESET_CODE_PATH`(`/api/members/password-resets/codes`) 화이트리스트 추가(add-only, `evaluateMemberScope`·기존 화이트리스트 8개 불변). 상단 누적 `linked_func`/`spec` 주석에 FUNC-member-008 추가.

**주요 결정(계획에서 재확인된 사항 포함)**
- 사람 수정 (1)(target 정규화)·(2)(발송 로그 모양)·(3)(UPSERT SET 순서)는 계획 본문에 이미 반영돼 있어 그대로 구현했다.
- `PHONE_PATTERN`을 `MemberSignupService`보다 넓혀(`^01[016789][-]?[0-9]{3,4}[-]?[0-9]{4}$`) 하이픈 포함 표기("010-1234-5678")도 정규화 전 형식 검증을 통과하도록 했다 — STORY가 "정규화가 1단계 매칭 결과를 바꾸지 않는다"고 명시했는데, 기존 숫자-only 패턴으로는 하이픈 표기가 애초에 형식 오류(400)가 되어 그 전제가 성립하지 않기 때문이다(계획에 정확한 정규식이 명시되지 않아 이 자리에서 내린 구현 판단).
- 로그 회귀 테스트는 dao.touchRequest에 전달된 codeHash를 캡처해 0~999999 전수 브루트포스로 평문 코드를 역산한 뒤 로그에 없음을 실증했다(코드 원문을 직접 알 방법이 없어 이 방식으로 "어디에도 없음"을 검증).

**검증**: `mvnw test` 전체 스위트 414/0(기존 394 + 신규 20, 실패 0). `mvnw compile`/`test-compile` 통과.

## Dev 기록 (round2 — QA FAIL 재작업)

재작업 지시 6건 중 사람이 이번 라운드 범위로 확정한 3건(필수1 + low(4) + low(5))만 반영했다.
medium(일일 상한)은 범위 밖으로 손대지 않았다 — `harness/decisions.all.md`에 이미 5줄 판단 기록이
있음을 확인(round1에서 선반영됨, "2026-09-13 — 비밀번호 재설정 코드 요청 API에 일일 상한·정리
배치를 두지 않는다"). low(3)(`expiresInSeconds` 상수 유지)·low(6)(`normalize`의 `\s` 제거 분기
유지)은 사람 코멘트 "(4) expiresInSeconds 상수 600과 공백 분기는 그대로(설계상 허용)"에 따라
그대로 뒀다.

**수정 파일**
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java`
  — (필수1) `touchRequest` 호출 뒤 `dao.selectByTarget(normalizedTarget)`으로 재조회해, 저장된
  `code_hash`가 방금 만든 `codeHash`와 같을 때만(SR-231 round5 `last_token` 재조회 대조와 동일
  기법) 발송 로그를 남기도록 순서를 바꿨다(로그를 `touchRequest` 앞 → 뒤로 이동 + `admitted` 판정
  추가). 응답은 여전히 무조건 202·상수 바디. (low(4)) `PHONE_PATTERN`을 `MemberRegistrationService.
  PHONE_PATTERN`(`^01[016789][0-9]{7,8}$`, 하이픈 불허)과 문자 단위로 동일하게 되돌려, 같은 입력이
  가입 API에서는 400·재설정 API에서는 202가 되던 불일치를 없앴다(이제 둘 다 400). 클래스 상단
  javadoc에 두 변경의 배경을 기록.
- `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberPasswordResetServiceTest.java`
  — 신규 `requestPasswordResetCode_withinCooldown_noSendLogAndStoredHashUnchanged`(쿨다운으로
  버려진 요청은 발송 로그 0건 + 재조회 해시가 이전 값 그대로임을 로그 어펜더로 검증, 필수1 회귀
  테스트). 기존 `logsChannelAndMaskedTarget_neverLogsPlainCode`는 새 판정 로직 때문에 목 DAO의
  `selectByTarget`이 `touchRequest`에 전달된 codeHash를 그대로 되돌려주도록 스텁하는 헬퍼
  `stubDaoAdmitsWrite()`를 추가해 통과하도록 고쳤다. 기존 `..._phoneWithAndWithoutHyphens_
  normalizeToSameTarget`은 하이픈 표기가 더 이상 유효 형식이 아니게 되어 성립하지 않아
  `..._hyphenatedPhone_rejectedLikeSiblingSignupApi`(400 확인)로 교체하고, 정규화 자체가 여전히
  동작함을 확인하는 `..._plainDigitPhone_normalizesToSameTarget`을 별도로 추가했다.
- `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberPasswordResetDaoTest.java` —
  (low(5)) 동시성 테스트의 target을 계획대로 `UUID` 접미로 바꿨다(고정 접미 `TARGET +
  "-concurrency"` → `"pwreset-dao-concurrency-" + UUID.randomUUID() + "@example.com"`).
  `@AfterEach`가 그 값을 지울 수 있도록 인스턴스 필드(`concurrencyTarget`)로 옮겼다.

**검증**: `mvnw compile`/`test-compile` 통과. `mvnw test` 전체 스위트 416/0(기존 414 + 서비스
테스트 순증 2 — 하이픈 테스트 1개 교체·정규화 테스트 1개·쿨다운 테스트 1개 추가, 실패 0).

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-13 — FAIL
원인(cause): `plan_gap` — 계획 "순서·보안" 5→6(발송 로그 → touchRequest)이 "쿨다운으로 버려질 코드를 이미 발송해 버린다"는 경우를 묻지 않았다. 구현은 계획을 이탈하지 않았다(계획의 순서 결정 자체가 결함).

- **Layer1 스펙: FAIL** — 계획·사람 수정 (1)(3)은 정확히 구현됐다. 정규화는 PK·DAO 인자·응답 body 3곳에 일관 적용(`MemberPasswordResetService:98,108,110`, 서비스 테스트 ArgumentCaptor 2건이 `01012345678`/`foo@bar.com` 동일성 실증), 정규화 규칙은 사람이 지목한 `MemberRegistrationService.normalizePhone`(`replaceAll("[-\\s]","")`)과 문자 단위로 동일. UPSERT SET의 `created_at` 최후 배치는 DAO 테스트 59초/61초 짝으로 **내가 직접 재실행해 실증**(`created_at`이 앞에 있었다면 61초 케이스가 실패한다 — 순서 고정이 실제로 증명됨). 코드 원문은 응답·로그·DB 어디에도 없다(해시만; 브루트포스 역산 테스트까지 있음). 회원 테이블 미조회도 생성자에 `MemberDao` 부재로 구조적으로 강제됨. **그러나** SR 확정 요건 "재전송은 60초 쿨다운"이 **발송 경로에는 전혀 적용되지 않는다** — 아래 필수 수정 1.
- **Layer2 보안: CONCERNS** — 무인증 화이트리스트 추가는 정확 일치 1건·add-only(기존 8건·`evaluateMemberScope` 불변)이고, 컨트롤러 테스트가 `X-Api-Key` 없이 202를 양성 확인한다. 500은 정제 메시지(`MBR-5000`)로만 응답하고 원본은 로그에만. 존재 오라클 없음(회원 조회 자체가 없어 실존/탈퇴/미존재 target이 같은 쿼리 1건·같은 202). 쿨다운 오라클도 없음(응답 상수). 다만 target별 60초 외에 **어떤 상한도, 만료행 정리도 없다** — 형제 API(`MemberSignupService`)는 일일 5회 상한 + `MemberSignupMaintenanceScheduler` purge를 가진다. 권고 1.
- **Layer3 회귀: PASS** — 공유 파일 2개 모두 add-only임을 원문으로 확인(`ApiKeyAuthFilter`는 상수 1개 + `isOpenRoute` OR절 1개, `application.yml`은 schema-locations 목록 끝 1항목). 기존 `MBR-4xxx` 대역과 `MBR-4100` 충돌 없음(전 소스 grep). `@RestControllerAdvice(assignableTypes=...)`로 스코프 한정 — 기존 핸들러와 무간섭. DDL 멱등성은 이미 테이블이 존재하는 DB에 두 번째 부팅이 성공한 것으로 실증(`@SpringBootTest` 컨텍스트 기동 성공). FUNC-009/007 범위 침범 없음(오늘 변경 파일 전수 확인 — shop-web 2개 파일은 00:08~00:13의 FUNC-member-004 산출물이고 008 배치는 08:59~09:02). `selectByTarget`/`deleteByTarget`은 계획에 명시된 승인 범위. 신규 20건(6+10+4) 전부 통과를 직접 재실행해 확인.

- 필수 수정(FAIL시):
  1. **`MemberPasswordResetService:105`의 "발송"이 쿨다운 판정보다 앞에서 무조건 실행된다.** `log.info(…발송…)` → `dao.touchRequest(…)` 순서라, 60초 안 재요청도 (a) 발송 이벤트를 또 남기고 — SR "재전송은 60초 쿨다운"이 DB 행에만 걸리고 발송 경로에는 안 걸린다 — (b) 방금 생성·발송한 코드 B는 UPSERT가 버리고 DB엔 코드 A의 해시가 남아, 이 로그가 실제 게이트웨이로 교체되는 순간(이 FUNC이 정의한 유일한 발송 지점) 수신자는 FUNC-009에서 **절대 검증되지 않는 코드**를 받는다. 사람이 지목한 재사용 대상 `MemberSignupService.requestVerificationCode`는 반대 순서다 — 레이트리밋 판정 통과 → `writeCode` → **그 다음에** 발송 로그. 수정: `touchRequest` 뒤 이미 있는 `dao.selectByTarget(normalizedTarget)`으로 저장된 `code_hash`가 방금 만든 `codeHash`와 같은지 확인해 **같을 때만** 발송 로그를 남긴다(SR-231 round5의 `last_token` 대조와 동일 기법). 응답은 여전히 무조건 202·상수 바디이므로 오라클은 새지 않는다. 서비스 테스트에 "쿨다운으로 버려진 경우 발송 로그가 없다" 케이스를 추가한다.

- 권고(CONCERNS시):
  1. (medium, 보안) 무인증 쓰기 경로에 target별 60초 쿨다운뿐이고 신규 target은 매 요청 새 행을 만든다 — 형식만 맞는 임의 target(`0101234xxxx`·랜덤 이메일)을 무제한 POST하면 `MEMBER_PASSWORD_RESETS`가 무한 증식하고(정리 배치 범위 밖), 필수 수정 1과 겹치면 임의 대상에게 무제한 발송이 된다. 일일 상한을 SR이 명시하지 않은 것은 사실이니 **둘 중 하나를 이번에 정한다**: ① 형제 API와 같은 target별 일일 상한을 추가(응답은 여전히 202 — 차등 응답 금지), 또는 ② `harness/decisions.all.md`에 "수용한 위험 + 반증 조건"으로 5줄 기록하고 후속 SR로 이월.
  2. (low) `expiresInSeconds`가 항상 600이라, 쿨다운으로 무시된 요청에는 실제 잔여 유효시간을 최대 60초 과대 표기한다. 상수 유지가 **맞다**(실제 잔여를 돌려주면 "최근에 재설정을 요청했는가"라는 활동 오라클이 된다) — 대신 이 트레이드오프를 `decisions.all.md`에 남기고, FUNC-007 화면의 "남은 유효시간"이 이 값을 절대 기준으로 신뢰하지 않게 STORY에 못 박는다.
  3. (low) `PHONE_PATTERN`을 형제 가입 API(`^01[016789][0-9]{7,8}$`)보다 넓혀(`[-]?` 허용) 같은 입력 `"010-1234-5678"`이 재설정에서는 202, 가입 인증코드에서는 400이 된다. 자릿수 범위는 동일하고 dev가 근거를 밝혔으니 차단하지 않되, 두 API 중 어느 쪽이 정본인지 결정해 007 화면이 어느 쪽을 보내도 되는지 남긴다.
  4. (low) DAO 동시성 테스트 target이 계획의 `UUID` 접미가 아닌 고정 접미(`TARGET + "-concurrency"`)다. `@AfterEach` 정리가 있어 사례집 SR-232 r2는 충족하나, surefire를 병렬화하면 충돌한다.
  5. (low) `normalize`의 `\s` 제거 분기는 `PHONE_PATTERN`이 내부 공백을 허용하지 않아 도달 불가(사문화). 참조 구현과의 대조 목적이면 주석 한 줄로 명시.
  - (오탐 배제 기록) "이메일을 소문자화해 저장하면 `MEMBERS.email`(원문 대소문자 보존, 소문자화 코드 없음)과 FUNC-009 매칭이 깨진다"를 의심해 실측했으나, `sl_lab`의 `members.email`·`member_password_resets.target` 모두 `utf8mb4_general_ci`(대소문자 무구분)라 `=` 비교가 성립한다 — 결함 아님. 단 이 안전성은 collation에 의존하므로 FUNC-009가 `BINARY`/`utf8mb4_bin` 비교를 도입하면 깨진다.

### QA Gate — 2026-09-13 — CONCERNS (round 2)
원인(cause): `spec_gap` — 코드 결함은 없다(round1 필수1은 실증적으로 해소). 남은 것은 사람이 확정한 "휴대폰 형식은 형제 가입 API와 동일(하이픈 불허)"이 **STORY "계약" 절 본문(line 86)과 정면으로 모순**된 채 남아 있는 것이다. 이 절이 `/sl-recon-inf`가 INF-MBR-006을 역생성할 원문이고 FUNC-007/009 dev가 읽을 계약이라, 텍스트를 그대로 두면 다음 FUNC이 하이픈 표기를 보내도 된다고 읽는다. 코드가 아니라 정본 텍스트의 갭이라 차단하지 않는다(라운드 추가 비용 > 텍스트 1줄 수정 비용).

- **Layer1 스펙: CONCERNS** — **round1 필수1은 해소됐다(직접 실증)**. `MemberPasswordResetService:124~136`이 `dao.touchRequest`(UPSERT) → `dao.selectByTarget` 재조회 → `codeHash.equals(stored.getCodeHash())`일 때만 발송 로그를 남기는 순서로 바뀌었고(SR-231 r5 `last_token` 대조 기법 그대로), 응답은 여전히 무조건 202·상수 바디라 오라클은 새지 않는다. "쿨다운이면 로그 0건"은 서비스 테스트 `requestPasswordResetCode_withinCooldown_noSendLogAndStoredHashUnchanged`가 `logAppender.list`가 **완전히 비어 있음**으로 검증한다(발송 로그만이 아니라 이 로거의 전 출력 0건 — 더 강한 조건). 목 DAO의 한계(서비스 테스트는 SQL을 안 탄다)는 **세 테스트의 합성으로 메워져 있음을 확인**했다: ① `MemberPasswordResetDaoTest.touchRequest_within59Seconds_*`가 실 DB에서 59초 재요청 시 `code_hash` 불변을 증명, ② `touchRequest_newTarget_*`가 실 DAO가 전달한 해시를 **문자 단위로 그대로 되돌려준다**는 것을 증명(`assertThat(saved.getCodeHash()).isEqualTo("a".repeat(64))` — 재조회 대조가 실 DB에서도 성립하는 유일한 전제), ③ 서비스 테스트가 비교 로직을 증명. 즉 "쿨다운이면 실 DB에서도 admitted=false"가 성립한다. low(4) `PHONE_PATTERN`은 `MemberRegistrationService.PHONE_PATTERN`(`^01[016789][0-9]{7,8}$`)과 **문자 단위로 동일**함을 양쪽 원문 대조로 확인(하이픈 입력은 이제 양쪽 다 400). low(5) DAO 동시성 target은 `"pwreset-dao-concurrency-" + UUID.randomUUID() + "@example.com"` + 인스턴스 필드 `concurrencyTarget`을 `@AfterEach`가 정리. medium(일일 상한)은 사람 결정대로 범위 밖이며 `harness/decisions.all.md`에 5줄(정한 것/왜/반증 조건/되돌리기)이 실제로 있는 것을 확인했다 — **재차 막지 않는다**. 남은 갭은 권고 1(계약 텍스트 모순).
- **Layer2 보안: PASS** — 재조회 추가로 무인증 경로의 쿼리가 1건→2건이 됐지만 **차등 응답·차등 시간이 생기지 않는다**: 두 쿼리 모두 `target` PK 단건이고 회원 테이블은 여전히 일절 조회하지 않으며(생성자에 `MemberDao` 부재로 구조적 강제 유지), 실존/미존재/탈퇴 target이 완전히 동일한 경로를 탄다. 쿨다운 판정 결과는 **로그에만** 반영되고 응답 바디는 상수(`{channel, target, 600}`)라 클라이언트가 관측할 수 있는 오라클이 새로 생기지 않았다. 코드 원문은 응답·로그·DB 어디에도 없다(브루트포스 역산 테스트 유지, `stubDaoAdmitsWrite()`로 발송 경로가 실제 실행되는 조건에서 검증됨 — round1보다 오히려 강해졌다). 화이트리스트는 정확 일치 1건·add-only 그대로(`evaluateMemberScope`·기존 8건 불변). 500은 정제 메시지(`MBR-5000`)만, 원본은 로그에만. 일일 상한 부재는 사람이 수용하고 반증 조건까지 기록한 결정(SR-297 이월)이므로 이슈로 다시 세우지 않는다.
- **Layer3 회귀: PASS** — 전체 스위트를 **내가 직접 재실행**해 surefire 집계 **416/0**(failures 0·errors 0·skipped 0) 확인, 기준선 394(`.speclinker/test_baseline.json`) 대비 순증 22(6+12+4), 감소 0. round2가 만진 파일은 서비스 1개 + 테스트 2개뿐이고 공유 파일(`ApiKeyAuthFilter`·`application.yml`)은 round1 상태 그대로 add-only임을 diff 원문으로 재확인했다. `PHONE_PATTERN` 축소가 기존 호출자를 깨지 않음을 확인: `password-resets` 경로를 부르는 코드는 자기 컨트롤러 테스트 6건이 전부이고(전 모듈 grep), shop-web에는 비밀번호 재설정 화면이 아직 없다(FUNC-member-007은 `status: Approved`·GATE `gate: null`, `modules/shop-web/src/pages`에 해당 페이지 없음) — 즉 지금 고쳐야 깨질 소비자가 없다(타이밍이 맞다). 교체된 테스트(`..._phoneWithAndWithoutHyphens_*` → `..._hyphenatedPhone_rejectedLikeSiblingSignupApi`)는 삭제가 아니라 대체이고, 정규화 자체의 회귀는 `..._plainDigitPhone_normalizesToSameTarget`·`..._emailCaseAndWhitespaceVariants_*`가 계속 지킨다. `rules_check.py` 결과 must 0(should 1은 `ApiKeyAuthFilter` 458줄이나 **이 FUNC 이전에 이미 451줄**로 상한을 넘겨 있었다 — 이 라운드 귀속 아님).

- 권고(CONCERNS시):
  1. (medium, spec) **STORY "계약" 절(line 86)이 구현과 모순된다.** 그 줄은 아직 "예: 요청이 `"010-1234-5678"`이면 응답은 `"01012345678"`"이라고 적고 있는데, round2 이후 그 입력은 400 `MBR-4100`이다. 사람 코멘트 (2)가 형제 가입 API 형식을 정본으로 확정했으므로 **코드가 맞고 텍스트가 틀렸다**. 이 절은 ① `/sl-recon-inf`가 INF-MBR-006 본문을 역생성할 원문이고 ② FUNC-007(화면)·FUNC-009(확정)가 읽을 계약이라, 방치하면 다음 FUNC이 하이픈 표기를 보내도 된다고 구현한 뒤 400을 맞는다(사례집 "결정표가 서술보다 우선한다 — 테스트/문서를 결정표에 맞춘다", SR-232 r3와 같은 계열). 조치: line 86의 하이픈 예시를 "하이픈·공백 포함 표기는 형식 오류(400 MBR-4100) — 형제 가입 API와 동일"로 정정하고, 정규화 예시는 이메일(`Foo@Bar.COM` → `foo@bar.com`)만 남긴다. 재작업 지시 4가 요구한 "FUNC-007 화면이 어느 표기를 보내도 되는지"는 `STORY-FUNC-member-007.md`(아직 `Approved`, 구현 전)에 한 줄로 못 박는다 — 지금이 가장 싼 시점이다(코드 변경 0).
  2. (low, correctness) 재조회가 실패하면 **쓰기는 이미 커밋됐는데 발송은 일어나지 않는다**. `dao.touchRequest` 성공 후 `dao.selectByTarget`이 `DataAccessException`을 던지면 500 `MBR-5000`이 나가고, DB에는 아무에게도 전달되지 않은 코드 A의 해시가 남는다 — 사용자가 즉시 재시도해도 쿨다운이 코드 B를 버리므로(로그도 없음) **60초간 어떤 코드도 받지 못한다**. round2가 두 번째 쿼리를 도입해 새로 생긴 창이다. 응답을 분기하면 오라클이 되므로 응답은 그대로 두고, 선택지는 두 가지뿐이다: 재조회 실패를 `catch`해 "발송 미확정" 경고 로그만 남기고 202로 내려보내거나(사용자 관점 개선 없음, 로그로만 관측), 이 창을 수용한 위험으로 `decisions.all.md`에 남긴다. 후속 SR-297(일일 상한·정리 배치)에 묶는 것이 자연스럽다.
  3. (low, test-coverage) 새 테스트의 마지막 assert(`dao.selectByTarget(target).getCodeHash()).isEqualTo(previousHash)`, 서비스 테스트 line 244)는 **목이 스텁된 값을 되돌려주는지 확인할 뿐 아무것도 증명하지 않는다**(tautology). 이 테스트에서 실제 하중을 지는 것은 `logAppender.list).isEmpty()` 하나이고, "쿨다운이면 DB 해시가 안 바뀐다"는 `MemberPasswordResetDaoTest.touchRequest_within59Seconds_*`가 실 DB로 증명한다. 지워도 커버리지 손실이 없고, 남기면 다음 세션이 "서비스 테스트가 DB 불변을 검증한다"고 오독한다 — 삭제하거나 주석으로 "이 assert는 목 스텁 확인용이며 실증은 DaoTest 소관"임을 명시.
  4. (low, spec) 재작업 지시 3(`expiresInSeconds` 상수 600의 과대 표기 트레이드오프)의 `decisions.all.md` 기록은 없다. 사람 코멘트 "(4) ... 그대로(설계상 허용)"로 코드는 확정됐으니 차단하지 않되, FUNC-007 화면이 이 값을 절대 잔여시간으로 신뢰하면 "남은 유효시간" 표시가 최대 60초 틀린다 — 권고 1의 STORY-007 한 줄에 같이 적는 것이 가장 싸다.
  - (오탐 배제 기록) round2가 도입한 재조회가 "목 DAO 없이는 절대 로그를 남기지 않는다"(=운영에서 발송이 아예 안 된다)는 역결함을 의심했으나, `MemberPasswordResetDaoTest.touchRequest_newTarget_*`가 실 DAO 왕복에서 해시가 문자 단위로 보존됨을 증명하므로 신규 target·쿨다운 경과 target은 실 DB에서도 `admitted=true`가 된다 — 결함 아님. 또 `code_hash CHAR(64)`의 trailing-space 절단·`utf8mb4_general_ci` 대소문자 무구분이 비교를 흐트릴 가능성도 검토했으나, 값이 공백 없는 소문자 hex 64자 고정이라 양쪽 모두 무해하다(단 이 안전성은 "해시를 항상 소문자 hex로만 쓴다"는 관례에 의존한다 — FUNC-009가 대문자 hex를 쓰면 깨진다).

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [high/spec] MemberPasswordResetService:105 — '발송' 로그가 쿨다운 판정(dao.touchRequest, :108)보다 앞에서 무조건 실행된다. 60초 안 재요청도 발송 이벤트를 또 남기므로 SR 확정 요건 '재전송은 60초 쿨다운'이 DB 행에만 걸리고 발송 경로에는 전혀 안 걸린다. 게다가 방금 생성·발송한 코드 B는 UPSERT가 버리고 DB엔 코드 A의 해시가 남아, 발송된 코드와 저장된 해시가 갈린다(DAO 테스트 59초 케이스가 code_hash 불변을 실증하므로 확정). 사람이 재사용 대상으로 지목한 MemberSignupService.requestVerificationCode는 반대 순서다 — 레이트리밋 통과 → writeCode → 그 다음 발송 로그. → touchRequest 뒤 이미 존재하는 dao.selectByTarget으로 저장된 code_hash가 방금 만든 codeHash와 같은지 확인해 같을 때만 발송 로그를 남긴다(SR-231 round5 last_token 대조와 동일 기법). 응답은 여전히 무조건 202·상수 바디이므로 오라클은 새지 않는다. 서비스 테스트에 '쿨다운으로 버려진 경우 발송 로그 없음' 케이스 추가.
2. [medium/security] 무인증 쓰기 경로에 target별 60초 쿨다운뿐이고, 신규 target은 매 요청 새 행을 만든다. 형식만 맞는 임의 target을 무제한 POST하면 MEMBER_PASSWORD_RESETS가 무한 증식하고(만료행 정리 배치는 범위 밖), 위 high 이슈와 겹치면 임의 대상에게 무제한 발송이 된다. 형제 API(MemberSignupService)는 target별 일일 5회 상한 + MemberSignupMaintenanceScheduler purge를 둘 다 가진다. → ① 형제 API와 같은 target별 일일 상한 추가(응답은 여전히 202 — 차등 응답 금지), 또는 ② harness/decisions.all.md에 '수용한 위험 + 반증 조건' 5줄로 기록하고 후속 SR로 이월. 둘 중 하나를 이번 라운드에 정한다.
3. [low/spec] expiresInSeconds가 항상 600이라 쿨다운으로 무시된 요청에는 실제 잔여 유효시간을 최대 60초 과대 표기한다. 상수 유지 자체는 옳다(실제 잔여를 돌려주면 '최근에 재설정을 요청했는가' 활동 오라클이 된다). → 트레이드오프를 decisions.all.md에 기록하고, FUNC-007 화면의 '남은 유효시간'이 이 값을 절대 기준으로 신뢰하지 않도록 STORY에 명시.
4. [low/spec] PHONE_PATTERN을 형제 가입 API(^01[016789][0-9]{7,8}$)보다 넓혀 하이픈을 허용해, 같은 입력 '010-1234-5678'이 재설정에서는 202·가입 인증코드에서는 400이 된다(자릿수 범위는 동일, dev가 근거 명시). → 두 API 중 어느 쪽 형식이 정본인지 결정하고, FUNC-007 화면이 어느 표기를 보내도 되는지 STORY/INF에 남긴다.
5. [low/regression] DAO 동시성 테스트 target이 계획의 UUID 접미가 아니라 고정 접미(TARGET + "-concurrency")다. @AfterEach 정리가 있어 사례집 SR-232 r2는 충족하나 surefire 병렬화 시 충돌한다. → 계획대로 UUID 접미로 바꾸거나, 병렬화하지 않는다는 전제를 테스트 주석에 남긴다.
6. [low/spec] MemberPasswordResetService.normalize의 \s 제거 분기는 PHONE_PATTERN이 내부 공백을 허용하지 않아 도달 불가(사문화 코드). → 참조 구현(normalizePhone)과의 문자 단위 동일성 유지가 목적이면 그 의도를 주석 한 줄로 명시.

대상 파일: modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java

사람 코멘트: [결정 요약] 필수 1건 재작업 + low 2건은 같은 라운드에서 같이(싸고 같은 파일). medium(일일 상한)은 SR 범위 밖 — decisions.all.md에 수용 기록하고 후속 SR로 이월. [구현 방식] (1) 순서: 코드 생성→해시→touchRequest(UPSERT)→dao.selectByTarget으로 저장된 code_hash가 방금 만든 해시와 같을 때만 발송 로그(SR-231 r5 last_token 대조 기법). 쿨다운으로 버려졌으면 로그 없음. 응답은 어느 경우든 202·상수 바디. (2) PHONE_PATTERN·normalize는 새로 두지 않고 형제 가입 API의 상수·메서드(MemberRegistrationService.normalizePhone과 그 검증 정규식)를 그대로 재사용해 같은 입력이 한쪽 202·한쪽 400이 되는 일을 없앤다. (3) DAO 동시성 테스트 target에 UUID 접미(계획대로). (4) expiresInSeconds 상수 600과 공백 분기는 그대로(설계상 허용). [테스트·완료 조건] 서비스 테스트에 60초 안 재요청 시 발송 로그 0건·DB 해시 불변 케이스 추가(로그 어펜더 캡처). mvn test 전량 통과, 기준선 394/0 유지. [후속 SR·이월] 일일 상한·만료 정리 배치 → 후속 SR 접수(SR-297 예정), decisions.all.md 5줄.
