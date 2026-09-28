/** @jest-environment jsdom */
// SR-357 — 마이페이지 최근 본 상품(/shop/mypage/recently-viewed) 통합 테스트. `WishListPage.test.tsx`
// 관례 그대로(jsdom, URL+메서드 분기 fetch mock, `useNavigate` 스텁, 테스트별 고유 memberId/apiKey/sku).
declare const jest: typeof import('@jest/globals').jest

import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import RecentlyViewedPage from './RecentlyViewedPage'
import { saveSession } from '../session'
import type { RecentlyViewedItem } from '../types'

const mockNavigate = jest.fn()
jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom') as Record<string, unknown>
  return { ...actual, useNavigate: () => mockNavigate }
})

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

function viewedItem(i: number, overrides: Partial<RecentlyViewedItem> = {}): RecentlyViewedItem {
  return {
    sku: `sku-rvpage-${i}`, productName: `최근본상품${i}`, price: 10000 * i, imageUrl: null,
    soldOut: false, viewedAt: '2026-09-27T10:00:00',
    ...overrides,
  }
}

function session(i: number) {
  return {
    memberId: `m-rvpage-${i}`, memberName: '홍길동', grade: 'NORMAL',
    apiKey: `key-rvpage-${i}`, refreshToken: `r-rvpage-${i}`, refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
  }
}

describe('RecentlyViewedPage', () => {
  let fetchMock: import('@jest/globals').jest.Mock<typeof fetch>
  let listHeaders: Record<string, string>[]

  beforeEach(() => {
    fetchMock = jest.fn()
    listHeaders = []
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear()
    mockNavigate.mockClear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  function routeFetch(handlers: {
    list?: () => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const method = (init?.method ?? 'GET').toUpperCase()
      const [path] = url.split('?')

      if (path === '/api/members/me/recently-viewed' && method === 'GET') {
        const headers = (init?.headers ?? {}) as Record<string, string>
        listHeaders.push(headers)
        return handlers.list ? handlers.list() : jsonResponse(200, { items: [] })
      }
      if (path.startsWith('/api/cart')) return jsonResponse(200, { items: [] })
      throw new Error('unexpected url ' + url + ' ' + method)
    })
  }

  function renderPage() {
    return render(
      <MemoryRouter initialEntries={['/shop/mypage/recently-viewed']}>
        <Routes>
          <Route path="/shop/mypage/recently-viewed" element={<RecentlyViewedPage />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  // linked_tc: TC-FUNC-order-rvp-001
  // (1) 비로그인 진입 → /login?redirect=%2Fshop%2Fmypage%2Frecently-viewed로 즉시 이동, 목록 GET 안 나감.
  test('비로그인 진입 — 로그인 화면으로 리다이렉트되고 목록 조회는 호출되지 않는다', async () => {
    routeFetch({})
    renderPage()

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith(
      '/login?redirect=' + encodeURIComponent('/shop/mypage/recently-viewed'), { replace: true },
    ))
    expect(listHeaders).toHaveLength(0)
  })

  // linked_tc: TC-FUNC-order-rvp-002
  // (2) 0건 응답 → "최근 본 상품이 없습니다" 안내(오류 상태와 문구·역할 구분).
  test('0건 — "최근 본 상품이 없습니다" 안내', async () => {
    saveSession(session(1))
    routeFetch({ list: () => jsonResponse(200, { items: [] }) })
    renderPage()

    await screen.findByText('최근 본 상품이 없습니다')
    expect(listHeaders[0]['X-Api-Key']).toBe('key-rvpage-1')
  })

  // linked_tc: TC-FUNC-order-rvp-003
  // (3) GET 실패(500) → 오류 문구 + 다시 시도, 클릭 시 재요청 성공.
  test('목록 조회 실패(500) — 오류 문구 + 다시 시도, 클릭 시 재요청이 성공한다', async () => {
    saveSession(session(2))
    let calls = 0
    routeFetch({
      list: () => {
        calls += 1
        return calls === 1 ? jsonResponse(500, { code: 'RCV-5000', message: 'boom' }, false) : jsonResponse(200, { items: [] })
      },
    })
    renderPage()

    await screen.findByText(/불러오지 못했습니다/)
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }))
    await screen.findByText('최근 본 상품이 없습니다')
    expect(calls).toBe(2)
  })

  // linked_tc: TC-FUNC-order-rvp-004
  // (4) N건 응답 → 상품명·가격·대표 이미지·품절 배지가 응답 순서 그대로 렌더(서버가 이미 viewed_at
  // 내림차순 — 클라 재정렬 없음). 재작업(round2 필수1) — imageUrl 있는 행/null인 행을 함께 단언한다.
  test('N건 — 상품명·가격·대표 이미지·품절 배지가 응답 순서 그대로 렌더된다', async () => {
    saveSession(session(3))
    const rows = [
      viewedItem(1, { imageUrl: '/images/products/sku-1001.svg' }),
      viewedItem(2, { soldOut: true, imageUrl: null }),
    ]
    routeFetch({ list: () => jsonResponse(200, { items: rows }) })
    renderPage()

    await screen.findByText(rows[0].productName)
    const rowEls = screen.getAllByRole('listitem')
    expect(rowEls).toHaveLength(2)
    expect(rowEls[0].textContent).toContain(rows[0].productName)
    expect(rowEls[0].textContent).toContain('10,000원')
    expect(rowEls[1].textContent).toContain(rows[1].productName)
    expect(rowEls[1].textContent).toContain('품절')

    // imageUrl 있는 행 → <img alt=상품명 src=imageUrl>, null인 행 → 이니셜 플레이스홀더(대표이미지 없음).
    const img = screen.getByAltText(rows[0].productName) as HTMLImageElement
    expect(img.tagName).toBe('IMG')
    expect(img.src).toContain('/images/products/sku-1001.svg')
    expect(screen.getByLabelText(`${rows[1].productName} 대표이미지 없음`)).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-order-rvp-005
  // (5) X-Api-Key 헤더가 회원 apiKey 그대로 실리는지 확인(다른 마이페이지 화면과 동일 관례).
  test('목록 조회 요청에 회원 apiKey가 X-Api-Key로 실린다', async () => {
    saveSession(session(4))
    routeFetch({ list: () => jsonResponse(200, { items: [] }) })
    renderPage()

    await waitFor(() => expect(listHeaders).toHaveLength(1))
    expect(listHeaders[0]['X-Api-Key']).toBe('key-rvpage-4')
  })
})
