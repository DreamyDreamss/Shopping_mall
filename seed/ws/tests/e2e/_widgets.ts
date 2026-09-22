// 이 파일은 build_e2e_plan.py가 생성합니다 — 직접 편집하면 재생성 시 덮어써집니다.
import { type Page, type Locator } from '@playwright/test'

/**
 * 화면설계서(UIS) §4 위젯·액션 표의 **위젯 id**로 요소를 찾는다.
 * 프로젝트마다 테스트 훅이 다르므로 흔한 것부터 차례로 시도하고, 마지막에 레이블로 폴백한다.
 * 어느 것도 안 맞으면 테스트가 실패한다 — 못 찾은 것을 통과로 둔갑시키지 않는다.
 */
export function widget(page: Page, id: string, label?: string): Locator {
  // 문서 표기 정규화(v4.19.35, 실증 IMP-035) — UIS 표의 id는 마크다운 백틱(`#btnCheckout`)·
  // '#' 접두·설명 괄호가 섞여 온다. 종전에는 이를 그대로 `#${id}`에 넣어 CSS 파서가
  // "Unexpected token" 크래시(13건 실측) — 의미 간극(문서≠DOM)과 달리 이건 구문 사고다.
  const clean = id.trim().replace(/^`+|`+$/g, '').replace(/^#/, '').trim()
  const attr = clean.replace(/"/g, '\"')
  const parts = [
    `[data-testid="${attr}"]`, `[data-test="${attr}"]`,
    `[name="${attr}"]`, `[aria-label="${attr}"]`, `[id="${attr}"]`,
  ]
  // bare #id 형태는 CSS 식별자로 유효할 때만(공백·괄호 id는 [id="…"]가 이미 커버)
  if (/^[A-Za-z_][A-Za-z0-9_-]*$/.test(clean)) parts.splice(2, 0, `#${clean}`)
  const byHook = page.locator(parts.join(', '))
  if (!label) return byHook.first()
  return byHook.or(page.getByLabel(label, { exact: false }))
           .or(page.getByRole('button', { name: label, exact: false }))
           .first()
}
