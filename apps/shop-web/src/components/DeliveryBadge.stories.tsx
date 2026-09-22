// linked_func: FUNC-order-001 — 배송상태 배지의 상태들
import type { Meta, StoryObj } from '@storybook/react-vite'
import { DeliveryBadge } from './DeliveryBadge'

const meta = {
  title: '주문/배송상태 배지',
  component: DeliveryBadge,
  tags: ['UIS-ORD-001', 'FUNC-order-001'],
} satisfies Meta<typeof DeliveryBadge>
export default meta

type Story = StoryObj<typeof meta>

export const 출고대기: Story = { args: { state: 'READY' } }
export const 출고: Story = { args: { state: 'SHIPPED' } }
export const 배송완료: Story = { args: { state: 'DELIVERED' } }

/** 이력 없음 — "-"다. 빈 문자열도 "없음"도 아니다(UIS-ORD-001 §5). */
export const 이력없음: Story = { args: { state: null } }
