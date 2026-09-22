---
inf-id: INF-MBR-006
name: 비밀번호 재설정 코드 요청
layer: api
method: POST
path: /api/members/password-resets/codes
domain: member
domain-code: MBR
srs-f: [TBD]
screens: []
tables:
  - MEMBER_PASSWORD_RESETS
  - MEMBER_PASSWORD_RESET_RATE_LIMITS
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetController.java:13-28
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetController.java:38-44
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java:108-139
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java:141-171
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetApiException.java:12-30
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberPasswordResetExceptionHandler.java:28-49
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetDao.java:16-40
  - modules/shop-api/src/main/resources/mapper/memberPasswordReset.xml:13-22
  - modules/shop-api/src/main/resources/mapper/memberPasswordReset.xml:24-28
  - modules/shop-api/src/main/resources/db/V5__member_password_resets.sql:18-26
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:152-155
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:203-216
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetRateLimitDao.java
  - modules/shop-api/src/main/resources/mapper/memberPasswordResetRateLimit.xml
  - modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberPasswordResetRateLimit.java
---

> [반영: FUNC-member-008] 2026-09-13

# INF-MBR-006: POST /api/members/password-resets/codes — 비밀번호 재설정 코드 요청

> **개요:** 비밀번호를 잊은 회원이 이메일 또는 휴대폰번호를 제출하면 6자리 코드를 생성해 10분간 유효하게 저장하고 "발송"한다(이 랩은 실제 발송 게이트웨이가 없어 로그로 대체). 코드 확인·새 비밀번호 반영은 별도 API(FUNC-member-009, 범위 밖).

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetController.java:39-44`

## 요청

- Method: POST
- Path: /api/members/password-resets/codes
- Content-Type: application/json
- 인증: 없음(무인증) — `ApiKeyAuthFilter`의 `isOpenRoute` 화이트리스트(`MEMBER_PASSWORD_RESET_CODE_PATH`, 정확 일치)에 등록. 로그인 전(비밀번호를 잊은) 사용자가 호출하므로 아직 API 키가 없기 때문(회원가입·로그인·refresh 화이트리스트와 동일한 이유).

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| target | Body | string | Y | 이메일 또는 휴대폰번호. 형식으로 채널을 자동 판별한다(정규식 매칭 실패 시 400). 100자 초과·빈 값도 400 |

## 응답 (202 Accepted)

```json
{
  "channel": "EMAIL",
  "target": "user@example.com",
  "expiresInSeconds": 600
}
```

- `channel`: 판별된 채널 — `EMAIL` 또는 `SMS`.
- `target`: 정규화된 값(원문 echo가 아님) — 이메일은 `trim().toLowerCase()`, 휴대폰은 숫자만(하이픈·공백 제거, 단 현재 휴대폰 정규식이 하이픈을 애초에 허용하지 않으므로 이 분기는 사실상 도달하지 않는다).
- `expiresInSeconds`: 코드 유효기간(고정값 600 = 10분). 실제로 코드가 갱신됐는지(쿨다운 통과 여부), 회원이 존재하는지는 이 응답만으로 절대 구분할 수 없다(오라클 방지 — 형식 오류가 아닌 한 항상 이 모양의 202).
- 코드 원문은 이 응답에도, 어떤 로그에도 담기지 않는다(SHA-256 해시만 저장).

## 비즈니스 규칙

- **형식 오류만 400, 그 외는 항상 202(존재 오라클·쿨다운 오라클 방지)**: 빈 값 / 100자 초과 / 이메일·휴대폰 형식 불일치만 400 `MBR-4100`. 회원 존재 여부, 쿨다운(60초 이내 재요청) 위반 여부는 응답에 전혀 드러나지 않는다 — 어느 경우든 동일한 202·`VerificationCodeResult` 바디(사례집 SR-231 "존재 판정 오라클"의 일반화 적용).
- **회원 테이블을 구조적으로 조회하지 않음**: `MemberPasswordResetService` 생성자는 `MemberDao`를 주입받지 않는다 — 회원 존재/탈퇴 여부와 무관하게 항상 같은 처리 경로를 타도록 클래스 구조 자체로 강제한다.
- **target 정규화·형식 정합**: 이메일은 `trim()+toLowerCase()`, 휴대폰은 `MemberRegistrationService.PHONE_PATTERN`(`^01[016789][0-9]{7,8}$`, 하이픈 불허)과 문자 단위로 동일한 정규식을 재사용한다(round2, 형제 가입 API와 판정 불일치를 없애기 위한 사람 수정) — 같은 입력이 재설정·가입 두 API에서 다르게 판정되는 것을 방지. PK(target)가 정규화 값이므로 같은 대상의 다른 표기는 같은 행·같은 쿨다운을 공유한다.
- **발송 로그는 재조회 대조 후에만 남긴다(round2, QA FAIL 재작업)**: `touchRequest` UPSERT 직후 `selectByTarget`으로 다시 읽어, 저장된 `code_hash`가 이번 요청이 만든 해시와 같을 때만(=쿨다운에 걸리지 않고 실제로 반영됐을 때만) "발송" 로그를 남긴다. round1은 이 순서 없이 무조건 로그를 남겨, 쿨다운에 걸린 요청도 로그가 남고 정작 저장된 코드는 이전 것이라 발송 로그와 실제 저장 코드가 불일치하는 결함이 있었다(SR-231 round5 재조회 대조와 동일 기법). 응답은 어느 경우든 무조건 202·상수 바디라 이 판정은 로그 여부에만 쓰이고 오라클을 만들지 않는다.
- **`memberPasswordReset.xml` UPSERT SET 순서 의존(구현 주의)**: `ON DUPLICATE KEY UPDATE`의 다섯 대입(`code_hash`/`expires_at`/`consumed_at`/`attempt_count`/`created_at`) 중 `created_at`이 반드시 맨 마지막이어야 한다 — MySQL/MariaDB는 SET을 좌→우로 평가하므로, 앞선 네 IF 조건의 `TIMESTAMPDIFF(SECOND, created_at, #{now})` 판정이 항상 갱신 전(원본) `created_at` 값을 보게 하기 위함이다(`MEMBER_SIGNUP_RATE_LIMITS` round6과 동일 원리). 순서가 바뀌면 쿨다운 판정이 첫 번째 IF에서만 정확하고 이후 컬럼은 이미 갱신된 `created_at`을 봐서 조용히 깨진다.
- **코드값 의미**: `cooldownSeconds=60`(동일 target 재요청 쿨다운), `EXPIRES_IN_SECONDS=600`(코드 유효 10분), `CODE_LENGTH=6`(숫자 코드) — 모두 SR-234 확정 요건.
- **레거시/round 이력**: round1은 휴대폰 정규식을 하이픈 허용으로 넓혔다가 형제 가입 API(`MemberRegistrationService`)와 같은 입력에 다른 판정(202 vs 400)을 내는 불일치가 QA에서 지적되어(재작업 지시 low(4)), round2에서 하이픈 불허로 되돌렸다(현재 코드 = round2 상태).
- **일일 요청 상한 5회(SR-297 #2)**: 쿨다운 판정과 나란히, target별 일일 상한(`DAILY_REQUEST_LIMIT=5`)을 전용 카운터 테이블(`MEMBER_PASSWORD_RESET_RATE_LIMITS`, SCH-MBR-009)에 대한 원자 UPSERT(`MemberPasswordResetRateLimitDao#touchDailyLimit`)로 판정한다 — 가입 `MemberSignupRateLimitDao#touchRateLimit`과 문자 단위로 동일한 SQL 본문(세션 변수 없이 SET 좌→우 평가만으로 결정적, round6 원리). 이 판정이 코드 테이블 UPSERT(`touchRequest`)보다 **먼저** 실행되고, 상한 초과 시 `touchRequest`를 아예 호출하지 않아 발송·코드 갱신 자체를 생략한다(응답은 그래도 항상 동일한 202).
- **거부 로그 사유 분기(SR-297 #2, QA CONCERNS round1 재작업)**: 일일 상한 게이트가 거부하면 서버 로그 1줄을 남기되, 재조회한 `dailyCount >= DAILY_REQUEST_LIMIT`이면 "일일 상한 초과", 그 외(쿨다운 미경과)면 "쿨다운"으로 사유를 갈라 적는다(형제 `MemberSignupService#rejectionFor`와 같은 판별 기준). `target`은 `mask()`로 가려 원문을 남기지 않는다. 이 로그는 서버 관측용일 뿐 응답에는 전혀 반영되지 않는다(오라클 방지).
- **알려진 동작 — 자정 경계(SR-297 #2, STEP 3-0 사람 확인)**: 자정 직전 요청 뒤 60초 이내에 날짜가 바뀌면, 일일 상한 카운터(SCH-MBR-009, `day_key` PK)는 새 날짜 행이 생겨 쿨다운 없이 admit(일일 카운트 소비)하지만, 이 코드 테이블의 자체 쿨다운(`created_at` 기준 60초)은 여전히 걸려 코드가 갱신되지 않을 수 있다 — 일일 카운트만 소비되고 코드는 안 나가는 조합이 가능하다. 응답은 이 경우에도 항상 202·동일 바디라 클라이언트에는 드러나지 않는다(교차-day_key 조회로 이 조합을 막는 것은 범위 밖).

## 트랜잭션 순서

비트랜잭션, 개별 autocommit statement(`@Transactional` 미사용) — 두 UPSERT(`touchDailyLimit`·`touchRequest`)가 각각 자체 원자 판정이라 응용 레이어의 트랜잭션이 불필요. `MEMBER_PASSWORD_RESET_RATE_LIMITS`와 `MEMBER_PASSWORD_RESETS`는 완전히 분리된 테이블이라 두 UPSERT 사이에 걸치는 락이 없다.

1. target 형식 검증(빈 값 / 100자 초과 / 이메일·휴대폰 정규식). 실패 시 400 `MBR-4100`, 이후 단계 없음.
2. 채널 판별(`EMAIL`/`SMS`) 및 target 정규화.
3. `now`/`dayKey` 계산, 요청 고유 토큰(UUID) 생성.
4. **(SR-297 #2)** `MEMBER_PASSWORD_RESET_RATE_LIMITS` UPSERT(`touchDailyLimit`) — 쿨다운(60초) AND 일일상한(5회) 둘 다 만족할 때만 `daily_count`/`last_requested_at`/`last_token`을 갱신 → `selectRateLimit` 재조회 → `lastToken`이 내 토큰과 같으면 `admitted`.
5. `admitted=false`(쿨다운 또는 상한 초과)면 **6~9단계를 건너뛰고 바로 10단계**로 — 거부 사유(재조회한 `dailyCount>=5`면 "일일 상한 초과", 아니면 "쿨다운")를 가려 서버 로그 1줄만 남긴다(target 마스킹).
6. 6자리 코드 생성 → SHA-256(소문자 hex) 해시.
7. `MEMBER_PASSWORD_RESETS` UPSERT(`touchRequest`) — 코드 테이블 자체 쿨다운(60초) 미만이면 다섯 컬럼 모두 기존 값 유지(no-op), 이상이면 다섯 컬럼 모두 새 값으로 갱신(`attempt_count`는 0, `consumed_at`은 NULL로 리셋).
8. `MEMBER_PASSWORD_RESETS`를 target으로 재조회(`selectByTarget`).
9. 재조회한 `code_hash`가 6단계에서 만든 해시와 같으면(=실제로 반영됐으면) 발송 로그 1줄 기록(코드 원문 미포함, target은 마스킹). 다르면(코드 테이블 쿨다운에 걸려 이전 코드가 그대로 남음 — 자정 경계 등) 로그 생략.
10. 202 응답(`VerificationCodeResult`) — 4~9단계 결과와 무관하게 항상 동일한 모양.

## 사이드이펙트

- `MEMBER_PASSWORD_RESETS`(코드 테이블) + `MEMBER_PASSWORD_RESET_RATE_LIMITS`(SR-297 #2, 일일 상한 카운터 테이블) 두 테이블만 다룬다 — 회원 테이블(`MEMBERS`)은 여전히 조회·변경하지 않는다(`MemberPasswordResetService` 생성자가 `MemberDao`를 주입받지 않는 구조적 강제).
- "발송"은 실제 게이트웨이 호출이 아니라 로그 1줄 기록으로 대체된다(이 랩 환경의 시뮬레이션, `MemberSignupService`의 가입 인증코드 발송 로그와 동일 모양·수준).
- 일일 상한 거부 시에도 서버 로그 1줄(사유 포함)을 남긴다(위 "거부 로그 사유 분기" 참고) — 응답에는 반영되지 않는다.

## 오류 응답

| 코드 | HTTP | 사유 | 발생 조건 |
|------|------|------|---------|
| MBR-4100 | 400 | 이메일 또는 휴대폰번호 형식이 올바르지 않습니다 | target이 빈 값 / 100자 초과 / 이메일·휴대폰 정규식 불일치 |
| MBR-5000 | 500 | 일시적인 오류입니다 | `DataAccessException` — 정제된 메시지만 응답, 원본은 서버 로그에만(`MemberPasswordResetExceptionHandler`) |

## 참조 테이블

- `MEMBER_PASSWORD_RESETS`
- `MEMBER_PASSWORD_RESET_RATE_LIMITS`(SR-297 #2, SCH-MBR-009)

## curl 예시

```bash
curl -X POST /api/members/password-resets/codes \
  -H "Content-Type: application/json" \
  -d '{"target": "user@example.com"}'
```

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-16 | SR-297 | #2 | 일일 상한 5회 게이트(MEMBER_PASSWORD_RESET_RATE_LIMITS UPSERT)를 코드 UPSERT 앞에 배선, 거부 로그 사유 분기, 자정 경계 알려진 동작 기록 | shop-api@b7ae32f |
