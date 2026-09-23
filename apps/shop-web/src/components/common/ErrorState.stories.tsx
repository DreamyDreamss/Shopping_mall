// SR-310 — ErrorState 스토리.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ErrorState } from './ErrorState'

const meta = {
  title: '공통/ErrorState',
  component: ErrorState,
  args: {
    subMessage: '요청하신 페이지를 찾을 수 없습니다',
    onHome: () => {},
    onBack: () => {},
  },
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof ErrorState>
export default meta

type Story = StoryObj<typeof meta>

export const 기본: Story = { tags: ['state:기본'],}
