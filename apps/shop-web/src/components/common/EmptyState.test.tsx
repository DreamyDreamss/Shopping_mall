/** @jest-environment jsdom */
// SR-310 round1 QA 재작업(low, 함께) — EmptyState 최소 RTL 테스트(렌더 확인·버튼 콜백 호출).
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { EmptyState } from './EmptyState'

describe('EmptyState', () => {
  test('안내 문구와 액션 버튼을 렌더한다', () => {
    render(<EmptyState message="장바구니가 비어 있습니다" actionLabel="쇼핑 계속하기" onAction={jest.fn()} />)
    expect(screen.getByText('장바구니가 비어 있습니다')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '쇼핑 계속하기' })).toBeInTheDocument()
  })

  test('액션 버튼을 누르면 onAction이 호출된다', () => {
    const onAction = jest.fn()
    render(<EmptyState message="비어 있습니다" actionLabel="계속하기" onAction={onAction} />)
    fireEvent.click(screen.getByRole('button', { name: '계속하기' }))
    expect(onAction).toHaveBeenCalledTimes(1)
  })

  test('railSlot을 넘기면 함께 렌더한다', () => {
    render(
      <EmptyState message="비어 있습니다" actionLabel="계속하기" onAction={jest.fn()} railSlot={<div>추천 레일</div>} />,
    )
    expect(screen.getByText('추천 레일')).toBeInTheDocument()
  })
})
