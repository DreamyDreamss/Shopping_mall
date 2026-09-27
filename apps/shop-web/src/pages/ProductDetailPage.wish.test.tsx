/** @jest-environment jsdom */
// SR-356 — 상품 상세(/shop/products/:sku) 찜하기(위시리스트) 통합 테스트. `ProductDetailPage.test.tsx`
// 관례를 이어 쓰되 파일을 분리한다(`.reviews.test.tsx`와 동일 이유 — 원본을 더 키우지 않는다,
// 규칙 `lab/file-size-cap`). routeFetch도 이 파일 전용 축약판(detail + 찜 등록/해제/목록만)을 둔다.
declare const jest: typeof import('@jest/globals').jest

import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import ProductDetailPage from './ProductDetailPage'
import { saveSession } from '../session'
import type { ProductDetail, WishItem } from '../types'

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
    sku: `sku-wish-${i}`, productName: `찜테스트상품${i}`, price: 10000 + i * 1000, stockQty,
    saleYn: 'Y', listPrice: null, imageUrl: null, soldOut: stockQty !== null && stockQty <= 0, wishCount: 0,
    ...overrides,
  }
}

function session(i: number) {
  return {
    memberId: `m-wish-${i}`, memberName: '홍길동', grade: 'NORMAL',
    apiKey: `key-wish-${i}`, refreshToken: `r-wish-${i}`, refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
  }
}

function wishItem(sku: string, overrides: Partial<WishItem> = {}): WishItem {
  return { sku, productName: `상품-${sku}`, price: 10000, imageUrl: null, soldOut: false, wishedAt: '2026-09-19T10:00:00', ...overrides }
}

describe('ProductDetailPage — 찜하기', () => {
  let fetchMock: import('@jest/globals').jest.Mock<typeof fetch>
  let addWishCalls: string[]
  let removeWishCalls: string[]
  let wishesListCalls: number[]

  beforeEach(() => {
    fetchMock = jest.fn()
    addWishCalls = []
    removeWishCalls = []
    wishesListCalls = []
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear()
    mockNavigate.mockClear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  /** URL로 분기하는 fetch 라우터 — 상품 상세 + 찜 등록/해제/목록만 다룬다(STORY "테스트" 절, 자체 축약
   * routeFetch). 관련상품(`/api/products`)·장바구니 배지(`/api/cart`)는 기존 코드의 `.catch`가 이미
   * 빈 값으로 흡수하므로 기본값만 둔다. */
  function routeFetch(handlers: {
    detail?: (sku: string) => Response | Promise<Response>
    wishesList?: (page: number) => Response | Promise<Response>
    addWish?: (body: { sku: string }) => Response | Promise<Response>
    removeWish?: (sku: string) => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const method = (init?.method ?? 'GET').toUpperCase()
      const [path, queryString] = url.split('?')

      if (path === '/api/members/me/wishes' && method === 'GET') {
        const params = new URLSearchParams(queryString ?? '')
        const page = Number(params.get('page') ?? '1')
        wishesListCalls.push(page)
        return handlers.wishesList ? handlers.wishesList(page) : jsonResponse(200, { items: [] })
      }
      if (path === '/api/members/me/wishes' && method === 'POST') {
        const body = init?.body ? (JSON.parse(String(init.body)) as { sku: string }) : { sku: '' }
        addWishCalls.push(body.sku)
        return handlers.addWish ? handlers.addWish(body) : jsonResponse(200, null)
      }
      const removeMatch = path.match(/^\/api\/members\/me\/wishes\/(.+)$/)
      if (removeMatch && method === 'DELETE') {
        const sku = decodeURIComponent(removeMatch[1])
        removeWishCalls.push(sku)
        return handlers.removeWish ? handlers.removeWish(sku) : jsonResponse(204, null)
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
  const wishToggle = () => within(infoSection()).getByRole('button', { name: '찜 토글' })

  // (1) wishCount=7 응답 → 화면에 7 표시.
  test('wishCount=7 응답 — 찜 수가 7로 표시되고 내 찜 목록 링크가 있다', async () => {
    const p = product(1, { wishCount: 7 })
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    expect(within(infoSection()).getByLabelText('찜 수')).toHaveTextContent('찜 7')
    expect(within(infoSection()).getByRole('link', { name: '내 찜 목록' })).toHaveAttribute('href', '#/shop/mypage/wishes')
  })

  // (2) 비로그인 상태로 찜 버튼 클릭 → /login?redirect=...로 이동, POST가 한 번도 안 감.
  test('비로그인 상태로 찜 버튼 클릭 — 로그인 화면으로 이동하고 등록 POST는 나가지 않는다', async () => {
    const p = product(2)
    routeFetch({ detail: sku => jsonResponse(200, { ...p, sku }) })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(wishToggle())

    expect(mockNavigate).toHaveBeenCalledWith('/login?redirect=' + encodeURIComponent('/shop/products/' + p.sku))
    expect(addWishCalls).toHaveLength(0)
  })

  // (3) 로그인 + 찜 안 한 상태에서 클릭 → 즉시 "찜됨" + wishCount+1(낙관적), 지연 중에도 유지, 성공
  // 응답 도착 후에도 동일.
  test('로그인 + 찜 안 한 상태에서 클릭 — 즉시 찜됨으로 바뀌고 wishCount가 +1 유지된다', async () => {
    saveSession(session(3))
    const p = product(3, { wishCount: 4 })
    let resolveAdd: (() => void) | undefined
    const addPromise = new Promise<Response>(resolve => { resolveAdd = () => resolve(jsonResponse(200, null)) })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      wishesList: () => jsonResponse(200, { items: [] }),
      addWish: () => addPromise,
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    fireEvent.click(wishToggle())

    // 서버 응답 전(낙관적 갱신) — 이미 찜됨 + 5.
    expect(wishToggle()).toHaveTextContent('찜됨')
    expect(within(infoSection()).getByLabelText('찜 수')).toHaveTextContent('찜 5')

    resolveAdd?.()
    await waitFor(() => expect(wishToggle()).not.toBeDisabled())
    expect(wishToggle()).toHaveTextContent('찜됨')
    expect(within(infoSection()).getByLabelText('찜 수')).toHaveTextContent('찜 5')
    expect(addWishCalls).toEqual([p.sku])
  })

  // (4) 로그인 + 찜한 상태에서 클릭 → DELETE 호출, 낙관적으로 -1.
  test('로그인 + 찜한 상태에서 클릭 — DELETE 호출과 함께 낙관적으로 -1 된다', async () => {
    saveSession(session(4))
    const p = product(4, { wishCount: 6 })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      wishesList: () => jsonResponse(200, { items: [wishItem(p.sku)] }),
      removeWish: () => jsonResponse(204, null),
    })
    renderPage(p.sku)

    await waitFor(() => expect(wishToggle()).toHaveTextContent('찜됨'))
    fireEvent.click(wishToggle())

    expect(within(infoSection()).getByLabelText('찜 수')).toHaveTextContent('찜 5')
    await waitFor(() => expect(removeWishCalls).toEqual([p.sku]))
  })

  // (5) 토글 실패(500/404) → 버튼·카운트가 클릭 직전 값으로 정확히 되돌아간다(연속 2회 — 롤백 산술
  // 버그 실측: 재연산이면 두 번째 롤백에서 값이 어긋난다).
  test('토글 실패(500/404) — 버튼·카운트가 클릭 직전 값으로 정확히 되돌아간다(연속 2회)', async () => {
    saveSession(session(5))
    const p = product(5, { wishCount: 9 })
    let addCallCount = 0
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      wishesList: () => jsonResponse(200, { items: [] }),
      addWish: () => {
        addCallCount += 1
        return addCallCount === 1
          ? jsonResponse(500, { code: 'ORD-5000', message: '일시적 오류' }, false)
          : jsonResponse(404, { code: 'ORD-4041', message: '상품 없음' }, false)
      },
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)

    fireEvent.click(wishToggle())
    await waitFor(() => expect(wishToggle()).not.toBeDisabled())
    expect(wishToggle()).toHaveTextContent('찜하기')
    expect(within(infoSection()).getByLabelText('찜 수')).toHaveTextContent('찜 9')

    fireEvent.click(wishToggle())
    await waitFor(() => expect(wishToggle()).not.toBeDisabled())
    expect(wishToggle()).toHaveTextContent('찜하기')
    expect(within(infoSection()).getByLabelText('찜 수')).toHaveTextContent('찜 9')
    expect(addCallCount).toBe(2)
  })

  // (6) 연타 — 같은 이벤트 루프에서 두 번 클릭해도 POST/DELETE는 1회만(실패 사례집 SR-302 #1 대조 —
  // act() 한 스코프에서 묶어 발사).
  test('연타 — 같은 이벤트 루프에서 두 번 클릭해도 등록 호출은 1회만 나간다', async () => {
    saveSession(session(6))
    const p = product(6, { wishCount: 1 })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      wishesList: () => jsonResponse(200, { items: [] }),
      addWish: () => jsonResponse(200, null),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    const toggle = wishToggle()
    act(() => {
      toggle.click()
      toggle.click()
    })

    await waitFor(() => expect(addWishCalls).toHaveLength(1))
  })

  // (7) 진입 시 1페이지 응답에 대상 sku가 있으면 최초 렌더부터 "찜됨" 상태.
  test('진입 시 1페이지 응답에 대상 sku가 있으면 최초 렌더부터 찜됨 상태다', async () => {
    saveSession(session(7))
    const p = product(7, { wishCount: 2 })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      wishesList: () => jsonResponse(200, { items: [wishItem(p.sku)] }),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await waitFor(() => expect(wishToggle()).toHaveTextContent('찜됨'))
  })

  // (8, 사람 수정 round1) 21번째로 찜한 상품(2페이지 첫 번째 행) — page=1은 20건이 꽉 찼지만 대상
  // sku가 없고, page=2 1건에 있다 → page=1, page=2 순서로 호출되고 최종 찜됨으로 표시된다.
  test('21번째로 찜한 상품 — page=1(20건 꽉 참) 다음 page=2까지 이어 조회해 찜됨으로 표시한다', async () => {
    saveSession(session(8))
    const p = product(8, { wishCount: 21 })
    const page1 = Array.from({ length: 20 }, (_, i) => wishItem(`sku-other-${i}`))
    const page2 = [wishItem(p.sku)]
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      wishesList: page => jsonResponse(200, { items: page === 1 ? page1 : page2 }),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    await waitFor(() => expect(wishesListCalls).toEqual([1, 2]))
    expect(wishToggle()).toHaveTextContent('찜됨')
  })

  // (9, 재작업 round2 — QA round1 FAIL 필수1 재현) 찜 여부 확인 루프가 1페이지 응답을 기다리는 동안
  // 사용자가 찜 버튼을 눌러 등록에 성공한다. 그 뒤에야 확인 루프의 지연 응답(대상 sku가 없는 목록 — 원래
  // 라면 wished=false로 판정될 응답)이 도착한다 — 이 응답이 이미 성공한 토글 결과를 덮어쓰면 안 된다.
  // (고치기 전: 화면이 '♡ 찜하기' + 카운트만 +1로 남아, 서버는 찜함/화면은 안 찜함 상태가 됐다 — QA
  // 프로브 실측과 동일한 재현.)
  test('찜 확인 루프 진행 중 토글 성공 — 루프의 뒤늦은 응답이 토글 이후 상태를 덮어쓰지 않는다', async () => {
    saveSession(session(9))
    const p = product(9, { wishCount: 3 })
    let resolveList: ((r: Response) => void) | undefined
    const pendingList = new Promise<Response>(resolve => { resolveList = resolve })
    routeFetch({
      detail: sku => jsonResponse(200, { ...p, sku }),
      wishesList: () => pendingList,
      addWish: () => jsonResponse(200, null),
    })
    renderPage(p.sku)

    await screen.findByText(p.productName)
    // 확인 루프는 아직 1페이지 응답을 기다리는 중 — 이 시점에 사용자가 토글한다.
    fireEvent.click(wishToggle())
    expect(wishToggle()).toHaveTextContent('찜됨')
    await waitFor(() => expect(addWishCalls).toEqual([p.sku]))
    await waitFor(() => expect(wishToggle()).not.toBeDisabled())
    expect(wishToggle()).toHaveTextContent('찜됨')
    expect(within(infoSection()).getByLabelText('찜 수')).toHaveTextContent('찜 4')

    // 확인 루프의 지연 응답 도착 — 대상 sku가 없는 0건 목록(고치기 전이면 wished=false로 되돌린다).
    resolveList?.(jsonResponse(200, { items: [] }))
    await act(async () => {
      await Promise.resolve()
      await Promise.resolve()
      await Promise.resolve()
    })
    expect(wishToggle()).toHaveTextContent('찜됨')
    expect(within(infoSection()).getByLabelText('찜 수')).toHaveTextContent('찜 4')
  })
})
