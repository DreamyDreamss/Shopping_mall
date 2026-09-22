// SR-302 — 추천 상품 그리드 상태(목록있음·빈목록·로딩·조회실패).
// SR-306 — "목록있음"의 앞 4행을 실 SKU(1001~1004, DB 실측 2026-09-17)로 교체해 할인·정가없음·
// 할인0%·이미지없음이 그리드 안에서 섞여 보이는 상태를 보여준다. 나머지 행은 listPrice/imageUrl null.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductGrid } from './ProductGrid'
import type { Product } from '../../types'

const realRows: Product[] = [
  { sku: 'SKU-1001', productName: '스탠딩 데스크', price: 390000, stockQty: 12, saleYn: 'Y', listPrice: 450000, imageUrl: '/images/products/sku-1001.svg' },
  { sku: 'SKU-1002', productName: '기계식 키보드', price: 129000, stockQty: 20, saleYn: 'Y', listPrice: null, imageUrl: '/images/products/sku-1002.svg' },
  { sku: 'SKU-1003', productName: '4K 모니터', price: 450000, stockQty: 8, saleYn: 'Y', listPrice: 450000, imageUrl: '/images/products/sku-1003.svg' },
  { sku: 'SKU-1004', productName: '단종 마우스', price: 35000, stockQty: 0, saleYn: 'N', listPrice: 42000, imageUrl: null },
]

const rows: Product[] = [
  ...realRows,
  ...Array.from({ length: 6 }, (_, i) => ({
    sku: `sku-${i + 4}`, productName: `상품 ${i + 5}`, price: 10000 + i * 1500,
    stockQty: i === 3 ? 0 : 8, saleYn: 'Y', listPrice: null, imageUrl: null,
  })),
]

const meta = {
  title: '쇼핑홈/추천 상품 그리드',
  component: ProductGrid,
  args: { onSelect: () => {}, onRetry: () => {} },
  tags: ['UIS-ORD-008'],
} satisfies Meta<typeof ProductGrid>
export default meta

type Story = StoryObj<typeof meta>

/** 목록있음 — 10건 중 상위 8건만(가정값). 4번째 품절 상품은 배지+흐림. */
export const 목록있음: Story = { args: { rows } }

/** 빈목록 — "표시할 상품이 없습니다" + 카테고리 안내. */
export const 빈목록: Story = { args: { rows: [] } }

/** 로딩 — 그리드 대신 진행 표시. */
export const 로딩: Story = { args: { rows: [], loading: true } }

/** 조회실패 — 문구 + [다시 시도]만 보인다(서버 계약은 그대로, 확정 답변 api_error). */
export const 조회실패: Story = { args: { rows: [], error: '500 Internal Server Error' } }
