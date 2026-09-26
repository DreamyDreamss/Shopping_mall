// SR-351 — 별점 1~5점 분포 막대(5줄). fetch 없음(규칙 web-fetch-only-in-api) — 순수 표시 부품.
export interface RatingDistributionBarProps {
  distribution: Record<'1' | '2' | '3' | '4' | '5', number>
  reviewCount: number
}

const STARS = ['5', '4', '3', '2', '1'] as const

export function RatingDistributionBar({ distribution, reviewCount }: RatingDistributionBarProps) {
  // reviewCount===0이어도 분모 0 나눗셈을 막는다(전부 폭 0으로 렌더).
  const denom = Math.max(1, reviewCount)

  return (
    <div aria-label="별점 분포" style={{ display: 'flex', flexDirection: 'column', gap: 4, minWidth: 170 }}>
      {STARS.map(star => {
        const count = distribution[star] ?? 0
        const pct = Math.round((count / denom) * 100)
        return (
          <div key={star} style={{ display: 'flex', alignItems: 'center', gap: 6, fontSize: 11.5, color: '#666' }}>
            <span style={{ width: 22 }}>{star}점</span>
            <div style={{ flex: 1, height: 6, background: '#eef0f2', borderRadius: 3, overflow: 'hidden' }}>
              <div style={{ width: `${pct}%`, height: '100%', background: '#e0a100' }} />
            </div>
            <span style={{ width: 26, textAlign: 'right' }}>{count}</span>
          </div>
        )
      })}
    </div>
  )
}
