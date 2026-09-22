// linked_func: FUNC-order-001 — 주문 목록 그리드
import type { OrderRow } from '../types'
import { DeliveryBadge } from './DeliveryBadge'

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

export interface OrderTableProps {
  rows: OrderRow[]
  loading?: boolean
  /** 조회가 실패했을 때의 사유. 있으면 그리드·건수는 **모두 숨긴다**(UIS-ORD-001 §2 시나리오4, SR-208). */
  error?: string | null
  onOpen?: (orderNo: string) => void
  /** 사용자가 고른 행 — 있으면 그 행을 강조한다(SR-228). 없으면 종전과 같다 */
  selectedOrderNo?: string | null
}

/**
 * 주문 목록 그리드 — 6열(주문번호·회원·상태·총액·주문일시·배송상태).
 *
 * 표시 계약(UIS-ORD-001 §5):
 *  · 4xx/5xx면 그리드·"조회 결과 없음"·"전체 N건"을 **모두 숨기고** 오류만 보여 준다(SR-208).
 *    — 오류인데 "0건"이라고 말하면 사람은 "조건에 맞는 게 없구나"로 읽는다. 다른 사실이다.
 *  · 결과가 0건이면 "조회 결과 없음".
 *  · 배송 이력이 없으면 배송상태는 "-".
 */
export function OrderTable({ rows, loading, error, onOpen, selectedOrderNo }: OrderTableProps) {
  if (error) {
    return (
      <div role="alert" style={{ border: '1px solid #e0b4b4', background: '#fff6f6',
                                 color: '#912d2b', borderRadius: 6, padding: '12px 14px' }}>
        주문을 조회하지 못했습니다 — {error}
      </div>
    )
  }
  if (loading) return <div aria-busy="true" style={{ padding: 14, color: '#666' }}>조회 중…</div>
  if (!rows.length) return <div style={{ padding: 14, color: '#666' }}>조회 결과 없음</div>

  return (
    <>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
        <thead>
          <tr style={{ background: '#f6f7f9', textAlign: 'left' }}>
            {['주문번호', '회원', '상태', '총액', '주문일시', '배송상태'].map(h => (
              <th key={h} style={{ padding: '8px 10px', borderBottom: '1px solid #e3e5e8', fontWeight: 600 }}>{h}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map(r => (
            <tr key={r.orderNo} aria-selected={r.orderNo === selectedOrderNo || undefined}
                style={{ borderBottom: '1px solid #eef0f2',
                         background: r.orderNo === selectedOrderNo ? '#fff8e1' : undefined }}>
              <td style={{ padding: '8px 10px' }}>
                <a href={`#/orders/${r.orderNo}`} onClick={e => { if (onOpen) { e.preventDefault(); onOpen(r.orderNo) } }}
                   style={{ color: '#0b4ea2' }}>{r.orderNo}</a>
              </td>
              {/* 탈퇴·미존재 회원은 빈 문자열로 온다(SR-221 LEFT JOIN) — 화면은 "(탈퇴)"로 읽어 준다 */}
              <td style={{ padding: '8px 10px' }}>{r.memberName || <span style={{ color: '#999' }}>(탈퇴)</span>}</td>
              <td style={{ padding: '8px 10px' }}>{r.orderState}</td>
              <td style={{ padding: '8px 10px', textAlign: 'right', fontVariantNumeric: 'tabular-nums' }}>{won(r.totalAmount)}</td>
              <td style={{ padding: '8px 10px', fontVariantNumeric: 'tabular-nums' }}>{r.orderedAt}</td>
              <td style={{ padding: '8px 10px' }}><DeliveryBadge state={r.deliveryState} /></td>
            </tr>
          ))}
        </tbody>
      </table>
      <div style={{ padding: '10px 2px', color: '#444', fontSize: 13 }}>전체 {rows.length}건</div>
    </>
  )
}
