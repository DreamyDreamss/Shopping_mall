// SR-304 — "함께 보면 좋은 상품" 후보 계산. `discountRate.ts`/`productListFilters.ts`와 같은 관례로
// 로직을 파일로 빼서 유닛테스트하고, 컴포넌트/페이지에 복제하지 않는다.
import type { Product } from '../../types'

/**
 * 전용 추천 API/카테고리 데이터가 없어(STORY "데이터" 절) 추천 알고리즘이 아니라 "현재 상품을 제외한
 * 앞 limit개"만 반환한다 — 이미 받은 전체 상품 목록(`fetchProducts`, 무인자) 결과 안에서만 계산한다.
 */
export function pickRelatedProducts(products: Product[], currentSku: string, limit = 8): Product[] {
  return products.filter(p => p.sku !== currentSku).slice(0, limit)
}
