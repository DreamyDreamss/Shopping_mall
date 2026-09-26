// LAB-105.1 — 게이트 회신(사람 수정 ②) 반영: 4개 택배사 URL + 미등록 코드/null 단언.
import { describe, expect, test } from '@jest/globals'
import { carrierTrackingUrl } from './carrierTracking'

describe('carrierTrackingUrl', () => {
  test("CJ대한통운 — carrierCode='CJ'는 invoiceNo를 담은 URL을 반환한다", () => {
    const url = carrierTrackingUrl('CJ', 'INV-1')
    expect(url).not.toBeNull()
    expect(url).toContain('INV-1')
  })

  test("한진택배 — carrierCode='HANJIN'은 invoiceNo를 담은 URL을 반환한다", () => {
    const url = carrierTrackingUrl('HANJIN', 'INV-1')
    expect(url).not.toBeNull()
    expect(url).toContain('INV-1')
  })

  test("롯데택배 — carrierCode='LOTTE'는 invoiceNo를 담은 URL을 반환한다", () => {
    const url = carrierTrackingUrl('LOTTE', 'INV-1')
    expect(url).not.toBeNull()
    expect(url).toContain('INV-1')
  })

  test("우체국택배 — carrierCode='EPOST'는 invoiceNo를 담은 URL을 반환한다", () => {
    const url = carrierTrackingUrl('EPOST', 'INV-1')
    expect(url).not.toBeNull()
    expect(url).toContain('INV-1')
  })

  test('carrierCode가 null이면 null(링크 숨김)', () => {
    expect(carrierTrackingUrl(null, 'INV-1')).toBeNull()
  })

  test('invoiceNo가 null이면 null(링크 숨김)', () => {
    expect(carrierTrackingUrl('CJ', null)).toBeNull()
  })

  test("미등록 코드('UNKNOWN')는 null(링크 숨김)", () => {
    expect(carrierTrackingUrl('UNKNOWN', 'INV-1')).toBeNull()
  })

  // round2 재작업 지시 4(low/security) — 객체 리터럴 인덱싱의 프로토타입 상속 키 취약점 회귀 가드.
  // `Object.hasOwn` 가드 전에는 이 코드가 `Object.prototype.toString`을 잡아 값을 반환했다.
  test("프로토타입 상속 키('toString')는 등록된 택배사가 아니라 null(링크 숨김)", () => {
    expect(carrierTrackingUrl('toString', 'INV-1')).toBeNull()
  })
})
