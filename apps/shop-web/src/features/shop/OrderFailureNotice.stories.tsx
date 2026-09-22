// SR-305 — 결제 실패 안내(400/404/409는 서버 message 그대로, 그 외는 고정 안내).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { OrderFailureNotice } from './OrderFailureNotice'

const meta = {
  title: '주문서/결제 실패 안내',
  component: OrderFailureNotice,
  tags: ['UIS-ORD-011', 'shows-error'],
} satisfies Meta<typeof OrderFailureNotice>
export default meta

type Story = StoryObj<typeof meta>

/** 빈장바구니 — 400. */
export const 빈장바구니400: Story = { args: { status: 400, message: '장바구니가 비어 있습니다' } }

/** 회원없음 — 404. */
export const 회원없음404: Story = { args: { status: 404, message: '회원 없음: m-1' } }

/** 재고부족 — 409. */
export const 재고부족409: Story = { args: { status: 409, message: '재고 부족: sku-1(가용0/요청2)' } }

/** 미정의상태 — 5xx 등은 서버 원문 대신 고정 안내. */
export const 일시적오류: Story = { args: { status: 500, message: 'Internal error at com.sm.lab...' } }
