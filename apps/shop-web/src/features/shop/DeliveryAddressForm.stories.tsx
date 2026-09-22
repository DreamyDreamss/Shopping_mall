// SR-305 — 주문서 배송지 입력 상태들(기본/입력완료/필드별 오류).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { DeliveryAddressForm } from './DeliveryAddressForm'

const meta = {
  title: '주문서/배송지 입력',
  component: DeliveryAddressForm,
  args: {
    recipientName: '', phone: '', zipcode: '', roadAddress: '', detailAddress: '', errors: {},
    onRecipientNameChange: () => {}, onPhoneChange: () => {}, onDetailAddressChange: () => {},
    onOpenZipcodeSearch: () => {},
  },
  tags: ['UIS-ORD-011'],
} satisfies Meta<typeof DeliveryAddressForm>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 전부 미입력. */
export const 기본: Story = {}

/** 입력완료 — 우편번호 찾기까지 마친 상태. */
export const 입력완료: Story = {
  args: {
    recipientName: '홍길동', phone: '010-1234-5678',
    zipcode: '06236', roadAddress: '서울 강남구 테헤란로 1', detailAddress: '101동 202호',
  },
}

/** 배송지오류 — [결제하기] 시도 후 필드별 안내(전 필드 누락). */
export const 배송지오류: Story = {
  args: {
    errors: {
      recipientName: '수령인을 입력해 주세요',
      phone: '연락처를 입력해 주세요',
      zipcode: '우편번호 찾기로 주소를 선택해 주세요',
      detailAddress: '상세주소를 입력해 주세요',
    },
  },
  tags: ['shows-error'],
}
