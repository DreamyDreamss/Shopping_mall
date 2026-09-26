// SR-351 — 상품평 요약(평균 별점 + 분포 + 건수). StarRatingDisplay·RatingDistributionBar 조립.
// fetch 없음(규칙 web-fetch-only-in-api).
import { StarRatingDisplay } from './StarRatingDisplay'
import { RatingDistributionBar } from './RatingDistributionBar'

export interface ReviewRatingSummaryProps {
  avgRating: number | null
  reviewCount: number
  distribution: Record<'1' | '2' | '3' | '4' | '5', number>
}

export function ReviewRatingSummary({ avgRating, reviewCount, distribution }: ReviewRatingSummaryProps) {
  return (
    <section aria-label="상품평 요약" style={{ display: 'flex', gap: 24, alignItems: 'flex-start', padding: '10px 0' }}>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, minWidth: 76 }}>
        <StarRatingDisplay value={avgRating} />
        <span style={{ fontSize: 12, color: '#666' }}>{reviewCount}건</span>
      </div>
      <RatingDistributionBar distribution={distribution} reviewCount={reviewCount} />
    </section>
  )
}
