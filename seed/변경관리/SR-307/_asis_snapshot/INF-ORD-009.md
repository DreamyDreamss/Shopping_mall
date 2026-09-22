---
inf-id: INF-ORD-009
method: GET
path: /api/products/{sku}
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - PRODUCTS
anchors:
  - src/main/java/com/sm/lab/shop/controller/ProductController.java:27-31
  - src/main/java/com/sm/lab/shop/service/ProductService.java:23-29
  - src/main/java/com/sm/lab/shop/dao/ProductDao.java:12
  - src/main/resources/mapper/product.xml:14-18
---

# INF-ORD-009: GET /api/products/{sku} — 상품 단건 조회

> **개요:** SKU로 상품 1건을 조회한다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:27-31`

## 요청

- Method: GET
- Path: /api/products/{sku}
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| sku | PathVariable | string | Y | 상품 SKU |

## 응답 (200 OK)

```json
{
  "sku": "string",
  "productName": "string",
  "price": 0,
  "stockQty": 0,
  "saleYn": "Y"
}
```

## 비즈니스 규칙

- 목록 조회([[INF-ORD-008]])와 달리 `sale_yn` 필터가 없다 — 판매종료 상품도 SKU를 알면 조회 가능

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 404 | 상품 없음 | sku로 조회된 상품이 없음 |

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| PRODUCTS | [[SCH-ORD-005]] |

## curl 예시

```bash
curl -X GET /api/products/SKU001 \
  -H "Content-Type: application/json"
```
