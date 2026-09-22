// SR-304 — 상품 상세 이미지 영역 상태(기본·이미지없음·로드실패).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductImageGallery } from './ProductImageGallery'

const meta = {
  title: '쇼핑상세/이미지 갤러리',
  component: ProductImageGallery,
  args: { productName: '스탠딩 데스크' },
  tags: ['UIS-ORD-010'],
} satisfies Meta<typeof ProductImageGallery>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 대표 이미지+썸네일 정확히 1개(같은 이미지 재사용, 실 SKU-1001 이미지 — 재작업 round 2:
 * 이전엔 THUMBNAIL_COUNT=3으로 같은 이미지를 3번 반복해 "여러 장"처럼 보였다, QA 권고 1 대응). */
export const 기본: Story = { args: { imageUrl: '/images/products/sku-1001.svg' } }

/** 이미지없음 — 이니셜 대체 영역만 보이고 썸네일 자체를 렌더하지 않는다. */
export const 이미지없음: Story = { args: { imageUrl: null } }

/**
 * 로드실패 — 재작업(round 2) QA 권고 4/사람 지시 4: `ProductCard`와 동일한 onError 폴백을 검증한다.
 * 실제 네트워크 404가 아니라 **깨진 data URI**를 써서 축 E(storybook 콘솔 오류 판정)를 막지 않는다
 * (SR-306 #1 r3~r4 사례 대조 — 진짜 404 URL을 스토리에 썼다가 3라운드 막힌 전례). 이미지 로드
 * 실패로 콘솔에 디코딩 오류가 남는 것은 이 스토리의 의도된 상태이므로 `shows-error` 태그를 단다.
 */
export const 로드실패: Story = {
  args: { imageUrl: 'data:image/png;base64,not-a-real-image-!!' },
  tags: ['shows-error'],
}
