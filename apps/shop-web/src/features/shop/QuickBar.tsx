// SR-311 — 앱 셸 좌측 세로 퀵바(≥1200px 전용, AC). 홈·마이·QR·TOP은 항상 노출, ON AIR·카테고리는
// `QUICK_NAV_ITEMS`의 `implemented:false`로 숨긴다(SR-319·SR-237 전까지, 확정문답 scr_entry).
import { useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import type { SessionResult } from '../../types'
import { QUICK_NAV_ITEMS } from './shopStatic'
import { QrPopup } from './QrPopup'

export interface QuickBarProps {
  session: SessionResult | null
}

const ICONS: Record<string, string> = { home: '🏠', onAir: '📺', category: '📋', my: '👤' }

const railStyle: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 'var(--space-5)',
  width: 64, flexShrink: 0, paddingTop: 'var(--space-6)',
}

const itemStyle: React.CSSProperties = {
  display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, width: '100%',
  border: 0, background: 'none', cursor: 'pointer', color: 'var(--color-text-secondary)',
  fontSize: 'var(--text-2xs)', padding: 0, fontFamily: 'inherit',
}

export function QuickBar({ session }: QuickBarProps) {
  const navigate = useNavigate()
  const location = useLocation()
  const [qrOpen, setQrOpen] = useState(false)

  // '마이'만 세션 유무로 목적지가 갈린다(`Gnb`의 로그인 링크와 동일한 session prop을 그대로 재사용
  // — 신규 자격 판정 로직을 만들지 않는다, STORY "순서·보안" 절). 그 외 노출 항목('홈')은 고정 경로.
  // round2 재작업 지시 7 — pathname만 실으면 쿼리스트링(예: `?keyword=가방`)이 로그인 후 유실된다 —
  // pathname+search를 함께 실어 기존 `resolveRedirectTarget` 규약(같은 오리진 상대경로만 허용)을 그대로
  // 확장한다.
  const handleNav = (key: string) => {
    if (key === 'home') { navigate('/shop'); return }
    if (key === 'my') {
      navigate(session ? '/shop/mypage/addresses' : '/login?redirect=' + encodeURIComponent(location.pathname + location.search))
    }
  }

  return (
    <nav aria-label="빠른 메뉴" style={railStyle}>
      {QUICK_NAV_ITEMS.filter(item => item.implemented).map(item => (
        <button key={item.key} type="button" style={itemStyle} onClick={() => handleNav(item.key)}>
          <span aria-hidden="true" style={{ fontSize: 20 }}>{ICONS[item.key]}</span>
          {item.label}
        </button>
      ))}
      <button type="button" style={itemStyle} onClick={() => setQrOpen(true)}>
        <span aria-hidden="true" style={{ fontSize: 20 }}>⬜</span>
        QR
      </button>
      <button type="button" style={itemStyle} onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
        <span aria-hidden="true" style={{ fontSize: 20 }}>⬆</span>
        TOP
      </button>
      <QrPopup open={qrOpen} onClose={() => setQrOpen(false)} />
    </nav>
  )
}
