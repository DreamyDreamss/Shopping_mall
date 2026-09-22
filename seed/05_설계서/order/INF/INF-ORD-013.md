---
inf-id: INF-ORD-013
method: DELETE
path: /api/cart/items/{sku}
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - CART_ITEMS
  - PRODUCTS
anchors:
  - src/main/java/com/sm/lab/shop/controller/CartController.java:41-46
  - src/main/java/com/sm/lab/shop/service/CartService.java:86-91
  - src/main/java/com/sm/lab/shop/dao/CartDao.java:13-14
  - src/main/java/com/sm/lab/shop/dao/CartDao.java:30
  - src/main/resources/mapper/cart.xml:6-13
  - src/main/resources/mapper/cart.xml:46-50
---

# INF-ORD-013: DELETE /api/cart/items/{sku} — 장바구니 품목 삭제

> **개요:** 장바구니에서 품목 1건을 삭제한다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/CartController.java:41-46`

## 요청

- Method: DELETE
- Path: /api/cart/items/{sku}
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| sku | PathVariable | string | Y | 상품 SKU |
| memberId | Query | string | Y | 회원 ID |

## 응답 (204 No Content)

응답 본문 없음.

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 404 | 장바구니 품목 없음 | memberId+sku 조합의 품목 없음 |

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| CART_ITEMS | [[SCH-ORD-006]] |
| PRODUCTS | [[SCH-ORD-005]] |

## curl 예시

```bash
curl -X DELETE "/api/cart/items/SKU001?memberId=M001" \
  -H "Content-Type: application/json"
```
