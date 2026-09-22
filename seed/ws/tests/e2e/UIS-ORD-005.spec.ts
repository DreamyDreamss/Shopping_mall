// 이 파일은 build_e2e_plan.py가 화면설계서(UIS)에서 생성했습니다.
// 출처: docs/05_설계서/order/UIS/UIS-ORD-005_장바구니/spec.md
// 시나리오·위젯 id는 그 문서의 §2·§4에서 그대로 가져왔습니다. TODO(사람) 표시는
// 문서 서술만으로는 조작을 확정할 수 없는 지점입니다 — 채우기 전엔 그 단계가 검증되지 않습니다.
import { test, expect } from '@playwright/test'
import { widget } from './_widgets'

const ROUTE = '/cart'

test.describe('UIS-ORD-005 · 장바구니', () => {

  // linked_tc: TC-FUNC-order-033
  test('시나리오: 회원별 장바구니 조회', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. `/cart` 또는 `/cart?memberId=`로 진입한다. `memberId`를 안 주면 서버가 `MemberService.list()`의', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — `/cart` 또는 `/cart?memberId=`로 진입한다. `memberId`를 안 주면 서버가 `MemberService.list()`의
    })
    await test.step('2. 상단 회원 셀렉트(1)에서 다른 회원을 고르면 `onchange="this.form.submit()"`으로 **즉시 재조회**된다', async () => {
      const el = widget(page, '`#btnSelect`', '조회')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
    await test.step('3. 품목 표에서 상품명·단가·수량·품목합계를 확인하고, 표 아래 총합계(모든 품목의 `lineTotal` 합)를', async () => {
      const el = widget(page, 'qty input (행별)', '수량')
      await expect(el).toBeVisible()
      // TODO(사람): 입력값 지정 — await el.fill('...')
    })
  })

  // linked_tc: TC-FUNC-order-034
  test('시나리오: 품목 수량 변경', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 해당 행의 수량 입력칸(3)을 원하는 값으로 고친다(`min="1"`).', async () => {
      const el = widget(page, 'qty input (행별)', '수량')
      await expect(el).toBeVisible()
      // TODO(사람): 입력값 지정 — await el.fill('...')
    })
    await test.step('2. 같은 행의 [변경] 버튼(4)을 누르면 `POST /cart/items/{sku}/update`가 `memberId`(hidden)와 `qty`를', async () => {
      const el = widget(page, '변경 버튼 (행별)', '변경')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
    await test.step('3. 서버(`CartViewController#updateQty` → `CartService.updateQty`)가 처리 후 `redirect:/cart?memberId=...`', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 서버(`CartViewController#updateQty` → `CartService.updateQty`)가 처리 후 `redirect:/cart?memberI
    })
    await test.step('4. 재조회된 화면에서 바뀐 수량·품목합계·총합계를 확인한다. `qty<1`이면 400, 재고 초과면 409로 거부되며', async () => {
      const el = widget(page, '`#btnSelect`', '조회')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
  })

  // linked_tc: TC-FUNC-order-035
  test('시나리오: 품목 삭제', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 해당 행의 [삭제] 버튼(5)을 누른다. **수량을 0으로 낮춰 사라지게 하는 방식이 아니라 삭제는 반드시', async () => {
      const el = widget(page, 'qty input (행별)', '수량')
      await expect(el).toBeVisible()
      // TODO(사람): 입력값 지정 — await el.fill('...')
    })
    await test.step('2. `POST /cart/items/{sku}/delete`가 `memberId`(hidden)를 실어 제출된다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — `POST /cart/items/{sku}/delete`가 `memberId`(hidden)를 실어 제출된다.
    })
    await test.step('3. 서버(`CartViewController#delete` → `CartService.delete`)가 처리 후 `redirect:/cart?memberId=...`.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 서버(`CartViewController#delete` → `CartService.delete`)가 처리 후 `redirect:/cart?memberId=...`
    })
    await test.step('4. 재조회된 화면에서 해당 행이 사라지고 총합계가 다시 계산된다. 마지막 품목을 지우면 빈 상태로', async () => {
      const el = widget(page, '`#btnSelect`', '조회')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
  })

  // linked_tc: TC-FUNC-order-036
  test('시나리오: 빈 장바구니 → 상품 목록으로 이동', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 선택된 회원의 장바구니에 품목이 없으면 표·총합계·[주문하기] 버튼 대신 "장바구니가 비어 있습니다"', async () => {
      const el = widget(page, '`#btnCheckout`', '주문하기')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
    await test.step('2. 링크를 클릭하면 `GET /product/list`(UIS-ORD-003)로 이동해 담을 상품을 고른다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 링크를 클릭하면 `GET /product/list`(UIS-ORD-003)로 이동해 담을 상품을 고른다.
    })
    await test.step('3. 품목이 있는 상태에서는 표·총합계·[주문하기] 버튼 아래에 같은 "상품 목록으로" 링크(7)가 별도로', async () => {
      const el = widget(page, '`#btnCheckout`', '주문하기')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
  })

  // linked_tc: TC-FUNC-order-037
  test('시나리오: 체크아웃(주문하기)', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 품목이 있는 상태에서만 [주문하기] 버튼(6)이 노출된다(`list.html:77`,', async () => {
      const el = widget(page, '`#btnCheckout`', '주문하기')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
    await test.step('2. 버튼(6)을 누르면 `POST /cart/checkout`가 `memberId`(hidden)를 실어 제출된다(PRG 패턴).', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 버튼(6)을 누르면 `POST /cart/checkout`가 `memberId`(hidden)를 실어 제출된다(PRG 패턴).
    })
    await test.step('3. 서버(`CartViewController#checkout` → `CartService#checkout`)가 해당 회원 장바구니를', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 서버(`CartViewController#checkout` → `CartService#checkout`)가 해당 회원 장바구니를
    })
    await test.step('4. 실패(400 빈 장바구니 · 404 회원 없음 · 409 재고부족/판매중지/상품없음 — 사전 스윕 또는', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 실패(400 빈 장바구니 · 404 회원 없음 · 409 재고부족/판매중지/상품없음 — 사전 스윕 또는
    })
  })
})
