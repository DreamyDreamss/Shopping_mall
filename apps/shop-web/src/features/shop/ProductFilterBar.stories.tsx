// SR-303 — 상품 목록 필터바 상태(기본·정렬변경·재고필터켬).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductFilterBar } from './ProductFilterBar'

const meta = {
  title: '쇼핑홈/상품 목록 필터바',
  component: ProductFilterBar,
  args: {
    onInStockOnlyChange: () => {}, onPriceMinChange: () => {}, onPriceMaxChange: () => {}, onSortKeyChange: () => {},
  },
  tags: ['UIS-ORD-009'],
} satisfies Meta<typeof ProductFilterBar>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 재고 필터 꺼짐, 가격대 미입력, 추천순. */
export const 기본: Story = {
  args: { inStockOnly: false, priceMin: '', priceMax: '', sortKey: 'recommend', resultCount: 24 },
}

/** 정렬변경 — 낮은 가격순 선택. */
export const 정렬변경: Story = {
  args: { inStockOnly: false, priceMin: '', priceMax: '', sortKey: 'priceAsc', resultCount: 24 },
}

/** 재고필터켬 — 재고 있는 상품만 체크, 결과 개수가 그만큼 줄어든 상태. */
export const 재고필터켬: Story = {
  args: { inStockOnly: true, priceMin: '', priceMax: '', sortKey: 'recommend', resultCount: 17 },
}
