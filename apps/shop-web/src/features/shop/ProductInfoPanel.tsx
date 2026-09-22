// SR-304 — 상품 상세 정보 영역(상품명·판매가·정가·할인율·재고상태·수량 선택·장바구니 담기/바로 구매).
// 할인율·취소선 표시 조건은 `discountRate.ts`를 그대로 재사용한다(재계산 금지 — 실패 사례집 SR-306
// #1 부동소수점 사례 대조).
//
// 이 부품은 fetch/자격 판정을 하지 않는다(규칙 `web-fetch-only-in-api`, STORY "폴백·우회 경로의
// 자격 판정" 절) — 담기/구매 가능 여부(`disabledReason`)와 진행 상태(`addStatus`)는 전부 부모
// (`ProductDetailPage`)가 세션·재고·서버 응답을 보고 판정해 내려준 값을 그대로 표시만 한다.
// 연타 방지(동기 ref 잠금)도 부모(`inFlightRef`) 몫이다 — 이 부품은 `addStatus==='pending'`이면
// 버튼을 비활성화하는 표시만 담당한다(프레임워크 실행 모델 함정 절 — 사용자 더블클릭 대응).
import { hasListPriceDiscount, calcDiscountRate } from './discountRate'
import type { Product } from '../../types'

export type AddStatus = 'idle' | 'pending' | 'success' | 'error'
export type AddAction = 'cart' | 'buyNow'

export interface ProductInfoPanelProps {
  product: Product
  qty: number
  /** 수량 변경 요청 — 클램프(1~재고)는 호출자(`ProductDetailPage`)가 한다(상태관리 방식 절). */
  onQtyChange: (qty: number) => void
  onAddToCart: () => void
  onBuyNow: () => void
  addStatus: AddStatus
  addErrorMessage?: string | null
  /** 마지막으로 눌린 버튼 — 성공 알림 문구를 가르는 데만 쓴다. */
  lastAction?: AddAction | null
  /** 담기/구매를 막아야 하는 사유(품절·로그인 필요). null/undefined면 막지 않는다. */
  disabledReason?: string | null
}

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

const primaryButtonStyle: React.CSSProperties = {
  border: 0, borderRadius: 5, background: '#0b4ea2', color: '#fff', fontSize: 14, fontWeight: 700,
  padding: '10px 20px', cursor: 'pointer', flex: 1,
}

const secondaryButtonStyle: React.CSSProperties = {
  border: '1px solid #0b4ea2', borderRadius: 5, background: '#fff', color: '#0b4ea2', fontSize: 14,
  fontWeight: 700, padding: '10px 20px', cursor: 'pointer', flex: 1,
}

const disabledButtonStyle: React.CSSProperties = {
  border: '1px solid #d5d8dd', background: '#eef0f2', color: '#999', cursor: 'not-allowed',
}

export function ProductInfoPanel({
  product, qty, onQtyChange, onAddToCart, onBuyNow, addStatus, addErrorMessage, lastAction, disabledReason,
}: ProductInfoPanelProps) {
  const soldOut = product.stockQty <= 0
  const hasDiscount = hasListPriceDiscount(product.price, product.listPrice)
  const discountRate = calcDiscountRate(product.price, product.listPrice)
  const pending = addStatus === 'pending'
  const actionsDisabled = !!disabledReason || pending

  return (
    <section aria-label="상품 정보">
      <h1 style={{ fontSize: 19, marginBottom: 10 }}>{product.productName}</h1>

      <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, flexWrap: 'wrap' }}>
        {discountRate > 0 && <span style={{ color: '#e0392c', fontSize: 18, fontWeight: 800 }}>{discountRate}%</span>}
        <span style={{ fontSize: 20, fontWeight: 800 }}>{won(product.price)}</span>
      </div>
      {hasDiscount && (
        <div style={{ fontSize: 13, color: '#999', textDecoration: 'line-through', marginTop: 2 }}>
          {won(product.listPrice!)}
        </div>
      )}

      <div aria-label="재고 상태" style={{ marginTop: 12, fontSize: 13, color: soldOut ? '#912d2b' : '#333' }}>
        {soldOut ? '품절' : `재고 ${product.stockQty}개`}
      </div>

      {!soldOut && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 14 }}>
          <label htmlFor="product-detail-qty" style={{ fontSize: 13, color: '#666' }}>수량</label>
          <button type="button" aria-label="수량 감소" onClick={() => onQtyChange(qty - 1)}
                  disabled={actionsDisabled || qty <= 1}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 4, background: '#fff', width: 28, height: 28, cursor: 'pointer' }}>
            −
          </button>
          <input id="product-detail-qty" aria-label="수량" type="number" value={qty} min={1} max={product.stockQty}
                 disabled={actionsDisabled}
                 onChange={e => onQtyChange(Number(e.target.value))}
                 style={{ width: 56, textAlign: 'center', border: '1px solid #d5d8dd', borderRadius: 4, padding: '4px 6px' }} />
          <button type="button" aria-label="수량 증가" onClick={() => onQtyChange(qty + 1)}
                  disabled={actionsDisabled || qty >= product.stockQty}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 4, background: '#fff', width: 28, height: 28, cursor: 'pointer' }}>
            +
          </button>
        </div>
      )}

      {disabledReason && (
        <div role="note" style={{ marginTop: 10, fontSize: 12.5, color: '#912d2b' }}>{disabledReason}</div>
      )}

      <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
        <button type="button" onClick={onAddToCart} disabled={actionsDisabled}
                style={actionsDisabled ? { ...secondaryButtonStyle, ...disabledButtonStyle } : secondaryButtonStyle}>
          {pending ? '담는 중…' : '장바구니 담기'}
        </button>
        <button type="button" onClick={onBuyNow} disabled={actionsDisabled}
                style={actionsDisabled ? { ...primaryButtonStyle, ...disabledButtonStyle } : primaryButtonStyle}>
          {pending ? '담는 중…' : '바로 구매'}
        </button>
      </div>

      {addStatus === 'error' && addErrorMessage && (
        <div role="alert" style={{ marginTop: 12, fontSize: 12.5, color: '#912d2b' }}>{addErrorMessage}</div>
      )}

      {addStatus === 'success' && (
        <div role="status" style={{ marginTop: 12, fontSize: 12.5, color: '#136b2f', display: 'flex', alignItems: 'center', gap: 8 }}>
          {/* 사람 수정(STEP 3-0) — "바로 구매"는 결제 연동 없이 담기와 동일한 API를 호출하고, 이동 없이
              그 자리에서 알림+[장바구니 보기](비활성)만 보여준다(SR-305 라우트가 아직 없어 이동시키지
              않는다). "장바구니 담기"는 이 문맥이 필요 없어 문구만 다르다(라벨 구분 유지). */}
          <span>
            {lastAction === 'buyNow' ? '바로 구매 대신 장바구니에 담았습니다' : '장바구니에 담았습니다'}
          </span>
          {lastAction === 'buyNow' && (
            <button type="button" disabled title="준비 중"
                    style={{ border: '1px solid #d5d8dd', borderRadius: 5, background: '#eef0f2', color: '#999',
                             fontSize: 12, padding: '4px 10px', cursor: 'not-allowed' }}>
              장바구니 보기
            </button>
          )}
        </div>
      )}
    </section>
  )
}
