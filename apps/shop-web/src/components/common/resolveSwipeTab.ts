// SR-310 — 스와이프 판정 순수함수(Tabs.tsx와 분리해 단위테스트). 임계값 미만 이동은 무시하고,
// 임계값 이상이면 방향에 따라 인접 탭으로 넘어간다(경계(첫/마지막)에서는 범위를 벗어나지 않는다 —
// 랩핑하지 않는다).
export function resolveSwipeTab(
  startX: number,
  endX: number,
  thresholdPx: number,
  activeIndex: number,
  count: number,
): number {
  const delta = endX - startX
  if (Math.abs(delta) < thresholdPx) return activeIndex
  const direction = delta < 0 ? 1 : -1 // 왼쪽으로 밀면(delta<0) 다음 탭, 오른쪽으로 밀면 이전 탭
  const next = activeIndex + direction
  if (next < 0 || next >= count) return activeIndex
  return next
}
