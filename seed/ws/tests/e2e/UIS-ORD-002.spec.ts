// 이 파일은 build_e2e_plan.py가 화면설계서(UIS)에서 생성했습니다.
// 출처: docs/05_설계서/order/UIS/UIS-ORD-002_주문상세/spec.md
// 시나리오·위젯 id는 그 문서의 §2·§4에서 그대로 가져왔습니다. TODO(사람) 표시는
// 문서 서술만으로는 조작을 확정할 수 없는 지점입니다 — 채우기 전엔 그 단계가 검증되지 않습니다.
import { test, expect } from '@playwright/test'
import { widget } from './_widgets'

const ROUTE = '/order/{orderNo}'

test.describe('UIS-ORD-002 · 주문 상세', () => {

  // linked_tc: TC-FUNC-order-026
  test('시나리오: 주문 상세 조회', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 주문 목록 화면(`/order/list`)에서 특정 주문을 클릭하거나, `/order/{orderNo}` 라우트로 직접 진입한다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 주문 목록 화면(`/order/list`)에서 특정 주문을 클릭하거나, `/order/{orderNo}` 라우트로 직접 진입한다.
    })
    await test.step('2. 서버(`OrderViewController#orderDetail`)가 `orderService.detail(orderNo)`로 주문 1건을 조회해 화면에 렌더한다(GET 시점에 이미 데이터 확정 —', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 서버(`OrderViewController#orderDetail`)가 `orderService.detail(orderNo)`로 주문 1건을 조회해 화면에 렌더한다
    })
    await test.step('3. 상단 표에서 회원·상태·총액·주문일시를 확인한다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 상단 표에서 회원·상태·총액·주문일시를 확인한다.
    })
    await test.step('4. 「주문 상품」 표에서 라인별 SKU·상품명·수량·단가를 확인한다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 「주문 상품」 표에서 라인별 SKU·상품명·수량·단가를 확인한다.
    })
    await test.step('5. 「배송」 표에서 배송 이력(배송번호·상태·송장·출고일시)을 확인한다. 배송 이력이 없으면 "배송 없음" 1행이 표시된다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 「배송」 표에서 배송 이력(배송번호·상태·송장·출고일시)을 확인한다. 배송 이력이 없으면 "배송 없음" 1행이 표시된다.
    })
    await test.step('6. 목록으로 돌아가려면 상단 「← 주문 목록」 링크로 이동한다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 목록으로 돌아가려면 상단 「← 주문 목록」 링크로 이동한다.
    })
  })

  // linked_tc: TC-FUNC-order-027
  test('시나리오: 주문 취소', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 화면 하단 「주문 취소」 버튼(①)을 클릭한다.', async () => {
      const el = widget(page, '`#btnCancel`', '주문 취소')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
    await test.step('2. `PATCH /api/orders/{orderNo}/cancel` 호출 → 완료 시 `location.reload()`로 화면을 재조회해 갱신된 상태(CANCELED 등)를 반영한다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — `PATCH /api/orders/{orderNo}/cancel` 호출 → 완료 시 `location.reload()`로 화면을 재조회해 갱신된 상태(CANCEL
    })
    await test.step('3. 별도 확인(confirm) 다이얼로그 없이 즉시 요청이 전송된다(소스에 confirm 없음 — 8. 미확인 사항 참고).', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 별도 확인(confirm) 다이얼로그 없이 즉시 요청이 전송된다(소스에 confirm 없음 — 8. 미확인 사항 참고).
    })
  })
})
