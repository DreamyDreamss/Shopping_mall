// SR-306.1 round2(재작업) — round1 QA FAIL 필수수정 1·2 — 할인율 정수 연산 표 기반 테스트.
// QA가 지목한 17개 비율(정가 100,000 기준 7·8·9·10·11·19·20·21·22·29·32·33·44·45·57·58·66%)은
// `Math.floor((1 - price/listPrice) * 100)`(round1 구현) 아래서 1%p 낮게 나오던 값이다 — 이 표는
// 그 17개 전부를 포함해, 고친 정수 연산(`Math.floor((listPrice - price) * 100 / listPrice)`)이
// 1~99% 전 구간에서 정확한지 고정한다.
//
// 고치기 전 실패 재현(사람 지시) — 이 테스트를 만든 시점에 `discountRate.ts`가 아직 round1의
// `Math.floor((1 - price/listPrice) * 100)` 식이었을 때 실행한 결과, 90,000/100,000(실제 10%) 케이스가
// 정확히 `9`를 반환해 `expected: 10`과 불일치로 실패했다(Dev 기록에 실행 로그 원문 기록).
import { describe, expect, test } from '@jest/globals'
import { calcDiscountRate, hasListPriceDiscount } from './discountRate'

describe('calcDiscountRate — 정수 연산(먼저 곱하고 나중에 나눈다)', () => {
  // linked_tc: TC-FUNC-shop-006
  // QA가 지목한 17개 비율 — round1 부동소수점 구현(`Math.floor((1 - price/listPrice) * 100)`)이
  // 전부 1%p 낮게(6·7·8·9·10·18·19·20·21·31·32·43·44·65) 반환했거나, 계획서 공식
  // (`Math.floor((listPrice - price) / listPrice * 100)`)이 낮게(28·56·57) 반환했던 값들이다.
  const qaFlaggedRatios: { price: number; listPrice: number; expected: number }[] = [
    { price: 93000, listPrice: 100000, expected: 7 },
    { price: 92000, listPrice: 100000, expected: 8 },
    { price: 91000, listPrice: 100000, expected: 9 },
    { price: 90000, listPrice: 100000, expected: 10 },
    { price: 89000, listPrice: 100000, expected: 11 },
    { price: 81000, listPrice: 100000, expected: 19 },
    { price: 80000, listPrice: 100000, expected: 20 },
    { price: 79000, listPrice: 100000, expected: 21 },
    { price: 78000, listPrice: 100000, expected: 22 },
    { price: 71000, listPrice: 100000, expected: 29 },
    { price: 68000, listPrice: 100000, expected: 32 },
    { price: 67000, listPrice: 100000, expected: 33 },
    { price: 56000, listPrice: 100000, expected: 44 },
    { price: 55000, listPrice: 100000, expected: 45 },
    { price: 43000, listPrice: 100000, expected: 57 },
    { price: 42000, listPrice: 100000, expected: 58 },
    { price: 34000, listPrice: 100000, expected: 66 },
  ]

  test.each(qaFlaggedRatios)(
    'price=$price listPrice=$listPrice → $expected%(QA 지목 비율)',
    ({ price, listPrice, expected }) => {
      expect(calcDiscountRate(price, listPrice)).toBe(expected)
    },
  )

  // linked_tc: TC-FUNC-shop-006
  // QA 리포트 본문이 직접 든 예시 — 실측 로그와 1:1 대조용(위 표와 값이 겹쳐도 남긴다).
  test('90,000/100,000 = 10.0% (QA 실측: round1은 9%를 반환)', () => {
    expect(calcDiscountRate(90000, 100000)).toBe(10)
  })
  test('80,000/100,000 = 20.0% (QA 실측: round1은 19%를 반환)', () => {
    expect(calcDiscountRate(80000, 100000)).toBe(20)
  })
  test('2,000/2,500 = 20.0% (QA 실측: round1은 19%를 반환)', () => {
    expect(calcDiscountRate(2000, 2500)).toBe(20)
  })

  // linked_tc: TC-FUNC-shop-006
  // 사람 코멘트 — 내림이 필요한 케이스(정확히 나눠떨어지지 않는 비율)도 함께 고정한다.
  test('35,000/42,000 = 16.67%… → 내림 16%', () => {
    expect(calcDiscountRate(35000, 42000)).toBe(16)
  })

  // linked_tc: TC-FUNC-shop-007
  // 경계값 — 1%와 99%.
  test('1% 경계: 99,000/100,000', () => {
    expect(calcDiscountRate(99000, 100000)).toBe(1)
  })
  test('99% 경계: 1,000/100,000', () => {
    expect(calcDiscountRate(1000, 100000)).toBe(99)
  })

  // linked_tc: TC-FUNC-shop-008
  // 0%·가드 — listPrice가 없거나 price 이상이면 계산 자체를 하지 않고 0(배지 숨김 신호)을 반환한다.
  test('listPrice === price → 0%(할인 아님)', () => {
    expect(calcDiscountRate(450000, 450000)).toBe(0)
  })
  test('listPrice < price → 0%(정가가 판매가보다 낮음, 비정상 데이터 방어)', () => {
    expect(calcDiscountRate(50000, 40000)).toBe(0)
  })
  test('listPrice === null → 0%(정가 없음)', () => {
    expect(calcDiscountRate(129000, null)).toBe(0)
  })
})

describe('hasListPriceDiscount — 취소선 표시 조건(calcDiscountRate와 같은 가드)', () => {
  // linked_tc: TC-FUNC-shop-008
  test('listPrice > price면 참', () => {
    expect(hasListPriceDiscount(390000, 450000)).toBe(true)
  })
  test('listPrice === price면 거짓', () => {
    expect(hasListPriceDiscount(450000, 450000)).toBe(false)
  })
  test('listPrice가 null이면 거짓', () => {
    expect(hasListPriceDiscount(129000, null)).toBe(false)
  })
})
