---
story-id: STORY-SR-350.1
item: SR-350.1
title: 주문 목록
status: Done
domain: order
created: 2026-09-21
spec_markers: 0
sr-id: SR-350
approved_sha: a9d02e76bbf3
---

# STORY-SR-350.1 — 주문 목록 최근 30일 빠른 기간 필터 — 주문 목록

## Story
주문 목록 최근 30일 빠른 기간 필터 — 주문 목록


## 변경 컨텍스트 (SR-350)
> 이 story는 변경요청 **SR-350 — 주문 목록 최근 30일 빠른 기간 필터** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-350/00_요구사항.md`

### 확정된 요건 문답 6건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 주문 목록에서 "최근 30일" 빠른 기간 필터를 추가한다. 지금은 시작일·종료일을 직접 골라야 해서 가장 흔한 조회(최근 한 달)에 클릭이 세 번 든다. 버튼 하나로 오늘 기준 30일 범위를 채운다. / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 조회 결과 전부(데이터 계약 불변)
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면이 전부: UIS-ORD-001 주문 목록
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 결과 0건이면 "조회 결과가 없습니다", 조회 실패면 토스트로 사유와 [다시 시도], 권한 없음이면 목록 대신 안내 문구
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 모든 표시 조건(§5)을 스토리로
- **이 화면에 어디서 들어오는가? (진입점이 이번 범위에 포함되는가)** — 기존 화면에서 이동(어디서인지 명시)

## 🖼 화면 상태 (스토리) — 깨뜨리지 말 것
> 이 기능의 화면에 **이미 있는 상태 18건**이다(대상 스토리북, `.speclinker/storybook_index.json`). 게이트 축 E(`story_gate.py`)가 이것들을 실제로 렌더해 깨진 것을 잡고, 고친 부품에 스토리가 없으면 알린다(차단은 하네스 규칙 `story-per-component`가 채택돼 있을 때 축 C가 한다). 새로 만든 상태(빈 목록·오류·권한 없음 등)는 스토리로 추가하라.
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
- **UIS-ORD-001 · 주문 요약 카드** (`./src/components/OrderSummaryCard.stories.tsx`)
  - 집계있음
  - 결과없음
  - 조회실패숨김
- **UIS-ORD-001 · 주문 목록 그리드** (`./src/components/OrderTable.stories.tsx`)
  - 목록있음
  - 결과없음
  - 조회중
  - 조회실패
  - 탈퇴회원포함
  - 미지의배송코드
  - 선택행강조

## 수용 기준 (Acceptance Criteria)
- [x] INF-ORD-003 (GET /api/orders): `del_yn = 'N'` 상시필터 → 논리삭제된 주문 제외
- [x] INF-ORD-003 (GET /api/orders): `memberId` 파라미터가 있으면 해당 회원 주문만, `orderState` 파라미터가 있으면 해당 상태만 필터(LAB-101 추가 요구사항)
- [x] INF-ORD-003 (GET /api/orders): `offset = max(0, page - 1) * size`
- [x] INF-ORD-003 (GET /api/orders): `ORDERS`와 `MEMBERS`를 조인해 `memberName`을 함께 반환(회원 미탈퇴 여부와 무관하게 조인만 수행)
- [x] INF-ORD-003 (GET /api/orders): 목록 응답의 각 항목은 `items`/`deliveries`를 채우지 않음(상세는 [[INF-ORD-004]])
- [x] INF-ORD-003 (GET /api/orders): **[반영: FUNC-order-001]** 기간 필터는 `orderState`/`memberId`와 **AND**로 결합되며, `ordered_at`(DATETIME) 기준
- [x] INF-ORD-003 (GET /api/orders): **[반영: FUNC-order-001]** `startDate`/`endDate`는 각각 독립적으로 기본값이 적용된 뒤 유효 구간(`effectiveStart`~`effectiveEnd`)이

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-003: INF-ORD-003: GET /api/orders — 주문 목록 조회 / > **개요:** 회원·주문상태·조회기간으로 필터링한 주문 목록을 페이징 조회한다. (linked_func: FUNC-order-001, LAB-101) / > [반영: FUNC-order-001] SR-205 — 조회 기간(startDate/endDate) 필터 추가. / > **근거 소스:** `src/main/java/com/sm/lab/shop/controller/OrderController.java:28-44` — [docs/05_설계서/order/INF/INF-ORD-003.md](../../05_설계서/order/INF/INF-ORD-003.md)
- **SCH**: (연결 없음)
- **UIS** UIS-ORD-001: ![화면 개요](preview_annotated.png) / UIS-ORD-001: 주문 목록 / > **근거 소스(권위):** `src/main/resources/templates/order/list.html` + / > `src/main/java/com/sm/lab/shop/controller/OrderViewController.java`. 소스폴백 모드(스크린샷 없음).
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)


## 📖 도메인 용어 정본 (JIT — 용어집)
> 같은 대상을 새 코드명으로 만들지 말 것 — 아래가 이 도메인의 정본 용어다. 전체·확정 근거: `docs/viewer/glossary.json`(뷰어 [용어집]).
| 용어 | 정본 코드 | 정의 |
|------|----------|------|
| 가용 재고 | `STOCK_QTY` | 가용 재고 |
| 배송 상태 | `DELIVERY_STATE` | 배송 상태 (READY/SHIPPED/DELIVERED/CANCELED) |
| 배송번호 | `DELIVERY_NO` | 배송번호 |
| 상태 | `ORDER_STATE` | 상태 (PLACED/PAID/SHIPPED/PARTIAL_SHIPPED/CANCELED/DONE) |
| 상품 SKU | `SKU` | 상품 SKU |
| 상품명 | `PRODUCT_NAME` | 상품명 |
| 주문번호 | `ORDER_NO` | 주문번호 (yyyymmdd+seq) |
| 주문일시 | `ORDERED_AT` | 주문일시 |
- ⚠ **[변경**: 정본 미확정(충돌) — 같은 용어가 코드 IMAGE_URL · LIST_PRICE 로 갈립니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **논리 삭제**: 정본 미확정(충돌) — 코드 DEL_YN가 다른 용어(탈퇴 여부)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(장바구니 수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **장바구니 수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
(dev-agent가 생성 파일·주요 결정 기록)

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)
