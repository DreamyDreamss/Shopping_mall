// SR-351 — 상품평 목록 조회 실패 안내. 이 화면 전용 문구라 `ProductNotFoundNotice`(상품 조회 실패
// 전용 문구·분기)를 재사용하지 않는다(실측 확인, 재사용 가능한 기존 부품 없음). 스타일은 그 부품과
// 톤을 통일한다. fetch 없음(규칙 web-fetch-only-in-api) — 재시도는 부모가 한다.
export interface ReviewListErrorNoticeProps {
  onRetry: () => void
}

const boxStyle: React.CSSProperties = {
  border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b', borderRadius: 6,
  padding: '20px 18px', display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start',
}

const buttonStyle: React.CSSProperties = {
  border: '1px solid #912d2b', borderRadius: 5, background: '#fff', color: '#912d2b',
  fontSize: 12.5, padding: '5px 12px', cursor: 'pointer',
}

export function ReviewListErrorNotice({ onRetry }: ReviewListErrorNoticeProps) {
  return (
    <div role="alert" style={boxStyle}>
      <span>상품평을 불러오지 못했습니다</span>
      <button type="button" onClick={onRetry} style={buttonStyle}>다시 시도</button>
    </div>
  )
}
