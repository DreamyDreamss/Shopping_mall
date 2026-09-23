// SR-304 — 상품정보제공고시 표 상태(기본, 전항목 '-').
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductNoticeTable } from './ProductNoticeTable'

const meta = {
  title: '쇼핑상세/상품정보제공고시',
  component: ProductNoticeTable,
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ProductNoticeTable>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 값 소스가 없어 전 항목이 '-'로 표시된다. */
export const 기본: Story = { tags: ['state:기본'],}
