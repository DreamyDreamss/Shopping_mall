/** @jest-environment jsdom */
// SR-310 round1 QA 재작업(low, 함께) — Toast 최소 RTL 테스트(렌더 확인·버튼 콜백 호출).
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { Toast } from './Toast'

describe('Toast', () => {
  test('message를 렌더한다', () => {
    render(<Toast message="장바구니에 담았습니다" onDismiss={jest.fn()} />)
    expect(screen.getByRole('status')).toHaveTextContent('장바구니에 담았습니다')
  })

  test('닫기 버튼을 누르면 onDismiss가 호출된다', () => {
    const onDismiss = jest.fn()
    render(<Toast message="알림" onDismiss={onDismiss} />)
    fireEvent.click(screen.getByRole('button', { name: '토스트 닫기' }))
    expect(onDismiss).toHaveBeenCalledTimes(1)
  })
})
