// SR-235 — 마이페이지 배송지 관리(`/shop/mypage/addresses`) 등록/수정 폼. 우편번호/도로명주소는
// `ZipcodeSearchModal`(부모가 오픈) 선택으로만 채워지는 읽기전용 필드다(`DeliveryAddressForm`과 동일
// 관례이나 필드 구성이 달라(entranceMethod/deliveryMemo/기본설정 체크박스 추가) 별도 컴포넌트로 둔다,
// STORY "범위 밖" 절 — 기존 `DeliveryAddressForm.tsx`는 손대지 않는다).
//
// 검증은 `deliveryAddressValidation.ts`(재사용, 신규 파일 아님)를 그대로 쓴다 — 그 모듈의
// `DeliveryAddressErrors` 키가 `recipientName`이라 이 폼의 `recipient` 필드는 `errors.recipientName`으로
// 오류를 받는다(필드 이름은 다르지만 검증 로직은 완전히 동일, STORY "파일" 절).
import type { DeliveryAddressErrors } from '../shop/deliveryAddressValidation'

export interface AddressFormValue {
  recipient: string
  phone: string
  zipcode: string
  roadAddress: string
  detailAddress: string
  entranceMethod: string
  deliveryMemo: string
  isDefault: boolean
}

export interface AddressFormProps {
  mode: 'add' | 'edit'
  value: AddressFormValue
  onChange: (value: AddressFormValue) => void
  errors: DeliveryAddressErrors
  /** 신규 등록 폼에서만 의미 있음 — 회원 최초 배송지면 서버가 `isDefault` 값과 무관하게 기본으로
   * 강제한다(INF-MBR-008 "비즈니스 규칙"). 체크박스를 disabled+checked로 보여줘 사용자가 끌 수 없음을
   * 미리 알린다. */
  isFirstAddress: boolean
  onOpenZipcodeSearch: () => void
  onSave: () => void
  onCancel: () => void
  saving?: boolean
}

const fieldStyle: React.CSSProperties = { display: 'flex', flexDirection: 'column', gap: 4, marginBottom: 12 }
const inputStyle: React.CSSProperties = { border: '1px solid #d5d8dd', borderRadius: 5, padding: '7px 10px', fontSize: 13 }
const labelStyle: React.CSSProperties = { fontSize: 12.5, color: '#666' }
const errorStyle: React.CSSProperties = { fontSize: 11.5, color: '#912d2b' }

export function AddressForm({
  mode, value, onChange, errors, isFirstAddress, onOpenZipcodeSearch, onSave, onCancel, saving,
}: AddressFormProps) {
  const disabled = !!saving
  const set = (patch: Partial<AddressFormValue>) => onChange({ ...value, ...patch })

  return (
    <form aria-label={mode === 'add' ? '배송지 추가' : '배송지 수정'}
          onSubmit={e => { e.preventDefault(); if (!disabled) onSave() }}
          style={{ maxWidth: 420 }}>
      <div style={fieldStyle}>
        <label htmlFor="address-recipient" style={labelStyle}>수령인</label>
        <input id="address-recipient" value={value.recipient} onChange={e => set({ recipient: e.target.value })}
               aria-invalid={!!errors.recipientName} style={inputStyle} />
        {errors.recipientName && <span role="alert" style={errorStyle}>{errors.recipientName}</span>}
      </div>

      <div style={fieldStyle}>
        <label htmlFor="address-phone" style={labelStyle}>연락처</label>
        <input id="address-phone" value={value.phone} onChange={e => set({ phone: e.target.value })}
               aria-invalid={!!errors.phone} style={inputStyle} />
        {errors.phone && <span role="alert" style={errorStyle}>{errors.phone}</span>}
      </div>

      <div style={fieldStyle}>
        <label htmlFor="address-zipcode" style={labelStyle}>우편번호</label>
        <div style={{ display: 'flex', gap: 8 }}>
          <input id="address-zipcode" value={value.zipcode} readOnly aria-invalid={!!errors.zipcode}
                 style={{ ...inputStyle, flex: '0 0 120px', background: '#f7f8fa' }} />
          <button type="button" onClick={onOpenZipcodeSearch}
                  style={{ border: '1px solid #0b4ea2', borderRadius: 5, background: '#fff', color: '#0b4ea2',
                           fontSize: 12.5, padding: '0 12px', cursor: 'pointer' }}>
            우편번호 찾기
          </button>
        </div>
        <input aria-label="도로명주소" value={value.roadAddress} readOnly style={{ ...inputStyle, background: '#f7f8fa' }} />
        {errors.zipcode && <span role="alert" style={errorStyle}>{errors.zipcode}</span>}
      </div>

      <div style={fieldStyle}>
        <label htmlFor="address-detail" style={labelStyle}>상세주소</label>
        <input id="address-detail" value={value.detailAddress} onChange={e => set({ detailAddress: e.target.value })}
               aria-invalid={!!errors.detailAddress} style={inputStyle} />
        {errors.detailAddress && <span role="alert" style={errorStyle}>{errors.detailAddress}</span>}
      </div>

      <div style={fieldStyle}>
        <label htmlFor="address-entrance" style={labelStyle}>공동현관 출입 방법</label>
        <input id="address-entrance" value={value.entranceMethod} onChange={e => set({ entranceMethod: e.target.value })}
               style={inputStyle} />
      </div>

      <div style={fieldStyle}>
        <label htmlFor="address-memo" style={labelStyle}>배송 요청사항</label>
        <input id="address-memo" value={value.deliveryMemo} onChange={e => set({ deliveryMemo: e.target.value })}
               style={inputStyle} />
      </div>

      {/* 수정은 isDefault를 다루지 않는다(INF-MBR-008 — 기본 지정은 전용 API로만) — 등록일 때만 보여준다. */}
      {mode === 'add' && (
        <div style={{ ...fieldStyle, flexDirection: 'row', alignItems: 'center', gap: 8 }}>
          <input id="address-is-default" type="checkbox" checked={isFirstAddress ? true : value.isDefault}
                 disabled={isFirstAddress}
                 onChange={e => set({ isDefault: e.target.checked })} />
          <label htmlFor="address-is-default" style={labelStyle}>기본 배송지로 설정</label>
        </div>
      )}
      {mode === 'add' && isFirstAddress && (
        <p style={{ fontSize: 11.5, color: '#888', marginTop: -6, marginBottom: 12 }}>
          첫 배송지는 자동으로 기본 배송지로 설정됩니다
        </p>
      )}

      <div style={{ display: 'flex', gap: 8, marginTop: 6 }}>
        <button type="submit" disabled={disabled}
                style={{ border: 0, borderRadius: 5, padding: '9px 16px', fontSize: 13, fontWeight: 700,
                         color: '#fff', background: disabled ? '#7ea3cf' : '#0b4ea2',
                         cursor: disabled ? 'not-allowed' : 'pointer' }}>
          {saving ? '저장 중…' : '저장'}
        </button>
        <button type="button" onClick={onCancel} disabled={disabled}
                style={{ border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#666',
                         fontSize: 13, padding: '9px 16px', cursor: disabled ? 'not-allowed' : 'pointer' }}>
          취소
        </button>
      </div>
    </form>
  )
}
