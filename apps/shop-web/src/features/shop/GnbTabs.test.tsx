/** @jest-environment jsdom */
// SR-311 round2 재작업 지시 6 — 현재 경로와 일치하는 GNB 탭이 없으면 어떤 탭도 강조(aria-selected)되지
// 않아야 한다(이전엔 `?? visibleTabs[0]`로 '홈'이 임의로 선택 처리됐다). 실제 운영 설정값
// (`GNB_TAB_CONFIG` — 지금은 '홈'만 `implemented:true`)을 그대로 쓴다. 배지(`badge`) 렌더 자체의
// 실물 검증은 그 기능이 실제로 그려지는 `Tabs`(`components/common/Tabs.test.tsx`·`.stories.tsx`)
// 쪽에서 한다 — GnbTabs는 config의 `badge` 값을 그대로 전달만 하는 얇은 배선이다.
import '@testing-library/jest-dom/jest-globals'
import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, test } from '@jest/globals'
import { GnbTabs } from './GnbTabs'

describe('GnbTabs', () => {
  test('현재 경로가 탭 경로와 일치하면 그 탭만 선택 표시된다', () => {
    render(<MemoryRouter initialEntries={['/shop']}><GnbTabs /></MemoryRouter>)
    expect(screen.getByRole('tab', { name: '홈' })).toHaveAttribute('aria-selected', 'true')
  })

  test('현재 경로와 일치하는 탭이 없으면 어떤 탭도 선택 표시되지 않는다(임의로 첫 탭을 고르지 않는다)', () => {
    render(<MemoryRouter initialEntries={['/shop/cart']}><GnbTabs /></MemoryRouter>)
    expect(screen.getByRole('tab', { name: '홈' })).toHaveAttribute('aria-selected', 'false')
  })
})
