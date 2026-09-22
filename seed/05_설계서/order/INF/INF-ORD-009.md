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
  "saleYn": "Y",
  "listPrice": 0,
  "imageUrl": "string"
}
```

> [변경: SR-306] 2026-09-17 — `listPrice`(정가, number|null)·`imageUrl`(대표 이미지 경로, string|null) 필드
> 추가(필드 추가만, 기존 5필드 이름·순서·타입 불변, 하위호환). 값이 없으면 `null`. [[INF-ORD-008]]과 동일 계약.

## 비즈니스 규칙

- 목록 조회([[INF-ORD-008]])와 달리 `sale_yn` 필터가 없다 — 판매종료 상품도 SKU를 알면 조회 가능
- [반영: SR-306] `listPrice`·`imageUrl`은 표시 전용 — 주문·장바구니 금액 계산은 종전대로 `price`만 쓴다.

## 인증

> [변경: SR-307] 2026-09-17 — [[INF-ORD-008]]과 동일: 무키 GET은 공개(`ApiKeyAuthFilter` doFilterInternal
> 예외), 키를 보낸 요청은 종전 판정 그대로. **공개 대상 SKU 형식은 `SKU-` 접두사 + 영숫자·하이픈**
> (`^/api/products/(SKU-[A-Za-z0-9-]+)$`)으로 한정된다 — 이 형식과 맞지 않는 세그먼트(예: 미래에 추가될
> `/api/products/export` 같은 서브리소스)는 이 경로 패턴에 매치되지 않아 무키 401(fail-closed)로 남는다.

## 오류 응답

| 코드 | 사유 | 발생 조건 |
|------|------|---------|
| 401 | 인증 실패 | 키가 있으나 무효, 또는 GET 외 메서드로 무키 요청, 또는 sku가 `SKU-` 형식이 아니어서 공개 판정에 매치되지 않음(위 "인증" 절 참고) |
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

## 변경 이력(이전 기록)
| 날짜 | 변경 내용 | 변경자 |
|------|---------|-------|
| 2026-09-17 | SR-307: 무키 GET 공개(앱 서빙 /shop 401 해소) + 공개 대상 SKU 형식(`SKU-` 접두사) 명시 — 쿼리·응답 스키마 불변, "인증" 절 신설 | sl-aidd STEP 5.5 재동기화 |

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-17 | SR-307 | #1 | 무키 GET 공개 + 공개 대상 SKU 형식(SKU- 접두사) 명시 — 쿼리·응답 스키마 불변 | shop-api@cd859a1 |
| 2026-09-17 | SR-306 | #2 | 응답에 listPrice·imageUrl 필드 추가(필드 추가만, 하위호환, 값 없으면 null) | shop-api, shop-api@7eebc9f, shop-api@e1d18da, shop-web@cbe9816 |
