// SR-304 — "함께 보면 좋은 상품" 레일 상태(기본·비어있음).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { RelatedProducts } from './RelatedProducts'
import type { Product } from '../../types'

const rows: Product[] = [
  { sku: 'SKU-1001', productName: '스탠딩 데스크', price: 390000, stockQty: 12, saleYn: 'Y', listPrice: 450000, imageUrl: null },
  { sku: 'SKU-1002', productName: '기계식 키보드', price: 129000, stockQty: 20, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'SKU-1003', productName: '4K 모니터', price: 450000, stockQty: 8, saleYn: 'Y', listPrice: 450000, imageUrl: null },
]

const meta = {
  title: '쇼핑상세/함께 보면 좋은 상품',
  component: RelatedProducts,
  args: { onSelect: () => {} },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof RelatedProducts>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 후보 N개가 가로 스크롤 레일로 보인다. */
export const 기본: Story = { args: { products: rows } }

/** 비어있음 — 후보가 없으면 섹션 자체를 렌더하지 않는다(빈 렌더가 정답인 상태). */
export const 비어있음: Story = {
  args: { products: [] },
  tags: ['renders-nothing'], // 이 상태는 아무것도 그리지 않는 것이 정답이다 — 빈 렌더를 고장으로 보지 않게
}
