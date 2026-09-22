// SR-304 — 상품 상세 최초 로딩 상태.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductDetailSkeleton } from './ProductDetailSkeleton'

const meta = {
  title: '쇼핑상세/로딩 스켈레톤',
  component: ProductDetailSkeleton,
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ProductDetailSkeleton>
export default meta

type Story = StoryObj<typeof meta>

/** 로딩 — 이미지·정보 영역 골격만 보인다. */
export const 로딩: Story = {}
