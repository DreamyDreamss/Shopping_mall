---
inf-id: INF-ORD-007
name: 주문 배송 목록 조회
layer: api
method: GET
path: /api/orders/{orderNo}/deliveries
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
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java:64-68
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java:127-135
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java:30,33,34-35
  - modules/shop-api/src/main/resources/mapper/order.xml:54-60,73-79,81-88
---

# INF-ORD-007: GET /api/orders/{orderNo}/deliveries — 주문 배송 목록 조회

> **개요:** 주문 1건의 배송 목록만 반환한다.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java:64-68`

## 요청

- Method: GET
- Path: /api/orders/{orderNo}/deliveries
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| orderNo | PathVariable | string | Y | 주문번호 |

## 응답 (200 OK)

```json
[
  {
    "deliveryNo": "string",
    "orderNo": "string",
    "deliveryState": "READY | SHIPPED | DELIVERED | CANCELED",
    "invoiceNo": "string",
    "shippedAt": "2026-01-01T00:00:00"
  }
]
```

## 비즈니스 규칙

- 내부적으로 [[INF-ORD-004]]와 동일한 `OrderService.detail()`을 호출한다 — `ORDER_ITEMS`(`PRODUCTS` 조인 포함)도
  함께 조회되지만 응답에는 `deliveries`만 반환된다(불필요한 내부 조회이나 기능상 영향 없음)
- 배송 목록은 `shipped_at DESC, delivery_no DESC`로 정렬(미발송 `READY` 건은 `shipped_at`이 NULL이라 하단 — LAB-113)
- 배송이 없으면 빈 배열

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 404 | 주문 없음 | orderNo로 조회된 주문이 없음 |

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
curl -X GET /api/orders/20260101-0101/deliveries \
  -H "Content-Type: application/json"
```

## 변경 이력

| 날짜 | 변경 내용 | 변경자 |
|------|---------|-------|
| 2026-08-23 | 배송 이력 정렬을 최신순으로(shipped_at DESC, 미발송 null은 하단 · tie는 delivery_no DESC) — LAB-113, linked_func: FUNC-order-006 | /sl-change --quick (auto) |
