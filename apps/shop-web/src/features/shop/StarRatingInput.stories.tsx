// SR-351 — 별점 선택 상태(기본(미선택)·선택됨·비활성).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { expect, fn, userEvent, within } from 'storybook/test'
import { StarRatingInput } from './StarRatingInput'

const meta = {
  title: '쇼핑상세/상품평/별점 선택',
  component: StarRatingInput,
  args: { onChange: fn() },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof StarRatingInput>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 미선택(0점). 3번째 별을 클릭하면 onChange(3)이 호출된다. */
export const 기본: Story = {
  tags: ['state:기본'],
  args: { value: 0 },
  play: async ({ canvasElement, args }) => {
    const canvas = within(canvasElement)
    await userEvent.click(canvas.getByRole('button', { name: '별점 3점' }))
    await expect(args.onChange).toHaveBeenCalledWith(3)
  },
}

/** 선택됨 — 4점이 이미 선택된 상태. */
export const 선택됨: Story = { args: { value: 4 } }

/** 비활성 — 제출 중이라 클릭할 수 없다. */
export const 비활성: Story = { args: { value: 4, disabled: true } }
