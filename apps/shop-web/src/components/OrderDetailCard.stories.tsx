// linked_func: FUNC-order-002 — 주문 상세의 상태들
import type { Meta, StoryObj } from '@storybook/react-vite'
import { OrderDetailCard } from './OrderDetailCard'
import type { OrderDetail } from '../types'

const base: OrderDetail = {
  orderNo: '20260816-0001', memberId: 'm001', memberName: '김주문', orderState: 'DONE',
  totalAmount: 128000, orderedAt: '2026-08-16 10:22', deliveryState: 'DELIVERED',
  items: [
    { productNo: 'p001', productName: '무선 마우스', quantity: 2, unitPrice: 24000 },
    { productNo: 'p002', productName: '기계식 키보드', quantity: 1, unitPrice: 80000 },
  ],
  deliveries: [
    { deliveryNo: 'D-002', state: 'DELIVERED', shippedAt: '2026-08-17 09:10' },
    { deliveryNo: 'D-001', state: 'SHIPPED', shippedAt: '2026-08-16 18:40' },
  ],
}

const meta = {
  title: '주문/주문 상세',
  component: OrderDetailCard,
  tags: ['UIS-ORD-002', 'FUNC-order-002'],
} satisfies Meta<typeof OrderDetailCard>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 품목 2건 + 배송 이력 2건(출고일시 DESC). */
export const 기본: Story = { args: { order: base } }

/**
 * 미출고 섞임 — `shippedAt`이 null인 건은 **맨 뒤**로 간다.
 * 이 화면의 정렬 기준(출고일시)은 주문 목록의 "최신"(배송번호) 기준과 다르며, 그건 의도된 것이다.
 */
export const 미출고섞임: Story = {
  args: {
    order: {
      ...base,
      deliveries: [
        { deliveryNo: 'D-003', state: 'READY', shippedAt: null },
        ...base.deliveries,
      ],
    },
  },
}

/** 배송 이력 없음 — "배송 이력이 없습니다"(빈 목록을 그리지 않는다). */
export const 배송이력없음: Story = { args: { order: { ...base, deliveries: [], deliveryState: null } } }

/** 품목 없음 — 데이터 이상이지만 화면이 죽지 않아야 한다. */
export const 품목없음: Story = { args: { order: { ...base, items: [] } } }

/** 탈퇴 회원 — 회원명이 비어도 주문은 보인다(SR-221). */
export const 탈퇴회원: Story = { args: { order: { ...base, memberName: null } } }

/** 불러오는 중. */
export const 로딩: Story = { args: { order: null, loading: true } }

/** 조회 실패 — 사유를 그대로 싣는다. */
export const 조회실패: Story = { args: { order: null, error: '404 Not Found' } }

/** 주문 없음 — 오류가 아니라 "찾을 수 없습니다". */
export const 주문없음: Story = { args: { order: null } }

/** 조회 실패 + 다시 시도 — 버튼이 보이고 onRetry를 부른다(SR-229) */
export const 조회실패다시시도: Story = { args: { order: null, error: '503 Service Unavailable', onRetry: () => {} } }
