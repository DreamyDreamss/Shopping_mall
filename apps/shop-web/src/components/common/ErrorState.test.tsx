/** @jest-environment jsdom */
// SR-310 round1 QA 재작업(low, 함께) — ErrorState 최소 RTL 테스트(렌더 확인·버튼 콜백 호출).
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { ErrorState } from './ErrorState'

describe('ErrorState', () => {
  test('고정 카피와 보조 문구·버튼 2종을 렌더한다', () => {
    render(<ErrorState subMessage="요청하신 페이지를 찾을 수 없습니다" onHome={jest.fn()} onBack={jest.fn()} />)
    expect(screen.getByText('이용에 불편을 드려 죄송합니다.')).toBeInTheDocument()
    expect(screen.getByText('요청하신 페이지를 찾을 수 없습니다')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '홈으로' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '돌아가기' })).toBeInTheDocument()
  })

  test('홈으로 버튼을 누르면 onHome이 호출된다', () => {
    const onHome = jest.fn()
    render(<ErrorState subMessage="오류" onHome={onHome} onBack={jest.fn()} />)
    fireEvent.click(screen.getByRole('button', { name: '홈으로' }))
    expect(onHome).toHaveBeenCalledTimes(1)
  })

  test('돌아가기 버튼을 누르면 onBack이 호출된다', () => {
    const onBack = jest.fn()
    render(<ErrorState subMessage="오류" onHome={jest.fn()} onBack={onBack} />)
    fireEvent.click(screen.getByRole('button', { name: '돌아가기' }))
    expect(onBack).toHaveBeenCalledTimes(1)
  })
})
