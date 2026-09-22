// SR-310 — 공통 Badge 5종(TV상품·무료 배송·무이자 N·LIVE 카운트다운 슬롯·할인율). 할인율은
// 이 컴포넌트 안에서 다시 계산하지 않는다 — 호출부가 기존 `discountRate.ts`의 `calcDiscountRate`로
// 계산해 `rate`로 그대로 넘긴다(실패 사례집 SR-306 #1: 소비처가 자체 계산 로직을 새로 만들어 정본과
// 어긋난 사례와 같은 함정을 피한다).
//
// round1 QA 재작업(low) — props가 variant별 판별 유니온이 아니라 months/rate가 전부 optional이라
// installment에 months 누락 시 "무이자 0개월", discount에 rate 누락 시 "0%"가 타입 에러 없이 조용히
// 렌더됐다. variant별 판별 유니온으로 바꿔 installment는 months 필수, discount는 rate 필수로 고정한다.
import type { ReactNode } from 'react'

export type BadgeVariant = 'tv' | 'freeShipping' | 'installment' | 'live' | 'discount'

export type BadgeProps =
  | { variant: 'tv' }
  | { variant: 'freeShipping' }
  | { variant: 'installment'; /** "무이자 {N}개월"로 표시한다. */ months: number }
  | { variant: 'live'; /** 카운트다운 텍스트 슬롯(실제 초단위 갱신 타이머는 이 배지를 쓰는 화면 SR 몫). */ countdownSlot?: ReactNode }
  | { variant: 'discount'; /** 호출부가 calcDiscountRate로 계산해 넘긴 내림 정수 %. */ rate: number }

const BASE_STYLE: React.CSSProperties = {
  display: 'inline-flex',
  alignItems: 'center',
  gap: 'var(--space-1)',
  borderRadius: 'var(--radius-sm)',
  padding: '2px var(--space-2)',
  fontSize: 'var(--text-2xs)',
  fontWeight: 'var(--font-weight-bold)',
  whiteSpace: 'nowrap',
}

export function Badge(props: BadgeProps) {
  switch (props.variant) {
    case 'tv':
      return <span style={{ ...BASE_STYLE, background: 'var(--color-text)', color: '#fff' }}>TV상품</span>

    case 'freeShipping':
      return (
        <span style={{ ...BASE_STYLE, background: 'var(--color-success-bg)', color: 'var(--color-success)' }}>
          무료 배송
        </span>
      )

    case 'installment':
      return (
        <span
          style={{
            ...BASE_STYLE, background: '#fff', color: 'var(--color-text-secondary)',
            border: '1px solid var(--color-text-tertiary)',
          }}
        >
          무이자 {props.months}개월
        </span>
      )

    case 'live':
      return (
        <span style={{ ...BASE_STYLE, background: 'var(--color-brand)', color: '#fff' }}>
          LIVE
          {props.countdownSlot != null && <span>{props.countdownSlot}</span>}
        </span>
      )

    case 'discount':
      return (
        <span style={{ ...BASE_STYLE, background: 'var(--color-price)', color: '#fff' }}>{props.rate}%</span>
      )
  }
}
