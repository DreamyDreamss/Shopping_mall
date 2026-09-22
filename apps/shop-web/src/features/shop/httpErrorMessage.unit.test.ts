// 재작업(round 2, QA FAIL 필수3) — 공용 필터가 4개 표시 지점 모두에서 같은 판정을 내리는지 순수
// 함수 단위로 고정한다.
import { describe, expect, test } from '@jest/globals'
import { GENERIC_ERROR_MESSAGE, looksLikeInternalDetail, toDisplayMessage } from './httpErrorMessage'
import { ApiError, OrderHttpError } from '../../api'

describe('looksLikeInternalDetail', () => {
  test('정상 사유 문구는 걸리지 않는다', () => {
    expect(looksLikeInternalDetail('재고 초과: 가용 2, 요청 3')).toBe(false)
    expect(looksLikeInternalDetail('장바구니가 비어 있습니다')).toBe(false)
  })

  test('자바 예외 클래스명·스택프레임·파일 경로는 걸린다', () => {
    expect(looksLikeInternalDetail('java.lang.NullPointerException: cannot invoke')).toBe(true)
    expect(looksLikeInternalDetail('at com.sl.order.CartService.checkout(CartService.java:154)')).toBe(true)
    expect(looksLikeInternalDetail('C:\\workspace\\shop-api\\src\\main\\java\\CartService.java')).toBe(true)
    expect(looksLikeInternalDetail('/main/java/com/sl/order/CartService.java')).toBe(true)
  })
})

describe('toDisplayMessage', () => {
  test('OrderHttpError의 정상 메시지는 그대로 노출한다', () => {
    expect(toDisplayMessage(new OrderHttpError(409, '재고 초과: 가용 2, 요청 3'))).toBe('재고 초과: 가용 2, 요청 3')
  })

  test('OrderHttpError라도 내부 상세로 보이면 일반 문구로 대체한다', () => {
    const e = new OrderHttpError(500, 'java.lang.RuntimeException: boom at com.sl.order.CartService.checkout(CartService.java:154)')
    expect(toDisplayMessage(e)).toBe(GENERIC_ERROR_MESSAGE)
  })

  test('OrderHttpError가 아니면(네트워크 오류 등) 일반 문구다', () => {
    expect(toDisplayMessage(new Error('network down'))).toBe(GENERIC_ERROR_MESSAGE)
  })

  // 재작업(round 3, QA CONCERNS 권고2) — 정규식 거부목록만으로는 계약 밖 5xx라도 "정상 문구처럼
  // 보이는" 메시지를 걸러내지 못했다(예: 그 정규식이 못 잡는 형태의 NPE 메시지). 상태코드 허용목록이
  // 주 방어선으로 승격됐는지를, 정규식이 절대 걸지 않을 평범한 문장으로 확인한다 — 이 테스트는
  // 상태코드 검사가 없던 이전 구현이면 실패해야 한다(메시지가 그대로 노출됨).
  test('계약 밖 상태(5xx)면 메시지가 정상 문구처럼 보여도 일반 문구로 대체한다(상태코드 허용목록이 주 방어선)', () => {
    const e = new OrderHttpError(500, '요청을 처리할 수 없습니다')
    expect(toDisplayMessage(e)).toBe(GENERIC_ERROR_MESSAGE)
  })

  // 계약에 없는 4xx(예: 401/403)도 허용목록 밖이면 원문을 보여주지 않는다 — 허용목록은 "4xx면 전부"가
  // 아니라 "이 SR의 계약표에 있는 상태만"이다.
  test('계약에 정의되지 않은 4xx도 허용목록 밖이면 일반 문구로 대체한다', () => {
    const e = new OrderHttpError(401, '인증이 필요합니다')
    expect(toDisplayMessage(e)).toBe(GENERIC_ERROR_MESSAGE)
  })

  // SR-235 재작업(round2, QA FAIL 필수2) — `MyAddressesPage`가 상태코드를 안 보는 자체
  // `toAddressErrorMessage`를 새로 정의했었다. 이 함수를 `ApiError`(`{code,message}` 계열)도 받도록
  // 넓혀 삭제했으므로, `ApiError`에도 같은 허용목록·내부상세 필터가 적용되는지 고정한다.
  test('ApiError({code,message}) 계열도 같은 상태코드 허용목록·내부상세 필터를 적용한다', () => {
    const allowed = new ApiError(409, { code: 'MBR-4201', message: '배송지는 최대 10개까지 등록할 수 있습니다' })
    expect(toDisplayMessage(allowed)).toBe('배송지는 최대 10개까지 등록할 수 있습니다')

    const outOfContract = new ApiError(500, { code: 'MBR-5000', message: '요청을 처리할 수 없습니다' })
    expect(toDisplayMessage(outOfContract)).toBe(GENERIC_ERROR_MESSAGE)

    const internalDetail = new ApiError(400, { code: 'MBR-4200', message: 'java.lang.NullPointerException: boom' })
    expect(toDisplayMessage(internalDetail)).toBe(GENERIC_ERROR_MESSAGE)
  })
})
