// SR-361 — 상품 상세 화면 최근 상품평 미리보기 상태(빈목록·한건·세건·조회실패, 확정 답변의 스토리
// 목록 그대로). "세건" 스토리는 play function으로 실제 렌더 개수를 세어(서버가 5건을 줘도 3개만) 슬라이스
// 로직이 깨져도 통과해버리는 거짓보증을 피한다(SR-353/SR-310 사례 대조).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { expect, fn, within } from 'storybook/test'
import { ProductReviewPreview } from './ProductReviewPreview'
import type { ReviewListResult } from '../../types'

function reviewListResult(overrides: Partial<ReviewListResult> = {}): ReviewListResult {
  return {
    totalCount: 0, page: 1, size: 10, avgRating: null, reviewCount: 0,
    distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 }, items: [], ...overrides,
  }
}

const meta = {
  title: '쇼핑상세/상품평/최근 상품평 미리보기',
  component: ProductReviewPreview,
  args: { listStatus: 'ready', listResult: reviewListResult(), onRetry: fn(), onViewAll: fn() },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ProductReviewPreview>
export default meta

type Story = StoryObj<typeof meta>

/** 빈목록 — 상품평 0건. "아직 상품평이 없습니다" + [리뷰 전체 보기] 버튼은 그대로 보인다. */
export const 빈목록: Story = {
  tags: ['state:빈'],
  args: { listResult: reviewListResult() },
}

/** 한건 — 상품평 1건만. */
export const 한건: Story = {
  args: {
    listResult: reviewListResult({
      totalCount: 1, reviewCount: 1, avgRating: 5,
      items: [{ reviewId: 1, rating: 5, content: '배송이 빠르고 품질도 좋았습니다', authorName: '홍*동', createdAt: '2026-09-20T00:00:00Z' }],
    }),
  },
}

/** 세건 — 서버가 5건을 돌려줘도 미리보기는 정확히 3건만 렌더한다(play function으로 개수 실측). */
export const 세건: Story = {
  args: {
    listResult: reviewListResult({
      totalCount: 5, reviewCount: 5, avgRating: 4.2,
      items: Array.from({ length: 5 }, (_, i) => ({
        reviewId: i + 1, rating: 4, content: `상품평 본문 ${i + 1}번째 테스트 내용입니다`,
        authorName: '김*수', createdAt: `2026-09-${10 + i}T00:00:00Z`,
      })),
    }),
  },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    const items = await canvas.findAllByRole('listitem')
    await expect(items).toHaveLength(3)
  },
}

/** 조회실패 — "상품평을 불러오지 못했습니다" + [다시 시도]. */
export const 조회실패: Story = {
  tags: ['state:오류'],
  args: { listStatus: 'error', listResult: null },
}
