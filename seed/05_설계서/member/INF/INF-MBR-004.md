---
inf-id: INF-MBR-004
name: 로그아웃
layer: api
method: POST
path: /api/members/sessions/logout
domain: member
domain-code: MBR
srs-f: [TBD]
screens: []
tables:
  - MEMBER_API_KEYS
  - MEMBER_REFRESH_TOKENS
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSessionController.java:28-43
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSessionService.java:64-93
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSessionService.java:95-116
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberSessionExceptionHandler.java:41-48
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberApiKeyDao.java:44-61
  - modules/shop-api/src/main/resources/mapper/memberApiKey.xml:36-58
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberRefreshTokenDao.java:62-67
  - modules/shop-api/src/main/resources/mapper/memberRefreshToken.xml:51-58
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:227-244
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:365-370
---

> [반영: FUNC-member-006] 2026-09-12

# INF-MBR-004: POST /api/members/sessions/logout — 로그아웃

> **개요:** 로그인 세션을 끝낸다 — 이 회원의 API 키와 리프레시 토큰 전체를 폐기한다. 관련: INF-MBR-005(리프레시) — 같은 세션 생명주기의 반대편.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSessionController.java:39-43`

## 요청

- Method: POST
- Path: /api/members/sessions/logout
- Content-Type: (바디 없음)
- 인증: **필요**. `X-Api-Key` 헤더는 `ApiKeyAuthFilter`가 먼저 검증한다(화이트리스트에 없음 — 인증 필요 경로). 컨트롤러는 그 헤더 값을 다시 읽어 어느 회원의 세션인지 서비스가 해석한다(필터가 memberId를 별도로 실어주지 않음).

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| X-Api-Key | Header | string | Y | 로그인/리프레시로 발급받은 API 키 |

## 응답 (204 No Content)

바디 없음. **항상 204**(idempotent) — `X-Api-Key`가 `MEMBER_API_KEYS`에서 조회되지 않아도(admin 키, 이미 폐기된 키 등) 에러가 아니라 "이미 로그아웃된 상태와 동일"로 취급해 204를 반환한다.

## 비즈니스 규칙

- **memberId 해석**: `X-Api-Key` 값으로 `MemberApiKeyDao#selectMemberIdByApiKey`(로그인/003이 이미 만든 메서드, `MEMBERS.del_yn='N'` + `revoked_at IS NULL` 필터 포함)를 조회한다. 못 찾으면(정적 admin 키, 이미 폐기된 키 등) **에러 없이 idempotent 204 no-op** — 두 폐기 DAO 호출 자체가 일어나지 않는다.
- **트랜잭션(메서드 한정)**: `MemberSessionService#logout`만 `@Transactional`이다(`refresh()`는 비트랜잭션 유지 — 사례집 SR-231 r2 "실패 경로에서 남아야 하는 카운터를 트랜잭션에 넣지 말라" 원칙은 여기 두 폐기에는 적용되지 않는다. 둘 다 함께 성공/실패해야 하는 단일 논리 동작이기 때문).
- **두 폐기 순서(사람 확정, round2 재작업)**: `refreshTokenDao.revokeAllForMember`(리프레시 토큰 전체 폐기) → `apiKeyDao.revokeByMemberId`(API 키 폐기) 순으로 실행한다. 트랜잭션이 있어 중간 실패는 롤백되지만, 혹시 트랜잭션 경계 밖에서 재사용되는 상황까지 대비해 "apiKey가 아직 안 죽었으면 재시도 가능"이 되도록 이 순서로 고정했다(반대 순서였던 round1 구현은 apiKey만 죽고 리프레시 토큰이 살아남아, 그 토큰으로 refresh를 치면 폐기된 apiKey가 새 키로 rotate되어 로그아웃되지 않은 것과 같은 상태로 세션이 되살아나는 결함이 있었다).
- **회원 단위 전체 폐기(개별 세션 아님)**: `MEMBER_API_KEYS`가 회원당 1개뿐인 기존 설계와 일관되게, 이 회원의 활성 리프레시 토큰 **전체**를 폐기한다(디바이스별 개별 로그아웃 모델 아님 — 범위 밖으로 명시적 결정).
- **폐기는 DELETE가 아니라 `revoked_at` 세팅**: `MEMBER_API_KEYS`/`MEMBER_REFRESH_TOKENS` 모두 행을 지우지 않고 `revoked_at`을 채운다 — 재조회 시 조건(`revoked_at IS NULL`)에서만 제외된다.
- **세션 종료 시 장바구니 유지(SR 수용기준)**: 이 API는 `CART_ITEMS`를 건드리지 않는다 — 이 요건은 "손대지 않음"으로 충족된다(통합테스트가 로그아웃 전후 장바구니 수량 불변을 단언).
- **레거시/미해결 주의(사람이 확인, 후속 SR 후보)**: `MEMBER_REFRESH_TOKENS`에 `member_id` 인덱스가 없어 `revokeAllForMember`가 풀스캔이다 — 로그아웃이 트랜잭션이 되면서 그 스캔의 락이 커밋 시점까지 유지된다(랩 규모에서는 실패로 이어지지 않는 low 리스크로 QA가 판단, 후속 SR에서 멱등 인덱스 추가 권고).
- **레거시 픽스처 주의**: 정적 맵 회원(M-0001, `lab-member-0001-key`)은 `MEMBER_API_KEYS`에 행이 없어 idempotent no-op 분기를 타 204를 반환하지만 실제로 폐기되는 것이 없다(랩 픽스처 특성 — 실 로그인 회원은 전부 DB 발급 키를 쓰므로 실사용 영향 없음).

## 트랜잭션 순서

`@Transactional`(메서드 한정)

1. `MEMBER_API_KEYS`에서 `X-Api-Key`로 memberId 조회(`selectMemberIdByApiKey`). 못 찾으면 여기서 종료 — 204(변경 없음).
2. `MEMBER_REFRESH_TOKENS` UPDATE — 이 회원의 활성 리프레시 토큰 전체 폐기(`revokeAllForMember`).
3. `MEMBER_API_KEYS` UPDATE — 이 회원의 API 키 폐기(`revokeByMemberId`).
4. 커밋 → 204. (2·3 중 실패 시 전체 롤백 — 부분 폐기 상태가 남지 않는다.)

## 사이드이펙트

- `CART_ITEMS`/`ORDERS` 등 세션 밖 테이블은 건드리지 않는다.

## 오류 응답

| 코드 | HTTP | 사유 | 발생 조건 |
|------|------|------|---------|
| (필터, `{"error":"unauthorized"}`) | 401 | 인증 실패 | `X-Api-Key` 헤더 없음 — `ApiKeyAuthFilter`가 컨트롤러 도달 전 default-deny로 차단(컨트롤러 레벨 `{code,message}` 봉투가 아니라 필터 전역 포맷) |
| MBR-5000 | 500 | 일시적인 오류입니다 | `DataAccessException` — 정제된 메시지만 응답, 원본은 서버 로그에만(`MemberSessionExceptionHandler`) |

## 참조 테이블

- `MEMBER_API_KEYS`
- `MEMBER_REFRESH_TOKENS`

## curl 예시

```bash
curl -X POST /api/members/sessions/logout \
  -H "X-Api-Key: mk_1a2b3c4d5e6f..."
```
