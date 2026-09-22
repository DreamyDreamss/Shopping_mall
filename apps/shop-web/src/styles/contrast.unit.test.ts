// SR-309 — contrast.ts(WCAG 대비 계산) 단위 테스트. 알려진 값(흑백·동일색)과 AA 문턱(4.5:1)
// 인접 경계값을 고정한다.
import { describe, expect, test } from '@jest/globals'
import { contrastRatio, hexLuminance, meetsAA } from './contrast'

describe('hexLuminance — 상대휘도', () => {
  test('흰색(#FFFFFF) → 1', () => {
    expect(hexLuminance('#FFFFFF')).toBeCloseTo(1, 5)
  })
  test('검정(#000000) → 0', () => {
    expect(hexLuminance('#000000')).toBeCloseTo(0, 5)
  })
  test('3자리 축약 hex(#fff)도 6자리와 동일하게 계산', () => {
    expect(hexLuminance('#fff')).toBeCloseTo(hexLuminance('#ffffff'), 10)
  })
  test('비-hex 입력(rgba(...) 문자열)은 조용히 NaN을 내지 않고 예외를 던진다', () => {
    expect(() => hexLuminance('rgba(16, 16, 16, 0.3)')).toThrow()
  })
  test('빈 문자열(tokens.css 미로드 시 getPropertyValue 반환값)도 예외를 던진다', () => {
    expect(() => hexLuminance('')).toThrow()
  })
})

describe('contrastRatio — 명도 대비율', () => {
  test('흑백 조합 → 21:1(최대 대비)', () => {
    expect(contrastRatio('#000000', '#FFFFFF')).toBeCloseTo(21, 1)
  })
  test('동일색 조합 → 1:1(대비 없음)', () => {
    expect(contrastRatio('#713FC5', '#713FC5')).toBeCloseTo(1, 5)
  })
  test('인자 순서를 바꿔도 결과는 같다(더 밝은 색을 분자로 자동 정렬)', () => {
    expect(contrastRatio('#101010', '#F9F9F9')).toBeCloseTo(contrastRatio('#F9F9F9', '#101010'), 10)
  })
})

describe('meetsAA — AA 문턱(4.5:1 인접 경계값)', () => {
  test('4.49:1(일반 텍스트) → 미달(거짓)', () => {
    expect(meetsAA(4.49)).toBe(false)
  })
  test('4.51:1(일반 텍스트) → 통과(참)', () => {
    expect(meetsAA(4.51)).toBe(true)
  })
  test('정확히 4.5:1 → 통과(경계 포함, 참)', () => {
    expect(meetsAA(4.5)).toBe(true)
  })
  test('큰 텍스트는 3:1이 문턱 — 2.99는 미달', () => {
    expect(meetsAA(2.99, true)).toBe(false)
  })
  test('큰 텍스트 3.01은 통과', () => {
    expect(meetsAA(3.01, true)).toBe(true)
  })
  test('큰 텍스트 정확히 3:1 → 통과(경계 포함, 참) — WCAG 큰 글씨 기준 24px/18.66px 굵게에 대응', () => {
    expect(meetsAA(3.0, true)).toBe(true)
  })
  test('큰 텍스트 문턱(3:1)에서는 일반 텍스트라면 미달일 4.0도 통과', () => {
    expect(meetsAA(4.0, true)).toBe(true)
    expect(meetsAA(4.0, false)).toBe(false)
  })
})
