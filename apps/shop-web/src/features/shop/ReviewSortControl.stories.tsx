// SR-351 — 상품평 정렬 상태(기본(최신순)). 각 정렬 클릭 시 onChange 인자를 단언한다(play).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { expect, fn, userEvent, within } from 'storybook/test'
import { ReviewSortControl } from './ReviewSortControl'

const meta = {
  title: '쇼핑상세/상품평/정렬',
  component: ReviewSortControl,
  args: { value: 'latest', onChange: fn() },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ReviewSortControl>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 최신순이 눌린 상태. 별점높은순/별점낮은순 클릭 시 각각 정확한 인자로 onChange가 불린다. */
export const 기본: Story = {
  tags: ['state:기본'],
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    await expect(canvas.getByRole('button', { name: '최신순' })).toHaveAttribute('aria-pressed', 'true')

    await userEvent.click(canvas.getByRole('button', { name: '별점높은순' }))
    await expect(args.onChange).toHaveBeenLastCalledWith('ratingDesc')

    await userEvent.click(canvas.getByRole('button', { name: '별점낮은순' }))
    await expect(args.onChange).toHaveBeenLastCalledWith('ratingAsc')
  },
}

/** 별점높은순선택 — value가 'ratingDesc'로 통제된 상태(버튼 눌림 표시 확인). */
export const 별점높은순선택: Story = { args: { value: 'ratingDesc' } }
