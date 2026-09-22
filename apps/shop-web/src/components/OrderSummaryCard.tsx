// linked_func: FUNC-order-001 — 주문 목록 요약 카드(SR-227)
import type { OrderRow } from '../types'

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

export interface OrderSummaryCardProps {
  rows: OrderRow[]
  /** 조회 실패면 카드도 숨긴다 — 그리드와 같은 규칙(UIS-ORD-001 §5, SR-208) */
  error?: string | null
}

/**
 * 조회 결과 요약 — 전체 건수·총 주문금액·상태별 건수(SR-227).
 * 0건이면 "집계할 주문이 없습니다"(0원·0건을 늘어놓지 않는다 — 없는 집계는 없다고 말한다).
 */
export function OrderSummaryCard({ rows, error }: OrderSummaryCardProps) {
  if (error) return null
  if (!rows.length) {
    return <div style={{ padding: '10px 12px', color: '#666', fontSize: 13, border: '1px dashed #d5d8dd',
                         borderRadius: 6, marginBottom: 12 }}>집계할 주문이 없습니다</div>
  }
  const total = rows.reduce((s, r) => s + r.totalAmount, 0)
  const byState = new Map<string, number>()
  for (const r of rows) byState.set(r.orderState, (byState.get(r.orderState) ?? 0) + 1)
  return (
    <div style={{ display: 'flex', gap: 18, alignItems: 'baseline', flexWrap: 'wrap', padding: '10px 12px',
                  background: '#f6f7f9', borderRadius: 6, marginBottom: 12, fontSize: 13 }}>
      <span><b style={{ fontSize: 16 }}>{rows.length}</b>건</span>
      <span>총 <b style={{ fontVariantNumeric: 'tabular-nums' }}>{won(total)}</b></span>
      <span style={{ color: '#555' }}>
        {[...byState].map(([k, n]) => `${k} ${n}`).join(' · ')}
      </span>
    </div>
  )
}
