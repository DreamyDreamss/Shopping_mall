/** @jest-environment jsdom */
// SR-310 round1 QA 재작업(low) — ToastStack이 호출부가 상한을 지키지 않고 toasts를 4개 이상
// 넘겨도 스스로 최대 max(기본 3)개만 렌더하는지 확인한다(capToasts 방어적 적용).
import '@testing-library/jest-dom/jest-globals'
import { render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { ToastStack } from './ToastStack'
import type { ToastItem } from './toastQueue'

function toast(id: string): ToastItem {
  return { id, message: `메시지-${id}` }
}

describe('ToastStack', () => {
  test('toasts가 3개 이하면 전부 렌더한다', () => {
    render(<ToastStack toasts={[toast('1'), toast('2')]} onDismiss={jest.fn()} />)
    expect(screen.getAllByRole('status')).toHaveLength(2)
  })

  test('toasts가 5개여도 최신 3개만 렌더한다(호출부가 상한을 안 지켜도 자체 방어)', () => {
    render(
      <ToastStack
        toasts={[toast('1'), toast('2'), toast('3'), toast('4'), toast('5')]}
        onDismiss={jest.fn()}
      />,
    )
    expect(screen.getAllByRole('status')).toHaveLength(3)
    expect(screen.getByText('메시지-3')).toBeInTheDocument()
    expect(screen.getByText('메시지-4')).toBeInTheDocument()
    expect(screen.getByText('메시지-5')).toBeInTheDocument()
    expect(screen.queryByText('메시지-1')).not.toBeInTheDocument()
  })

  test('max prop으로 상한을 바꿀 수 있다', () => {
    render(
      <ToastStack toasts={[toast('1'), toast('2'), toast('3')]} onDismiss={jest.fn()} max={1} />,
    )
    expect(screen.getAllByRole('status')).toHaveLength(1)
    expect(screen.getByText('메시지-3')).toBeInTheDocument()
  })
})
