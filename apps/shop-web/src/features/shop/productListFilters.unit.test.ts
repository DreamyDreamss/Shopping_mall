// SR-303 — productListFilters 유닛 테스트(가격대 필터·정렬·빈 사유 판정).
import { describe, expect, test } from '@jest/globals'
import { filterByPriceRange, resolveEmptyReason, sortProducts } from './productListFilters'
import type { Product } from '../../types'

function product(sku: string, price: number): Product {
  return { sku, productName: `상품-${sku}`, price, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null }
}

describe('filterByPriceRange', () => {
  const rows = [product('a', 10000), product('b', 20000), product('c', 30000)]

  // linked_tc: TC-FUNC-shop-products-001
  test('min만 지정 — min 이상만 통과(경계값 포함)', () => {
    expect(filterByPriceRange(rows, '20000', '')).toEqual([product('b', 20000), product('c', 30000)])
  })

  // linked_tc: TC-FUNC-shop-products-002
  test('max만 지정 — max 이하만 통과(경계값 포함)', () => {
    expect(filterByPriceRange(rows, '', '20000')).toEqual([product('a', 10000), product('b', 20000)])
  })

  // linked_tc: TC-FUNC-shop-products-003
  test('min·max 둘 다 지정 — 범위 안(양끝 포함)만 통과', () => {
    expect(filterByPriceRange(rows, '15000', '25000')).toEqual([product('b', 20000)])
  })

  // linked_tc: TC-FUNC-shop-products-004
  test('min=max=정확히 한 상품의 가격 — 그 상품만 경계값 일치로 통과', () => {
    expect(filterByPriceRange(rows, '20000', '20000')).toEqual([product('b', 20000)])
  })

  // linked_tc: TC-FUNC-shop-products-005
  test('둘 다 빈값 — 원본 그대로(필터링 없음)', () => {
    expect(filterByPriceRange(rows, '', '')).toEqual(rows)
  })

  // linked_tc: TC-FUNC-shop-products-006
  test('둘 다 숫자로 파싱 안 되는 값 — 그 경계는 무시(원본 그대로)', () => {
    expect(filterByPriceRange(rows, 'abc', 'xyz')).toEqual(rows)
  })
})

describe('sortProducts', () => {
  const rows = [product('c', 30000), product('a', 10000), product('b', 20000)]

  // linked_tc: TC-FUNC-shop-products-007
  test('recommend — 입력 순서 그대로 유지(원본 배열 훼손 없이 얕은 복사)', () => {
    const result = sortProducts(rows, 'recommend')
    expect(result).toEqual(rows)
    expect(result).not.toBe(rows)
  })

  // linked_tc: TC-FUNC-shop-products-008
  test('priceAsc — 가격 오름차순', () => {
    expect(sortProducts(rows, 'priceAsc').map(p => p.sku)).toEqual(['a', 'b', 'c'])
  })

  // linked_tc: TC-FUNC-shop-products-009
  test('priceDesc — 가격 내림차순', () => {
    expect(sortProducts(rows, 'priceDesc').map(p => p.sku)).toEqual(['c', 'b', 'a'])
  })
})

describe('resolveEmptyReason', () => {
  // linked_tc: TC-FUNC-shop-products-010
  test('count > 0 — 안내 필요 없음(null)', () => {
    expect(resolveEmptyReason(3, '', false)).toBeNull()
    expect(resolveEmptyReason(1, '검색어', true)).toBeNull()
  })

  // linked_tc: TC-FUNC-shop-products-011
  test('검색어 있음 + count 0 — keyword(필터 여부와 무관하게 검색 문제를 최우선)', () => {
    expect(resolveEmptyReason(0, '없는상품', false)).toBe('keyword')
    expect(resolveEmptyReason(0, '없는상품', true)).toBe('keyword')
  })

  // linked_tc: TC-FUNC-shop-products-012
  test('검색어 없음 + 필터 있음 + count 0 — filter', () => {
    expect(resolveEmptyReason(0, '', true)).toBe('filter')
  })

  // linked_tc: TC-FUNC-shop-products-013
  test('검색어 없음 + 필터 없음 + count 0 — none', () => {
    expect(resolveEmptyReason(0, '', false)).toBe('none')
  })

  // linked_tc: TC-FUNC-shop-products-014
  test('검색어가 공백뿐이면 없는 것으로 취급', () => {
    expect(resolveEmptyReason(0, '   ', false)).toBe('none')
  })
})
