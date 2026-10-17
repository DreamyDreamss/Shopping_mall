/** @jest-environment jsdom */
import { beforeEach, afterEach, expect, jest, test } from '@jest/globals'
import { addCartItem, fetchCart, updateCartItemQty, deleteCartItem, checkoutCart, fetchOrder, searchZipcodes } from './api'
import { saveSession } from './session'

const originalFetch = global.fetch
beforeEach(() => {
  localStorage.clear()
  global.fetch = jest.fn(async () => ({ ok: true, json: async () => ({ items: [] }) })) as unknown as typeof fetch
})
afterEach(() => { global.fetch = originalFetch; localStorage.clear() })

test('packaged member requests carry their own key without depending on a privileged dev proxy', async () => {
  saveSession({ memberId:'AUDIT-MEMBER', memberName:'Audit', grade:'BRONZE', apiKey:'member-owned-key', refreshToken:'test', refreshTokenExpiresAt:'2099-01-01' })
  await addCartItem('AUDIT-MEMBER','SKU-AUDIT',1)
  await fetchCart('AUDIT-MEMBER')
  await updateCartItemQty('AUDIT-MEMBER','SKU-AUDIT',2)
  await deleteCartItem('AUDIT-MEMBER','SKU-AUDIT')
  await checkoutCart('AUDIT-MEMBER')
  await fetchOrder('OWNED-ORDER')
  await searchZipcodes('감사 주소')
  expect(global.fetch).toHaveBeenCalledTimes(7)
  for (const [,init] of (global.fetch as jest.MockedFunction<typeof fetch>).mock.calls) {
    expect(new Headers(init?.headers).get('X-Api-Key')).toBe('member-owned-key')
  }
})

test('an unsigned session cannot synthesize a privileged fallback credential', async () => {
  await fetchCart('AUDIT-MEMBER')
  const [,init] = (global.fetch as jest.MockedFunction<typeof fetch>).mock.calls[0]
  expect(new Headers(init?.headers).has('X-Api-Key')).toBe(false)
})
