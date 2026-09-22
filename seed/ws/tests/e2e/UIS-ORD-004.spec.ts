// 이 파일은 build_e2e_plan.py가 화면설계서(UIS)에서 생성했습니다.
// 출처: docs/05_설계서/order/UIS/UIS-ORD-004_상품상세/spec.md
// 시나리오·위젯 id는 그 문서의 §2·§4에서 그대로 가져왔습니다. TODO(사람) 표시는
// 문서 서술만으로는 조작을 확정할 수 없는 지점입니다 — 채우기 전엔 그 단계가 검증되지 않습니다.
import { test, expect } from '@playwright/test'
import { widget } from './_widgets'

const ROUTE = '/product/{sku}'

test.describe('UIS-ORD-004 · 상품 상세', () => {

  // linked_tc: TC-FUNC-order-029
  test('시나리오: 상품 상세 조회 (정상 SKU)', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 상품 목록 화면(`/product/list`, UIS-ORD-003)에서 상품명을 클릭하거나, `/product/{sku}` 라우트로', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 상품 목록 화면(`/product/list`, UIS-ORD-003)에서 상품명을 클릭하거나, `/product/{sku}` 라우트로
    })
    await test.step('2. 서버(`ProductViewController#productDetail` → `loadProduct`)가 `productService.get(sku)`를', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 서버(`ProductViewController#productDetail` → `loadProduct`)가 `productService.get(sku)`를
    })
    await test.step('3. 표에서 SKU·상품명·가격(`#,###원` 포맷)·재고·상태(재고 0이면 "품절", 그 외 "판매중")를 확인한다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 표에서 SKU·상품명·가격(`#,###원` 포맷)·재고·상태(재고 0이면 "품절", 그 외 "판매중")를 확인한다.
    })
    await test.step('4. 목록으로 돌아가려면 상단 「← 목록으로」 링크로 이동한다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 목록으로 돌아가려면 상단 「← 목록으로」 링크로 이동한다.
    })
  })

  // linked_tc: TC-FUNC-order-030
  test('시나리오: 존재하지 않는 SKU로 접근', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 등록되지 않은 `sku`로 `/product/{sku}`에 접근한다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 등록되지 않은 `sku`로 `/product/{sku}`에 접근한다.
    })
    await test.step('2. `productService.get(sku)`가 던지는 404(`ResponseStatusException`)를 `loadProduct`가 흡수하고', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — `productService.get(sku)`가 던지는 404(`ResponseStatusException`)를 `loadProduct`가 흡수하고
    })
    await test.step('3. 화면에는 "상품을 찾을 수 없습니다" 안내 문구만 표시된다(상세 표·담기 폼 없음).', async () => {
      const el = widget(page, '[담기] 버튼', '담기')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
    await test.step('4. 「← 목록으로」 링크는 이 상태에서도 동일하게 노출되어 목록으로 복귀할 수 있다.', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 「← 목록으로」 링크는 이 상태에서도 동일하게 노출되어 목록으로 복귀할 수 있다.
    })
  })

  // linked_tc: TC-FUNC-order-031
  test('시나리오: 장바구니 담기 (재고 있음, SR-202 실측)', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 재고(`stockQty`) > 0인 상품 상세 화면 하단의 담기 폼에서 회원을 선택한다(`memberService.list()`가', async () => {
      const el = widget(page, '회원 선택 (`name="memberId"`)', '회원')
      await expect(el).toBeVisible()
      // TODO(사람): 선택할 값 지정 — await el.selectOption({ index: 1 })
    })
    await test.step('2. 수량을 입력한다(입력값 기본 1, `min="1"` — 클라이언트 힌트일 뿐 서버가 실검증한다).', async () => {
      const el = widget(page, '수량 입력 (`name="qty"`)', '수량')
      await expect(el).toBeVisible()
      // TODO(사람): 입력값 지정 — await el.fill('...')
    })
    await test.step('3. [담기] 버튼을 클릭하면 `POST /product/{sku}/cart` {memberId, qty}가 제출된다. 컨트롤러는', async () => {
      const el = widget(page, '[담기] 버튼', '담기')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
    await test.step('4. 같은 회원·같은 상품 재담기는 `CartService`가 DB 원자 UPSERT로 수량을 합산한다(D2,', async () => {
      const el = widget(page, '회원 선택 (`name="memberId"`)', '회원')
      await expect(el).toBeVisible()
      // TODO(사람): 선택할 값 지정 — await el.selectOption({ index: 1 })
    })
    await test.step('5. **성공**: 서버가 `redirect:/product/{sku}`로 302 리다이렉트하며 `RedirectAttributes` 플래시에', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — **성공**: 서버가 `redirect:/product/{sku}`로 302 리다이렉트하며 `RedirectAttributes` 플래시에
    })
    await test.step('6. **거부**(400 수량<1 / 404 회원·상품 없음 / 409 재고초과·품절): 동일하게 302 리다이렉트 +', async () => {
      const el = widget(page, '회원 선택 (`name="memberId"`)', '회원')
      await expect(el).toBeVisible()
      // TODO(사람): 선택할 값 지정 — await el.selectOption({ index: 1 })
    })
    await test.step('7. 두 경우 모두 F5(새로고침)는 리다이렉트된 **GET**을 재요청할 뿐이라 `CartService.addItem`이', async () => {
      // TODO(사람): 이 단계는 위젯을 특정하지 못했습니다 — 두 경우 모두 F5(새로고침)는 리다이렉트된 **GET**을 재요청할 뿐이라 `CartService.addItem`이
    })
  })

  // linked_tc: TC-FUNC-order-032
  test('시나리오: 품절 상품', async ({ page }) => {
    await page.goto(ROUTE)
    await expect(page).toHaveURL(new RegExp(ROUTE.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')))
    await test.step('1. 재고(`stockQty`) == 0인 상품 상세 화면에서는 담기 폼 자체가 렌더되지 않고, "품절 상품은 담을', async () => {
      const el = widget(page, '[담기] 버튼', '담기')
      await expect(el).toBeVisible()
      await el.click()
      await page.waitForLoadState('networkidle')
    })
  })
})
