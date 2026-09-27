/** @jest-environment jsdom */
// SR-351 STORY-3 — 상품 상세(/shop/products/:sku) 상품평 탭 통합 테스트. `ProductDetailPage.test.tsx`
// 관례(jsdom, URL 분기 fetch mock, 테스트별 고유 sku) 그대로 이어 쓰되, 파일을 분리했다(재작업 round2
// 사람 지시 2 — 리뷰 케이스 9건이 원본 파일을 417→734줄로 키워 규칙 `lab/file-size-cap`(should, 300줄)을
// 어겼다, QA CONCERNS 권고 2). routeFetch도 상품평 라우팅(목록/작성/삭제·주문 자격 조회)만 갖는 축약판을
// 이 파일에 별도로 둔다(원본은 detail/list/addItem만).
import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import ProductDetailPage from './ProductDetailPage'
import { saveSession } from '../session'
import type { OrderDetail, OrderItem, OrderRow, ProductDetail, ReviewListResult } from '../types'

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

// 테스트마다 인덱스로 유일화한다(STORY "테스트 격리" 절) — sku·상품명 문자열 충돌을 막는다.
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

describe('ProductDetailPage — 상품평 탭', () => {
  let fetchMock: jest.Mock<typeof fetch>

  beforeEach(() => {
    fetchMock = jest.fn()
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear() // 세션 키·myReviewsStorage가 다음 테스트로 새지 않게 한다.
  })

  afterEach(() => {
    localStorage.clear()
  })

  /** URL로 분기하는 fetch 라우터 — 상품평 목록/작성/삭제(`/api/products/:sku/reviews[/:id]`), 주문
   * 자격 조회(`/api/orders`, `/api/orders/:orderNo`)를 상품 상세(`/api/products/:sku`)보다 먼저
   * 검사한다(가장 구체적인 경로부터). */
  function routeFetch(handlers: {
    detail?: (sku: string) => Response | Promise<Response>
    list?: () => Response | Promise<Response>
    reviews?: (sku: string, sort: string, page: string) => Response | Promise<Response>
    createReview?: (sku: string, body: { rating: number; content: string; orderNo: string }) => Response | Promise<Response>
    deleteReview?: (sku: string, reviewId: string) => Response | Promise<Response>
    orders?: () => Response | Promise<Response>
    orderDetail?: (orderNo: string) => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const method = (init?.method ?? 'GET').toUpperCase()
      const [path, queryString] = url.split('?')

      if (path.startsWith('/api/products/') && path.includes('/reviews')) {
        const parts = path.slice('/api/products/'.length).split('/') // [sku, 'reviews', maybe reviewId]
        const sku = decodeURIComponent(parts[0])
        if (parts.length === 2) {
          if (method === 'GET') {
            const params = new URLSearchParams(queryString ?? '')
            if (!handlers.reviews) throw new Error('unexpected reviews GET ' + url)
            return handlers.reviews(sku, params.get('sort') ?? '', params.get('page') ?? '')
          }
          if (method === 'POST') {
            const body = init?.body ? JSON.parse(String(init.body)) as { rating: number; content: string; orderNo: string } : { rating: 0, content: '', orderNo: '' }
            if (!handlers.createReview) throw new Error('unexpected review POST ' + url)
            return handlers.createReview(sku, body)
          }
        }
        if (parts.length === 3 && method === 'DELETE') {
          if (!handlers.deleteReview) throw new Error('unexpected review DELETE ' + url)
          return handlers.deleteReview(sku, parts[2])
        }
        throw new Error('unexpected review call ' + method + ' ' + url)
      }
      if (path.startsWith('/api/orders/')) {
        const orderNo = decodeURIComponent(path.slice('/api/orders/'.length))
        if (!handlers.orderDetail) throw new Error('unexpected order detail call ' + url)
        return handlers.orderDetail(orderNo)
      }
      if (path.startsWith('/api/orders')) {
        return handlers.orders ? handlers.orders() : jsonResponse(200, [])
      }
      if (path.startsWith('/api/products/')) {
        const sku = decodeURIComponent(path.slice('/api/products/'.length))
        if (!handlers.detail) throw new Error('unexpected detail call ' + url)
        return handlers.detail(sku)
      }
      if (path.startsWith('/api/products')) {
        return handlers.list ? handlers.list() : jsonResponse(200, [])
      }
      if (path.startsWith('/api/cart')) return jsonResponse(200, { items: [] })
      throw new Error('unexpected url ' + url)
    })
  }

  function emptyReviewList(overrides: Partial<ReviewListResult> = {}): ReviewListResult {
    return {
      totalCount: 0, page: 1, size: 10, avgRating: null, reviewCount: 0,
      distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 }, items: [], ...overrides,
    }
  }

  function orderRow(i: number, overrides: Partial<OrderRow> = {}): OrderRow {
    return {
      orderNo: `ORD-rvw-${i}`, memberId: session.memberId, memberName: session.memberName,
      orderState: 'DONE', totalAmount: 10000, orderedAt: '2026-01-01T00:00:00Z', deliveryState: null,
      ...overrides,
    }
  }

  function orderDetailFor(row: OrderRow, items: OrderItem[]): OrderDetail {
    return { ...row, items, deliveries: [], shipments: [] }
  }

  function renderPage(sku: string) {
    return render(
      <MemoryRouter initialEntries={[`/shop/products/${sku}`]}>
        <Routes>
          <Route path="/shop/products/:sku" element={<ProductDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  // `reviewRegion()`은 항상 탭 클릭 뒤에만 쓴다(탭 진입 전에는 존재하지 않음).
  const reviewRegion = () => screen.getByRole('region', { name: '상품평' })

  test('상품평 탭 — 진입 전에는 GET이 나가지 않고, 클릭 후에만 나간다(AC "탭 진입 시에만")', async () => {
    const p = product(20)
    let reviewCalls = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      list: () => jsonResponse(200, [p]),
      reviews: () => { reviewCalls += 1; return jsonResponse(200, emptyReviewList()) },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(reviewCalls).toBe(0)

    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))
    await screen.findByText('아직 상품평이 없습니다')
    expect(reviewCalls).toBe(1)
  })

  test('상품평 탭 — 평균 별점·건수·목록이 렌더된다', async () => {
    const p = product(21)
    const list: ReviewListResult = {
      totalCount: 1, page: 1, size: 10, avgRating: 4.5, reviewCount: 1,
      distribution: { '1': 0, '2': 0, '3': 0, '4': 1, '5': 0 },
      items: [{ reviewId: 1, rating: 4, content: '배송이 빠르고 품질도 좋았습니다', authorName: '홍*동', createdAt: '2026-09-20T00:00:00Z' }],
    }
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => jsonResponse(200, list),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))

    await within(reviewRegion()).findByText('배송이 빠르고 품질도 좋았습니다')
    expect(within(reviewRegion()).getByText('1건')).toBeInTheDocument()
    // 재작업(round2 사람 지시 5) — 개별 rating(정수)은 "4"로 그대로 표시한다(평균 "4.5"와 자릿수가 다름).
    expect(within(reviewRegion()).getByLabelText('별점 4점')).toBeInTheDocument()
  })

  test('상품평 탭 — 목록 조회 500 시 안내+[다시 시도], 재시도하면 성공한다', async () => {
    const p = product(23)
    let calls = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => {
        calls += 1
        return calls === 1 ? jsonResponse(500, { message: 'boom' }, false) : jsonResponse(200, emptyReviewList())
      },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))
    await within(reviewRegion()).findByText('상품평을 불러오지 못했습니다')

    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '다시 시도' }))
    await within(reviewRegion()).findByText('아직 상품평이 없습니다')
    expect(calls).toBe(2)
  })

  test('상품평 탭 — 비로그인이면 작성 폼 대신 로그인 안내가 뜬다', async () => {
    const p = product(24)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => jsonResponse(200, emptyReviewList()),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    const callsBefore = fetchMock.mock.calls.length
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))

    await within(reviewRegion()).findByText('로그인 후 작성할 수 있습니다')
    expect(within(reviewRegion()).queryByRole('form', { name: '상품평 작성' })).not.toBeInTheDocument()
    // 비로그인이면 주문 자격 조회(fetchOrders) 자체가 나가지 않는다(순서·보안 2, 부수효과가 판정보다
    // 먼저 실행되지 않게 — RUN8/SR-234 FUNC-member-008 r1 대조).
    fetchMock.mock.calls.forEach(call => {
      expect(String(call[0])).not.toMatch(/^\/api\/orders/)
    })
    expect(fetchMock.mock.calls.length).toBeGreaterThan(callsBefore)
  })

  test('상품평 탭 — 로그인 상태에서 주문 자격 조회 후(구매 이력 있음) 작성 폼이 뜬다', async () => {
    saveSession(session)
    const p = product(25)
    const row = orderRow(1)
    const detail = orderDetailFor(row, [{ productNo: p.sku, productName: p.productName, quantity: 1, unitPrice: p.price }])
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => jsonResponse(200, emptyReviewList()),
      orders: () => jsonResponse(200, [row]),
      orderDetail: () => jsonResponse(200, detail),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))

    // 폼 자체는 주문 자격 조회가 끝나기 전(loading)에도 렌더된다(그 외 분기) — 자격 조회가 실제로
    // 끝나 후보가 채워졌는지는 옵션이 나타나는 것으로 기다려 확인한다.
    await within(reviewRegion()).findByRole('option', { name: `${row.orderNo} (2026-01-01)` })
  })

  // 사람 수정 결정 3번 — 정렬 변경 경합. 먼저 보낸 요청(최신순)의 응답이 늦게 도착해도, 그사이 사용자가
  // 별점높은순→최신순으로 더 최근에 보낸 요청 결과를 덮어쓰지 않아야 한다(requestId 가드 직접 검증).
  test('상품평 탭 — 정렬 변경 경합: 지연 응답이 최신 요청 결과를 덮어쓰지 않는다', async () => {
    const p = product(26)
    const listA: ReviewListResult = {
      ...emptyReviewList(), totalCount: 1, reviewCount: 1, avgRating: 5,
      items: [{ reviewId: 1, rating: 5, content: '최신순 결과 A 입니다 테스트', authorName: '홍*동', createdAt: '2026-09-20T00:00:00Z' }],
    }
    const listB: ReviewListResult = {
      ...emptyReviewList(), totalCount: 1, reviewCount: 1, avgRating: 3,
      items: [{ reviewId: 2, rating: 3, content: '별점높은순 결과 B 입니다 테스트', authorName: '김*수', createdAt: '2026-09-10T00:00:00Z' }],
    }
    const listC: ReviewListResult = {
      ...emptyReviewList(), totalCount: 1, reviewCount: 1, avgRating: 5,
      items: [{ reviewId: 3, rating: 5, content: '최신순 결과 C 입니다 테스트', authorName: '박*환', createdAt: '2026-09-25T00:00:00Z' }],
    }

    let resolveB!: (r: Response) => void
    const pendingB = new Promise<Response>(res => { resolveB = res })
    let latestCalls = 0

    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: (_sku, sort) => {
        if (sort === 'ratingDesc') return pendingB // 응답을 붙잡아 둔다(늦게 도착하는 요청)
        latestCalls += 1
        return jsonResponse(200, latestCalls === 1 ? listA : listC)
      },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))
    await within(reviewRegion()).findByText('최신순 결과 A 입니다 테스트')

    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '별점높은순' })) // 응답 대기 중(지연)
    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '최신순' })) // 더 나중 요청, 즉시 응답
    await within(reviewRegion()).findByText('최신순 결과 C 입니다 테스트')

    // 지연됐던 별점높은순 응답이 이제서야 도착해도 화면을 덮어쓰지 않는다.
    await act(async () => {
      resolveB(jsonResponse(200, listB))
      await Promise.resolve()
    })
    expect(within(reviewRegion()).queryByText('별점높은순 결과 B 입니다 테스트')).not.toBeInTheDocument()
    expect(within(reviewRegion()).getByText('최신순 결과 C 입니다 테스트')).toBeInTheDocument()
  })

  // 재작업(round2, QA CONCERNS 권고 1 + 사람 코멘트 [구현 방식]) — 이미 목록이 렌더된 상태에서 정렬
  // 변경 GET이 실패하면, 직전 목록은 그대로 남기고 인라인 role="alert" 경고+[다시 시도]가 뜬다(전체
  // 대체가 아니다). 정렬 버튼의 활성 표시는 실패한 요청(별점높은순)이 아니라 성공한 마지막 요청
  // (최신순)에 맞춰 되돌아간다 — 재시도를 누르면 실패했던 요청 파라미터 그대로 재호출해 성공하면
  // 경고가 사라지고 버튼 표시도 새 결과에 맞춰 갱신된다. 재현 시나리오는 QA round1 발견 그대로.
  test('상품평 탭 — 이미 목록이 있는 상태에서 정렬 변경 GET이 500이면 인라인 경고+다시 시도, 정렬 버튼은 이전 성공값 유지', async () => {
    const p = product(29)
    const latestList: ReviewListResult = {
      totalCount: 1, page: 1, size: 10, avgRating: 5, reviewCount: 1,
      distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 1 },
      items: [{ reviewId: 1, rating: 5, content: '최신순 결과 유지 확인용 리뷰입니다', authorName: '홍*동', createdAt: '2026-09-20T00:00:00Z' }],
    }
    const ratingDescRetrySuccess: ReviewListResult = {
      totalCount: 1, page: 1, size: 10, avgRating: 3, reviewCount: 1,
      distribution: { '1': 0, '2': 0, '3': 1, '4': 0, '5': 0 },
      items: [{ reviewId: 2, rating: 3, content: '별점높은순 재시도 성공 리뷰입니다', authorName: '김*수', createdAt: '2026-09-10T00:00:00Z' }],
    }
    let ratingDescCalls = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: (_sku, sort) => {
        if (sort === 'ratingDesc') {
          ratingDescCalls += 1
          return ratingDescCalls === 1
            ? jsonResponse(500, { message: 'boom' }, false)
            : jsonResponse(200, ratingDescRetrySuccess)
        }
        return jsonResponse(200, latestList)
      },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))
    await within(reviewRegion()).findByText('최신순 결과 유지 확인용 리뷰입니다')
    expect(within(reviewRegion()).getByRole('button', { name: '최신순' })).toHaveAttribute('aria-pressed', 'true')

    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '별점높은순' }))

    // 재조회 실패 — 직전 목록(최신순 결과)이 그대로 남은 채로 인라인 경고+다시 시도가 뜬다.
    await within(reviewRegion()).findByText('상품평을 불러오지 못했습니다')
    expect(within(reviewRegion()).getByText('최신순 결과 유지 확인용 리뷰입니다')).toBeInTheDocument()
    // 정렬 버튼의 활성 표시는 실패한 요청(별점높은순)이 아니라 성공한 마지막 요청(최신순)으로 되돌아간다.
    expect(within(reviewRegion()).getByRole('button', { name: '최신순' })).toHaveAttribute('aria-pressed', 'true')
    expect(within(reviewRegion()).getByRole('button', { name: '별점높은순' })).toHaveAttribute('aria-pressed', 'false')

    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '다시 시도' }))

    // 재시도는 실패했던 요청 파라미터(별점높은순) 그대로 재호출한다 — 성공하면 경고가 사라지고
    // 정렬 버튼 표시도 새 결과(별점높은순)에 맞춰 갱신된다.
    await within(reviewRegion()).findByText('별점높은순 재시도 성공 리뷰입니다')
    expect(within(reviewRegion()).queryByText('상품평을 불러오지 못했습니다')).not.toBeInTheDocument()
    expect(within(reviewRegion()).getByRole('button', { name: '별점높은순' })).toHaveAttribute('aria-pressed', 'true')
    expect(within(reviewRegion()).getByRole('button', { name: '최신순' })).toHaveAttribute('aria-pressed', 'false')
    expect(ratingDescCalls).toBe(2)
  })

  // 사람 수정 결정 — 작성 성공 시 1페이지·최신순으로 강제 이동한다(제출 전 정렬·페이지가 무엇이었든).
  test('상품평 작성 성공 — 목록이 1페이지·최신순으로 이동해 재조회된다', async () => {
    saveSession(session)
    const p = product(27)
    const row = orderRow(2)
    const orderDetailRow = orderDetailFor(row, [{ productNo: p.sku, productName: p.productName, quantity: 1, unitPrice: p.price }])

    const initialLatestPage1: ReviewListResult = {
      totalCount: 15, page: 1, size: 10, avgRating: 4, reviewCount: 15,
      distribution: { '1': 0, '2': 0, '3': 0, '4': 10, '5': 5 },
      items: [{ reviewId: 1, rating: 4, content: '기존 최신순 1페이지 리뷰입니다', authorName: '익*명', createdAt: '2026-09-01T00:00:00Z' }],
    }
    const ratingDescPage1: ReviewListResult = {
      totalCount: 15, page: 1, size: 10, avgRating: 4, reviewCount: 15,
      distribution: { '1': 0, '2': 0, '3': 0, '4': 10, '5': 5 },
      items: [{ reviewId: 2, rating: 3, content: '별점높은순 1페이지 리뷰입니다', authorName: '익*명', createdAt: '2026-08-01T00:00:00Z' }],
    }
    const ratingDescPage2: ReviewListResult = {
      totalCount: 15, page: 2, size: 10, avgRating: 4, reviewCount: 15,
      distribution: { '1': 0, '2': 0, '3': 0, '4': 10, '5': 5 },
      items: [{ reviewId: 3, rating: 3, content: '별점높은순 2페이지 리뷰입니다', authorName: '익*명', createdAt: '2026-07-01T00:00:00Z' }],
    }
    const afterSubmitLatestPage1: ReviewListResult = {
      totalCount: 16, page: 1, size: 10, avgRating: 4, reviewCount: 16,
      distribution: { '1': 0, '2': 0, '3': 0, '4': 10, '5': 6 },
      items: [{ reviewId: 999, rating: 5, content: '방금 작성한 새 리뷰입니다', authorName: '홍*동', createdAt: '2026-09-27T00:00:00Z' }],
    }

    const latestPage1Calls: number[] = []
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      orders: () => jsonResponse(200, [row]), orderDetail: () => jsonResponse(200, orderDetailRow),
      reviews: (_sku, sort, page) => {
        if (sort === 'latest' && page === '1') {
          latestPage1Calls.push(1)
          return jsonResponse(200, latestPage1Calls.length === 1 ? initialLatestPage1 : afterSubmitLatestPage1)
        }
        if (sort === 'ratingDesc' && page === '1') return jsonResponse(200, ratingDescPage1)
        if (sort === 'ratingDesc' && page === '2') return jsonResponse(200, ratingDescPage2)
        return jsonResponse(200, emptyReviewList())
      },
      createReview: (_sku, body) => jsonResponse(201, {
        reviewId: 999, sku: p.sku, orderNo: body.orderNo, rating: body.rating, content: body.content,
        createdAt: '2026-09-27T00:00:00Z',
      }),
    })

    renderPage(p.sku)
    await screen.findByText(p.productName)
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))
    await within(reviewRegion()).findByText('기존 최신순 1페이지 리뷰입니다')

    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '별점높은순' }))
    await within(reviewRegion()).findByText('별점높은순 1페이지 리뷰입니다')
    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '다음' }))
    await within(reviewRegion()).findByText('별점높은순 2페이지 리뷰입니다')

    // 주문 자격 조회가 끝나 옵션이 실제로 채워질 때까지 기다린다(폼 자체는 loading 중에도 렌더된다).
    await within(reviewRegion()).findByRole('option', { name: `${row.orderNo} (2026-01-01)` })
    fireEvent.change(within(reviewRegion()).getByRole('combobox', { name: '구매하신 주문' }), { target: { value: row.orderNo } })
    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '별점 5점' }))
    fireEvent.change(within(reviewRegion()).getByRole('textbox', { name: '상품평' }), { target: { value: '충분히 긴 상품평 작성 테스트 내용입니다' } })
    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '상품평 등록' }))

    await within(reviewRegion()).findByText('방금 작성한 새 리뷰입니다')
    expect(within(reviewRegion()).getByRole('button', { name: '최신순' })).toHaveAttribute('aria-pressed', 'true')
    expect(within(reviewRegion()).queryByText('별점높은순 2페이지 리뷰입니다')).not.toBeInTheDocument()
  })

  test('상품평 작성 — 403/409 응답이 고정 문구로 표시되고, 409는 입력값을 그대로 유지한다', async () => {
    saveSession(session)
    const p = product(28)
    const row = orderRow(3)
    const orderDetailRow = orderDetailFor(row, [{ productNo: p.sku, productName: p.productName, quantity: 1, unitPrice: p.price }])
    let attempt = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      orders: () => jsonResponse(200, [row]), orderDetail: () => jsonResponse(200, orderDetailRow),
      reviews: () => jsonResponse(200, emptyReviewList()),
      createReview: () => {
        attempt += 1
        if (attempt === 1) return jsonResponse(403, { code: 'RVW-4031', message: '구매 이력이 없습니다' }, false)
        return jsonResponse(409, { code: 'RVW-4091', message: '이미 작성한 상품평이 있습니다' }, false)
      },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))
    // 주문 자격 조회가 끝나 옵션이 실제로 채워질 때까지 기다린다(폼 자체는 loading 중에도 렌더된다).
    await within(reviewRegion()).findByRole('option', { name: `${row.orderNo} (2026-01-01)` })

    fireEvent.change(within(reviewRegion()).getByRole('combobox', { name: '구매하신 주문' }), { target: { value: row.orderNo } })
    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '별점 5점' }))
    fireEvent.change(within(reviewRegion()).getByRole('textbox', { name: '상품평' }), { target: { value: '충분히 긴 상품평 내용입니다 테스트용' } })
    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '상품평 등록' }))

    await within(reviewRegion()).findByText('이 상품을 구매한 회원만 작성할 수 있습니다')

    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '상품평 등록' }))
    await within(reviewRegion()).findByText('이미 이 주문으로 작성한 상품평이 있습니다')
    // 409에도 입력값(주문·본문·별점)이 초기화되지 않고 그대로 유지된다(사람 수정 결정 2번). 재작업
    // (round3 사람 지시 4, QA round2 low 4) — 별점은 combobox/textbox와 달리 `StarRatingInput`이
    // `aria-pressed`로 그리므로 값이 아니라 이 속성으로 유지를 확인한다.
    expect(within(reviewRegion()).getByRole('combobox', { name: '구매하신 주문' })).toHaveValue(row.orderNo)
    expect(within(reviewRegion()).getByRole('textbox', { name: '상품평' })).toHaveValue('충분히 긴 상품평 내용입니다 테스트용')
    expect(within(reviewRegion()).getByRole('button', { name: '별점 5점' })).toHaveAttribute('aria-pressed', 'true')
  })

  // 재작업(round3 사람 지시 1, QA round2 low 1) — 정렬 변경이 실패한 뒤, 사용자가 원래 누르려던
  // 정렬을 다시 눌러도(값 자체는 실패 시도와 동일) 요청이 나가야 한다(dead click 방지). target이
  // 이미 그 값이라 React가 동일 state로 판단해도, 시도 nonce가 effect 의존을 바꿔 재요청을 강제한다.
  test('상품평 탭 — 정렬 실패 후 같은 정렬을 다시 클릭해도 재조회가 나간다(dead click 방지)', async () => {
    const p = product(30)
    const latestList: ReviewListResult = {
      totalCount: 1, page: 1, size: 10, avgRating: 5, reviewCount: 1,
      distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 1 },
      items: [{ reviewId: 1, rating: 5, content: '최신순 결과 유지 확인용 리뷰 재작업', authorName: '홍*동', createdAt: '2026-09-20T00:00:00Z' }],
    }
    const ratingDescSuccess: ReviewListResult = {
      totalCount: 1, page: 1, size: 10, avgRating: 3, reviewCount: 1,
      distribution: { '1': 0, '2': 0, '3': 1, '4': 0, '5': 0 },
      items: [{ reviewId: 2, rating: 3, content: '별점높은순 재클릭 성공 리뷰 재작업', authorName: '김*수', createdAt: '2026-09-10T00:00:00Z' }],
    }
    let ratingDescCalls = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: (_sku, sort) => {
        if (sort === 'ratingDesc') {
          ratingDescCalls += 1
          // 첫 시도는 실패, "다시 시도" 버튼이 아니라 같은 정렬 버튼을 또 눌렀을 때(두 번째 시도)만 성공.
          return ratingDescCalls === 1
            ? jsonResponse(500, { message: 'boom' }, false)
            : jsonResponse(200, ratingDescSuccess)
        }
        return jsonResponse(200, latestList)
      },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))
    await within(reviewRegion()).findByText('최신순 결과 유지 확인용 리뷰 재작업')

    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '별점높은순' }))
    await within(reviewRegion()).findByText('상품평을 불러오지 못했습니다')
    expect(ratingDescCalls).toBe(1)

    // "다시 시도" 버튼이 아니라, 정렬 버튼 자체를 다시 누른다 — 값은 이미 실패한 target과 같다.
    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '별점높은순' }))

    await within(reviewRegion()).findByText('별점높은순 재클릭 성공 리뷰 재작업')
    expect(ratingDescCalls).toBe(2)
  })

  // 재작업(round3 사람 지시 2, QA round2 low 2) — 작성 성공 직후의 재조회(1페이지·최신순)가 실패하면
  // 화면의 정렬 버튼·목록이 실제로 표시 중인 결과와 어긋나면 안 된다. applied를 낙관적으로 먼저
  // 바꾸지 않으므로, 재조회가 실패해도 버튼은 직전 성공값(별점높은순)에 그대로 남고 인라인 경고가 뜬다.
  test('상품평 작성 성공 — 뒤따르는 재조회가 실패하면 인라인 경고가 뜨고 정렬 버튼은 실제 목록과 일치한다', async () => {
    saveSession(session)
    const p = product(31)
    const row = orderRow(4)
    const orderDetailRow = orderDetailFor(row, [{ productNo: p.sku, productName: p.productName, quantity: 1, unitPrice: p.price }])
    const initialLatestPage1: ReviewListResult = {
      totalCount: 1, page: 1, size: 10, avgRating: 4, reviewCount: 1,
      distribution: { '1': 0, '2': 0, '3': 0, '4': 1, '5': 0 },
      items: [{ reviewId: 1, rating: 4, content: '초기 최신순 리뷰 재작업 확인용', authorName: '익*명', createdAt: '2026-08-15T00:00:00Z' }],
    }
    const ratingDescPage1: ReviewListResult = {
      totalCount: 1, page: 1, size: 10, avgRating: 3, reviewCount: 1,
      distribution: { '1': 0, '2': 0, '3': 1, '4': 0, '5': 0 },
      items: [{ reviewId: 2, rating: 3, content: '별점높은순 기존 리뷰 재작업 확인용', authorName: '익*명', createdAt: '2026-08-01T00:00:00Z' }],
    }
    // 최신순 1페이지 요청은 첫 호출(탭 진입 시 초기 로드)만 성공하고, 두 번째 호출(작성 직후 재조회)은
    // 실패시킨다 — 별점높은순으로 전환해 둔 뒤 작성하면, 그 재조회가 이 두 번째 호출에 해당한다.
    let latestPage1Calls = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      orders: () => jsonResponse(200, [row]), orderDetail: () => jsonResponse(200, orderDetailRow),
      reviews: (_sku, sort, page) => {
        if (sort === 'latest' && page === '1') {
          latestPage1Calls += 1
          return latestPage1Calls === 1 ? jsonResponse(200, initialLatestPage1) : jsonResponse(500, { message: 'boom' }, false)
        }
        return jsonResponse(200, ratingDescPage1)
      },
      createReview: (_sku, body) => jsonResponse(201, {
        reviewId: 999, sku: p.sku, orderNo: body.orderNo, rating: body.rating, content: body.content,
        createdAt: '2026-09-27T00:00:00Z',
      }),
    })

    renderPage(p.sku)
    await screen.findByText(p.productName)
    fireEvent.click(screen.getByRole('tab', { name: '상품평' }))
    await within(reviewRegion()).findByText('초기 최신순 리뷰 재작업 확인용')

    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '별점높은순' }))
    await within(reviewRegion()).findByText('별점높은순 기존 리뷰 재작업 확인용')
    expect(within(reviewRegion()).getByRole('button', { name: '별점높은순' })).toHaveAttribute('aria-pressed', 'true')

    await within(reviewRegion()).findByRole('option', { name: `${row.orderNo} (2026-01-01)` })
    fireEvent.change(within(reviewRegion()).getByRole('combobox', { name: '구매하신 주문' }), { target: { value: row.orderNo } })
    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '별점 5점' }))
    fireEvent.change(within(reviewRegion()).getByRole('textbox', { name: '상품평' }), { target: { value: '충분히 긴 상품평 작성 재조회 실패 테스트' } })
    fireEvent.click(within(reviewRegion()).getByRole('button', { name: '상품평 등록' }))

    // 작성 자체는 성공했지만(폼이 비워짐), 뒤따르는 최신순 1페이지 재조회가 500이라 인라인 경고가 뜬다.
    await within(reviewRegion()).findByText('상품평을 불러오지 못했습니다')
    // 목록·정렬 버튼은 실제로 표시 중인 결과(별점높은순)와 일치한다 — applied가 앞서가지 않는다.
    expect(within(reviewRegion()).getByText('별점높은순 기존 리뷰 재작업 확인용')).toBeInTheDocument()
    expect(within(reviewRegion()).getByRole('button', { name: '별점높은순' })).toHaveAttribute('aria-pressed', 'true')
    expect(within(reviewRegion()).getByRole('button', { name: '최신순' })).toHaveAttribute('aria-pressed', 'false')
    expect(latestPage1Calls).toBe(2)
  })
})
