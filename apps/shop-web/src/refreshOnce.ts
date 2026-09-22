// linked_func: FUNC-member-004
// spec: docs/05_설계서/member/INF/INF-MBR-005.md
import type { SessionResult } from './types'

/**
 * 부팅 시 무음 리프레시(App.tsx `useSilentRefresh`)를 위한 **모듈 스코프 단일 in-flight 가드**.
 *
 * React 19 StrictMode dev는 같은 `useEffect`를 effect→cleanup→effect 순서로 두 번 실행한다(같은
 * fiber, 동기적으로 연이어). 이 두 번째 실행이 `refreshSession()`을 다시 POST하면, INF-MBR-005가
 * 성공 시 구 토큰을 즉시 폐기하는 **회전 API**라 두 번째 요청이 첫 요청의 `revokeByTokenHash` 뒤에
 * 도달할 때 401(`MBR-4012`)을 받아 무음으로 세션이 지워진다(round1 QA FAIL 근거). `alive` 같은
 * 언마운트 플래그는 state 쓰기만 막을 뿐 **두 번째 네트워크 요청 자체를 막지 못한다** — 그래서
 * 요청을 발사하기 전 단계에서 모듈 스코프로 dedup한다.
 *
 * 같은 `token`으로 들어온 두 번째 호출은 첫 번째가 반환한 **같은 Promise를 그대로 공유**한다(재요청
 * 금지). in-flight 요청이 settle되면(성공·실패 무관) 가드를 비워 다음 부팅이나 재로그인에서는 새
 * 요청이 나가게 한다 — 그렇지 않으면 실제로 새 토큰이 필요한 후속 호출까지 영구히 막힌다.
 *
 * `fn`을 인자로 받는 이유는 두 가지: (1) 기존 `refreshSession()`을 감싸기만 하고 새 API를 만들지
 * 않기 위해(재작업 지시 1), (2) 테스트에서 실제 `fetch`를 목으로 바꿔 호출 횟수를 단언하기 위해
 * (`refreshOnce.unit.test.ts`).
 *
 * round2 QA 권고 4(round3 재작업 지시 3) — 테스트 전용 리셋 함수를 프로덕션 export로 내보내던 것을
 * 없앴다. 테스트는 그 대신 매 케이스마다 `jest.resetModules()` + `await import('./refreshOnce')`로
 * 이 모듈을 새로 로드해 `inFlight`를 초기 상태(`null`)로 되돌린다 — 프로덕션 번들에는 테스트만을
 * 위한 진입점이 남지 않는다.
 */
let inFlight: { token: string; promise: Promise<SessionResult> } | null = null

export function refreshOnce(
  token: string,
  fn: (token: string) => Promise<SessionResult>,
): Promise<SessionResult> {
  if (inFlight && inFlight.token === token) return inFlight.promise
  const promise = fn(token).finally(() => {
    if (inFlight?.promise === promise) inFlight = null
  })
  inFlight = { token, promise }
  return promise
}
