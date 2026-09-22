/** @jest-environment jsdom */
// SR-310 — PopupCarousel 포커스 트랩·Esc 닫기·복귀 + "오늘은 그만 보기" 호출 순서(onClose 뒤
// onDismissToday를 별도로 — 둘을 하나로 묶지 않는다). <StrictMode>로 감싸 렌더해 React 19 이중
// 이펙트에서도 트리거 포착이 어긋나지 않는지 실제로 행사한다.
import '@testing-library/jest-dom/jest-globals'
import { StrictMode } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { PopupCarousel } from './PopupCarousel'

const slides = [
  { id: '1', background: '#fff' },
  { id: '2', background: '#000' },
]

describe('PopupCarousel', () => {
  test('열리면 첫 포커스 가능 요소("오늘은 그만 보기")로 포커스가 이동한다', () => {
    render(
      <StrictMode>
        <PopupCarousel open slides={slides} onClose={jest.fn()} onDismissToday={jest.fn()} />
      </StrictMode>,
    )
    expect(screen.getByRole('button', { name: '오늘은 그만 보기' })).toHaveFocus()
  })

  test('Esc 키를 누르면 onClose가 호출된다', () => {
    const onClose = jest.fn()
    render(
      <StrictMode>
        <PopupCarousel open slides={slides} onClose={onClose} onDismissToday={jest.fn()} />
      </StrictMode>,
    )
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  test('닫힌 뒤 트리거 요소로 포커스가 복귀한다', () => {
    const trigger = document.createElement('button')
    document.body.appendChild(trigger)
    trigger.focus()

    const { rerender } = render(
      <StrictMode>
        <PopupCarousel open slides={slides} onClose={jest.fn()} onDismissToday={jest.fn()} />
      </StrictMode>,
    )
    rerender(
      <StrictMode>
        <PopupCarousel open={false} slides={slides} onClose={jest.fn()} onDismissToday={jest.fn()} />
      </StrictMode>,
    )
    expect(trigger).toHaveFocus()

    document.body.removeChild(trigger)
  })

  test('오늘은 그만 보기 클릭 — onClose 다음에 onDismissToday를 별도로 호출한다', () => {
    const calls: string[] = []
    const onClose = jest.fn(() => calls.push('close'))
    const onDismissToday = jest.fn(() => calls.push('dismissToday'))
    render(
      <StrictMode>
        <PopupCarousel open slides={slides} onClose={onClose} onDismissToday={onDismissToday} />
      </StrictMode>,
    )
    fireEvent.click(screen.getByRole('button', { name: '오늘은 그만 보기' }))
    expect(calls).toEqual(['close', 'dismissToday'])
  })

  test('1/5·마지막 등 initialIndex로 시작 슬라이드를 지정할 수 있다', () => {
    render(
      <StrictMode>
        <PopupCarousel open slides={slides} initialIndex={1} onClose={jest.fn()} onDismissToday={jest.fn()} />
      </StrictMode>,
    )
    expect(screen.getByText('2 / 2')).toBeInTheDocument()
  })

  test('오늘은 그만 보기 클릭 후 체크 표시(aria-pressed)가 켜진다', () => {
    render(
      <StrictMode>
        <PopupCarousel open slides={slides} onClose={jest.fn()} onDismissToday={jest.fn()} />
      </StrictMode>,
    )
    const optionButton = screen.getByRole('button', { name: /오늘은 그만 보기/ })
    expect(optionButton).toHaveAttribute('aria-pressed', 'false')
    fireEvent.click(optionButton)
    expect(optionButton).toHaveAttribute('aria-pressed', 'true')
  })

  test('autoAdvanceMs=0이면 자동으로 슬라이드가 넘어가지 않는다', () => {
    jest.useFakeTimers()
    try {
      render(
        <StrictMode>
          <PopupCarousel open slides={slides} autoAdvanceMs={0} onClose={jest.fn()} onDismissToday={jest.fn()} />
        </StrictMode>,
      )
      expect(screen.getByText('1 / 2')).toBeInTheDocument()
      jest.advanceTimersByTime(10000)
      expect(screen.getByText('1 / 2')).toBeInTheDocument()
    } finally {
      jest.useRealTimers()
    }
  })
})
