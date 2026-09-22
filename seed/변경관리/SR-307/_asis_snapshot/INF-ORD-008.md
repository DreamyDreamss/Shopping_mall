---
inf-id: INF-ORD-008
method: GET
path: /api/products
domain: order
domain-code: ORD
srs-f: [TBD]
screens: []
tables:
  - PRODUCTS
anchors:
  - src/main/java/com/sm/lab/shop/controller/ProductController.java:24-31
  - src/main/java/com/sm/lab/shop/service/ProductService.java:19-22
  - src/main/java/com/sm/lab/shop/dao/ProductDao.java:11
  - src/main/resources/mapper/product.xml:7-16
---

> [변경: SR-201] 2026-08-22

# INF-ORD-008: GET /api/products — 판매중 상품 목록 조회

> **개요:** 판매중 상태인 상품 전체를 SKU 순으로 조회한다.

> **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:24-31`

## 요청

- Method: GET
- Path: /api/products
- Content-Type: application/json

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| keyword | Query | string | N | 상품명 부분 일치 검색어. 미지정 시 전체(판매중) 목록 반환(하위 호환) — [반영: FUNC-order-007] 2026-08-22 |
| inStock | Query | boolean | N | true면 재고 1 이상(`stock_qty >= 1`)인 상품만 반환. 미지정/false는 기존 동작과 동일(하위 호환) — [반영: FUNC-order-007](SR-220) 2026-09-09. non-boolean 값은 Spring 바인딩 실패로 400 |

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
- [반영: FUNC-order-007] `keyword` 파라미터가 있으면 `product_name` 부분 일치(LIKE) 검색 —
  `sale_yn = 'Y'` 상시필터와 AND 결합. `keyword` 미지정 시 기존과 동일하게 전체(판매중) 목록 반환(하위 호환,
  기존 소비처 무영향). 응답 스키마(`sku`/`productName`/`price`/`stockQty`/`saleYn`) 변경 없음 —
  "품절" 판정은 화면(UIS-ORD-003) 몫으로 `stockQty` 값을 그대로 사용.
- [반영: FUNC-order-007](SR-220) `inStock=true`면 `stock_qty >= 1` 조건을 `sale_yn = 'Y'` 상시필터와
  AND 결합해 품절 상품을 제외한다. `inStock` 미지정 또는 `false`는 기존 동작 그대로(하위 호환).
  별도 count 쿼리·페이징이 없어 목록 결과와 총 건수가 항상 일치한다. 응답 스키마 변경 없음.

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
curl -X GET /api/products \
  -H "Content-Type: application/json"
```

[반영: FUNC-order-007] `keyword` 검색 예시:

```bash
curl -X GET "/api/products?keyword=셔츠" \
  -H "Content-Type: application/json"
```

[반영: FUNC-order-007](SR-220) `inStock` 필터 예시(품절 제외):

```bash
curl -X GET "/api/products?inStock=true" \
  -H "Content-Type: application/json"
```

## 변경 이력

| 날짜 | 변경 내용 | 변경자 |
|------|---------|-------|
| 2026-08-22 | SR-201: keyword 부분일치 검색 파라미터 추가(구현 반영 — FUNC-order-007). path 표기를 실측 라우트(/api/products — 트레일링 슬래시 404)로 정정 | sl-aidd STEP 5.5 재동기화 |
| 2026-09-09 | SR-220: inStock 쿼리 파라미터 추가(재고 1 이상만 조회, 미지정 시 하위호환) — 구현 반영(FUNC-order-007) | sl-aidd STEP 5.5 재동기화 |
