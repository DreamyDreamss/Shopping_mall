// SR-305 — 주문 완료 상태.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { OrderCompleteNotice } from './OrderCompleteNotice'

const meta = {
  title: '주문서/주문 완료',
  component: OrderCompleteNotice,
  tags: ['UIS-ORD-011'],
} satisfies Meta<typeof OrderCompleteNotice>
export default meta

type Story = StoryObj<typeof meta>

// 재작업(round 2, QA FAIL 필수2) — `totalAmount: 55000`은 `CartSummary.전체선택`의
// `productAmount: 55000`과 값을 맞춘 것이다(우연이 아니라 의도). 두 스토리 모두
// `calcCartTotals`/`calcTotalsFromProductAmount`를 거쳐 배송비 3,000원 + 결제금액 58,000원으로
// 같은 숫자를 보여준다 — 예전엔 `CartSummary.전체선택.payableAmount: 58000`과 이 스토리의
// `totalAmount: 58000`이 "우연히" 같아 보여 3,000원 어긋난 계산 모순이 드러나지 않았다.
/** 주문완료 — 주문번호와 다음 행동(주문 내역 보기·쇼핑 계속하기). 상품금액/배송비/결제금액이
 * `CartSummary.전체선택`의 결제예정금액과 같은 숫자를 말한다(58,000원). */
export const 주문완료: Story = { args: { orderNo: '20260917-000001', totalAmount: 55000, itemCount: 2 } }
