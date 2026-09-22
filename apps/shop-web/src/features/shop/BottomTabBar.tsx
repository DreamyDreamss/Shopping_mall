// SR-311 — 앱 셸 하단 탭바(<750px 전용, AC). `QuickBar`와 같은 `QUICK_NAV_ITEMS` 설정을 써서
// ON AIR·카테고리를 같은 이유로 숨긴다(사람 확인 사항 1 — 승인). QR·TOP은 AC 원문에 없어 넣지 않는다.
import { useLocation, useNavigate } from 'react-router-dom'
import type { SessionResult } from '../../types'
import { QUICK_NAV_ITEMS } from './shopStatic'

export interface BottomTabBarProps {
  session: SessionResult | null
}

const ICONS: Record<string, string> = { home: '🏠', onAir: '📺', category: '📋', my: '👤' }

const barStyle: React.CSSProperties = {
  position: 'fixed', bottom: 0, left: 0, right: 0, display: 'flex',
  borderTop: '1px solid var(--color-surface-1)', background: '#fff', zIndex: 'var(--z-sticky)',
}

const itemStyle: React.CSSProperties = {
  flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2,
  border: 0, background: 'none', cursor: 'pointer', color: 'var(--color-text-secondary)',
  fontSize: 'var(--text-2xs)', padding: 'var(--space-2) 0', fontFamily: 'inherit',
}

export function BottomTabBar({ session }: BottomTabBarProps) {
  const navigate = useNavigate()
  const location = useLocation()

  // `QuickBar.handleNav`와 동일 판정(같은 session prop 재사용, 신규 자격 판정 없음). round2 재작업
  // 지시 7 — pathname+search를 함께 실어 쿼리스트링 유실을 막는다(QuickBar와 동일).
  const handleNav = (key: string) => {
    if (key === 'home') { navigate('/shop'); return }
    if (key === 'my') {
      navigate(session ? '/shop/mypage/addresses' : '/login?redirect=' + encodeURIComponent(location.pathname + location.search))
    }
  }

  return (
    <nav aria-label="하단 메뉴" style={barStyle}>
      {QUICK_NAV_ITEMS.filter(item => item.implemented).map(item => (
        <button key={item.key} type="button" style={itemStyle} onClick={() => handleNav(item.key)}>
          <span aria-hidden="true" style={{ fontSize: 18 }}>{ICONS[item.key]}</span>
          {item.label}
        </button>
      ))}
    </nav>
  )
}
