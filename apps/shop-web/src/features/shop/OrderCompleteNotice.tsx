// SR-305 — 주문서(`/shop/order`) 결제 성공 후 완료 상태(같은 라우트 안에서 전환, 별도 이동 없음).
// [주문 내역 보기]는 확정 답변("주문 완료 후 기존 주문 목록 화면으로 이어진다")대로 `/`(기존 주문
// 목록)로 이동한다 — 상세(`/orders/:orderNo`)가 아니다. `PasswordResetDoneStep.tsx`와 같은 이유로
// `<a href="#/...">`를 쓴다(HashRouter, Storybook Router 데코레이터 불필요).
//
// 재작업(round 2, QA FAIL 필수2) — 서버 checkout 응답의 `totalAmount`는 상품금액만이다
// (`CartService.checkout` — 배송비 개념이 서버에 없음). 이 화면이 `totalAmount`를 무라벨 총액처럼
// 그대로 보여주면, "상품금액+배송비"를 결제예정금액으로 보여주는 주문서(`OrderPage`)와 같은 흐름에서
// 3,000원 어긋난다. `cartTotals.ts`의 같은 배송비 규칙(`calcTotalsFromProductAmount`)을 적용해
// 주문서와 동일한 3줄(상품금액/배송비/결제금액) 구성으로 보여준다 — 배송비가 서버 정본이 아니라는
// 사실은 화면 문구가 아니라 UIS 스펙 본문에 남긴다(사람 수정 지시).
import { calcTotalsFromProductAmount } from './cartTotals'

export interface OrderCompleteNoticeProps {
  orderNo: string
  totalAmount: number
  itemCount: number
}

const won = (n: number) => n.toLocaleString('ko-KR') + '원'
const rowStyle: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', fontSize: 13, padding: '4px 0' }

export function OrderCompleteNotice({ orderNo, totalAmount, itemCount }: OrderCompleteNoticeProps) {
  const totals = calcTotalsFromProductAmount(totalAmount)

  return (
    <div role="status" style={{ padding: '40px 16px', textAlign: 'center' }}>
      <p style={{ fontSize: 15, fontWeight: 700, marginBottom: 8 }}>주문이 완료되었습니다</p>
      <p style={{ fontSize: 13, color: '#666', marginBottom: 4 }}>주문번호 {orderNo}</p>
      <p style={{ fontSize: 13, color: '#666', marginBottom: 12 }}>{itemCount}개 상품</p>

      <div role="group" aria-label="주문 금액" style={{ maxWidth: 260, margin: '0 auto 20px', textAlign: 'left' }}>
        <div style={rowStyle}><span>상품금액</span><span>{won(totals.productAmount)}</span></div>
        <div style={rowStyle}><span>배송비</span><span>{won(totals.shippingFee)}</span></div>
        <div style={{ ...rowStyle, fontWeight: 800, borderTop: '1px solid #eef0f2', marginTop: 6, paddingTop: 8 }}>
          <span>결제금액</span><span>{won(totals.payableAmount)}</span>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 10, justifyContent: 'center' }}>
        <a href="#/" style={{ border: '1px solid #0b4ea2', borderRadius: 5, color: '#0b4ea2',
                               fontSize: 13, fontWeight: 700, padding: '8px 16px', textDecoration: 'none' }}>
          주문 내역 보기
        </a>
        <a href="#/shop/products" style={{ border: '1px solid #d5d8dd', borderRadius: 5, color: '#333',
                                            fontSize: 13, padding: '8px 16px', textDecoration: 'none' }}>
          쇼핑 계속하기
        </a>
      </div>
    </div>
  )
}
