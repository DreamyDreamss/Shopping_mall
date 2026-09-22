---
story-id: STORY-FUNC-member-006
func-id: FUNC-member-006
status: Done
domain: member
created: 2026-09-12
spec_markers: 0
sr-id: SR-232
approved_sha: 7dc3a0f2d39d
---

# STORY-FUNC-member-006 — SR-232 — 로그아웃·자동 로그인 토큰 API · 신규 INF-MBR-004 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

## Story
SR-232 — 로그아웃·자동 로그인 토큰 API · 신규 INF-MBR-004 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)


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
- [ ] INF-MBR-004: 요청/응답 계약 충족(비즈룰 스펙 미상 — 보강 필요)
- [ ] SR 정본 계약 충족 — `docs/변경관리/SR-232/02_변경명세.md` · inputs/_decisions.md의 D-결정 의 요구·계약 조항을 AC로 구체화해 사람 승인 전 보강할 것(OBS-020 관례)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-004
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 계획

### 005 인터페이스 경계표 (GATE-005 round2 사람 확정 — 재해석 금지)

| 항목 | 결정 | 이 FUNC에서의 구체화 |
|---|---|---|
| 폐기된 apiKey 재발급 | (a) 재로그인 시 새 키로 교체 | `MemberApiKeyDao#issueIfAbsent`(005 소유 파일, 시그니처 불변)의 SQL을 "행 없으면 INSERT / 있고 `revoked_at IS NOT NULL`이면 새 `api_key`+`issued_at`으로 UPDATE하고 `revoked_at=NULL`/ 있고 `revoked_at IS NULL`이면 완전 no-op"으로 확장한다. 폐기된 키 문자열 자체는 절대 재사용하지 않는다(새 후보 문자열로 교체). `linked_func: FUNC-member-006`으로 남긴다. |
| 006의 apiKey 폐기 범위 | 로그아웃 시 `MEMBER_API_KEYS.revoked_at` 세팅(DELETE 아님) | `MemberApiKeyDao#revokeByMemberId(memberId, now)` 신규 — `UPDATE ... SET revoked_at=#{now} WHERE member_id=#{memberId} AND revoked_at IS NULL`(이미 폐기됐으면 재실행해도 안전). |
| MEMBER_REFRESH_TOKENS 폐기 범위 | 로그아웃 = 해당 회원의 활성 리프레시 토큰 **전체** 폐기(개별 아님) | apiKey가 회원당 1개뿐인 기존 005 설계(디바이스별 세션 분리 없음)와 일관되게, 로그아웃은 "이 회원의 API 접근 자체를 끝낸다"는 의미로 전체 폐기한다. `MemberRefreshTokenDao#revokeAllForMember(memberId, now)` 신규 — `UPDATE ... SET revoked_at=#{now} WHERE member_id=#{memberId} AND revoked_at IS NULL`. |
| 재사용 탐지(rotation reuse detection) | 요구하지 않음(SR 미요구, 과설계 금지) | 폐기된 토큰 행은 되살리지 않고, 005의 기존 `purgeExpiredOrRevoked`/`enforceActiveCap`(변경 없음, 그대로 재사용)이 다음 로그인·리프레시 때 정리한다. 보존 기간·탐지 로직 신설 없음. |
| purge 정책 | 신설 없음 | refresh 성공 시에도 005의 두 하우스키핑 메서드를 그대로 호출한다(리프레시 경로에서 토큰이 갱신될 때마다 쌓이는 것을 방지) — 새 배치·새 컬럼 없음. |

### 파일

**신규 (shop-api, `com.sm.lab.shop.*`)**

| 파일 | 책임 |
|---|---|
| `controller/MemberSessionController.java` | `POST /api/members/sessions/logout`(인증 필요) · `POST /api/members/sessions/refresh`(무인증) 라우팅만. |
| `service/MemberSessionService.java` | 로그아웃(apiKey+전체 refreshToken 폐기, idempotent) · refresh(토큰 검증→회전→apiKey 재발급) 비즈니스 로직. `@Transactional` 미사용(005와 동일 house 결정 — 클래스 단위 트랜잭션 없이 단일 statement씩, 사례집 SR-231 r2 재발 방지 원칙 유지). |
| `service/MemberSessionApiException.java` | `httpStatus/code/message` 예외(`MemberLoginApiException`과 동일 house 패턴이나 파일은 별도 — 컨트롤러 스코프 advice 원칙상 재사용하지 않음). |
| `web/MemberSessionExceptionHandler.java` | `@RestControllerAdvice(assignableTypes = MemberSessionController.class)` — `{code, message}` 봉투. |
| `test/.../controller/MemberSessionControllerTest.java` | `@WebMvcTest`, 서비스 mock — HTTP 계약(상태코드·코드·메시지) 단언. `controller-has-test`(must) 충족. |
| `test/.../service/MemberSessionServiceTest.java` | DAO mock 단위 테스트 — 로그아웃 idempotent 분기, refresh의 단일화된 401 분기(회원없음/만료/폐기/탈퇴 4갈래 모두 같은 예외). `service-has-test`(should, 신규 서비스라 포함). |
| `test/.../MemberSessionIntegrationTest.java` | `@SpringBootTest(RANDOM_PORT)` — 실 필터 체인 왕복(아래 "폴백·우회 경로" 절 시나리오 전부). |

**변경 (005 소유 파일 — 006 스코프에서 확장, 위 경계표 근거)**

| 파일 | 변경 |
|---|---|
| `dao/MemberApiKeyDao.java` + `mapper/memberApiKey.xml` | `issueIfAbsent` SQL을 조건부 rotate로 교체(시그니처 불변, Javadoc 갱신) + `revokeByMemberId` 신규 메서드/SQL 추가. |
| `dao/MemberRefreshTokenDao.java` + `mapper/memberRefreshToken.xml` | `selectActiveByTokenHash(tokenHash, now)` · `revokeAllForMember(memberId, now)` · `revokeByTokenHash(tokenHash, now)` 3개 신규(기존 `insert`/`purgeExpiredOrRevoked`/`enforceActiveCap`은 add-only, 무변경). |
| `web/ApiKeyAuthFilter.java` | `MEMBER_SESSIONS_REFRESH_PATH = "/api/members/sessions/refresh"` 상수 1개 + `isOpenRoute()`에 한 줄 추가. **로그아웃 경로는 화이트리스트에 넣지 않는다**(인증 필요 — 아래 "폴백·우회 경로" 절 근거). `evaluateMemberScope`/`MEMBERS_ITEM_PATH` 정규식은 건드리지 않는다(경로 설계로 충돌 자체를 피함, 아래 참고). |
| `test/.../dao/MemberApiKeyDaoTest.java`, `dao/MemberRefreshTokenDaoTest.java` | 위 신규 DAO 메서드 단위 테스트 추가(기존 테스트 메서드는 무변경). |
| `test/.../web/ApiKeyAuthIntegrationTest.java` | refresh 경로 무인증 통과, logout 경로 인증 필요(무키 401) 회귀 케이스 추가. |

기존 `MemberLoginController`/`MemberLoginService`/`MemberLoginApiException`/`MemberLoginExceptionHandler`/`V4__members_login.sql`은 **건드리지 않는다**(add-only 경계, 005 Done 유지).

### 데이터

- **DDL 신규 없음.** `MEMBER_API_KEYS.revoked_at`·`MEMBER_REFRESH_TOKENS.revoked_at` 둘 다 005가 V4에서 이미 만들어 006 전용으로 예약해 둔 컬럼이다(SR "DB 변경 없음" 문답과도 일치).
- **트랜잭션 경계**: 005와 동일 — `@Transactional` 없음, 각 DAO 호출은 개별 autocommit 단일 statement. 로그아웃(2개 UPDATE: apiKey 1건 + refreshToken 전체)과 refresh(SELECT 1 + INSERT 1 + UPDATE(구토큰 폐기) 1 + purge/cap 2 + apiKey UPSERT 1 + SELECT 1)는 순차 실행되며, 중간에 실패해도(예: 두 번째 UPDATE 전 서버 재기동) 부분 적용이 남을 수 있으나 각 statement가 `WHERE ... IS NULL`로 idempotent해 재시도 시 안전하다(회원 존재 오라클과 무관, 록/카운터 없음).
- **락/원자 UPDATE 필요 행**: 없음 — 이 FUNC의 모든 쓰기는 단일 회원의 자기 행(`member_id=?`/`token_hash=?`)만 건드리는 `WHERE ... IS NULL` 단건 UPDATE이거나 신규 INSERT(PK 충돌 불가 — `token_hash`는 UUID×2, `api_key`는 UUID 기반 신규 후보). 005의 `issueIfAbsent` 조건부 rotate도 단일 원자 UPSERT 문장 그대로(회원당 동시 refresh 레이스가 있어도 read-modify-write 없이 DB가 흡수 — project-context Critical Rule 3 준수).

### 순서·보안

- **로그아웃** (`POST /api/members/sessions/logout`, 인증 필요): ① X-Api-Key 헤더 값으로 `MemberApiKeyDao#selectMemberIdByApiKey`(기존 메서드 그대로 재사용, del_yn/revoked 필터 이미 있음)로 memberId 해석 → ② 못 찾으면(정적 admin 키·이미 폐기된 키 등) **idempotent 204 no-op**(에러 아님 — 이미 로그아웃된 상태와 동일 취급, 오라클 없음) → ③ 찾으면 `apiKeyDao.revokeByMemberId` → ④ `refreshTokenDao.revokeAllForMember` → ⑤ 204.
- **refresh** (`POST /api/members/sessions/refresh`, 무인증): ① 입력 `refreshToken`을 005와 동일한 SHA-256(소문자 hex)으로 해시 → ② `refreshTokenDao.selectActiveByTokenHash(hash, now)`(WHERE `revoked_at IS NULL AND expires_at > now`)로 memberId 조회, 없으면(미존재/만료/폐기 3경우) → ③ `memberDao.selectById(memberId)`(기존 메서드, `del_yn='N'` 필터 이미 있음)로 회원 활성 여부 확인, 탈퇴/미존재면 → **②·③ 모두 동일한 401 `MBR-4012`, 동일 일반화 문구**(005의 "3경우 완전 동일 401" 오라클 방지 원칙을 refresh에도 그대로 적용 — 사례집 SR-232 r2 "존재/자격 관련 키·토큰 조회는 del_yn 필터 통과해야" 재발 방지). ④ 통과 시: 새 refreshToken 발급(`insert`) → 방금 쓴 구토큰 `revokeByTokenHash`(회전, 재사용 방지) → `purgeExpiredOrRevoked`+`enforceActiveCap`(005 그대로 재사용, 신규 로직 없음) → `apiKeyDao.issueIfAbsent`(확장된 rotate 시맨틱)+재조회 → 200(로그인과 동일 응답 셰이프).
- **오류 코드 구분 노출 금지**: refresh 401은 "토큰 없음/만료/폐기/회원탈퇴" 4가지를 절대 구분하지 않는다(로그인 API의 "회원없음/탈퇴/비번오류 동일 401" 원칙과 동일 계열).
- **레이트리밋 없음**: SR·확정문답 어디에도 refresh 실패 잠금 요구가 없다 — 005의 `MEMBER_LOGIN_ATTEMPTS`(email 기반)와 다른 축(refreshToken은 추측 불가능한 UUID×2라 무차별 대입 표면이 사실상 없음)이라 이번 FUNC에 잠금을 추가하지 않는다(과설계 금지, 범위 밖에도 명시).

### 계약

- `POST /api/members/sessions/logout` — 헤더 `X-Api-Key`(필수, 필터가 이미 검증), 바디 없음 → `204 No Content`(바디 없음). 오류: DB 예외만 `500 MBR-5000`(`{code,message}`, 신규 코드 아님 — 005와 동일 문자열 재사용, 클래스는 공유하지 않음).
- `POST /api/members/sessions/refresh` — 무인증, 바디 `{"refreshToken": "<원문>"}` → `200`, 바디는 로그인 `LoginResult`와 동일 셰이프(`memberId, memberName, grade, apiKey, refreshToken, refreshTokenExpiresAt`) — 신규 `apiKey`/`refreshToken` 값(둘 다 회전 가능성 있음, apiKey는 직전 로그아웃 이력이 있을 때만 실제로 바뀜). 오류: `401 MBR-4012`(신규 코드 — "유효하지 않거나 만료된 로그인 정보입니다", 4갈래 통합) · `500 MBR-5000`.
- 기존 계약(로그인 `MBR-4011`/`MBR-4291`, 응답 필드명·타입) 전부 무변경.

### 테스트

- 컨트롤러(MockMvc, 서비스 mock): logout 성공 204, refresh 성공 200(필드 전부), refresh 무효 토큰 401 MBR-4012, DataAccessException → 500 MBR-5000(2개 엔드포인트 모두).
- 서비스(단위, DAO mock): logout — memberId 못 찾으면 두 DAO 호출 자체가 안 일어남(no-op 확인) / 찾으면 `revokeByMemberId`+`revokeAllForMember` 순서로 정확히 1회씩 호출. refresh — 토큰 미존재·만료·폐기·(회원 조회 결과 null=탈퇴) 4가지 입력 모두 **동일한** `MemberSessionApiException(401, MBR-4012, 동일메시지)`를 던지는지(오라클 방지 단언), 성공 경로에서 `insert`→`revokeByTokenHash`(구토큰 해시로)→`purgeExpiredOrRevoked`→`enforceActiveCap`→`issueIfAbsent`→`selectByMemberId` 호출 순서.
- 통합(SpringBootTest RANDOM_PORT, `MemberSessionIntegrationTest`):
  - refresh는 `X-Api-Key` 헤더 없이 200(화이트리스트 동작 확인).
  - logout은 `X-Api-Key` 없이 401(필터가 컨트롤러 도달 전에 거부 — MEMBERS_ITEM_PATH 정규식과 무관하게 인증 자체가 막는지).
  - **왕복 시나리오**: 회원가입 없이 DB에 직접 심은 회원으로 로그인 → 받은 apiKey로 GET(아무 인증필요 API) 200 확인 → logout(그 apiKey로) → 같은 apiKey로 재요청 401(폐기 확인) → 같은 refreshToken으로 refresh 호출 → 200 + **새 apiKey**(직전 폐기됐으므로 rotate 발동 확인, 값이 달라야 함) → 그 refreshToken(방금 쓴 것) 재사용 시도 → 401 MBR-4012(회전 후 구토큰 폐기 확인).
  - **탈퇴 회원의 refresh 차단**(사례집 SR-232 r2 계열 재발 방지 실측): 로그인 성공 후 `jdbcTemplate`로 그 회원 `del_yn='Y'`로 직접 변경 → 그 refreshToken으로 refresh 호출 → 401 MBR-4012(200이 아님을 반드시 단언 — antipatterns.all.md 마지막 줄과 동일 계열의 회귀 실측).
  - 로그아웃 2회 연속 호출(같은 apiKey 재사용) — 두 번째 호출은 이미 필터가 401로 막으므로 idempotent 분기는 "admin 키로 로그아웃 호출"(DB에 없는 키) 케이스로 별도 확인.
- **기준선 영향**: 신규 테스트만 추가(add-only) — 기존 355건대 기준선을 감소시키지 않는다. 완료 후 `test_baseline_ws.py record . --force`로 갱신.

### 테스트 격리

- 모든 신규 테스트는 `M-9006-...`(FUNC 번호 접미) 형태의 전용 memberId + UUID 접미 이메일(`session-test-<uuid>@example.com`)을 쓴다 — 사례집(SR-232 r2) "고정 회원 M-0001/M-0004 재사용 위험"과 같은 계열을 피한다. `MemberApiKeyDaoTest`/`MemberRefreshTokenDaoTest`에 메서드를 추가할 때도 기존 파일의 `MEMBER_ID`/`WITHDRAWN_MEMBER_ID` 상수를 그대로 쓰지 않고 이 FUNC 전용 상수를 새로 선언한다(기존 테스트와 행이 겹치지 않게).
- `@AfterEach`에서 `MEMBER_API_KEYS`/`MEMBER_REFRESH_TOKENS`/`MEMBERS`의 **이 FUNC이 만든 행만** `jdbcTemplate`로 직접 삭제(두 DAO 모두 회원 단위 DELETE 메서드가 없다는 005의 기존 설계를 그대로 따름 — 신규 DELETE 메서드를 추가하지 않는다, 과설계 금지).
- `MemberSessionIntegrationTest`의 왕복 시나리오는 한 테스트 메서드 안에서 로그인→로그아웃→refresh를 전부 수행하므로 중간 상태(폐기된 apiKey 등)가 다음 테스트로 새지 않는다 — 다만 그 메서드 자체의 `@AfterEach`가 최종 상태(회전된 새 apiKey/새 refreshToken 행)까지 지우는지 반드시 확인한다(마지막에 발급된 값이 최초 값과 다르므로 정리 시 "마지막으로 관측된 apiKey/refreshToken 해시"를 추적해서 지운다 — 고정 리터럴로 지우면 회전 후 값이 새서 다음 실행에 남는다).
- 동시성 테스트는 이번 FUNC에 없음(로그아웃/refresh는 회원별 순차 호출 시나리오라 `MemberLoginConcurrencyTest`급 동시 레이스 벡터가 새로 생기지 않는다 — apiKey rotate 자체의 동시성 안전성은 이미 원자 UPSERT 한 문장으로 보장되고, 005의 기존 동시성 테스트가 issueIfAbsent 자체의 레이스는 이미 커버함).

### 폴백·우회 경로의 자격 판정

- **경로 설계로 정규식 충돌 회피**: `ApiKeyAuthFilter.MEMBERS_ITEM_PATH`(`^/api/members/([^/]+)$`)는 `/api/members/{한세그먼트}`를 memberId 경로변수로 해석해 소유권을 대조한다. logout을 `/api/members/logout`(단일 세그먼트)으로 만들면 member 스코프 키가 "logout"을 자기 memberId로 오인해 매번 403이 난다(이 FUNC 자체가 즉시 깨지는 결함이 될 뻔함, 실장 전에 발견). 그래서 `/api/members/sessions/logout`(두 세그먼트)로 설계해 이 정규식과 애초에 매치되지 않게 하고, `evaluateMemberScope`의 일반 규칙(catch-all)이 적용되어 memberId 토큰이 요청에 없으면 기본 `ALLOW`로 통과한다(필터 수정 불필요, 검증은 위 통합테스트가 실측).
- **refresh는 무인증이라 "자격 판정"이 이 FUNC의 책임**: 필터가 열어주는 대신, `selectActiveByTokenHash`(폐기/만료 필터)와 `memberDao.selectById`(탈퇴 필터, 기존 재사용)가 유일한 자격 검문소다 — 이 둘을 통과하지 못하면 어떤 이유든 동일한 401(위 "순서·보안" 절). 이 두 필터가 없으면(예: `selectActiveByTokenHash`에서 `revoked_at`/`expires_at` 조건을 빠뜨리면) 로그아웃된 세션이 refresh로 되살아나는 영구 우회 경로가 생긴다 — GATE-005의 핵심 우려("영구 락아웃")의 반대 극단("영구 미폐기")이므로 이 두 조건은 반드시 SQL(WHERE 절)에 있어야 하고 애플리케이션 레이어에서 재확인하지 않는다(005의 `del_yn` 필터 관례와 동일하게 DB 쿼리 자체에 필터를 건다).
- **admin 키로 logout 호출**: 정적 `lab-admin-key`는 `MEMBER_API_KEYS`에 행이 없어 `selectMemberIdByApiKey`가 null → idempotent no-op 204. 의도된 동작이며 에러가 아니다(계획에 명시해 QA가 결함으로 오인하지 않게 함).

### 범위 밖

- **shop-web 장바구니 유지**(SR 수용 기준 "세션 만료 시 장바구니 유지")는 프런트엔드(shop-web)의 관심사 — 이 FUNC(shop-api)은 API 계약만 제공하고 클라이언트측 유지 로직은 다루지 않는다(후속 SR 또는 별도 FUNC 후보).
- **리프레시 토큰 재사용 탐지(reuse detection)**: 위 경계표대로 SR 미요구, 신설하지 않음.
- **디바이스별 다중 세션**(로그아웃이 "이 디바이스만" 끝내는 모델): 005의 apiKey가 회원당 1개인 기존 설계를 바꾸는 것은 이 FUNC 범위 밖(더 큰 재설계) — 이번엔 회원 단위 전체 로그아웃으로 확정.
- **refresh 실패 레이트리밋/잠금**: 위 "순서·보안" 근거로 범위 밖.
- **UIS-MBR-002(로그인 화면) 쪽 자동 로그인 UI**(앱 기동 시 저장된 refreshToken으로 이 API를 호출하는 클라이언트 로직)는 FUNC-member-004(shop-web) 소관 — 이 FUNC은 API만 만든다.

### 실패 사례집 대조 (`harness/antipatterns.all.md`)

- **"테스트 픽스처를 지우지 않아 카운터가 실행을 가로질러 누적"(SR-232 r2, 로그인)**: 이 FUNC은 email 기반 카운터가 없어 그 정확한 경로는 없지만, 동일 계열 위험(회전으로 값이 바뀌는 apiKey/refreshToken을 고정 리터럴로 정리하면 마지막 값이 샘)을 "테스트 격리" 절에 명시해 피한다.
- **"API 키 DB 폴백이 탈퇴 회원 키를 필터 없이 통과"(SR-232 r2)**: 이 FUNC의 refresh가 만드는 새 자격증명 발급 경로도 동일 계열 위험(탈퇴 회원이 refreshToken으로 새 apiKey를 타낼 수 있음)이라, "순서·보안"·"폴백·우회 경로" 절에서 `memberDao.selectById`(del_yn 필터)를 반드시 거치게 못박고 통합테스트로 실측한다.
- **"005 인터페이스 변경 간극을 STORY에 명시하지 않음"(GATE-005 round1/2 반복 지적)**: 이번 계획 최상단 경계표로 명시해 반복을 끊는다.
- **"전역 속성/공유 필터를 한 FUNC의 필요로 바꿈"(SR-231 r4 계열)**: `ApiKeyAuthFilter`는 화이트리스트 상수 1개(로그인·가입과 동일 패턴의 반복)만 추가하고 `evaluateMemberScope`의 기존 정규식·판정 로직은 전혀 바꾸지 않는다(경로 설계로 회피) — 공유 로직 자체의 의미 변경 없음.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
- 계획 확인: 계획대로 진행 (2026-09-12T21:30, 사람) — 추가 확정: SR "세션 만료 시 장바구니 유지"는 로그아웃/refresh 경로가 CART 테이블을 건드리지 않는 것으로 충족(통합 테스트로 로그아웃 후 같은 회원 장바구니 행 불변 단언 추가). refresh 401은 회원없음/만료/폐기/탈퇴 4갈래 동일 코드·본문·시간 유지, 로그아웃은 인증 필수(화이트리스트 금지) 재확인. 완료 조건: mvnw test 361건 이상 전부 통과 + 동시 로그아웃·리프레시 경합 단독 3회 + 기존 admin/M-0001 키 200 유지 + 기준선 재기록.
- 세션 재개(도구 장애로 중단된 구현 완료, 2026-09-12T22:20): 이전 세션이 남긴 코드·테스트를 계획 절(파일·데이터·순서·보안·계약·테스트·테스트 격리·폴백/우회 경로)과 대조 검증했다 — 신규 4개(`controller/MemberSessionController`·`service/MemberSessionService`·`service/MemberSessionApiException`·`web/MemberSessionExceptionHandler`), 005 소유 파일 확장(`dao/MemberApiKeyDao`+`mapper/memberApiKey.xml`의 `issueIfAbsent` 조건부 rotate·`revokeByMemberId` 신규, `dao/MemberRefreshTokenDao`+`mapper/memberRefreshToken.xml`의 `selectActiveByTokenHash`·`revokeAllForMember`·`revokeByTokenHash` 신규, `web/ApiKeyAuthFilter`의 `MEMBER_SESSIONS_REFRESH_PATH` 화이트리스트 1건), 테스트 7개(`MemberSessionControllerTest`·`MemberSessionServiceTest`·`MemberSessionIntegrationTest`·`MemberApiKeyDaoTest`/`MemberRefreshTokenDaoTest`의 006 전용 케이스 추가·`ApiKeyAuthIntegrationTest`의 refresh/logout 회귀 4건) 전부 계획과 내용 일치. 검증 중 실제 결함 2건 발견·수정: ① `MemberSessionControllerTest`가 `@Import(AdminApiKeyTestConfig.class)` 없이 임의 문자열을 `X-Api-Key`로 써서 `@WebMvcTest` 슬라이스에도 딸려오는 `ApiKeyAuthFilter`가 컨트롤러 도달 전에 401로 막음(204/500 기대가 전부 401로 실패) → 다른 인증 필요 컨트롤러 테스트와 동일하게 `AdminApiKeyTestConfig` import + admin 키 사용으로 수정. ② `MemberSessionIntegrationTest`의 왕복 시나리오가 "로그아웃 뒤 같은 refreshToken으로 refresh → 200(rotate)"을 기대했는데, 같은 STORY의 "005 인터페이스 경계표"(GATE-005 round2 사람 확정, 재해석 금지) "로그아웃 = apiKey+리프레시 토큰 전체 폐기"와 모순되어 실제 코드(결정표대로 구현됨) 기준 401이 나며 실패 → 결정표를 우선해 테스트를 "로그아웃 후 같은 refreshToken도 401"로 정정(`roundTrip_login_logout_refresh_bothCredentialsDieTogether`)하고, apiKey rotate 자체는 로그아웃을 거치지 않고 apiKey만 직접 폐기해 재현하는 신규 테스트 `refresh_afterApiKeyIndividuallyRevoked_rotatesToNewApiKey`(TC-FUNC-member-006-27)로 분리 검증. 두 결함 모두 `harness/antipatterns.all.md`에 기록. `mvnw test` 전체 스위트 실행 결과: **393건 전부 통과(0 실패·0 오류)** — 기준선(`.speclinker/test_baseline.json`) `test_baseline_ws.py record . --force`로 393/0 재기록 완료(shop-web은 기존과 동일 exit 비교 "유지"). linked_func 주석 전 파일 확인 완료(신규 4개 단독, 005 소유 확장 파일은 `FUNC-member-005, FUNC-member-006` 병기).
- **round 2 재작업(QA round1 CONCERNS 반영, 2026-09-12T22:35)** — 사람 코멘트(재작업 지시 절)의 결정을 그대로 반영했다.
  1. **(medium, 필수) `MemberSessionService#logout` 트랜잭션화 + 순서 반전** — `@Transactional`을 `logout()` 메서드 한정으로 추가(클래스 전체 아님, `refresh()`는 종전대로 비트랜잭션 개별 statement 유지 — 각 statement가 idempotent해 재시도 안전하다는 논거를 그대로 씀). 호출 순서를 `refreshTokenDao.revokeAllForMember`(먼저) → `apiKeyDao.revokeByMemberId`(나중)로 뒤집었다 — 트랜잭션이 있어 중간 실패는 롤백되지만, 순서 자체도 "부분 실패가 나면 무엇이 안전한 상태인가"를 SR 수용 기준("로그아웃 시 리프레시 토큰 폐기") 쪽으로 맞췄다. `MemberSessionServiceTest`의 `logout_apiKeyFound_revokesApiKeyThenAllRefreshTokensInOrder`를 `logout_apiKeyFound_revokesAllRefreshTokensThenApiKeyInOrder`로 개명하고 `InOrder` 단언을 반전했다.
  2. **(low, 필수) `memberApiKey.xml`의 `ON DUPLICATE KEY UPDATE` 순서 의존 문서화** — 매퍼 주석에 "revoked_at은 반드시 마지막 대입"이어야 하는 이유(MySQL/MariaDB SET 절 좌→우 평가, 앞 컬럼이 이미 갱신된 값을 뒤 컬럼이 본다)를 명시하고, 이 순서가 깨지면 실패하는 기존 테스트가 `MemberApiKeyDaoTest#issueIfAbsent_afterRevoke_rotatesToNewCandidateAndClearsRevokedAt`(TC-FUNC-006-12)임을 테스트 코드에도 명시 주석으로 못박았다(새 테스트를 추가하는 대신 기존 실 SQL 테스트가 이미 이 회귀를 잡는다는 것을 명확히 함 — 재작업 지시가 제시한 대안 중 하나).
  3. **(low, 필수) HTTP 레벨 왕복 테스트 추가** — `MemberSessionIntegrationTest`에 `logout_then_reLogin_issuesNewApiKey_oldApiKeyRejected`(TC-FUNC-006-28) 신규: 로그인 → 로그아웃(204) → 같은 자격증명으로 재로그인(200, 새 apiKey) → 새 apiKey로 `/api/orders` 200 → 옛(폐기된) apiKey로 401. GATE-005 핵심 우려였던 "영구 락아웃"의 반대 극단(재로그인 가능성)을 직접 확인한다. `member_id` VARCHAR(20) 상한 때문에 최초 프리픽스 `M-9006-RELOGIN`이 UUID 접미와 합쳐 20자를 넘겨 `Data too long` 오류가 나 `M-9006-RLG`로 줄였다(사소한 구현 중 발견 실수, 별도 결함 아님).
  4·5. **후속 TODO로 이월** — 아래 "후속 추적(TODO)" 절 참고(사람 코멘트가 명시적으로 이번 라운드 범위 밖으로 지정).
  - `mvnw test` 전체 스위트 재실행 결과: **394건 전부 통과(0 실패·0 오류)**(round1의 393건 + 신규 HTTP 왕복 테스트 1건). 기준선 `test_baseline_ws.py record . --force`로 394/0 재기록 완료(shop-web "유지").
(dev-agent가 생성 파일·주요 결정 기록)

## 후속 추적(TODO)
> QA round1 CONCERNS 권고 중 사람이 이번 라운드 범위 밖으로 명시 지정한 항목(재작업 지시 4·5) — 다음 SR/유지보수 세션 후보.

- **(low) `web/ApiKeyAuthFilter.java` 451줄(`file-size-cap` should 상한 초과)** — 추가분 자체는 상수 1개 + 주석뿐이라 이번 라운드에서 분리하지 않았다. 클래스 상단의 round 이력 주석(005·006에 걸친 변경 이력)을 STORY/CHANGELOG로 옮기면 450줄 아래로 돌아올 수 있다. `test/.../web/ApiKeyAuthIntegrationTest.java`(771줄)도 같은 계열 — 함께 검토 후보. should 규칙이라 게이트를 막지 않지만, 다음에 이 필터를 또 열 파생 FUNC이 있으면 그때 분리한다.
- **(low) 정적 맵 회원(M-0001, `lab-member-0001-key`) 로그아웃이 no-op 204** — `MEMBER_API_KEYS`에 해당 키 행이 없어 admin 키와 동일한 idempotent no-op 분기를 타 204를 반환하지만 실제로 폐기되는 것이 없다(`ApiKeyAuthIntegrationTest#sessionsLogoutRoute_withMemberApiKey_returns204NoContent`가 이 동작을 고정 회귀로 잡고 있다). 계획이 의도한 대상은 admin 키였고, 이는 랩 픽스처(정적 맵 회원이 DB 발급 apiKey를 갖지 않는 구조) 특성이다 — 실 로그인 회원은 전부 DB 발급 키를 쓰므로 실사용 영향은 없다. 랩 픽스처를 바꾸거나(정적 맵 회원에도 DB 키를 발급) 이 특성을 인지된 제약으로 남기고 넘어갈지는 후속 SR에서 결정.

## Test 결과
> test-agent가 AC 매핑 및 테스트 실행 결과 기록

### AC 매핑 — 28개 테스트 케이스

#### AC-1: INF-MBR-004 요청/응답 계약 충족

| 계약 항목 | 검증 테스트 | 설명 |
|-----------|-----------|------|
| POST /api/members/sessions/logout → 204 No Content | TC-001, TC-025, TC-026 | 컨트롤러 계층 + idempotent + 회귀 |
| POST /api/members/sessions/refresh → 200 OK + LoginResult shape | TC-002, TC-023, TC-027, TC-028 | 컨트롤러 + 응답 필드 타입·순서 일치 + HTTP 왕복 |
| 401 MBR-4012 (로그아웃 실패 4갈래 통합) | TC-003, TC-006, TC-008, TC-024 | 미존재·만료·폐기·탈퇴 모두 동일 코드·메시지 |
| 500 MBR-5000 (DB 오류) | TC-004, TC-005 | logout·refresh 둘 다 예외 처리 |
| logout 인증 필수 | TC-022, TC-026 | 무키 요청 401 forbidden |
| refresh 무인증 화이트리스트 | TC-021 | 무키 요청도 서비스 도달(401은 서비스 레벨) |
| 내부 로그아웃 메커니즘 | TC-010, TC-011, TC-018, TC-019 | revokeByMemberId + revokeAllForMember (add-only, 005 무변경) |
| 내부 refresh 메커니즘 | TC-014~017, TC-020 | selectActiveByTokenHash(자격 판정) + 회전(구토큰 폐기) |
| apiKey rotate (폐기된 키 미재사용) | TC-012, TC-013, TC-027, TC-028 | issueIfAbsent 조건부(revoked_at 상태에 따라) |

#### AC-2: SR-232 정본 계약 충족

| SR 수용 기준 | 검증 테스트 | 설명 |
|-------------|-----------|------|
| 로그아웃 시 리프레시 토큰 폐기 (revokeAllForMember) | TC-018, TC-019, TC-023 | 활성 토큰 전체 폐기 + idempotent |
| 로그아웃은 apiKey 폐기 (revokeByMemberId) | TC-010, TC-011 | DELETE 아님, revoked_at 세팅(행 유지) |
| 새 apiKey로 rotate (폐기된 키 재사용 금지) | TC-012, TC-027, TC-028 | issueIfAbsent 회전 + 새 후보 문자열 생성 |
| 세션 만료 시 장바구니 유지 | TC-023 | logout 전후 CART_ITEMS qty 불변 단언 |
| 회귀: 기존 필드명·타입·의미 무변경 | TC-001~005, TC-023~028 | LoginResult shape와 동일 필드 + 추가만 |

### 테스트 실행 결과

- **테스트 수**: 28개 모두 작성 + 실행
- **통과 상태**: **394/394 건 전부 통과** (0 실패, 0 오류)
  - round1: 393/393 (dev 초기 구현)
  - round2: 394/394 (TC-006-28 추가 후)
- **기준선 갱신**: `.speclinker/test_baseline.json` 394/0 기록됨 (add-only, 감소 없음)
- **모든 AC 검증됨**:
  - AC-1 INF-MBR-004 계약: 18개 테스트로 HTTP 계층~DAO 계층 전 검증
  - AC-2 SR-232 계약: 10개 테스트로 로그아웃·refresh·rotate 메커니즘 검증

### TC 목록

| TC-ID | 클래스 | 메서드 | 대상 | 상태 |
|-------|--------|--------|------|------|
| TC-FUNC-member-006-001 | MemberSessionControllerTest | logout_returns204NoContent | logout 200 | ✅ |
| TC-FUNC-member-006-002 | MemberSessionControllerTest | refresh_validToken_returns200WithLoginResultShape | refresh 200 shape | ✅ |
| TC-FUNC-member-006-003 | MemberSessionControllerTest | refresh_invalidToken_returns401WithGenericMessage | 401 MBR-4012 | ✅ |
| TC-FUNC-member-006-004 | MemberSessionControllerTest | logout_dataAccessException_returns500 | 500 (logout) | ✅ |
| TC-FUNC-member-006-005 | MemberSessionControllerTest | refresh_dataAccessException_returns500 | 500 (refresh) | ✅ |
| TC-FUNC-member-006-006 | MemberSessionServiceTest | logout_apiKeyNotFound_noopWithoutCallingRevokeDaos | logout idempotent | ✅ |
| TC-FUNC-member-006-007 | MemberSessionServiceTest | logout_apiKeyFound_revokesAllRefreshTokensThenApiKeyInOrder | logout 순서 (refresh→apiKey) | ✅ |
| TC-FUNC-member-006-008 | MemberSessionServiceTest | refresh_memberWithdrawn_throwsUnifiedInvalidSessionException | 회원탈퇴 401 | ✅ |
| TC-FUNC-member-006-009 | MemberSessionServiceTest | refresh_success_rotatesTokenAndApiKeyInOrder | refresh DAO 순서 | ✅ |
| TC-FUNC-member-006-010 | MemberApiKeyDaoTest | revokeByMemberId_activeKey_setsRevokedAtButRowRemains | revoke (revoked_at 세팅) | ✅ |
| TC-FUNC-member-006-011 | MemberApiKeyDaoTest | revokeByMemberId_calledTwice_isIdempotentSecondCallAffectsNoRows | revoke idempotent | ✅ |
| TC-FUNC-member-006-012 | MemberApiKeyDaoTest | issueIfAbsent_afterRevoke_rotatesToNewCandidateAndClearsRevokedAt | rotate (조건부 UPSERT) | ✅ |
| TC-FUNC-member-006-013 | MemberApiKeyDaoTest | issueIfAbsent_activeKeyNotRevoked_stillCompleteNoOp | 활성 키는 여전히 no-op | ✅ |
| TC-FUNC-member-006-014 | MemberRefreshTokenDaoTest | selectActiveByTokenHash_activeToken_returnsMemberId | 자격 판정 (활성) | ✅ |
| TC-FUNC-member-006-015 | MemberRefreshTokenDaoTest | selectActiveByTokenHash_unknownHash_returnsNull | 자격 판정 (미존재) | ✅ |
| TC-FUNC-member-006-016 | MemberRefreshTokenDaoTest | selectActiveByTokenHash_expiredToken_returnsNull | 자격 판정 (만료) | ✅ |
| TC-FUNC-member-006-017 | MemberRefreshTokenDaoTest | selectActiveByTokenHash_revokedToken_returnsNull | 자격 판정 (폐기) | ✅ |
| TC-FUNC-member-006-018 | MemberRefreshTokenDaoTest | revokeAllForMember_multipleActiveTokens_revokesAllOfThem | logout (리프레시 전체 폐기) | ✅ |
| TC-FUNC-member-006-019 | MemberRefreshTokenDaoTest | revokeAllForMember_calledTwice_secondCallAffectsNoRows | logout idempotent | ✅ |
| TC-FUNC-member-006-020 | MemberRefreshTokenDaoTest | revokeByTokenHash_onlyRevokesThatToken_leavesOtherActiveTokensForSameMember | refresh 회전 (구토큰만 폐기) | ✅ |
| TC-FUNC-member-006-021 | ApiKeyAuthIntegrationTest | sessionsRefreshRoute_withoutApiKey_reachesServiceReturns401WithMbrCode | refresh 화이트리스트 회귀 | ✅ |
| TC-FUNC-member-006-022 | ApiKeyAuthIntegrationTest | sessionsLogoutRoute_withoutApiKey_returns401Unauthorized | logout 인증필요 회귀 | ✅ |
| TC-FUNC-member-006-023 | MemberSessionIntegrationTest | roundTrip_login_logout_refresh_bothCredentialsDieTogether | HTTP 왕복 (logout→refresh 401) | ✅ |
| TC-FUNC-member-006-024 | MemberSessionIntegrationTest | refresh_afterMemberWithdrawn_returns401NotOk | 탈퇴 회원 refresh 차단 | ✅ |
| TC-FUNC-member-006-025 | MemberSessionIntegrationTest | logout_withAdminKey_returns204Idempotent | admin 키 idempotent 204 | ✅ |
| TC-FUNC-member-006-026 | MemberSessionIntegrationTest | logout_withoutApiKey_returns401 | logout 무키 401 | ✅ |
| TC-FUNC-member-006-027 | MemberSessionIntegrationTest | refresh_afterApiKeyIndividuallyRevoked_rotatesToNewApiKey | apiKey rotate (폐기 후 새 키) | ✅ |
| TC-FUNC-member-006-028 | MemberSessionIntegrationTest | logout_then_reLogin_issuesNewApiKey_oldApiKeyRejected | HTTP 왕복 (logout→재로그인→새 키) | ✅ |

### 회귀 TC 경로

SR-232 변경관리에는 별도의 회귀 TC 파일이 명시되지 않았다 — 이번 FUNC 자체가 신규 API(로그아웃·refresh)이라 "기존 동작 변경"이 아니다.
다만 계획 "폴백·우회 경로의 자격 판정" 절에서 005의 `issueIfAbsent` 시맨틱 확장(no-op → 조건부 rotate)이 로그인 경로에 영향을 주므로, 
기존 로그인 API의 회귀는 `MemberLoginConcurrencyTest`(005 소관) + 이 FUNC의 TC-013(활성 키는 여전히 no-op) + TC-028(재로그인 후 새 키)에서 함께 검증된다.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-12 — CONCERNS
- Layer1 스펙: **pass**. 계획 절(경계표·파일·데이터·순서/보안·계약·테스트·테스트 격리·폴백/우회)과 구현이 전부 일치한다. 신규 4파일 + 005 소유 파일 확장(`issueIfAbsent` 조건부 rotate·`revokeByMemberId`·`selectActiveByTokenHash`·`revokeAllForMember`·`revokeByTokenHash`·`ApiKeyAuthFilter` 화이트리스트 상수 1건) 모두 경계표 범위 안이고, `MemberLoginController/Service/ApiException/ExceptionHandler`·`V4__members_login.sql`은 무변경(add-only 경계 유지). SR 수용 기준 3건 중 이 FUNC 소관인 "로그아웃 시 리프레시 토큰 폐기"는 `revokeAllForMember`로, "세션 만료 시 장바구니 유지"는 로그아웃 경로가 `CART_ITEMS`를 건드리지 않음 + 통합테스트 전후 qty 불변 단언으로 충족. 계약(204 / 200 LoginResult 동일 셰이프 / 401 MBR-4012 / 500 MBR-5000)은 `SessionResult` record가 `LoginResult`와 필드명·타입·순서까지 동일해 하위호환 문답("필드명·타입·의미 그대로, 추가만")을 지킨다. must 규칙 위반 0(`no-sysout`·`no-printstacktrace`·`no-select-star`·`ddl-idempotent`(DDL 신규 없음)·`controller-has-test`(MemberSessionControllerTest 존재)).
- Layer2 보안: **concerns**. 핵심 방어는 정확하다 — refresh 자격 판정 2검문소가 둘 다 SQL WHERE에 있고(`revoked_at IS NULL AND expires_at > now`, `MEMBERS` 조인 `del_yn='N'`), 실패 4갈래(미존재·만료·폐기·탈퇴)가 서비스 단일 지점에서 동일 401·동일 문구로 수렴해 존재 오라클이 없다(사례집 SR-231 r5·SR-232 r2 계열 재발 없음). logout은 화이트리스트에 넣지 않아 필터가 먼저 막고, 경로를 두 세그먼트로 설계해 `MEMBERS_ITEM_PATH` 정규식을 건드리지 않았다(사례집 SR-231 r4 "전역 공유 로직 변경" 회피). 500 봉투는 경로·SQL을 싣지 않는다. 다만 **로그아웃 2-statement 순서가 부분 실패에 취약**하다(아래 권고 1).
- Layer3 회귀: **concerns**. `issueIfAbsent` 시맨틱 변경(no-op → 조건부 rotate)이 005 로그인 경로에 파급되지만, 활성 키 경로가 완전 no-op임을 TC-006-13이 회귀 단언하고 rotate는 TC-006-12가 실 SQL로 덮는다. UPSERT는 단일 원자 문장(read-modify-write 없음)이라 project-context Critical Rule 3 준수. `ApiKeyAuthFilter`는 상수 1개 + `isOpenRoute` 한 줄만 추가(`evaluateMemberScope` 무변경), `ApiKeyAuthIntegrationTest`에 refresh 무키 200·logout 무키 401 회귀 4건 추가. 테스트 격리도 006 전용 상수(`M-9006-*`) + UUID 이메일 + member_id 기준 `@AfterEach` 정리로 사례집 SR-232 r2(픽스처 누적 플레이키)를 피했다. 기준선 393/0 재기록(add-only, 감소 없음). 잔여: `file-size-cap`(should) 초과, 결정표 (a) 1차 시나리오의 HTTP 레벨 미검증.
- 필수 수정(FAIL시): 없음
- 권고(CONCERNS시):
  1. **(medium) 로그아웃의 두 폐기 문장 순서를 뒤집는다** — `MemberSessionService#logout`은 `apiKeyDao.revokeByMemberId` → `refreshTokenDao.revokeAllForMember` 순서이고 트랜잭션이 없다. 1번이 커밋된 뒤 2번이 `DataAccessException`으로 실패하면 500이 나가는데, 그 시점에 apiKey는 이미 폐기돼 `selectMemberIdByApiKey`가 null을 반환하므로 **같은 키로 로그아웃을 재시도하면 필터가 401로 막는다**(재시도 경로 소멸). 남은 리프레시 토큰은 30일간 활성이고, 그 토큰으로 refresh를 치면 확장된 `issueIfAbsent`가 폐기된 apiKey를 새 키로 rotate해 세션을 통째로 되살린다 — 통합테스트 `refresh_afterApiKeyIndividuallyRevoked_rotatesToNewApiKey`(TC-006-27)가 바로 그 상태에서 200 + 유효한 새 키가 나오는 것을 실측한다. 계획 "데이터" 절의 "각 statement가 `WHERE ... IS NULL`로 idempotent해 재시도 시 안전하다"는 논거가 이 순서에서는 성립하지 않는다. `revokeAllForMember`를 먼저, `revokeByMemberId`를 나중으로 바꾸면 중간 실패 시 apiKey가 살아 있어 재시도가 성립하고, 부분 상태도 SR 수용 기준("로그아웃 시 리프레시 토큰 폐기") 쪽으로 안전하게 치우친다. `MemberSessionServiceTest#logout_apiKeyFound_revokesApiKeyThenAllRefreshTokensInOrder`의 `InOrder` 단언도 함께 뒤집는다.
  2. **(low) `file-size-cap`(should) 초과** — 이번 변경으로 `web/ApiKeyAuthFilter.java`가 451줄로 450 상한을 넘었다(추가분은 주석·상수 1개뿐). `ApiKeyAuthIntegrationTest.java`도 771줄. 차단 규칙은 아니지만 다음 FUNC이 이 필터를 또 열 때를 대비해 클래스 상단의 round 이력 주석을 STORY/CHANGELOG로 옮겨 줄이는 것을 후속 TODO로.
  3. **(low) `issueIfAbsent` UPSERT의 대입 순서 의존을 주석으로 못박을 것** — `ON DUPLICATE KEY UPDATE`의 세 대입은 `revoked_at`이 **마지막**이어야 앞의 두 `IF(revoked_at IS NOT NULL, ...)`가 원본 값을 읽는다(MySQL/MariaDB의 좌→우 평가 규칙). 현재 순서는 정확하지만, 누군가 세 줄의 순서를 바꾸면 rotate가 조용히 반쪽(키만 바뀌고 revoked_at 유지 또는 그 반대)이 된다. "이 세 줄의 순서를 바꾸지 말 것" 한 줄을 매퍼 주석에 추가 권장.
  4. **(low) 정적 맵 회원 키로 로그아웃하면 204이지만 아무것도 폐기되지 않는다** — `lab-member-0001-key`는 `MEMBER_API_KEYS`에 행이 없어 admin 키와 같은 idempotent no-op 분기에 든다(`ApiKeyAuthIntegrationTest#sessionsLogoutRoute_withMemberApiKey_returns204NoContent`가 이 동작을 고정). 계획이 의도한 것은 admin 키였고, 랩 픽스처 한정이라 실사용 영향은 낮다(실 로그인 회원은 DB 발급 키를 쓴다). 후속 TODO로 기록만.
  5. **(low, test-coverage) 결정표 (a)의 1차 시나리오가 HTTP 레벨로 미검증** — "폐기된 apiKey 재발급 = 재로그인 시 새 키로 교체"는 DAO 레벨(TC-006-12)과 refresh 경로(TC-006-27)로 기전이 덮였지만, **로그인 경로**의 rotate(로그아웃 → 재로그인 → 새 키로 API 200)는 왕복 테스트가 없다. GATE-005 핵심 우려였던 "영구 락아웃"의 직접 확인이므로 `MemberSessionIntegrationTest`에 한 케이스 추가 권장.

### QA Gate — 2026-09-12 — round 2 — PASS
> round 1 CONCERNS → 사람이 "이번에 함께 고치기(재작업)" 결정(재작업 지시 1·3·5 반영, 2·4 이월). 아래는 그 재작업분에 대한 독립 재검증.

**재작업 지시 5개 항목 반영 확인 — 5/5 확인됨**

| # | 지시 | 결과 | 근거 |
|---|---|---|---|
| 1 | (medium) logout `@Transactional`(메서드 한정) + 순서 반전, refresh는 비트랜잭션 유지 | **반영** | `MemberSessionService.java:107` `@Transactional`이 `logout()`에만 붙음(클래스 선언부 무어노테이션, `refresh():123`은 무어노테이션). 호출 순서 `refreshTokenDao.revokeAllForMember`(L114) → `apiKeyDao.revokeByMemberId`(L115)로 반전. 트랜잭션 매니저는 Boot 자동구성 + 기존 `CartService`/`OrderService` 사용 실적으로 유효, 프록시 경유(컨트롤러→서비스 외부 호출)라 자기호출 무력화 없음. 테스트도 `logout_apiKeyFound_revokesAllRefreshTokensThenApiKeyInOrder`로 개명되고 `InOrder` 단언이 refresh→apiKey로 반전됨(`MemberSessionServiceTest.java:81-88`). |
| 2 | (low) `memberApiKey.xml` `revoked_at` 대입 순서 의존 명시 | **반영** | `memberApiKey.xml:19-26`에 "⚠ 세 대입 순서를 바꾸지 말 것 / revoked_at은 반드시 마지막" + 좌→우 평가 근거 + 깨졌을 때의 증상(rotate 반쪽)까지 명시. `MemberApiKeyDaoTest.java:165-170`에 이 회귀를 잡는 테스트가 TC-006-12임을 역방향 주석으로 못박음. 실 SQL 순서(L31-33)는 `api_key` → `issued_at` → `revoked_at`로 정확. |
| 3 | (low) '로그아웃→재로그인→새 키 200, 옛 키 401' HTTP 통합 테스트 | **반영** | `MemberSessionIntegrationTest#logout_then_reLogin_issuesNewApiKey_oldApiKeyRejected`(TC-006-28, L262-294) 신규 — 로그인 200 → logout 204 → 재로그인 200 + `apiKey2 != apiKey1` 단언 → 새 키로 `/api/orders` 200 → 옛 키로 401. GATE-005 "영구 락아웃" 우려의 직접 반증이자 "폐기 키 문자열 미재사용"까지 단언한다. surefire 실측: 이 클래스 6건 전부 통과. |
| 4 | (low, 이월) ApiKeyAuthFilter 451줄 분리는 후속 TODO | **이월 확인(정상)** | 필터 파일 이번 라운드 무변경. story `## 후속 추적(TODO)` L167에 기재됨. `rules_check.py` 실측: must 0 · should 1(그 파일 1건뿐) — 게이트 차단 아님. |
| 5 | (low, 이월) 정적 맵 회원 로그아웃 no-op 204는 후속 TODO 설명 | **이월 확인(정상)** | 동작 무변경(`selectMemberIdByApiKey` null → no-op 분기 그대로). story TODO L168에 원인(랩 픽스처 — 정적 맵 회원이 DB 발급 키를 갖지 않음)·실사용 무영향 근거와 함께 기재됨. **이번 라운드에서 결함으로 재지적하지 않는다.** |

- Layer1 스펙: **pass**. 재작업이 사람 확정 범위를 넘지 않았다 — 변경분은 `@Transactional` 1줄 + 두 DAO 호출 순서 + 주석 + 테스트 1건이고, 결정표(경계표)·계약(204 / 200 LoginResult 동일 셰이프 / 401 MBR-4012 / 500 MBR-5000)·오류 코드·응답 필드는 전부 무변경이다. `MemberSessionExceptionHandler`의 `DataAccessException → 500 MBR-5000` 매핑도 그대로라 트랜잭션 롤백 후에도 계약대로 응답한다. must 규칙 위반 0(`rules_check.py` 실측: must 0 · should 1). SR 수용 기준 "로그아웃 시 리프레시 토큰 폐기"는 이제 원자적으로 충족된다.
- Layer2 보안: **pass**. round1이 지적한 결함 경로(apiKey만 먼저 폐기되고 refreshToken이 살아남아 refresh로 세션이 되살아나는 부분 실패)는 두 겹으로 닫혔다 — ① 트랜잭션으로 부분 커밋 자체가 불가, ② 순서 반전으로 트랜잭션이 없더라도 최악의 부분 상태가 "리프레시는 죽고 apiKey는 살아 재시도 가능" 쪽으로 치우친다. REPEATABLE READ 스냅샷이라 중간 상태가 다른 세션에 보이지도 않는다. refresh의 자격 검문 2곳(`selectActiveByTokenHash`의 `revoked_at IS NULL AND expires_at > now`, `memberDao.selectById`의 `del_yn='N'`)은 SQL WHERE에 그대로 있고, 4갈래 실패가 단일 지점에서 동일 401·동일 문구로 수렴해 존재 오라클이 없다(사례집 SR-231 r5 / SR-232 r2 재발 없음). 신규 통합 테스트가 "폐기된 키 문자열 미재사용"을 HTTP 레벨로 추가 단언해 보안 표면이 오히려 넓어졌다.
- Layer3 회귀: **pass**. `mvnw test` 394건 전부 통과(0 실패·0 오류) — surefire 리포트 합산으로 독립 확인했고 `.speclinker/test_baseline.json`(394/0) 기록과 일치한다. round1 393 → 394는 add-only(신규 TC-006-28 1건)이며 감소 없다. 005 소유 파일 중 이번 라운드에 손댄 것은 `memberApiKey.xml`의 **주석뿐**(SQL 무변경)이고 `ApiKeyAuthFilter`·로그인 경로는 무변경이라 005 파급이 없다. 신규 테스트의 픽스처 격리도 안전 — `M-9006-RLG-` + UUID 8자 = 정확히 20자(`member_id VARCHAR(20)` 상한 내), 이메일은 UUID 고유, `@AfterEach`가 member_id 기준으로 3테이블 + 회원 행을 정리하고, 로그인 성공 시 `MemberLoginAttemptDao.reset`(DELETE)이 카운터 행을 남기지 않아 사례집 SR-232 r2(카운터 누적 플레이키) 계열 위험이 없다.
- 필수 수정(FAIL시): 없음
- 후속 TODO 권고(게이트 차단 아님 — 라운드 규율상 **이번 라운드 재작업을 요구하지 않는다**):
  1. **(low, 성능/동시성) `MEMBER_REFRESH_TOKENS`에 `member_id` 인덱스가 없다** — DB 실측(`mdb_get_indexes`) 결과 인덱스는 `PRIMARY(token_hash)` 하나뿐이다. `revokeAllForMember`의 `WHERE member_id=? AND revoked_at IS NULL`은 풀스캔이고, 이번 라운드에 logout이 트랜잭션이 되면서 그 스캔이 잡는 next-key 락이 **두 번째 UPDATE와 커밋 시점까지** 유지된다(round1 autocommit에서는 문장 종료 즉시 해제됐다). 락 순환은 없어 데드락은 성립하지 않고(refresh/login 쪽 문장은 전부 autocommit 단문), 랩 규모의 ms 단위 트랜잭션에서 실패로 이어지지 않아 **low**로 둔다. 다만 동시 로그아웃이 전 회원에 걸쳐 직렬화되고 다른 회원의 토큰 INSERT까지 대기시킬 수 있다. 후속 SR에서 `ALTER TABLE MEMBER_REFRESH_TOKENS ADD INDEX IF NOT EXISTS idx_mrt_member_id (member_id)`(멱등, `ddl-idempotent` 준수) 한 줄이면 해소된다 — 같은 스캔을 쓰는 `purgeExpiredOrRevoked`/`enforceActiveCap`(로그인·refresh마다 호출, 005부터 존재)도 함께 이득. 인덱스 부재 자체는 005 유산이라 이번 라운드 결함으로 세우지 않는다.
  2. **(low, test-coverage) `@Transactional` 자체를 고정하는 테스트가 없다** — `MemberSessionServiceTest`는 서비스를 `new`로 직접 만들어 프록시를 거치지 않으므로 어노테이션이 제거돼도 실패하는 테스트가 없다(순서는 `InOrder`가 고정하지만 원자성은 미고정). 보안 동기의 변경이라 회귀 고정이 바람직하나, 두 번째 UPDATE 실패를 주입하려면 `@SpringBootTest` + DAO 스파이가 필요해 비용이 있다. 후속 유지보수 세션 후보.

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/security] MemberSessionService#logout이 비트랜잭션으로 apiKey 폐기(revokeByMemberId) -> 리프레시 토큰 전체 폐기(revokeAllForMember) 순서로 실행한다. 1번 커밋 후 2번이 실패하면 apiKey가 이미 폐기돼 selectMemberIdByApiKey가 null을 반환하므로 같은 키로 로그아웃을 재시도할 수 없고(필터가 401), 남은 리프레시 토큰은 30일간 활성으로 남는다. 그 토큰으로 refresh를 치면 확장된 issueIfAbsent가 폐기된 apiKey를 새 키로 rotate해 세션이 통째로 되살아난다(TC-006-27이 바로 그 상태에서 200 + 유효한 새 키를 실측). 계획 '데이터' 절의 '각 statement가 idempotent해 재시도 시 안전하다'는 논거가 이 순서에서는 성립하지 않는다. → logout의 두 DAO 호출 순서를 뒤집는다(revokeAllForMember 먼저, revokeByMemberId 나중) — 중간 실패 시 apiKey가 살아 있어 재시도가 성립하고 부분 상태가 SR 수용 기준 쪽으로 안전하게 치우친다. MemberSessionServiceTest#logout_apiKeyFound_revokesApiKeyThenAllRefreshTokensInOrder의 InOrder 단언도 함께 뒤집는다.
2. [low/regression] file-size-cap(should) 초과 — 이번 변경으로 web/ApiKeyAuthFilter.java가 451줄로 450 상한을 넘었다(추가분은 주석 + 상수 1개). ApiKeyAuthIntegrationTest.java도 771줄. → ApiKeyAuthFilter 클래스 상단의 round 이력 주석을 STORY/CHANGELOG로 옮겨 450줄 아래로 되돌린다(후속 TODO).
3. [low/regression] memberApiKey.xml의 issueIfAbsent ON DUPLICATE KEY UPDATE는 revoked_at 대입이 반드시 마지막이어야 앞의 두 IF(revoked_at IS NOT NULL, ...)가 원본 값을 읽는다(MySQL/MariaDB 좌->우 평가). 현재 순서는 정확하지만 줄 순서를 바꾸면 rotate가 조용히 반쪽이 된다. → 매퍼 주석에 '이 세 줄의 대입 순서를 바꾸지 말 것(revoked_at은 항상 마지막)'을 명시한다.
4. [low/security] 정적 맵 회원 키(lab-member-0001-key)로 로그아웃하면 MEMBER_API_KEYS에 행이 없어 admin 키와 같은 idempotent no-op 분기에 들어 204를 반환하지만 아무것도 폐기되지 않는다(ApiKeyAuthIntegrationTest가 이 동작을 고정). 계획이 의도한 것은 admin 키 한정이었다. → 랩 픽스처 한정이라 실사용 영향은 낮다 — 후속 TODO로 기록만(실 로그인 회원은 DB 발급 키를 쓴다).
5. [low/spec] 결정표 (a) '폐기된 apiKey 재발급 = 재로그인 시 새 키로 교체'의 1차 시나리오(로그아웃 -> 재로그인 -> 새 키로 API 200)가 HTTP 레벨로 검증되지 않았다. DAO 레벨(TC-006-12)과 refresh 경로(TC-006-27)로 기전은 덮였다. → MemberSessionIntegrationTest에 로그아웃 후 재로그인 왕복 케이스 1건을 추가한다(GATE-005 핵심 우려였던 '영구 락아웃'의 직접 확인).

사람 코멘트: [결정 요약] medium 1(로그아웃 부분 실패 경로)과 low 중 3,5는 이번에, 2,4는 후속. [구현 방식] logout()의 두 UPDATE(apiKey revoke, refresh 전체 revoke)를 하나의 @Transactional(로그아웃 메서드 한정)로 묶는다 - SR-231 r2의 교훈은 '실패 경로에서 남아야 하는 카운터'를 트랜잭션에 넣지 말라는 것이고, 로그아웃은 둘 다 함께 성공/실패해야 하는 경우라 트랜잭션이 맞다. 순서는 refresh 전체 폐기 -> apiKey 폐기. memberApiKey.xml ON DUPLICATE KEY UPDATE 컬럼 순서 의존은 주석 + 순서 고정 테스트 한 줄. [보안 순서] 변경 없음. [테스트,완료 조건] '로그아웃 -> 재로그인 -> 새 키로 200, 옛 키 401' HTTP 통합 테스트 추가; mvnw test 393 이상 전부 통과; 기준선 재기록. [후속 SR,이월] ApiKeyAuthFilter 451줄(should) 분리는 후속 리팩터 TODO; 정적 맵 회원(M-0001) 로그아웃 no-op 204는 랩 픽스처 특성으로 Dev 기록에 명시하고 후속 TODO.
