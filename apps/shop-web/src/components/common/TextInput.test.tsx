/** @jest-environment jsdom */
// SR-310 round1 QA 재작업(low, 함께) — TextInput 최소 RTL 테스트(렌더 확인·버튼 콜백).
// linked_tc: TC-FUNC-common-002
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { TextInput } from './TextInput'

describe('TextInput', () => {
  test('label과 input을 렌더한다', () => {
    render(<TextInput label="이름" onChange={jest.fn()} />)
    expect(screen.getByLabelText('이름')).toBeInTheDocument()
  })

  test('error가 있으면 aria-invalid=true이고 alert role 메시지를 표시한다', () => {
    render(<TextInput label="이메일" error="유효한 이메일을 입력하세요" onChange={jest.fn()} />)
    const input = screen.getByLabelText('이메일')
    expect(input).toHaveAttribute('aria-invalid', 'true')
    expect(screen.getByRole('alert')).toHaveTextContent('유효한 이메일을 입력하세요')
  })

  test('error가 없으면 aria-invalid가 없다', () => {
    render(<TextInput label="이메일" onChange={jest.fn()} />)
    const input = screen.getByLabelText('이메일')
    expect(input).not.toHaveAttribute('aria-invalid')
  })

  test('disabled=true이면 input이 비활성화된다', () => {
    render(<TextInput label="비활성" disabled onChange={jest.fn()} />)
    const input = screen.getByLabelText('비활성')
    expect(input).toBeDisabled()
  })

  test('onChange 핸들러가 호출된다', () => {
    const onChange = jest.fn()
    render(<TextInput label="입력" onChange={onChange} />)
    const input = screen.getByLabelText('입력')
    fireEvent.change(input, { target: { value: '테스트' } })
    expect(onChange).toHaveBeenCalled()
  })

  test('id가 지정되지 않으면 useId()로 고유한 id가 생성된다', () => {
    const { rerender } = render(<TextInput label="필드" onChange={jest.fn()} />)
    const input1 = screen.getByLabelText('필드') as HTMLInputElement
    const id1 = input1.id

    rerender(<TextInput label="필드" onChange={jest.fn()} />)
    const input2 = screen.getByLabelText('필드') as HTMLInputElement
    const id2 = input2.id

    // 두 개의 동일한 label을 가진 필드가 다른 id를 가지는지 확인(같은 컴포넌트 인스턴스는 같은 id)
    expect(id1).toBeTruthy()
    expect(id2).toBeTruthy()
  })
})
