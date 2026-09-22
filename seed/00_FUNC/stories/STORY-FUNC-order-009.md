---
story-id: STORY-FUNC-order-009
func-id: FUNC-order-009
status: Approved
domain: order
created: 2026-09-11
spec_markers: 0
sr-id: SR-230
approved_sha: 727174fe821d
---

# STORY-FUNC-order-009 — 상품 목록

## Story
상품 목록


## 변경 컨텍스트 (SR-230)
> 이 story는 변경요청 **SR-230 — SR-230** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-230/00_요구사항.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 상품 목록 화면(UIS-ORD-003, shop-api Thymeleaf)의 품절 상품 표시 문구를 '품절'에서 '일시 품절'로 바꾼다. 담기 차단 동작은 그대로다. 서버 렌더 화면이며 API·DB는 손대지 않는다. / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 조회 결과 전부(데이터 계약 불변)
- **기존 클라이언트와의 하위호환이 필요한가?** — 기존 필드명·타입·의미는 그대로 둔다(추가만)
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 기존 오류 응답 형식을 그대로 쓴다
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음
- **기존 데이터 이관·백필이 필요한가?** — 불필요(신규 데이터만)
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면이 전부: UIS-ORD-003 상품 목록
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 0건·오류·로딩 표시는 기존 규칙(UIS §5)을 그대로 따른다
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 바뀌거나 새로 생기는 상태마다 스토리를 남긴다(기존 스토리는 깨뜨리지 않는다)

## 수용 기준 (Acceptance Criteria)
- [ ] INF-ORD-008 (GET /api/products): `sale_yn = 'Y'` 상시필터 → 판매중 상품만 반환(단건 조회인 [[INF-ORD-009]]는 이 필터가 없음)
- [ ] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007] `keyword` 파라미터가 있으면 `product_name` 부분 일치(LIKE) 검색 —
- [ ] INF-ORD-008 (GET /api/products): [반영: FUNC-order-007](SR-220) `inStock=true`면 `stock_qty >= 1` 조건을 `sale_yn = 'Y'` 상시필터와

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-008: > [변경: SR-201] 2026-08-22 / INF-ORD-008: GET /api/products — 판매중 상품 목록 조회 / > **개요:** 판매중 상태인 상품 전체를 SKU 순으로 조회한다. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/ProductController.java:24-31` — [docs/05_설계서/order/INF/INF-ORD-008.md](../../05_설계서/order/INF/INF-ORD-008.md)
- **SCH** SCH-ORD-005: SCH-ORD-005: products / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-005.md](../../05_설계서/order/SCH/SCH-ORD-005.md)
- **UIS** UIS-ORD-003: UIS-ORD-003: 상품 목록 / > **근거 소스(권위):** `src/main/resources/templates/product/list.html` (Thymeleaf 서버 렌더) + / > `src/main/java/com/sm/lab/shop/controller/ProductViewController.java`. 스크린샷은 보조. / > SR-201로 신규 구현된 화면(FUNC-order-009, Done). > [반영: FUNC-order-009] SR-214(정렬 셀렉트)·
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)
- **기존 구현 파일**: modules/shop-api/src/main/java/com/sm/lab/shop/controller/ProductViewController.java, modules/shop-api/src/main/java/com/sm/lab/shop/dao/ProductDao.java, modules/shop-api/src/main/java/com/sm/lab/shop/service/ProductService.java, modules/shop-api/src/test/java/com/sm/lab/shop/controller/ProductViewControllerTest.java, modules/shop-api/src/test/java/com/sm/lab/shop/dao/ProductDaoTest.java, modules/shop-api/src/test/java/com/sm/lab/shop/service/ProductServiceTest.java

## 📏 적용 규칙 (JIT — .claude/rules)
> 이 FUNC가 건드리는 파일에 적용되는 프로젝트 규칙이다. 전문은 아래 파일을 **직접 Read**하고 지킬 것 — `must` 위반은 STEP 5.3 축 C(`rules_check.py`)가 차단한다. 정본: 워크스페이스 `.claude/rules/`(뷰어 [rules]에서 편집).

| 규칙 | severity | 파일 |
|---|---|---|
| 콘솔 출력 금지 | must | `.claude/rules/lab/no-sysout.md` |

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
