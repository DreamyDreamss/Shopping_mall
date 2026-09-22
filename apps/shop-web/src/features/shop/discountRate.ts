// SR-306.1 round2(재작업) — 할인율 계산을 한 곳에 모은다. ProductCard·스토리·테스트가 전부 이 파일의
// 함수만 쓴다(계산 로직을 각자 복제하지 않는다 — 사람 지시).
// round1 QA FAIL: `Math.floor((1 - price/listPrice) * 100)`처럼 나눗셈을 먼저 한 뒤 100을 곱하고
// 내림하면, 부동소수점 나눗셈의 오차가 정수 경계 바로 아래로 떨어지는 비율이 생긴다(예:
// 90,000/100,000 = 실제 10.0%인데 9%로 표시 — 1~99% 구간에서 17개 비율이 이 클래스의 오차를 낸다).
// 고친 식은 먼저 (listPrice - price) * 100을 정수로 계산한 뒤 listPrice로 나눈다(정수 연산).

/** 취소선(정가) 표시 여부 — listPrice가 있고 판매가보다 클 때만 참이다. */
export function hasListPriceDiscount(price: number, listPrice: number | null): listPrice is number {
  return listPrice != null && listPrice > price
}

/**
 * 할인율(내림 정수 %)을 계산한다. listPrice가 없거나 price 이상이면 할인이 아니므로 계산 자체를
 * 하지 않고 0을 반환한다(호출부는 0을 "배지 숨김"으로 취급 — 확정답변 8).
 */
export function calcDiscountRate(price: number, listPrice: number | null): number {
  if (!hasListPriceDiscount(price, listPrice)) return 0
  return Math.floor(((listPrice - price) * 100) / listPrice)
}
