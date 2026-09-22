// SR-305 — 장바구니(`/shop/cart`) 한 줄(썸네일·상품명·단가·수량 스테퍼·라인합계·체크박스·삭제).
//
// 이 부품은 fetch/자격 판정을 하지 않는다(규칙 `web-fetch-only-in-api`, `ProductInfoPanel`과 동일
// 원칙) — 수량 클램프(1~재고) 판정과 실제 PATCH 호출은 전부 부모(`CartPage`)가 하고, 이 부품은
// 클램프되지 않은 "의도한" 값을 그대로 `onQtyChange`로 올려보낸다. 표시되는 `qty`는 항상 서버가
// 마지막으로 확인해 준 값이다(STORY "사람 수정" 절 — 낙관적으로 먼저 올리지 않는다, PATCH 진행
// 중/실패 시에도 이 값이 곧 "직전 확인된 수량"이라 별도 되돌리기 로직이 필요 없다).
//
// 재작업(round 2, QA FAIL 필수1) — 수량 직접입력란은 원래 `value={qty}`(서버 확인값)로 완전 제어되고
// `onChange` 매 키 입력마다 부모로 커밋을 올려보내 즉시 PATCH가 나갔다. 재고 20·현재 2인 줄에서
// "15"를 치려던 사용자는 첫 글자 "1" 시점에 PATCH{qty:1}이 확정되고(동시에 입력란이 `pending`으로
// 비활성화돼) 둘째 글자를 받지 못했다. 이제 입력 중에는 로컬 draft 문자열만 바꾸고, blur 또는 Enter
// 에서만 부모로 커밋한다(스테퍼 ± 버튼은 기존대로 즉시 커밋). 전송 중에도 입력란을 disabled로 만들지
// 않는다(사람 수정 지시) — 진행 표시("변경 중…")만 하고 입력은 계속 받는다.
//
// 재작업(round 2, 실측 수정) — draft를 `useEffect`로 `qty` prop과 동기화하는 첫 구현은 커밋 직후
// draft를 곧장 "직전 값"으로 되돌려 놓고, PATCH 응답이 와서 `qty` prop이 바뀔 때 effect가 다시
// 동기화해 주길 기다렸다. 그런데 effect는 커밋 완료 후 별도 렌더 사이클에서 뒤늦게 실행되는 값이라
// (passive effect), 그 사이 값(직전 값)을 사용자가 보게 되는 순간이 생기고 테스트에서도 이 타이밍이
// 불안정하게 관찰됐다(실측: 수량 증가 버튼만 눌러도 표시값이 새 `qty`로 갱신되지 않는 경우 발생).
// 대신 draft를 "편집 중 여부"를 겸하는 `string | null`로 두고 렌더마다 파생시킨다 — 편집 중이 아니면
// (`qtyDraft === null`) 항상 `qty` prop을 그대로 보여주므로 effect 없이도 매 렌더에서 최신값이다.
//
// 재작업(round 3, QA CONCERNS 권고1) — 위 재설계가 입력란에 `onFocus={() => setQtyDraft(String(qty))}`를
// 남겨 뒀는데, 이 seed가 그 순간의 `qty`를 draft에 박제해 `commitQtyDraft`의 `qtyDraft === null` 가드를
// 절대 발동하지 않는 사문으로 만들었다. PATCH가 in-flight인 동안 입력란에 focus만 했다가(타이핑 없음)
// 응답이 도착해 `qty`가 바뀐 뒤 blur하면, draft는 focus 시점의 옛 `qty`를 그대로 들고 있어 그 값으로
// 다시 PATCH를 커밋했다 — 방금 서버가 확인해 준 수량이 소리 없이 되돌아가는 결함(round 1 필수1과 같은
// 실패 계열). 표시는 이미 `qtyDraft ?? String(qty)` 파생이라 seed 없이도 편집 전엔 항상 최신 `qty`가
// 보이므로 onFocus를 없앤다 — draft는 오직 `onChange`에서만 채워진다(실제 타이핑이 있었을 때만).
import { useState } from 'react'

export interface CartLineItemProps {
  sku: string
  productName: string
  imageUrl: string | null
  price: number
  /** 서버가 마지막으로 확인해 준 수량(정본) — 낙관적 갱신 없음. */
  qty: number
  lineTotal: number
  stockQty: number
  selected: boolean
  onToggleSelected: (sku: string) => void
  /** 클램프 전의 "의도한" 값 — 클램프·PATCH 호출 판단은 부모 몫. */
  onQtyChange: (sku: string, requestedQty: number) => void
  onDelete: (sku: string) => void
  /** PATCH/DELETE 진행 중 — true면 조작 버튼을 비활성화한다. */
  pending?: boolean
  /** 클라이언트 클램프 즉시 안내("1 미만 불가"/"최대 수량", API 호출 없음). */
  warningMessage?: string | null
  /** 서버가 PATCH를 거부한 사유(400/409) — 서버 원문 그대로(STORY "순서·보안" 2). */
  errorMessage?: string | null
}

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

const thumbBoxStyle: React.CSSProperties = {
  width: 64, height: 64, background: '#f2f3f5', borderRadius: 6, overflow: 'hidden', flex: '0 0 auto',
  display: 'flex', alignItems: 'center', justifyContent: 'center',
}

export function CartLineItem({
  sku, productName, imageUrl, price, qty, lineTotal, stockQty, selected,
  onToggleSelected, onQtyChange, onDelete, pending, warningMessage, errorMessage,
}: CartLineItemProps) {
  const [imgLoadFailed, setImgLoadFailed] = useState(false)
  const showImage = !!imageUrl && !imgLoadFailed
  const initial = productName.trim().charAt(0) || '?'
  const disabled = !!pending

  // 수량 직접입력 draft(재작업 필수1) — `null`이면 "편집 중이 아님"을 뜻하고, 그때는 항상 `qty`
  // prop을 그대로 보여준다(effect 없이 매 렌더에서 파생시킨다 — PATCH 응답으로 `qty`가 바뀌면 다음
  // 렌더에 곧바로 반영된다). 편집 중에는 사용자가 입력한 문자열을 그대로 보여준다.
  const [qtyDraft, setQtyDraft] = useState<string | null>(null)
  const displayedQty = qtyDraft ?? String(qty)

  const commitQtyDraft = () => {
    if (qtyDraft === null) return // 편집 없이 blur(예: focus만 됐다 벗어남) — 커밋할 것이 없다
    const raw = qtyDraft.trim()
    const parsed = Number(raw)
    setQtyDraft(null) // 편집 종료 — 다시 `qty` prop을 보여준다(낙관적 갱신 없음 원칙 유지)
    if (raw === '' || !Number.isFinite(parsed)) return // 빈 값/비숫자는 직전 확인 수량으로 되돌린다(사람 수정 지시)
    if (Math.trunc(parsed) === qty) return // 직전 확인 수량과 같으면 PATCH를 호출하지 않는다
    onQtyChange(sku, Math.trunc(parsed))
  }

  return (
    <div role="listitem" aria-label={productName}
         style={{ display: 'flex', gap: 12, alignItems: 'flex-start', padding: '14px 4px', borderBottom: '1px solid #eef0f2' }}>
      <input type="checkbox" aria-label={`${productName} 선택`} checked={selected} disabled={disabled}
             onChange={() => onToggleSelected(sku)} style={{ marginTop: 22 }} />
      <div style={thumbBoxStyle}>
        {showImage ? (
          <img src={imageUrl!} alt={productName} onError={() => setImgLoadFailed(true)}
               style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
        ) : (
          <span aria-label={`${productName} 대표이미지 없음`} style={{ fontSize: 22, fontWeight: 700, color: '#aab0b8' }}>
            {initial}
          </span>
        )}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13.5, color: '#222', marginBottom: 4 }}>{productName}</div>
        <div style={{ fontSize: 12.5, color: '#666' }}>{won(price)}</div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 10 }}>
          <button type="button" aria-label={`${productName} 수량 감소`} disabled={disabled}
                  onClick={() => onQtyChange(sku, qty - 1)}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 4, background: '#fff', width: 26, height: 26, cursor: disabled ? 'not-allowed' : 'pointer' }}>
            −
          </button>
          <input aria-label={`${productName} 수량`} type="number" value={displayedQty} min={1} max={stockQty}
                 onChange={e => setQtyDraft(e.target.value)}
                 onBlur={commitQtyDraft}
                 onKeyDown={e => { if (e.key === 'Enter') { e.preventDefault(); e.currentTarget.blur() } }}
                 style={{ width: 48, textAlign: 'center', border: '1px solid #d5d8dd', borderRadius: 4, padding: '3px 4px' }} />
          <button type="button" aria-label={`${productName} 수량 증가`} disabled={disabled}
                  onClick={() => onQtyChange(sku, qty + 1)}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 4, background: '#fff', width: 26, height: 26, cursor: disabled ? 'not-allowed' : 'pointer' }}>
            +
          </button>
          {pending && <span role="status" style={{ fontSize: 11.5, color: '#999' }}>변경 중…</span>}
        </div>

        {warningMessage && (
          <div role="note" style={{ marginTop: 6, fontSize: 11.5, color: '#a06a00' }}>{warningMessage}</div>
        )}
        {errorMessage && (
          <div role="alert" style={{ marginTop: 6, fontSize: 11.5, color: '#912d2b' }}>{errorMessage}</div>
        )}
      </div>
      <div style={{ textAlign: 'right', display: 'flex', flexDirection: 'column', alignItems: 'flex-end', gap: 8 }}>
        <div style={{ fontSize: 14, fontWeight: 700 }}>{won(lineTotal)}</div>
        <button type="button" aria-label={`${productName} 삭제`} onClick={() => onDelete(sku)} disabled={disabled}
                style={{ border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#666',
                         fontSize: 12, padding: '4px 10px', cursor: disabled ? 'not-allowed' : 'pointer' }}>
          삭제
        </button>
      </div>
    </div>
  )
}
