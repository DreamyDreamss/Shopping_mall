---
uis-id: UIS-ORD-011
name: shop_order
domain: order
domain-code: ORD
layer: ui
route: /shop/order
screens_role: 주화면
api_hints:
  - "GET /api/cart"
  - "GET /api/zipcodes"
  - "POST /api/cart/checkout"
access_control:
  - "화면 열람 자체는 세션 없으면 GET /api/cart를 호출하지 않고 \"로그인이 필요합니다\" + 로그인 링크만 표시 — 별도 인가(auth) 게이팅 없음(OrderPage.tsx:84,162-166)"
  - "[결제하기]는 checkoutInFlightRef 동기 잠금으로 연타 방지(state 지연에 의존하지 않음, OrderPage.tsx:60,114-115,138)"
  - "[결제하기]는 배송지 클라이언트 검증 통과 전에는 checkoutCart를 호출하지 않는다(API 호출 없는 1차 방어선, OrderPage.tsx:117-121)"
anchors:
  - "modules/shop-web/src/App.tsx:109"
  - "modules/shop-web/src/pages/OrderPage.tsx"
  - "modules/shop-web/src/pages/OrderPage.test.tsx"
  - "modules/shop-web/src/features/shop/DeliveryAddressForm.tsx"
  - "modules/shop-web/src/features/shop/ZipcodeSearchModal.tsx"
  - "modules/shop-web/src/features/shop/PaymentMethodSelect.tsx"
  - "modules/shop-web/src/features/shop/OrderItemsSummary.tsx"
  - "modules/shop-web/src/features/shop/OrderCompleteNotice.tsx"
  - "modules/shop-web/src/features/shop/OrderFailureNotice.tsx"
  - "modules/shop-web/src/features/shop/deliveryAddressValidation.ts"
  - "modules/shop-web/src/features/shop/cartTotals.ts"
  - "modules/shop-web/src/features/shop/httpErrorMessage.ts"
  - "modules/shop-web/src/api.ts:261-284"
revision_history:
  - "2026-09-17 골격 생성(spec_resync_check, zero-LLM)"
  - "2026-09-17 SOP급 보강(ddd-ui-agent, source-authority) — SR-305 STORY-1 round1~3 QA 재동기화 대상 반영"
---

# UIS-ORD-011: shop_order

> **근거 소스(권위):** `modules/shop-web/src/pages/OrderPage.tsx` 외 `features/shop/*`(SR-305.1
> 신규 구현). DOM 스냅샷 없음(소스폴백 모드) — 소스 슬라이스 + `docs/변경관리/SR-305/STORY-1.md`
> (확정 답변·구현 계획·QA round1~3) + `.speclinker/storybook_index.json`의 스토리 15건을 근거로 작성.

## 1. 화면 개요

- 라우트: `/shop/order` (spa-route, `modules/shop-web/src/App.tsx:109`)
- 목적: 쇼핑 장바구니(`/shop/cart`, UIS-ORD-012)에서 [주문하기]로 진입하는 **주문서·주문완료 화면**
  (SR-305 신규, 한 라우트 안에서 두 상태를 오간다). 배송지를 입력하고 우편번호를 검색해 채운 뒤,
  결제 수단(표시만)을 확인하고 [결제하기]를 누르면 기존 `POST /api/cart/checkout`을 호출해 주문을
  생성한다. 성공하면 같은 라우트 안에서 완료 상태로 전환되어 주문번호와 다음 행동(주문 내역
  보기/쇼핑 계속하기)을 보여준다. 실제 결제 연동·쿠폰·적립금·주문 취소·환불은 이번 SR 범위 밖이다.
- 진입 경로: 장바구니 화면(`/shop/cart`)의 [주문하기](전체선택 상태에서만 활성) → `navigate('/shop/order')`.
  라우터 state에는 의존하지 않는다 — **이 화면은 진입 시 자체적으로 `GET /api/cart`를 재조회**한다
  (새로고침·직접 URL 진입에도 견고, `OrderPage.tsx:1-4,62-86`).
- 접근 권한: 세션(memberId) 없으면 `GET /api/cart` 자체를 호출하지 않고 "로그인이 필요합니다" +
  로그인 링크만 표시한다(신규 신원확인 경로 없음, `OrderPage.tsx:84,162-166`). 별도 인가(auth)
  게이팅은 없다.

## 2. 화면 구성

| 블록 | 역할 | 구성 요소 | 소스 근거 |
|---|------|----------|----------|
| GNB | 검색·세션 표시·장바구니 수량 배지·로그아웃(기존 화면과 공통 재사용) | `Gnb` | `OrderPage.tsx:10,157-158` |
| 비로그인 안내 | 세션 없을 때 화면 전체를 대체 | 인라인 안내 + 로그인 링크 | `OrderPage.tsx:162-166` |
| 로딩 표시 | `GET /api/cart` 응답 전 | `role="status"` 인라인 | `OrderPage.tsx:167-168` |
| 로드 실패 안내 | `GET /api/cart` 실패 시 | `role="alert"` + [다시 시도] | `OrderPage.tsx:169-178` |
| 빈 장바구니 안내 | 조회 결과 0건(직접 URL 진입·이미 주문 완료 후 재진입 등) | 인라인 안내 + `/shop/cart` 링크 | `OrderPage.tsx:182-186` |
| 주문 상품 요약 | 현재 장바구니 전량(부분 선택 체크아웃 미지원이라 화면·서버가 항상 일치) | `OrderItemsSummary` | `OrderItemsSummary.tsx` |
| 배송지 입력 | 수령인·연락처·우편번호(읽기전용)·도로명주소(읽기전용)·상세주소 + 필드별 오류 | `DeliveryAddressForm` | `DeliveryAddressForm.tsx` |
| 우편번호 검색 모달 | [우편번호 찾기] 클릭 시 오픈, 검색어 입력 → 목록 → 선택 | `ZipcodeSearchModal` | `ZipcodeSearchModal.tsx` |
| 결제 수단 선택 | 라디오 3종(표시 전용, 항상 기본값 선택돼 있어 빈 상태 없음) | `PaymentMethodSelect` | `PaymentMethodSelect.tsx` |
| 결제 합계 | 상품금액/배송비/결제예정금액 3줄 | `<aside aria-label="결제 합계">` | `OrderPage.tsx:201-207` |
| 결제 실패 안내 | 체크아웃 실패 시 | `OrderFailureNotice` | `OrderFailureNotice.tsx` |
| [결제하기] 버튼 | 배송지 검증 → 체크아웃 실행 | `<button>` | `OrderPage.tsx:213-218` |
| 주문 완료 안내 | 체크아웃 성공 시 위 전체를 대체 | `OrderCompleteNotice` | `OrderCompleteNotice.tsx` |

> 주문 상품 요약~[결제하기]까지는 세션 있음 + 로드 성공 + 장바구니 비어있지 않음 + 체크아웃 미완료
> 상태에서만 렌더된다. 비로그인/로딩/로드실패/빈장바구니/주문완료는 서로 배타적으로 화면 전체를
> 대체한다(`OrderPage.tsx:162-220`의 분기 순서 그대로).

## 3. 입력·검증 규칙

- **배송지 필드(`deliveryAddressValidation.ts`)** — [결제하기] 클릭 시 부모(`OrderPage`)가 먼저
  호출하는 순수 함수, API 호출 없음:
  1. 수령인 미입력 → "수령인을 입력해 주세요"
  2. 연락처 미입력 → "연락처를 입력해 주세요"
  3. 우편번호 또는 도로명주소 미입력 → "우편번호 찾기로 주소를 선택해 주세요"(둘은 `ZipcodeSearchModal`
     선택으로만 함께 채워지는 한 쌍이라 하나의 오류로 묶는다 — 직접 타이핑 입력란이 아니다)
  4. 상세주소 미입력 → "상세주소를 입력해 주세요"
  검증 실패 시 필드별 오류만 세팅하고 `checkoutCart`는 호출되지 않는다. **입력값은 그대로 유지된다**
  (재입력 방지, `deliveryAddressValidation.ts:17-25`, `OrderPage.tsx:117-121`).
- **우편번호·도로명주소는 읽기전용**이다 — 직접 타이핑 불가, `[우편번호 찾기]` → `ZipcodeSearchModal`
  선택(`handleZipcodeSelect`)으로만 채워진다(`DeliveryAddressForm.tsx:46-58`, `OrderPage.tsx:102-107`).
  선택 시 zipcode 오류는 즉시 지워진다(`OrderPage.tsx:106`).
- **우편번호 검색어**: 자유 입력(`zipcodeQuery`), 형식 검증은 프런트에 없음 — 서버(`GET /api/zipcodes`,
  400 검색어 미달/초과)가 판정한다(`OrderPage.tsx:93-100`, `api.ts:276-284`).
- **연타 방지**: `checkoutInFlightRef` 동기 플래그로 [결제하기] 클릭 시점에 즉시 잠그고 요청 종료
  (`finally`) 시 해제한다. React state 반영 지연에 의존하지 않는다(`OrderPage.tsx:60,114-115,138`).
- **체크아웃 요청 파라미터는 `{memberId}`뿐** — 배송지 입력값·결제수단 선택값은 **서버로 전송되지
  않는다**. 화면에 보이는 배송지/결제수단은 표시·클라이언트 검증 목적일 뿐이며, 우편번호 검색
  자체는 서버 API(`GET /api/zipcodes`)를 재사용하지만 그 검색 결과(선택한 주소)가 서버에 저장되는
  것은 아니다(`api.ts:261-274`, `PaymentMethodSelect.tsx:1-3`). 이 사실은 화면 문구로도 숨기지 않는다
  (`PaymentMethodSelect.tsx:2-3`의 "실제 결제 연동은 하지 않습니다" 안내).

## 4. 호출 API

| 트리거 | API | 용도 | 성공 처리 | 실패 처리 |
|---|---|---|---|---|
| 화면 진입(세션 있을 때만) | `GET /api/cart` (`fetchCart`) | 장바구니 재조회 — 라우터 state 미의존 | `cartRows` 세팅 → 0건이면 빈 장바구니 안내, 1건 이상이면 주문서 렌더 | `toDisplayMessage`로 일반화한 오류 문구 + [다시 시도](`OrderPage.tsx:62-86,169-178`) |
| [우편번호 찾기] → 검색 | `GET /api/zipcodes?q=` (`searchZipcodes`) | 도로명·지번 검색 | 결과 목록 렌더(0건도 200 — "검색 결과가 없습니다") | `toDisplayMessage`로 일반화한 오류 문구(`OrderPage.tsx:93-100`, `ZipcodeSearchModal.tsx:70-73`) |
| [결제하기] 클릭 | (API 호출 없음) `validateDeliveryAddress` | 배송지 클라이언트 검증 먼저 | 통과 시에만 다음 단계(`checkoutCart`) 진행 | 실패 시 필드별 안내, 입력값 유지, API 미호출(`OrderPage.tsx:117-121`) |
| [결제하기] 클릭(검증 통과 시) | `POST /api/cart/checkout` (`checkoutCart`, 요청 바디 `{memberId}`뿐) | 주문 생성 | `checkoutResult` 세팅 → 완료 상태 전환 + `cartRows`를 빈 배열로(GNB 배지 갱신) | 400/404/409는 서버 `message` 그대로 노출, 그 외(계약 밖 5xx 등)는 일반 문구로 대체(§5, `OrderPage.tsx:124-138`, `api.ts:261-274`) |
| 로드 실패 시 [다시 시도] | `GET /api/cart` 재호출 | 복구 | 위 진입 API와 동일 | 동일 |
| [로그인 하러 가기] | (API 아님) `<a href="#/login">` | 로그인 화면 이동 | 해당 없음 | 해당 없음 |
| [장바구니로 이동](빈 장바구니) | (API 아님) `<a href="#/shop/cart">` | 장바구니 화면 이동 | 해당 없음 | 해당 없음 |
| [주문 내역 보기](완료 후) | (API 아님) `<a href="#/">` | **기존 주문 목록 화면**으로 이동 — 상세(`/orders/:orderNo`) 아님(확정 답변) | 해당 없음 | 해당 없음 |
| [쇼핑 계속하기](완료 후) | (API 아님) `<a href="#/shop/products">` | 상품 목록 이동 | 해당 없음 | 해당 없음 |

> 이 화면이 신규로 만든 서버 API는 없다 — `GET /api/cart`·`GET /api/zipcodes`·`POST /api/cart/checkout`
> 전부 기존 엔드포인트 재사용(하위호환 확정 답변, `STORY-1.md` "계약" 절). `OrderHttpError`(HTTP
> status만 보관, `code` 없음)는 서버 계약이 아니므로 INF 문서에 새 오류 봉투로 반영하지 않는다.

### 4.1 결제 합계 — 주문서·주문완료 3줄 일치 (중요, QA round1~3 재확인 대상)

주문서(결제예정금액)와 주문완료 화면은 **같은 3줄 구성**(상품금액/배송비/결제금액)을 같은 계산
로직으로 보여준다. 화면이 다르지만 서로 다른 숫자를 말하면 사용자가 "결제 전/후 금액이 다르다"고
느끼는 결함이 되므로(QA round2 FAIL 필수2 원인) 이 일치를 명시한다.

- **주문서(체크아웃 전)**: `cartTotals.calcCartTotals(cartRows 전체, 전체 sku)` → 상품금액은
  `lineTotal` 합산(서버 값 재사용, 클라이언트 재계산 금지), 배송비는 `SHIPPING_FEE_FLAT`(선택
  항목 1개 이상이면 고정 부과), 결제예정금액 = 상품금액+배송비(`OrderPage.tsx:88-91,201-207`).
- **주문완료(체크아웃 후)**: `OrderCompleteNotice`가 `cartTotals.calcTotalsFromProductAmount(
  checkoutResult.totalAmount)`로 **같은 배송비 규칙**을 적용해 같은 3줄을 재구성한다
  (`OrderCompleteNotice.tsx:12,24`, `cartTotals.ts:42-45`).
- **서버 `checkout` 응답의 `totalAmount`는 상품금액에만 대응한다** — `CartService.checkout`에는
  배송비 개념이 없다(서버 정본 필드가 아니다). 두 화면이 "상품금액" 줄에 이 값을 매핑하고,
  "배송비"·"결제금액" 줄은 **클라이언트가 계산해 붙인다**(`CheckoutResult.totalAmount` 필드
  자체 정의는 `types.ts:114-118` 참조).

## 5. 표시 조건(상태)

> 표시 조건은 이 화면 하위 부품 각각의 `.stories.tsx`로 남겨져 있다(`.speclinker/storybook_index.json`
> 기준, 이 화면(UIS-ORD-011)에 연결된 스토리 15건). 한 행 = 한 스토리.

| 요소 | 표시 조건 | 근거 | 스토리 |
|------|----------|------|--------|
| 배송지 입력 — 기본 | 초기 상태(전 필드 공란, 오류 없음) | `DeliveryAddressForm.tsx:25-68` | [기본](story:주문서-배송지-입력--기본) |
| 배송지 입력 — 입력완료 | 전 필드 유효값 채움(우편번호/도로명주소는 모달 선택 결과) | `DeliveryAddressForm.tsx:46-58` | [입력완료](story:주문서-배송지-입력--입력완료) |
| 배송지 입력 — 배송지오류 | `[결제하기]` 클릭 시 검증 실패 — 필드별 `role="alert"` 오류 문구 | `deliveryAddressValidation.ts:18-24`, `DeliveryAddressForm.tsx:35,42,57,64` | [배송지오류](story:주문서-배송지-입력--배송지오류) |
| 우편번호 검색 — 검색전 | 모달 오픈 직후, 검색어 미입력·결과 없음 | `ZipcodeSearchModal.tsx:56-73` | [검색전](story:주문서-우편번호-검색--검색전) |
| 우편번호 검색 — 결과있음 | 검색 성공, 결과 1건 이상 → 선택 가능한 목록 | `ZipcodeSearchModal.tsx:57-69` | [결과있음](story:주문서-우편번호-검색--결과있음) |
| 우편번호 검색 — 결과없음 | 검색 성공, 결과 0건 → "검색 결과가 없습니다" | `ZipcodeSearchModal.tsx:71` | [결과없음](story:주문서-우편번호-검색--결과없음) |
| 우편번호 검색 — 검색중 | `zipcodeLoading===true` → `role="status"` "검색 중…" | `ZipcodeSearchModal.tsx:53`, `OrderPage.tsx:94,99` | [검색중](story:주문서-우편번호-검색--검색중) |
| 우편번호 검색 — 조회실패 | 검색 API 실패 → `role="alert"` 오류 문구(내부 상세 문자열이면 일반 문구로 대체) | `ZipcodeSearchModal.tsx:54`, `OrderPage.tsx:98`, `httpErrorMessage.ts` | [조회실패](story:주문서-우편번호-검색--조회실패) |
| 결제 수단 — 기본선택 | 초기값 `CARD`(신용/체크카드) 선택 | `PaymentMethodSelect.tsx:4,11-15,44`, `OrderPage.tsx:44` | [기본선택](story:주문서-결제-수단--기본선택) |
| 결제 수단 — 계좌이체선택 | `BANK_TRANSFER` 선택 상태(표시만, 서버 미전송) | `PaymentMethodSelect.tsx:17-35` | [계좌이체선택](story:주문서-결제-수단--계좌이체선택) |
| 상품 요약 — 주문서기본 | 현재 장바구니 전량 읽기전용 리스트 | `OrderItemsSummary.tsx:12-26` | [주문서기본](story:주문서-상품-요약--주문서기본) |
| 결제 실패 안내 — 빈장바구니400 | 체크아웃 400(장바구니가 비어 있음) — 서버 message 그대로 | `OrderFailureNotice.tsx:31-41`, `httpErrorMessage.ts:24` | [빈장바구니400](story:주문서-결제-실패-안내--빈장바구니400) |
| 결제 실패 안내 — 회원없음404 | 체크아웃 404(회원 없음) — 서버 message 그대로 | 상동 | [회원없음404](story:주문서-결제-실패-안내--회원없음404) |
| 결제 실패 안내 — 재고부족409 | 체크아웃 409(재고 부족) — 서버 message 그대로 | 상동 | [재고부족409](story:주문서-결제-실패-안내--재고부족409) |
| 결제 실패 안내 — 일시적오류 | 계약 밖 상태(허용목록 400/404/409 밖, 예 5xx) 또는 내부 상세 문자열 혼입 — 일반 문구로 대체 | `httpErrorMessage.ts:24,28-30,38-47`, `OrderFailureNotice.tsx:32-34` | [일시적오류](story:주문서-결제-실패-안내--일시적오류) |
| 주문 완료 — 주문완료 | 체크아웃 200 성공 → 주문번호·상품금액/배송비/결제금액 3줄·[주문 내역 보기]/[쇼핑 계속하기] | `OrderCompleteNotice.tsx:23-52` | [주문완료](story:주문서-주문-완료--주문완료) |

> `OrderPage` 자체(컨테이너)는 규칙 `story-per-component` 예외(`src/pages/`)라 스토리 대상이 아니다
> — 위 상태 조합은 `OrderPage.test.tsx`(정상 진입/빈 장바구니/배송지 미입력/우편번호 선택 반영/
> 결제 성공/연타 방지/로드·검색 실패 메시지 필터/금액 표기 일치/결제 실패 3종)로 통합 검증된다.

## 6. 미확인 사항

- **배송비 정책 미확정(`SHIPPING_FEE_FLAT = 3,000원`)**: 명세·용어집·기존 코드 어디에도 배송비
  계산 규칙의 근거가 없다. "선택 항목 1개 이상이면 고정 3,000원, 없으면 0원(무료배송 기준금액
  없음)"은 **사람이 STEP 3-0 게이트에서 임시 확정한 값**이며, 실제 정책(기본요금·무료배송
  기준금액·정본 위치)은 후속 SR-308에서 확정한다(`cartTotals.ts:7-11`, `STORY-1.md` "사람 수정"·
  "후속 SR" 절). 정책이 바뀌면 이 화면과 스펙 모두 갱신이 필요하다.
- **배송지 서버 미저장**: 입력한 배송지(수령인·연락처·우편번호·도로명주소·상세주소)는 화면 표시·
  클라이언트 검증 전용이며 **어디에도 저장되지 않는다** — 체크아웃 요청 바디는 `{memberId}`뿐이고,
  `MemberAddressController`(배송지 CRUD, SR-235)는 이 SR이 쓰지 않는다. 실제로 배송지를 주문에
  반영하려면 `POST /api/cart/checkout` 계약 자체를 바꿔야 하는 **후속 SR 대상**이다
  (`STORY-1.md` "범위 밖" 절).
- **부분선택 체크아웃 미지원**: 이 화면은 항상 장바구니 전량을 주문한다(`CartService.checkout`이
  선택 여부와 무관하게 회원 장바구니 전체를 전환) — 개별 상품만 골라 결제하는 기능은 서버 계약
  변경이 필요해 이번 범위 밖이다(부분선택 차단 UI 자체는 장바구니 화면 `UIS-ORD-012` 쪽 책임).
- **in-flight 중 blur 커밋 유실(장바구니 화면 쪽 후속 TODO)**: 이 화면 자체의 결함은 아니지만
  같은 SR의 자매 화면(`/shop/cart`)에 남은 알려진 이월 항목이라 참고용으로만 남긴다
  (`STORY-1.md` "후속 추적(TODO)" 절).

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-17 | SR-305 | #1 | 장바구니·주문서 화면 신규(주문서/주문완료 부분) — 결제예정금액·완료금액 3줄 일치, checkout 바디는 {memberId}뿐, 배송지·결제수단은 표시·검증 전용(서버 미전송), 배송비 SHIPPING_FEE_FLAT=3000 임시가정(SR-308 이월) | shop-web@ca3335c |
