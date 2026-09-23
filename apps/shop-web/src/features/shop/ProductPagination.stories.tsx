// SR-303 — 상품 목록 페이지네이션 상태(기본(중간 페이지)·마지막페이지(다음 버튼 비활성)).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductPagination } from './ProductPagination'

const meta = {
  title: '쇼핑홈/상품 목록 페이지네이션',
  component: ProductPagination,
  args: { onPrev: () => {}, onNext: () => {} },
  tags: ['UIS-ORD-009'],
} satisfies Meta<typeof ProductPagination>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 중간 페이지, 이전/다음 둘 다 활성. */
export const 기본: Story = { tags: ['state:기본'], args: { page: 3, totalPages: 5 } }

/** 마지막페이지 — 다음 버튼 비활성. */
export const 마지막페이지: Story = { args: { page: 5, totalPages: 5 } }

/** 첫페이지 — 이전 버튼 비활성. */
export const 첫페이지: Story = { args: { page: 1, totalPages: 5 } }
