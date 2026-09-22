// SR-310 — 공통 TextInput. `:focus` 테두리색만 실제 CSS 의사클래스가 필요해 TextInput.css로 두고,
// 오류·비활성 표시는 기존 코드베이스 관례(CartLineItem 등)대로 prop 기반 인라인 스타일로 처리한다.
//
// round1 QA 재작업(low) — id 미지정 시 폴백이 `cmn-text-input-${label}`이라 같은 label을 가진
// 입력이 한 화면에 둘이면 DOM id가 중복돼 label htmlFor 연결이 첫 요소로 몰렸다. React useId()로
// 컴포넌트 인스턴스마다 고유한 id를 생성하도록 바꾼다.
import { useId, type InputHTMLAttributes } from 'react'
import './TextInput.css'

export interface TextInputProps extends Omit<InputHTMLAttributes<HTMLInputElement>, 'className'> {
  label: string
  error?: string | null
}

export function TextInput({ label, error, disabled, id, ...rest }: TextInputProps) {
  const generatedId = useId()
  const inputId = id ?? generatedId
  const fieldStyle: React.CSSProperties = {
    width: '100%',
    boxSizing: 'border-box',
    borderRadius: 'var(--radius-sm)',
    padding: 'var(--space-3)',
    fontSize: 'var(--text-sm)',
    border: `1px solid ${error ? 'var(--color-error)' : 'var(--color-text-tertiary)'}`,
    background: disabled ? 'var(--color-surface-2)' : '#fff',
    color: disabled ? 'var(--color-text-tertiary)' : 'var(--color-text)',
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 'var(--space-1)' }}>
      <label htmlFor={inputId} style={{ fontSize: 'var(--text-xs)', color: 'var(--color-text-secondary)' }}>
        {label}
      </label>
      <input
        id={inputId}
        className="cmn-text-input"
        style={fieldStyle}
        disabled={disabled}
        aria-invalid={!!error || undefined}
        {...rest}
      />
      {error && (
        <p role="alert" style={{ margin: 0, fontSize: 'var(--text-xs)', color: 'var(--color-error)' }}>
          {error}
        </p>
      )}
    </div>
  )
}
