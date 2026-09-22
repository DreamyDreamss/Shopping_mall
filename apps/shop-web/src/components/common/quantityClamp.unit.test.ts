// SR-310 — clampQuantity 경계값·범위밖 보정 단위테스트.
import { describe, expect, test } from '@jest/globals'
import { clampQuantity } from './quantityClamp'

describe('clampQuantity', () => {
  test('범위 안의 값은 그대로 반환한다', () => {
    expect(clampQuantity(5, 1, 10)).toBe(5)
  })

  test('min 미만은 min으로 보정한다', () => {
    expect(clampQuantity(0, 1, 10)).toBe(1)
    expect(clampQuantity(-5, 1, 10)).toBe(1)
  })

  test('max 초과는 max로 보정한다', () => {
    expect(clampQuantity(999, 1, 10)).toBe(10)
  })

  test('경계값(min·max)은 그대로 통과한다', () => {
    expect(clampQuantity(1, 1, 10)).toBe(1)
    expect(clampQuantity(10, 1, 10)).toBe(10)
  })

  test('소수점은 정수로 절사한다(반올림 아님)', () => {
    expect(clampQuantity(3.9, 1, 10)).toBe(3)
  })

  test('NaN·Infinity는 min으로 보정한다', () => {
    expect(clampQuantity(NaN, 1, 10)).toBe(1)
    expect(clampQuantity(Infinity, 1, 10)).toBe(1)
    expect(clampQuantity(-Infinity, 1, 10)).toBe(1)
  })
})
