// SR-351 — 작성 폼용 1~5 인터랙티브 별점 선택. fetch 없음(규칙 web-fetch-only-in-api) — 선택값은
// controlled(부모가 소유)로만 바뀐다.
export interface StarRatingInputProps {
  /** 0이면 "미선택". */
  value: number
  onChange: (value: number) => void
  disabled?: boolean
}

export function StarRatingInput({ value, onChange, disabled }: StarRatingInputProps) {
  return (
    <div role="group" aria-label="별점" style={{ display: 'flex', gap: 4 }}>
      {[1, 2, 3, 4, 5].map(n => (
        <button key={n} type="button" aria-label={`별점 ${n}점`} aria-pressed={value >= n} disabled={disabled}
                onClick={() => onChange(n)}
                style={{
                  border: 0, background: 'none', fontSize: 22, lineHeight: 1, padding: 0,
                  color: value >= n ? '#e0a100' : '#d5d8dd', cursor: disabled ? 'not-allowed' : 'pointer',
                }}>
          ★
        </button>
      ))}
    </div>
  )
}
