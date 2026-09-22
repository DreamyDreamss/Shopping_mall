// SR-303 — 상품 목록(검색·필터) 화면의 결과 영역(로딩/빈결과(사유별)/조회실패/결과있음). 카드는 기존
// `ProductCard`를 재사용하되(확정 문답 "상품 그리드는 홈의 상품 카드 재사용"), `ProductGrid.tsx`(홈
// 전용, 8개 캡+단일 빈 문구)는 이 화면 요건과 맞지 않아 재사용하지 않는다 — 페이지네이션이 이미
// 개수를 잘라 주므로 여기엔 표시 개수 상한이 없다.
import type { Product } from '../../types'
import { ProductCard } from './ProductCard'
import { PAGE_SIZE, type EmptyReason } from './productListFilters'

/** 로딩 스켈레톤 카드 개수 — round1 QA 권고 4(low/spec): 실제 페이지 크기와 다른 값(8)을 따로 뒀더니
 * 로딩 골격 개수와 실제 페이지 카드 수가 어긋났다. `ProductListPage`의 페이지 slice와 같은 값
 * (`PAGE_SIZE`, `productListFilters.ts`)에서 파생시켜 항상 맞춘다. */
const SKELETON_CARD_COUNT = PAGE_SIZE

export interface ProductListGridProps {
  rows: Product[]
  loading?: boolean
  /** 조회 실패 사유(네트워크·5xx). 서버 계약은 그대로이고 화면 문구만 붙인다(확정 답변 api_error). */
  error?: string | null
  /** rows가 비었을 때 어떤 안내를 보일지(`resolveEmptyReason` 결과 그대로) — rows가 비어있지 않으면 무시된다. */
  emptyReason?: EmptyReason | null
  onRetry?: () => void
  onSelect?: (sku: string) => void
  /** "검색 결과가 없습니다" 상태의 [검색어 지우기] 버튼. */
  onClearKeyword?: () => void
  /** "조건에 맞는 상품이 없습니다" 상태의 [필터 초기화] 버튼. */
  onResetFilters?: () => void
}

const secondaryButton: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#333',
  fontSize: 12.5, padding: '5px 12px', cursor: 'pointer', marginTop: 8,
}

const gridStyle: React.CSSProperties = {
  display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 14,
}

export function ProductListGrid({
  rows, loading, error, emptyReason, onRetry, onSelect, onClearKeyword, onResetFilters,
}: ProductListGridProps) {
  if (error) {
    return (
      <div role="alert" style={{
        border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b',
        borderRadius: 6, padding: '14px 16px', display: 'flex', flexDirection: 'column', gap: 8, alignItems: 'flex-start',
      }}>
        <span>불러오지 못했습니다 — {error}</span>
        {onRetry && (
          <button type="button" onClick={onRetry}
                  style={{ border: '1px solid #912d2b', borderRadius: 5, background: '#fff', color: '#912d2b',
                           fontSize: 12.5, padding: '5px 12px', cursor: 'pointer' }}>
            다시 시도
          </button>
        )}
      </div>
    )
  }

  if (loading) {
    return (
      <div role="status" aria-label="상품 목록 불러오는 중" style={gridStyle}>
        {Array.from({ length: SKELETON_CARD_COUNT }).map((_, i) => (
          <div key={i} aria-hidden="true" style={{ aspectRatio: '3 / 4', borderRadius: 8, background: '#eef0f2' }} />
        ))}
      </div>
    )
  }

  if (!rows.length) {
    if (emptyReason === 'keyword') {
      return (
        <div style={{ padding: '20px 4px', color: '#666' }}>
          <div>검색 결과가 없습니다</div>
          {onClearKeyword && (
            <button type="button" onClick={onClearKeyword} style={secondaryButton}>검색어 지우기</button>
          )}
        </div>
      )
    }
    if (emptyReason === 'filter') {
      return (
        <div style={{ padding: '20px 4px', color: '#666' }}>
          <div>조건에 맞는 상품이 없습니다</div>
          {onResetFilters && (
            <button type="button" onClick={onResetFilters} style={secondaryButton}>필터 초기화</button>
          )}
        </div>
      )
    }
    return <div style={{ padding: '20px 4px', color: '#666' }}>표시할 상품이 없습니다</div>
  }

  return (
    <div style={gridStyle}>
      {rows.map(p => <ProductCard key={p.sku} product={p} onSelect={onSelect} />)}
    </div>
  )
}
