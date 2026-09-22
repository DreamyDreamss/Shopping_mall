// SR-302 — 카테고리 숏컷. 카테고리 데이터·필터 API 자체가 없어(`PRODUCTS`엔 카테고리 컬럼도 없음)
// 정적 표시만 한다(범위 밖 — 확정 답변 scope_freeze).
// SR-303 재작업 지시(round1) — 항목 클릭을 감지할 수 있도록 옵셔널 `onSelect` 1개만 추가한다
// (`ProductCard`의 role="button"/tabIndex/onKeyDown 관례를 그대로 따름). 레이아웃·스타일은 건드리지
// 않는다. 클릭된 라벨을 어떻게 쓸지는 호출부(`ShopHomePage`) 몫 — round1은 라벨을 keyword로 넘겼다가
// 시드 데이터와 안 맞아 round2에서 되돌렸다(호출부만 수정, 이 컴포넌트는 무관).
import type { CategoryShortcut } from './shopStatic'

export interface CategoryShortcutsProps {
  categories: CategoryShortcut[]
  onSelect?: (label: string) => void
}

export function CategoryShortcuts({ categories, onSelect }: CategoryShortcutsProps) {
  if (!categories.length) return null
  return (
    <nav aria-label="카테고리 바로가기" style={{ display: 'flex', gap: 18, overflowX: 'auto', padding: '14px 0' }}>
      {categories.map(c => (
        <div key={c.id} role={onSelect ? 'button' : undefined} tabIndex={onSelect ? 0 : undefined}
             onClick={onSelect ? () => onSelect(c.label) : undefined}
             onKeyDown={onSelect ? e => { if (e.key === 'Enter' || e.key === ' ') onSelect(c.label) } : undefined}
             style={{
               display: 'flex', flexDirection: 'column', alignItems: 'center', minWidth: 60,
               color: '#333', fontSize: 12, gap: 4, cursor: onSelect ? 'pointer' : 'default',
             }}>
          <span aria-hidden="true" style={{ fontSize: 24 }}>{c.icon}</span>
          <span>{c.label}</span>
        </div>
      ))}
    </nav>
  )
}
