// SR-351 — 작성 영역 상태(기본(로그인, 폼)·비로그인).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ReviewWriteArea } from './ReviewWriteArea'

const meta = {
  title: '쇼핑상세/상품평/작성 영역',
  component: ReviewWriteArea,
  args: {
    loggedIn: true,
    eligibleOrders: [{ orderNo: 'ORD-2026-0001', orderedAt: '2026-08-01T10:00:00Z' }],
    orderLookupStatus: 'ready', onRetryOrderLookup: () => {},
    orderNo: '', onOrderNoChange: () => {}, rating: 0, onRatingChange: () => {},
    content: '', onContentChange: () => {}, fieldErrors: {}, status: 'idle', onSubmit: () => {},
  },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ReviewWriteArea>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 로그인 상태, 작성 폼이 보인다. */
export const 기본: Story = { tags: ['state:기본'] }

/** 비로그인 — 폼 대신 "로그인 후 작성할 수 있습니다" 고정 문구. */
export const 비로그인: Story = { args: { loggedIn: false } }
