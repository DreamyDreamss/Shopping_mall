// SR-304 — 상품정보제공고시 표(항목-값 2열). 값 소스가 없어 전 항목 값은 '-'(확정 답변 "고지 항목
// 값 없음 → '-'" 그대로 전항목 적용, STORY "데이터" 절).
import { PRODUCT_NOTICE_ITEMS } from './productNoticeItems'

export function ProductNoticeTable() {
  return (
    <section aria-label="상품정보제공고시">
      <h2 style={{ fontSize: 15, marginBottom: 10 }}>상품정보제공고시</h2>
      <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }}>
        <tbody>
          {PRODUCT_NOTICE_ITEMS.map(item => (
            <tr key={item} style={{ borderBottom: '1px solid #eef0f2' }}>
              <th scope="row" style={{ textAlign: 'left', width: '35%', padding: '8px 10px', color: '#666', fontWeight: 500 }}>
                {item}
              </th>
              <td style={{ padding: '8px 10px', color: '#222' }}>-</td>
            </tr>
          ))}
        </tbody>
      </table>
    </section>
  )
}
