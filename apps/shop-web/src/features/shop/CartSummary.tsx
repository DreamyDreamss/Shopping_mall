// SR-305 — 장바구니(`/shop/cart`) 합계 영역(선택 항목 기준 상품금액/배송비/결제예정금액 + [주문하기]).
//
// [주문하기]는 선택 개수 === 전체 개수일 때만 활성화한다(STORY "순서·보안" 4 — `CartService.checkout`
// 이 선택 여부와 무관하게 장바구니 전체를 주문으로 전환하는 서버 계약이라, 부분 선택 상태로 그대로
// 태우면 화면 표시(선택분)와 실제 서버 동작(전체)이 어긋난다). 부분 선택 중에만 안내 문구를 노출하고
// (사람 수정 — 상시 노출 아님), 비활성 버튼의 사유를 `aria-describedby`로 그 문구와 연결해 보조기기
// 에도 전달한다. 계산은 `cartTotals.ts`만 쓴다(재계산 금지).
//
// 재작업(round 2, 권고 1) — 전체 선택 해제(0건)도 [주문하기]가 비활성인 상태라 사유가 필요하다.
// 부분선택과 0건 선택은 동시에 성립하지 않으므로(상호 배타) 같은 `aria-describedby` id 하나를
// 공유해도 안전하다.
export interface CartSummaryProps {
  productAmount: number
  shippingFee: number
  payableAmount: number
  selectedCount: number
  totalCount: number
  onOrder: () => void
}

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

const rowStyle: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', fontSize: 13, padding: '4px 0' }

const DISABLED_REASON_ID = 'cart-summary-disabled-reason'

export function CartSummary({ productAmount, shippingFee, payableAmount, selectedCount, totalCount, onOrder }: CartSummaryProps) {
  const allSelected = totalCount > 0 && selectedCount === totalCount
  const isPartial = selectedCount > 0 && selectedCount < totalCount
  const isNoneSelected = totalCount > 0 && selectedCount === 0
  const disabled = !allSelected

  const disabledReasonText = isPartial
    ? '부분 선택 주문은 지원하지 않습니다 — 제외할 상품은 삭제해 주세요'
    : isNoneSelected
      ? '주문할 상품을 선택해 주세요'
      : null

  return (
    <aside aria-label="주문 합계" style={{ border: '1px solid #eef0f2', borderRadius: 8, padding: '16px 18px', minWidth: 240 }}>
      <div style={rowStyle}><span>상품금액</span><span>{won(productAmount)}</span></div>
      <div style={rowStyle}><span>배송비</span><span>{won(shippingFee)}</span></div>
      <div style={{ ...rowStyle, fontWeight: 800, fontSize: 15, borderTop: '1px solid #eef0f2', marginTop: 8, paddingTop: 10 }}>
        <span>결제예정금액</span><span>{won(payableAmount)}</span>
      </div>

      {disabledReasonText && (
        <p id={DISABLED_REASON_ID} role="note" style={{ marginTop: 10, fontSize: 12, color: '#a06a00' }}>
          {disabledReasonText}
        </p>
      )}

      <button type="button" onClick={onOrder} disabled={disabled}
              aria-describedby={disabledReasonText ? DISABLED_REASON_ID : undefined}
              style={{
                marginTop: 14, width: '100%', border: 0, borderRadius: 6, padding: '11px 0',
                fontSize: 14, fontWeight: 700, color: '#fff',
                background: disabled ? '#c7cdd6' : '#0b4ea2', cursor: disabled ? 'not-allowed' : 'pointer',
              }}>
        주문하기
      </button>
    </aside>
  )
}
