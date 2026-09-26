// SR-351 — 리뷰 1건 상태(기본(타인, 삭제버튼 없음)·본인리뷰(삭제버튼 있음)·삭제중).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ReviewListItem } from './ReviewListItem'
import type { ReviewItem } from '../../types'

const review: ReviewItem = {
  reviewId: 1, rating: 4, content: '배송이 빠르고 품질도 좋았습니다. 재구매 의사 있어요.',
  authorName: '홍*동', createdAt: '2026-09-20T10:00:00Z',
}

const meta = {
  title: '쇼핑상세/상품평/목록 항목',
  component: ReviewListItem,
  args: { review, onDeleteRequest: () => {} },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ReviewListItem>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 타인이 쓴 리뷰, 삭제 버튼이 없다. */
export const 기본: Story = { tags: ['state:기본'], args: { isOwn: false } }

/** 본인리뷰 — 삭제 버튼이 보인다. */
export const 본인리뷰: Story = { args: { isOwn: true } }

/** 삭제중 — 삭제 버튼이 "삭제 중…"으로 잠긴다. */
export const 삭제중: Story = { args: { isOwn: true, deleting: true } }
