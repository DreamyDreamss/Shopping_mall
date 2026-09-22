// SR-310 — 수량 스테퍼(−/수량/+). 직접입력은 `CartLineItem`의 draft 패턴을 재사용하되 서버 왕복이
// 없으므로 편집 중엔 로컬 문자열만 바꾸고, blur·Enter 커밋 시 `clampQuantity`로 경계를 보정한 뒤에만
// `onChange`를 호출한다(값이 실제로 달라졌을 때만 — 불필요한 알림을 올리지 않는다).
import { useState } from 'react'
import { clampQuantity } from './quantityClamp'

export interface QuantityStepperProps {
  value: number
  min: number
  max: number
  /** 클램프까지 끝난 확정 값. */
  onChange: (committed: number) => void
  label?: string
}

export function QuantityStepper({ value, min, max, onChange, label = '수량' }: QuantityStepperProps) {
  const [draft, setDraft] = useState<string | null>(null)
  const displayed = draft ?? String(value)

  const commitDraft = () => {
    if (draft === null) return // 편집 없이 blur(focus만 됐다 벗어남) — 커밋할 것이 없다
    const parsed = Number(draft.trim())
    setDraft(null)
    const clamped = clampQuantity(Number.isFinite(parsed) ? parsed : value, min, max)
    if (clamped !== value) onChange(clamped)
  }

  const step = (delta: number) => {
    const next = clampQuantity(value + delta, min, max)
    if (next !== value) onChange(next)
  }

  const disabledMinus = value <= min
  const disabledPlus = value >= max

  return (
    <div role="group" aria-label={label} style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-2)' }}>
      <button
        type="button"
        aria-label={`${label} 감소`}
        disabled={disabledMinus}
        onClick={() => step(-1)}
        style={{
          border: '1px solid var(--color-text-tertiary)', borderRadius: 'var(--radius-sm)', background: '#fff',
          width: 28, height: 28, cursor: disabledMinus ? 'not-allowed' : 'pointer',
          opacity: disabledMinus ? 0.5 : 1,
        }}
      >
        −
      </button>
      <input
        aria-label={label}
        type="number"
        value={displayed}
        min={min}
        max={max}
        onChange={e => setDraft(e.target.value)}
        onBlur={commitDraft}
        onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); e.currentTarget.blur() } }}
        style={{
          width: 44, textAlign: 'center', border: '1px solid var(--color-text-tertiary)',
          borderRadius: 'var(--radius-sm)', padding: 'var(--space-1) var(--space-1)',
        }}
      />
      <button
        type="button"
        aria-label={`${label} 증가`}
        disabled={disabledPlus}
        onClick={() => step(1)}
        style={{
          border: '1px solid var(--color-text-tertiary)', borderRadius: 'var(--radius-sm)', background: '#fff',
          width: 28, height: 28, cursor: disabledPlus ? 'not-allowed' : 'pointer',
          opacity: disabledPlus ? 0.5 : 1,
        }}
      >
        +
      </button>
    </div>
  )
}
