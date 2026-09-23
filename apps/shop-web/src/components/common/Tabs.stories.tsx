// SR-310 — Tabs 상태별(기본·스티키 고정) 스토리.
import { useState } from 'react'
import type { Meta, StoryObj } from '@storybook/react-vite'
import { Tabs } from './Tabs'

const sampleTabs = [
  { key: 'all', label: '전체' },
  { key: 'best', label: '베스트' },
  { key: 'new', label: '신상품' },
]

const meta = {
  title: '공통/Tabs',
  component: Tabs,
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof Tabs>
export default meta

type Story = StoryObj<typeof meta>

function Demo({ sticky }: { sticky?: boolean }) {
  const [activeKey, setActiveKey] = useState('all')
  return <Tabs tabs={sampleTabs} activeKey={activeKey} onChange={setActiveKey} sticky={sticky} />
}

const demoArgs = { tabs: sampleTabs, activeKey: 'all', onChange: () => {} } // render가 상태를 관리해 무시된다(타입 요건 충족용)

export const 기본: Story = { tags: ['state:기본'], args: demoArgs, render: () => <Demo /> }
export const 스티키고정: Story = { args: demoArgs, render: () => <Demo sticky /> }

// SR-311 round2 재작업 지시 3 — 탭 라벨 위 점(빨간 라벨) 마커를 badge:true 탭으로 실물 검증한다.
const badgeTabs = [
  { key: 'all', label: '전체' },
  { key: 'best', label: '베스트', badge: true },
  { key: 'new', label: '신상품' },
]
export const 배지있는탭: Story = {
  args: demoArgs,
  render: () => <Tabs tabs={badgeTabs} activeKey="all" onChange={() => {}} />,
}
