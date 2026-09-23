// linked_func: FUNC-order-001 — 검색 조건의 상태들
import type { Meta, StoryObj } from '@storybook/react-vite'
import { OrderFilters } from './OrderFilters'
import { EMPTY_QUERY } from '../types'

const meta = {
  title: '주문/검색 조건',
  component: OrderFilters,
  args: { onChange: () => {}, onSearch: () => {} },
  tags: ['UIS-ORD-001', 'FUNC-order-001'],
} satisfies Meta<typeof OrderFilters>
export default meta

type Story = StoryObj<typeof meta>

/** 빈 조건 — 모두 선택 입력이라 이대로 검색하면 전체 조회다(막지 않는다). */
export const 빈조건: Story = { args: { value: EMPTY_QUERY } }

/** 조건을 채운 상태 — 입력한 것끼리 AND로 결합된다. */
export const 조건입력: Story = {
  args: { value: { memberId: 'm001', orderState: 'SHIPPING', startDate: '2026-08-01', endDate: '2026-08-31' } },
}

/** 기간 역전 — 서버에 보내기 전에 화면이 막고 사유를 말한다. */
export const 기간역전: Story = {
  args: {
    value: { ...EMPTY_QUERY, startDate: '2026-08-31', endDate: '2026-08-01' },
    invalid: '시작일이 종료일보다 늦습니다',
  },
}

/** 조회 중 — 버튼이 잠긴다(중복 조회 방지). */
export const 조회중: Story = { tags: ['state:로딩'], args: { value: EMPTY_QUERY, busy: true } }
