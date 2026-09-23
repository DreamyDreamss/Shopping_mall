// linked_func: FUNC-order-001 — 주문 목록 그리드의 상태들
// 각 스토리는 UIS-ORD-001 §5(접근 권한·표시 조건)의 한 줄에 대응한다.
// 이 대응이 Speclinker가 문서↔실물을 잇는 근거다 — 이름을 바꾸면 링크도 같이 갱신해야 한다.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { OrderTable } from './OrderTable'
import type { OrderRow } from '../types'

const rows: OrderRow[] = [
  { orderNo: '20260816-0001', memberId: 'm001', memberName: '김주문', orderState: 'DONE',
    totalAmount: 128000, orderedAt: '2026-08-16 10:22', deliveryState: 'DELIVERED' },
  { orderNo: '20260816-0002', memberId: 'm002', memberName: '이배송', orderState: 'SHIPPING',
    totalAmount: 43900, orderedAt: '2026-08-16 11:03', deliveryState: 'READY' },
  { orderNo: '20260816-0003', memberId: 'm003', memberName: null, orderState: 'PAID',
    totalAmount: 7900, orderedAt: '2026-08-16 12:41', deliveryState: null },
]

const meta = {
  title: '주문/주문 목록 그리드',
  component: OrderTable,
  // Speclinker 링크 — index.json에 실리는 것은 tags뿐이라(parameters는 안 실린다)
  // 스펙 ID를 태그로 단다. 새 문법을 만들지 않고 기존 ID를 그대로 쓴다.
  tags: ['UIS-ORD-001', 'FUNC-order-001'],
} satisfies Meta<typeof OrderTable>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 3건. 배송 이력이 없는 건은 배송상태가 "-"다. */
export const 목록있음: Story = { tags: ['state:기본'], args: { rows } }

/** 0건 — "조회 결과 없음". 오류와 **다른 사실**이다. */
export const 결과없음: Story = { tags: ['state:빈'], args: { rows: [] } }

/** 조회 중 — 그리드 대신 진행 표시. */
export const 조회중: Story = { tags: ['state:로딩'], args: { rows: [], loading: true } }

/**
 * 조회 실패(SR-208) — 그리드·"조회 결과 없음"·"전체 N건"을 **모두 숨긴다**.
 * 오류인데 "0건"이라고 말하면 사람은 "조건에 맞는 게 없구나"로 읽는다.
 */
export const 조회실패: Story = { tags: ['state:오류'], args: { rows: [], error: '500 Internal Server Error' } }

/** 탈퇴 회원(SR-221) — 회원명이 비어도 주문이 사라지지 않고 "(탈퇴)"로 표시된다. */
export const 탈퇴회원포함: Story = { args: { rows: [rows[2]] } }

/** 알 수 없는 배송 코드 — 조용히 감추지 않고 코드를 그대로 보여 준다. */
export const 미지의배송코드: Story = {
  args: { rows: [{ ...rows[0], deliveryState: 'RETURNED' as OrderRow['deliveryState'] }] },
}

/** 선택 행 강조 — 사용자가 고른 행이 눈에 띈다(SR-228). 선택이 없으면 목록있음과 같다 */
export const 선택행강조: Story = { args: { rows, selectedOrderNo: rows[1].orderNo } }
