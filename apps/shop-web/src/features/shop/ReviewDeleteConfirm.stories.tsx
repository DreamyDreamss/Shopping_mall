// SR-351 — 상품평 삭제 확인 다이얼로그 상태(기본·삭제중).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ReviewDeleteConfirm } from './ReviewDeleteConfirm'

const meta = {
  title: '쇼핑상세/상품평/삭제 확인',
  component: ReviewDeleteConfirm,
  args: { open: true, onCancel: () => {}, onConfirm: () => {} },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ReviewDeleteConfirm>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 제목/본문/[취소]/[삭제] 확정 문구 그대로. */
export const 기본: Story = { tags: ['state:기본'] }

/** 삭제중 — [삭제] 버튼이 "삭제 중…"으로 잠긴다. */
export const 삭제중: Story = { args: { confirming: true } }
