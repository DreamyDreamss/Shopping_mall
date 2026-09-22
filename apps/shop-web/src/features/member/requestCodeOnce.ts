// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-006.md
import type { VerificationCodeResult } from '../../types'

/**
 * 비밀번호 재설정 코드 요청(1단계 제출·재전송)을 위한 **모듈 스코프 단일 in-flight 가드**.
 *
 * `refreshOnce.ts`(FUNC-member-004)와 동일한 패턴을 `target` 키로 복제한 것이다 — 공용 유틸로
 * 일반화하지 않고 이 FUNC 전용으로 작게 복제했다(타입이 다르고, 공용화하면 `refreshOnce.unit.test.ts`
 * 회귀 위험만 늘어난다, STORY 구현계획).
 *
 * 이 화면의 코드 요청은 현재 클릭 핸들러(1단계 제출·재전송 버튼)에서만 호출돼 StrictMode dev의
 * effect 이중 실행 대상이 **아니다**. 그럼에도 이 가드를 선제 적용하는 이유(AC9, STORY "프레임워크
 * 실행 모델 함정" 절): 향후 "단계 전환 시 코드 요청을 자동 재확인"하는 식으로 `useEffect`에 옮겨
 * 적는 리팩터링이 들어오면 `useSilentRefresh`(FUNC-member-004, RUN8 004 r1)와 같은 증상이 즉시
 * 재현된다 — 그래서 호출부가 아니라 **호출 대상 자체**를 구조적으로 이중 발사 불가능하게 만든다.
 * INF-MBR-006 자체는 idempotent(쿨다운 UPSERT no-op)라 이중 호출이 데이터 정합성을 깨지는 않지만,
 * AC9는 "네트워크 호출 자체가 두 번 나가지 않을 것"을 요구하므로 idempotency만으로는 충족되지 않는다.
 *
 * 같은 `target`으로 들어온 두 번째 호출은 첫 번째가 반환한 같은 Promise를 그대로 공유한다. in-flight
 * 요청이 settle되면(성공·실패 무관) 가드를 비워 다음 요청(재전송 포함)은 새로 나가게 한다.
 */
let inFlight: { target: string; promise: Promise<VerificationCodeResult> } | null = null

export function requestCodeOnce(
  target: string,
  fn: (target: string) => Promise<VerificationCodeResult>,
): Promise<VerificationCodeResult> {
  if (inFlight && inFlight.target === target) return inFlight.promise
  const promise = fn(target).finally(() => {
    if (inFlight?.promise === promise) inFlight = null
  })
  inFlight = { target, promise }
  return promise
}
