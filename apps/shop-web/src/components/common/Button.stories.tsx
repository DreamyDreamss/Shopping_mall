// SR-310 — Button 3종 × 상태별(기본·호버·포커스·비활성·로딩) 스토리. 호버·포커스는 정적 렌더로는
// CSS 의사클래스가 잡히지 않아 `play` 함수로 `userEvent.hover`/`element.focus()`를 실행해 실제
// 상태를 강제한다.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { userEvent, within } from 'storybook/test'
import { Button } from './Button'

const meta = {
  title: '공통/Button',
  component: Button,
  args: { children: '버튼' },
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof Button>
export default meta

type Story = StoryObj<typeof meta>

async function hoverButton({ canvasElement }: { canvasElement: HTMLElement }) {
  const canvas = within(canvasElement)
  await userEvent.hover(canvas.getByRole('button'))
}

function focusButton({ canvasElement }: { canvasElement: HTMLElement }) {
  const canvas = within(canvasElement)
  canvas.getByRole('button').focus()
}

// --- 주(primary) -----------------------------------------------------------
export const 주_기본: Story = { args: { variant: 'primary' } }
export const 주_호버: Story = { args: { variant: 'primary' }, play: hoverButton }
export const 주_포커스: Story = { args: { variant: 'primary' }, play: focusButton }
export const 주_비활성: Story = { args: { variant: 'primary', disabled: true } }
export const 주_로딩: Story = { args: { variant: 'primary', loading: true } }

// --- 보조(secondary) ---------------------------------------------------------
export const 보조_기본: Story = { args: { variant: 'secondary' } }
export const 보조_호버: Story = { args: { variant: 'secondary' }, play: hoverButton }
export const 보조_포커스: Story = { args: { variant: 'secondary' }, play: focusButton }
export const 보조_비활성: Story = { args: { variant: 'secondary', disabled: true } }
export const 보조_로딩: Story = { args: { variant: 'secondary', loading: true } }

// --- 구매(purchase, 그라데이션) -----------------------------------------------
export const 구매_기본: Story = { args: { variant: 'purchase' } }
export const 구매_호버: Story = { args: { variant: 'purchase' }, play: hoverButton }
export const 구매_포커스: Story = { args: { variant: 'purchase' }, play: focusButton }
export const 구매_비활성: Story = { args: { variant: 'purchase', disabled: true } }
export const 구매_로딩: Story = { args: { variant: 'purchase', loading: true } }
