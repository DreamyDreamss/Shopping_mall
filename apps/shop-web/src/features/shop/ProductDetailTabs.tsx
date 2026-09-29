// SR-304 — 상품 상세 탭(상세정보/구매정보/상품평/상품문의). `Product`에 설명·배송정책 필드가 없어
// 상세정보/구매정보는 고정 정적 문구로 빈 값을 명시한다(확정 문답 밖 해석, STEP 3-0 게이트에서 승인됨
// — STORY "데이터" 절). "준비 중"은 확정 답변상 상품평/상품문의 전용이다.
import { useEffect, useState } from 'react'

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
  /** SR-361 — 지정 시에만 내부 탭 state를 이 값으로 동기화하는 controlled-override. 미지정이면
   * 완전 비제어로 기존과 100% 동일(기존 스토리 4개는 이 prop을 안 써 무영향). "리뷰 전체 보기" 클릭으로
   * 부모가 탭을 강제로 여는 유일한 방법 — 컴포넌트는 내부 state를 계속 갖되 외부 값이 오면 따라간다. */
  activeTab?: ProductDetailTabKey
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

export function ProductDetailTabs({ initialTab = 'detail', reviewContent, onTabChange, activeTab }: ProductDetailTabsProps) {
  const [tab, setTab] = useState<ProductDetailTabKey>(initialTab)

  // 훅은 최상위에서만 부른다(must) — 조건 분기는 effect 내부에만 둔다(early return 아님). 의존배열은
  // 반드시 [activeTab](외부 prop)만 넣는다 — 여기에 tab(내부 state)까지 넣으면 자기 자신을 계속
  // 재동기화하는 루프가 생긴다(STORY "프레임워크 실행 모델 함정" 절).
  useEffect(() => {
    if (activeTab !== undefined) setTab(activeTab)
  }, [activeTab])

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
