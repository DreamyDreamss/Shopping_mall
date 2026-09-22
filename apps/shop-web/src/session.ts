// linked_func: FUNC-member-004
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md
import type { SessionResult } from './types'

/**
 * 세션 저장 — localStorage 단일 키에 통째로 저장/교체한다(STORY "데이터" 절, 사람 결정 3).
 * 여러 키로 쪼개면 갱신 도중(탭 종료·예외) "일부만 갱신된 세션"이 남을 수 있어, 반드시
 * 이 한 곳의 `saveSession`/`clearSession`만으로 원자적 교체 하나로 처리한다.
 *
 * 후속: `refreshToken`을 localStorage에 두는 것은 XSS로 탈취 가능한 노출 위험이 있다 — 랩 규모에서는
 * 수용하되(현재 백엔드 계약이 토큰을 JSON 바디로 반환해 다른 선택지가 없다), httpOnly 쿠키 전환은
 * 백엔드 계약 변경이 선행돼야 하는 후속 SR 대상이다(사람 결정 3, STORY "범위 밖" 절과 동일 결론).
 */
const STORAGE_KEY = 'sl.member.session'

export function saveSession(session: SessionResult): void {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(session))
}

export function loadSession(): SessionResult | null {
  const raw = localStorage.getItem(STORAGE_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as SessionResult
  } catch {
    return null
  }
}

export function clearSession(): void {
  localStorage.removeItem(STORAGE_KEY)
}
