// SR-305 — 장바구니(`/shop/cart`)·주문서(`/shop/order`) 합계 계산(순수 함수, 서버 호출 없음).
// `CartPage`/`OrderPage`가 모두 이 함수만 쓴다(계산 로직을 화면에 복제하지 않는다, `discountRate.ts`
// 관례). 상품금액은 항상 서버가 돌려준 `lineTotal`(price×qty를 서버가 이미 계산한 값)을 그대로
// 합산한다 — 클라이언트가 price×qty를 재계산해 서버 응답을 덮지 않는다(STORY "사람 수정" 절, "수량·
// 금액은 서버 응답을 정본으로 다시 그린다").
//
// ⚠ 배송비 규칙(STORY "데이터" 절 — 명세·용어집·기존 코드 어디에도 근거 없음, 사람 확인 필요로
// 임시 확정한 값): 선택 항목이 1개 이상이면 고정 요금, 없으면 0원(무료배송 기준금액 없음). 정책
// 미확정 — SR-308에서 확정한다(사람 수정 STEP 3-0 게이트 회신). 값을 바꾸거나 이 가정 자체를
// 거부하려면 이 상수 하나만 고치면 된다.
export const SHIPPING_FEE_FLAT = 3000

export interface CartTotalsLine {
  sku: string
  lineTotal: number
}

export interface CartTotals {
  productAmount: number
  shippingFee: number
  payableAmount: number
}

/**
 * 선택된 품목(`selectedSkus`) 기준 상품금액/배송비/결제예정금액을 계산한다. `OrderPage`(전체 장바구니
 * 체크아웃)는 전체 sku 집합을 넘겨 호출한다.
 */
export function calcCartTotals(lines: CartTotalsLine[], selectedSkus: ReadonlySet<string>): CartTotals {
  const selectedLines = lines.filter(line => selectedSkus.has(line.sku))
  const productAmount = selectedLines.reduce((sum, line) => sum + line.lineTotal, 0)
  const shippingFee = selectedLines.length > 0 ? SHIPPING_FEE_FLAT : 0
  return { productAmount, shippingFee, payableAmount: productAmount + shippingFee }
}

/**
 * 재작업(round 2, QA FAIL 필수2) — 개별 라인 데이터 없이 상품금액 하나만 아는 상황(체크아웃 완료
 * 응답의 `totalAmount` — `CartService.checkout`이 돌려주는 값은 상품금액만이고 배송비 개념이 서버에
 * 없다)에도 `calcCartTotals`와 같은 배송비 규칙을 적용한다. 주문서(`OrderPage`)와 주문완료
 * (`OrderCompleteNotice`)가 같은 3줄(상품금액/배송비/결제금액)을 같은 숫자로 말하게 하기 위함이다 —
 * 두 화면이 서로 다른 계산을 하다가 배송비만큼(3,000원) 어긋났던 결함의 재발 방지.
 */
export function calcTotalsFromProductAmount(productAmount: number): CartTotals {
  const shippingFee = productAmount > 0 ? SHIPPING_FEE_FLAT : 0
  return { productAmount, shippingFee, payableAmount: productAmount + shippingFee }
}
