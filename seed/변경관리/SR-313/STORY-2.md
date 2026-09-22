---
story-id: STORY-SR-313.2
item: SR-313.2
title: GET /api/products — 판매중 상품 목록 조회
status: Approved
domain: order
created: 2026-09-19
spec_markers: 0
sr-id: SR-313
approved_sha: ccf0e30c675e
---

# STORY-SR-313.2 — 카탈로그 기반 스키마 — 카테고리 3단·브랜드·상품 이미지·상품정보고시 — GET /api/products — 판매중 상품 목록 조회

## Story
카탈로그 기반 스키마 — 카테고리 3단·브랜드·상품 이미지·상품정보고시 — GET /api/products — 판매중 상품 목록 조회


## 변경 컨텍스트 (SR-313)
> 이 story는 변경요청 **SR-313 — 카탈로그 기반 스키마 — 카테고리 3단·브랜드·상품 이미지·상품정보고시** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-313/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-313/02_변경명세.md`

### 확정된 요건 문답 7건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 요구사항 '범위' 절 그대로 — 포함: 신규 테이블 CATEGORIES(3단 트리)·BRANDS·PRODUCT_IMAGES·PRODUCT_NOTICES·PRODUCT_TAGS, PRODUCTS에 category_id·brand_id·summary·origin·created_at 추가(NULL 허용), 다음 번호 V 파일 IF NOT EXISTS 멱등 DDL(구조만, 상품 데이터 없음), GET /api/categories(트리+판매중 상품 수, 공개), 상품 목록·단건 응답에 brand·categoryPath·images·notices·tags 필드 추가. 대분류 9개는 KT알파쇼핑 카테고리 서랍 기준(_lab/catalog/catalog.json categories와 같은 이름·순서). 제외: 데이터 적재(SR-314), 필터·정렬 API(SR-315), 관리자 편집(SR-279), 옵션/단품(SR-342), TV상품·무이자·청구할인 속성(SR-318).
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 4개 상품(SKU-1001~1004)의 조회·장바구니·주문·체크아웃 흐름과 sku 키·order_items 계약 불변. 기존 상품 목록·단건 응답의 기존 필드명·타입·의미 불변(새 필드는 추가만, 값이 없으면 null/빈 배열). 기존 V 마이그레이션 파일은 수정하지 않는다.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 필드 추가만. GET /api/products·/api/products/{sku}의 기존 필드는 그대로 두고 brand{id,name}·categoryPath[]·images[]·notices[]·tags[]를 추가한다. 새 엔드포인트 GET /api/categories는 공개 GET(무키 허용, 기존 /api/products 공개 판정과 같은 방식).
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 기존 오류 봉투(code+message) 그대로. GET /api/categories는 오류 경로 없음(빈 트리면 빈 배열). 상품 단건의 404 계약 불변. 새 오류 코드 없음.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음 — 새 테이블은 상품 도메인 내부. ORDERS·ORDER_ITEMS·CART_ITEMS는 sku로만 PRODUCTS를 참조하므로 컬럼 추가의 영향이 없다. 정산·통계 집계 테이블 없음.
- **기존 데이터 이관·백필이 필요한가?** — 구조 이관만 — 기존 PRODUCTS 4행은 새 컬럼이 NULL로 남는다(카테고리·브랜드 연결은 SR-314 적재기가 채운다). 백필 없음. DDL은 멱등(CREATE TABLE IF NOT EXISTS · ALTER TABLE ADD COLUMN IF NOT EXISTS).
- **이 SR이 바꾸는 테이블을 읽는 다른 API(INF-ORD-004, INF-ORD-005, INF-ORD-006, INF-ORD-010, INF-ORD-011, INF-ORD-012, INF-ORD-013)의 결과·** — 달라지지 않는다 — PRODUCTS에는 NULL 허용 컬럼만 추가되고 기존 컬럼·행 값은 불변이다. 주문·장바구니·재고 계열 API(INF-ORD-004·005·006·010·011·012·013)는 기존 컬럼(sku·product_name·price·stock_qty·sale_yn·list_price·image_url)만 읽으므로 결과와 오류 판정이 같다. 확인 방법: 이 API들의 기존 테스트가 그대로 통과해야 한다(회귀 범위).

### 같은 SR의 다른 항목 — 먼저 끝난 항목이 만든 것을 다시 만들지 않는다
- **SR-313 #1** 카테고리 트리 조회 — Approved
- **SR-313 #3** GET /api/products/{sku} — 상품 단건 조회 — 미생성

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-313/02_변경명세.md`에서 도출)
- [ ] INF-ORD-008: 요청 파라미터 `keyword`·`inStock` 불변(추가·변경 없음).
- [ ] INF-ORD-008: 응답에 `brand`{id,name}·`categoryPath`[대,중,소]·`images`[]·`notices`[]·`tags`[] 5개 필드 추가(기존 7필드 이름·순서·타입 불변, 값이 없으면 `brand`는 null, 나머지는 빈 배열 — 확정 문답 regression_keep).
- [ ] INF-ORD-008: 인증·오류(400/401) 계약 불변, 새 오류 코드 없음.
- [ ] INF-ORD-008: 참조 테이블에 CATEGORIES·BRANDS·PRODUCT_IMAGES·PRODUCT_NOTICES·PRODUCT_TAGS 5개 신규 테이블 추가(PRODUCTS.category_id·brand_id를 통한 조회 대상).
- [ ] SCH-ORD-005: 컬럼 5개 추가(모두 NULL 허용) — `category_id`(소분류, CATEGORIES 참조)·`brand_id`(BRANDS 참조)·`summary`·`origin`·`created_at`.
- [ ] SCH-ORD-005: 기존 7컬럼의 이름·타입·NULL 여부·기본값·의미 불변(확정 문답 regression_keep).
- [ ] SCH-ORD-005: 기존 인덱스(없음)·기존 FK/관찰조인(`sku`→ORDER_ITEMS) 불변.
- [ ] SCH-ORD-005: 신규 FK(NULL 허용): `products.category_id` → CATEGORIES.category_id, `products.brand_id` → BRANDS.brand_id.
- [ ] SCH-ORD-005: 상시 필터 `SALE_YN='Y'` 불변.
- [ ] SCH-ORD-005: 기존 4행(SKU-1001~1004)은 신규 5컬럼이 NULL로 남는다(백필 없음 — 확정 문답 db_migration).
- [ ] SCH-ORD-005: DDL은 다음 번호 V 파일에 `CREATE TABLE IF NOT EXISTS`·`ALTER TABLE ADD COLUMN IF NOT EXISTS`로 멱등 추가(기존 V 파일 수정 없음).

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**
- [ ] INF-ORD-008 (GET /api/products): `sale_yn = 'Y'` 상시필터 → 판매중 상품만 반환(단건 조회인 [[INF-ORD-009]]는 이 필터가 없음)
- [ ] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007] `keyword` 파라미터가 있으면 `product_name` 부분 일치(LIKE) 검색 —
- [ ] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007](SR-220) `inStock=true`면 `stock_qty >= 1` 조건을 `sale_yn = 'Y'` 상시필터와
- [ ] INF-ORD-008 (GET /api/products): [반영: SR-306] `listPrice`·`imageUrl`은 표시 전용 — 주문·장바구니 금액 계산은 종전대로 `price`만 쓴다.

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-008: > [변경: SR-201] 2026-08-22 / INF-ORD-008: GET /api/products — 판매중 상품 목록 조회 / > **개요:** 판매중 상태인 상품 전체를 SKU 순으로 조회한다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:24-31` — [docs/05_설계서/order/INF/INF-ORD-008.md](../../05_설계서/order/INF/INF-ORD-008.md)
- **SCH** SCH-ORD-005: SCH-ORD-005: products / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-005.md](../../05_설계서/order/SCH/SCH-ORD-005.md)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)


## 🔧 쿼리 작성 가이드 (JIT — 실쿼리 관찰)
> AIDD로 쿼리/DAO 생성 시 준수. 소스 SQL에서 채굴한 사실(논리 FK·상시필터). 구조화 원천: `docs\05_설계서\_machine\query_patterns.json`.

**조인 경로 (논리 FK — DB 미선언이라도 코드에서 관찰됨)**
| A.컬럼 | = | B.컬럼 | 관찰 |
|--------|---|--------|------|
| CART_ITEMS.SKU | = | PRODUCTS.SKU | 4 |
| ORDER_ITEMS.SKU | = | PRODUCTS.SKU | 2 |

**상시 필터 (누락하면 결과가 틀어진다 — soft-delete·테넌트 스코프)**
| 테이블 | 조건 | 빈도 |
|--------|------|------|
| PRODUCTS | SALE_YN = 'Y' | 2 |

## 📖 도메인 용어 정본 (JIT — 용어집)
> 같은 대상을 새 코드명으로 만들지 말 것 — 아래가 이 도메인의 정본 용어다. 전체·확정 근거: `docs/viewer/glossary.json`(뷰어 [용어집]).
| 용어 | 정본 코드 | 정의 |
|------|----------|------|
| 가용 재고 | `STOCK_QTY` | 가용 재고 |
| 배송번호 | `DELIVERY_NO` | 배송번호 |
| 상태 | `ORDER_STATE` | 상태 (PLACED/PAID/SHIPPED/PARTIAL_SHIPPED/CANCELED/DONE) |
| 상품 SKU | `SKU` | 상품 SKU |
| 상품명 | `PRODUCT_NAME` | 상품명 |
| 주문번호 | `ORDER_NO` | 주문번호 (yyyymmdd+seq) |
| 주문일시 | `ORDERED_AT` | 주문일시 |
| 출고일시 | `SHIPPED_AT` | 출고일시 |
- ⚠ **논리 삭제**: 정본 미확정(충돌) — 코드 DEL_YN가 다른 용어(탈퇴 여부)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(장바구니 수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **장바구니 수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **주문 회원**: 정본 미확정(충돌) — 코드 MEMBER_ID가 다른 용어(회원 ID)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
(dev-agent가 생성 파일·주요 결정 기록)

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)
