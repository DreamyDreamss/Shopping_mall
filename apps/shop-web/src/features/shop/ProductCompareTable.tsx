// SR-360 — 상품 비교 화면(`/shop/compare`) 비교 표. `RecentlyViewedSection.tsx`의 controlled-props
// 패턴(status 분기, 자체 fetch 없음 — 규칙 `web-fetch-only-in-api`)을 그대로 재사용한다. 목록 조회·빼기
// 실행은 전부 부모(`ProductComparePage`)가 한다.
//
// 행=속성, 열=상품인 전치 구조라 `OrderTable.tsx`의 실제 `<table>` 관례를 따른다(여기 회원
// 컴포넌트들은 `role="list"` div였지만 이 화면은 표가 의미상 더 맞는다, STORY "파일" 절). 열마다 로컬
// 상태(이미지 로드 실패 등)를 갖지 않으므로 `.map()` 안에서 훅을 호출할 필요가 없다 — 훅은 최상위에서만
// 부른다(규칙 `typescript-react-hooks-rules`)는 이 컴포넌트에 애초에 적용될 여지가 없다.
import type { ProductCompareItem } from '../../types'

export type ProductCompareStatus = 'loading' | 'ready' | 'error'

export interface ProductCompareTableProps {
  status: ProductCompareStatus
  items: ProductCompareItem[]
  onRemove: (sku: string) => void
  /** 빼기가 진행 중인 sku — 그 열의 버튼만 비활성화한다(`WishList.pendingSku`와 동일 패턴). */
  pendingSku: string | null
  onRetry: () => void
}

const won = (n: number) => n.toLocaleString('ko-KR') + '원'

const alertBoxStyle: React.CSSProperties = {
  border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b', borderRadius: 6, padding: '16px 18px',
  display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start', fontSize: 12.5,
}
const cellStyle: React.CSSProperties = { padding: '8px 10px', borderBottom: '1px solid #eef0f2' }
const headCellStyle: React.CSSProperties = {
  ...cellStyle, background: '#f6f7f9', fontWeight: 600, textAlign: 'left', verticalAlign: 'top',
}
const removeButtonStyle: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', color: '#333', fontSize: 12,
  fontWeight: 400, padding: '4px 10px', cursor: 'pointer',
}

export function ProductCompareTable({ status, items, onRemove, pendingSku, onRetry }: ProductCompareTableProps) {
  if (status === 'loading') {
    return <div role="status" style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>불러오는 중…</div>
  }

  if (status === 'error') {
    // AC 고정 문구 그대로 — 서버 상세는 절대 이어붙이지 않는다(STORY "순서·보안" 5).
    return (
      <div role="alert" style={alertBoxStyle}>
        <span>비교함을 불러오지 못했습니다</span>
        <button type="button" onClick={onRetry}
                style={{ border: '1px solid #912d2b', borderRadius: 5, background: '#fff', color: '#912d2b',
                         fontSize: 12.5, padding: '5px 12px', cursor: 'pointer' }}>
          다시 시도
        </button>
      </div>
    )
  }

  if (items.length === 0) {
    return (
      <div style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>
        <p style={{ marginBottom: 14 }}>비교함이 비어 있습니다 — 상품 상세에서 [비교함 담기]를 누르세요</p>
        <a href="#/shop/products"
           style={{ display: 'inline-block', border: '1px solid #0b4ea2', borderRadius: 5, color: '#0b4ea2',
                    fontSize: 13, fontWeight: 700, padding: '8px 18px', textDecoration: 'none' }}>
          상품 보러 가기
        </a>
      </div>
    )
  }

  return (
    <table style={{ width: '100%', borderCollapse: 'collapse', fontSize: 14 }}>
      <thead>
        <tr>
          <th scope="col" style={headCellStyle}></th>
          {items.map(it => (
            <th key={it.sku} scope="col" style={headCellStyle}>
              <a href={`#/shop/products/${encodeURIComponent(it.sku)}`}
                 style={{ display: 'block', marginBottom: 6, color: '#222', textDecoration: 'none' }}>
                {it.productName}
              </a>
              <button type="button" aria-label={`${it.productName} 비교함에서 빼기`}
                      disabled={pendingSku === it.sku} onClick={() => onRemove(it.sku)}
                      style={removeButtonStyle}>
                빼기
              </button>
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        <tr>
          <th scope="row" style={headCellStyle}>가격</th>
          {items.map(it => <td key={it.sku} style={cellStyle}>{won(it.price)}</td>)}
        </tr>
        <tr>
          <th scope="row" style={headCellStyle}>정가</th>
          {/* listPrice null 표기는 AC 미명시 추론(평점 표기 톤과의 일관성) — STORY "범위 밖" 절. */}
          {items.map(it => <td key={it.sku} style={cellStyle}>{it.listPrice != null ? won(it.listPrice) : '—'}</td>)}
        </tr>
        <tr>
          <th scope="row" style={headCellStyle}>평점</th>
          {items.map(it => <td key={it.sku} style={cellStyle}>{it.avgRating != null ? it.avgRating.toFixed(1) : '—'}</td>)}
        </tr>
        <tr>
          <th scope="row" style={headCellStyle}>리뷰 수</th>
          {items.map(it => <td key={it.sku} style={cellStyle}>{it.reviewCount}건</td>)}
        </tr>
        <tr>
          <th scope="row" style={headCellStyle}>재고</th>
          {/* stockQty===0을 재계산하지 않고 서버가 이미 계산한 soldOut을 그대로 쓴다(`WishItem.soldOut`
              소비 관례와 동일). */}
          {items.map(it => <td key={it.sku} style={cellStyle}>{it.soldOut ? '품절' : `${it.stockQty}개`}</td>)}
        </tr>
      </tbody>
    </table>
  )
}
