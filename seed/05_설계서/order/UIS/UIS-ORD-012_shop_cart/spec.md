---
uis-id: UIS-ORD-012
name: shop_cart
domain: order
domain-code: ORD
layer: ui
route: /shop/cart
screens_role: 주화면
api_hints:
  - "GET /api/cart"
  - "GET /api/products"
  - "PATCH /api/cart/items/{sku}"
  - "DELETE /api/cart/items/{sku}"
access_control:
  - "세션(memberId) 없으면 GET /api/cart 자체를 호출하지 않고 \"로그인이 필요합니다\" + 로그인 링크만 표시 — 별도 인가(auth) 게이팅 없음, 신규 신원확인 경로 없음(CartPage.tsx:76-79,186-190)"
  - "라인 조작(체크박스·수량 ±/직접입력·삭제)은 해당 sku의 PATCH/DELETE가 in-flight인 동안 그 줄만 비활성(pending, 연타 방지) — 다른 줄은 계속 조작 가능(CartPage.tsx:108-158, CartLineItem.tsx:69,90,107,117,134)"
  - "[주문하기]는 선택 개수 === 전체 개수(전체선택)일 때만 활성 — 서버 checkout이 선택 여부와 무관하게 전체 장바구니를 주문 전환하는 계약이라 부분선택 상태로는 진행시키지 않는다(CartSummary.tsx:28,31,53)"
  - "[주문하기] 연타 방지 — 동기 ref(orderNavigateRef) 잠금, state 지연에 의존하지 않음(CartPage.tsx:50,161-165)"
anchors:
  - "modules/shop-web/src/App.tsx:108"
  - "modules/shop-web/src/pages/CartPage.tsx"
  - "modules/shop-web/src/pages/CartPage.test.tsx"
  - "modules/shop-web/src/features/shop/CartLineItem.tsx"
  - "modules/shop-web/src/features/shop/CartEmptyState.tsx"
  - "modules/shop-web/src/features/shop/CartSummary.tsx"
  - "modules/shop-web/src/features/shop/cartTotals.ts"
  - "modules/shop-web/src/features/shop/httpErrorMessage.ts"
  - "modules/shop-web/src/features/shop/Gnb.tsx:36-49"
  - "modules/shop-web/src/api.ts:230-259"
  - "modules/shop-web/src/types.ts:105-111"
  - "docs/변경관리/SR-305/STORY-1.md"
revision_history:
  - "2026-09-17 골격 생성(spec_resync_check, zero-LLM)"
  - "2026-09-17 소스 기준 SOP급 본문 보강(ddd-ui-agent, source-authority) — SR-305"
---

# UIS-ORD-012: shop_cart

> **근거 소스(권위):** `modules/shop-web/src/pages/CartPage.tsx` 외 `features/shop/{CartLineItem,CartEmptyState,CartSummary,cartTotals,httpErrorMessage}.*`(SR-305
> 신규 구현). DOM 스냅샷 없음(소스폴백 모드) — 소스 슬라이스 + `docs/변경관리/SR-305/STORY-1.md`
> (확정 요건 문답·구현 계획·QA round1~3 결과) + `.speclinker/storybook_index.json`의 스토리 10건을
> 근거로 작성.

## 1. 화면 개요

- 라우트: `/shop/cart` (spa-route, `modules/shop-web/src/App.tsx:108`)
- 목적: 쇼핑몰 장바구니 화면(SR-305 신규). 로그인 회원이 담아둔 상품을 조회해 수량을 조절하거나
  삭제하고, 주문할 상품을 선택해 합계(상품금액/배송비/결제예정금액)를 확인한 뒤 주문서
  (`/shop/order`, UIS-ORD-011)로 넘어가는 진입 화면이다. 결제 자체는 이 화면 범위 밖(주문서에서
  처리)이고, 이 화면은 조회·조작·합계 미리보기·주문서 이동까지만 담당한다.
- 진입 경로: `Gnb`의 장바구니 아이콘(`<a href="#/shop/cart">`, `Gnb.tsx:40`) — 쇼핑홈·상품목록·
  상품상세 세 화면이 공유하는 유일한 진입점(SR-305에서 기존 `title="준비 중"` 비활성 아이콘을
  실제 링크로 전환해 닫음, 사례집 "신규 화면 진입점 누락" 재발 방지). 그 외 직접 URL 진입.
- 접근 권한: 화면 라우트 자체는 인증 게이팅이 없으나(라우트 진입은 항상 가능), 세션(memberId)이
  없으면 `GET /api/cart` 호출 자체를 하지 않고 "로그인이 필요합니다" 안내만 보여준다(§5, §4 참조).

## 2. 화면 구성

| 블록 | 역할 | 구성 요소 | 소스 근거 |
|---|------|----------|----------|
| GNB | 검색·세션 표시·장바구니 수량 배지(`cartItemCount`)·로그아웃(기존 화면과 공통 재사용) | `Gnb` | `CartPage.tsx:9,181-182` |
| 로그인 필요 안내 | 세션 없을 때 목록 대신 표시 | 안내 문구 + `#/login` 링크 | `CartPage.tsx:186-190` |
| 로딩 표시 | GET 응답 전 | `role="status"` "불러오는 중…" | `CartPage.tsx:192` |
| 로드 실패 안내 | GET 실패(네트워크/5xx 등) | `role="alert"` 오류 문구 + [다시 시도] | `CartPage.tsx:193-202` |
| 빈 장바구니 안내 | 담긴 상품 0건 | `CartEmptyState`("담긴 상품이 없습니다" + [쇼핑 계속하기]) | `CartEmptyState.tsx` |
| 장바구니 목록 | 라인아이템 반복 렌더(체크박스·썸네일·상품명·단가·수량 스테퍼/직접입력·라인합계·삭제) | `CartLineItem` × N | `CartPage.tsx:207-215`, `CartLineItem.tsx` |
| 합계 패널 | 선택 항목 기준 상품금액/배송비/결제예정금액 + [주문하기] | `CartSummary` | `CartPage.tsx:217-219`, `CartSummary.tsx` |

> 로그인 필요 / 로딩 / 로드 실패 / 빈 장바구니 / (목록+합계) 다섯 상태는 상호 배타이며 이 순서로
> 우선순위가 매겨진다(`CartPage.tsx:186-221`의 조건부 렌더 체인).

## 3. 입력·검증 규칙

- **memberId**: 세션(`loadSession()`)에서만 취득 — 프런트가 게스트용 임의 값을 만들지 않는다.
  세션이 없으면 `GET /api/cart` 자체를 호출하지 않는다(`CartPage.tsx:76-79`, STORY "순서·보안" 1 —
  신규 신원확인 경로를 만들지 않는다).
- **선택 상태(selected)**: 초기값은 응답으로 받은 전체 sku(전체선택 기본값, `CartPage.tsx:64`).
  체크박스 토글로 개별 증감(`toggleSelected`, `CartPage.tsx:94-100`).
- **수량 변경 클램프(1~stockQty)**: `handleQtyChange`가 요청값을 `1 ~ Math.max(1, stockQty)` 범위로
  자르고(`Math.min(Math.max(1, safeRaw), max)`), 비정수/`NaN`은 직전 확인 수량으로 대체한다
  (`CartPage.tsx:108-121`). 클램프 결과가 현재 수량과 같으면(경계 시도) PATCH를 호출하지 않고
  즉시 경고만 표시한다 — "1개 미만으로는 변경할 수 없습니다" / "최대 수량은 {max}개입니다"
  (`CartPage.tsx:117-121`).
- **수량 직접입력은 로컬 draft 상태로만 유지되다 커밋 시에만 PATCH가 나간다**: `CartLineItem`의
  입력란은 `qtyDraft`(문자열, `null`=편집 중 아님)를 별도로 두고, `onChange`마다 draft 문자열만
  바꾼다 — **연속 키 입력마다 커밋하지 않는다**(`CartLineItem.tsx:74-85,112-113`). 커밋은
  `onBlur` 또는 `Enter`(→ `blur()` 위임)에서만 일어난다(`CartLineItem.tsx:113-116`). 스테퍼
  (±버튼)는 클릭 즉시 커밋(draft 경유 없음, `CartLineItem.tsx:107-121`). 이는 round1~2 QA가
  실측 지적한 결함의 재작업 결과다 — 이전 구현(`value={qty}` 완전 제어 + 매 키 입력마다 커밋)은
  재고 20·현재 2인 줄에서 "15"를 치려던 사용자가 "1" 시점에 PATCH{qty:1}이 먼저 확정되고
  입력란이 비활성화돼 "5"를 받지 못하는 결함이 있었다(`CartLineItem.tsx:9-30` 주석). 빈 값/
  비숫자로 blur하면 직전 확인 수량으로 되돌리고 PATCH를 호출하지 않는다
  (`CartLineItem.tsx:82`). 커밋값이 직전 확인 수량과 같아도 PATCH를 호출하지 않는다
  (`CartLineItem.tsx:83`).
- **표시값은 항상 서버가 마지막으로 확인해 준 수량**: 낙관적 갱신 없음. PATCH 진행 중/실패
  시에도 별도로 "되돌릴" 로컬 값을 두지 않는다 — 그 값이 곧 직전 확인 수량이다
  (`CartPage.tsx:102-107,130-133`).
- **연타 방지(품목별)**: `inFlightSkusRef`(Set) — 같은 sku에 PATCH/DELETE가 진행 중이면 새 요청을
  무시한다(`CartPage.tsx:110,125,135,142-143,155`). [주문하기]는 별도 동기 ref
  (`orderNavigateRef`)로 잠근다(`CartPage.tsx:161-165`).
- **알려진 미해결 결함(회귀 아님, 후속 추적)**: 어떤 sku의 PATCH가 in-flight인 동안 같은 줄
  입력란에서 blur 커밋이 들어오면 `inFlightSkusRef` 가드에서 **안내 없이 조용히 버려진다**
  (`CartPage.tsx:108-110` 진입부 가드). QA round2 CONCERNS 권고3·round3 재작업 지시로
  식별되었으나 "이번에는 고치지 않음"으로 이월(STORY-1.md "후속 추적(TODO)" 절, SR-308 이전
  범위 밖 — §6 참조).

## 4. 호출 API

| 트리거 | API | 용도 | 성공 처리 | 실패 처리 |
|---|---|---|---|---|
| 화면 진입(세션 있을 때만) | GET /api/cart (`fetchCart`) | 장바구니 라인 조회(`{items,totalAmount}`에서 items만 사용) | `cartRows` 세팅, 전체 sku를 `selected`로 초기화(전체선택 기본) | 그 외 상태 → `loadError`에 표시 문구, [다시 시도] 버튼으로 재호출(`load`) |
| 화면 진입(1회) | GET /api/products (`fetchProducts`, 무인자, 기존 API 재사용) | 라인별 재고(`stockQty`)·이미지(`imageUrl`) join(신규 API 아님) | sku로 매칭해 라인아이템 구성. 매칭 실패 시 재고 상한을 현재 수량으로 두어 조작을 부당하게 막지 않음(`CartPage.tsx:82-89`) | 실패 시 `Promise.all`이 함께 실패 → 위 로드 실패 안내로 합류 |
| 수량 ± 클릭 / 직접입력 blur·Enter 커밋 | PATCH /api/cart/items/{sku} (`updateCartItemQty`) | 클램프 통과한 수량으로 변경 | 응답의 `qty`·`lineTotal`로 그 행을 다시 그림(서버 응답이 정본) | 400(qty)/404(품목없음)/409(재고초과) → 서버 message를 그 줄 오류로 표시 + 표시값은 직전 확인 수량 유지. 5xx 등 계약 밖 → 일반 문구(§5·httpErrorMessage.ts) |
| 삭제 클릭 | DELETE /api/cart/items/{sku} (`deleteCartItem`) | 즉시 삭제(확인 다이얼로그 없음) | 그 행 제거 + 선택·경고·오류 상태 정리 | 404 등 → 그 줄 오류 표시(행은 유지) |
| [주문하기] 클릭(전체선택일 때만 활성) | (API 아님) `navigate('/shop/order')` | 주문서(UIS-ORD-011)로 이동 | 라우트 이동, 상태 전달 없음(주문서가 자체적으로 GET /api/cart 재조회) | 해당 없음 |
| [쇼핑 계속하기](빈 장바구니) | (API 아님) `<a href="#/shop/products">` | 상품 목록으로 이동 | 라우트 이동 | 해당 없음 |
| [로그인 하러 가기](세션 없음) | (API 아님) `<a href="#/login">` | 로그인 화면 이동 | 라우트 이동 | 해당 없음 |

> 이 화면이 신규로 만든 서버 API는 없다 — 기존 GET/PATCH/DELETE `/api/cart*`를 그대로 소비만
> 한다(구현 범위는 `shop-web` 프런트엔드로 한정, STORY-1.md "확정된 요건 문답" 절). 프런트
> 전용 오류 타입 `OrderHttpError`(HTTP status만 보관, `code` 없음, `api.ts:184-190`)는 서버
> 계약이 아니므로 새 오류 봉투로 반영하지 않는다.

### 내부 상세 문자열 노출 방지 — 상태코드 허용목록이 주 방어선

서버 message를 그대로 화면에 보여줄지는 **상태코드 허용목록**(`ALLOWED_MESSAGE_STATUSES = [400, 404,
409]`, `httpErrorMessage.ts:24`)이 1차로 판정한다 — 이 목록에 없는 상태(5xx 등 어떤 컨트롤러도
명시 처리하지 않는 상태)는 메시지가 아무리 정상 문구처럼 보여도 무조건 일반 문구
(`"일시적 오류입니다. 다시 시도해 주세요"`)로 대체한다. 허용목록을 통과한 메시지에는 보조로
정규식 거부목록(`looksLikeInternalDetail`, 자바 예외 클래스명·스택프레임·파일경로 패턴)을
추가로 적용한다(`httpErrorMessage.ts:38-47`). 라인 오류·장바구니 로드 실패 두 표시 지점 모두
같은 함수(`toDisplayMessage`) 하나만 쓴다(round2 QA FAIL 필수3 재작업 — 이전엔 필터가
`OrderFailureNotice` 한 곳에만 있어 이 두 지점이 서버 원문을 무필터로 노출했다).

## 5. 표시 조건(상태)

> 표시 조건은 이 화면(및 하위 부품) 각각의 `.stories.tsx`로 남겨져 있다(`.speclinker/storybook_index.json`
> 기준 10개). 한 행 = 한 스토리.
>
> ⚠ **스토리 태그 불일치(미확인 사항 §6 참조)**: 아래 스토리들은 실제로 이 화면(`CartLineItem`/
> `CartEmptyState`/`CartSummary`)의 상태이지만, `.stories.tsx` 파일의 `tags`는 `UIS-ORD-011`로
> 박혀 있다(STORY-1.md 작성 시점의 예약 ID가 그대로 남음 — 실제로 `UIS-ORD-011`은 주문서
> `shop_order` 화면에 배정됨). 컴포넌트 경로(`features/shop/Cart*.tsx`)로 대조해 이 화면 것으로
> 확인했다.

| 요소 | 표시 조건 | 근거 | 스토리 |
|------|----------|------|--------|
| 라인아이템 — 기본 | 선택됨, 조작 가능한 정상 상태 | `CartLineItem.tsx` | [기본](story:장바구니-라인아이템--기본) |
| 라인아이템 — 선택해제 | 체크박스만 꺼짐 | `CartLineItem.tsx:90` | [선택해제](story:장바구니-라인아이템--선택해제) |
| 라인아이템 — 수량변경중 | `pending=true` → 조작 버튼 비활성 + "변경 중…" 표시 | `CartLineItem.tsx:107,117,122,134` | [수량변경중](story:장바구니-라인아이템--수량변경중) |
| 라인아이템 — 재고초과안내 | 클라이언트 클램프가 최대 수량을 즉시 안내(API 호출 없음) | `CartPage.tsx:118`, `CartLineItem.tsx:125-127` | [재고초과안내](story:장바구니-라인아이템--재고초과안내) |
| 라인아이템 — 최소수량안내 | 1 미만으로 내리려는 시도를 즉시 안내(API 호출 없음) | `CartPage.tsx:118` | [최소수량안내](story:장바구니-라인아이템--최소수량안내) |
| 라인아이템 — 수량변경실패 | PATCH 400/409 응답 → 서버 message 그대로 그 줄에 노출(`shows-error`) | `CartPage.tsx:133`, `CartLineItem.tsx:128-130` | [수량변경실패](story:장바구니-라인아이템--수량변경실패) |
| 빈 장바구니 | 담긴상품 0건 | `CartPage.tsx:203-204`, `CartEmptyState.tsx` | [빈장바구니](story:장바구니-빈-장바구니--빈장바구니) |
| 합계 — 전체선택 | selectedCount === totalCount → [주문하기] 활성 | `CartSummary.tsx:28,31` | [전체선택](story:장바구니-합계--전체선택) |
| 합계 — 부분선택 | 0 < selectedCount < totalCount → [주문하기] 비활성 + "부분 선택 주문은 지원하지 않습니다 — 제외할 상품은 삭제해 주세요"(aria-describedby로 버튼과 연결) | `CartSummary.tsx:29,33-34,47-54` | [부분선택](story:장바구니-합계--부분선택) |
| 합계 — 선택없음 | selectedCount === 0(totalCount>0) → [주문하기] 비활성 + "주문할 상품을 선택해 주세요"(같은 aria-describedby id 재사용, 부분선택과 상호 배타) | `CartSummary.tsx:30,35-37` | [선택없음](story:장바구니-합계--선택없음) |

> 문서화되지 않은 추가 상태(스토리 없음, 소스 근거만): 로그인 필요(`CartPage.tsx:186-190`) ·
> 로딩 중(`CartPage.tsx:192`) · 장바구니 로드 실패(`CartPage.tsx:193-202`, [다시 시도] 포함) —
> 이 셋은 `CartPage`(컨테이너, 스토리 대상 아님, 규칙 `story-per-component` 제외 대상)가 직접
> 렌더하는 상태라 부품 스토리로 남지 않는다.

## 6. 합계 규칙 — 배송비는 임시 가정(정책 미확정)

`cartTotals.ts`의 `calcCartTotals`가 선택된 라인의 `lineTotal`만 합산해 상품금액을 구하고
(서버가 이미 계산한 `price×qty`를 그대로 쓴다 — 클라이언트가 재계산해 서버 응답을 덮지 않음),
결제예정금액 = 상품금액 + 배송비다.

> ⚠ **배송비 규칙(`SHIPPING_FEE_FLAT = 3000`, `cartTotals.ts:11`)은 서버 정본이 아니라 임시
> 가정이다.** 선택 항목이 1개 이상이면 고정 3,000원, 0건이면 0원(무료배송 기준금액 없음) —
> 명세·용어집·기존 코드 어디에도 이 규칙의 근거가 없고, 사람이 STEP 3-0 게이트 회신에서
> "계획대로 진행"으로 임시 확정한 값이다(STORY-1.md "데이터" 절, "사람 수정" 절). **정책의
> 실제 확정(기본요금·무료배송 기준금액·정본 위치)은 후속 SR-308 대상**이며, 이 화면과
> `OrderPage`/`OrderCompleteNotice`가 이 상수 하나에 함께 의존한다(배송비만큼 두 화면이 서로
> 어긋나던 결함의 재발 방지, `calcTotalsFromProductAmount` 참고).

## 7. 부분선택 체크아웃 미지원 — 서버 계약과의 정합

서버(`CartService.checkout`, `POST /api/cart/checkout`)는 **선택 여부와 무관하게 회원의 장바구니
전체를 주문으로 전환**한다(요청 바디는 `{memberId}`뿐, 계약 변경 금지 확정답변). 화면이 "선택
항목만 주문"하는 것처럼 보이는 상태로 그대로 다음 화면(주문서)에 넘기면, 화면 표시(선택분)와
실제 서버 동작(전체)이 어긋난다.

이를 막기 위해 이 화면은 **[주문하기]를 선택 개수 === 전체 개수(전체선택)일 때만 활성화**하고,
부분선택이면 안내 문구로 막는다(§5 "합계 — 부분선택"). 제외하고 싶은 상품은 개별 [삭제]로
치우는 것이 대체 경로다(개별 삭제 API는 이미 있음). 합계(상품금액/배송비/결제예정금액) 자체는
선택 상태 기준으로 계속 미리보기를 보여준다(전체선택 강제와 무관하게).

## 8. 미확인 사항

- **배송비 정책 미확정**: §6에서 명시한 대로 `SHIPPING_FEE_FLAT=3000` 규칙은 명세·용어집·기존
  코드 어디에도 근거가 없는 임시 가정이다. 실제 기본요금·무료배송 기준금액·정본 위치 확정은
  **SR-308**(후속) 대상 — 이 SR(SR-305) 및 이 화면 범위 밖.
- **부분선택 체크아웃 미지원(서버 계약 변경 필요, 범위 밖)**: `CartService.checkout`이 품목
  단위 주문을 지원하지 않아 이 화면은 "전체선택 아니면 [주문하기] 비활성"으로 우회했다.
  품목 단위 체크아웃이 필요해지면 `POST /api/cart/checkout`에 skus 파라미터를 추가하는 계약
  변경이 필요하며, 이는 새 API/필드 추가 없음이라는 SR-305 확정답변을 벗어나므로 **후속 SR
  대상**(STORY-1.md "범위 밖" 절).
- **스토리 태그(`tags`) 불일치**: §5의 10개 스토리는 실제로 이 화면(UIS-ORD-012)의 상태이지만
  `.stories.tsx`의 `tags`는 STORY 작성 시점의 예약 ID `UIS-ORD-011`을 그대로 갖고 있다(실제
  `UIS-ORD-011`은 주문서 `shop_order` 화면에 배정됨, STORY-1.md QA round1 코멘트 "예약 ID
  그대로 — 사례집 줄 45 재발 주의" 참조). `story_link.py sync --uis UIS-ORD-012 --apply`로
  태그를 교정하는 것을 권고 — 현재는 컴포넌트 경로 대조로 수동 확인해 연결했다.
- **in-flight 중 blur 커밋이 안내 없이 버려지는 결함**(§3 참조): QA round2 CONCERNS 권고3·
  round3 재작업 지시로 식별됐으나 "이번에는 고치지 않음"으로 명시 이월(STORY-1.md "후속
  추적(TODO)" 절) — 결함이지만 이번 SR 범위에서 의도적으로 미해결.
- 그 외 화면 구조·API·표시 조건·클램프/커밋 규칙은 소스(`CartPage.tsx` 외 `features/shop/Cart*`)와
  STORY-1.md 확정 답변·QA 결과로 전부 확인됨.

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-17 | SR-305 | #1 | 장바구니 화면(/shop/cart) 신규 — 수량 직접입력 draft/blur 커밋, 부분선택 체크아웃 차단(서버 전체전환 계약), 내부오류 상태코드 허용목록, 배송비 SHIPPING_FEE_FLAT=3000 임시가정(SR-308 이월) | shop-web@ca3335c |
| 2026-09-17 | SR-305 | #2 | 소스 기준 SOP급 본문 보강(화면개요·구성·입력검증·API·표시조건·배송비 임시가정·부분선택 미지원·미확인사항) | shop-web@ca3335c |
