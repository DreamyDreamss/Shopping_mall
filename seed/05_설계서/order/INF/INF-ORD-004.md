---
inf-id: INF-ORD-004
method: GET
path: /api/orders/{orderNo}
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - ORDERS
  - MEMBERS
  - ORDER_ITEMS
  - PRODUCTS
  - ORDER_DELIVERY
anchors:
  - src/main/java/com/sm/lab/shop/controller/OrderController.java:46-50
  - src/main/java/com/sm/lab/shop/service/OrderService.java:127-135
  - src/main/java/com/sm/lab/shop/dao/OrderDao.java:30,33,35
  - src/main/resources/mapper/order.xml:54-60,73-79,83-88
---

# INF-ORD-004: GET /api/orders/{orderNo} — 주문 상세 조회

> **개요:** 주문 1건을 라인(아이템)·배송 정보를 포함해 조회한다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/OrderController.java:46-50`

## 요청

- Method: GET
- Path: /api/orders/{orderNo}
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| orderNo | PathVariable | string | Y | 주문번호 |

## 응답 (200 OK)

```json
{
  "orderNo": "string",
  "memberId": "string",
  "memberName": "string",
  "orderState": "PLACED | PAID | SHIPPED | PARTIAL_SHIPPED | CANCELED | DONE",
  "totalAmount": 0,
  "orderedAt": "2026-01-01T00:00:00",
  "items": [
    {
      "orderNo": "string",
      "lineNo": 1,
      "sku": "string",
      "productName": "string",
      "qty": 0,
      "unitPrice": 0
    }
  ],
  "deliveries": [
    {
      "deliveryNo": "string",
      "orderNo": "string",
      "deliveryState": "READY | SHIPPED | DELIVERED | CANCELED",
      "invoiceNo": "string",
      "shippedAt": "2026-01-01T00:00:00"
    }
  ]
}
```

## 비즈니스 규칙

- `del_yn = 'N'` 상시필터 → 논리삭제된 주문 조회 시 404
- `items`는 `ORDER_ITEMS`와 `PRODUCTS`를 조인해 `productName`을 함께 반환
- `deliveries`는 배송이 없으면 빈 배열

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 404 | 주문 없음 | orderNo로 조회된 주문이 없거나 삭제됨 |
| 401 | 인증 실패 | 토큰 없음/만료 |
| 403 | 인가 실패 | member 스코프 키가 타 회원 주문에 접근 |

## 인증·인가 (SR-204 — 전역 정책)
> 이 오류 계약은 `/api/**` 전역에 적용되는 횡단 정책이다(구현: `ApiKeyAuthFilter`). 화면
> 라우트(/product/**·/cart·/order/**)는 무인증 유지. 개별 엔드포인트 INF는 이 정책을 상속한다.

| 조건 | 상태 | 본문 |
|------|------|------|
| `X-Api-Key` 미제시/무효 | 401 | `{"error":"unauthorized"}` |
| member 스코프 키로 타 회원 주문 접근(소유권 불일치) | 403 | `{"error":"forbidden"}` |

- 소유권 판정은 `orderNo`로 `OrderDao.selectByOrderNo`를 조회한 실제 `member_id`와 요청 키의
  스코프를 대조한다(토큰 문자열 대조가 아님, SR-204 D15). admin 스코프(`*`) 키는 이 필터를
  통과해 컨트롤러의 404 분기까지 도달하지만, member 스코프 키는 주문이 없어도(조회 결과 null)
  소유권을 확인할 수 없으므로 컨트롤러 도달 전에 403으로 거부된다(즉 member 키로는 존재하지
  않는 orderNo도 404가 아니라 403으로 응답됨).

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| ORDERS | [[SCH-ORD-004]] |
| MEMBERS | [[SCH-ORD-001]] |
| ORDER_ITEMS | [[SCH-ORD-003]] |
| PRODUCTS | [[SCH-ORD-005]] |
| ORDER_DELIVERY | [[SCH-ORD-002]] |

## curl 예시

```bash
curl -X GET /api/orders/20260101-0101 \
  -H "Content-Type: application/json"
```
