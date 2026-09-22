// SR-310 — 공통 Button 3종(주·보조·구매). :hover/:focus-visible은 실제 CSS 의사클래스가 필요해
// 인라인 스타일로는 표현할 수 없다 — 이 컴포넌트가 이 코드베이스에서 처음으로 별도 CSS 파일
// (Button.css)로 인라인스타일을 벗어나는 지점이다.
import type { ButtonHTMLAttributes } from 'react'
import './Button.css'

export type ButtonVariant = 'primary' | 'secondary' | 'purchase'

export interface ButtonProps extends Omit<ButtonHTMLAttributes<HTMLButtonElement>, 'className'> {
  /** 주(검정 채움) · 보조(흰 바탕+회색 테두리) · 구매(파랑→보라 그라데이션). 기본은 주. */
  variant?: ButtonVariant
  /** true면 클릭을 막고 "처리 중…"으로 라벨을 대체한다(disabled도 함께 적용). */
  loading?: boolean
}

export function Button({
  variant = 'primary', loading, disabled, children, type = 'button', ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      className={`cmn-btn cmn-btn--${variant}`}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading ? '처리 중…' : children}
    </button>
  )
}
