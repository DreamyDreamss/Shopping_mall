/** @jest-environment jsdom */
// SR-310 — BottomSheet 포커스 트랩·Esc 닫기·닫힌 뒤 포커스 복귀. React 19 StrictMode 이중
// 이펙트에서도 트리거 포착이 어긋나지 않는지 실제로 행사하기 위해 <StrictMode>로 감싸 렌더한다.
import '@testing-library/jest-dom/jest-globals'
import { StrictMode } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { BottomSheet } from './BottomSheet'

describe('BottomSheet', () => {
  test('열리면 첫 포커스 가능 요소(닫기 버튼)로 포커스가 이동한다', () => {
    render(
      <StrictMode>
        <BottomSheet open onClose={jest.fn()}>
          <button type="button">확인</button>
        </BottomSheet>
      </StrictMode>,
    )
    expect(screen.getByRole('button', { name: '닫기' })).toHaveFocus()
  })

  test('Esc 키를 누르면 onClose가 호출된다', () => {
    const onClose = jest.fn()
    render(
      <StrictMode>
        <BottomSheet open onClose={onClose}>
          <button type="button">확인</button>
        </BottomSheet>
      </StrictMode>,
    )
    fireEvent.keyDown(document, { key: 'Escape' })
    expect(onClose).toHaveBeenCalledTimes(1)
  })

  test('닫힌 뒤 트리거 요소로 포커스가 복귀한다', () => {
    const trigger = document.createElement('button')
    trigger.textContent = '외부 트리거'
    document.body.appendChild(trigger)
    trigger.focus()
    expect(trigger).toHaveFocus() // 준비 확인

    const { rerender } = render(
      <StrictMode>
        <BottomSheet open onClose={jest.fn()}>
          <button type="button">확인</button>
        </BottomSheet>
      </StrictMode>,
    )
    expect(screen.getByRole('button', { name: '닫기' })).toHaveFocus()

    rerender(
      <StrictMode>
        <BottomSheet open={false} onClose={jest.fn()}>
          <button type="button">확인</button>
        </BottomSheet>
      </StrictMode>,
    )
    expect(trigger).toHaveFocus()

    document.body.removeChild(trigger)
  })

  test('열린 채로 부모가 재렌더돼도(인라인 onClose 신원 변경) 내부 포커스가 유지된다', () => {
    // round1 QA 재작업 — 임시 RTL 프로브의 재현 절차를 정식 회귀 테스트로 옮긴다: 시트 안 '확인'
    // 버튼에 포커스를 준 뒤 onClose만 새 인라인 화살표로 바꿔 rerender해도(부모 재렌더 시늉),
    // useFocusTrap의 초기 포커스 이동 effect가 다시 돌아 포커스를 '닫기' 버튼으로 되돌리면 안 된다.
    const { rerender } = render(
      <StrictMode>
        <BottomSheet open onClose={() => {}}>
          <button type="button">확인</button>
        </BottomSheet>
      </StrictMode>,
    )
    const confirmButton = screen.getByRole('button', { name: '확인' })
    confirmButton.focus()
    expect(confirmButton).toHaveFocus()

    rerender(
      <StrictMode>
        <BottomSheet open onClose={() => {}}>
          <button type="button">확인</button>
        </BottomSheet>
      </StrictMode>,
    )
    expect(confirmButton).toHaveFocus()
  })

  test('open=false면 아무것도 렌더하지 않는다', () => {
    render(
      <StrictMode>
        <BottomSheet open={false} onClose={jest.fn()}>
          <button type="button">확인</button>
        </BottomSheet>
      </StrictMode>,
    )
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument()
  })
})
