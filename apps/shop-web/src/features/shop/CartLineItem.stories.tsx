// SR-305 — 장바구니 라인아이템 상태들(기본/수량변경중/재고초과 안내/1미만 시도 안내/PATCH 실패).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { CartLineItem } from './CartLineItem'

const meta = {
  title: '장바구니/라인아이템',
  component: CartLineItem,
  args: {
    sku: 'sku-cart-1', productName: '테스트 상품', imageUrl: null, price: 15000,
    qty: 2, lineTotal: 30000, stockQty: 5, selected: true,
    onToggleSelected: () => {}, onQtyChange: () => {}, onDelete: () => {},
  },
  tags: ['UIS-ORD-012'],
} satisfies Meta<typeof CartLineItem>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 선택됨, 조작 가능. */
export const 기본: Story = { tags: ['state:기본'],}

/** 선택 해제 — 체크박스만 꺼진 상태. */
export const 선택해제: Story = { args: { selected: false } }

/** 수량변경중 — PATCH 진행 중, 조작 버튼 비활성 + "변경 중…" 표시. */
export const 수량변경중: Story = { args: { pending: true } }

/** 재고초과 안내 — 클라이언트 클램프가 최대 수량을 즉시 안내(API 호출 없음). */
export const 재고초과안내: Story = { args: { warningMessage: '최대 수량은 5개입니다', qty: 5 } }

/** 1미만시도 안내 — 1 아래로 내리려는 시도를 즉시 막는다. */
export const 최소수량안내: Story = { args: { warningMessage: '1개 미만으로는 변경할 수 없습니다', qty: 1 } }

/** PATCH 실패(재고 로드 이후 변경 등) — 서버 message 그대로 노출. */
export const 수량변경실패: Story = {
  args: { errorMessage: '재고 초과: 가용 1, 요청 5', qty: 1 },
  tags: ['shows-error'],
}
