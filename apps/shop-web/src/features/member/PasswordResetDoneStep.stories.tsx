// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-007.md
import type { Meta, StoryObj } from '@storybook/react-vite'
import { PasswordResetDoneStep } from './PasswordResetDoneStep'

const meta = {
  title: '회원/비밀번호 재설정/4단계 완료',
  component: PasswordResetDoneStep,
  tags: ['UIS-MBR-003', 'FUNC-member-007'],
} satisfies Meta<typeof PasswordResetDoneStep>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 완료 문구 + 로그인 이동 링크. */
export const 기본: Story = {}
