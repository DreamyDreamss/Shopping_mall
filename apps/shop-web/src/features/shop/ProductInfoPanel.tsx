// SR-304 — 상품 상세 정보 영역(상품명·판매가·정가·할인율·재고상태·수량 선택·장바구니 담기/바로 구매).
// 할인율·취소선 표시 조건은 `discountRate.ts`를 그대로 재사용한다(재계산 금지 — 실패 사례집 SR-306
// #1 부동소수점 사례 대조).
//
// 이 부품은 fetch/자격 판정을 하지 않는다(규칙 `web-fetch-only-in-api`, STORY "폴백·우회 경로의
// 자격 판정" 절) — 담기/구매 가능 여부(`disabledReason`)와 진행 상태(`addStatus`)는 전부 부모
// (`ProductDetailPage`)가 세션·재고·서버 응답을 보고 판정해 내려준 값을 그대로 표시만 한다.
// 연타 방지(동기 ref 잠금)도 부모(`inFlightRef`/`notifyInFlightRef`) 몫이다 — 이 부품은
// `addStatus==='pending'`/`notifyStatus==='requested'`면 버튼을 비활성화하는 표시만 담당한다
// (프레임워크 실행 모델 함정 절 — 사용자 더블클릭 대응).
//
// SR-353 — `product.soldOut`(서버 계산값, INF-ORD-009)이 품절 판정의 유일한 근거다(더 이상
// `stockQty<=0`을 이 부품에서 계산하지 않는다). 품절이면 장바구니/바로구매 버튼 대신 재입고 알림 신청
// 버튼 1개만 보인다(`notifyStatus`·`onNotifyRequest`는 부모가 로그인 판정 뒤에만 전환한다, STORY
// "순서·보안" 절).
import { hasListPriceDiscount, calcDiscountRate } from './discountRate'
import type { ProductDetail } from '../../types'

export type AddStatus = 'idle' | 'pending' | 'success' | 'error'
export type AddAction = 'cart' | 'buyNow'
export type NotifyStatus = 'idle' | 'requested'

export interface ProductInfoPanelProps {
  product: ProductDetail
  qty: number
  /** 수량 변경 요청 — 클램프(1~재고)는 호출자(`ProductDetailPage`)가 한다(상태관리 방식 절). */
  onQtyChange: (qty: number) => void
  onAddToCart: () => void
  onBuyNow: () => void
  addStatus: AddStatus
  addErrorMessage?: string | null
  /** 마지막으로 눌린 버튼 — 성공 알림 문구를 가르는 데만 쓴다. */
  lastAction?: AddAction | null
  /** 담기/구매를 막아야 하는 사유(로그인 필요). 품절은 더 이상 이 사유로 표시하지 않는다(버튼 자체가
   * 재입고 알림 신청으로 바뀐다) — null/undefined면 막지 않는다. */
  disabledReason?: string | null
  /** 재입고 알림 신청 상태 — 'requested'면 버튼이 "신청됨"으로 비활성화된다. */
  notifyStatus: NotifyStatus
  onNotifyRequest: () => void
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
  notifyStatus, onNotifyRequest,
}: ProductInfoPanelProps) {
  const soldOut = product.soldOut
  const hasDiscount = hasListPriceDiscount(product.price, product.listPrice)
  const discountRate = calcDiscountRate(product.price, product.listPrice)
  const pending = addStatus === 'pending'
  const actionsDisabled = !!disabledReason || pending
  const notifyRequested = notifyStatus === 'requested'
  // 계약(STORY "계약" 절): soldOut이면 "품절"(문구 불변), 아니면 stockQty!=null && <=10일 때만
  // "남은 수량 N개" — 조건 미충족 시 값이 아니라 엘리먼트 자체를 숨긴다(null이면 라벨 렌더 안 함).
  const stockLabel = soldOut
    ? '품절'
    : product.stockQty != null && product.stockQty <= 10
      ? `남은 수량 ${product.stockQty}개`
      : null

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

      {stockLabel !== null && (
        <div aria-label="재고 상태" style={{ marginTop: 12, fontSize: 13, color: soldOut ? '#912d2b' : '#333' }}>
          {stockLabel}
        </div>
      )}

      {!soldOut && (
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 14 }}>
          <label htmlFor="product-detail-qty" style={{ fontSize: 13, color: '#666' }}>수량</label>
          <button type="button" aria-label="수량 감소" onClick={() => onQtyChange(qty - 1)}
                  disabled={actionsDisabled || qty <= 1}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 4, background: '#fff', width: 28, height: 28, cursor: 'pointer' }}>
            −
          </button>
          <input id="product-detail-qty" aria-label="수량" type="number" value={qty} min={1} max={product.stockQty ?? undefined}
                 disabled={actionsDisabled}
                 onChange={e => onQtyChange(Number(e.target.value))}
                 style={{ width: 56, textAlign: 'center', border: '1px solid #d5d8dd', borderRadius: 4, padding: '4px 6px' }} />
          {/* SR-353 재작업(권고 3) — stockQty=null인데 soldOut=false인 방어 조합에서 이 버튼의
              비활성 기준이 `ProductDetailPage.handleQtyChange`의 클램프 상한(`stockQty ?? 1`)과
              일치해야 한다. 이전에는 `?? Infinity`라 클램프가 qty를 1에 묶어 둬도 버튼은 영원히
              활성으로 남아 눌러도 아무 일도 안 일어나는 죽은 버튼이 됐다(QA CONCERNS #3). */}
          <button type="button" aria-label="수량 증가" onClick={() => onQtyChange(qty + 1)}
                  disabled={actionsDisabled || qty >= (product.stockQty ?? 1)}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 4, background: '#fff', width: 28, height: 28, cursor: 'pointer' }}>
            +
          </button>
        </div>
      )}

      {!soldOut && disabledReason && (
        <div role="note" style={{ marginTop: 10, fontSize: 12.5, color: '#912d2b' }}>{disabledReason}</div>
      )}

      {soldOut ? (
        <div style={{ display: 'flex', gap: 10, marginTop: 16 }}>
          <button type="button" onClick={onNotifyRequest} disabled={notifyRequested}
                  style={notifyRequested ? { ...primaryButtonStyle, ...disabledButtonStyle } : primaryButtonStyle}>
            {notifyRequested ? '신청됨' : '재입고 알림 신청'}
          </button>
        </div>
      ) : (
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
      )}

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
