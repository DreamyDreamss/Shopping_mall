/** @jest-environment jsdom */
// SR-310 round1 QA 재작업(low, 함께) — Button 최소 RTL 테스트(렌더 확인·버튼 콜백).
// linked_tc: TC-FUNC-common-001
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { Button } from './Button'

describe('Button', () => {
  test('primary variant를 렌더한다', () => {
    render(<Button variant="primary">주 버튼</Button>)
    const btn = screen.getByRole('button', { name: '주 버튼' })
    expect(btn).toHaveClass('cmn-btn--primary')
  })

  test('secondary variant를 렌더한다', () => {
    render(<Button variant="secondary">보조 버튼</Button>)
    const btn = screen.getByRole('button', { name: '보조 버튼' })
    expect(btn).toHaveClass('cmn-btn--secondary')
  })

  test('purchase variant를 렌더한다', () => {
    render(<Button variant="purchase">구매 버튼</Button>)
    const btn = screen.getByRole('button', { name: '구매 버튼' })
    expect(btn).toHaveClass('cmn-btn--purchase')
  })

  test('loading=true이면 라벨이 "처리 중…"으로 대체되고 disabled가 적용된다', () => {
    render(<Button loading>저장하기</Button>)
    const btn = screen.getByRole('button')
    expect(btn).toHaveTextContent('처리 중…')
    expect(btn).toBeDisabled()
    expect(btn).toHaveAttribute('aria-busy', 'true')
  })

  test('disabled=true인 경우 버튼이 비활성화된다', () => {
    render(<Button disabled>비활성</Button>)
    const btn = screen.getByRole('button')
    expect(btn).toBeDisabled()
  })

  test('onClick 핸들러가 호출된다', () => {
    const onClick = jest.fn()
    render(<Button onClick={onClick}>클릭</Button>)
    fireEvent.click(screen.getByRole('button'))
    expect(onClick).toHaveBeenCalledTimes(1)
  })
})
