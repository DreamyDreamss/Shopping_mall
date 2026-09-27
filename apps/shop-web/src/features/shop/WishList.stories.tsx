// SR-356 — 내 찜 목록 상태(기본/빈목록/해제중).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { WishList } from './WishList'
import type { WishItem } from '../../types'

function wish(i: number, overrides: Partial<WishItem> = {}): WishItem {
  return {
    sku: `SKU-wish-${i}`, productName: `찜 상품 ${i}`, price: 10000 * i, imageUrl: null, soldOut: false,
    wishedAt: '2026-09-19T10:00:00',
    ...overrides,
  }
}

const meta = {
  title: '마이페이지/찜 목록',
  component: WishList,
  args: { onRemove: () => {}, pendingSku: null },
  tags: ['UIS-ORD-100'],
} satisfies Meta<typeof WishList>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 3건, 품절 1건 포함. */
export const 기본: Story = {
  tags: ['state:기본'],
  args: { items: [wish(1), wish(2, { soldOut: true }), wish(3)] },
}

/** 빈목록 — "찜한 상품이 없습니다" + 상품 보러 가기 링크. */
export const 빈목록: Story = { tags: ['state:빈'], args: { items: [] } }

/** 해제중 — 한 행이 pendingSku로 잠긴 상태(버튼 disabled 시각 확인). */
export const 해제중: Story = {
  tags: ['state:로딩'],
  args: { items: [wish(1), wish(2)], pendingSku: 'SKU-wish-2' },
}
