/** @jest-environment jsdom */
// SR-303 — 상품 목록(검색·필터) 통합 테스트. `ShopHomePage.test.tsx`의 관례(jsdom, URL 분기
// `routeFetch`, `@jest/globals`의 `expect`)를 그대로 따른다.
import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import ProductListPage from './ProductListPage'

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

function queryOf(url: string): URLSearchParams {
  return new URLSearchParams(url.split('?')[1] ?? '')
}

interface ProductStub {
  sku: string; productName: string; price: number; stockQty: number; saleYn: string
  listPrice?: number | null; imageUrl?: string | null
}

// 테스트마다 인덱스로 유일화한다(STORY "테스트 격리" 절) — sku·상품명 문자열 충돌을 막는다.
function product(i: number, overrides: Partial<ProductStub> = {}): ProductStub {
  return {
    sku: `sku-${i}`, productName: `테스트상품${i}`, price: 10000 + i * 1000, stockQty: 5, saleYn: 'Y',
    listPrice: null, imageUrl: null, ...overrides,
  }
}

describe('ProductListPage', () => {
  let fetchMock: jest.Mock<typeof fetch>

  beforeEach(() => {
    fetchMock = jest.fn()
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear() // 세션 키 + 최근본상품 키가 다음 테스트로 새지 않게 한다.
  })

  afterEach(() => {
    localStorage.clear()
  })

  /** URL로 분기하는 fetch 라우터 — 이 화면은 비로그인 기본 상태라 `/api/cart`는 호출되지 않는다. */
  function routeFetch(handlers: { products?: (url: string) => Response | Promise<Response> }) {
    fetchMock.mockImplementation(async (input: unknown) => {
      const url = String(input)
      if (url.startsWith('/api/products')) {
        if (!handlers.products) throw new Error('unexpected /api/products call')
        return handlers.products(url)
      }
      if (url.startsWith('/api/cart')) return jsonResponse(200, { items: [] })
      throw new Error('unexpected url ' + url)
    })
  }

  // 재작업 지시 1 — `ShopHomePage`가 `?keyword=`를 실어 이 화면으로 이동한다(Gnb 검색 제출·
  // CategoryShortcuts 클릭 공통). 기본값은 무파라미터(진입 경로 신설 이전과 동일한 초기 상태).
  function renderPage(initialPath = '/shop/products') {
    return render(
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/shop/products" element={<ProductListPage />} />
          {/* 상세 화면 자체는 별도 SR(SR-304) — 여기서는 navigate가 실제로 일어났는지만 확인하는
              캐치올 placeholder. `App.tsx`엔 이 라우트를 추가하지 않는다(STORY "라우팅 연결" 절). */}
          <Route path="/shop/products/:sku" element={<div>상세(placeholder)</div>} />
        </Routes>
      </MemoryRouter>,
    )
  }

  const gridSection = () => screen.getByRole('region', { name: '상품 목록' })

  // linked_tc: TC-FUNC-shop-products-015
  test('로딩 시 스켈레톤 노출 → 응답 도착 후 그리드 렌더(GET /api/products 확인)', async () => {
    let requestedUrl: string | undefined
    routeFetch({
      products: url => { requestedUrl = url; return jsonResponse(200, [product(1), product(2)]) },
    })
    renderPage()
    expect(screen.getByRole('status', { name: '상품 목록 불러오는 중' })).toBeInTheDocument()
    await within(gridSection()).findByText('테스트상품1')
    expect(within(gridSection()).getByText('테스트상품2')).toBeInTheDocument()
    expect(requestedUrl).toBe('/api/products')
  })

  // 재작업(round 1 QA CONCERNS carry-back, 2026-09-17) — `ShopHomePage`의 검색 제출·카테고리 클릭이
  // `?keyword=`로 이 화면에 진입할 때, 그 값이 입력창 초기값(appliedKeyword)으로 반영되고 최초
  // 조회부터 그 keyword로 나가는지 확인한다.
  // linked_tc: TC-FUNC-shop-products-016
  test('URL의 keyword 쿼리가 초기 검색어로 입력창에 반영되고, 최초 조회부터 그 keyword로 나간다', async () => {
    let requestedUrl: string | undefined
    routeFetch({
      products: url => { requestedUrl = url; return jsonResponse(200, [product(1)]) },
    })
    renderPage('/shop/products?keyword=운동화')
    await within(gridSection()).findByText('테스트상품1')

    expect(screen.getByLabelText('상품 검색')).toHaveValue('운동화')
    expect(queryOf(requestedUrl ?? '').get('keyword')).toBe('운동화')
  })

  // linked_tc: TC-FUNC-shop-products-017
  test('검색 제출 → keyword 쿼리로 재요청되고, 0건이면 "검색어 지우기"로 초기화 재요청한다', async () => {
    const urls: string[] = []
    routeFetch({
      products: url => {
        urls.push(url)
        return queryOf(url).get('keyword') === 'nope' ? jsonResponse(200, []) : jsonResponse(200, [product(1)])
      },
    })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')

    const searchInput = screen.getByLabelText('상품 검색')
    fireEvent.change(searchInput, { target: { value: 'nope' } })
    fireEvent.submit(searchInput.closest('form')!)

    await within(gridSection()).findByText('검색 결과가 없습니다')
    expect(queryOf(urls[urls.length - 1]).get('keyword')).toBe('nope')

    fireEvent.click(within(gridSection()).getByRole('button', { name: '검색어 지우기' }))
    await within(gridSection()).findByText('테스트상품1')
    expect(queryOf(urls[urls.length - 1]).get('keyword')).toBeNull()
    expect(screen.getByLabelText('상품 검색')).toHaveValue('')
  })

  // linked_tc: TC-FUNC-shop-products-018
  test('재고 필터 on → inStock=true로 재요청되고, 0건이면 "필터 초기화"로 재고만 리셋한다(검색어 입력값은 건드리지 않음)', async () => {
    const urls: string[] = []
    routeFetch({
      products: url => {
        urls.push(url)
        return queryOf(url).get('inStock') === 'true' ? jsonResponse(200, []) : jsonResponse(200, [product(1)])
      },
    })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')

    // 검색창에 입력만 해두고(제출 안 함, appliedKeyword는 여전히 빈 값) — 필터 초기화가 검색어
    // 관련 상태를 건드리지 않는지 확인하는 대조군.
    fireEvent.change(screen.getByLabelText('상품 검색'), { target: { value: '아직제출안한검색어' } })

    fireEvent.click(screen.getByLabelText('재고 있는 상품만'))
    await within(gridSection()).findByText('조건에 맞는 상품이 없습니다')
    expect(queryOf(urls[urls.length - 1]).get('inStock')).toBe('true')

    fireEvent.click(within(gridSection()).getByRole('button', { name: '필터 초기화' }))
    await within(gridSection()).findByText('테스트상품1')
    expect(queryOf(urls[urls.length - 1]).get('inStock')).toBeNull()
    expect(screen.getByLabelText('상품 검색')).toHaveValue('아직제출안한검색어')
    expect(screen.getByLabelText('재고 있는 상품만')).not.toBeChecked()
  })

  // linked_tc: TC-FUNC-shop-products-019
  test('가격대 입력은 재요청 없이 클라이언트에서만 좁혀진다(경계값 포함)', async () => {
    let callCount = 0
    routeFetch({
      products: () => { callCount += 1; return jsonResponse(200, [product(1), product(2), product(3)]) },
    })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')
    expect(callCount).toBe(1)

    // product(i) 가격 = 10000 + i*1000 → 1:11000, 2:12000, 3:13000. min=max=12000은 product(2) 가격과
    // 정확히 일치한다(경계값 포함 확인).
    fireEvent.change(screen.getByLabelText('최소 가격'), { target: { value: '12000' } })
    fireEvent.change(screen.getByLabelText('최대 가격'), { target: { value: '12000' } })

    await within(gridSection()).findByText('테스트상품2')
    expect(within(gridSection()).queryByText('테스트상품1')).not.toBeInTheDocument()
    expect(within(gridSection()).queryByText('테스트상품3')).not.toBeInTheDocument()
    expect(callCount).toBe(1) // 가격대 입력은 서버 재요청을 만들지 않는다.
  })

  // linked_tc: TC-FUNC-shop-products-020
  test('정렬 select 변경 시 카드 렌더 순서가 바뀐다(추천순 → 낮은가격순 → 높은가격순)', async () => {
    // 서버 순서를 일부러 가격순이 아니게 반환해(3,1,2) 정렬 로직이 실제로 재배열하는지 검증한다.
    routeFetch({ products: () => jsonResponse(200, [product(3), product(1), product(2)]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품3')

    const namesInOrder = () =>
      within(gridSection()).getAllByText(/^테스트상품\d+$/).map(el => el.textContent)

    expect(namesInOrder()).toEqual(['테스트상품3', '테스트상품1', '테스트상품2']) // recommend = 응답 순서 그대로

    fireEvent.change(screen.getByLabelText('정렬'), { target: { value: 'priceAsc' } })
    expect(namesInOrder()).toEqual(['테스트상품1', '테스트상품2', '테스트상품3'])

    fireEvent.change(screen.getByLabelText('정렬'), { target: { value: 'priceDesc' } })
    expect(namesInOrder()).toEqual(['테스트상품3', '테스트상품2', '테스트상품1'])
  })

  // linked_tc: TC-FUNC-shop-products-021
  test('페이지네이션 — 다음 클릭으로 다음 페이지, 마지막 페이지 다음 비활성, 첫 페이지 이전 비활성', async () => {
    const rows = Array.from({ length: 14 }, (_, i) => product(i + 1)) // 14건 → PAGE_SIZE(12) 초과
    routeFetch({ products: () => jsonResponse(200, rows) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')

    expect(screen.getByRole('button', { name: '이전' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '다음' })).not.toBeDisabled()
    expect(within(gridSection()).queryByText('테스트상품13')).not.toBeInTheDocument() // 1페이지엔 1~12만

    fireEvent.click(screen.getByRole('button', { name: '다음' }))
    await within(gridSection()).findByText('테스트상품13')
    expect(within(gridSection()).getByText('테스트상품14')).toBeInTheDocument()
    expect(within(gridSection()).queryByText('테스트상품1')).not.toBeInTheDocument()

    expect(screen.getByRole('button', { name: '다음' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '이전' })).not.toBeDisabled()
  })

  // linked_tc: TC-FUNC-shop-products-022
  test('조회 실패 → 오류 문구+[다시 시도], 클릭 시 재조회 성공', async () => {
    let calls = 0
    routeFetch({
      products: () => {
        calls += 1
        return calls === 1 ? jsonResponse(500, { message: 'boom' }, false) : jsonResponse(200, [product(1)])
      },
    })
    renderPage()
    await within(gridSection()).findByText(/불러오지 못했습니다/)
    fireEvent.click(within(gridSection()).getByRole('button', { name: '다시 시도' }))
    await within(gridSection()).findByText('테스트상품1')
    expect(calls).toBe(2)
  })

  // 실패 사례집 대조(SR-302 #1) — fireEvent.click 3회 분리 호출은 첫 클릭에서 버튼이 DOM에서
  // 사라져(로딩으로 교체) 2·3번째가 빈 노드에 발사되는 거짓 보증을 만든다. 네이티브 `.click()`을
  // 하나의 `act()` 안에서 flush 없이 연달아 호출해 "버튼이 살아있는 상태에서의 연타"를 재현한다.
  // linked_tc: TC-FUNC-shop-products-023
  test('다시 시도 연타 시 실제 요청이 한 번만 나간다(in-flight 가드)', async () => {
    let calls = 0
    routeFetch({
      products: () => {
        calls += 1
        return calls === 1 ? jsonResponse(500, { message: 'boom' }, false) : jsonResponse(200, [product(1)])
      },
    })
    renderPage()
    await within(gridSection()).findByText(/불러오지 못했습니다/)

    const retryBtn = within(gridSection()).getByRole('button', { name: '다시 시도' })
    act(() => {
      retryBtn.click()
      retryBtn.click()
      retryBtn.click()
    })

    // 최초 마운트 1회(실패) + 연타 중 실제로 통과한 1회 = 2. 가드가 없었다면 세 클릭 모두 새 요청을
    // 만들어 calls가 2보다 커졌을 것이다.
    expect(calls).toBe(2)
    await within(gridSection()).findByText('테스트상품1')
    expect(calls).toBe(2)
  })

  // 프레임워크 실행 모델 함정(STORY 절) — 서로 다른 파라미터의 요청 두 개가 겹쳐 나가고, 나중에
  // 보낸 요청의 응답이 먼저 도착해도 화면은 항상 "가장 나중에 보낸 요청"의 결과를 반영해야 한다
  // (세대 카운터 검증). `inFlightKeyRef`(같은 파라미터만 억제)로는 이 케이스를 막을 수 없다.
  // linked_tc: TC-FUNC-shop-products-024
  test('응답 순서 역전 시 더 나중에 보낸 요청의 결과만 반영된다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1)]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')

    const pending: { resolve: (r: Response) => void }[] = []
    fetchMock.mockImplementation(() => new Promise<Response>(resolve => { pending.push({ resolve }) }))

    // 첫 번째(더 먼저 보낸) 요청 — 검색 제출.
    const searchInput = screen.getByLabelText('상품 검색')
    fireEvent.change(searchInput, { target: { value: 'old' } })
    fireEvent.submit(searchInput.closest('form')!)

    // 두 번째(더 나중에 보낸, 다른 파라미터) 요청 — 재고 필터 토글.
    fireEvent.click(screen.getByLabelText('재고 있는 상품만'))

    expect(pending.length).toBe(2)

    // 응답 순서를 뒤바꾼다 — 나중 요청을 먼저, 먼저 요청을 나중에 resolve한다.
    await act(async () => { pending[1].resolve(jsonResponse(200, [product(9, { productName: '최신결과' })])) })
    await within(gridSection()).findByText('최신결과')

    await act(async () => { pending[0].resolve(jsonResponse(200, [product(8, { productName: '오래된결과' })])) })

    expect(within(gridSection()).queryByText('오래된결과')).not.toBeInTheDocument()
    expect(within(gridSection()).getByText('최신결과')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-shop-products-025
  test('카드 클릭 → 상세 경로로 navigate하고, "최근 본 상품"(localStorage)은 건드리지 않는다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1)]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')

    fireEvent.click(within(gridSection()).getByText('테스트상품1'))

    await screen.findByText('상세(placeholder)')
    expect(localStorage.getItem('sl.shop.recentlyViewed')).toBeNull()
  })
})
