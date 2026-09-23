// SR-235 — 배송지 목록 상태들(0건/1건/10건상한/기본배송지전환중).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { AddressList } from './AddressList'
import type { MemberAddress } from '../../types'

function address(i: number, overrides: Partial<MemberAddress> = {}): MemberAddress {
  return {
    addressId: i, memberId: 'm-story-1', recipient: `수령인${i}`, phone: `010-1234-${String(1000 + i)}`,
    phoneNorm: `010123${String(1000 + i)}`, zipcode: '06236', roadAddress: `서울 강남구 테헤란로 ${i}`,
    detailAddress: `${i}동 ${i}호`, entranceMethod: null, deliveryMemo: null, isDefault: 'N',
    lastUsedAt: '2026-09-19T10:00:00', createdAt: '2026-09-19T10:00:00', updatedAt: '2026-09-19T10:00:00',
    ...overrides,
  }
}

const meta = {
  title: '마이페이지/배송지 목록',
  component: AddressList,
  args: { onAdd: () => {}, onEdit: () => {}, onDelete: () => {}, onSetDefault: () => {}, pendingAddressId: null },
  tags: ['UIS-MBR-004'],
} satisfies Meta<typeof AddressList>
export default meta

type Story = StoryObj<typeof meta>

/** 0건 — 안내 문구 + [추가] 버튼. */
export const 목록_0건: Story = { tags: ['state:빈'], args: { addresses: [] } }

/** 1건 — 기본 배지 표시. */
export const 목록_1건: Story = { tags: ['state:기본'], args: { addresses: [address(1, { isDefault: 'Y' })] } }

/** 10건 도달 — [추가] 버튼 비활성 + "최대 10개" 안내. */
export const 목록_10건상한: Story = {
  args: { addresses: Array.from({ length: 10 }, (_, i) => address(i + 1, { isDefault: i === 0 ? 'Y' : 'N' })) },
}

/** 기본 배송지 전환 중 — 어느 한 행이 pendingAddressId로 잠긴 상태(버튼 disabled 시각 확인). */
export const 기본배송지전환중: Story = { tags: ['state:로딩'],
  args: {
    addresses: [address(1, { isDefault: 'Y' }), address(2)],
    pendingAddressId: 2,
  },
}
