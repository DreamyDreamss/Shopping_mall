---
story-id: STORY-FUNC-order-010
func-id: FUNC-order-010
status: Done
domain: order
created: 2026-09-09
spec_markers: 0
sr-id: SR-216
approved_sha: 8e96d0b6ee63
---

# STORY-FUNC-order-010 — 상품 상세

## Story
상품 상세


## 변경 컨텍스트 (SR-225)
> 이 story는 변경요청 **SR-225 — SR-225** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-225/00_요구사항.md`

### 확정된 요건 문답 8건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 금액이 15000처럼 붙어 나와 자릿수를 잘못 읽는다. - 화면의 모든 금액을 15,000원 형식으로 표시한다 - API 응답의 숫자 타입은 그대로다(문자열로 바꾸면 계산이 깨진다) - CSV는 계산에 쓰이므로 구분 기호를 넣지 않는다 / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 조회 결과 전부(데이터 계약 불변)
- **기존 클라이언트와의 하위호환이 필요한가?** — 필드 추가·표시 변경만이라 하위호환이 유지된다. 기존 필드명·타입·의미는 그대로 둔다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 이 SR은 새 오류 계약을 만들지 않는다(요구 본문에 코드가 명시된 경우 그 코드를 따른다). 기존 오류 응답 형식·상태코드는 그대로다.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음
- **기존 데이터 이관·백필이 필요한가?** — 불필요(신규 데이터만)
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 요구 본문에 나열된 화면이 전부다. 같은 데이터를 쓰는 다른 화면은 이번 범위가 아니다.
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 빈 값은 '-'로, 조회 결과 0건은 안내 문구로 표시한다. 오류는 배너로 따로 알린다.

## 변경 컨텍스트 (SR-216)
> 이 story는 변경요청 **SR-216 — 상품 상세 품절 배지와 담기 버튼 비활성** 에서 나왔다(reopen — 이전 라운드는 SR-225, 이력은 아래 "## Dev 기록"·"## QA 결과"에 보존). 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-216/00_요구사항.md`
- **요구 요지**: 재고가 0인데 [장바구니 담기]가 눌리면 오류만 나고 사용자가 이유를 모른다 — ① 재고 0이면 상품명 옆에 '품절' 배지를 띄운다 ② 담기 버튼을 비활성화하고 이유를 옆에 적는다 ③ 서버도 재고 0이면 담기를 거부한다(화면만 막으면 API 직접 호출로 뚫린다).
- **⚠ FUNC-order-010 스코프 정정(이번 세션 확인 필수)**: SR-216의 stories는 `[FUNC-order-010, FUNC-order-011]`이다. 요구 ③(서버측 재고0 거부)은 **이미 FUNC-order-010의 기존 AC**(아래 INF-ORD-010 항목)로 구현·QA 완료돼 있다(`CartService.addItem`이 `stock_qty=0`이면 UPSERT 전 409 거부) — 이번 라운드에서 서버 로직을 다시 만들 필요는 없고, **행동 불변을 재검증**만 하면 된다. 이번 라운드에서 FUNC-order-010이 실제로 새로 만들 것은 **화면(UIS-ORD-004, product/detail.html)의 표시뿐**이다: 상품명 옆 '품절' 배지 + 담기 버튼 비활성화(+ 옆에 사유 텍스트). (현재 구현은 재고 0일 때 폼 자체를 숨기고 안내문만 보여준다 — "비활성화된 버튼을 보여주며 사유를 옆에 적는" 요건과는 다르다.)
- **다른 FUNC 금지**: FUNC-order-011(서버측 방어의 소유 FUNC일 수 있음)은 이번 작업 범위가 아니다 — 이미 구현된 CartService 로직을 재사용·재검증만 하고 코드를 건드리지 않는다.

### 확정된 요건 문답 8건 (SR-216) — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 재고가 0인데 [장바구니 담기]가 눌린다. 누르면 오류가 나서 사용자가 이유를 모른다. - 재고 0이면 상품명 옆에 '품절' 배지를 띄운다 - 담기 버튼을 비활성화하고 이유를 옆에 적는다 - 서버도 재고 0이면 담기를 거부한다(화면만 막으면 API 직접 호출로 뚫린다) / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 조회 결과·응답 형식·건수는 그대로여야 한다. 이번 변경 밖의 화면·API는 무변경.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필드 추가·표시 변경만이라 하위호환이 유지된다. 기존 필드명·타입·의미는 그대로 둔다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 이 SR은 새 오류 계약을 만들지 않는다(요구 본문에 코드가 명시된 경우 그 코드를 따른다). 기존 오류 응답 형식·상태코드는 그대로다.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음
- **기존 데이터 이관·백필이 필요한가?** — 불필요(신규 데이터만) — 스키마를 바꾸지 않는다.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면은 UIS-ORD-001, UIS-ORD-002, UIS-ORD-003, UIS-ORD-004, UIS-ORD-005(SR 원장의 일반 답변). **FUNC-order-010 소유 화면은 UIS-ORD-004(상품 상세) 1개뿐** — 이번 라운드의 실제 변경 대상은 이 화면으로 한정한다.
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 빈 값은 '-'로, 조회 결과 0건은 안내 문구로 표시한다. 오류는 배너로 따로 알린다.

## 수용 기준 (Acceptance Criteria)
- [x] INF-ORD-009 (GET /api/products/{sku}): 목록 조회([[INF-ORD-008]])와 달리 `sale_yn` 필터가 없다 — 판매종료 상품도 SKU를 알면 조회 가능 (기존 ProductController/ProductService/ProductDao — 미변경, 검증만)
- [x] INF-ORD-010 (POST /api/cart/items): 같은 상품 재담기는 DB 원자 UPSERT(`INSERT ... ON DUPLICATE KEY UPDATE qty = qty + VALUES(qty)`)로 수량 합산(PK `member_id,sku`) — 동시 담기에도 lost update·PK 중복 500 없음 (기존 CartService.addItem/CartDao.upsertMergeQty — FUNC-order-011/012 소유, 미변경·재사용만)
- [x] INF-ORD-010 (POST /api/cart/items): 판매중지(`sale_yn='N')` 상품은 담기 거부(409) (기존 CartService.requireOnSale — 미변경)
- [x] INF-ORD-010 (POST /api/cart/items): 품절(`stock_qty=0`)은 UPSERT 이전에 조기 거부(409) — 합산·원복 사이클 자체가 불필요 (기존 CartService.addItem — 미변경)
- [x] INF-ORD-010 (POST /api/cart/items): UPSERT 직후 최종(합산) qty가 재고를 초과하면, 방금 더한 만큼만 되돌리고(원복값 0 이하면 행 삭제) 409 — UPSERT가 잡은 행 잠금이 트랜잭션 종료까지 유지되어 판정·원복 사이 다른 트랜잭션 개입 불가 (기존 CartService.revertMerge — 미변경)
- [x] INF-ORD-010 (POST /api/cart/items): 담기는 재고를 차감하지 않는다(보관 전용) (기존 구현 — 미변경)
- [x] INF-ORD-011 (GET /api/cart/): 품목은 담은 순(`added_at, sku`)으로 정렬된다 (기존 CartDao.selectItems 매퍼 — 미변경, 이 화면(product/detail)에서는 직접 소비하지 않음)
- [x] INF-ORD-011 (GET /api/cart/): `totalAmount`는 각 품목의 `price × qty`(lineTotal) 합계로, 조회 시점에 애플리케이션에서 계산한다(저장 컬럼 아님) (기존 CartService.get — 미변경)

**변경(TO-BE, SR-216) — 이 SR이 UIS-ORD-004(상품 상세)에서 바꾸는 것**
- [x] 재고 0(`product.stockQty == 0`)이면 상품명 옆에 '품절' 배지(시각적으로 구분되는 표기)를 띄운다 — 현재 있는 표 안 "상태: 품절" 텍스트 행과는 별도로, 제목 라인에서 바로 보이는 배지 형태로 추가한다
- [x] 담기 버튼을 `disabled` 속성으로 비활성 표시하고 옆에 비활성 사유(예: "품절 상품은 담을 수 없습니다")를 적는다 — 현재는 재고 0일 때 폼 자체를 숨기고 안내문만 별도로 보여주는 구조이므로, 버튼이 보이되 비활성인 형태로 바꾼다
- [x] 재고 > 0일 때는 배지·비활성 문구가 나타나지 않고 기존과 동일하게 동작한다(회귀)
- [x] INF-ORD-010 (POST /api/cart/items): 서버측 `stock_qty=0` 거부(409)는 기존 CartService.addItem 로직을 그대로 재사용하며 이번 라운드에서 변경하지 않는다 — 화면만 막으면 API 직접 호출로 뚫린다는 요건이 기존 구현으로 이미 충족됨을 재검증(회귀 테스트로 확인)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-009: INF-ORD-009: GET /api/products/{sku} — 상품 단건 조회 / > **개요:** SKU로 상품 1건을 조회한다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:27-31` / 요청 — [docs/05_설계서/order/INF/INF-ORD-009.md](../../05_설계서/order/INF/INF-ORD-009.md)
- **INF** INF-ORD-010: INF-ORD-010: POST /api/cart/items — 장바구니 담기 / > **개요:** 회원 장바구니에 상품을 담는다. 같은 상품을 재담기하면 기존 수량에 합산된다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/CartController.java:22-26` / 요청 — [docs/05_설계서/order/INF/INF-ORD-010.md](../../05_설계서/order/INF/INF-ORD-010.md)
- **INF** INF-ORD-011: INF-ORD-011: GET /api/cart/ — 장바구니 조회 / > **개요:** 회원의 장바구니 품목 전체와 총액을 조회한다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/CartController.java:28-32` / 요청 — [docs/05_설계서/order/INF/INF-ORD-011.md](../../05_설계서/order/INF/INF-ORD-011.md)
- **SCH** SCH-ORD-001: SCH-ORD-001: members / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-001.md](../../05_설계서/order/SCH/SCH-ORD-001.md)
- **SCH** SCH-ORD-005: SCH-ORD-005: products / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-005.md](../../05_설계서/order/SCH/SCH-ORD-005.md)
- **SCH** SCH-ORD-006: SCH-ORD-006: cart_items / **근거 소스:** `src/main/java/com/sm/lab/shop/controller/CartController.java` + `src/main/resources/mapper/cart.xml` / 컬럼 설명 / | 컬럼명 | 타입 | NULL | 키 | 기본값 | 설명 | — [docs/05_설계서/order/SCH/SCH-ORD-006.md](../../05_설계서/order/SCH/SCH-ORD-006.md)
- **UIS** UIS-ORD-004: > [변경: SR-202] 2026-08-22 / > [변경: SR-202] 2026-08-23 — 담기 폼 반영(FUNC-order-010 r6~r7 구현 실측) / UIS-ORD-004: 상품 상세 / > **근거 소스(권위):** `src/main/resources/templates/product/detail.html` (Thymeleaf 서버 렌더) +
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)
- **기존 구현 파일**: _src/shop-api/src/main/java/com/sm/lab/shop/controller/ProductViewController.java, _src/shop-api/src/test/java/com/sm/lab/shop/controller/ProductViewControllerTest.java


## 🔧 쿼리 작성 가이드 (JIT — 실쿼리 관찰)
> AIDD로 쿼리/DAO 생성 시 준수. 소스 SQL에서 채굴한 사실(논리 FK·상시필터). 구조화 원천: `docs\05_설계서\_machine\query_patterns.json`.

**조인 경로 (논리 FK — DB 미선언이라도 코드에서 관찰됨)**
| A.컬럼 | = | B.컬럼 | 관찰 |
|--------|---|--------|------|
| CART_ITEMS.SKU | = | PRODUCTS.SKU | 4 |
| MEMBERS.MEMBER_ID | = | ORDERS.MEMBER_ID | 4 |
| ORDER_ITEMS.SKU | = | PRODUCTS.SKU | 2 |

**상시 필터 (누락하면 결과가 틀어진다 — soft-delete·테넌트 스코프)**
| 테이블 | 조건 | 빈도 |
|--------|------|------|
| MEMBERS | DEL_YN = 'N' | 4 |
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
- [x] 컨트롤러/핸들러 (기존 ProductViewController — 미변경)
- [x] 서비스/비즈니스 로직 (기존 ProductService/CartService — 미변경, FUNC-order-010은 재사용만)
- [x] 데이터 접근 레이어 (기존 ProductDao/CartDao — 미변경)
- [x] 단위 테스트 (기존 ProductViewControllerTest 18건 — 재실행 확인, PASS)

## Dev 기록
(dev-agent가 생성 파일·주요 결정 기록)

**SR-225 (금액 표기 천단위 구분) — 코드 변경 없음, 검증만 수행**

- 이번 SR-225의 실질 요구는 story 상단 "변경 컨텍스트" 절의 확정 요건 — "화면의 모든 금액을 15,000원
  형식으로 표시한다"(API 응답 숫자 타입은 유지). FUNC-order-010의 화면 범위는 UIS-ORD-004(상품 상세,
  `product/detail.html`) 1개뿐이다.
- `modules/shop-api/src/main/resources/templates/product/detail.html`의 "가격" 행을 확인한 결과,
  이미 `${#numbers.formatInteger(product.price, 3, 'COMMA')} + '원'`로 천단위 콤마 포맷이 적용돼
  있었다(예: `25,000원`) — SR-201 최초 구현 시점부터 적용된 패턴으로, 이번 SR-225 대상 요구를 신규
  변경 없이 이미 충족한다.
- `INF-ORD-009`(GET /api/products/{sku}) JSON 응답의 `price` 필드는 `Product.price`(long) 그대로
  직렬화되어 숫자 타입을 유지한다 — "API 응답의 숫자 타입은 그대로다" 요건과 배치되지 않으므로
  `ProductController`/`ProductService`/`ProductDao`는 변경하지 않았다.
- 장바구니 담기(`INF-ORD-010`)·조회(`INF-ORD-011`) 관련 수용 기준은 `CartService`/`CartDao`
  (FUNC-order-011/012 소유)가 이미 구현·검증 완료한 로직을 `ProductViewController.addToCart`가
  그대로 재사용하는 구조라, 이번 FUNC 범위에서 별도 구현·수정을 하지 않았다(다른 FUNC 소유 코드는
  건드리지 않는다는 원칙 준수).
- 검증: `./mvnw.cmd -Dtest=ProductViewControllerTest test` — Tests run: 18, Failures: 0, Errors: 0,
  BUILD SUCCESS (담기 폼·PRG 리다이렉트·404 안내·5xx 미흡수 등 기존 회귀 전부 유지 확인).
- 생성/수정 파일: 없음 (기존 구현이 SR-225 요건을 이미 충족함을 확인만 함).
- `linked_func: FUNC-order-010` 주석은 기존 `ProductViewController.java`, `product/detail.html`,
  `ProductViewControllerTest.java`에 이미 존재 — 추가 삽입 불필요.

**SR-216 (상품 상세 품절 배지 + 담기 버튼 비활성) — 화면(UIS-ORD-004)만 변경**

- 스코프: story 상단 "변경 컨텍스트 (SR-216)"의 정정에 따라, 이번 라운드에서 FUNC-order-010이 실제로
  구현할 것은 `product/detail.html` 표시뿐이다. 서버측 `stock_qty=0` 거부(409, `CartService.addItem`)는
  이미 구현·QA 완료돼 있어 변경하지 않고 재검증만 했다(요구 ③). `CartService`/`CartController`/
  `CartDao`(FUNC-order-011 소유)는 건드리지 않았다.
- `modules/shop-api/src/main/resources/templates/product/detail.html` 수정:
  - `<head><style>`에 `.badge-soldout`(빨강 배경 흰 글씨 pill), `.disabled-reason`(빨강 텍스트) 클래스 추가.
  - `<h1>` 제목 라인: 기존에는 SKU만 표시했는데, "상품명 옆에 배지"(AC1)를 title line에 두라는 요건을
    충족하기 위해 상품명(`product.productName`)도 h1에 추가하고, 그 옆에
    `th:if="${product.stockQty == 0}"`로 게이트한 `<span class="badge-soldout">품절</span>`을 넣었다.
    재고 > 0이면 이 span 자체가 렌더되지 않아 회귀(AC3)를 만족한다. 표 안의 기존 "상태" 행(SR-201)은
    그대로 두고 손대지 않았다 — 이번 배지는 그와 별개의 표기.
  - 담기 폼: 기존 `<div th:if="${product.stockQty > 0}">...폼...</div>` + 별도
    `<div th:if="${product.stockQty == 0}"><p>품절 상품은 담을 수 없습니다</p></div>` (폼 자체를
    숨기는 구조)를, **폼을 감싸던 `th:if`를 제거**하고 항상 렌더되는 하나의 `<div><form>...</form></div>`로
    바꿨다. `<select name="memberId">`, `<input name="qty">`, `<button type="submit">`에 각각
    `th:disabled="${product.stockQty == 0}"`를 추가했고(Thymeleaf 고정값 불리언 속성 → `disabled="disabled"`
    로 렌더), 버튼 바로 옆에 `th:if="${product.stockQty == 0}"`로 게이트한
    `<span class="disabled-reason">품절 상품은 담을 수 없습니다</span>`를 추가했다(AC2). 재고 > 0이면
    `th:disabled`가 `false`라 `disabled` 속성 자체가 렌더되지 않고 사유 span도 없어 기존과 동일(AC3).
  - `members` 모델 속성은 기존 `ProductViewController.loadProduct`가 `product != null`이면 재고와 무관하게
    항상 채우던 것이라(컨트롤러 미변경) 품절 상태에서도 회원 셀렉트 옵션이 그대로 렌더된다 — disabled만
    적용, 데이터 자체는 안 건드림.
  - 서버 방어 재확인(AC4): 화면의 `disabled` 속성은 브라우저 제출만 막을 뿐 API 직접 호출은 막지 않는다
    (disabled 필드는 폼 데이터에서 아예 빠짐). `CartService.addItem`이 UPSERT 이전에
    `product.getStockQty() == 0`이면 409를 던지는 기존 로직(변경 없음)이 여전히 최종 방어선임을
    `CartServiceTest`(모듈 전체 스위트에 포함, 미변경으로 그대로 통과)로 재확인했다.
- `modules/shop-api/src/test/java/com/sm/lab/shop/controller/ProductViewControllerTest.java` 수정:
  - `detail_inStock_rendersAddToCartFormWithMemberSelect`(TC-010-08)에 회귀 단언 2건 추가:
    `not(containsString("class=\"badge-soldout\""))`, `not(containsString("disabled=\"disabled\""))`.
    (주의: bare `"badge-soldout"`/`"disabled"` 문자열로 단언하면 `<style>`의 CSS 클래스 정의
    `.badge-soldout { ... }`/`.disabled-reason { ... }` 텍스트 자체에 걸려 항상 실패한다 — 속성값 형태로
    구체화해 오탐을 피함, 1차 시도에서 실측으로 확인.)
  - `detail_soldOut_hidesAddToCartFormShowsUnavailableMessage`(TC-010-09, "폼 미노출" 기대)를
    `detail_soldOut_showsDisabledAddToCartFormWithReason`으로 교체 — 폼(`name="qty"`, `name="memberId"`)이
    여전히 렌더되고 `disabled="disabled"`가 나타나며 비활성 사유 문구가 함께 있는지 확인.
  - 신규 `detail_soldOut_showsSoldOutBadgeNextToTitle`(TC-010-14) 추가 — `class="badge-soldout"` 렌더 확인(AC1).
  - `ProductController`/`ProductService`/`ProductDao`/`CartService`/`CartController`/`CartDao`는 미변경.
- 검증: `mvnw.cmd -Dtest=ProductViewControllerTest test` — Tests run: 24, Failures: 0, Errors: 0.
  이어서 `mvnw.cmd test`(shop-api 모듈 전체) — Tests run: 221, Failures: 0, Errors: 0, BUILD SUCCESS
  (CartServiceTest/CartDaoTest 등 FUNC-order-011/012 소유 회귀 포함 전부 통과 — 서버측 방어 무변경 확인).
- 생성/수정 파일:
  - `modules/shop-api/src/main/resources/templates/product/detail.html` (수정)
  - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/ProductViewControllerTest.java` (수정)
- `linked_func: FUNC-order-010` 주석은 두 파일 모두 기존에 이미 있어 유지, 추가 삽입 불필요.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-09 — CONCERNS
> SR-225 라운드. dev-agent "코드 변경 없음(기존 구현이 요건 충족)" 주장을 실측 재검증했고 **타당함을 확인**했다.

- **Layer1 스펙: concerns** (요건 충족 확인, 문서 추적성만 흠결)
  - **천단위 콤마 — 충족(실측)**: `modules/shop-api/src/main/resources/templates/product/detail.html:33`이
    `${#numbers.formatInteger(product.price, 3, 'COMMA')} + '원'`. UIS-ORD-004(이 FUNC의 유일 화면)에서
    금액 필드는 `price` 하나뿐이며(SKU·상품명·재고·상태는 금액 아님) 그 하나가 포맷돼 있다.
  - **"이미 있던 것"이라는 dev 판단 교차검증**: ① `git status --porcelain modules/` 공백,
    ② 전 템플릿 6곳(`product/list:37`, `cart/list:51,60,72`, `member/detail:47`, `order/detail:24,37`,
    `order/list:67`)이 동일한 `formatInteger(...,3,'COMMA')` 관례를 공유 — detail.html만 방금 고쳐진
    형태가 아니다. ③ 기존 테스트가 이미 `"25,000원"`을 assert(아래 Layer3). 세 근거 모두 일치.
  - **INF-ORD-009 응답 타입 불변 — 충족(실측)**: `Product.price`는 primitive `long`,
    `ProductController.get(sku)`는 `Product`를 그대로 반환 → JSON 숫자 직렬화 유지. 포맷은 뷰 계층에만
    있고 도메인·컨트롤러에 누출되지 않았다("문자열로 바꾸면 계산이 깨진다" 요건과 배치 없음).
  - **CSV 구분기호 금지 — 위반 없음**: `OrderService.exportCsv:130`이 `.append(o.getTotalAmount())`로
    raw long을 쓴다(FUNC-order-013 소유이나 SR 교차 제약이라 확인).
  - 흠결1: Dev Notes "기존 구현 파일"이 `_src/shop-api/...`를 가리키나 실제 소스 루트는
    `modules/shop-api`(project.env `SOURCE_1_PATH`). `_src/`는 `.sr-worktrees/SR-209|SR-210` 아래에만
    존재하는 stale 경로 — dev는 올바른 경로를 찾았으나 다음 세션을 오도할 수 있다.
  - 흠결2: 수용 기준 8건이 전부 SR-202 시점(INF-ORD-009/010/011 장바구니 계약)을 물려받은 것으로,
    **SR-225의 실제 요건(금액 표기)을 명시한 AC가 0건**이다. 검증은 "변경 컨텍스트" 절을 근거로
    수행할 수밖에 없었다 — AC↔SR 추적성 단절.
- **Layer2 보안: pass** — 코드 변경 0건이라 신규 공격 표면 없음. detail.html은 전부 `th:text`
  (HTML 이스케이프)로 `th:utext` 미사용 → 렌더 XSS 없음. `addToCartError`(CartService reason)도
  이스케이프 경로. `linked_func` 주석은 Thymeleaf 주석(`<!--/* */-->`)이라 렌더 본문 미노출이며
  테스트가 이를 집행(`detail_found_doesNotExposeLinkedFuncCommentInRenderedBody`).
  로그인 부재로 `memberId`를 파라미터로 받는 구조는 이 랩의 기존 설계이자 기존 GATE 기록 사항 —
  SR-225 신규 이슈 아님.
- **Layer3 회귀: pass** — 변경 0건이라 회귀 위험이 구조적으로 없고, 실측으로도 확인:
  `ProductViewControllerTest` **Tests run: 18, Failures: 0, Errors: 0**(qa가 직접 재실행,
  surefire 리포트 확인 — dev 보고 수치와 일치). 특히 `ProductViewControllerTest.java:137`이
  `containsString("25,000원")`을 이미 assert하므로 **SR-225 요건이 회귀 테스트로 보호되고 있다**
  ("변경 없음"으로 닫히는 SR이 무보호로 남는 흔한 실패에 해당하지 않음 — 신규 테스트 추가 불요).
- 권고(CONCERNS시):
  1. Dev Notes "기존 구현 파일"의 `_src/shop-api/...` → `modules/shop-api/...`로 정정
     (story 생성기 `build_story`의 소스 루트 해석이 `.sr-worktrees` 잔재를 잡는지 확인 필요).
  2. SR-225 요건을 직접 표현하는 AC 1건을 story에 추가(예: "상품 상세의 가격은 `25,000원`처럼
     천단위 구분 표기 / API `price`는 숫자 타입 유지") — 현재 AC만 보면 이번 SR의 합격 조건을 알 수 없다.
  3. 조치 불요(인지): 재고 `stockQty`는 콤마 미적용 — 금액이 아니므로 SR-225 범위 밖이 맞다.
