/** @jest-environment jsdom */
// SR-310 round1 QA 재작업(low, 함께) — Skeleton 최소 RTL 테스트(렌더 확인). 콜백이 없는 순수
// 표시 컴포넌트라 카드·리스트 두 variant 모두 role="status"로 렌더되는지만 확인한다.
import '@testing-library/jest-dom/jest-globals'
import { render, screen } from '@testing-library/react'
import { describe, expect, test } from '@jest/globals'
import { Skeleton } from './Skeleton'

describe('Skeleton', () => {
  test('variant=card를 렌더한다', () => {
    render(<Skeleton variant="card" />)
    expect(screen.getByRole('status', { name: '불러오는 중' })).toBeInTheDocument()
  })

  test('variant=list를 렌더한다', () => {
    render(<Skeleton variant="list" />)
    expect(screen.getByRole('status', { name: '불러오는 중' })).toBeInTheDocument()
  })
})
