// SR-304 — 상품 상세 탭(상세정보/구매정보/상품평/상품문의). `Product`에 설명·배송정책 필드가 없어
// 상세정보/구매정보는 고정 정적 문구로 빈 값을 명시한다(확정 문답 밖 해석, STEP 3-0 게이트에서 승인됨
// — STORY "데이터" 절). "준비 중"은 확정 답변상 상품평/상품문의 전용이다.
import { useState } from 'react'

export type ProductDetailTabKey = 'detail' | 'purchase' | 'review' | 'inquiry'

export interface ProductDetailTabsProps {
  /** 스토리북에서 특정 탭 상태를 바로 보여주기 위한 초기값. 기본은 '상세정보'. */
  initialTab?: ProductDetailTabKey
}

const TABS: { key: ProductDetailTabKey; label: string }[] = [
  { key: 'detail', label: '상세정보' },
  { key: 'purchase', label: '구매정보' },
  { key: 'review', label: '상품평' },
  { key: 'inquiry', label: '상품문의' },
]

const TAB_CONTENT: Record<ProductDetailTabKey, string> = {
  detail: '등록된 상세 설명이 없습니다.',
  purchase: '배송·교환·환불 안내는 상품정보제공고시를 참고하세요.',
  review: '준비 중입니다.',
  inquiry: '준비 중입니다.',
}

const tabButtonStyle = (active: boolean): React.CSSProperties => ({
  border: 0, borderBottom: active ? '2px solid #0b4ea2' : '2px solid transparent',
  background: 'none', padding: '10px 14px', fontSize: 13.5, cursor: 'pointer',
  color: active ? '#0b4ea2' : '#666', fontWeight: active ? 700 : 400,
})

export function ProductDetailTabs({ initialTab = 'detail' }: ProductDetailTabsProps) {
  const [tab, setTab] = useState<ProductDetailTabKey>(initialTab)

  return (
    <section aria-label="상품 상세 탭">
      <div role="tablist" aria-label="상품 상세 탭 목록" style={{ display: 'flex', borderBottom: '1px solid #eef0f2' }}>
        {TABS.map(t => (
          <button key={t.key} type="button" role="tab" aria-selected={tab === t.key}
                  onClick={() => setTab(t.key)} style={tabButtonStyle(tab === t.key)}>
            {t.label}
          </button>
        ))}
      </div>
      <div role="tabpanel" style={{ padding: '18px 6px', color: '#555', fontSize: 13.5, minHeight: 60 }}>
        {TAB_CONTENT[tab]}
      </div>
    </section>
  )
}
