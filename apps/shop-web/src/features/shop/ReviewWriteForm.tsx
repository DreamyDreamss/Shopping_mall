// SR-351 — 상품평 작성 폼. fetch 없음(규칙 web-fetch-only-in-api) — 제출은 부모(`useProductReviews`)가
// api.ts를 통해 한다. 렌더 분기 우선순위(STORY "파일" 절, 사람 수정 결정 1번): 주문 조회 실패 >
// 구매 이력 없음(403과 같은 의미의 고정 문구, 새 문구를 만들지 않는다) > 정상 폼. status==='error'
// (제출 409 등)에도 orderNo/rating/content 값은 그대로 유지한다(사람 수정 결정 2번 — 부모가 값을
// 초기화하지 않고 그대로 내려준다, 이 부품은 받은 값을 그대로 보여줄 뿐이다).
import { StarRatingInput } from './StarRatingInput'

export type WriteStatus = 'idle' | 'pending' | 'success' | 'error'
export type OrderLookupStatus = 'idle' | 'loading' | 'ready' | 'error'

export interface EligibleOrder {
  orderNo: string
  orderedAt: string
}

export interface WriteFieldErrors {
  orderNo?: string
  rating?: string
  content?: string
}

export interface ReviewWriteFormProps {
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
  status: WriteStatus
  /** 403/409/일반오류 고정 문구. */
  bannerMessage?: string
  onSubmit: () => void
}

const noticeBoxStyle: React.CSSProperties = {
  border: '1px solid #eef0f2', background: '#f8f9fa', color: '#555', borderRadius: 6,
  padding: '16px 18px', fontSize: 13, display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start',
}

const retryButtonStyle: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#333',
  fontSize: 12.5, padding: '5px 12px', cursor: 'pointer',
}

const fieldErrorStyle: React.CSSProperties = { color: '#912d2b', fontSize: 12, margin: 0 }

const selectStyle: React.CSSProperties = {
  display: 'block', marginTop: 4, border: '1px solid #d5d8dd', borderRadius: 5, padding: '7px 8px',
  fontSize: 13, width: '100%', maxWidth: 320,
}

const textareaStyle: React.CSSProperties = {
  display: 'block', marginTop: 4, border: '1px solid #d5d8dd', borderRadius: 5, padding: '8px 10px',
  fontSize: 13, width: '100%', maxWidth: 480, resize: 'vertical',
}

const submitButtonStyle: React.CSSProperties = {
  border: 0, borderRadius: 5, background: '#0b4ea2', color: '#fff', fontSize: 13.5, fontWeight: 700,
  padding: '9px 18px', cursor: 'pointer', alignSelf: 'flex-start',
}

export function ReviewWriteForm({
  eligibleOrders, orderLookupStatus, onRetryOrderLookup, orderNo, onOrderNoChange, rating, onRatingChange,
  content, onContentChange, fieldErrors, status, bannerMessage, onSubmit,
}: ReviewWriteFormProps) {
  const pending = status === 'pending'

  if (orderLookupStatus === 'error') {
    return (
      <div role="alert" style={noticeBoxStyle}>
        <span>주문 정보를 불러오지 못했습니다</span>
        <button type="button" onClick={onRetryOrderLookup} style={retryButtonStyle}>다시 시도</button>
      </div>
    )
  }

  if (eligibleOrders.length === 0 && orderLookupStatus === 'ready') {
    return <div role="note" style={noticeBoxStyle}>이 상품을 구매한 회원만 작성할 수 있습니다</div>
  }

  return (
    <form aria-label="상품평 작성" onSubmit={e => { e.preventDefault(); onSubmit() }}
          style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
      <label style={{ fontSize: 12.5, color: '#555' }}>
        구매하신 주문
        <select value={orderNo} onChange={e => onOrderNoChange(e.target.value)} disabled={pending} style={selectStyle}>
          <option value="">선택해 주세요</option>
          {eligibleOrders.map(o => (
            <option key={o.orderNo} value={o.orderNo}>{o.orderNo} ({o.orderedAt.slice(0, 10)})</option>
          ))}
        </select>
      </label>
      {fieldErrors.orderNo && <p role="alert" style={fieldErrorStyle}>{fieldErrors.orderNo}</p>}

      <StarRatingInput value={rating} onChange={onRatingChange} disabled={pending} />
      {fieldErrors.rating && <p role="alert" style={fieldErrorStyle}>{fieldErrors.rating}</p>}

      <label style={{ fontSize: 12.5, color: '#555' }}>
        상품평
        <textarea value={content} onChange={e => onContentChange(e.target.value)} disabled={pending}
                  maxLength={1000} rows={4} style={textareaStyle} />
      </label>
      <div style={{ fontSize: 11.5, color: '#999' }}>{content.length} / 1000자</div>
      {fieldErrors.content && <p role="alert" style={fieldErrorStyle}>{fieldErrors.content}</p>}

      {bannerMessage && <div role="alert" style={{ ...fieldErrorStyle, fontSize: 12.5 }}>{bannerMessage}</div>}

      <button type="submit" disabled={pending} style={submitButtonStyle}>
        {pending ? '등록 중…' : '상품평 등록'}
      </button>
    </form>
  )
}
