// SR-302 — 상품 카드 상태(기본·할인·품절·이미지없음).
// SR-306 — 정가·이미지가 실제로 응답에 들어와(SR-306 #2), 상태를 실 SKU 데이터로 갱신하고
// 정가없음·할인0퍼센트·이미지있음·이미지로드실패 4상태를 추가했다(DB 실측, 2026-09-17, STORY 구현계획).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductCard } from './ProductCard'
import type { Product } from '../../types'

const product: Product = {
  sku: 'sku-1', productName: '스토리북 샘플 상품', price: 29000, stockQty: 12, saleYn: 'Y',
  listPrice: null, imageUrl: null,
}

const meta = {
  title: '쇼핑홈/상품 카드',
  component: ProductCard,
  args: { onSelect: () => {} },
  tags: ['UIS-ORD-008'],
} satisfies Meta<typeof ProductCard>
export default meta

type Story = StoryObj<typeof meta>

/** 기본 — 정가·이미지 없이 판매가만(합성 데이터, 실 SKU 4건 중 이 조합 없음). */
export const 기본: Story = { args: { product } }

/**
 * 할인 — 실 SKU-1001(스탠딩 데스크), price 390,000 / listPrice 450,000 / image
 * `/images/products/sku-1001.svg`(DB 실측, 2026-09-17). 취소선 정가와 내림 정수 할인율(13%)이
 * 실제로 계산돼 보인다.
 */
export const 할인: Story = {
  args: {
    product: {
      sku: 'SKU-1001', productName: '스탠딩 데스크', price: 390000, stockQty: 12, saleYn: 'Y',
      listPrice: 450000, imageUrl: '/images/products/sku-1001.svg',
    },
  },
}

/** 품절 — 배지와 함께 카드 전체가 흐려진다. */
export const 품절: Story = { args: { product: { ...product, stockQty: 0 } } }

/**
 * 이미지없음 — 실 SKU-1004(단종 마우스), price 35,000 / listPrice 42,000 / image null / stockQty 0 /
 * saleYn N(DB 실측, 2026-09-17). 이 SKU는 실데이터상 품절이기도 해 품절 배지가 함께 보이는 것이
 * 맞다 — stockQty를 임의로 올려 지어내지 않는다.
 */
export const 이미지없음: Story = {
  args: {
    product: {
      sku: 'SKU-1004', productName: '단종 마우스', price: 35000, stockQty: 0, saleYn: 'N',
      listPrice: 42000, imageUrl: null,
    },
  },
}

/**
 * 정가없음 — 실 SKU-1002(기계식 키보드), price 129,000 / listPrice null / image
 * `/images/products/sku-1002.svg`(DB 실측, 2026-09-17). 정가가 없으면 listPrice > price 조건이
 * 성립하지 않아 취소선·할인 배지 둘 다 그려지지 않는다.
 */
export const 정가없음: Story = {
  args: {
    product: {
      sku: 'SKU-1002', productName: '기계식 키보드', price: 129000, stockQty: 20, saleYn: 'Y',
      listPrice: null, imageUrl: '/images/products/sku-1002.svg',
    },
  },
}

/**
 * 이미지있음 — 정가없음과 동일 SKU-1002 데이터를 다른 관점에서 재노출한다(정가 유무와 무관하게
 * 이미지가 실제로 로드되는 케이스를 보여주는 것이 이 상태의 목적).
 */
export const 이미지있음: Story = {
  args: {
    product: {
      sku: 'SKU-1002', productName: '기계식 키보드', price: 129000, stockQty: 20, saleYn: 'Y',
      listPrice: null, imageUrl: '/images/products/sku-1002.svg',
    },
  },
}

/**
 * 할인0퍼센트 — 실 SKU-1003(4K 모니터), price 450,000 / listPrice 450,000(=판매가, DB 실측,
 * 2026-09-17). listPrice가 price보다 크지 않아 취소선·할인 배지 둘 다 그려지지 않아야 한다.
 */
export const 할인0퍼센트: Story = {
  args: {
    product: {
      sku: 'SKU-1003', productName: '4K 모니터', price: 450000, stockQty: 8, saleYn: 'Y',
      listPrice: 450000, imageUrl: '/images/products/sku-1003.svg',
    },
  },
}

/**
 * 이미지로드실패 — 깨진 data URI(합성 데이터)를 imageUrl로 줘 `onError` 핸들러가 실제로
 * 타는지(이니셜 대체 영역으로 전환) 육안 확인한다.
 * 실제 404 네트워크 경로(`/images/products/does-not-exist.svg`)를 쓰지 않는 이유: 축E
 * (story_gate.py → storybook_render.js)는 favicon을 뺀 콘솔 error가 1건이라도 있으면 그
 * 스토리를 깨진 것으로 판정하는데, 404 요청은 브라우저 콘솔에 error를 남겨 구조적으로 통과할
 * 수 없다(round3 QA 실측). data URI는 디코드 실패로 `onerror`는 그대로 발생시키되 네트워크
 * 요청이 아니라 콘솔 error를 남기지 않는다(QA 실측: `_tmp/qa_img_probe.js`, 축E와 동일 판정
 * 기준으로 콘솔error 0건 확인, round4).
 */
export const 이미지로드실패: Story = {
  args: {
    product: {
      sku: 'SKU-9999', productName: '이미지 깨짐 상품', price: 10000, stockQty: 5, saleYn: 'Y',
      listPrice: null, imageUrl: 'data:image/png;base64,AAAAAAAA',
    },
  },
}
