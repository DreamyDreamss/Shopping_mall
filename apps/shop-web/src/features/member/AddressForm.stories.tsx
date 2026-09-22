// SR-235 — 배송지 등록/수정 폼 상태들(신규/수정/검증오류).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { AddressForm } from './AddressForm'

const meta = {
  title: '마이페이지/배송지 폼',
  component: AddressForm,
  args: {
    onChange: () => {}, onOpenZipcodeSearch: () => {}, onSave: () => {}, onCancel: () => {},
    errors: {}, isFirstAddress: false,
  },
  tags: ['UIS-MBR-004'],
} satisfies Meta<typeof AddressForm>
export default meta

type Story = StoryObj<typeof meta>

/** 신규 — 빈 값(첫 배송지 아님, 기본설정 체크박스 사용 가능). */
export const 신규: Story = {
  args: {
    mode: 'add',
    value: {
      recipient: '', phone: '', zipcode: '', roadAddress: '', detailAddress: '',
      entranceMethod: '', deliveryMemo: '', isDefault: false,
    },
  },
}

/** 수정 — 기존 값 prefill, 기본설정 체크박스 숨김(INF-MBR-008 "수정은 isDefault를 다루지 않음"). */
export const 수정: Story = {
  args: {
    mode: 'edit',
    value: {
      recipient: '홍길동', phone: '010-1234-5678', zipcode: '06236',
      roadAddress: '서울특별시 강남구 테헤란로 123', detailAddress: '101동 202호',
      entranceMethod: '공동현관 비밀번호 1234', deliveryMemo: '부재 시 경비실에 맡겨주세요', isDefault: false,
    },
  },
}

/** 검증오류 — 수령인/연락처/우편번호/상세주소 전부 오류. */
export const 검증오류: Story = {
  args: {
    mode: 'add',
    value: {
      recipient: '', phone: '', zipcode: '', roadAddress: '', detailAddress: '',
      entranceMethod: '', deliveryMemo: '', isDefault: false,
    },
    errors: {
      recipientName: '수령인을 입력해 주세요',
      phone: '연락처를 입력해 주세요',
      zipcode: '우편번호 찾기로 주소를 선택해 주세요',
      detailAddress: '상세주소를 입력해 주세요',
    },
  },
}
