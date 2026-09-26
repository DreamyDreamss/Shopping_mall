// SR-304 — 상품 상세 탭(상세정보/구매정보/상품평/상품문의). `Product`에 설명·배송정책 필드가 없어
// 상세정보/구매정보는 고정 정적 문구로 빈 값을 명시한다(확정 문답 밖 해석, STEP 3-0 게이트에서 승인됨
// — STORY "데이터" 절). "준비 중"은 확정 답변상 상품평/상품문의 전용이다.
import { useState } from 'react'

export type ProductDetailTabKey = 'detail' | 'purchase' | 'review' | 'inquiry'

export interface ProductDetailTabsProps {
  /** 스토리북에서 특정 탭 상태를 바로 보여주기 위한 초기값. 기본은 '상세정보'. */
  initialTab?: ProductDetailTabKey
  // SR-351 — 상품평 탭에 실제 콘텐츠를 꽂기 위한 옵션 prop. 미지정 시 기존 "준비 중입니다." 폴백과
  // 정확히 같아 기존 스토리(기본·상품평·상품문의·탭전환)는 이 prop 없이도 그대로 통과한다(회귀 없음,
  // 실측 근거 — 이 파일과 ProductDetailTabs.stories.tsx는 수정 대상이 아니다).
  reviewContent?: React.ReactNode
  /** 탭이 바뀔 때 부모에 알린다(옵션 — 미지정 시 옵셔널 체이닝으로 no-op, 기존 동작 불변). */
  onTabChange?: (tab: ProductDetailTabKey) => void
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

export function ProductDetailTabs({ initialTab = 'detail', reviewContent, onTabChange }: ProductDetailTabsProps) {
  const [tab, setTab] = useState<ProductDetailTabKey>(initialTab)

  const handleTabClick = (key: ProductDetailTabKey) => {
    setTab(key)
    onTabChange?.(key)
  }

  return (
    <section aria-label="상품 상세 탭">
      <div role="tablist" aria-label="상품 상세 탭 목록" style={{ display: 'flex', borderBottom: '1px solid #eef0f2' }}>
        {TABS.map(t => (
          <button key={t.key} type="button" role="tab" aria-selected={tab === t.key}
                  onClick={() => handleTabClick(t.key)} style={tabButtonStyle(tab === t.key)}>
            {t.label}
          </button>
        ))}
      </div>
      <div role="tabpanel" style={{ padding: '18px 6px', color: '#555', fontSize: 13.5, minHeight: 60 }}>
        {tab === 'review' ? (reviewContent ?? TAB_CONTENT.review) : TAB_CONTENT[tab]}
      </div>
    </section>
  )
}
