// linked_func: FUNC-order-001 — 주문 목록의 배송상태 열(SR-210 · 문구 SR-226)
import type { DeliveryState } from '../types'

const LABEL: Record<string, { text: string; bg: string; fg: string }> = {
  READY: { text: '출고 대기중', bg: '#fff4e5', fg: '#8a5a00' },
  SHIPPED: { text: '출고', bg: '#e8f1ff', fg: '#0b4ea2' },
  DELIVERED: { text: '배송완료', bg: '#e9f7ec', fg: '#136b2f' },
}

/**
 * 배송 이력이 **없으면 "-"** 다 — 빈 문자열이나 "없음"이 아니다(UIS-ORD-001 §5 표시조건).
 * 상태 코드가 표에 없으면 코드를 그대로 보여 준다(조용히 감추면 새 코드가 추가돼도 아무도 모른다).
 */
export function DeliveryBadge({ state }: { state: DeliveryState }) {
  if (!state) return <span aria-label="배송 이력 없음">-</span>
  const s = LABEL[state]
  if (!s) return <span title="알 수 없는 상태 코드">{state}</span>
  return (
    <span style={{
      background: s.bg, color: s.fg, borderRadius: 4, padding: '2px 7px',
      fontSize: 12, fontWeight: 600, whiteSpace: 'nowrap',
    }}>{s.text}</span>
  )
}
