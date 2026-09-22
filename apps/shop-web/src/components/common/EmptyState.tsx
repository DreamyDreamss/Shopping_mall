// SR-310 — 빈 상태 규격(bench/screenshot-10 빈 장바구니 구성): 가운데 라인 아이콘 + 한 줄 안내 +
// 테두리 버튼(Button variant=secondary 재사용) + 하단 추천 레일 슬롯. 실제 추천 데이터는 범위 밖.
import type { ReactNode } from 'react'
import { Button } from './Button'

export interface EmptyStateProps {
  message: string
  actionLabel: string
  onAction: () => void
  /** bench/screenshot-10 하단 추천 레일 자리 — 실제 추천 데이터는 이 SR 범위 밖. */
  railSlot?: ReactNode
}

export function EmptyState({ message, actionLabel, onAction, railSlot }: EmptyStateProps) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 'var(--space-4)', padding: 'var(--space-8) var(--space-4)' }}>
      <svg
        aria-hidden="true" width="48" height="48" viewBox="0 0 24 24" fill="none"
        stroke="currentColor" strokeWidth="1.5"
        style={{ color: 'var(--color-text-tertiary)' }}
      >
        <path
          d="M3 7h18M5 7l1.5 12a2 2 0 0 0 2 1.8h7a2 2 0 0 0 2-1.8L19 7M9 11v5M15 11v5"
          strokeLinecap="round" strokeLinejoin="round"
        />
      </svg>
      <p style={{ margin: 0, fontSize: 'var(--text-base)', color: 'var(--color-text-secondary)' }}>{message}</p>
      <Button variant="secondary" onClick={onAction}>{actionLabel}</Button>
      {railSlot && <div style={{ width: '100%', marginTop: 'var(--space-6)' }}>{railSlot}</div>}
    </div>
  )
}
