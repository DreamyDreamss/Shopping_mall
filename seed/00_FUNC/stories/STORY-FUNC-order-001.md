---
story-id: STORY-FUNC-order-001
func-id: FUNC-order-001
status: Approved
domain: order
created: 2026-09-11
spec_markers: 0
sr-id: SR-228
approved_sha: 1a06edc96b7e
---

# STORY-FUNC-order-001 — 주문 목록

## Story
주문 목록


## 변경 컨텍스트 (SR-228)
> 이 story는 변경요청 **SR-228 — SR-228** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-228/00_요구사항.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 주문 목록 화면(UIS-ORD-001)의 그리드(OrderTable)에서 사용자가 클릭한 행을 강조 표시한다(selectedOrderNo). 선택 행이 없으면 기존과 같다. 선택 행 강조 상태를 화면 상태(스토리)로 남긴다. shop-web 화면만 바뀐다. / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 조회 결과 전부(데이터 계약 불변)
- **기존 클라이언트와의 하위호환이 필요한가?** — 기존 필드명·타입·의미는 그대로 둔다(추가만)
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 기존 오류 응답 형식을 그대로 쓴다
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음
- **기존 데이터 이관·백필이 필요한가?** — 불필요(신규 데이터만)
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면이 전부: UIS-ORD-001 주문 목록
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 0건·오류·로딩 표시는 기존 규칙(UIS §5)을 그대로 따른다
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 바뀌거나 새로 생기는 상태마다 스토리를 남긴다(기존 스토리는 깨뜨리지 않는다)

## 🖼 화면 상태 (스토리) — 깨뜨리지 말 것
> 이 기능의 화면에 **이미 있는 상태 14건**이다(대상 스토리북, `.speclinker/storybook_index.json`). 게이트 축 E(`story_gate.py`)가 이것들을 실제로 렌더해 깨진 것을 잡고, **고친 부품에 스토리가 없으면 FAIL**을 낸다. 새로 만든 상태(빈 목록·오류·권한 없음 등)는 스토리로 추가하라.
- **UIS-ORD-001 · 배송상태 배지** (`./src/components/DeliveryBadge.stories.tsx`)
  - 출고대기
  - 출고
  - 배송완료
  - 이력없음
- **UIS-ORD-001 · 검색 조건** (`./src/components/OrderFilters.stories.tsx`)
  - 빈조건
  - 조건입력
  - 기간역전
  - 조회중
- **UIS-ORD-001 · 주문 목록 그리드** (`./src/components/OrderTable.stories.tsx`)
  - 목록있음
  - 결과없음
  - 조회중
  - 조회실패
  - 탈퇴회원포함
  - 미지의배송코드

## 수용 기준 (Acceptance Criteria)
- [ ] INF-ORD-003 (GET /api/orders): `del_yn = 'N'` 상시필터 → 논리삭제된 주문 제외
- [ ] INF-ORD-003 (GET /api/orders): `memberId` 파라미터가 있으면 해당 회원 주문만, `orderState` 파라미터가 있으면 해당 상태만 필터(LAB-101 추가 요구사항)
- [ ] INF-ORD-003 (GET /api/orders): `offset = max(0, page - 1) * size`
- [ ] INF-ORD-003 (GET /api/orders): `ORDERS`와 `MEMBERS`를 조인해 `memberName`을 함께 반환(회원 미탈퇴 여부와 무관하게 조인만 수행)
- [ ] INF-ORD-003 (GET /api/orders): 목록 응답의 각 항목은 `items`/`deliveries`를 채우지 않음(상세는 [[INF-ORD-004]])
- [ ] INF-ORD-003 (GET /api/orders): **[반영: FUNC-order-001]** 기간 필터는 `orderState`/`memberId`와 **AND**로 결합되며, `ordered_at`(DATETIME) 기준
- [ ] INF-ORD-003 (GET /api/orders): **[반영: FUNC-order-001]** `startDate`/`endDate`는 각각 독립적으로 기본값이 적용된 뒤 유효 구간(`effectiveStart`~`effectiveEnd`)이

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-003: INF-ORD-003: GET /api/orders — 주문 목록 조회 / > **개요:** 회원·주문상태·조회기간으로 필터링한 주문 목록을 페이징 조회한다. (linked_func: FUNC-order-001, LAB-101) / > [반영: FUNC-order-001] SR-205 — 조회 기간(startDate/endDate) 필터 추가. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/OrderController.java:28-44` — [docs/05_설계서/order/INF/INF-ORD-003.md](../../05_설계서/order/INF/INF-ORD-003.md)
- **SCH** SCH-ORD-001: SCH-ORD-001: members / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-001.md](../../05_설계서/order/SCH/SCH-ORD-001.md)
- **SCH** SCH-ORD-004: SCH-ORD-004: orders / > **FUNC-ID:** [TBD] | **SRS-F:** [TBD] | **API:** [TBD] | **화면:** [TBD] / **근거 소스:** `DB 실측(db-main MCP describe)` — 컬럼 타입·NULL·기본값은 MariaDB(sl_lab) 실스키마 조회로 사실화(P1/SR-202 enrichment). [미확인] 헤더 잔존은 자기모순이라 정정(OBS-018, 2026-08-23) / 컬럼 설명 — [docs/05_설계서/order/SCH/SCH-ORD-004.md](../../05_설계서/order/SCH/SCH-ORD-004.md)
- **UIS** UIS-ORD-001: ![화면 개요](preview_annotated.png) / UIS-ORD-001: 주문 목록 / > **근거 소스(권위):** `src/main/resources/templates/order/list.html` + / > `src/main/java/com/sm/lab/shop/controller/OrderViewController.java`. 소스폴백 모드(스크린샷 없음).
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)
- **기존 구현 파일**: modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderController.java, modules/shop-api/src/main/java/com/sm/lab/shop/controller/OrderViewController.java, modules/shop-api/src/main/java/com/sm/lab/shop/dao/OrderDao.java, modules/shop-api/src/main/java/com/sm/lab/shop/service/OrderService.java, modules/shop-api/src/test/java/com/sm/lab/shop/controller/OrderControllerTest.java, modules/shop-api/src/test/java/com/sm/lab/shop/controller/OrderListEndToEndIntegrationTest.java, modules/shop-api/src/test/java/com/sm/lab/shop/controller/OrderViewControllerTest.java, modules/shop-api/src/test/java/com/sm/lab/shop/dao/OrderDaoTest.java, modules/shop-api/src/test/java/com/sm/lab/shop/service/OrderServiceTest.java

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
| MEMBERS.MEMBER_ID | = | ORDERS.MEMBER_ID | 4 |

**상시 필터 (누락하면 결과가 틀어진다 — soft-delete·테넌트 스코프)**
| 테이블 | 조건 | 빈도 |
|--------|------|------|
| ORDERS | DEL_YN = 'N' | 8 |
| MEMBERS | DEL_YN = 'N' | 4 |

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
