// SR-311 — 앱 셸 하단 탭바 상태(로그인/비로그인 — '마이' 목적지만 갈린다).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { MemoryRouter } from 'react-router-dom'
import { BottomTabBar } from './BottomTabBar'
import type { SessionResult } from '../../types'

const session: SessionResult = {
  memberId: 'm-1', memberName: '홍길동', grade: 'NORMAL',
  apiKey: 'k-1', refreshToken: 'r-1', refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
}

const meta = {
  title: '쇼핑셸/BottomTabBar',
  component: BottomTabBar,
  tags: ['UIS-CMN-003'],
  decorators: [Story => <MemoryRouter initialEntries={['/shop']}><Story /></MemoryRouter>],
} satisfies Meta<typeof BottomTabBar>
export default meta

type Story = StoryObj<typeof meta>

/** 비로그인 — 홈·마이 2항목만 노출(ON AIR·카테고리는 QuickBar와 같은 설정값으로 숨김). */
export const 비로그인: Story = { args: { session: null } }

/** 로그인 — 항목 구성은 동일, '마이' 클릭 시 배송지 관리로 이동. */
export const 로그인: Story = { args: { session } }
