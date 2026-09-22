/** @jest-environment jsdom */
// SR-302 — 쇼핑 홈(메인) 통합 테스트. 이 프로젝트 테스트 관례(`PasswordResetPage.test.tsx`)를 따라
// `@jest/globals`의 `expect`를 쓴다.
import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useSearchParams } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import ShopHomePage from './ShopHomePage'
import { saveSession } from '../session'
import { recordViewed } from '../features/shop/recentlyViewedStorage'
import type { SessionResult } from '../types'

// 재작업(round 1 QA CONCERNS carry-back, 2026-09-17) — 검색 제출·카테고리 클릭이 실제로
// `/shop/products`로 이동하며 검색어를 `?keyword=`로 넘기는지 확인하는 캐치올 placeholder(실제
// `ProductListPage` 대신). 목록 화면 자체의 검증(초기 검색어가 입력창에 반영되는지 등)은
// `ProductListPage.test.tsx` 몫이다 — 이 파일은 `ShopHomePage`의 네비게이션 트리거만 검증한다.
function ProductsPlaceholder() {
  const [params] = useSearchParams()
  return <div>{`상품목록(placeholder) keyword=${params.get('keyword') ?? ''}`}</div>
}

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

interface ProductStub {
  sku: string; productName: string; price: number; stockQty: number; saleYn: string
  listPrice?: number | null; imageUrl?: string | null
}

function product(i: number, overrides: Partial<ProductStub> = {}): ProductStub {
  return {
    sku: `sku-${i}`, productName: `테스트상품${i}`, price: 10000 + i * 1000, stockQty: 5, saleYn: 'Y',
    listPrice: null, imageUrl: null, ...overrides,
  }
}

const session: SessionResult = {
  memberId: 'm-1', memberName: '홍길동', grade: 'NORMAL',
  apiKey: 'k-1', refreshToken: 'r-1', refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
}

describe('ShopHomePage', () => {
  let fetchMock: jest.Mock<typeof fetch>

  beforeEach(() => {
    fetchMock = jest.fn()
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear() // 세션 키 + 최근본상품 키 둘 다 지워 테스트 간 상태가 새지 않게 한다.
  })

  afterEach(() => {
    localStorage.clear()
  })

  /** URL로 분기하는 fetch 라우터 — 상품 조회와 장바구니 조회가 마운트 시 함께 나갈 수 있어
   * 순서에 의존하는 mockResolvedValueOnce 체인 대신 URL 기준으로 응답을 맞춘다. */
  function routeFetch(handlers: {
    products?: () => Response | Promise<Response>
    cart?: () => Response | Promise<Response>
    logout?: (init?: RequestInit) => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      if (url.startsWith('/api/products')) {
        if (!handlers.products) throw new Error('unexpected /api/products call')
        return handlers.products()
      }
      if (url.startsWith('/api/cart')) {
        if (!handlers.cart) throw new Error('unexpected /api/cart call')
        return handlers.cart()
      }
      if (url.startsWith('/api/members/sessions/logout')) {
        if (!handlers.logout) throw new Error('unexpected logout call')
        return handlers.logout(init)
      }
      throw new Error('unexpected url ' + url)
    })
  }

  function renderPage() {
    return render(
      <MemoryRouter initialEntries={['/']}>
        <Routes>
          <Route path="/" element={<ShopHomePage />} />
          <Route path="/shop/products" element={<ProductsPlaceholder />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  const gridSection = () => screen.getByRole('region', { name: '추천 상품' })
  const recentSection = () => screen.getByRole('region', { name: '최근 본 상품' })

  test('로딩 후 상품 그리드가 렌더된다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1), product(2), product(3)]) })
    renderPage()
    expect(screen.getByText('불러오는 중…')).toBeInTheDocument()
    await within(gridSection()).findByText('테스트상품1')
    expect(within(gridSection()).getByText('테스트상품2')).toBeInTheDocument()
  })

  test('상품 0건이면 "표시할 상품이 없습니다" + 카테고리 안내를 보인다', async () => {
    routeFetch({ products: () => jsonResponse(200, []) })
    renderPage()
    await within(gridSection()).findByText('표시할 상품이 없습니다')
    expect(within(gridSection()).getByText(/카테고리/)).toBeInTheDocument()
  })

  test('조회 실패는 오류문구+[다시 시도]를 보이고, 클릭 시 재호출한다', async () => {
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

  test('비로그인 GNB는 로그인 링크와 장바구니 수량 0을 보인다', async () => {
    routeFetch({ products: () => jsonResponse(200, []) })
    renderPage()
    await within(gridSection()).findByText('표시할 상품이 없습니다')
    expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument()
    expect(screen.getByLabelText('장바구니 0개')).toBeInTheDocument()
  })

  test('로그인 상태 GNB는 회원명+로그아웃을 보이고, 장바구니 수량은 GET /api/cart 합산값이다', async () => {
    saveSession(session)
    routeFetch({
      products: () => jsonResponse(200, []),
      cart: () => jsonResponse(200, { items: [{ sku: 'a', qty: 2 }, { sku: 'b', qty: 3 }], totalAmount: 100 }),
    })
    renderPage()
    await within(gridSection()).findByText('표시할 상품이 없습니다')
    expect(screen.getByText('홍길동님')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument()
    await waitFor(() => expect(screen.getByLabelText('장바구니 5개')).toBeInTheDocument())
  })

  test('상품 카드 클릭 → localStorage에 기록되고 "최근 본 상품" 섹션이 없음에서 그 상품으로 바뀐다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1)]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')
    expect(within(recentSection()).getByText('최근 본 상품이 없습니다')).toBeInTheDocument()
    fireEvent.click(within(gridSection()).getByText('테스트상품1'))
    await within(recentSection()).findByText('테스트상품1')
    expect(within(recentSection()).queryByText('최근 본 상품이 없습니다')).not.toBeInTheDocument()
  })

  // --- 사람 수정(계획 확인 게이트, 2026-09-17) 추가 테스트 -----------------------------------

  test('정가·이미지가 없어도 ProductCard 레이아웃이 깨지지 않는다(이니셜 대체 영역 항상 렌더)', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1)]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')
    expect(within(gridSection()).getByLabelText('테스트상품1 대표이미지 없음')).toBeInTheDocument()
    expect(within(gridSection()).queryByRole('img')).not.toBeInTheDocument()
  })

  test('품절 상품은 그리드에 품절 배지로 표시된다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1, { stockQty: 0 })]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')
    expect(within(gridSection()).getByText('품절')).toBeInTheDocument()
  })

  test('최근 본 상품이 8개를 초과하면 가장 오래된 항목부터 제거된다', async () => {
    const rows = Array.from({ length: 9 }, (_, i) => product(i)) // sku-0..sku-8
    routeFetch({ products: () => jsonResponse(200, rows) })
    // sku-1..sku-8을 미리 기록(sku-1이 가장 오래됨, sku-8이 가장 최근) — 정확히 cap(8)만큼 채운다.
    for (let i = 1; i <= 8; i++) recordViewed(`sku-${i}`)

    renderPage()
    await within(gridSection()).findByText('테스트상품0') // 그리드 상위 8개(sku-0..sku-7)에 포함됨
    fireEvent.click(within(gridSection()).getByText('테스트상품0')) // 9번째 기록 → cap 초과

    await within(recentSection()).findByText('테스트상품0')
    expect(within(recentSection()).getByText('테스트상품8')).toBeInTheDocument() // 가장 최근이던 것 유지
    expect(within(recentSection()).queryByText('테스트상품1')).not.toBeInTheDocument() // 가장 오래된 것 제거
  })

  // --- 재작업(round 1 QA CONCERNS carry-back, 2026-09-17) 추가 테스트 ---------------------------

  test('로그아웃 클릭 시 logout API가 호출되고, 그 API가 실패해도 로컬 세션이 지워진다', async () => {
    saveSession(session)
    let logoutInit: RequestInit | undefined
    routeFetch({
      products: () => jsonResponse(200, []),
      cart: () => jsonResponse(200, { items: [] }),
      // 서버 폐기가 실패(500)해도 로그아웃 자체가 막히면 안 된다(사람 코멘트 2) — 로컬 세션 정리는
      // 이 호출의 성공 여부와 무관하게 이어져야 한다.
      logout: init => {
        logoutInit = init
        return jsonResponse(500, { code: 'MBR-5000', message: 'boom' }, false)
      },
    })
    renderPage()
    await within(gridSection()).findByText('표시할 상품이 없습니다')
    expect(screen.getByText('홍길동님')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '로그아웃' }))

    // 서버 폐기(POST /api/members/sessions/logout, X-Api-Key: 세션의 apiKey)가 먼저 나갔는지.
    await waitFor(() => expect(logoutInit).toBeDefined())
    expect(logoutInit?.method).toBe('POST')
    expect((logoutInit?.headers as Record<string, string>)['X-Api-Key']).toBe(session.apiKey)

    // 실패했어도(500) 로컬 세션은 정리되어 GNB가 비로그인 상태로 돌아간다.
    await waitFor(() => expect(screen.getByRole('link', { name: '로그인' })).toBeInTheDocument())
    expect(screen.queryByText('홍길동님')).not.toBeInTheDocument()
  })

  // round2 QA FAIL 필수 수정 2 — 종전 테스트는 `fireEvent.click`을 세 번 따로 불렀다. RTL의
  // `fireEvent`는 호출마다 `act()`로 감싸 렌더를 즉시 flush하는데, `ProductGrid`는 `error`를 최우선
  // 분기하므로 첫 클릭에서 `setError(null)+setLoading(true)`가 반영되는 순간 [다시 시도] 버튼이
  // DOM에서 통째로 사라진다(로딩 스피너로 교체) — 2·3번째 `fireEvent.click`은 이미 제거된 노드에
  // 발사돼 React 루트 위임 리스너에 닿지 않고 `onClick`이 아예 실행되지 않는다. 그 결과 `calls===2`가
  // `inFlightRef` 가드 없이도 성립해 "연타 시 1회만"이라는 완료 조건을 거짓으로 보증했다(round2 QA
  // FAIL 근거).
  //
  // 고친 방식: 네이티브 `.click()` 세 번을 **하나의 `act()` 안에서** flush 없이 연달아 호출한다.
  // `act()` 콜백이 끝나기 전까지는 리렌더가 커밋되지 않으므로, 버튼은 세 번의 동기 클릭이 모두
  // 디스패치되는 동안 DOM에 그대로 남아있다 — 이제야 실제로 "버튼이 살아있는 상태에서의 연타"를
  // 재현하고, `inFlightRef`(await 이전에 동기 세팅되는 ref)가 2·3번째 호출을 실제로 막는지 검증한다.
  //
  // 가드 제거 확인(사람 지시) — `ShopHomePage.tsx`의 `if (inFlightRef.current) return`을 임시로 지운
  // 채 이 테스트만 단독 실행해 실패를 직접 확인했다(Dev 기록 참조): 가드가 없으면 세 번의 동기 클릭이
  // 전부 `fetchProducts()`를 호출해 `calls`가 4(최초 1 + 연타 3)로 튀어 `expect(calls).toBe(2)`가
  // "Expected: 2 / Received: 4"로 실패했다. 가드를 복원하자 다시 통과했다.
  test('재시도 버튼 연타 시 요청이 한 번만 나간다(in-flight 가드)', async () => {
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

  // --- SR-306.1 추가 테스트 — 정가·할인율·이미지가 응답 필드에서 직접 파생돼 그려진다 ---------------

  // linked_tc: TC-FUNC-shop-001
  test('정가가 판매가보다 크면 취소선 정가와 내림 정수 할인율 배지가 정확한 텍스트로 렌더된다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1, { price: 390000, listPrice: 450000 })]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')
    expect(within(gridSection()).getByText('13%')).toBeInTheDocument()
    expect(within(gridSection()).getByText('450,000원')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-shop-002
  test('정가가 판매가와 같으면(할인 0%) 배지·취소선 둘 다 렌더되지 않는다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1, { price: 450000, listPrice: 450000 })]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')
    expect(within(gridSection()).queryByText(/%$/)).not.toBeInTheDocument()
    expect(within(gridSection()).queryByText('450,000원', { selector: 'div' })).not.toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-shop-003
  test('정가가 없으면(listPrice: null) 배지·취소선 둘 다 렌더되지 않는다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1, { listPrice: null })]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')
    expect(within(gridSection()).queryByText(/%$/)).not.toBeInTheDocument()
    expect(within(gridSection()).queryByText(/원$/, { selector: 'div' })).not.toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-shop-004
  test('imageUrl이 있으면 img가 렌더되고 이니셜 대체 영역은 없다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1, { imageUrl: '/images/products/sku-1001.svg' })]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')
    expect(within(gridSection()).getByRole('img')).toBeInTheDocument()
    expect(within(gridSection()).queryByLabelText('테스트상품1 대표이미지 없음')).not.toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-shop-005
  test('imageUrl이 있어도 이미지 로드가 실패하면 이니셜 대체 영역으로 전환되고 img는 사라진다', async () => {
    routeFetch({ products: () => jsonResponse(200, [product(1, { imageUrl: '/images/products/sku-1001.svg' })]) })
    renderPage()
    await within(gridSection()).findByText('테스트상품1')
    const img = within(gridSection()).getByRole('img')
    fireEvent.error(img)
    expect(within(gridSection()).getByLabelText('테스트상품1 대표이미지 없음')).toBeInTheDocument()
    expect(within(gridSection()).queryByRole('img')).not.toBeInTheDocument()
  })

  // --- 재작업(round 1 QA CONCERNS carry-back, 2026-09-17) 추가 테스트 — 진입 경로 신설 ------------

  // linked_tc: TC-FUNC-shop-products-026
  test('검색어 제출 시 /shop/products로 이동하며 검색어가 쿼리로 전달된다(재작업 지시 1)', async () => {
    routeFetch({ products: () => jsonResponse(200, []) })
    renderPage()
    await within(gridSection()).findByText('표시할 상품이 없습니다')

    const searchInput = screen.getByLabelText('상품 검색')
    fireEvent.change(searchInput, { target: { value: '운동화' } })
    fireEvent.submit(searchInput.closest('form')!)

    await screen.findByText('상품목록(placeholder) keyword=운동화')
  })

  // linked_tc: TC-FUNC-shop-products-027
  test('검색창을 비운 채 제출하면 무파라미터로 /shop/products에 도달한다', async () => {
    routeFetch({ products: () => jsonResponse(200, []) })
    renderPage()
    await within(gridSection()).findByText('표시할 상품이 없습니다')

    fireEvent.submit(screen.getByLabelText('상품 검색').closest('form')!)

    await screen.findByText('상품목록(placeholder) keyword=')
  })

  // round2 재작업 지시 1·2 — round1의 "라벨을 keyword로 넘긴다"는 시드 데이터와 라벨이 겹치지
  // 않아 항상 빈 결과였다(사람이 지시 결함을 인정하고 되돌림). keyword 없이 전체 목록으로 이동한다.
  // linked_tc: TC-FUNC-shop-products-028
  test('카테고리 숏컷 클릭 시 keyword 없이 /shop/products로 이동한다(전체 목록, round2 재작업 지시 1)', async () => {
    routeFetch({ products: () => jsonResponse(200, []) })
    renderPage()
    await within(gridSection()).findByText('표시할 상품이 없습니다')

    fireEvent.click(screen.getByText('패션'))

    await screen.findByText('상품목록(placeholder) keyword=')
  })
})
