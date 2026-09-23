// SR-303 — 상품 목록 결과 영역 상태(결과있음·결과없음(검색어)·결과없음(필터)·로딩·조회실패).
// 조회실패는 `error`를 문자열 prop으로 직접 주입할 뿐 실제 네트워크 요청을 하지 않는다(콘솔 오류 없음
// — `ProductGrid.stories.tsx`와 동일 패턴, `shows-error` 태그가 필요 없다).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { ProductListGrid } from './ProductListGrid'
import type { Product } from '../../types'

const rows: Product[] = [
  { sku: 'SKU-1001', productName: '스탠딩 데스크', price: 390000, stockQty: 12, saleYn: 'Y', listPrice: 450000, imageUrl: null },
  { sku: 'SKU-1002', productName: '기계식 키보드', price: 129000, stockQty: 20, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'SKU-1003', productName: '4K 모니터', price: 450000, stockQty: 8, saleYn: 'Y', listPrice: 450000, imageUrl: null },
  { sku: 'SKU-1004', productName: '단종 마우스', price: 35000, stockQty: 0, saleYn: 'N', listPrice: 42000, imageUrl: null },
]

const meta = {
  title: '쇼핑홈/상품 목록 그리드',
  component: ProductListGrid,
  args: { onSelect: () => {}, onRetry: () => {}, onClearKeyword: () => {}, onResetFilters: () => {} },
  tags: ['UIS-ORD-009'],
} satisfies Meta<typeof ProductListGrid>
export default meta

type Story = StoryObj<typeof meta>

/** 결과있음 — 정렬·페이지 적용 후 넘어온 카드 그대로 렌더(표시 개수 상한 없음). */
export const 결과있음: Story = { args: { rows } }

/** 결과없음(검색어) — "검색 결과가 없습니다" + [검색어 지우기]. */
export const 결과없음_검색어: Story = { args: { rows: [], emptyReason: 'keyword' } }

/** 결과없음(필터) — "조건에 맞는 상품이 없습니다" + [필터 초기화]. */
export const 결과없음_필터: Story = { args: { rows: [], emptyReason: 'filter' } }

/** 로딩 — 그리드 자리에 골격(스켈레톤) 카드. */
export const 로딩: Story = { tags: ['state:로딩'], args: { rows: [], loading: true } }

/** 조회실패 — 문구 + [다시 시도]만 보인다(서버 계약은 그대로, 확정 답변 api_error). */
export const 조회실패: Story = { tags: ['state:오류'], args: { rows: [], error: '500 Internal Server Error' } }
