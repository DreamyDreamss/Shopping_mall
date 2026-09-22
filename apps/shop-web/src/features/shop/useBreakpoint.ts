// SR-311 — 앱 셸 브레이크포인트 훅. `window.matchMedia`는 쓰지 않는다 — jsdom은 기본적으로
// `matchMedia`를 구현하지 않아 호출 즉시 TypeError가 나고, 이 SR이 8개 페이지 전부를 AppShell로
// 감싸므로 그 순간 기존 페이지 테스트 전부가 마운트 단계에서 하드 크래시한다(STORY "프레임워크 실행
// 모델 함정" 절). `window.innerWidth` 읽기 + `resize` 이벤트 리스너만 쓴다(jsdom 기본 지원).
import { useEffect, useState } from 'react'
import { classifyBreakpoint, type BreakpointState } from './breakpoint'

export function useBreakpoint(): BreakpointState {
  const [state, setState] = useState<BreakpointState>(() => classifyBreakpoint(window.innerWidth))

  useEffect(() => {
    const handleResize = () => setState(classifyBreakpoint(window.innerWidth))
    window.addEventListener('resize', handleResize)
    return () => window.removeEventListener('resize', handleResize)
  }, [])

  return state
}
