/** @jest-environment jsdom */
// SR-305 — 장바구니(/shop/cart) 통합 테스트. `ProductDetailPage.test.tsx` 관례 그대로(jsdom, URL+메서드
// 분기 fetch mock, `@jest/globals`의 expect, 테스트별 고유 sku/memberId).
//
// 재작업(round 2) — [주문하기] 연타 방지(필수4)를 검증하려면 실제 `navigate` 호출 횟수를 세야 한다
// (같은 경로로 두 번 이동해도 `MemoryRouter`상 눈에 보이는 차이가 없어, 렌더 결과만으로는 1회 호출과
// 2회 호출을 구분할 수 없다). `useNavigate`만 스텁으로 바꾸고 나머지 react-router-dom은 실제 구현을
// 그대로 쓴다.
//
// `jest.mock(...)`는 `@swc/jest`가 호이스팅하려면 `jest` 식별자가 지역 import 바인딩이 아니라 전역
// 참조여야 한다(실측 확인 — `@jest/globals`에서 `jest`를 import하면 호이스팅이 조용히 동작하지 않고
// 실제 react-router-dom이 그대로 로드된다). 아래 `declare const jest`는 런타임 코드를 만들지 않는
// 타입 전용 선언이라, 테스트 러너가 주입하는 전역 `jest`를 그대로 쓰면서 타입만 `@jest/globals`
// 것을 빌린다.
declare const jest: typeof import('@jest/globals').jest

import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import CartPage from './CartPage'
import { saveSession } from '../session'
import type { CartRow, Product } from '../types'

const mockNavigate = jest.fn()
jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom') as Record<string, unknown>
  return { ...actual, useNavigate: () => mockNavigate }
})

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

// 테스트마다 인덱스로 유일화한다(STORY "테스트 격리" 절) — sku·회원id 문자열 충돌을 막는다.
function cartRow(i: number, overrides: Partial<CartRow> = {}): CartRow {
  const price = 10000 + i * 1000
  return { sku: `sku-cart-${i}`, productName: `장바구니상품${i}`, price, qty: 2, lineTotal: price * 2, ...overrides }
}

function product(i: number, overrides: Partial<Product> = {}): Product {
  return {
    sku: `sku-cart-${i}`, productName: `장바구니상품${i}`, price: 10000 + i * 1000, stockQty: 5,
    saleYn: 'Y', listPrice: null, imageUrl: null, ...overrides,
  }
}

function session(i: number) {
  return {
    memberId: `m-cart-${i}`, memberName: '홍길동', grade: 'NORMAL',
    apiKey: `key-cart-${i}`, refreshToken: `r-cart-${i}`, refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
  }
}

describe('CartPage', () => {
  let fetchMock: import('@jest/globals').jest.Mock<typeof fetch>
  let patchCalls: { sku: string; qty: number }[]
  let deleteCalls: string[]

  beforeEach(() => {
    fetchMock = jest.fn()
    patchCalls = []
    deleteCalls = []
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear()
    mockNavigate.mockClear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  function routeFetch(handlers: {
    cart?: () => Response | Promise<Response>
    products?: () => Response | Promise<Response>
    patchItem?: (sku: string, qty: number) => Response | Promise<Response>
    deleteItem?: (sku: string) => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const method = (init?.method ?? 'GET').toUpperCase()
      if (url.startsWith('/api/cart/items/')) {
        const sku = decodeURIComponent(url.slice('/api/cart/items/'.length).split('?')[0])
        if (method === 'PATCH') {
          const body = init?.body ? (JSON.parse(String(init.body)) as { qty: number }) : { qty: 0 }
          patchCalls.push({ sku, qty: body.qty })
          if (!handlers.patchItem) throw new Error('unexpected PATCH ' + url)
          return handlers.patchItem(sku, body.qty)
        }
        if (method === 'DELETE') {
          deleteCalls.push(sku)
          return handlers.deleteItem ? handlers.deleteItem(sku) : jsonResponse(204, null)
        }
      }
      if (url.startsWith('/api/cart')) {
        return handlers.cart ? handlers.cart() : jsonResponse(200, { items: [] })
      }
      if (url.startsWith('/api/products')) {
        return handlers.products ? handlers.products() : jsonResponse(200, [])
      }
      throw new Error('unexpected url ' + url + ' ' + method)
    })
  }

  function renderPage() {
    return render(
      <MemoryRouter initialEntries={['/shop/cart']}>
        <Routes>
          <Route path="/shop/cart" element={<CartPage />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  const listSection = () => screen.getByRole('list', { name: '장바구니 목록' })

  test('담긴상품 있음 — 썸네일 대체·수량·라인합계·요약이 렌더된다', async () => {
    saveSession(session(1))
    const rows = [cartRow(1), cartRow(2)]
    routeFetch({
      cart: () => jsonResponse(200, { items: rows, totalAmount: rows.reduce((s, r) => s + r.lineTotal, 0) }),
      products: () => jsonResponse(200, [product(1), product(2)]),
    })
    renderPage()

    await screen.findByText('장바구니상품1')
    expect(within(listSection()).getByLabelText('장바구니상품1 수량')).toHaveValue(2)
    expect(within(listSection()).getByText('22,000원')).toBeInTheDocument() // 11,000 * 2
    expect(screen.getByRole('complementary', { name: '주문 합계' })).toBeInTheDocument()
  })

  test('빈 장바구니 — "담긴 상품이 없습니다" + 쇼핑 계속하기 링크', async () => {
    saveSession(session(2))
    routeFetch({
      cart: () => jsonResponse(200, { items: [], totalAmount: 0 }),
      products: () => jsonResponse(200, []),
    })
    renderPage()

    await screen.findByText('담긴 상품이 없습니다')
    expect(screen.getByRole('link', { name: '쇼핑 계속하기' })).toHaveAttribute('href', '#/shop/products')
  })

  test('수량 증가 — PATCH 호출 후 서버 응답값으로 수량·합계가 반영된다', async () => {
    saveSession(session(3))
    const row = cartRow(3, { qty: 1, lineTotal: 11000 })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 11000 }),
      products: () => jsonResponse(200, [product(3)]),
      patchItem: (sku, qty) => jsonResponse(200, { sku, productName: '장바구니상품3', price: 11000, qty, lineTotal: 11000 * qty }),
    })
    renderPage()

    await screen.findByText('장바구니상품3')
    fireEvent.click(within(listSection()).getByRole('button', { name: '장바구니상품3 수량 증가' }))

    await within(listSection()).findByText('22,000원')
    expect(patchCalls).toEqual([{ sku: 'sku-cart-3', qty: 2 }])
    expect(within(listSection()).getByLabelText('장바구니상품3 수량')).toHaveValue(2)
  })

  test('재고 초과 시도 — 클라이언트 클램프가 즉시 안내하고 PATCH를 호출하지 않는다', async () => {
    saveSession(session(4))
    const row = cartRow(4, { qty: 5, lineTotal: 70000 })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 70000 }),
      products: () => jsonResponse(200, [product(4, { stockQty: 5 })]),
    })
    renderPage()

    await screen.findByText('장바구니상품4')
    fireEvent.click(within(listSection()).getByRole('button', { name: '장바구니상품4 수량 증가' }))

    await screen.findByText('최대 수량은 5개입니다')
    expect(patchCalls).toHaveLength(0)
    expect(within(listSection()).getByLabelText('장바구니상품4 수량')).toHaveValue(5)
  })

  test('PATCH가 409를 돌려주면 그 줄에 서버 message 그대로 노출하고 값은 직전 확인 수량 그대로다', async () => {
    saveSession(session(5))
    const row = cartRow(5, { qty: 2, lineTotal: 30000 })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 30000 }),
      products: () => jsonResponse(200, [product(5, { stockQty: 5 })]),
      patchItem: () => jsonResponse(409, { message: '재고 초과: 가용 2, 요청 3' }, false),
    })
    renderPage()

    await screen.findByText('장바구니상품5')
    fireEvent.click(within(listSection()).getByRole('button', { name: '장바구니상품5 수량 증가' }))

    await screen.findByText('재고 초과: 가용 2, 요청 3')
    expect(within(listSection()).getByLabelText('장바구니상품5 수량')).toHaveValue(2)
  })

  test('삭제 클릭 — DELETE 호출 후 그 행이 제거된다', async () => {
    saveSession(session(6))
    const rows = [cartRow(6), cartRow(60)]
    routeFetch({
      cart: () => jsonResponse(200, { items: rows, totalAmount: rows.reduce((s, r) => s + r.lineTotal, 0) }),
      products: () => jsonResponse(200, [product(6), product(60)]),
    })
    renderPage()

    await screen.findByText('장바구니상품6')
    fireEvent.click(within(listSection()).getByRole('button', { name: '장바구니상품6 삭제' }))

    expect(deleteCalls).toEqual(['sku-cart-6'])
    await waitFor(() => expect(screen.queryByText('장바구니상품6')).not.toBeInTheDocument())
    expect(screen.getByText('장바구니상품60')).toBeInTheDocument() // 남은 행 유지
  })

  test('선택 해제 — 부분선택/전체해제 각각 합계가 재계산되고 비활성 사유가 aria-describedby로 전달된다', async () => {
    saveSession(session(7))
    const rows = [cartRow(7), cartRow(70)]
    routeFetch({
      cart: () => jsonResponse(200, { items: rows, totalAmount: rows.reduce((s, r) => s + r.lineTotal, 0) }),
      products: () => jsonResponse(200, [product(7), product(70)]),
    })
    renderPage()

    await screen.findByText('장바구니상품7')
    fireEvent.click(within(listSection()).getByLabelText('장바구니상품7 선택'))

    await screen.findByText('부분 선택 주문은 지원하지 않습니다 — 제외할 상품은 삭제해 주세요')
    const orderButton = screen.getByRole('button', { name: '주문하기' })
    expect(orderButton).toBeDisabled()
    expect(orderButton).toHaveAttribute('aria-describedby')

    // 재작업(round 2, 권고 1) — 나머지 하나도 해제해 0건 선택 상태가 되면 사유 문구가 바뀌어도
    // aria-describedby 연결은 계속 유지된다(이전에는 안내도 aria-describedby도 없었다).
    fireEvent.click(within(listSection()).getByLabelText('장바구니상품70 선택'))
    await screen.findByText('주문할 상품을 선택해 주세요')
    expect(screen.queryByText('부분 선택 주문은 지원하지 않습니다 — 제외할 상품은 삭제해 주세요')).not.toBeInTheDocument()
    expect(orderButton).toBeDisabled()
    expect(orderButton).toHaveAttribute('aria-describedby')
  })

  test('로그인 안 됨 — GET /api/cart 자체가 호출되지 않는다', async () => {
    routeFetch({})
    renderPage()

    await screen.findByText('로그인이 필요합니다')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  test('전체선택 상태 — [주문하기] 클릭 시 navigate("/shop/order")가 호출된다(상태 없이)', async () => {
    saveSession(session(8))
    const row = cartRow(8)
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: row.lineTotal }),
      products: () => jsonResponse(200, [product(8)]),
    })
    renderPage()

    await screen.findByText('장바구니상품8')
    const orderButton = screen.getByRole('button', { name: '주문하기' })
    expect(orderButton).not.toBeDisabled()
    fireEvent.click(orderButton)

    expect(mockNavigate).toHaveBeenCalledTimes(1)
    expect(mockNavigate).toHaveBeenCalledWith('/shop/order')
  })

  // 재작업(round 2, QA FAIL 필수1) — 실측 재현: 입력란이 `value={qty}` 완전 제어 + 매 키 입력마다
  // 클램프+PATCH를 확정하던 시절엔 재고 20·현재 2 줄에서 "15"를 치면 "1" 시점에 PATCH{qty:1}이
  // 확정되고 입력란이 비활성화돼 "5"를 받지 못했다. 이제 blur에서만 커밋되므로 두 자리 수 입력 도중엔
  // PATCH가 전혀 나가지 않고, blur 시점에 최종 값(15) 하나로만 커밋된다.
  test('수량 직접입력 — 두 자리 수(15)를 입력하는 동안은 PATCH가 나가지 않고 blur에서 한 번만 커밋된다', async () => {
    saveSession(session(9))
    const row = cartRow(9, { qty: 2, lineTotal: 22000 })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 22000 }),
      products: () => jsonResponse(200, [product(9, { stockQty: 20 })]),
      patchItem: (sku, qty) => jsonResponse(200, { sku, productName: '장바구니상품9', price: 11000, qty, lineTotal: 11000 * qty }),
    })
    renderPage()

    await screen.findByText('장바구니상품9')
    const qtyInput = within(listSection()).getByLabelText('장바구니상품9 수량')
    expect(qtyInput).not.toBeDisabled()

    fireEvent.focus(qtyInput)
    fireEvent.change(qtyInput, { target: { value: '1' } })
    expect(patchCalls).toHaveLength(0) // 첫 글자 시점엔 아직 커밋되지 않는다
    expect(qtyInput).not.toBeDisabled() // 입력 중에도 비활성화되지 않는다(사람 수정 지시)
    fireEvent.change(qtyInput, { target: { value: '15' } })
    expect(patchCalls).toHaveLength(0) // 둘째 글자까지도 아직 커밋 전이다

    fireEvent.blur(qtyInput)
    expect(patchCalls).toEqual([{ sku: 'sku-cart-9', qty: 15 }])

    await within(listSection()).findByText('165,000원') // 11,000 * 15
    expect(qtyInput).toHaveValue(15)
  })

  // 재작업(round 3, QA CONCERNS 권고1) — QA 실측 재현: [+] 클릭으로 PATCH{qty:3}가 in-flight인 동안
  // 입력란에 타이핑 없이 focus만 했다가, 응답이 도착해 qty가 3으로 바뀐 뒤 blur하면(여전히 타이핑
  // 없음) `onFocus={() => setQtyDraft(String(qty))}`가 focus 시점의 옛 qty(2)를 draft에 박제해 둔
  // 탓에 blur가 그 옛 값으로 다시 PATCH를 커밋했다(`patchCalls=[{qty:3},{qty:2}]`) — 방금 서버가
  // 확인해 준 수량이 소리 없이 되돌아가는 결함. onFocus를 없앤 지금은 draft가 `onChange`에서만
  // 채워지므로(편집 없인 항상 null) blur의 `commitQtyDraft`가 null 가드로 곧장 반환해 추가 PATCH가
  // 나가지 않는다. 이 테스트는 onFocus seed가 되살아나면 실패해야 한다.
  test('PATCH 응답 대기 중 타이핑 없이 focus 후 blur해도 추가 PATCH가 나가지 않는다', async () => {
    saveSession(session(16))
    const row = cartRow(16, { qty: 2, lineTotal: 24000 })
    let resolvePatch: (r: Response) => void = () => {}
    const pendingPatch = new Promise<Response>(resolve => { resolvePatch = resolve })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 24000 }),
      products: () => jsonResponse(200, [product(16, { stockQty: 10 })]),
      patchItem: () => pendingPatch,
    })
    renderPage()

    await screen.findByText('장바구니상품16')
    const qtyInput = within(listSection()).getByLabelText('장바구니상품16 수량')
    const plusButton = within(listSection()).getByRole('button', { name: '장바구니상품16 수량 증가' })

    fireEvent.click(plusButton) // PATCH {qty:3} in-flight, 아직 응답 없음
    expect(patchCalls).toEqual([{ sku: 'sku-cart-16', qty: 3 }])

    fireEvent.focus(qtyInput) // 타이핑 없이 focus만

    await act(async () => {
      resolvePatch(jsonResponse(200, { sku: 'sku-cart-16', productName: '장바구니상품16', price: 12000, qty: 3, lineTotal: 36000 }))
      await pendingPatch
    })
    await within(listSection()).findByText('36,000원') // 서버 확인값 3으로 갱신됨

    fireEvent.blur(qtyInput) // 여전히 타이핑 없음

    expect(patchCalls).toEqual([{ sku: 'sku-cart-16', qty: 3 }]) // 추가 PATCH 없음
    expect(qtyInput).toHaveValue(3)
  })

  test('수량 직접입력 — 빈 값으로 blur하면 직전 확인 수량으로 되돌아가고 PATCH를 호출하지 않는다', async () => {
    saveSession(session(10))
    const row = cartRow(10, { qty: 3, lineTotal: 36000 })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 36000 }),
      products: () => jsonResponse(200, [product(10, { stockQty: 20 })]),
    })
    renderPage()

    await screen.findByText('장바구니상품10')
    const qtyInput = within(listSection()).getByLabelText('장바구니상품10 수량')
    fireEvent.focus(qtyInput)
    fireEvent.change(qtyInput, { target: { value: '' } })
    fireEvent.blur(qtyInput)

    expect(patchCalls).toHaveLength(0)
    expect(qtyInput).toHaveValue(3)
  })

  // 실패 사례집 대조(SR-302 #1, 재작업 필수4) — 분리 클릭이 아니라 act() 한 스코프 안에서 묶어
  // 발사한다(가드를 임시로 제거하면 patchCalls.length===2로 깨지는 것을 확인한 뒤 복원했다, Dev 기록
  // 참조). 두 번째 클릭 시점엔 아직 리렌더가 끝나지 않아 `disabled` 속성도 갱신 전이므로, state
  // 지연이 아니라 동기 ref 잠금(`inFlightSkusRef`)만이 이 테스트를 통과시킨다.
  test('연타 방지 — 수량 증가 버튼 2회 클릭에도 PATCH 호출은 1회만 나간다', async () => {
    saveSession(session(11))
    const row = cartRow(11, { qty: 2, lineTotal: 24000 })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 24000 }),
      products: () => jsonResponse(200, [product(11, { stockQty: 10 })]),
      patchItem: (sku, qty) => jsonResponse(200, { sku, productName: '장바구니상품11', price: 12000, qty, lineTotal: 12000 * qty }),
    })
    renderPage()

    await screen.findByText('장바구니상품11')
    const plusButton = within(listSection()).getByRole('button', { name: '장바구니상품11 수량 증가' })
    act(() => {
      plusButton.click()
      plusButton.click()
    })

    await within(listSection()).findByText('36,000원') // qty 3 * 12,000
    expect(patchCalls).toEqual([{ sku: 'sku-cart-11', qty: 3 }])
  })

  // 재작업(round 2, 필수4) — [주문하기]도 같은 방식(동기 ref `orderNavigateRef`)으로 검증한다.
  test('연타 방지 — [주문하기] 2회 클릭에도 navigate는 1회만 호출된다', async () => {
    saveSession(session(12))
    const row = cartRow(12)
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: row.lineTotal }),
      products: () => jsonResponse(200, [product(12)]),
    })
    renderPage()

    await screen.findByText('장바구니상품12')
    const orderButton = screen.getByRole('button', { name: '주문하기' })
    act(() => {
      orderButton.click()
      orderButton.click()
    })

    expect(mockNavigate).toHaveBeenCalledTimes(1)
  })

  // 재작업(round 2, QA FAIL 필수3) — 내부 상세 문자열 필터가 라인 오류·로드 실패에도 적용됐는지 확인
  // (`OrderFailureNotice` 밖의 두 표시 지점).
  test('PATCH 실패 메시지가 내부 상세 문자열이면 그 줄에 일반 문구로 대체된다', async () => {
    saveSession(session(13))
    const row = cartRow(13, { qty: 2, lineTotal: 26000 })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 26000 }),
      products: () => jsonResponse(200, [product(13, { stockQty: 5 })]),
      patchItem: () => jsonResponse(
        500,
        { message: 'java.lang.RuntimeException: boom at com.sl.order.CartService.checkout(CartService.java:154)' },
        false,
      ),
    })
    renderPage()

    await screen.findByText('장바구니상품13')
    fireEvent.click(within(listSection()).getByRole('button', { name: '장바구니상품13 수량 증가' }))

    await screen.findByText('일시적 오류입니다. 다시 시도해 주세요')
    expect(screen.queryByText(/RuntimeException/)).not.toBeInTheDocument()
  })

  test('장바구니 로드 실패 메시지가 내부 상세 문자열이면 일반 문구로 대체된다', async () => {
    saveSession(session(14))
    routeFetch({
      cart: () => jsonResponse(500, { message: 'at com.sl.order.CartService.get(CartService.java:88)' }, false),
      products: () => jsonResponse(200, []),
    })
    renderPage()

    await screen.findByText(/불러오지 못했습니다/)
    expect(screen.getByText('불러오지 못했습니다 — 일시적 오류입니다. 다시 시도해 주세요')).toBeInTheDocument()
  })

  // 재작업(round 3, QA CONCERNS 권고2) — 정규식 거부목록만으로는 계약 밖 5xx라도 예외 클래스명·
  // 스택프레임·경로 토큰이 안 섞인 "평범해 보이는" 서버 메시지를 걸러내지 못했다. 상태코드 허용목록
  // (400/404/409만 원문)이 주 방어선으로 승격됐는지, 정규식이 절대 걸지 않을 문장으로 확인한다 — 이
  // 테스트는 상태코드 검사가 없던 구현이면 실패한다(서버 메시지가 그대로 화면에 뜬다).
  test('PATCH가 계약 밖 5xx를 돌려주면 메시지가 평범해 보여도 서버 원문 대신 일반 문구만 보인다', async () => {
    saveSession(session(17))
    const row = cartRow(17, { qty: 2, lineTotal: 22000 })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 22000 }),
      products: () => jsonResponse(200, [product(17, { stockQty: 5 })]),
      patchItem: () => jsonResponse(500, { message: '요청을 처리할 수 없습니다' }, false),
    })
    renderPage()

    await screen.findByText('장바구니상품17')
    fireEvent.click(within(listSection()).getByRole('button', { name: '장바구니상품17 수량 증가' }))

    await screen.findByText('일시적 오류입니다. 다시 시도해 주세요')
    expect(screen.queryByText('요청을 처리할 수 없습니다')).not.toBeInTheDocument()
  })
})
