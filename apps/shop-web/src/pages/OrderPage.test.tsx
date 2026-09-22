/** @jest-environment jsdom */
// SR-305 — 주문서·주문완료(/shop/order) 통합 테스트. `ProductDetailPage.test.tsx` 관례(jsdom, URL 분기
// fetch mock, `@jest/globals`의 expect, 테스트별 고유 memberId).
import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import OrderPage from './OrderPage'
import { saveSession } from '../session'
import type { CartRow } from '../types'

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

function cartRow(i: number, overrides: Partial<CartRow> = {}): CartRow {
  const price = 10000 + i * 1000
  return { sku: `sku-order-${i}`, productName: `주문상품${i}`, price, qty: 2, lineTotal: price * 2, ...overrides }
}

function session(i: number) {
  return {
    memberId: `m-order-${i}`, memberName: '홍길동', grade: 'NORMAL',
    apiKey: `key-order-${i}`, refreshToken: `r-order-${i}`, refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
  }
}

describe('OrderPage', () => {
  let fetchMock: jest.Mock<typeof fetch>
  let checkoutCalls: string[]

  beforeEach(() => {
    fetchMock = jest.fn()
    checkoutCalls = []
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  function routeFetch(handlers: {
    cart?: () => Response | Promise<Response>
    zipcodes?: (q: string) => Response | Promise<Response>
    checkout?: (memberId: string) => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      if (url.startsWith('/api/cart/checkout')) {
        const body = init?.body ? (JSON.parse(String(init.body)) as { memberId: string }) : { memberId: '' }
        checkoutCalls.push(body.memberId)
        if (!handlers.checkout) throw new Error('unexpected checkout call')
        return handlers.checkout(body.memberId)
      }
      if (url.startsWith('/api/cart')) {
        return handlers.cart ? handlers.cart() : jsonResponse(200, { items: [] })
      }
      if (url.startsWith('/api/zipcodes')) {
        const q = new URLSearchParams(url.split('?')[1] ?? '').get('q') ?? ''
        return handlers.zipcodes ? handlers.zipcodes(q) : jsonResponse(200, { items: [] })
      }
      throw new Error('unexpected url ' + url)
    })
  }

  function renderPage() {
    return render(
      <MemoryRouter initialEntries={['/shop/order']}>
        <Routes>
          <Route path="/shop/order" element={<OrderPage />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  /** 배송지 유효 입력(우편번호는 모달 검색+선택으로만 채워진다, 읽기전용). */
  async function fillValidAddress() {
    fireEvent.change(screen.getByLabelText('수령인'), { target: { value: '홍길동' } })
    fireEvent.change(screen.getByLabelText('연락처'), { target: { value: '010-1111-2222' } })
    fireEvent.change(screen.getByLabelText('상세주소'), { target: { value: '101동 202호' } })
    fireEvent.click(screen.getByRole('button', { name: '우편번호 찾기' }))
    fireEvent.change(screen.getByLabelText('도로명·지번 검색어'), { target: { value: '테헤란로' } })
    fireEvent.click(screen.getByRole('button', { name: '검색' }))
    const resultButton = await screen.findByRole('button', { name: /06236/ })
    fireEvent.click(resultButton)
  }

  test('정상 진입 — 장바구니 요약이 렌더된다', async () => {
    saveSession(session(1))
    routeFetch({ cart: () => jsonResponse(200, { items: [cartRow(1), cartRow(2)], totalAmount: 0 }) })
    renderPage()

    await screen.findByText('주문상품1 × 2')
    expect(screen.getByText('주문상품2 × 2')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '결제하기' })).toBeInTheDocument()
  })

  test('빈 장바구니로 진입 — 안내+링크, 체크아웃 API는 호출되지 않는다', async () => {
    saveSession(session(2))
    routeFetch({ cart: () => jsonResponse(200, { items: [], totalAmount: 0 }) })
    renderPage()

    await screen.findByText('주문할 상품이 없습니다')
    expect(screen.getByRole('link', { name: '장바구니로 이동' })).toHaveAttribute('href', '#/shop/cart')
    expect(checkoutCalls).toHaveLength(0)
  })

  test('배송지 미입력 상태에서 [결제하기] 클릭 — 필드별 안내, 체크아웃 API는 호출되지 않는다', async () => {
    saveSession(session(3))
    routeFetch({ cart: () => jsonResponse(200, { items: [cartRow(3)], totalAmount: 0 }) })
    renderPage()

    await screen.findByText('주문상품3 × 2')
    fireEvent.click(screen.getByRole('button', { name: '결제하기' }))

    expect(await screen.findByText('수령인을 입력해 주세요')).toBeInTheDocument()
    expect(screen.getByText('연락처를 입력해 주세요')).toBeInTheDocument()
    expect(screen.getByText('우편번호 찾기로 주소를 선택해 주세요')).toBeInTheDocument()
    expect(screen.getByText('상세주소를 입력해 주세요')).toBeInTheDocument()
    expect(checkoutCalls).toHaveLength(0)
  })

  test('우편번호 찾기 모달에서 항목 선택 — zipcode/roadAddress 필드에 반영된다', async () => {
    saveSession(session(4))
    routeFetch({
      cart: () => jsonResponse(200, { items: [cartRow(4)], totalAmount: 0 }),
      zipcodes: () => jsonResponse(200, {
        items: [{ zipcode: '06236', roadAddress: '서울 강남구 테헤란로 1', sido: '서울', sigungu: '강남구' }],
      }),
    })
    renderPage()

    await screen.findByText('주문상품4 × 2')
    fireEvent.click(screen.getByRole('button', { name: '우편번호 찾기' }))
    fireEvent.change(screen.getByLabelText('도로명·지번 검색어'), { target: { value: '테헤란로' } })
    fireEvent.click(screen.getByRole('button', { name: '검색' }))
    fireEvent.click(await screen.findByRole('button', { name: /06236/ }))

    expect(screen.getByLabelText('우편번호')).toHaveValue('06236')
    expect(screen.getByLabelText('도로명주소')).toHaveValue('서울 강남구 테헤란로 1')
  })

  test('결제하기 성공 — 완료 상태(주문번호·주문 내역 보기는 "/"로, 쇼핑 계속하기)', async () => {
    saveSession(session(5))
    routeFetch({
      cart: () => jsonResponse(200, { items: [cartRow(5)], totalAmount: 0 }),
      zipcodes: () => jsonResponse(200, {
        items: [{ zipcode: '06236', roadAddress: '서울 강남구 테헤란로 1', sido: '서울', sigungu: '강남구' }],
      }),
      checkout: () => jsonResponse(200, { orderNo: '20260917-000001', totalAmount: 24000, itemCount: 1 }),
    })
    renderPage()

    await screen.findByText('주문상품5 × 2')
    await fillValidAddress()
    fireEvent.click(screen.getByRole('button', { name: '결제하기' }))

    await screen.findByText('주문이 완료되었습니다')
    expect(screen.getByText('주문번호 20260917-000001')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: '주문 내역 보기' })).toHaveAttribute('href', '#/')
    expect(screen.getByRole('link', { name: '쇼핑 계속하기' })).toHaveAttribute('href', '#/shop/products')
    expect(checkoutCalls).toEqual([session(5).memberId])
    // 재작업(round 2, 권고 2) — 서버가 이미 deleteAllItems로 비운 상태다. 완료 화면에 머무는 동안
    // GNB 장바구니 배지도 그 상태와 맞아야 한다(이전엔 주문 전 수량 "2"가 그대로 남아 있었다).
    expect(screen.getByLabelText('장바구니 0개')).toBeInTheDocument()
  })

  // 재작업(round 2, QA FAIL 필수2) — 주문서 결제예정금액과 주문완료 금액이 같은 상품금액/배송비
  // 구성으로 일치하는지 확인한다(이전엔 완료 화면이 서버 totalAmount를 무라벨 총액처럼 그대로 써서
  // 배송비 3,000원만큼 어긋났다).
  test('금액 표기 — 결제예정금액과 주문완료 금액이 같은 상품금액/배송비/결제금액 3줄로 일치한다', async () => {
    saveSession(session(11))
    const row = cartRow(11, { price: 15000, qty: 2, lineTotal: 30000 })
    routeFetch({
      cart: () => jsonResponse(200, { items: [row], totalAmount: 0 }),
      zipcodes: () => jsonResponse(200, {
        items: [{ zipcode: '06236', roadAddress: '서울 강남구 테헤란로 1', sido: '서울', sigungu: '강남구' }],
      }),
      // 서버 checkout 응답의 totalAmount는 상품금액만이다(CartService.checkout — 배송비 개념 없음).
      checkout: () => jsonResponse(200, { orderNo: '20260917-000777', totalAmount: 30000, itemCount: 1 }),
    })
    renderPage()

    await screen.findByText('주문상품11 × 2')
    const preCheckoutTotals = screen.getByRole('complementary', { name: '결제 합계' })
    expect(within(preCheckoutTotals).getByText('30,000원')).toBeInTheDocument() // 상품금액
    expect(within(preCheckoutTotals).getByText('3,000원')).toBeInTheDocument() // 배송비
    expect(within(preCheckoutTotals).getByText('33,000원')).toBeInTheDocument() // 결제예정금액

    await fillValidAddress()
    fireEvent.click(screen.getByRole('button', { name: '결제하기' }))

    await screen.findByText('주문이 완료되었습니다')
    // 완료 화면도 같은 3줄 구성(상품금액/배송비/결제금액)으로 같은 숫자를 말한다 — 주문서와 어긋나지 않는다.
    const postCheckoutTotals = screen.getByRole('group', { name: '주문 금액' })
    expect(within(postCheckoutTotals).getByText('30,000원')).toBeInTheDocument()
    expect(within(postCheckoutTotals).getByText('3,000원')).toBeInTheDocument()
    expect(within(postCheckoutTotals).getByText('33,000원')).toBeInTheDocument()
  })

  // 실패 사례집 대조(SR-302 #1, 재작업 필수4) — 분리 클릭이 아니라 act() 한 스코프 안에서 묶어
  // 발사한다(가드를 임시로 제거하면 checkoutCalls.length===2로 깨지는 것을 확인한 뒤 복원했다, Dev
  // 기록 참조).
  test('연타 방지 — [결제하기] 2회 클릭에도 checkout 호출은 1회만 나간다', async () => {
    saveSession(session(7))
    routeFetch({
      cart: () => jsonResponse(200, { items: [cartRow(7)], totalAmount: 0 }),
      zipcodes: () => jsonResponse(200, {
        items: [{ zipcode: '06236', roadAddress: '서울 강남구 테헤란로 1', sido: '서울', sigungu: '강남구' }],
      }),
      checkout: () => jsonResponse(200, { orderNo: '20260917-000099', totalAmount: 24000, itemCount: 1 }),
    })
    renderPage()

    await screen.findByText('주문상품7 × 2')
    await fillValidAddress()
    const payButton = screen.getByRole('button', { name: '결제하기' })
    act(() => {
      payButton.click()
      payButton.click()
    })

    await screen.findByText('주문이 완료되었습니다')
    expect(checkoutCalls).toHaveLength(1)
  })

  // 재작업(round 2, QA FAIL 필수3) — 내부 상세 문자열 필터가 로드 실패·우편번호 검색 실패에도
  // 적용됐는지 확인한다(`OrderFailureNotice` 밖의 두 표시 지점).
  test('장바구니 로드 실패 메시지가 내부 상세 문자열이면 일반 문구로 대체된다', async () => {
    saveSession(session(9))
    routeFetch({
      cart: () => jsonResponse(500, { message: 'at com.sl.order.CartService.get(CartService.java:88)' }, false),
    })
    renderPage()

    await screen.findByText(/불러오지 못했습니다/)
    expect(screen.getByText('불러오지 못했습니다 — 일시적 오류입니다. 다시 시도해 주세요')).toBeInTheDocument()
  })

  test('우편번호 검색 실패 메시지가 내부 상세 문자열이면 일반 문구로 대체된다', async () => {
    saveSession(session(10))
    routeFetch({
      cart: () => jsonResponse(200, { items: [cartRow(10)], totalAmount: 0 }),
      zipcodes: () => jsonResponse(
        500,
        { message: 'java.lang.RuntimeException: boom at com.sl.order.ZipcodeService.search(ZipcodeService.java:40)' },
        false,
      ),
    })
    renderPage()

    await screen.findByText('주문상품10 × 2')
    fireEvent.click(screen.getByRole('button', { name: '우편번호 찾기' }))
    fireEvent.change(screen.getByLabelText('도로명·지번 검색어'), { target: { value: '테헤란로' } })
    fireEvent.click(screen.getByRole('button', { name: '검색' }))

    await screen.findByText('일시적 오류입니다. 다시 시도해 주세요')
    expect(screen.queryByText(/RuntimeException/)).not.toBeInTheDocument()
  })

  const failureCases: { status: number; message: string }[] = [
    { status: 400, message: '장바구니가 비어 있습니다' },
    { status: 404, message: '회원 없음: m-order-6' },
    { status: 409, message: '재고 부족: sku-order-6(가용0/요청2)' },
  ]

  test.each(failureCases)('결제하기 실패($status) — 서버 message 그대로 노출 + 입력값 보존', async ({ status, message }) => {
    saveSession(session(6))
    routeFetch({
      cart: () => jsonResponse(200, { items: [cartRow(6)], totalAmount: 0 }),
      zipcodes: () => jsonResponse(200, {
        items: [{ zipcode: '06236', roadAddress: '서울 강남구 테헤란로 1', sido: '서울', sigungu: '강남구' }],
      }),
      checkout: () => jsonResponse(status, { message }, false),
    })
    renderPage()

    await screen.findByText('주문상품6 × 2')
    await fillValidAddress()
    fireEvent.click(screen.getByRole('button', { name: '결제하기' }))

    await screen.findByRole('alert')
    expect(screen.getByRole('alert')).toHaveTextContent(message)
    // 입력값은 그대로 유지된다(재입력 방지, 확정 답변 "입력을 잃지 않는다").
    expect(screen.getByLabelText('수령인')).toHaveValue('홍길동')
    expect(screen.getByLabelText('연락처')).toHaveValue('010-1111-2222')
    expect(screen.getByLabelText('상세주소')).toHaveValue('101동 202호')
  })
})
