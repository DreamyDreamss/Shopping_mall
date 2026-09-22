---
inf-id: INF-ORD-003
method: GET
path: /api/orders/
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - ORDERS
  - MEMBERS
anchors:
  - src/main/java/com/sm/lab/shop/controller/OrderController.java:21-28
  - src/main/java/com/sm/lab/shop/service/OrderService.java:30-36
  - src/main/java/com/sm/lab/shop/dao/OrderDao.java:15-17
  - src/main/resources/mapper/order.xml:8-33
---

# INF-ORD-003: GET /api/orders/ — 주문 목록 조회

> **개요:** 회원·주문상태로 필터링한 주문 목록을 페이징 조회한다. (linked_func: FUNC-order-001, LAB-101)

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/OrderController.java:21-28`

## 요청

- Method: GET
- Path: /api/orders/
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Query | string | N | 회원 ID 필터 |
| orderState | Query | string | N | 주문상태 필터 |
| page | Query | int | N | 페이지 번호(기본 1) |
| size | Query | int | N | 페이지 크기(기본 20) |

## 응답 (200 OK)

```json
{
  "totalCount": 0,
  "page": 1,
  "items": [
    {
      "orderNo": "string",
      "memberId": "string",
      "memberName": "string",
      "orderState": "PLACED | PAID | SHIPPED | PARTIAL_SHIPPED | CANCELED | DONE",
      "totalAmount": 0,
      "orderedAt": "2026-01-01T00:00:00",
      "items": null,
      "deliveries": null
    }
  ]
}
```

## 비즈니스 규칙

- `del_yn = 'N'` 상시필터 → 논리삭제된 주문 제외
- `memberId` 파라미터가 있으면 해당 회원 주문만, `orderState` 파라미터가 있으면 해당 상태만 필터(LAB-101 추가 요구사항)
- `offset = max(0, page - 1) * size`
- `ORDERS`와 `MEMBERS`를 조인해 `memberName`을 함께 반환(회원 미탈퇴 여부와 무관하게 조인만 수행)
- 목록 응답의 각 항목은 `items`/`deliveries`를 채우지 않음(상세는 [[INF-ORD-004]])

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 400 | 유효성 실패 | 파라미터 타입 오류 |
| 401 | 인증 실패 | 토큰 없음/만료 |

## 인증·인가 (SR-204 — 전역 정책)
> 이 오류 계약은 `/api/**` 전역에 적용되는 횡단 정책이다(구현: `ApiKeyAuthFilter`). 화면
> 라우트(/product/**·/cart·/order/**)는 무인증 유지. 개별 엔드포인트 INF는 이 정책을 상속한다.

| 조건 | 상태 | 본문 |
|------|------|------|
| `X-Api-Key` 미제시/무효 | 401 | `{"error":"unauthorized"}` |
| member 스코프 키로 타 회원 자원 접근(소유권 불일치) | 403 | `{"error":"forbidden"}` |
| 회원 목록·주문 export 등 관리 자원에 member 키 | 403 | admin 키 전용 |

- 키 매핑은 앱 설정 `lab.api-keys`(admin→`*`, member→회원ID). 경로 판정은 서블릿 정규화 경로
  기준(퍼센트 인코딩·matrix 파라미터 우회 차단, default-deny). 소유권은 자원의 실제 member_id를
  DB 대조(orders/{orderNo}) — 토큰 문자열 대조가 아니다(SR-204 D15).

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| ORDERS | [[SCH-ORD-004]] |
| MEMBERS | [[SCH-ORD-001]] |

## curl 예시

```bash
curl -X GET "/api/orders/?memberId=M0001&orderState=PLACED&page=1&size=20" \
  -H "Content-Type: application/json"
```
