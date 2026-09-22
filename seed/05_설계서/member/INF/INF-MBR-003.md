---
inf-id: INF-MBR-003
name: 로그인
layer: api
method: POST
path: /api/members/login
domain: member
domain-code: MBR
srs-f: [TBD]
screens: []
tables:
  - MEMBER_LOGIN_ATTEMPTS
  - MEMBER_REFRESH_TOKENS
  - MEMBER_API_KEYS
  - MEMBERS
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberLoginController.java:22-38
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberLoginService.java:56-207
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberLoginApiException.java:13-42
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberLoginExceptionHandler.java:25-54
  - modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberCredential.java:16-33
  - modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberLoginAttempt.java:12-26
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberDao.java:79-91
  - modules/shop-api/src/main/resources/mapper/member.xml:66-70
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberLoginAttemptDao.java:17-40
  - modules/shop-api/src/main/resources/mapper/memberLoginAttempt.xml:6-31
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberRefreshTokenDao.java:28-49
  - modules/shop-api/src/main/resources/mapper/memberRefreshToken.xml:6-40
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberApiKeyDao.java:17-44
  - modules/shop-api/src/main/resources/mapper/memberApiKey.xml:6-36
  - modules/shop-api/src/main/resources/db/V4__members_login.sql:1-58
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:136-200
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:218-255
---

> [반영: FUNC-member-005] 2026-09-12

# INF-MBR-003: POST /api/members/login — 로그인

> **개요:** 이메일+비밀번호로 로그인해 세션(리프레시 토큰 30일 + API 키)을 발급한다. 5회 연속 실패 시 10분 잠금.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberLoginController.java:32-35`

## 요청

- Method: POST
- Path: /api/members/login
- Content-Type: application/json
- 인증: 없음(무인증) — 로그인 전에는 아직 API 키가 없다. `ApiKeyAuthFilter`의 `isOpenRoute` 화이트리스트(`MEMBER_LOGIN_PATH`, 정확 일치)에 등록되어 있다(가입 관련 엔드포인트들과 동일한 이유의 예외).

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| email | Body | string | Y | 로그인 이메일(정규화는 `trim()`뿐 — 대소문자 동일시는 DB 콜레이션에 의존) |
| password | Body | string | Y | 비밀번호(평문 전송, 서버에서 BCrypt 대조) |

## 응답 (200 OK)

```json
{
  "memberId": "M-0053",
  "memberName": "홍길동",
  "grade": "SILVER",
  "apiKey": "mk_1a2b3c4d5e6f...",
  "refreshToken": "5f2c9e10-....-....-....-............5f2c9e10-....-....-....-............",
  "refreshTokenExpiresAt": "2026-10-12T10:00:00"
}
```

- `memberId`/`memberName`/`grade`: `MEMBERS` 조회 값(`MemberCredential`을 통해 전달, `passwordHash`는 응답에 절대 포함하지 않는다)
- `apiKey`: 이 회원이 이후 `/api/**` 호출에 쓸 키. 최초 로그인이면 신규 발급, 이미 있으면 기존 값 그대로 재조회(재발급 아님) — `MEMBER_API_KEYS.member_id`가 PK라 회원당 항상 1개
- `refreshToken`: 30일 유효, **원문이 노출되는 유일한 응답**. DB에는 SHA-256 해시(소문자 hex)만 저장(`MEMBER_REFRESH_TOKENS.token_hash`)
- `refreshTokenExpiresAt`: `LocalDateTime` 직렬화(ISO-8601 유사 문자열) — 정확한 포맷 단언은 FUNC-member-004(로그인 화면) 쪽 테스트가 함

## 비즈니스 규칙

- **순서(사람이 직접 확정, 임의 변경 금지)**: ① 잠금 여부 확인(`MEMBER_LOGIN_ATTEMPTS`, 비밀번호 검증 전 — BCrypt 비용 절약) → ② `MEMBERS`를 email로 조회(탈퇴 회원도 포함해서, `del_yn` 필터 없이) → ③ 비밀번호 대조 → ④ 성공 시 실패카운터 reset → 세션 발급.
- **존재 오라클 방지(3경우 완전 동일 응답)**: "회원 없음" / "탈퇴 회원(`del_yn='Y'`)" / "비밀번호 불일치" — 이 세 경우는 완전히 동일한 401 `MBR-4011`, 동일한 일반화 문구(`"이메일 또는 비밀번호가 올바르지 않습니다 (n/5)"`, 사유를 절대 구분해 노출하지 않음), 동일한 카운터 증가 의미로 응답한다. 회원이 없거나 탈퇴한 경우에도 더미 BCrypt 해시로 `matches()`를 돌려 응답시간을 비슷하게 만든다(타이밍 사이드채널 완화, should).
- **잠금 카운터는 회원 존재와 무관**: `MEMBER_LOGIN_ATTEMPTS`는 email 문자열 자체가 PK — 가입되지 않은 이메일도 카운트된다. 이렇게 해야 "이 이메일이 잠겼는가"라는 관찰 자체가 계정 존재의 오라클이 되지 않는다.
- **5번째 실패 = 즉시 429(사람 확인 2로 확정)**: 4번째까지는 401(`n/5` 문구), **5번째 실패는 401이 아니라 곧바로 429 `MBR-4291`**이다. `touchFailure` 한 문장(원자 UPSERT)이 `fail_count` 증가와 `locked_until` 세팅을 같은 SET 목록 좌→우 평가로 처리한다(세션변수 미사용). 잠금 만료 후 재실패는 `fail_count`를 1로 리셋(무한 누적 방지).
- **DB 폴백 인증(사람 확인 3으로 확정)**: `ApiKeyAuthFilter`는 정적 `lab.api-keys` 맵(admin·M-0001만 등록됨) 조회가 실패했을 때만(요청당 최대 1회) `MemberApiKeyDao#selectMemberIdByApiKey`로 DB 폴백 조회한다. 이 폴백은 `MEMBERS` 조인 + `del_yn='N'` + `revoked_at IS NULL`로 좁혀져 있어(round9 재작업), 탈퇴 회원이나 폐기된 키는 종전과 동일한 401을 받는다. 기존 정적 맵 경로(admin·M-0001)는 이 추가로 코드·동작이 전혀 바뀌지 않는다(정적 맵 히트 시 DB 조회 자체가 일어나지 않음).
- **리프레시 토큰 하우스키핑**: 로그인 성공마다 새 토큰 INSERT 직후 이 회원의 만료·폐기 행을 지우고(`purgeExpiredOrRevoked`), 활성 토큰이 상한(5개)을 넘으면 `issued_at` 오래된 순으로 초과분을 지운다(`enforceActiveCap`). 정확한 상한 강제가 아니라 하우스키핑 — 동시 로그인이 6건 이상이면 방금 발급한 토큰이 지워질 수도 있다(사람이 넘긴 low 이슈).
- **API 키 동시 최초발급 레이스 방지**: `issueIfAbsent`는 `member_id` PK에 대한 no-op UPSERT(`ON DUPLICATE KEY UPDATE member_id = member_id`)로, 동시에 여러 로그인 요청이 들어와도 실제로 저장되는 키는 하나뿐이다. 호출부는 이 UPSERT 직후 반드시 `selectByMemberId`로 재조회해야 한다(이 요청이 넘긴 후보 키가 실제로 쓰였다는 보장이 없음).
- **`@Transactional` 미사용(사람 결정)**: 이 클래스는 트랜잭션을 전혀 쓰지 않는다 — 모든 DB 문장을 개별 autocommit 단일 statement로 처리한다(사례집 SR-231 r2 "카운터 증가가 트랜잭션 롤백에 같이 사라짐" 재발 방지).
- **레거시/미해결 주의(QA가 지적, 사람이 waiver 수용 — FUNC-006 착수 시 확정 예정)**: `MEMBER_API_KEYS.revoked_at`을 세팅하는 운영 코드는 아직 없다(FUNC-006 로그아웃 미구현). FUNC-006이 로그아웃에서 `revoked_at`을 세팅하기 시작하면, 이 로그인 API의 `issueIfAbsent`(no-op UPSERT, `member_id`가 PK)는 그 폐기 표시를 지우지 않고 이어지는 `selectByMemberId`도 `revoked_at` 필터가 없어 죽은 키를 그대로 반환한다 — 로그아웃 후 재로그인한 회원이 200 응답과 함께 이미 무효화된 `apiKey`를 받는 **영구 락아웃** 경로가 남아 있다. 폐기된 키를 되살리거나 새로 교체하는 재발급 경로가 이 API에는 없다. 사람이 확정한 처리 방향(a: 로그인이 폐기 키를 새 키로 교체 / b: 006이 DELETE로 폐기 / c: apiKey 폐기를 006 범위에서 제외)은 FUNC-006 착수 시점에 결정한다(현재는 FUNC-006이 `Approved`·미구현 상태라 즉시 영향 없음).

## 트랜잭션 순서

1. (비트랜잭션, 읽기) `MEMBER_LOGIN_ATTEMPTS`에서 잠금 여부 확인(`selectAttempt`). 잠겨 있으면 즉시 429 응답, 이후 단계 진행 안 함.
2. (비트랜잭션, 읽기) `MEMBERS`를 email로 조회(`selectAuthByEmail`, `del_yn` 필터 없음 — 탈퇴 회원도 조회해 동일 401로 처리하기 위함).
3. (비트랜잭션) 비밀번호 대조 실패(회원 없음/탈퇴/비번오류) → `touchFailure`(원자 UPSERT)로 실패 카운터 증가(+임계 도달 시 `locked_until` 세팅) → 401 또는 429 응답, 이후 단계 진행 안 함.
4. (비트랜잭션) 대조 성공 → `reset`(단일 DELETE)으로 실패 카운터 제거.
5. (비트랜잭션) `MEMBER_REFRESH_TOKENS` INSERT(신규 토큰) → 같은 회원의 만료·폐기 행 DELETE(`purgeExpiredOrRevoked`) → 활성 토큰 상한 초과분 DELETE(`enforceActiveCap`).
6. (비트랜잭션) `MEMBER_API_KEYS` no-op UPSERT(`issueIfAbsent`) → `selectByMemberId`로 재조회 → 200 응답.

## 사이드이펙트

- 로그인 성공마다 `MEMBER_REFRESH_TOKENS`에서 **이 회원의** 만료·폐기 토큰 삭제 + 활성 토큰 5개 초과분 삭제(로그인 자체의 목적인 "신규 토큰 발급" 외의 하우스키핑 삭제).
- **공유 보안 필터 변경**: `ApiKeyAuthFilter`에 DB 폴백 조회 경로가 이번 FUNC에서 추가되어, 이 로그인 API가 발급한 `apiKey`로 들어오는 **다른 모든 `/api/**` 요청**의 인증 판정에 `MemberApiKeyDao` 조회가 새로 개입한다(정적 맵 히트 시에는 개입하지 않음 — 기존 admin·M-0001 경로 무변경).

## 오류 응답

응답 봉투는 모두 `{ "code": "...", "message": "..." }` 형태(`MBR-4291`만 `retryAfterSeconds` 필드 추가, `MemberLoginExceptionHandler`).

| 코드 | HTTP | 사유 | 발생 조건 |
|------|------|------|---------|
| MBR-4011 | 401 | 이메일 또는 비밀번호가 올바르지 않습니다 (n/5) | 회원 없음 / 탈퇴 회원(`del_yn='Y'`) / 비밀번호 불일치 — **세 경우 모두 동일 코드·문구**(존재 오라클 방지) |
| MBR-4291 | 429 | 로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요(+`retryAfterSeconds`) | `MEMBER_LOGIN_ATTEMPTS.locked_until`이 현재 시각보다 미래(5회 실패 도달, 10분 잠금) |
| MBR-5000 | 500 | 일시적인 오류입니다. 잠시 후 다시 시도해 주세요 | `DataAccessException` — 정제된 메시지만 응답, 원본은 서버 로그에만(`MemberLoginExceptionHandler`) |

## 참조 테이블

- `MEMBER_LOGIN_ATTEMPTS`
- `MEMBER_REFRESH_TOKENS`
- `MEMBER_API_KEYS`
- `MEMBERS`

## curl 예시

```bash
curl -X POST /api/members/login \
  -H "Content-Type: application/json" \
  -d '{"email": "user@example.com", "password": "abcd1234"}'
```

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-15 | QUICK-20260915-1 | #1 | 429/5000 오류 문구를 실제 코드 상수와 일치시킴(잠시 후 다시 시도해 주세요 완곡화) |  |
