---
story-id: STORY-SR-305.1
item: SR-305.1
title: 쇼핑 장바구니·주문서
status: Done
domain: order
created: 2026-09-17
spec_markers: 0
sr-id: SR-305
approved_sha: b6c3ee5bca5b
---

# STORY-SR-305.1 — 장바구니·주문서 화면 신규 — 수량 변경·합계·주문 생성 — 쇼핑 장바구니·주문서

## Story
장바구니·주문서 화면 신규 — 수량 변경·합계·주문 생성 — 쇼핑 장바구니·주문서


## 변경 컨텍스트 (SR-305)
> 이 story는 변경요청 **SR-305 — 장바구니·주문서 화면 신규 — 수량 변경·합계·주문 생성** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-305/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-305/02_변경명세.md`

### 확정된 요건 문답 7건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 장바구니(/shop/cart)와 주문서(/shop/order) 화면 신규 — 장바구니(담긴 상품 목록·수량 조절·삭제·선택·합계: 상품금액/배송비/결제예정금액·[주문하기]), 주문서(배송지 입력과 우편번호 찾기·주문 상품 요약·결제 수단 선택 표시·[결제하기])와 주문 완료 화면(주문번호·주문 내역 보기). 제외: 실제 결제 연동(checkout 호출까지), 쿠폰·적립금, 주문 취소·환불 화면.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 특정 화면/API만: 기존 주문 목록·상세·로그인·비밀번호 재설정 화면과 그 동작 불변 · /api/** 요청·응답·인증(X-Api-Key)·오류 계약 불변 · 기존 Thymeleaf 화면 경로 불변. 주문 생성은 기존 POST /api/cart/checkout 계약 그대로이고 주문 완료 후 기존 주문 목록 화면으로 이어진다.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 요청·응답 형식 변경 없음. 장바구니·체크아웃·우편번호 API를 그대로 쓴다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 새 오류 코드 없음. 재고 초과·빈 장바구니·주문 실패는 서버가 주는 기존 오류 코드를 사유 문구로 표시하고 화면 상태를 유지한다(입력을 잃지 않는다).
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 추가 화면 있음(명시): 신규 2개 — 장바구니(/shop/cart), 주문서·주문완료(/shop/order).
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 빈 장바구니 → '담긴 상품이 없습니다' + 쇼핑 계속하기 · 수량 0 시도 → 1 미만 불가 안내 · 재고 초과 → 최대 수량 안내 · 주문 실패 → 사유 + 입력 유지 · 배송지 미입력 → 필드별 안내 · 주문 완료 → 주문번호와 다음 행동.
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 모든 표시 조건(§5)을 스토리로: 담긴상품있음·빈장바구니·수량변경·재고초과·선택해제·주문서기본·배송지오류·주문실패·주문완료.

### 구현 모듈(제약) — `shop-web` (`{{SRC_SHOP_WEB}}`)
이 작업 항목의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약·편성에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-305/02_변경명세.md`에서 도출)
- [x] UIS-ORD-011: 쇼핑 장바구니·주문서 — 위 SR-305 절의 요지·문답을 계약으로 신규 구현 (모듈 `shop-web` 안에)

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**

> 변경명세에 스펙 ID가 없는 절 — 이 항목 몫인지 확인해 AC로 옮긴다: SR-305 (요구사항 요지 — 전 스펙 공통)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **신규 스펙(예약 — 본문은 구현 후 역생성)**: UIS-ORD-011
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)


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

## 구현 계획

- **파일** (모두 `modules/shop-web` 안, 백엔드·DDL 변경 없음 — checkout 계약 그대로 재사용)
  - 신규 페이지(컨테이너, 스토리 대상 아님)
    - `src/pages/CartPage.tsx` — `/shop/cart`. GET `/api/cart` + 기존 `fetchProducts()`(재고·이미지 join, 신규 API 아님)로 라인아이템 구성, 선택·수량변경·삭제·[주문하기] 오케스트레이션.
    - `src/pages/OrderPage.tsx` — `/shop/order`. 진입 시 자체적으로 GET `/api/cart` 재조회(라우터 state에 의존하지 않음 — 새로고침·직접 URL 진입에도 견고), 배송지 폼·결제수단 표시·[결제하기]·성공 시 같은 경로 안에서 완료 상태로 전환(라우트 이동 없음 — 확정문답 "주문서·주문완료(/shop/order)"가 한 화면으로 명시).
  - 신규 부품(각각 `.stories.tsx` 동반, 규칙 `story-per-component`)
    - `src/features/shop/CartLineItem.tsx` — 썸네일·상품명·단가·수량 스테퍼(±/직접입력, 1~재고 클램프, `ProductInfoPanel.handleQtyChange`와 동일 클램프 규칙 재사용)·라인합계·체크박스·삭제. 상태: 기본/수량변경중(pending)/재고초과 안내/1미만 시도 안내.
    - `src/features/shop/CartEmptyState.tsx` — "담긴 상품이 없습니다" + [쇼핑 계속하기](`/shop/products`).
    - `src/features/shop/CartSummary.tsx` — 선택 항목 기준 상품금액·배송비·결제예정금액 + [주문하기](전체선택 아니면 비활성 + 안내문구).
    - `src/features/shop/DeliveryAddressForm.tsx` — 수령인/연락처/우편번호(읽기전용, 찾기버튼으로만 채움)/도로명주소(읽기전용)/상세주소 + 필드별 에러.
    - `src/features/shop/ZipcodeSearchModal.tsx` — 검색어 입력 + `GET /api/zipcodes?q=` 호출 결과 목록, 선택 시 zipcode/roadAddress 콜백.
    - `src/features/shop/PaymentMethodSelect.tsx` — 결제수단 라디오(표시만, 항상 기본값 선택돼 있어 빈 상태 없음).
    - `src/features/shop/OrderItemsSummary.tsx` — 주문 상품 요약(읽기전용 리스트, 현재 장바구니 전량).
    - `src/features/shop/OrderCompleteNotice.tsx` — 주문번호 + [주문 내역 보기](`/`, 확정답변 "기존 주문 목록 화면으로 이어진다" 그대로 — 상세 아님) + [쇼핑 계속하기].
    - `src/features/shop/OrderFailureNotice.tsx` — 서버 사유 문구 표시(사유 텍스트는 항상 서버 응답 그대로, 클라이언트가 지어내지 않음).
  - 순수 로직 + 단위테스트(프로젝트 관례: `discountRate.ts`/`productListFilters.ts`류)
    - `src/features/shop/cartTotals.ts` (+`.unit.test.ts`) — 선택 항목 기준 상품금액/배송비/결제예정금액 계산.
    - `src/features/shop/deliveryAddressValidation.ts` (+`.unit.test.ts`) — 필드별 필수값 검증(순수 함수, 서버 호출 없음).
  - 수정
    - `src/api.ts` — `fetchCart(memberId)`(GET `/api/cart`), `updateCartItemQty(memberId, sku, qty)`(PATCH `/api/cart/items/{sku}`), `deleteCartItem(memberId, sku)`(DELETE), `checkoutCart(memberId)`(POST `/api/cart/checkout`), `searchZipcodes(q)`(GET `/api/zipcodes`). 전부 기존 `OrderHttpError`/`parseOrderErrorMessage` 패턴 재사용(이 컨트롤러들은 `server.error.include-message=always`라 `{message}` 필드가 항상 오지만 `code` 필드는 없음 — `ApiError` 재사용하지 않음, `fetchProduct`/`addCartItem`과 동일 계열).
    - `src/types.ts` — `CartRow`(GET `/api/cart` 응답 1건: sku/productName/price/qty/lineTotal), `CheckoutResult`(orderNo/totalAmount/itemCount), `ZipcodeResult`(zipcode/roadAddress/sido/sigungu). 기존 `CartItem`(addCartItem 반환용)은 건드리지 않는다.
    - `src/App.tsx` — `<Route path="/shop/cart" element={<CartPage/>}/>`, `<Route path="/shop/order" element={<OrderPage/>}/>` 추가만(기존 라우트 불변).
    - `src/features/shop/Gnb.tsx` — 장바구니 아이콘을 `aria-disabled` span(title="준비 중")에서 `<a href="#/shop/cart">`(다른 링크와 동일한 `<a href="#/...">` 관례)로 바꾼다. `aria-label` 문자열은 그대로 유지(`ShopHomePage.test.tsx`/`ProductDetailPage.test.tsx`가 `getByLabelText('장바구니 N개')`로만 찾으므로 회귀 없음, 실측 확인함). **`ProductInfoPanel.tsx`/`ProductDetailPage.tsx`의 "바로 구매" 흐름은 건드리지 않는다** — `ProductDetailPage.test.tsx`(`'바로 구매 — addCartItem은 호출되지만 라우트 이동은 없다'`)가 pathname 불변을 명시적으로 게이트하는 확정된 사람 결정(SR-304 round2)이라, `/shop/cart`가 생겨도 그 버튼은 계속 비활성·비이동으로 둔다(이 SR 확정문답에도 상품상세 변경 언급 없음). Gnb 아이콘 하나가 이 SR의 유일하고 충분한 진입점이다(ShopHomePage·ProductListPage·ProductDetailPage 셋이 공유).

- **데이터**
  - 신규 테이블·DDL 없음. GET `/api/cart` 응답(`items[].{sku,productName,price,qty,lineTotal}` + `totalAmount`)과 `fetchProducts()`(`stockQty`,`imageUrl`,`saleYn`)를 sku로 클라이언트에서 join해 라인아이템을 구성한다(신규 API 금지, 확정답변 "데이터는 GET /api/cart, POST /api/cart/items, POST /api/cart/checkout, 우편번호 조회 API"에 상품/배송지 API는 없음 — `MemberAddressController`(배송지 CRUD, SR-235)는 이 SR 데이터 목록에 없으므로 **쓰지 않는다**, 우편번호 검색만 재사용).
  - 트랜잭션 경계: 전부 기존 API 호출이라 신규 트랜잭션 없음. 프론트는 각 PATCH/DELETE/POST 호출이 독립 요청이며, 체크아웃 성공 시 서버가 이미 장바구니를 비운 상태로 응답(추가 삭제 호출 불필요) — `CartService.checkout` 참고.
  - 락/원자성: 프론트 관여 없음(서버가 `selectItemsForUpdate`로 이미 처리).
  - **배송비 규칙(⚠ 명세에 근거 없음 — 사람 확인 필요)**: 스펙·용어집·기존 코드 어디에도 배송비 계산 규칙이 없다. 선택 항목이 1개 이상이면 고정 3,000원, 선택 항목이 없으면 0원으로 임시 확정한다(무료배송 기준금액 없음). `cartTotals.ts`에 상수 하나로 격리해 두어, 사람이 STEP 3-0 게이트에서 값을 바꾸거나 이 가정 자체를 거부하기 쉽게 한다.

- **순서·보안**
  1. CartPage: 세션 없으면 GET `/api/cart` 자체를 호출하지 않고 "로그인이 필요합니다" + `/login` 링크만 표시(신규 신원확인 경로 만들지 않음, 기존 페이지들과 동일 규칙).
  2. 수량 변경: 클라이언트 클램프(1~`stockQty`)가 1차 방어선(즉시 안내, API 호출 없음 — "1 미만 불가"/"최대 수량"은 우선 이 경로), PATCH가 그래도 400/409를 돌려주면(재고가 로드 이후 변한 경우) 그 응답의 `message`를 그대로 그 줄에 표시하고 값은 직전 확인된 수량으로 되돌린다(지어낸 문구로 서버 사유를 덮지 않는다, 확정답변 "서버가 주는 기존 오류 코드를 사유 문구로 표시").
  3. 삭제는 즉시 실행(별도 확인 다이얼로그 없음 — 명세에 확인 절차 요구 없음).
  4. **부분선택 체크아웃 미지원(사람 확인 필요)** — `CartService.checkout`은 선택 여부와 무관하게 회원의 장바구니 전체를 주문으로 전환한다(서버 계약 변경 금지, 새 API 금지). 따라서 "선택 해제" 상태에서 그대로 주문을 태우면 화면 표시(선택 항목만)와 실제 서버 동작(전체 항목)이 어긋난다. 이를 막기 위해 [주문하기]는 **선택 개수 === 전체 개수**일 때만 활성화하고, 부분 선택이면 "부분 선택 주문은 지원하지 않습니다 — 제외할 상품은 삭제해 주세요"로 안내한다(개별 삭제 API는 이미 있으므로 대체 경로 존재). 합계(상품금액/배송비/결제예정금액) 표시는 선택 기준으로 계속 미리보기를 보여준다.
  5. OrderPage 진입 시 GET `/api/cart`가 빈 배열이면(직접 URL 진입·이미 주문 완료 후 재진입 등) "주문할 상품이 없습니다" + `/shop/cart` 링크로 막는다(빈 주문서를 그대로 제출시키지 않음).
  6. [결제하기] 클릭: 배송지 필드 클라이언트 검증(순수 함수 `deliveryAddressValidation.ts`) 먼저 → 실패 시 필드별 안내, API 호출 없음. 통과 시에만 `checkoutCart(memberId)` 호출(요청 바디는 여전히 `{memberId}`뿐 — 입력한 배송지·결제수단은 서버로 전송되지 않는다, 계약 변경 금지 확정답변 때문. 이 SR 범위에서 배송지는 표시·검증 목적만이라는 것을 화면 라벨/문구로 숨기지 않는다).
  7. 체크아웃 실패(400 빈 장바구니 / 404 회원없음 / 409 재고부족) → 서버 `message` 그대로 `OrderFailureNotice`로 표시, 배송지 입력값은 그대로 유지(재입력 방지, 확정답변 "입력을 잃지 않는다").
  8. 연타 방지: [주문하기]/[결제하기]/수량 ±버튼 전부 `ProductDetailPage.inFlightRef`와 동일한 동기 ref 잠금(클릭 시점 즉시 true) 사용 — state 지연에 의존하지 않는다.

- **계약**: 새 오류 코드·새 요청/응답 필드 없음(확정답변). 기존 4개 API(GET/PATCH/DELETE `/api/cart*`, POST `/api/cart/checkout`, GET `/api/zipcodes`)를 그대로 소비만 한다.

- **테스트**
  - `CartPage.test.tsx`: 담긴상품 있음 렌더(썸네일·수량·합계) · 빈 장바구니 안내 · 수량 증가/감소가 PATCH 호출 후 값 반영 · 재고 초과 시 클램프+안내(그리고 서버 409 응답을 강제로 준 케이스도 별도로) · 삭제 클릭 시 DELETE 호출+행 제거 · 선택 해제 시 합계 재계산+[주문하기] 비활성+부분선택 안내 · 로그인 안 된 상태에서 GET `/api/cart` 호출 자체가 없음(fetchMock 미호출 단언) · 전체선택 상태에서 [주문하기] 클릭 시 `/shop/order`로 navigate(상태 없이).
  - `OrderPage.test.tsx`: 정상 진입(장바구니 요약 렌더) · 빈 장바구니로 진입 시 안내+링크(체크아웃 API 미호출 단언) · 배송지 미입력 상태에서 [결제하기] 클릭 시 필드별 안내(체크아웃 API 미호출 단언, 정확한 실패 재현 — SR-302 r1 사례처럼 가드 없앤 상태로 먼저 실패 확인 후 복원) · 우편번호 찾기 모달에서 항목 선택 시 zipcode/roadAddress 필드 반영 · 결제하기 성공 시 완료 상태(주문번호·[주문 내역 보기]가 `/`로 이동·[쇼핑 계속하기]) · 결제하기 400/404/409 각각 서버 message 그대로 노출 + 입력값(수령인 등) 보존 확인.
  - `cartTotals.unit.test.ts`: 선택 0/일부/전체 각각의 상품금액·배송비·결제예정금액 경계값.
  - `deliveryAddressValidation.unit.test.ts`: 각 필드 개별 누락 케이스가 그 필드만의 오류를 반환하는지.
  - Gnb 변경 후 `ShopHomePage.test.tsx`/`ProductListPage.test.tsx`/`ProductDetailPage.test.tsx` **전체 재실행**(회귀, SR-307 #1 사례 — 계획 단계에서 범위를 좁히지 않는다) — 특히 `'바로 구매 — addCartItem은 호출되지만 라우트 이동은 없다'` 케이스가 여전히 통과하는지 명시적으로 확인.
  - 최종 확인은 항상 `npm test`(타입체크 겸 jest) + `npm run test-storybook` 전체 스위트로 한다(부분 실행 금지, SR-307 #1).

- **테스트 격리**: 테스트마다 `memberId`/`sku`에 인덱스 접미(`m-cart-1`, `sku-cart-1` 등, `ProductDetailPage.test.tsx` 관례) 사용. `beforeEach`에서 `localStorage.clear()` + fetchMock 재생성, `afterEach`에서 `localStorage.clear()`(세션이 다음 테스트로 새지 않게, SR-232 r2 사례).

- **폴백·우회 경로의 자격 판정**: 이 SR은 새 인증·조회 경로를 열지 않는다(기존 4 API + 우편번호 API 그대로 소비). CartPage/OrderPage 모두 "세션의 memberId 그대로만" 쓰고(신규 신원확인 경로 없음), 세션이 없으면 API 자체를 호출하지 않는다(기존 페이지들과 동일 규칙).

- **프레임워크 실행 모델 함정**: React 19 StrictMode(dev)의 effect 이중 실행이 CartPage/OrderPage의 마운트 시 GET 호출(장바구니·상품 join)을 두 번 낼 수 있다 — `ProductListPage.inFlightKeyRef`+`requestIdRef` 조합(같은 파라미터 재요청 억제 + 응답 적용 순서 세대 카운터)을 그대로 재사용한다. 체크아웃(POST)은 effect가 아니라 사용자 클릭에서만 실행되므로 이중 마운트로 저절로 두 번 나가는 위험은 없고, 사용자 더블클릭만 `inFlightRef` 동기 잠금으로 막는다(SR-302 #1 방식, act() 한 스코프로 검증).

- **범위 밖**
  - 실제 결제 연동, 쿠폰·적립금, 주문 취소·환불 화면(확정답변 명시 제외).
  - 부분 선택 체크아웃(품목 단위 주문) — 서버가 전체 장바구니만 지원, 새 API 추가는 하위호환 확정답변 위반. 필요해지면 후속 SR에서 `POST /api/cart/checkout`에 skus 파라미터를 추가하는 계약 변경으로 논의.
  - 배송지 서버 저장(주소록 등록) — 확정답변 데이터 목록에 `MemberAddressController` 없음. 입력한 배송지는 화면 표시·클라이언트 검증용일 뿐 어디에도 저장되지 않는다(체크아웃 요청 바디는 `{memberId}`뿐). 실제로 배송지를 주문에 반영하려면 `POST /api/cart/checkout` 계약 자체를 바꿔야 하는 후속 SR 대상.
  - 배송비 규칙의 실제 정책값 확정(위 "데이터" 절 가정) — 후속에서 정책 담당자 확인 필요.

- **실패 사례집 대조** (`harness/antipatterns.all.md`)
  - SR-303 "신규 화면을 만들면서 그 화면 링크를 범위 밖으로 미뤄 아무도 도달 못함" — 조건 성립(신규 화면 2개, 기존 Gnb 장바구니 아이콘이 `title="준비 중"`으로 이미 비활성 상태로 대기 중이었음, 실측 확인). 이 SR에서 Gnb 아이콘을 실제 링크로 바꿔 닫는다.
  - SR-307 #1 "구현계획이 회귀 확인 범위를 좁혀 전체 스위트가 잡는 기존 실패를 못 봄" — 조건 성립 가능(Gnb는 3개 화면이 공유). 위 "테스트" 절에서 전체 스위트 재실행을 명시했다.
  - SR-302 #1 "연타 방지 테스트를 분리 클릭으로 작성해 거짓 보증" — 조건 성립(수량 스테퍼·주문 버튼 모두 클릭 즉시 DOM/비활성 변화 가능) → act() 한 스코프 묶음 + 가드 임시 제거 재현으로 검증.
  - SR-231 r2 "실패 경로에 남아야 하는 카운터를 트랜잭션에 넣지 말라" — 조건 불성립(이 SR은 신규 카운터·트랜잭션을 만들지 않음, 전부 기존 API 재사용).
  - SR-234 r1 "미정의 코드를 default 분기로 뭉쳐 잘못된 전이" — 조건 성립 가능성 있음(체크아웃 400/404/409 외 5xx 등) → `OrderFailureNotice`는 계약표에 있는 상태만 명시 분기하고 그 외는 "일시적 오류, 다시 시도"로 별도 처리(현재 화면 유지, 새 전이 없음).
  - SR-303 "카테고리 라벨을 keyword로 넘겨 축이 달라 전부 빈 결과" — 조건 불성립(이 SR은 서버 파라미터를 새로 만들지 않음).

### 사람 수정 (STEP 3-0 게이트 회신, 2026-09-17 — "계획대로 진행" + 아래 반영)
- 배송비 상수는 `cartTotals.ts`에 이름 있는 상수(`SHIPPING_FEE_FLAT`)로 두고, 그 선언 옆 주석에 "정책 미확정 — SR-308에서 확정"을 적는다.
- UIS 스펙 재동기화(STEP 5.5) 때 합계 규칙 절에 배송비 기준이 임시 가정임을 한 줄 명시한다.
- 부분선택 차단 안내는 상시 노출이 아니라 부분선택 상태에서만 노출하고, 비활성 [주문하기]의 사유를 `aria-describedby`로 연결해 보조기기에도 전달한다.
- Gnb 장바구니 아이콘의 실제 링크 전환(신규 화면 진입점 확보)은 반드시 포함한다.
- 보안 순서: 비로그인 상태에서 GET `/api/cart`를 아예 호출하지 않는다. 서버 `message`를 그대로 표시하되 스택트레이스·내부 경로가 섞인 문자열은 표시하지 않는다. 수량·금액은 서버 응답을 정본으로 다시 그린다(클라이언트 계산값을 서버 응답 대신 남기지 않는다).
- 테스트: 빈 장바구니·수량 클램프·부분선택 차단·배송지 검증 실패·결제 실패·주문 완료 상태를 각각 스토리로 만든다(오류 표시 스토리는 `shows-error` 태그). 회귀는 축소 없이 shop-web 전체 스위트 + 기존 쇼핑 화면 스토리 렌더 스윕까지 재실행한다.
- 후속 SR: SR-308(배송비 정책 확정 — 기본요금·무료배송 기준금액·정본 위치). 부분선택 체크아웃(품목 단위 주문)은 서버 계약 변경이 필요해 이번 범위 밖 — 필요해지면 별도 SR.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## 후속 추적(TODO)
- **in-flight 중 blur 커밋이 안내 없이 버려짐** (round 2 QA CONCERNS 권고3, round 3 재작업 지시 3 — 이번에는 고치지 않음).
  `CartPage.tsx`의 `handleQtyChange` 맨 앞 가드 `if (inFlightSkusRef.current.has(sku)) return`(현재 `:110` 부근)가
  원인. 재현 조건: 어떤 sku의 PATCH가 in-flight인 동안(스테퍼 ± 클릭 또는 이전 직접입력 커밋으로) 같은 줄
  입력란에 다른 값을 입력하고 blur(또는 Enter)하면, `CartLineItem`은 정상적으로 `onQtyChange`를 호출하지만
  `CartPage`가 이 가드에서 조용히 버린다 — 입력란 표시값은 이후 PATCH 응답이 도착하면 서버 확인값으로
  되돌아가므로 사용자에게 "왜 반영이 안 됐는지" 안내가 전혀 없다. 후속 조치 후보: 이 시점의 커밋을
  큐잉해 직전 PATCH 완료 후 이어서 보내거나, 최소한 "이전 변경 처리 중 — 다시 시도해 주세요" 같은 안내를
  `lineWarnings`/`lineErrors`에 남긴다.

## Dev 기록
계획 확인: 계획대로 진행 (2026-09-17) — 사람 코멘트 반영分은 위 "### 사람 수정" 절 참조.

### 구현 완료 (2026-09-17)

**신규 파일**
- `modules/shop-web/src/pages/CartPage.tsx` — `/shop/cart` 컨테이너. GET `/api/cart` + `fetchProducts()` join, 선택·수량변경(클램프 후 PATCH)·삭제·[주문하기] 오케스트레이션.
- `modules/shop-web/src/pages/CartPage.test.tsx` — 담긴상품/빈장바구니/수량증가/재고초과클램프/PATCH 409/삭제/선택해제(부분선택 안내)/비로그인 미호출/전체선택 후 이동, 총 9 케이스.
- `modules/shop-web/src/pages/OrderPage.tsx` — `/shop/order` 컨테이너. 진입 시 자체 GET `/api/cart` 재조회, 배송지 검증 → `checkoutCart` → 같은 라우트 안 완료 상태 전환.
- `modules/shop-web/src/pages/OrderPage.test.tsx` — 정상진입/빈장바구니/배송지검증실패/우편번호선택반영/결제성공/결제실패(400·404·409 `test.each`), 총 8 케이스.
- `modules/shop-web/src/features/shop/CartLineItem.tsx` (+`.stories.tsx`) — 썸네일·수량 스테퍼·라인합계·체크박스·삭제. qty는 항상 서버 확인값(낙관적 갱신 없음 — 실패해도 "되돌릴" 상태가 없다).
- `modules/shop-web/src/features/shop/CartEmptyState.tsx` (+`.stories.tsx`)
- `modules/shop-web/src/features/shop/CartSummary.tsx` (+`.stories.tsx`) — 부분선택일 때만 안내 노출 + `aria-describedby`로 비활성 [주문하기]와 연결(사람 수정 반영).
- `modules/shop-web/src/features/shop/DeliveryAddressForm.tsx` (+`.stories.tsx`)
- `modules/shop-web/src/features/shop/ZipcodeSearchModal.tsx` (+`.stories.tsx`) — fetch 없음(규칙 `web-fetch-only-in-api`), 검색 실행은 `OrderPage`가 오케스트레이션.
- `modules/shop-web/src/features/shop/PaymentMethodSelect.tsx` (+`.stories.tsx`) — 표시 전용, 체크아웃 바디에 포함되지 않음을 문구로 명시.
- `modules/shop-web/src/features/shop/OrderItemsSummary.tsx` (+`.stories.tsx`)
- `modules/shop-web/src/features/shop/OrderCompleteNotice.tsx` (+`.stories.tsx`) — [주문 내역 보기]는 `/`(주문 목록)로, 상세 아님.
- `modules/shop-web/src/features/shop/OrderFailureNotice.tsx` (+`.stories.tsx`, `shows-error`) — 400/404/409만 서버 message 그대로, 그 외 상태·스택트레이스성 문자열은 고정 안내로 대체(SR-234 r1 대조).
- `modules/shop-web/src/features/shop/cartTotals.ts` (+`.unit.test.ts`) — 상품금액은 서버 `lineTotal` 그대로 합산(재계산 금지). `SHIPPING_FEE_FLAT`(3,000원 고정, "정책 미확정 — SR-308에서 확정" 주석).
- `modules/shop-web/src/features/shop/deliveryAddressValidation.ts` (+`.unit.test.ts`) — 필드별 검증(zipcode/roadAddress는 하나의 오류로 묶임 — 항상 모달 선택으로만 채워지는 쌍).

**수정 파일**
- `modules/shop-web/src/api.ts` — `fetchCart`/`updateCartItemQty`/`deleteCartItem`/`checkoutCart`/`searchZipcodes` 추가(전부 `OrderHttpError` 계열 재사용, 신규 API 없음).
- `modules/shop-web/src/types.ts` — `CartRow`/`CheckoutResult`/`ZipcodeResult` 추가(기존 `CartItem`은 불변).
- `modules/shop-web/src/App.tsx` — `/shop/cart`, `/shop/order` 라우트 추가(기존 라우트 불변).
- `modules/shop-web/src/features/shop/Gnb.tsx` — 장바구니 아이콘을 `aria-disabled` span에서 `<a href="#/shop/cart">`로 전환(SR-303 사례 대조 — 신규 화면 진입점 확보). `aria-label` 문자열은 유지.

**주요 결정**
- 수량 표시는 항상 서버 확인값만 사용(낙관적 갱신 없음) — 클램프가 현재값과 같으면(경계 시도) API를 호출하지 않고 안내만 표시, 다르면 클램프된 값으로 PATCH. 실패 시 "되돌리기" 로직이 불필요(표시값이 애초에 안 바뀌었으므로).
- 부분선택 체크아웃은 서버가 지원하지 않아(`CartService.checkout` 전체 전환) [주문하기]는 선택수===전체수일 때만 활성화, 부분선택 시에만 안내 노출 + `aria-describedby` 연결.
- `OrderPage`는 상품/재고 join이 필요 없어(요약은 읽기전용) `fetchProducts()`를 호출하지 않음 — `CartPage`와 데이터 요구가 다름을 반영.
- 검증: `npm test`(타입체크+jest, 162 passed) 전체 통과, `npm run build-storybook` 성공(신규 스토리 포함 전부 컴파일). `test-storybook`(playwright 기반 실제 렌더 스윕)은 이 세션 환경에 정적 서버 실행 수단이 없어 미실행 — STEP 5 게이트(축 E)에서 재확인 필요.

다음 단계: /sl-test

### 재작업 완료 (2026-09-17)

round 1 QA FAIL 필수 4건 + 권고 2건을 아래와 같이 반영했다. 코드는 이미 재작업 상태로 존재했으나(이전 세션 중단 추정) `npm test`가 타입 오류로 실패하고 있었고, 타입 오류를 고치자 실제 런타임 결함이 하나 드러나 함께 고쳤다 — 아래 "발견·수정" 항목 참조.

**수정/생성 파일** (전부 `modules/shop-web` 안, 워크스페이스 상대 경로)
- `modules/shop-web/src/features/shop/CartLineItem.tsx` — 수량 직접입력 draft 로직 재설계(아래 "발견·수정" 참조). `useEffect`+`editingRef` 조합을 제거하고 `qtyDraft: string | null`(`null` = 편집 중 아님)로 단순화, blur/Enter 커밋 시점 분리는 그대로 유지.
- `modules/shop-web/src/pages/CartPage.tsx` — 라인 오류·로드 실패 메시지에 `toDisplayMessage`(공용 헬퍼) 적용(기존 상태 그대로, 재확인만 함).
- `modules/shop-web/src/pages/OrderPage.tsx` — 로드 실패·우편번호 검색 실패에 `toDisplayMessage` 적용, 결제예정금액을 상품금액/배송비/결제금액 3줄로 표시, 체크아웃 성공 시 `setCartRows([])`(기존 상태 그대로, 재확인만 함).
- `modules/shop-web/src/features/shop/OrderCompleteNotice.tsx` — `calcTotalsFromProductAmount`로 같은 3줄 구성 표시(기존 상태 그대로, 재확인만 함).
- `modules/shop-web/src/features/shop/CartSummary.tsx` — 0건 선택 시에도 `aria-describedby`로 사유 연결(기존 상태 그대로, 재확인만 함).
- `modules/shop-web/src/features/shop/httpErrorMessage.ts` (+`.unit.test.ts`) — `looksLikeInternalDetail`/`toDisplayMessage` 공용 헬퍼(기존 상태 그대로, 재확인만 함).
- `modules/shop-web/src/pages/CartPage.test.tsx` — 타입 오류 수정(`jest.Mock<typeof fetch>` → `import('@jest/globals').jest.Mock<typeof fetch>`, 아래 "발견·수정" 참조). 두 자리 수 직접입력·연타 방지(수량 ±/주문하기)·내부 상세 문자열 필터 테스트는 기존 상태 그대로.

**발견·수정 (이번 세션에서 실제로 고친 결함)**
1. **타입 체크 실패** — `CartPage.test.tsx`가 `jest.mock(...)` 호이스팅을 위해 `declare const jest: typeof import('@jest/globals').jest`로 값만 선언했는데, `jest.Mock<typeof fetch>` 타입 참조는 네임스페이스가 필요해 전역 `jest` 네임스페이스(비어 있음)로 오인되어 `npm test`가 타입 오류로 즉시 중단됐다. `import('@jest/globals').jest.Mock<typeof fetch>` 인라인 임포트 타입으로 교체해 런타임 호이스팅 동작은 그대로 두고 타입만 올바르게 해결했다.
2. **수량 직접입력 실측 회귀(필수1 재발)** — 타입 오류를 고친 뒤 실행해 보니 `CartLineItem`의 `useEffect`(`qty` prop → `qtyDraft` 동기화) 기반 구현이 실제로 깨져 있었다: 수량 ± 버튼만 눌러도(직접입력 아님) PATCH 성공 후 표시값이 새 `qty`로 갱신되지 않는 케이스가 재현됐다(`CartPage.test.tsx`의 "수량 증가"·"두 자리 수 직접입력" 두 테스트가 실패, `console.log`로 effect가 새 qty로 실행되는 것까지는 확인했으나 그 결과가 이어지는 동기 단언 시점에 반영되지 않음 — passive effect 타이밍 경합). `useEffect`+`editingRef` 조합을 제거하고 `qtyDraft`를 "편집 중 여부를 겸하는 `string | null`"로 재설계했다 — `null`이면 매 렌더에서 곧바로 `qty` prop을 보여주므로(파생값, effect 불필요) 이 경합이 원천적으로 사라진다. 재작업 지시 1의 요구(입력 중 로컬 draft, blur/Enter 커밋, 전송 중 비활성화 금지, 빈 값/비숫자 되돌림)는 모두 그대로 유지된다.

**재작업 지시 1~6 대응 확인**
1. [high/spec] 수량 직접입력 — 위 "발견·수정" 2 참조. `CartPage.test.tsx` "두 자리 수(15)" 케이스로 고정, blur 전 PATCH 미호출·입력란 비활성화 없음·blur 시 1회 커밋을 확인.
2. [medium/spec] 금액 표기 모순 — `OrderPage.tsx`(결제 합계 3줄) + `OrderCompleteNotice.tsx`(`calcTotalsFromProductAmount`)가 같은 계산 함수 계열을 써 같은 숫자를 보여준다. `OrderPage.test.tsx` "금액 표기" 테스트로 두 화면의 3줄이 일치함을 확인.
3. [medium/security] 내부 상세 문자열 필터 — `httpErrorMessage.ts`(`looksLikeInternalDetail`/`toDisplayMessage`)로 공용화해 `CartPage`(라인 오류·로드 실패)·`OrderPage`(로드 실패·우편번호 검색 실패)·`OrderFailureNotice` 5개 지점 전부 같은 함수를 거친다. `httpErrorMessage.unit.test.ts` + 각 화면 테스트로 확인.
4. [medium/regression] 연타 방지 가드 테스트 — `CartPage.test.tsx`(수량 ± 2회 클릭, 주문하기 2회 클릭)·`OrderPage.test.tsx`(결제하기 2회 클릭) 각각 `act()` 한 스코프에서 검증하는 테스트 존재 확인(호출 1회 단언).
5. [low/spec] 0건 선택 사유 미전달 — `CartSummary.tsx`의 `isNoneSelected` 분기 + `aria-describedby` 연결 확인, `CartPage.test.tsx` "선택 해제" 테스트가 부분선택→0건선택 전환까지 검증.
6. [low/regression] GNB 배지 stale — `OrderPage.tsx` 체크아웃 성공 콜백의 `setCartRows([])` 확인, `OrderPage.test.tsx` "결제하기 성공" 테스트가 `getByLabelText('장바구니 0개')`로 고정.

**테스트 실행 결과**
- `npm test`(타입체크+jest, shop-web): **17 suites / 179 tests 전부 통과**.
- `npm run build-storybook`: 성공(신규 스토리 전부 컴파일).
- `test-storybook`: `npx http-server storybook-static -p 6178 -s`로 정적 서버를 직접 띄운 뒤 `npx test-storybook --url http://127.0.0.1:6178 --ci`로 실행 — **37 suites / 127 stories 전부 통과**(콘솔 오류 없음, `shows-error` 태그 스토리 포함). 이전 라운드에서 "정적 서버 실행 수단이 없어 미실행"이라 기록됐던 축 E 미확인 상태를 이번에 닫았다.

**미해결·리스크**
- 없음(재작업 지시 1~6, 권고 3건 모두 반영·검증 완료).
- 배송비 정책(SR-308) 및 부분선택 체크아웃은 계획대로 범위 밖 유지.

### 재작업 완료 round3 (2026-09-17)

round 2 QA CONCERNS의 "재작업 지시" 1~3(사람 코멘트 [결정 요약]: 1·2는 이번에 고치고, 3은 후속 추적만)을
아래와 같이 반영했다.

**수정/생성 파일** (전부 `modules/shop-web` 안, 워크스페이스 상대 경로)
- `modules/shop-web/src/features/shop/CartLineItem.tsx` — 수량 입력란의 `onFocus={() => setQtyDraft(String(qty))}`
  seed를 제거(재작업 지시 1). 표시는 이미 `qtyDraft ?? String(qty)` 파생이라 seed 없이도 편집 전엔 항상 최신
  `qty`를 보여준다. 이 seed가 `commitQtyDraft`의 `qtyDraft === null` 가드를 절대 발동하지 않는 사문으로
  만들었던 원인이라, 제거만으로 가드가 되살아난다. 상단 설계 주석에 round3 재작업 이력 추가.
- `modules/shop-web/src/features/shop/httpErrorMessage.ts` — `toDisplayMessage`를 정규식 거부목록
  단독 판정에서 **상태코드 허용목록**(`ALLOWED_MESSAGE_STATUSES = [400, 404, 409]`, `OrderFailureNotice`가
  이미 쓰던 값과 동일) 우선 판정으로 재설계(재작업 지시 2). 계약에 정의된 4xx만 서버 message를 그대로
  보여주고, 5xx·계약 밖 상태(예: 401 등)는 message가 아무리 정상 문구처럼 보여도 항상 `GENERIC_ERROR_MESSAGE`로
  대체한다. `looksLikeInternalDetail`(정규식 거부목록)은 허용목록을 통과한 메시지에 대한 보조 필터로만 남겼다.
- `modules/shop-web/src/features/shop/OrderFailureNotice.tsx` — 로컬 `KNOWN_STATUSES` 상수를 제거하고
  `httpErrorMessage.ts`의 공용 `ALLOWED_MESSAGE_STATUSES`를 import해 사용(값 갈라짐 방지, 중복 정의 금지
  원칙 유지). 판정 로직·표시 동작은 그대로다.
- `modules/shop-web/src/features/shop/httpErrorMessage.unit.test.ts` — 재현 테스트 2건 추가: ①
  계약 밖 5xx의 message가 정규식에 안 걸리는 평범한 문장이어도 일반 문구로 대체되는지(상태코드 허용목록이
  주 방어선임을 확인, 이전 구현이면 실패), ② 계약에 없는 4xx(401)도 허용목록 밖이면 일반 문구로 대체되는지.
- `modules/shop-web/src/pages/CartPage.test.tsx` — 재현 테스트 2건 추가:
  ① "PATCH 응답 대기 중 타이핑 없이 focus 후 blur해도 추가 PATCH가 나가지 않는다" — [+] 클릭으로
  PATCH{qty:3}를 in-flight로 만든 뒤 입력란에 focus만 하고, 응답이 도착해 qty가 3으로 반영된 후 blur해도
  `patchCalls`가 늘지 않음을 단언(onFocus seed를 되살리면 `[{qty:3},{qty:2}]`로 실패한다).
  ② "PATCH가 계약 밖 5xx를 돌려주면 메시지가 평범해 보여도 서버 원문 대신 일반 문구만 보인다" — 500
  응답의 message(`'요청을 처리할 수 없습니다'`, 정규식이 걸지 않는 평범한 문장)가 화면에 노출되지 않고
  `GENERIC_ERROR_MESSAGE`만 보임을 단언.

**재작업 지시 1~3 대응 확인**
1. [medium/spec] onFocus 박제 버그 — 위 CartLineItem.tsx 수정으로 해소, `CartPage.test.tsx` "PATCH 응답
   대기 중 타이핑 없이 focus 후 blur" 테스트로 고정.
2. [low/security] 거부목록 → 허용목록 승격 — 위 httpErrorMessage.ts 수정으로 해소, unit test 2건 +
   `CartPage.test.tsx` "계약 밖 5xx" 테스트로 고정. `OrderFailureNotice`가 원래 쓰던 상태코드 판정을
   공용 모듈로 끌어올려 4개 지점(라인오류/CartPage 로드실패/OrderPage 로드실패/우편번호 검색실패) +
   `OrderFailureNotice` 전부 같은 허용목록을 통과해야 서버 message가 보인다.
3. [low/spec] in-flight 중 blur 커밋 유실 — 이번에 고치지 않음(사람 코멘트 지시대로). 현상·재현 조건·
   후속 조치 후보를 위 "## 후속 추적(TODO)" 절에 기록했다.

**테스트 실행 결과**
- `npm test`(타입체크+jest, shop-web): **17 suites / 183 tests 전부 통과**.
- `npm run build-storybook`: 성공.
- `test-storybook`: `npx http-server storybook-static -p 6180 -s`로 정적 서버를 직접 띄운 뒤
  `npx test-storybook --url http://127.0.0.1:6180 --ci`로 실행 — **37 suites / 127 stories 전부 통과**
  (콘솔 오류 없음). Gnb 공유 3개 화면(`ShopHomePage`/`ProductListPage`/`ProductDetailPage`) 포함 전체
  스위트 축소 없이 재실행.

**미해결·리스크**
- in-flight 중 blur 커밋 유실(위 "## 후속 추적(TODO)") — 사람 결정으로 이번 라운드 범위 밖.
- 배송비 정책(SR-308) 및 부분선택 체크아웃은 계획대로 범위 밖 유지.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-17 — FAIL
- **Layer1 스펙**: 구조·계약은 계획대로다(신규 API 0건, `{memberId}`만 보내는 checkout, 기존 4 API + 우편번호 API만 소비, 라우트 2개 추가, Gnb 진입점 연결, 부분선택 차단 + `aria-describedby`, `SHIPPING_FEE_FLAT` 상수·주석, 서버 응답 정본화). 다만 **확정 문답이 요구한 "수량 조절" 중 직접 입력 경로가 실제로 깨져 있고**(아래 필수1, 실측 재현 완료), **주문서 결제예정금액과 주문완료 화면 금액이 서로 다른 값을 말한다**(필수2). → `concerns`가 아니라 `fail`.
- **Layer2 보안**: 비로그인 시 GET `/api/cart` 미호출(테스트로 고정됨), 세션 `memberId` 외 신원확인 경로 없음, 부품 직접 fetch 없음(규칙 `web-fetch-only-in-api` 준수) — 여기까지는 계획·사람 수정대로다. 그러나 사람 수정 "스택트레이스·내부 경로가 섞인 문자열은 표시하지 않는다"가 `OrderFailureNotice` **한 곳에만** 적용됐다(필수3). `application.yml`의 `include-message: always` + Spring 기본 오류 바디 조합이라 5xx 예외 메시지가 나머지 3개 표시 지점으로 그대로 새어 나온다.
- **Layer3 회귀**: `npm test` 전체 16 suite / 162 케이스 통과(직접 재실행 확인) — Gnb의 `span`→`<a href="#/shop/cart">` 전환에도 `ShopHomePage`/`ProductListPage`/`ProductDetailPage` 회귀 없음(`getByLabelText('장바구니 N개')` 기준 그대로). "바로 구매 — 라우트 이동 없음" 케이스도 통과. 기존 라우트·API 계약·DDL 무변경 확인. 다만 계획이 사례집(SR-302 #1) 대조로 **명시 약속한 연타 가드 검증 테스트가 하나도 없다**(필수4) — 가드 3개(`inFlightSkusRef`/`checkoutInFlightRef`/`orderNavigateRef`)를 지워도 전 스위트가 통과한다. `npm run test-storybook`은 이번 세션 미실행(dev 기록대로) → STEP 5 축 E에서 확인.

- 필수 수정(FAIL시):
  1. **수량 직접입력이 두 자리 수량을 입력할 수 없고, 의도와 다른 값을 서버에 커밋한다** (`features/shop/CartLineItem.tsx:73-75` + `pages/CartPage.tsx:112-142`). 입력란이 `value={qty}`(서버 확인값) 완전 제어인데 `onChange` 매 키 입력마다 클램프→PATCH가 나가고 동시에 `pending`으로 **입력란이 비활성화**된다. 재고 20·현재 2인 줄에서 "15"를 치려고 첫 글자 `1`을 누른 시점에 `PATCH {qty:1}`이 확정되고 입력란이 disabled로 바뀌어 둘째 글자를 받을 수 없다 — 사용자는 15를 의도했는데 장바구니가 1로 바뀐다. (임시 프로브 테스트로 실측 재현: `patchCalls == [{qty:1}]`, 입력란 `disabled`, 표시값 2로 되돌아감. 프로브는 판정 후 삭제.) `ProductInfoPanel`은 같은 클램프를 쓰지만 수량이 **로컬 상태**라 이 문제가 없다 — 계획의 "동일 클램프 규칙 재사용"이 커밋 시점(키 입력마다 vs blur/Enter)까지는 정하지 않았다. 입력 중에는 로컬 draft를 두고 blur/Enter(또는 debounce)에서만 PATCH하도록 고치고, "두 자리 수량 직접 입력" 케이스를 테스트로 고정할 것.
  2. **결제예정금액과 주문완료 금액이 3,000원 어긋난다** (`pages/OrderPage.tsx:195-198` vs `features/shop/OrderCompleteNotice.tsx:18`). 주문서는 `calcCartTotals`로 `상품금액 + SHIPPING_FEE_FLAT`를 "결제예정금액"으로 보여주는데, 완료 화면은 서버 `checkout` 응답의 `totalAmount`를 그대로 쓴다. `CartService.checkout:154-157`은 `order.getTotalAmount()`(상품금액만, 배송비 개념 자체가 서버에 없음)를 돌려주므로 같은 흐름에서 27,000원 → 24,000원으로 금액이 바뀌고 어느 화면도 "배송비 별도"라고 말하지 않는다. `OrderCompleteNotice.stories`의 `totalAmount: 58000`이 `CartSummary.전체선택`의 `payableAmount: 58000`과 우연히 같아 스토리에서도 이 모순이 드러나지 않는다. 완료 화면 금액에 명시 라벨(예: `상품금액 24,000원`)을 붙이거나 배송비 별도 문구를 넣어 두 화면이 같은 것을 말하게 할 것(배송비 정책 자체는 SR-308 이월이 맞다 — 여기서 고칠 것은 **표기 모순**이다).
  3. **내부 상세 문자열 필터가 4개 표시 지점 중 1곳에만 적용됐다**(사람 수정 미이행). `looksLikeInternalDetail`은 `OrderFailureNotice.tsx:24-31`에만 있고, ① `CartPage.tsx:137` 라인별 PATCH/DELETE 실패 메시지, ② `CartPage.tsx:200` 로드 실패, ③ `OrderPage.tsx:170` 로드 실패, ④ `OrderPage.tsx:99`→`ZipcodeSearchModal` 검색 실패는 `OrderHttpError.message`(= 서버 `{message}` 원문)를 무필터로 렌더한다. `modules/shop-api/src/main/resources/application.yml:5`가 `include-message: always`이므로 500 응답의 예외 메시지가 그대로 화면에 뜬다. 필터를 공용 헬퍼로 올려 `toMessage()` 한 곳에서 걸 것(4개 지점 동시 해결).
  4. **연타 방지 가드 검증 테스트 부재** — 계획 "순서·보안 8"과 "실패 사례집 대조(SR-302 #1)"가 "`act()` 한 스코프 묶음 + 가드 임시 제거 재현으로 검증"을 명시 약속했으나 `CartPage.test.tsx`/`OrderPage.test.tsx` 어디에도 이중 클릭 케이스가 없다. 거짓 보증(사례집 줄 34)보다 나쁜 무검증 상태다. 최소 ① [결제하기] 2회 클릭 → `checkoutCalls.length === 1`, ② 수량 ± 2회 클릭 → `patchCalls.length === 1`, ③ [주문하기] 2회 클릭 → navigate 1회를 각각 `act()` 한 스코프에서 발사하고, 가드를 임시로 지워 실제로 깨지는지 확인한 뒤 되돌릴 것.

- 권고(다음 라운드에 함께, 별도 게이트 세우지 않음):
  1. (low) 전체 선택 해제(0건) 상태에서 [주문하기]가 비활성인데 **사유가 전혀 전달되지 않는다**(`CartSummary.tsx:25-26` — `isPartial`이 `selectedCount > 0` 조건이라 0건은 안내도 `aria-describedby`도 없다). `CartSummary.stories.선택없음`이 이 상태를 의도로 문서화하고는 있으나, 화면상 막다른 길이라 "주문할 상품을 선택해 주세요" 한 줄을 같은 방식으로 연결하는 편이 사람 수정의 취지(비활성 사유를 보조기기에도 전달)에 맞는다.
  2. (low) 체크아웃 성공 후 `cartRows`를 비우지 않아 **GNB 장바구니 배지가 주문 전 수량 그대로 남는다**(`OrderPage.tsx:151`, 서버는 `deleteAllItems`로 이미 비운 상태). 완료 전환 시 `setCartRows([])`면 끝난다(다른 화면으로 이동하면 자연 치유되지만, 완료 화면에 머무는 동안 틀린 값이다).
  3. (low) `npm run test-storybook` 미실행(dev 기록대로 세션 환경 제약) — STEP 5 축 E에서 신규 스토리 25개 렌더 스윕을 반드시 통과시킬 것. 특히 `shows-error` 태그 스토리 3개(`CartLineItem.수량변경실패`·`DeliveryAddressForm.배송지오류`·`ZipcodeSearchModal.조회실패`·`OrderFailureNotice` 전체)가 콘솔 error를 남기지 않는지 확인(SR-306 #1 r3~r4 사례).

- 재동기화 입력(STEP 5.5 — 권고 아님, 반영 대상):
  - UIS-ORD-011(신규, 예약 ID 그대로 — 사례집 줄 45 재발 주의): 합계 규칙 절에 **배송비 기준이 임시 가정(SR-308 확정 예정)** 임을 한 줄 명시(사람 수정 지시). 아울러 **서버 `checkout` 응답 `totalAmount`에는 배송비가 포함되지 않는다**는 사실을 같은 절에 적어 필수2의 재발을 막을 것.
  - UIS-ORD-011: 배송지·결제수단은 이번 범위에서 **화면 표시·클라이언트 검증 전용이며 서버로 전송되지 않는다**(checkout 바디 `{memberId}`뿐)는 것을 화면 설계서 본문에 명시.

### QA Gate — 2026-09-17 — CONCERNS (round 2)
> round 1 FAIL의 필수 4건·권고 3건을 **dev 자체 보고를 믿지 않고 직접 재현·재실행해** 확인했다. 7건 전부 실제로 해소됐다. 남은 것은 이번 라운드 재작업이 새로 만든 draft 로직의 경합 1건(medium)과 필터 술어의 잔여 취약 1건(low)뿐이라 차단하지 않는다.

- **Layer1 스펙**: 필수1·필수2·권고1 모두 해소 확인.
  - **필수1(수량 직접입력) 해소** — `CartLineItem.tsx:66-77`이 `qtyDraft: string | null`(= 편집 중 여부 겸용) 파생값으로 재설계돼 `useEffect` 동기화 경합이 사라졌다. 입력 중 PATCH 없음·blur/Enter 1회 커밋·빈값/비숫자는 직전 확인 수량 복귀가 `CartPage.test.tsx:269-295`(재고20·현재2에서 "1"→"15"→blur, `patchCalls === [{qty:15}]`)와 `:297-314`(빈 값 blur → PATCH 0회, 표시 3 유지)로 고정됐다. **입력란은 `disabled` 속성을 아예 받지 않는다**(`:104-109` — 체크박스·±·삭제만 `disabled`) — 사람 수정 지시 "전송 중에도 비활성화 금지"를 구조적으로 만족하며, QA가 PATCH 실제 in-flight 상태에서 별도 프로브로 `not.toBeDisabled()`를 재확인했다.
  - **필수2(금액 표기 모순) 해소** — 주문서 `OrderPage.tsx:201-207`(`aside` "결제 합계")와 완료 `OrderCompleteNotice.tsx:32-38`(`role=group` "주문 금액")가 **같은 3줄(상품금액/배송비/결제금액)** 구성이고 둘 다 `cartTotals.ts`의 같은 `SHIPPING_FEE_FLAT`를 거친다(`calcCartTotals` / `calcTotalsFromProductAmount`). 무라벨 총액 표시는 두 화면 어디에도 남아 있지 않다. `OrderPage.test.tsx:172-200`이 30,000/3,000/33,000을 두 화면에서 각각 단언한다. 스토리의 "우연한 일치"도 의도적 정렬로 바뀌었다(`CartSummary.전체선택` productAmount 55,000 ↔ `OrderCompleteNotice.주문완료` totalAmount 55,000 → 양쪽 58,000).
  - **권고1(0건 선택 사유) 해소** — `CartSummary.tsx:30,33-37,54`의 `isNoneSelected` 분기 + 부분선택과 공유하는 `aria-describedby` id. `CartPage.test.tsx:230-236`이 부분선택→0건 전환까지 검증.
  - ⚠ 잔여 medium 1건은 아래 권고 1(이번 라운드 신규 코드에서 발생, QA 프로브로 실측 재현).
- **Layer2 보안**: 필수3 해소 확인. `httpErrorMessage.ts`(신규 공용 모듈)의 `toDisplayMessage`가 지시한 4개 지점 전부를 통과한다 — ① `CartPage.tsx:133` 라인 PATCH 실패 ② `CartPage.tsx:67` 로드 실패 ③ `OrderPage.tsx:75` 로드 실패 ④ `OrderPage.tsx:98` 우편번호 검색 실패. 지시 밖 ⑤ `CartPage.tsx:153` DELETE 실패까지 같은 함수를 거치고, `OrderFailureNotice.tsx:14,30`도 중복 정의 없이 같은 모듈을 import한다(공용화 요구 충족). 검증: `httpErrorMessage.unit.test.ts` 5케이스 + 화면 테스트 4건(`CartPage.test.tsx:363,384`, `OrderPage.test.tsx:230,241`)이 스택프레임·예외 클래스명 문자열을 일반 문구로 대체하는지 단언. 비로그인 시 GET `/api/cart` 미호출·세션 `memberId` 외 신원확인 경로 없음·부품 직접 fetch 0건(`grep` 실측)·`console.log` 0건도 유지. 잔여 low는 아래 권고 2.
- **Layer3 회귀**: 필수4·권고2·권고3 모두 해소 확인(전부 QA가 직접 실행).
  - **필수4(연타 가드 검증) 해소 — 가드 제거 재현까지 QA가 직접 수행**. `inFlightSkusRef`(`CartPage.tsx:110`)·`orderNavigateRef`(`:162`)·`checkoutInFlightRef`(`OrderPage.tsx:115`) 세 줄을 실제로 주석 처리하고 `npx jest -t '연타 방지'`를 돌려 **3건 전부 정확히 실패**함을 확인했다(`patchCalls` 1→2, `navigate` 1→2회, `checkoutCalls` 1→2). 이후 복원·재확인 완료. SR-302 #1의 "거짓 보증" 패턴(분리 클릭)이 아니라 `act()` 한 스코프 묶음으로 작성돼 있다.
  - **권고2(GNB 배지 stale) 해소** — `OrderPage.tsx:131` `setCartRows([])`, `OrderPage.test.tsx:166`이 `getByLabelText('장바구니 0개')`로 고정. 완료 분기(`:179`)가 빈 장바구니 분기(`:182`)보다 앞이라 전환 후 "주문할 상품이 없습니다"로 새지 않는다.
  - **권고3(축E 미실행) 해소** — dev 기록의 주장을 QA가 재실행해 검증: `npx http-server storybook-static -p 6179 -s` + `npx test-storybook --ci` → **37 suites / 127 stories 전부 통과**(콘솔 error 0). `shows-error` 태그 스토리 포함. round 1에서 열어 둔 축 E 미확인 상태가 닫혔다.
  - 전체 스위트 `npm test`(타입체크+jest) **17 suites / 179 tests 통과**(QA 직접 재실행, 축소 없음). Gnb `span`→`<a href="#/shop/cart">` 전환에도 `ShopHomePage`/`ProductListPage`/`ProductDetailPage` 회귀 없음("바로 구매 — 라우트 이동 없음" 포함). 기존 라우트·API 계약·DDL 무변경. 규칙 점검: `web-fetch-only-in-api`·`story-per-component`(신규 부품 9개 전부 `.stories.tsx` 동반)·`file-size-cap`(CartPage 225줄·OrderPage 229줄) 모두 준수.

- 권고(CONCERNS시 — medium은 이 라운드에 전부 열거, 다음 라운드로 미루지 않음):
  1. **(medium/spec — 이번 라운드 신규 코드)** `CartLineItem.tsx:105`의 `onFocus={() => setQtyDraft(String(qty))}`가 그 시점의 `qty`를 draft에 **박제**해, PATCH 응답이 도착해 `qty`가 바뀌면 draft가 낡은 값으로 남는다. 그 결과 **타이핑을 전혀 하지 않았는데도 blur가 옛 수량으로 PATCH를 커밋한다**. QA 프로브로 실측 재현: 재고20·현재2인 줄에서 [+] 클릭(PATCH{qty:3} in-flight) → 응답 대기 중 수량 입력란 클릭(focus만) → 응답 도착(qty=3) → 입력란 밖 클릭(blur) ⇒ `patchCalls = [{qty:3}, {qty:2}]`, 최종 표시 2. 사용자가 방금 올린 수량이 소리 없이 되돌아간다(round 1 필수1과 같은 실패 계열 — "의도하지 않은 값이 서버에 커밋"). 근본 원인은 `commitQtyDraft`(`:70`)의 `if (qtyDraft === null) return` 가드와 그 주석("편집 없이 blur — 커밋할 것이 없다")이 **`onFocus`가 항상 draft를 채우는 탓에 절대 발동하지 않는 사문**이라는 점이다. 수정: `onFocus` 핸들러를 없애고 draft는 `onChange`에서만 세팅한다(표시는 이미 `qtyDraft ?? String(qty)` 파생이라 seed가 불필요하고, 기존 테스트도 `fireEvent.focus` 후 `change`를 쓰므로 그대로 통과한다). 재현 테스트 1건을 `CartPage.test.tsx`에 고정할 것.
  2. **(low/security)** `toDisplayMessage`(`httpErrorMessage.ts:21-24`)는 **거부 목록(regex)** 방식이라 상태코드를 보지 않는다 — 계약에 없는 5xx의 `message`라도 `Exception`/` at x(`/경로 토큰이 없으면 원문 그대로 화면에 나온다. `include-message: always`(`shop-api/application.yml:5`) + 이 저장소의 5xx 매핑 부재(`web/*ExceptionHandler`는 특정 예외만 처리) 조합에서 예컨대 NPE 메시지 `Cannot invoke "…Cart.getItems()" because "cart" is null`은 필터를 그대로 통과해 내부 클래스·메서드명을 노출한다. 같은 파일을 쓰는 `OrderFailureNotice.tsx:29-30`은 이미 **상태코드 허용 목록**(400/404/409)을 함께 쓰고 있어 안전하다 — 그 방식을 `toDisplayMessage`로 끌어올리면(계약 상태만 원문, 그 외 일반 문구) 거부 목록의 취약성이 통째로 사라진다. 술어 자체는 round 1에도 있던 코드라 이번 게이트를 세우지 않는다(라운드 규율).
  3. **(low/spec)** 어떤 sku의 PATCH가 in-flight인 동안 들어온 blur 커밋은 `CartPage.tsx:110`에서 **아무 안내 없이 버려진다**. 사람 수정 지시대로 입력란은 계속 살아 있어 사용자는 값을 칠 수 있는데 커밋만 사라지고 표시는 옛 값으로 돌아간다. 위 권고 1을 고칠 때 "변경 중…" 표시 중의 커밋을 큐잉하거나 최소한 안내를 남기는 편이 일관된다.

- 재동기화 입력(STEP 5.5 — 권고 아님, 반영 대상 / round 1 항목 유효, 이월):
  - UIS-ORD-011(신규, **예약 ID 그대로** — 사례집 줄 45 재발 주의): 합계 규칙 절에 **배송비 기준이 임시 가정(SR-308 확정 예정)** 임을 명시하고, **서버 `checkout` 응답 `totalAmount`에는 배송비가 포함되지 않는다**(화면이 `calcTotalsFromProductAmount`로 배송비를 덧붙여 3줄로 보여준다)는 사실을 같은 절에 적는다.
  - UIS-ORD-011: 배송지·결제수단은 이번 범위에서 **화면 표시·클라이언트 검증 전용이며 서버로 전송되지 않는다**(checkout 바디 `{memberId}`뿐)는 것을 본문에 명시.

### QA Gate — 2026-09-17 — PASS (round 3)
> round 2 CONCERNS 중 사람이 "이번에 함께 고치기"로 정한 권고 1·2를 **dev 보고를 믿지 않고 직접 코드를 열어 확인하고, 두 수정 모두 되돌려 테스트가 실제로 깨지는지(falsification probe) 재현**한 뒤 복원했다. 두 건 다 실제로 해소됐고, 이번 라운드 신규 코드에서 새 medium 이상은 나오지 않았다. 권고 3은 사람 결정대로 `## 후속 추적(TODO)`에 기록만 된 것을 확인했다.

- **Layer1 스펙**: 재작업 지시 1·3 모두 확인.
  - **지시 1(onFocus 박제) 해소 — 가드 복원 재현까지 QA가 직접 수행**. `CartLineItem.tsx:112-116`에 `onFocus` 핸들러가 실제로 없다(파일에 남은 `onFocus` 문자열 2건은 24·30행 설계 주석뿐 — `grep "onFocus="` 실측 0건). draft는 `onChange`에서만 채워지고(`:113`), 표시는 `qtyDraft ?? String(qty)` 파생(`:75`)이라 seed 없이도 편집 전엔 항상 최신 `qty`를 보여준다. 그 결과 `commitQtyDraft`(`:77-85`)의 `if (qtyDraft === null) return` 가드가 사문에서 실제 방어선으로 되살아났다. **테스트가 가짜 통과가 아님을 확인**: `onFocus={() => setQtyDraft(String(qty))}` 한 줄을 실제로 되살려 `npx jest -t "focus 후 blur"`를 돌리자 `CartPage.test.tsx:333`이 정확히 round 2 QA 프로브와 같은 값(`patchCalls = [{qty:3}, {qty:2}]`)으로 **실패**했고, 복원 후 통과했다. 재현 테스트(`CartPage.test.tsx:304-335`)는 PATCH를 수동 resolve하는 pending Promise로 **실제 in-flight 상태**를 만들고 그 사이 `fireEvent.focus` → 응답 도착(`act`) → `fireEvent.blur` 순서를 그대로 밟는다(스텁된 통과가 아니다).
  - **지시 3(in-flight blur 커밋 유실) — 기록만 확인**. `## 후속 추적(TODO)` 절이 신설돼 원인 지점(`CartPage.tsx:110`의 `if (inFlightSkusRef.current.has(sku)) return`), 재현 조건(±/직접입력 커밋으로 PATCH in-flight인 동안 같은 줄에 다른 값 입력 후 blur/Enter → 조용히 드롭, 응답 도착 시 표시값이 서버 확인값으로 되돌아가 안내 없음), 후속 조치 후보(커밋 큐잉 또는 `lineWarnings`/`lineErrors` 안내)가 모두 적혀 있다. 코드는 사람 결정대로 손대지 않은 것을 확인(가드가 원형 그대로 `:110`에 있다).
  - 회귀 없음: 두 자리 직접입력(`:269-295`)·빈 값 blur 복귀(`:337-354`)·입력란 `not.toBeDisabled()` 단언이 그대로 통과한다.
- **Layer2 보안**: 재작업 지시 2 확인.
  - `httpErrorMessage.ts:24`에 `ALLOWED_MESSAGE_STATUSES = [400, 404, 409]` 공용 상수가 생겼고 `toDisplayMessage`(`:38-48`)가 **상태코드 허용목록을 첫 조건으로** 판정한 뒤 `looksLikeInternalDetail`을 보조로만 쓴다. `OrderFailureNotice.tsx:19,32`는 로컬 `KNOWN_STATUSES`를 지우고 같은 상수를 import한다 — **중복 정의 0건**(`grep` 실측: 상태코드 리터럴 목록은 `httpErrorMessage.ts:24` 한 곳뿐).
  - **5개 표시 지점 전수 확인**(`grep` 실측): ① `CartPage.tsx:133` 라인 PATCH 실패 ② `CartPage.tsx:67` 로드 실패 ③ `CartPage.tsx:153` DELETE 실패 ④ `OrderPage.tsx:75` 로드 실패 ⑤ `OrderPage.tsx:98` 우편번호 검색 실패 — 전부 `toDisplayMessage`를 거친다. `OrderPage.tsx:136`만 `e.message`를 원문 보관하지만 렌더는 `OrderFailureNotice`(같은 허용목록)가 한다. SR-305 파일 중 서버 문자열을 무필터로 렌더하는 곳은 없다.
  - **테스트가 가짜 통과가 아님을 확인**: `toDisplayMessage`에서 `ALLOWED_MESSAGE_STATUSES.includes(e.status) &&` 한 줄만 지우고 `npx jest -t "계약"`을 돌리자 **3건 실패**(unit 2건 + `CartPage.test.tsx:440` 화면 1건). 즉 허용목록이 실제 하중을 받는 방어선이고, 5xx의 평범한 문장(`'요청을 처리할 수 없습니다'` — 정규식 거부목록이 절대 못 잡는 형태)이 그 한 줄 없이는 화면에 그대로 뜬다. 복원 후 전부 통과.
  - **허용목록이 계약을 좁히지 않는지 역검증**: 이 SR이 소비하는 API의 실제 상태코드를 서버에서 확인했다 — `CartService.java`의 `HttpStatus` 사용은 400/404/409 **세 종류뿐**(`:135,140,183,191,198,206,213,230,232`), `ZipcodeController.java:37`은 400(MBR-4202)과 200(0건 포함)뿐. 허용목록이 계약 전체를 덮으므로 정상 사유 문구가 일반 문구로 가려지는 회귀는 없다(`OrderPage.test.tsx:268` `test.each` 400/404/409 3건이 서버 message 원문 노출 + 입력값 보존을 계속 단언하며 통과).
- **Layer3 회귀**: round 1 필수 4건·round 2 해소분 전부 유지, 전 스위트 QA 직접 재실행.
  - `npm test`(타입체크+jest) **17 suites / 183 tests 통과**(축소 없음, QA 직접 2회 실행 — 프로브 전/후).
  - `npm run test-storybook`: `npx http-server storybook-static -p 6181 -s` + `npx test-storybook --url http://127.0.0.1:6181 --ci` → **37 suites / 127 stories 전부 통과**(QA가 `build-storybook` 재빌드 후 직접 실행). `shows-error` 태그 스토리 포함, 콘솔 error 0.
  - 연타 가드 3개(`CartPage.tsx:110`·`:162`, `OrderPage.tsx:115`) 원형 유지 확인, 해당 테스트 3건 통과. 금액 3줄 일치(`OrderPage.test.tsx:172`)·GNB 배지 0개 전환(`:144`)·0건 선택 `aria-describedby`(`CartPage.test.tsx:213`)도 통과 — **httpErrorMessage 리팩터가 `OrderFailureNotice`의 400/404/409 노출 동작을 깨지 않았다**.
  - 규칙: `web-fetch-only-in-api`(부품 직접 fetch 0건)·`story-per-component`·`no-console.log` 준수. 이번 라운드 변경분은 `modules/shop-web` 밖으로 나가지 않았다(스코프 준수). 기존 라우트·API 계약·DDL 무변경.

- 권고: 없음(이번 라운드 신규 코드에서 medium 이상 0건, low도 0건).

- 후속 TODO(게이트 아님 — 이 SR 범위 밖 코드):
  - in-flight blur 커밋 유실 — 사람 결정으로 이월, `## 후속 추적(TODO)` 절에 기록 완료.
  - **형제 화면들이 같은 노출 패턴을 그대로 갖고 있다**(SR-305 파일 아님 — SR-302/303/304 및 기존 화면): `ShopHomePage.tsx:55`·`ProductListPage.tsx:69`·`OrderListPage.tsx:27`·`OrderDetailPage.tsx:19`가 `e.message`를 무필터로 렌더한다. `include-message: always` 환경에서 이번 라운드가 장바구니·주문서에서 막은 것과 **같은 계열의 5xx 원문 노출**이다. 이번 게이트를 세우지 않지만(라운드 규율 — 이전 라운드에도 있던, 게다가 이 SR이 만들지 않은 코드), `httpErrorMessage.ts`가 이미 공용 모듈이므로 후속 SR에서 한 번에 적용할 것을 권한다.

- 재동기화 입력(STEP 5.5 — 권고 아님, 반영 대상 / round 1·2 항목 유효, 이월):
  - UIS-ORD-011(신규, **예약 ID 그대로** — 사례집 줄 45 재발 주의): 합계 규칙 절에 **배송비 기준이 임시 가정(SR-308 확정 예정)** 임을 명시하고, **서버 `checkout` 응답 `totalAmount`에는 배송비가 포함되지 않는다**(화면이 `calcTotalsFromProductAmount`로 배송비를 덧붙여 3줄로 보여준다)는 사실을 같은 절에 적는다.
  - UIS-ORD-011: 배송지·결제수단은 이번 범위에서 **화면 표시·클라이언트 검증 전용이며 서버로 전송되지 않는다**(checkout 바디 `{memberId}`뿐)는 것을 본문에 명시.
  - UIS-ORD-011(round 3 추가): 오류 표시 규칙 절에 **계약 상태(400/404/409)의 서버 message만 화면에 그대로 노출하고 그 외 상태는 일반 문구로 대체한다**는 판정 기준을 적는다(`ALLOWED_MESSAGE_STATUSES` 정본 위치: `modules/shop-web/src/features/shop/httpErrorMessage.ts`).

## 재작업 지시
> round 2 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] CartLineItem.tsx:105 onFocus가 그 시점 qty를 draft에 박제해, PATCH 응답으로 qty가 바뀌면 타이핑 없이 blur만 해도 옛 수량으로 PATCH가 커밋된다(QA 프로브 실측: [+] 클릭 후 응답 대기 중 입력란 focus → 응답 도착 → blur ⇒ patchCalls=[{qty:3},{qty:2}], 방금 올린 수량이 소리 없이 되돌아감). commitQtyDraft의 qtyDraft===null 가드는 onFocus 탓에 절대 발동하지 않는 사문이다. 이번 라운드 draft 재설계가 새로 만든 결함이며 round1 필수1과 같은 실패 계열(의도하지 않은 값의 서버 커밋) → onFocus 핸들러를 제거하고 draft는 onChange에서만 세팅한다(표시는 이미 qtyDraft ?? String(qty) 파생이라 seed 불필요, 기존 테스트도 그대로 통과). 위 재현 시나리오를 CartPage.test.tsx에 테스트로 고정할 것
2. [low/security] httpErrorMessage.ts:21-24 toDisplayMessage가 상태코드를 보지 않는 거부목록(regex) 방식이라, 계약 밖 5xx의 message라도 Exception/스택프레임/경로 토큰이 없으면 원문 그대로 노출된다(include-message: always + 5xx 전역 매핑 부재 조합에서 NPE 메시지의 내부 클래스·메서드명이 그대로 화면에 뜬다). 술어 자체는 round1에도 있던 코드라 게이트를 세우지 않는다 → OrderFailureNotice.tsx:29-30이 이미 쓰는 상태코드 허용목록(400/404/409만 원문, 그 외 일반 문구)을 toDisplayMessage로 끌어올린다
3. [low/spec] CartPage.tsx:110 — 어떤 sku의 PATCH가 in-flight인 동안 들어온 blur 커밋이 아무 안내 없이 버려진다. 사람 수정 지시대로 입력란은 계속 활성이라 사용자는 값을 칠 수 있는데 커밋만 사라지고 표시는 옛 값으로 돌아간다 → 위 medium을 고칠 때 '변경 중…' 상태의 커밋을 큐잉하거나 최소한 드롭 사실을 안내한다

사람 코멘트: [결정 요약] 권고 1번과 2번은 이번 라운드에서 고친다. 1번은 라운드1 필수1과 같은 계열(사용자가 올린 수량이 조용히 되돌아감)이라 후속으로 미룰 성질이 아니고, 2번은 거부목록 방식이 5xx 원문 노출을 막지 못하는 구조적 구멍이다. 3번(in-flight 중 blur 커밋 유실)만 STORY 후속 추적에 남긴다.
[구현 방식] 1) CartLineItem.tsx의 onFocus seed(setQtyDraft(String(qty)))를 제거한다 — 표시는 이미 qtyDraft ?? String(qty) 파생이라 seed가 없어도 같은 값이 보이고, commitQtyDraft의 null 가드(타이핑하지 않았으면 전송하지 않음)가 되살아난다. 2) toDisplayMessage를 상태코드 기준 허용목록으로 바꾼다: 4xx 중 서버가 계약으로 정의한 응답의 message만 그대로 보여 주고, 5xx와 계약 밖 상태는 서버 문자열을 아예 쓰지 않고 일반 문구로 대체한다(토큰 거부목록은 보조 수단으로만 남긴다). 3) in-flight 중 blur 커밋 유실은 이번에 고치지 않는다 — STORY 후속 추적(TODO)에 현상·재현 조건과 함께 남긴다.
[보안 순서] 5xx와 계약 밖 상태에서는 서버 message를 어떤 경로로도 렌더하지 않는다(include-message:always 환경에서 내부 클래스·메서드명이 그대로 나온다). 네 개 표시 지점 모두 같은 공용 함수를 거친다.
[테스트·완료 조건] 권고1의 재현 테스트를 추가한다: [+]로 PATCH 전송 중 입력란에 타이핑 없이 focus 후 blur 했을 때 추가 PATCH가 나가지 않는 것을 단언(이 테스트는 onFocus seed를 되살리면 실패해야 한다). 권고2는 5xx 응답의 서버 message가 화면에 나타나지 않고 일반 문구가 보이는 것을 단언한다. 회귀는 축소 없이 shop-web 전체 스위트 + test-storybook 스윕 재실행.
[후속 SR·이월] SR-308(배송비 정책 확정) 유지. 이번에 안 고치는 것: in-flight 중 blur 커밋이 안내 없이 버려지는 것(CartPage.tsx) — STORY 후속 추적에 기록. 재동기화(STEP 5.5)에서 UIS-ORD-011 예약 ID를 그대로 쓰고, 배송비 임시 가정·서버 totalAmount에 배송비 미포함·배송지/결제수단 미전송 사실을 스펙 본문에 명시한다.

## test-agent 실행 결과 (2026-09-17)

### TC 현황 및 AC 매핑

**수용 기준(AC)**:
- **UIS-ORD-011**: 쇼핑 장바구니·주문서 화면 신규 구현

**세부 확인 항목** (구현 계획 기준):

#### 장바구니 화면(/shop/cart) 검증 케이스
- 담긴상품 렌더(썸네일·상품명·단가·수량·라인합계) — `CartPage.test.tsx`
- 빈 장바구니 안내 + [쇼핑 계속하기] 링크 — `CartPage.test.tsx`
- 수량 증가/감소 스테퍼 + PATCH 호출 및 값 반영 — `CartPage.test.tsx`
- 수량 직접입력(두 자리 수 포함) + blur 커밋 — `CartPage.test.tsx`
- 재고 초과 클램프 + 안내 메시지 — `CartPage.test.tsx`
- 서버 409(재고부족) 응답 처리 + 안내 유지 — `CartPage.test.tsx`
- 상품 삭제 + DELETE 호출 + 행 제거 — `CartPage.test.tsx`
- 부분선택 시 합계 재계산 및 [주문하기] 비활성화 + `aria-describedby` 안내 — `CartPage.test.tsx`
- 0건 선택 시 [주문하기] 비활성화 + 사유 전달(접근성 준수) — `CartPage.test.tsx`
- 전체선택 후 [주문하기] 클릭 → `/shop/order` 라우트 이동 — `CartPage.test.tsx`
- 비로그인 상태에서 GET `/api/cart` 호출 자체 미실행 — `CartPage.test.tsx`
- PATCH 응답 대기 중 타이핑 없이 focus→blur 시 추가 PATCH 미발생(연타 방지) — `CartPage.test.tsx`
- 서버 오류 메시지 필터링(계약 밖 5xx는 일반 문구로 대체) — `CartPage.test.tsx`

#### 주문서 화면(/shop/order) 검증 케이스
- 정상 진입 시 장바구니 요약 및 배송지 폼 렌더 — `OrderPage.test.tsx`
- 빈 장바구니 진입 시 안내 + 링크(체크아웃 API 미호출) — `OrderPage.test.tsx`
- 배송지 미입력 상태에서 [결제하기] 클릭 시 필드별 검증 오류(API 미호출) — `OrderPage.test.tsx`
- 우편번호 찾기 모달에서 항목 선택 시 zipcode/roadAddress 필드 자동 채우기 — `OrderPage.test.tsx`
- 결제하기 성공 시 주문번호 및 [주문 내역 보기]/[쇼핑 계속하기] 렌더 — `OrderPage.test.tsx`
- 결제하기 실패(400/404/409) 시 서버 message 그대로 노출 + 배송지 입력값 보존 — `OrderPage.test.tsx`
- [결제하기] 2회 클릭 시 체크아웃 API 1회만 호출(연타 방지) — `OrderPage.test.tsx`
- 금액 3줄(상품금액/배송비/결제금액) 일치 — 주문서·주문완료 화면 동일 계산 사용 — `OrderPage.test.tsx`
- GNB 장바구니 배지 0개 전환(체크아웃 성공 후 `setCartRows([])` 실행) — `OrderPage.test.tsx`

#### 단위 테스트 (순수 로직 검증)
- `cartTotals.unit.test.ts` — 선택 0/일부/전체 각각의 상품금액·배송비·결제예정금액 경계값
- `deliveryAddressValidation.unit.test.ts` — 각 필드(수령인/연락처/우편번호/도로명주소) 개별 누락 시 필드별 오류 반환
- `httpErrorMessage.unit.test.ts` — 상태코드 허용목록(400/404/409) 기반 필터링 + 거부목록 보조 검증

#### 회귀 테스트 (기존 동작 무변경 확인)
- `ShopHomePage.test.tsx` — Gnb 장바구니 아이콘 `<a href="#/shop/cart">` 클릭 정상 작동
- `ProductListPage.test.tsx` — 카테고리/검색 필터 및 페이지네이션 유지
- `ProductDetailPage.test.tsx` — "바로 구매" 버튼의 라우트 이동 없음 유지(`addCartItem` 호출은 정상)
- 기존 주문 목록·주문 상세·로그인·비밀번호 재설정 화면 및 API 무변경

### 테스트 실행 결과

**단위·통합 테스트 (Jest)**:
- 실행: `cd {{WS}}\modules\shop-web && npm test -- --watchAll=false`
- 결과: **17 suites / 183 tests 통과**
- 실행 시간: 5.538초
- 내용:
  - CartPage.test.tsx: 수량 증감, 직접입력, 삭제, 선택, 연타 방지, 비로그인, 오류 처리 등 다중 시나리오
  - OrderPage.test.tsx: 진입, 배송지 검증, 우편번호 검색, 결제 성공/실패(3가지), 연타 방지, GNB 배지 전환 등
  - cartTotals.unit.test.ts: 배송비 포함 계산, 선택 수량별 경계값
  - deliveryAddressValidation.unit.test.ts: 필드별 검증 규칙
  - httpErrorMessage.unit.test.ts: 상태코드 허용목록 기반 필터링, 5xx 일반 문구 대체
  - 회귀 테스트(ShopHomePage, ProductListPage, ProductDetailPage 포함): 기존 기능 무변경

**UI 컴포넌트 스토리 테스트 (test-storybook + Playwright)**:
- 빌드: `npm run build-storybook` — 성공 (storybook-static/ 생성)
- 실행: `npx http-server storybook-static -p 6182 -s` + `npx test-storybook --url http://127.0.0.1:6182 --ci`
- 결과: **37 suites / 127 stories 통과**
- 실행 시간: 15.459초
- 신규 부품 스토리 포함:
  - CartEmptyState.stories.tsx — 빈 장바구니 상태
  - CartLineItem.stories.tsx — 상품 라인 아이템(기본/수량변경중/재고초과/1미만 시도)
  - CartSummary.stories.tsx — 합계 및 [주문하기](전체선택/부분선택 각각)
  - DeliveryAddressForm.stories.tsx — 배송지 입력 폼 및 필드 오류 상태
  - ZipcodeSearchModal.stories.tsx — 우편번호 검색 모달
  - PaymentMethodSelect.stories.tsx — 결제수단 선택(표시 전용)
  - OrderItemsSummary.stories.tsx — 주문 상품 요약(읽기전용)
  - OrderCompleteNotice.stories.tsx — 주문번호 및 다음 행동
  - OrderFailureNotice.stories.tsx(`shows-error` 태그) — 400/404/409 오류 메시지 표시
  - 기존 화면 스토리(ShopHomePage, ProductListPage, ProductDetailPage 포함) 무변경

### 커버리지 분석

| AC 세부 항목 | 커버하는 TC | 상태 |
|-------------|----------|------|
| 장바구니 렌더 | CartPage.test.tsx "담긴상품" | ✅ PASS |
| 빈 장바구니 | CartPage.test.tsx "빈 장바구니 안내" | ✅ PASS |
| 수량 스테퍼 | CartPage.test.tsx "수량 증가/감소" | ✅ PASS |
| 수량 직접입력 | CartPage.test.tsx "두 자리 수 직접입력" | ✅ PASS |
| 재고 클램프 | CartPage.test.tsx "재고 초과 클램프", cartTotals.unit.test.ts | ✅ PASS |
| 상품 삭제 | CartPage.test.tsx "삭제 클릭" | ✅ PASS |
| 부분선택 | CartPage.test.tsx "선택 해제", "0건 선택" | ✅ PASS |
| 합계 계산 | cartTotals.unit.test.ts, CartPage.test.tsx | ✅ PASS |
| [주문하기] | CartPage.test.tsx "전체선택 후 주문하기" | ✅ PASS |
| 배송지 검증 | deliveryAddressValidation.unit.test.ts, OrderPage.test.tsx | ✅ PASS |
| 우편번호 찾기 | OrderPage.test.tsx "우편번호 선택 반영" | ✅ PASS |
| [결제하기] | OrderPage.test.tsx "결제 성공" | ✅ PASS |
| 결제 실패 처리 | OrderPage.test.tsx "결제 실패(400/404/409)" | ✅ PASS |
| 주문 완료 | OrderPage.test.tsx "주문번호 표시" | ✅ PASS |
| 비로그인 보안 | CartPage.test.tsx "비로그인 미호출" | ✅ PASS |
| 오류 필터링 | CartPage.test.tsx, OrderPage.test.tsx, httpErrorMessage.unit.test.ts | ✅ PASS |
| 연타 방지 | CartPage.test.tsx, OrderPage.test.tsx (가드 임시 제거 재현) | ✅ PASS |
| Gnb 진입점 | ShopHomePage.test.tsx, ProductDetailPage.test.tsx | ✅ PASS |
| 기존 화면 회귀 | ShopHomePage.test.tsx, ProductListPage.test.tsx, ProductDetailPage.test.tsx | ✅ PASS |

### 품질 판정

- **통과율**: 183/183 (100%) — 단위·통합 + 127/127 (100%) — UI 스토리
- **규칙 준수**:
  - `web-fetch-only-in-api`: 부품 직접 fetch 0건 ✅
  - `story-per-component`: 신규 부품 9개 전부 `.stories.tsx` 동반 ✅
  - `no-console.log`: 콘솔 로그 0건 ✅
  - `file-size-cap`: CartPage 225줄·OrderPage 229줄(자바 450줄 한도 준수) ✅
- **보안**: 비로그인 미호출, 세션 `memberId` 외 신경 확인 경로 없음, 오류 메시지 필터링 ✅
- **회귀 무결성**: 기존 라우트·API 계약·DDL 무변경, 기존 화면 무변경 ✅
- **근거**: QA round 3 PASS(dev-agent 재작업 3라운드 거쳐 합의됨) + test-agent 독립 재실행 검증 완료

### 결론

**납품 가능**: SR-305.1 수용 기준(UIS-ORD-011)의 모든 세부 조건이 실행 테스트로 1:1 매핑되고 검증되었다. 단위·통합·UI 스토리 전 영역에서 회귀 없음을 확인했다.
