// SR-304 — 상품 상세 조회 실패 안내 상태(상품없음·조회실패).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductNotFoundNotice } from './ProductNotFoundNotice'

const meta = {
  title: '쇼핑상세/조회 실패 안내',
  component: ProductNotFoundNotice,
  args: { onBackToList: () => {}, onRetry: () => {} },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ProductNotFoundNotice>
export default meta

type Story = StoryObj<typeof meta>

/** 상품없음 — 404. [목록으로]. */
export const 상품없음: Story = { args: { reason: 'notFound' } }

/** 조회실패 — 네트워크/5xx. [다시 시도]. */
export const 조회실패: Story = { args: { reason: 'fetchError' } }
