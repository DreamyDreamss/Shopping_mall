---
inf-id: INF-ORD-005
name: 주문 생성
layer: api
method: POST
path: /api/orders
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - PRODUCTS
  - ORDERS
  - ORDER_ITEMS
  - MEMBERS
  - ORDER_DELIVERY
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java:52-56
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java:137-166
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/ProductDao.java:15-16
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java:30,33,35-37
  - modules/shop-api/src/main/resources/mapper/product.xml:18-29
  - modules/shop-api/src/main/resources/mapper/order.xml:54-60,73-98
---

# INF-ORD-005: POST /api/orders — 주문 생성

> **개요:** 요청된 상품 라인들을 검증·재고 차감한 뒤 신규 주문을 생성한다.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java:52-56`

## 요청

- Method: POST
- Path: /api/orders
- Content-Type: application/json

```json
{
  "memberId": "string",
  "items": [
    { "sku": "string", "qty": 0 }
  ]
}
```

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| memberId | Body | string | Y | 회원 ID |
| items | Body | array | Y | 주문 라인 목록(sku, qty) |

## 응답 (200 OK)

```json
{
  "orderNo": "string",
  "memberId": "string",
  "memberName": "string",
  "orderState": "PLACED",
  "totalAmount": 0,
  "orderedAt": "2026-01-01T00:00:00",
  "items": [ { "orderNo": "string", "lineNo": 1, "sku": "string", "productName": "string", "qty": 0, "unitPrice": 0 } ],
  "deliveries": []
}
```

## 비즈니스 규칙

- 각 라인의 상품이 `sale_yn = 'Y'`(판매중)가 아니면 400
- 재고 차감(`decreaseStock`)이 0행이면 재고 부족으로 409 — `WHERE stock_qty >= qty` 조건부 UPDATE로 동시성 가드
- 주문번호(`orderNo`)는 `yyyyMMdd-{4자리 시퀀스}` 형식으로 채번(`AtomicInteger`, 시작값 100 — 인스턴스 내 메모리 채번, 재기동 시 초기화)
- 신규 주문 상태는 항상 `PLACED`
- `totalAmount`는 각 라인 `단가 × 수량`의 합

## 트랜잭션 순서

1. 라인별로 `PRODUCTS` 조회 → 판매상태 검증 → 재고 조건부 차감(`UPDATE ... WHERE stock_qty >= qty`)
2. `ORDERS` INSERT (order_state='PLACED')
3. 라인별 `ORDER_ITEMS` INSERT
4. 생성된 주문을 [[INF-ORD-004]]와 동일한 조회(`selectByOrderNo` + `selectItems` + `selectDeliveries`)로 재조회하여 반환

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 400 | 판매 중인 상품이 아님 | 상품 미존재 또는 `sale_yn != 'Y'` |
| 409 | 재고 부족 | 차감 대상 수량이 현재 재고보다 많음 |

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| PRODUCTS | [[SCH-ORD-005]] |
| ORDERS | [[SCH-ORD-004]] |
| ORDER_ITEMS | [[SCH-ORD-003]] |
| MEMBERS | [[SCH-ORD-001]] |
| ORDER_DELIVERY | [[SCH-ORD-002]] |

## curl 예시

```bash
curl -X POST /api/orders \
  -H "Content-Type: application/json" \
  -d '{"memberId":"M0001","items":[{"sku":"SKU001","qty":2}]}'
```
