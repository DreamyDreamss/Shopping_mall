// SR-351 — 별점 표시 상태(기본·평균없음·개별정수).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { StarRatingDisplay } from './StarRatingDisplay'

const meta = {
  title: '쇼핑상세/상품평/별점 표시',
  component: StarRatingDisplay,
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof StarRatingDisplay>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 평균 4.5점. */
export const 기본: Story = { tags: ['state:기본'], args: { value: 4.5 } }

/** 평균없음 — 리뷰 0건, '-'만 표시(0.0 표기 금지). */
export const 평균없음: Story = { args: { value: null } }

/** 개별정수 — 리뷰 1건의 rating(항상 정수), "4"로 표시(평균용 소수 1자리 포맷 재사용 금지, 재작업 round2). */
export const 개별정수: Story = { args: { value: 4, format: 'integer' } }
