// linked_func: FUNC-member-004
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md
import type { Meta, StoryObj } from '@storybook/react-vite'
import { expect, userEvent, within } from 'storybook/test'
import { LoginForm } from './LoginForm'

const meta = {
  title: '회원/로그인',
  component: LoginForm,
  args: { onChange: () => {}, onSubmit: () => {} },
  tags: ['UIS-MBR-002', 'FUNC-member-004'],
} satisfies Meta<typeof LoginForm>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 빈 입력. */
export const 기본: Story = { args: { value: { email: '', password: '' } } }

/** 입력됨 — 이메일·비밀번호를 채운 상태. */
export const 입력됨: Story = { args: { value: { email: 'user@example.com', password: 'abcd1234' } } }

/** 제출 중 — 버튼이 잠긴다(중복 제출 방지). */
export const 제출중: Story = {
  args: { value: { email: 'user@example.com', password: 'abcd1234' }, busy: true },
}

/** 실패401 — 서버 문구를 그대로("(n/5)" 포함, INF-MBR-003 존재 오라클 방지 문구 재구현 금지). */
export const 실패401: Story = {
  args: {
    value: { email: 'user@example.com', password: 'wrong' },
    error: { code: 'MBR-4011', message: '이메일 또는 비밀번호가 올바르지 않습니다 (3/5)' },
  },
}

/**
 * 잠금429 — 서버의 완곡 문구("...잠시 후 다시 시도해 주세요")만 표시되고, `retryAfterSeconds`가
 * 다 지날 때까지 제출이 막힌다(INF-MBR-003, 5회 실패). round2 QA 재작업 — 정확한 초는 더 이상
 * 화면에 노출하지 않는다(완곡화 요건과 상충 방지, `retryAfterSeconds`는 버튼 비활성 타이머로만 쓰인다).
 */
export const 잠금429: Story = {
  args: {
    value: { email: 'user@example.com', password: 'wrong' },
    error: { code: 'MBR-4291', message: '로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요', retryAfterSeconds: 600 },
  },
}

/** 비밀번호토글 — 클릭 시 input[type=password] → text로 바뀐다. */
export const 비밀번호토글: Story = {
  args: { value: { email: '', password: 'abcd1234' } },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    const input = canvas.getByLabelText('비밀번호', { selector: 'input' }) as HTMLInputElement
    await expect(input.type).toBe('password')
    await userEvent.click(canvas.getByRole('button', { name: '보기' }))
    await expect(input.type).toBe('text')
  },
}
