/** @jest-environment jsdom */
// SR-302 재작업(round 2 QA FAIL 필수 수정 1, 사람 지시) — App.tsx의 "/shop 최초 진입" 리다이렉트는
// 더 이상 "/" 라우트 element를 바꿔치기하지 않는다(round1 회귀 원인). 대신 `applyShopBootRedirect()`
// (App.tsx export, 실제 부팅에서는 `main.tsx`가 `createRoot(...).render(<App/>)` 이전에 1회 호출)를
// 라우터 마운트 전에 직접 호출해 "부팅 시 1회" 시나리오를 재현한다 — `<App/>` 자체는 항상 "/" →
// `<OrderListPage/>`인 순수 라우터라, 리다이렉트 여부는 이 함수를 호출했는지/안 했는지로만 갈린다.
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import App, { applyShopBootRedirect } from './App'

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

describe('App — /shop 서버 경로 진입(INF-ORD-017)', () => {
  const originalPathname = window.location.pathname

  beforeEach(() => {
    localStorage.clear()
    // 상품/장바구니/주문 목록 어느 쪽이 불려도 조용히 빈 목록으로 응답 — 이 describe의 관심사는
    // 어느 페이지가 마운트되는지이지 데이터 내용이 아니다. 다만 주문 상세(`GET /api/orders/:orderNo`)만은
    // `OrderDetail` 형태(items·deliveries 배열 포함)로 응답해야 한다 — 빈 배열을 그대로 주면
    // `OrderDetailCard`가 `order.deliveries`를 스프레드하다 크래시해 트리 전체가 언마운트된다(round2
    // 재작업 검증 중 실측).
    global.fetch = jest.fn(async (input: unknown) => {
      if (String(input).startsWith('/api/orders/')) {
        return jsonResponse(200, {
          orderNo: '20260101-1', memberId: 'm-1', memberName: '홍길동', orderState: 'PAID',
          totalAmount: 10000, orderedAt: '2026-01-01T00:00:00', items: [], deliveries: [],
        })
      }
      return jsonResponse(200, [])
    }) as unknown as typeof fetch
  })

  afterEach(() => {
    localStorage.clear()
    window.history.pushState({}, '', originalPathname || '/')
    window.location.hash = ''
  })

  test('/shop 서버 경로 + 빈 해시로 최초 진입하면(부팅 리다이렉트 적용) 쇼핑 홈(ShopHomePage)이 렌더된다', async () => {
    window.history.pushState({}, '', '/shop')
    window.location.hash = ''
    applyShopBootRedirect() // main.tsx가 render() 전에 호출하는 것과 동일한 부팅 1회성 호출
    render(<App />)
    await screen.findByRole('link', { name: 'SL Shop' })
  })

  test('/shop이 아닌 경로(기존 사용자 경로)는 부팅 리다이렉트를 적용해도 주문 목록이 그대로 기본이다', async () => {
    window.history.pushState({}, '', '/')
    window.location.hash = ''
    applyShopBootRedirect() // pathname이 /shop이 아니라 아무 일도 하지 않는다(멱등 조건)
    render(<App />)
    await screen.findByRole('heading', { name: '주문 목록' })
  })

  // round2 QA FAIL 필수 수정 1 — round1은 "/" 라우트 element 자체를 `isShopServerRoot() ?
  // <Navigate to="/shop"/> : <OrderListPage/>`로 바꿔치기해, pathname이 /shop인 동안은 해시가 이미
  // '#/'여도(=사용자가 이미 주문 목록에 와 있는 상태여도) 렌더마다 다시 쇼핑 홈으로 튕겼다. 이 테스트는
  // "/shop 서버 경로 + 해시가 이미 있음(최초 진입이 아님, 부팅 리다이렉트도 그 조건에서 멱등하게
  // 아무 일도 하지 않음)"을 재현해, "/" 라우트가 pathname과 무관하게 항상 주문 목록을 그대로
  // 렌더하는지(더 이상 상시 리다이렉트가 없는지) 직접 검증한다.
  test('#/ 로 이동한 상태(=이미 진입한 뒤)라면 /shop 서버 경로에서도 주문 목록이 렌더된다(쇼핑 홈으로 튕기지 않음)', async () => {
    window.history.pushState({}, '', '/shop')
    window.location.hash = '#/'
    applyShopBootRedirect() // 해시가 이미 있어 아무 일도 하지 않는다(멱등 조건)
    render(<App />)
    await screen.findByRole('heading', { name: '주문 목록' })
    expect(screen.queryByRole('link', { name: 'SL Shop' })).not.toBeInTheDocument()
  })

  // 재작업 지시 — "주문 상세 화면에서 '← 주문 목록' 클릭 시 정상 이동한다"(/shop 마운트 포함).
  test('/shop 마운트에서 주문 상세의 "← 주문 목록" 클릭 시 주문 목록으로 정상 이동한다', async () => {
    window.history.pushState({}, '', '/shop')
    window.location.hash = '#/orders/20260101-1'
    applyShopBootRedirect() // 해시가 이미 있어 아무 일도 하지 않는다(멱등 조건)
    render(<App />)

    const backLink = await screen.findByRole('link', { name: /주문 목록/ })
    fireEvent.click(backLink)

    await screen.findByRole('heading', { name: '주문 목록' })
  })
})
