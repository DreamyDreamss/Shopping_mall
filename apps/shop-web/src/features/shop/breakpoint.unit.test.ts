// SR-311 — 앱 셸 브레이크포인트 경계값(749/750/1199/1200) 표 기반 테스트.
import { describe, expect, test } from '@jest/globals'
import { classifyBreakpoint } from './breakpoint'

describe('classifyBreakpoint — 750/1200 경계', () => {
  const table: { width: number; expected: ReturnType<typeof classifyBreakpoint>; label: string }[] = [
    { width: 320, expected: { isMobile: true, isTablet: false, isDesktopRail: false }, label: '모바일 최소' },
    { width: 749, expected: { isMobile: true, isTablet: false, isDesktopRail: false }, label: '750 미만 경계(749) → 모바일' },
    { width: 750, expected: { isMobile: false, isTablet: true, isDesktopRail: false }, label: '750 경계(750) → 태블릿' },
    { width: 1024, expected: { isMobile: false, isTablet: true, isDesktopRail: false }, label: 'jsdom 기본값(1024) → 태블릿' },
    { width: 1199, expected: { isMobile: false, isTablet: true, isDesktopRail: false }, label: '1200 미만 경계(1199) → 태블릿' },
    { width: 1200, expected: { isMobile: false, isTablet: false, isDesktopRail: true }, label: '1200 경계(1200) → 데스크톱' },
    { width: 1920, expected: { isMobile: false, isTablet: false, isDesktopRail: true }, label: '데스크톱 최대' },
  ]

  test.each(table)('width=$width → $label', ({ width, expected }) => {
    expect(classifyBreakpoint(width)).toEqual(expected)
  })
})
