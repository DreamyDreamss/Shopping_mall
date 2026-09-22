/** @jest-environment jsdom */
// SR-311 — 앱 셸(AppShell) 통합 테스트. 브레이크포인트 3구간(1280/390/1024)에서 퀵바·레일·하단탭바
// 노출이 AC대로 갈리는지, GNB 탭·푸터는 구간과 무관하게 항상 뜨는지, TOP 스크롤·QR 팝업이 동작하는지
// 확인한다. `useBreakpoint`는 `window.innerWidth`만 읽으므로(STORY "프레임워크 실행 모델 함정" 절 —
// `matchMedia`를 쓰지 않는다) `Object.defineProperty` + `resize` 이벤트로 폭을 바꾼다.
import '@testing-library/jest-dom/jest-globals'
import type { ComponentProps } from 'react'
import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import { AppShell } from './AppShell'
import { recordViewed } from './recentlyViewedStorage'
import type { Product } from '../../types'

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

const recentProduct: Product = {
  sku: 'sku-shell-test-1', productName: '셸 테스트 상품', price: 10000, stockQty: 5, saleYn: 'Y',
  listPrice: null, imageUrl: null,
}

/** `fireEvent`가 내부에서 이미 `act()`로 감싸므로 별도 래핑이 필요 없다. 테스트가 끝나기 전에
 * 기본값(1024)으로 복원한다 — 안 그러면 이후 실행되는 다른 파일의 페이지 테스트가 예기치 않은
 * breakpoint로 렌더될 수 있다(STORY "테스트 격리" 절). */
function setInnerWidth(width: number) {
  Object.defineProperty(window, 'innerWidth', { value: width, configurable: true })
  fireEvent(window, new Event('resize'))
}

describe('AppShell', () => {
  beforeEach(() => {
    global.fetch = jest.fn(async () => jsonResponse(200, [recentProduct])) as unknown as typeof fetch
    localStorage.clear() // 최근본상품 키가 다음 테스트로 새지 않게 한다(사례집 SR-232 r2와 동일 클래스).
  })

  afterEach(() => {
    localStorage.clear()
    setInnerWidth(1024)
  })

  function renderShell(overrides: Partial<ComponentProps<typeof AppShell>> = {}) {
    return render(
      <MemoryRouter initialEntries={['/shop']}>
        <AppShell session={null} cartItemCount={0} searchValue="" onSearchChange={() => {}}
                  onSearchSubmit={() => {}} onLogout={() => {}} {...overrides}>
          <div>본문 콘텐츠</div>
        </AppShell>
      </MemoryRouter>,
    )
  }

  test('헤더(Gnb)·GNB 탭("홈"만 노출)·푸터는 브레이크포인트와 무관하게 항상 뜬다', () => {
    renderShell()
    expect(screen.getByRole('link', { name: 'SL Shop' })).toBeInTheDocument()
    expect(screen.getByRole('tab', { name: '홈' })).toBeInTheDocument()
    expect(screen.queryByText('편성표')).not.toBeInTheDocument()
    expect(screen.queryByText('VIP라운지')).not.toBeInTheDocument()
    expect(screen.getByText(/사업자등록번호/)).toBeInTheDocument()
  })

  test('1280px(≥1200) — 좌측 퀵바 + 우측 레일이 뜬다', async () => {
    recordViewed(recentProduct.sku)
    renderShell()
    setInnerWidth(1280)
    expect(await screen.findByLabelText('빠른 메뉴')).toBeInTheDocument()
    expect(await screen.findByLabelText('사이드 정보')).toBeInTheDocument()
    expect(screen.queryByLabelText('하단 메뉴')).not.toBeInTheDocument()
  })

  // round3 재작업 지시 2·3 — 레일 카드 유무와 무관하게 [퀵바 칸|750px 본문|레일 칸] 3열 그리드가 유지돼야
  // 한다(레일이 내용 없이 null을 반환해도 그 "칸"은 그대로 있어야 본문이 122px씩 튀지 않는다, round2
  // QA round2 권고 2). 실제 픽셀 좌표는 jsdom이 레이아웃을 계산하지 않아 잴 수 없으므로(브라우저 기반
  // 검증은 AppShell.stories.tsx의 play 함수가 한다), 여기서는 구조적 가드 — (a) 레일 칸(`appshell-rail-slot`)이
  // 카드 유무와 무관하게 DOM에 항상 존재하는지, (b) 좌우 칸이 고정폭(64px/220px) 비대칭이 아니라 동일
  // 비율(1fr/1fr)로 그리드가 짜여 있는지(본문이 항상 정가운데) — 둘 다 지우면(가드를 되돌리면) 실패한다.
  test('레일 카드 유무와 무관하게 레일 칸이 유지되고, 그리드가 좌우 대칭(1fr/1fr)이다', async () => {
    // 레일에 내용이 있는 경우
    recordViewed(recentProduct.sku)
    const withContent = renderShell()
    setInnerWidth(1280)
    await screen.findByLabelText('사이드 정보')
    const railSlotWithContent = withContent.container.querySelector('[data-testid="appshell-rail-slot"]')
    const gridWithContent = screen.getByText('본문 콘텐츠').closest('main')?.parentElement
    expect(railSlotWithContent).toBeInTheDocument()
    expect(railSlotWithContent?.textContent).not.toBe('')
    // round4 재작업 지시 1 — 좌우 칸의 min-content 하한을 0으로 강제(minmax(0, 1fr))해 레일(220px)·
    // 퀵바(64px) 콘텐츠 폭과 무관하게 트랙이 항상 대칭이 되게 한다(round3 QA FAIL 필수수정 1 — 이전
    // '1fr var(--layout-content-width) 1fr'은 1200~1310px 대역에서 레일 유무에 따라 최대 51px 비대칭).
    expect(gridWithContent?.style.gridTemplateColumns)
      .toBe('minmax(0, 1fr) var(--layout-content-width) minmax(0, 1fr)')
    withContent.unmount()
    setInnerWidth(1024)

    // 레일이 아무것도 그리지 않는 경우(최근 본 상품 0개) — 칸 자체는 그대로 남아야 한다.
    localStorage.clear()
    const withoutContent = renderShell()
    setInnerWidth(1280)
    const railSlotWithoutContent = withoutContent.container.querySelector('[data-testid="appshell-rail-slot"]')
    const gridWithoutContent = screen.getByText('본문 콘텐츠').closest('main')?.parentElement
    expect(railSlotWithoutContent).toBeInTheDocument() // 칸 자체는 유지(내용만 비어 있음)
    expect(railSlotWithoutContent?.textContent).toBe('')
    // 레일 내용 유무와 무관하게 같은 대칭 그리드 — 본문 위치가 달라지지 않는다.
    expect(gridWithoutContent?.style.gridTemplateColumns).toBe(gridWithContent?.style.gridTemplateColumns)
  })

  test('390px(<750) — 하단 탭바만 뜨고 퀵바·레일은 없다', () => {
    renderShell()
    setInnerWidth(390)
    expect(screen.getByLabelText('하단 메뉴')).toBeInTheDocument()
    expect(screen.queryByLabelText('빠른 메뉴')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('사이드 정보')).not.toBeInTheDocument()
  })

  test('1024px(jsdom 기본값, 750~1199 태블릿 대역) — 퀵바·레일·하단탭바 전부 없다', () => {
    renderShell()
    setInnerWidth(1024)
    expect(screen.queryByLabelText('빠른 메뉴')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('사이드 정보')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('하단 메뉴')).not.toBeInTheDocument()
  })

  test('TOP 클릭 시 window.scrollTo가 호출된다', async () => {
    const scrollToSpy = jest.fn()
    window.scrollTo = scrollToSpy as unknown as typeof window.scrollTo
    renderShell()
    setInnerWidth(1280)
    await screen.findByLabelText('빠른 메뉴')
    fireEvent.click(screen.getByRole('button', { name: 'TOP' }))
    expect(scrollToSpy).toHaveBeenCalledWith({ top: 0, behavior: 'smooth' })
  })

  test('QR 클릭 시 팝업이 열리고 QR SVG가 뜨며 콘솔 오류가 없다(외부 이미지 네트워크 요청 없음)', async () => {
    const consoleErrorSpy = jest.spyOn(console, 'error').mockImplementation(() => {})
    renderShell()
    setInnerWidth(1280)
    await screen.findByLabelText('빠른 메뉴')
    fireEvent.click(screen.getByRole('button', { name: 'QR' }))
    const dialog = await screen.findByRole('dialog', { name: 'QR 코드' })
    expect(dialog.querySelector('svg')).toBeInTheDocument()
    expect(consoleErrorSpy).not.toHaveBeenCalled()
    consoleErrorSpy.mockRestore()
  })
})
