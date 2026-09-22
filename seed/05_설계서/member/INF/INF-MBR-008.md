---
inf-id: INF-MBR-008
name: 배송지 CRUD
layer: api
method: GET, POST, PUT, DELETE, PUT
path: /api/members/me/addresses (+ /{addressId}, /{addressId}/default)
domain: member
domain-code: MBR
srs-f: [TBD]
screens: []
tables:
  - MEMBER_ADDRESSES
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberAddressController.java:36-51
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberAddressController.java:53-97
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberAddressController.java:99-113
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressService.java:82-128
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressService.java:130-148
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressService.java:150-190
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressService.java:192-260
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressApiException.java:13-31
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberAddressExceptionHandler.java:27-48
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberAddressDao.java:20-71
  - modules/shop-api/src/main/resources/mapper/memberAddress.xml:8-92
  - modules/shop-api/src/main/resources/db/V7__member_addresses.sql:18-35
  - modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberAddress.java:17-61
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:173-179
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:301-307
---

> [반영: FUNC-member-011] 2026-09-13

# INF-MBR-008: 배송지 CRUD API — 회원 배송지 관리

> **개요:** 회원이 자신의 배송지를 목록조회·등록·수정·삭제·기본 배송지 설정하는 API 5종. AS-IS 없음 —
> SR-235(FUNC-member-011)로 신설된 완전 신규 계약이며, 코드가 곧 정본이다.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberAddressController.java:36-113`

## 엔드포인트 목록

| Method | Path | 설명 | 성공 코드 |
|--------|------|------|-----------|
| GET | /api/members/me/addresses | 배송지 목록(최근 사용 순) | 200 |
| POST | /api/members/me/addresses | 배송지 등록 | 201 |
| PUT | /api/members/me/addresses/{addressId} | 배송지 수정 | 200 |
| DELETE | /api/members/me/addresses/{addressId} | 배송지 삭제(소프트) | 204 |
| PUT | /api/members/me/addresses/{addressId}/default | 기본 배송지로 설정 | 200 |

## 인증·스코프

- 전 경로 `X-Api-Key` 필요(무인증 화이트리스트 아님).
- URL에 `memberId`가 없는 "me" 자원이다 — `ApiKeyAuthFilter#evaluateMemberScope`의
  `MEMBER_ME_ADDRESSES_PATH`(`^/api/members/me/addresses(?:/.*)?$`) 정규식이 5개 경로 전부(메서드
  무관)를 `Decision.SCOPE_TO_SELF`로 판정한다 — 기존 `GET /api/orders`(memberId 토큰 없는 목록 조회)
  강제-자기자신 메커니즘을 재사용한다(새 우회 경로 없음).
- admin 키(`*`)로 호출하면 `evaluateMemberScope` 자체를 타지 않아 이 강제가 일어나지 않는다. 이때
  `memberId` 쿼리 파라미터가 비어 있으면 컨트롤러의 `requireMemberId`가 400 `MBR-4200`으로 명시적으로
  막는다 — "관리자가 특정 회원 대신 /me를 호출"하는 시나리오는 이 SR에서 정의하지 않는다(사람 확정).

## 요청

### 1) GET /api/members/me/addresses — 목록

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Query | string | (강제 주입) | 클라이언트가 직접 넘기지 않는다 — `ApiKeyAuthFilter`의 SCOPE_TO_SELF가 호출자 자신의 memberId로 강제 덮어쓴다 |

### 2) POST /api/members/me/addresses — 등록

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Query | string | (강제 주입) | 목록과 동일 |
| recipient | Body | string | Y | 받는사람 이름, 1~50자 |
| phone | Body | string | Y | 휴대폰번호 원문, 1~20자, 정규화 후 `MemberRegistrationService.PHONE_PATTERN` 형식 일치 필요 |
| zipcode | Body | string | Y | 우편번호, 숫자 5자리 (`^[0-9]{5}$`) — ZIPCODES 대조는 이 API 범위 밖(FUNC-member-012 몫), 클라이언트 값을 그대로 저장 |
| roadAddress | Body | string | Y | 도로명 주소, 1~200자 |
| detailAddress | Body | string | Y | 상세 주소, 1~200자 |
| entranceMethod | Body | string | N | 공동현관 출입방법, 최대 200자 |
| deliveryMemo | Body | string | N | 배송 메모, 최대 200자 |
| isDefault | Body | boolean | N | 기본 배송지 지정 희망. 첫 등록이면 이 값과 무관하게 무조건 기본 강제 |

### 3) PUT /api/members/me/addresses/{addressId} — 수정

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Query | string | (강제 주입) | 목록과 동일 |
| addressId | Path | long | Y | 수정할 배송지 ID |
| recipient / phone / zipcode / roadAddress / detailAddress / entranceMethod / deliveryMemo | Body | (등록과 동일 타입·검증) | (필수 여부 등록과 동일) | 등록과 동일한 검증 규칙 재사용(`validateFields`) |
| isDefault | Body | boolean | - | **무시됨** — `MemberAddressService.update`는 `is_default`를 갱신하지 않는다(기본 지정은 전용 API로만) |

### 4) DELETE /api/members/me/addresses/{addressId} — 삭제

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Query | string | (강제 주입) | 목록과 동일 |
| addressId | Path | long | Y | 삭제할 배송지 ID |

본문 없음.

### 5) PUT /api/members/me/addresses/{addressId}/default — 기본 설정

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Query | string | (강제 주입) | 목록과 동일 |
| addressId | Path | long | Y | 기본으로 지정할 배송지 ID |

본문 없음.

## 응답

배송지 객체 공통 셰이프(`MemberAddress` — 단건 응답은 봉투 없이 그대로, 목록은 `{items: [...]}` 봉투):

```json
{
  "addressId": 12,
  "memberId": "M-0053",
  "recipient": "홍길동",
  "phone": "010-1234-5678",
  "phoneNorm": "01012345678",
  "zipcode": "06236",
  "roadAddress": "서울특별시 강남구 테헤란로 123",
  "detailAddress": "101동 202호",
  "entranceMethod": "공동현관 비밀번호 1234",
  "deliveryMemo": "부재 시 경비실에 맡겨주세요",
  "isDefault": "Y",
  "lastUsedAt": "2026-09-13T10:00:00",
  "createdAt": "2026-09-13T10:00:00",
  "updatedAt": "2026-09-13T10:00:00"
}
```

- `isDefault`: 이 프로젝트의 Y/N 플래그 관례(`Product.saleYn`과 동일) — boolean이 아니라 문자열 `"Y"`/`"N"` 그대로 노출된다.
- `phoneNorm`: 서버가 계산한 정규화 값(숫자만)도 `MemberAddress` getter에 `@JsonIgnore`가 없어 응답에 그대로 포함된다 — 별도 응답 DTO가 없다(컨트롤러가 도메인 객체를 직접 반환).
- `lastUsedAt`은 등록 시 NULL이 아니라 `createdAt`과 동일값으로 채워진다(사람 수정 — 목록/승계 정렬이 NULL 흔들림 없이 안정적으로 동작하게 하기 위함). 이 API 범위에서는 등록 이후 갱신되지 않는다(주문 시 갱신은 SR-255 몫).
- 목록 GET 응답: `{ "items": [ {..위 셰이프..}, ... ] }` — 0건이어도 200 + 빈 배열(404 아님).
- 등록 POST 응답: 201 + 생성된 배송지 객체(봉투 없음).
- 수정 PUT 응답: 200 + 수정된 배송지 객체(재조회 결과, 봉투 없음).
- 삭제 DELETE 응답: 204, 본문 없음.
- 기본설정 PUT 응답: 200 + 배송지 객체. 이미 기본이면 DB를 건드리지 않고 조회한 현재 상태를 그대로 반환(no-op 멱등).

## 비즈니스 규칙

> 이 섹션은 "완전한 사양"이 아니라 짧은 abstract다. 정본 진실은 frontmatter `anchors:`의 소스 —
> 여기엔 핵심 분기·코드값 의미만 적는다.

- **존재+소유 단일 SQL(존재 오라클 방지)**: `MemberAddressDao#selectOwned`가 `member_id`+`address_id`를
  한 WHERE절로 묶어 "배송지 없음"과 "남의 배송지"를 원천적으로 같은 결과(null)로 만든다 → 두 경우
  모두 동일 404 `MBR-4041`, 동일 문구. 판정을 분리하는 코드는 없다(수정·삭제·기본설정 3곳 공통, 사례집
  SR-231 r5 재발 방지).
- **회원당 최대 10개**: 등록 시 락(`selectByMemberIdForUpdate`)으로 얻은 목록 크기가 10 이상이면
  409 `MBR-4201`. 카운트는 락 이후에만 신뢰한다(동시 등록 레이스 방지).
- **기본 배송지 자동 승계**: 삭제 대상이 기본(`is_default='Y'`)이었으면, 락으로 얻은 목록(대상 제외)
  중 `last_used_at DESC, created_at DESC` 1순위가 자동으로 새 기본이 된다. 후보가 없으면(마지막 남은
  배송지 삭제) 승계 없이 그대로 기본 없음 상태로 종료.
- **첫 등록은 무조건 기본**: `existing.isEmpty()`이면 요청의 `isDefault` 값과 무관하게 기본으로
  강제한다(첫 배송지가 기본이 아닌 상태로 남는 경우를 만들지 않음).
- **`last_used_at`은 NULL 없이 등록**: 등록 시 `created_at`과 동일값으로 채운다 — 목록/승계 정렬
  키가 NULL 여부로 흔들리지 않게 하기 위한 사람 확정 결정. 주문 시점 갱신은 이 FUNC 범위 밖(SR-255).
- **락 순서 고정(1곳 공통)**: `register`/`delete`/`setDefault` 세 경로 모두 쓰기 문장 이전에
  `selectByMemberIdForUpdate`(`SELECT ... FOR UPDATE`)를 먼저 호출하는 동일 순서를 지킨다 — 두
  트랜잭션이 다른 순서로 잠그면 데드락(사례집 SR-231 r3 재발 방지).
  - `delete`는 이 락 호출 이전에 `selectOwned`로 존재+소유를 먼저 확인한다(싸게 실패). 승계 판정은
    락 이후 재조회 없이 이 락 목록(`locked`) 자체를 그대로 쓴다 — MariaDB REPEATABLE READ에서
    비잠금 재조회는 락 대기 중 커밋된 변경을 못 볼 수 있어서다(`selectNextDefaultCandidate`는 더 이상
    호출되지 않음, 매퍼 테스트용으로만 잔존).
- **URL에 memberId 없음**: `/api/members/me/addresses`는 `ApiKeyAuthFilter`의 `SCOPE_TO_SELF`
  메커니즘이 memberId를 강제한다(위 "인증·스코프" 절). admin 키로는 400으로 막힌다.
- **ZIPCODES 대조는 범위 밖**: `zipcode`/`roadAddress`/`detailAddress`는 클라이언트가 보낸 값을
  검증(형식·길이)만 하고 그대로 저장한다 — 우편번호 실재 여부 대조는 형제 FUNC-member-012 몫.
- **수정은 `isDefault`를 다루지 않음**: 필드 수정과 기본 지정을 분리된 API로 명확히 나눴다 —
  `update`가 `is_default`를 건드리면 승계 로직 없이 기본이 조용히 바뀌는 사이드이펙트가 생기므로
  의도적으로 배제.

## 트랜잭션 순서

### POST 등록 (`@Transactional`)
1. 휴대폰번호 정규화 + 필드 검증(400, DB 접근 전).
2. `MEMBER_ADDRESSES` 락 조회(`selectByMemberIdForUpdate`, 첫 문장) — 개수 확인.
3. 10개 이상이면 409, 이후 단계 없음(아직 아무것도 쓰기 전이라 롤백 불필요).
4. 기존 배송지가 있고 이번 등록이 기본이면 `clearDefaultForMember`(기존 기본 해제).
5. `MEMBER_ADDRESSES` INSERT(`last_used_at`=`created_at`=현재시각, AUTO_INCREMENT로 `addressId` 채번).

### DELETE 삭제 (`@Transactional`)
1. `selectOwned`로 존재+소유 확인(없으면 404, 이후 단계 없음).
2. `MEMBER_ADDRESSES` 락 조회(`selectByMemberIdForUpdate`, register/setDefault와 동일 지점) — 삭제
   대상의 `is_default`와 승계 후보를 이 잠금 목록에서 판정.
3. `MEMBER_ADDRESSES` UPDATE(`softDelete`, `del_yn='Y'`).
4. 삭제 대상이 기본이었으면 승계 후보에 `setDefault` UPDATE(후보 없으면 생략).

### PUT 기본설정 (`@Transactional`)
1. `selectOwned`로 존재+소유 확인(없으면 404).
2. 이미 기본이면 즉시 반환(DB 쓰기 없음, no-op 성공).
3. `MEMBER_ADDRESSES` 락 조회(`selectByMemberIdForUpdate`).
4. `clearDefaultForMember` UPDATE → `setDefault` UPDATE.

### PUT 수정 (`@Transactional`, 단일 UPDATE — 락 불필요)
1. 필드 검증(400).
2. `selectOwned`로 존재+소유 확인(없으면 404).
3. `MEMBER_ADDRESSES` UPDATE(`updateAddress`, `is_default` 제외).

### GET 목록 (비트랜잭션)
1. `selectList` 단건 SELECT(최근 사용 순, `del_yn='N'`).

## 사이드이펙트

- DELETE가 대상 배송지의 `is_default`가 `'Y'`였을 경우, 메인 목적(삭제) 외에 **다른 행**(승계 후보)의
  `is_default`를 `'Y'`로 UPDATE한다 — 호출자가 명시적으로 요청하지 않은 추가 쓰기.
- POST 등록이 기본으로 강제되는 경우(첫 등록 또는 `isDefault=true`), 기존 기본 배송지 행의
  `is_default`를 `'N'`으로 먼저 UPDATE한다(`clearDefaultForMember`) — 등록 대상 외 행 변경.
- 그 외 `CART_ITEMS`/`ORDERS`/`MEMBERS` 등 이 도메인 밖 테이블은 건드리지 않는다(FK 참조만 존재).

## 오류 응답

| 코드 | HTTP | 사유 | 발생 조건 |
|------|------|------|---------|
| MBR-4200 | 400 | 유효성 실패 | `memberId` 누락(admin 키로 /me 호출 등) / 등록·수정 필드 검증 실패(길이·형식·필수값) — 위반 필드명이 message에 포함됨 |
| MBR-4041 | 404 | 배송지를 찾을 수 없습니다 | 수정·삭제·기본설정 대상이 없거나(미존재) 다른 회원 소유(남의 것) — **두 경우 완전 동일 코드·문구**(존재 오라클 방지) |
| MBR-4201 | 409 | 배송지는 최대 10개까지 등록할 수 있습니다 | 등록 시 락 조회 결과 기존 배송지 수가 10개 이상 |
| MBR-5000 | 500 | 일시적인 오류입니다 | `DataAccessException` — 정제된 메시지만 응답, 원본(경로·SQL·커넥션 정보 포함)은 서버 로그에만(`MemberAddressExceptionHandler`, 로그에도 주소 본문·연락처는 남기지 않음) |

## 참조 테이블

- `MEMBER_ADDRESSES`

## curl 예시

```bash
# 1) 목록
curl -X GET "/api/members/me/addresses" \
  -H "X-Api-Key: <member-key>"

# 2) 등록
curl -X POST "/api/members/me/addresses" \
  -H "X-Api-Key: <member-key>" \
  -H "Content-Type: application/json" \
  -d '{"recipient":"홍길동","phone":"010-1234-5678","zipcode":"06236","roadAddress":"서울특별시 강남구 테헤란로 123","detailAddress":"101동 202호","entranceMethod":"공동현관 비밀번호 1234","deliveryMemo":"부재 시 경비실에 맡겨주세요","isDefault":true}'

# 3) 수정
curl -X PUT "/api/members/me/addresses/12" \
  -H "X-Api-Key: <member-key>" \
  -H "Content-Type: application/json" \
  -d '{"recipient":"홍길동","phone":"010-1234-5678","zipcode":"06236","roadAddress":"서울특별시 강남구 테헤란로 123","detailAddress":"103동 501호","entranceMethod":"","deliveryMemo":""}'

# 4) 삭제
curl -X DELETE "/api/members/me/addresses/12" \
  -H "X-Api-Key: <member-key>"

# 5) 기본 설정
curl -X PUT "/api/members/me/addresses/12/default" \
  -H "X-Api-Key: <member-key>"
```

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-15 | SR-235 | #2 | 배송지 CRUD API 구현 — /api/members/me/addresses 목록·등록·수정·삭제·기본 지정(회원당 최대 10개, 기본 1개, 기본 삭제 시 최근 사용 순 승계), 409 MBR-4201·404 MBR-4041(타인 소유 포함)·400 MBR-4200. v4 시절 완료분이라 이력이 빠져 있어 사후 기록(2026-09-19) | shop-api@1a0cece |
