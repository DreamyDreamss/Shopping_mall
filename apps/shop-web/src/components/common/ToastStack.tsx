// SR-310 — 토스트 스택(최대 3개 동시 노출). 상한은 호출부가 toastQueue의 `pushToast`로 미리
// 적용해 오는 것이 기본 경로이지만, round1 QA 재작업(low) — AC "최대 3개 동시 노출"이 요구사항의
// 토스트 규약이므로 호출부 책임으로만 남기지 않고 ToastStack 자신도 `capToasts`로 방어적으로
// 강제한다(호출부가 상한 없이 4개 이상을 넘겨도 최신 3개만 렌더).
import { capToasts, type ToastItem } from './toastQueue'
import { Toast } from './Toast'

export interface ToastStackProps {
  toasts: ToastItem[]
  onDismiss: (id: string) => void
  /** 동시 노출 상한. 기본 3(요구사항 고정값) — 초과분은 가장 오래된 것부터 제거한다. */
  max?: number
}

export function ToastStack({ toasts, onDismiss, max = 3 }: ToastStackProps) {
  const capped = capToasts(toasts, max)
  return (
    <div
      aria-label="토스트 목록"
      style={{
        position: 'fixed', right: 'var(--space-5)', bottom: 'var(--space-5)',
        display: 'flex', flexDirection: 'column', gap: 'var(--space-2)', zIndex: 'var(--z-toast)',
      }}
    >
      {capped.map(t => (
        <Toast key={t.id} message={t.message} variant={t.variant} onDismiss={() => onDismiss(t.id)} />
      ))}
    </div>
  )
}
