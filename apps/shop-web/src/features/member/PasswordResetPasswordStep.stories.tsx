// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-007.md
import type { Meta, StoryObj } from '@storybook/react-vite'
import { PasswordResetPasswordStep } from './PasswordResetPasswordStep'

const meta = {
  title: '회원/비밀번호 재설정/3단계 새 비밀번호',
  component: PasswordResetPasswordStep,
  args: { onChange: () => {}, onSubmit: () => {} },
  tags: ['UIS-MBR-003', 'FUNC-member-007'],
} satisfies Meta<typeof PasswordResetPasswordStep>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 빈 입력. */
export const 기본: Story = { tags: ['state:기본'], args: { value: '' } }

/** 제출 중 — 버튼이 잠긴다(중복 제출 방지). */
export const 제출중: Story = { tags: ['state:로딩'], args: { value: 'newPass123', busy: true } }

/** 비밀번호규칙오류 — 400 MBR-4001(8~64자, 영문+숫자 포함 규칙 위반). */
export const 비밀번호규칙오류: Story = {
  args: {
    value: 'short',
    error: { code: 'MBR-4001', message: '비밀번호 형식이 올바르지 않습니다' },
  },
}
