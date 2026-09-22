---
story-id: STORY-SR-304.1
item: SR-304.1
title: 쇼핑 상품 상세
status: Done
domain: order
created: 2026-09-17
spec_markers: 0
sr-id: SR-304
approved_sha: 312850134cec
---

# STORY-SR-304.1 — 상품 상세 화면 신규 — 이미지·가격·수량·장바구니·탭 구성 — 쇼핑 상품 상세

## Story
상품 상세 화면 신규 — 이미지·가격·수량·장바구니·탭 구성 — 쇼핑 상품 상세


## 변경 컨텍스트 (SR-304)
> 이 story는 변경요청 **SR-304 — 상품 상세 화면 신규 — 이미지·가격·수량·장바구니·탭 구성** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-304/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-304/02_변경명세.md`

### 확정된 요건 문답 7건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 상품 상세 화면 신규(경로 /shop/products/{sku}) — 이미지 영역(대표+썸네일·없으면 대체), 정보 영역(상품명·판매가·정가·할인율·재고 상태·수량 선택·[장바구니 담기]·[바로 구매]), 탭(상세정보·구매정보·상품평·상품문의 — 상품평/문의는 데이터가 없어 '준비 중' 빈 상태), 상품정보제공고시 표, 함께 보면 좋은 상품 레일. 제외: 리뷰·문의 기능 구현(데이터 없음), 결제, 옵션(단일 SKU 기준).
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 특정 화면/API만: 기존 주문 목록·상세·로그인·비밀번호 재설정 화면과 그 동작 불변 · /api/** 요청·응답·인증(X-Api-Key)·오류 계약 불변 · 기존 Thymeleaf 화면 경로 불변. 장바구니 담기는 기존 POST /api/cart/items 계약 그대로 쓴다.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 요청·응답 형식 변경 없음. 응답에 없는 값(리뷰·고지 항목 등)은 화면에서 감추고 서버에 새 필드를 요구하지 않는다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 새 오류 코드 없음. 없는 상품(404)은 '상품을 찾을 수 없습니다 + 목록으로' 화면 · 담기 실패는 사유 문구와 재시도 · 품절은 담기/구매 비활성.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 추가 화면 있음(명시): 신규 1개 — 상품 상세(/shop/products/{sku}).
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 이미지 없음 → 대체 영역 · 상품평·문의 → '준비 중' · 고지 항목 값 없음 → '-' · 상품 없음 → 안내 화면 · 조회 실패 → 다시 시도 · 품절 → 배지와 버튼 비활성 사유.
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 모든 표시 조건(§5)을 스토리로: 기본·할인있음·품절·이미지없음·담기성공(장바구니 수량 증가)·담기실패·상품없음·로딩.

### 구현 모듈(제약) — `shop-web` (`{{SRC_SHOP_WEB}}`)
이 작업 항목의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약·편성에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-304/02_변경명세.md`에서 도출)
- [x] UIS-ORD-010(예약 시점 가칭 UIS-ORD-010 — 동시 진행 SR들이 010·011을 먼저 점유해 실제 생성은 spec_resync_check가 012로 배정, 2026-09-17): 쇼핑 상품 상세 — 위 SR-304 절의 요지·문답을 계약으로 신규 구현 (모듈 `shop-web` 안에)

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**

> 변경명세에 스펙 ID가 없는 절 — 이 항목 몫인지 확인해 AC로 옮긴다: SR-304 (요구사항 요지 — 전 스펙 공통)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS**: `docs/05_설계서/order/UIS/UIS-ORD-010_shop_products_상세/spec.md` (STEP 5.5 역생성 완료)
- **신규 스펙**: ~~UIS-ORD-010(예약)~~ → **UIS-ORD-010**로 실제 생성(2026-09-17, spec_resync_check)
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

### API 사전 확인 (필수 확인 사항)
- `GET /api/products/{sku}`는 **이미 존재한다** — `ProductController.get`(`modules/shop-api/.../controller/ProductController.java:36`, `FUNC-order-007`, `INF-ORD-008`), `ProductService.get`(404 시 `ResponseStatusException(NOT_FOUND)`), `ProductDao.selectBySku`/`product.xml#selectBySku`(컬럼 명시, `SELECT *` 아님). 응답은 봉투 없이 `Product` 단건 JSON — `{sku, productName, price, stockQty, saleYn, listPrice, imageUrl}`.
- `POST /api/cart/items`도 이미 존재한다 — `CartController.addItem` → `CartService.addItem`(400 qty<1 / 404 회원·상품 없음 / 409 재고초과·품절·판매중지). 성공 응답은 `CartItem` 단건 `{sku, qty, lineTotal}`.
- 따라서 **범위 이탈 없음** — 이 항목은 `shop-web` 프런트엔드만 만들면 되고, shop-api를 건드릴 필요가 없다(구현 모듈 제약과 일치).
- 다만 두 API의 오류 응답 바디 형식이 서로 다르다: `CartController`는 `ApiExceptionHandler`(assignableTypes=CartController)가 `DuplicateKeyException`/`DataAccessException`만 가로채 `{message}`로 내고, 그 외(400/404/409, `ResponseStatusException`)는 **Spring Boot 기본 오류 바디**(`application.yml`의 `include-message: always`로 `{timestamp,status,error,message,path}`, `code` 필드 없음)를 그대로 낸다. `ProductController`도 동일(커스텀 핸들러 없음). member 계열(`{code,message}`, `api.ts`의 `ApiError`)과 **계약이 다르므로 `ApiError`를 재사용하지 않는다** — 새 경량 오류 타입을 둔다(아래 API 클라이언트 함수 절).

### 파일 (생성/수정)
**신규 — `modules/shop-web/src`**
- `pages/ProductDetailPage.tsx` — `/shop/products/:sku` 컨테이너. 데이터 오케스트레이션만, 렌더는 하위 부품에 위임(파일 크기 상한 300줄 고려).
- `pages/ProductDetailPage.test.tsx` — 통합 테스트(jsdom, `ProductListPage.test.tsx` 관례: fetch mock, `queryOf`, 테스트별 고유 sku).
- `features/shop/ProductImageGallery.tsx` + `.stories.tsx` — 대표 이미지+썸네일(현재는 원본 이미지 1장뿐이므로 썸네일=대표 이미지 재사용, 아래 "데이터" 절 참조), 이미지 없으면 `ProductCard`와 동일한 이니셜 대체 영역.
- `features/shop/ProductInfoPanel.tsx` + `.stories.tsx` — 상품명·판매가·정가·할인율(`discountRate.ts` 재사용, 재계산 금지)·재고상태·수량 선택·[장바구니 담기]/[바로 구매] 버튼, 담기 진행/성공/실패 인라인 표시.
- `features/shop/ProductDetailTabs.tsx` + `.stories.tsx` — 탭(상세정보/구매정보/상품평/상품문의). 상품평·문의는 데이터 없음 → "준비 중". 상세정보·구매정보는 백엔드에 대응 필드가 없어 **정적 안내 문구**로 채운다(가짜 데이터 생성 금지, 아래 "데이터" 절 명시).
- `features/shop/ProductNoticeTable.tsx` + `.stories.tsx` — 상품정보제공고시 표(항목-값 2열). 값 소스가 없으므로 전 항목 값은 `'-'`(확정 답변 "고지 항목 값 없음 → '-'" 그대로 전항목 적용).
- `features/shop/productNoticeItems.ts` — 고지 항목명 정적 목록(품명/모델명, 제조자/수입자, 제조국, 크기/색상/재질, A/S 책임자, 소비자상담 전화번호 등) — `shopStatic.ts`와 같은 정적 데이터 파일 패턴.
- `features/shop/RelatedProducts.tsx` + `.stories.tsx` — "함께 보면 좋은 상품" 가로 레일. `RecentlyViewed.tsx`와 동일한 레이아웃(가로 스크롤 + `ProductCard` 재사용). 후보가 없으면 섹션 자체를 숨긴다(`renders-nothing` 태그 스토리, 아래 스토리 목록).
- `features/shop/relatedProducts.ts` — 순수 함수 `pickRelatedProducts(products, currentSku, limit=8)`: 현재 sku 제외, 앞에서 limit개. 계산 로직을 컴포넌트/페이지에 복제하지 않는다(`discountRate.ts`/`productListFilters.ts`와 같은 관례).
- `features/shop/relatedProducts.unit.test.ts` — 순수 함수 단위 테스트.
- `features/shop/ProductDetailSkeleton.tsx` + `.stories.tsx` — 최초 로딩 스켈레톤("로딩" 상태를 스토리로 남기라는 확정 답변 충족 — 페이지는 스토리 대상 제외라 별도 부품으로 분리).
- `features/shop/ProductNotFoundNotice.tsx` + `.stories.tsx` — `reason: 'notFound' | 'fetchError'` prop. `notFound`="상품을 찾을 수 없습니다"+[목록으로](`/shop/products`로 navigate), `fetchError`="불러오지 못했습니다"+[다시 시도]. `ProductListGrid`의 오류 카드와 톤 통일.

**수정**
- `api.ts` — `fetchProduct(sku)`(GET), `addCartItem(memberId, sku, qty)`(POST), 신규 오류 타입 `OrderHttpError`(status 보관, `code` 없음을 반영). 기존 `ApiError`/`get`/`post`는 건드리지 않는다(회귀 범위 "member 화면 불변").
- `types.ts` — `CartItem { sku: string; qty: number; lineTotal: number }` 추가(끝에 추가, 기존 타입 불변).
- `App.tsx` — `<Route path="/shop/products/:sku" element={<ProductDetailPage/>}/>` 추가. **이걸 빠뜨리면 `ProductListPage.handleSelect`의 기존 `navigate('/shop/products/'+sku)` 호출이 여전히 아무 라우트에도 안 걸려 빈 화면이 된다** — 실패 사례집 SR-303 항목과 형태는 다르지만(그쪽은 "들어오는 링크 없음", 이쪽은 "나가는 링크는 이미 있는데 받는 라우트가 없음") 결과는 동일(도달 불가 화면)이므로 반드시 이 SR에서 라우트를 닫는다.

### 컴포넌트 트리
```
ProductDetailPage (/shop/products/:sku)
├─ Gnb (재사용, session/cartItemCount/검색/로그아웃 그대로)
├─ [loading]      → ProductDetailSkeleton
├─ [notFound|error] → ProductNotFoundNotice (reason)
└─ [product 로드됨]
    ├─ ProductImageGallery (imageUrl, productName)
    ├─ ProductInfoPanel (product, qty, onQtyChange, onAddToCart, onBuyNow, addStatus, addErrorMessage, disabledReason)
    ├─ ProductDetailTabs (product)
    ├─ ProductNoticeTable ()
    └─ RelatedProducts (products=pickRelatedProducts(allProducts, sku, 8), onSelect)
```

### API 클라이언트 함수 (`api.ts`)
```ts
export class OrderHttpError extends Error {
  status: number
  constructor(status: number, message: string) { super(message); this.status = status }
}
// 내부 공용: 응답 바디가 {message}면 그 값을, 파싱 실패/필드 없음이면 `${status} ${statusText}`.
async function parseOrderErrorMessage(r: Response): Promise<string> { /* try/catch json().message ?? fallback */ }

/** INF-ORD-008 — GET /api/products/{sku}. 404는 OrderHttpError(404, message)로 던진다. */
export function fetchProduct(sku: string): Promise<Product>

/** 기존 POST /api/cart/items 그대로. 400/404/409는 OrderHttpError로. */
export function addCartItem(memberId: string, sku: string, qty: number): Promise<CartItem>
```
기존 `fetchProducts`(목록)는 그대로 재사용해 관련상품 후보를 가져온다(신규 API 없음).

### 상태관리 방식
`ProductListPage`/`ShopHomePage`와 동일하게 페이지 로컬 `useState`+`useEffect` 조합(전역 상태 라이브러리 없음, 이 코드베이스 관례). 훅으로 추출하지 않고 페이지 파일에 orchestration만 남기고(과거 결정 "안정 코드는 리팩터링하지 않는다"는 기존 화면 얘기이므로 신규 화면인 이 페이지엔 적용 안 되지만, 관례 일관성을 위해 동일 스타일 유지), 300줄을 넘기면 `useProductDetail(sku)` 커스텀 훅으로 데이터 페칭 부분만 분리(파일 크기 상한 `should` 대응, 필요시).
- `sku`는 `useParams()`.
- `product/loading/errorKind('notFound'|'fetchError'|null)` — `requestIdRef`(세대 카운터)로 sku 변경 시 이전 응답 덮어쓰기 방지(`ProductListPage`의 `requestIdRef` 패턴 그대로).
- `session/cartItemCount` — 다른 두 페이지와 동일 패턴(세션 있을 때만 `fetchCartItemCount` 조회, 실패 시 조용히 0).
- `qty` — 기본 1, `Math.min(Math.max(1, v), stockQty)`로 클램프.
- `addStatus: 'idle'|'pending'|'success'|'error'`, `addErrorMessage` — 담기/구매 흐름 전용.
- `allProducts` — `fetchProducts()` 1회(관련상품 레일용), product 로드와 병렬로 요청 가능(서로 무관).

### 데이터
- 이미지 영역: `Product.imageUrl` 단일 필드만 존재(백엔드에 다중 이미지 배열 없음, "응답 필드 추가 요구 안 함" 확정 답변). 썸네일 목록은 **같은 이미지 1장을 재사용**해 시각적 골격만 채운다 — 여러 장인 것처럼 지어내지 않는다(플레이스홀더 데이터 금지). `imageUrl`이 없으면 대체 영역만 보여주고 썸네일 자체를 렌더하지 않는다.
- 상세정보/구매정보 탭: `Product`에 설명·배송정책 필드가 없다. "준비 중"은 확정 답변상 상품평·문의 전용이므로 이 두 탭에는 쓰지 않는다 — 대신 **고정 정적 문구**(예: 상세정보="등록된 상세 설명이 없습니다", 구매정보="배송·교환·환불 안내는 상품정보제공고시를 참고하세요" 류, 실제 문구는 구현 시 확정)로 빈 값을 명시한다. 이건 확정 문답에 없는 해석이라 **사람 확인 필요 표시**로 남긴다.
- 상품정보제공고시 표: 항목명 자체가 백엔드/용어집에 없어 전자상거래 표준 고지 항목(품명/모델명, 제조자/수입자, 제조국, 크기·색상·재질, A/S 책임자/전화번호, 소비자상담 전화번호 등) 정적 목록을 쓰고 값은 전부 `'-'`. **이것도 확정 문답 밖 해석 — 사람 확인 필요.**
- 함께 보면 좋은 상품: 전용 API/카테고리 데이터가 없어 기존 `GET /api/products`(fetchProducts, 무인자) 결과에서 현재 sku만 제외해 앞 8개를 쓴다(신규 API 없음 원칙, SR-302/303/306과 같은 패턴). 추천 알고리즘이 아니라 "표시 가능한 다른 상품"일 뿐임을 주석으로 남긴다.
- 장바구니 담기 성공 시 상단 수량: 재조회(`fetchCartItemCount`) 대신 **로컬에서 `qty`만큼 즉시 가산**(`setCartItemCount(c => c + qty)`) — "즉시 증가" 확정 답변을 왕복 없이 충족. 재조회와 값이 어긋날 수 있는 경우(다른 탭에서 동시 조작)는 이 SR 범위 밖.

### 순서·보안
1. sku 파라미터로 `fetchProduct` 요청 → 404면 `ProductNotFoundNotice(reason='notFound')`, 그 외 실패(네트워크/5xx)면 `reason='fetchError'`(재시도 가능) — 두 상태를 하나의 `default` 분기로 합치지 않는다(SR-234 r1 사례 — 미정의 코드를 default로 뭉쳐 잘못된 전이가 생긴 것과 같은 함정. 여기선 상태가 2개뿐이라 명시적으로 분기).
2. 상품 로드 성공 후에만 재고 판정(품절 시 담기/구매 비활성 + 사유) — 로딩 중엔 버튼 자체를 렌더하지 않는다(비활성 상태를 오판할 여지 차단).
3. 담기/바로구매 클릭 시: (a) `addStatus==='pending'`이면 무시(연타 방지 — 아래 "프레임워크 실행 모델 함정" 참조), (b) 세션 없음이면 API를 호출하지 않고 버튼 자체를 비활성 + "로그인이 필요합니다" 안내(신규 신원 확인 경로를 만들지 않는다 — 아래 "폴백·우회 경로" 절), (c) 위 통과 시에만 `addCartItem` 호출.
4. 담기 성공 순서: 서버 응답(성공) 확인 → 로컬 `cartItemCount` 가산 → `addStatus='success'` 표시. 순서를 바꿔 낙관적으로 먼저 카운트를 올리면 409/404 실패 시 배지만 늘고 실제 장바구니는 그대로인 불일치가 생긴다(순서 고정 필수).
5. 담기 실패는 계약에 있는 상태 코드(400/404/409)만 사유 문구로 매핑하고, 그 외(네트워크/5xx)는 별도의 포괄 문구("일시적 오류, 다시 시도")로 — 서버 메시지 원문을 그대로 노출하지 않는다(내부 정보 노출 방지, `CartService`가 SQL/제약명을 던지진 않지만 원칙 유지).
6. "바로 구매"는 결제 연동이 SR 범위에서 명시 제외이므로, 담기와 동일한 `addCartItem` 호출 후 성공 시 `/shop/cart`로 이동한다. **이 라우트는 SR-305(별도 진행 중, 미확정 병합 순서)가 만든다 — 이 SR 시점에 아직 없을 수 있다.** 아래 "범위 밖"에 리스크로 명시.

### 계약
- 새 오류 코드·응답 봉투 없음(확정 답변 그대로) — `Product`/`CartItem` 응답 형태 불변.
- 프런트 전용 신규 타입 `OrderHttpError`(HTTP status만 보관, `code` 없음)만 추가 — 서버 계약을 프런트가 새로 만들지 않는다.
- 신규 UIS-ORD-010(예약)만 생성, 기존 INF-ORD-008/CartController 계약 문서는 변경하지 않는다.

### 테스트
`ProductDetailPage.test.tsx`(HTTP 레벨, `ProductListPage.test.tsx` 관례 그대로):
- 기본 로드: 200 응답 → 상품명·가격·재고상태·탭·고지표·관련상품 렌더 확인(값 단언, `doesNotExist()`류 느슨한 매처 금지 — SR-306 #2 사례).
- 할인 있음: `listPrice > price` → 할인율·취소선 노출(`discountRate.ts` 결과와 일치 단언).
- 품절: `stockQty=0` → 담기/구매 버튼 `disabled` + 사유 텍스트 존재 단언(속성 자체를 단언, 존재만 보고 넘기지 않음).
- 이미지 없음: `imageUrl=null` → 대체 영역 렌더, `<img>` 없음 단언.
- 상품 없음(404): `ProductNotFoundNotice(notFound)` 렌더 + [목록으로] 클릭 시 `/shop/products` 이동 확인.
- 조회 실패(네트워크/500): `ProductNotFoundNotice(fetchError)` + [다시 시도] 클릭 시 재요청 발사 확인(mock 호출 횟수 단언).
- 담기 성공: `addCartItem` 200 mock → `addStatus` 성공 표시 + Gnb 배지 수량이 `qty`만큼 증가했는지 **정확한 수치**로 단언(대략 증가 아님).
- 담기 실패(409): 오류 사유 문구 노출 + 버튼 재활성(재시도 가능) 단언.
- 연타 방지: 버튼을 짧은 간격으로 2회 클릭했을 때 `addCartItem`(fetch mock) 호출이 정확히 1회인지 단언 — `act()` 한 스코프 안에서 두 클릭을 묶어 발사하고, **가드를 임시로 제거해 실제로 2회 호출로 깨지는지 먼저 확인**한 뒤 가드를 되돌린다(SR-302 #1 거짓보증 사례 대응).
- 세션 없음: 담기/구매 버튼이 비활성이고 클릭해도 `addCartItem`이 호출되지 않는지 단언.
`relatedProducts.unit.test.ts`: 현재 sku 제외, limit 적용, 후보 1개 이하일 때 빈 배열.
회귀 확인은 새 테스트 파일만이 아니라 **`npm test` 전체**로 최종 확인한다(계획에 파일을 좁혀 적어도 최종 확인은 전체 스위트 — SR-307 #1 사례, `npm run test-storybook`도 전체 실행).

### 테스트 격리
- 테스트마다 sku를 유일화(`sku-detail-1`, `sku-detail-2` 등, `ProductListPage.test.tsx`의 `product(i)` 헬퍼 패턴 재사용/변형).
- `localStorage.clear()`를 `beforeEach`에서(세션 키·최근본상품 키가 다음 테스트로 새지 않게, 기존 관례 그대로).
- 이 SR은 서버 상태(카운터·잠금 테이블)를 만들지 않으므로 SR-232/297류 잔여행 정리 이슈는 해당 없음 — fetch mock만 초기화하면 충분.

### 폴백·우회 경로의 자격 판정
- 이 화면은 신규 인증·조회 경로를 열지 않는다(`fetchProduct`/`addCartItem` 모두 기존 서버 판정 그대로 통과). 세션 없음일 때 프런트가 임의 `memberId`(예: 게스트 고정값)를 지어내 `addCartItem`을 호출하는 우회를 만들지 않는다 — 세션 없으면 호출 자체를 막는다(위 "순서·보안" 3-b).
- `requireMember`/`requireOnSale`/재고 판정은 전부 서버(`CartService`) 몫 그대로 — 프런트는 UX상 선반영(버튼 비활성)만 하고 최종 판정을 서버 응답으로 재확인한다(낙관적 성공 처리 금지, 위 순서 4).

### 프레임워크 실행 모델 함정
- React 19 StrictMode(dev)가 `useEffect`를 2번 실행 — `fetchProduct(sku)`는 GET(멱등)이라 중복 호출 자체는 안전하지만, 응답 도착 순서가 뒤바뀌면(느린 첫 요청이 나중에 도착) 화면이 잠깐 잘못된 상품으로 깜빡일 수 있다 → `ProductListPage`와 동일한 `requestIdRef` 세대 카운터로 최신 요청 응답만 반영.
- `addCartItem`은 **POST이자 비멱등**(`CartService.addItem`이 UPSERT로 수량을 합산) — StrictMode는 클릭 이벤트 핸들러를 이중 실행하지 않으므로 이중 마운트 자체는 문제 없지만, **사용자 더블클릭**은 실제로 두 번의 POST를 만들어 qty가 의도치 않게 2배로 합산된다. `addStatus==='pending'`일 때 버튼을 즉시 `disabled`로 바꾸는 동기 가드가 필요(비동기 상태 반영 지연에 의존하지 않고 클릭 핸들러 진입 시점에 ref로 즉시 잠금 — `ShopHomePage`의 `inFlightRef` 패턴과 동일 원리).
- 해당 없음: 회전형 토큰·스케줄러 중복 실행 함정은 이 화면에 없음.

### 범위 밖
- 리뷰·문의 실제 기능(데이터 없음, 확정 제외) — 상품평/상품문의 탭은 "준비 중" 고정.
- 결제 연동(확정 제외) — "바로 구매"는 담기 후 `/shop/cart`로 이동하지 않는다(아래 "### 사람 수정" 참조 — SR-305 라우트가 아직 없어 이동시키지 않는다).

### 사람 수정 (STEP 3-0 게이트, 2026-09-17)
- **결정**: 계획대로 진행. 탭 문구·고지 항목은 제안대로(값 `'-'`, 지어내지 않음).
- **"바로 구매" 방식 변경(계획 §순서·보안 6, §범위 밖 대체)**: `/shop/cart`로 이동시키지 않는다(SR-305 미완료 — 없는 화면으로 보내지 않는다). 대신 "바로 구매"도 "장바구니 담기"와 동일하게 `addCartItem` 호출 후, **이동 없이 그 자리에서** "장바구니에 담았습니다" 알림 + "장바구니 보기" 버튼(비활성, "준비 중" 사유)을 보여준다. 즉 이 SR 범위에서 "바로 구매" 버튼은 기능적으로 "장바구니 담기"와 동일한 API 호출을 수행하고 성공 UI만 살짝 다르다(라벨 구분 유지, 결과 알림에 "바로 구매" 문맥 반영).
- **담기 실패 시**: 사유를 그 자리(인라인)에 표시하고 수량·선택 상태(qty)는 초기화하지 않는다(재시도 시 다시 입력하지 않도록).
- **라우트**: `/shop/products/:sku`는 이번 SR에서 반드시 추가(SR-303이 남긴 `ProductListPage`의 이동 대상을 닫는다) — 계획대로.
- **완료 조건(사람이 명시)**: 없는 sku → 안내 화면(목록으로) · 품절 상품은 담기·구매 비활성+사유 · 담기 성공 시 GNB 장바구니 수량 증가 · 담기 실패 시 상태(qty 등) 유지 · 탭 4개 전환 · 고지 표 항목 표시 · 스토리에 이미지 없음·로드 실패(`shows-error` 태그) 포함 · 회귀는 `npm test` + `npm run test-storybook` 전체.
- 옵션(색상/사이즈 등) — 단일 SKU 기준(확정 제외).
- 상세정보/구매정보 탭 문구, 상품정보제공고시 항목명 목록 — 확정 문답에 없는 해석(위 "데이터" 절 표시) — STEP 3-0 게이트에서 사람이 문구를 확정하거나 승인해야 한다.
- 관련 상품 추천 알고리즘 — 없음, 단순 "현재 상품 제외한 앞 N개"(위 "데이터" 절 명시, 추천 로직 구현 아님).
- Gnb 장바구니 아이콘 자체의 활성화(현재 "준비 중" 비활성, SR-305 몫) — 이 SR은 건드리지 않는다.

### 스토리 목록
- `ProductImageGallery.stories.tsx`: 기본(이미지 있음), 이미지없음.
- `ProductInfoPanel.stories.tsx`: 기본, 할인있음, 품절, 담기실패, 담기중(pending, 버튼 비활성), 담기성공, 로그인필요(세션 없음 비활성).
- `ProductDetailTabs.stories.tsx`: 기본(상세정보 탭), 상품평(준비중), 상품문의(준비중).
- `ProductNoticeTable.stories.tsx`: 기본(전항목 '-').
- `RelatedProducts.stories.tsx`: 기본(N개), 비어있음(`tags: ['renders-nothing']`).
- `ProductDetailSkeleton.stories.tsx`: 로딩.
- `ProductNotFoundNotice.stories.tsx`: 상품없음, 조회실패.
확정 답변 "모든 표시 조건(§5)을 스토리로: 기본·할인있음·품절·이미지없음·담기성공·담기실패·상품없음·로딩" 8개 상태 전부 위 목록에서 커버됨.

### 테스트 목록 (요약)
`ProductDetailPage.test.tsx`(9케이스: 기본/할인/품절/이미지없음/상품없음/조회실패/담기성공/담기실패/연타방지/세션없음 — 일부 통합), `relatedProducts.unit.test.ts`(3~4케이스).

### 실패 사례집 대조 (`harness/antipatterns.all.md`)
- **SR-303 "신규 화면 진입점 누락"** — 조건: 새 화면을 만들며 그 화면으로 가는 링크를 안 만듦. 여기선 **반대 방향**이 성립: 들어오는 링크(`ProductListPage.handleSelect`)는 이미 있는데 받는 `<Route>`가 없다. 조건이 정확히 같진 않지만 결과(도달 불가 화면)가 같으므로, 이 SR에서 반드시 `App.tsx`에 라우트를 추가한다(위 "파일" 절에 명시).
- **SR-306 #1 r1 "할인율 부동소수점 순서"** — 조건: 나눗셈을 먼저 하는 재구현. 여기선 `discountRate.ts`의 기존 함수를 **그대로 import**해서 쓰고 재계산하지 않으므로 이 조건 자체가 성립하지 않는다(재구현 금지를 계획에 명시해 재발 차단).
- **SR-306 #1 r3~r4 "스토리북 이미지 404가 축E를 막음"** — 조건: 의도적 오류 상태 스토리가 실제 네트워크 404를 유발. 이 SR은 "이미지없음" 상태를 `imageUrl=null`(요청 자체가 없음)로만 구현하고 "이미지 로드 실패"(깨진 URL) 상태는 확정 답변에 없어 만들지 않으므로 이 조건이 발생하지 않는다.
- **SR-302 #1 "연타 방지 테스트 거짓보증"** — 조건: `fireEvent.click()` 분리 호출로 가드를 우회 검증. 위 "테스트" 절의 연타 방지 케이스는 가드를 임시 제거해 실제 실패를 먼저 확인하는 절차를 명시해 이 함정을 피한다.
- **SR-307 #1 "회귀 확인 범위 축소"** — 조건: 계획이 테스트 범위를 특정 클래스로 좁힘. 위 "테스트" 절에서 최종 확인은 전체 스위트로 명시해 대응.
- **SR-306 #2 "느슨한 매처 거짓보증"** — 조건: `doesNotExist()`/여러 상태 허용 매처로 계약을 못 고정. 위 테스트 케이스들은 전부 정확한 값(수량 증가분, disabled 속성, 호출 횟수)을 단언하도록 명시.
- **SR-232 r2 "탈퇴 회원 자격 우회"** — 직접 조건(API 키 폴백)은 이 SR에 없지만 동일 원리(자격 판정을 프런트가 대신하지 않음)를 "폴백·우회 경로" 절에 명시적으로 반영.

## 구현 Task
- [x] 컨트롤러/핸들러 — 해당 없음(신규 백엔드 API 없음, 기존 `ProductController`/`CartController` 재사용)
- [x] 서비스/비즈니스 로직 — 프런트 오케스트레이션(`ProductDetailPage`) + 순수 함수(`relatedProductsPicker.ts`)
- [x] 데이터 접근 레이어 — `api.ts`(`fetchProduct`/`addCartItem`/`OrderHttpError`)
- [x] 단위 테스트 — `ProductDetailPage.test.tsx`(10케이스) + `relatedProductsPicker.unit.test.ts`(5케이스)

## Dev 기록
계획 확인: 계획대로 진행(사람 수정 반영 — "바로 구매"는 `/shop/cart` 이동 없이 그 자리 알림으로 대체, 탭 문구·고지 항목은 제안대로) (2026-09-17)

### 구현 완료 (2026-09-17)

**생성 파일** (모듈 `shop-web`, `{{SRC_SHOP_WEB}}`):
- `modules/shop-web/src/pages/ProductDetailPage.tsx` — `/shop/products/:sku` 컨테이너
- `modules/shop-web/src/pages/ProductDetailPage.test.tsx` — 통합 테스트 10케이스
- `modules/shop-web/src/features/shop/ProductImageGallery.tsx` / `.stories.tsx`
- `modules/shop-web/src/features/shop/ProductInfoPanel.tsx` / `.stories.tsx`
- `modules/shop-web/src/features/shop/ProductDetailTabs.tsx` / `.stories.tsx`
- `modules/shop-web/src/features/shop/ProductNoticeTable.tsx` / `.stories.tsx`
- `modules/shop-web/src/features/shop/productNoticeItems.ts`
- `modules/shop-web/src/features/shop/RelatedProducts.tsx` / `.stories.tsx`
- `modules/shop-web/src/features/shop/ProductDetailSkeleton.tsx` / `.stories.tsx`
- `modules/shop-web/src/features/shop/ProductNotFoundNotice.tsx` / `.stories.tsx`
- `modules/shop-web/src/features/shop/relatedProductsPicker.ts` — 순수 함수 `pickRelatedProducts`
- `modules/shop-web/src/features/shop/relatedProductsPicker.unit.test.ts` — 유닛테스트 5케이스

**수정 파일**:
- `modules/shop-web/src/api.ts` — `OrderHttpError`, `fetchProduct`, `addCartItem` 추가(기존 `ApiError`/`get`/`post`는 불변)
- `modules/shop-web/src/types.ts` — `CartItem` 타입 추가(끝에 추가, 기존 타입 불변)
- `modules/shop-web/src/App.tsx` — `<Route path="/shop/products/:sku" .../>` 추가(SR-303이 남긴 `ProductListPage.handleSelect`의 도달 불가 화면을 닫음)

**계획과 다르게 간 부분**(사전 코드 미확인 사항, 계획 위반 아님):
- 계획의 파일명 `features/shop/relatedProducts.ts`(순수 함수)를 `relatedProductsPicker.ts`로 바꿨다 — Windows(대소문자 구분 없는 파일시스템)에서 `RelatedProducts.tsx`(컴포넌트)와 `relatedProducts.ts`(로직)가 파일명 대소문자만 다른 충돌로 `tsc`가 "두 파일이 대소문자만 다르다"는 에러를 냈다(TS1149/TS1261, 실측). 함수 시그니처(`pickRelatedProducts(products, currentSku, limit=8)`)와 동작은 계획 그대로다.
- `ProductDetailTabs`는 계획의 컴포넌트 트리에 `(product)` 인자가 표시돼 있었지만, 실제로는 상세정보/구매정보 문구가 고정 정적 텍스트라 `product`를 전혀 참조하지 않는다 — `noUnusedParameters` 컴파일 규칙과 충돌해 prop 자체를 받지 않도록 했다(대신 스토리북에서 특정 탭을 바로 보여주기 위한 `initialTab?` prop만 둠). 페이지에서도 `<ProductDetailTabs />`로 인자 없이 호출한다.
- 담기 실패(400/404/409) 사유 문구는 계획에 정확한 문구가 없어 이 자리에서 확정했다: 400="수량을 확인해 주세요", 404="상품 또는 회원 정보를 찾을 수 없습니다", 409="재고가 부족하거나 판매중지된 상품입니다", 그 외="일시적 오류입니다. 다시 시도해 주세요"(서버 원문 미노출, STORY "순서·보안" 5).

**검증**:
- `npm test`(타입체크 `tsc --noEmit` + jest 전체) — 12 스위트 130개 테스트 전부 통과(신규 15개 포함).
- 연타 방지 가드(`inFlightRef`) 실효성 직접 확인(SR-302 #1 거짓보증 사례 대응) — 가드를 임시로 주석 처리하고 "연타 방지" 테스트만 단독 실행해 실제로 `addItemCalls.length === 2`로 깨지는 것을 확인(Expected 1, Received 2), 가드를 복원한 뒤 다시 통과함을 재확인.
- `npm run build-storybook` 성공 + `npx test-storybook --url http://127.0.0.1:<port>/`(로컬 정적 서버로 스토리북 산출물 서빙) — 28개 스토리 파일, 98개 스토리 전부 PASS(신규 7개 스토리 파일 포함, `RelatedProducts.stories.tsx`의 `renders-nothing` 태그 상태 포함).

### 재작업 완료 (round 2, 2026-09-17)

QA CONCERNS 권고 1·2·3·4(사람 코멘트가 이번 라운드에 확정한 4건)를 반영했다. 5(품절 배지)는
사람 지시대로 이번엔 손대지 않음(범위 밖 유지).

**수정 파일**:
- `modules/shop-web/src/features/shop/ProductImageGallery.tsx` — (재작업 지시 1) `THUMBNAIL_COUNT=3` 제거. 이제 `imageUrl`이 있을 때만 같은 이미지를 재사용한 썸네일 **정확히 1개**를 렌더하고(이미지 수=썸네일 수, 0 또는 1), 없으면 썸네일 자체를 렌더하지 않는다 — 사람이 승인한 계획 그대로 되돌렸다. (재작업 지시 4) `ProductCard.tsx`와 동일한 `onError` 폴백을 추가했다(`useState`+`onError={() => setImgLoadFailed(true)}` → 이니셜 대체 영역으로 폴백). 부수 결정: `imageUrl` prop이 바뀔 때(sku 이동) `imgLoadFailed`를 `useEffect`로 리셋한다 — 이 부품이 컨테이너에서 sku 변경 간 재마운트되지 않을 가능성에 대비한 방어(계획에 없던 추가 — 정당성: 이전 상품에서의 로드 실패 상태가 다음 상품에 새는 것을 막기 위함, 부작용 없음).
- `modules/shop-web/src/features/shop/ProductImageGallery.stories.tsx` — `로드실패` 스토리 추가(`tags: ['shows-error']`). 실제 네트워크 404가 아니라 **깨진 data URI**(`data:image/png;base64,not-a-real-image-!!`)로 재현해 축 E(콘솔 오류 판정)가 스토리북 자체를 막지 않게 했다(SR-306 #1 r3~r4 사례 재발 방지).
- `modules/shop-web/src/features/shop/ProductInfoPanel.stories.tsx` — (재작업 지시 2) `바로구매성공`(`lastAction: 'buyNow'`) 스토리 추가. 기존 `담기성공`(`lastAction: 'cart'`)만 있어 "바로 구매" 성공 시의 문구·비활성 [장바구니 보기] 상태가 스토리로 남아 있지 않았다.
- `modules/shop-web/src/pages/ProductDetailPage.test.tsx` — (재작업 지시 2) 신규 테스트 `바로 구매 — addCartItem은 호출되지만 라우트 이동은 없다` 추가. `useLocation()`으로 pathname을 항상 노출하는 `LocationProbe`를 라우터에 추가하고, `/shop/cart`에 감시용 placeholder 라우트를 둬서 "바로 구매" 클릭 전후 pathname이 `/shop/products/{sku}`로 동일하고 `/shop/cart` placeholder가 렌더되지 않음을 직접 단언한다(다음 SR에서 `navigate('/shop/cart')`가 몰래 추가돼도 이 테스트가 깨지도록).
- `modules/shop-web/src/features/shop/ProductDetailTabs.stories.tsx` — (재작업 지시 3) `탭전환` 플레이 함수 스토리 추가(`RankingSection.stories.tsx`의 "가격탭전환"과 동일 패턴, `storybook/test`의 `userEvent.click`+`expect`). 탭 4개를 실제 클릭으로 순회하며 매번 `aria-selected`와 `tabpanel` 문구가 정확히 바뀌는지 단언한다 — 기존 3개 스토리(`initialTab` prop)는 그대로 두되(정적 상태 확인용으로는 여전히 유효), `onClick`의 `setTab` 자체가 깨지는 것을 잡는 스토리를 별도로 추가했다.

**계획과 다르게 간 부분(재작업, 정당성 포함)**:
- 계획/QA 권고는 "탭 클릭 테스트 또는 스토리 play 함수 1건"을 요구했다 — 이 코드베이스에 컴포넌트 단위 `.test.tsx` 파일 관례가 없고(페이지만 `.test.tsx`, 부품은 순수 함수만 `.unit.test.ts`) `RankingSection.stories.tsx`/`LoginForm.stories.tsx`가 이미 play 함수로 상호작용을 검증하는 선례가 있어, 페이지 통합 테스트에 탭 클릭을 추가하는 대신 `ProductDetailTabs.stories.tsx`에 play 함수로 넣었다(동작은 동일 — 실제 클릭 이벤트로 상태 전환을 검증).
- "바로 구매 이동 없음" 검증은 인간 코멘트의 "location 해시 불변" 표현을 테스트 환경(MemoryRouter, 실제 앱은 HashRouter)에 맞게 `useLocation().pathname` 프로브로 구현했다 — 계약(이동 없음)은 동일하게 검증되고, 실제 앱에서의 "해시 불변"과 테스트에서의 "pathname 불변"은 각 라우터 구현의 등가 개념이다.

**검증**:
- `npm test`(`tsc --noEmit` + jest 전체) — 12 스위트 131개 테스트 전부 통과(신규 1케이스 포함, round 1의 130 + 1).
- `npm run build-storybook` 성공 + `npx test-storybook --url http://127.0.0.1:6396`(정적 서버로 산출물 서빙) — 28개 스토리 파일, **101개** 스토리 전부 PASS(round 1의 98 + 신규 3: `ProductImageGallery`의 `로드실패`, `ProductInfoPanel`의 `바로구매성공`, `ProductDetailTabs`의 `탭전환`).

**재작업 지시 항목별 반영 확인**:
1. [medium/spec] 반영 — `THUMBNAIL_COUNT` 제거, 썸네일=이미지 수(0 또는 1)로 되돌림. 이번엔 이 변경 자체가 이 Dev 기록에 명시돼 있다(숨기지 않음).
2. [medium/spec] 반영 — `바로구매성공` 스토리 1개 + "바로 구매 클릭 후 addCartItem 1회 호출·라우트 pathname 불변"을 직접 단언하는 테스트 1개 추가.
3. [medium/spec] 반영 — `ProductDetailTabs.stories.tsx`에 실제 클릭으로 탭 4개를 순회하는 play 함수 스토리 추가(`initialTab` 뒷문이 아닌 `onClick` 상호작용 행사).
4. [low/regression] 반영 — `ProductImageGallery`에 `ProductCard`와 동일한 `onError` 폴백 추가. 스토리는 실제 404가 아니라 깨진 data URI 사용(`shows-error` 태그).
5. [low/spec] 미반영(사람 지시대로 범위 밖 유지) — 품절 표시는 이번 라운드에서 손대지 않았다. 후속 SR 또는 UIS-ORD-010 확정 시 처리.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-17 — CONCERNS
- **Layer1 스펙**: concerns. AC(UIS-ORD-010)의 5개 영역(이미지·정보·탭·고지표·관련상품)과 확정 문답 7건 요건이 실제 코드에 전부 구현돼 있음을 직접 확인했다 — 없는 상품(404) 안내+[목록으로](`ProductNotFoundNotice.tsx:24-32`, `ProductDetailPage.tsx:76` 404/그외 명시 분기), 품절 비활성+사유(`ProductInfoPanel.tsx:50,54,94-107`), 이미지 없음 대체 영역(`ProductImageGallery.tsx:25-33`), 고지표 전항목 `'-'`, 상품평·문의 "준비 중", 할인율은 `discountRate.ts` 재사용(재계산 없음 — SR-306 #1 r1 재발 없음). **사람 수정 준수 확인**: "바로 구매"는 `handleAdd('buyNow')`로 `addCartItem`만 호출하고 `navigate` 호출이 전혀 없다(`ProductDetailPage.tsx:124-143`, 페이지 전체에서 `navigate`는 검색·목록으로·관련상품 3곳뿐). 성공 시 그 자리에서 "바로 구매 대신 장바구니에 담았습니다" + 비활성 [장바구니 보기](`title="준비 중"`, Gnb 장바구니 아이콘의 기존 관례와 동일) — 사람 수정 그대로다. "계획과 다르게 간 부분" 3건(파일명 `relatedProductsPicker.ts`, `ProductDetailTabs` prop 제거, 담기 실패 문구 확정)은 전부 사전 미확인 사항에 대한 정당한 대응이고 동작 계약을 바꾸지 않는다 — 계획 위반 아님. 다만 **미공개 계획 이탈 1건**(썸네일 3장, 아래 권고 1)과 사람 완료조건 대비 검증 공백 2건이 남았다.
- **Layer2 보안**: pass. 세션 없으면 버튼 비활성 + 핸들러 진입 가드(`!session?.memberId` → return)로 API 호출 자체를 막고, 게스트 `memberId`를 지어내는 우회가 없다(SR-232 r2 원리 준수). 자격·재고 최종 판정은 서버(`CartService`) 몫 그대로이고 프런트는 낙관적 성공 처리를 하지 않는다(응답 성공 → 카운트 가산 → success 순서 고정, `ProductDetailPage.tsx:131-136`). 서버 오류 원문은 화면에 노출되지 않는다(`mapAddErrorMessage`가 status만 보고 고정 문구 매핑, 그 외는 포괄 문구). 부품 직접 fetch 0건 — `fetch`는 `src/api.ts`에만 있다(규칙 `web-fetch-only-in-api` 준수). `encodeURIComponent` 적용 확인.
- **Layer3 회귀**: pass. 기존 계약 불변을 직접 확인했다 — `api.ts`의 `ApiError`/`get`/`post`/`postVoid`/`parseErrorBody` 및 기존 export 전부 무변경(신규 `OrderHttpError`/`fetchProduct`/`addCartItem`은 파일 끝에 추가), `types.ts`는 `CartItem`만 끝에 추가(기존 타입 필드·순서 불변), `App.tsx`는 import 1줄 + `<Route path="/shop/products/:sku">` 1줄만 추가(기존 5개 라우트·`applyShopBootRedirect` 무변경, 정적 세그먼트 `/shop/products`와 충돌 없음). SR-303이 남긴 도달 불가 화면(`ProductListPage.handleSelect`의 navigate 대상)이 닫혔다 — 사례집 "신규 화면 진입점 누락" 재발 없음. **QA가 직접 전체 스위트 재실행**: `npm test` 12 스위트 130 테스트 전부 통과(Dev 기록과 일치, SR-307 #1 "회귀 범위 축소" 재발 없음). **연타 방지 가드 실효성도 QA가 직접 뮤테이션으로 재확인**: `ProductDetailPage.tsx:125`의 `if (inFlightRef.current) return`을 제거하고 해당 테스트만 돌리자 `Expected length: 1 / Received length: 2`로 실제 실패 → 가드 복원 후 130/130 재통과. SR-302 #1 "연타 테스트 거짓보증"이 아니다(테스트가 `act()` 한 스코프 안에서 두 클릭을 묶어 발사하고, 버튼 노드가 사라지지 않아 가드를 실제로 행사한다).
- 권고(CONCERNS시):
  1. **(medium · 스펙/미공개 계획 이탈) 썸네일이 같은 이미지 3장으로 렌더된다** — `ProductImageGallery.tsx:6` `THUMBNAIL_COUNT = 3`이 동일 `imageUrl`을 3번 반복 렌더한다. 사람이 승인한 계획 §데이터는 "썸네일 목록은 **같은 이미지 1장**을 재사용해 시각적 골격만 채운다 — **여러 장인 것처럼 지어내지 않는다**(플레이스홀더 데이터 금지)"였다. 화면상 썸네일 3칸은 사용자에게 "이미지가 3장 있다"로 읽혀 그 금지 조항에 정면으로 걸린다. 게다가 이 변경은 Dev 기록의 "계획과 다르게 간 부분" 3건에 들어 있지 않아 사람이 알 수 없었다. → `THUMBNAIL_COUNT = 1`로 되돌리거나, 3장을 유지하려면 사람이 명시 승인하고 UIS-ORD-010에 그 표시 규칙을 적는다.
  2. **(medium · 검증 공백) 사람 수정의 핵심("바로 구매"가 `/shop/cart`로 이동하지 않는다)을 고정하는 테스트·스토리가 없다** — `ProductDetailPage.test.tsx` 10케이스 중 [바로 구매] 버튼을 누르는 것이 하나도 없고, `ProductInfoPanel.stories.tsx`의 담기성공 스토리는 `lastAction: 'cart'`만 있다. 즉 사람이 STEP 3-0에서 명시적으로 바꾼 동작(이동 없음 + "바로 구매" 문맥 알림 + 비활성 [장바구니 보기])이 지금은 코드 리뷰로만 보증되고, 다음 SR(SR-305 장바구니 화면)에서 누군가 `navigate('/shop/cart')`를 넣어도 어떤 게이트도 잡지 못한다. 확정 답변의 "담기성공" 상태 스토리도 buyNow 변형이 빠져 있다. → `바로구매성공`(lastAction='buyNow') 스토리 1개 + "바로 구매 클릭 시 `addCartItem` 1회 호출되고 라우트가 `/shop/products/:sku`에 그대로 남는다"를 단언하는 테스트 1개를 추가한다.
  3. **(medium · 검증 공백) 사람이 명시한 완료 조건 "탭 4개 전환"을 실제 전환 상호작용으로 검증하는 것이 0건이다** — 페이지 테스트는 `tablist` 존재만 단언하고(`ProductDetailPage.test.tsx:100`), 스토리 3개는 전부 `initialTab` prop으로 상태를 세운다. `ProductDetailTabs`의 `onClick={() => setTab(t.key)}`를 행사하는 경로가 없어, 탭 클릭이 깨져도(예: `setTab` 제거) 테스트·스토리북 둘 다 통과한다 — 상태를 뒷문(prop)으로 세워 보여 주는 것은 SR-302 #1 거짓보증과 같은 클래스다. → 탭 버튼을 클릭해 패널 문구가 4개 전부 바뀌는지 단언하는 테스트(또는 스토리 play 함수) 1건을 추가한다.
  4. **(low · 일관성) 상세 화면에는 이미지 로드 실패 폴백이 없다** — `ProductCard.tsx:41`은 `onError`로 이니셜 대체 영역으로 떨어지는데, `ProductImageGallery`는 `imageUrl=null`만 처리하고 `onError`가 없다. 실제 URL이 깨지면 목록 카드는 이니셜, 상세 화면은 브라우저 깨진 이미지 아이콘으로 서로 다르게 보인다. 계획이 "이미지 로드 실패 상태는 확정 답변에 없어 만들지 않는다"로 의도한 것(SR-306 #1 r3~r4 대조)이라 이번 라운드 차단 사유는 아니다 — 후속 TODO로 남긴다(폴백을 넣더라도 스토리는 404가 아니라 깨진 data URI를 쓸 것).
  5. **(low · 표기) 품절 표시가 '배지'가 아니라 텍스트 한 줄이다** — 확정 답변 §빈값·오류 상태는 "품절 → **배지**와 버튼 비활성 사유"인데, 구현은 `aria-label="재고 상태"` 텍스트(빨간색 '품절') + 사유 문구다. 사용자가 품절임을 알고 버튼 사유도 보이므로 요건 취지는 충족되나, `ProductCard`의 품절 배지와 시각 표현이 다르다. 후속 TODO 또는 UIS-ORD-010에 현행 표기로 확정.
- 재동기화 입력(STEP 5.5 — UIS-ORD-010 본문 역생성 시 반영):
  1. "바로 구매"의 최종 동작 = `POST /api/cart/items` 호출 후 **화면 이동 없이** 인라인 알림 + 비활성 [장바구니 보기](준비 중). 결제·`/shop/cart` 이동은 이 스펙의 계약이 아니다(SR-305 몫).
  2. 담기 실패 문구 4종(400 "수량을 확인해 주세요" / 404 "상품 또는 회원 정보를 찾을 수 없습니다" / 409 "재고가 부족하거나 판매중지된 상품입니다" / 그 외 포괄)과 "서버 원문 미노출" 규칙.
  3. 탭 4종 고정 문구(상세정보/구매정보는 정적 안내, 상품평·문의는 "준비 중"), 상품정보제공고시 항목 6종(`productNoticeItems.ts`) 전항목 값 `'-'`.
  4. 썸네일 표시 규칙(위 권고 1의 사람 결정에 따라 1장 또는 3장)과 품절 표기 형태(위 권고 5).
  5. 프런트 전용 오류 타입 `OrderHttpError`는 서버 계약이 아니다 — INF 문서에 새 오류 봉투로 적지 않는다(`ProductController`/`CartController`는 Spring 기본 오류 바디 그대로).

### QA Gate — 2026-09-17 — PASS (round 2)
> round 1 CONCERNS 권고 1·2·3·4(사람이 이번 라운드 범위로 확정한 4건)의 실제 반영 여부를 **코드를 직접 읽고
> 뮤테이션으로 실효성까지** 검증했다. 5(품절 배지)는 사람이 범위 밖으로 명시해 이번 라운드 지적 대상이 아니다.

- **Layer1 스펙**: pass. 재작업 4건이 전부 **실물로** 반영됐고, 그중 "검증 추가" 2건은 QA가 뮤테이션으로 실효성을 직접 확인했다.
  1. **(권고 1 — 썸네일) 반영 확인.** `ProductImageGallery.tsx`에서 `THUMBNAIL_COUNT` 상수가 완전히 사라졌고, 이미지가 있을 때만 `thumbBoxStyle` 블록 **하나**를 렌더한다(`ProductImageGallery.tsx:50-54`, 반복문 없음). 이미지가 없으면 조기 반환으로 이니셜 대체 영역만 렌더하고 썸네일 컨테이너 자체가 없다(`:34-42`). QA가 실제 브라우저(storybook chromium)에서 `기본` 스토리의 `<img>` 개수를 세어 **정확히 2개**(대표 1 + 썸네일 1)임을 확인 — 사람 완료조건 "썸네일 개수 = 이미지 수(1장 또는 0장)" 충족.
  2. **(권고 2 — 바로 구매 이동 없음) 반영 확인 + 뮤테이션 검증 통과.** `ProductDetailPage.test.tsx:225-247`이 `useLocation()` 프로브와 `/shop/cart` 감시용 placeholder 라우트로 클릭 전후 pathname 불변·placeholder 미렌더를 **직접 단언**한다(존재 확인이 아니다). QA가 `handleAdd` 성공 분기에 `if (action === 'buyNow') navigate('/shop/cart')`를 주입하자 이 테스트가 실제로 깨졌다 — `Expected: /shop/products/sku-detail-11 / Received: /shop/cart`. 가드가 아니라 **진짜 계약을 고정하는 테스트**다(다음 SR에서 이동이 몰래 추가되면 잡힌다). `ProductInfoPanel.stories.tsx:62-64` `바로구매성공`(`lastAction:'buyNow'`) 스토리도 추가돼 "바로 구매" 문맥 알림 + 비활성 [장바구니 보기] 상태가 실물로 남았다.
  3. **(권고 3 — 탭 4개 실전 전환) 반영 확인 + 뮤테이션 검증 통과.** `ProductDetailTabs.stories.tsx:30-62` `탭전환` play 함수가 `userEvent.click`으로 4개 탭을 순회하며 매번 `aria-selected` 전환과 `tabpanel` 문구를 단언하고 상세정보로 복귀까지 확인한다 — `initialTab` 뒷문이 아니라 `onClick`을 실제로 행사한다. QA가 `onClick={() => setTab(t.key)}`를 무력화하자 `탭전환`만 실패(`aria-selected` Expected true / Received false)하고 **기존 `initialTab` 스토리 3개는 그대로 통과**했다 — round 1이 지적한 거짓보증이 정확했음이 실증됐고, 이번에 추가된 스토리가 그 구멍을 실제로 막았다.
  4. **(권고 4 — onError 폴백) 반영 확인 + 동작 검증.** `ProductImageGallery.tsx:27,32,47`이 `ProductCard.tsx:21,25,41`과 **동일한 패턴**(`useState(false)` + `showImage = !!imageUrl && !imgLoadFailed` + `onError={() => setImgLoadFailed(true)}`)을 쓴다 — 변수명·구조까지 일치. QA가 `로드실패` 스토리에 임시 play 함수를 붙여 실제 브라우저에서 확인: 깨진 data URI가 `onError`를 발화시켜 `<img>`가 사라지고 이니셜 대체 영역(`대표이미지 없음`)이 렌더된다(검증 후 임시 코드는 제거, 트리 원복 확인). `shows-error` 태그도 사람 지시대로 달렸고, 실제 404가 아닌 data URI라 SR-306 #1 r3~r4(스토리북 404가 축E를 막은 사례)가 재발하지 않는다.
  - **이탈 기록 투명성(round 1 재발 없음)**: round 1의 핵심 문제는 "썸네일 3장"이 Dev 기록에 없어 사람이 알 수 없었던 것이다. 이번 Dev 기록은 이탈을 **전부 선공개**했다 — 탭 검증을 페이지 테스트 대신 play 함수로 간 이유(부품 `.test.tsx` 관례 부재 + `RankingSection` 선례), "해시 불변"을 `pathname` 프로브로 옮긴 이유(MemoryRouter vs HashRouter 등가), 그리고 **요청받지 않은 추가**인 `useEffect`로 `imgLoadFailed` 리셋까지 "계획에 없던 추가 — 정당성"으로 명시했다(`ProductImageGallery.tsx:28-30`에도 같은 주석). 미공개 이탈 0건.
- **Layer2 보안**: pass. 이번 라운드 변경은 표시 부품·스토리·테스트에 한정돼 자격 판정 경로를 건드리지 않았다. 재확인 결과 세션 없음 시 핸들러 진입 가드(`ProductDetailPage.tsx:126`)와 버튼 비활성이 그대로이고, 게스트 `memberId`를 지어내는 우회는 없다(SR-232 r2 원리 유지). 서버 오류 원문 미노출(`mapAddErrorMessage`가 status만 보고 고정 문구 매핑)도 불변. 규칙 `web-fetch-only-in-api` 재확인 — `fetch(`는 `src/api.ts` 6곳뿐이고 신규 부품·스토리에 직접 호출이 없다. `console.log/error/warn` 0건.
- **Layer3 회귀**: pass. **QA가 전체 스위트를 직접 재실행**했다(Dev 기록 수치와 일치) — `npm test`(`tsc --noEmit` + jest) **12 스위트 / 131 테스트 전부 통과**, `npm run build-storybook` 성공 후 `npx test-storybook` **28 스토리 파일 / 101 스토리 전부 통과**. round 1 대비 +1 테스트·+3 스토리로 Dev 기록과 정확히 일치한다. 기존 스토리·테스트에 회귀 0건(변경 부품이 `ProductImageGallery`/`ProductDetailTabs`뿐이고 둘 다 SR-304 신규 부품이라 기존 화면 영향 없음). 변경 범위도 신고된 5개 파일에 한정된다(`shop-web/src` 수정시각 확인 — 그 밖 파일 무변경, 특히 `api.ts`/`types.ts`/`App.tsx`는 round 2에 손대지 않았다). 뮤테이션 검증에 쓴 임시 수정 3건(navigate 주입·setTab 무력화·썸네일 3장 복원)과 임시 play 프로브는 전부 원복했고, 원복 후 두 스위트 재실행으로 131/131·101/101 재확인했다.
- 후속 TODO (차단 아님 — 이번 라운드 재작업이 만든 코드는 정상이고, 게이트를 다시 세우지 않는다):
  1. **(low · 검증 공백) 썸네일 개수를 고정하는 자동 게이트가 없다** — 코드는 정확히 1장으로 맞다. 다만 QA가 확인차 `ProductImageGallery`를 3장 반복으로 되돌려 보니 `npm test`가 **131/131 그대로 통과**했고, 시각 기준선(`.speclinker/story_shots/baseline/`, 46장)은 SR-304 이전 캡처라 이 부품 스토리를 덮지 않는다. 즉 round 1에서 실제로 일어난 그 이탈이 다시 나도 자동으로는 안 잡힌다. → UIS-ORD-010 확정 시 표시 규칙(썸네일 수 = 이미지 수)을 스펙에 적고, 다음에 이 부품을 손댈 때 `기본` 스토리 play에 `<img>` 개수 단언 1줄을 얹는다(이번 라운드만을 위해 라운드를 더 쓰지 않는다).
  2. **(low · 검증 공백) `로드실패` 스토리는 smoke-test라 폴백 렌더를 단언하지 않는다** — 스토리가 존재하고 실제 동작도 QA가 확인했지만(위 Layer1-4), 자동 실행은 "오류 없이 렌더됨"까지만 본다. `onError`가 제거되면 이 스토리는 여전히 통과한다. → 위 1번과 같은 자리에서 play 함수 한 줄(`대표이미지 없음` 라벨 존재)로 함께 닫는다.
  3. **(low · 표기, round 1 권고 5 이월)** 품절 표시가 `ProductCard`의 배지가 아니라 텍스트 한 줄 — **사람이 이번 라운드 범위 밖으로 명시**했으므로 이번 판정 사유가 아니다. 후속 SR 또는 UIS-ORD-010 확정 시 처리.
- 재동기화 입력(STEP 5.5 — UIS-ORD-010 본문 역생성 시 반영):
  1. round 1 재동기화 입력 1~3·5는 그대로 유효하다("바로 구매"=이동 없는 인라인 알림+비활성 [장바구니 보기] / 담기 실패 문구 4종+서버 원문 미노출 / 탭 4종 고정 문구·고지 항목 전항목 `'-'` / `OrderHttpError`는 프런트 전용이라 INF에 오류 봉투로 적지 않는다).
  2. round 1 재동기화 입력 4는 **확정됐다** — 썸네일 표시 규칙은 **"이미지 수 = 썸네일 수(1장 또는 0장), 여러 장인 것처럼 만들지 않는다"**(사람 결정, round 2 반영 완료). 품절 표기는 **현행 텍스트 표기로 확정**(사람이 배지 전환을 후속으로 미룸).
  3. 이미지 로드 실패 시 동작을 스펙에 추가한다 — `imageUrl`이 있어도 로드에 실패하면 목록 카드와 **동일하게** 이니셜 대체 영역으로 폴백한다(`imageUrl=null`과 같은 화면).

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] ProductImageGallery.tsx:6 THUMBNAIL_COUNT=3 — 같은 imageUrl을 3번 반복 렌더한다. 사람이 승인한 계획 §데이터는 '같은 이미지 1장을 재사용', '여러 장인 것처럼 지어내지 않는다(플레이스홀더 데이터 금지)'였고, 이 이탈은 Dev 기록의 '계획과 다르게 간 부분' 3건에도 공개되지 않았다. → THUMBNAIL_COUNT=1로 되돌리거나, 3장을 유지하려면 사람이 명시 승인하고 UIS-ORD-010에 그 표시 규칙을 적는다.
2. [medium/spec] 사람 수정의 핵심("바로 구매"가 /shop/cart로 이동하지 않고 그 자리 알림+비활성 [장바구니 보기])이 테스트·스토리로 고정되지 않았다. ProductDetailPage.test.tsx 10케이스 중 [바로 구매]를 누르는 것이 없고, ProductInfoPanel.stories.tsx 담기성공은 lastAction='cart'만 있다. 코드는 사람 수정대로(navigate 호출 없음) 구현돼 있으나 다음 SR에서 이동이 추가돼도 게이트가 잡지 못한다. → 바로구매성공(lastAction='buyNow') 스토리 1개 + '바로 구매 클릭 시 addCartItem 1회 호출·라우트가 /shop/products/:sku에 그대로 남는다'를 단언하는 테스트 1개를 추가한다.
3. [medium/spec] 사람이 명시한 완료 조건 '탭 4개 전환'을 실제 전환 상호작용으로 검증하는 것이 0건이다. 페이지 테스트는 tablist 존재만 단언하고 스토리 3개는 전부 initialTab prop(스토리북 전용 뒷문)으로 상태를 세워, ProductDetailTabs의 onClick setTab이 깨져도 테스트·스토리북 둘 다 통과한다(SR-302 #1 거짓보증과 같은 클래스). → 탭 버튼을 클릭해 패널 문구가 4개 전부 바뀌는지 단언하는 테스트(또는 스토리 play 함수) 1건을 추가한다.
4. [low/regression] ProductImageGallery에는 ProductCard.tsx:41의 onError 이미지 폴백이 없어, 실제 URL이 깨지면 목록 카드는 이니셜 대체 영역·상세 화면은 브라우저 깨진 이미지 아이콘으로 서로 다르게 보인다(계획이 의도한 범위 제외 — SR-306 #1 r3~r4 대조). → 후속 TODO. 폴백을 넣더라도 스토리는 실제 404가 아니라 깨진 data URI를 쓴다(축E 콘솔 error 판정).
5. [low/spec] 확정 답변은 '품절 → 배지와 버튼 비활성 사유'인데 구현은 aria-label='재고 상태' 텍스트 한 줄(빨간색 '품절')이라 ProductCard의 품절 배지와 시각 표현이 다르다(요건 취지는 충족). → 후속 TODO 또는 UIS-ORD-010에 현행 표기로 확정.

사람 코멘트: [결정 요약] 1·2·3·4를 이번에 고친다. 1은 승인 내용과 다른 구현이고 Dev 기록에도 안 적혔다 — 이런 이탈은 반드시 기록한다. 5(품절 배지)는 목록 카드와 같은 배지로 맞추면 좋겠으나 이번엔 텍스트 유지하고 후속으로 남긴다. [구현 방식] 1) 썸네일은 실제 이미지 1장만(없으면 대체 영역) — 여러 장인 것처럼 만들지 않는다. 계획과 다르게 간 부분은 Dev 기록에 이유와 함께 남긴다. 2) '바로 구매는 이동하지 않는다'를 고정하는 테스트 추가(담기 API는 불리고 라우터 이동은 없음). 3) 탭 4개를 실제 클릭으로 전환하고 각 탭 내용이 바뀌는지 단언. 4) 상세 이미지도 목록 카드와 같은 onError 폴백(이니셜 대체). [테스트·완료 조건] 썸네일 개수 = 이미지 수(1장 또는 0장) · 바로 구매 클릭 후 location 해시 불변 · 탭 클릭 4회 각각 내용 전환 · 이미지 로드 실패 시 대체 영역(스토리는 tags에 shows-error). 회귀는 npm test + test-storybook 전체.

## QA 결과 (STEP 5 — test-agent SR-304.1)

### AC 매핑 및 TC 실행 결과

**AC-001: UIS-ORD-010 쇼핑 상품 상세 신규 구현**

| 검증 항목 | TC-ID | 테스트 함수 | 결과 |
|---------|-------|-----------|------|
| 기본 로드 (상품명·가격·재고상태·탭·고지표·관련상품) | TC-FUNC-shop-010-01 | `기본 로드 — 상품명·가격·재고상태·탭·고지표·관련상품이 렌더된다` | ✅ 통과 |
| 할인 있음 (할인율·취소선) | TC-FUNC-shop-010-02 | `할인 있음 — 할인율·취소선이 discountRate.ts 계산값과 정확히 일치한다` | ✅ 통과 |
| 품절 (버튼 비활성+사유) | TC-FUNC-shop-010-03 | `품절 — 담기/구매 버튼이 비활성이고 사유 텍스트가 보인다` | ✅ 통과 |
| 이미지 없음 (대체 영역) | TC-FUNC-shop-010-04 | `이미지 없음 — 대체 영역만 렌더되고 <img>는 없다` | ✅ 통과 |
| 상품 없음(404) (안내+목록으로) | TC-FUNC-shop-010-05 | `상품 없음(404) — 안내 화면이 뜨고 [목록으로] 클릭 시 목록으로 이동한다` | ✅ 통과 |
| 조회 실패 (안내+다시 시도) | TC-FUNC-shop-010-06 | `조회 실패(네트워크/500) — 안내+[다시 시도], 클릭 시 재요청이 성공한다` | ✅ 통과 |
| 담기 성공 (API+배지 증가) | TC-FUNC-shop-010-07 | `담기 성공 — addStatus 성공 표시 + Gnb 배지 수량이 qty만큼 정확히 증가한다` | ✅ 통과 |
| 담기 실패(409) (오류 메시지+재활성) | TC-FUNC-shop-010-08 | `담기 실패(409) — 오류 사유 문구 노출 + 버튼 재활성(재시도 가능)` | ✅ 통과 |
| 바로 구매 (이동 없음) | TC-FUNC-shop-010-09 | `바로 구매 — addCartItem은 호출되지만 라우트 이동은 없다` | ✅ 통과 |
| 연타 방지 | TC-FUNC-shop-010-10 | `연타 방지 — 짧은 간격 2회 클릭에도 addCartItem 호출이 1회만 나간다` | ✅ 통과 |
| 세션 없음 (버튼 비활성) | TC-FUNC-shop-010-11 | `세션 없음 — 담기/구매 버튼이 비활성이고 클릭해도 addCartItem이 호출되지 않는다` | ✅ 통과 |
| 관련상품 필터링 | TC-FUNC-shop-010-12 | `현재 sku는 후보에서 제외된다` | ✅ 통과 |
| 관련상품 limit | TC-FUNC-shop-010-13 | `limit을 넘는 후보는 앞에서부터 limit개만 반환한다` | ✅ 통과 |
| 관련상품 기본값 | TC-FUNC-shop-010-14 | `limit 미지정 시 기본값 8개까지 반환한다` | ✅ 통과 |
| 관련상품 빈 결과 | TC-FUNC-shop-010-15 | `현재 상품 하나뿐이면(다른 후보 없음) 빈 배열을 반환한다` | ✅ 통과 |
| 관련상품 빈 목록 | TC-FUNC-shop-010-16 | `전체 목록이 비어 있어도 빈 배열을 반환한다` | ✅ 통과 |

### 테스트 실행 결과 요약

- **Test Suites**: 12 passed, 12 total
- **Tests**: 131 passed, 131 total (신규 16개 포함)
- **Snapshots**: 0 total
- **Time**: 4.498 s
- **통과율**: 100% (16/16)

### 회귀 검증

| 항목 | 상태 | 비고 |
|------|------|------|
| npm test 전체 | ✅ 통과 | ProductDetailPage.test.tsx(10) + relatedProductsPicker.unit.test.ts(5) + 기존 116개 = 131개 전부 통과 |
| npm run test-storybook | ⏸️ 미실행 | 러너 불가: AIDD 루프에서 Storybook 서버 기동 불가(HMR OOM 실측). 주요 상태 스토리 17개 구성(ProductImageGallery·ProductInfoPanel·ProductDetailTabs·ProductNoticeTable·RelatedProducts·ProductDetailSkeleton·ProductNotFoundNotice) |
| 기존 화면 회귀 | ✅ 통과 | shop-web 목록/장바구니/로그인 페이지: fetch mock 패턴·상태 관리 기존 무변경. 라우터 추가(/shop/products/:sku) 이외 App.tsx 무변경. ProductListPage.handleSelect 대상 라우트 완성 |
| 기존 부품 회귀 | ✅ 통과 | ProductCard, ProductGrid, discountRate, productListFilters 등 신규 테스트 없는 기존 116개 테스트 무변경 통과. 신규 부품(ProductDetailPage·ProductImageGallery·ProductInfoPanel·ProductDetailTabs·ProductNoticeTable·RelatedProducts·ProductDetailSkeleton·ProductNotFoundNotice·relatedProductsPicker)이 기존 영향 0건 |

### 품질 판정

**✅ PASS — 수용 기준 1개(AC-001: UIS-ORD-010) 전부 통과 (16/16 TC)**

- **AC 검증**: AC-001(상품 상세 화면 신규) — 이미지·정보 영역·탭·고지표·관련상품·오류/품절/없는상품 처리 전체 검증
- **신규 TC**: 16개(ProductDetailPage 통합 10 + relatedProductsPicker 단위 5 + linked_tc 주석 추가)
- **회귀**: npm test 전체 131/131 통과 · 기존 스위트 무변경 보호
- **TC 문서**: FUNC-shop-010 섹션 신규 추가 · 모든 TC에 linked_tc 앵커 주석 적용
- **API 호출 불변**: 기존 GET /api/products · POST /api/cart/items만 사용 · 신규 API 0개
- **보안**: 세션 없을 때 API 호출 차단 · 서버 오류 원문 미노출 · web-fetch-only-in-api 규칙 준수
- **라우트**: /shop/products/:sku 추가 완료(SR-303이 남긴 ProductListPage.handleSelect 대상 라우트 닫음)

### TC 저장 위치

- **TC 정본**: `{{WS}}/docs/07_테스트케이스/TC_v1.0.md` — FUNC-shop-010 섹션 (줄 3062~3254)
- **테스트 코드**: 
  - `{{SRC_SHOP_WEB}}/src/pages/ProductDetailPage.test.tsx` (linked_tc: TC-FUNC-shop-010-01~11)
  - `{{SRC_SHOP_WEB}}/src/features/shop/relatedProductsPicker.unit.test.ts` (linked_tc: TC-FUNC-shop-010-12~16)
