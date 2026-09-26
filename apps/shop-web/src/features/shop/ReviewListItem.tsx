// SR-351 — 리뷰 목록 1건(마스킹된 authorName·별점·본문·작성일 + 본인 리뷰면 삭제 버튼). authorName은
// 서버가 이미 마스킹한 값(원본 이름은 API로 나가지 않는다, STORY "가리는 형식과 원본 보존 방침" 절).
// createdAt은 날짜만 표시(이 저장소에 날짜 포맷 유틸이 없어 새로 만들지 않고 슬라이스만, 실측 확인).
// fetch 없음(규칙 web-fetch-only-in-api) — 삭제 확인/호출은 부모(`ProductReviewsSection`)가 한다.
import { StarRatingDisplay } from './StarRatingDisplay'
import type { ReviewItem } from '../../types'

export interface ReviewListItemProps {
  review: ReviewItem
  /** 이 브라우저에서 만든 리뷰인지(`myReviewsStorage`, 실제 소유권 재검증은 서버가 매 요청마다 한다). */
  isOwn: boolean
  /** 이 리뷰가 삭제 진행 중이면 삭제 버튼을 잠근다. */
  deleting?: boolean
  onDeleteRequest: (reviewId: number) => void
}

export function ReviewListItem({ review, isOwn, deleting, onDeleteRequest }: ReviewListItemProps) {
  return (
    <li style={{ listStyle: 'none', borderBottom: '1px solid #eef0f2', padding: '12px 0', display: 'flex', flexDirection: 'column', gap: 6 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
        {/* 개별 rating은 항상 정수 — 평균용 소수 1자리 포맷을 재사용하지 않는다(재작업 round2). */}
        <StarRatingDisplay value={review.rating} format="integer" />
        <span style={{ fontSize: 12.5, color: '#666' }}>{review.authorName}</span>
        <span style={{ fontSize: 11.5, color: '#999' }}>{review.createdAt.slice(0, 10)}</span>
      </div>
      <p style={{ margin: 0, fontSize: 13, color: '#333', whiteSpace: 'pre-wrap' }}>{review.content}</p>
      {isOwn && (
        <button type="button" onClick={() => onDeleteRequest(review.reviewId)} disabled={deleting}
                style={{
                  alignSelf: 'flex-start', border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff',
                  color: '#912d2b', fontSize: 12, padding: '4px 10px', cursor: deleting ? 'not-allowed' : 'pointer',
                }}>
          {deleting ? '삭제 중…' : '삭제'}
        </button>
      )}
    </li>
  )
}
