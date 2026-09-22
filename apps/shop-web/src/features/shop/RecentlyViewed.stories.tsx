// SR-302 — 최근 본 상품 상태(있음·없음).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { RecentlyViewed } from './RecentlyViewed'
import type { Product } from '../../types'

const products: Product[] = [
  { sku: 'r1', productName: '최근 본 상품 A', price: 15000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'r2', productName: '최근 본 상품 B', price: 22000, stockQty: 0, saleYn: 'Y', listPrice: null, imageUrl: null },
]

const meta = {
  title: '쇼핑홈/최근 본 상품',
  component: RecentlyViewed,
  tags: ['UIS-ORD-008'],
} satisfies Meta<typeof RecentlyViewed>
export default meta

type Story = StoryObj<typeof meta>

export const 있음: Story = { args: { products } }
export const 없음: Story = { args: { products: [] } }
