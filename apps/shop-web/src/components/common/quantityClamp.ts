// SR-310 — 수량 경계 보정 순수함수(QuantityStepper와 분리해 discountRate.ts와 같은 방식으로
// 단위테스트한다). 범위 밖 값·비정상 값은 항상 안전한 경계값으로 떨어진다.
export function clampQuantity(raw: number, min: number, max: number): number {
  if (!Number.isFinite(raw)) return min
  const truncated = Math.trunc(raw)
  if (truncated < min) return min
  if (truncated > max) return max
  return truncated
}
