// SR-304 — "함께 보면 좋은 상품" 후보 계산(현재 sku 제외·limit 적용·후보 없음).
import { describe, expect, test } from '@jest/globals'
import { pickRelatedProducts } from './relatedProductsPicker'
import type { Product } from '../../types'

function product(i: number): Product {
  return {
    sku: `sku-related-${i}`, productName: `관련테스트상품${i}`, price: 10000 + i * 1000, stockQty: 5,
    saleYn: 'Y', listPrice: null, imageUrl: null,
  }
}

describe('pickRelatedProducts', () => {
  // linked_tc: TC-FUNC-shop-010-12
  test('현재 sku는 후보에서 제외된다', () => {
    const products = [product(1), product(2), product(3)]
    const result = pickRelatedProducts(products, 'sku-related-2')
    expect(result.map(p => p.sku)).toEqual(['sku-related-1', 'sku-related-3'])
  })

  // linked_tc: TC-FUNC-shop-010-13
  test('limit을 넘는 후보는 앞에서부터 limit개만 반환한다', () => {
    const products = [product(1), product(2), product(3), product(4), product(5)]
    const result = pickRelatedProducts(products, 'sku-related-1', 2)
    expect(result.map(p => p.sku)).toEqual(['sku-related-2', 'sku-related-3'])
  })

  // linked_tc: TC-FUNC-shop-010-14
  test('limit 미지정 시 기본값 8개까지 반환한다', () => {
    const products = Array.from({ length: 10 }, (_, i) => product(i + 1))
    const result = pickRelatedProducts(products, 'sku-related-1')
    expect(result).toHaveLength(8)
  })

  // linked_tc: TC-FUNC-shop-010-15
  test('현재 상품 하나뿐이면(다른 후보 없음) 빈 배열을 반환한다', () => {
    const result = pickRelatedProducts([product(1)], 'sku-related-1')
    expect(result).toEqual([])
  })

  // linked_tc: TC-FUNC-shop-010-16
  test('전체 목록이 비어 있어도 빈 배열을 반환한다', () => {
    expect(pickRelatedProducts([], 'sku-related-1')).toEqual([])
  })
})
