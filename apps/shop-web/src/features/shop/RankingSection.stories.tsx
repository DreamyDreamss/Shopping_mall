// SR-302 — 랭킹 탭 전환.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { expect, userEvent, within } from 'storybook/test'
import { RankingSection } from './RankingSection'
import type { Product } from '../../types'

const products: Product[] = [
  { sku: 'p1', productName: '인기 1위 상품', price: 30000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'p2', productName: '인기 2위 상품', price: 12000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'p3', productName: '인기 3위 상품', price: 45000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'p4', productName: '인기 4위 상품(최저가)', price: 8000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'p5', productName: '인기 5위 상품', price: 21000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'p6', productName: '응답순서 6번째(랭킹 미표시)', price: 5000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
]

const meta = {
  title: '쇼핑홈/랭킹',
  component: RankingSection,
  args: { products, onSelect: () => {} },
  tags: ['UIS-ORD-008'],
} satisfies Meta<typeof RankingSection>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — "인기" 탭, 응답 순서 그대로 상위 5(가정값). */
export const 인기탭: Story = {}

/** 탭 전환 — "가격"을 누르면 가격 오름차순 상위 5로 바뀐다(가정값, 최저가 상품이 1번으로 온다). */
export const 가격탭전환: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    await userEvent.click(canvas.getByRole('tab', { name: '가격' }))
    await expect(canvas.getByText('인기 4위 상품(최저가)')).toBeInTheDocument()
  },
}
