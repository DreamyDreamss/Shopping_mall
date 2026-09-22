// SR-302 — GNB 상태(비로그인·로그인·장바구니 수량 0/N).
// SR-311 round2 — 검색 아이콘 버튼이 `useNavigate()`를 쓰므로(재작업 지시 1) 스토리에도 Router 컨텍스트가
// 필요하다.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { MemoryRouter } from 'react-router-dom'
import { Gnb } from './Gnb'
import type { SessionResult } from '../../types'

const session: SessionResult = {
  memberId: 'm-1', memberName: '홍길동', grade: 'NORMAL',
  apiKey: 'k-1', refreshToken: 'r-1', refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
}

const meta = {
  title: '쇼핑홈/GNB',
  component: Gnb,
  args: { searchValue: '', onSearchChange: () => {}, onSearchSubmit: () => {}, onLogout: () => {} },
  tags: ['UIS-ORD-008'],
  decorators: [Story => <MemoryRouter initialEntries={['/shop']}><Story /></MemoryRouter>],
} satisfies Meta<typeof Gnb>
export default meta

type Story = StoryObj<typeof meta>

/** 비로그인 — 로그인 링크, 장바구니 수량 0(배지 없음). */
export const 비로그인: Story = { args: { session: null, cartItemCount: 0 } }

/** 로그인 — 회원명+로그아웃, 장바구니 수량 0. */
export const 로그인: Story = { args: { session, cartItemCount: 0 } }

/** 장바구니 담김 — 로그인 상태에서 수량 배지(N)가 보인다. */
export const 장바구니담김: Story = { args: { session, cartItemCount: 5 } }
