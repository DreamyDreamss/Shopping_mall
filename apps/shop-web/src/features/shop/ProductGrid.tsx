// SR-302 — 추천 상품 그리드(로딩/빈목록/조회실패+다시시도/목록있음).
import type { Product } from '../../types'
import { ProductCard } from './ProductCard'

/**
 * 가정값 — 그리드 표시 개수는 SR-302 확정 문답에 숫자로 없다("추천·랭킹 알고리즘 없음, 응답 순서
 * 그대로"만 확정). 이 8은 구현 계획에서 정한 값이고(계획 확인 게이트, 사람 수정 승인), 바꾸려면
 * 이 상수만 고치면 된다. 정렬은 응답 순서 그대로(가공하지 않음).
 */
export const PRODUCT_GRID_DISPLAY_COUNT = 8

export interface ProductGridProps {
  rows: Product[]
  loading?: boolean
  /** 조회 실패 사유(네트워크·5xx). 서버 계약은 그대로이고 화면 문구만 붙인다(확정 답변 api_error). */
  error?: string | null
  onRetry?: () => void
  onSelect?: (sku: string) => void
}

export function ProductGrid({ rows, loading, error, onRetry, onSelect }: ProductGridProps) {
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

  if (loading) return <div aria-busy="true" style={{ padding: 14, color: '#666' }}>불러오는 중…</div>

  if (!rows.length) {
    return (
      <div style={{ padding: '20px 4px', color: '#666' }}>
        <div>표시할 상품이 없습니다</div>
        <div style={{ fontSize: 12.5, marginTop: 4, color: '#999' }}>위 카테고리에서 다른 상품을 찾아보세요</div>
      </div>
    )
  }

  const visible = rows.slice(0, PRODUCT_GRID_DISPLAY_COUNT)
  return (
    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(150px, 1fr))', gap: 14 }}>
      {visible.map(p => <ProductCard key={p.sku} product={p} onSelect={onSelect} />)}
    </div>
  )
}
