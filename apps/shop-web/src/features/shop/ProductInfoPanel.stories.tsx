// SR-304 — 상품 상세 정보 영역 상태(기본·할인있음·품절·담기실패·담기중·담기성공·로그인필요).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductInfoPanel } from './ProductInfoPanel'
import type { Product } from '../../types'

const product: Product = {
  sku: 'SKU-1002', productName: '기계식 키보드', price: 129000, stockQty: 20, saleYn: 'Y',
  listPrice: null, imageUrl: null,
}

const meta = {
  title: '쇼핑상세/상품 정보 패널',
  component: ProductInfoPanel,
  args: {
    product, qty: 1, onQtyChange: () => {}, onAddToCart: () => {}, onBuyNow: () => {},
    addStatus: 'idle', addErrorMessage: null, lastAction: null, disabledReason: null,
  },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ProductInfoPanel>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 정가 없음, 담기/구매 가능. */
export const 기본: Story = {}

/** 할인있음 — 실 SKU-1001(스탠딩 데스크), price 390,000 / listPrice 450,000(13% 할인, DB 실측). */
export const 할인있음: Story = {
  args: {
    product: { sku: 'SKU-1001', productName: '스탠딩 데스크', price: 390000, stockQty: 12, saleYn: 'Y', listPrice: 450000, imageUrl: null },
  },
}

/** 품절 — 재고 0. 수량 선택 자체가 사라지고 담기/구매 버튼이 비활성 + 사유가 보인다. */
export const 품절: Story = {
  args: {
    product: { ...product, stockQty: 0 },
    disabledReason: '품절된 상품입니다',
  },
}

/** 담기실패 — 409(재고초과·품절·판매중지) 사유 인라인 표시, 버튼은 재시도 가능하도록 다시 활성. */
export const 담기실패: Story = {
  args: { addStatus: 'error', addErrorMessage: '재고가 부족하거나 판매중지된 상품입니다' },
}

/** 담기중 — pending, 버튼 비활성 + "담는 중…" 표시(연타 방지). */
export const 담기중: Story = {
  args: { addStatus: 'pending' },
}

/** 담기성공 — "장바구니 담기" 클릭 성공(lastAction='cart'). */
export const 담기성공: Story = {
  args: { addStatus: 'success', lastAction: 'cart' },
}

/**
 * 바로구매성공 — 재작업(round 2) QA 권고 2/사람 지시 2: 사람 수정("바로 구매"는 `/shop/cart`로
 * 이동하지 않고 그 자리에서 알림+비활성 [장바구니 보기])이 낳는 화면 상태를 스토리로 고정한다
 * (lastAction='buyNow' → "바로 구매 대신 장바구니에 담았습니다" + 비활성 [장바구니 보기]).
 */
export const 바로구매성공: Story = {
  args: { addStatus: 'success', lastAction: 'buyNow' },
}

/** 로그인필요 — 세션 없음. 재고는 있어도 담기/구매 자체를 막는다(신규 신원 확인 경로를 만들지 않음). */
export const 로그인필요: Story = {
  args: { disabledReason: '로그인이 필요합니다' },
}
