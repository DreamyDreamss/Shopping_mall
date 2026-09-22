/** @jest-environment jsdom */
// SR-310 — QuantityStepper 경계 disabled·직접입력 보정 커밋.
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { QuantityStepper } from './QuantityStepper'

describe('QuantityStepper', () => {
  test('최솟값에서 감소 버튼이 비활성화된다', () => {
    render(<QuantityStepper value={1} min={1} max={10} onChange={jest.fn()} />)
    expect(screen.getByRole('button', { name: '수량 감소' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '수량 증가' })).not.toBeDisabled()
  })

  test('최댓값에서 증가 버튼이 비활성화된다', () => {
    render(<QuantityStepper value={10} min={1} max={10} onChange={jest.fn()} />)
    expect(screen.getByRole('button', { name: '수량 증가' })).toBeDisabled()
    expect(screen.getByRole('button', { name: '수량 감소' })).not.toBeDisabled()
  })

  test('직접입력 후 blur — 범위 밖 값은 경계로 보정해 onChange를 호출한다', () => {
    const onChange = jest.fn()
    render(<QuantityStepper value={5} min={1} max={10} onChange={onChange} />)
    const input = screen.getByRole('spinbutton', { name: '수량' })
    fireEvent.change(input, { target: { value: '999' } })
    fireEvent.blur(input)
    expect(onChange).toHaveBeenCalledWith(10)
  })

  test('직접입력 후 blur — 범위 안 값은 그대로 커밋된다', () => {
    const onChange = jest.fn()
    render(<QuantityStepper value={5} min={1} max={10} onChange={onChange} />)
    const input = screen.getByRole('spinbutton', { name: '수량' })
    fireEvent.change(input, { target: { value: '7' } })
    fireEvent.blur(input)
    expect(onChange).toHaveBeenCalledWith(7)
  })

  test('편집 없이 blur만 하면 onChange를 호출하지 않는다', () => {
    const onChange = jest.fn()
    render(<QuantityStepper value={5} min={1} max={10} onChange={onChange} />)
    const input = screen.getByRole('spinbutton', { name: '수량' })
    fireEvent.focus(input)
    fireEvent.blur(input)
    expect(onChange).not.toHaveBeenCalled()
  })

  test('+ 버튼 클릭은 즉시 1 증가로 커밋된다', () => {
    const onChange = jest.fn()
    render(<QuantityStepper value={5} min={1} max={10} onChange={onChange} />)
    fireEvent.click(screen.getByRole('button', { name: '수량 증가' }))
    expect(onChange).toHaveBeenCalledWith(6)
  })
})
