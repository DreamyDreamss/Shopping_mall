// SR-311 — 앱 셸 상태별 스토리. 확정문답 scr_states 6개 상태(PC 1280px·태블릿 1024px·모바일 390px·
// 최근 본 상품 0/3개·로그인/비로그인 헤더·QR 팝업 열림)를 커버한다. viewport addon이 이 프로젝트에
// 설정돼 있지 않아(`.storybook/main.ts`에 addons 없음) 믿을 수 없다 — 대신 `forceBreakpoint` prop으로
// 상태를 결정적으로 고정한다(STORY "테스트" 절).
import type { Meta, StoryObj } from '@storybook/react-vite'
import { MemoryRouter } from 'react-router-dom'
import { expect, userEvent, within } from 'storybook/test'
import { AppShell } from './AppShell'
import { recordViewed } from './recentlyViewedStorage'
import type { Product, SessionResult } from '../../types'

const session: SessionResult = {
  memberId: 'm-1', memberName: '홍길동', grade: 'NORMAL',
  apiKey: 'k-1', refreshToken: 'r-1', refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
}

const recentProducts: Product[] = [
  { sku: 'sku-shell-1', productName: '셸 데모 상품 1', price: 39000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'sku-shell-2', productName: '셸 데모 상품 2', price: 59000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
  { sku: 'sku-shell-3', productName: '셸 데모 상품 3', price: 79000, stockQty: 5, saleYn: 'Y', listPrice: null, imageUrl: null },
]

/**
 * `RightRail`(AppShell 자식)은 자체 `useEffect`로 `fetchProducts()`를 부른다(STORY "데이터" 절) —
 * MSW가 없어 `window.fetch`를 렌더 시점(자식 effect 이전)에 동기 교체한다(`RightRail.stories.tsx`와
 * 동일 기법). `viewedSkus`가 비어 있으면 레일의 "최근 본 상품" 카드는 자연히 숨는다.
 */
function withFixture(viewedSkus: string[]) {
  return (Story: () => React.ReactElement) => {
    localStorage.clear()
    viewedSkus.forEach(sku => recordViewed(sku))
    window.fetch = (async () => ({ ok: true, status: 200, statusText: '', json: async () => recentProducts })) as unknown as typeof fetch
    return <Story />
  }
}

const bodyPlaceholder = (
  <div style={{ padding: 'var(--space-6) var(--space-4)', color: 'var(--color-text-secondary)' }}>
    (본문 콘텐츠 영역 — 화면마다 다른 내용이 여기 들어간다)
  </div>
)

/**
 * round3 재작업 지시 2·3 — "본문이 뷰포트 가운데 고정"을 jsdom이 아니라 실제 레이아웃 엔진(test-storybook
 * = 실브라우저)에서 검증한다. 본문(`<main>`)의 수평 중심이 스토리 캔버스 컨테이너의 수평 중심과 같은지
 * 잰다 — 레일 카드가 있는 스토리(최근본상품_3개)·없는 스토리(최근본상품_0개) 양쪽에 이 play를 붙여
 * 둘 다 통과해야 "레일 카드 유무와 무관하게 본문이 가운데 고정"이 실측으로 확인된다(하나라도 실패하면
 * 좌우 칸이 다시 비대칭 고정폭(64px/220px)으로 되돌아갔다는 뜻).
 */
async function assertMainCentered({ canvasElement }: { canvasElement: HTMLElement }) {
  const canvas = within(canvasElement)
  const main = canvas.getByText('(본문 콘텐츠 영역 — 화면마다 다른 내용이 여기 들어간다)').closest('main')
  if (!main) throw new Error('main 요소를 찾지 못했다')
  const mainRect = main.getBoundingClientRect()
  const containerRect = canvasElement.getBoundingClientRect()
  const mainCenter = mainRect.left + mainRect.width / 2
  const containerCenter = containerRect.left + containerRect.width / 2
  await expect(Math.abs(mainCenter - containerCenter)).toBeLessThan(2)
}

const meta = {
  title: '쇼핑셸/AppShell',
  component: AppShell,
  args: {
    session: null, cartItemCount: 0, searchValue: '', onSearchChange: () => {}, onSearchSubmit: () => {},
    onLogout: () => {}, children: bodyPlaceholder,
  },
  tags: ['UIS-CMN-003'],
  decorators: [Story => <MemoryRouter initialEntries={['/shop']}><Story /></MemoryRouter>, withFixture([])],
} satisfies Meta<typeof AppShell>
export default meta

type Story = StoryObj<typeof meta>

/** PC 1280px — 좌측 퀵바 + 750px 본문 + 우측 레일(레일은 최근 본 상품이 없어 이 스토리에선 숨음). */
export const PC_1280: Story = { args: { forceBreakpoint: 'desktop' } }

/** 태블릿 1024px — 퀵바·레일·하단탭바 전부 없는 단일 컬럼 본문만(AC 미정 구간의 보수적 기본값). */
export const 태블릿_1024: Story = { args: { forceBreakpoint: 'tablet' } }

/** 모바일 390px — 전폭 본문 + 하단 탭바. */
export const 모바일_390: Story = { args: { forceBreakpoint: 'mobile' } }

/** 최근 본 상품 3개 — PC 레일에 그라데이션 헤더 카드가 보인다. 본문이 뷰포트 가운데 고정인지 실측(play). */
export const 최근본상품_3개: Story = {
  args: { forceBreakpoint: 'desktop' },
  decorators: [withFixture(['sku-shell-1', 'sku-shell-2', 'sku-shell-3'])],
  play: assertMainCentered,
}

/** 최근 본 상품 0개 — 레일 "카드"는 렌더되지 않지만 헤더·GNB 탭·본문·푸터는 항상 그려진다(셸 자체가
 * 통째로 비는 상태가 아니므로 round3 재작업 지시 5 — `renders-nothing` 태그를 달지 않는다). 카드가
 * 없어도 레일 "칸"은 유지돼 본문이 최근본상품_3개와 같은 위치(가운데)에 고정되는지 실측(play). */
export const 최근본상품_0개: Story = {
  args: { forceBreakpoint: 'desktop' },
  decorators: [withFixture([])],
  play: assertMainCentered,
}

/** 로그인 헤더 — 회원명+로그아웃, '마이' 목적지가 배송지 관리로 고정. */
export const 로그인_헤더: Story = { args: { forceBreakpoint: 'desktop', session, cartItemCount: 3 } }

/** 비로그인 헤더 — 로그인 링크, '마이' 클릭 시 로그인 화면으로. */
export const 비로그인_헤더: Story = { args: { forceBreakpoint: 'desktop', session: null } }

/** QR 팝업 열림 — 좌측 퀵바의 QR 버튼을 실제로 클릭해(play) 팝업이 뜬 상태를 보여준다. */
export const QR_팝업_열림: Story = {
  args: { forceBreakpoint: 'desktop' },
  play: async ({ canvasElement }) => {
    const canvas = within(canvasElement)
    await userEvent.click(canvas.getByRole('button', { name: 'QR' }))
  },
}
