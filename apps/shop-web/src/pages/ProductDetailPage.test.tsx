/** @jest-environment jsdom */
// SR-304 — 상품 상세(/shop/products/:sku) 통합 테스트. `ProductListPage.test.tsx` 관례 그대로
// (jsdom, URL 분기 fetch mock, `@jest/globals`의 expect, 테스트별 고유 sku).
import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useLocation } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import ProductDetailPage from './ProductDetailPage'
import { calcDiscountRate } from '../features/shop/discountRate'
import { saveSession } from '../session'
import type { ProductDetail } from '../types'

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

// 테스트마다 인덱스로 유일화한다(STORY "테스트 격리" 절) — sku·상품명 문자열 충돌을 막는다.
// SR-353 — Product → ProductDetail. soldOut은 (overrides.stockQty ?? 5) <= 0으로 파생시켜, 기존
// 10여 개 호출부의 {stockQty:0} 오버라이드가 각 호출부를 고치지 않아도 자동으로 soldOut:true가 되게 한다.
function product(i: number, overrides: Partial<ProductDetail> = {}): ProductDetail {
  const stockQty = overrides.stockQty ?? 5
  return {
    sku: `sku-detail-${i}`, productName: `상세테스트상품${i}`, price: 10000 + i * 1000, stockQty,
    saleYn: 'Y', listPrice: null, imageUrl: null, soldOut: stockQty !== null && stockQty <= 0,
    ...overrides,
  }
}

const session = {
  memberId: 'm-detail-1', memberName: '홍길동', grade: 'NORMAL',
  apiKey: 'key-detail-1', refreshToken: 'r-detail-1', refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
}

describe('ProductDetailPage', () => {
  let fetchMock: jest.Mock<typeof fetch>
  let addItemCalls: { memberId: string; sku: string; qty: number }[]

  beforeEach(() => {
    fetchMock = jest.fn()
    addItemCalls = []
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear() // 세션 키가 다음 테스트로 새지 않게 한다.
  })

  afterEach(() => {
    localStorage.clear()
  })

  /** URL로 분기하는 fetch 라우터 — 상품 상세(`/api/products/:sku`)를 목록(`/api/products`)보다 먼저
   * 검사한다(두 경로 접두가 같다). SR-351 상품평 관련 라우팅(목록/작성/삭제·주문 자격 조회)은
   * `ProductDetailPage.reviews.test.tsx`가 자체 routeFetch로 따로 갖는다(file-size-cap 분리,
   * STORY-3 재작업 round2 사람 지시 2). */
  function routeFetch(handlers: {
    detail?: (sku: string) => Response | Promise<Response>
    list?: () => Response | Promise<Response>
    addItem?: (body: { memberId: string; sku: string; qty: number }) => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const [path] = url.split('?')

      if (path.startsWith('/api/products/')) {
        const sku = decodeURIComponent(path.slice('/api/products/'.length))
        if (!handlers.detail) throw new Error('unexpected detail call ' + url)
        return handlers.detail(sku)
      }
      if (path.startsWith('/api/products')) {
        return handlers.list ? handlers.list() : jsonResponse(200, [])
      }
      if (path.startsWith('/api/cart/items')) {
        const body = init?.body ? (JSON.parse(String(init.body)) as { memberId: string; sku: string; qty: number }) : { memberId: '', sku: '', qty: 0 }
        addItemCalls.push(body)
        return handlers.addItem ? handlers.addItem(body) : jsonResponse(200, { sku: body.sku, qty: body.qty, lineTotal: 0 })
      }
      if (path.startsWith('/api/cart')) return jsonResponse(200, { items: [] })
      throw new Error('unexpected url ' + url)
    })
  }

  // 재작업(round 2) QA 권고 2/사람 지시 2 — 사람 수정("바로 구매"는 /shop/cart로 이동하지 않는다)을
  // 고정하려면 실제 경로 변화 여부를 확인할 수단이 필요하다. `/shop/cart` 라우트에 감시용 문구를
  // 두고(존재해도 실제로 렌더되지 않아야 함), 현재 pathname을 항상 노출하는 프로브를 Routes 밖에
  // 둔다("location 해시 불변"의 테스트 환경 등가물 — 실제 앱은 HashRouter, 테스트는 MemoryRouter).
  // SR-353 — 재입고 알림 신청(비로그인) 리다이렉트 검증용으로 location.search도 노출한다.
  function LocationProbe() {
    const location = useLocation()
    return (
      <>
        <div data-testid="location-pathname">{location.pathname}</div>
        <div data-testid="location-search">{location.search}</div>
      </>
    )
  }

  function renderPage(sku: string) {
    return render(
      <MemoryRouter initialEntries={[`/shop/products/${sku}`]}>
        <LocationProbe />
        <Routes>
          <Route path="/shop/products/:sku" element={<ProductDetailPage />} />
          <Route path="/shop/products" element={<div>목록(placeholder)</div>} />
          <Route path="/shop/cart" element={<div>장바구니(placeholder)</div>} />
          <Route path="/login" element={<div>로그인(placeholder)</div>} />
        </Routes>
      </MemoryRouter>,
    )
  }

  const infoSection = () => screen.getByRole('region', { name: '상품 정보' })
  const relatedSection = () => screen.getByRole('region', { name: '함께 보면 좋은 상품' })

  // linked_tc: TC-FUNC-shop-010-01
  test('기본 로드 — 상품명·가격·재고상태·탭·고지표·관련상품이 렌더된다', async () => {
    const p1 = product(1)
    const p2 = product(2)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p1, sku }),
      list: () => jsonResponse(200, [p1, p2]),
    })
    renderPage(p1.sku)

    // "상품 정보" region은 product 로드가 끝난 뒤에야 존재한다(로딩 중엔 스켈레톤만 있음) — 먼저
    // screen 레벨에서 로드 완료를 기다린 뒤에 scoped 쿼리(infoSection())로 넘어간다.
    await screen.findByText(p1.productName)
    expect(within(infoSection()).getByText('11,000원')).toBeInTheDocument()
    // SR-353 — 재고 5개(10개 이하)는 "남은 수량 N개" 문구로 바뀐다(구 "재고 N개" 아님).
    expect(within(infoSection()).getByLabelText('재고 상태')).toHaveTextContent('남은 수량 5개')
    expect(screen.getByRole('tablist', { name: '상품 상세 탭 목록' })).toBeInTheDocument()
    expect(screen.getByRole('heading', { name: '상품정보제공고시' })).toBeInTheDocument()

    // 관련상품 레일 — 현재 sku(p1)는 제외되고 나머지(p2)만 보인다.
    await within(relatedSection()).findByText(p2.productName)
    expect(within(relatedSection()).queryByText(p1.productName)).not.toBeInTheDocument()
  })

  // SR-353 — 재고 12(>10)는 "재고 상태" 라벨 자체가 렌더되지 않는다(값이 아니라 엘리먼트를 숨김,
  // 확정 답변 scr_empty_state를 문자 그대로).
  test('재고 12개(10개 초과) — "재고 상태" 라벨이 렌더되지 않는다', async () => {
    const p = product(12, { stockQty: 12 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(within(infoSection()).queryByLabelText('재고 상태')).not.toBeInTheDocument()
  })

  // SR-353 재작업(round 2, 사람 코멘트 ③/QA CONCERNS #3) — stockQty=null && soldOut=false(현재
  // 백엔드가 만들지 않는 방어 조합)에서 [수량 증가] 버튼이 죽은 버튼이면 안 된다. 이전에는
  // `handleQtyChange`의 클램프 상한(`stockQty ?? 1`)과 `ProductInfoPanel`의 버튼 disabled 조건
  // (`stockQty ?? Infinity`)이 어긋나 버튼은 영원히 활성인데 눌러도 qty가 1에 묶여 아무 일도
  // 일어나지 않았다. 두 기본값을 1로 맞춰 qty=1에서 버튼이 처음부터 비활성이어야 한다.
  test('재고 null(soldOut=false 방어 조합) — 수량 증가 버튼이 처음부터 비활성이다', async () => {
    // round 3(QA CONCERNS carry-back) — 비로그인 렌더면 disabledReason(로그인 필요)이 이미
    // actionsDisabled를 true로 만들어 버튼을 비활성화시키므로, `?? 1` 방어 자체는 전혀 검증되지
    // 않는 거짓 보증이었다(ProductInfoPanel.tsx: `actionsDisabled || qty >= (product.stockQty ?? 1)`).
    // 로그인 상태로 렌더해 `?? 1` 단독으로 검증한다.
    saveSession(session)
    const p = product(16, { stockQty: null, soldOut: false })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(within(infoSection()).getByLabelText('수량')).toHaveValue(1)
    expect(within(infoSection()).getByRole('button', { name: '수량 증가' })).toBeDisabled()
  })

  // linked_tc: TC-FUNC-shop-010-02
  test('할인 있음 — 할인율·취소선이 discountRate.ts 계산값과 정확히 일치한다', async () => {
    const p = product(2, { price: 34000, listPrice: 42000 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    const expectedRate = calcDiscountRate(p.price, p.listPrice)
    expect(within(infoSection()).getByText(`${expectedRate}%`)).toBeInTheDocument()
    expect(within(infoSection()).getByText('42,000원')).toBeInTheDocument() // 취소선 정가
  })

  // linked_tc: TC-FUNC-shop-010-03
  // SR-353 재작성 — 품절이면 장바구니 담기/바로 구매 버튼이 아예 사라지고(비활성이 아니라 부재),
  // [재입고 알림 신청] 버튼이 대신 보인다. 기존 role="note" "품절된 상품입니다"는 더 이상 렌더되지
  // 않는다(품절이 disabledReason 경로에서 빠졌다, STORY "계약" 절).
  test('품절 — 담기/구매 버튼이 사라지고 재입고 알림 신청 버튼이 보인다', async () => {
    saveSession(session) // 세션은 있어도(로그인 사유와 섞이지 않게) 재고 0이면 품절 표시가 뜨는지만 본다.
    const p = product(3, { stockQty: 0 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(within(infoSection()).getByLabelText('재고 상태')).toHaveTextContent('품절')
    expect(within(infoSection()).queryByRole('button', { name: '장바구니 담기' })).not.toBeInTheDocument()
    expect(within(infoSection()).queryByRole('button', { name: '바로 구매' })).not.toBeInTheDocument()
    expect(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' })).toBeInTheDocument()
    expect(within(infoSection()).queryByRole('note')).not.toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-shop-010-04
  test('이미지 없음 — 대체 영역만 렌더되고 <img>는 없다', async () => {
    const p = product(4, { imageUrl: null })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(screen.getByLabelText(`${p.productName} 대표이미지 없음`)).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-shop-010-05
  test('상품 없음(404) — 안내 화면이 뜨고 [목록으로] 클릭 시 목록으로 이동한다', async () => {
    routeFetch({ detail: () => jsonResponse(404, { message: '상품 없음' }, false) })
    renderPage('sku-detail-5')

    await screen.findByText('상품을 찾을 수 없습니다')
    fireEvent.click(screen.getByRole('button', { name: '목록으로' }))
    await screen.findByText('목록(placeholder)')
  })

  // linked_tc: TC-FUNC-shop-010-06
  test('조회 실패(네트워크/500) — 안내+[다시 시도], 클릭 시 재요청이 성공한다', async () => {
    let calls = 0
    const p = product(6)
    routeFetch({
      detail: sku => {
        calls += 1
        return calls === 1 ? jsonResponse(500, { message: 'boom' }, false) : jsonResponse(200, { ...p, sku })
      },
    })
    renderPage(p.sku)

    await screen.findByText('불러오지 못했습니다')
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }))
    await screen.findByText(p.productName)
    expect(calls).toBe(2)
  })

  // linked_tc: TC-FUNC-shop-010-07
  test('담기 성공 — addStatus 성공 표시 + Gnb 배지 수량이 qty만큼 정확히 증가한다', async () => {
    saveSession(session)
    const p = product(7, { stockQty: 5 })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
      addItem: body => jsonResponse(200, { sku: body.sku, qty: body.qty, lineTotal: body.qty * p.price }),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await screen.findByLabelText('장바구니 0개') // 세션 있음 → GET /api/cart 조회(빈 목록) 반영 확인

    // 수량을 3으로 올린 뒤 담기 — "정확한 수치"로 증가분을 검증하기 위함(대략 증가 아님).
    fireEvent.click(within(infoSection()).getByRole('button', { name: '수량 증가' }))
    fireEvent.click(within(infoSection()).getByRole('button', { name: '수량 증가' }))
    expect(within(infoSection()).getByLabelText('수량')).toHaveValue(3)

    fireEvent.click(within(infoSection()).getByRole('button', { name: '장바구니 담기' }))

    await within(infoSection()).findByText('장바구니에 담았습니다')
    expect(addItemCalls).toEqual([{ memberId: session.memberId, sku: p.sku, qty: 3 }])
    expect(screen.getByLabelText('장바구니 3개')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-shop-010-08
  test('담기 실패(409) — 오류 사유 문구 노출 + 버튼 재활성(재시도 가능)', async () => {
    saveSession(session)
    const p = product(8)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
      addItem: () => jsonResponse(409, { message: '재고 초과: 가용 0, 요청 1' }, false),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(within(infoSection()).getByRole('button', { name: '장바구니 담기' }))

    await within(infoSection()).findByText('재고가 부족하거나 판매중지된 상품입니다')
    expect(within(infoSection()).getByRole('button', { name: '장바구니 담기' })).not.toBeDisabled()
  })

  // 재작업(round 2) QA 권고 2/사람 지시 2 — 사람 수정("바로 구매"는 담기와 동일한 addCartItem을
  // 호출하지만 /shop/cart로 이동하지 않는다, SR-305 라우트가 아직 없어 이동시키지 않는다)이 코드
  // 리뷰로만 보증되고 있었다(다음 SR에서 navigate가 몰래 추가돼도 아무 게이트도 못 잡음). pathname이
  // 클릭 전후로 정확히 동일함을 단언해 회귀를 게이트로 고정한다.
  // linked_tc: TC-FUNC-shop-010-09
  test('바로 구매 — addCartItem은 호출되지만 라우트 이동은 없다', async () => {
    saveSession(session)
    const p = product(11, { stockQty: 5 })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
      addItem: body => jsonResponse(200, { sku: body.sku, qty: body.qty, lineTotal: body.qty * p.price }),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    const pathnameBefore = screen.getByTestId('location-pathname').textContent
    expect(pathnameBefore).toBe(`/shop/products/${p.sku}`)

    fireEvent.click(within(infoSection()).getByRole('button', { name: '바로 구매' }))

    await within(infoSection()).findByText('바로 구매 대신 장바구니에 담았습니다')
    expect(addItemCalls).toEqual([{ memberId: session.memberId, sku: p.sku, qty: 1 }])
    // 이동이 없었다 — pathname이 그대로고, /shop/cart 라우트의 감시용 문구도 렌더되지 않았다.
    expect(screen.getByTestId('location-pathname')).toHaveTextContent(`/shop/products/${p.sku}`)
    expect(screen.queryByText('장바구니(placeholder)')).not.toBeInTheDocument()
    expect(screen.getByText(p.productName)).toBeInTheDocument()
  })

  // 실패 사례집 대조(SR-302 #1) — 연타 검증은 act() 한 스코프 안에서 flush 없이 묶어 발사한다.
  // 가드(`ProductDetailPage.tsx`의 `if (inFlightRef.current) return`)를 임시로 제거해 이 테스트가
  // 실제로 addItemCalls.length===2로 깨지는지 먼저 확인한 뒤 가드를 복원했다(Dev 기록 참조).
  // linked_tc: TC-FUNC-shop-010-10
  test('연타 방지 — 짧은 간격 2회 클릭에도 addCartItem 호출이 1회만 나간다', async () => {
    saveSession(session)
    const p = product(9)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    const addBtn = within(infoSection()).getByRole('button', { name: '장바구니 담기' })
    act(() => {
      addBtn.click()
      addBtn.click()
    })

    await within(infoSection()).findByText('장바구니에 담았습니다')
    expect(addItemCalls).toHaveLength(1)
  })

  // linked_tc: TC-FUNC-shop-010-11
  test('세션 없음 — 담기/구매 버튼이 비활성이고 클릭해도 addCartItem이 호출되지 않는다', async () => {
    const p = product(10)
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    const addBtn = within(infoSection()).getByRole('button', { name: '장바구니 담기' })
    const buyBtn = within(infoSection()).getByRole('button', { name: '바로 구매' })
    expect(addBtn).toBeDisabled()
    expect(buyBtn).toBeDisabled()

    fireEvent.click(addBtn)
    expect(addItemCalls).toHaveLength(0)
  })

  // SR-353 — 재입고 알림 신청(로그인 상태) — 버튼이 "신청됨"으로 바뀌고 완료 토스트가 뜬다.
  test('재입고 알림 신청(로그인) — 버튼이 신청됨으로 바뀌고 토스트가 뜬다', async () => {
    saveSession(session)
    const p = product(13, { stockQty: 0 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' }))

    expect(within(infoSection()).getByRole('button', { name: '신청됨' })).toBeDisabled()
    expect(await screen.findByRole('status')).toHaveTextContent('재입고 알림 신청이 완료되었습니다')
  })

  // SR-353 — 재입고 알림 신청(비로그인) — 로그인 판정이 먼저다(부수효과보다 앞선 순서, STORY
  // "순서·보안" 절). 즉시 로그인 화면으로 이동하고 redirect 쿼리가 정확히 붙으며, 네트워크 호출은
  // 새로 나가지 않는다(신청됨으로 낙관 전환 금지).
  test('재입고 알림 신청(비로그인) — 로그인 화면으로 이동하고 redirect 쿼리가 붙는다', async () => {
    const p = product(14, { stockQty: 0 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    const callsBefore = fetchMock.mock.calls.length
    fireEvent.click(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' }))

    expect(screen.getByTestId('location-pathname')).toHaveTextContent('/login')
    expect(screen.getByTestId('location-search')).toHaveTextContent(
      '?redirect=' + encodeURIComponent('/shop/products/' + p.sku),
    )
    expect(fetchMock.mock.calls.length).toBe(callsBefore)
  })

  // 실패 사례집 대조(SR-302 #1) — 연타 검증은 act() 한 스코프 안에서 flush 없이 묶어 발사한다. 가드
  // (`ProductDetailPage.tsx`의 `notifyInFlightRef`)를 임시로 제거해 이 테스트가 실제로 토스트 2개로
  // 깨지는지 먼저 확인한 뒤 가드를 복원했다(Dev 기록 참조).
  test('재입고 알림 신청 연타 방지 — 토스트가 1개만 뜬다', async () => {
    saveSession(session)
    const p = product(15, { stockQty: 0 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    const notifyBtn = within(infoSection()).getByRole('button', { name: '재입고 알림 신청' })
    act(() => {
      notifyBtn.click()
      notifyBtn.click()
    })

    const toasts = await screen.findAllByRole('status')
    expect(toasts).toHaveLength(1)
  })

  // linked_tc: TC-FUNC-shop-010-17
  // SR-353 — AC-5: stockQty=null(재고 정보 미상)이면 "수량 표기"(= aria-label="재고 상태" 라벨,
  // 예: "남은 수량 5개"/"품절") 자체가 렌더되지 않는다는 것을 직접 검증한다(AC-5 "표기 자체를
  // 숨긴다" 문자 그대로, `ProductInfoPanel.tsx`의 `stockLabel !== null &&` 게이트).
  // 구매 수량 선택기(aria-label="수량", 수량 감소/증가 버튼)는 stockQty 유무와 무관하게 계속
  // 렌더되는 것이 구현 계획·QA round3 PASS의 전제라 그 부재를 단언하지 않는다(round 4 재작업 —
  // 이전 버전은 이 둘을 혼동해 잘못된 단언을 했었다).
  test('재고 정보 미상(stockQty=null && soldOut=false) — "재고 상태" 라벨이 렌더되지 않는다', async () => {
    saveSession(session)
    const p = product(17, { stockQty: null, soldOut: false })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    // "재고 상태" 라벨 자체가 렌더되지 않음 (값이 아니라 엘리먼트를 숨김)
    expect(within(infoSection()).queryByLabelText('재고 상태')).not.toBeInTheDocument()
    // 수량 선택기는 stockQty=null 여부와 무관하게 계속 렌더된다(삭제 대상 아님)
    expect(within(infoSection()).getByLabelText('수량')).toBeInTheDocument()
  })
})
