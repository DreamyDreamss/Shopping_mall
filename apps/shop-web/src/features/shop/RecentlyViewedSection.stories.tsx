// SR-357 — 마이페이지 최근 본 상품 상태(기본/빈목록/로딩/조회실패). 전부 컨트롤드 props로 렌더한다
// (네트워크 없음, `ProductReviewsSection.stories.tsx`와 동일 수준, play function 없음 — 시각 스냅샷 대상).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { RecentlyViewedSection } from './RecentlyViewedSection'
import type { RecentlyViewedItem } from '../../types'

function viewed(i: number, overrides: Partial<RecentlyViewedItem> = {}): RecentlyViewedItem {
  return {
    sku: `SKU-recent-${i}`, productName: `최근 본 상품 ${i}`, price: 10000 * i, imageUrl: null,
    soldOut: false, viewedAt: '2026-09-27T10:00:00',
    ...overrides,
  }
}

const meta = {
  title: '마이페이지/최근 본 상품',
  component: RecentlyViewedSection,
  args: { onRetry: () => {} },
  tags: ['UIS-ORD-101'],
} satisfies Meta<typeof RecentlyViewedSection>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 3건, 품절 1건 포함, 대표 이미지 1건(그 외는 imageUrl 없음 플레이스홀더). */
export const 기본: Story = {
  tags: ['state:기본'],
  args: {
    status: 'ready',
    items: [
      viewed(1, { imageUrl: '/images/products/sku-1001.svg' }),
      viewed(2, { soldOut: true }),
      viewed(3),
    ],
  },
}

/** 빈목록 — "최근 본 상품이 없습니다" 안내(오류 상태와 문구·역할 구분). */
export const 빈목록: Story = { tags: ['state:빈'], args: { status: 'ready', items: [] } }

/** 로딩 — 목록 조회 진행 중. */
export const 로딩: Story = { tags: ['state:로딩'], args: { status: 'loading', items: [] } }

/** 조회실패 — "불러오지 못했습니다 — 다시 시도" 버튼. */
export const 조회실패: Story = { tags: ['state:오류'], args: { status: 'error', items: [] } }
