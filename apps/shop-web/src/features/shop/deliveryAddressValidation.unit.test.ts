// SR-305 — deliveryAddressValidation.ts: 필드별 개별 누락 케이스가 그 필드만의 오류를 반환하는지.
import { describe, expect, test } from '@jest/globals'
import { hasDeliveryAddressErrors, validateDeliveryAddress, type DeliveryAddressInput } from './deliveryAddressValidation'

function validInput(overrides: Partial<DeliveryAddressInput> = {}): DeliveryAddressInput {
  return {
    recipientName: '홍길동',
    phone: '010-1234-5678',
    zipcode: '06236',
    roadAddress: '서울 강남구 테헤란로 1',
    detailAddress: '101동 202호',
    ...overrides,
  }
}

describe('validateDeliveryAddress', () => {
  test('전부 채워짐 — 오류 없음', () => {
    expect(validateDeliveryAddress(validInput())).toEqual({})
    expect(hasDeliveryAddressErrors(validateDeliveryAddress(validInput()))).toBe(false)
  })

  test('수령인 누락 — recipientName만 오류', () => {
    const errors = validateDeliveryAddress(validInput({ recipientName: '' }))
    expect(errors).toEqual({ recipientName: '수령인을 입력해 주세요' })
  })

  test('수령인 공백만 — recipientName만 오류(trim)', () => {
    const errors = validateDeliveryAddress(validInput({ recipientName: '   ' }))
    expect(errors).toEqual({ recipientName: '수령인을 입력해 주세요' })
  })

  test('연락처 누락 — phone만 오류', () => {
    const errors = validateDeliveryAddress(validInput({ phone: '' }))
    expect(errors).toEqual({ phone: '연락처를 입력해 주세요' })
  })

  test('우편번호 미선택(zipcode·roadAddress 둘 다 빈값) — zipcode 하나로 묶여 오류', () => {
    const errors = validateDeliveryAddress(validInput({ zipcode: '', roadAddress: '' }))
    expect(errors).toEqual({ zipcode: '우편번호 찾기로 주소를 선택해 주세요' })
  })

  test('도로명주소만 빈값(비정상 상태 방어) — zipcode 오류로 잡힌다', () => {
    const errors = validateDeliveryAddress(validInput({ roadAddress: '' }))
    expect(errors).toEqual({ zipcode: '우편번호 찾기로 주소를 선택해 주세요' })
  })

  test('상세주소 누락 — detailAddress만 오류', () => {
    const errors = validateDeliveryAddress(validInput({ detailAddress: '' }))
    expect(errors).toEqual({ detailAddress: '상세주소를 입력해 주세요' })
  })

  test('전부 누락 — 4개 필드 전부 오류(zipcode는 하나로 묶임)', () => {
    const errors = validateDeliveryAddress({ recipientName: '', phone: '', zipcode: '', roadAddress: '', detailAddress: '' })
    expect(Object.keys(errors).sort()).toEqual(['detailAddress', 'phone', 'recipientName', 'zipcode'])
    expect(hasDeliveryAddressErrors(errors)).toBe(true)
  })
})
