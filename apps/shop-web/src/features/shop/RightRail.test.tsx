/** @jest-environment jsdom */
// SR-311 round3 재작업 지시 3 — round2 재작업 지시 4("최근 본 sku가 0개면 fetchProducts 자체를 부르지
// 않는다", `RightRail.tsx:52`)가 가드만 들어가고 실물 검증이 없었다(round2 QA round2 권고 3). `fetch`를
// 직접 스파이해 호출 여부를 확인한다 — `jest.mock('../../api', ...)`는 이 프로젝트의 `@swc/jest` 설정에서
// 정적 import보다 먼저 적용되지 않아(hoisting 미지원, `GnbTabs.test.tsx` 옆 주석·STORY "테스트" 절 실측
// 근거) 대신 `AppShell.test.tsx`/`RightRail.stories.tsx`와 동일하게 `global.fetch`를 교체한다.
import '@testing-library/jest-dom/jest-globals'
import { render, screen } from '@testing-library/react'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import { RightRail } from './RightRail'
import { recordViewed } from './recentlyViewedStorage'
import type { Product } from '../../types'

function jsonResponse(body: unknown): Response {
  return { ok: true, status: 200, statusText: '', json: async () => body } as Response
}

const product: Product = {
  sku: 'sku-rail-guard-1', productName: '레일 가드 테스트 상품', price: 10000, stockQty: 5, saleYn: 'Y',
  listPrice: null, imageUrl: null,
}

describe('RightRail — 최근 본 sku 0개면 fetchProducts 미호출(round3 재작업 지시 3)', () => {
  let fetchSpy: jest.Mock

  beforeEach(() => {
    localStorage.clear()
    fetchSpy = jest.fn(async () => jsonResponse([product])) as unknown as jest.Mock
    global.fetch = fetchSpy as unknown as typeof fetch
  })

  afterEach(() => {
    localStorage.clear()
  })

  test('최근 본 sku가 없으면 fetchProducts(=fetch)를 부르지 않고 아무것도 렌더하지 않는다', () => {
    const { container } = render(<RightRail />)
    expect(fetchSpy).not.toHaveBeenCalled()
    expect(container).toBeEmptyDOMElement()
  })

  test('최근 본 sku가 있으면 fetchProducts(=fetch)를 부르고 카드가 뜬다', async () => {
    recordViewed(product.sku)
    render(<RightRail />)
    expect(await screen.findByLabelText('사이드 정보')).toBeInTheDocument()
    expect(fetchSpy).toHaveBeenCalledTimes(1)
  })
})
