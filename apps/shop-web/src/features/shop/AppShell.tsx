// SR-311 — 앱 셸 조립 지점. shop-web 쇼핑 라우트(#/shop/**) 전체의 공통 레이아웃(AC) — 헤더(`Gnb`,
// 무변경 재사용) → 가로 GNB 탭(`GnbTabs`) → (≥1200px: 좌측 퀵바 + 750px 본문 + 우측 레일 / 750~1199px:
// 본문만 / <750px: 전폭 본문 + 하단 탭바) → 푸터(`ShopFooter`, 무변경 재사용) 순으로 배치한다.
import type { ReactNode } from 'react'
import type { SessionResult } from '../../types'
import { Gnb } from './Gnb'
import { GnbTabs } from './GnbTabs'
import { QuickBar } from './QuickBar'
import { BottomTabBar } from './BottomTabBar'
import { RightRail, type EntitlementCounts } from './RightRail'
import { ShopFooter } from './ShopFooter'
import { useBreakpoint } from './useBreakpoint'
import { classifyBreakpoint, type BreakpointState } from './breakpoint'

export interface AppShellProps {
  session: SessionResult | null
  cartItemCount: number
  searchValue: string
  onSearchChange: (value: string) => void
  onSearchSubmit: () => void
  onLogout: () => void
  children: ReactNode
  /** 보유 쿠폰/주문 내역 수 — 값이 오면(SR-276 이후) 그대로 카드가 뜬다. 지금은 아무도 채우지 않는다
   * (STORY "계약" 절). */
  entitlementCounts?: EntitlementCounts
  /** 테스트/스토리 전용 — 실제 `useBreakpoint()` 대신 상태를 결정적으로 고정한다. 기본은 undefined
   * (실측 `window.innerWidth` 사용, STORY "테스트" 절). */
  forceBreakpoint?: 'mobile' | 'tablet' | 'desktop'
  /** round4 재작업 지시 2 — 검색 아이콘으로 이 화면에 막 도착했을 때, 도착 화면(페이지)이
   * `location.state.focusSearch`를 보고 내려주는 신호. `Gnb`는 이 값을 받아서만 검색 입력에
   * 포커스한다(클릭 시점 자체 판단 없음, Gnb.tsx 참조). 기본은 undefined(포커스 없음). */
  autoFocusSearch?: boolean
}

const FORCED_STATE: Record<'mobile' | 'tablet' | 'desktop', BreakpointState> = {
  mobile: classifyBreakpoint(390),
  tablet: classifyBreakpoint(1024),
  desktop: classifyBreakpoint(1280),
}

export function AppShell({
  session, cartItemCount, searchValue, onSearchChange, onSearchSubmit, onLogout, children,
  entitlementCounts, forceBreakpoint, autoFocusSearch,
}: AppShellProps) {
  // 훅 호출 순서는 조건 없이 항상 동일해야 한다(Rules of Hooks) — forceBreakpoint가 있어도
  // useBreakpoint()는 항상 부른다(내부 window.innerWidth 값을 실제로 쓰지 않을 뿐).
  const measured = useBreakpoint()
  const { isMobile, isDesktopRail } = forceBreakpoint ? FORCED_STATE[forceBreakpoint] : measured

  return (
    // round2 재작업 지시 5 — 모바일 하단 여백(하단탭바 높이만큼)은 셸 컨테이너(푸터 포함) 바닥에 둔다.
    // <main>에만 걸려 있으면 position:fixed 하단탭바가 그 아래 있는 ShopFooter를 가린다.
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', paddingBottom: isMobile ? 56 : 0 }}>
      <Gnb session={session} cartItemCount={cartItemCount} searchValue={searchValue}
           onSearchChange={onSearchChange} onSearchSubmit={onSearchSubmit} onLogout={onLogout}
           autoFocusSearch={autoFocusSearch} />
      <GnbTabs />
      {/* round3 재작업 지시 2 — ≥1200px는 [퀵바 칸 | 750px 본문 | 레일 칸] 3열 "고정" 그리드다.
          이전엔 flex + justifyContent:center에 퀵바(64px)·레일(220px) 폭이 비대칭이고, 레일은 카드가
          없으면 컴포넌트 자체가 null을 반환해 DOM에서 완전히 사라져 본문 중심이 레일 유무에 따라
          122px씩 튀었다(round2 QA round2 권고 2).
          round4 재작업 지시 1(round3 QA FAIL 필수수정 1) — `1fr`은 CSS에서 `minmax(auto, 1fr)`이라
          트랙이 칸 안 콘텐츠의 min-content(레일 220px·flexShrink:0, 퀵바 64px) 아래로 줄지 않는다.
          좌우 여유가 440px 미만인 1200~1310px 대역에서는 레일 칸만 220px 하한에 눌려 퀵바 칸이
          상대적으로 줄면서 좌우가 비대칭이 됐고(레일이 카드 없이 null이면 그 칸의 min-content가 0이라
          다시 대칭이 되므로), 결국 본문 위치가 "최근 본 상품 localStorage 상태"에 따라 최대 51px
          움직였다(QA 실측: 1200px 51px, 1240px 31px, 1280px 11px, 1366px 이상 0px). `minmax(0, 1fr)`은
          그 칸의 min-content 하한 자체를 0으로 강제해(QA 수정안 그대로) 콘텐츠가 칸보다 넓어도 트랙
          폭 계산에 영향을 주지 않는다 — 좌우 두 칸은 항상 정확히 같은 폭으로 계산되고(레일/퀵바
          콘텐츠는 필요하면 자기 칸 밖으로 넘칠 뿐, 트랙을 밀어내지 않는다), 가운데 750px 본문은 콘텐츠
          유무·상태와 완전히 무관하게 컨테이너 정중앙에 고정된다(실측: AppShell.stories.tsx
          최근본상품_3개/0개 스토리의 play가 getBoundingClientRect로 검증, 1200/1240/1280 세 폭을
          test-storybook으로 실측 확인 — 아래 "검증" 참조). */}
      <div style={{
        flex: 1, display: isDesktopRail ? 'grid' : 'flex',
        justifyContent: isDesktopRail ? undefined : 'center',
        gridTemplateColumns: isDesktopRail ? 'minmax(0, 1fr) var(--layout-content-width) minmax(0, 1fr)' : undefined,
        gap: 'var(--space-6)', padding: isMobile ? 0 : '0 var(--space-4)',
      }}>
        {isDesktopRail && (
          <div data-testid="appshell-quickbar-slot" style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <QuickBar session={session} />
          </div>
        )}
        {/* round2 재작업 지시 2 — 750px 이상은 항상 --layout-content-width(750px)로 폭을 고정한다(모바일만
            전폭). 이전에는 태블릿(750~1199) 구간에 상한이 없어 1199px→1200px 경계에서 폭이 급락했다
            (비단조). 이제 750px 이상은 전부 750px 고정이라 폭이 단조롭게 유지된다. */}
        <main style={{ width: '100%', maxWidth: isMobile ? '100%' : 'var(--layout-content-width)' }}>
          {children}
        </main>
        {isDesktopRail && (
          // 레일 칸(1fr) 자체는 항상 렌더한다 — `RightRail`이 내용 없음(null)을 반환해도 이 wrapper는
          // 그대로 DOM에 남아 트랙 폭을 유지한다(round3 재작업 지시 2, "레일 칸은 내용이 없어도 자리를
          // 유지").
          <div data-testid="appshell-rail-slot" style={{ display: 'flex', justifyContent: 'flex-start' }}>
            <RightRail entitlementCounts={entitlementCounts} />
          </div>
        )}
      </div>
      <ShopFooter />
      {isMobile && <BottomTabBar session={session} />}
    </div>
  )
}
