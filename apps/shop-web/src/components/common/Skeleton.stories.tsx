// SR-310 — Skeleton 상태별(카드·리스트) 스토리.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { Skeleton } from './Skeleton'

const meta = {
  title: '공통/Skeleton',
  component: Skeleton,
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof Skeleton>
export default meta

type Story = StoryObj<typeof meta>

export const 카드: Story = { args: { variant: 'card' } }
export const 리스트: Story = { args: { variant: 'list' } }
