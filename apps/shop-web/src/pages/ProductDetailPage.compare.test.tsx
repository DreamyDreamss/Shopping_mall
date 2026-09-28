/** @jest-environment jsdom */
// SR-360 — 상품 상세(/shop/products/:sku) 비교함 담기/빼기 통합 테스트. `ProductDetailPage.wish.test.tsx`
// 구조를 그대로 복제한다(원본 파일을 더 키우지 않는다, 규칙 `lab/file-size-cap` 정신 유지 — STORY
// "파일" 절). routeFetch도 이 파일 전용 축약판(detail + 비교함 등록/해제/목록만)을 둔다.
declare const jest: typeof import('@jest/globals').jest

import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import ProductDetailPage from './ProductDetailPage'
import { saveSession } from '../session'
import type { ProductCompareItem, ProductDetail } from '../types'

const mockNavigate = jest.fn()
jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom') as Record<string, unknown>
  return { ...actual, useNavigate: () => mockNavigate }
})

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

// 테스트마다 인덱스로 유일화한다(STORY "테스트 격리" 절) — sku·memberId·apiKey 문자열 충돌을 막는다.
function product(i: number, overrides: Partial<ProductDetail> = {}): ProductDetail {
  const stockQty = overrides.stockQty ?? 5
  return {
    sku: `sku-compare-${i}`, productName: `비교테스트상품${i}`, price: 10000 + i * 1000, stockQty,
    saleYn: 'Y', listPrice: null, imageUrl: null, soldOut: stockQty !== null && stockQty <= 0, wishCount: 0,
    ...overrides,
  }
}

function session(i: number) {
  return {
    memberId: `m-compare-${i}`, memberName: '홍길동', grade: 'NORMAL',
    apiKey: `key-compare-${i}`, refreshToken: `r-compare-${i}`, refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
  }
}

function compareItem(sku: string, overrides: Partial<ProductCompareItem> = {}): ProductCompareItem {
  return {
    sku, productName: `상품-${sku}`, price: 10000, listPrice: null, imageUrl: null, avgRating: null,
    reviewCount: 0, stockQty: 5, soldOut: false, comparedAt: '2026-09-28T10:00:00', ...overrides,
  }
}

describe('ProductDetailPage — 비교함', () => {
  let fetchMock: import('@jest/globals').jest.Mock<typeof fetch>
  let addCompareCalls: string[]
  let removeCompareCalls: string[]

  beforeEach(() => {
    fetchMock = jest.fn()
    addCompareCalls = []
    removeCompareCalls = []
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear()
    mockNavigate.mockClear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  /** URL로 분기하는 fetch 라우터 — 상품 상세 + 비교함 등록/해제/목록만 다룬다(STORY "테스트" 절, 자체
   * 축약 routeFetch). 관련상품(`/api/products`)·장바구니 배지(`/api/cart`)·찜 목록(`/api/members/me/
   * wishes`)·최근 본 상품(`/api/members/me/recently-viewed`)은 기존 코드의 `.catch`가 이미 빈 값으로
   * 흡수하므로 기본값만 둔다. */
  function routeFetch(handlers: {
    detail?: (sku: string) => Response | Promise<Response>
    comparesList?: () => Response | Promise<Response>
    addCompare?: (body: { sku: string }) => Response | Promise<Response>
    removeCompare?: (sku: string) => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const method = (init?.method ?? 'GET').toUpperCase()
      const [path, queryString] = url.split('?')
      void queryString

      if (path === '/api/members/me/recently-viewed' && method === 'POST') {
        return jsonResponse(204, undefined)
      }
      if (path.includes('/reviews')) {
        return jsonResponse(200, { totalCount: 0, page: 1, size: 10, avgRating: null, reviewCount: 0, distribution: { '1': 0, '2': 0, '3': 0, '4': 0, '5': 0 }, items: [] })
      }
      if (path === '/api/members/me/wishes' && method === 'GET') {
        return jsonResponse(200, { items: [] })
      }
      if (path === '/api/members/me/compares' && method === 'GET') {
        return handlers.comparesList ? handlers.comparesList() : jsonResponse(200, { items: [] })
      }
      if (path === '/api/members/me/compares' && method === 'POST') {
        const body = init?.body ? (JSON.parse(String(init.body)) as { sku: string }) : { sku: '' }
        addCompareCalls.push(body.sku)
        return handlers.addCompare ? handlers.addCompare(body) : jsonResponse(200, { sku: body.sku, compared: true })
      }
      const removeMatch = path.match(/^\/api\/members\/me\/compares\/(.+)$/)
      if (removeMatch && method === 'DELETE') {
        const sku = decodeURIComponent(removeMatch[1])
        removeCompareCalls.push(sku)
        return handlers.removeCompare ? handlers.removeCompare(sku) : jsonResponse(204, null)
      }
      if (path.startsWith('/api/products/')) {
        const sku = decodeURIComponent(path.slice('/api/products/'.length))
        if (!handlers.detail) throw new Error('unexpected detail call ' + url)
        return handlers.detail(sku)
      }
      if (path.startsWith('/api/products')) return jsonResponse(200, [])
      if (path.startsWith('/api/cart')) return jsonResponse(200, { items: [] })
      throw new Error('unexpected url ' + url + ' ' + method)
    })
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

  const infoSection = () => screen.getByRole('region', { name: '상품 정보' })
  const compareToggle = () => within(infoSection()).getByRole('button', { name: '비교함 토글' })

  // (1) 로그인 + 비교함 목록에 이 sku 없음 → "비교함 담기" 라벨, comparesList 1회 호출.
  test('로그인 + 비교함 목록에 이 sku 없음 — "비교함 담기" 라벨이 보이고 목록 조회가 1회 나간다', async () => {
    saveSession(session(1))
    const p = product(1)
    let comparesCalls = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      comparesList: () => { comparesCalls += 1; return jsonResponse(200, { items: [] }) },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await waitFor(() => expect(comparesCalls).toBe(1))
    expect(compareToggle()).toHaveTextContent('비교함 담기')
  })

  // (2) 로그인 + 비교함 목록에 이 sku 있음 → 초기 compared=true, "비교함에서 빼기" 라벨.
  test('로그인 + 비교함 목록에 이 sku 있음 — 초기 렌더부터 "비교함에서 빼기" 라벨이다', async () => {
    saveSession(session(2))
    const p = product(2)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      comparesList: () => jsonResponse(200, { items: [compareItem(p.sku)] }),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await waitFor(() => expect(compareToggle()).toHaveTextContent('비교함에서 빼기'))
  })

  // (3) "비교함 담기" 클릭 성공(200) → 낙관적으로 즉시 "비교함에서 빼기", addCompare 바디 {sku} 확인.
  test('"비교함 담기" 클릭 성공 — 낙관적으로 즉시 "비교함에서 빼기"로 바뀌고 addCompare 바디가 정확하다', async () => {
    saveSession(session(3))
    const p = product(3)
    let resolveAdd: (() => void) | undefined
    const addPromise = new Promise<Response>(resolve => { resolveAdd = () => resolve(jsonResponse(200, { sku: p.sku, compared: true })) })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      comparesList: () => jsonResponse(200, { items: [] }),
      addCompare: () => addPromise,
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(compareToggle())

    // 서버 응답 전(낙관적 갱신) — 이미 "비교함에서 빼기".
    expect(compareToggle()).toHaveTextContent('비교함에서 빼기')

    resolveAdd?.()
    await waitFor(() => expect(compareToggle()).not.toBeDisabled())
    expect(compareToggle()).toHaveTextContent('비교함에서 빼기')
    expect(addCompareCalls).toEqual([p.sku])
  })

  // (4) "비교함 담기" 클릭이 409 CMP-4091 — 롤백(다시 "비교함 담기") + 인라인 문구(서버 원문 그대로).
  test('"비교함 담기" 클릭이 409 — 롤백되고 서버 원문 그대로 인라인 오류가 뜬다', async () => {
    saveSession(session(4))
    const p = product(4)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      comparesList: () => jsonResponse(200, { items: [] }),
      addCompare: () => jsonResponse(409, { code: 'CMP-4091', message: '비교함은 최대 4개까지 담을 수 있습니다' }, false),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(compareToggle())

    await waitFor(() => expect(compareToggle()).not.toBeDisabled())
    expect(compareToggle()).toHaveTextContent('비교함 담기')
    await within(infoSection()).findByText('비교함은 최대 4개까지 담을 수 있습니다')
  })

  // (5) "비교함에서 빼기" 클릭 성공(204) → removeCompare 호출, 라벨이 "비교함 담기"로 복귀.
  test('"비교함에서 빼기" 클릭 성공 — removeCompare가 호출되고 라벨이 "비교함 담기"로 복귀한다', async () => {
    saveSession(session(5))
    const p = product(5)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      comparesList: () => jsonResponse(200, { items: [compareItem(p.sku)] }),
      removeCompare: () => jsonResponse(204, null),
    })
    renderPage(p.sku)

    await waitFor(() => expect(compareToggle()).toHaveTextContent('비교함에서 빼기'))
    fireEvent.click(compareToggle())

    await waitFor(() => expect(removeCompareCalls).toEqual([p.sku]))
    await waitFor(() => expect(compareToggle()).toHaveTextContent('비교함 담기'))
  })

  // (6) 비로그인 상태에서 버튼 클릭 → fetchCompares/addCompare 어느 것도 호출되지 않고 로그인 화면으로
  // 정확한 경로로 이동한다.
  test('비로그인 상태로 버튼 클릭 — 목록/등록 어느 것도 호출되지 않고 로그인 화면으로 이동한다', async () => {
    const p = product(6)
    let comparesCalls = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      comparesList: () => { comparesCalls += 1; return jsonResponse(200, { items: [] }) },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(compareToggle())

    expect(mockNavigate).toHaveBeenCalledWith('/login?redirect=' + encodeURIComponent('/shop/products/' + p.sku))
    expect(comparesCalls).toBe(0)
    expect(addCompareCalls).toHaveLength(0)
  })

  // (7) "비교하기" 링크가 로그인 여부와 무관하게 항상 렌더된다.
  test('"비교하기" 링크가 항상 렌더된다(로그인 여부 무관)', async () => {
    const p = product(7)
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(within(infoSection()).getByRole('link', { name: '비교하기' })).toHaveAttribute('href', '#/shop/compare')
  })

  // (8) 연타 — 같은 이벤트 루프에서 두 번 클릭해도 등록 호출은 1회만(실패 사례집 SR-302 #1 대조).
  test('연타 — 같은 이벤트 루프에서 두 번 클릭해도 등록 호출은 1회만 나간다', async () => {
    saveSession(session(8))
    const p = product(8)
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      comparesList: () => jsonResponse(200, { items: [] }),
      addCompare: () => jsonResponse(200, { sku: p.sku, compared: true }),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    const toggle = compareToggle()
    act(() => {
      toggle.click()
      toggle.click()
    })

    await waitFor(() => expect(addCompareCalls).toHaveLength(1))
  })
})
