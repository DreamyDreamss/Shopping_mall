---
uis-id: UIS-ORD-010
name: shop_products_상세
domain: order
domain-code: ORD
layer: ui
route: /shop/products/{*}
screens_role: 주화면
api_hints:
  - "GET /api/products/{sku}"
  - "POST /api/cart/items"
  - "GET /api/products"
  - "GET /api/cart"
access_control:
  - "장바구니 담기/바로 구매 버튼: 세션(memberId) 없으면 비활성 + \"로그인이 필요합니다\" — 별도 인가(auth) 게이팅 없음, 화면 자체(상품 조회)는 인증 없이 접근 가능(ProductDetailPage.tsx:126,145-146)"
  - "재고 없음(stockQty<=0)이면 담기/구매 버튼 비활성 + \"품절된 상품입니다\"(ProductDetailPage.tsx:147-149, ProductInfoPanel.tsx:70-71,94-95)"
  - "담기/구매 진행 중(addStatus==='pending')이면 버튼 비활성(연타 방지, inFlightRef 동기 잠금, ProductDetailPage.tsx:124-127,142)"
  - "프런트는 자격·재고 최종 판정을 하지 않는다 — 서버(CartService) 응답으로만 확정하고 낙관적 성공 처리 없음(성공 확인 → 로컬 카운트 가산 순서 고정, ProductDetailPage.tsx:130-136)"
anchors:
  - "modules/shop-web/src/App.tsx:102"
  - "modules/shop-web/src/pages/ProductDetailPage.tsx"
  - "modules/shop-web/src/features/shop/ProductImageGallery.tsx"
  - "modules/shop-web/src/features/shop/ProductInfoPanel.tsx"
  - "modules/shop-web/src/features/shop/ProductDetailTabs.tsx"
  - "modules/shop-web/src/features/shop/ProductNoticeTable.tsx"
  - "modules/shop-web/src/features/shop/productNoticeItems.ts"
  - "modules/shop-web/src/features/shop/RelatedProducts.tsx"
  - "modules/shop-web/src/features/shop/relatedProductsPicker.ts"
  - "modules/shop-web/src/features/shop/ProductDetailSkeleton.tsx"
  - "modules/shop-web/src/features/shop/ProductNotFoundNotice.tsx"
  - "modules/shop-web/src/api.ts:181-219"
revision_history:
  - "2026-09-17 골격 생성(spec_resync_check, zero-LLM)"
---

# UIS-ORD-010: shop_products_상세

> **근거 소스(권위):** `modules/shop-web/src/pages/ProductDetailPage.tsx` 외 `features/shop/*`(SR-304.1
> 신규 구현). DOM 스냅샷 없음(스토리북/앱 미기동, 소스폴백 모드) — 소스 슬라이스 + STORY-1.md
> (확정 답변·QA 결과) + `.speclinker/storybook_index.json`의 스토리 21건을 근거로 작성.

## 1. 화면 개요

- 라우트: `/shop/products/:sku` (spa-route, `modules/shop-web/src/App.tsx:102`)
- 목적: 쇼핑몰 상품 상세 화면(SR-304 신규). 상품 목록(SR-303, `/shop/products`)에서 상품을 선택하면
  이 경로로 진입해 대표 이미지, 가격(판매가·정가·할인율), 재고 상태를 확인하고 수량을 선택해
  장바구니에 담거나 바로 구매를 시도한다. 그 아래로 상세정보/구매정보/상품평/상품문의 탭,
  상품정보제공고시 표, 함께 보면 좋은 상품 레일을 제공한다. 옵션(색상·사이즈 등)은 없음(단일 SKU
  기준, 확정 제외) — 결제 연동도 이 화면 범위 밖이다.
- 진입 경로: `ProductListPage.handleSelect`의 `navigate('/shop/products/'+sku)`(SR-303이 남기고
  SR-304가 라우트를 닫아 도달 가능해짐, `App.tsx:99-102`) · `RelatedProducts` 카드 선택으로 다른
  sku 상세에 재진입(`ProductDetailPage.tsx:184-185`).
- 접근 권한: 화면 열람 자체는 인증 불요(공개 조회). 담기/바로 구매만 세션(memberId) 필요 — 별도
  인가(auth) 게이팅은 없다(자세한 조건은 `access_control` frontmatter 및 §5 참조).

## 2. 화면 구성

| 블록 | 역할 | 구성 요소 | 소스 근거 |
|---|------|----------|----------|
| GNB | 검색·세션 표시·장바구니 수량 배지·로그아웃(기존 화면과 공통 재사용, 신규 아님) | `Gnb` | `ProductDetailPage.tsx:9,155-156` |
| 로딩 스켈레톤 | 최초 상품 조회 응답 전 표시 | `ProductDetailSkeleton` | `ProductDetailSkeleton.tsx` |
| 조회 실패 안내 | 상품 없음(404)·조회 실패(그 외) 시 이미지/정보/탭/고지표/관련상품 전체를 대체 | `ProductNotFoundNotice` | `ProductNotFoundNotice.tsx`, `ProductDetailPage.tsx:160-163` |
| 이미지 갤러리 | 대표 이미지 + 썸네일(같은 이미지 1장 재사용, 여러 장으로 지어내지 않음) / 이미지 없음·로드 실패 시 이니셜 대체 영역 | `ProductImageGallery` | `ProductImageGallery.tsx` |
| 상품 정보 패널 | 상품명·판매가·정가·할인율·재고상태·수량 선택·[장바구니 담기]/[바로 구매]·진행 상태 표시 | `ProductInfoPanel` | `ProductInfoPanel.tsx` |
| 상세 탭 | 상세정보/구매정보(고정 정적 문구)·상품평/상품문의("준비 중") 4탭 전환 | `ProductDetailTabs` | `ProductDetailTabs.tsx` |
| 상품정보제공고시 | 전자상거래 표준 고지 항목 6종, 값은 전항목 `'-'`(값 소스 없음) | `ProductNoticeTable` + `productNoticeItems.ts` | `ProductNoticeTable.tsx` |
| 함께 보면 좋은 상품 | 현재 상품 제외, 앞 8개 가로 스크롤 레일 — 추천 알고리즘 아님. 후보 없으면 섹션 자체 숨김 | `RelatedProducts` + `relatedProductsPicker.ts` | `RelatedProducts.tsx`, `relatedProductsPicker.ts` |

> 이미지 갤러리·상품 정보 패널·상세 탭·고지표·관련상품 블록은 상품 로드 성공 시에만 렌더된다 —
> 로딩 중엔 스켈레톤만, 실패 시엔 안내만 렌더한다(셋이 상호 배타, `ProductDetailPage.tsx:158-188`).

## 3. 입력·검증 규칙

- **sku(경로 파라미터)**: `useParams<{sku}>()`로 취득. 프런트 측 형식 검증 없음 — 존재 여부는 서버
  응답(200/404)으로만 판정한다(`ProductDetailPage.tsx:36,60-80`).
- **수량(qty)**: 기본값 1. `handleQtyChange`가 `1 ~ product.stockQty` 범위로 클램프하고
  (`Math.min(Math.max(1, safe), max)`), 비정수·`NaN` 입력은 1로 대체한다(`ProductDetailPage.tsx:117-122`).
  `<input min=1 max=stockQty>`는 브라우저 힌트일 뿐 실제 강제는 이 클램프 함수가 한다. 재고가
  0이면 수량 선택 UI 자체가 렌더되지 않는다(`ProductInfoPanel.tsx:74`).
- **담기/구매 가능 조건**(`disabledReason`, `ProductDetailPage.tsx:145-149`):
  1. 세션 없음 → `"로그인이 필요합니다"`
  2. 재고 0(`stockQty<=0`) → `"품절된 상품입니다"`
  3. 위 두 사유가 모두 없을 때만 버튼 활성. `addStatus==='pending'`이면 위 사유와 무관하게 버튼만
     추가로 비활성(연타 방지, 별도 사유 문구 없음).
- **연타 방지**: `inFlightRef` 동기 플래그 — 클릭 핸들러 진입 시점에 즉시 잠그고 요청 종료(`finally`)
  시 해제한다. React state 반영 지연에 의존하지 않는다(`ProductDetailPage.tsx:124-127,142`).
- **담기 요청 파라미터**: `{memberId, sku, qty}`(`addCartItem` 호출부, `ProductDetailPage.tsx:130`).
  `memberId`는 세션에서만 취득하고 프런트가 게스트용 임의 값을 만들지 않는다(신규 신원 확인 경로
  없음).

## 4. 호출 API

| 트리거 | API | 용도 | 성공 처리 | 실패 처리 |
|---|---|---|---|---|
| 화면 진입 / sku 변경 | `GET /api/products/{sku}` (INF-ORD-009, `fetchProduct`) | 상품 단건 조회 | `product` 상태 세팅, `qty=1`·`addStatus='idle'`로 초기화 | 404 → `errorKind='notFound'`, 그 외(네트워크/5xx) → `errorKind='fetchError'`(두 사유를 하나의 default로 뭉치지 않음) |
| 화면 진입(1회) | `GET /api/products` (INF-ORD-008, `fetchProducts()`, 무인자, 기존 API 재사용) | 관련상품 레일 후보 확보(전용 API 없음) | `allProducts` 세팅 → `pickRelatedProducts`로 현재 sku 제외 앞 8개 산출 | 실패 시 조용히 빈 배열 |
| 화면 진입(세션 있을 때만) | `GET /api/cart?memberId=...` (INF-ORD-011, `fetchCartItemCount`, 기존 API 재사용) | GNB 장바구니 수량 배지 | 아이템 `qty` 합산 표시 | 실패 시 조용히 0 |
| [장바구니 담기] 클릭 | `POST /api/cart/items` (INF-ORD-010, `addCartItem`) | 담기 처리, body `{memberId, sku, qty}` | 응답 성공 확인 → 로컬 `cartItemCount += qty` → `addStatus='success'`, "장바구니에 담았습니다" 표시(순서 고정, 낙관적 처리 금지) | 400/404/409는 사유 문구 매핑, 그 외는 포괄 문구(서버 원문 미노출) — `qty`·선택 상태는 초기화하지 않음 |
| [바로 구매] 클릭 | `POST /api/cart/items` (동일 엔드포인트, `lastAction='buyNow'`) | 담기와 동일 API 호출 — 결제 연동 없음(SR 범위 제외) | 성공 시 **이동 없이** "바로 구매 대신 장바구니에 담았습니다" + 비활성 [장바구니 보기](title="준비 중", `/shop/cart`는 SR-305 몫) | 담기와 동일 |
| 관련상품 카드 선택 | (API 아님) `navigate('/shop/products/'+sku)` | 다른 상품 상세로 재진입 | 라우트 이동 후 위 진입 API들이 새 sku로 재요청 | 해당 없음 |
| [목록으로] / [다시 시도] | (API 아님) | 오류 화면의 복구 동작 | 목록으로: `navigate('/shop/products')` / 다시 시도: `load(sku)` 재호출 | 해당 없음 |

담기 실패 문구 매핑(`mapAddErrorMessage`, `ProductDetailPage.tsx:26-33`): `400` = "수량을 확인해
주세요" · `404` = "상품 또는 회원 정보를 찾을 수 없습니다" · `409` = "재고가 부족하거나 판매중지된
상품입니다" · 그 외 = "일시적 오류입니다. 다시 시도해 주세요".

> 이 화면이 신규로 만든 서버 API는 없다 — 전부 기존 `ProductController`/`CartController` 엔드포인트
> 재사용(구현 범위는 `shop-web` 프런트엔드로 한정, STORY-1.md "API 사전 확인" 절). 프런트 전용 오류
> 타입 `OrderHttpError`(HTTP status만 보관, `code` 없음, `api.ts:181-187`)는 서버 계약이 아니므로
> INF 문서에 새 오류 봉투로 반영하지 않는다.

## 5. 표시 조건(상태)

> 표시 조건은 이 화면(및 하위 부품) 각각의 `.stories.tsx`로 전부 남겨져 있다(`.speclinker/storybook_index.json`
> 기준 21개). 한 행 = 한 스토리.

| 요소 | 표시 조건 | 근거 | 스토리 |
|------|----------|------|--------|
| 로딩 스켈레톤 | 최초 상품 조회 응답 전(`loading===true`) | `ProductDetailPage.tsx:158-159`, `ProductDetailSkeleton.tsx` | [로딩](story:쇼핑상세-로딩-스켈레톤--로딩) |
| 상세 탭 — 기본 | 상품 로드 성공, 초기 탭 = '상세정보'(고정 정적 문구) | `ProductDetailTabs.tsx:20-21,33` | [기본](story:쇼핑상세-상세-탭--기본) |
| 상세 탭 — 상품평 | '상품평' 탭 선택 시 "준비 중입니다." | `ProductDetailTabs.tsx:23` | [상품평](story:쇼핑상세-상세-탭--상품평) |
| 상세 탭 — 상품문의 | '상품문의' 탭 선택 시 "준비 중입니다." | `ProductDetailTabs.tsx:24` | [상품문의](story:쇼핑상세-상세-탭--상품문의) |
| 상세 탭 — 탭전환 | 탭 4개를 실제 클릭으로 순회 — 매번 `aria-selected`·`tabpanel` 문구가 정확히 바뀜(play 함수 검증) | `ProductDetailTabs.tsx:38-45` | [탭전환](story:쇼핑상세-상세-탭--탭전환) |
| 이미지 갤러리 — 기본 | `imageUrl` 있음 → 대표 이미지 + 같은 이미지 재사용 썸네일 1개 | `ProductImageGallery.tsx:44-56` | [기본](story:쇼핑상세-이미지-갤러리--기본) |
| 이미지 갤러리 — 이미지없음 | `imageUrl=null` → 이니셜 대체 영역, 썸네일 자체 없음 | `ProductImageGallery.tsx:34-42` | [이미지없음](story:쇼핑상세-이미지-갤러리--이미지없음) |
| 이미지 갤러리 — 로드실패 | `imageUrl`은 있으나 로드 실패(`onError`) → 이미지없음과 동일하게 이니셜 대체 영역으로 폴백 | `ProductImageGallery.tsx:27,32,47` | [로드실패](story:쇼핑상세-이미지-갤러리--로드실패) |
| 상품 정보 패널 — 기본 | 정상 재고, 할인 없음(`listPrice`가 `price`보다 크지 않음) | `ProductInfoPanel.tsx:56-72` | [기본](story:쇼핑상세-상품-정보-패널--기본) |
| 상품 정보 패널 — 할인있음 | `listPrice > price` → 할인율 배지 + 정가 취소선(`discountRate.ts` 재사용, 재계산 없음) | `ProductInfoPanel.tsx:51,61,64-68` | [할인있음](story:쇼핑상세-상품-정보-패널--할인있음) |
| 상품 정보 패널 — 품절 | `stockQty<=0` → 재고 표시 "품절"(텍스트, 배지 아님 — 후속 TODO), 담기/구매 비활성 + 사유, 수량 선택 UI 숨김 | `ProductInfoPanel.tsx:50,70-71,74,94-107` | [품절](story:쇼핑상세-상품-정보-패널--품절) |
| 상품 정보 패널 — 담기실패 | `addStatus='error'` → `role=alert` 사유 문구 노출, `qty`·선택 상태 유지, 버튼 재활성 | `ProductInfoPanel.tsx:109-111` | [담기실패](story:쇼핑상세-상품-정보-패널--담기실패) |
| 상품 정보 패널 — 담기중 | `addStatus='pending'` → 버튼 문구 "담는 중…" + 비활성(연타 방지) | `ProductInfoPanel.tsx:53-54,101,105` | [담기중](story:쇼핑상세-상품-정보-패널--담기중) |
| 상품 정보 패널 — 담기성공 | `addStatus='success'`, `lastAction='cart'` → "장바구니에 담았습니다" | `ProductInfoPanel.tsx:113-119` | [담기성공](story:쇼핑상세-상품-정보-패널--담기성공) |
| 상품 정보 패널 — 바로구매성공 | `addStatus='success'`, `lastAction='buyNow'` → "바로 구매 대신 장바구니에 담았습니다" + 비활성 [장바구니 보기](title="준비 중") | `ProductInfoPanel.tsx:118-127` | [바로구매성공](story:쇼핑상세-상품-정보-패널--바로구매성공) |
| 상품 정보 패널 — 로그인필요 | 세션 없음(`disabledReason`) → 담기/구매 비활성 + "로그인이 필요합니다" | `ProductInfoPanel.tsx:94-95`, `ProductDetailPage.tsx:145-146` | [로그인필요](story:쇼핑상세-상품-정보-패널--로그인필요) |
| 조회 실패 안내 — 상품없음 | 404 → "상품을 찾을 수 없습니다" + [목록으로] | `ProductNotFoundNotice.tsx:24-32` | [상품없음](story:쇼핑상세-조회-실패-안내--상품없음) |
| 조회 실패 안내 — 조회실패 | 네트워크/5xx → "불러오지 못했습니다" + [다시 시도] | `ProductNotFoundNotice.tsx:34-41` | [조회실패](story:쇼핑상세-조회-실패-안내--조회실패) |
| 상품정보제공고시 — 기본 | 항상 표시, 6항목(품명/모델명·제조자/수입자·제조국·크기색상재질·A/S 책임자·소비자상담전화) 전부 값 `'-'` | `ProductNoticeTable.tsx`, `productNoticeItems.ts` | [기본](story:쇼핑상세-상품정보제공고시--기본) |
| 함께 보면 좋은 상품 — 기본 | 관련상품 후보(현재 sku 제외 앞 8개) 1개 이상 → 가로 레일 렌더 | `RelatedProducts.tsx:15-30` | [기본](story:쇼핑상세-함께-보면-좋은-상품--기본) |
| 함께 보면 좋은 상품 — 비어있음 | 후보 0개(현재 상품이 유일) → 섹션 자체를 렌더하지 않음(`renders-nothing`) | `RelatedProducts.tsx:16` | [비어있음](story:쇼핑상세-함께-보면-좋은-상품--비어있음) |

> 미확정/후속 TODO(§6 참조): 품절 표기가 `ProductCard`의 배지가 아니라 텍스트 한 줄(사람이 이번 SR
> 범위 밖으로 명시, STORY-1.md QA round1 권고5/round2 후속TODO3).

## 6. 미확인 사항

- **품절 표기 형태**: 확정 답변은 "품절 → 배지"였으나 구현은 텍스트 한 줄(`aria-label="재고 상태"`,
  빨간색 '품절')이다. 사람이 STORY-1.md QA round1에서 이번 SR 범위 밖으로 명시적으로 미룸(후속 SR
  또는 이 UIS 확정 시 처리) — 결함이 아니라 알려진 이월 항목이다.
- **상세정보/구매정보 탭 문구, 상품정보제공고시 항목명 목록**: `Product`에 대응 필드가 없어 확정
  문답 밖 해석으로 정적 문구/항목을 둔 것 — STEP 3-0 게이트에서 사람이 승인했다(STORY-1.md "데이터"
  절, "사람 수정" 절). 실제 상품 설명·배송정책·법정 고지 데이터가 백엔드에 추가되면 이 화면과
  스펙 모두 갱신이 필요하다.
- **"바로 구매"의 `/shop/cart` 이동**: SR-305(장바구니 화면, 별도 진행)가 완료되기 전까지는 이동하지
  않고 인라인 알림 + 비활성 [장바구니 보기]로 대체된다(사람 결정, STORY-1.md "사람 수정" 절). SR-305
  병합 후 이 동작이 바뀔 수 있다 — 이 UIS는 그 변경의 영향범위 후보다.
- **Gnb 장바구니 아이콘 자체의 활성화**: 현재 "준비 중" 비활성(SR-305 몫) — 이 화면은 건드리지 않음.
- 그 외 화면 구조·API·표시 조건은 소스(`ProductDetailPage.tsx` 외 `features/shop/*`)와 STORY-1.md
  확정 답변·QA 결과로 전부 확인됨(미확인 없음).

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-17 | SR-304 | #1 | 쇼핑 상품 상세 화면(/shop/products/{sku}) 신규 구현 — 이미지·정보패널·탭·고지표·관련상품, GET /api/products/{sku}·POST /api/cart/items 재사용, 코드 기준 소스로 역생성 | shop-web@4ebe84e |
