// linked_func: FUNC-order-001 — 주문 목록 화면(컨테이너)
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { OrderFilters } from '../components/OrderFilters'
import { OrderTable } from '../components/OrderTable'
import { OrderSummaryCard } from '../components/OrderSummaryCard'
import { fetchOrders } from '../api'
import { EMPTY_QUERY, type OrderQuery, type OrderRow } from '../types'

/** 시작일이 종료일보다 늦으면 서버에 보낼 필요가 없다 — 화면에서 먼저 막고 사유를 말한다. */
function invalidReason(q: OrderQuery): string | null {
  if (q.startDate && q.endDate && q.startDate > q.endDate) return '시작일이 종료일보다 늦습니다'
  return null
}

export default function OrderListPage() {
  const nav = useNavigate()
  const [query, setQuery] = useState<OrderQuery>(EMPTY_QUERY)
  const [rows, setRows] = useState<OrderRow[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [selected, setSelected] = useState<string | null>(null)

  const load = async (q: OrderQuery) => {
    setLoading(true); setError(null)
    try { setRows(await fetchOrders(q)) }
    catch (e) { setError(e instanceof Error ? e.message : String(e)); setRows([]) }
    finally { setLoading(false) }
  }
  useEffect(() => { void load(EMPTY_QUERY) }, [])

  return (
    <div style={{ maxWidth: 1040, margin: '0 auto', padding: '20px 18px', fontFamily: 'system-ui, sans-serif' }}>
      <h1 style={{ fontSize: 18, marginBottom: 14 }}>주문 목록</h1>
      <OrderFilters value={query} onChange={setQuery} onSearch={() => void load(query)}
                    busy={loading} invalid={invalidReason(query)} />
      {!loading && <OrderSummaryCard rows={rows} error={error} />}
      <OrderTable rows={rows} loading={loading} error={error}
                  selectedOrderNo={selected}
                  onOpen={no => { setSelected(no); nav('/orders/' + no) }} />
    </div>
  )
}
