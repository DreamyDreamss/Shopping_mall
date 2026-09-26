// SR-351 — 상품평 탭 최상위 컨테이너. 순수 props만 받는다(자체 fetch 없음, 규칙 web-fetch-only-in-api
// — "스마트" 로직 없이 훅(`useProductReviews.ts`)이 만든 상태만 그린다). 로딩/조회실패/빈목록/목록/
// 작성영역/삭제확인모달을 이 파일에서 배치한다(STORY "파일" 절).
import { ReviewRatingSummary } from './ReviewRatingSummary'
import { ReviewSortControl } from './ReviewSortControl'
import { ReviewListItem } from './ReviewListItem'
import { ReviewListErrorNotice } from './ReviewListErrorNotice'
import { ReviewDeleteConfirm } from './ReviewDeleteConfirm'
import { ReviewWriteArea, type EligibleOrder, type OrderLookupStatus, type WriteFieldErrors, type WriteStatus } from './ReviewWriteArea'
import { ProductPagination } from './ProductPagination'
import type { ReviewSort, ReviewListResult } from '../../types'

export type ListStatus = 'idle' | 'loading' | 'ready' | 'error'
export type DeleteStatus = 'idle' | 'pending'

export interface ProductReviewsSectionProps {
  loggedIn: boolean

  listStatus: ListStatus
  listResult: ReviewListResult | null
  onRetryList: () => void
  isOwn: (reviewId: number) => boolean
  sort: ReviewSort
  onSortChange: (value: ReviewSort) => void
  page: number
  onPageChange: (value: number) => void

  eligibleOrders: EligibleOrder[]
  orderLookupStatus: OrderLookupStatus
  onRetryOrderLookup: () => void
  orderNo: string
  onOrderNoChange: (value: string) => void
  rating: number
  onRatingChange: (value: number) => void
  content: string
  onContentChange: (value: string) => void
  fieldErrors: WriteFieldErrors
  writeStatus: WriteStatus
  bannerMessage?: string
  onSubmit: () => void

  deleteTarget: number | null
  deleteStatus: DeleteStatus
  deleteErrorMessage: string | null
  onDeleteRequest: (reviewId: number) => void
  onDeleteCancel: () => void
  onDeleteConfirm: () => void
}

const alertStyle: React.CSSProperties = {
  border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b', borderRadius: 6,
  padding: '10px 14px', fontSize: 12.5, margin: '10px 0',
}

export function ProductReviewsSection(props: ProductReviewsSectionProps) {
  const {
    loggedIn, listStatus, listResult, onRetryList, isOwn, sort, onSortChange, page, onPageChange,
    eligibleOrders, orderLookupStatus, onRetryOrderLookup,
    orderNo, onOrderNoChange, rating, onRatingChange, content, onContentChange,
    fieldErrors, writeStatus, bannerMessage, onSubmit,
    deleteTarget, deleteStatus, deleteErrorMessage, onDeleteRequest, onDeleteCancel, onDeleteConfirm,
  } = props

  const totalPages = listResult ? Math.max(1, Math.ceil(listResult.totalCount / Math.max(1, listResult.size))) : 1

  return (
    <section aria-label="상품평">
      {/* 최초 로드(아직 listResult가 없음)일 때만 전체를 스켈레톤/오류로 대체한다. 정렬·페이지 변경으로
          다시 로딩/오류에 들어가도 직전 목록·정렬 컨트롤은 그대로 보여준다(사람 수정 결정 3번의 정렬
          변경 경합 시나리오 — 사용자가 응답을 기다리는 동안에도 다음 정렬을 계속 누를 수 있어야 한다).
          최신 데이터가 도착하면 아래 listResult가 그대로 최신값으로 교체된다(요청 세대 가드는 훅 몫). */}
      {listStatus === 'loading' && !listResult ? (
        <div aria-busy="true" style={{ padding: '24px 0', color: '#999', fontSize: 13 }}>불러오는 중…</div>
      ) : listStatus === 'error' && !listResult ? (
        <ReviewListErrorNotice onRetry={onRetryList} />
      ) : listResult ? (
        <>
          <ReviewRatingSummary avgRating={listResult.avgRating} reviewCount={listResult.reviewCount}
                                distribution={listResult.distribution} />
          {/* 재작업(round2, QA CONCERNS 권고 1) — 최초 로드 이후의 재조회 실패(정렬 변경·페이지 이동·
              작성/삭제 후 재조회)는 직전 목록을 그대로 둔 채 이 인라인 경고+[다시 시도]로 알린다.
              정렬 버튼의 활성 표시(`sort` prop)는 이 실패와 무관하게 훅이 이미 마지막 성공값으로
              되돌려 놓는다(useProductReviews.ts의 applied/target 분리). */}
          {listStatus === 'error' && <ReviewListErrorNotice onRetry={onRetryList} />}
          {deleteErrorMessage && <div role="alert" style={alertStyle}>{deleteErrorMessage}</div>}
          {listResult.items.length === 0 ? (
            <div role="status" style={{ color: '#666', fontSize: 13, padding: '20px 0' }}>아직 상품평이 없습니다</div>
          ) : (
            <>
              <div style={{ margin: '14px 0' }}>
                <ReviewSortControl value={sort} onChange={onSortChange} />
              </div>
              <ul style={{ margin: 0, padding: 0 }}>
                {listResult.items.map(item => (
                  <ReviewListItem key={item.reviewId} review={item} isOwn={isOwn(item.reviewId)}
                                  deleting={deleteStatus === 'pending' && deleteTarget === item.reviewId}
                                  onDeleteRequest={onDeleteRequest} />
                ))}
              </ul>
              <div style={{ marginTop: 14 }}>
                <ProductPagination page={page} totalPages={totalPages}
                                    onPrev={() => onPageChange(Math.max(1, page - 1))}
                                    onNext={() => onPageChange(Math.min(totalPages, page + 1))} />
              </div>
            </>
          )}
        </>
      ) : null}

      <div style={{ marginTop: 24 }}>
        <ReviewWriteArea loggedIn={loggedIn} eligibleOrders={eligibleOrders} orderLookupStatus={orderLookupStatus}
                          onRetryOrderLookup={onRetryOrderLookup} orderNo={orderNo} onOrderNoChange={onOrderNoChange}
                          rating={rating} onRatingChange={onRatingChange} content={content} onContentChange={onContentChange}
                          fieldErrors={fieldErrors} status={writeStatus} bannerMessage={bannerMessage} onSubmit={onSubmit} />
      </div>

      <ReviewDeleteConfirm open={deleteTarget != null} onCancel={onDeleteCancel} onConfirm={onDeleteConfirm}
                            confirming={deleteStatus === 'pending'} />
    </section>
  )
}
