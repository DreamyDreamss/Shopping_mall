// linked_func: FUNC-order-002 — 주문 상세 **화면 전체**(컨테이너)
//
// `OrderDetailCard`는 자기 스토리가 있지만, 그 카드가 「← 주문 목록」 링크와 함께 한 화면에
// 놓였을 때의 모습은 아무도 안 보여 준다. SpecLens 4b의 「화면」 모드가 찾는 것이 이것이다.
//
// 이 페이지는 props가 없다 — `useParams()`로 주문번호를 받아 `fetchOrder()`가 서버를 부른다.
// 그래서 두 가지를 세운다: 주소(`MemoryRouter` + `Routes`로 진짜 `:orderNo`를 물려준다)와
// `fetch`(의존성을 새로 들이지 않고 스토리마다 갈아 끼운다).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import OrderDetailPage from './OrderDetailPage'
import type { OrderDetail } from '../types'

const ORDER_NO = '20260816-0001'

const base: OrderDetail = {
  orderNo: ORDER_NO, memberId: 'm001', memberName: '김주문', orderState: 'DONE',
  totalAmount: 128000, orderedAt: '2026-08-16 10:22', deliveryState: 'DELIVERED',
  items: [
    { productNo: 'p001', productName: '무선 마우스', quantity: 2, unitPrice: 24000 },
    { productNo: 'p002', productName: '기계식 키보드', quantity: 1, unitPrice: 80000 },
  ],
  deliveries: [
    { deliveryNo: 'D-002', state: 'DELIVERED', shippedAt: '2026-08-17 09:10' },
    { deliveryNo: 'D-001', state: 'SHIPPED', shippedAt: '2026-08-16 18:40' },
  ],
}

/** `/api/orders/{no}`만 가로챈다 — 모르는 요청을 삼키면 화면이 왜 비었는지 알 수 없어진다. */
function stubOrder(body: OrderDetail | null, opts: { status?: number; delay?: number } = {}) {
  const real = window.fetch
  window.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
    if (!/\/api\/orders\/[^/?]+/.test(url)) return real(input, init)
    if (opts.delay) await new Promise(r => setTimeout(r, opts.delay))
    if (opts.status && opts.status >= 400) {
      const msg = opts.status === 404 ? '주문을 찾을 수 없습니다' : '주문 조회에 실패했습니다'
      return new Response(JSON.stringify({ message: msg }),
                          { status: opts.status, headers: { 'Content-Type': 'application/json' } })
    }
    return new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } })
  }) as typeof window.fetch
  return () => { window.fetch = real }
}

const meta = {
  title: '주문/주문 상세 화면',
  component: OrderDetailPage,
  // `UIS-ORD-007`이 이 화면의 **현행** 스펙이다(라우트 `/orders/:orderNo`).
  // `UIS-ORD-002`는 같은 화면을 옛 라우트(`/order/{orderNo}`)로 적은 것이라 함께 단다.
  tags: ['UIS-ORD-007', 'UIS-ORD-002', 'FUNC-order-002'],
  decorators: [(Story: () => React.ReactElement) => (
    // 진짜 `:orderNo`를 물려준다 — `MemoryRouter`만 쓰면 `useParams()`가 비어 `/api/orders/`를 부른다.
    <MemoryRouter initialEntries={[`/orders/${ORDER_NO}`]}>
      <Routes><Route path="/orders/:orderNo" element={<Story />} /></Routes>
    </MemoryRouter>
  )],
  parameters: { layout: 'fullscreen' },
} satisfies Meta<typeof OrderDetailPage>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 품목 2건 + 배송 이력 2건. 위에 「← 주문 목록」으로 돌아가는 길이 있다. */
export const 기본: Story = {
  tags: ['state:기본'],
  beforeEach: () => stubOrder(base),
}

/** 품목 없음 — 데이터 이상이지만 화면이 죽지 않아야 한다. 빈 목록을 그리지 않고 사유를 적는다. */
export const 품목없음: Story = {
  tags: ['state:빈'],
  beforeEach: () => stubOrder({ ...base, items: [], deliveries: [], deliveryState: null }),
}

/** 불러오는 중. */
export const 조회중: Story = {
  tags: ['state:로딩'],
  beforeEach: () => stubOrder(base, { delay: 100_000 }),
}

/** 없는 주문번호 — 404. 사유를 그대로 싣는다("0건"이라고 말하지 않는다). */
export const 조회실패: Story = {
  tags: ['state:오류'],
  beforeEach: () => stubOrder(null, { status: 404 }),
}
