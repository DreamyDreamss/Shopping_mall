// linked_func: FUNC-member-004
// spec: docs/05_설계서/member/INF/INF-MBR-005.md
// round1 QA FAIL 필수수정 1 재발 방지 — App.tsx의 "부팅 시 1회" 무음 리프레시가 StrictMode(React 19)
// dev 더블 이펙트에서도 실제 네트워크 요청은 1회만 나가는지를 fetch 호출 횟수와 Promise identity로
// 단언한다. round2 QA 지적(무효 단언) 반영 — fetch mock이 호출마다 다른 객체를 반환하도록 바꿔
// "mock이 항상 같은 값을 주므로 dedup 없이도 통과"하던 허점을 없앴다. saveSession이 실제로
// 호출되는지는 이 파일이 단언하지 않는다(그건 App.tsx 쪽 통합 동작 — round2 QA 권고 2, round3
// 재작업 지시 2에서 이 주석 정정).
import { beforeEach, describe, expect, jest, test } from '@jest/globals'
import type { SessionResult } from './types'

/** 호출마다 다른 세션 객체를 만든다 — 재요청이 실제로 일어났는지 값으로도 구분하기 위해서다. */
function makeSession(seq: number): SessionResult {
  return {
    memberId: 'm1',
    memberName: '홍길동',
    grade: 'BASIC',
    apiKey: `k-rotated-${seq}`,
    refreshToken: `rt-rotated-${seq}`,
    refreshTokenExpiresAt: '2099-01-01T00:00:00',
  }
}

describe('refreshOnce — 부팅 무음 리프레시 in-flight 가드', () => {
  let refreshOnce: typeof import('./refreshOnce').refreshOnce
  let refreshSession: typeof import('./api').refreshSession
  let fetchCallSeq: number

  beforeEach(async () => {
    // round3 재작업 지시 3 — 테스트 전용 __resetRefreshOnceForTest를 프로덕션 export에서 없앴으므로,
    // 모듈을 매 케이스마다 새로 로드해 모듈 스코프 `inFlight` 가드를 초기 상태로 되돌린다.
    jest.resetModules()
    fetchCallSeq = 0
    global.fetch = jest.fn(async () => ({
      ok: true,
      json: async () => makeSession(fetchCallSeq++),
    })) as unknown as typeof fetch
    ;({ refreshOnce } = await import('./refreshOnce'))
    ;({ refreshSession } = await import('./api'))
  })

  test('StrictMode effect→cleanup→effect처럼 같은 토큰으로 두 번 호출해도 fetch는 1회만 나가고 같은 Promise를 공유한다', async () => {
    // App.tsx의 useEffect가 StrictMode dev에서 두 번 실행되는 것을 그대로 재현: 같은 refreshToken으로
    // 정리(cleanup) 없이 연달아 두 번 호출한다.
    const p1 = refreshOnce('rt-1', refreshSession)
    const p2 = refreshOnce('rt-1', refreshSession)

    // await 전에 Promise 그 자체의 identity를 비교한다 — dedup 가드가 없으면 fn(token)이 두 번
    // 호출돼 서로 다른 Promise 객체가 반환되므로 이 단언이 이 시점에 곧바로 실패한다(가드를
    // 지우면 반드시 깨진다, round2 QA 권고 4·round3 재작업 지시 4).
    expect(p1).toBe(p2)
    expect(fetch).toHaveBeenCalledTimes(1)

    const [r1, r2] = await Promise.all([p1, p2])
    expect(r1).toBe(r2)
    expect(r1).toEqual(makeSession(0))
    expect(fetch).toHaveBeenCalledTimes(1)
  })

  test('요청이 settle된 뒤에는 새 호출이 실제로 fetch를 다시 낸다(영구 차단 방지)', async () => {
    const r1 = await refreshOnce('rt-1', refreshSession)
    expect(fetch).toHaveBeenCalledTimes(1)

    const r2 = await refreshOnce('rt-1', refreshSession)
    expect(fetch).toHaveBeenCalledTimes(2)
    // mock이 호출마다 다른 객체를 주므로, 재요청이 실제로 일어났다면 값도 달라야 한다.
    expect(r1).not.toEqual(r2)
  })

  test('다른 토큰의 in-flight 요청은 서로 뭉개지 않는다', async () => {
    const p1 = refreshOnce('rt-a', refreshSession)
    const p2 = refreshOnce('rt-b', refreshSession)
    expect(p1).not.toBe(p2)
    await Promise.all([p1, p2])
    expect(fetch).toHaveBeenCalledTimes(2)
  })
})
