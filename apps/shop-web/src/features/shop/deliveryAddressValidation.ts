// SR-305 — 주문서(`/shop/order`) 배송지 필드 검증(순수 함수, 서버 호출 없음). [결제하기] 클릭 시
// `OrderPage`가 이 함수를 먼저 호출해 통과할 때만 `checkoutCart`를 부른다(STORY "순서·보안" 6).
// 우편번호/도로명주소는 `ZipcodeSearchModal` 선택으로만 채워지는 한 쌍이라(직접 타이핑 입력란이
// 아님, `DeliveryAddressForm` 읽기전용) 하나의 필드(zipcode) 오류로 묶어 안내한다.
export interface DeliveryAddressInput {
  recipientName: string
  phone: string
  zipcode: string
  roadAddress: string
  detailAddress: string
}

export type DeliveryAddressField = 'recipientName' | 'phone' | 'zipcode' | 'detailAddress'

export type DeliveryAddressErrors = Partial<Record<DeliveryAddressField, string>>

/** 필드별 필수값 검증 — 누락된 필드만 그 필드 키로 오류 문구를 반환한다(다른 필드로 새지 않는다). */
export function validateDeliveryAddress(input: DeliveryAddressInput): DeliveryAddressErrors {
  const errors: DeliveryAddressErrors = {}
  if (!input.recipientName.trim()) errors.recipientName = '수령인을 입력해 주세요'
  if (!input.phone.trim()) errors.phone = '연락처를 입력해 주세요'
  if (!input.zipcode.trim() || !input.roadAddress.trim()) errors.zipcode = '우편번호 찾기로 주소를 선택해 주세요'
  if (!input.detailAddress.trim()) errors.detailAddress = '상세주소를 입력해 주세요'
  return errors
}

export function hasDeliveryAddressErrors(errors: DeliveryAddressErrors): boolean {
  return Object.keys(errors).length > 0
}
