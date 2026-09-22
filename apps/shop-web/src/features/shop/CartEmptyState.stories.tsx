// SR-305 — 장바구니 빈 상태.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { CartEmptyState } from './CartEmptyState'

const meta = {
  title: '장바구니/빈 장바구니',
  component: CartEmptyState,
  tags: ['UIS-ORD-012'],
} satisfies Meta<typeof CartEmptyState>
export default meta

type Story = StoryObj<typeof meta>

/** 빈장바구니 — 담긴 상품이 없을 때. */
export const 빈장바구니: Story = {}
