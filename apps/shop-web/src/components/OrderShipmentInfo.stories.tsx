// LAB-105.1 — 주문 상세 "배송 정보" 블록의 상태들.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { expect, within } from 'storybook/test'
import { OrderShipmentInfo } from './OrderShipmentInfo'
import type { OrderShipment } from '../types'

const meta = {
  title: '주문/배송 정보',
  component: OrderShipmentInfo,
  tags: ['UIS-ORD-002'],
} satisfies Meta<typeof OrderShipmentInfo>
export default meta

type Story = StoryObj<typeof meta>

/**
 * 불러오지 못함 — round2 재작업 지시 1(round1 QA FAIL 필수수정 1, AC 68). `shipments`가
 * `undefined`(응답에 그 키 자체가 없음 — 정보 부재)이면 "출고 준비 중"(빈 배열의 문구)이 아니라
 * "배송 정보를 불러오지 못했습니다"를 보인다. play로 문구 존재를 단언한다(사례집 55행 — 단언 없는
 * 스토리는 렌더만 확인된다는 교훈 반영).
 */
export const 불러오지못함: Story = {
  tags: ['state:오류'],
  args: { shipments: undefined },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    await expect(canvas.getByText('배송 정보를 불러오지 못했습니다')).toBeInTheDocument()
  },
}

/** 출고 전 — shipments 빈 배열이면 "출고 준비 중" 한 줄만(AC 66). */
export const 출고전: Story = { tags: ['state:빈'], args: { shipments: [] } }

/**
 * 단건 출고 — 택배사명·송장번호·복사 버튼·[배송 조회] 링크가 함께 보인다(AC 63·64).
 * 복사 버튼은 invoiceNo가 있는 행에서만 렌더된다(게이트 회신 반영 — 사람 수정 ①).
 */
export const 단건출고: Story = {
  args: {
    shipments: [
      { deliveryNo: 'D-0815-1', carrierCode: 'CJ', carrierName: 'CJ대한통운', invoiceNo: 'INV-88010', shippedAt: '2026-08-15 18:00' },
    ] as OrderShipment[],
  },
}

/**
 * 부분 출고 2건 — 한 건은 송장 등록 완료(복사 버튼·링크 있음), 한 건은 "송장 등록 대기"
 * (복사 버튼·링크 없음, AC 67).
 */
export const 부분출고2건: Story = {
  args: {
    shipments: [
      { deliveryNo: 'D-0816-1', carrierCode: 'CJ', carrierName: 'CJ대한통운', invoiceNo: 'INV-88031', shippedAt: '2026-08-17 08:20' },
      { deliveryNo: 'D-0816-2', carrierCode: null, carrierName: null, invoiceNo: null, shippedAt: null },
    ] as OrderShipment[],
  },
}
