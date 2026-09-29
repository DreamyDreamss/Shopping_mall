// SR-361 — 상품 상세 화면 최근 상품평 미리보기(최근 3건). `ProductReviewsSection`/`ReviewListItem`과
// 같은 데이터(`useProductReviews`의 `listStatus`/`listResult`)를 그대로 받아 앞 3건만 슬라이스해
// 그린다(자체 fetch 없음, 규칙 web-fetch-only-in-api). 별점은 `StarRatingDisplay`(format="integer")를
// 재사용하고, 조회 실패 상태는 기존 `ReviewListErrorNotice`의 고정 카피를 그대로 재사용한다(SR-235 #1
// 사례 대조 — 자체 오류 문구를 새로 만들지 않는다). 목록 컨테이너는 `AddressList`/`WishList`와 동일하게
// `role="list"`/`role="listitem"`을 명시한 div로 짠다(`<ul>`+`list-style:none`은 Chrome이 암묵적
// list/listitem 역할을 제거해 스토리 play function의 개수 단언이 흔들릴 수 있다).
import { ReviewListErrorNotice } from './ReviewListErrorNotice'
import { StarRatingDisplay } from './StarRatingDisplay'
import type { ListStatus } from './ProductReviewsSection'
import type { ReviewListResult } from '../../types'

const PREVIEW_COUNT = 3
const CONTENT_MAX_LENGTH = 60

/** 본문 60자 초과 시 앞 60자 + "…"로 자른다(AC "본문 앞 60자, 넘으면 …"). 정확히 60자는 그대로 둔다. */
export function truncateReviewContent(content: string, maxLength: number = CONTENT_MAX_LENGTH): string {
  return content.length > maxLength ? content.slice(0, maxLength) + '…' : content
}

export interface ProductReviewPreviewProps {
  listStatus: ListStatus
  listResult: ReviewListResult | null
  onRetry: () => void
  onViewAll: () => void
}

const itemStyle: React.CSSProperties = {
  borderBottom: '1px solid #eef0f2', padding: '12px 0', display: 'flex', flexDirection: 'column', gap: 6,
}

const viewAllButtonStyle: React.CSSProperties = {
  marginTop: 14, border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff',
  color: '#333', fontSize: 12.5, padding: '6px 14px', cursor: 'pointer',
}

export function ProductReviewPreview({ listStatus, listResult, onRetry, onViewAll }: ProductReviewPreviewProps) {
  return (
    <section aria-label="최근 상품평">
      <h2 style={{ fontSize: 16, fontWeight: 700, margin: '0 0 14px' }}>최근 상품평</h2>
      {listStatus === 'error' && !listResult ? (
        <ReviewListErrorNotice onRetry={onRetry} />
      ) : listResult ? (
        <>
          {listResult.items.length === 0 ? (
            // `role="status"`를 달지 않는다 — `ProductReviewsSection`의 동명 빈 상태(탭 진입 시에만
            // 렌더)와 달리 이 미리보기는 화면 진입 즉시 상시 렌더돼, `role="status"`를 달면 이 화면의
            // 토스트(`ToastStack`, 동일 role) 단언(`getByRole('status')`/`queryByRole('status')`)과
            // 항상 충돌한다(실측 확인 — 재입고 알림 토스트 테스트가 깨짐).
            <div style={{ color: '#666', fontSize: 13, padding: '20px 0' }}>아직 상품평이 없습니다</div>
          ) : (
            <div role="list" aria-label="최근 상품평 목록">
              {listResult.items.slice(0, PREVIEW_COUNT).map(item => (
                <div role="listitem" key={item.reviewId} style={itemStyle}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                    <StarRatingDisplay value={item.rating} format="integer" />
                    <span style={{ fontSize: 12.5, color: '#666' }}>{item.authorName}</span>
                    <span style={{ fontSize: 11.5, color: '#999' }}>{item.createdAt.slice(0, 10)}</span>
                  </div>
                  <p style={{ margin: 0, fontSize: 13, color: '#333', whiteSpace: 'pre-wrap' }}>
                    {truncateReviewContent(item.content)}
                  </p>
                </div>
              ))}
            </div>
          )}
          <button type="button" onClick={onViewAll} style={viewAllButtonStyle}>리뷰 전체 보기</button>
        </>
      ) : (
        <div aria-busy="true" style={{ padding: '24px 0', color: '#999', fontSize: 13 }}>불러오는 중…</div>
      )}
    </section>
  )
}
