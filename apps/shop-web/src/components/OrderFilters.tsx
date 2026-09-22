// linked_func: FUNC-order-001 — 주문 목록 검색 조건
import type { OrderQuery } from '../types'
import { ORDER_STATES } from '../types'

export interface OrderFiltersProps {
  value: OrderQuery
  onChange: (next: OrderQuery) => void
  onSearch: () => void
  busy?: boolean
  /** 입력값이 규칙에 안 맞을 때의 사유(예: 시작일이 종료일보다 늦다) — 있으면 검색을 막는다 */
  invalid?: string | null
}

const box: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, padding: '6px 8px', fontSize: 13, minWidth: 130,
}

/**
 * 검색 조건 — 모두 **선택 입력**이고 입력한 것끼리 AND로 결합된다(UIS-ORD-001 §2).
 * 조건이 하나도 없으면 전체 조회다(막지 않는다 — 운영에서 실제로 쓰는 동선이다).
 */
export function OrderFilters({ value, onChange, onSearch, busy, invalid }: OrderFiltersProps) {
  const set = (k: keyof OrderQuery) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    onChange({ ...value, [k]: e.target.value })

  return (
    <form onSubmit={e => { e.preventDefault(); if (!invalid) onSearch() }}
          style={{ display: 'flex', flexWrap: 'wrap', gap: 8, alignItems: 'flex-end', marginBottom: 14 }}>
      <label style={{ fontSize: 12, color: '#555' }}>회원 ID<br />
        <input style={box} value={value.memberId} onChange={set('memberId')} placeholder="예: m001" /></label>
      <label style={{ fontSize: 12, color: '#555' }}>주문상태<br />
        <select style={box} value={value.orderState} onChange={set('orderState')}>
          <option value="">전체</option>
          {ORDER_STATES.map(s => <option key={s} value={s}>{s}</option>)}
        </select></label>
      <label style={{ fontSize: 12, color: '#555' }}>시작일<br />
        <input style={box} type="date" value={value.startDate} onChange={set('startDate')} /></label>
      <label style={{ fontSize: 12, color: '#555' }}>종료일<br />
        <input style={box} type="date" value={value.endDate} onChange={set('endDate')} /></label>
      <button type="submit" disabled={busy || !!invalid}
              style={{ border: 0, borderRadius: 5, padding: '8px 16px', fontSize: 13, fontWeight: 600,
                       background: invalid ? '#c8ccd2' : '#0b4ea2', color: '#fff',
                       cursor: invalid ? 'not-allowed' : 'pointer' }}>
        {busy ? '조회 중…' : '검색'}
      </button>
      {invalid && <span role="alert" style={{ color: '#912d2b', fontSize: 12.5 }}>{invalid}</span>}
    </form>
  )
}
