// SR-304 — 상품 상세 정보 영역 상태(기본·할인있음·품절·담기실패·담기중·담기성공·로그인필요).
// SR-353 — product를 ProductDetail로(soldOut 필드 추가), notifyStatus/onNotifyRequest 추가.
// "재고 충분/재고 임박/품절(신청 전)/품절(신청됨)" 4상태(확정 답변)는 기본=재고 충분, 재고임박(신규),
// 품절=품절(신청 전), 품절신청됨(신규)로 대응한다.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductInfoPanel } from './ProductInfoPanel'
import type { ProductDetail } from '../../types'

const product: ProductDetail = {
  sku: 'SKU-1002', productName: '기계식 키보드', price: 129000, stockQty: 20, saleYn: 'Y',
  listPrice: null, imageUrl: null, soldOut: false, wishCount: 0,
}

const meta = {
  title: '쇼핑상세/상품 정보 패널',
  component: ProductInfoPanel,
  args: {
    product, qty: 1, onQtyChange: () => {}, onAddToCart: () => {}, onBuyNow: () => {},
    addStatus: 'idle', addErrorMessage: null, lastAction: null, disabledReason: null,
    notifyStatus: 'idle', onNotifyRequest: () => {},
    // SR-356 — 찜 수 0(0도 '0'으로 표시), 찜 안 한 상태, 토글 idle.
    wishCount: 0, wished: false, wishToggleStatus: 'idle', wishErrorMessage: null, onToggleWish: () => {},
  },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ProductInfoPanel>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 재고 충분(20개, 10개 초과라 재고 상태 라벨 자체가 없음), 정가 없음, 담기/구매 가능. */
export const 기본: Story = { tags: ['state:기본'],}

/** 할인있음 — 실 SKU-1001(스탠딩 데스크), price 390,000 / listPrice 450,000(13% 할인, DB 실측). */
export const 할인있음: Story = {
  args: {
    product: { sku: 'SKU-1001', productName: '스탠딩 데스크', price: 390000, stockQty: 12, saleYn: 'Y', listPrice: 450000, imageUrl: null, soldOut: false, wishCount: 0 },
  },
}

/** 재고임박 — 재고 3개(10개 이하). "남은 수량 3개" 라벨이 보인다(확정 답변 scope_freeze). */
export const 재고임박: Story = {
  args: {
    product: { ...product, stockQty: 3, soldOut: false },
  },
}

/** 품절(신청 전) — 재고 0. 수량 선택·담기/구매 버튼이 사라지고 [재입고 알림 신청] 버튼만 보인다. */
export const 품절: Story = {
  args: {
    product: { ...product, stockQty: 0, soldOut: true },
    notifyStatus: 'idle',
  },
}

/** 품절(신청됨) — 재입고 알림 신청 완료 후. 버튼 라벨이 "신청됨"으로 바뀌고 비활성화된다. */
export const 품절신청됨: Story = {
  args: {
    product: { ...product, stockQty: 0, soldOut: true },
    notifyStatus: 'requested',
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

// SR-356 — 찜 수·찜/해제 토글 상태(기본=찜0은 위 `기본` 스토리 기본값으로 이미 커버).

/** 찜N — 찜 수 12건, 아직 찜 안 한 상태. */
export const 찜N: Story = {
  args: { wishCount: 12, wished: false },
}

/** 찜됨 — 찜한 상태(버튼 라벨이 "♥ 찜됨"으로 바뀐다). */
export const 찜됨: Story = {
  args: { wishCount: 13, wished: true },
}

/** 찜토글중 — pending, 라벨은 낙관 반영값(wished) 그대로 두고 버튼만 비활성(연타 방지). */
export const 찜토글중: Story = {
  args: { wished: true, wishToggleStatus: 'pending' },
}

/** 찜토글실패 — 낙관적 갱신 롤백 후 오류 문구 인라인 표시. */
export const 찜토글실패: Story = {
  args: { wishToggleStatus: 'error', wishErrorMessage: '일시적 오류입니다. 다시 시도해 주세요' },
}

// SR-358 — 상품 상세 조회수. "없음/0" 상태는 위 `기본` 스토리가 이미 커버한다(fixture가 viewCount를
// 채우지 않아 `?? 0`으로 「조회 0」이 자동으로 나온다, 확정 답변).

/** 조회수 — 조회수 1,234회. 천 단위 쉼표 표기를 스토리로 고정한다. */
export const 조회수: Story = {
  args: { product: { ...product, viewCount: 1234 } },
}
