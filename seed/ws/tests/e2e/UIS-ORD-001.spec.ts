// 이 파일은 build_e2e_plan.py가 화면설계서(UIS)에서 생성했습니다.
// 출처: docs/05_설계서/order/UIS/UIS-ORD-001_주문목록/spec.md
// 시나리오·위젯 id는 그 문서의 §2·§4에서 그대로 가져왔습니다. TODO(사람) 표시는
// 문서 서술만으로는 조작을 확정할 수 없는 지점입니다 — 채우기 전엔 그 단계가 검증되지 않습니다.
import { test, expect } from '@playwright/test'
import { widget } from './_widgets'

const ROUTE = '/order/list'

test.describe('UIS-ORD-001 · 주문 목록', () => {

  // linked_tc: TC-FUNC-order-025
  test('시나리오: 주문 조회', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 「회원 ID」에 조회할 회원의 ID를 입력한다(선택 — 비우면 전체 회원 대상). (1)', async () => {
      const el = widget(page, '`memberId`', '회원 ID')
      await expect(el).toBeVisible()
      // TODO(사람): 입력값 지정 — await el.fill('...')
    })
    await test.step('2. 「주문상태」 드롭다운에서 상태를 선택한다(선택 — 기본값 "전체"). (2)', async () => {
      const el = widget(page, '`selState`', '주문상태')
      await expect(el).toBeVisible()
      // TODO(사람): 선택할 값 지정 — await el.selectOption({ index: 1 })
    })
    await test.step('3. 「검색」 버튼을 클릭한다. 폼이 `GET /order/list`로 재요청되며 `memberId`·`orderState` 쿼리', async () => {
      const el = widget(page, '`memberId`', '회원 ID')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
    await test.step('4. 목록에서 원하는 주문의 주문번호 링크를 클릭하면 `GET /order/{orderNo}`로 이동해 주문 상세', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 목록에서 원하는 주문의 주문번호 링크를 클릭하면 `GET /order/{orderNo}`로 이동해 주문 상세
    })
  })

  // linked_tc: TC-FUNC-order-038
  test('시나리오: 조회기간 프리셋 버튼 클릭(SR-209) — 최근 7일·30일·90일 중 클릭 시 startDate/endDate가 오늘 기준 N일 전~오늘로 채워지고 즉시 GET /order/list 재조회되며, 기존 memberId/orderState 입력값은 AND 결합으로 유지된다', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))

    // Step 1: 프리셋 버튼 3개가 렌더되는지 확인 (항상 렌더, searchError 무관)
    await test.step('1. 검색조건 블록에서 [최근 7일]·[최근 30일]·[최근 90일] 프리셋 버튼이 렌더된다', async () => {
      const btnPreset7 = widget(page, '`btnPreset7`', '최근 7일')
      const btnPreset30 = widget(page, '`btnPreset30`', '최근 30일')
      const btnPreset90 = widget(page, '`btnPreset90`', '최근 90일')
      await expect(btnPreset7).toBeVisible()
      await expect(btnPreset30).toBeVisible()
      await expect(btnPreset90).toBeVisible()
    })

    // Step 2: 최근 7일 프리셋 클릭
    await test.step('2. [최근 7일](`#btnPreset7`)을 클릭하면 #startDate·#endDate가 오늘-7일~오늘로 채워지고 폼이 즉시 제출된다', async () => {
      const btnPreset7 = widget(page, '`btnPreset7`', '최근 7일')
      const startDateInput = widget(page, '`startDate`', '시작일')
      const endDateInput = widget(page, '`endDate`', '종료일')

      // GET /order/list 요청을 기다린다
      const responsePromise = page.waitForResponse(resp => resp.url().includes('/order/list') && resp.status() === 200)

      // 프리셋 버튼 클릭
      await btnPreset7.click()

      // 응답이 올 때까지 기다린다 (재조회 실행 확인)
      const response = await responsePromise
      await expect(response.status()).toBe(200)

      // startDate/endDate 값이 채워졌는지 확인 (값은 yyyy-MM-dd 형식)
      const startValue = await startDateInput.inputValue()
      const endValue = await endDateInput.inputValue()
      expect(startValue).toBeTruthy()
      expect(endValue).toBeTruthy()

      // 날짜 형식 검증
      const dateRegex = /^\d{4}-\d{2}-\d{2}$/
      expect(startValue).toMatch(dateRegex)
      expect(endValue).toMatch(dateRegex)
    })

    // Step 3: 최근 30일 프리셋 클릭 (다시 한 번 테스트)
    await test.step('3. [최근 30일](`#btnPreset30`)을 클릭하면 #startDate·#endDate가 오늘-30일~오늘로 채워지고 즉시 재조회된다', async () => {
      const btnPreset30 = widget(page, '`btnPreset30`', '최근 30일')
      const startDateInput = widget(page, '`startDate`', '시작일')

      const responsePromise = page.waitForResponse(resp => resp.url().includes('/order/list') && resp.status() === 200)

      await btnPreset30.click()

      const response = await responsePromise
      await expect(response.status()).toBe(200)

      const startValue = await startDateInput.inputValue()
      expect(startValue).toBeTruthy()

      const dateRegex = /^\d{4}-\d{2}-\d{2}$/
      expect(startValue).toMatch(dateRegex)
    })

    // Step 4: memberId/orderState 값이 유지되는지 확인
    await test.step('4. 기존 memberId·orderState 입력값은 폼에 그대로 남아 AND로 함께 결합되어 재조회된다', async () => {
      // 현재 폼 상태에서 memberId와 orderState 값이 있는지 확인
      const memberIdInput = widget(page, '`memberId`', '회원 ID')
      const orderStateSelect = widget(page, '`selState`', '주문상태')

      // 값이 있으면 프리셋 클릭 후에도 유지되는지 확인
      const memberIdBefore = await memberIdInput.inputValue()
      const orderStateBefore = await orderStateSelect.inputValue()

      // 프리셋 클릭 (90일)
      const btnPreset90 = widget(page, '`btnPreset90`', '최근 90일')
      const responsePromise = page.waitForResponse(resp => resp.url().includes('/order/list') && resp.status() === 200)

      await btnPreset90.click()

      const response = await responsePromise
      await expect(response.status()).toBe(200)

      // 클릭 후 memberId와 orderState 값이 동일하게 유지되는지 확인
      const memberIdAfter = await memberIdInput.inputValue()
      const orderStateAfter = await orderStateSelect.inputValue()

      expect(memberIdAfter).toBe(memberIdBefore)
      expect(orderStateAfter).toBe(orderStateBefore)
    })
  })
})
