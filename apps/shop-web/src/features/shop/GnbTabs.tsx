// SR-311 — 앱 셸 가로 GNB 탭. 상호작용(스티키·좌우 스와이프·role=tablist)은 SR-310 공통
// `components/common/Tabs`를 그대로 쓴다(재구현 금지) — `activeKey`는 `useLocation().pathname`을
// `GNB_TAB_CONFIG[].path`와 매칭해 계산하고, `onChange`는 `implemented:true`(도착 화면이 있는) 탭만
// navigate한다. 도착 화면이 없는 탭은 이 화면에서 아예 목록에서 뺀다(AC "숨김").
//
// SR-311 round2 재작업 지시 3·6 — "탭 위 빨간 라벨"은 더 이상 `Tabs` 바깥에 별도 오버레이 행을 겹쳐
// 그리지 않는다(그 방식은 오버레이 셀을 전부 bold로 뒀는데 `Tabs.css`의 활성 탭만 bold라 탭이 2개
// 이상 노출되면 점이 어긋났다). 대신 `Tabs`가 새로 연 `badge` 계약(탭 버튼 안쪽에 점을 그림, 탭
// 텍스트·활성 상태와 무관하게 항상 정렬)으로 그린다. 현재 경로와 일치하는 탭이 없으면(예: 장바구니·
// 로그인 화면) 존재하지 않는 key를 넘겨 어떤 탭도 `aria-selected`되지 않게 한다(임의로 '홈'을 골라
// 강조하지 않는다).
import { useLocation, useNavigate } from 'react-router-dom'
import { Tabs } from '../../components/common/Tabs'
import { GNB_TAB_CONFIG } from './shopStatic'

export function GnbTabs() {
  const location = useLocation()
  const navigate = useNavigate()

  const visibleTabs = GNB_TAB_CONFIG.filter(t => t.implemented && t.path)
  if (visibleTabs.length === 0) return null

  const activeTab = visibleTabs.find(t => t.path === location.pathname)

  const handleChange = (key: string) => {
    const target = visibleTabs.find(t => t.key === key)
    if (target?.path) navigate(target.path)
  }

  return (
    <Tabs
      tabs={visibleTabs.map(t => ({ key: t.key, label: t.label, badge: t.badge }))}
      activeKey={activeTab?.key ?? ''}
      onChange={handleChange}
      sticky
    />
  )
}
