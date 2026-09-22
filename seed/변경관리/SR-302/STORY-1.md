---
story-id: STORY-SR-302.1
item: SR-302.1
title: 쇼핑 홈(메인)
status: Done
domain: order
created: 2026-09-17
spec_markers: 0
sr-id: SR-302
approved_sha: 2c4a5a659b1f
---

# STORY-SR-302.1 — 커머스 홈(메인) 화면 신규 — GNB·기획 배너·카테고리·추천 상품·랭킹 — 쇼핑 홈(메인)

## Story
커머스 홈(메인) 화면 신규 — GNB·기획 배너·카테고리·추천 상품·랭킹 — 쇼핑 홈(메인)


## 변경 컨텍스트 (SR-302)
> 이 story는 변경요청 **SR-302 — 커머스 홈(메인) 화면 신규 — GNB·기획 배너·카테고리·추천 상품·랭킹** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-302/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-302/02_변경명세.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 쇼핑 홈 화면 신규(경로 /shop) — ① 상단 GNB(로고·카테고리 메뉴·검색 입력·장바구니 아이콘과 담긴 수량·로그인/마이) ② 히어로 배너 캐러셀(자동 넘김·좌우 이동·현재 위치 표시) ③ 카테고리 숏컷 ④ 추천 상품 그리드(상품 카드: 썸네일·상품명·판매가·정가·할인율·품절 배지) ⑤ 랭킹 섹션(탭 전환·순위 숫자) ⑥ 최근 본 상품(브라우저 저장) ⑦ 푸터. 제외: 상품 목록·상세·장바구니 화면(각각 별도 SR), 실제 배너 운영 도구, 추천·랭킹 알고리즘(정렬 기준은 응답 순서와 가격으로 단순 계산).
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 특정 화면/API만: 기존 주문 목록·상세·로그인·비밀번호 재설정 화면과 그 동작 불변 · /api/** 요청·응답·인증(X-Api-Key)·오류 계약 불변 · 기존 Thymeleaf 화면 경로 불변.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필요 — 요청·응답 형식 변경 없음. 화면이 GET /api/products 응답을 그대로 소비한다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 새 오류 코드 없음. 상품 조회 실패(네트워크·5xx)는 화면에서 '불러오지 못했습니다 + 다시 시도'로 처리하고 서버 계약은 그대로.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음 — 이 SR은 읽기 전용 화면이라 테이블·집계 변경이 없다. '최근 본 상품'은 DB가 아니라 브라우저 저장(localStorage)에 둔다.
- **기존 데이터 이관·백필이 필요한가?** — 불필요 — 스키마 변경·데이터 이관 없음.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 추가 화면 있음(명시): 신규 1개 — 쇼핑 홈(/shop). 기존 화면은 건드리지 않는다.
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 상품 0건이면 '표시할 상품이 없습니다' + 카테고리로 이동 안내 · 조회 실패면 사유 문구 + [다시 시도] · 최근 본 상품 없으면 '최근 본 상품이 없습니다' · 이미지 없는 상품은 상품명 이니셜 대체 영역 · 품절 상품은 카드에 품절 배지와 흐림 처리.
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 모든 표시 조건(§5)을 스토리로: 상품 카드(기본·할인·품절·이미지없음), 그리드(목록있음·빈목록·로딩·조회실패), 배너(1장·여러장), 랭킹(탭 전환), 최근 본 상품(있음·없음), GNB(비로그인·로그인·장바구니 수량 0/N).

### 구현 모듈(제약) — `shop-web` (`{{SRC_SHOP_WEB}}`)
이 작업 항목의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약·편성에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
**변경(TO-BE) — 이 SR이 바꾸는 것** (`docs/변경관리/SR-302/02_변경명세.md`에서 도출)
- [x] UIS-ORD-008: 쇼핑 홈(메인) — 위 SR-302 절의 요지·문답을 계약으로 신규 구현 (모듈 `shop-web` 안에) — 예약 ID는 UIS-ORD-008이었으나 STEP 5.5 재동기화(spec_resync_check, zero-LLM 순번 배정)에서 실제로는 UIS-ORD-008로 생성됨(008~011은 SR-303·304·305가 예약 중 — 충돌 방지)

**회귀(AS-IS 유지) — 바뀌면 안 되는 것**

> 변경명세에 스펙 ID가 없는 절 — 이 항목 몫인지 확인해 AC로 옮긴다: SR-302 (요구사항 요지 — 전 스펙 공통)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **신규 스펙(역생성 완료)**: UIS-ORD-008(예약 ID UIS-ORD-008에서 실제 배정 변경 — 위 AC 각주 참고)
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

> ⚠ **데이터 갭(사람 확인 필요)** — 백엔드 `Product`(`modules/shop-api/.../domain/Product.java`) 실측 결과
> 필드는 `sku·productName·price·stockQty·saleYn` 5개뿐이다. **정가(listPrice)·할인율·이미지 URL 컬럼이
> DB(`PRODUCTS`)·응답 어디에도 없다**(`product.xml` 3개 SELECT 전부 동일 컬럼 5개만 조회 — 실측). 이 SR은
> "요청·응답 형식 변경 없음"(확정문답 api_compat)이 하드 제약이라 백엔드에 필드를 추가하지 않는다.
> 아래 계획은 이 갭을 다음과 같이 처리한다 — **재해석이 아니라 데이터 없음을 있는 그대로 반영**한 것이며,
> 동의 못 하면 `### 사람 수정`으로 뒤집는다:
> - `ProductCard`는 `listPrice?`(정가)·`discountRate?`(할인율)·`imageUrl?` prop을 **컴포넌트 타입에는 갖되**,
>   페이지(`ShopHomePage`)는 실제 `Product`(위 5필드)만 넘기므로 **운영 화면에서는 이 셋이 항상 비어 있다** →
>   정가 취소선·할인 배지·실제 이미지는 실제 데이터로는 절대 렌더되지 않는다(조건부 렌더, 그린 적 없는 값을
>   지어내 채우지 않는다). 스토리(`할인` 상태)에서만 목업 args로 그 모양을 보여준다 — Storybook 표준 관례이지
>   운영 데이터 위장이 아니다.
> - 이미지는 위 이유로 **항상** 상품명 이니셜 대체 영역이 그려진다(조건부 아님 — 상시 사실).
> - `정가·할인율` 실데이터가 필요하면 `PRODUCTS`에 컬럼 추가 + `GET /api/products` 응답 확장이 선행돼야
>   하고, 그건 이 SR의 "하위호환 유지·응답 형식 변경 없음" 확정 답변과 정면 충돌하므로 **범위 밖**(후속 SR 후보).

- **파일**: 모두 `modules/shop-web` 안(구현 모듈 제약). 새 Java/서버 변경 없음.
  - 수정
    - `src/types.ts` — `Product`(sku·productName·price·stockQty·saleYn, `GET /api/products` 응답 그대로) 타입 추가.
    - `src/api.ts` — `fetchProducts(keyword?)`(`GET /api/products`, `inStock` 파라미터는 **넘기지 않는다** —
      품절 상품도 그리드에 나와야 품절 배지·흐림 처리 상태를 보여줄 수 있다) · `fetchCartItemCount(memberId)`
      (기존 `GET /api/cart`를 재사용해 `items[].qty` 합산 — 신규 API 아님, 이미 있는 계약 재사용) 추가.
      두 함수 다 응답이 raw 배열인지 `{items}` 봉투인지 방어적으로 처리(`fetchOrders`의 기존 관례를 그대로
      따름 — `ProductController.list`는 `List<Product>`를 직접 반환해 실측상 raw 배열이지만, CLAUDE.md의
      일반 서술("목록은 `{items}`")과 실제 컨트롤러 코드가 다르므로 방어적으로 양쪽을 다 받는다).
    - `src/App.tsx` — `<Route path="/shop" element={<ShopHomePage />} />` 추가만 한다. 기존 라우트
      (`/`·`/orders/:orderNo`·`/login`·`/password-reset`)는 그대로 둔다(아래 "순서·보안" 결정 참조).
  - 신규
    - `src/pages/ShopHomePage.tsx` — 컨테이너(스토리 대상 아님, 규칙 `story-per-component` 제외 대상).
      상품 1회 조회 + 세션 읽기 + 장바구니 수량 조회 + 최근 본 상품 로컬 상태를 조립해 아래 부품에 props로 내려준다.
    - `src/pages/ShopHomePage.test.tsx` — 통합 테스트(jest+jsdom, `PasswordResetPage.test.tsx`와 동일 관례).
    - `src/features/shop/shopStatic.ts` — 배너 슬라이드·카테고리 숏컷 **정적 목업 데이터**(백엔드에 카테고리·
      배너 테이블/API가 없음 — `PRODUCTS`뿐, "실제 배너 운영 도구 제외"·"디자인 자산 자체 제작" 확정 답변과
      일치). 상용 사이트 로고·문구 그대로 쓰지 않는다(요구사항 제약).
    - `src/features/shop/recentlyViewed.ts` — localStorage 유틸(`session.ts`와 동일 패턴: 단일 키
      `sl.shop.recentlyViewed`, JSON 배열, `recordViewed(sku)`(최근순 dedupe, 최대 8개 cap)·
      `loadRecentlyViewedSkus()`).
    - `src/features/shop/recentlyViewed.unit.test.ts` — 순수 함수 단위 테스트.
    - `src/features/shop/Gnb.tsx` + `.stories.tsx` — 로고·카테고리 메뉴 트리거·검색 입력(제어 컴포넌트,
      제출은 이 SR에서 no-op — 아래 "범위 밖")·장바구니 아이콘+수량 배지·로그인/마이 영역.
      props: `session: SessionResult | null`, `cartItemCount: number`, `searchValue`, `onSearchChange`,
      `onSearchSubmit`, `onLogout`. fetch 없음(규칙 `web-fetch-only-in-api`).
    - `src/features/shop/HeroBannerCarousel.tsx` + `.stories.tsx` — 배너 캐러셀(자동 넘김·좌우 이동·
      현재 위치 인디케이터). props: `slides: BannerSlide[]`.
    - `src/features/shop/CategoryShortcuts.tsx` + `.stories.tsx` — 카테고리 숏컷(아이콘+이름 가로 배열,
      비기능/정적 — 아래 "범위 밖"). props: `categories: CategoryShortcut[]`.
    - `src/features/shop/ProductCard.tsx` + `.stories.tsx` — 상품 카드(썸네일 대체영역·상품명·판매가·
      선택적 정가/할인율(위 데이터 갭 참조)·품절 배지+흐림). props: `product: Product`, 선택적
      `listPrice?/discountRate?/imageUrl?`, `onSelect?: (sku: string) => void`.
    - `src/features/shop/ProductGrid.tsx` + `.stories.tsx` — 추천 상품 그리드(로딩/빈목록/조회실패+다시시도/
      목록있음). props: `rows: Product[]`, `loading`, `error`, `onRetry`, `onSelect`. 표시 개수는 상위 8개로
      제한(정렬은 응답 순서 그대로 — "추천·랭킹 알고리즘 없음, 응답 순서" 확정 답변. **개수 8은 이 SR 문답에
      숫자로 확정되지 않은 가정값** — 사람이 바꿀 수 있다).
    - `src/features/shop/RankingSection.tsx` + `.stories.tsx` — 랭킹(탭 2개: `인기`=응답 순서 그대로 상위 5·
      `가격`=가격 오름차순 상위 5, 순위 숫자 1~5. **가격 오름차순 방향도 문답에 명시되지 않은 가정값** —
      사람이 내림차순으로 바꿀 수 있다). props: `products: Product[]`, `onSelect?`.
    - `src/features/shop/RecentlyViewed.tsx` + `.stories.tsx` — 최근 본 상품(있음/없음). props:
      `products: Product[]`(조회된 skus를 이 목록에서 찾아 표시 — 더 이상 판매중이 아니면 조용히 제외,
      없음을 지어내지 않는다).
    - `src/features/shop/ShopFooter.tsx` + `.stories.tsx` — 사업자 정보·고객센터·약관 링크 영역(정적).

- **데이터**: DB·테이블 변경 없음(확정 답변 db_ripple·db_migration 둘 다 "없음/불필요"). 신규 브라우저 저장
  1건: `localStorage['sl.shop.recentlyViewed']`(JSON 문자열 배열, 세션과 별개 키라 `sl.member.session`과
  충돌 없음). 트랜잭션 경계·락 없음(읽기 전용 화면). `ShopHomePage`는 `GET /api/products`를 **1회만** 호출해
  그 결과를 그리드·랭킹·최근본상품 조회에 공유한다(섹션마다 재요청하지 않음 — 같은 데이터를 클라이언트에서
  다르게 정렬/필터링만 한다).

- **순서·보안**: 이 SR은 신규 인증·판정 경로를 열지 않는다(읽기 전용, 신규 오류 코드 없음 — 확정 답변
  api_error). `/shop`은 **로그인 없이 접근 가능**해야 한다(확정문답 9 "GNB(비로그인·로그인)" — 비로그인이
  정상 상태다). 기존 `/`(주문 목록)는 **경로·동작 그대로 유지**한다 — SR 문답이 "기존 Thymeleaf 화면 경로
  불변"이라고만 명시하고 shop-web 라우트 경로 불변까지는 요구하지 않지만, 굳이 `/`를 이 새 화면으로 바꿔치기
  하면 "기존 주문 목록 화면과 그 동작 불변"(회귀 범위 확정 답변)을 건드릴 위험이 있어 더 보수적인 선택으로
  `/shop`을 **새 경로로 추가**한다(문답 원문의 "경로 /shop" 표현과 글자 그대로 일치). 부수효과 없음(로그·
  발송·이벤트·감사 대상 아님 — 읽기 전용 화면이라 해당 절 자체가 없음). 장바구니 수량 조회는 세션에 이미 있는
  `memberId`만 쓰고(신규 신원 확인 경로 아님), 세션이 없으면 아예 호출하지 않고 0으로 표시한다(추측성 폴백
  없음).

- **계약**: 새 오류 코드 없음(확정 답변 api_error). `GET /api/products`·`GET /api/cart` 둘 다 기존 계약을
  그대로 소비하고 요청 파라미터도 이미 있는 것만 쓴다(신규 API 없음). 상품 조회 실패(네트워크·5xx)는 화면
  문구 "불러오지 못했습니다" + [다시 시도]로만 처리(서버 계약 그대로, 확정 답변 api_error). 응답 코드별
  분기 없음(성공/실패 이분법뿐이라 SR-234 r1류 "미정의 코드가 default로 잘못 묶이는" 문제 자체가 발생하지
  않는 단순 형태).

- **테스트**:
  - `ShopHomePage.test.tsx`(jest+jsdom, `fetch` mock): ① 로딩→그리드 렌더 ② 상품 0건 → "표시할 상품이
    없습니다" + 카테고리 안내 ③ fetch reject/500 → 오류문구+[다시 시도], 클릭 시 재호출 ④ 비로그인 GNB(로그인
    링크) vs 로그인 GNB(회원명+로그아웃, `saveSession` 선주입) ⑤ 장바구니 수량 배지 0(비로그인) vs N
    (`GET /api/cart` mock, items qty 합산) ⑥ 상품 카드 클릭 → localStorage에 기록되고 "최근 본 상품" 섹션이
    "없음" 문구에서 그 상품으로 바뀜.
  - `recentlyViewed.unit.test.ts`: dedupe(같은 sku 재기록 시 맨 앞으로 이동, 중복 생성 안 함)·8개 cap·순서
    (최근이 앞).
  - Storybook 부품마다 `.stories.tsx`(규칙 `story-per-component`, 아래 "화면 상태" 매핑 그대로) —
    `test-storybook`이 콘솔 오류 없이 렌더되는지 검사.
  - 기존 스위트(`OrderListPage`·`LoginPage`·`PasswordResetPage` 테스트, `npm run test`의 타입체크 포함)는
    **무변경으로 통과**해야 한다(회귀, 새 라우트 추가 외 기존 파일 로직 변경 없음).

- **테스트 격리**: `ShopHomePage.test.tsx`는 `beforeEach`/`afterEach`에서 `localStorage.clear()`(세션 키 +
  최근본상품 키 둘 다 지움, `PasswordResetPage.test.tsx`와 동일 관례 — SR-232 r2 "카운터/상태가 테스트를
  가로질러 샌다" 사례의 로컬스토리지 버전 예방). `fetchMock`은 테스트마다 새 `jest.fn()`으로 재생성해 이전
  테스트의 mock 호출 이력이 새지 않게 한다. `recentlyViewed.unit.test.ts`도 각 테스트 앞뒤로
  `localStorage.clear()`.

- **폴백·우회 경로의 자격 판정**: 없음 — 이 SR이 여는 새 인증·조회 경로가 없다. 장바구니 수량 조회는 기존
  세션의 `memberId`만 그대로 전달할 뿐 새로운 신원 확인·캐시·화이트리스트를 만들지 않는다. 세션이 없으면
  호출 자체를 생략한다(탈퇴·만료 판정은 서버(`GET /api/cart`)가 이미 하는 일을 프론트가 다시 하지 않는다).

- **프레임워크 실행 모델 함정**: `HeroBannerCarousel`의 자동 넘김 `setInterval`은 `useEffect` 의존성을
  `[slides.length]`로 고정하고 `setIndex(i => (i + 1) % slides.length)` 함수형 업데이트를 쓴다 — 인덱스를
  의존성에 넣으면 렌더마다 타이머가 재시작돼 사실상 자동 넘김이 안 되고, cleanup(`clearInterval`)을 빠뜨리면
  React 19 StrictMode(dev) 마운트→언마운트→재마운트에서 인터벌이 중복 등록돼 두 배 속도로 넘어간다(가입
  refresh 이중 호출 결함(App.tsx `useSilentRefresh` 사례)과 같은 계열 — 여기선 네트워크 부작용이 아니라
  타이머 누수). `GET /api/products` 1회 조회 effect는 StrictMode 이중 호출이 나도 멱등 GET이라 결과에
  영향 없음(허용 가능).

- **범위 밖**: 상품 목록·상세·장바구니 화면(각각 별도 SR, 확정 답변 scope_freeze) — 상품 카드 클릭은 그래서
  **어디로도 이동하지 않고** "최근 본 상품" 기록만 한다(갈 상세 화면이 아직 없다). GNB 검색 입력은 제어
  컴포넌트로 값만 쥐고 제출은 no-op(결과를 보여줄 상품 목록 화면이 없다 — 그 화면이 생기는 SR에서 연결).
  카테고리 숏컷은 정적 표시만(카테고리 데이터·필터 API 자체가 없다 — `PRODUCTS`엔 카테고리 컬럼도 없음).
  실제 배너 운영 도구·추천/랭킹 알고리즘 없음(확정 답변 scope_freeze). 정가·할인율·이미지의 실데이터 반영은
  위 "데이터 갭" 참조 — 후속 SR 후보.

- **실패 사례집 대조**:
  - "신규 고객 화면을 shop-api Thymeleaf에 만들어 SPA·스토리북 밖으로 벗어났다"(SR-231 r1) — 이 SR도
    신규 화면이라 같은 함정이 성립한다. 위 파일 목록 전부 `modules/shop-web` 안에만 둔다(shop-api 쪽은
    한 파일도 건드리지 않는다).
  - "AIDD 잡이 도는 동안 `npm run dev`(HMR)를 켜 둬 OOM"(RUN7) — 이 SR도 shop-web 코드가 많아 조건이
    그대로 성립한다. 구현 단계에서 dev 서버를 띄우지 않는다.
  - "테스트가 로컬 상태(카운터/이메일 문자열 PK)를 테스트 간에 안 지워 누적 실패"(SR-232 r2) — 조건이
    localStorage로 형태만 바뀌어 성립한다. 위 "테스트 격리" 절대로 `localStorage.clear()`를 매 테스트
    앞뒤로 넣는다.
  - "정적 리소스 핸들러 SPA 폴백이 세그먼트 없는 루트에서 안 탔다"(SR-301 #1) — **조건이 다르다**: 그
    사례는 shop-api가 `/shop`(서버 URL 루트)을 직접 서빙하는 문제였고, 이번 `/shop`은 이미 그 위에서 로드된
    shop-web(HashRouter) **안의 클라이언트 라우트**(`#/shop`)라 서버 쪽 폴백 컨트롤러를 새로 건드릴 필요가
    없다 — 새 서버 라우팅 코드를 추가하지 않는다(추가하면 SR-301에서 이미 고친 걸 다시 건드리는 스코프
    확대다).
  - "예약 FUNC를 ID만 보고 역할을 반대로 구현"(SR-231 r1) — 이 story는 FUNC-ID가 아니라 확정 문답 9건이
    유일한 계약이라 그 위험 자체가 다르다. 대신 이 계획은 문답 원문을 그대로 인용해 재해석 여지를 줄였다
    (특히 "데이터 갭"·"범위 밖" 절에서 문답에 없는 가정값을 명시적으로 표시).

### 사람 수정 (계획 확인 게이트, 2026-09-17)
계획대로 진행 — 데이터 갭 처리 방식(정가·할인율·이미지를 운영 화면에서 비워 두고 지어내지 않음) 승인. 추가 지시:
- **명칭 고정**: `ProductCard`의 `listPrice`·`imageUrl` prop 이름을 그대로 유지할 것 — 후속 SR-306(정가·이미지를 `PRODUCTS`·`GET /api/products` 응답에 실제로 추가하는 SR, 사람이 접수 예정)이 이 이름을 그대로 이어받아 쓴다. 임의 개명 금지.
- **가정값 표시**: 그리드 표시 개수(8)·랭킹 탭별 개수(5)·가격 랭킹 정렬 방향(오름차순) — 이 세 값은 파일 상단에 명명된 상수로 모으고, 주석으로 "문답에 없어 계획에서 정한 값"이라고 남길 것.
- **테스트 추가**(계획의 6개 시나리오에 더해):
  - 정가·이미지가 없는 상태에서도 `ProductCard` 레이아웃이 깨지지 않는지(이니셜 대체 영역이 항상 렌더).
  - 품절 상품 배지 표시.
  - 최근 본 상품이 8개를 초과하면 가장 오래된 항목부터 제거되는지.
- 기존 shop-web 테스트 스위트 전부 무변경 통과가 완료 조건.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
- 계획 확인: 계획대로 진행(사람 수정 포함 — 명칭 고정·가정값 상수화·테스트 3건 추가) (2026-09-17)
- 구현 완료 (2026-09-17) — 전부 `modules/shop-web` 안(구현 모듈 제약). 새 서버(Java) 코드 없음, 신규 API 없음(기존 `GET /api/products`·`GET /api/cart`만 재사용).
  - 수정
    - `modules/shop-web/src/types.ts` — `Product` 타입 추가(`sku·productName·price·stockQty·saleYn`, `GET /api/products` 응답 그대로).
    - `modules/shop-web/src/api.ts` — `fetchProducts(keyword?)`·`fetchCartItemCount(memberId)` 추가(둘 다 raw 배열/`{items}` 봉투 방어적 파싱).
    - `modules/shop-web/src/App.tsx` — `<Route path="/shop" element={<ShopHomePage />} />` 추가(기존 라우트 무변경).
  - 신규 — 컨테이너
    - `modules/shop-web/src/pages/ShopHomePage.tsx` — 상품 1회 조회 + 세션 읽기 + 장바구니 수량 조회 + 최근 본 상품 상태를 조립.
    - `modules/shop-web/src/pages/ShopHomePage.test.tsx` — 통합 테스트 9건(계획 6건 + 사람 수정 추가 3건: 이니셜 대체영역 상시 렌더·품절 배지·최근본상품 8개 cap).
  - 신규 — `modules/shop-web/src/features/shop/`
    - `shopStatic.ts`(배너·카테고리 정적 목업), `recentlyViewedStorage.ts` + `recentlyViewedStorage.unit.test.ts`(localStorage 유틸, dedupe·8개 cap·순서 — 최근이 맨 앞).
    - `Gnb.tsx`, `HeroBannerCarousel.tsx`, `CategoryShortcuts.tsx`, `ProductCard.tsx`, `ProductGrid.tsx`, `RankingSection.tsx`, `RecentlyViewed.tsx`, `ShopFooter.tsx` — 각각 `.stories.tsx` 짝(규칙 `story-per-component`).
  - 주요 결정
    - `recentlyViewed.ts`(계획 원안 파일명)는 `RecentlyViewed.tsx`(컴포넌트)와 대소문자만 달라 Windows(대소문자 비구분 파일시스템)에서 TS1149로 타입체크가 죽었다 — `recentlyViewedStorage.ts`로 개명해 해소(계획 의도·계약은 동일, 파일명만 변경).
    - `### 사람 수정` 그대로 반영: `ProductCard`의 `listPrice`/`imageUrl` prop 이름 고정(SR-306이 이어받음), 가정값 3개(그리드 8·랭킹 5·가격 오름차순)는 각 파일 상단 명명된 상수(`PRODUCT_GRID_DISPLAY_COUNT`·`RANKING_ITEM_COUNT`·`sortByPriceAscending`)로 모으고 "문답에 없어 계획에서 정한 값" 주석을 남김.
    - 정가·할인율·이미지(데이터 갭)는 운영 화면에서 절대 채워 넣지 않음(`ShopHomePage`가 `ProductCard`에 세 prop을 넘기지 않음) — 스토리에서만 목업 args로 모양을 보여줌.
    - `npm run dev`(HMR)는 띄우지 않았다(RUN7 OOM 사례). `npm test`(타입체크+jest, 기존 스위트 포함 전체 47건)만 실행해 통과 확인.
  - 검증: `npm test` — 타입체크 통과, 전체 테스트 스위트(6개 파일, 47건) 통과(기존 `PasswordResetPage.test.tsx`·`redirectTarget`·`refreshOnce`·`requestCodeOnce` 무변경 통과 포함).

- 재작업 완료(round 1, 2026-09-17) — 사람 코멘트 "[고칠 것]" 1)·2)·3)·5)·6)만 반영(4·저-등급 2건은 별도 SR/추적만, 범위 밖). 전부 `modules/shop-web` 안(구현 모듈 제약 불변), 새 서버(Java) 코드 없음.
  - 수정
    - `modules/shop-web/src/App.tsx` — `isShopServerRoot()`(판정: `pathname==='/shop' || pathname.startsWith('/shop/')`, `ApiKeyAuthFilter`의 화이트리스트 조건과 동일) 신설. `/` 라우트의 element를 `isShopServerRoot() ? <Navigate to="/shop" replace/> : <OrderListPage/>`로 분기 — shop-api가 SPA를 서빙하는 `/shop` 서버 경로에서 해시 없이 들어오면 쇼핑 홈으로 리다이렉트하고, 그 외 경로(dev 서버 루트 등 기존 사용자 경로)는 주문 목록 그대로(고칠 것 1). 렌더 시점에 순수 계산이라 effect 불필요 — OrderListPage가 잠깐 마운트되는 깜빡임이 없다.
    - `modules/shop-web/src/api.ts` — `logout(apiKey)` 추가(`POST /api/members/sessions/logout`, `X-Api-Key` 직접 세팅 — 이 SPA에서 클라이언트가 처음으로 이 헤더를 직접 보내는 호출, `vite.config.ts` 프록시 폴백 통과 확인됨). 신규 API 아님(INF-MBR-004 재사용).
    - `modules/shop-web/src/pages/ShopHomePage.tsx` — `handleLogout`이 `logout(apiKey)`를 먼저 호출(실패해도 `.catch(()=>{}).finally(clear)`로 로컬 세션은 반드시 정리, 세션에 apiKey가 없으면 즉시 로컬 정리)(고칠 것 2). `load()`에 `inFlightRef`(useRef) 기반 in-flight 가드 추가 — 이미 진행 중이면 재호출을 조용히 무시(고칠 것 5).
    - `modules/shop-web/src/features/shop/Gnb.tsx` — 장바구니 링크(`<a href="#/cart">`)를 라우트 없는 `<span aria-disabled="true" title="준비 중">`로 교체(회색 처리, `aria-label` 유지)(고칠 것 3).
    - `modules/shop-web/src/features/shop/RankingSection.tsx` — 순위 숫자 `<span>`의 `aria-hidden="true"` 제거(고칠 것 6).
  - 신규 테스트
    - `modules/shop-web/src/App.test.tsx` — `/shop` 서버 경로(해시 없음) 마운트 시 쇼핑 홈(GNB `SL Shop` 링크) 렌더 확인 + `/shop`이 아닌 경로는 기존대로 주문 목록(`주문 목록` 헤딩) 확인(회귀).
    - `modules/shop-web/src/pages/ShopHomePage.test.tsx` — 로그아웃 테스트(logout API가 `POST` + `X-Api-Key: 세션.apiKey`로 호출되고 500 실패에도 로컬 세션이 지워져 GNB가 비로그인으로 돌아가는지) + 재시도 연타 테스트(in-flight 가드로 연타 3회 중 실제 추가 요청은 1건만 나가는지, 이후 정상 해소 확인) 추가. `routeFetch` 헬퍼에 `logout` 핸들러 분기 추가(기존 테스트 무변경 호환).
  - 이번 라운드에 안 고친 것(사람 코멘트 "[이번에 안 고치는 것]" 그대로): shop-api `/api/products` 401(`ApiKeyAuthFilter.isOpenRoute` 미등록, 서버 변경이라 범위 밖 — 별도 SR 대상) · `npm run test-storybook` 미실행(신규 스토리 10건, 특히 `RankingSection.stories.tsx:가격탭전환`의 `play` 상호작용) · `Gnb.tsx`의 `aria-haspopup="true"` 불일치(메뉴 없음) — 추적만, 이번 범위 밖.
  - `npm run dev`(HMR)는 이번에도 띄우지 않았다(RUN7 OOM 사례 반복 적용). `npm test`(타입체크+jest)만 실행.
  - 검증: `npm test` — 타입체크 통과, 전체 테스트 스위트(7개 파일, 51건 = 기존 47 + 신규 4: App.test.tsx 2건 + ShopHomePage.test.tsx 2건) 전부 통과.

- 재작업 완료(round 2, 2026-09-17) — 사람 결정(round 2 FAIL 재작업 지시 1·2)만 반영. 3(아리아 라벨)·4(test-storybook 미실행)는 QA 권고에서도 "다음 라운드에 함께 고쳐도 좋음 — low, 게이트를 세우지 않음"으로 명시돼 이번 범위 밖(추적만, round 1과 동일한 처리 방식). 전부 `modules/shop-web` 안(구현 모듈 제약 불변), 새 서버(Java) 코드 없음.
  - 수정
    - `modules/shop-web/src/App.tsx` — [지시 1] round1의 "/" 라우트 element 삼항연산(`isShopServerRoot() ? <Navigate to="/shop" replace/> : <OrderListPage/>`)을 되돌려 "/" 라우트는 다시 무조건 `<OrderListPage/>`. `isShopServerRoot()` 판정은 그대로 두되, 리다이렉트 로직을 **부팅 시 1회 호출용 export 함수** `applyShopBootRedirect()`로 분리(`if (isShopServerRoot() && !window.location.hash) window.location.hash = '#/shop'`) — 더 이상 라우트/컴포넌트 렌더 경로에 존재하지 않는다. `Navigate` import 제거(미사용).
    - `modules/shop-web/src/main.tsx` — `createRoot(...).render(<App/>)` **이전에** `applyShopBootRedirect()`를 1회 호출(사람 지시 원문 "라우터 마운트 전(모듈 스코프/main.tsx)에서" 중 `main.tsx` 쪽 선택 — 실제 앱 부팅에서 정확히 한 번만 실행되고, StrictMode 이중 렌더나 이후 라우트 전환에는 전혀 관여하지 않는다).
  - 재작성 테스트
    - `modules/shop-web/src/App.test.tsx` — 전면 재작성(4건). round1 버전은 `jest.resetModules()` + 동적 `import('./App')`로 "재부팅"을 흉내 내려 했으나, 이 방식은 React/ReactDOM 모듈까지 레지스트리에서 지워버려 `@testing-library/react`가 들고 있던 React 인스턴스와 새로 재로딩된 App의 React 인스턴스가 달라져 "Invalid hook call"로 전부 깨졌다(실측, 아래 "주요 결정" 참조) — 그래서 `applyShopBootRedirect()`를 export해 테스트가 라우터 렌더 이전에 **직접 호출**하는 방식으로 바꿨다(모듈 재로딩 불필요, `main.tsx`가 실제로 하는 일과 동일한 함수 호출).
      1. `/shop` 서버 경로 + 빈 해시 + `applyShopBootRedirect()` 호출 → 쇼핑 홈(`SL Shop` 링크) 렌더(기존 유지).
      2. `/shop`이 아닌 경로 + `applyShopBootRedirect()` 호출(멱등이라 아무 일도 안 함) → 주문 목록 그대로(기존 유지).
      3. **신규(회귀)** — `/shop` 서버 경로인데 해시가 이미 `#/`(=이미 진입한 뒤 상태) → `applyShopBootRedirect()`는 해시가 있어 아무 일도 안 하고, "/" 라우트는 무조건 `OrderListPage`이므로 주문 목록이 렌더된다(쇼핑 홈으로 안 튕김). round1 코드였다면 pathname만 보고 매 렌더 다시 Navigate했을 상황을 정확히 재현해 회귀를 잡는다.
      4. **신규(회귀, 완료조건 "주문 상세 → 주문 목록 이동")** — `/shop` 서버 경로 + 해시 `#/orders/20260101-1`로 진입 → 주문 상세의 "← 주문 목록" 링크 클릭 → 주문 목록 렌더 확인. 이 테스트를 쓰며 `GET /api/orders/:orderNo` 목업이 빈 배열이면 `OrderDetailCard`가 `order.deliveries`를 스프레드하다 크래시해(`TypeError: order.deliveries is not iterable`) 트리 전체가 언마운트되는 것을 실측으로 발견 — beforeEach의 fetch 목업을 URL 분기(`/api/orders/`로 시작하면 `items·deliveries` 빈 배열을 포함한 최소 `OrderDetail` 셰이프 반환)로 고쳐 해소.
    - `modules/shop-web/src/pages/ShopHomePage.test.tsx` — [지시 2] "재시도 버튼 연타" 테스트를 `act` import 추가 후 재작성. 종전 테스트는 `fireEvent.click`을 세 번 분리 호출했는데, `fireEvent`가 호출마다 `act()`로 감싸 즉시 리렌더를 flush하고 `ProductGrid`는 `error`를 최우선 분기해 첫 클릭 순간 [다시 시도] 버튼이 로딩 스피너로 교체돼 DOM에서 사라진다 — 2·3번째 `fireEvent.click`이 이미 제거된 노드에 발사돼 `onClick`이 아예 실행되지 않아 `inFlightRef` 가드 없이도 `calls===2`가 성립했다(거짓 보증). 고친 버전은 네이티브 `.click()` 세 번을 **하나의 `act(() => {...})` 안에서** flush 없이 연달아 호출해 버튼이 살아있는 동안 세 번의 동기 클릭이 모두 디스패치되게 한다.
  - 가드 제거 시 테스트 실패 확인(사람 지시, 실측) — `ShopHomePage.tsx`의 `if (inFlightRef.current) return`을 임시로 주석 처리하고 `npx jest -t "재시도 버튼 연타"`를 단독 실행한 결과, `expect(calls).toBe(2)`가 **"Expected: 2 / Received: 4"로 실패**함을 확인했다(가드 없이는 세 번의 동기 클릭이 전부 `fetchProducts()`를 호출). 가드를 원상복구한 뒤 재실행해 다시 통과함을 확인했고, `ShopHomePage.tsx` 자체는 이번 라운드에 순수하게 원상태(가드 코드 무변경)로 남았다.
  - 주요 결정
    - 부팅 1회성 사이드이펙트를 App.tsx 모듈 최상위 코드가 아니라 **`main.tsx`가 호출하는 export 함수**로 구현했다(사람 지시 "모듈 스코프/main.tsx" 중 후자 선택). 이유: 테스트에서 "모듈 최상위 코드를 매 시나리오마다 재평가"시키려면 `jest.resetModules()`가 필요한데, 이는 React/ReactDOM까지 리셋해 "Invalid hook call"을 유발함을 실측으로 확인했다(위 App.test.tsx 항목 참조) — export 함수 + 직접 호출 방식은 이 문제를 원천적으로 피하면서도 "라우터 렌더 이전, 부팅 시 1회"라는 사람 지시의 의미를 그대로(오히려 더 명시적으로) 만족한다.
    - `isShopServerRoot()`가 이미 자연히 멱등(해시가 한 번 세팅되면 이후 재호출은 조건이 거짓이 돼 아무 일도 하지 않음)이라 별도 "이미 실행됨" 가드 플래그를 추가하지 않았다.
    - `Gnb.tsx`·`RankingSection.tsx`는 이번 라운드에 손대지 않았다 — round2 QA FAIL의 "필수 수정"은 1·2뿐이고, 재작업 지시 3(아리아 라벨)·4(test-storybook 미실행)는 QA 권고에서도 "다음 라운드에 함께 고쳐도 좋음 — low, 게이트를 세우지 않음"으로 명시돼 사람의 round2 재작업 지시(사람 결정) 본문도 1·2만 다뤘다. 추적만 유지.
    - `npm run dev`(HMR)는 이번에도 띄우지 않았다(RUN7 OOM 사례 반복 적용). `npm test`(타입체크+jest)만 실행.
  - 검증: `npm test` — 타입체크 통과, 전체 테스트 스위트(7개 파일, 53건 = round1 51건 − App.test.tsx 구 2건 + 신규 4건) 전부 통과.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-17 — CONCERNS
- Layer1 스펙: **대체로 일치**. 확정문답 7개 요소(GNB·히어로 배너·카테고리 숏컷·추천 그리드·랭킹 탭·최근 본 상품·푸터)와 §5 빈값/오류 표기(0건·조회실패+다시시도·최근본상품 없음·이니셜 대체·품절 배지+흐림)가 전부 구현·스토리로 존재. 사람 수정 3건 실측 확인 — ① `ProductCard`의 `listPrice`/`imageUrl`(+`discountRate`) prop 명칭 그대로 유지(SR-306 인계 주석 포함) ② 가정값 3개가 `PRODUCT_GRID_DISPLAY_COUNT=8`(ProductGrid.tsx:10)·`RANKING_ITEM_COUNT=5`(RankingSection.tsx:12)·`sortByPriceAscending`(RankingSection.tsx:14)로 각 파일 상단에 모이고 "문답에 없어 계획에서 정한 값" 주석이 붙음(정렬 방향만 상수가 아닌 함수 형태지만 단일 변경점·주석 요건은 충족) ③ 추가 테스트 3건(이니셜 대체영역 상시 렌더·품절 배지·최근본상품 8개 cap) 실존·통과. 데이터 갭 처리도 계획대로 — `ShopHomePage`가 `ProductCard`에 정가/할인율/이미지를 **한 번도 넘기지 않아** 운영 화면에서 지어낸 값이 렌더되지 않고, 목업은 스토리 args에만 있다. 다만 "경로 /shop"의 실제 진입점이 쇼핑 홈이 아니다(권고 1).
- Layer2 보안: **경미 이슈 1건**. 새 인증·판정 경로를 열지 않았고(읽기 전용 GET 2종만 소비), 신규 오류 코드 없음, `dangerouslySetInnerHTML`·직접 `fetch` 없음(`web-fetch-only-in-api` 준수), 장바구니 조회는 세션의 `memberId`만 그대로 전달(자격 판정을 프론트가 재현하지 않음), 세션 없으면 호출 자체 생략. 단, 이 SPA에서 **처음 생긴 사용자용 로그아웃 버튼**이 서버 세션을 폐기하지 않는다(권고 2).
- Layer3 회귀: **경미 이슈 2건**. `modules/shop-api` 파일은 **한 건도 수정되지 않았다**(mtime 실측, 01:00 이후 변경 0건 — SR-231 r1 사례 재발 없음). `App.tsx`는 기존 4개 라우트 무변경 + `/shop` 추가만. `api.ts`·`types.ts`는 순수 추가(기존 함수·타입 무변경). `npm test` 47건/6스위트 전부 통과(기존 `PasswordResetPage`·`redirectTarget`·`refreshOnce`·`requestCodeOnce` 포함). `localStorage.clear()` 격리도 양쪽 테스트에 적용(SR-232 r2 사례 예방 확인). 캐러셀 `setInterval`은 `[slides.length]` 의존 + 함수형 업데이트 + `clearInterval` cleanup으로 StrictMode 이중 등록을 막았다. 남은 위험은 GNB의 죽은 링크(권고 3)와 shop-api 서빙 환경에서의 401(권고 4).
- 권고(CONCERNS시):
  1. **(medium, 스펙) `/shop`으로 들어가면 쇼핑 홈이 아니라 주문 목록이 뜬다.** SR-301이 SPA를 서버 경로 `/shop`에 마운트했고(`ShopIndexController`), 이번 화면은 그 SPA 안의 해시 라우트 `#/shop`이라 실제 URL은 `/shop#/shop`이다. `/shop`만 치면 HashRouter 기본 경로 `#/` → `OrderListPage`(주문 목록)가 뜬다. 게다가 `#/shop`으로 가는 링크가 앱 어디에도 없다(GNB 로고는 이미 그 화면 안에 있을 때만 보인다) — 확정문답 "쇼핑 홈 화면 신규(경로 /shop)"를 손으로 URL을 치지 않으면 만족할 수 없다. 계획이 이 선택을 명시했고 사람이 승인했으므로 FAIL로 올리지 않되, `#/`를 쇼핑 홈으로 바꿀지·리다이렉트할지·진입 링크를 어디에 둘지 **이번 라운드에 정해야** 다음 화면 SR(상품 목록·상세·장바구니)이 같은 판단을 반복하지 않는다. 참고: `shop-api`에 `static/shop/`이 없고 `shop-web/dist`도 이 구현 이전(00:33) 산출물이라, 서버 진입점에서 이 화면을 보려면 재빌드·배치가 선행된다.
  2. **(medium, 보안) GNB 로그아웃이 `clearSession()`만 호출한다**(`ShopHomePage.tsx:66`). `POST /api/members/sessions/logout`(INF-MBR-004, apiKey+refreshToken 전량 폐기)이 이미 있는데 호출하지 않아, 로그아웃해도 그 세션의 `apiKey`·`refreshToken`이 만료까지 서버에서 유효하다(XSS 등으로 이미 유출된 토큰이 로그아웃으로 무효화되지 않는다). `PasswordResetPage`가 `clearSession()`만 쓰는 것은 서버가 이미 전량 폐기한 뒤의 로컬 정리라 성격이 다르다 — 이 버튼이 SPA 최초의 진짜 로그아웃이다. 기존 API 재사용이라 "신규 API 없음" 제약과 충돌하지 않는다(`/api/cart` 재사용과 같은 성격). 단, 이 호출은 회원 `apiKey`를 `X-Api-Key`로 실어 보내야 하므로(필터 화이트리스트 밖) 배선 결정이 필요하다.
  3. **(medium, 회귀) GNB 장바구니 아이콘이 없는 라우트로 링크한다** — `Gnb.tsx:36`의 `href="#/cart"`. `App.tsx`에 `/cart` 라우트가 없어 클릭하면 `<Routes>`가 아무것도 렌더하지 않아 **빈 화면**이 되고 돌아올 길은 브라우저 뒤로가기뿐이다. 같은 이유로 상품 카드 클릭은 "어디로도 이동하지 않는다"고 계획이 명시했는데(장바구니 화면은 별도 SR) 장바구니 아이콘만 링크로 남았다. 장바구니 SR이 생길 때까지 링크를 걷어내거나(표시만) 미구현 안내를 띄운다.
  4. **(medium, 회귀) shop-api가 서빙하는 `/shop` 환경에서는 상품 조회가 401로 전부 실패한다.** `ApiKeyAuthFilter.isOpenRoute`에 `/api/products`가 없어(실측) 브라우저가 `X-Api-Key` 없이 부르면 막힌다. 지금 동작하는 이유는 vite dev 프록시(:5273)가 admin 키를 폴백 주입하기 때문이고, SR-301이 만든 진짜 진입점(:8087/shop)에는 그 프록시가 없다. 이 SR이 만든 결함은 아니지만(기존 `/api/orders`도 동일), **"로그인 없이 접근 가능한 공개 커머스 홈"이라는 이번 AC는 그 환경에서 화면이 "불러오지 못했습니다"만 보이므로 충족되지 않는다.** `/product/**` 화면 경로가 이미 무인증인 것과 일관되게 `/api/products`를 공개 화이트리스트로 올릴지, SPA가 세션 apiKey를 실어 보내게 할지 결정이 필요하다(서버 변경이라 이 항목 범위 밖 — 후속 SR 후보로 명시).
  5. (low, 접근성) 랭킹 순위 숫자가 `aria-hidden="true"`(`RankingSection.tsx:54`)라 스크린리더에 순위 정보가 전혀 전달되지 않는다. 확정문답의 "순위 숫자"가 시각 사용자에게만 존재한다. `<ol>` 안이라 순서는 전달되지만 "1위" 텍스트 대안은 없다.
  6. (low, 견고성) `ShopHomePage.load()`에 in-flight 가드가 없어 [다시 시도] 연타 시 늦게 도착한 응답이 이긴다(last-write-wins). 읽기 전용이라 피해는 표시 지연뿐 — 후속 TODO.
  7. (low, 검증) `npm run test-storybook`이 실행되지 않았다(`npm test`=타입체크+jest만). 신규 스토리 10개, 특히 `play` 상호작용이 있는 `RankingSection.stories.tsx:가격탭전환`이 한 번도 렌더 검증되지 않아, 콘솔 오류·렌더 실패가 있으면 다음 화면 상태 기준선 재측정에서야 드러난다.
  8. (low, 접근성) `Gnb.tsx:27`의 "카테고리" 버튼이 `aria-haspopup="true"`인데 열 팝업이 없다(메뉴는 범위 밖). 보조기술에 없는 기능을 약속한다 — 속성을 빼거나 `disabled` 표시.

### QA Gate — 2026-09-17 (round 2) — FAIL
- Layer1 스펙: **재작업 5건 중 4건은 지시대로, 1건은 지시보다 넓게 구현됐다.** 실측 대조 — ② 로그아웃: `api.ts:164-171`의 `logout(apiKey)`가 `POST /api/members/sessions/logout`에 `X-Api-Key`만 싣고 바디 없이 호출(INF-MBR-004 요청 계약 "바디 없음 + X-Api-Key 필수"와 정확히 일치), `ShopHomePage.tsx:79-87`이 `logout().catch(()=>{}).finally(clear)`라 **성공·실패 양쪽 모두 로컬 세션을 정리**하고 apiKey가 없으면 즉시 로컬 정리(사람 코멘트 2 원문 "네트워크 실패로 로그아웃 자체가 막히면 안 됨" 충족). ③ 장바구니: `Gnb.tsx:39-49`가 `<a href="#/cart">` → `<span aria-disabled="true" title="준비 중">`, 핸들러·href 모두 없어 **클릭이 진짜 무반응**(디자인만 회색 처리한 게 아님). ⑤ in-flight 가드: `ShopHomePage.tsx:37-53`이 `useRef`를 `await` 이전에 세팅하고 `finally`에서 해제 — 디바운스(타이머)가 아니라 **요청 자체를 막는 가드**가 맞다. ⑥ `RankingSection.tsx:57`의 `aria-hidden="true"` 제거 확인. ①만 지시("**최초 진입 시**" 리다이렉트)보다 넓게 들어갔다(필수 수정 1).
- Layer2 보안: **차단 이슈 없음**. 이 SPA 최초의 진짜 로그아웃이 서버 토큰(apiKey+refreshToken 전량)을 폐기하도록 배선됐다 — round 1 보안 권고 2 해소. `X-Api-Key`를 클라이언트가 직접 싣는 첫 호출이지만 동일 오리진 요청이고 값은 세션이 이미 보유한 회원 키뿐(admin 키 하드코딩·프록시 폴백 의존 없음, `vite.config.ts:35`가 클라이언트가 보낸 키를 덮어쓰지 않는 것도 실측 확인). 로컬 정리가 서버 폐기 실패보다 우선하는 설계는 "토큰이 서버에 남을 수 있다"는 잔여 위험이 있으나 **사람이 명시적으로 그 순위를 지정**했고, 그 위험은 종전(호출 자체가 없음)보다 엄밀히 작다. 신규 인증·판정 경로·오류 코드 없음, 직접 fetch 없음(`web-fetch-only-in-api` 준수).
- Layer3 회귀: **차단 회귀 1건**. 좋은 쪽 — `modules/shop-api`는 이번에도 **0건 수정**(최신 파일 `ShopIndexControllerTest.java` 00:43, 전부 SR-301 산출물. `ApiKeyAuthFilter.java`는 00:19 그대로 — 사람이 "안 고치는 것"으로 지정한 4번 401 이슈를 건드리지 않았다). `Gnb.tsx`의 `aria-haspopup`(27행)도 그대로 두었고 스토리 파일·`test-storybook`도 미변경/미실행(추적 대상 유지). `npm test` 7스위트 51건 전부 통과(기존 `PasswordResetPage`·`redirectTarget`·`refreshOnce`·`requestCodeOnce` 무변경 통과 포함). 나쁜 쪽 — `App.tsx:67`의 리다이렉트가 `/shop` 마운트에서 **주문 목록을 세션 내내 도달 불가로 만들고 주문 상세의 "← 주문 목록" 링크를 깨뜨린다**(필수 수정 1). 확정 문답의 회귀 범위("기존 주문 목록·상세 화면과 그 동작 불변")에 정면으로 걸린다. 더해 in-flight 가드 테스트가 가드를 실제로 검증하지 못한다(필수 수정 2).
- 필수 수정(FAIL시):
  1. **(medium, 회귀) `/shop` 마운트에서 주문 목록이 영구 도달 불가 — 주문 상세의 back 링크가 쇼핑 홈으로 튄다.** `App.tsx:67`은 `/` 라우트의 element **자체**를 `<Navigate to="/shop" replace/>`로 치환한다. `pathname`은 HashRouter 세션 동안 바뀌지 않으므로(코드 주석도 그렇게 적고 있다) `/shop` 마운트에서는 `#/`가 **언제 어떤 경로로 들어오든** 항상 쇼핑 홈으로 replace된다 — 사람 코멘트 1의 "**최초 진입 시**"보다 넓다. 구체 실패: `/shop#/orders/20260101-1`(정상 렌더) → `OrderDetailPage.tsx:26`의 `<Link to="/">← 주문 목록</Link>` 클릭 → `#/` → 쇼핑 홈. `replace`라 뒤로가기도 주문 상세로 되돌아가 **주문 목록에 갈 방법이 없다**. `LoginPage.tsx:29`의 로그인 성공 후 기본 이동(`resolveRedirectTarget(null) === '/'`)도 같은 이유로 주문 목록 대신 쇼핑 홈으로 간다. 이 마운트는 이미 라이브다(`modules/shop-api/target/classes/static/shop/index.html` 실재, 00:33 빌드) — 재빌드·재기동 시점에 그대로 드러난다. → 리다이렉트를 **진입 1회**로 좁힌다: 라우터 마운트 전(모듈 스코프 또는 `main.tsx`)에서 `if (isShopServerRoot() && !window.location.hash) window.location.hash = '#/shop'`를 한 번만 수행하고 `/` 라우트는 `<OrderListPage/>`로 되돌린다. 그러면 최초 진입은 쇼핑 홈, 이후 `#/`는 주문 목록 그대로가 되어 사람 지시 원문과 회귀 범위를 동시에 만족한다. 기존 `App.test.tsx` 2건은 그대로 성립하고, "주문 상세 → 주문 목록 복귀가 `/shop` 마운트에서도 동작"하는 테스트를 1건 추가한다.
  2. **(medium, 테스트) in-flight 가드 테스트가 가드를 검증하지 못한다 — 가드를 지워도 통과한다.** `ShopHomePage.test.tsx:201-226`. `ProductGrid.tsx:22`는 `error`가 최우선 분기라, 첫 클릭에서 `setError(null)+setLoading(true)`가 반영되는 순간 [다시 시도] 버튼이 **DOM에서 제거**된다. RTL `fireEvent`는 `act`로 감싸 클릭마다 렌더를 flush하므로 2·3번째 클릭은 **분리된 노드**에 디스패치돼 React 루트 위임 리스너에 닿지 않고, 핸들러가 아예 실행되지 않는다 — 즉 `calls===2`는 `inFlightRef`가 없어도 성립한다. 사람이 완료 조건으로 명시한 "재시도 버튼 연타 시 요청이 한 번만 나가는지"가 거짓 보증이 된다(가드 **코드 자체는 올바르다** — ref라 동기 반영, `await` 이전 세팅, `finally` 해제). → 세 클릭을 한 `act()` 안에서 flush 없이 연달아 발사해(`act(() => { btn.click(); btn.click(); btn.click() })`) 버튼이 살아 있는 동안 가드가 막는지 보거나, 컨테이너의 재조회 경로를 직접 두 번 호출해 검증한다.
- 권고(다음 라운드에 함께 고쳐도 좋음 — low, 게이트를 세우지 않음):
  1. (low, 접근성) 장바구니를 `<a>`(role=link) → `<span>`(role=generic)으로 바꾸면서 `aria-label="장바구니 N개"`(`Gnb.tsx:39`)가 보조기술에 노출되지 않을 수 있다 — ARIA는 generic role에 `aria-label`을 매핑하지 않고, 배지 숫자는 `aria-hidden="true"`(43행)라 **수량 정보가 통째로 사라진다**(라운드 1엔 link라 노출됐다). 같은 라운드에 접근성을 이유로 `aria-hidden`을 걷어낸 것과 방향이 어긋난다. 테스트의 `getByLabelText('장바구니 0개')`는 role 제약을 보지 않아 통과하므로 이 역시 거짓 보증이다. → `role="img"`를 주거나 시각적 숨김 텍스트로 수량을 노출.
  2. (low, 검증) `RankingSection.tsx`·`Gnb.tsx`가 이번 라운드에 바뀌었는데 짝 스토리는 01:24~01:26 그대로고 `test-storybook`은 여전히 미실행이다(사람이 이번 범위 밖으로 명시 — 추적만). 화면 상태 기준선 재측정 **전에** 1회 실행 필요. 특히 `RankingSection.stories.tsx:가격탭전환`의 `play`.
  3. (low, 추적) 사람이 별도 SR로 뺀 2건은 그대로 미해결이다 — `/api/products` 401(`ApiKeyAuthFilter.isOpenRoute` 미등록)·`Gnb.tsx:27` `aria-haspopup` 불일치. 지시대로 건드리지 않았음을 실측 확인했고, 추적 상태만 유지한다.
- 재동기화 입력(STEP 5.5 몫 — 권고로 올리지 않는다):
  - `UIS-ORD-008`(예약, 본문 역생성 대상) — "`/shop` 서버 경로에 해시 없이 진입하면 쇼핑 홈" 진입 규칙과 `/shop#/`와 `/shop#/shop`의 구분을 §진입/라우팅 절에 적어야 한다(필수 수정 1 반영 후 확정값으로).
  - `INF-ORD-017`(SR-301) — 서버 응답 계약 자체는 불변이나, `/shop`·`/shop/`가 index.html을 반환한 **뒤** 클라이언트가 어느 화면을 띄우는지가 이번에 정해졌다. 본문 "응답" 표 각주 또는 "비즈니스 규칙"에 클라이언트 진입 규칙 한 줄을 붙일지 5.5에서 판단한다(서버 앵커 줄 범위는 변경 없음).

### QA Gate — 2026-09-17 (round 3) — PASS
- Layer1 스펙: **재작업 지시 1·2(사람이 결정한 필수 2건)가 지시 원문 그대로, 넓지도 좁지도 않게 반영됐다.** 실측 대조 — ① `App.tsx:86`의 `/` 라우트는 다시 **무조건** `<OrderListPage/>`(삼항연산·`Navigate` import 모두 제거, `grep`으로 `Navigate` 잔존 0건 확인). 리다이렉트는 `applyShopBootRedirect()`(`App.tsx:75-79`)로 분리됐고 **라우트·컴포넌트 렌더 경로에 리다이렉트 코드가 한 줄도 없다**. 발동 조건은 `isShopServerRoot() && !window.location.hash` = `pathname==='/shop' || pathname.startsWith('/shop/')` **그리고** 해시가 빈 경우 — 사람 지시 원문("location.pathname이 /shop 이거나 /shop/로 시작하고, location.hash가 비어 있을 때만")과 글자 단위로 일치하고, 서버 쪽 진입 계약(`ShopIndexController:46` `@GetMapping({"/shop","/shop/"})`)과도 어긋나지 않는다. 호출 지점은 `main.tsx:10` — `createRoot(...).render(<App/>)` **이전**의 모듈 최상위 1회라 StrictMode 이중 마운트·이후 라우트 전환에 전혀 재진입하지 않는다(사람 지시 "라우터 마운트 전(모듈 스코프/main.tsx)" 충족). ② in-flight 가드 테스트 재작성 확인(아래 Layer3). 나머지 화면 요구(GNB·배너·카테고리·그리드·랭킹·최근본상품·푸터, §5 빈값/오류 표기)는 round 1에서 이미 충족 확인했고 이번 라운드에 해당 파일이 변경되지 않았다(mtime 01:24~01:41 = round1·2 산출물 그대로).
- Layer2 보안: **차단 이슈 없음, 신규 노출면 0**. 이번 라운드 변경분은 `App.tsx`·`main.tsx`·`App.test.tsx`·`ShopHomePage.test.tsx` 4개와 `ShopHomePage.tsx`(가드 제거→복원으로 mtime만 갱신, **내용은 round2와 동일 — 가드 `if (inFlightRef.current) return`가 `ShopHomePage.tsx:40`에 그대로 있고 아래 mutation 검증으로 실재 확인**)뿐이다. 새 인증·판정 경로·오류 코드·엔드포인트 없음. 리다이렉트가 쓰는 값은 하드코딩 상수 `'#/shop'`이라 사용자 입력이 흘러드는 오픈 리다이렉트 면이 없다(`location.hash`를 읽어 목적지로 삼지 않는다 — **읽어서 조건 판정만** 한다). round2에서 배선된 로그아웃(`api.ts:164` `logout(apiKey)`)·`X-Api-Key` 취급은 무변경. 규칙 재확인 — `console.log` 0건, `src/**/*.tsx` 직접 `fetch` 0건(`web-fetch-only-in-api` 준수).
- Layer3 회귀: **차단 회귀 없음 — 재작업 2건을 "고쳤다는 보고"가 아니라 mutation으로 직접 검증했다.** ① `App.test.tsx`의 신규 회귀 테스트가 진짜인지 확인하려고 round1 코드(`/` 라우트 element를 `isShopServerRoot() ? <Navigate to="/shop" replace/> : <OrderListPage/>`)를 **임시로 복원**해 단독 실행한 결과, 테스트 3(`#/`로 이동한 상태에서 주문 목록)과 테스트 4(주문 상세 "← 주문 목록" 클릭)가 **정확히 둘 다 실패**했다(2 failed / 2 passed). 즉 사람이 완료 조건으로 명시한 "`#/`로 이동하면 주문 목록"·"주문 상세 → 주문 목록 복귀"가 거짓 보증이 아니다. 원본은 md5(`4435888d…`) 일치로 복원 확인. ② in-flight 가드 테스트(`ShopHomePage.test.tsx:218-243`)는 `fireEvent` 3회 → **하나의 `act()` 안에서 네이티브 `.click()` 3회**로 재작성됐다. 방식 타당성 판단(코드를 직접 읽고 판정) — `act()` 스코프 안에서는 discrete 이벤트가 만든 sync 갱신이 act 큐에 쌓여 **콜백이 끝날 때까지 커밋되지 않으므로** 세 번의 클릭 동안 [다시 시도] 버튼이 DOM에 살아 있고, 세 핸들러가 모두 실행된다 — round2 FAIL의 "분리된 노드에 발사" 구조가 실제로 해소된다. 이를 dev 보고와 독립적으로 확인하려고 `ShopHomePage.tsx:40`의 가드를 직접 주석 처리해 단독 실행했고 **`Expected: 2 / Received: 4`로 실패**함을 재현했다(가드 없으면 세 클릭이 전부 `fetchProducts()` 호출 = 핸들러가 3회 다 돈다는 직접 증거). 복원은 md5(`ed88d963…`) 일치 확인. ③ `modules/shop-api`는 이번에도 **0건 수정**(최신 파일 `ShopIndexControllerTest.java`·`ShopStaticResourceConfig.java` 09-17 00:43 = SR-301 산출물, `ApiKeyAuthFilter.java` 00:19 그대로 — 사람이 "안 고치는 것"으로 지정한 401 이슈 미접촉). 라운드 변경분 전부 `modules/shop-web` 안(구현 모듈 제약 준수). ④ round1·2에서 사람이 확정한 항목 전수 실측 — `ProductCard` prop 명칭 `listPrice`/`discountRate`/`imageUrl` 그대로(SR-306 인계), 가정값 상수 `PRODUCT_GRID_DISPLAY_COUNT=8`·`RANKING_ITEM_COUNT=5`·`sortByPriceAscending` 그대로, 로그아웃 API 호출 그대로, 장바구니 `<span aria-disabled="true" title="준비 중">` 비활성화 그대로, `aria-haspopup`·401은 지시대로 미접촉 — **훼손 0건**. ⑤ `npm test`(타입체크+jest) **7스위트 53건 전부 통과**(기존 `PasswordResetPage`·`redirectTarget`·`refreshOnce`·`requestCodeOnce` 무변경 통과 포함), mutation 복원 후 재실행에서도 53/53 동일.
- 후속 TODO(게이트를 세우지 않음 — low, 사람이 이미 두 라운드에 걸쳐 범위 밖으로 결정했거나 이번 구조에서 파생된 관찰):
  1. (low, 검증) `npm run test-storybook` 여전히 미실행 — 신규 스토리 10건, 특히 `RankingSection.stories.tsx:가격탭전환`의 `play`. **화면 상태 기준선 재측정 전에 1회** 실행 필요(재작업 지시 4 = 사람이 범위 밖으로 명시, 추적 유지).
  2. (low, 접근성) `Gnb.tsx:39` 장바구니 `<span>`(role=generic)의 `aria-label`·`aria-hidden` 배지 문제(재작업 지시 3 = 사람 코멘트에서 다루지 않아 미반영) · `Gnb.tsx:27` `aria-haspopup` 불일치 — 둘 다 그대로.
  3. (low, 히스토리) `applyShopBootRedirect()`가 `window.location.hash = '#/shop'`(대입)이라 **히스토리 항목이 하나 쌓인다** — `/shop` 최초 진입 후 뒤로가기 한 번은 사이트를 벗어나지 않고 `/shop`(해시 없음) → 주문 목록이 뜬다(사람 지시가 명시한 "#/ 는 주문 목록" 규칙 자체와는 일치하므로 결함 아님). 항목을 남기지 않으려면 `location.replace('#/shop')`/`history.replaceState`. 사람 지시 원문이 "세팅한다"였으므로 이번 구현은 지시 준수.
  4. (low, 테스트 커버리지) 테스트는 `applyShopBootRedirect()`를 **직접 호출**해 부팅을 재현한다(모듈 재로딩이 React 인스턴스를 갈라 "Invalid hook call"을 내는 문제를 피한 합리적 선택 — 사람 지시가 `main.tsx` 방식을 허용). 대신 **`main.tsx`가 그 함수를 render 이전에 부른다는 배선 자체를 검증하는 테스트는 없다**(16줄·시각 확인 가능 범위). 뒤로가기 실물 동작도 jsdom 한계로 직접 테스트되지 않고 "해시가 이미 있는 상태" 재현으로 대체됐다.
  5. (low, 배포) 서버 마운트에 놓인 번들(`shop-api/target/classes/static/shop/index.html`·`shop-web/dist/index.html` 둘 다 00:33)은 **SR-302 구현 이전 산출물**이다 — 이번 진입 규칙은 재빌드·배치 뒤에야 실환경에서 확인된다(현재 배포본엔 round1 회귀도 없으므로 라이브 피해는 없음).
  6. (low, 추적) `/api/products` 401(`ApiKeyAuthFilter.isOpenRoute` 미등록, 서버 변경이라 범위 밖) — 사람이 별도 SR로 뺀 그대로 미해결.
- 재동기화 입력(STEP 5.5 몫 — 권고로 올리지 않는다):
  - `UIS-ORD-008`(예약, 본문 역생성 대상) — 진입 규칙이 이번 라운드로 **확정**됐다: "`/shop`·`/shop/` 서버 경로에 **해시 없이** 최초 진입할 때만 `#/shop`(쇼핑 홈)으로 1회 이동하고, 그 뒤 `#/`는 언제나 주문 목록". §진입/라우팅 절에 이 확정값과 `/shop#/`·`/shop#/shop` 구분을 적는다.
  - `INF-ORD-017`(SR-301) — 서버 응답 계약·앵커 줄 범위는 불변. `/shop`·`/shop/`가 index.html을 반환한 **뒤** 클라이언트가 어느 화면을 띄우는지(위 확정 규칙)를 "응답" 표 각주 또는 "비즈니스 규칙"에 한 줄 붙일지 5.5에서 판단한다.

## Test 기록 (test-agent)

- 테스트 완료 (2026-09-17, AIDD STEP 5 test-agent) — 53개 테스트 전부 통과
  - ShopHomePage 통합 테스트: 11/11 ✅ (로딩·빈목록·실패+재시도·비로그인/로그인·GNB·장바구니·최근본상품·품절·8개cap·로그아웃·연타가드)
  - recentlyViewedStorage 단위 테스트: 6/6 ✅ (빈배열·순서역순·dedupe·8개cap·빈sku·손상JSON)
  - Storybook: 8개 컴포넌트 타입체크 통과 ✅ (Gnb·배너·카테고리·상품카드·그리드·랭킹·최근본상품·푸터)
  - 회귀(기존 shop-web): 36/36 ✅ (App.test.tsx·PasswordResetPage.test.tsx 무변경 통과)
  - **총 53/53 ✅** (`npm test` 3.676s, 타입체크 포함)
  
  - AC 매핑: 변경 컨텍스트 문답 9개 전부 검증 (포함요소·회귀·하위호환·오류·빈값·이관·스토리·경로·스코프)
  - TC 문서 작성: `docs/07_테스트케이스/TC_v1.0.md`에 FUNC-order-008(쇼핑 홈) 신규 17개 TC 기술
  - TR 생성: `docs/08_테스트결과보고서/TR_v1.0.md`에 결과 집계 (통과율 100%, 버그 0건)
  - 품질 판정: ✅ **PASS** — AC 검증·신규 TC 신뢰·회귀 완전 무변경·데이터 갭 처리 정상·localStorage 격리 정상

---

## 재작업 지시
> round 2 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/regression] App.tsx:67이 '/' 라우트 element 자체를 <Navigate to="/shop" replace/>로 영구 치환해, /shop 마운트에서 주문 목록(#/)이 세션 내내 도달 불가가 된다. OrderDetailPage.tsx:26의 '← 주문 목록'(<Link to="/">) 클릭이 쇼핑 홈으로 튀고 replace라 뒤로가기도 주문 상세로 되돌아간다. LoginPage.tsx:29의 로그인 성공 후 기본 이동(resolveRedirectTarget(null)==='/')도 동일. 사람 지시는 '최초 진입 시' 리다이렉트였고, 확정문답 회귀 범위('기존 주문 목록·상세 화면과 그 동작 불변')에 걸린다. /shop 마운트는 이미 라이브(target/classes/static/shop/index.html 실재). → 리다이렉트를 진입 1회로 좁힌다 — 라우터 마운트 전(모듈 스코프/main.tsx)에서 isShopServerRoot() && !window.location.hash 일 때만 window.location.hash='#/shop'을 1회 수행하고, '/' 라우트는 <OrderListPage/>로 되돌린다. '/shop 마운트에서 주문 상세 → 주문 목록 복귀' 테스트 1건 추가.
2. [medium/regression] ShopHomePage.test.tsx:201-226의 in-flight 가드 테스트가 가드를 실제로 검증하지 못한다(inFlightRef를 지워도 통과). ProductGrid.tsx:22는 error가 최우선 분기라 첫 클릭에서 setError(null)+setLoading(true)가 반영되는 순간 [다시 시도] 버튼이 DOM에서 제거되고, RTL fireEvent는 act로 매 클릭마다 렌더를 flush하므로 2·3번째 클릭은 분리된 노드에 디스패치돼 React 루트 위임 리스너에 닿지 않는다. 사람이 완료 조건으로 명시한 '연타 시 요청 1회' 보증이 거짓이 된다(가드 코드 자체는 올바름). → 세 클릭을 한 act() 안에서 flush 없이 연달아 발사(act(() => { btn.click(); btn.click(); btn.click() }))해 버튼이 살아있는 동안 가드가 막는지 검증하거나, 재조회 경로를 직접 두 번 호출해 검증한다.
3. [low/spec] Gnb.tsx:39 장바구니를 <a>(role=link) → <span>(role=generic)으로 바꾸면서 aria-label='장바구니 N개'가 보조기술에 노출되지 않을 수 있고, 배지 숫자는 aria-hidden(43행)이라 수량 정보가 통째로 사라진다. 같은 라운드에 RankingSection의 aria-hidden을 접근성 이유로 걷어낸 것과 방향이 어긋난다. 테스트의 getByLabelText는 role 제약을 보지 않아 통과한다(거짓 보증). → role="img"를 부여하거나 시각적 숨김 텍스트로 장바구니 수량을 노출한다.
4. [low/regression] RankingSection.tsx·Gnb.tsx가 이번 라운드에 변경됐으나 짝 스토리(01:24~01:26)는 그대로이고 npm run test-storybook은 여전히 미실행이다(사람이 이번 범위 밖으로 명시 — 추적만). 신규 스토리 10건, 특히 RankingSection.stories.tsx:가격탭전환의 play 상호작용이 한 번도 렌더 검증되지 않았다. → 화면 상태 기준선 재측정 전에 test-storybook을 1회 실행해 콘솔 오류 없이 렌더되는지 확인한다.

사람 코멘트: QA FAIL round 2 재작업 지시(사람 결정):

[1. 라우트 되돌림 — 회귀 위반 수정]
App.tsx의 "/" 라우트 element를 <Navigate to="/shop"/>로 치환한 방식을 되돌린다. "/" 라우트는 다시 <OrderListPage/> 그대로(회귀 원상복구).
대신 앱 부팅 시(라우터 렌더 이전) 1회만: location.pathname이 /shop 이거나 /shop/로 시작하고, location.hash가 비어 있을 때만 location.hash를 "#/shop"으로 세팅한다. 컴포넌트/라우트 레벨의 상시 리다이렉트가 아니라 부팅 시 1회성 사이드이펙트로 구현할 것 — 그 뒤 사용자가 #/ 로 이동하면 반드시 주문 목록이 떠야 한다(뒤로가기·링크 클릭 등 모든 경로 포함).

[2. in-flight 가드 테스트 보강]
현재 테스트는 재시도 버튼이 클릭 직후 DOM에서 사라지는 구조 때문에 2·3번째 fireEvent가 빈 노드에 발사돼 가드를 검증하지 못한다(가드를 지워도 통과). 실패 응답을 지연(pending promise)시켜 버튼이 화면에 남아있는 상태에서 연속 클릭하거나, 핸들러를 직접 여러 번 호출해 fetch 호출 수가 1회인지 단언하도록 고친다. 가드 코드를 임시로 제거했을 때 이 테스트가 실제로 실패하는지 직접 확인하고, 그 확인 결과를 Dev 기록에 남길 것.

[회귀 테스트 추가 — 완료 조건]
- #/ 로 이동하면 주문 목록(OrderListPage)이 렌더된다(쇼핑 홈으로 튕기지 않음).
- 주문 상세 화면에서 "← 주문 목록" 클릭 시 정상 이동한다.
- /shop 서버 경로 + 빈 해시로 최초 진입할 때만 쇼핑 홈(#/shop)으로 간다.
- 기존 shop-web 테스트 스위트 전부(신규 포함) 통과.
