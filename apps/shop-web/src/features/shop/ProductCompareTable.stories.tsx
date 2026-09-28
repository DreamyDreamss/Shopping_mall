// SR-360 — 상품 비교 화면 상태(기본/가득/빈목록/조회실패). 전부 컨트롤드 props로 렌더한다(네트워크
// 없음, `RecentlyViewedSection.stories.tsx`와 동일 수준, play function 없음 — 시각 스냅샷 대상).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductCompareTable } from './ProductCompareTable'
import type { ProductCompareItem } from '../../types'

function compareItem(i: number, overrides: Partial<ProductCompareItem> = {}): ProductCompareItem {
  return {
    sku: `SKU-compare-${i}`, productName: `비교 상품 ${i}`, price: 10000 * i, listPrice: null,
    imageUrl: null, avgRating: 4.3, reviewCount: 12, stockQty: 10, soldOut: false,
    comparedAt: '2026-09-27T10:00:00',
    ...overrides,
  }
}

const meta = {
  title: '상품/상품 비교',
  component: ProductCompareTable,
  args: { onRemove: () => {}, onRetry: () => {} },
  tags: ['UIS-ORD-102'],
} satisfies Meta<typeof ProductCompareTable>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 2건, 정가·평점 있는 상품과 없는 상품을 함께 보여준다. */
export const 기본: Story = {
  tags: ['state:기본'],
  args: {
    status: 'ready',
    pendingSku: null,
    items: [
      compareItem(1, { listPrice: 12000 }),
      compareItem(2, { avgRating: null, reviewCount: 0, soldOut: true }),
    ],
  },
}

/** 가득 — 최대 4건, 열 4개가 전부 렌더되는지 확인. */
export const 가득: Story = {
  tags: ['state:가득'],
  args: {
    status: 'ready',
    pendingSku: null,
    items: [compareItem(1), compareItem(2), compareItem(3), compareItem(4)],
  },
}

/** 빈목록 — "비교함이 비어 있습니다 — 상품 상세에서 [비교함 담기]를 누르세요"(AC 원문). */
export const 빈목록: Story = { tags: ['state:빈'], args: { status: 'ready', items: [], pendingSku: null } }

/** 조회실패 — "비교함을 불러오지 못했습니다" + 다시 시도 버튼(AC 원문, 서버 상세 미노출). */
export const 조회실패: Story = { tags: ['state:오류'], args: { status: 'error', items: [], pendingSku: null } }
