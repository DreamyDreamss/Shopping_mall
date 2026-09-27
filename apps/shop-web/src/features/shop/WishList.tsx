// SR-356 — 내 찜 목록(`/shop/mypage/wishes`) 목록. `AddressList.tsx`와 동일 구조 — 0건이면 내부에서
// 빈 상태("찜한 상품이 없습니다" + [상품 보러 가기], `CartEmptyState`와 동일 톤)를 렌더하고, N건이면
// `role="list"` 안에 행을 인라인으로 그린다(별도 `WishListItem.tsx` 파일로 안 쪼갬 — `AddressList`가
// 행을 인라인으로 그리는 것과 동일 관례).
//
// 이 부품은 fetch를 하지 않는다(규칙 `web-fetch-only-in-api`) — 목록 조회·찜 해제 실행은 전부 부모
// (`WishListPage`)가 한다. 상품명 클릭 시 상세로 이동하는 `<a>`는 AC에 명시는 없으나 목록에서 상품으로
// 못 돌아가면 화면이 막다른 길이 되는 최소 내비게이션이다(STORY "범위 밖" 절).
import type { WishItem } from '../../types'

export interface WishListProps {
  items: WishItem[]
  onRemove: (sku: string) => void
  /** 해제가 진행 중인 sku — 그 행의 버튼만 비활성화한다(`AddressList.pendingAddressId`와 동일 패턴). */
  pendingSku: string | null
}

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

const rowStyle: React.CSSProperties = {
  display: 'flex', alignItems: 'center', gap: 12, padding: '14px 4px', borderBottom: '1px solid #eef0f2',
}
const removeButtonStyle: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#333', fontSize: 12,
  padding: '4px 10px', cursor: 'pointer', flexShrink: 0,
}
const soldOutBadgeStyle: React.CSSProperties = {
  display: 'inline-block', background: '#eef0f2', color: '#666', borderRadius: 4, fontSize: 10.5,
  fontWeight: 700, padding: '1px 6px', marginLeft: 8,
}

export function WishList({ items, onRemove, pendingSku }: WishListProps) {
  if (items.length === 0) {
    return (
      <div style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>
        <p style={{ marginBottom: 14 }}>찜한 상품이 없습니다</p>
        <a href="#/shop/products"
           style={{ display: 'inline-block', border: '1px solid #0b4ea2', borderRadius: 5, color: '#0b4ea2',
                    fontSize: 13, fontWeight: 700, padding: '8px 18px', textDecoration: 'none' }}>
          상품 보러 가기
        </a>
      </div>
    )
  }

  return (
    <div role="list" aria-label="찜 목록">
      {items.map(it => {
        const pending = pendingSku === it.sku
        return (
          <div role="listitem" key={it.sku} style={rowStyle}>
            <div style={{ flex: 1, minWidth: 0 }}>
              <a href={`#/shop/products/${encodeURIComponent(it.sku)}`}
                 style={{ fontSize: 13.5, color: '#222', textDecoration: 'none' }}>
                {it.productName}
              </a>
              {it.soldOut && <span style={soldOutBadgeStyle}>품절</span>}
              <div style={{ fontSize: 13, color: '#333', marginTop: 4, fontWeight: 700 }}>{won(it.price)}</div>
            </div>
            {pending && <span role="status" style={{ fontSize: 11.5, color: '#999' }}>처리 중…</span>}
            <button type="button" aria-label={`${it.productName} 찜 해제`} disabled={pending}
                    onClick={() => onRemove(it.sku)} style={removeButtonStyle}>
              찜 해제
            </button>
          </div>
        )
      })}
    </div>
  )
}
