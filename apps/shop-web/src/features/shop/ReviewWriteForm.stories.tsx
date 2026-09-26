// SR-351 — 상품평 작성 폼 상태(기본/필드오류/제출중/403배너/409배너/구매이력없음/주문조회실패).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ReviewWriteForm } from './ReviewWriteForm'

const eligibleOrders = [
  { orderNo: 'ORD-2026-0001', orderedAt: '2026-08-01T10:00:00Z' },
  { orderNo: 'ORD-2026-0002', orderedAt: '2026-09-01T10:00:00Z' },
]

const meta = {
  title: '쇼핑상세/상품평/작성 폼',
  component: ReviewWriteForm,
  args: {
    eligibleOrders, orderLookupStatus: 'ready', onRetryOrderLookup: () => {},
    orderNo: '', onOrderNoChange: () => {}, rating: 0, onRatingChange: () => {},
    content: '', onContentChange: () => {}, fieldErrors: {}, status: 'idle', onSubmit: () => {},
  },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ReviewWriteForm>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 주문 선택 가능, 미입력 상태. */
export const 기본: Story = { tags: ['state:기본'] }

/** 필드오류 — 400 검증 실패가 필드별로 표시된다. */
export const 필드오류: Story = {
  args: {
    orderNo: '', rating: 0, content: '짧음',
    fieldErrors: { orderNo: '구매하신 주문을 선택해 주세요', rating: '별점을 선택해 주세요', content: '본문은 10자 이상 1000자 이하로 입력해 주세요' },
  },
}

/** 제출중 — 모든 입력·버튼이 잠긴다. */
export const 제출중: Story = {
  args: { orderNo: eligibleOrders[0].orderNo, rating: 4, content: '배송이 빠르고 좋았습니다.', status: 'pending' },
}

/** 403배너 — 제출 시점에 구매 이력이 사라진 경우(경합) 고정 문구. */
export const 배너403: Story = {
  args: {
    orderNo: eligibleOrders[0].orderNo, rating: 4, content: '배송이 빠르고 좋았습니다.', status: 'error',
    bannerMessage: '이 상품을 구매한 회원만 작성할 수 있습니다',
  },
}

/** 409배너 — 이미 그 주문으로 작성한 상품평이 있을 때, 입력값은 유지된 채 배너만 뜬다. */
export const 배너409: Story = {
  args: {
    orderNo: eligibleOrders[0].orderNo, rating: 4, content: '배송이 빠르고 좋았습니다.', status: 'error',
    bannerMessage: '이미 이 주문으로 작성한 상품평이 있습니다',
  },
}

/** 구매이력없음 — 이 상품을 산 적이 없는 회원, 폼 대신 고정 문구만 렌더된다. */
export const 구매이력없음: Story = { args: { eligibleOrders: [] } }

/** 주문조회실패 — 자격 조회 자체가 실패(네트워크·5xx), 403 문구와 다른 재시도 문구. */
export const 주문조회실패: Story = { args: { orderLookupStatus: 'error', eligibleOrders: [] } }
