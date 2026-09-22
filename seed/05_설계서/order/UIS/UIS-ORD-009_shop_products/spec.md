---
uis-id: UIS-ORD-009
name: shop_products
domain: order
domain-code: ORD
layer: ui
route: /shop/products
screens_role: 주화면
api_hints:
  - "GET /api/products"
access_control: []
anchors:
  - "modules/shop-web/src/pages/ProductListPage.tsx"
  - "modules/shop-web/src/features/shop/productListFilters.ts"
  - "modules/shop-web/src/features/shop/ProductFilterBar.tsx"
  - "modules/shop-web/src/features/shop/ProductListGrid.tsx"
  - "modules/shop-web/src/features/shop/ProductPagination.tsx"
  - "modules/shop-web/src/App.tsx:97"
revision_history:
  - "2026-09-17 신규 생성(SR-303.1) — 예약 ID 그대로(번호 재계산 금지)"
---

# UIS-ORD-009: shop_products

> [반영: SR-303.1] — 상품 목록(검색·필터) 화면 신규. 홈(`UIS-ORD-008`)의 GNB 검색·카테고리 숏컷에서 진입.

## 1. 화면 개요

- 라우트: `/shop/products` (spa-route, `modules/shop-web/src/App.tsx:97`)
- 목적: 키워드 검색·재고 필터·정렬·페이지 이동으로 상품을 찾는 화면. 정렬·페이지네이션·가격대 필터는 전부
  **클라이언트 계산**(서버 API 계약 변경 없음, `GET /api/products`의 `keyword`·`inStock`만 사용).
- 로그인 없이 접근 가능(`GET /api/products`는 SR-307에서 공개 경로로 확정).

### 진입 경로 (round1~3 확정)

- 홈(`UIS-ORD-008`) GNB 검색 제출 → `?keyword={검색어}`를 실어 이 화면으로 이동, 초기 검색어로 반영·입력창에 표시.
- 홈 카테고리 숏컷 클릭 → **파라미터 없이** 이 화면으로 이동(전체 목록). 카테고리 데이터·필터 API가 없어
  라벨을 검색어로 대체하지 않는다(round2에서 "라벨→keyword"를 시도했다가 시드 상품명과 전혀 겹치지 않아
  6/6 빈 결과가 되는 결함으로 판명, round3에서 원복). 실제 카테고리 필터링은 서버에 카테고리 데이터가
  생긴 뒤 후속 SR에서 다룬다.
- 이 화면 안에서의 재검색·재필터는 URL을 갱신하지 않는다(`useSearchParams`를 초기값 읽기 전용으로만 사용) —
  `?keyword=A`로 들어와 화면 안에서 `B`로 재검색한 뒤 새로고침·북마크·공유하면 `A` 결과가 열린다(의도된 동작).

## 2. 화면 구성

위에서부터(부품 경로는 `modules/shop-web/src/features/shop/`):

| 영역 | 부품 | 내용 |
|---|---|---|
| 상단 GNB | `Gnb.tsx`(홈과 공유) | 로고·카테고리 메뉴·검색 입력(제출 시 이 화면으로 이동)·장바구니 아이콘·로그인 상태 표시 |
| 필터바 | `ProductFilterBar.tsx` | 재고 있는 상품만 체크박스, 가격대 min/max 입력, 정렬 select(추천순/낮은가격순/높은가격순), 결과 개수 텍스트. fetch 없음(전부 controlled) |
| 상품 그리드 | `ProductListGrid.tsx` | 로딩(스켈레톤)/결과없음(검색어·필터·일반)/조회실패/결과있음 렌더. 카드 자체는 홈과 같은 `ProductCard` 재사용 |
| 페이지네이션 | `ProductPagination.tsx` | 이전/다음 버튼 + "페이지 N / M". 첫 페이지 이전 비활성, 마지막 페이지 다음 비활성 |

컨테이너 `ProductListPage.tsx`(페이지, 스토리 대상 아님)가 검색/필터/정렬/페이지 상태를 총괄하고 위 4개 부품을 조립한다.

## 3. 입력·검증 규칙

- 서버 반영 상태(`appliedKeyword`, `inStockOnly`)가 바뀔 때만 `GET /api/products` 재호출. 나머지(가격대, 정렬,
  페이지)는 클라이언트 전용 — 재요청 없음.
- 파생 순서(고정): 서버 응답 → 가격대 필터(`filterByPriceRange`) → 정렬(`sortProducts`) → 페이지 slice.
  결과 개수 텍스트는 가격 필터까지 적용하고 정렬·페이지 적용 **전**의 배열 길이.
- `PAGE_SIZE = 12` — 확정 문답에 페이지당 개수 규정이 없어 계획 확인 게이트에서 사람이 정한 값(상수, `productListFilters.ts`).
- `appliedKeyword`/`inStockOnly`/`priceMin`/`priceMax`/`sortKey` 중 하나라도 바뀌면 `page`를 1로 리셋.
- 재시도(조회 실패 후 [다시 시도]) 연타는 같은 파라미터 요청을 1회로 억제(`inFlightKeyRef`). 서로 다른 요청이
  겹칠 때 응답이 역전되면 세대 카운터(`requestIdRef`)로 최신 요청의 응답만 반영.
- 같은 검색어를 다시 제출하면 React state가 바뀌지 않아 재조회가 일어나지 않는다(알려진 동작, 후속 개선 후보).

## 4. 호출 API

| 용도 | API | 비고 |
|---|---|---|
| 상품 목록 조회 | `GET /api/products` (`INF-ORD-008`) | `keyword`(검색어)·`inStock`(재고 필터)만 사용. 정렬·페이지·가격대는 서버에 보내지 않는다. 무인증 공개(SR-307) |

## 5. 표시 조건(상태)

| 상태 | 표시 |
|---|---|
| 로딩 | `ProductListGrid`가 스켈레톤(`PAGE_SIZE` 개수 파생) 표시 |
| 결과 있음 | 상품 카드 그리드(홈과 동일 `ProductCard`) |
| 결과 없음(검색어) | "검색 결과가 없습니다" + [검색어 지우기] |
| 결과 없음(필터) | "조건에 맞는 상품이 없습니다" + [필터 초기화](재고·가격대 리셋, 검색어는 유지) |
| 결과 없음(일반) | 안내 문구만(버튼 없음) |
| 조회 실패 | 사유 문구 + [다시 시도](연타해도 실제 요청은 1회) |
| 첫 페이지 | [이전] 비활성 |
| 마지막 페이지 | [다음] 비활성 |
| 정렬 변경 | 추천순(서버 응답 순서)·낮은가격순·높은가격순으로 카드 순서 재배열 |
| 재고 필터 켬 | `inStock=true`로 재조회, 품절 상품 제외 |

상태별 실물은 스토리북 스토리로 남아 있다(`ProductFilterBar.stories.tsx`·`ProductListGrid.stories.tsx`·
`ProductPagination.stories.tsx` — 부품 3개, 확정문답 8상태 전부 커버).

> 보강: `/sl-sync --apply --kind=uis` 또는 ddd-ui-agent

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-17 | SR-303 | #1 | 상품 목록(검색·필터) 화면 신규 — /shop/products, GET /api/products의 keyword·inStock 사용, 정렬·페이지·가격대는 클라이언트 계산 | shop-web@59f2008 |
