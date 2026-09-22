// SR-305 — 주문서(`/shop/order`) 배송지 입력(수령인/연락처/우편번호(읽기전용)/도로명주소(읽기전용)/
// 상세주소 + 필드별 에러). 우편번호·도로명주소는 [우편번호 찾기] 버튼으로 여는 `ZipcodeSearchModal`
// 선택으로만 채워진다(직접 타이핑 불가 — 읽기전용). 검증(`deliveryAddressValidation.ts`)은 부모
// (`OrderPage`)가 [결제하기] 클릭 시 수행하고, 그 결과(`errors`)를 그대로 받아 표시만 한다.
import type { DeliveryAddressErrors } from './deliveryAddressValidation'

export interface DeliveryAddressFormProps {
  recipientName: string
  phone: string
  zipcode: string
  roadAddress: string
  detailAddress: string
  errors: DeliveryAddressErrors
  onRecipientNameChange: (value: string) => void
  onPhoneChange: (value: string) => void
  onDetailAddressChange: (value: string) => void
  onOpenZipcodeSearch: () => void
}

const fieldStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 4, marginBottom: 12 }
const inputStyle: React.CSSProperties = { border: '1px solid #d5d8dd', borderRadius: 5, padding: '7px 10px', fontSize: 13 }
const labelStyle: React.CSSProperties = { fontSize: 12.5, color: '#666' }
const errorStyle: React.CSSProperties = { fontSize: 11.5, color: '#912d2b' }

export function DeliveryAddressForm({
  recipientName, phone, zipcode, roadAddress, detailAddress, errors,
  onRecipientNameChange, onPhoneChange, onDetailAddressChange, onOpenZipcodeSearch,
}: DeliveryAddressFormProps) {
  return (
    <section aria-label="배송지 입력" style={{ maxWidth: 420 }}>
      <div style={fieldStyle}>
        <label htmlFor="delivery-recipient" style={labelStyle}>수령인</label>
        <input id="delivery-recipient" value={recipientName} onChange={e => onRecipientNameChange(e.target.value)}
               aria-invalid={!!errors.recipientName} style={inputStyle} />
        {errors.recipientName && <span role="alert" style={errorStyle}>{errors.recipientName}</span>}
      </div>

      <div style={fieldStyle}>
        <label htmlFor="delivery-phone" style={labelStyle}>연락처</label>
        <input id="delivery-phone" value={phone} onChange={e => onPhoneChange(e.target.value)}
               aria-invalid={!!errors.phone} style={inputStyle} />
        {errors.phone && <span role="alert" style={errorStyle}>{errors.phone}</span>}
      </div>

      <div style={fieldStyle}>
        <label htmlFor="delivery-zipcode" style={labelStyle}>우편번호</label>
        <div style={{ display: 'flex', gap: 8 }}>
          <input id="delivery-zipcode" value={zipcode} readOnly aria-invalid={!!errors.zipcode}
                 style={{ ...inputStyle, flex: '0 0 120px', background: '#f7f8fa' }} />
          <button type="button" onClick={onOpenZipcodeSearch}
                  style={{ border: '1px solid #0b4ea2', borderRadius: 5, background: '#fff', color: '#0b4ea2',
                           fontSize: 12.5, padding: '0 12px', cursor: 'pointer' }}>
            우편번호 찾기
          </button>
        </div>
        <input aria-label="도로명주소" value={roadAddress} readOnly style={{ ...inputStyle, background: '#f7f8fa' }} />
        {errors.zipcode && <span role="alert" style={errorStyle}>{errors.zipcode}</span>}
      </div>

      <div style={fieldStyle}>
        <label htmlFor="delivery-detail" style={labelStyle}>상세주소</label>
        <input id="delivery-detail" value={detailAddress} onChange={e => onDetailAddressChange(e.target.value)}
               aria-invalid={!!errors.detailAddress} style={inputStyle} />
        {errors.detailAddress && <span role="alert" style={errorStyle}>{errors.detailAddress}</span>}
      </div>
    </section>
  )
}
