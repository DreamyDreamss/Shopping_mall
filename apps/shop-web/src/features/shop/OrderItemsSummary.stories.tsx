// SR-305 — 주문서 상품 요약(읽기전용).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { OrderItemsSummary } from './OrderItemsSummary'

const meta = {
  title: '주문서/상품 요약',
  component: OrderItemsSummary,
  tags: ['UIS-ORD-011'],
} satisfies Meta<typeof OrderItemsSummary>
export default meta

type Story = StoryObj<typeof meta>

/** 주문서기본 — 장바구니 전량이 그대로 요약된다. */
export const 주문서기본: Story = {
  args: {
    items: [
      { sku: 'sku-order-1', productName: '테스트 상품A', price: 15000, qty: 2, lineTotal: 30000 },
      { sku: 'sku-order-2', productName: '테스트 상품B', price: 8000, qty: 1, lineTotal: 8000 },
    ],
  },
}
