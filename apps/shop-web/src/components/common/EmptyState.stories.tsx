// SR-310 — EmptyState 스토리.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { EmptyState } from './EmptyState'

const meta = {
  title: '공통/EmptyState',
  component: EmptyState,
  args: { message: '장바구니가 비어 있습니다', actionLabel: '쇼핑 계속하기', onAction: () => {} },
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof EmptyState>
export default meta

type Story = StoryObj<typeof meta>

export const 기본: Story = { tags: ['state:기본', 'state:빈'],}

/** bench/screenshot-10 구성 그대로 — 하단 추천 레일 슬롯까지 채운 상태. */
export const 추천레일포함: Story = {
  args: {
    railSlot: (
      <div style={{ display: 'flex', gap: 8 }}>
        <div style={{ width: 100, height: 100, background: 'var(--color-surface-1)', borderRadius: 8 }} />
        <div style={{ width: 100, height: 100, background: 'var(--color-surface-1)', borderRadius: 8 }} />
      </div>
    ),
  },
}
