// SR-304 — 상품 상세 탭 상태(기본(상세정보)·상품평(준비중)·상품문의(준비중)·탭전환(실제 클릭)).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { expect, userEvent, within } from 'storybook/test'
import { ProductDetailTabs } from './ProductDetailTabs'

const meta = {
  title: '쇼핑상세/상세 탭',
  component: ProductDetailTabs,
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ProductDetailTabs>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 상세정보 탭이 먼저 보인다("등록된 상세 설명이 없습니다"). */
export const 기본: Story = { tags: ['state:기본'], args: { initialTab: 'detail' } }

/** 상품평 — 데이터가 없어 "준비 중"만 보인다. */
export const 상품평: Story = { args: { initialTab: 'review' } }

/** 상품문의 — 데이터가 없어 "준비 중"만 보인다. */
export const 상품문의: Story = { args: { initialTab: 'inquiry' } }

/**
 * 탭전환 — 재작업(round 2) QA 권고 3/사람 지시 3: 위 3개 스토리는 `initialTab` prop(스토리북
 * 전용 뒷문)으로 상태를 세워, `onClick`의 `setTab` 자체가 깨져도 스토리북이 잡지 못했다(SR-302 #1
 * 거짓보증과 같은 클래스). 이 스토리는 `RankingSection.stories.tsx`의 "가격탭전환"과 같은 패턴으로
 * 탭 4개를 **실제 클릭**으로 전환하며 매번 `aria-selected`와 패널 문구가 정확히 바뀌는지 단언한다.
 */
export const 탭전환: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    const detailTab = canvas.getByRole('tab', { name: '상세정보' })
    const purchaseTab = canvas.getByRole('tab', { name: '구매정보' })
    const reviewTab = canvas.getByRole('tab', { name: '상품평' })
    const inquiryTab = canvas.getByRole('tab', { name: '상품문의' })

    // 기본 상태 — 상세정보가 선택돼 있다.
    await expect(detailTab).toHaveAttribute('aria-selected', 'true')
    await expect(canvas.getByRole('tabpanel')).toHaveTextContent('등록된 상세 설명이 없습니다.')

    await userEvent.click(purchaseTab)
    await expect(purchaseTab).toHaveAttribute('aria-selected', 'true')
    await expect(detailTab).toHaveAttribute('aria-selected', 'false')
    await expect(canvas.getByRole('tabpanel')).toHaveTextContent('배송·교환·환불 안내는 상품정보제공고시를 참고하세요.')

    await userEvent.click(reviewTab)
    await expect(reviewTab).toHaveAttribute('aria-selected', 'true')
    await expect(purchaseTab).toHaveAttribute('aria-selected', 'false')
    await expect(canvas.getByRole('tabpanel')).toHaveTextContent('준비 중입니다.')

    await userEvent.click(inquiryTab)
    await expect(inquiryTab).toHaveAttribute('aria-selected', 'true')
    await expect(reviewTab).toHaveAttribute('aria-selected', 'false')
    await expect(canvas.getByRole('tabpanel')).toHaveTextContent('준비 중입니다.')

    // 상세정보로 복귀 — 4개 탭 모두 왕복 전환이 가능함을 확인한다.
    await userEvent.click(detailTab)
    await expect(detailTab).toHaveAttribute('aria-selected', 'true')
    await expect(canvas.getByRole('tabpanel')).toHaveTextContent('등록된 상세 설명이 없습니다.')
  },
}
