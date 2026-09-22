// SR-302 — 최근 본 상품(있음/없음).
import type { Product } from '../../types'
import { ProductCard } from './ProductCard'

export interface RecentlyViewedProps {
  /**
   * 조회된 최근 본 sku들을 현재 상품 목록에서 찾아 표시할 것만 넘긴다 — 더 이상 판매중이 아니면
   * 호출자(`ShopHomePage`)가 조용히 제외한다(없음을 지어내지 않는다, STORY 구현계획).
   */
  products: Product[]
}

export function RecentlyViewed({ products }: RecentlyViewedProps) {
  return (
    <section aria-label="최근 본 상품">
      <h2 style={{ fontSize: 16, marginBottom: 10 }}>최근 본 상품</h2>
      {!products.length ? (
        <div style={{ padding: 14, color: '#666' }}>최근 본 상품이 없습니다</div>
      ) : (
        <div style={{ display: 'flex', gap: 12, overflowX: 'auto' }}>
          {products.map(p => (
            <div key={p.sku} style={{ minWidth: 150 }}>
              <ProductCard product={p} />
            </div>
          ))}
        </div>
      )}
    </section>
  )
}
