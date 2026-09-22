// SR-305 — 주문서(`/shop/order`) 주문 상품 요약(읽기전용, 현재 장바구니 전량 — 부분 선택 체크아웃
// 미지원이라 화면과 서버 동작이 항상 일치한다, STORY "순서·보안" 4). 값은 `fetchCart` 응답 그대로다
// (사람 수정 — 서버 응답을 정본으로 다시 그린다).
import type { CartRow } from '../../types'

export interface OrderItemsSummaryProps {
  items: CartRow[]
}

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

export function OrderItemsSummary({ items }: OrderItemsSummaryProps) {
  return (
    <section aria-label="주문 상품 요약" style={{ border: '1px solid #eef0f2', borderRadius: 8, padding: '12px 16px' }}>
      <h2 style={{ fontSize: 13, fontWeight: 700, margin: '0 0 10px' }}>주문 상품 ({items.length}개)</h2>
      <ul role="list" style={{ listStyle: 'none', margin: 0, padding: 0 }}>
        {items.map(item => (
          <li key={item.sku} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 13, padding: '6px 0' }}>
            <span>{item.productName} × {item.qty}</span>
            <span style={{ fontWeight: 700 }}>{won(item.lineTotal)}</span>
          </li>
        ))}
      </ul>
    </section>
  )
}
