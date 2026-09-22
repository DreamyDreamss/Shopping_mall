// SR-302 — 배너 캐러셀 상태(1장·여러장).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { HeroBannerCarousel } from './HeroBannerCarousel'
import { BANNER_SLIDES } from './shopStatic'

const meta = {
  title: '쇼핑홈/히어로 배너',
  component: HeroBannerCarousel,
  tags: ['UIS-ORD-008'],
} satisfies Meta<typeof HeroBannerCarousel>
export default meta

type Story = StoryObj<typeof meta>

/** 여러 장 — 좌우 이동 버튼과 현재 위치 인디케이터가 보인다. */
export const 여러장: Story = { args: { slides: BANNER_SLIDES } }

/** 1장 — 넘길 대상이 없어 좌우 버튼·인디케이터를 감춘다. */
export const 한장: Story = { args: { slides: [BANNER_SLIDES[0]] } }
