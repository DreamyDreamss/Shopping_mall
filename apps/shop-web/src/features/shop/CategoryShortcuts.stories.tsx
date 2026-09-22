// SR-302 — 카테고리 숏컷 상태.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { CategoryShortcuts } from './CategoryShortcuts'
import { CATEGORY_SHORTCUTS } from './shopStatic'

const meta = {
  title: '쇼핑홈/카테고리 숏컷',
  component: CategoryShortcuts,
  // SR-303 재작업 지시 2 — 클릭 시 상품 목록 화면으로 이동하는 실사용 형태(onSelect 있음)를
  // 기본 상태로 노출한다(`ProductCard.stories.tsx`와 동일 관례).
  args: { onSelect: () => {} },
  tags: ['UIS-ORD-008'],
} satisfies Meta<typeof CategoryShortcuts>
export default meta

type Story = StoryObj<typeof meta>

export const 기본: Story = { args: { categories: CATEGORY_SHORTCUTS } }
