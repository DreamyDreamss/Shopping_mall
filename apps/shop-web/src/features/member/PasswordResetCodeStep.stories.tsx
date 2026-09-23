// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-007.md
import type { Meta, StoryObj } from '@storybook/react-vite'
import { PasswordResetCodeStep } from './PasswordResetCodeStep'

const meta = {
  title: '회원/비밀번호 재설정/2단계 코드',
  component: PasswordResetCodeStep,
  args: { onCodeChange: () => {}, onNext: () => {}, onResend: () => {}, onRestart: () => {} },
  tags: ['UIS-MBR-003', 'FUNC-member-007'],
} satisfies Meta<typeof PasswordResetCodeStep>
export default meta

type Story = StoryObj<typeof meta>

const base = { target: 'user@example.com', channel: 'EMAIL' as const, code: '' }

/** 기본 — 카운트다운 진행중, 재전송은 쿨다운 중. */
export const 기본: Story = { tags: ['state:기본'],
  args: { ...base, remainingSeconds: 540, resendRemainingSeconds: 42 },
}

/** 재전송쿨다운끝 — 60초가 지나 재전송 버튼이 활성화된 상태. */
export const 재전송쿨다운끝: Story = {
  args: { ...base, remainingSeconds: 480, resendRemainingSeconds: 0 },
}

/** 코드오류 — 409 MBR-4102(코드 불일치), 입력은 그대로 남아 있다. */
export const 코드오류: Story = {
  args: {
    ...base, code: '000000', remainingSeconds: 300, resendRemainingSeconds: 0,
    error: { code: 'MBR-4102', message: '코드가 올바르지 않습니다' },
  },
}

/** 만료 — 410 MBR-4101, 코드 입력 대신 "다시 요청" 버튼만 보인다. */
export const 만료: Story = {
  args: {
    ...base, remainingSeconds: 0, resendRemainingSeconds: 0,
    error: { code: 'MBR-4101', message: '인증코드가 만료되었습니다. 다시 요청해 주세요' },
  },
}

/** 시도초과 — 409 MBR-4103, 만료와 동일한 "다시 요청" 흐름. */
export const 시도초과: Story = {
  args: {
    ...base, remainingSeconds: 120, resendRemainingSeconds: 0,
    error: { code: 'MBR-4103', message: '코드 확인 시도 횟수를 초과했습니다. 다시 요청해 주세요' },
  },
}
