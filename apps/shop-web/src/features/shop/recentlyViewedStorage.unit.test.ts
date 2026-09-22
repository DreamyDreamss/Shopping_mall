/** @jest-environment jsdom */
// SR-302 — "최근 본 상품" localStorage 유틸: dedupe(맨 앞 이동)·8개 cap(가장 오래된 것부터 제거)·순서.
import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import { loadRecentlyViewedSkus, recordViewed } from './recentlyViewedStorage'

describe('recentlyViewed', () => {
  beforeEach(() => localStorage.clear())
  afterEach(() => localStorage.clear())

  test('기록이 없으면 빈 배열', () => {
    expect(loadRecentlyViewedSkus()).toEqual([])
  })

  test('기록한 순서의 역순(최근이 맨 앞)으로 반환한다', () => {
    recordViewed('a')
    recordViewed('b')
    recordViewed('c')
    expect(loadRecentlyViewedSkus()).toEqual(['c', 'b', 'a'])
  })

  test('같은 sku를 다시 보면 맨 앞으로 이동하고 중복 항목을 만들지 않는다', () => {
    recordViewed('a')
    recordViewed('b')
    recordViewed('a')
    expect(loadRecentlyViewedSkus()).toEqual(['a', 'b'])
  })

  test('8개를 초과하면 가장 오래된 항목부터 제거된다', () => {
    for (let i = 1; i <= 9; i++) recordViewed(`sku-${i}`)
    const skus = loadRecentlyViewedSkus()
    expect(skus).toHaveLength(8)
    expect(skus[0]).toBe('sku-9') // 가장 최근
    expect(skus).not.toContain('sku-1') // 가장 오래된 항목이 밀려남
  })

  test('빈 sku는 기록하지 않는다', () => {
    recordViewed('')
    expect(loadRecentlyViewedSkus()).toEqual([])
  })

  test('손상된 JSON이 저장돼 있으면 빈 배열로 취급한다', () => {
    localStorage.setItem('sl.shop.recentlyViewed', '{broken')
    expect(loadRecentlyViewedSkus()).toEqual([])
  })
})
