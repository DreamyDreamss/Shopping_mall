/** @jest-environment jsdom */
// SR-360 — 상품 비교 화면(/shop/compare) 통합 테스트. `WishListPage.test.tsx`/`RecentlyViewedPage.test.tsx`
// 관례 그대로(jsdom, URL+메서드 분기 fetch mock, `useNavigate` 스텁, 테스트별 고유 memberId/apiKey/sku).
// sku 접미는 `sku-compare-page-`(`ProductDetailPage.compare.test.tsx`의 `sku-compare-`와 겹치지 않게 구분).
declare const jest: typeof import('@jest/globals').jest

import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import ProductComparePage from './ProductComparePage'
import { saveSession } from '../session'
import type { ProductCompareItem } from '../types'

const mockNavigate = jest.fn()
jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom') as Record<string, unknown>
  return { ...actual, useNavigate: () => mockNavigate }
})

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

function compareItem(i: number, overrides: Partial<ProductCompareItem> = {}): ProductCompareItem {
  return {
    sku: `sku-compare-page-${i}`, productName: `비교페이지상품${i}`, price: 10000 * i, listPrice: null,
    imageUrl: null, avgRating: 4.5, reviewCount: 3, stockQty: 10, soldOut: false,
    comparedAt: '2026-09-27T10:00:00',
    ...overrides,
  }
}

function session(i: number) {
  return {
    memberId: `m-comparepage-${i}`, memberName: '홍길동', grade: 'NORMAL',
    apiKey: `key-comparepage-${i}`, refreshToken: `r-comparepage-${i}`, refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
  }
}

describe('ProductComparePage', () => {
  let fetchMock: import('@jest/globals').jest.Mock<typeof fetch>
  let listHeaders: Record<string, string>[]
  let removeCalls: string[]

  beforeEach(() => {
    fetchMock = jest.fn()
    listHeaders = []
    removeCalls = []
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear()
    mockNavigate.mockClear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  function routeFetch(handlers: {
    list?: () => Response | Promise<Response>
    remove?: (sku: string) => Response | Promise<Response>
    cart?: () => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const method = (init?.method ?? 'GET').toUpperCase()
      const [path] = url.split('?')

      if (path === '/api/members/me/compares' && method === 'GET') {
        const headers = (init?.headers ?? {}) as Record<string, string>
        listHeaders.push(headers)
        return handlers.list ? handlers.list() : jsonResponse(200, { items: [] })
      }
      const removeMatch = path.match(/^\/api\/members\/me\/compares\/(.+)$/)
      if (removeMatch && method === 'DELETE') {
        const sku = decodeURIComponent(removeMatch[1])
        removeCalls.push(sku)
        return handlers.remove ? handlers.remove(sku) : jsonResponse(204, null)
      }
      if (path === '/api/cart') return handlers.cart ? handlers.cart() : jsonResponse(200, { items: [] })
      throw new Error('unexpected url ' + url + ' ' + method)
    })
  }

  function renderPage() {
    return render(
      <MemoryRouter initialEntries={['/shop/compare']}>
        <Routes>
          <Route path="/shop/compare" element={<ProductComparePage />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  // (1) 비로그인 진입 → /login?redirect=%2Fshop%2Fcompare로 즉시 이동, 목록 GET 안 나감(문자열 toEqual —
  // 사례집 SR-353 #1, 라벨 존재만 보지 않는다).
  test('비로그인 진입 — 로그인 화면으로 리다이렉트되고 목록 조회는 호출되지 않는다', async () => {
    routeFetch({})
    renderPage()

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith(
      '/login?redirect=' + encodeURIComponent('/shop/compare'), { replace: true },
    ))
    expect(listHeaders).toHaveLength(0)
  })

  // (2) 0건 응답 → AC 고정 문구 전체가 그대로 렌더된다(부분 문자열 아님).
  test('0건 — AC 고정 문구가 그대로 렌더된다', async () => {
    saveSession(session(1))
    routeFetch({ list: () => jsonResponse(200, { items: [] }) })
    renderPage()

    await screen.findByText('비교함이 비어 있습니다 — 상품 상세에서 [비교함 담기]를 누르세요')
    expect(listHeaders[0]['X-Api-Key']).toBe('key-comparepage-1')
  })

  // (3) 2건 응답 → 열 2개, 가격/정가/평점/리뷰수/재고 값이 fixture와 정확히 일치. 평점 null은 "—",
  // soldOut true는 "품절".
  test('2건 — 열 2개, 속성 값이 fixture와 정확히 일치한다', async () => {
    saveSession(session(2))
    const rows = [
      compareItem(1, { listPrice: null, avgRating: null, reviewCount: 0, stockQty: 5, soldOut: false }),
      compareItem(2, { listPrice: 25000, avgRating: 4.2, reviewCount: 12, stockQty: 0, soldOut: true }),
    ]
    routeFetch({ list: () => jsonResponse(200, { items: rows }) })
    renderPage()

    await screen.findByText(rows[0].productName)
    expect(screen.getByText(rows[1].productName)).toBeInTheDocument()

    expect(screen.getByText('10,000원')).toBeInTheDocument() // rows[0].price
    expect(screen.getByText('20,000원')).toBeInTheDocument() // rows[1].price
    expect(screen.getByText('25,000원')).toBeInTheDocument() // rows[1].listPrice
    expect(screen.getAllByText('—')).toHaveLength(2) // rows[0]의 정가·평점
    expect(screen.getByText('4.2')).toBeInTheDocument() // rows[1].avgRating
    expect(screen.getByText('0건')).toBeInTheDocument() // rows[0].reviewCount
    expect(screen.getByText('12건')).toBeInTheDocument() // rows[1].reviewCount
    expect(screen.getByText('5개')).toBeInTheDocument() // rows[0].stockQty
    expect(screen.getByText('품절')).toBeInTheDocument() // rows[1].soldOut
  })

  // (4) 4건(상한) 응답 → 열 4개 전부 렌더(오버플로 없음만 확인 — 5번째 거절은 SR-360 #2/#5 책임).
  test('4건(상한) — 열 4개 전부 렌더된다', async () => {
    saveSession(session(3))
    const rows = [compareItem(1), compareItem(2), compareItem(3), compareItem(4)]
    routeFetch({ list: () => jsonResponse(200, { items: rows }) })
    renderPage()

    await screen.findByText(rows[0].productName)
    expect(screen.getAllByRole('button', { name: /비교함에서 빼기/ })).toHaveLength(4)
    rows.forEach(r => expect(screen.getByText(r.productName)).toBeInTheDocument())
  })

  // (5) 조회 실패(500) → AC 고정 문구(서버 상세 미노출) + 다시 시도 클릭 시 재요청 성공.
  test('조회 실패(500) — AC 고정 문구만 뜨고 서버 상세는 노출되지 않으며, 다시 시도 클릭 시 재요청이 성공한다', async () => {
    saveSession(session(4))
    let calls = 0
    routeFetch({
      list: () => {
        calls += 1
        return calls === 1
          ? jsonResponse(500, { code: 'CMP-5000', message: 'internal secret detail' }, false)
          : jsonResponse(200, { items: [] })
      },
    })
    renderPage()

    await screen.findByText('비교함을 불러오지 못했습니다')
    expect(screen.queryByText(/internal secret detail/)).not.toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }))
    await screen.findByText('비교함이 비어 있습니다 — 상품 상세에서 [비교함 담기]를 누르세요')
    expect(calls).toBe(2)
  })

  // (6) [빼기] 클릭 성공(204) → removeCompare가 정확한 sku로 1회 호출된 뒤 fetchCompares가 다시
  // 호출됨(총 2회)을 단언하고, 두 번째 응답(그 sku 제외)이 반영돼 열이 사라짐을 확인 — 낙관적 로컬
  // 제거가 아니라 실제 재조회로 검증한다.
  test('[빼기] 클릭 성공 — DELETE 후 목록을 다시 읽어 그 열이 사라진다', async () => {
    saveSession(session(5))
    const rowA = compareItem(1)
    const rowB = compareItem(2)
    let listCalls = 0
    routeFetch({
      list: () => {
        listCalls += 1
        return jsonResponse(200, { items: listCalls === 1 ? [rowA, rowB] : [rowB] })
      },
    })
    renderPage()

    await screen.findByText(rowA.productName)
    fireEvent.click(screen.getByRole('button', { name: `${rowA.productName} 비교함에서 빼기` }))

    await waitFor(() => expect(screen.queryByText(rowA.productName)).not.toBeInTheDocument())
    expect(removeCalls).toEqual([rowA.sku])
    expect(listCalls).toBe(2)
    expect(screen.getByText(rowB.productName)).toBeInTheDocument()
  })

  // (7) [빼기] 클릭 실패 → 목록이 그대로(낙관적 제거 없음이므로 롤백할 것도 없음), 인라인 오류만 뜨고
  // 다른 열은 영향 없음.
  test('[빼기] 클릭 실패 — 목록은 그대로이고 인라인 오류만 뜬다', async () => {
    saveSession(session(6))
    const rowA = compareItem(1)
    const rowB = compareItem(2)
    routeFetch({
      list: () => jsonResponse(200, { items: [rowA, rowB] }),
      remove: () => jsonResponse(404, { code: 'CMP-4041', message: '비교함에 없는 상품입니다' }, false),
    })
    renderPage()

    await screen.findByText(rowA.productName)
    fireEvent.click(screen.getByRole('button', { name: `${rowA.productName} 비교함에서 빼기` }))

    expect(await screen.findByText('비교함에 없는 상품입니다')).toBeInTheDocument()
    expect(screen.getByText(rowA.productName)).toBeInTheDocument()
    expect(screen.getByText(rowB.productName)).toBeInTheDocument()
  })

  // (8) 연타 — 같은 열 [빼기]를 같은 이벤트 루프에서 두 번 클릭해도 removeCompare 호출은 1회만
  // (act() 한 스코프 안에서 미분리 발사 — 사례집 SR-302 #1).
  test('빼기 연타 — 같은 열 2회 클릭에도 DELETE 호출은 1회만 나간다', async () => {
    saveSession(session(7))
    const row = compareItem(1)
    routeFetch({ list: () => jsonResponse(200, { items: [row] }) })
    renderPage()

    await screen.findByText(row.productName)
    const removeButton = screen.getByRole('button', { name: `${row.productName} 비교함에서 빼기` })
    act(() => {
      removeButton.click()
      removeButton.click()
    })

    await waitFor(() => expect(removeCalls).toEqual([row.sku]))
  })

  // (9) GNB 장바구니 배지 — 기존 fetchCartItemCount 재사용 확인(가벼운 스모크 1건).
  test('GNB 장바구니 배지 — fetchCartItemCount가 반영된다', async () => {
    saveSession(session(8))
    routeFetch({
      list: () => jsonResponse(200, { items: [] }),
      cart: () => jsonResponse(200, { items: [{ sku: 'x', qty: 2 }] }),
    })
    renderPage()

    expect(await screen.findByLabelText('장바구니 2개')).toBeInTheDocument()
  })
})
