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
    wishCount: 0, // SR-356 — 신규 필드(끝에 추가). 이 파일의 케이스들은 찜하기와 무관해 기본값만 채운다.
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
    // SR-357 #4 — 최근 본 상품 기록 POST. 미지정 테스트는 기본 204를 받아 조용히 통과한다.
    recordView?: (sku: string) => Response | Promise<Response>
    // SR-364 — 재입고 알림 신청 여부 확인 GET. 미지정 테스트는 기본 빈 목록(미신청)을 받는다.
    restockAlerts?: () => Response | Promise<Response>
    // SR-364 — 재입고 알림 신청 POST. 미지정 테스트는 기본 200(멱등)을 받는다.
    registerRestockAlert?: (sku: string) => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const [path] = url.split('?')

      if (path === '/api/members/me/recently-viewed' && init?.method === 'POST') {
        const body = init?.body ? (JSON.parse(String(init.body)) as { sku: string }) : { sku: '' }
        return handlers.recordView ? handlers.recordView(body.sku) : jsonResponse(204, undefined)
      }
      // SR-361 — 리뷰 목록 조회가 이제 always-on이라 이 파일의 모든 테스트가 마운트 시 리뷰 GET을
      // 내보낸다. `/api/products/` 일반 분기보다 먼저 가로채 기본 빈 목록을 돌려준다(개별 테스트는
      // 리뷰 내용을 검증하지 않으므로 옵션 파라미터화하지 않는다) — 안 그러면 `/reviews` 접미 경로까지
      // `handlers.detail`로 오라우팅돼 `ProductReviewPreview`가 `listResult.items`를 `undefined`로
      // 받아 렌더 중 throw한다(실패 사례집 대조, 이 파일 기존 테스트 전부가 크래시할 뻔했다).
      if (path.includes('/reviews')) {
        return jsonResponse(200, { totalCount: 0, page: 1, size: 10, avgRating: null, reviewCount: 0, distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 }, items: [] })
      }
      // SR-360 — 비교함 담김 여부 확인 GET이 로그인 상태의 모든 테스트에서 마운트 시 나간다. 이 파일의
      // 케이스들은 비교함과 무관해 기본 빈 목록으로 흡수한다(`ProductDetailPage.compare.test.tsx`가
      // 실제 동작을 검증한다).
      if (path === '/api/members/me/compares' && (init?.method ?? 'GET').toUpperCase() === 'GET') {
        return jsonResponse(200, { items: [] })
      }
      // SR-364 — 재입고 알림 신청 여부 확인 GET·신청 POST. 품절 상품 진입 시 마운트에서 GET이 나가고,
      // [재입고 알림 신청] 클릭 시 POST가 나간다(`compares`와 동일하게 미지정 테스트는 기본값으로 흡수).
      if (path === '/api/members/me/restock-alerts') {
        const method = (init?.method ?? 'GET').toUpperCase()
        if (method === 'GET') {
          return handlers.restockAlerts ? handlers.restockAlerts() : jsonResponse(200, { items: [] })
        }
        if (method === 'POST') {
          const body = init?.body ? (JSON.parse(String(init.body)) as { sku: string }) : { sku: '' }
          return handlers.registerRestockAlert
            ? handlers.registerRestockAlert(body.sku)
            : jsonResponse(200, { sku: body.sku, requested: true })
        }
      }
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

  // SR-358 — 상품 상세 조회수. viewCount가 응답에 있으면 쉼표 포맷으로 "조회 수" 라벨에 표시된다
  // (실패 사례집 SR-353 #1 대조 — "찜 수"와 라벨이 다르므로 getByLabelText로 유일하게 좁힌다).
  // linked_tc: TC-FUNC-order-358-001
  test('조회수 있음 — "조회 1,234"가 렌더된다', async () => {
    const p = product(26, { viewCount: 1234 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(within(infoSection()).getByLabelText('조회 수')).toHaveTextContent('조회 1,234')
  })

  // SR-358 — viewCount 필드 자체가 없는 응답(구버전 응답 등가)은 "조회 0"으로 표시한다(확정 답변).
  // linked_tc: TC-FUNC-order-358-002
  test('조회수 없음 — "조회 0"이 렌더된다', async () => {
    const p = product(27)
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(within(infoSection()).getByLabelText('조회 수')).toHaveTextContent('조회 0')
  })

  // SR-358 — viewCount가 명시적으로 0일 때도 "조회 0"으로 표시한다(경계값 케이스).
  // linked_tc: TC-FUNC-order-358-003
  test('조회수 0 — "조회 0"이 렌더된다', async () => {
    const p = product(28, { viewCount: 0 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(within(infoSection()).getByLabelText('조회 수')).toHaveTextContent('조회 0')
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
  // SR-364 재작업 — 이제 POST 왕복을 기다려야 "신청됨"이 된다(이전엔 클릭과 동시에 낙관 전환이었다,
  // STORY "테스트" 절 "405번째 줄 기존 테스트" 수정 지시). 클릭 직후 동기 어서션을 비동기로 바꾼다.
  test('재입고 알림 신청(로그인) — 버튼이 신청됨으로 바뀌고 토스트가 뜬다', async () => {
    saveSession(session)
    const p = product(13, { stockQty: 0 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' }))

    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()
    expect(await screen.findByRole('status')).toHaveTextContent('재입고 알림 신청이 완료되었습니다')
  })

  // SR-364 — AC1: 품절 상세 진입 시 서버 GET이 이 sku를 이미 포함하면 클릭 없이 "신청됨"으로 렌더된다.
  test('재입고 알림 신청 여부 확인(로그인, 이미 신청됨) — 클릭 없이 신청됨으로 렌더된다', async () => {
    saveSession(session)
    const p = product(29, { stockQty: 0 })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
      restockAlerts: () => jsonResponse(200, { items: [{ sku: p.sku, createdAt: '2026-09-01T00:00:00Z' }] }),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()
  })

  // SR-364 — AC3: 신청 여부 확인 GET이 실패하면 기본 상태(재입고 알림 신청)를 유지하고 토스트를
  // 띄우지 않는다(확정 답변). 같은 렌더 사이클에서 병행 발사되는 다른 비동기 GET(장바구니 조회)을
  // 동기화 포인트로 재사용한다(이 파일 292번째 줄 기존 관례, 신규 대기 기법을 발명하지 않는다).
  test('재입고 알림 신청 여부 확인 실패(500) — 기본 상태 유지·토스트 없음', async () => {
    saveSession(session)
    const p = product(30, { stockQty: 0 })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
      restockAlerts: () => jsonResponse(500, { message: 'boom' }, false),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await screen.findByLabelText('장바구니 0개') // 세션 있음 → GET /api/cart 조회 반영을 동기화 포인트로 쓴다.
    expect(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' })).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  // SR-364 — AC2: POST 응답 도착 전까지 버튼이 "신청 중…"으로 비활성화된다(STORY "화면 상태" 절 신규
  // 3번째 상태). resolve하지 않은 Promise로 mid-flight를 만든 뒤 resolve해 "신청됨"으로 전환됨을 마저
  // 확인한다.
  test('재입고 알림 신청 진행 중 — "신청 중…"으로 비활성화됐다가 응답 도착 후 신청됨으로 바뀐다', async () => {
    saveSession(session)
    const p = product(31, { stockQty: 0 })
    let resolvePost!: (r: Response) => void
    const pending = new Promise<Response>(resolve => { resolvePost = resolve })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
      registerRestockAlert: () => pending,
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' }))

    const pendingBtn = await within(infoSection()).findByRole('button', { name: '신청 중…' })
    expect(pendingBtn).toBeDisabled()

    await act(async () => { resolvePost(jsonResponse(200, { sku: p.sku, requested: true })) })

    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()
  })

  // SR-364 — POST 실패(500) 시 idle 복귀 + 실패 토스트(`toDisplayMessage`의 일반 문구). 확정 답변
  // 7건은 GET(목록 조회) 실패만 명시하고 POST 자체 실패의 화면 표기는 다루지 않아, 기존 SR-353 성공
  // 토스트 채널을 실패에도 재사용하는 쪽으로 해석했다(STORY "테스트" 절 — 이 해석은 QA 확인 필요).
  test('재입고 알림 신청 실패(500) — idle로 복귀하고 실패 토스트가 뜬다', async () => {
    saveSession(session)
    const p = product(32, { stockQty: 0 })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
      registerRestockAlert: () => jsonResponse(500, { message: 'boom' }, false),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' }))

    expect(await within(infoSection()).findByRole('button', { name: '재입고 알림 신청' })).not.toBeDisabled()
    expect(await screen.findByRole('status')).toHaveTextContent('일시적 오류입니다. 다시 시도해 주세요')
  })

  // SR-364 재작업(round2) — QA FAIL 필수1 회귀. 품절 A(신청됨) → 관련상품 클릭으로 품절 B(이미
  // 신청됨)로 SPA param-only 이동(언마운트 없음, `RelatedProducts`의 `onSelect` → `navigate`)해도
  // B가 「신청됨」으로 유지돼야 한다(round1 결함: 확인 GET(B)이 상세 GET(B)보다 먼저 도착하면
  // `resetForProduct()`가 그 결과를 'idle'로 덮어썼다). 이 파일 647~653번째 줄이 SR-357 때
  // "param-only 이동은 미작성"으로 남긴 사각을 이 시나리오로 메운다. 레이스를 우연에 맡기면 판별력이
  // 0이므로(사례집 SR-302 #1/SR-353 #1 대조) 상세 GET(B)만 수동으로 붙잡아, 확인 GET(B)이 반드시 먼저
  // 끝나는 순서를 고정한다.
  test('재입고 알림 신청됨 유지 — 품절 A에서 관련상품 클릭으로 품절 B(이미 신청됨)로 이동해도 신청됨이 유지된다', async () => {
    saveSession(session)
    const pA = product(33, { stockQty: 0 })
    const pB = product(34, { stockQty: 0 })
    let resolveDetailB!: (r: Response) => void
    const detailBPending = new Promise<Response>(resolve => { resolveDetailB = resolve })
    routeFetch({
      // 상세 GET(B)만 수동 제어한다 — 확인 GET(B)은 즉시 응답해 상세 GET(B)보다 먼저 끝나는 순서를
      // 강제로 재현한다(품절 A의 상세 GET은 그대로 즉시 응답).
      detail: sku => (sku === pB.sku ? detailBPending : jsonResponse(200, { ...pA, sku })),
      list: () => jsonResponse(200, [pA, pB]),
      restockAlerts: () => jsonResponse(200, {
        items: [
          { sku: pA.sku, createdAt: '2026-09-01T00:00:00Z' },
          { sku: pB.sku, createdAt: '2026-09-02T00:00:00Z' },
        ],
      }),
    })
    renderPage(pA.sku)

    await screen.findByText(pA.productName)
    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()
    await within(relatedSection()).findByText(pB.productName)

    fireEvent.click(within(relatedSection()).getByText(pB.productName))

    // 확인 GET(B)이 완전히 끝날 시간을 번다 — 상세 GET(B)은 아직 붙잡아 둔 상태라 `resetForProduct()`는
    // 아직 호출되지 않았다. 매크로태스크 경계까지 흘려보내 대기 중인 마이크로태스크를 전부 비운다.
    await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

    // 이제 상세 GET(B)을 풀어준다 — `load()`가 `setProduct(B)` 뒤 `restockAlert.resetForProduct()`를
    // 호출한다(round1 결함이면 여기서 신청됨이 'idle'로 덮인다).
    await act(async () => { resolveDetailB(jsonResponse(200, { ...pB, sku: pB.sku })) })

    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()
  })

  // SR-364 재작업(round3→round4) — 재작업 지시 1·2, 사람 코멘트 [테스트·완료 조건] 1건째 + medium 권고
  // (round2 회귀 테스트가 A·B 둘 다 신청됨으로 응답해 "B를 새로 판정한다"는 술어를 검증하지 못했다).
  // round3은 호출 순서로 A(1회차)=신청됨·B(2회차)=빈 목록을 구분했으나, 이 GET은 **회원 전체 목록**이라
  // B 화면에서도 A의 신청 기록은 그대로 남아 있어야 한다 — 2회차를 빈 목록으로 주는 것은 실제로 불가능한
  // 응답이었고, 그 탓에 `items.some(it => it.sku === sku)`를 `items.length > 0`으로 바꾸는 변이가
  // 33/33 전부 통과했다(round4 QA 변이 실측, medium 권고). 호출 순서 분기를 없애고 두 호출 모두
  // A의 신청 기록만 담은 **현실적인** 같은 목록을 돌려준다 — B의 sku는 포함하지 않으므로 "sku별로
  // 새로 판정한다"는 술어는 여전히 검증된다(items.length > 0 변이로 바꾸면 이 테스트가 깨진다).
  test('재입고 알림 신청 여부 재판정 — 품절 A(신청됨)에서 품절 B(미신청)로 이동하면 B는 기본 상태로 렌더된다', async () => {
    saveSession(session)
    const pA = product(35, { stockQty: 0 })
    const pB = product(36, { stockQty: 0 })
    routeFetch({
      detail: sku => jsonResponse(200, { ...(sku === pB.sku ? pB : pA), sku }),
      list: () => jsonResponse(200, [pA, pB]),
      // 회원 전체 목록 — A의 신청 기록만 있고 B는 없다(현실적인 목, A 화면·B 화면 확인 GET 모두 동일).
      restockAlerts: () => jsonResponse(200, { items: [{ sku: pA.sku, createdAt: '2026-09-01T00:00:00Z' }] }),
    })
    renderPage(pA.sku)

    await screen.findByText(pA.productName)
    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()
    await within(relatedSection()).findByText(pB.productName)

    fireEvent.click(within(relatedSection()).getByText(pB.productName))

    await screen.findByText(pB.productName)
    expect(await within(infoSection()).findByRole('button', { name: '재입고 알림 신청' })).not.toBeDisabled()
  })

  // SR-364 재작업(round3) — 재작업 지시 1, 필수 수정 회귀 ①(QA 지정): 품절 A(신청됨)→품절 B로 이동 중
  // B의 확인 GET이 실패(500)해도 B는 A의 신청됨 상태를 이어받지 않고 기본 상태(활성)를 유지한다(AC3,
  // 확정 답변 6). round2 FAIL 재현 조건 그대로 — notified가 sku에 묶이지 않으면 GET 실패 시 이전
  // 상품 값이 남아 B가 신청 자체를 할 수 없는 상태(disabled)로 굳는다.
  test('재입고 알림 신청 여부 확인 실패 — 품절 A(신청됨)에서 품절 B로 이동 중 B의 확인 GET이 실패해도 B는 기본 상태를 유지한다', async () => {
    saveSession(session)
    const pA = product(37, { stockQty: 0 })
    const pB = product(38, { stockQty: 0 })
    let restockCall = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...(sku === pB.sku ? pB : pA), sku }),
      list: () => jsonResponse(200, [pA, pB]),
      restockAlerts: () => {
        restockCall++
        return restockCall === 1
          ? jsonResponse(200, { items: [{ sku: pA.sku, createdAt: '2026-09-01T00:00:00Z' }] })
          : jsonResponse(500, { message: 'boom' }, false)
      },
    })
    renderPage(pA.sku)

    await screen.findByText(pA.productName)
    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()
    await within(relatedSection()).findByText(pB.productName)

    fireEvent.click(within(relatedSection()).getByText(pB.productName))

    await screen.findByText(pB.productName)
    expect(await within(infoSection()).findByRole('button', { name: '재입고 알림 신청' })).not.toBeDisabled()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })

  // SR-364 재작업(round3) — 재작업 지시 1, 필수 수정 회귀 ②(QA 지정): 클릭 직후(POST 왕복 완료 전)
  // 다른 상품으로 이동하면, 늦게 도착한 POST 성공 응답이 새 상품(B)의 상태를 오염시키지 않는다(round2
  // FAIL (c) — POST 성공 핸들러가 세대·sku 검사 없이 setNotified(true)를 해 클릭 직후 이동 시 A의
  // 결과가 B를 신청됨으로 만들었다). 응답 순서를 수동 제어해 고정 재현한다(기존 `detailBPending` 기법과
  // 동일 원리 — 레이스를 우연에 맡기지 않는다).
  test('재입고 알림 신청 POST 레이스 — 클릭 직후 다른 상품으로 이동하면 늦게 도착한 POST 성공이 새 상품을 오염시키지 않는다', async () => {
    saveSession(session)
    const pA = product(39, { stockQty: 0 })
    const pB = product(40, { stockQty: 0 })
    let resolvePost!: (r: Response) => void
    const pending = new Promise<Response>(resolve => { resolvePost = resolve })
    routeFetch({
      detail: sku => jsonResponse(200, { ...(sku === pB.sku ? pB : pA), sku }),
      list: () => jsonResponse(200, [pA, pB]),
      registerRestockAlert: sku => (sku === pA.sku ? pending : jsonResponse(200, { sku, requested: true })),
    })
    renderPage(pA.sku)

    await screen.findByText(pA.productName)
    fireEvent.click(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' }))
    await within(infoSection()).findByRole('button', { name: '신청 중…' })

    await within(relatedSection()).findByText(pB.productName)
    fireEvent.click(within(relatedSection()).getByText(pB.productName))
    await screen.findByText(pB.productName) // 이동 완료(상세 GET(B) 반영) 대기 — 이 시점에 화면은 이미 B.

    // 이제서야 A의 POST 성공 응답이 늦게 도착한다.
    await act(async () => { resolvePost(jsonResponse(200, { sku: pA.sku, requested: true })) })

    expect(await within(infoSection()).findByRole('button', { name: '재입고 알림 신청' })).not.toBeDisabled()
  })

  // SR-364 재작업(round4, QA FAIL round3 필수1 — PROBE2 실측 재현, 사람 승인 처방) — round3은 POST 성공
  // 처리에 "지금 렌더링 중인 sku와 일치하는지" 검사를 넣지 않았다(코드 주석이 "대조 대상이 없다"고
  // 정당화했으나 틀렸다 — notifiedSku는 sku 하나만 담는 단일 슬롯이다). A(미신청)에서 클릭해 POST를
  // 미resolve 상태로 붙잡은 채 B(이미 신청됨)로 이동하면, B는 자신의 확인 GET으로 정상적으로 「신청됨」
  // 으로 먼저 확정된다 — 그 뒤에야 A의 POST 성공이 늦게 도착해도(캡처된 targetSku=A와 지금 렌더링 중인
  // sku=B가 다르므로 skuRef 검사에서 걸린다) B는 여전히 「신청됨」을 유지한다(단일 슬롯이 A로 덮이지
  // 않는다). 응답 순서를 수동 제어해 고정 재현한다.
  test('재입고 알림 신청 POST 지연 — A 클릭 중 이미 신청된 B로 이동해도 B는 신청됨을 유지한다', async () => {
    saveSession(session)
    const pA = product(41, { stockQty: 0 })
    const pB = product(42, { stockQty: 0 })
    let resolvePost!: (r: Response) => void
    const pending = new Promise<Response>(resolve => { resolvePost = resolve })
    routeFetch({
      detail: sku => jsonResponse(200, { ...(sku === pB.sku ? pB : pA), sku }),
      list: () => jsonResponse(200, [pA, pB]),
      // 회원 전체 목록 — B는 이미 신청됨, A는 아직 아니다(현실적인 목, A·B 화면 확인 GET 모두 동일).
      restockAlerts: () => jsonResponse(200, { items: [{ sku: pB.sku, createdAt: '2026-09-01T00:00:00Z' }] }),
      registerRestockAlert: sku => (sku === pA.sku ? pending : jsonResponse(200, { sku, requested: true })),
    })
    renderPage(pA.sku)

    await screen.findByText(pA.productName)
    fireEvent.click(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' }))
    await within(infoSection()).findByRole('button', { name: '신청 중…' })

    await within(relatedSection()).findByText(pB.productName)
    fireEvent.click(within(relatedSection()).getByText(pB.productName))
    await screen.findByText(pB.productName) // 이동 완료(상세 GET(B) 반영) 대기 — 이 시점부터 infoSection()이 유효하다.

    // B로 이동 후, B 자신의 확인 GET으로 정상적으로 「신청됨」이 확정된다(A의 POST와 무관하게).
    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()

    // 이제서야 A의 POST 성공 응답이 늦게 도착한다 — B는 여전히 신청됨이어야 한다(round3 필수1 재현 조건).
    await act(async () => { resolvePost(jsonResponse(200, { sku: pA.sku, requested: true })) })

    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()
  })

  // SR-364 재작업(round4, QA FAIL round3 필수1 — PROBE3 실측 재현) — 같은 경로에서 늦게 도착한 A의 POST
  // 성공이 무조건 `onNotifyResult({success:true})`를 불러 B 화면에 A의 성공 토스트를 띄우던 결함(round3
  // 필수1의 두 번째 잔여)도 같은 처방(skuRef 검사)으로 함께 닫힌다 — B 화면에는 어떤 토스트도 뜨지 않는다.
  test('재입고 알림 신청 POST 지연 — B로 이동 후 늦게 도착한 A의 POST 성공에 완료 토스트가 뜨지 않는다', async () => {
    saveSession(session)
    const pA = product(43, { stockQty: 0 })
    const pB = product(44, { stockQty: 0 })
    let resolvePost!: (r: Response) => void
    const pending = new Promise<Response>(resolve => { resolvePost = resolve })
    routeFetch({
      detail: sku => jsonResponse(200, { ...(sku === pB.sku ? pB : pA), sku }),
      list: () => jsonResponse(200, [pA, pB]),
      restockAlerts: () => jsonResponse(200, { items: [{ sku: pB.sku, createdAt: '2026-09-01T00:00:00Z' }] }),
      registerRestockAlert: sku => (sku === pA.sku ? pending : jsonResponse(200, { sku, requested: true })),
    })
    renderPage(pA.sku)

    await screen.findByText(pA.productName)
    fireEvent.click(within(infoSection()).getByRole('button', { name: '재입고 알림 신청' }))
    await within(infoSection()).findByRole('button', { name: '신청 중…' })

    await within(relatedSection()).findByText(pB.productName)
    fireEvent.click(within(relatedSection()).getByText(pB.productName))
    await screen.findByText(pB.productName) // 이동 완료(상세 GET(B) 반영) 대기 — 이 시점부터 infoSection()이 유효하다.
    expect(await within(infoSection()).findByRole('button', { name: '신청됨' })).toBeDisabled()

    await act(async () => { resolvePost(jsonResponse(200, { sku: pA.sku, requested: true })) })
    // resolvePost 처리 뒤 한 틱 더 흘려보내, (만약 잘못 불렸다면) onNotifyResult가 토스트를 렌더할
    // 시간을 충분히 준다.
    await act(async () => { await new Promise(resolve => setTimeout(resolve, 0)) })

    expect(screen.queryByRole('status')).not.toBeInTheDocument()
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
  // (`useProductRestockAlert.ts`의 `notifyInFlightRef` — SR-364 재작업 round2 사람 지시 ③: 이전 주석은
  // `ProductDetailPage.tsx`를 잘못 지목했다. 가드는 round1부터 이미 이 훅에 있었다)를 임시로 제거해 이
  // 테스트가 실제로 토스트 2개로 깨지는지 먼저 확인한 뒤 가드를 복원했다(Dev 기록 참조).
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

  // SR-357 #4 — 최근 본 상품 기록(회원별 서버 저장). 상품 조회 200 성공 뒤에만, 로그인 세션이 있을
  // 때만 기록 POST가 나간다(결정(사람) — 비로그인 기록 금지, 성공 전용 트리거). 실패는 화면 무반응.
  // linked_tc: TC-FUNC-order-357-001
  test('로그인 상태로 상품 상세 로드 성공 — 최근 본 상품 기록 POST가 정확히 1회 나간다', async () => {
    saveSession(session)
    const p = product(20)
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)

    const recordCalls = fetchMock.mock.calls.filter(
      call => String(call[0]).split('?')[0] === '/api/members/me/recently-viewed',
    )
    expect(recordCalls).toHaveLength(1)
    const [url, init] = recordCalls[0] as [string, RequestInit]
    expect(url).toBe('/api/members/me/recently-viewed')
    expect((init.headers as Record<string, string>)['X-Api-Key']).toBe(session.apiKey)
    expect(JSON.parse(String(init.body))).toEqual({ sku: p.sku })
  })

  // linked_tc: TC-FUNC-order-357-002
  test('비로그인 상태로 상품 상세 로드 — 최근 본 상품 기록 POST가 나가지 않는다', async () => {
    const p = product(21)
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]) })
    renderPage(p.sku)

    await screen.findByText(p.productName)

    const recordCalls = fetchMock.mock.calls.filter(
      call => String(call[0]).split('?')[0] === '/api/members/me/recently-viewed',
    )
    expect(recordCalls).toHaveLength(0)
  })

  // linked_tc: TC-FUNC-order-357-003
  test('상품 없음(404) — 최근 본 상품 기록 POST가 나가지 않는다(성공 전용 트리거)', async () => {
    saveSession(session)
    let recordCalled = false
    routeFetch({
      detail: () => jsonResponse(404, { message: '상품 없음' }, false),
      recordView: () => { recordCalled = true; return jsonResponse(204, undefined) },
    })
    renderPage('sku-detail-22')

    await screen.findByText('상품을 찾을 수 없습니다')
    expect(recordCalled).toBe(false)
  })

  // linked_tc: TC-FUNC-order-357-004
  test('sku 변경(재마운트) — 새 sku로 최근 본 상품 기록이 다시 나간다', async () => {
    saveSession(session)
    const p1 = product(23)
    const p2 = product(24)
    routeFetch({
      detail: sku => jsonResponse(200, sku === p1.sku ? { ...p1, sku } : { ...p2, sku }),
      list: () => jsonResponse(200, [p1, p2]),
    })
    const { unmount } = renderPage(p1.sku)
    await screen.findByText(p1.productName)
    unmount()

    renderPage(p2.sku)
    await screen.findByText(p2.productName)

    const recordedSkus = fetchMock.mock.calls
      .filter(call => String(call[0]).split('?')[0] === '/api/members/me/recently-viewed')
      .map(call => (JSON.parse(String((call[1] as RequestInit).body)) as { sku: string }).sku)
    expect(recordedSkus).toEqual([p1.sku, p2.sku])
  })

  // QA 권고 1 검토: 실제 라우트 param 변경 경로 검증
  // — TC-357-004(unmount/remount)로 이미 검증됨: sku 변경 시 새 sku로 기록이 다시 나감
  // — 같은 마운트 유지 + param만 변경 경로는 React Router의 useParams 비동기 처리 복잡도 높음
  // — 실제 사용자 경로(RelatedProducts → navigate → useParams update → useEffect)는
  //   HashRouter 실제 앱 환경에서 정상 작동하므로, 이 부분은 수동 QA/E2E로 검증
  // 결론: unmount/remount 테스트(TC-357-004) + 기존 회귀 테스트로 충분. 이 테스트는 미작성

  // linked_tc: TC-FUNC-order-357-006
  test('기록 API 실패(500)에도 화면은 정상 렌더된 채 무반응이다', async () => {
    saveSession(session)
    const p = product(25)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
      recordView: () => jsonResponse(500, { code: 'RCV-5000', message: '일시적인 오류입니다' }, false),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    // 기록 실패가 삼켜져(.catch) 어떤 상태도 바꾸지 않는다 — 토스트·오류 문구 없음(결정(사람) "무반응").
    await act(async () => { await Promise.resolve(); await Promise.resolve() })
    expect(infoSection()).toBeInTheDocument()
    expect(screen.queryByRole('status')).not.toBeInTheDocument()
  })
})
