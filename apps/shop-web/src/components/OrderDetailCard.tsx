// linked_func: FUNC-order-002 — 주문 상세
import type { OrderDetail } from '../types'
import { DeliveryBadge } from './DeliveryBadge'

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

export interface OrderDetailCardProps {
  order: OrderDetail | null
  loading?: boolean
  error?: string | null
  /** 조회 실패일 때 [다시 시도]가 부른다(SR-229) */
  onRetry?: () => void
}

const cell: React.CSSProperties = { padding: '7px 10px', borderBottom: '1px solid #eef0f2', fontSize: 13.5 }

/**
 * 주문 상세 — 주문 요약 + 주문 품목 + 배송 이력.
 *
 * 배송 이력 정렬은 **출고일시(shippedAt) DESC**다 — 주문 목록의 "최신"(delivery_no DESC)과 기준이
 * 다르며, 그건 의도된 것이다(UIS-ORD-001 개정 1.2가 그 차이와 사유를 기록하고 있다).
 * 미출고(READY, shippedAt null)는 맨 뒤로 보낸다.
 */
export function OrderDetailCard({ order, loading, error, onRetry }: OrderDetailCardProps) {
  if (error) {
    return <div role="alert" style={{ border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b',
                                      borderRadius: 6, padding: '12px 14px' }}>
      주문을 불러오지 못했습니다 — {error}
      {onRetry && <button type="button" onClick={onRetry} style={{ marginLeft: 10 }}>다시 시도</button>}</div>
  }
  if (loading) return <div aria-busy="true" style={{ padding: 14, color: '#666' }}>불러오는 중…</div>
  if (!order) return <div style={{ padding: 14, color: '#666' }}>주문을 찾을 수 없습니다.</div>

  const deliveries = [...order.deliveries].sort((a, b) => {
    if (!a.shippedAt && !b.shippedAt) return 0
    if (!a.shippedAt) return 1
    if (!b.shippedAt) return -1
    return b.shippedAt.localeCompare(a.shippedAt)
  })

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 18 }}>
      <section>
        <h2 style={{ fontSize: 15, margin: '0 0 8px' }}>주문 {order.orderNo}</h2>
        <table style={{ borderCollapse: 'collapse', width: '100%' }}>
          <tbody>
            <tr><th style={{ ...cell, textAlign: 'left', width: 110, color: '#555' }}>회원</th>
              <td style={cell}>{order.memberName || <span style={{ color: '#999' }}>(탈퇴)</span>} ({order.memberId})</td></tr>
            <tr><th style={{ ...cell, textAlign: 'left', color: '#555' }}>상태</th><td style={cell}>{order.orderState}</td></tr>
            <tr><th style={{ ...cell, textAlign: 'left', color: '#555' }}>총액</th>
              <td style={{ ...cell, fontVariantNumeric: 'tabular-nums' }}>{won(order.totalAmount)}</td></tr>
            <tr><th style={{ ...cell, textAlign: 'left', color: '#555' }}>주문일시</th><td style={cell}>{order.orderedAt}</td></tr>
          </tbody>
        </table>
      </section>

      <section>
        <h3 style={{ fontSize: 14, margin: '0 0 6px' }}>주문 품목 {order.items.length}건</h3>
        {order.items.length === 0
          ? <div style={{ color: '#666', fontSize: 13 }}>품목이 없습니다.</div>
          : <table style={{ width: '100%', borderCollapse: 'collapse' }}>
              <thead><tr style={{ background: '#f6f7f9', textAlign: 'left' }}>
                {['상품', '수량', '단가', '금액'].map(h =>
                  <th key={h} style={{ ...cell, fontWeight: 600 }}>{h}</th>)}
              </tr></thead>
              <tbody>
                {order.items.map(i => (
                  <tr key={i.productNo}>
                    <td style={cell}>{i.productName}</td>
                    <td style={{ ...cell, fontVariantNumeric: 'tabular-nums' }}>{i.quantity}</td>
                    <td style={{ ...cell, fontVariantNumeric: 'tabular-nums' }}>{won(i.unitPrice)}</td>
                    <td style={{ ...cell, fontVariantNumeric: 'tabular-nums' }}>{won(i.unitPrice * i.quantity)}</td>
                  </tr>
                ))}
              </tbody>
            </table>}
      </section>

      <section>
        <h3 style={{ fontSize: 14, margin: '0 0 6px' }}>배송 이력</h3>
        {deliveries.length === 0
          ? <div style={{ color: '#666', fontSize: 13 }}>배송 이력이 없습니다.</div>
          : <ul style={{ margin: 0, paddingLeft: 18, fontSize: 13.5, lineHeight: 1.9 }}>
              {deliveries.map(d => (
                <li key={d.deliveryNo}>
                  <DeliveryBadge state={d.state} />{' '}
                  <span style={{ color: '#555' }}>{d.deliveryNo}</span>{' — '}
                  {d.shippedAt ?? <span style={{ color: '#999' }}>미출고</span>}
                </li>
              ))}
            </ul>}
      </section>
    </div>
  )
}
