// SR-311 — 앱 셸 브레이크포인트 경계 판정(순수함수). 경계는 AC 그대로: 750px 미만은 모바일(하단
// 탭바), 1200px 이상은 데스크톱(퀵바+우측 레일), 그 사이(750~1199)는 퀵바·레일·하단탭바 전부 없는
// 단일 컬럼(AC에 별도 규정이 없는 구간 — 넓은 본문 하나만 보여주는 것이 가장 보수적인 선택).
export interface BreakpointState {
  isMobile: boolean
  isTablet: boolean
  isDesktopRail: boolean
}

export function classifyBreakpoint(width: number): BreakpointState {
  if (width < 750) return { isMobile: true, isTablet: false, isDesktopRail: false }
  if (width < 1200) return { isMobile: false, isTablet: true, isDesktopRail: false }
  return { isMobile: false, isTablet: false, isDesktopRail: true }
}
