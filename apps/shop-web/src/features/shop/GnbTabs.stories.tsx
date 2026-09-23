// SR-311 — 앱 셸 GNB 탭 상태(현재는 '홈' 1개만 노출).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { MemoryRouter } from 'react-router-dom'
import { GnbTabs } from './GnbTabs'

const meta = {
  title: '쇼핑셸/GnbTabs',
  component: GnbTabs,
  tags: ['UIS-CMN-003'],
  decorators: [Story => <MemoryRouter initialEntries={['/shop']}><Story /></MemoryRouter>],
} satisfies Meta<typeof GnbTabs>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 도착 화면이 있는 '홈'만 탭으로 보인다(나머지 8개는 AC대로 숨김). */
export const 기본: Story = { tags: ['state:기본'],}
