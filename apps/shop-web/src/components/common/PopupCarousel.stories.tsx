// SR-310 — PopupCarousel 상태별(1/5·마지막·오늘은 그만 보기) 스토리. 슬라이드는 실제 이미지가
// 아니라 배경색 블록(합성 데이터) — ProductCard.stories.tsx의 "이미지로드실패" 사례와 같은 이유로
// 실제 이미지 경로를 쓰지 않는다(범위 밖 절 참조).
//
// round1 QA 재작업(low) — 자동 넘김(기본 4초)이 스토리북에서도 돌아 '마지막' 상태가 시간에 따라
// 바뀌는 문제를 `autoAdvanceMs: 0`(전 스토리 공통 args)으로 정지해 없앤다. '오늘은그만보기'
// 스토리는 `play`로 실제 옵션 버튼을 클릭해 체크된 상태(dismissChecked)를 시각적으로 구별한다.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { userEvent, within } from 'storybook/test'
import { PopupCarousel } from './PopupCarousel'

const slides = [
  { id: '1', background: 'var(--gradient-primary)' },
  { id: '2', background: 'var(--gradient-accent)' },
  { id: '3', background: 'var(--color-primary-bg)' },
  { id: '4', background: 'var(--color-info-bg)' },
  { id: '5', background: 'var(--color-warning-bg)' },
]

const meta = {
  title: '공통/PopupCarousel',
  component: PopupCarousel,
  args: { open: true, slides, onClose: () => {}, onDismissToday: () => {}, autoAdvanceMs: 0 },
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof PopupCarousel>
export default meta

type Story = StoryObj<typeof meta>

/** 1/5 — 첫 슬라이드로 시작. */
export const 첫번째: Story = {}

/** 마지막 — 5장 중 마지막(5/5) 슬라이드로 시작. 자동 넘김을 꺼서(autoAdvanceMs: 0) 시간에 따라
 *  바뀌지 않는다. */
export const 마지막: Story = { args: { initialIndex: 4 } }

/** 오늘은 그만 보기 — 옵션 버튼을 실제로 클릭해(play) 체크된 상태(dismissChecked)를 시각적으로
 *  구별한다(클릭 시 onClose 뒤 onDismissToday를 각각 호출 — 이 스토리는 둘 다 no-op이라 팝업이
 *  닫히지 않고 체크된 모습이 유지된다). */
export const 오늘은그만보기: Story = {
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    await userEvent.click(canvas.getByRole('button', { name: /오늘은 그만 보기/ }))
  },
}
