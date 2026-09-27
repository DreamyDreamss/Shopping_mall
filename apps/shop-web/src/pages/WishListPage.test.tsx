/** @jest-environment jsdom */
// SR-356 — 내 찜 목록(/shop/mypage/wishes) 통합 테스트. `MyAddressesPage.test.tsx` 관례 그대로(jsdom,
// URL+메서드 분기 fetch mock, `useNavigate` 스텁, 테스트별 고유 memberId/apiKey/sku).
declare const jest: typeof import('@jest/globals').jest

import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import WishListPage from './WishListPage'
import { saveSession } from '../session'
import type { WishItem } from '../types'

const mockNavigate = jest.fn()
jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom') as Record<string, unknown>
  return { ...actual, useNavigate: () => mockNavigate }
})

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

function wishItem(i: number, overrides: Partial<WishItem> = {}): WishItem {
  return {
    sku: `sku-wishpage-${i}`, productName: `찜페이지상품${i}`, price: 10000 * i, imageUrl: null,
    soldOut: false, wishedAt: '2026-09-19T10:00:00',
    ...overrides,
  }
}

function session(i: number) {
  return {
    memberId: `m-wishpage-${i}`, memberName: '홍길동', grade: 'NORMAL',
    apiKey: `key-wishpage-${i}`, refreshToken: `r-wishpage-${i}`, refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
  }
}

describe('WishListPage', () => {
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
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const method = (init?.method ?? 'GET').toUpperCase()
      const [path] = url.split('?')

      if (path === '/api/members/me/wishes' && method === 'GET') {
        const headers = (init?.headers ?? {}) as Record<string, string>
        listHeaders.push(headers)
        return handlers.list ? handlers.list() : jsonResponse(200, { items: [] })
      }
      const removeMatch = path.match(/^\/api\/members\/me\/wishes\/(.+)$/)
      if (removeMatch && method === 'DELETE') {
        const sku = decodeURIComponent(removeMatch[1])
        removeCalls.push(sku)
        return handlers.remove ? handlers.remove(sku) : jsonResponse(204, null)
      }
      if (path.startsWith('/api/cart')) return jsonResponse(200, { items: [] })
      throw new Error('unexpected url ' + url + ' ' + method)
    })
  }

  function renderPage() {
    return render(
      <MemoryRouter initialEntries={['/shop/mypage/wishes']}>
        <Routes>
          <Route path="/shop/mypage/wishes" element={<WishListPage />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  // (1) 비로그인 진입 → /login?redirect=%2Fshop%2Fmypage%2Fwishes로 즉시 이동, 목록 GET 안 나감.
  test('비로그인 진입 — 로그인 화면으로 리다이렉트되고 목록 조회는 호출되지 않는다', async () => {
    routeFetch({})
    renderPage()

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith(
      '/login?redirect=' + encodeURIComponent('/shop/mypage/wishes'), { replace: true },
    ))
    expect(listHeaders).toHaveLength(0)
  })

  // (2) 0건 응답 → "찜한 상품이 없습니다" + 상품 보러 가기 링크(href).
  test('0건 — 안내 문구 + 상품 보러 가기 링크', async () => {
    saveSession(session(1))
    routeFetch({ list: () => jsonResponse(200, { items: [] }) })
    renderPage()

    await screen.findByText('찜한 상품이 없습니다')
    expect(screen.getByRole('link', { name: '상품 보러 가기' })).toHaveAttribute('href', '#/shop/products')
    expect(listHeaders[0]['X-Api-Key']).toBe('key-wishpage-1')
  })

  // (3) 목록 GET 실패(500) → 오류 문구 + 다시 시도, 클릭 시 재요청.
  test('목록 조회 실패(500) — 오류 문구 + 다시 시도, 클릭 시 재요청이 성공한다', async () => {
    saveSession(session(2))
    let calls = 0
    routeFetch({
      list: () => {
        calls += 1
        return calls === 1 ? jsonResponse(500, { code: 'ORD-5000', message: 'boom' }, false) : jsonResponse(200, { items: [] })
      },
    })
    renderPage()

    await screen.findByText(/불러오지 못했습니다/)
    fireEvent.click(screen.getByRole('button', { name: '다시 시도' }))
    await screen.findByText('찜한 상품이 없습니다')
    expect(calls).toBe(2)
  })

  // (4) N건 응답 → 행 렌더, "찜 해제" 클릭 → DELETE 호출 + 그 행이 즉시 사라짐.
  test('N건 — 행이 렌더되고 찜 해제 클릭 시 DELETE 호출 + 그 행이 즉시 사라진다', async () => {
    saveSession(session(3))
    const rows = [wishItem(1), wishItem(2)]
    routeFetch({ list: () => jsonResponse(200, { items: rows }) })
    renderPage()

    await screen.findByText(rows[0].productName)
    fireEvent.click(screen.getByRole('button', { name: `${rows[0].productName} 찜 해제` }))

    await waitFor(() => expect(screen.queryByText(rows[0].productName)).not.toBeInTheDocument())
    expect(removeCalls).toEqual([rows[0].sku])
    expect(screen.getByText(rows[1].productName)).toBeInTheDocument()
  })

  // (5) 해제 API 실패 → 지웠던 행이 되돌아옴(롤백) + 오류 문구.
  test('해제 API 실패 — 지웠던 행이 되돌아오고 오류 문구가 뜬다', async () => {
    saveSession(session(4))
    const rows = [wishItem(1)]
    routeFetch({
      list: () => jsonResponse(200, { items: rows }),
      remove: () => jsonResponse(404, { code: 'ORD-4041', message: '찜을 찾을 수 없습니다' }, false),
    })
    renderPage()

    await screen.findByText(rows[0].productName)
    fireEvent.click(screen.getByRole('button', { name: `${rows[0].productName} 찜 해제` }))

    expect(await screen.findByText('찜을 찾을 수 없습니다')).toBeInTheDocument()
    expect(screen.getByText(rows[0].productName)).toBeInTheDocument()
  })

  // (6) 해제 연타(같은 행 2회 클릭) → DELETE 1회만.
  test('해제 연타 — 같은 행 2회 클릭에도 DELETE 호출은 1회만 나간다', async () => {
    saveSession(session(5))
    const rows = [wishItem(1)]
    routeFetch({ list: () => jsonResponse(200, { items: rows }) })
    renderPage()

    await screen.findByText(rows[0].productName)
    const removeButton = screen.getByRole('button', { name: `${rows[0].productName} 찜 해제` })
    act(() => {
      removeButton.click()
      removeButton.click()
    })

    await waitFor(() => expect(removeCalls).toEqual([rows[0].sku]))
  })

  // (7, 재작업 round2 — QA round1 FAIL 필수2 재현) A행 해제(실패 예정)와 B행 해제(즉시 성공)를 겹쳐
  // 누른다. B가 먼저 성공해 화면에서 사라진 뒤에야 A의 실패 응답이 늦게 도착한다 — A의 롤백이 클릭
  // 시점 스냅샷 전체를 되돌리면 이미 서버에서 지워진 B까지 되살아난다(QA 프로브 실측과 동일 재현).
  // 3번째 행(C)로 "원래 자리"(B 다음이 아니라 원래 있던 위치, 여기선 C 앞)에 복원되는지도 함께 본다.
  test('A행 해제 실패 + B행 해제 성공 동시 진행 — A의 롤백이 이미 사라진 B를 되살리지 않는다', async () => {
    saveSession(session(6))
    const rowA = wishItem(1)
    const rowB = wishItem(2)
    const rowC = wishItem(3)
    let resolveA: ((r: Response) => void) | undefined
    const aPromise = new Promise<Response>(resolve => { resolveA = resolve })
    routeFetch({
      list: () => jsonResponse(200, { items: [rowA, rowB, rowC] }),
      remove: sku => (sku === rowA.sku ? aPromise : jsonResponse(204, null)),
    })
    renderPage()

    await screen.findByText(rowA.productName)
    fireEvent.click(screen.getByRole('button', { name: `${rowA.productName} 찜 해제` }))
    fireEvent.click(screen.getByRole('button', { name: `${rowB.productName} 찜 해제` }))

    // B는 즉시 성공해 화면에서 사라진다. A는 아직 응답 대기 중(이미 낙관적으로 사라진 상태).
    await waitFor(() => expect(screen.queryByText(rowB.productName)).not.toBeInTheDocument())
    expect(screen.queryByText(rowA.productName)).not.toBeInTheDocument()

    // A의 실패 응답이 뒤늦게 도착 — A만 원래 자리(C 앞)에 복원되고, B는 되살아나지 않는다.
    resolveA?.(jsonResponse(404, { code: 'ORD-4041', message: '찜을 찾을 수 없습니다' }, false))
    expect(await screen.findByText(rowA.productName)).toBeInTheDocument()
    expect(screen.queryByText(rowB.productName)).not.toBeInTheDocument()
    expect(screen.getByText(rowC.productName)).toBeInTheDocument()

    const order = screen.getAllByRole('listitem').map(el => el.textContent ?? '')
    expect(order[0]).toContain(rowA.productName)
    expect(order[1]).toContain(rowC.productName)
  })
})
