// SR-304 — 상품 상세 최초 로딩 스켈레톤("로딩" 상태를 스토리로 남기라는 확정 답변 충족 — 페이지는
// 스토리 대상이 아니라 별도 부품으로 분리했다).
export function ProductDetailSkeleton() {
  return (
    <div role="status" aria-label="상품 상세 불러오는 중" style={{ display: 'flex', gap: 32, padding: '20px 0', flexWrap: 'wrap' }}>
      <div aria-hidden="true" style={{ flex: '0 0 320px', aspectRatio: '1 / 1', borderRadius: 8, background: '#eef0f2' }} />
      <div aria-hidden="true" style={{ flex: '1 1 320px', display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div style={{ width: '60%', height: 22, borderRadius: 4, background: '#eef0f2' }} />
        <div style={{ width: '40%', height: 28, borderRadius: 4, background: '#eef0f2' }} />
        <div style={{ width: '30%', height: 16, borderRadius: 4, background: '#eef0f2' }} />
        <div style={{ width: '100%', height: 120, borderRadius: 6, background: '#eef0f2' }} />
      </div>
    </div>
  )
}
