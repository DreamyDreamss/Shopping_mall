---
inf-id: INF-ORD-006
name: 주문 취소
layer: api
method: PATCH
path: /api/orders/{orderNo}/cancel
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
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java:59-61
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java:169-188
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java:30-38
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/ProductDao.java:15-18
  - modules/shop-api/src/main/resources/mapper/order.xml:54-60,73-79,83-88,100-107
  - modules/shop-api/src/main/resources/mapper/product.xml:32-36
---

# INF-ORD-006: PATCH /api/orders/{orderNo}/cancel — 주문 취소

> **개요:** 주문을 취소 상태로 바꾸고, 이미 취소·완료 상태이거나 출고 완료된 배송이 있으면 거부하며, 취소 성공 시 라인별 재고를 원복한다.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java:59-61`

## 요청

- Method: PATCH
- Path: /api/orders/{orderNo}/cancel
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| orderNo | PathVariable | String | Y | 취소할 주문번호 |

## 응답 (200 OK)

```json
{
  "orderNo": "20260907-0101",
  "memberId": "M0001",
  "memberName": "홍길동",
  "orderState": "CANCELED",
  "totalAmount": 45000,
  "orderedAt": "2026-09-01T10:20:30",
  "items": [
    {
      "orderNo": "20260907-0101",
      "lineNo": 1,
      "sku": "SKU-001",
      "productName": "상품A",
      "qty": 2,
      "unitPrice": 15000
    }
  ],
  "deliveries": [
    {
      "deliveryNo": "D0001",
      "orderNo": "20260907-0101",
      "deliveryState": "READY",
      "invoiceNo": null,
      "shippedAt": null
    }
  ]
}
```

취소 완료 후의 주문 상세(items·deliveries 포함)를 그대로 반환한다. `deliveries`가 없는 주문은 빈 배열.

## 비즈니스 규칙

- 주문 상태가 `CANCELED` 또는 `DONE`이면 → 409 거부(이미 종결된 주문)
- 배송 중 하나라도 `SHIPPED` 또는 `DELIVERED` 상태이면 → 409 거부(코드 `ORD-4001`, 출고 완료 배송은 취소 불가)
- 상태 전이(UPDATE)는 `order_state NOT IN ('CANCELED','DONE')` 조건부 쿼리로 실행 — 동시 요청 중 하나가 먼저 선점하면 0행이 되어 409로 거부(레이스 가드, 재고는 원복하지 않음)
- 상태 전이가 성공한 뒤에만 라인별 재고를 원복한다 (linked_func: FUNC-order-002)

## 트랜잭션 순서

1. `ORDERS`+`MEMBERS` 조회(주문 존재·현재 상태 확인) — 없으면 404
2. `ORDER_ITEMS`+`PRODUCTS`, `ORDER_DELIVERY` 조회(라인·배송 상태 확인)
3. `ORDERS.order_state`를 `CANCELED`로 조건부 UPDATE (미종결 상태일 때만, 레이스 가드)
4. 주문 라인마다 `PRODUCTS.stock_qty` 증가(재고 원복)
5. `ORDERS`+`MEMBERS`, `ORDER_ITEMS`+`PRODUCTS`, `ORDER_DELIVERY` 재조회 후 응답

## 사이드이펙트

- 취소 성공 시: 주문에 포함된 각 라인의 `sku`만큼 `PRODUCTS.stock_qty`를 증가시킨다(메인 목적인 `ORDERS` 상태 변경 외 추가 처리).

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 404 | 주문 없음 | `orderNo`에 해당하는 주문이 없음 |
| 409 | 취소 불가 상태 | 주문 상태가 이미 `CANCELED` 또는 `DONE` |
| 409 | 출고 완료 배송 존재 (ORD-4001) | 배송 중 `SHIPPED`/`DELIVERED` 상태가 하나라도 있음 |
| 409 | 취소 불가 상태(동시 처리됨) | 상태 전이 UPDATE가 0행 — 동시 요청이 먼저 취소·완료 처리함 |

## 참조 테이블

- `ORDERS`
- `MEMBERS`
- `ORDER_ITEMS`
- `PRODUCTS`
- `ORDER_DELIVERY`

## curl 예시

```bash
curl -X PATCH /api/orders/20260907-0101/cancel \
  -H "Content-Type: application/json"
```
