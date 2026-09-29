/** @jest-environment jsdom */
// SR-365 — 가격 바로 아래 배송비 안내 한 줄. 장바구니와 같은 배송비 규칙(`cartTotals.ts`의
// `calcTotalsFromProductAmount`)을 그대로 재사용하므로 임계값 분기는 여기서 만들지 않는다(오늘의
// `cartTotals.ts`는 금액>0이면 항상 고정요금 3,000원 — STORY "범위 밖" 절). 이 컴포넌트에 지금까지
// 없던 첫 RTL 유닛 테스트라 아래 두 케이스만 담는다(STORY "테스트" 절).
import '@testing-library/jest-dom/jest-globals'
import { render, screen } from '@testing-library/react'
import { describe, expect, test } from '@jest/globals'
import { ProductInfoPanel } from './ProductInfoPanel'
import type { ProductDetail } from '../../types'

const baseProduct: ProductDetail = {
  sku: 'SKU-1002', productName: '기계식 키보드', price: 129000, stockQty: 20, saleYn: 'Y',
  listPrice: null, imageUrl: null, soldOut: false, wishCount: 0,
}

const baseProps = {
  qty: 1,
  onQtyChange: () => {},
  onAddToCart: () => {},
  onBuyNow: () => {},
  addStatus: 'idle' as const,
  addErrorMessage: null,
  lastAction: null,
  disabledReason: null,
  notifyStatus: 'idle' as const,
  onNotifyRequest: () => {},
  wishCount: 0,
  wished: false,
  wishToggleStatus: 'idle' as const,
  wishErrorMessage: null,
  onToggleWish: () => {},
  compared: false,
  compareStatus: 'idle' as const,
  compareErrorMessage: null,
  onToggleCompare: () => {},
}

describe('ProductInfoPanel — 배송비 안내', () => {
  // linked_tc: TC-SR-365-001
  test('가격이 있으면 배송비 안내 한 줄이 정확한 문구로 표시된다(임의 임계값 문구 없음)', () => {
    render(<ProductInfoPanel {...baseProps} product={baseProduct} />)
    expect(screen.getByLabelText('배송비 안내')).toHaveTextContent('배송비 3,000원')
  })

  // linked_tc: TC-SR-365-002
  test('가격이 0이면 배송비 안내 줄이 렌더되지 않는다', () => {
    render(<ProductInfoPanel {...baseProps} product={{ ...baseProduct, price: 0 }} />)
    expect(screen.queryByLabelText('배송비 안내')).toBeNull()
  })
})
