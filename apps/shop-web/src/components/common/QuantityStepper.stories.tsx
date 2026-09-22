// SR-310 — QuantityStepper 상태별(중간값·최솟값·최댓값·직접입력 보정) 스토리.
import { useState } from 'react'
import type { Meta, StoryObj } from '@storybook/react-vite'
import { QuantityStepper } from './QuantityStepper'

const meta = {
  title: '공통/QuantityStepper',
  component: QuantityStepper,
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof QuantityStepper>
export default meta

type Story = StoryObj<typeof meta>

export const 중간값: Story = { args: { value: 5, min: 1, max: 10, onChange: () => {} } }
export const 최솟값: Story = { args: { value: 1, min: 1, max: 10, onChange: () => {} } }
export const 최댓값: Story = { args: { value: 10, min: 1, max: 10, onChange: () => {} } }

/** 직접입력 보정 — 입력값을 범위 밖으로 바꾸고 포커스를 벗어나면(blur) 경계로 스스로 보정된다. */
export const 직접입력보정: Story = {
  args: { value: 5, min: 1, max: 10, onChange: () => {} }, // render가 실제 상태를 관리해 무시된다(타입 요건 충족용)
  render: () => {
    function Demo() {
      const [value, setValue] = useState(5)
      return <QuantityStepper value={value} min={1} max={10} onChange={setValue} />
    }
    return <Demo />
  },
}
