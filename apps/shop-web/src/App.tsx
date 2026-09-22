// linked_func: FUNC-member-004
// linked_func: FUNC-member-007 — 비밀번호 재설정 라우트 추가
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md · INF-MBR-006.md · INF-MBR-007.md
import { useEffect } from 'react'
import { HashRouter, Route, Routes } from 'react-router-dom'
import OrderListPage from './pages/OrderListPage'
import OrderDetailPage from './pages/OrderDetailPage'
import LoginPage from './pages/LoginPage'
import PasswordResetPage from './pages/PasswordResetPage'
import ShopHomePage from './pages/ShopHomePage'
import ProductListPage from './pages/ProductListPage'
import ProductDetailPage from './pages/ProductDetailPage'
import CartPage from './pages/CartPage'
import OrderPage from './pages/OrderPage'
import MyAddressesPage from './pages/MyAddressesPage'
import { ApiError, refreshSession } from './api'
import { refreshOnce } from './refreshOnce'
import { clearSession, loadSession, saveSession } from './session'

/**
 * 부팅 시 1회 — 저장된 refreshToken이 있으면 무음으로 세션을 롤링 갱신한다("30일 자동 로그인"의
 * 실제 갱신 지점, STORY 구현계획 — 이게 없으면 재발급 없이 그냥 30일 뒤 만료된다).
 *
 * round1 QA FAIL: React 19 StrictMode dev가 이 effect를 두 번 실행해 같은 refreshToken을 두 번
 * POST했다 — INF-MBR-005는 회전 API(성공 시 구 토큰 즉시 폐기)라 두 번째 요청이 401(MBR-4012)로
 * 튕기며 무음 로그아웃됐다. `alive` 언마운트 플래그는 state 쓰기만 막을 뿐 두 번째 네트워크 요청
 * 자체를 막지 못했다(재작업 지시 1). 그래서 실제 요청 발사는 `refreshOnce()`(모듈 스코프 in-flight
 * 가드)에 맡긴다 — 같은 토큰의 두 번째 호출은 첫 번째와 같은 Promise를 공유해 재요청하지 않는다.
 *
 * 응답 처리는 `alive` 가드 없이 **항상** `saveSession`한다(재작업 지시 2) — 응답이 돌아온 시점에
 * 서버는 이미 구 토큰을 폐기했으므로, 여기서 저장을 건너뛰면 localStorage엔 폐기된 구 토큰만 남아
 * 다음 부팅에서 401 → clearSession으로 확정 로그아웃된다. 여기엔 React state 쓰기가 없으니
 * 언마운트 가드가 애초에 필요 없다.
 *
 * 401(MBR-4012, 미존재/만료/폐기/탈퇴 4가지 사유가 완전히 동일하게 응답됨)일 때만 저장된 세션을
 * 조용히 지운다 — "자격 없음"과 "애초에 로그인한 적 없음"을 프론트가 구분하면 서버가 이미 막아 둔
 * 존재 오라클을 프론트가 재현하게 된다(STORY "폴백·우회 경로의 자격 판정" 절). 그 외 오류(예: 500·
 * 네트워크 장애)는 일시적일 수 있으므로 세션을 지우지 않고 다음 부팅 때 재시도한다.
 */
function useSilentRefresh() {
  useEffect(() => {
    const session = loadSession()
    if (!session?.refreshToken) return
    refreshOnce(session.refreshToken, refreshSession)
      .then(next => saveSession(next))
      .catch(e => { if (e instanceof ApiError && e.status === 401) clearSession() })
  }, [])
}

/**
 * SR-302 재작업(round 1 QA 권고 1, 사람 코멘트 1) — shop-api가 이 SPA를 서버 경로 `/shop` 밑에
 * 정적 서빙한다(INF-ORD-017). 그 진입점의 실제 URL은 `/shop#/`(해시 없음)이었고 `#/shop`으로 가는
 * 링크가 앱 어디에도 없어 아무도 쇼핑 홈에 도달하지 못했다. 서버 경로가 `/shop`(또는 `/shop/`)이고
 * 해시가 비어 있을 때만 이 판정이 참이다 — 판정 자체는 `ApiKeyAuthFilter`의 화이트리스트 조건
 * (INF-ORD-017 §비즈니스 규칙 "equals('/shop') || startsWith('/shop/')")과 동일하게 맞춘다.
 */
function isShopServerRoot(): boolean {
  const { pathname } = window.location
  return pathname === '/shop' || pathname.startsWith('/shop/')
}

/**
 * SR-302 재작업(round 2 QA FAIL 필수 수정 1, 사람 지시) — round 1은 "/" 라우트의 element 자체를
 * `isShopServerRoot() ? <Navigate to="/shop" replace/> : <OrderListPage/>`로 바꿔치기했다. `pathname`은
 * HashRouter 세션 동안 바뀌지 않으므로(위 주석 그대로) 이 삼항연산은 **렌더마다 재평가**돼, `/shop`
 * 마운트에서는 `#/`로 오는 모든 경로(주문 상세 "← 주문 목록", 로그인 후 기본 이동, 뒤로가기)가 매번
 * 다시 쇼핑 홈으로 튕겼다 — 주문 목록이 세션 내내 도달 불가가 되는 회귀였다(확정 답변 "기존 주문
 * 목록·상세 화면과 그 동작 불변"과 충돌).
 *
 * 그래서 리다이렉트를 **부팅 시 1회**로 좁힌다: 라우트/컴포넌트 레벨 코드가 아니라 라우터 렌더 이전에
 * 호출되는 별도 함수로 뺀다(사람 지시 원문 "라우터 마운트 전(모듈 스코프/main.tsx)에서"). 실제 부팅
 * 경로는 `main.tsx`가 `createRoot(...).render(<App/>)` 이전에 이 함수를 **정확히 한 번** 호출한다 —
 * React 컴포넌트 함수처럼 렌더마다·StrictMode 이중마운트마다 재실행되지 않는다. 해시가 이미 있으면
 * (사용자가 이미 어떤 경로로든 들어온 상태) 그 값을 그대로 존중해 손대지 않는다 — 오직 "/shop(또는
 * /shop/) + 해시 없음"(최초 진입)일 때만 쇼핑 홈으로 1회 보낸다(그 뒤 재호출해도 해시가 이미 있어
 * 자연히 아무 일도 하지 않는다 — 별도 가드 플래그가 필요 없는 멱등 조건). 이후 "/" 라우트는 다시
 * `<OrderListPage/>` 그대로이므로, 사용자가 `#/`로 이동하는 어떤 경로(링크·뒤로가기)도 이 함수를
 * 다시 거치지 않고 정상적으로 주문 목록을 띄운다.
 */
export function applyShopBootRedirect(): void {
  if (isShopServerRoot() && !window.location.hash) {
    window.location.hash = '#/shop'
  }
}

export default function App() {
  useSilentRefresh()
  return (
    <HashRouter>
      <Routes>
        <Route path="/" element={<OrderListPage />} />
        <Route path="/orders/:orderNo" element={<OrderDetailPage />} />
        <Route path="/login" element={<LoginPage />} />
        <Route path="/password-reset" element={<PasswordResetPage />} />
        {/* SR-302 — 쇼핑 홈(메인)은 새 경로로 추가한다("/"는 기존 주문 목록 그대로 유지, 로그인
            없이 접근 가능해야 한다는 확정 답변과 회귀 범위 보수적 선택). `main.tsx`가 부팅 시 1회
            호출하는 `applyShopBootRedirect()`가 "/shop" 서버 경로 최초 진입만 이 라우트로 보낸다. */}
        <Route path="/shop" element={<ShopHomePage />} />
        {/* SR-303 — 상품 목록(검색·필터). 카드 클릭은 상세 경로로 navigate만 하고(상세 화면은
            SR-304), 그 경로에 대응하는 <Route>는 여기 추가하지 않는다(확정 답변 scope_freeze). */}
        <Route path="/shop/products" element={<ProductListPage />} />
        {/* SR-304 — 상품 상세. `ProductListPage.handleSelect`의 기존 navigate('/shop/products/'+sku)
            호출은 이미 있었고(SR-303) 받는 라우트가 없어 도달 불가였다 — 이 SR에서 닫는다(사람 수정,
            STORY "파일" 절). */}
        <Route path="/shop/products/:sku" element={<ProductDetailPage />} />
        {/* SR-305 — 장바구니·주문서(주문완료는 같은 라우트 안에서 상태 전환, 별도 라우트 없음).
            Gnb 장바구니 아이콘이 이 SR에서 실제 링크로 바뀌어 이 두 화면의 유일하고 충분한
            진입점이 된다(STORY "파일" 절). */}
        <Route path="/shop/cart" element={<CartPage />} />
        <Route path="/shop/order" element={<OrderPage />} />
        {/* SR-235 — 마이페이지 배송지 관리. 마이페이지 허브 화면은 만들지 않는다(재승인 조건) — 이
            라우트 1개가 유일한 진입점(Gnb 링크 1줄 + 직접 URL). */}
        <Route path="/shop/mypage/addresses" element={<MyAddressesPage />} />
      </Routes>
    </HashRouter>
  )
}
