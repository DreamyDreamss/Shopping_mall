// SR-235 — 마이페이지 배송지 관리(`/shop/mypage/addresses`) 목록. 이미 최근 사용 순으로 정렬된 배열을
// 그대로 받아 렌더할 뿐 프론트에서 재정렬하지 않는다(정렬은 서버 `selectList` 몫, STORY "파일" 절).
// 이 부품은 fetch를 하지 않는다(규칙 `web-fetch-only-in-api`) — 목록 조회·등록/수정/삭제/기본설정
// 실행은 전부 부모(`MyAddressesPage`)가 한다.
import type { MemberAddress } from '../../types'

const MAX_ADDRESSES = 10

export interface AddressListProps {
  addresses: MemberAddress[]
  onAdd: () => void
  onEdit: (addressId: number) => void
  onDelete: (addressId: number) => void
  onSetDefault: (addressId: number) => void
  /** 기본설정/삭제가 진행 중인 행의 addressId — 그 행의 버튼을 비활성화해 연타를 시각적으로도 막는다. */
  pendingAddressId: number | null
}

const headerRow: React.CSSProperties = { display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 14 }
const rowStyle: React.CSSProperties = {
  display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', gap: 12,
  padding: '14px 4px', borderBottom: '1px solid #eef0f2',
}
const badgeStyle: React.CSSProperties = {
  display: 'inline-block', background: '#0b4ea2', color: '#fff', borderRadius: 4,
  fontSize: 10.5, fontWeight: 700, padding: '1px 6px', marginBottom: 6,
}
const buttonStyle: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#333',
  fontSize: 12, padding: '4px 10px', cursor: 'pointer',
}
const primaryButtonStyle: React.CSSProperties = {
  border: 0, borderRadius: 5, background: '#0b4ea2', color: '#fff', fontSize: 13,
  fontWeight: 700, padding: '7px 14px', cursor: 'pointer',
}

export function AddressList({ addresses, onAdd, onEdit, onDelete, onSetDefault, pendingAddressId }: AddressListProps) {
  const atLimit = addresses.length >= MAX_ADDRESSES

  return (
    <section aria-label="배송지 관리">
      <div style={headerRow}>
        <h1 style={{ fontSize: 18, margin: 0 }}>배송지 관리 ({addresses.length})</h1>
        <button type="button" onClick={onAdd} disabled={atLimit}
                style={{ ...primaryButtonStyle, ...(atLimit ? { background: '#c8ccd2', cursor: 'not-allowed' } : {}) }}>
          추가
        </button>
      </div>

      {atLimit && (
        <div role="note" style={{ fontSize: 12, color: '#a06a00', marginBottom: 12 }}>
          배송지는 최대 10개까지 등록할 수 있습니다
        </div>
      )}

      {addresses.length === 0 ? (
        <div style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>
          등록된 배송지가 없습니다
        </div>
      ) : (
        <div role="list" aria-label="배송지 목록">
          {addresses.map(a => {
            const pending = pendingAddressId === a.addressId
            return (
              <div role="listitem" key={a.addressId} style={rowStyle}>
                <div style={{ flex: 1, minWidth: 0 }}>
                  {a.isDefault === 'Y' && <span style={badgeStyle}>기본</span>}
                  <div style={{ fontSize: 13.5, color: '#222' }}>{a.recipient} · {a.phone}</div>
                  <div style={{ fontSize: 12.5, color: '#666', marginTop: 2 }}>
                    {a.roadAddress} {a.detailAddress}
                  </div>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 6, flexShrink: 0 }}>
                  {pending && <span role="status" style={{ fontSize: 11.5, color: '#999' }}>처리 중…</span>}
                  {a.isDefault !== 'Y' && (
                    <button type="button" aria-label={`${a.recipient} 기본으로 설정`} disabled={pending}
                            onClick={() => onSetDefault(a.addressId)} style={buttonStyle}>
                      기본으로 설정
                    </button>
                  )}
                  <button type="button" aria-label={`${a.recipient} 수정`} disabled={pending}
                          onClick={() => onEdit(a.addressId)} style={buttonStyle}>
                    수정
                  </button>
                  <button type="button" aria-label={`${a.recipient} 삭제`} disabled={pending}
                          onClick={() => onDelete(a.addressId)} style={buttonStyle}>
                    삭제
                  </button>
                </div>
              </div>
            )
          })}
        </div>
      )}
    </section>
  )
}
