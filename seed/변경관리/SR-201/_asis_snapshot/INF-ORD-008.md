---
inf-id: INF-ORD-008
method: GET
path: /api/products/
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - PRODUCTS
anchors:
  - src/main/java/com/sm/lab/shop/controller/ProductController.java:21-25
  - src/main/java/com/sm/lab/shop/service/ProductService.java:19-21
  - src/main/java/com/sm/lab/shop/dao/ProductDao.java:11
  - src/main/resources/mapper/product.xml:7-12
---

# INF-ORD-008: GET /api/products/ — 판매중 상품 목록 조회

> **개요:** 판매중 상태인 상품 전체를 SKU 순으로 조회한다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:21-25`

## 요청

- Method: GET
- Path: /api/products/
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| (없음) | - | - | - | - |

## 응답 (200 OK)

```json
[
  {
    "sku": "string",
    "productName": "string",
    "price": 0,
    "stockQty": 0,
    "saleYn": "Y"
  }
]
```

## 비즈니스 규칙

- `sale_yn = 'Y'` 상시필터 → 판매중 상품만 반환(단건 조회인 [[INF-ORD-009]]는 이 필터가 없음)

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 400 | 유효성 실패 | 필수 필드 누락 |
| 401 | 인증 실패 | 토큰 없음/만료 |

## 참조 테이블

| 테이블 | SCH |
|--------|-----|
| PRODUCTS | [[SCH-ORD-005]] |

## curl 예시

```bash
curl -X GET /api/products/ \
  -H "Content-Type: application/json"
```
