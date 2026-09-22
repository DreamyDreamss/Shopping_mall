// SR-304 — "함께 보면 좋은 상품" 가로 레일. `RecentlyViewed.tsx`와 동일 레이아웃(가로 스크롤 +
// `ProductCard` 재사용). 추천 알고리즘이 아니라 `pickRelatedProducts`(순수 함수, `relatedProducts.ts`)가
// 고른 "표시 가능한 다른 상품"일 뿐이다(STORY "데이터" 절).
import type { Product } from '../../types'
import { ProductCard } from './ProductCard'

export interface RelatedProductsProps {
  /** 이미 `pickRelatedProducts`로 걸러진 후보만 받는다(현재 sku 제외·limit 적용은 호출자 몫). */
  products: Product[]
  onSelect?: (sku: string) => void
}

/** 후보가 없으면 섹션 자체를 숨긴다 — "표시할 상품이 없습니다" 같은 빈 상태를 지어내지 않는다
 * (STORY "스토리 목록" 절, `renders-nothing` 태그 스토리로 이 빈 렌더를 고장으로 보지 않게 한다). */
export function RelatedProducts({ products, onSelect }: RelatedProductsProps) {
  if (!products.length) return null

  return (
    <section aria-label="함께 보면 좋은 상품">
      <h2 style={{ fontSize: 16, marginBottom: 10 }}>함께 보면 좋은 상품</h2>
      <div style={{ display: 'flex', gap: 12, overflowX: 'auto' }}>
        {products.map(p => (
          <div key={p.sku} style={{ minWidth: 150 }}>
            <ProductCard product={p} onSelect={onSelect} />
          </div>
        ))}
      </div>
    </section>
  )
}
