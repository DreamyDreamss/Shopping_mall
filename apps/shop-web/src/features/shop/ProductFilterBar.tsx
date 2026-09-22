// SR-303 — 상품 목록(검색·필터) 화면의 필터·정렬 바. fetch 없음(규칙 `web-fetch-only-in-api`) — 전부
// controlled 컴포넌트로 부모(`ProductListPage`)가 상태를 소유한다.
import type { SortKey } from './productListFilters'

export interface ProductFilterBarProps {
  inStockOnly: boolean
  onInStockOnlyChange: (value: boolean) => void
  priceMin: string
  onPriceMinChange: (value: string) => void
  priceMax: string
  onPriceMaxChange: (value: string) => void
  sortKey: SortKey
  onSortKeyChange: (value: SortKey) => void
  /** 정렬/페이지 적용 **전**(가격 필터까지 적용한) 배열 길이(STORY "데이터" 절). */
  resultCount: number
}

const box: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, padding: '6px 8px', fontSize: 13, width: 90,
}

const SORT_OPTIONS: { value: SortKey; label: string }[] = [
  { value: 'recommend', label: '추천순' },
  { value: 'priceAsc', label: '낮은 가격순' },
  { value: 'priceDesc', label: '높은 가격순' },
]

export function ProductFilterBar({
  inStockOnly, onInStockOnlyChange, priceMin, onPriceMinChange, priceMax, onPriceMaxChange,
  sortKey, onSortKeyChange, resultCount,
}: ProductFilterBarProps) {
  return (
    <div style={{
      display: 'flex', flexWrap: 'wrap', alignItems: 'flex-end', gap: 14,
      padding: '10px 0', borderBottom: '1px solid #eef0f2', fontFamily: 'system-ui, sans-serif',
    }}>
      <label style={{ fontSize: 13, color: '#333', display: 'flex', alignItems: 'center', gap: 6 }}>
        <input type="checkbox" checked={inStockOnly}
               onChange={e => onInStockOnlyChange(e.target.checked)} />
        재고 있는 상품만
      </label>

      <label style={{ fontSize: 12, color: '#555' }}>최소 가격<br />
        <input style={box} type="number" inputMode="numeric" value={priceMin}
               onChange={e => onPriceMinChange(e.target.value)} placeholder="0" />
      </label>
      <label style={{ fontSize: 12, color: '#555' }}>최대 가격<br />
        <input style={box} type="number" inputMode="numeric" value={priceMax}
               onChange={e => onPriceMaxChange(e.target.value)} placeholder="제한 없음" />
      </label>

      <label style={{ fontSize: 12, color: '#555' }}>정렬<br />
        <select style={box} value={sortKey}
                onChange={e => onSortKeyChange(e.target.value as SortKey)}>
          {SORT_OPTIONS.map(o => <option key={o.value} value={o.value}>{o.label}</option>)}
        </select>
      </label>

      <span style={{ marginLeft: 'auto', fontSize: 12.5, color: '#666' }}>총 {resultCount}개</span>
    </div>
  )
}
