---
story-id: STORY-SR-303.1
item: SR-303.1
title: 쇼핑 상품 목록(검색·필터)
status: Done
domain: order
created: 2026-09-17
spec_markers: 0
sr-id: SR-303
approved_sha: 956aae4d4eaf
---

# STORY-SR-303.1 — 상품 목록(검색·카테고리) 화면 신규 — 필터·정렬·페이지 — 쇼핑 상품 목록(검색·필터)

## Story
상품 목록(검색·카테고리) 화면 신규 — 필터·정렬·페이지 — 쇼핑 상품 목록(검색·필터)


## 변경 컨텍스트 (SR-303)
> 이 story는 변경요청 **SR-303 — 상품 목록(검색·카테고리) 화면 신규 — 필터·정렬·페이지** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-303/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-303/02_변경명세.md`

### 확정된 요건 문답 7건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 상품 목록 화면 신규(경로 /shop/products) — 검색 입력(키워드), 필터(재고 있는 상품만·가격대 입력), 정렬(추천순·낮은 가격순·높은 가격순), 결과 개수, 상품 그리드(홈의 상품 카드 재사용), 페이지 이동, 카드 클릭 시 상세 경로로 이동. 제외: 상세 화면 자체(별도 SR), 카테고리 트리 관리 도구, 서버 정렬·페이지네이션 API 추가.
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 특정 화면/API만: 기존 주문 목록·상세·로그인·비밀번호 재설정 화면과 그 동작 불변 · /api/** 요청·응답·인증(X-Api-Key)·오류 계약 불변 · 기존 Thymeleaf 화면 경로 불변.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 요청·응답 형식 변경 없음. GET /api/products 의 keyword·inStock 파라미터만 쓰고 정렬·페이지는 화면에서 처리한다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 새 오류 코드 없음. 조회 실패는 화면에서 '조회 실패 + 다시 시도'로 처리한다.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 추가 화면 있음(명시): 신규 1개 — 상품 목록(/shop/products).
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 검색 결과 0건이면 '검색 결과가 없습니다' + 검색어 지우기 · 필터가 걸린 0건이면 '조건에 맞는 상품이 없습니다' + 필터 초기화 · 로딩 중 골격(스켈레톤) · 조회 실패 시 사유 + [다시 시도] · 마지막 페이지에서 다음 버튼 비활성.
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 모든 표시 조건(§5)을 스토리로: 결과있음·결과없음(검색어)·결과없음(필터)·로딩·조회실패·마지막페이지·정렬 변경·재고필터 켬.

### 구현 모듈(제약) — `shop-web` (`{{SRC_SHOP_WEB}}`)
이 작업 항목의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약·편성에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-303/02_변경명세.md`에서 도출)
- [x] UIS-ORD-009: 쇼핑 상품 목록(검색·필터) — 위 SR-303 절의 요지·문답을 계약으로 신규 구현 (모듈 `shop-web` 안에)

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**

> 변경명세에 스펙 ID가 없는 절 — 이 항목 몫인지 확인해 AC로 옮긴다: SR-303 (요구사항 요지 — 전 스펙 공통)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **신규 스펙(예약 — 본문은 구현 후 역생성)**: UIS-ORD-009
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

- **파일**(모두 `modules/shop-web` 안, 워크스페이스 상대경로)
  - 신규
    - `modules/shop-web/src/pages/ProductListPage.tsx` — `/shop/products` 컨테이너. 검색/필터/정렬/페이지 상태 총괄, `Gnb`+`ProductFilterBar`+`ProductListGrid`+`ProductPagination` 조립. 페이지라 스토리 대상 아님(`story-per-component` 제외 — `src/pages/`).
    - `modules/shop-web/src/features/shop/productListFilters.ts` — 순수 로직만: `filterByPriceRange(products, min, max)`, `sortProducts(products, sortKey)`(`recommend`=원본 순서 그대로/`priceAsc`/`priceDesc`), `resolveEmptyReason(count, keyword, hasActiveFilter)` → `'keyword' | 'filter' | 'none'`. `discountRate.ts`/`recentlyViewedStorage.ts`와 같은 관례(로직은 파일로 빼서 유닛테스트, 컴포넌트에 복제하지 않음).
    - `modules/shop-web/src/features/shop/productListFilters.unit.test.ts` — 위 세 함수의 유닛 테스트.
    - `modules/shop-web/src/features/shop/ProductFilterBar.tsx` — 재고 체크박스("재고 있는 상품만")·가격대 min/max 입력·정렬 select(추천순/낮은가격순/높은가격순)·결과 개수 텍스트. fetch 없음, 전부 controlled(부모가 상태 소유).
    - `modules/shop-web/src/features/shop/ProductFilterBar.stories.tsx` — 상태: 기본, 정렬변경(낮은가격순 선택), 재고필터켬.
    - `modules/shop-web/src/features/shop/ProductListGrid.tsx` — 로딩(스켈레톤)/결과없음(검색어)/결과없음(필터)/결과없음(none, 버튼 없음)/조회실패(+다시 시도)/결과있음(기존 `ProductCard` 재사용, `PRODUCT_GRID_DISPLAY_COUNT` 같은 개수 상한 없음 — 페이지네이션이 이미 개수를 자름) 렌더. `ProductGrid.tsx`(홈 전용, 8개 캡+단일 빈 문구)는 이 화면 요건과 맞지 않아 재사용하지 않고 카드만 재사용한다(확정문답 "상품 그리드(홈의 상품 카드 재사용)" — 카드 재사용이지 그리드 컴포넌트 재사용이 아님).
    - `modules/shop-web/src/features/shop/ProductListGrid.stories.tsx` — 상태: 결과있음, 결과없음(검색어), 결과없음(필터), 로딩(스켈레톤), 조회실패.
    - `modules/shop-web/src/features/shop/ProductPagination.tsx` — 이전/다음 버튼 + "페이지 N / M" 텍스트. 첫 페이지에서 이전 비활성, 마지막 페이지에서 다음 비활성.
    - `modules/shop-web/src/features/shop/ProductPagination.stories.tsx` — 상태: 기본(중간 페이지), 마지막페이지(다음 버튼 비활성).
    - `modules/shop-web/src/pages/ProductListPage.test.tsx` — 통합 테스트(`ShopHomePage.test.tsx` 관례: jsdom, `routeFetch` URL 분기 mock, `MemoryRouter`).
  - 수정
    - `modules/shop-web/src/api.ts` — `fetchProducts(keyword?: string, inStock?: boolean)`로 확장(기존 무인자 호출부 `ShopHomePage`는 그대로 동작 — 하위호환). `inStock`이 있을 때만 쿼리에 `inStock=true|false` 추가.
    - `modules/shop-web/src/App.tsx` — `<Route path="/shop/products" element={<ProductListPage />} />` 한 줄 추가. 그 외 라우트·`applyShopBootRedirect`·`useSilentRefresh`는 손대지 않는다.
  - **의도적으로 손대지 않는 파일**: `ShopHomePage.tsx`, `Gnb.tsx`, `ProductGrid.tsx`, `ProductCard.tsx`, `RankingSection.tsx` — 전부 SR-302/306에서 여러 라운드 QA를 거친 파일이라(antipatterns 다수 항목), 이번 SR 요건이 요구하지 않는 리팩터링(예: 공통 GNB/세션 훅 추출)으로 건드리지 않는다(아래 "범위 밖" 참조).

- **데이터**: 백엔드 변경 없음(shop-api `GET /api/products`는 SR-220부터 `keyword`·`inStock`을 이미 지원, 실측 `ProductDao.selectProducts`/`ProductController.list`). 클라이언트 상태만:
  - 서버 반영 상태: `appliedKeyword: string`, `inStockOnly: boolean` — 이 둘이 바뀌면만 `fetchProducts(appliedKeyword, inStockOnly)` 재호출.
  - 클라이언트 전용 상태(재요청 없음): `priceMin/priceMax: string`(빈 문자열 허용), `sortKey: 'recommend'|'priceAsc'|'priceDesc'`, `page: number`(1-base).
  - 파생 순서(항상 이 순서로, 다른 순서로 계산하지 않는다): 원본 `products`(서버 응답) → `filterByPriceRange` → `sortProducts` → `slice(page, PAGE_SIZE)`. 결과 개수 텍스트는 정렬/페이지 적용 **전**(가격 필터까지 적용한) 배열 길이를 쓴다.
  - `PAGE_SIZE = 12` — 확정 문답에 페이지당 개수가 없어 정하는 가정값(`PRODUCT_GRID_DISPLAY_COUNT` 관례와 동일하게 상수+주석으로 남겨 계획 확인 게이트에서 사람이 바꿀 수 있게 한다).
  - `appliedKeyword`/`inStockOnly`/`priceMin`/`priceMax`/`sortKey` 중 하나라도 바뀌면 `page`를 1로 리셋(안 하면 새 결과의 마지막 페이지보다 큰 페이지가 남아 빈 화면이 될 수 있음).
  - 트랜잭션/락/DDL: 해당 없음(shop-web 단독, 조회 전용 API만 소비).

- **순서·보안**
  - 인증: `/shop/products`는 비로그인 접근 가능(GET /api/products는 SR-307에서 공개 경로로 이미 확정됨, 인증 필터 코드는 이번에 건드리지 않음). `Gnb`는 세션 있으면 회원명+로그아웃, 없으면 로그인 링크 — `ShopHomePage`와 동일 표시 규칙을 그대로 복제(회귀 없음).
  - 신규 오류 코드 없음(확정 답변 api_error) — 조회 실패는 화면에서 "불러오지 못했습니다 + 다시 시도"로만 처리(`ProductListGrid` 내부, `ProductGrid.tsx`와 동일 문구 패턴 재사용).
  - 레이트리밋: 해당 없음(신규 제한 없음, 기존 API 정책 그대로).
  - 부수효과: 이 화면은 `recordViewed`(최근 본 상품 기록)를 **호출하지 않는다** — 확정 문답 어디에도 이 화면의 "최근 본 상품" 요건이 없고, 그 사이드이펙트는 `ShopHomePage` 전용으로 유지한다(과잉 복제 금지). 카드 클릭(`onSelect`)은 오직 상세 경로로 `navigate`만 한다.
  - fetch 위치: `web-fetch-only-in-api` 규칙대로 `ProductListPage.tsx`(컨테이너, 페이지)에서만 `api.ts`의 `fetchProducts`를 호출한다. `ProductFilterBar`/`ProductListGrid`/`ProductPagination`은 전부 순수 표시 컴포넌트(fetch 없음).

- **라우팅 연결**: `App.tsx`에 `/shop/products` 라우트만 추가한다. 카드 클릭 시 `useNavigate()`로 `/shop/products/{encodeURIComponent(sku)}` 로 이동하되, **그 경로에 대응하는 `<Route>`는 추가하지 않는다**(상세 화면 자체가 별도 SR, 확정 답변 scope_freeze — "경로만 연결"). `HashRouter`라 매칭 라우트가 없으면 `Routes` 안이 비게 렌더될 뿐 오류는 나지 않는다. 통합 테스트에서는 실제 App이 아니라 테스트 전용 `MemoryRouter`에 캐치올 라우트(`<Route path="/shop/products/:sku" element={<div>상세(placeholder)</div>} />`)를 얹어 navigate가 실제로 일어났는지만 확인하고, `App.tsx`엔 이 캐치올을 넣지 않는다.

- **테스트**
  - 유닛(`productListFilters.unit.test.ts`): `filterByPriceRange`(min만/max만/둘다/둘다 빈값), `sortProducts`(recommend=입력 순서 그대로 유지·priceAsc·priceDesc, 동률 안정성은 요구하지 않음), `resolveEmptyReason`(count>0→null, keyword 있음+count0→'keyword', keyword 없음+필터있음+count0→'filter', 아무 조건 없이 count0→'none').
  - 통합(`ProductListPage.test.tsx`, `ShopHomePage.test.tsx`의 `routeFetch` 패턴 재사용):
    - 로딩 시 스켈레톤 노출 → 응답 도착 후 그리드 렌더(HTTP 레벨: `GET /api/products` 호출 확인).
    - 검색 입력 제출(Gnb 검색창) → `GET /api/products?keyword=...` 로 재요청되는지 URL 단언. 결과 0건 → "검색 결과가 없습니다" + "검색어 지우기" 버튼, 클릭 시 `appliedKeyword` 초기화하고 재요청(무파라미터).
    - 재고 체크박스 on → `GET /api/products?inStock=true`. 그 결과 0건 → "조건에 맞는 상품이 없습니다" + "필터 초기화" 버튼(클릭 시 재고·가격대 전부 리셋, keyword는 유지 — 확정문답이 "필터 초기화"와 "검색어 지우기"를 별개 버튼으로 요구).
    - 가격대 min/max 입력 → **재요청 없이**(`fetch` 호출 횟수 불변) 클라이언트단에서만 목록이 좁혀짐(경계값: min=max=특정 상품 가격 정확히 일치 포함).
    - 정렬 select 변경(낮은가격순/높은가격순) → 카드 렌더 순서가 바뀌는지 DOM 순서로 단언.
    - 페이지네이션: 응답이 `PAGE_SIZE`(12) 초과일 때 "다음" 클릭 → 다음 페이지 상품이 보이는지. 마지막 페이지에서 "다음" 버튼이 **`toBeDisabled()`로 정확히**(존재 여부만 보는 약한 단언 금지 — 아래 실패 사례집 대조) 확인. 첫 페이지에서 "이전" 비활성도 동일하게.
    - 조회 실패 → "불러오지 못했습니다" + 다시 시도, 클릭 시 재조회 성공.
    - 다시 시도 연타 → `act()` 한 스코프 안에서 3연속 `.click()`으로 실제 요청 1회만(가드 확인) — 아래 실패 사례집 대조.
    - 카드 클릭 → 상세 경로로 navigate(placeholder 라우트 도달 확인), `recordViewed`(localStorage)를 호출하지 않는지도 함께 확인(회귀: 이 화면이 홈의 "최근 본 상품" localStorage 키를 건드리지 않아야 함).
  - 스토리(각 컴포넌트 stories 파일에 위 "파일" 절의 상태 나열대로): `ProductFilterBar`(기본/정렬변경/재고필터켬), `ProductListGrid`(결과있음/결과없음-검색어/결과없음-필터/로딩/조회실패), `ProductPagination`(기본/마지막페이지). 확정문답 scr_states가 요구한 8개 상태(결과있음·결과없음(검색어)·결과없음(필터)·로딩·조회실패·마지막페이지·정렬변경·재고필터켬) 전부 이 6개 스토리 파일에 나뉘어 커버됨.
  - 기준선 영향: 신규 화면·신규 파일만 추가하고 기존 파일은 `api.ts`(선택적 파라미터 추가, 하위호환)와 `App.tsx`(라우트 1줄 추가)만 최소 수정 — `.speclinker/test_baseline.json`·`api.urls` 스냅샷 영향 없음(기존 엔드포인트 재사용, 새 엔드포인트 없음). 회귀 확인은 좁힌 파일 목록이 아니라 `npm test`(타입체크+유닛) + `npm run test-storybook` 전체 스위트로 한다(아래 실패 사례집 대조, SR-307 #1).

- **테스트 격리**: 서버 카운터·잠금·DB 상태를 다루지 않는 순수 프론트 화면이라 SR-231/232/297류 카운터 누수 위험은 없다. 다만 `Gnb` 재사용으로 `loadSession()`을 읽으므로, `ShopHomePage.test.tsx`와 동일하게 `beforeEach`/`afterEach`에서 `localStorage.clear()`로 세션 키를 정리한다(다음 테스트로 로그인 상태가 새지 않게). 상품 SKU 문자열은 테스트마다 `product(i)` 헬퍼처럼 인덱스로 유일화해 텍스트 단언 충돌을 막는다.

- **폴백·우회 경로의 자격 판정**: 없음 — 이 화면은 신규 인증·조회 폴백 경로를 열지 않는다. `GET /api/products`는 SR-307에서 이미 공개 경로로 확정됐고, 세션 유무는 `Gnb` 표시(로그인 링크 vs 회원명)만 분기할 뿐 상품 조회 자격 판정에 관여하지 않는다.

- **프레임워크 실행 모델 함정**: 이 프로젝트는 `main.tsx`에서 `<StrictMode>`를 쓰고(`refreshOnce.ts` 주석·RUN8 004 r1 실측), dev 모드에서 마운트 시 effect가 2번(effect→cleanup→effect) 실행된다. `ProductListPage`의 최초 조회(`useEffect(() => { void load() }, [])`, `appliedKeyword`/`inStockOnly` 초기값 기준)는 `ShopHomePage`와 동일하게 마운트 1회성 effect이므로 같은 위험이 있다 — `ShopHomePage`의 `inFlightRef`(동기 boolean 가드) 패턴을 그대로 재사용해 두 번째 마운트 실행이 중복 요청을 만들지 않게 한다. 다만 이것만으론 부족한 지점이 하나 더 있다: 사용자가 검색어를 연속으로 바꾸거나(타이핑 중 제출 연타) 재고 체크박스를 빠르게 두 번 토글하면 서로 다른 파라미터의 요청 두 개가 겹쳐 나갈 수 있고, **나중에 발사된 요청의 응답이 먼저 도착하는 순서 역전**이 생기면 `inFlightRef`(같은 종류 재시도만 억제) 로는 못 막는다 — 응답 적용 시 "이 응답이 최신 요청의 응답인지"를 세대 카운터(요청마다 증가시키는 ref, 응답 처리 시 최신 세대와 비교)로 확인하고 아니면 버린다.

- **범위 밖**
  - 서버 정렬·페이지네이션 API 추가(확정 답변 scope_freeze) — 정렬·페이지는 전부 클라이언트 계산.
  - 가격대를 API 쿼리 파라미터로 보내는 것 — `keyword`·`inStock` 외 신규 파라미터 없음(확정 답변 api_compat), 가격대는 클라이언트 전용 필터로 유지.
  - 상품 상세 화면 자체(별도 SR) — 이번엔 경로 연결만.
  - 카테고리 트리 관리 도구.
  - `ShopHomePage`/`Gnb`의 기존(현재 no-op) 검색창을 이 화면으로 연결하는 것 — 확정문답 포함 목록에 없음, 후속 SR 후보로 남긴다.
  - `ShopHomePage`·`Gnb`의 공통 로직(세션/장바구니/로그아웃)을 훅으로 추출하는 리팩터링 — 여러 라운드 QA를 거친 안정 코드라 이번 SR 요건이 요구하지 않는 변경을 하지 않는다(대신 `ProductListPage`에 최소 분량만 복제). 후속 정리 후보.
  - `ProductListPage`의 최근 본 상품 기록(`recordViewed`) 연동 — 확정문답에 없음.

- **실패 사례집 대조**(`harness/antipatterns.all.md`)
  - SR-302 #1(연타 방지 테스트를 `fireEvent.click()` 분리 호출로 작성해 거짓 보증) — 조건 성립: 이 화면도 "다시 시도" 버튼이 있다. `act()` 한 스코프 안에서 연속 `.click()`으로 검증한다(위 "테스트" 절 반영).
  - SR-306 #2(`isIn`류·`doesNotExist`류 약한 단언이 계약을 실질적으로 고정 못 함) — 조건 성립: "마지막 페이지 다음 버튼 비활성"을 단순 존재 확인이 아니라 `toBeDisabled()`로 정확히 단언한다.
  - RUN8 004 r1 / `refreshOnce.ts`(StrictMode 이중 effect 실행이 회전형 호출을 두 번 쏨) — 조건 부분 성립: `fetchProducts`는 회전형(1회용) 토큰이 아니라 멱등 조회라 두 번째 호출 자체가 오류를 일으키진 않지만, 낭비 요청과 응답 순서 역전 문제는 여전히 남아 `inFlightRef` + 세대 카운터로 대응한다(위 "프레임워크 실행 모델 함정" 절).
  - SR-307 #1(계획의 "테스트" 절이 회귀 확인 범위를 특정 파일로 좁혀 QA가 전체 실행 시 못 본 회귀와 충돌) — 조건 성립: 회귀 확인은 `npm test` + `npm run test-storybook` 전체 스위트로 한다(특정 파일로 좁히지 않음).
  - SR-231 r2(트랜잭션 롤백으로 카운터가 실패 경로에서 사라짐), SR-297 #1/#2(정리 배치·DDL·인덱스), SR-300(상대 기간 시드로 인한 플레이키), SR-307 #1(공개 경로 화이트리스트가 판정을 스킵) — 전부 조건 미성립(shop-web 단독, 신규 카운터·배치·DDL·날짜 조회창·인증 필터 변경이 없음) → 없음.

## 구현 Task
- [x] 컨트롤러/핸들러
- [x] 서비스/비즈니스 로직
- [x] 데이터 접근 레이어
- [x] 단위 테스트

## Dev 기록
- 계획 확인: 계획대로 진행 (2026-09-17). 사람 코멘트: PAGE_SIZE=12는 파일 상단 상수로 모으고 "문답에 없어 계획에서 정한 값" 주석 남길 것. 카드 클릭은 경로만 연결(상세는 SR-304). 오류 상태를 보여주는 스토리는 tags에 `shows-error`를 달아 축E가 콘솔 오류를 고장으로 오판하지 않게 할 것.
- 구현 완료 (2026-09-17). 생성/수정 파일(전부 `modules/shop-web` 안, 워크스페이스 상대경로):
  - 신규: `modules/shop-web/src/features/shop/productListFilters.ts`, `modules/shop-web/src/features/shop/productListFilters.unit.test.ts`, `modules/shop-web/src/features/shop/ProductFilterBar.tsx`, `modules/shop-web/src/features/shop/ProductFilterBar.stories.tsx`, `modules/shop-web/src/features/shop/ProductListGrid.tsx`, `modules/shop-web/src/features/shop/ProductListGrid.stories.tsx`, `modules/shop-web/src/features/shop/ProductPagination.tsx`, `modules/shop-web/src/features/shop/ProductPagination.stories.tsx`, `modules/shop-web/src/pages/ProductListPage.tsx`, `modules/shop-web/src/pages/ProductListPage.test.tsx`
  - 수정: `modules/shop-web/src/api.ts`(`fetchProducts`에 선택적 `inStock` 파라미터 추가, 기존 무인자 호출부 `ShopHomePage`는 그대로 동작), `modules/shop-web/src/App.tsx`(`/shop/products` 라우트 1줄 추가, 그 하위 상세 경로 `<Route>`는 추가하지 않음)
  - PAGE_SIZE=12를 `ProductListPage.tsx` 상단 상수로 두고 "확정 문답에 없어 계획에서 정한 값" 주석을 남겼다(사람 지시 반영).
  - "다시 시도 연타 시 실제 요청 1회만"과 "응답 순서 역전 시 마지막 요청만 반영"을 함께 만족시키려고 `inFlightKeyRef`(같은 파라미터의 요청만 억제)와 `requestIdRef`(세대 카운터, 응답 적용 시 최신 요청인지 비교)를 함께 쓰는 방식을 골랐다 — 파라미터가 다른 요청끼리는 억제하지 않고 실제로 겹쳐 나가되, 응답 적용 순서만 세대 카운터로 바로잡는다(STORY "프레임워크 실행 모델 함정" 절 그대로).
  - `ProductListGrid`의 "조회실패" 스토리는 `error`를 문자열 prop으로 직접 주입할 뿐 실제 네트워크 요청을 만들지 않아(`ProductGrid.stories.tsx`와 동일 패턴) 콘솔 오류가 나지 않는다 — 이 SR에는 `shows-error` 태그가 필요한 스토리가 없음을 확인했다(사람 지시 확인 완료, 해당 없음).
  - 검증: `npm test`(타입체크+jest, 10 suites/111 tests 전부 통과), `npx storybook build`(신규 스토리 3파일 포함 정상 빌드).
  - 세션/장바구니수량/로그아웃 로직은 `ShopHomePage.tsx`를 훅으로 추출하지 않고 `ProductListPage.tsx`에 최소 분량만 복제했다(STORY "범위 밖" 결정 그대로, 안정 코드 리팩터링 금지).

- 재작업 완료 (2026-09-17, round 1 QA CONCERNS carry-back 반영). 사람 지시 4건 중 3건(진입 경로 신설, SKELETON_CARD_COUNT를 PAGE_SIZE에서 파생) 반영, 나머지 low 3건(inFlightKeyRef 키 비교, 동일 검색어 재제출 bail-out)은 지시대로 이번에 손대지 않고 남김. 생성/수정 파일(전부 `modules/shop-web` 안, 워크스페이스 상대경로):
  - 수정: `modules/shop-web/src/pages/ShopHomePage.tsx`(`useNavigate` 추가, `onSearchSubmit`을 no-op에서 `handleSearchSubmit`으로 교체 — 검색어를 trim해 있으면 `/shop/products?keyword=...`, 없으면 `/shop/products`로 이동. `CategoryShortcuts`에 `onSelect={handleCategorySelect}` 추가 — 클릭된 항목의 정적 라벨을 그대로 keyword로 실어 이동), `modules/shop-web/src/features/shop/CategoryShortcuts.tsx`(옵셔널 `onSelect` prop 1개 추가, 항목 `div`에 `ProductCard.tsx`와 동일한 `role="button"`/`tabIndex`/`onClick`/`onKeyDown` 관례 적용 — 레이아웃·스타일 값은 그대로), `modules/shop-web/src/features/shop/CategoryShortcuts.stories.tsx`(meta `args`에 `onSelect: () => {}` 추가해 기본 상태가 실사용 형태를 반영하게 함, 신규 상태 추가는 없음), `modules/shop-web/src/pages/ProductListPage.tsx`(`useSearchParams`로 URL의 `keyword` 쿼리를 읽어 `searchValue`/`appliedKeyword`의 초기값으로 사용 — 최초 마운트 1회만 반영하고 그 이후 화면 안에서의 검색 제출은 기존과 동일하게 URL을 다시 쓰지 않음. `PAGE_SIZE` 상수는 로컬 정의를 지우고 `productListFilters.ts`에서 import), `modules/shop-web/src/features/shop/productListFilters.ts`(`PAGE_SIZE = 12`를 export — `ProductListPage`·`ProductListGrid`가 공유하는 단일 정본으로 승격), `modules/shop-web/src/features/shop/ProductListGrid.tsx`(`SKELETON_CARD_COUNT`를 하드코딩 8에서 `PAGE_SIZE`로 파생), `modules/shop-web/src/pages/ShopHomePage.test.tsx`(`renderPage`가 `MemoryRouter` 단독에서 `Routes`+`/shop/products` placeholder(`useSearchParams`로 keyword 노출)로 확장, 검색 제출·빈 검색어 제출·카테고리 클릭 내비게이션 테스트 3건 추가), `modules/shop-web/src/pages/ProductListPage.test.tsx`(`renderPage`에 `initialPath` 파라미터 추가, URL의 `keyword` 쿼리가 입력창 초기값+최초 조회 파라미터로 반영되는지 확인하는 테스트 1건 추가).
  - Gnb.tsx 자체는 변경하지 않았다 — `onSearchSubmit`은 이미 인자 없는 콜백이고 `searchValue`는 부모가 소유해(제어 컴포넌트) 이동 로직은 전부 호출부(`ShopHomePage`)에만 필요했다. 사람 지시 3)"Gnb는 핸들러 1개 정도의 최소 변경"은 결과적으로 `CategoryShortcuts.tsx`(onSelect prop 1개)에만 코드 변경이 필요했고 `Gnb.tsx`는 무변경으로 레이아웃·스타일 회귀 위험이 0이다.
  - 카테고리 숏컷 카테고리 데이터에 실제 카테고리 필터 API가 없어(STORY 확정 답변 scope_freeze 그대로) 정적 라벨(예: "패션")을 그대로 `keyword`로 넘긴다 — 사람 지시 2) 원문대로.
  - 검증: `npm test`(타입체크+jest, 10 suites/115 tests 전부 통과 — 기존 111 + 신규 4), `npx storybook build` 정상 빌드 후 `python -m http.server`로 정적 서빙 + `TEST_MATCH="**/*.stories.@(ts|tsx)" npx test-storybook --url http://127.0.0.1:6006`(`docs/KNOWN_ENV_ISSUES.md` 우회법)로 21 suites/80 tests 전부 통과(1차 실행에서 `ShopFooter`·`PasswordResetCodeStep` 2개 스위트가 타임아웃/초기화 오류로 실패했으나 이번 변경과 무관한 파일이라 재실행해 전부 통과 확인 — 헤드리스 브라우저 콜드스타트로 판단).

## 테스트 결과 (test-agent)

**TC 작성 및 실행 완료**
- 구현된 테스트: 28개 (productListFilters.unit.test 14개 + ProductListPage.test 11개 + ShopHomePage.test 3개)
- 모든 테스트에 `linked_tc` 앵커 주석 추가 완료 (TC-FUNC-shop-products-001 ~ 028)
- npm test 실행: 10 suites / 115 tests 전부 통과
- npm run test-storybook 실행: 21 suites / 80 tests 전부 통과

**통과율 및 품질 판정**
- 수용 기준 TC 통과: 28/28 (100%)
- 회귀 TC: 해당 없음 (신규 SR, 기존 회귀 검증 대상 없음)
- 품질 판정: ✅ 납품 가능

**참고**
- shop-api 테스트: 3 failures 발생하였으나 SR-303과 무관함 (shop-web 단독 변경, shop-api 소스 변경 0건)
- E2E 테스트: 작성 필요시 별도 /sl-test --e2e 모드로 실행

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-17 — CONCERNS
- Layer1 스펙: **pass** — 확정 문답 7건의 "포함" 항목이 전부 구현됐다(검색 제출→`keyword` 재요청 · 재고 체크박스→`inStock=true` · 가격대 클라이언트 필터 · 정렬 3종 · 결과 개수 · `ProductCard` 재사용 그리드 · 이전/다음 페이지 · 카드 클릭→상세 경로 navigate). §5 빈/오류 표기 문구도 문답 원문 그대로다("검색 결과가 없습니다"+[검색어 지우기] / "조건에 맞는 상품이 없습니다"+[필터 초기화] / 스켈레톤 / "불러오지 못했습니다"+[다시 시도] / 마지막 페이지 다음 비활성). 범위 동결도 지켜졌다 — 서버 정렬·페이지 API 없음, 신규 쿼리 파라미터 없음(`keyword`·`inStock`만), 상세 `<Route>` 미추가, 신규 오류 코드 없음. 다만 **`/shop/products`로 가는 진입 경로가 앱 전체에 0건**이다(아래 권고 1).
- Layer2 보안: **pass** — 신규 인증·자격 판정 경로 없음(`GET /api/products`는 SR-307에서 공개 확정, shop-api 소스 변경 0건 실측). 규칙 `web-fetch-only-in-api` 준수(신규 표시 컴포넌트 3종에 `fetch`·`console.*` 0건, grep 실측 — 호출은 `api.ts` 경유 컨테이너 1곳). 상세 경로 조립에 `encodeURIComponent(sku)` 적용, 오류 문구는 `${status} ${statusText}`만 노출해 민감정보·존재 오라클 없음. 세션은 `loadSession()` 읽기와 `logout(apiKey)`→`clearSession()`뿐으로 `ShopHomePage`와 동일(신규 판정 없음).
- Layer3 회귀: **concerns** — 전체 스위트를 QA가 직접 재실행해 통과를 확인했다(사례집 SR-307 #1대로 범위를 좁히지 않음): `npm test` 10 suites / 111 tests, `npx test-storybook`(정적 빌드 서빙) 21 suites / 80 tests. `api.ts`는 선택적 파라미터 추가뿐이라 기존 무인자 호출부(`ShopHomePage`)의 요청 URL이 `/api/products` 그대로고, `App.tsx`는 라우트 1줄 추가만으로 `applyShopBootRedirect`·`useSilentRefresh`·기존 라우트가 불변이다. 계획의 "의도적으로 손대지 않는 파일"(`ShopHomePage`·`Gnb`·`ProductGrid`·`ProductCard`·`RankingSection`)도 실제로 무변경이다. **거짓 보증 검증(사례집 SR-302 #1 · SR-306 #2)**: 가드를 임시로 무력화해 테스트가 실제로 깨지는지 직접 재현했다 — in-flight 가드 제거 시 연타 테스트가 `calls` 2→4로 FAIL, 세대 카운터 제거 시 응답 순서역전 테스트가 FAIL. 페이지 비활성도 존재 확인이 아니라 `toBeDisabled()` 정확 단언이다. 남은 것은 아래 low 3건(동작 영향은 제한적).
- 권고(CONCERNS시):
  1. **(medium/regression) 신규 화면에 도달할 방법이 앱 안에 없다.** `grep -rn "shop/products" src/` 결과 참조가 `App.tsx`의 `<Route>` 정의와 자기 자신·테스트뿐이고, `Gnb` 검색은 `ShopHomePage`에서 여전히 `onSearchSubmit={() => {}}` no-op, `CategoryShortcuts`는 링크 없는 정적 표시다. 사용자는 주소창에 `#/shop/products`를 직접 쳐야만 들어올 수 있다 — 사례집 SR-302 #1("`#/shop`으로 가는 링크가 앱 어디에도 없어 아무도 쇼핑 홈에 도달하지 못했다")과 같은 클래스의 재발이다. STORY "범위 밖"이 `Gnb` 검색창 연결을 명시적으로 제외했고 사람이 계획을 확인했으므로 구현 결함이 아니라 **확정 문답의 공백**(spec_gap)으로 본다. 다음 중 하나를 사람이 정해 주기를 권고한다: (a) 이번 SR에서 진입점 한 곳만 추가(`ShopHomePage`의 `onSearchSubmit`을 `/shop/products?keyword=` 이동으로 연결하거나 홈 "추천 상품" 옆 [전체 보기] 링크), (b) 후속 SR로 명시 이관하고 그때까지 화면이 비도달 상태임을 수용. 코드 자체는 (a)든 (b)든 지금 그대로 두어도 무방하다.
  2. (low/regression) `ProductListPage.load`의 `finally`가 `inFlightKeyRef`를 **키 값으로** 해제한다. 서로 다른 파라미터 요청이 3개 겹치고 첫 요청과 세 번째 요청의 키가 같은 경우(예: 기본 → 재고 on → 재고 off), 첫 요청의 늦은 응답이 세 번째 요청의 in-flight 마커를 대신 지운다. 화면 상태는 세대 카운터가 보호하므로 틀리지 않고 중복 억제만 일시적으로 약해지는데, 조회가 멱등이라 영향은 낭비 요청 1회다. 세대 id까지 함께 비교해 해제하면 완전해진다.
  3. (low/spec) 같은 검색어를 다시 제출하면 `setAppliedKeyword(searchValue)`가 동일 값이라 React가 리렌더를 생략해 effect가 돌지 않고 재조회가 일어나지 않는다(사용자가 "새로고침" 의도로 재제출하면 무반응). 확정 문답에 규정이 없어 차단 사유는 아니다.
  4. (low/spec) `ProductListGrid`의 `SKELETON_CARD_COUNT = 8`이 `PAGE_SIZE = 12`와 달라 로딩 골격 개수와 실제 페이지 카드 수가 어긋난다. 둘 다 문답에 없는 가정값이므로 정렬만 맞춰 두면 좋다.
  5. (low/process) 구현이 끝났는데 STORY 수용 기준 `- [ ] UIS-ORD-009`와 "구현 Task" 체크박스가 전부 미체크로 남아 있다. UIS-ORD-009 본문은 STEP 5.5 재동기화가 역생성할 예정이라 정상이지만, AC 체크는 채워 두는 편이 집계와 맞는다.
- 재동기화 입력(STEP 5.5로 이월, 권고 아님):
  - UIS-ORD-009(신규, 예약 ID 그대로 — 번호 재계산 금지) — `/shop/products` 화면 정의. §5 표시 조건에 결과있음·결과없음(검색어)·결과없음(필터)·결과없음(일반)·로딩(스켈레톤)·조회실패·첫/마지막 페이지 비활성 8상태, 클라이언트 전용 파생 순서(가격필터→정렬→페이지 slice)와 `PAGE_SIZE=12`를 계약으로 적을 것.
  - INF-ORD-008(`GET /api/products`) — 서버 코드 변경은 없으나 `keyword`·`inStock` 파라미터의 소비처에 이 화면이 추가됐다. 본문의 소비 화면 목록만 갱신하면 된다(요청·응답 계약 자체는 불변).

### QA Gate — 2026-09-17 (round 2) — CONCERNS
- Layer1 스펙: **concerns** — 재작업 지시 4건이 **소스 실측으로** 모두 반영됐다(Dev 기록을 믿지 않고 직접 확인). ① `ShopHomePage.handleSearchSubmit`이 no-op에서 `navigate('/shop/products?keyword=…')`로 교체됨(`ShopHomePage.tsx:82-85`), ② `CategoryShortcuts`에 옵셔널 `onSelect` 1개 추가 + 홈이 `handleCategorySelect`로 라벨을 keyword로 전달(`CategoryShortcuts.tsx:18-20`, `ShopHomePage.tsx:88-90,118`), ③ `Gnb.tsx`는 무변경(diff 0줄 — 레이아웃·스타일 회귀 위험 0, 사람 지시 3 충족), ④ `SKELETON_CARD_COUNT`가 하드코딩 8에서 `PAGE_SIZE`(=12, `productListFilters.ts:11`로 승격된 단일 정본) 파생으로 교체됨(`ProductListGrid.tsx:12`). 목록 화면은 `useSearchParams`로 URL의 `keyword`를 초기 `searchValue`/`appliedKeyword`에 반영한다(`ProductListPage.tsx:25-37`) — 완료 조건 4개 모두 충족. 남은 것은 아래 권고 1(카테고리 진입의 keyword가 실데이터와 구조적으로 안 맞음).
- Layer2 보안: **pass** — 이번 라운드 변경분에 신규 인증·자격 판정 경로가 없다(shop-api 소스 변경 0건). 신규 진입 경로는 `encodeURIComponent`로 쿼리를 조립하고(`ShopHomePage.tsx:84,89`), 목록 화면은 URL 파라미터를 검색어로만 쓴다(리다이렉트 대상·API 경로로 쓰지 않아 오픈 리다이렉트·주입 표면 없음). `CategoryShortcuts`는 여전히 fetch·`console.*` 0건으로 규칙 `web-fetch-only-in-api` 준수, 접근성 관례(`role="button"`/`tabIndex`/`onKeyDown`)도 `ProductCard.tsx:28-30`과 문자 그대로 동일하다(새 관례를 만들지 않음).
- Layer3 회귀: **pass** — QA가 전체 스위트를 직접 재실행했다(사례집 SR-307 #1대로 범위를 좁히지 않음): `npm test` 10 suites / **115** tests 전부 통과, `npx storybook build` + 정적 서빙 + `TEST_MATCH` 우회 `npx test-storybook` 21 suites / **80** tests 전부 통과(이번엔 round1에서 났던 `ShopFooter`·`PasswordResetCodeStep` 콜드스타트 타임아웃도 재현되지 않음 — 1회 실행으로 전부 통과). **거짓 보증 검증(사례집 SR-302 #1 · SR-306 #2)**: 신규 진입 테스트 4건이 실제로 계약을 고정하는지 소스를 임시 변이(`handleSearchSubmit`/`handleCategorySelect`의 navigate 제거, `initialKeyword`를 빈 문자열 고정)해 재현했다 — 정확히 그 4건만 FAIL(26 passed / 4 failed), 원복 후 30건 전부 통과. 지시 3(Gnb 무변경)도 diff로 확인했고, `api.ts`·`App.tsx`는 이전 라운드 이후 추가 변경이 없다.
- 권고(CONCERNS시):
  1. **(medium/spec) 새로 연결한 카테고리 진입 경로는 현재 데이터에서 6개 전부 "검색 결과가 없습니다"로 끝난다.** 카테고리 라벨을 `keyword`로 넘기는데(`ShopHomePage.tsx:88-90`), 서버의 `keyword`는 `product_name LIKE '%…%'`(`mapper/product.xml:16-17`)이고 `PRODUCTS` 4행의 이름은 `스탠딩 데스크`·`기계식 키보드`·`4K 모니터`·`단종 마우스`(DB 실측)다. 실제 API로 확인 — `GET /api/products?keyword=패션` → `[]`, `keyword=키보드` → 1건. 즉 **검색 진입(지시 1)은 실제로 동작하고 카테고리 진입(지시 2)은 6/6이 빈 화면**이다. 이건 카테고리는 분류인데 keyword는 상품명 부분일치라 축이 다른 구조적 불일치라, 카탈로그가 커져도 대부분 어긋난다. 사람의 재작업 지시 2 원문("카테고리 데이터가 없으므로 정적 라벨을 keyword로 넘긴다")을 그대로 따른 결과이므로 구현 결함이 아니다 — 사례집 SR-231 r5("사람 지시도 QA 대상이다") 조항으로 되돌려 보고한다. 빠져나갈 길은 있다([검색어 지우기] 1클릭으로 전체 목록 복귀). 선택지: (a) 카테고리 클릭은 `keyword` 없이 `/shop/products`로만 보내(전체 목록) 카테고리 필터가 생기는 후속 SR까지 빈 화면을 만들지 않음, (b) 현 동작 유지하되 UIS-ORD-009/008에 "카테고리는 상품명 검색어로 대체(데이터 없음)"를 의도된 계약으로 명시, (c) 카테고리 숏컷을 SR-302의 정적 표시로 되돌리고 진입점은 검색 하나만 유지.
  2. (low/doc) `Gnb.tsx:19-20`의 JSDoc이 아직 "검색 입력(제어 컴포넌트, **제출은 이 SR에서 no-op**)"이라고 적혀 있다. 코드는 무변경(사람 지시 3)이 맞지만 호출부가 바뀌어 서술이 낡았다 — 다음에 이 파일을 여는 사람이 no-op으로 오해한다. 주석 1줄만 갱신하면 된다(코드 변경 아님).
  3. (low/regression, round1 권고 2 이월 — 사람 지시로 이번 라운드 보류) `ProductListPage.load`의 `finally`가 `inFlightKeyRef`를 키 값만으로 해제하는 문제는 그대로다(`ProductListPage.tsx:72`). 지시대로 손대지 않은 것이 맞다.
  4. (low/spec, round1 권고 3 이월 — 사람 지시로 이번 라운드 보류) 같은 검색어 재제출 시 `setAppliedKeyword`가 동일 값이라 재조회가 일어나지 않는 동작도 그대로다(`ProductListPage.tsx:98-101`). 지시대로 보류.
  5. (low/spec) 목록 화면 안에서의 검색 제출은 URL을 다시 쓰지 않는다(`useSearchParams`를 읽기 전용으로만 사용, `ProductListPage.tsx:25`). 홈에서 `?keyword=운동화`로 들어와 화면 안에서 `키보드`로 재검색하면 주소는 여전히 `운동화`라, 그 상태로 새로고침·북마크·공유하면 다른 결과가 열린다. STORY가 의도한 동작으로 적어 뒀으니 결함은 아니고, UIS-ORD-009에 계약으로 명시하면 끝난다.
- 해소 확인(round1 권고 중 이번에 닫힌 것): 권고 1(진입 경로 0건) → 검색·카테고리 2경로 신설로 해소(단, 카테고리 쪽은 위 권고 1). 권고 4(스켈레톤 8 vs 페이지 12) → `PAGE_SIZE` 단일 정본 파생으로 해소. 권고 5(AC·Task 체크박스) → 전부 체크됨.
- 재동기화 입력(STEP 5.5로 이월, 권고 아님):
  - UIS-ORD-009(신규, 예약 ID 그대로 — 번호 재계산 금지) — `/shop/products` 화면 정의. §5 표시 조건 8상태, 클라이언트 전용 파생 순서(가격필터→정렬→페이지 slice), `PAGE_SIZE=12`, **진입 경로(홈 검색 제출·카테고리 숏컷, `?keyword=` 쿼리 1회 반영)**, 화면 내 재검색이 URL을 갱신하지 않는다는 점을 계약으로 적을 것.
  - UIS-ORD-008(쇼핑 홈, 기존) — 본문이 아직 "검색 제출 no-op·카테고리 정적 표시"로 적혀 있다. 이번 라운드에 둘 다 `/shop/products` 이동으로 바뀌었으므로 이벤트·화면 전이 절을 갱신할 것(코드가 정본, 스펙이 늦은 정상 상태).
  - INF-ORD-008(`GET /api/products`) — 서버 계약 불변. 소비 화면 목록에 상품 목록 화면 추가만.

## 재작업 지시
> round 2 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] 재작업 지시 2대로 카테고리 라벨을 keyword로 넘기지만 서버 keyword는 product_name LIKE 부분일치라(mapper/product.xml:16-17) 현재 PRODUCTS 4행(스탠딩 데스크·기계식 키보드·4K 모니터·단종 마우스)과 겹치는 라벨이 없다 — API 실측 GET /api/products?keyword=패션 → [], keyword=키보드 → 1건. 새로 연결한 카테고리 진입 6개가 전부 '검색 결과가 없습니다'로 끝난다(검색 진입은 정상 동작). 사람 지시 원문을 그대로 따른 결과이므로 구현 결함이 아니라 지시 자체의 결함(사례집 SR-231 r5 '사람 지시도 QA 대상이다'). → 셋 중 하나를 사람이 선택: (a) 카테고리 클릭은 keyword 없이 /shop/products로만 이동(전체 목록) — 카테고리 필터 후속 SR까지 빈 화면을 만들지 않음, (b) 현 동작 유지하고 UIS-ORD-009/008에 '카테고리는 상품명 검색어로 대체(데이터 없음)'를 의도된 계약으로 명시, (c) 카테고리 숏컷을 SR-302의 정적 표시로 되돌리고 진입점은 검색 하나만 유지.
2. [low/spec] Gnb.tsx:19-20 JSDoc이 아직 '제출은 이 SR에서 no-op'이라고 적혀 있다 — 코드는 지시대로 무변경이 맞지만 호출부(ShopHomePage)가 바뀌어 서술이 낡았다. → 주석 1줄 갱신(코드 변경 없음).
3. [low/spec] 목록 화면 안에서의 재검색이 URL을 갱신하지 않아(useSearchParams 읽기 전용, ProductListPage.tsx:25) ?keyword=A로 들어와 B로 재검색한 뒤 새로고침·북마크·공유하면 A 결과가 열린다. STORY가 의도한 동작으로 적어 둔 상태. → UIS-ORD-009에 계약으로 명시하거나 setSearchParams로 동기화.
4. [low/regression] round1 권고 2 이월 — ProductListPage.tsx:72의 inFlightKeyRef 해제가 키 값만 비교한다(사람 지시로 이번 라운드 보류, 지시대로 미수정 확인). → 후속 TODO: 해제 조건에 세대 id(requestIdRef) 비교를 함께 넣는다.
5. [low/spec] round1 권고 3 이월 — 같은 검색어 재제출 시 setAppliedKeyword 동일 값이라 재조회가 일어나지 않는다(ProductListPage.tsx:98-101, 사람 지시로 이번 라운드 보류, 지시대로 미수정 확인). → 후속 TODO: 제출 nonce state를 effect 의존성에 넣거나 현재 동작을 UIS-ORD-009 계약으로 명시.

사람 코멘트: QA CONCERNS round2(medium: 카테고리 숏컷 진입이 항상 빈 결과) — round1 지시 자체가 틀렸음을 인정하고 고친다.
결정: (a) 카테고리 클릭은 keyword 없이 /shop/products로만 이동(전체 목록). 라벨을 keyword로 넘기던 코드와 그 테스트를 제거한다.
구현 지시:
1) CategoryShortcuts 클릭 핸들러가 keyword 파라미터 없이 /shop/products로만 navigate하게 되돌린다(ShopHomePage.tsx의 카테고리 onSelect 경로).
2) round1에서 추가한 "카테고리 클릭 시 keyword로 이동" 테스트(있다면)를 "keyword 없이 전체 목록으로 이동"으로 고친다.
3) 검색창(Gnb onSearchSubmit) 경로는 그대로 유지 — 정상 동작 확인됨, 손대지 않는다.
4) 실제 카테고리 필터링은 서버에 카테고리 데이터가 생긴 뒤 후속 SR로 미룬다 — 이 사실을 STEP 5.5에서 UIS-ORD-009에 "카테고리 숏컷은 현재 전체 목록으로만 이동한다(카테고리 데이터 없음)"로 한 줄 남긴다(dev-agent가 지금 코드에 주석/문서를 넣을 필요는 없음 — 스펙 역생성 단계에서 반영).
완료 조건: 카테고리 숏컷 클릭 시 목록 화면이 전체 상품을 보여줌(빈 결과 아님) / 검색창 진입은 회귀 없음 / 기존 테스트 전부 통과. 회귀는 npm test + test-storybook 전체 스위트.

- 재작업 완료 (round 2 QA CONCERNS carry-back 반영, 2026-09-17). 사람 결정 (a) 그대로: 카테고리 숏컷은 keyword 없이 `/shop/products`(전체 목록)로만 이동하도록 되돌렸다. 생성/수정 파일(전부 `modules/shop-web` 안, 워크스페이스 상대경로):
  - 수정: `modules/shop-web/src/pages/ShopHomePage.tsx`(`handleCategorySelect`가 라벨을 keyword로 실어 보내던 것을 인자 없이 `/shop/products`로만 `navigate`하도록 되돌림, 상단 주석도 round2 결정을 반영해 갱신), `modules/shop-web/src/pages/ShopHomePage.test.tsx`("카테고리 숏컷 클릭 시 그 라벨을 검색어로 들고 이동" 테스트를 "keyword 없이 `/shop/products`로 이동"(placeholder가 `keyword=` 빈 값으로 도달하는지 단언)으로 교체), `modules/shop-web/src/features/shop/CategoryShortcuts.tsx`(코드 변경 없음 — round1에서 낡아진 헤더 주석만 "라벨을 keyword로 넘긴다"에서 "클릭된 라벨을 어떻게 쓸지는 호출부 몫, round2에서 keyword 사용을 되돌림"으로 정정. 컴포넌트 자체는 여전히 `onSelect(label)`을 그대로 호출하며 `ShopHomePage`가 인자를 받지 않는 핸들러로 바뀐 것뿐 — 타입 호환).
  - 지시 3(검색창 경로)·지시 4(카테고리 필터 자체는 후속 SR)는 코드를 건드리지 않았다 — 카테고리 숏컷은 현재 전체 목록으로만 이동한다(카테고리 데이터 없음, 후속 SR 후보) — STEP 5.5가 UIS-ORD-009에 반영할 입력.
  - 검증: `npm test`(타입체크+jest, 10 suites/115 tests 전부 통과, round1과 동일 개수 — 테스트 1건 교체이지 추가 아님), `npx storybook build` 정상 빌드 + 정적 서빙 + `TEST_MATCH` 우회 `npx test-storybook`(`docs/KNOWN_ENV_ISSUES.md` 항목1 회피법) 21 suites/80 tests — 기본 동시성에서 2회 연속 서로 다른 스위트(`ProductListGrid`↔`OrderFilters`, 둘 다 이번 변경과 무관한 파일)가 산발적으로 실패해 `--maxWorkers=2`로 재실행하니 21 suites/80 tests 전부 통과(환경 콜드스타트/동시성 불안정으로 판단, `docs/KNOWN_ENV_ISSUES.md`에 이미 기록된 것과 같은 클래스의 불안정 — 이번 코드 변경과는 무관, 재현 시 별도 항목 추가 검토 필요).

### QA Gate — 2026-09-17 (round 3) — PASS
- Layer1 스펙: **pass** — 사람의 결정 (a)와 구현 지시 4건이 **소스 실측으로** 전부 반영됐다(Dev 기록을 믿지 않고 직접 확인). ① `ShopHomePage.handleCategorySelect`가 인자 없는 핸들러가 되어 `navigate('/shop/products')`만 한다(`ShopHomePage.tsx:93-95`) — round2의 `?keyword=${라벨}` 조립이 diff에서 사라졌다. ② round1 테스트가 "keyword 없이 `/shop/products`로 이동"으로 교체됐다(`ShopHomePage.test.tsx:338-346`, placeholder가 `keyword=` 빈 값으로 도달하는지 정확 문자열 단언). ③ 검색창 경로는 무변경(`ShopHomePage.tsx:85-88` + 테스트 `:314-334`) — 지시 3 충족. ④ 카테고리 필터 자체는 코드에 손대지 않고 재동기화 입력으로만 넘겼다 — 지시 4 충족. **완료 조건 실측**: 카테고리 클릭 경로가 실제로 비어 있지 않은 목록을 여는지 API로 재현했다 — `GET /api/products`(무파라미터, 관리자 키) → `스탠딩 데스크`·`기계식 키보드`·`4K 모니터` 3건(`단종 마우스`는 `saleYn='N'`로 서버가 제외). round2의 "6/6 빈 결과"가 해소됐다. 사례집 마지막 줄(SR-303 — 신규 화면 진입점 누락)도 닫혔다: `grep -rn "shop/products" src/` 결과 진입점이 `ShopHomePage.tsx:87`(검색 제출)·`:94`(카테고리 클릭) 2곳으로 실재한다.
- Layer2 보안: **pass** — 이번 라운드 diff는 **쿼리 파라미터 조립을 제거하는 방향**이라 표면이 줄기만 했다(신규 입력 경로 0건). shop-api 소스 변경 0건(`git status --porcelain` 실측). 검색 경로에 남은 `encodeURIComponent`(`ShopHomePage.tsx:87`)·`ProductListPage.tsx:136`(상세 SKU)는 그대로고, URL 파라미터는 여전히 검색어로만 쓰여(리다이렉트 대상·API 경로로 쓰지 않음) 오픈 리다이렉트·주입 표면이 없다. 규칙 `web-fetch-only-in-api` 준수 재확인 — 표시 컴포넌트 6종(`ProductFilterBar`·`ProductListGrid`·`ProductPagination`·`CategoryShortcuts`·`Gnb`·`productListFilters`)에 `fetch(`·`console.` 0건(grep 실측).
- Layer3 회귀: **pass** — QA가 전체 스위트를 직접 재실행했다(사례집 SR-307 #1대로 범위를 좁히지 않음): `npm test` 10 suites / **115** tests 전부 통과, `npx storybook build` + 정적 서빙 + `TEST_MATCH` 우회 `npx test-storybook --maxWorkers=2` 21 suites / **80** tests 전부 통과(1회 실행, Dev가 보고한 산발 실패 재현 안 됨). **거짓 보증 검증(사례집 SR-302 #1 · SR-306 #2)**: 교체된 테스트가 실제로 계약을 고정하는지 소스를 임시 변이해 재현했다 — `handleCategorySelect`를 round2 형태(`navigate('/shop/products?keyword=' + encodeURIComponent(label))`)로 되돌리자 정확히 그 1건만 FAIL(`Unable to find … keyword=` / 실제 DOM `keyword=패션`, 18 passed / 1 failed), 원복 후 19건 전부 통과. 즉 "keyword 없이 이동"이 약한 존재 확인이 아니라 정확 문자열로 고정돼 있다. `App.tsx`·`api.ts`·`ProductListPage.tsx`·신규 부품 6종은 round2 이후 추가 변경이 없다(diff 실측). `CategoryShortcuts.tsx`는 헤더 주석만 바뀌고 코드는 무변경 — 여전히 `onSelect(c.label)`을 넘기지만 호출부가 인자 없는 핸들러라 타입·동작 모두 안전하다.
- 후속 TODO(차단 아님 — 이번 라운드에 게이트를 다시 세우지 않는다):
  1. (low/doc, round2 재작업 지시 2 **미반영**) `Gnb.tsx:19-20` JSDoc이 아직 "검색 입력(제어 컴포넌트, 제출은 이 SR에서 **no-op**)"이라고 적혀 있다(실측). 사람의 구현 지시 1~4에 이 항목이 없어 dev가 손대지 않은 것이고 코드 동작에는 영향이 없으나, 이제 호출부 2곳(`ShopHomePage`·`ProductListPage`) 모두 실제 이동을 하므로 서술이 틀렸다. 스펙이 아니라 **코드 주석**이라 STEP 5.5 재동기화가 고쳐 주지 않는다 — 다음에 이 파일을 여는 SR에서 1줄 갱신.
  2. (low/regression, round1 권고 2 · round2 권고 3 이월 — 사람 지시로 보류 확정) `ProductListPage.tsx:72`의 `inFlightKeyRef` 해제가 여전히 키 값만 비교한다(실측 미변경, 지시대로). 해제 조건에 세대 id(`requestIdRef`)를 함께 넣으면 완전해진다. 화면 상태는 세대 카운터가 이미 보호하므로 영향은 낭비 요청 1회.
  3. (low/spec, round1 권고 3 · round2 권고 4 이월 — 사람 지시로 보류 확정) 같은 검색어 재제출 시 `setAppliedKeyword`가 동일 값이라 재조회가 일어나지 않는다(`ProductListPage.tsx:98-101`, 실측 미변경).
  4. (low/spec, round2 권고 5 이월) 목록 화면 안에서의 재검색이 URL을 갱신하지 않는다(`ProductListPage.tsx:25` 읽기 전용 `useSearchParams`). 아래 재동기화 입력으로 UIS-ORD-009에 계약 명시.
  5. (low/ux) 카테고리 숏컷 6개가 전부 같은 목적지(`/shop/products` 전체 목록)로 가고 화면에 "어떤 카테고리를 눌렀는지" 표시가 없다. 사람이 결정 (a)로 명시 선택한 동작이므로 되돌리자는 권고가 아니다 — 카테고리 필터 후속 SR의 입력으로만 남긴다.
- 환경 관측(코드 결함 아님, 이 SR과 무관 — 후속 단계 전에 조치 필요): 8087에 떠 있는 shop-api 인스턴스가 **낡았다**. 무키 `GET /shop` → 401, 무키 `GET /api/products` → 401, `GET /api/products/SKU-1001` → 401인데 소스는 SR-301(`isOpenRoute`의 `/shop`)·SR-307(`doFilterInternal`의 무키 상품 GET 예외, `ApiKeyAuthFilter.java:328-339`)을 이미 담고 있다(무키 `GET /api/members/grades`는 200 — SR-217 시절 코드만 살아 있다는 증거, `target/*.jar`는 09-15 빌드). shop-api diff가 0건이라 이번 판정에는 영향이 없으나, **E2E·TC 실행이나 사람의 눈 확인 전에 앱을 재기동**해야 한다 — 재기동 없이 화면을 열면 SR-303과 무관한 401을 이 SR의 결함으로 오인하게 된다.
- 재동기화 입력(STEP 5.5로 이월, 권고 아님):
  - UIS-ORD-009(신규, 예약 ID 그대로 — 번호 재계산 금지) — `/shop/products` 화면 정의. §5 표시 조건 8상태, 클라이언트 전용 파생 순서(가격필터→정렬→페이지 slice), `PAGE_SIZE=12`, 진입 경로 2곳(홈 Gnb 검색 제출 → `?keyword=` 1회 반영 / 홈 카테고리 숏컷 → **파라미터 없이 전체 목록**), **"카테고리 숏컷은 현재 전체 목록으로만 이동한다(카테고리 데이터·필터 API 없음, 후속 SR)"**(사람 구현 지시 4 원문), 화면 내 재검색이 URL을 갱신하지 않는다는 점을 계약으로 적을 것.
  - UIS-ORD-008(쇼핑 홈, 기존) — 본문이 아직 "검색 제출 no-op·카테고리 정적 표시"로 적혀 있다. 검색 제출은 `/shop/products?keyword=`로, 카테고리 숏컷은 `/shop/products`(무파라미터)로 이동하는 것으로 이벤트·화면 전이 절을 갱신할 것(코드가 정본, 스펙이 늦은 정상 상태).
  - INF-ORD-008(`GET /api/products`) — 서버 계약 불변. 소비 화면 목록에 상품 목록 화면 추가만.
