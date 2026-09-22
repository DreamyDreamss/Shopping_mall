// SR-305 — 주문서(`/shop/order`) 결제 수단 표시(선택 UI만, 실제 결제 연동은 범위 밖 — 확정 답변).
// 항상 기본값이 선택돼 있어 빈 상태가 없다(STORY "파일" 절). 체크아웃 요청 바디는 `{memberId}`뿐이라
// 이 선택값은 서버로 전송되지 않는다 — 그 사실을 라벨 문구로 숨기지 않는다(STORY "순서·보안" 6).
export type PaymentMethod = 'CARD' | 'BANK_TRANSFER' | 'VIRTUAL_ACCOUNT'

export interface PaymentMethodSelectProps {
  value: PaymentMethod
  onChange: (value: PaymentMethod) => void
}

const OPTIONS: { value: PaymentMethod; label: string }[] = [
  { value: 'CARD', label: '신용/체크카드' },
  { value: 'BANK_TRANSFER', label: '실시간 계좌이체' },
  { value: 'VIRTUAL_ACCOUNT', label: '가상계좌' },
]

export function PaymentMethodSelect({ value, onChange }: PaymentMethodSelectProps) {
  return (
    <fieldset aria-label="결제 수단" style={{ border: '1px solid #eef0f2', borderRadius: 8, padding: '12px 16px' }}>
      <legend style={{ fontSize: 13, fontWeight: 700, padding: '0 4px' }}>결제 수단</legend>
      <p style={{ fontSize: 11.5, color: '#999', marginTop: 0 }}>
        이번 SR에서는 표시만 하고 실제 결제 연동은 하지 않습니다.
      </p>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
        {OPTIONS.map(opt => (
          <label key={opt.value} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 13 }}>
            <input type="radio" name="payment-method" value={opt.value}
                   checked={value === opt.value} onChange={() => onChange(opt.value)} />
            {opt.label}
          </label>
        ))}
      </div>
    </fieldset>
  )
}
