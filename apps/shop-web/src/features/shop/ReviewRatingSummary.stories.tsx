// SR-351 — 상품평 요약 상태(기본·리뷰없음).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ReviewRatingSummary } from './ReviewRatingSummary'

const meta = {
  title: '쇼핑상세/상품평/요약',
  component: ReviewRatingSummary,
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ReviewRatingSummary>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 평균 4.3점, 20건. */
export const 기본: Story = {
  tags: ['state:기본'],
  args: { avgRating: 4.3, reviewCount: 20, distribution: { '1': 1, '2': 1, '3': 2, '4': 6, '5': 10 } },
}

/** 리뷰없음 — 평균 자리 '-', 0건. */
export const 리뷰없음: Story = {
  args: { avgRating: null, reviewCount: 0, distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 } },
}
