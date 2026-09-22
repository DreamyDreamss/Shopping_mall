---
uis-id: UIS-CMN-003
name: 앱 셸
domain: common
domain-code: CMN
layer: ui
route: (없음 — 화면 라우트 아님, shop-web 쇼핑 라우트(#/shop/**) 전체에 적용되는 공통 레이아웃 셸)
screens_role: 공통 레이아웃 셸(AppShell) — 화면 조립 지점
api_hints: []
access_control:
  - "좌측 퀵바/하단 탭바 '마이' 목적지: session 유무로 `#/shop/mypage/addresses` vs `#/login?redirect=...` 분기 — 각 페이지가 이미 판정한 `session` prop을 표시용으로 재사용할 뿐, AppShell/QuickBar/BottomTabBar 자신은 세션을 새로 조회·캐시하지 않는다(신규 자격 판정 없음)."
  - "로그인 리다이렉트 파라미터는 `location.pathname + location.search`를 `resolveRedirectTarget`(무변경, 오픈 리다이렉트 차단)에 그대로 위임한다."
anchors:
  - "modules/shop-web/src/features/shop/AppShell.tsx"
  - "modules/shop-web/src/features/shop/AppShell.stories.tsx"
  - "modules/shop-web/src/features/shop/GnbTabs.tsx"
  - "modules/shop-web/src/features/shop/QuickBar.tsx"
  - "modules/shop-web/src/features/shop/BottomTabBar.tsx"
  - "modules/shop-web/src/features/shop/RightRail.tsx"
  - "modules/shop-web/src/features/shop/QrPopup.tsx"
  - "modules/shop-web/src/features/shop/breakpoint.ts"
  - "modules/shop-web/src/features/shop/useBreakpoint.ts"
  - "modules/shop-web/src/features/shop/shopStatic.ts"
  - "modules/shop-web/src/features/shop/Gnb.tsx"
  - "modules/shop-web/src/pages/ProductListPage.tsx"
revision_history:
  - "2026-09-19 SR-311.1 구현 후 역생성(코드 기준, STEP 5.5, round1~4 재작업 반영)"
---

# UIS-CMN-003: 앱 셸

> SR-311.1로 신규 도입된 shop-web 공통 레이아웃 셸(AppShell) — [[UIS-CMN-002]](공통 컴포넌트)와 같은 급의
> "화면이 아닌 공통 조립 지점" 문서다. 쇼핑 라우트(`#/shop/**`) 8개 화면 전부를 감싸며, 화면 자신의
> 비즈니스 로직·상태·라우트는 바꾸지 않는다(리스킨은 SR-322·326 몫). SR-309 디자인 토큰·SR-310 공통
> 컴포넌트([[UIS-CMN-002]]의 `Tabs`·`BottomSheet`)를 재사용한다. 4라운드 QA(round1 CONCERNS → round2
> CONCERNS → round3 FAIL → round4 CONCERNS/납품 가능)를 거친 **최종 구현**만을 기술한다.

## 1. 개요

`AppShell.tsx`가 조립 지점이다. 8개 페이지(`ShopHomePage`·`ProductListPage`·`ProductDetailPage`·
`CartPage`·`OrderPage`·`MyAddressesPage`·`LoginPage`·`PasswordResetPage`)가 각자의 본문을
`<AppShell>{children}</AppShell>`로 감싼다. `App.tsx`의 라우트 목록·부팅 리다이렉트(`/shop` →
`#/shop`)는 이 SR에서 무변경이며, 기존 주문 목록 라우트(`#/`, `#/orders/*` — `OrderListPage`/
`OrderDetailPage`)는 셸 밖 그대로다.

배치 순서: `Gnb`(헤더, 무변경 재사용) → `GnbTabs`(가로 GNB 탭) → 브레이크포인트별 본문 영역 →
`ShopFooter`(무변경 재사용) → (모바일만) `BottomTabBar`.

## 2. 브레이크포인트 계약

경계 판정은 순수함수 `classifyBreakpoint(width)`(`breakpoint.ts`)가 정본이고, `useBreakpoint()`가
`window.innerWidth` + `resize` 리스너로 이를 구동한다(`window.matchMedia`는 의도적으로 쓰지 않는다 —
jsdom이 미구현이라 쓰면 8개 페이지 테스트 전부가 마운트 단계에서 하드 크래시한다).

| 폭 | 상태 | 본문 | 퀵바/레일 | 하단 탭바 |
|---|---|---|---|---|
| < 750px | `isMobile` | 전폭(`100%`) | 없음 | 있음(고정, 하단) |
| 750~1199px | `isTablet` | `var(--layout-content-width)`(750px) 고정, 가운데 | 없음 | 없음 |
| ≥ 1200px | `isDesktopRail` | `var(--layout-content-width)`(750px), 3열 그리드 가운데 칸 | 있음 | 없음 |

> **태블릿(750~1199px) 대역은 AC·확정문답 어디에도 규정이 없다** — "퀵바·레일·하단탭바 전부 없는
> 단일 컬럼"은 `breakpoint.ts` 주석에 적힌 구현 쪽의 보수적 선택이며, 이 코드 주석만이 유일한 정본이다.
> 후속 SR에서 재검토 시 이 사실을 전제로 삼을 것.

`AppShell`은 테스트/스토리 전용 `forceBreakpoint?: 'mobile' | 'tablet' | 'desktop'` prop을 받아
`useBreakpoint()`의 실측값 대신 상태를 결정적으로 고정할 수 있다(기본 `undefined` = 실측 사용).

## 3. 헤더(Gnb) — 무변경 재사용 + 검색 아이콘

`Gnb.tsx` 자체(로고·카테고리 버튼·검색 입력·장바구니 수량·로그인/마이 영역)는 SR-311 이전 그대로다.
이번 SR은 검색 입력 옆에 돋보기 아이콘 버튼(`aria-label="검색 화면으로 이동"`)만 추가했다. 기존
`aria-label="상품 검색"` 입력(직접 타이핑·Enter 제출)은 그대로 남아 있어 `ShopHomePage.test.tsx`/
`ProductListPage.test.tsx`의 기존 검색 회귀가 없다.

검색 아이콘 클릭(`handleSearchIconClick`) 동작(3분기, round4 최종):

1. **검색어가 있으면** — `onSearchSubmit()`을 호출한다(각 페이지가 이미 가진 제출 핸들러, Enter와
   완전히 같은 채널). 새 검색 구현이 아니다.
2. **검색어가 없고 이미 `/shop/products`(목록 화면) 안이면** — 이동할 곳이 없으므로 같은
   `onSearchSubmit()`을 호출해 페이지가 스스로 "검색어 없음"으로 정리하게 한다.
3. **검색어가 없고 목록 화면 밖이면** — `navigate('/shop/products', { state: { focusSearch: true } })`로
   이동만 한다. **`Gnb`는 클릭 시점에 스스로 포커스를 걸지 않는다.**

포커스 인계(round3 FAIL → round4 해소): 라우트 전환 시 현재 `Gnb` 인스턴스는 곧 언마운트되므로
클릭 시점에 `focus()`를 걸면 사라질 인스턴스에 거는 것이라 운영에서 동작하지 않는다(round3 QA 실측:
`document.activeElement`가 `BODY`). 대신 `Gnb`는 `autoFocusSearch?: boolean` prop을 받아 **마운트
이펙트**(`useEffect(..., [])`)에서만 자기 자신의 검색 입력에 포커스한다. 목적지 화면(`ProductListPage`)이
마운트 시 `location.state.focusSearch`를 읽어 `focusSearchOnMount`를 계산하고, 이를 `AppShell`→`Gnb`의
`autoFocusSearch`로 내려준 뒤 **같은 마운트에서** `navigate(location.pathname + location.search,
{ replace: true })`로 state를 즉시 소거한다(뒤로가기로 돌아와도 다시 포커스되지 않음, 같은 라우트로의
`replace`라 재마운트 없음). 이 왕복 계약을 갖는 곳은 현재 `ProductListPage` 하나뿐이다(검색 아이콘의
빈 검색어 이동 목적지가 그 화면 하나이므로).

## 4. GNB 탭 (GnbTabs)

`GnbTabs.tsx`는 SR-310 공통 `Tabs`([[UIS-CMN-002]] §6)를 그대로 쓴다(재구현 없음). 탭 목록은
`shopStatic.ts`의 `GNB_TAB_CONFIG`(설정값 한 곳) — 요구사항 배경의 9개 라벨(편성표·TV쇼핑·특가쇼·홈·
주말엔보너스·브랜드관·랭킹·VIP라운지·혜택/이벤트) 순서를 유지하되, **지금은 '홈'만 `implemented:true`
(`path:'/shop'`)이고 나머지 8개는 `path:null`로 GNB에서 완전히 빠진다**(도착 화면 없는 탭 숨김, AC).

- `activeKey`는 `useLocation().pathname`을 `visibleTabs[].path`와 매칭해 계산한다. **일치하는 탭이
  없으면(`/shop/cart`·`/login` 등) 존재하지 않는 key(`''`)를 넘겨 어떤 탭도 `aria-selected`되지
  않는다** — round1에서는 `?? visibleTabs[0]`로 임의로 '홈'이 강조됐던 결함이 round2에서 해소됐다.
- "탭 위 빨간 라벨"은 `Tabs`의 `TabItem.badge?: boolean` 계약(SR-311이 [[UIS-CMN-002]]에 새로 연
  확장, 탭 버튼 내부에 점을 그려 활성 여부·노출 탭 수와 무관하게 정렬 — round1의 별도 오버레이 방식은
  탭이 2개 이상일 때 어긋나 round2에서 전삭제되고 이 방식으로 대체됐다)로 그린다. **현재 `GNB_TAB_CONFIG`의
  어느 탭도 `badge:true`가 아니라 실제로 화면에 점이 뜨는 경로는 없다** — 설정값만 켜면 바로 표시된다.
- `onChange`는 `implemented:true`(`path` 있는) 탭만 `navigate(path)`한다.

## 5. 좌측 퀵바 / 하단 탭바 (QuickBar / BottomTabBar)

두 컴포넌트는 `shopStatic.ts`의 `QUICK_NAV_ITEMS`(설정값 한 곳, 홈·ON AIR·카테고리·마이)를 공유한다
— 하단 탭바가 퀵바와 같은 설정을 쓰는 것은 AC가 명시하지 않아 구현이 추론한 것이고, 사람이 구현 계획
확인 단계에서 승인했다(STORY "사람 확인 사항" 1).

| key | 라벨 | `implemented` | 비고 |
|---|---|---|---|
| home | 홈 | true | `#/shop` |
| onAir | ON AIR | **false** | 방송중 기능(SR-319) 전까지 숨김 |
| category | 카테고리 | **false** | 서랍(SR-237) 전까지 숨김 |
| my | 마이 | true | session 유무로 목적지 분기(아래) |

- **QuickBar**(≥1200px, `features/shop/QuickBar.tsx`): 세로 배치. `QUICK_NAV_ITEMS.filter(implemented)`
  로 홈·마이만 노출 + QR·TOP은 목록과 무관하게 항상 노출(4항목 실질 표시: 홈·마이·QR·TOP).
  - **마이**: `session`이 있으면 `#/shop/mypage/addresses`, 없으면
    `#/login?redirect=` + `encodeURIComponent(location.pathname + location.search)`.
  - **QR**: 내부 `useState`로 `QrPopup` 오픈(§7).
  - **TOP**: `window.scrollTo({ top: 0, behavior: 'smooth' })`.
- **BottomTabBar**(<750px, `features/shop/BottomTabBar.tsx`): 가로 고정(`position:fixed; bottom:0`),
  `QUICK_NAV_ITEMS.filter(implemented)`로 홈·마이만 노출(QR·TOP은 AC 원문에 없어 하단 탭바에 없다).
  '마이' 분기 로직은 QuickBar와 동일.
- 리다이렉트 파라미터는 **`location.pathname + location.search`**를 싣는다(round1: `pathname`만 실어
  `?keyword=가방` 같은 쿼리가 로그인 후 유실되던 결함, round2에서 해소).

## 6. 우측 레일 (RightRail)

≥1200px 전용(`features/shop/RightRail.tsx`). 두 카드 모두 "값 없으면 숨김"이고, **둘 다 없으면
컴포넌트 자체가 `null`을 반환**한다(그 경우에도 AppShell의 레일 "칸" 자체는 §8 그리드 계약에 따라
DOM에 남는다 — 카드만 없을 뿐 트랙은 유지).

- **최근 본 상품 카드**: `recentlyViewedStorage.loadRecentlyViewedSkus()`(localStorage)로 sku 목록을
  얻고, 자체 `useEffect`+`inFlightRef`(StrictMode 이중 마운트 가드)로 기존 `fetchProducts()`(신규
  엔드포인트 아님, `api.ts`의 기존 export)를 1회 호출해 sku→상품명을 붙인다. 최대 3개, 그라데이션
  (`--gradient-accent`) 헤더 카드. **sku가 0개면 `fetchProducts()` 자체를 부르지 않는다**(round1:
  카드가 어차피 숨는데도 GET이 먼저 나가던 결함 — `LoginPage`/`PasswordResetPage`처럼 원래 상품 API를
  안 부르던 화면에서도 불필요한 호출이 생겼었다. round2에서 `if (loadRecentlyViewedSkus().length ===
  0) return` 가드로 해소). 화면 본문이 같은 API를 자기 목적으로 이미 호출하는 것과는 별개 호출이라
  화면당 GET이 1회 더 생기는 것 자체는 범위 밖으로 남겨졌다(§10).
- **보유 쿠폰/주문 내역 수 카드**: `entitlementCounts?: { coupons: number; orders: number }` prop —
  undefined거나 둘 다 0이면 렌더하지 않는다. **이번 SR은 이 prop을 아무도 채우지 않는다**(쿠폰
  데이터는 SR-276 확정 제외, 주문 수는 기존 주문 도메인 몫) — "값 없으면 숨김" AC는 항상-숨김으로
  충족되고, 호출부가 값을 채우는 순간(SR-276 이후) 그대로 뜨는 계약만 열려 있다.

## 7. QR 팝업 (QrPopup)

`features/shop/QrPopup.tsx` — SR-310 공통 `BottomSheet`(무변경 재사용, `ariaLabel="QR 코드"`)로 감싸고
`qrcode.react`의 `<QRCodeSVG>`로 **로컬에서** SVG를 그린다(외부 QR 생성 API 없음 → 네트워크 요청 자체가
없다 — 실패 사례집 SR-306 #1 대조로 의도적으로 고른 방식). `url`은 열릴 때 `window.location.href`를
그대로 담는다(HashRouter라 현재 해시 경로까지 포함, AC "현재 페이지 URL").

## 8. ≥1200px 3열 그리드 — 본문 정렬 계약

```css
display: grid;
grid-template-columns: minmax(0, 1fr) var(--layout-content-width) minmax(0, 1fr);
```

좌측 칸(`appshell-quickbar-slot`, `QuickBar`, `justify-content:flex-end`) · 가운데 칸(`<main>`, 750px) ·
우측 칸(`appshell-rail-slot`, `RightRail`, `justify-content:flex-start`) — **레일·퀵바 칸은 `isDesktopRail`
에만 조건부이지 카드 콘텐츠 유무에 조건부가 아니다**, 즉 최근 본 상품이 0개라 `RightRail`이 `null`을
반환해도 그 칸(래퍼 `<div>`)은 DOM에 그대로 남는다.

`minmax(0, 1fr)`(round4 최종, round3 FAIL의 필수수정)이 핵심이다 — 단순 `1fr`은 CSS에서
`minmax(auto, 1fr)`이라 트랙이 칸 안 콘텐츠의 min-content(레일 220px·`flexShrink:0`, 퀵바 64px) 아래로
줄지 않는다. 1200~1310px처럼 좌우 여유가 440px 미만인 대역에서 레일 칸만 220px 하한에 눌려 좌우가
비대칭이 되고, 레일이 카드 없이 `null`이면(min-content 0) 다시 대칭이 되는 식으로 **본문이 사용자의
localStorage 상태(최근 본 상품 유무)에 따라 최대 51px(1200px 기준, 1240px 31px·1280px 11px·1366px
이상 0px) 움직이는 결함**이 round2~round3에 걸쳐 있었다. `minmax(0, 1fr)`은 그 min-content 하한을
0으로 강제해 **좌우 트랙이 콘텐츠 유무·뷰포트 폭과 무관하게 항상 동일하게 계산**되고, 가운데 750px
본문은 컨테이너 정중앙에 고정된다.

**round4 QA가 1200/1240/1280/1310/1366/1920px × 레일 있음/없음 12조합을 playwright 실측으로 검증**:
`<main>` 중심이 전 폭에서 캔버스 중심과 오차 0px, 레일 카드 유무 간 차이도 전 폭 0px. `AppShell.
stories.tsx`의 `최근본상품_3개`/`최근본상품_0개` 두 스토리 모두 `play`(`assertMainCentered`, 허용오차
2px)로 이를 고정한다.

> **트레이드오프(결함 아님, 알려진 제약 §10)**: `minmax(0, 1fr)`가 트랙 하한을 0으로 만든 대가로,
> 1200~1237px 대역에서 레일(220px 고정폭)이 칸보다 좁아지면 **레일이 칸 밖으로 흘러나가 가로 스크롤이
> 생긴다**(본문 좌표 자체는 흔들리지 않음). round3 QA 수정안이 제시하고 사람이 그대로 승인한 절충이다.

## 9. 푸터

`ShopFooter`(무변경 재사용) — 사업자 정보 자리만 마련되어 있고 실제 내용은 SR-329 몫. 모바일에서는
`BottomTabBar`(고정 하단)가 푸터를 가리지 않도록, 하단 여백(`paddingBottom: isMobile ? 56 : 0`)을
`<main>`이 아니라 셸 최외곽 컨테이너(`<div style="minHeight:100vh">`)에 둔다(round1: `<main>`에만
있어 푸터 마지막 줄이 하단 탭바에 가려지던 결함, round2에서 해소).

## 10. 알려진 제약 (결함 은폐 금지)

- **[SR-322 추적] 1200~1237px에서 우측 레일이 뷰포트 밖으로 최대 19px 넘친다**(가로 스크롤 발생) —
  §8의 `minmax(0,1fr)` 절충이 낳은 트레이드오프. 본문(`<main>`) 좌표 자체는 이 구간에서도 흔들리지
  않는다(0px 실측). round3 QA 수정안·사람 지시가 이미 승인한 결과라 round4 QA는 결함으로 올리지
  않았다. SR-322(기존 화면 리스킨)에서 함께 재검토 예정.
- **`ProductListPage`의 `location.state.focusSearch` 소거(`replace`) 로직에 테스트 가드가 없다** —
  그 분기를 무력화해도 41 suites / 312 tests가 그대로 통과한다(round4 QA 실측). 동작 자체는 QA가
  임시 테스트로 정상 확인했으나(소거 후 `focusSearch`가 false로 남음), 조용한 되돌림을 스위트가 못
  잡는 커버리지 구멍으로 남아 있다.
- **태블릿(750~1199px) 대역은 AC·확정문답에 규정이 없다** — "퀵바·레일·하단탭바 없는 단일 컬럼"은
  구현(`breakpoint.ts` 주석)만이 유일한 정본이다(§2).
- **GNB 탭이 2개 이상 열리면(SR-237/SR-319 이후) "선택 없음" 상태의 좌우 스와이프가 첫 탭이 아니라
  두 번째 탭으로 간다**(`GnbTabs.tsx`가 불일치 시 `activeKey=''`를 넘기는데 `Tabs.tsx`는 기준 인덱스를
  `Math.max(0, findIndex)`로 0으로 잡는다) — 지금은 노출 탭이 '홈' 1개뿐이라 무해하며, 사람이 "이번
  범위 대상 아님"으로 명시해 후속 TODO로 보류됐다.
- **목록 화면(`/shop/products`) 안에서 빈 검색어로 검색 아이콘을 누르면 URL의 `?keyword=`가 stale로
  남는다** — `ProductListPage`가 마운트 시 1회만 URL을 읽는 기존 설계(SR-303)와 얽힌 것으로 round3
  신규 결함은 아니며, 사람이 "이번 범위 대상 아님"으로 명시해 후속 TODO로 보류됐다.
- **§8의 `assertMainCentered` 실좌표 검증(storybook `play`)은 `project.env`에
  `STORYBOOK_TEST_CMD`가 설정돼야 AIDD 축E에서 실행된다** — 없으면 축E는 콘솔 오류만 보는 내장
  렌더러로 폴백해 `play`를 돌리지 않는다. 이 SR의 QA round4 이후 사람 지시로
  `modules/shop-web/scripts/test-storybook-ci.cjs` + `project.env`의 `STORYBOOK_TEST_CMD` 설정이
  추가되어 이후 라운드부터는 축E가 이 검증을 자동으로 포함한다.

## 11. 데이터 흐름·보안

새 인증·인가 경로 없음. 유일한 자격 분기는 §5의 '마이' 목적지이고 각 페이지가 이미 `loadSession()`으로
판정한 `session` prop을 표시용으로 재사용할 뿐이다. 신규 데이터 호출은 §6의 `fetchProducts()`(기존
공개 상품 목록 엔드포인트, `api.ts` 경유 — 규칙 `web-fetch-only-in-api` 준수, 부품 직접 `fetch`·
`console.log` 0건)뿐이고 백엔드·DB 변경은 없다. QR(§7)에 담기는 `window.location.href`에 토큰류 없음.

## 12. 적용 화면 (8개, 렌더 트리만 교체)

`ShopHomePage`·`ProductListPage`·`ProductDetailPage`·`CartPage`·`OrderPage`·`MyAddressesPage`·
`LoginPage`·`PasswordResetPage` — 각 페이지의 `<Gnb/>` + 폭 제약 래퍼(`maxWidth:1040/920/720/640` 등)를
`<AppShell>{기존 본문}</AppShell>`로 교체했다(비즈니스 로직·상태·effect는 무변경). `LoginPage`·
`PasswordResetPage`는 이 SR 이전에는 헤더 자체가 없던 화면이라 `session`/`cartItemCount`/`searchValue`
상태를 새로 추가했다. **손대지 않은 것**: `App.tsx`(라우트 목록·부팅 리다이렉트 전부 무변경),
`OrderListPage`/`OrderDetailPage`(`#/`, `#/orders/*` — 제외 대상), `Gnb.tsx`의 기존 부분·`ShopFooter`·
`RecentlyViewed`(전부 무변경 재사용).

## 13. 범위 밖 (후속 SR)

카테고리 서랍 내용(SR-237) · 검색 동작 고도화(자동완성 등, SR-239) · 쿠폰 수 데이터 연동(SR-276) ·
헤더 '기프티쇼' 토글 · 기존 주문 목록(`#/`, `#/orders/*`) 셸 적용 · 백엔드 변경 · 푸터 실제 내용
(SR-329) · 기존 화면 내부 리스킨(SR-322·326) · §10의 1200~1237px 레일 오버플로 완화(SR-322) ·
`RightRail`↔페이지 간 `fetchProducts` 중복 호출 최적화.

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-19 | SR-311 | #1 | 앱 셸(AppShell) 신규 — shop-web 쇼핑 라우트 전체에 공통 레이아웃(퀵바/GNB탭/레일/하단탭바) 적용 | shop-web@b1c7c6f5850d, shop-web@0acc411 |
