// SR-351 — 상품평 삭제 확인 다이얼로그. `AddressDeleteConfirm.tsx`와 동일한 오버레이+패널 스타일
// (role="dialog")로 새로 만든다(그 부품은 배송지 전용 문구가 하드코딩돼 있어 재사용 대신 같은 스타일을
// 복제 — 그 부품 자체 주석의 선례와 동일 판단). 문구는 STEP 3-0 "사람 수정"으로 확정됐다(더 이상
// [미상]이 아니다). fetch 없음(규칙 web-fetch-only-in-api) — 실제 삭제 호출은 [삭제] 클릭 시 부모가 한다.
export interface ReviewDeleteConfirmProps {
  open: boolean
  onCancel: () => void
  onConfirm: () => void
  /** DELETE 진행 중 — true면 [삭제] 버튼을 잠근다(연타 방지 시각 확인). */
  confirming?: boolean
}

const overlayStyle: React.CSSProperties = {
  position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)',
  display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 50,
}

const panelStyle: React.CSSProperties = {
  background: '#fff', borderRadius: 8, padding: 20, width: 340,
  display: 'flex', flexDirection: 'column', gap: 12,
}

export function ReviewDeleteConfirm({ open, onCancel, onConfirm, confirming }: ReviewDeleteConfirmProps) {
  if (!open) return null
  const disabled = !!confirming

  return (
    <div style={overlayStyle} onClick={onCancel}>
      <div role="dialog" aria-label="상품평 삭제 확인" style={panelStyle} onClick={e => e.stopPropagation()}>
        <h2 style={{ fontSize: 15, margin: 0 }}>상품평 삭제</h2>
        <div style={{ fontSize: 12.5, color: '#666' }}>
          이 상품평을 삭제할까요? 삭제하면 목록과 평균 별점에서 빠집니다.
        </div>
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 4 }}>
          <button type="button" onClick={onCancel} disabled={disabled}
                  style={{
                    border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#666',
                    fontSize: 13, padding: '7px 14px', cursor: disabled ? 'not-allowed' : 'pointer',
                  }}>
            취소
          </button>
          <button type="button" onClick={onConfirm} disabled={disabled}
                  style={{
                    border: 0, borderRadius: 5, background: disabled ? '#d79a99' : '#912d2b', color: '#fff',
                    fontSize: 13, fontWeight: 700, padding: '7px 14px', cursor: disabled ? 'not-allowed' : 'pointer',
                  }}>
            {confirming ? '삭제 중…' : '삭제'}
          </button>
        </div>
      </div>
    </div>
  )
}
