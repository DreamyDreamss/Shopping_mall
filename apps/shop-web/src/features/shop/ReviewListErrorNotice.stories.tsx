// SR-351 — 상품평 목록 조회 실패 안내 상태(기본).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ReviewListErrorNotice } from './ReviewListErrorNotice'

const meta = {
  title: '쇼핑상세/상품평/목록 조회 실패',
  component: ReviewListErrorNotice,
  args: { onRetry: () => {} },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ReviewListErrorNotice>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — "상품평을 불러오지 못했습니다" + [다시 시도]. */
export const 기본: Story = { tags: ['state:기본'] }
