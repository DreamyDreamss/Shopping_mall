/** @jest-environment jsdom */
// SR-311 round3 재작업 지시 3 — `QuickBar.test.tsx`와 동일 사유·동일 가드(`BottomTabBar.tsx:33`)의
// 실물 검증. round2 재작업 지시 7이 두 컴포넌트에 같은 패턴으로 들어갔으므로 테스트도 짝을 맞춘다.
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, useLocation } from 'react-router-dom'
import { describe, expect, test } from '@jest/globals'
import { BottomTabBar } from './BottomTabBar'

function LocationProbe() {
  const location = useLocation()
  return <div data-testid="location-probe">{location.pathname}{location.search}</div>
}

describe('BottomTabBar — 로그인 리다이렉트 쿼리스트링 보존(round3 재작업 지시 3)', () => {
  test('비로그인 상태에서 "마이"를 누르면 redirect 파라미터에 pathname+search가 그대로 담긴다', () => {
    render(
      <MemoryRouter initialEntries={['/shop/products?keyword=가방']}>
        <BottomTabBar session={null} />
        <LocationProbe />
      </MemoryRouter>,
    )
    fireEvent.click(screen.getByRole('button', { name: /마이/ }))
    const [pathname, search] = (screen.getByTestId('location-probe').textContent ?? '').split('?')
    expect(pathname).toBe('/login')
    expect(new URLSearchParams(search).get('redirect')).toBe('/shop/products?keyword=가방')
  })
})
