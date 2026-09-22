// SR-305 — cartTotals.ts 경계값(선택 0/일부/전체).
import { describe, expect, test } from '@jest/globals'
import { calcCartTotals, calcTotalsFromProductAmount, SHIPPING_FEE_FLAT } from './cartTotals'

const lines = [
  { sku: 'sku-cart-t1', lineTotal: 10000 },
  { sku: 'sku-cart-t2', lineTotal: 25000 },
  { sku: 'sku-cart-t3', lineTotal: 5000 },
]

describe('calcCartTotals', () => {
  test('선택 0건 — 상품금액 0, 배송비 0, 결제예정금액 0', () => {
    expect(calcCartTotals(lines, new Set())).toEqual({ productAmount: 0, shippingFee: 0, payableAmount: 0 })
  })

  test('선택 일부(1건) — 그 품목 lineTotal만 합산 + 고정 배송비', () => {
    const result = calcCartTotals(lines, new Set(['sku-cart-t2']))
    expect(result).toEqual({
      productAmount: 25000,
      shippingFee: SHIPPING_FEE_FLAT,
      payableAmount: 25000 + SHIPPING_FEE_FLAT,
    })
  })

  test('선택 일부(2건) — 선택된 두 품목의 lineTotal만 합산', () => {
    const result = calcCartTotals(lines, new Set(['sku-cart-t1', 'sku-cart-t3']))
    expect(result).toEqual({
      productAmount: 15000,
      shippingFee: SHIPPING_FEE_FLAT,
      payableAmount: 15000 + SHIPPING_FEE_FLAT,
    })
  })

  test('전체 선택 — 전 품목 lineTotal 합산 + 고정 배송비 1회만', () => {
    const result = calcCartTotals(lines, new Set(['sku-cart-t1', 'sku-cart-t2', 'sku-cart-t3']))
    expect(result).toEqual({
      productAmount: 40000,
      shippingFee: SHIPPING_FEE_FLAT,
      payableAmount: 40000 + SHIPPING_FEE_FLAT,
    })
  })

  test('빈 장바구니(lines 자체가 0건) — 선택 집합이 있어도 전부 0', () => {
    expect(calcCartTotals([], new Set(['sku-none']))).toEqual({ productAmount: 0, shippingFee: 0, payableAmount: 0 })
  })

  test('lineTotal을 재계산하지 않는다 — price*qty와 다른 값이어도 그대로 합산(서버 응답 정본)', () => {
    // 서버가 이미 계산해 준 lineTotal을 그대로 신뢰한다(클라이언트가 price*qty로 다시 계산하지 않음).
    const skewedLines = [{ sku: 'sku-cart-skew', lineTotal: 999 }]
    const result = calcCartTotals(skewedLines, new Set(['sku-cart-skew']))
    expect(result.productAmount).toBe(999)
  })
})

// 재작업(round 2, QA FAIL 필수2) — 주문서·주문완료가 같은 규칙을 쓰는지 경계값으로 고정한다.
describe('calcTotalsFromProductAmount', () => {
  test('상품금액이 0보다 크면 calcCartTotals와 같은 배송비가 붙는다', () => {
    const viaLines = calcCartTotals([{ sku: 'sku-a', lineTotal: 30000 }], new Set(['sku-a']))
    const viaAmount = calcTotalsFromProductAmount(30000)
    expect(viaAmount).toEqual(viaLines)
    expect(viaAmount).toEqual({ productAmount: 30000, shippingFee: SHIPPING_FEE_FLAT, payableAmount: 30000 + SHIPPING_FEE_FLAT })
  })

  test('상품금액 0 — 배송비도 0(빈 주문은 없지만 경계값으로 확인)', () => {
    expect(calcTotalsFromProductAmount(0)).toEqual({ productAmount: 0, shippingFee: 0, payableAmount: 0 })
  })
})
