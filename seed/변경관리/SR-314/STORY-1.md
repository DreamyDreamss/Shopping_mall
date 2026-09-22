---
story-id: STORY-SR-314.1
item: SR-314.1
title: 카탈로그 적재 관리 명령
status: Approved
domain: product
created: 2026-09-19
spec_markers: 0
sr-id: SR-314
approved_sha: d87360fc08de
---

# STORY-SR-314.1 — 카탈로그 적재·외부 이미지 서빙 — catalog.json(kshop 실제 상품) 적재기 — 카탈로그 적재 관리 명령

## Story
카탈로그 적재·외부 이미지 서빙 — catalog.json(kshop 실제 상품) 적재기 — 카탈로그 적재 관리 명령


## 변경 컨텍스트 (SR-314)
> 이 story는 변경요청 **SR-314 — 카탈로그 적재·외부 이미지 서빙 — catalog.json(kshop 실제 상품) 적재기** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-314/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-314/02_변경명세.md`

### 확정된 요건 문답 8건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: ① 이미지 서빙 — 설정 키 shop.product-image-location(예: file:{{WS}}/_lab/catalog/images/)을 /images/products/** 에 매핑, 설정이 없거나 파일이 없으면 기존 SVG 폴백, 공개 GET. ② 매니페스트 계약 = 실제 파일 {{WS}}/_lab/catalog/catalog.json(KT알파쇼핑 수집분): 최상위 source·categories[]·brands[]·products[]; 카테고리 = {code, name, source_id?, children[]}(대분류 9 → 중분류, 코드 예 FOD·FODMT); 브랜드 = {code(KB001…), name}; 상품 = {sku(SKU-3001…), name, brand, category(중분류 코드), price, list_price|null, stock_qty, sale_yn, description, tags[], image('images/SKU-xxxx.jpg'), tv_product, free_shipping, installment_months|null, card_discount_pct|null, source_id
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 SKU-1001~1004의 값·장바구니·주문·체크아웃 흐름 불변(적재기는 이 SKU를 건드리지 않는다). 설정 키가 없으면 기동 동작이 지금과 완전히 같다(적재 없음·이미지 SVG). 기존 /images/products/*.svg 경로 유지. 기존 테스트 전부 통과.
- **기존 클라이언트와의 하위호환이 필요한가?** — 추가만 — 새 관리 명령 POST /api/admin/catalog/reload(관리자 API 키 전용)와 이미지 정적 경로. 기존 엔드포인트 계약 불변.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 관리 명령: 무키 401 · 회원 키 403(기존 인가 규약) · 매니페스트 파일 없음/파싱 실패 400 PRD-4001(메시지에 경로·사유) · 성공 200 {loaded, skipped[{sku, reason}], ignoredFields[]}. 이미지 없음은 404(정적 리소스 기본).
- **배치 실행 주기와 재실행(중복 실행) 멱등성은?** — 주기 없음 — 기동 시 1회(옵트인 설정이 있을 때) + 수동 관리 명령. 같은 매니페스트를 여러 번 적재해도 행 수·값 불변(UPSERT, 코드 기준 키: 카테고리 code·브랜드 code·상품 sku). 매니페스트에서 빠진 상품은 삭제하지 않고 sale_yn='N'으로만 내린다(주문 이력 보존).
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음 — 상품 도메인 테이블만 쓴다. 주문·장바구니는 sku 참조라 영향 없음.
- **기존 데이터 이관·백필이 필요한가?** — 스키마 변경 없음(SR-313이 만든 구조를 채운다). 데이터 적재 자체가 이 SR의 산출물이며 기존 행 백필 없음.
- **이 SR이 바꾸는 테이블을 읽는 다른 API(INF-ORD-004, INF-ORD-005, INF-ORD-006, INF-ORD-008, INF-ORD-009, INF-ORD-010, INF-ORD-011, INF-** — 코드 계약은 그대로이고 결과 데이터만 늘어난다 — 적재 후 PRODUCTS에 SKU-3001~3060이 추가되므로 목록 계열(INF-ORD-008 상품 목록, 장바구니·주문에서 상품 조회)은 더 많은 행을 돌려준다. 오류 판정(sale_yn='N' 주문 거부·재고 부족 등)의 규칙은 불변. 기존 SKU-1001~1004의 값은 적재기가 건드리지 않으므로 기존 테스트의 기대값은 그대로다. 주의: 테스트가 '전체 상품 수'를 고정값으로 단언하면 적재 후 깨질 수 있다 — 적재는 옵트인 설정이 있을 때만 일어나고 테스트 프로파일에는 설정하지 않는다.

### 구현 모듈(제약) — `shop-api` (`{{SRC_SHOP_API}}`)
이 작업 항목의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약·편성에서 사람이 지정한 값).

### 같은 SR의 다른 항목 — 먼저 끝난 항목이 만든 것을 다시 만들지 않는다
- **SR-314 #2** 카탈로그 매니페스트 적재(상품·카테고리·브랜드·이미지) — 미생성

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-314/02_변경명세.md`에서 도출)
- [ ] INF-PRD-002: `POST /api/admin/catalog/reload` 신설.
- [ ] INF-PRD-002: 인가: 무키 401, 회원 키 403(기존 인가 규약 재사용), 관리자 API 키만 허용.
- [ ] INF-PRD-002: 요청 본문 [미상](요구사항·확정 문답에 본문 스키마 명시 없음 — 매니페스트 경로는 `shop.catalog.manifest` 설정 키로 서버 측 결정되는 것으로 보이며, 요청 파라미터 여부는 [미상]).
- [ ] INF-PRD-002: 응답: 매니페스트 파일 없음/파싱 실패 시 400 `PRD-4001`(메시지에 경로·사유 포함), 성공 시 200 `{loaded, skipped[{sku, reason}], ignoredFields[]}`.

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **신규 스펙(예약 — 본문은 구현 후 역생성)**: INF-PRD-002
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
