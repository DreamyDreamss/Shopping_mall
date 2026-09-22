// SR-304 — 상품 상세 조회 실패 안내(상품없음/조회실패). `ProductListGrid`의 오류 카드와 톤을
// 통일한다(테두리·배경·글자색 재사용).
// 순서·보안(STORY 절) — 두 사유를 하나의 default 분기로 합치지 않고 명시적으로 분기한다(SR-234 r1
// 사례 대조: 미정의 코드를 default로 뭉쳐 잘못된 전이가 생긴 것과 같은 함정을 피한다).
export interface ProductNotFoundNoticeProps {
  reason: 'notFound' | 'fetchError'
  /** notFound 상태의 [목록으로]. */
  onBackToList?: () => void
  /** fetchError 상태의 [다시 시도]. */
  onRetry?: () => void
}

const boxStyle: React.CSSProperties = {
  border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b', borderRadius: 6,
  padding: '20px 18px', display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start',
}

const buttonStyle: React.CSSProperties = {
  border: '1px solid #912d2b', borderRadius: 5, background: '#fff', color: '#912d2b',
  fontSize: 12.5, padding: '5px 12px', cursor: 'pointer',
}

export function ProductNotFoundNotice({ reason, onBackToList, onRetry }: ProductNotFoundNoticeProps) {
  if (reason === 'notFound') {
    return (
      <div role="alert" style={boxStyle}>
        <span>상품을 찾을 수 없습니다</span>
        {onBackToList && (
          <button type="button" onClick={onBackToList} style={buttonStyle}>목록으로</button>
        )}
      </div>
    )
  }
  return (
    <div role="alert" style={boxStyle}>
      <span>불러오지 못했습니다</span>
      {onRetry && (
        <button type="button" onClick={onRetry} style={buttonStyle}>다시 시도</button>
      )}
    </div>
  )
}
