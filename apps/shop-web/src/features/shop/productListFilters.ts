// SR-303 — 상품 목록(검색·필터) 화면의 순수 로직(가격대 필터·정렬·빈 사유 판정). `discountRate.ts`/
// `recentlyViewedStorage.ts`와 같은 관례로 로직을 파일로 빼서 유닛테스트하고, 컴포넌트에 복제하지 않는다.
import type { Product } from '../../types'

/**
 * 페이지당 개수 — 확정 문답에 숫자로 없어 계획에서 정한 값(계획 확인 게이트, 사람 지시 —
 * `PRODUCT_GRID_DISPLAY_COUNT`와 같은 관례로 상수+주석으로 남긴다). `ProductListPage`(페이지 slice)와
 * `ProductListGrid`(로딩 스켈레톤 카드 개수)가 이 한 값을 함께 참조한다 — round1 QA 권고 4(low/spec):
 * 두 파일에 각각 다른 값(12/8)을 따로 두면 로딩 골격 개수와 실제 페이지 카드 수가 어긋난다.
 */
export const PAGE_SIZE = 12

export type SortKey = 'recommend' | 'priceAsc' | 'priceDesc'

/**
 * 가격대 필터 — min/max는 화면 입력 그대로의 문자열(빈 문자열 허용)이다. 빈 값이거나 숫자로 파싱되지
 * 않으면 그 경계는 적용하지 않는다(하한/상한 중 하나만 채워도 동작). 경계값은 포함(inclusive) —
 * min과 정확히 같은 가격, max와 정확히 같은 가격 모두 통과한다.
 */
export function filterByPriceRange(products: Product[], min: string, max: string): Product[] {
  const minValue = parsePriceBound(min)
  const maxValue = parsePriceBound(max)
  if (minValue == null && maxValue == null) return products
  return products.filter(p => {
    if (minValue != null && p.price < minValue) return false
    if (maxValue != null && p.price > maxValue) return false
    return true
  })
}

function parsePriceBound(raw: string): number | null {
  const trimmed = raw.trim()
  if (!trimmed) return null
  const n = Number(trimmed)
  return Number.isFinite(n) ? n : null
}

/**
 * 정렬 — `recommend`는 서버 응답 순서를 그대로 유지한다(원본 배열은 고치지 않고 얕은 복사만 반환).
 * `priceAsc`/`priceDesc`는 가격 기준 정렬(동률 안정성은 요구하지 않음 — 확정 문답에 없음).
 */
export function sortProducts(products: Product[], sortKey: SortKey): Product[] {
  if (sortKey === 'recommend') return [...products]
  const sorted = [...products]
  sorted.sort((a, b) => (sortKey === 'priceAsc' ? a.price - b.price : b.price - a.price))
  return sorted
}

export type EmptyReason = 'keyword' | 'filter' | 'none'

/**
 * 결과 0건일 때 어떤 안내를 보일지 가른다. 검색어가 있으면 "검색 결과가 없습니다"를 최우선으로 보고
 * (확정 문답), 검색어 없이 필터(재고·가격대)만 걸려 있으면 "조건에 맞는 상품이 없습니다", 둘 다
 * 아니면(서버 응답 자체가 0건) 일반 빈 상태로 가른다. count > 0이면 안내가 필요 없어 null.
 */
export function resolveEmptyReason(count: number, keyword: string, hasActiveFilter: boolean): EmptyReason | null {
  if (count > 0) return null
  if (keyword.trim()) return 'keyword'
  if (hasActiveFilter) return 'filter'
  return 'none'
}
