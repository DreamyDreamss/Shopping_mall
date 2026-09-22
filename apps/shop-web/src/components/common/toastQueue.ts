// SR-310 — 토스트 큐 순수함수(ToastStack.tsx와 분리해 단위테스트). 최대 max개까지만 유지하고
// 초과분은 가장 오래된 것부터 제거한다(배열의 앞쪽이 오래된 것, 뒤쪽이 최신).
export interface ToastItem {
  id: string
  message: string
  variant?: 'default' | 'success' | 'error'
}

export function pushToast(existing: ToastItem[], next: ToastItem, max = 3): ToastItem[] {
  const merged = [...existing, next]
  if (merged.length <= max) return merged
  return merged.slice(merged.length - max)
}

// round1 QA 재작업(low) — AC "최대 3개 동시 노출"이 호출부가 pushToast를 쓸 때만 성립하고
// ToastStack 자신은 상한을 강제하지 않았다(toasts에 4개 이상을 그대로 넘기면 4개가 다 렌더됨).
// 요구사항의 토스트 규약이므로 호출부 책임으로 남기지 않고 ToastStack이 스스로도 강제한다 — 배열의
// 뒤쪽이 최신이라는 pushToast와 같은 규약으로 가장 오래된(앞쪽) 것부터 잘라낸다.
export function capToasts(toasts: ToastItem[], max = 3): ToastItem[] {
  if (max <= 0) return []
  return toasts.length <= max ? toasts : toasts.slice(toasts.length - max)
}
