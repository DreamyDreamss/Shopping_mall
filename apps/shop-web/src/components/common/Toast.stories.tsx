// SR-310 — Toast(단일 프레젠테이션) 스토리. 최대 3개·초과 시 제거 상태는 스택을 다루는
// ToastStack.stories.tsx가 담당한다.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { Toast } from './Toast'

const meta = {
  title: '공통/Toast',
  component: Toast,
  args: { message: '장바구니에 담았습니다', onDismiss: () => {} },
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof Toast>
export default meta

type Story = StoryObj<typeof meta>

export const 기본: Story = {}
export const 성공: Story = { args: { variant: 'success', message: '주문이 완료되었습니다' } }
export const 오류: Story = { args: { variant: 'error', message: '처리 중 오류가 발생했습니다' } }
