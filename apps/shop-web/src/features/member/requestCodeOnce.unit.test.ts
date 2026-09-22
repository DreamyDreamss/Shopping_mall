// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-006.md
// AC9 실증 — refreshOnce.unit.test.ts(FUNC-member-004)와 동형. 같은 target으로 StrictMode
// effect→cleanup→effect가 재현하는 이중 호출에서도 실제 네트워크 요청은 1회만 나가는지, Promise
// identity와 fetch 호출 횟수로 단언한다.
import { beforeEach, describe, expect, jest, test } from '@jest/globals'
import type { VerificationCodeResult } from '../../types'

/** 호출마다 다른 결과를 만든다 — 재요청이 실제로 일어났는지 값으로도 구분하기 위해서다. */
function makeResult(seq: number): VerificationCodeResult {
  return { channel: 'EMAIL', target: 'user@example.com', expiresInSeconds: 600 - seq }
}

describe('requestCodeOnce — 비밀번호 재설정 코드 요청 in-flight 가드', () => {
  let requestCodeOnce: typeof import('./requestCodeOnce').requestCodeOnce
  let requestPasswordResetCode: typeof import('../../api').requestPasswordResetCode
  let fetchCallSeq: number

  beforeEach(async () => {
    // 모듈 스코프 `inFlight` 가드가 이전 테스트로 새지 않게 매 케이스마다 모듈을 새로 로드한다
    // (SR-232 r2류의 "테스트를 가로지르는 누적 상태" 예방, refreshOnce.unit.test.ts와 동일 기법).
    jest.resetModules()
    fetchCallSeq = 0
    global.fetch = jest.fn(async () => ({
      ok: true,
      json: async () => makeResult(fetchCallSeq++),
    })) as unknown as typeof fetch
    ;({ requestCodeOnce } = await import('./requestCodeOnce'))
    ;({ requestPasswordResetCode } = await import('../../api'))
  })

  test('같은 target으로 연달아 두 번 호출해도 fetch는 1회만 나가고 같은 Promise를 공유한다', async () => {
    const p1 = requestCodeOnce('user@example.com', requestPasswordResetCode)
    const p2 = requestCodeOnce('user@example.com', requestPasswordResetCode)

    // await 전에 identity를 비교한다 — 가드가 없으면 fn(target)이 두 번 호출돼 서로 다른 Promise가
    // 반환되므로 이 단언이 이 시점에 곧바로 실패한다.
    expect(p1).toBe(p2)
    expect(fetch).toHaveBeenCalledTimes(1)

    const [r1, r2] = await Promise.all([p1, p2])
    expect(r1).toBe(r2)
    expect(r1).toEqual(makeResult(0))
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  test('요청이 settle된 뒤에는 새 호출이 실제로 fetch를 다시 낸다(영구 차단 방지)', async () => {
    const r1 = await requestCodeOnce('user@example.com', requestPasswordResetCode)
    expect(fetch).toHaveBeenCalledTimes(1)

    const r2 = await requestCodeOnce('user@example.com', requestPasswordResetCode)
    expect(fetch).toHaveBeenCalledTimes(2)
    expect(r1).not.toEqual(r2)
  })

  test('다른 target의 in-flight는 서로 뭉개지 않는다', async () => {
    const p1 = requestCodeOnce('a@example.com', requestPasswordResetCode)
    const p2 = requestCodeOnce('b@example.com', requestPasswordResetCode)
    expect(p1).not.toBe(p2)
    await Promise.all([p1, p2])
    expect(fetch).toHaveBeenCalledTimes(2)
  })
})
