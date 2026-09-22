// SR-310 — 공통 Tabs(스티키 고정·스와이프 전환). 기존 `ProductDetailTabs`(상품 상세 전용 고정
// 4탭)는 손대지 않는다(회귀 유지) — 이 컴포넌트는 범용이고 이번 SR에 어느 화면에도 끼우지 않는다.
// 스와이프 판정은 `resolveSwipeTab`(순수함수)으로 분리해 테스트하고, 이 파일은 그 함수만 호출한다.
import { useRef } from 'react'
import { resolveSwipeTab } from './resolveSwipeTab'
import './Tabs.css'

export interface TabItem {
  key: string
  label: string
  /** true면 라벨 위에 작은 점(신규/알림 표시 — SR-311 AC "탭 위 빨간 라벨")을 그린다. 탭 버튼 자체
   * 내부에 렌더하므로 탭 수·활성 굵기와 무관하게 항상 라벨과 같은 위치에 맞는다(SR-311 round1 QA
   * CONCERNS 3 — 이전엔 `Tabs` 바깥에 별도 오버레이 행을 겹쳐 그렸는데 오버레이 셀의 `fontWeight`가
   * 활성 탭의 실제 굵기와 달라 탭이 2개 이상 노출되면 점 위치가 어긋났다). */
  badge?: boolean
}

export interface TabsProps {
  tabs: TabItem[]
  activeKey: string
  onChange: (key: string) => void
  /** 스크롤 시 상단 고정(position: sticky) 여부. */
  sticky?: boolean
}

const SWIPE_THRESHOLD_PX = 40

export function Tabs({ tabs, activeKey, onChange, sticky }: TabsProps) {
  const touchStartX = useRef<number | null>(null)
  const activeIndex = Math.max(0, tabs.findIndex(t => t.key === activeKey))

  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0]?.clientX ?? null
  }

  const handleTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null) return
    const endX = e.changedTouches[0]?.clientX ?? touchStartX.current
    const nextIndex = resolveSwipeTab(touchStartX.current, endX, SWIPE_THRESHOLD_PX, activeIndex, tabs.length)
    touchStartX.current = null
    if (nextIndex !== activeIndex) onChange(tabs[nextIndex].key)
  }

  return (
    <div
      role="tablist"
      aria-label="탭 목록"
      className={sticky ? 'cmn-tabs cmn-tabs--sticky' : 'cmn-tabs'}
      onTouchStart={handleTouchStart}
      onTouchEnd={handleTouchEnd}
    >
      {tabs.map(t => (
        <button
          key={t.key}
          type="button"
          role="tab"
          aria-selected={t.key === activeKey}
          className={t.key === activeKey ? 'cmn-tabs__tab cmn-tabs__tab--active' : 'cmn-tabs__tab'}
          onClick={() => onChange(t.key)}
        >
          {t.badge && <span aria-hidden="true" className="cmn-tabs__badge-dot" />}
          {t.label}
        </button>
      ))}
    </div>
  )
}
