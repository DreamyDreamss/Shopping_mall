// SR-302 — 푸터.
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ShopFooter } from './ShopFooter'

const meta = {
  title: '쇼핑홈/푸터',
  component: ShopFooter,
  tags: ['UIS-ORD-008'],
} satisfies Meta<typeof ShopFooter>
export default meta

type Story = StoryObj<typeof meta>

export const 기본: Story = {}
