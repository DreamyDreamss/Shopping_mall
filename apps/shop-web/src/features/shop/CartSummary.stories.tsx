// SR-305 — 장바구니 합계 상태들(전체선택/부분선택/선택없음/빈장바구니).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { CartSummary } from './CartSummary'

const meta = {
  title: '장바구니/합계',
  component: CartSummary,
  args: { onOrder: () => {} },
  tags: ['UIS-ORD-012'],
} satisfies Meta<typeof CartSummary>
export default meta

type Story = StoryObj<typeof meta>

/** 전체선택 — [주문하기] 활성. */
export const 전체선택: Story = {
  args: { productAmount: 55000, shippingFee: 3000, payableAmount: 58000, selectedCount: 2, totalCount: 2 },
}

/** 부분선택 — [주문하기] 비활성 + 부분선택 안내(aria-describedby로 연결). */
export const 부분선택: Story = {
  args: { productAmount: 30000, shippingFee: 3000, payableAmount: 33000, selectedCount: 1, totalCount: 2 },
}

/** 선택없음 — 상품금액/배송비 0, [주문하기] 비활성 + "주문할 상품을 선택해 주세요" 안내가
 * `aria-describedby`로 버튼과 연결된다(재작업 round 2, 권고 1 — 부분선택과 같은 방식). */
export const 선택없음: Story = {
  args: { productAmount: 0, shippingFee: 0, payableAmount: 0, selectedCount: 0, totalCount: 2 },
}
