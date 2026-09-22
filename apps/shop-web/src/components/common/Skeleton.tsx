// SR-310 — 공통 Skeleton(카드·리스트). 기존 `ProductDetailSkeleton`(상품 상세 전용 고정형)은
// 그대로 둔다 — 이번엔 신규 병존만, 마이그레이션은 후속 SR.
export interface SkeletonProps {
  variant: 'card' | 'list'
}

const block: React.CSSProperties = { background: 'var(--color-surface-1)', borderRadius: 'var(--radius-sm)' }

export function Skeleton({ variant }: SkeletonProps) {
  if (variant === 'card') {
    return (
      <div role="status" aria-label="불러오는 중" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-2)', width: 160 }}>
        <div aria-hidden="true" style={{ ...block, aspectRatio: '1 / 1' }} />
        <div aria-hidden="true" style={{ ...block, height: 14, width: '80%' }} />
        <div aria-hidden="true" style={{ ...block, height: 14, width: '50%' }} />
      </div>
    )
  }

  return (
    <div role="status" aria-label="불러오는 중" style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
      {[0, 1, 2].map(i => (
        <div key={i} aria-hidden="true" style={{ ...block, height: 56, width: '100%' }} />
      ))}
    </div>
  )
}
