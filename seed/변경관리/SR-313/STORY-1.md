---
story-id: STORY-SR-313.1
item: SR-313.1
title: 카테고리 트리 조회
status: Approved
domain: product
created: 2026-09-19
spec_markers: 0
sr-id: SR-313
approved_sha: 2bb3f72d532a
---

# STORY-SR-313.1 — 카탈로그 기반 스키마 — 카테고리 3단·브랜드·상품 이미지·상품정보고시 — 카테고리 트리 조회

## Story
카탈로그 기반 스키마 — 카테고리 3단·브랜드·상품 이미지·상품정보고시 — 카테고리 트리 조회


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

### 구현 모듈(제약) — `shop-api` (`{{SRC_SHOP_API}}`)
이 작업 항목의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약·편성에서 사람이 지정한 값).

### 같은 SR의 다른 항목 — 먼저 끝난 항목이 만든 것을 다시 만들지 않는다
- **SR-313 #2** GET /api/products — 판매중 상품 목록 조회 — 미생성
- **SR-313 #3** GET /api/products/{sku} — 상품 단건 조회 — 미생성

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-313/02_변경명세.md`에서 도출)
- [ ] INF-PRD-001: `GET /api/categories` 신설.
- [ ] INF-PRD-001: 인증: 무키 GET 공개(기존 `/api/products` 공개 판정과 같은 방식 — 확정 문답 api_compat).
- [ ] INF-PRD-001: 응답: depth 1~3 카테고리 트리, 대분류 9개(의류/언더웨어·패션슈즈/잡화·화장품/이미용·가전/디지털·스포츠/레저·식품/건강·주방/생활/애견·가구/인테리어·여행/문화 — `_lab/catalog/catalog.json`의 categories와 같은 이름·순서), 각 노드에 판매중 상품 수(하위 포함) 포함.
- [ ] INF-PRD-001: 오류 경로 없음 — 빈 트리면 빈 배열(확정 문답 api_error).
- [ ] INF-PRD-001: 새 오류 코드 없음.
- [ ] INF-PRD-001: 참조 테이블: CATEGORIES(신규), 상품 수 집계에 PRODUCTS(sale_yn='Y' 필터) 조인.

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **신규 스펙(예약 — 본문은 구현 후 역생성)**: INF-PRD-001
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
(dev-agent가 생성 파일·주요 결정 기록)

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)
