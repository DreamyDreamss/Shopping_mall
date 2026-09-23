// SR-310 — TextInput 상태별(기본·포커스·오류·비활성) 스토리.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { within } from 'storybook/test'
import { TextInput } from './TextInput'

const meta = {
  title: '공통/TextInput',
  component: TextInput,
  args: { label: '이름', value: '', onChange: () => {} },
  tags: ['UIS-CMN-002'],
} satisfies Meta<typeof TextInput>
export default meta

type Story = StoryObj<typeof meta>

export const 기본: Story = { tags: ['state:기본'],}

export const 포커스: Story = {
  play: ({ canvasElement }) => {
    const canvas = within(canvasElement)
    canvas.getByLabelText('이름').focus()
  },
}

export const 오류: Story = { tags: ['state:오류'], args: { error: '이름을 입력해 주세요' } }

export const 비활성: Story = { args: { disabled: true } }
