// linked_func: FUNC-order-001 — 주문 목록 **화면 전체**(컨테이너)
//
// 왜 부품 스토리만으로는 모자랐나
// ────────────────────────────────
// `OrderTable`·`OrderFilters`·`OrderSummaryCard`는 각자 스토리가 있지만, 그것들이 **한 화면에
// 같이 놓였을 때**의 모습은 아무도 안 보여 준다. SpecLens 4b의 「화면」 모드가 찾는 것이 바로
// 이 페이지 자체 스토리다(부품 스토리는 「컴포넌트」 모드에서 본다).
//
// 이 페이지는 props가 없고 `fetchOrders()`가 직접 서버를 부른다. 그래서 상태를 만들려면
// `fetch`를 세워야 한다 — MSW 같은 의존성을 새로 들이지 않고 스토리마다 `window.fetch`를
// 갈아 끼운다. `loaders`가 렌더 **전에** 돌고 `beforeEach`의 정리 함수가 끝나고 되돌린다.
//
// 라우터가 필요하다: 행을 누르면 `useNavigate()`로 `/orders/{no}`로 간다. 진짜 주소를 바꾸지
// 않도록 `MemoryRouter`로 감싼다.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { MemoryRouter } from 'react-router-dom'
import OrderListPage from './OrderListPage'
import type { OrderRow } from '../types'

const rows: OrderRow[] = [
  { orderNo: '20260816-0001', memberId: 'm001', memberName: '김주문', orderState: 'DONE',
    totalAmount: 128000, orderedAt: '2026-08-16 10:22', deliveryState: 'DELIVERED' },
  { orderNo: '20260816-0002', memberId: 'm002', memberName: '이배송', orderState: 'SHIPPING',
    totalAmount: 43900, orderedAt: '2026-08-16 11:03', deliveryState: 'READY' },
  { orderNo: '20260816-0003', memberId: 'm003', memberName: null, orderState: 'PAID',
    totalAmount: 7900, orderedAt: '2026-08-16 12:41', deliveryState: null },
]

/** `/api/orders`만 가로챈다 — 다른 주소는 원래 `fetch`로 보낸다(모르는 요청을 삼키면
 *  화면이 왜 비었는지 알 수 없어진다). `delay`를 주면 로딩 상태에서 멈춘다. */
function stubOrders(body: OrderRow[] | null, opts: { status?: number; delay?: number } = {}) {
  const real = window.fetch
  window.fetch = (async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = typeof input === 'string' ? input : input instanceof URL ? input.href : input.url
    if (!url.includes('/api/orders')) return real(input, init)
    if (opts.delay) await new Promise(r => setTimeout(r, opts.delay))
    if (opts.status && opts.status >= 400) {
      return new Response(JSON.stringify({ message: '주문 조회에 실패했습니다' }),
                          { status: opts.status, headers: { 'Content-Type': 'application/json' } })
    }
    return new Response(JSON.stringify({ totalCount: (body ?? []).length, page: 1, items: body ?? [] }),
                        { status: 200, headers: { 'Content-Type': 'application/json' } })
  }) as typeof window.fetch
  return () => { window.fetch = real }
}

const meta = {
  title: '주문/주문 목록 화면',
  component: OrderListPage,
  // Speclinker 링크 — `index.json`에 실리는 것은 tags뿐이라 스펙 ID를 태그로 단다.
  // `UIS-ORD-006`이 이 화면의 **현행** 스펙이다(라우트 `/`). `UIS-ORD-001`은 같은 화면을
  // 옛 라우트(`/order/list`)로 적은 것이라 함께 단다 — 둘을 하나로 합치는 것은 스펙 쪽 일이다.
  tags: ['UIS-ORD-006', 'UIS-ORD-001', 'FUNC-order-001'],
  decorators: [(Story: () => React.ReactElement) => <MemoryRouter><Story /></MemoryRouter>],
  parameters: { layout: 'fullscreen' },
} satisfies Meta<typeof OrderListPage>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 필터·집계·목록이 한 화면에. 주문 3건. */
export const 기본: Story = {
  tags: ['state:기본'],
  beforeEach: () => stubOrders(rows),
}

/** 조회 결과 0건 — 집계와 목록이 **각각** "없음"을 말한다(오류와 다른 사실이다). */
export const 결과없음: Story = {
  tags: ['state:빈'],
  beforeEach: () => stubOrders([]),
}

/** 불러오는 중 — 집계 카드는 아예 그리지 않고 목록만 진행 표시(`{!loading && <OrderSummaryCard>}`). */
export const 조회중: Story = {
  tags: ['state:로딩'],
  beforeEach: () => stubOrders(rows, { delay: 100_000 }),
}

/** 조회 실패 — 서버 사유를 그대로 싣는다. 목록은 비우되 "0건"이라고 말하지 않는다. */
export const 조회실패: Story = {
  tags: ['state:오류'],
  beforeEach: () => stubOrders(null, { status: 500 }),
}
