// SR-303 — 상품 목록 페이지 이동(이전/다음 + "페이지 N / M"). 서버 페이지네이션 API 없이 전부
// 클라이언트 계산(확정 답변 scope_freeze). fetch 없음(규칙 `web-fetch-only-in-api`).
export interface ProductPaginationProps {
  /** 1-base 현재 페이지. */
  page: number
  totalPages: number
  onPrev: () => void
  onNext: () => void
}

const navButton: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#333',
  fontSize: 13, padding: '6px 14px', cursor: 'pointer',
}

const navButtonDisabled: React.CSSProperties = {
  ...navButton, color: '#aaa', cursor: 'not-allowed', background: '#f5f6f7',
}

export function ProductPagination({ page, totalPages, onPrev, onNext }: ProductPaginationProps) {
  // 결과가 없어 totalPages가 0이어도(빈 목록) "1 / 1"로 안전하게 표시하고 양쪽 다 비활성화한다.
  const safeTotalPages = Math.max(1, totalPages)
  const isFirstPage = page <= 1
  const isLastPage = page >= safeTotalPages

  return (
    <nav aria-label="상품 목록 페이지 네비게이션" style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      <button type="button" onClick={onPrev} disabled={isFirstPage} style={isFirstPage ? navButtonDisabled : navButton}>
        이전
      </button>
      <span style={{ fontSize: 13, color: '#555' }}>페이지 {page} / {safeTotalPages}</span>
      <button type="button" onClick={onNext} disabled={isLastPage} style={isLastPage ? navButtonDisabled : navButton}>
        다음
      </button>
    </nav>
  )
}
