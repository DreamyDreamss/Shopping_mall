---
inf-id: INF-ORD-012
method: PATCH
path: /api/cart/items/{sku}
domain: order
domain-code: ORD
srs-f: [TBD]
screens: ["UIS-ORD-005"]
tables:
  - CART_ITEMS
  - PRODUCTS
anchors:
  - src/main/java/com/sm/lab/shop/controller/CartController.java:34-39
  - src/main/java/com/sm/lab/shop/service/CartService.java:74-84
  - src/main/java/com/sm/lab/shop/dao/CartDao.java:13-14
  - src/main/java/com/sm/lab/shop/dao/CartDao.java:28
  - src/main/java/com/sm/lab/shop/dao/ProductDao.java:15
  - src/main/resources/mapper/cart.xml:6-13
  - src/main/resources/mapper/cart.xml:38-44
  - src/main/resources/mapper/product.xml:18-22
---

# INF-ORD-012: PATCH /api/cart/items/{sku} — 장바구니 수량 변경

> **개요:** 장바구니에 담긴 품목의 수량을 변경한다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/CartController.java:34-39`

## 요청

- Method: PATCH
- Path: /api/cart/items/{sku}
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| sku | PathVariable | string | Y | 상품 SKU |
| memberId | Query | string | Y | 회원 ID |
| qty | Body | int | Y | 변경할 수량 (1 이상) |

## 응답 (200 OK)

```json
{
  "memberId": "string",
  "sku": "string",
  "productName": "string",
  "price": 0,
  "qty": 0,
  "addedAt": "2026-08-23T10:00:00",
  "lineTotal": 0
}
```

## 비즈니스 규칙

- qty < 1은 400으로 거부한다 — 삭제는 명시적 DELETE로만 수행(실수 삭제 방지)
- 요청 qty가 상품 재고를 초과하거나 상품이 품절(stock_qty=0)이면 409

## 트랜잭션 순서

1. CART_ITEMS UPDATE(qty 변경)
2. CART_ITEMS SELECT(변경 결과 반환용, 상품명·현재 단가 조인)

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 400 | 유효성 실패 | qty < 1 |
| 404 | 장바구니 품목 없음 | memberId+sku 조합의 품목 없음 |
| 404 | 상품 없음 | sku로 조회된 상품 없음 |
| 409 | 재고 초과/품절 | 요청 qty가 가용 재고 초과 또는 상품 품절 |

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| CART_ITEMS | [[SCH-ORD-006]] |
| PRODUCTS | [[SCH-ORD-005]] |

## curl 예시

```bash
curl -X PATCH "/api/cart/items/SKU001?memberId=M001" \
  -H "Content-Type: application/json" \
  -d '{"qty":3}'
```
