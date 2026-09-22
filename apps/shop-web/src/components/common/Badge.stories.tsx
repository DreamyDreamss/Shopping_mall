// SR-310 — Badge 5종 각각의 스토리.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { Badge } from './Badge'

const meta = {
  title: '공통/Badge',
  component: Badge,
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof Badge>
export default meta

type Story = StoryObj<typeof meta>

export const TV상품: Story = { args: { variant: 'tv' } }
export const 무료배송: Story = { args: { variant: 'freeShipping' } }
export const 무이자: Story = { args: { variant: 'installment', months: 6 } }

/** 카운트다운 슬롯 — 텍스트만 받는다(실시간 갱신 타이머는 범위 밖). */
export const LIVE: Story = { args: { variant: 'live', countdownSlot: '00:12:34' } }

/** 호출부(예: ProductCard)가 기존 calcDiscountRate로 계산한 값을 그대로 넘긴다. */
export const 할인율: Story = { args: { variant: 'discount', rate: 30 } }
