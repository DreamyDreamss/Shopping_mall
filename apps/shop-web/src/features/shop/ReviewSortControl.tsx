// SR-351 — 상품평 정렬 3종(최신순/별점높은순/별점낮은순) 버튼 그룹. fetch 없음(규칙
// web-fetch-only-in-api) — controlled(부모가 정렬 상태와 재조회를 소유).
import type { ReviewSort } from '../../types'

export interface ReviewSortControlProps {
  value: ReviewSort
  onChange: (value: ReviewSort) => void
  disabled?: boolean
}

const OPTIONS: { value: ReviewSort; label: string }[] = [
  { value: 'latest', label: '최신순' },
  { value: 'ratingDesc', label: '별점높은순' },
  { value: 'ratingAsc', label: '별점낮은순' },
]

export function ReviewSortControl({ value, onChange, disabled }: ReviewSortControlProps) {
  return (
    <div role="group" aria-label="상품평 정렬" style={{ display: 'flex', gap: 6 }}>
      {OPTIONS.map(o => {
        const active = value === o.value
        return (
          <button key={o.value} type="button" disabled={disabled} aria-pressed={active}
                  onClick={() => onChange(o.value)}
                  style={{
                    border: '1px solid ' + (active ? '#0b4ea2' : '#d5d8dd'), borderRadius: 5,
                    background: active ? '#0b4ea2' : '#fff', color: active ? '#fff' : '#333',
                    fontSize: 12.5, padding: '5px 10px', cursor: disabled ? 'not-allowed' : 'pointer',
                  }}>
            {o.label}
          </button>
        )
      })}
    </div>
  )
}
