// 이 파일은 build_e2e_plan.py가 화면설계서(UIS)에서 생성했습니다.
// 출처: docs/05_설계서/order/UIS/UIS-ORD-003_상품목록/spec.md
// 시나리오·위젯 id는 그 문서의 §2·§4에서 그대로 가져왔습니다. TODO(사람) 표시는
// 문서 서술만으로는 조작을 확정할 수 없는 지점입니다 — 채우기 전엔 그 단계가 검증되지 않습니다.
import { test, expect } from '@playwright/test'
import { widget } from './_widgets'

const ROUTE = '/product/list'

test.describe('UIS-ORD-003 · 상품 목록', () => {

  // linked_tc: TC-FUNC-order-028
  test('시나리오: 상품 조회', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 「상품명」 입력란에 검색어를 입력한다(선택 — 비우면 전체 판매중 상품 대상). (1)', async () => {
      const el = widget(page, '`keyword`', '상품명')
      await expect(el).toBeVisible()
      // TODO(사람): 입력값 지정 — await el.fill('...')
    })
    await test.step('2. 「검색」 버튼을 클릭한다. 폼(`#searchForm`, method=GET)이 `GET /product/list?keyword=…`로', async () => {
      const el = widget(page, '`btnSearch`', '검색')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
    await test.step('3. 조회 결과 목록에서 원하는 상품의 상품명 링크를 클릭하면 `GET /product/{sku}`로 이동해 상품', async () => {
      const el = widget(page, '`keyword`', '상품명')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
  })
})
