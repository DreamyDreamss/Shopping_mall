// SR-302 — 쇼핑 홈(메인) 상단 GNB.
// SR-311 round2 재작업 지시 1 — 검색 입력 옆에 돋보기 아이콘 버튼을 추가한다.
// SR-311 round3 재작업 지시 1 — round2는 searchValue를 보지 않고 무조건 `navigate('/shop/products')`만
// 해서, 검색어를 친 뒤 아이콘을 누르면(Enter와 달리) 검색어가 버려졌다. 이제 검색어가 있으면 그 페이지의
// 기존 `onSearchSubmit`(Enter 제출과 동일 채널)을 그대로 호출한다 — SR-239가 다루는 신규 검색 구현이
// 아니라 이미 있는 제출 경로 재사용이다. 검색어가 없으면 목록 화면으로 이동하는데, **이미 그 목록
// 화면(`/shop/products`) 안에 있으면** `navigate()`로 쿼리만 지워도 `ProductListPage`는 최초 마운트 때만
// URL을 읽어(재초기화 없음) 화면에 남아 있는 이전 검색 결과와 새(빈) URL이 어긋난다 — 그래서 그 경우엔
// `navigate` 대신 같은 `onSearchSubmit`을 호출해(searchValue가 이미 비어 있으므로 그 페이지의 제출
// 핸들러가 알아서 "검색어 없음" 상태로 정리한다) URL·표시 상태를 일치시킨다(이 분기는 round4에서도
// 손대지 않는다 — 사람 코멘트가 "URL keyword stale 문제"는 이번 라운드 대상 아님으로 명시). 기존
// `aria-label="상품 검색"` 입력은 그대로 두어(ShopHomePage/ProductListPage 테스트가 그 라벨로
// 타이핑·제출) 회귀가 없다.
// SR-311 round4 재작업 지시 2(round3 QA FAIL 필수수정 2) — round3까지는 이동 직후 `searchInputRef.
// current?.focus()`를 여기서 직접 불렀는데, `App.tsx`처럼 라우트마다 별도 페이지가 자기 `AppShell`→
// `Gnb`를 렌더하는 구조에서는 그 navigate 호출로 **이 Gnb 인스턴스 자신이 곧 언마운트**돼(도착 화면은
// 새 Gnb 인스턴스를 새로 마운트한다) 클릭 시점의 focus()는 사라질 인스턴스에 걸려 운영에서 전혀
// 동작하지 않았다(같은 라우트에 남는 <Routes> 밖 테스트에서만 통과하는 거짓 보증, 사례집 SR-302 #1·
// SR-306 #2). 포커스는 Gnb가 클릭 시점에 스스로 판단하지 않고, "도착 화면"이 `autoFocusSearch` prop으로
// 내려줄 때만(마운트 이펙트) 이 인스턴스 자신의 입력에 건다 — 이 인스턴스가 바로 그 도착 화면의
// Gnb이므로 언마운트 경합이 없다(`ProductListPage.tsx` 참조).
import { useEffect, useRef } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { SessionResult } from '../../types'

export interface GnbProps {
  session: SessionResult | null
  cartItemCount: number
  searchValue: string
  onSearchChange: (value: string) => void
  onSearchSubmit: () => void
  onLogout: () => void
  /** round4 재작업 지시 2 — 검색 아이콘으로 막 도착한 화면이 `location.state.focusSearch`를 보고
   * 내려주는 신호(기본 undefined = 포커스 없음). Gnb는 클릭 시점에 스스로 focus를 걸지 않는다 —
   * 오직 이 prop이 true인 "마운트 시점"에만 자기 자신의 검색 입력에 포커스한다. */
  autoFocusSearch?: boolean
}

const bar: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 16, padding: '10px 18px',
  borderBottom: '1px solid #eef0f2', fontSize: 13, fontFamily: 'system-ui, sans-serif',
}

/**
 * 로고·카테고리 메뉴 트리거(정적 — 실제 메뉴는 범위 밖)·검색 입력(제어 컴포넌트, 제출은 이 SR에서
 * no-op)·장바구니 아이콘+수량 배지·로그인/마이 영역. fetch 없음(규칙 `web-fetch-only-in-api`) —
 * 잠금·수량·회원 정보는 전부 부모(`ShopHomePage`)가 이미 판정한 값을 그대로 받아 표시만 한다.
 */
export function Gnb({
  session, cartItemCount, searchValue, onSearchChange, onSearchSubmit, onLogout, autoFocusSearch,
}: GnbProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const searchInputRef = useRef<HTMLInputElement>(null)

  // round4 재작업 지시 2 — 이 Gnb 인스턴스가 "도착 화면"으로 막 마운트됐을 때만(autoFocusSearch=true)
  // 자기 자신의 검색 입력에 포커스한다. 마운트 이펙트(`[]` 의존성)라 이후 부모가 state를 소거해 prop이
  // false로 바뀌어도 다시 실행되지 않는다 — 한 번 도착했을 때만 포커스한다.
  useEffect(() => {
    if (autoFocusSearch) searchInputRef.current?.focus()
    // eslint: 마운트 시 1회만 — autoFocusSearch가 이후 false로 바뀌는 것(도착 화면이 state 소거)에
    // 반응해 다시 실행할 필요가 없다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  // round3 재작업 지시 1 — 검색어가 있으면 Enter와 완전히 같은 채널(onSearchSubmit)로 보낸다.
  const handleSearchIconClick = () => {
    if (searchValue.trim()) {
      onSearchSubmit()
      return
    }
    // 검색어 없음 + 이미 목록 화면 안: 이동할 곳이 없다 — 같은 onSearchSubmit을 호출해(searchValue가
    // 비어 있으므로) 그 페이지가 스스로 "검색어 없음" 상태로 정리하게 한다(round4 대상 아님, 위 파일
    // 상단 주석 참조).
    if (location.pathname === '/shop/products') {
      onSearchSubmit()
      return
    }
    // round4 재작업 지시 2 — 검색어 없음 + 목록 화면 밖: 포커스는 여기서 걸지 않는다. state로
    // "도착하면 포커스하라"는 신호만 실어 보내고, 그 신호를 도착 화면(`ProductListPage`)이 마운트 시
    // 읽어 자신의 `AppShell`→`Gnb`에 `autoFocusSearch`를 내려준다(위 useEffect가 실제 포커스를 건다) —
    // 그래야 지금 이 Gnb 인스턴스가 언마운트돼도(라우트 전환) 포커스가 유실되지 않는다.
    navigate('/shop/products', { state: { focusSearch: true } })
  }

  return (
    <header style={bar}>
      <a href="#/shop" style={{ fontWeight: 800, fontSize: 16, color: '#0b4ea2', textDecoration: 'none' }}>SL Shop</a>
      <button type="button" aria-haspopup="true"
              style={{ border: 0, background: 'none', fontSize: 13, color: '#333', cursor: 'pointer' }}>
        카테고리
      </button>
      <form onSubmit={e => { e.preventDefault(); onSearchSubmit() }}
            style={{ flex: 1, maxWidth: 360, display: 'flex', alignItems: 'center', gap: 6 }}>
        <button type="button" onClick={handleSearchIconClick} aria-label="검색 화면으로 이동"
                style={{ border: 0, background: 'none', fontSize: 14, color: '#666', cursor: 'pointer', padding: '0 2px', lineHeight: 1 }}>
          <span aria-hidden="true">🔍</span>
        </button>
        <input type="search" aria-label="상품 검색" value={searchValue} placeholder="상품을 검색해 보세요"
               ref={searchInputRef} onChange={e => onSearchChange(e.target.value)}
               style={{ flex: 1, minWidth: 0, border: '1px solid #d5d8dd', borderRadius: 5, padding: '6px 10px', fontSize: 13 }} />
      </form>
      {/* SR-305 — 장바구니 화면(`/shop/cart`)이 생겨 실제 링크로 바꾼다(다른 링크와 동일한
          `<a href="#/...">` 관례). `aria-label` 문자열은 그대로 유지한다 — `ShopHomePage.test.tsx`/
          `ProductDetailPage.test.tsx`가 `getByLabelText('장바구니 N개')`로만 찾으므로 회귀 없음
          (실측 확인, STORY "파일" 절). */}
      <a href="#/shop/cart" aria-label={`장바구니 ${cartItemCount}개`}
         style={{ position: 'relative', color: '#333', fontSize: 18, textDecoration: 'none' }}>
        🛒
        {cartItemCount > 0 && (
          <span aria-hidden="true"
                style={{ position: 'absolute', top: -6, right: -10, background: '#0b4ea2', color: '#fff',
                         borderRadius: 8, fontSize: 10, fontWeight: 700, padding: '1px 5px' }}>
            {cartItemCount}
          </span>
        )}
      </a>
      {session ? (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span>{session.memberName}님</span>
          {/* SR-235 — 마이페이지 배송지 관리 진입 링크(로그인 상태에서만 노출, AC). 마이페이지 허브
              화면은 없어 이 화면으로 직접 연결한다(재승인 조건). */}
          <a href="#/shop/mypage/addresses" style={{ color: '#333', fontSize: 12, textDecoration: 'none' }}>
            마이페이지 › 배송지 관리
          </a>
          <button type="button" onClick={onLogout}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', fontSize: 12, padding: '4px 10px' }}>
            로그아웃
          </button>
        </div>
      ) : (
        <a href="#/login" style={{ color: '#0b4ea2', textDecoration: 'none' }}>로그인</a>
      )}
    </header>
  )
}
