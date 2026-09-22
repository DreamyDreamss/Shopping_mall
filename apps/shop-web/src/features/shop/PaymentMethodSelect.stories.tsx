// SR-305 — 결제 수단 표시(항상 기본값 선택 — 빈 상태 없음).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { PaymentMethodSelect } from './PaymentMethodSelect'

const meta = {
  title: '주문서/결제 수단',
  component: PaymentMethodSelect,
  args: { onChange: () => {} },
  tags: ['UIS-ORD-011'],
} satisfies Meta<typeof PaymentMethodSelect>
export default meta

type Story = StoryObj<typeof meta>

/** 기본선택 — 카드가 기본값. */
export const 기본선택: Story = { args: { value: 'CARD' } }

/** 계좌이체선택 — 다른 옵션 선택 상태. */
export const 계좌이체선택: Story = { args: { value: 'BANK_TRANSFER' } }
