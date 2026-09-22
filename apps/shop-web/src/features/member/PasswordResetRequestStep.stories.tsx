// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-006.md
import type { Meta, StoryObj } from '@storybook/react-vite'
import { PasswordResetRequestStep } from './PasswordResetRequestStep'

const meta = {
  title: '회원/비밀번호 재설정/1단계 요청',
  component: PasswordResetRequestStep,
  args: { onChange: () => {}, onSubmit: () => {} },
  tags: ['UIS-MBR-003', 'FUNC-member-007'],
} satisfies Meta<typeof PasswordResetRequestStep>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 빈 입력. */
export const 기본: Story = { args: { value: '' } }

/** 입력됨 — 이메일을 채운 상태. */
export const 입력됨: Story = { args: { value: 'user@example.com' } }

/** 제출 중 — 버튼이 잠긴다(중복 제출 방지). */
export const 제출중: Story = { args: { value: 'user@example.com', busy: true } }

/** 형식오류 — 400 MBR-4100(이메일·휴대폰 형식 불일치). */
export const 형식오류: Story = {
  args: {
    value: 'not-a-valid-target',
    error: { code: 'MBR-4100', message: '이메일 또는 휴대폰번호 형식이 올바르지 않습니다' },
  },
}
