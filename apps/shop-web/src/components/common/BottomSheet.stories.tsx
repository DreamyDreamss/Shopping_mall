// SR-310 — BottomSheet 상태별(열림·닫힘) 스토리.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { BottomSheet } from './BottomSheet'

const meta = {
  title: '공통/BottomSheet',
  component: BottomSheet,
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof BottomSheet>
export default meta

type Story = StoryObj<typeof meta>

export const 열림: Story = {
  args: {
    open: true,
    onClose: () => {},
    children: <div style={{ padding: 'var(--space-2) 0' }}>구매 시트 내용</div>,
  },
}

export const 닫힘: Story = {
  tags: ['renders-nothing'],
  args: {
    open: false,
    onClose: () => {},
    children: <div>구매 시트 내용</div>,
  },
}
