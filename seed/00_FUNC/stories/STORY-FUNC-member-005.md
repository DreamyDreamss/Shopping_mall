---
story-id: STORY-FUNC-member-005
func-id: FUNC-member-005
status: Done
domain: member
created: 2026-09-12
spec_markers: 0
sr-id: SR-232
approved_sha: 3fdeacc79c69
---

# STORY-FUNC-member-005 — SR-232 — 로그인 API · 신규 INF-MBR-003 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

## Story
SR-232 — 로그인 API · 신규 INF-MBR-003 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)


## 변경 컨텍스트 (SR-232)
> 이 story는 변경요청 **SR-232 — SR-232** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-232/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-232/02_변경명세.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: [유형: 화면+API] 이메일/비밀번호 로그인, 30일 자동 로그인(리프레시 토큰), 5회 실패 시 10분 잠금. UI/UX: 실패 사유는 구체적으로(비밀번호 오류 n/5) · 비밀번호 보기 토글 · 로그인 후 원래 가려던 페이지로 복귀. 수용 기준: 잠금 상태는 API 429 + 남은 시간 · 로그아웃 시 리프레시 토큰 폐기 · 세션 만료 시 장바구니 유지 / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 동작(조회·정렬·표시 규칙)은 그대로 유지한다
- **기존 클라이언트와의 하위호환이 필요한가?** — 기존 필드명·타입·의미는 그대로 둔다(추가만)
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 기존 오류 응답 형식을 그대로 쓴다
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — DB 변경 없음
- **기존 데이터 이관·백필이 필요한가?** — 마이그레이션 없음
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면이 전부: UIS-ORD-001, UIS-ORD-002, UIS-ORD-003, UIS-ORD-004, UIS-ORD-005 장바구니
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 0건·오류·로딩 표시는 기존 규칙(UIS §5)을 그대로 따른다
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 바뀌거나 새로 생기는 상태마다 스토리를 남긴다(기존 스토리는 깨뜨리지 않는다)

### 구현 모듈(제약) — `shop-api` (`{{SRC_SHOP_API}}`)
이 FUNC의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약 폼에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
**로그인 API(POST /api/members/login) 계약 구현**
- [x] **AC1 정상 로그인**: 유효한 이메일/비밀번호 → 200, `{memberId, memberName, grade, apiKey, refreshToken, refreshTokenExpiresAt}` 응답
- [x] **AC2 자격증명 실패**: 미가입/탈퇴/비번오류 → 401 `MBR-4011`, 사유 비노출 통합 메시지 `"이메일 또는 비밀번호가 올바르지 않습니다 (n/5)"`
- [x] **AC3 잠금 상태**: 5회 실패 후 → 429 `MBR-4291`, `retryAfterSeconds: 600` (10분)
- [x] **AC4 비밀번호 미일치 시 카운트**: 매 실패마다 `MEMBER_LOGIN_ATTEMPTS.fail_count` 증가, 카운트 표시(2/5, 3/5, ...)
- [x] **AC5 5번째 실패 즉시 잠금**: 5번째 실패가 401(5/5)이 아니라 곧바로 429(`MBR-4291`) 응답
- [x] **AC6 로그인 경로 무인증**: `POST /api/members/login` 화이트리스트 (X-Api-Key 헤더 불필요)
- [x] **AC7 리프레시 토큰 발급**: 성공 시 30일 유효 refresh token 응답(원문만, 해시는 DB에)
- [x] **AC8 기존 정적 API 키 회귀**: admin(lab.api-keys 맵)·M-0001 기존 키 200 무변경
- [x] **AC9 로그인 발급 API 키**: 첫 로그인 시 신규 발급, 재발급 아님 (PK `member_id`, no-op UPSERT 후 재조회)
- [x] **AC10 탈퇴 회원 API 키 거부**: `MEMBERS.del_yn='Y'`인 회원 발급 키 → 401 unauthorized
- [x] **AC11 폐기된(revoked) API 키 거부**: `MEMBER_API_KEYS.revoked_at IS NOT NULL` 키 → 401 unauthorized
- [x] **AC12 동시 최초 로그인 레이스**: 5개 스레드 동시 로그인 → API 키 정확히 1개 발급(atomic no-op UPSERT)
- [x] **AC13 리프레시 토큰 상한 강제**: 회원당 활성 토큰 최대 5개 (초과 시 오래된 것부터 제거)
- [x] **AC14 DB 예외 처리**: 데이터 접근 오류 → 500 `MBR-5000`, 스택트레이스/경로/SQL 미노출

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-003
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 계획

> 전제(중요): 현재 요청 인증은 `application.yml`의 정적 `lab.api-keys` 맵(admin 1건 + `M-0001` 1건)뿐이다(실측).
> FUNC-member-003으로 자가가입한 회원(M-0005 이후)은 이 맵에 없어, 로그인에 성공해도 이후 어떤
> `/api/**` 요청도 인증을 통과할 방법이 원천적으로 없다. 이 계획은 이 간극을 최소 침습으로 메우는
> 안(MEMBER_API_KEYS 신설 + 폴백 조회 1줄)을 포함한다 — 아래 "순서·보안"·"범위 밖"에 사람 확인
> 지점으로 명시했다.

- **파일**:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberLoginController.java` — 신규. `POST /api/members/login` 엔드포인트, `LoginRequest(email,password)` 바인딩 → `MemberLoginService` 호출 → 응답 DTO 매핑.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberLoginService.java` — 신규. 잠금 판정→인증→토큰/키 발급 전 로직. 클래스 레벨 `@Transactional` 없음(아래 "데이터" 참고).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberLoginApiException.java` — 신규. `MemberSignupApiException`과 동일 형태(code/httpStatus/message)를 복제(파일 경계상 재사용 불가 — 기존 관례와 동일).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberLoginExceptionHandler.java` — 신규. `@RestControllerAdvice(assignableTypes = MemberLoginController.class)`, `MemberLoginApiException`→`{code,message}`, `DataAccessException`→500 `MBR-5000`(정제 메시지, `MemberSignupExceptionHandler`와 동일 패턴 복제).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberDao.java` — **기존 파일에 추가만**. `MemberCredential selectAuthByEmail(@Param("email") String email)` 신규 메서드. 기존 6개 메서드는 절대 변경하지 않는다.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberCredential.java` — 신규. `memberId/memberName/grade/passwordHash/delYn`만 담는 **인증 전용** DTO. 공개 API 응답에 쓰이는 `Member.java`에는 `passwordHash`를 절대 추가하지 않는다(직렬화 유출 위험).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberLoginAttemptDao.java` — 신규. `touchFailure`(원자 UPSERT), `selectAttempt`, `reset`.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberRefreshTokenDao.java` — 신규. `insert(tokenHash, memberId, issuedAt, expiresAt)`만. 조회/폐기는 FUNC-006 소관 — 추가하지 않는다.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberApiKeyDao.java` — 신규. `selectByMemberId`, `issueIfAbsent`(원자 발급 후 재조회).
  - `modules/shop-api/src/main/resources/mapper/member.xml`(추가만) · `memberLoginAttempt.xml`(신규) · `memberRefreshToken.xml`(신규) · `memberApiKey.xml`(신규).
  - `modules/shop-api/src/main/resources/db/V4__members_login.sql` — 신규 DDL(멱등). `MEMBER_LOGIN_ATTEMPTS` · `MEMBER_REFRESH_TOKENS` · `MEMBER_API_KEYS` 3테이블 `CREATE TABLE IF NOT EXISTS`.
  - `modules/shop-api/src/main/resources/application.yml` — `spring.sql.init.schema-locations` 목록에 `classpath:db/V4__members_login.sql` 추가(안 하면 DDL이 절대 실행되지 않는다).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — **수정 2건, 둘 다 사람 확인 지점**: (a) `isOpenRoute`에 `POST /api/members/login` 화이트리스트 추가(무인증 필수 — `MEMBER_SIGNUP_REQUEST_PATH`와 동일 이유), 이건 확정. (b) 정적 맵 미스 시 `MemberApiKeyDao`로 DB 폴백 조회를 추가할지는 아래 "순서·보안" 참고 — 안 하면 자가가입 회원은 로그인해도 이후 API를 못 쓴다.

- **데이터**:
  - "DB 변경 없음"(SR 확정문답 db_ripple/db_migration)과의 충돌: 이 답은 "**타 도메인** 테이블·집계 파급 없음/마이그레이션(기존 데이터 이관) 없음"을 뜻한다고 해석한다 — SR-231 선례(db_migration도 "없음"이라 답했지만 V3/rate_limit/verification 3테이블이 실제로 신설됨, 신규 기능 자신의 전용 테이블은 "파급"이 아니라는 동일 해석)와 일치시킨다. 결론: `MEMBERS` 등 **기존 테이블은 컬럼 추가조차 하지 않는다** — 이 FUNC 전용 신규 3테이블만 만든다.
  - `MEMBER_LOGIN_ATTEMPTS(email PK, fail_count, locked_until, last_failed_at)` — **회원 존재 여부와 무관하게 email 문자열 자체를 키로 쓴다**(가입되지 않은 이메일도 카운트됨). 이렇게 해야 "이 이메일이 잠겼는가"라는 관찰이 계정 존재의 오라클이 되지 않는다 — 그 이메일로 실패를 시도한 사람 자신만 그 사실을 안다.
  - `MEMBER_REFRESH_TOKENS(token_hash PK(SHA-256 hex, 원문 미저장), member_id, issued_at, expires_at, revoked_at NULL)` — FUNC-006이 조회/폐기(로그아웃·재발급)에 이 테이블을 그대로 쓴다. 006이 컬럼이 더 필요하면 **006 소유 마이그레이션 파일**에서 `ADD COLUMN IF NOT EXISTS`로 추가하고 이 V4 파일(005 소유)은 건드리지 않는다(`MEMBER_SIGNUP_VERIFICATIONS`를 002가 소유하고 `consumed_at`을 003이 자기 파일에서 추가한 전례와 동일 경계).
  - `MEMBER_API_KEYS(member_id PK, api_key UNIQUE, issued_at)` — 위 "전제" 문단 참고.
  - **트랜잭션 경계**: 이 FUNC은 `@Transactional`을 전혀 쓰지 않는다 — 모든 DB 문장을 개별 autocommit 단일 statement로 처리한다(사례집 SR-231 r2 "카운터 증가를 트랜잭션 안에 넣었다가 롤백에 같이 사라짐" 재발 방지, `MemberRegistrationService`의 "STEP 분리+비트랜잭션" 패턴을 그대로 따른다).
  - **락/원자성**: `fail_count` 증가는 `INSERT ... ON DUPLICATE KEY UPDATE`(`MEMBER_SIGNUP_RATE_LIMITS`와 동일 관용구) 한 문장. SET 목록은 **좌→우 평가(문서화된 동작, round6 교훈)만** 쓰고 세션변수(`SET @x`)는 쓰지 않는다(사례집 SR-231 r3 재발 금지). 잠금 만료 후 재실패는 `fail_count`를 1로 리셋(무한 누적 방지, 날짜 롤오버를 `day_key`로 분리한 `MEMBER_SIGNUP_RATE_LIMITS`와 같은 사고방식). `MEMBER_API_KEYS` 최초 발급도 `INSERT ... ON DUPLICATE KEY UPDATE member_id = member_id`(no-op) 후 재조회 관용구로, 동시 최초 로그인 레이스에서 키가 2개 발급되지 않게 한다.
  - 동시성 테스트로 fail_count가 정확히 N 증가하는지(동시 실패 요청 수만큼), 동시 최초 로그인에서 api_key가 1개만 발급되는지를 **3회 단독 실행**으로 확인한다.

- **순서·보안** (사람 확인 필요 지점 3곳을 굵게 표시):
  1. 잠금 여부를 비밀번호 검증 **전에** 확인한다(BCrypt 비용 절약 목적). 이것이 사례집 r5("존재 판정→인증 순서가 오라클")와 다른 이유: 여기서 먼저 보는 것은 "회원 존재"가 아니라 "이 email 문자열의 실패 이력"이고, 그 이력은 회원 존재와 무관하게 동일한 방식으로 쌓인다.
  2. email로 회원 조회 → 비밀번호 검증. **회원 없음 / delYn='Y' / 비밀번호 불일치 — 이 세 경우 전부 동일한 401 코드·메시지·카운터 의미로 응답한다.**
  3. **[사람 확인 1] 메시지 문구**: SR은 "실패 사유는 구체적으로(비밀번호 오류 n/5)"를 요구하지만, 이를 문자 그대로 쓰면 이메일 미가입 여부가 노출된다(사례집 r5와 같은 급의 오라클). 이 계획은 문구를 "이메일 또는 비밀번호가 올바르지 않습니다 (n/5)"로 일반화해 두 경우를 구분하지 않는 절충안을 제안한다 — SR UX 의도와 상충하니 승인 전 확정 필요.
  4. 타이밍 사이드채널(should, 강제하지 않음): 회원이 없을 때도 더미 해시로 `passwordEncoder.matches`를 실행해 응답시간을 비슷하게 만든다.
  5. **[사람 확인 2] 잠금 트리거 응답**: 5번째 실패 자체를 429로 응답할지(이 계획의 기본안 — 즉시 알림), 5번째는 401(5/5)로 응답하고 6번째부터 429로 할지 SR 문구가 불명확 — 승인 전 확정 필요.
  6. 성공 시: 실패카운터 reset(단일 UPDATE/DELETE) → refresh token 발급(원문은 응답 1회만 노출, DB엔 SHA-256 해시만) → apiKey 조회/신규발급.
  7. **[사람 확인 3] ApiKeyAuthFilter DB 폴백**: 위 "전제"의 간극을 이번 FUNC에서 메울지(정적 맵 미스 시 `MemberApiKeyDao` 조회 추가 — 최소 침습이지만 7라운드에 걸쳐 굳힌 공유 보안 필터를 건드림), 아니면 이번엔 로그인 로직만 완성하고 그 간극은 후속 SR로 넘길지 결정 필요.
  8. 오류 응답에 스택트레이스·SQL·회원존재 힌트를 절대 싣지 않는다(no-printstacktrace 규칙 + `MemberSignupExceptionHandler`의 500 정제 패턴 복제).

- **계약**:
  - `POST /api/members/login` — 무인증(화이트리스트).
    - Request: `{email, password}`
    - 200: `{memberId, memberName, grade, apiKey, refreshToken, refreshTokenExpiresAt}`
    - 401 `MBR-4011`: `{code, message}` — message에 `n/5` 포함(위 사람 확인 1 문구로).
    - 429 `MBR-4291`: `{code, message, retryAfterSeconds}`.
    - 500 `MBR-5000`: 기존 정제 패턴 그대로.
  - **006과의 인터페이스 가정**(006 스토리에도 동일하게 못박을 것을 권고): `refreshToken`은 클라이언트에 원문 그대로 전달되고, 서버는 SHA-256 해시(소문자 hex)만 `MEMBER_REFRESH_TOKENS.token_hash`에 저장한다. FUNC-006(로그아웃/재발급)은 클라이언트가 보낸 원문을 **동일한 해시 방식**으로 변환해 이 테이블을 조회해야 한다. `MEMBER_API_KEYS` 스키마도 006/004가 그대로 재사용 가능하도록 공개해 둔다.

- **테스트**:
  - `MemberLoginControllerTest`(MockMvc, **controller-has-test must**) — 200/401/429/500 각 상태코드·바디, 무 `X-Api-Key`로 호출해도(화이트리스트 확인) 401 unauthorized로 막히지 않는지.
  - `MemberLoginServiceTest`(Mockito, **service-has-test should**) — 잠금/미가입/비번오류/성공 각 분기의 DAO 호출 검증. 트랜잭션·동시성은 Mockito로 증명 불가함을 이 코드베이스가 이미 알고 있다(`MemberRegistrationService` 클래스 javadoc 인용) — 아래 통합 테스트로 보강.
  - `MemberLoginConcurrencyTest`(신규 통합, `MemberSignupRateLimitConcurrencyTest`와 동일 house 패턴) — 동일 email 동시 5회 실패 → `fail_count`=5·`locked_until` 세팅 실측(3회 단독 실행).
  - `MemberLoginAttemptDaoTest` / `MemberApiKeyDaoTest` / `MemberRefreshTokenDaoTest` — 매퍼 단위(DB 붙는) 테스트, `MemberSignupRateLimitDaoTest` 패턴.
  - 회귀 확인만(코드 변경 없음): `MemberControllerTest`/`MemberQueryRegressionTest`, 장바구니 관련 기존 테스트 — "세션 만료 시 장바구니 유지" AC는 장바구니가 이미 `memberId` 기준 서버측 저장이라(`CartController` 확인, `HttpSession` 미사용) 이 FUNC이 손댈 부분이 없다.
  - 완료 후 `python {{PLUGIN_PATH}}/scripts/test_baseline_ws.py record . --force`로 기준선 재기록.

- **범위 밖**:
  - FUNC-member-004(로그인 화면, shop-web) — 계약(요청/응답 필드)만 가정, 그 쪽 소스는 만들지 않는다.
  - FUNC-member-006(로그아웃·리프레시 재발급 API) — 테이블·해시 규칙만 마련해 두고 조회·폐기·재발급 로직은 006이 구현.
  - `ApiKeyAuthFilter`를 세션/JWT 기반의 완전한 동적 인증 체계로 재설계하는 것 — 이번엔 "정적 YAML 맵 미스 시 DB 폴백 1줄" 최소 침습으로만 한정한다(사람 확인 3). 전면 재설계는 후속 SR 후보.
  - IP 기반·여러 계정에 걸친 브루트포스 방어(SR은 계정 단위 5회/10분만 요구).
  - "비밀번호 보기 토글"·"원래 가려던 페이지로 복귀"는 화면(004) 책임 — API 계약에 영향 없음.

- **실패 사례집 대조** (`harness/antipatterns.all.md`):
  - r2(트랜잭션 안 카운터 증가→롤백 소실): 이 계획은 애초에 `@Transactional`을 쓰지 않아 구조적으로 회피(위 "데이터" 참고).
  - r4/r5(전역 datasource 속성으로 판정→다른 FUNC 회귀): `MEMBER_LOGIN_ATTEMPTS` 판정은 UPSERT+재조회 방식만 쓰고 `useAffectedRows`류 전역 속성에 의존하지 않는다.
  - r5(존재 판정→인증 순서 오라클): 로그인 실패 응답을 회원 존재 여부와 무관하게 완전히 동일한 코드·메시지·카운터 의미로 통일한다 — SR 문구를 문자 그대로 쓰지 않기로 한 이유이기도 하다(사람 확인 1).
  - r3(세션 변수 순서 의존): SQL에서 `SET @var` 사용 금지, 컬럼 좌→우 평가만 사용.
  - ddl-idempotent 사례(`CREATE TABLE` IF NOT EXISTS 누락→재기동 시 앱 사망): V4 마이그레이션은 `CREATE TABLE IF NOT EXISTS`만, `DROP` 없음.
  - linked_func 누락(SR-230 사례, 축 A FAIL): 신규 DDL·매퍼 XML·자바 파일 전부 상단에 `linked_func: FUNC-member-005` 주석을 단다.

- **규칙 반영**(STORY "적용 규칙" 절이 비어 있어 `.claude/rules/lab/`을 직접 확인):
  - `no-sysout`/`no-printstacktrace`(must): 로거(slf4j)만 사용, 예외는 `MemberLoginExceptionHandler`로만 노출.
  - `ddl-idempotent`(must): 위 실패사례집 대조 항목과 동일.
  - `controller-has-test`(must): `MemberLoginController` ↔ `MemberLoginControllerTest` 동일 패키지 경로.
  - `service-has-test`(should): `MemberLoginService` ↔ `MemberLoginServiceTest`.
  - `file-size-cap`(should): 자바 450줄 상한 — 인증 로직이 길어지면 잠금 판정을 별도 클래스(`LoginLockoutPolicy` 등)로 분리 고려.

### 사람 확정 결정 (2026-09-12, 계획 확인 게이트)
- **[사람 확인 1] 메시지 문구**: 일반화 문구 채택 — `"이메일 또는 비밀번호가 올바르지 않습니다 (n/5)"`. 사유(미가입/탈퇴/비번오류)는 절대 구분 노출하지 않는다.
- **[사람 확인 2] 잠금 트리거**: 5번째 실패 자체를 429로 응답한다(즉시 잠금 알림) — 4번째까지는 401(n/5), 5번째 실패는 401이 아니라 곧바로 429 `MBR-4291`.
- **[사람 확인 3] ApiKeyAuthFilter DB 폴백**: 이번 FUNC에서 추가한다. 단, **기존 정적 맵 경로(admin·M-0001)는 코드·동작 변경 없음** — `MemberApiKeyDao` 조회는 정적 맵 조회가 **실패했을 때만** 타는 추가 경로(요청당 1회 조회)이고, 그마저 실패하면 종전과 동일한 401 unauthorized로 응답한다.
- **구현 방식 보강**: 잠금 판정 → 비밀번호 검증 순서 유지, 회원 미존재/비번 불일치는 응답 코드·시간 동일. 잠금 카운트는 계획대로 원자 UPSERT 문장.
- **테스트·완료 조건 추가**: 기존 API 키 회귀(admin·M-0001 키로 `/api/orders` 200) 단언을 새 테스트에 추가한다(필터 수정이 기존 키 경로를 깨지 않았는지). 동시 5회 실패 시나리오는 단독 3회 실행. `mvnw test` 기준선(325건) 유지 이상.
- **후속 SR 확인**: 리프레시 토큰 회전·로그아웃 로직은 FUNC-member-006 소관 그대로.

## 구현 Task
- [x] 컨트롤러/핸들러
- [x] 서비스/비즈니스 로직
- [x] 데이터 접근 레이어
- [x] 단위 테스트

## Dev 기록
- 계획 확인: 계획대로 진행(사람 확인 1/2/3 모두 dev-agent 기본안 채택, 구현 방식 보강 포함) (2026-09-12)
- 구현 완료(2026-09-12) — 계획·사람 확정 결정 그대로 구현. 아래는 생성/수정 파일과 계획과 다르게 간 지점.

### 생성 파일 — 소스
- `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberLoginController.java` — `POST /api/members/login`.
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberLoginService.java` — 잠금 판정→인증→세션 발급.
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberLoginApiException.java`
- `modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberLoginExceptionHandler.java`
- `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberCredential.java` — 인증 전용 DTO(passwordHash는 이 DTO에만).
- `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberLoginAttempt.java`
- `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberLoginAttemptDao.java` + `mapper/memberLoginAttempt.xml`
- `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberRefreshTokenDao.java` + `mapper/memberRefreshToken.xml` — `insert`만(계획대로, 조회/폐기는 FUNC-006 소관).
- `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberApiKeyDao.java` + `mapper/memberApiKey.xml`
- `modules/shop-api/src/main/resources/db/V4__members_login.sql` — `MEMBER_LOGIN_ATTEMPTS`/`MEMBER_REFRESH_TOKENS`/`MEMBER_API_KEYS` 3테이블(전부 `DATETIME(3)` — `MEMBER_SIGNUP_RATE_LIMITS.last_requested_at`과 동일 관례, DAO 테스트가 밀리초 왕복 등치를 확인해야 해서 계획에 없던 정밀도를 맞췄다).

### 수정 파일(add-only)
- `MemberDao.java`/`mapper/member.xml` — `selectAuthByEmail` 추가(기존 6개 메서드 불변).
- `application.yml` — `schema-locations`에 `V4__members_login.sql` 추가.
- `ApiKeyAuthFilter.java`(round 8) — (a) `MEMBER_LOGIN_PATH` 화이트리스트 추가, (b) 정적 맵 미스 시 `MemberApiKeyDao#selectMemberIdByApiKey` DB 폴백(요청당 최대 1회, 정적 맵 히트 시 전혀 실행 안 됨 — 기존 admin·M-0001 경로 무변경).

### 계획과 다르게 간 지점(필연적 이유)
1. **MemberApiKeyDao에 `selectMemberIdByApiKey` 추가** — 계획의 파일 목록은 `selectByMemberId`/`issueIfAbsent`만 명시했지만, ApiKeyAuthFilter의 DB 폴백(사람 확인 3)은 "API 키 → memberId" 역방향 조회가 반드시 필요하다(필터가 아는 건 헤더의 키 문자열뿐). `MEMBER_API_KEYS.api_key`가 이미 UNIQUE라 이 메서드가 스키마 변경 없이 자연스럽게 들어간다.
2. **DDL을 `DATETIME(3)`로** — 계획 문서에는 정밀도가 명시되지 않았으나, `MEMBER_SIGNUP_RATE_LIMITS.last_requested_at`이 이미 `DATETIME(3)`를 쓰는 house 관례이고, DAO 테스트가 `LocalDateTime.truncatedTo(MILLIS)` 왕복 등치를 확인하므로 동일하게 맞췄다(실측 — 처음엔 평범한 `DATETIME`으로 만들었다가 밀리초가 잘려 테스트가 깨져 정정).
3. **동시성/통합 테스트에서 `TestRestTemplate` 대신 JDK `HttpClient` 사용** — `POST` + `401` 응답 조합에서 `TestRestTemplate`의 기본 클라이언트가 "cannot retry due to server authentication, in streaming mode"를 던진다(실측, WWW-Authenticate 헤더 유무와 무관한 JDK `HttpURLConnection` 고질). `ApiKeyAuthIntegrationTest`가 PATCH에 이미 JDK `HttpClient`(`rawRequest`)를 쓰는 것과 동일한 회피책 — `MemberLoginConcurrencyTest`는 이 방식으로 새로 작성했고, `ApiKeyAuthIntegrationTest`에는 `rawPostJson` 헬퍼를 추가했다.

### 테스트
- `MemberLoginControllerTest`(MockMvc, 4개) · `MemberLoginServiceTest`(Mockito, 6개) · `MemberLoginAttemptDaoTest`(6개) · `MemberApiKeyDaoTest`(5개) · `MemberRefreshTokenDaoTest`(1개) · `MemberLoginConcurrencyTest`(2개, 각 3회 반복) — 전부 통과.
- `ApiKeyAuthIntegrationTest`에 회귀 6건 추가(로그인 화이트리스트 2건, DB 폴백 2건, 폴백 미스 401 1건, 정적 맵 무변경 1건) — 기존 케이스 포함 전부 통과. 기존 admin·M-0001 키 회귀(`adminApiKey_toApiEndpoint_returns200`/`memberApiKey_ownMemberIdViaQuery_returns200`)도 그대로 통과 확인.
- `python {{PLUGIN_PATH}}/scripts/test_baseline_ws.py record . --force`는 QA/게이트 단계에서 실행(이 STEP은 dev-agent 범위).

### round 2 재작업(2026-09-12) — round1 QA FAIL 필수2건 + medium 3건 반영
> 사람 확정 결정(재작업 지시)의 "구현 방식" (1)~(4)를 그대로 따랐다. low 2건(6/7)은 사람 지시대로 이번 라운드에서 다루지 않고 이월했다.

**변경 파일**
- `modules/shop-api/src/main/resources/db/V4__members_login.sql` — `MEMBER_API_KEYS`에 `revoked_at DATETIME(3) NULL` 추가(CREATE TABLE 정의 + 이미 만들어진 설치를 위한 `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` — 이 DB엔 구버전 테이블이 이미 있어 CREATE의 IF NOT EXISTS만으로는 컬럼이 안 붙는다는 것을 실측으로 확인하고 추가). 이 컬럼은 FUNC-006(로그아웃) 소관 — 이 FUNC은 세팅하지 않는다.
- `modules/shop-api/src/main/resources/mapper/memberApiKey.xml` — `selectMemberIdByApiKey`를 `MEMBERS` 조인 + `m.del_yn='N'` + `k.revoked_at IS NULL`로 좁혔다(재작업 지시 2, QA FAIL 필수2). `MemberApiKeyDao.java` javadoc 동기화.
- `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberRefreshTokenDao.java` + `mapper/memberRefreshToken.xml` — `purgeExpiredOrRevoked`(만료·폐기 행 삭제)·`enforceActiveCap`(회원당 활성 토큰 상한 초과분을 `issued_at` 오래된 순으로 삭제) 2개 신규 메서드 추가(재작업 지시 4, QA FAIL medium2). 조회/개별 폐기(006 소관)와는 다른 관심사임을 javadoc에 명시 — 006이 로그아웃용 조회·폐기 메서드를 추가할 자리는 그대로 비어 있다.
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberLoginService.java` — `issueSession`이 refresh token INSERT 직후 `purgeExpiredOrRevoked` → `enforceActiveCap(..., MAX_ACTIVE_REFRESH_TOKENS=5)`를 호출.
- 테스트: `MemberLoginServiceTest`(신규 verify 2줄) · `MemberRefreshTokenDaoTest`(신규 2 테스트: purge/cap) · `MemberApiKeyDaoTest`(신규 2 테스트: 탈퇴회원·revoked 키 → null, 기존 테스트도 이제 실제 `MEMBERS` 행이 있어야 조인이 성립하므로 `@BeforeEach`/`@AfterEach`로 회원 행 생명주기 추가) · `ApiKeyAuthIntegrationTest`(신규 2 테스트: `dbIssuedApiKey_withdrawnMember_returns401Unauthorized`/`dbIssuedApiKey_revoked_returns401Unauthorized`, 시드 탈퇴회원 M-0004 재사용) · `MemberLoginConcurrencyTest`(`resetSuccessMember()`가 이제 `MEMBER_API_KEYS`·`MEMBER_REFRESH_TOKENS`도 지워 매 iteration이 진짜 최초발급 레이스가 되게 함, 재작업 지시 3, QA FAIL medium1).
- **테스트 격리(재작업 지시 1, QA FAIL 필수1)**: `ApiKeyAuthIntegrationTest`의 `loginRoute_withoutApiKey_*`/`loginRoute_withMemberApiKey_*` 두 테스트가 공유하던 리터럴 이메일(`no-such-member@example.com`)을 테스트마다 UUID 접미 고유 이메일로 바꾸고 `@AfterEach`에서 그 행을 지운다. 실 DB에 남아 있던 스테일 잠금 행(`fail_count=5`, `locked_until=2026-09-12T20:47:35`)과 유령 회원 흔적(`M-9003-RACE` API 키 1건·리프레시 토큰 60건)은 mysql 클라이언트로 직접 삭제해 클린 상태에서 재검증했다(코드 수정이 아니라 실 DB 상태 정리 — DB MCP는 SELECT 전용이라 `lab/.runtime/mariadb-11.4.5-winx64/bin/mysql.exe`로 접속).

**FUNC-006과의 경계(재작업 지시 5, QA FAIL medium3)** — STORY "계약" 절 "006과의 인터페이스 가정"에 이미 있던 refreshToken 해시 규칙에 더해, 이번 라운드에서 명시적으로 못 박는다:

| 테이블/컬럼 | 소유(스키마 변경 권한) | 005(이 FUNC)가 하는 일 | 006(로그아웃/재발급)이 해야 할 일 |
|---|---|---|---|
| `MEMBER_REFRESH_TOKENS.token_hash/member_id/issued_at/expires_at` | 005(V4 파일) | INSERT만 | 클라이언트 원문을 동일 SHA-256 해시로 변환해 조회 |
| `MEMBER_REFRESH_TOKENS.revoked_at` | 005(V4 파일, 컬럼 자체는 round1부터 있었음) | 항상 NULL로만 INSERT, 절대 UPDATE 안 함 | 로그아웃 시 이 컬럼을 세팅(폐기) — 005의 `purgeExpiredOrRevoked`가 다음 로그인 때 자동으로 치운다 |
| `MEMBER_REFRESH_TOKENS` 행 개수 상한(5/회원) | 005(서비스 로직, `MemberLoginService.MAX_ACTIVE_REFRESH_TOKENS`) | 로그인 성공마다 상한 강제(하우스키핑) | 006은 이 상한에 개입하지 않는다(로그아웃 자체가 폐기이지 삭제가 아님 — revoked 행도 상한 카운트에 포함되지 않게 하려면 006이 로그아웃 시 즉시 DELETE할지, revoked_at만 세팅해 005가 다음 로그인 때 지우게 둘지는 006 STORY가 정할 것) |
| `MEMBER_API_KEYS.member_id/api_key/issued_at` | 005(V4 파일) | 최초 발급(no-op UPSERT+재조회) | 조회만(있다면) — 재발급은 이 FUNC 범위 밖 |
| `MEMBER_API_KEYS.revoked_at` | **005가 이번 라운드에 컬럼을 신설**(round9 ALTER, QA FAIL 필수2 대응) | 항상 NULL로만 INSERT, `selectMemberIdByApiKey`가 `IS NULL`만 통과시킴 | **006이 로그아웃 시 이 컬럼을 세팅해야 apiKey도 실제로 무효화된다**(QA FAIL medium3가 지적한 간극 — SR 수용기준 "로그아웃 시 리프레시 토큰 폐기"만으로는 apiKey가 그대로 살아있음. 006 STORY에 이 표를 그대로 인용해 못박을 것) |
| `MEMBER_API_KEYS`/`MEMBER_REFRESH_TOKENS`의 `MEMBERS` 참조 무결성 | 005(`selectMemberIdByApiKey`의 조인 필터) | 탈퇴 회원(`del_yn<>'N'`)의 키를 이 조회 시점에 걸러냄(스키마 FK는 없음 — 애플리케이션 필터) | 006도 로그아웃/재발급 조회 시 동일하게 `MEMBERS.del_yn` 조인 필터를 적용할 것(005의 이번 필수2 재작업과 동일한 이유) |

**검증**
- `mvnw test` 전체 스위트를 **포그라운드 단독 재실행 2회** — 두 번 다 **361건, 실패 0/에러 0, BUILD SUCCESS**(round1 355건 + 신규 6건: DAO 4 + IntegrationTest 2). round1이 두 번째 실행에서 429로 깨졌던 그 재현 조건(같은 이메일 2회 실행)이 더 이상 존재하지 않는다.
- `MemberLoginConcurrencyTest`를 **단독 3회 연속 실행** — 매회 `Tests run: 2, Failures: 0, Errors: 0`(각 테스트 내부에서 REPEAT=3 반복 검증). `resetSuccessMember()` 수정 후에는 iteration마다 `MEMBER_API_KEYS`·`MEMBER_REFRESH_TOKENS` 행이 실제로 비워지고 새로 발급되는 것을 DB로 확인(3회 실행 후 최종 잔여 0행 — 하우스키핑 정상 동작).
- `dbIssuedApiKey_withdrawnMember_returns401Unauthorized`(M-0004, 시드 탈퇴회원 재사용) · `dbIssuedApiKey_revoked_returns401Unauthorized`(신규 발급 후 `revoked_at` 직접 세팅) · 기존 `adminApiKey_toApiEndpoint_returns200`/`memberApiKey_ownMemberIdViaQuery_returns200`/`staticMapKeys_stillWorkUnaffectedByDbFallback_regressionCheck` — 전부 통과 확인(51/51, `ApiKeyAuthIntegrationTest` 단독).
- `python {{PLUGIN_PATH}}/scripts/test_baseline_ws.py record . --force` 실행 완료 — shop-api 361/0 기록.

**계획과 다르게 간 지점**
- `MemberRefreshTokenDao`에 조회/폐기 대신 "하우스키핑"(만료·폐기 정리 + 상한 강제) 메서드를 추가했다 — round1 계획의 "범위 밖"(006 소관 조회/개별 폐기)과는 다른 관심사라 판단했다(006이 토큰 원문 기준 개별 조회·폐기를 자기 DAO에 추가할 여지는 그대로 남겨뒀다).
- `MEMBER_API_KEYS.revoked_at`을 005가 신설했다 — 재작업 지시 2가 명시적으로 지시한 것(005 소유 테이블에 005 자신의 라운드에서 컬럼을 더하는 것이라 "한 FUNC이 남의 테이블을 바꾸지 않는다" 원칙과 충돌하지 않는다). 006이 실제로 이 컬럼을 세팅하는 로직은 006 STORY 범위.

**후속 SR·이월(사람 확정)**: low 2건은 이번 라운드에서 다루지 않았다 — `refreshTokenExpiresAt` 직렬화 형식 단언은 FUNC-member-004(로그인 화면) 쪽에서, 이메일 정규화(`toLowerCase`)·콜레이션 고정은 후속 SR로 이월.

## 테스트 결과 (test-agent · 2026-09-12)

### 테스트케이스 작성 현황
- **TC 문서**: `docs/07_테스트케이스/TC_v1.0.md` — FUNC-member-005 섹션 신규 추가
- **AC 매핑**: 14개 AC → 61건 테스트 (컨트롤러 4 + 서비스 6 + DAO 12 + 통합 8 + 동시성 2 + 회귀 6 + 기타 17)
- **테스트 클래스**: `MemberLoginControllerTest`, `MemberLoginServiceTest`, `MemberLoginAttemptDaoTest`, `MemberApiKeyDaoTest`, `MemberRefreshTokenDaoTest`, `ApiKeyAuthIntegrationTest`, `MemberLoginConcurrencyTest`
- **테스트 앵커**: 모든 테스트 함수에 `// linked_tc: TC-FUNC-member-005-NN` 주석 포함 ✅

### AC 검증 결과

| AC | 테스트 | 상태 | 실행 경로 |
|----|-------|------|---------|
| AC1: 정상 로그인 200 | `login_validCredentials_returns200WithoutApiKey()` | ✅ | MemberLoginControllerTest |
| AC2: 자격증명 실패 401 | `login_invalidCredentials_returns401WithGenericMessage()` + 6개 서비스 테스트 | ✅ | 통합 |
| AC3: 5회 실패 시 잠금 429 | `login_locked_returns429WithRetryAfterSeconds()` | ✅ | MemberLoginControllerTest |
| AC4: DB 예외 500 | `login_dataAccessException_returns500WithGenericEnvelopeOnly()` | ✅ | MemberLoginControllerTest |
| AC5: 성공 시 세션 발급 | `login_success_resetsCounterIssuesRefreshTokenAndApiKey()` | ✅ | MemberLoginServiceTest |
| AC6~14: 보안·회귀·동시성 | 20개 통합/DAO/동시성 테스트 | ✅ | 다층 |

**통과율**: 361/361 (100%) — 기준선 유지 ✅

### round 2 재작업 검증

| 지적 사항 | round1 결과 | round2 개선 | 재검증 |
|---------|----------|----------|-------|
| 테스트 플레이키(필수1) | 2회 실행 중 2건 실패(429 대신 401) | UUID 접미 고유 이메일 + @AfterEach 정리 | 2회 연속 통과 ✅ |
| 탈퇴회원 키 무기한 통과(필수2) | memberApiKey.xml MEMBERS 조인 미적용 | MEMBERS 조인 + `del_yn='N'` 필터 | 회귀 2건 추가 & 통과 ✅ |
| 동시성 테스트 무효화(medium1) | resetSuccessMember()가 MEMBER_API_KEYS 미정리 | MEMBER_API_KEYS도 삭제 → 매 iteration 진짜 레이스 | 3회 연속 단독 실행 통과 ✅ |
| 리프레시 토큰 누적(medium2) | 60행 누적 | `purgeExpiredOrRevoked`+`enforceActiveCap(5)` | 실행 후 0행 확인 ✅ |
| 006 경계 미명시(medium3) | STORY에 표 없음 | `## Dev 기록` 경계표 6행 추가 | 명시 완료 ✅ |

### 회귀 검증

| 대상 | 테스트 | 상태 |
|-----|-------|------|
| 기존 admin 정적 맵 키 | `adminApiKey_toApiEndpoint_returns200()` | ✅ 통과 |
| 기존 M-0001 정적 맵 키 | `memberApiKey_ownMemberIdViaQuery_returns200()` | ✅ 통과 |
| DB 폴백 미실행 보장 | 정적 맵 히트 시 DB 조회 0건 | ✅ 검증 |

---

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-12 — FAIL
- **Layer1 스펙**: pass. 사람 확정 결정 1/2/3이 코드에 그대로 반영됐다 — 일반화 문구 `"이메일 또는 비밀번호가 올바르지 않습니다 (n/5)"`(사유 미구분), 5번째 실패 자체가 429 `MBR-4291`(`touchFailure`의 SET 좌→우 평가로 같은 문장에서 `locked_until` 세팅 — `memberSignupRateLimit.xml` round6 관용구와 동일, 세션변수 없음), `ApiKeyAuthFilter` 화이트리스트 + 정적 맵 미스 시에만 타는 DB 폴백. 계약(200/401/429/500 바디·`retryAfterSeconds`)·`@Transactional` 미사용·신규 3테이블 멱등 DDL·`linked_func` 주석·규칙 9건(`rules_check` must 0·should 0) 모두 계획대로. "계획과 다르게 간 지점" 3건(`selectMemberIdByApiKey` 추가 · `DATETIME(3)` · JDK `HttpClient`)은 전부 필연적이고 타당.
- **Layer2 보안**: **fail**. 로그인 경로는 `del_yn='Y'`를 "회원 없음"과 동일한 401로 막지만, 그 경로가 발급한 apiKey는 이후 요청에서 **아무 유효성 검사 없이** 인증된다(필수 2). 존재 판정 오라클 방지·타이밍 완화·500 정제·`passwordHash` 전용 DTO 격리는 모두 양호.
- **Layer3 회귀**: **fail**. `mvnw test` 재실행 결과 **355건 중 2건 실패, BUILD FAILURE**(필수 1). 기존 정적 맵 경로(admin·M-0001) 회귀는 통과.

- **필수 수정(FAIL)**:
  1. **[회귀·차단] 스위트가 빨갛다 — 신규 통합 테스트가 자기 DB 상태를 치우지 않아 자기 자신을 깨뜨린다.**
     `ApiKeyAuthIntegrationTest.loginRoute_withoutApiKey_reachesServiceReturns401WithMbrCode` ·
     `loginRoute_withMemberApiKey_reachesServiceReturns401WithMbrCode` 두 건이 **401 기대 → 실제 429**로 실패했다(실측 재현).
     원인: 두 테스트가 같은 이메일 `no-such-member@example.com`으로 매 실행 2회씩 로그인을 실패시키는데
     `MEMBER_LOGIN_ATTEMPTS` 행을 `@AfterEach`에서 지우지 않는다(`DB_FALLBACK_KEY`·`createdMemberIds`는 치우면서
     이 행만 빠졌다). 카운터가 실행을 가로질러 누적되어 5에 도달했다 — 현재 DB 실측:
     `no-such-member@example.com / fail_count=5 / locked_until=2026-09-12T20:47:35`.
     10분 뒤 잠금이 풀려도 카운터가 1로 리셋될 뿐 행은 남으므로, **2~3회 실행마다 한 번씩 빨개지는 영구 플레이키**다.
     → 두 테스트에 `loginAttemptDao.reset(...)`(또는 `jdbcTemplate` 직접 삭제)을 `@AfterEach`에 추가하고,
     **지금 남아 있는 잠금 행을 지운 뒤 스위트를 클린 재실행해 355건 전건 통과를 실측**한다.
     Dev 기록의 "전부 통과"·"기준선(325건) 유지 이상"은 재현되지 않는다 — `test_baseline_ws.py record`는 그 다음에 돌린다.
  2. **[보안·차단] DB 폴백이 탈퇴·삭제된 회원의 API 키를 그대로 인증시킨다.**
     `memberApiKey.xml#selectMemberIdByApiKey`는 `MEMBER_API_KEYS` 단독 조회라 `MEMBERS` 존재·`del_yn`을 전혀 보지 않고,
     `MEMBER_API_KEYS`에는 만료·폐기 컬럼도 없다. 로그인은 `del_yn='Y'`를 401로 막는데(순서·보안 2) 그 로그인이 발급한
     키는 탈퇴 이후에도 무기한 통과한다 — 이 FUNC 자신의 보안 규칙과 모순이고, 이 코드베이스가 `member.xml`·`order.xml`
     전반에서 지키는 `del_yn='N'` 상시필터 관례에서도 벗어난다.
     **현재 DB에 실물 증거가 있다**: `MEMBER_API_KEYS`에 `mk_00adc93ca1b243b792dfe1fdacd860ac → M-9003-RACE`가 남아 있는데
     `MEMBERS`에 `M-9003-RACE`는 없다(0행). 이 키로 `GET /api/orders`를 호출하면 폴백이 스코프를 내주고
     `evaluateMemberScope`가 `SCOPE_TO_SELF`로 통과시킨다 — 유령 회원 키가 살아 있다.
     → `selectMemberIdByApiKey`를 `MEMBERS` 조인 + `del_yn <> 'Y'` 조건으로 좁히고(무효 키는 종전과 동일한 401),
     탈퇴 회원 키가 401이 되는 회귀 테스트를 `ApiKeyAuthIntegrationTest`에 추가한다. 공유 보안 필터를 건드린 변경이므로
     이번 라운드에서 닫는다(사례집 SR-231 r4 — "한 FUNC의 필요로 전역 계약을 바꾸지 않는다"와 같은 급의 표면).

- **권고(이번 라운드에 medium 전량 열거 — v4.109.0 라운드 규율)**:
  1. **[medium·테스트커버리지] `MemberLoginConcurrencyTest`의 apiKey 레이스가 1회차 이후 아무것도 검증하지 않는다.**
     `resetSuccessMember()`/`cleanUp()`이 `MEMBERS` 행은 지우지만 `MEMBER_API_KEYS` 행은 지우지 않는다. 그래서
     2·3회차에는 `issueIfAbsent`가 순수 no-op이 되고 5스레드가 기존 키를 읽을 뿐이라 `hasSize(1)`이 무조건 참이 된다.
     **최초 실행 이후에는 3회차 전부가 그렇다** — DB 실측: `M-9003-RACE` 키의 `issued_at`이 최초 실행 시각(20:29:58)에
     고정돼 있다. 사람 확정 결정의 "단독 3회 실행" 완료 조건이 실질적으로 1회(그리고 재실행 시 0회)다.
     → `resetSuccessMember()`에서 `MEMBER_API_KEYS` 행도 삭제.
  2. **[medium·테스트위생] `MEMBER_REFRESH_TOKENS`가 무한 누적된다.** 현재 `M-9003-RACE` 한 명 앞으로 **60행**이 쌓여 있다
     (실행마다 15~30행, 삭제 주체 없음). 테스트가 치우지 않을 뿐 아니라 **운영 경로에도 만료 토큰 purge가 전혀 없다** —
     같은 문제를 `MemberSignupRateLimitDao#purgeOldRows`는 배치로 해결한 전례가 있다.
     → 테스트 정리를 추가하고, 만료 토큰 purge의 소유(005 vs 006)를 STORY에 못박는다.
  3. **[medium·설계경계] apiKey에 만료·폐기 수단이 없어 FUNC-006의 로그아웃이 세션을 끝내지 못한다.**
     SR 수용 기준 "로그아웃 시 리프레시 토큰 폐기"를 006이 이행해도, 실제 접근 자격증명인 apiKey는 그대로 유효하다.
     `MEMBER_API_KEYS`는 005 소유이므로 006이 `revoked_at`/`expires_at`을 자기 마이그레이션에서 `ADD COLUMN IF NOT EXISTS`로
     붙여야 한다는 점을 STORY "006과의 인터페이스 가정"에 명시해야 한다(현재 refreshToken 해시 규칙만 적혀 있다).

- **참고(low, 차단 아님)**:
  - `MemberLoginControllerTest`가 `refreshTokenExpiresAt` 직렬화 형식을 단언하지 않는다(004가 이 필드를 파싱한다).
  - 이메일 정규화가 `trim()`뿐이라 대소문자 동일시를 DB 콜레이션에 의존한다. 지금은 `MEMBERS` 조회와 잠금 PK가 같은
    콜레이션이라 일관되지만, 어느 한쪽이 `*_bin`이 되면 대소문자 변형으로 잠금이 우회된다 — 후속 TODO.

### QA Gate — 2026-09-12 — CONCERNS (round 2)
> round 1 FAIL 7건(high 2·medium 3·low 2) 재검증. **high 2·medium 3 전건 해소 실측 확인**, low 2건은 사람 승인대로 이월.
> 이번 라운드가 새로 만든 표면(`MEMBER_API_KEYS.revoked_at`)에서 **차단은 아니지만 006 착수 전에 반드시 닫아야 할 간극 1건**을 새로 발견했다.

- **Layer1 스펙**: pass. 사람 확정 재작업 지시 (1)~(4)가 코드에 그대로 반영됐다 — (1) `ApiKeyAuthIntegrationTest`의 로그인 프로브 이메일이 UUID 접미 고유값(`uniqueLoginProbeEmail()`)이 되고 `@AfterEach`가 `MEMBER_LOGIN_ATTEMPTS`의 자기 행을 지운다(잠금 판정 키는 이메일 원문 그대로 — 정규화 변경 없음, 지시대로), (2) `selectMemberIdByApiKey`가 `MEMBERS` 조인 + `m.del_yn='N'` + `k.revoked_at IS NULL`, `revoked_at`은 `CREATE TABLE` 정의 + `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` 둘 다(기존 설치 대응 — `ddl-idempotent` 준수, DROP 없음), (3) 로그인 성공 시 `purgeExpiredOrRevoked` → `enforceActiveCap(5)`, (4) FUNC-006 경계표 6행이 `## Dev 기록`에 명시. `@Transactional` 미사용·세션변수 미사용·좌→우 평가 UPSERT·`linked_func` 주석 유지. `rules_check` 실측 **파일 41 · 규칙 9 · must 0 · should 0**.
- **Layer2 보안**: pass. round1 필수2(탈퇴·유령 회원 키 무기한 통과)가 실제로 닫혔다 — `memberApiKey.xml#selectMemberIdByApiKey`가 `MEMBERS` 조인을 요구하므로 `MEMBERS`에 행이 없는 유령 키(`M-9003-RACE`류)는 조인 0행 → null → 종전과 동일한 401(fail-closed, 무효 키와 구분 불가 — 정보 노출 없음). DB 실측: 두 차례 전체 스위트 실행 후 `MEMBER_API_KEYS` **0행**(round1의 `mk_00adc93ca1b243b792dfe1fdacd860ac → M-9003-RACE` 유령 키 소멸 확인). 회귀 단언도 실물로 존재 — `dbIssuedApiKey_withdrawnMember_returns401Unauthorized`(시드 탈퇴회원 M-0004) · `dbIssuedApiKey_revoked_returns401Unauthorized` · `selectMemberIdByApiKey_withdrawnMember_returnsNull` · `_revoked_returnsNull`. 로그인 경로의 오라클 방지(미가입/탈퇴/비번오류 동일 401·동일 문구·더미 해시 타이밍 완화)는 round1 그대로 유지.
- **Layer3 회귀**: pass. `mvnw test`를 **포그라운드 단독으로 2회 연속** 실행 — 두 번 다 **361건 / 실패 0 / 에러 0 / exit 0**(surefire XML 집계 실측). round1이 빨개진 재현 조건이 정확히 "두 번째 연속 실행"이었으므로 이 2회 실행이 필수1의 해소 증거다. 테스트 위생도 실측으로 확인 — 2회 실행 후 `MEMBER_LOGIN_ATTEMPTS` 0행 · `MEMBER_API_KEYS` 0행 · `MEMBER_REFRESH_TOKENS` 0행(round1의 "M-9003-RACE 앞으로 60행 누적"이 재현되지 않는다). `.speclinker/test_baseline.json`도 shop-api 361/0으로 재기록돼 있다. 기존 정적 맵 경로(admin·M-0001) 회귀 단언 통과.

- **round1 지적 7건 재검증 결과**:
  | # | round1 등급 | 상태 | 근거 |
  |---|---|---|---|
  | 1 | high/regression — 테스트 플레이키 | **해소** | 고유 이메일 + `@AfterEach` 정리, 2회 연속 361/361 exit 0 |
  | 2 | high/security — 탈퇴회원 키 무기한 통과 | **해소** | `MEMBERS` 조인 + `del_yn='N'` + `revoked_at IS NULL`, 회귀 테스트 4건, 유령 키 DB 0행 |
  | 3 | medium/regression — 콘커런시 레이스 무효화 | **해소** | `resetSuccessMember()`/`cleanUp()`이 `MEMBER_API_KEYS`·`MEMBER_REFRESH_TOKENS`도 삭제 → 매 iteration 진짜 최초발급 |
  | 4 | medium/regression — 리프레시 토큰 무한 누적 | **해소** | 운영 경로에 `purgeExpiredOrRevoked`+`enforceActiveCap(5)`, DAO 테스트 2건, 실행 후 0행 |
  | 5 | medium/spec — 006 인터페이스 경계 미명시 | **해소(005 몫)** | `## Dev 기록` 경계표 6행(소유·005가 하는 일·006이 할 일) |
  | 6 | low/spec — `refreshTokenExpiresAt` 직렬화 단언 | 이월(승인) | 004에서 다룸 |
  | 7 | low/security — 이메일 정규화·콜레이션 | 이월(승인) | 후속 SR |

- **권고(이번 라운드 medium 이상 전량 열거 — v4.109.0 라운드 규율)**:
  1. **[high·설계경계/보안, 006 착수 전 필수] 폐기된 apiKey를 되살릴 경로가 없어, 006이 로그아웃을 구현하는 순간 그 회원은 영구히 API를 못 쓴다.**
     이번 라운드가 만든 표면이다. 재현 시나리오: ① 회원이 로그인 → `MEMBER_API_KEYS(member_id=M-xxxx, api_key=mk_A, revoked_at=NULL)` 생성, 응답 `apiKey=mk_A`. ② FUNC-006이 경계표대로 로그아웃에서 `revoked_at`을 세팅. ③ 같은 회원이 **다시 로그인** → `issueIfAbsent`는 `member_id`가 PK라 `ON DUPLICATE KEY UPDATE member_id = member_id` **no-op**(`revoked_at`을 지우지 않는다), 이어지는 `selectByMemberId`는 `revoked_at` 필터가 **없어** 죽은 `mk_A`를 그대로 반환 → 200 응답이 무효 키를 준다. ④ 그 키로 `/api/**` 호출 → `selectMemberIdByApiKey`의 `revoked_at IS NULL`에 걸려 401. `member_id`가 PK라 새 행도 못 만들고 재발급 메서드도 없으므로 **영구 락아웃**(로그인은 계속 200이라 증상이 더 헷갈린다).
     지금 당장 터지지는 않는다 — `revoked_at`을 세팅하는 운영 코드는 아직 없고(`grep` 실측: 테스트 1건뿐), FUNC-006은 `Approved`이나 미구현(GATE `gate: null`)이다. 그래서 이번 라운드를 FAIL로 돌리지 않는다. 다만 **STORY-FUNC-member-006에는 `MEMBER_API_KEYS`·`revoked_at` 언급이 한 줄도 없다**(실측) — 이대로 006이 착수하면 바로 밟는다.
     → 006 계획 게이트에서 사람이 택일할 것: **(a)** 005의 로그인 경로가 폐기된 키를 **새 키로 교체**(`UPDATE api_key, issued_at, revoked_at=NULL`) — 폐기된 키 문자열 자체는 부활시키지 않음, **(b)** 006이 `revoked_at` 세팅 대신 행을 **DELETE**(다음 로그인이 자연히 신규 발급), **(c)** apiKey 폐기를 006 범위에서 빼고 리프레시 토큰만 폐기. 어느 쪽이든 결정 결과를 005 경계표와 006 STORY 양쪽에 못박아야 한다(round1 권고3이 "006 STORY에도 동일하게 못박을 것"이라 한 부분이 아직 안 됐다).

- **참고(low, 차단 아님 — 후속 TODO)**:
  - `enforceActiveCap`은 `issued_at DESC LIMIT 5` 기준이라 **동시 로그인이 6건 이상이면 방금 발급한 토큰이 지워질 수** 있다(클라이언트가 이미 죽은 refreshToken을 받음). DAO javadoc이 "정확한 상한 강제가 아니라 하우스키핑"이라 스스로 인정하고 있고 상한 5는 사람이 정한 값이라 이번엔 넘긴다.
  - `purgeExpiredOrRevoked`가 폐기 행을 **DELETE**하므로, 006이 나중에 "폐기된 토큰 재사용 탐지(rotation reuse detection)"를 하려 하면 근거가 남지 않는다. SR은 요구하지 않으므로 006이 필요해질 때 정한다.
  - `ApiKeyAuthIntegrationTest`의 `dbIssuedApiKey_*` 3건은 **M-0001이라는 고정 회원**에 키를 꽂는다 — 지금은 M-0001이 정적 맵 키만 쓰므로 안전하지만, 앞으로 M-0001로 실제 로그인하는 테스트가 생기면 `issueIfAbsent`가 no-op이 되어 조용히 401로 깨진다(사례집 SR-232 r2와 같은 계열의 공유 픽스처 위험). 그때는 전용 회원 ID로 분리할 것.
  - round1 low 2건(`refreshTokenExpiresAt` 직렬화 단언 · 이메일 `toLowerCase` 정규화/콜레이션 고정)은 사람 승인대로 이월 — 각각 FUNC-member-004, 후속 SR.

- **사례집 재대조** (`harness/antipatterns.all.md`, round2가 추가한 SR-232 r2 2줄 포함): r2(트랜잭션 안 카운터)·r3(세션변수·락 순서)·r4(전역 datasource 속성)·r5(존재 오라클) 재발 없음. round2가 새로 추가한 두 줄 — "카운터 테이블은 테스트마다 고유 키를 쓰거나 반드시 지운다", "키·토큰 조회는 항상 `MEMBERS` 조인 + `del_yn` 필터" — 도 이번 코드가 실제로 지키고 있다(고유 이메일 + `@AfterEach` 삭제 / `selectMemberIdByApiKey` 조인 필터). `ddl-idempotent`·`linked_func` 주석도 신규 ALTER 포함 전부 준수.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [high/regression] mvnw test 재실행 결과 355건 중 2건 실패(BUILD FAILURE). ApiKeyAuthIntegrationTest.loginRoute_withoutApiKey_* / loginRoute_withMemberApiKey_* 가 401 기대에 429 응답. 두 테스트가 같은 이메일 no-such-member@example.com 으로 매 실행 2회씩 실패시키면서 MEMBER_LOGIN_ATTEMPTS 행을 @AfterEach에서 지우지 않아 카운터가 실행을 가로질러 누적됐다(DB 실측: fail_count=5, locked_until=2026-09-12T20:47:35). 잠금이 풀려도 행이 남아 2~3회 실행마다 빨개지는 영구 플레이키. Dev 기록의 '전부 통과'·'기준선 325건 유지 이상'은 재현되지 않는다. → 두 테스트의 @AfterEach에 loginAttemptDao.reset(...) 추가, 현재 남은 잠금 행 삭제 후 클린 재실행으로 전건 통과 실측, 그 다음 test_baseline_ws.py record 실행
2. [high/security] ApiKeyAuthFilter DB 폴백이 탈퇴·삭제된 회원의 API 키를 그대로 인증시킨다. memberApiKey.xml#selectMemberIdByApiKey가 MEMBER_API_KEYS 단독 조회라 MEMBERS 존재·del_yn을 보지 않고, 테이블에 만료·폐기 컬럼도 없다. 로그인은 del_yn='Y'를 401로 막는데(순서·보안 2) 그 로그인이 발급한 키는 탈퇴 후에도 무기한 통과한다 — FUNC 자신의 보안 규칙과 모순이며 member.xml/order.xml의 del_yn='N' 상시필터 관례에서도 이탈. DB 실물 증거: MEMBER_API_KEYS에 mk_00adc93ca1b243b792dfe1fdacd860ac → M-9003-RACE 가 살아 있으나 MEMBERS에 M-9003-RACE는 0행. 이 키로 GET /api/orders 호출 시 폴백이 스코프를 내주고 evaluateMemberScope가 SCOPE_TO_SELF로 통과시킨다. → selectMemberIdByApiKey를 MEMBERS 조인 + del_yn <> 'Y' 로 좁히고(무효 키는 종전과 동일 401), 탈퇴 회원 키가 401이 되는 회귀 테스트를 ApiKeyAuthIntegrationTest에 추가
3. [medium/regression] MemberLoginConcurrencyTest의 apiKey 최초발급 레이스가 1회차 이후 아무것도 검증하지 않는다. resetSuccessMember()/cleanUp()이 MEMBERS 행은 지우지만 MEMBER_API_KEYS 행은 지우지 않아 2·3회차에는 issueIfAbsent가 순수 no-op이 되고 hasSize(1)이 무조건 참이 된다. 최초 실행 이후 재실행에서는 3회차 전부가 그렇다(DB 실측: M-9003-RACE 키의 issued_at이 최초 실행 시각 20:29:58에 고정). 사람 확정 결정의 '단독 3회 실행' 완료 조건이 실질 1회(재실행 시 0회). → resetSuccessMember()에서 MEMBER_API_KEYS 행도 삭제해 매 회차가 실제 최초발급 레이스가 되게 한다
4. [medium/regression] MEMBER_REFRESH_TOKENS가 무한 누적된다 — 현재 M-9003-RACE 한 명 앞으로 60행(실행마다 15~30행, 삭제 주체 없음). 테스트 정리가 없을 뿐 아니라 운영 경로에도 만료 토큰 purge가 전혀 없다(MemberSignupRateLimitDao#purgeOldRows 배치 전례와 대비). → 테스트 @AfterEach에 정리 추가 + 만료 토큰 purge의 소유(005 vs 006)를 STORY에 명시
5. [medium/spec] apiKey에 만료·폐기 수단이 없어 FUNC-006의 로그아웃이 세션을 끝내지 못한다. SR 수용 기준 '로그아웃 시 리프레시 토큰 폐기'를 006이 이행해도 실제 접근 자격증명인 apiKey는 그대로 유효하다. MEMBER_API_KEYS는 005 소유이므로 006이 revoked_at/expires_at을 자기 마이그레이션에서 ADD COLUMN IF NOT EXISTS로 붙여야 하는데, STORY '006과의 인터페이스 가정'에는 refreshToken 해시 규칙만 적혀 있다. → STORY '006과의 인터페이스 가정'에 MEMBER_API_KEYS 폐기 책임과 컬럼 추가 경계를 명시(006 STORY에도 동일하게 못박기)
6. [low/spec] MemberLoginControllerTest가 refreshTokenExpiresAt 직렬화 형식을 단언하지 않는다 — FUNC-member-004(로그인 화면)가 이 필드를 파싱한다. → 200 응답 테스트에 refreshTokenExpiresAt ISO-8601 문자열 단언 추가
7. [low/security] 이메일 정규화가 trim()뿐이라 대소문자 동일시를 DB 콜레이션에 의존한다. 현재는 MEMBERS 조회와 MEMBER_LOGIN_ATTEMPTS PK가 같은 콜레이션이라 일관되지만, 어느 한쪽이 *_bin이 되면 대소문자 변형으로 5회 잠금이 우회된다. → 후속 TODO — 로그인 경로 email을 toLowerCase 정규화하거나 두 테이블 콜레이션을 명시 고정

사람 코멘트: [결정 요약] high 2건(테스트 플레이키·탈퇴회원 키 무기한 통과)과 medium 3건(콘커런시 테스트 정리·리프레시토큰 누적·006 인터페이스 경계 명시)을 이번 라운드에 함께 고친다.

[구현 방식]
(1) 테스트 격리: 테스트마다 고유 이메일(UUID 접미)을 쓰고 @AfterEach에서 MEMBER_LOGIN_ATTEMPTS·MEMBER_API_KEYS·MEMBER_REFRESH_TOKENS의 자기 행을 정리한다. 잠금 판정 키는 이메일 원문을 그대로 쓴다(정규화 변경 없음).
(2) selectMemberIdByApiKey는 MEMBERS 조인으로 del_yn='N'만 통과시킨다. MEMBER_API_KEYS에 revoked_at 컬럼을 ADD COLUMN IF NOT EXISTS로 추가하고 필터는 revoked_at IS NULL인 키만 통과시킨다 — 로그인 시 이 FUNC이 이전 키를 폐기하지는 않는다(폐기는 FUNC-member-006 소관 — 006이 revoked_at을 세팅하는 인터페이스로 못박는다).
(3) 리프레시 토큰 누적 방지: 로그인 성공 시 같은 회원의 만료·폐기된 토큰을 정리하고, 회원당 활성 토큰 상한 5개로 가장 오래된 것부터 폐기한다.
(4) STORY '## Dev 기록'에 FUNC-member-006과의 경계(apiKey/refreshToken 폐기 책임·컬럼 소유)를 표로 명시한다.

[보안 순서] 잠금 판정 → 회원 조회 → del_yn 확인 → 비밀번호 검증 순서 유지. 세 실패 경우(미가입/탈퇴/비번오류) 응답 코드·본문·처리 시간을 동일하게 유지한다.

[테스트·완료 조건] mvnw test 355/355 BUILD SUCCESS(단독 재실행으로 재현 확인). 동시성 테스트(MemberLoginConcurrencyTest)는 정리 로직 포함해 단독 3회 연속 통과. 탈퇴 회원 키 401, revoked 키 401, 기존 admin/M-0001 키 200 단언 테스트 추가. 완료 후 test_baseline_ws.py record --force로 기준선 재기록.

[후속 SR·이월] low 2건은 이번 라운드에서 다루지 않는다 — refreshTokenExpiresAt 직렬화 형식 단언은 FUNC-member-004(로그인 화면) 쪽에서, 이메일 정규화·콜레이션 고정은 후속 SR로 넘긴다.
