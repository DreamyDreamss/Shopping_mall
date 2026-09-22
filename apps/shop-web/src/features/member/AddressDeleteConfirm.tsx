// SR-235 — 마이페이지 배송지 관리 삭제 확인 다이얼로그. `ZipcodeSearchModal`과 동일한 오버레이+패널
// (`role="dialog"`) 스타일로 새로 만든다 — 기존에 재사용할 만한 confirm 다이얼로그가 없다(실측 확인,
// STORY "파일" 절). 이 부품은 fetch를 하지 않는다(규칙 `web-fetch-only-in-api`) — 실제 삭제 호출은
// [삭제] 확인 클릭 시 부모(`MyAddressesPage`)가 한다.
export interface AddressDeleteConfirmProps {
  open: boolean
  recipient: string
  roadAddress: string
  /** 삭제 대상이 기본 배송지면 승계 안내 문구를 함께 보여준다. */
  isDefault: boolean
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

export function AddressDeleteConfirm({
  open, recipient, roadAddress, isDefault, onCancel, onConfirm, confirming,
}: AddressDeleteConfirmProps) {
  if (!open) return null
  const disabled = !!confirming

  return (
    <div style={overlayStyle} onClick={onCancel}>
      <div role="dialog" aria-label="배송지 삭제 확인" style={panelStyle} onClick={e => e.stopPropagation()}>
        <h2 style={{ fontSize: 15, margin: 0 }}>이 배송지를 삭제할까요?</h2>
        <div style={{ fontSize: 12.5, color: '#666' }}>{recipient} · {roadAddress}</div>
        {isDefault && (
          <div role="note" style={{ fontSize: 12, color: '#a06a00' }}>
            삭제하면 다음 배송지가 자동으로 기본이 됩니다
          </div>
        )}
        <div style={{ display: 'flex', gap: 8, justifyContent: 'flex-end', marginTop: 4 }}>
          <button type="button" onClick={onCancel} disabled={disabled}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#666',
                           fontSize: 13, padding: '7px 14px', cursor: disabled ? 'not-allowed' : 'pointer' }}>
            취소
          </button>
          <button type="button" onClick={onConfirm} disabled={disabled}
                  style={{ border: 0, borderRadius: 5, background: disabled ? '#d79a99' : '#912d2b', color: '#fff',
                           fontSize: 13, fontWeight: 700, padding: '7px 14px', cursor: disabled ? 'not-allowed' : 'pointer' }}>
            {confirming ? '삭제 중…' : '삭제'}
          </button>
        </div>
      </div>
    </div>
  )
}
