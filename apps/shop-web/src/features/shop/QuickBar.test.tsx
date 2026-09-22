/** @jest-environment jsdom */
// SR-311 round3 재작업 지시 3 — round2 재작업 지시 7("로그인 리다이렉트가 pathname+search를 함께 싣는다",
// `QuickBar.tsx:39`)이 가드만 들어가고 실물 검증이 없었다(round2 QA round2 권고 3). 쿼리스트링이 있는
// 경로에서 비로그인 상태로 '마이'를 눌러 redirect 파라미터에 pathname+search가 그대로(인코딩된 채로)
// 담기는지 확인한다 — pathname만 실으면 `?keyword=가방` 같은 검색 조건이 로그인 후 사라진다(round2 권고 7).
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { describe, expect, test } from '@jest/globals'
import { QuickBar } from './QuickBar'

function LocationProbe() {
  const location = useLocation()
  return <div data-testid="location-probe">{location.pathname}{location.search}</div>
}

describe('QuickBar — 로그인 리다이렉트 쿼리스트링 보존(round3 재작업 지시 3)', () => {
  test('비로그인 상태에서 "마이"를 누르면 redirect 파라미터에 pathname+search가 그대로 담긴다', () => {
    render(
      <MemoryRouter initialEntries={['/shop/products?keyword=가방']}>
        <QuickBar session={null} />
        <LocationProbe />
      </MemoryRouter>,
    )
    fireEvent.click(screen.getByRole('button', { name: /마이/ }))
    const [pathname, search] = (screen.getByTestId('location-probe').textContent ?? '').split('?')
    expect(pathname).toBe('/login')
    expect(new URLSearchParams(search).get('redirect')).toBe('/shop/products?keyword=가방')
  })
})
