// linked_func: FUNC-order-002 — 주문 상세 화면(컨테이너)
import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { OrderDetailCard } from '../components/OrderDetailCard'
import { fetchOrder } from '../api'
import type { OrderDetail } from '../types'

export default function OrderDetailPage() {
  const { orderNo = '' } = useParams()
  const [order, setOrder] = useState<OrderDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let alive = true
    setLoading(true); setError(null)
    fetchOrder(orderNo)
      .then(o => { if (alive) setOrder(o) })
      .catch(e => { if (alive) { setError(e instanceof Error ? e.message : String(e)); setOrder(null) } })
      .finally(() => { if (alive) setLoading(false) })
    return () => { alive = false }
  }, [orderNo])

  return (
    <div style={{ maxWidth: 900, margin: '0 auto', padding: '20px 18px', fontFamily: 'system-ui, sans-serif' }}>
      <Link to="/" style={{ fontSize: 13, color: '#0b4ea2' }}>&larr; 주문 목록</Link>
      <div style={{ height: 12 }} />
      <OrderDetailCard order={order} loading={loading} error={error} />
    </div>
  )
}
