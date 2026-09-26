// SR-351 — 별점 분포 막대 상태(기본·전체0건).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { RatingDistributionBar } from './RatingDistributionBar'

const meta = {
  title: '쇼핑상세/상품평/별점 분포',
  component: RatingDistributionBar,
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof RatingDistributionBar>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 5점 위주 분포. */
export const 기본: Story = {
  tags: ['state:기본'],
  args: { reviewCount: 20, distribution: { '1': 1, '2': 1, '3': 2, '4': 6, '5': 10 } },
}

/** 전체0건 — 분모 0 나눗셈 방어(모든 막대 폭 0). */
export const 전체0건: Story = {
  args: { reviewCount: 0, distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 } },
}
