/** @jest-environment jsdom */
// SR-311 round4 재작업 지시 2(round3 QA FAIL 필수수정 2) — 검색 아이콘의 "이동 뒤 포커스"가 실제
// 운영 라우트 구조에서도 동작하는지 검증한다. round3의 `Gnb.test.tsx`는 `Gnb`를 `<Routes>` **밖**에
// 두어 navigate가 일어나도 같은 Gnb 인스턴스가 살아남는 구조였다 — 그래서 클릭 직후 `toHaveFocus()`가
// 통과했지만, 운영에서는 `App.tsx`처럼 라우트마다 별도 페이지가 자기 `AppShell`→`Gnb`를 새로 마운트해
// 이전 인스턴스는 언마운트된다(QA가 임시 재현 테스트로 실측: 클릭 후 `document.activeElement`는
// BODY였다). 이 파일은 그 실제 구조 — 출발 화면(`ShopHomePage`, `#/shop`)과 도착 화면
// (`ProductListPage`, `#/shop/products`)을 `App.tsx`와 동일한 경로로 매핑한 `<Routes>` 아래 실제로
// 렌더해, 아이콘 클릭 → **도착 화면의** 검색 입력이 `document.activeElement`인지 단언한다(사람 코멘트
// 원문 — "Gnb를 라우트 밖에 두는 테스트는 이 요건의 근거로 쓰지 않는다").
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import ShopHomePage from './ShopHomePage'
import ProductListPage from './ProductListPage'

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

describe('검색 아이콘 → 도착 화면 포커스 인계(SR-311 round4 재작업 지시 2)', () => {
  beforeEach(() => {
    // 이 시나리오는 비로그인·상품 응답 유무만 관심사라 두 화면(ShopHomePage/ProductListPage)이
    // 각자 부르는 GET /api/products를 전부 빈 목록으로 받아 둔다(/api/cart는 세션이 없어 호출되지
    // 않는다).
    global.fetch = jest.fn(async () => jsonResponse(200, [])) as unknown as typeof fetch
    localStorage.clear() // 최근본상품 키가 다음 테스트로 새지 않게 한다(STORY "테스트 격리" 절).
  })

  afterEach(() => {
    localStorage.clear()
  })

  function renderApp() {
    // App.tsx의 실제 라우트 정의 중 이 시나리오에 필요한 두 경로(/shop, /shop/products)만 그대로
    // 옮긴다 — HashRouter 대신 MemoryRouter로 초기 경로를 직접 지정할 뿐, 매핑되는 컴포넌트와 경로
    // 문자열은 App.tsx와 동일하다.
    return render(
      <MemoryRouter initialEntries={['/shop']}>
        <Routes>
          <Route path="/shop" element={<ShopHomePage />} />
          <Route path="/shop/products" element={<ProductListPage />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  test('#/shop에서 빈 검색어로 아이콘을 누르면 /shop/products로 이동하고, 그 화면의 검색 입력이 document.activeElement가 된다', async () => {
    renderApp()
    await screen.findByRole('link', { name: 'SL Shop' }) // 출발 화면(ShopHomePage) 마운트 확인

    fireEvent.click(screen.getByRole('button', { name: '검색 화면으로 이동' }))

    // 도착 화면(ProductListPage) 마운트 확인 — 이 시점의 Gnb는 ShopHomePage의 것과 다른 새 인스턴스다.
    await screen.findByRole('heading', { name: '상품 목록' })
    expect(screen.getByLabelText('상품 검색')).toHaveFocus()
  })
})
