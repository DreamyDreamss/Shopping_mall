// SR-311 — 앱 셸 우측 레일 상태(최근 본 상품 0/3개). `RightRail`은 이 코드베이스에서 처음으로 자체
// `useEffect`로 데이터를 불러오는 feature 부품이다(STORY "데이터" 절 — 화면 본문과 별개 호출). MSW
// 등 네트워크 목 도구가 이 프로젝트에 없어, 스토리는 `window.fetch`를 렌더 시점(자식 effect가 돌기
// 전)에 동기적으로 교체해 결정적인 상품 목록을 흘려보낸다 — `recentlyViewed` 키도 같은 시점에 채운다.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { RightRail } from './RightRail'
import { recordViewed } from './recentlyViewedStorage'
import type { Product } from '../../types'

const products: Product[] = [
  { sku: 'sku-rail-1', productName: '레일 상품 1', price: 39000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'sku-rail-2', productName: '레일 상품 2', price: 59000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'sku-rail-3', productName: '레일 상품 3', price: 79000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
]

function withFixture(viewedSkus: string[]) {
  return (Story: () => React.ReactElement) => {
    localStorage.clear()
    viewedSkus.forEach(sku => recordViewed(sku))
    window.fetch = (async () => ({ ok: true, status: 200, statusText: '', json: async () => products })) as unknown as typeof fetch
    return <Story />
  }
}

const meta = {
  title: '쇼핑셸/RightRail',
  component: RightRail,
  tags: ['UIS-CMN-003'],
} satisfies Meta<typeof RightRail>
export default meta

type Story = StoryObj<typeof meta>

/** 최근 본 상품 3개 — 그라데이션 헤더 카드. 쿠폰/주문 카드는 이번 SR에서 `entitlementCounts`를
 * 아무도 채우지 않아 항상 없다(STORY "계약" 절). */
export const 최근본상품_3개: Story = {
  decorators: [withFixture(['sku-rail-1', 'sku-rail-2', 'sku-rail-3'])],
}

/** 최근 본 상품 0개(+쿠폰/주문 카드도 없음) — 아무것도 그리지 않는 것이 정답인 상태. */
export const 비어있음: Story = {
  decorators: [withFixture([])],
  tags: ['renders-nothing'],
}
