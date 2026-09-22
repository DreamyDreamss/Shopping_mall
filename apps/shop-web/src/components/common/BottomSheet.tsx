// SR-310 — 구매 시트 용도의 BottomSheet. `ZipcodeSearchModal`처럼 controlled(오버레이+패널)
// 프레젠테이션이되 포커스 트랩·Esc·복귀는 `useFocusTrap`(BottomSheet·PopupCarousel 공유)을 쓴다.
//
// round1 QA 재작업(low) — aria-label="구매 시트"가 하드코딩돼 범용 공통 컴포넌트인데 다른 용도로
// 쓸 때 접근성 이름을 바꿀 수 없었다. ariaLabel? prop을 열되 기존 소비처가 없어 기본값은 그대로
// '구매 시트'로 유지한다(회귀 없음).
import type { ReactNode } from 'react'
import { useFocusTrap } from './useFocusTrap'

export interface BottomSheetProps {
  open: boolean
  onClose: () => void
  children: ReactNode
  /** 접근성 이름(aria-label). 범용 공통 컴포넌트라 용도별로 바꿀 수 있게 연다. 기본은 '구매 시트'. */
  ariaLabel?: string
}

const overlayStyle: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'var(--color-dim)',
  display: 'flex', alignItems: 'flex-end', justifyContent: 'center', zIndex: 'var(--z-modal)',
}

const panelStyle: React.CSSProperties = {
  background: '#fff',
  borderTopLeftRadius: 'var(--radius-lg)',
  borderTopRightRadius: 'var(--radius-lg)',
  width: '100%',
  maxWidth: 480,
  maxHeight: '80vh',
  overflowY: 'auto',
  padding: 'var(--space-5)',
  boxShadow: 'var(--shadow-lg)',
}

export function BottomSheet({ open, onClose, children, ariaLabel = '구매 시트' }: BottomSheetProps) {
  const { containerRef } = useFocusTrap(open, onClose)
  if (!open) return null

  return (
    <div style={overlayStyle} onClick={onClose}>
      <div
        ref={containerRef}
        role="dialog"
        aria-modal="true"
        aria-label={ariaLabel}
        style={panelStyle}
        onClick={e => e.stopPropagation()}
      >
        <button
          type="button"
          aria-label="닫기"
          onClick={onClose}
          style={{ border: 0, background: 'none', fontSize: 18, cursor: 'pointer', float: 'right' }}
        >
          ✕
        </button>
        <div style={{ clear: 'both' }}>{children}</div>
      </div>
    </div>
  )
}
