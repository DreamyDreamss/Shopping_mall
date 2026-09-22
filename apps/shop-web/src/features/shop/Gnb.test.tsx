/** @jest-environment jsdom */
// SR-311 round3 재작업 지시 1·3 — 검색 아이콘이 searchValue를 보고 분기하는지 실물 검증(round2 QA
// round2 권고 3: Gnb.test.tsx 자체가 없었다). 이동 여부는 useLocation을 보여주는 프로브 컴포넌트로
// 확인하고(Gnb는 <Routes> 밖에서도 useNavigate/useLocation을 쓸 수 있어 MemoryRouter만으로 충분하다),
// 목록 화면 안에서의 "검색어 없음" 클릭은 onSearchSubmit 스파이 호출로 확인한다(재작업 지시 1의
// "URL keyword와 표시 상태 일치" 요건 — 목록 화면 안에서는 이동 대신 그 페이지의 기존 제출 채널로
// 상태를 정리한다).
// SR-311 round4 재작업 지시 2(round3 QA FAIL 필수수정 2) — round3의 이 파일은 `Gnb`를 <Routes> **밖**에
// 두어 navigate가 호출돼도 같은 Gnb 인스턴스가 살아남는 구조였다. 그래서 클릭 직후 `toHaveFocus()`가
// 통과했지만, 운영에서는 라우트마다 별도 Gnb 인스턴스가 마운트돼(App.tsx) 그 포커스가 실제로는 전혀
// 동작하지 않았다(거짓 보증, 사례집 SR-302 #1·SR-306 #2). round4는 Gnb에서 클릭 시점 focus() 자체를
// 없앴으므로(Gnb.tsx) 이 파일은 이제 **이동 여부/state 페이로드**와 **같은 화면 안에서의 제출 위임**만
// 검증한다. "도착 화면에서 실제로 포커스되는지"는 이 파일이 아니라 App의 실제 라우트 쌍(ShopHomePage→
// ProductListPage)을 그대로 쓰는 `SearchFocusHandoff.test.tsx`가 근거다(Gnb를 라우트 밖에 두는 테스트를
// 그 요건의 근거로 쓰지 않는다, 사람 코멘트 원문).
import '@testing-library/jest-dom/jest-globals'
import type { ComponentProps } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { describe, expect, jest, test } from '@jest/globals'
import { Gnb } from './Gnb'

function LocationProbe() {
  const location = useLocation()
  return (
    <div data-testid="location-probe" data-focus-search={String(Boolean((location.state as { focusSearch?: boolean } | null)?.focusSearch))}>
      {location.pathname}
    </div>
  )
}

function renderGnb(initialEntry: string, overrides: Partial<ComponentProps<typeof Gnb>> = {}) {
  return render(
    <MemoryRouter initialEntries={[initialEntry]}>
      <Gnb session={null} cartItemCount={0} searchValue="" onSearchChange={() => {}}
           onSearchSubmit={() => {}} onLogout={() => {}} {...overrides} />
      <LocationProbe />
    </MemoryRouter>,
  )
}

describe('Gnb — 검색 아이콘(round3 재작업 지시 1)', () => {
  test('검색어가 있으면 이동하지 않고 onSearchSubmit만 호출한다(Enter와 같은 결과)', () => {
    const onSearchSubmit = jest.fn()
    renderGnb('/shop', { searchValue: '가방', onSearchSubmit })
    fireEvent.click(screen.getByRole('button', { name: '검색 화면으로 이동' }))
    expect(onSearchSubmit).toHaveBeenCalledTimes(1)
    expect(screen.getByTestId('location-probe')).toHaveTextContent('/shop')
  })

  test('검색어가 없고 목록 화면 밖이면 /shop/products로 이동하며 focusSearch state를 싣는다(포커스 자체는 이 Gnb 인스턴스가 걸지 않는다)', () => {
    const onSearchSubmit = jest.fn()
    renderGnb('/shop', { searchValue: '', onSearchSubmit })
    fireEvent.click(screen.getByRole('button', { name: '검색 화면으로 이동' }))
    expect(onSearchSubmit).not.toHaveBeenCalled()
    const probe = screen.getByTestId('location-probe')
    expect(probe).toHaveTextContent('/shop/products')
    expect(probe).toHaveAttribute('data-focus-search', 'true')
    // round4 재작업 지시 2 — Gnb는 클릭 시점에 스스로 focus()를 걸지 않는다(그 책임은 도착 화면이
    // autoFocusSearch prop으로 내려줄 때만 발동하는 마운트 이펙트 몫). 이 렌더는 autoFocusSearch를
    // 전달하지 않았으므로(=아무도 아직 소비하지 않은 state) 입력은 포커스되지 않는다.
    expect(screen.getByLabelText('상품 검색')).not.toHaveFocus()
  })

  test('검색어가 없고 이미 목록 화면 안이면 이동 대신 onSearchSubmit을 호출해 URL·표시 상태를 맞춘다', () => {
    const onSearchSubmit = jest.fn()
    renderGnb('/shop/products', { searchValue: '', onSearchSubmit })
    fireEvent.click(screen.getByRole('button', { name: '검색 화면으로 이동' }))
    expect(onSearchSubmit).toHaveBeenCalledTimes(1)
    expect(screen.getByTestId('location-probe')).toHaveTextContent('/shop/products')
  })

  test('autoFocusSearch=true로 마운트되면 이 인스턴스 자신의 검색 입력에 포커스한다(도착 화면이 내려주는 신호를 Gnb가 실제로 소비하는지)', () => {
    renderGnb('/shop/products', { autoFocusSearch: true })
    expect(screen.getByLabelText('상품 검색')).toHaveFocus()
  })
})
