// SR-351 — 읽기전용 별점 표시. 평균 별점(상품평 요약)과 리뷰 1건의 개별 rating(항상 정수) 둘 다에
// 재사용한다. fetch 없음(규칙 web-fetch-only-in-api).
// 재작업(round2, QA CONCERNS 권고 5) — 평균과 개별 rating은 표기 자릿수가 다르다: 평균은 소수 1자리
// ("4.3"), 개별 리뷰의 rating은 항상 정수라 그대로("4") 보여야 한다. 이전에는 개별 rating에도 평균용
// toFixed(1) 포맷을 그대로 재사용해 "4.0"으로 잘못 표시됐다 — `format` prop으로 두 용도를 가른다.
export interface StarRatingDisplayProps {
  /** null이면 아직 리뷰가 없다는 뜻 — 별 없이 '-'만 표시한다(AC "평균 자리 -, 0.0 표기 금지"). */
  value: number | null
  /** 'average'(기본) — 소수 1자리(toFixed(1)). 'integer' — 정수 그대로(리뷰 1건의 개별 rating 전용). */
  format?: 'average' | 'integer'
}

const STAR_FULL = '★'
const STAR_EMPTY = '☆'

export function StarRatingDisplay({ value, format = 'average' }: StarRatingDisplayProps) {
  if (value === null) {
    return <span aria-label="평균 별점 없음" style={{ color: '#999', fontSize: 14 }}>-</span>
  }
  const rounded = Math.min(5, Math.max(0, Math.round(value)))
  const stars = STAR_FULL.repeat(rounded) + STAR_EMPTY.repeat(5 - rounded)
  const displayValue = format === 'integer' ? String(rounded) : value.toFixed(1)

  return (
    <span aria-label={`별점 ${displayValue}점`} style={{ color: '#e0a100', fontSize: 14, fontWeight: 700 }}>
      {stars}
      <span style={{ color: '#333', fontWeight: 400, marginLeft: 4 }}>{displayValue}</span>
    </span>
  )
}
