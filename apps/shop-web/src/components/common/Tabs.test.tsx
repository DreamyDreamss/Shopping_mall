/** @jest-environment jsdom */
// SR-310 — Tabs 스와이프 전환(touchStart/Move/End)·스티키 클래스.
import '@testing-library/jest-dom/jest-globals'
import { fireEvent, render, screen } from '@testing-library/react'
import { describe, expect, jest, test } from '@jest/globals'
import { Tabs } from './Tabs'

const tabs = [
  { key: 'a', label: '탭A' },
  { key: 'b', label: '탭B' },
  { key: 'c', label: '탭C' },
]

function touchAt(x: number) {
  return { touches: [{ clientX: x }], changedTouches: [{ clientX: x }] }
}

describe('Tabs', () => {
  test('왼쪽으로 스와이프(임계값 이상)하면 다음 탭으로 전환된다', () => {
    const onChange = jest.fn()
    render(<Tabs tabs={tabs} activeKey="a" onChange={onChange} />)
    const tablist = screen.getByRole('tablist')
    fireEvent.touchStart(tablist, touchAt(200))
    fireEvent.touchEnd(tablist, touchAt(100))
    expect(onChange).toHaveBeenCalledWith('b')
  })

  test('오른쪽으로 스와이프하면 이전 탭으로 전환된다', () => {
    const onChange = jest.fn()
    render(<Tabs tabs={tabs} activeKey="b" onChange={onChange} />)
    const tablist = screen.getByRole('tablist')
    fireEvent.touchStart(tablist, touchAt(100))
    fireEvent.touchEnd(tablist, touchAt(200))
    expect(onChange).toHaveBeenCalledWith('a')
  })

  test('임계값 미만 이동은 탭을 바꾸지 않는다', () => {
    const onChange = jest.fn()
    render(<Tabs tabs={tabs} activeKey="a" onChange={onChange} />)
    const tablist = screen.getByRole('tablist')
    fireEvent.touchStart(tablist, touchAt(100))
    fireEvent.touchEnd(tablist, touchAt(110))
    expect(onChange).not.toHaveBeenCalled()
  })

  test('sticky prop이면 스티키 클래스를 붙인다', () => {
    render(<Tabs tabs={tabs} activeKey="a" onChange={jest.fn()} sticky />)
    expect(screen.getByRole('tablist')).toHaveClass('cmn-tabs--sticky')
  })

  test('기본은 스티키 클래스가 없다', () => {
    render(<Tabs tabs={tabs} activeKey="a" onChange={jest.fn()} />)
    expect(screen.getByRole('tablist')).not.toHaveClass('cmn-tabs--sticky')
  })

  test('클릭으로도 탭을 바꿀 수 있다', () => {
    const onChange = jest.fn()
    render(<Tabs tabs={tabs} activeKey="a" onChange={onChange} />)
    fireEvent.click(screen.getByRole('tab', { name: '탭C' }))
    expect(onChange).toHaveBeenCalledWith('c')
  })

  // SR-311 round2 재작업 지시 3 — badge는 탭 버튼 안쪽에서 그려지므로 활성 여부·노출 탭 수와 무관하게
  // 항상 해당 탭에만 붙는다(이전 오버레이 방식의 정렬 어긋남 재발 방지).
  test('badge:true 탭은 라벨 위에 점 마커를 그리고, 다른 탭에는 그리지 않는다', () => {
    const tabsWithBadge = [
      { key: 'a', label: '탭A', badge: true },
      { key: 'b', label: '탭B' },
    ]
    render(<Tabs tabs={tabsWithBadge} activeKey="a" onChange={jest.fn()} />)
    expect(screen.getByRole('tab', { name: '탭A' }).querySelector('.cmn-tabs__badge-dot')).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '탭B' }).querySelector('.cmn-tabs__badge-dot')).not.toBeInTheDocument()
  })
})
