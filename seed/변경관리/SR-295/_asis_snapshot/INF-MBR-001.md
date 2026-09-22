---
inf-id: INF-MBR-001
name: 회원가입 인증코드 발송
layer: api
method: POST
path: /api/members/signup/verification-codes
domain: member
domain-code: MBR
srs-f: [TBD]
screens: []
tables:
  - MEMBER_SIGNUP_VERIFICATIONS
  - MEMBER_SIGNUP_RATE_LIMITS
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSignupController.java:28-45
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSignupService.java:107-196
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSignupApiException.java:1-32
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberSignupExceptionHandler.java:33-57
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberSignupVerificationDao.java:12-42
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberSignupRateLimitDao.java:18-60
  - modules/shop-api/src/main/resources/mapper/memberSignupVerification.xml:11-19
  - modules/shop-api/src/main/resources/mapper/memberSignupRateLimit.xml:33-48
  - modules/shop-api/src/main/resources/db/member_signup_verifications.sql:30-42
  - modules/shop-api/src/main/resources/db/member_signup_rate_limits.sql:34-42
---

# INF-MBR-001: POST /api/members/signup/verification-codes — 회원가입 인증코드 발송

> **개요:** 이메일 또는 휴대폰번호를 받아 6자리 인증코드를 생성·저장(5분 유효)하고 "발송"한다(이 랩은 실제 발송 게이트웨이가 없어 로그로 대체). 인증코드 확인 + 회원 생성(가입완료)은 별도 API(INF-MBR-002, FUNC-member-003)의 책임이며 이 API 범위 밖이다.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSignupController.java:38-42`

## 요청

- Method: POST
- Path: /api/members/signup/verification-codes
- Content-Type: application/json
- 인증: 없음(무인증) — 신규 가입자는 아직 API 키가 없으므로 `ApiKeyAuthFilter` 화이트리스트(`MEMBER_SIGNUP_VERIFICATION_CODE_PATH`)에 등록되어 있다.

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| target | Body | string | Y | 인증코드를 받을 대상 — 이메일 주소 또는 휴대폰번호(하이픈 없이 숫자만). 최대 100자(DB 컬럼 `target VARCHAR(100)` 정합). 형식으로 채널(EMAIL/SMS)을 자동 판별한다 |

## 응답 (200 OK)

```json
{
  "channel": "EMAIL",
  "target": "user@example.com",
  "expiresInSeconds": 300
}
```

- `channel`: 판별된 발송 채널 — `EMAIL` 또는 `SMS`
- `target`: 요청받은 값의 에코(trim 처리됨) — 개인정보 누출 아님(요청자 본인이 보낸 값)
- `expiresInSeconds`: 코드 유효시간(고정값 300) — 코드 자체는 응답에 담기지 않는다(로그에도 마스킹)

## 비즈니스 규칙

- 인증코드는 6자리 숫자, 발급 시점부터 5분(300초) 유효(`MemberSignupService.EXPIRES_IN_SECONDS`)
- 채널 자동판별: 이메일 정규식 매칭 → `EMAIL`, 국내 휴대폰번호 패턴(`01[016789]` + 7~8자리, 하이픈 없음) 매칭 → `SMS`, 둘 다 아니면 400
- 레이트리밋(동일 `target` 기준, 채널 무관): 60초 쿨다운 + 하루 5회 상한. **거부된 요청은 quota를 소비하지 않는다** — 원자 UPSERT의 조건부 갱신이라 `daily_count`/`last_token`이 갱신되지 않으면 그 요청은 카운트에 반영되지 않는다
- 판정 방식(round5/round6, 사람 결정): 요청마다 생성한 UUID 토큰을 조건부로 `last_token`에 기록한 뒤 재조회한 값이 자기 토큰과 같으면 허용 — JDBC affected-rows나 datasource 전역 설정(`useAffectedRows`)에 더 이상 의존하지 않는다(그 설정이 이 FUNC 밖 `ProductDao.decreaseStock`의 반환값 시맨틱까지 바꿔 `POST /api/orders`를 200→409로 회귀시킨 적이 있어 제거됨)
- 발송은 시뮬레이션(로그 1줄, 개인정보 마스킹 — 앞 2자 제외 `*` 처리) — 실제 이메일/SMS 게이트웨이 미연동. 코드 값은 응답·로그 어디에도 노출하지 않는다
- 레거시 주의: `MEMBER_SIGNUP_VERIFICATIONS.requested_at`/`previous_requested_at`/`daily_count` 컬럼은 round3 잔재(deprecated) — round4부터 앱이 더 이상 쓰지 않는다(카운터는 `MEMBER_SIGNUP_RATE_LIMITS`로 완전히 이전됨). `attempt_count`는 이 API가 컬럼만 소유하고 쓰지 않는다(FUNC-member-003 가입완료 API가 코드 검증 시 사용 예정)
- 만료행 정리(purge)는 이 요청 경로에 없다 — `MemberSignupMaintenanceScheduler`(`@Scheduled`, 10분 주기 배치)가 `purgeExpiredCodes`/`purgeOldRows`를 별도로 호출한다

## 트랜잭션 순서

1. `MEMBER_SIGNUP_RATE_LIMITS`에 원자 UPSERT(`touchRateLimit`) — 쿨다운·일일상한 조건을 동시에 만족할 때만 `daily_count`/`last_requested_at`/`last_token`을 이번 요청 토큰으로 갱신(단일 문장, autocommit — `@Transactional` 없음. round4에서 의도적으로 제거해 `purgeExpired`와의 락 경합·데드락을 없앴다)
2. 같은 `target`/`day_key`를 재조회(`selectRateLimit`)해 `last_token`이 내 토큰과 같은지 확인 — 다르면 여기서 429로 즉시 응답하고 코드는 생성하지 않는다
3. 판정을 통과한 경우에만 `MEMBER_SIGNUP_VERIFICATIONS`에 원자 UPSERT(`writeCode`)로 6자리 코드·만료시각을 기록한다(1단계와 별개의 autocommit 문 — 하나의 트랜잭션으로 묶여 있지 않다)

## 오류 응답

응답 봉투는 모두 `{ "code": "...", "message": "..." }` 형태(`MemberSignupExceptionHandler`).

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 400 | MEMBER_TARGET_INVALID | `target`이 비었거나 100자를 초과, 또는 이메일·휴대폰번호 형식에 둘 다 불일치 |
| 429 | MEMBER_VERIFY_COOLDOWN | 동일 `target` 재발송을 60초 이내에 재요청(쿨다운 미경과) |
| 429 | MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED | 동일 `target`이 오늘 이미 5회 요청을 완료 |
| 500 | MBR-5000 | DB 계층 예외(`DataAccessException`) — 정제된 메시지만 응답, 경로·SQL·커넥션 정보는 노출하지 않으며 원본은 서버 로그에만 남는다(무인증 엔드포인트라 정보 노출 위험이 특히 큼) |

## 참조 테이블

- `MEMBER_SIGNUP_VERIFICATIONS`
- `MEMBER_SIGNUP_RATE_LIMITS`

## curl 예시

```bash
curl -X POST /api/members/signup/verification-codes \
  -H "Content-Type: application/json" \
  -d '{"target": "user@example.com"}'
```
