// SR-235 — 배송지 삭제 확인 다이얼로그 상태들(일반삭제확인/기본배송지삭제확인).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { AddressDeleteConfirm } from './AddressDeleteConfirm'

const meta = {
  title: '마이페이지/배송지 삭제 확인',
  component: AddressDeleteConfirm,
  args: {
    open: true, recipient: '홍길동', roadAddress: '서울특별시 강남구 테헤란로 123',
    isDefault: false, onCancel: () => {}, onConfirm: () => {},
  },
  tags: ['UIS-MBR-004'],
} satisfies Meta<typeof AddressDeleteConfirm>
export default meta

type Story = StoryObj<typeof meta>

/** 일반삭제확인 — 기본 배송지가 아닌 행. */
export const 일반삭제확인: Story = {}

/** 기본배송지삭제확인 — 승계 안내 문구 포함. */
export const 기본배송지삭제확인: Story = { args: { isDefault: true } }
