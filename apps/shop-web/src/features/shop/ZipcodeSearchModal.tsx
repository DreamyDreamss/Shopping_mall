// SR-305 — 주문서(`/shop/order`) 우편번호 검색 모달. 이 부품은 fetch를 하지 않는다(규칙
// `web-fetch-only-in-api`, `ProductInfoPanel`과 동일 원칙) — 검색 실행(`GET /api/zipcodes?q=`)은
// 부모(`OrderPage`)가 `searchZipcodes`(`src/api.ts`)로 수행하고, 이 부품은 검색어 입력·결과 목록·
// 선택 콜백만 담당한다.
import type { ZipcodeResult } from '../../types'

export interface ZipcodeSearchModalProps {
  open: boolean
  query: string
  onQueryChange: (value: string) => void
  onSearch: () => void
  results: ZipcodeResult[]
  loading?: boolean
  error?: string | null
  onSelect: (result: ZipcodeResult) => void
  onClose: () => void
}

const overlayStyle: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50,
}

const panelStyle: React.CSSProperties = {
  background: '#fff', borderRadius: 8, padding: 20, width: 360, maxHeight: '70vh',
  display: 'flex', flexDirection: 'column', gap: 12,
}

export function ZipcodeSearchModal({
  open, query, onQueryChange, onSearch, results, loading, error, onSelect, onClose,
}: ZipcodeSearchModalProps) {
  if (!open) return null

  return (
    <div style={overlayStyle} onClick={onClose}>
      <div role="dialog" aria-label="우편번호 검색" style={panelStyle} onClick={e => e.stopPropagation()}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <h2 style={{ fontSize: 15, margin: 0 }}>우편번호 찾기</h2>
          <button type="button" onClick={onClose} aria-label="닫기"
                  style={{ border: 0, background: 'none', fontSize: 16, cursor: 'pointer' }}>✕</button>
        </div>

        <form onSubmit={e => { e.preventDefault(); onSearch() }} style={{ display: 'flex', gap: 8 }}>
          <input aria-label="도로명·지번 검색어" value={query} onChange={e => onQueryChange(e.target.value)}
                 placeholder="도로명 또는 지번 주소"
                 style={{ flex: 1, border: '1px solid #d5d8dd', borderRadius: 5, padding: '7px 10px', fontSize: 13 }} />
          <button type="submit"
                  style={{ border: 0, borderRadius: 5, background: '#0b4ea2', color: '#fff', fontSize: 13, padding: '0 14px', cursor: 'pointer' }}>
            검색
          </button>
        </form>

        {loading && <div role="status" style={{ fontSize: 12.5, color: '#666' }}>검색 중…</div>}
        {error && <div role="alert" style={{ fontSize: 12.5, color: '#912d2b' }}>{error}</div>}

        {!loading && !error && (
          results.length > 0 ? (
            <ul style={{ listStyle: 'none', margin: 0, padding: 0, overflowY: 'auto' }}>
              {results.map(r => (
                <li key={r.zipcode + r.roadAddress}>
                  <button type="button" onClick={() => onSelect(r)}
                          style={{ width: '100%', textAlign: 'left', border: 0, borderBottom: '1px solid #eef0f2',
                                   background: 'none', padding: '8px 4px', cursor: 'pointer', fontSize: 12.5 }}>
                    <span style={{ fontWeight: 700, marginRight: 8 }}>[{r.zipcode}]</span>
                    {r.roadAddress}
                  </button>
                </li>
              ))}
            </ul>
          ) : (
            <div style={{ fontSize: 12.5, color: '#999' }}>검색 결과가 없습니다</div>
          )
        )}
      </div>
    </div>
  )
}
