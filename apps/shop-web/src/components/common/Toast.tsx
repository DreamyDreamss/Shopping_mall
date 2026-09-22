// SR-310 — 토스트 1개 프레젠테이션. 스택 관리(최대 3개·초과 제거)는 ToastStack + toastQueue 몫이고
// 이 컴포넌트는 표시만 한다.
export interface ToastProps {
  message: string
  variant?: 'default' | 'success' | 'error'
  onDismiss: () => void
}

const VARIANT_STYLE: Record<'default' | 'success' | 'error', React.CSSProperties> = {
  default: { background: 'var(--color-text)', color: '#fff' },
  success: { background: 'var(--color-success)', color: '#fff' },
  error: { background: 'var(--color-error)', color: '#fff' },
}

export function Toast({ message, variant = 'default', onDismiss }: ToastProps) {
  return (
    <div
      role="status"
      style={{
        ...VARIANT_STYLE[variant],
        borderRadius: 'var(--radius-sm)',
        padding: 'var(--space-3) var(--space-4)',
        display: 'flex',
        alignItems: 'center',
        gap: 'var(--space-3)',
        fontSize: 'var(--text-sm)',
        boxShadow: 'var(--shadow-md)',
      }}
    >
      <span>{message}</span>
      <button
        type="button"
        aria-label="토스트 닫기"
        onClick={onDismiss}
        style={{ border: 0, background: 'none', color: 'inherit', cursor: 'pointer', fontSize: 14 }}
      >
        ✕
      </button>
    </div>
  )
}
