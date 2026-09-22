// linked_func: FUNC-order-001 — 주문 요약 카드의 상태들(SR-227)
import type { Meta, StoryObj } from '@storybook/react-vite'
import { OrderSummaryCard } from './OrderSummaryCard'
import type { OrderRow } from '../types'

const meta = {
  title: '주문/주문 요약 카드',
  component: OrderSummaryCard,
  tags: ['UIS-ORD-001', 'FUNC-order-001'],
} satisfies Meta<typeof OrderSummaryCard>
export default meta

type Story = StoryObj<typeof meta>

const rows: OrderRow[] = [
  { orderNo: 'O-1001', memberId: 'm001', memberName: '김개발', orderState: 'PAID', totalAmount: 32000, orderedAt: '2026-08-01 10:00', deliveryState: 'READY' },
  { orderNo: 'O-1002', memberId: 'm002', memberName: '이운영', orderState: 'SHIPPING', totalAmount: 118000, orderedAt: '2026-08-02 11:30', deliveryState: 'SHIPPED' },
  { orderNo: 'O-1003', memberId: 'm003', memberName: null, orderState: 'PAID', totalAmount: 5400, orderedAt: '2026-08-03 09:12', deliveryState: null },
]

/** 집계 있음 — 건수·총액·상태별 건수 */
export const 집계있음: Story = { args: { rows } }

/** 0건 — 0원·0건을 늘어놓지 않고 "집계할 주문이 없습니다" */
export const 결과없음: Story = { args: { rows: [] } }

/** 조회 실패 — 카드도 숨긴다(그리드와 같은 규칙) */
export const 조회실패숨김: Story = {
  args: { rows, error: '500 Internal Server Error' },
  tags: ['renders-nothing'],   // 이 상태는 아무것도 그리지 않는 것이 정답이다 — 빈 렌더를 고장으로 보지 않게
}
