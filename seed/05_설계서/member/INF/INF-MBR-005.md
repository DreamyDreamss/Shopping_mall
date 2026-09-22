---
inf-id: INF-MBR-005
name: 자동 로그인 토큰(리프레시)
layer: api
method: POST
path: /api/members/sessions/refresh
domain: member
domain-code: MBR
srs-f: [TBD]
screens: []
tables:
  - MEMBER_REFRESH_TOKENS
  - MEMBER_API_KEYS
  - MEMBERS
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSessionController.java:28-36
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSessionController.java:45-52
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSessionService.java:64-93
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSessionService.java:118-149
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSessionService.java:151-165
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSessionService.java:167-169
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSessionApiException.java:17-35
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberSessionExceptionHandler.java:28-48
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberApiKeyDao.java:20-42
  - modules/shop-api/src/main/resources/mapper/memberApiKey.xml:8-34
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberRefreshTokenDao.java:30-60
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberRefreshTokenDao.java:69-75
  - modules/shop-api/src/main/resources/mapper/memberRefreshToken.xml:8-49
  - modules/shop-api/src/main/resources/mapper/memberRefreshToken.xml:60-67
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberDao.java:12-14
  - modules/shop-api/src/main/resources/mapper/member.xml:14-19
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:144-149
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:197-209
---

> [반영: FUNC-member-006] 2026-09-12

# INF-MBR-005: POST /api/members/sessions/refresh — 자동 로그인 토큰(리프레시)

> **개요:** 만료 임박한 로그인 정보를 30일 리프레시 토큰으로 자동 재발급한다. 관련: INF-MBR-004(로그아웃) — 같은 세션 생명주기의 반대편.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberSessionController.java:46-49`

## 요청

- Method: POST
- Path: /api/members/sessions/refresh
- Content-Type: application/json
- 인증: 없음(무인증) — `ApiKeyAuthFilter`의 `isOpenRoute` 화이트리스트(`MEMBER_SESSIONS_REFRESH_PATH`, 정확 일치)에 등록. 로그아웃 직후 자동 재로그인 시나리오 등 refresh 시점에는 유효한 API 키가 없을 수 있기 때문(로그인·가입 화이트리스트와 동일한 이유).

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| refreshToken | Body | string | Y | 로그인/이전 refresh가 발급한 리프레시 토큰 원문. 서버는 SHA-256(소문자 hex)으로 해시해 대조한다 — 원문은 DB에 저장되지 않는다 |

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

- 로그인(INF-MBR-003) `LoginResult`와 **완전히 동일한 필드명·타입·순서**(계약 — 기존 클라이언트 하위호환 유지)의 `SessionResult` 레코드.
- `refreshToken`: 이번 호출에서 새로 회전(rotate)된 신규 토큰 — 요청에 실었던 옛 토큰은 이 응답과 동시에 폐기된다(재사용 불가).
- `apiKey`: 직전에 로그아웃 이력이 있어 `revoked_at`이 세팅돼 있었을 때만 실제로 새 값으로 교체된다. 그렇지 않으면(계속 유효했던 키) 기존 값 그대로.

## 비즈니스 규칙

- **4갈래 완전 동일 401(존재 오라클 방지)**: "토큰 미존재" / "토큰 만료" / "토큰 폐기" / "회원 탈퇴(`del_yn='Y'`)" 이 네 경우는 완전히 동일한 401 `MBR-4012`, 동일한 일반화 문구(`"유효하지 않거나 만료된 로그인 정보입니다"`, 사유를 절대 구분해 노출하지 않는다) — 로그인(003) "3경우 동일 401" 원칙을 refresh에도 그대로 적용(사례집 SR-231 r5/SR-232 r2 계열 재발 방지).
- **자격 검문은 반드시 SQL WHERE에 있어야 함**: `MemberRefreshTokenDao#selectActiveByTokenHash`의 `revoked_at IS NULL AND expires_at > now`와 `MemberDao#selectById`의 `del_yn='N'` — 이 두 필터가 빠지면 로그아웃된 세션이 refresh로 영구히 되살아나는 우회 경로가 생긴다. 애플리케이션 레이어에서 재확인하지 않는다(003의 `del_yn` 필터 관례와 동일).
- **토큰 회전(rotation)**: 성공 시 새 토큰을 INSERT하고, 방금 사용한 구 토큰을 `revokeByTokenHash`로 개별 폐기한다(재사용 방지, 이 회원의 다른 활성 토큰에는 영향 없음). 이어서 003의 하우스키핑(`purgeExpiredOrRevoked`, `enforceActiveCap` 상한 5)을 그대로 재사용한다 — 신규 배치·신규 컬럼 없음.
- **apiKey 조건부 rotate(GATE-005 round2 경계표, 사람 확정)**: `issueIfAbsent`는 no-op UPSERT에서 조건부 rotate로 확장됐다 — 행 없으면 INSERT / `revoked_at IS NOT NULL`(로그아웃 이력)이면 새 후보 문자열로 교체하고 `revoked_at`을 NULL로 되돌림 / `revoked_at IS NULL`(계속 유효)이면 완전 no-op. 폐기된 키 문자열 자체는 절대 재사용하지 않는다. 시그니처는 003 그대로라 로그인 API도 이 SQL을 그대로 쓰지만, 활성 키 경로는 완전 no-op이라 로그인 쪽 회귀는 없다.
- **`memberApiKey.xml` UPSERT 대입 순서 의존(구현 주의, 사람이 매퍼 주석에 명시)**: `ON DUPLICATE KEY UPDATE`의 세 대입(`api_key`/`issued_at`/`revoked_at`)은 **`revoked_at`이 반드시 마지막**이어야 한다 — MySQL/MariaDB는 SET 절을 좌→우로 평가하며, 앞 컬럼이 이미 갱신된 값을 뒤 컬럼의 `IF(...)`가 보게 되어 있다. 순서가 바뀌면 rotate가 조용히 반쪽(키만 바뀌고 `revoked_at`은 그대로 남거나 그 반대)이 된다.
- **레이트리밋 없음(의도된 설계)**: `MEMBER_LOGIN_ATTEMPTS` 같은 실패 잠금이 없다 — refreshToken이 UUID×2라 무차별 대입 표면이 사실상 없다는 판단(과설계 금지, SR·확정문답에 요구 없음).
- **세션 만료 시 장바구니 유지(SR 수용기준)**: 이 API는 `CART_ITEMS`를 건드리지 않는다 — 이 요건은 "손대지 않음"으로 충족된다(통합테스트가 refresh 전후 장바구니 수량 불변을 단언).
- **레거시/미해결 주의(사람이 확인, 후속 SR 후보)**: `MEMBER_REFRESH_TOKENS`에 `member_id` 인덱스가 없어 `purgeExpiredOrRevoked`/`enforceActiveCap`이 풀스캔이다(랩 규모에서는 실패로 이어지지 않는 low 리스크로 QA가 판단, 후속 SR에서 멱등 인덱스 추가 권고).

## 트랜잭션 순서

비트랜잭션, 개별 autocommit statement — 003 관례 유지, 각 statement가 `WHERE ... IS NULL`류로 idempotent해 재시도 안전.

1. 입력 `refreshToken` 원문을 SHA-256(소문자 hex)으로 해시.
2. `MEMBER_REFRESH_TOKENS`에서 해시로 활성 토큰 조회(`selectActiveByTokenHash`, `revoked_at IS NULL AND expires_at > now`). 없으면 401 `MBR-4012`, 이후 단계 없음.
3. `MEMBERS`를 memberId로 조회(`selectById`, `del_yn='N'` 필터). 결과 없으면(탈퇴) 401 `MBR-4012`(2와 완전 동일 코드·문구), 이후 단계 없음.
4. `MEMBER_REFRESH_TOKENS` INSERT — 새 리프레시 토큰 발급(30일).
5. `MEMBER_REFRESH_TOKENS` UPDATE — 방금 쓴 구 토큰 개별 폐기(`revokeByTokenHash`, 회전).
6. `MEMBER_REFRESH_TOKENS` DELETE — 이 회원의 만료·폐기 행 정리(`purgeExpiredOrRevoked`, 003 하우스키핑 재사용).
7. `MEMBER_REFRESH_TOKENS` DELETE — 활성 토큰 상한(5) 초과분 정리(`enforceActiveCap`, 003 하우스키핑 재사용).
8. `MEMBER_API_KEYS` 조건부 rotate UPSERT(`issueIfAbsent`) → `selectByMemberId` 재조회.
9. 200 응답(`SessionResult`).

## 사이드이펙트

- 성공마다 003의 하우스키핑(만료·폐기 리프레시 토큰 삭제 + 활성 토큰 5개 초과분 삭제)이 부가로 실행된다(리프레시 발급 자체의 목적 외 정리 작업).
- apiKey 조건부 rotate(`issueIfAbsent` 시맨틱 확장)는 **로그인(INF-MBR-003) API가 호출하는 동일 메서드**에도 영향을 준다 — 로그인 쪽 활성 키 경로는 여전히 완전 no-op이라 회귀는 없다(TC-FUNC-member-006-13 실측).
- `CART_ITEMS`/`ORDERS` 등 세션 밖 테이블은 건드리지 않는다.

## 오류 응답

| 코드 | HTTP | 사유 | 발생 조건 |
|------|------|------|---------|
| MBR-4012 | 401 | 유효하지 않거나 만료된 로그인 정보입니다 | 토큰 미존재 / 만료 / 폐기 / 회원 탈퇴 — **네 경우 모두 동일 코드·문구**(존재 오라클 방지) |
| MBR-5000 | 500 | 일시적인 오류입니다 | `DataAccessException` — 정제된 메시지만 응답, 원본은 서버 로그에만(`MemberSessionExceptionHandler`) |

## 참조 테이블

- `MEMBER_REFRESH_TOKENS`
- `MEMBER_API_KEYS`
- `MEMBERS`

## curl 예시

```bash
curl -X POST /api/members/sessions/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "5f2c9e10-....-....-....-............5f2c9e10-....-....-....-............"}'
```
