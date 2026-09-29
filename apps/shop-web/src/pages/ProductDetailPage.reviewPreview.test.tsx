/** @jest-environment jsdom */
// SR-361 — 상품 상세(/shop/products/:sku) 최근 상품평 미리보기 통합 테스트. `ProductDetailPage.reviews.test.tsx`와
// 동일 관례(자체 축약 routeFetch, sku 인덱스로 격리)로 새 파일 분리했다(원본이 이미 569줄이라 더 키우지
// 않는다, 규칙 lab/file-size-cap 선례와 동일 이유, STORY "파일" 절).
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import ProductDetailPage from './ProductDetailPage'
import type { ProductDetail, ReviewListResult } from '../types'

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

// 테스트마다 인덱스로 유일화한다(STORY "테스트 격리" 절) — sku·상품명 문자열 충돌을 막는다.
function product(i: number, overrides: Partial<ProductDetail> = {}): ProductDetail {
  const stockQty = overrides.stockQty ?? 5
  return {
    sku: `sku-rvwprev-${i}`, productName: `미리보기테스트상품${i}`, price: 10000 + i * 1000, stockQty,
    saleYn: 'Y', listPrice: null, imageUrl: null, soldOut: stockQty !== null && stockQty <= 0,
    wishCount: 0, ...overrides,
  }
}

describe('ProductDetailPage — 최근 상품평 미리보기', () => {
  let fetchMock: jest.Mock<typeof fetch>

  beforeEach(() => {
    fetchMock = jest.fn()
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  /** URL로 분기하는 fetch 라우터 — `ProductDetailPage.reviews.test.tsx`의 축약판과 동일 관례(상품평
   * GET을 상품 상세보다 먼저 검사). 작성/삭제·주문 자격 조회는 이 파일의 시나리오에서 쓰지 않는다. */
  function routeFetch(handlers: {
    detail?: (sku: string) => Response | Promise<Response>
    list?: () => Response | Promise<Response>
    reviews?: (sku: string, sort: string, page: string) => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown) => {
      const url = String(input)
      const [path, queryString] = url.split('?')

      if (path.startsWith('/api/products/') && path.includes('/reviews')) {
        const sku = decodeURIComponent(path.slice('/api/products/'.length).split('/')[0])
        const params = new URLSearchParams(queryString ?? '')
        if (!handlers.reviews) throw new Error('unexpected reviews GET ' + url)
        return handlers.reviews(sku, params.get('sort') ?? '', params.get('page') ?? '')
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

  function renderPage(sku: string) {
    return render(
      <MemoryRouter initialEntries={[`/shop/products/${sku}`]}>
        <Routes>
          <Route path="/shop/products/:sku" element={<ProductDetailPage />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  const previewRegion = () => screen.getByRole('region', { name: '최근 상품평' })

  test('빈목록 — "아직 상품평이 없습니다"와 [리뷰 전체 보기] 버튼이 보인다', async () => {
    const p = product(1)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => jsonResponse(200, emptyReviewList()),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await within(previewRegion()).findByText('아직 상품평이 없습니다')
    expect(within(previewRegion()).getByRole('button', { name: '리뷰 전체 보기' })).toBeInTheDocument()
  })

  test('세건 초과 — 서버가 5건을 돌려줘도 미리보기는 정확히 3개만 렌더한다(슬라이스 검증)', async () => {
    const p = product(2)
    const items = Array.from({ length: 5 }, (_, i) => ({
      reviewId: i + 1, rating: 4, content: `본문 ${i + 1}번째 테스트`, authorName: '김*수',
      createdAt: `2026-09-${10 + i}T00:00:00Z`,
    }))
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => jsonResponse(200, emptyReviewList({ totalCount: 5, reviewCount: 5, avgRating: 4, items })),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await within(previewRegion()).findByText('본문 1번째 테스트')
    expect(within(previewRegion()).getAllByRole('listitem')).toHaveLength(3)
  })

  test('본문 60자 경계 — 정확히 60자는 그대로, 61자는 앞 60자+"…"로 렌더한다', async () => {
    const p = product(3)
    const exact60 = '가'.repeat(60)
    const over61 = '나'.repeat(61)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => jsonResponse(200, emptyReviewList({
        totalCount: 2, reviewCount: 2, avgRating: 5,
        items: [
          { reviewId: 1, rating: 5, content: exact60, authorName: '홍*동', createdAt: '2026-09-01T00:00:00Z' },
          { reviewId: 2, rating: 5, content: over61, authorName: '홍*동', createdAt: '2026-09-02T00:00:00Z' },
        ],
      })),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await within(previewRegion()).findByText(exact60)
    expect(within(previewRegion()).queryByText(exact60 + '…')).not.toBeInTheDocument()
    expect(within(previewRegion()).getByText(over61.slice(0, 60) + '…')).toBeInTheDocument()
    expect(within(previewRegion()).queryByText(over61)).not.toBeInTheDocument()
  })

  test('조회실패 — 안내+[다시 시도]가 뜨고, 상세 본문은 정상 표시된다', async () => {
    const p = product(4)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => jsonResponse(500, { message: 'boom' }, false),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await within(previewRegion()).findByText('상품평을 불러오지 못했습니다')
    expect(within(previewRegion()).getByRole('button', { name: '다시 시도' })).toBeInTheDocument()
    // 조회 실패에도 상세 본문(상품명)은 계속 정상 표시된다(순서·보안 절 — 서로 블로킹하지 않음).
    expect(screen.getByText(p.productName)).toBeInTheDocument()
  })

  test('재사용/중복호출 없음 — [리뷰 전체 보기] 클릭으로 탭이 열려도 리뷰 GET은 여전히 1회다', async () => {
    const p = product(5)
    let reviewCalls = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => { reviewCalls += 1; return jsonResponse(200, emptyReviewList()) },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await within(previewRegion()).findByText('아직 상품평이 없습니다')
    expect(reviewCalls).toBe(1)

    fireEvent.click(within(previewRegion()).getByRole('button', { name: '리뷰 전체 보기' }))

    const reviewTab = screen.getByRole('tab', { name: '상품평' })
    expect(reviewTab).toHaveAttribute('aria-selected', 'true')
    const reviewSection = screen.getByRole('region', { name: '상품평' })
    await within(reviewSection).findByText('아직 상품평이 없습니다')
    expect(reviewCalls).toBe(1)
  })

  test('authorName 노출 — 서버 마스킹값 그대로 렌더되고 원본 이름 텍스트는 없다', async () => {
    const p = product(6)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }), list: () => jsonResponse(200, [p]),
      reviews: () => jsonResponse(200, emptyReviewList({
        totalCount: 1, reviewCount: 1, avgRating: 4,
        items: [{ reviewId: 1, rating: 4, content: '마스킹된 이름 확인용 테스트 본문', authorName: '김*민', createdAt: '2026-09-05T00:00:00Z' }],
      })),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await within(previewRegion()).findByText('김*민')
    expect(within(previewRegion()).queryByText('김철민')).not.toBeInTheDocument()
  })
})
