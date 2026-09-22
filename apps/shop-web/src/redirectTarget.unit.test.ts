// linked_func: FUNC-member-004
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md
// round1 QA FAIL 필수수정 3·권고 4 재발 방지 — LoginPage.tsx의 redirect 처리가 이중 디코딩 없이
// 그대로 값을 쓰면서도 오픈 리다이렉트를 막는지 단위 테스트로 단언한다.
import { describe, expect, test } from '@jest/globals'
import { resolveRedirectTarget } from './redirectTarget'

describe('resolveRedirectTarget', () => {
  test('같은 오리진 상대경로는 그대로 통과한다', () => {
    expect(resolveRedirectTarget('/orders/123')).toBe('/orders/123')
  })

  test('절대 URL(오픈 리다이렉트)은 기본 "/"로 막는다', () => {
    expect(resolveRedirectTarget('https://evil.com')).toBe('/')
  })

  test('"%" 포함 상대경로는 이중 디코딩 없이 그대로 통과한다', () => {
    // useSearchParams().get()이 이미 퍼센트 디코딩을 끝낸 값을 준다고 가정 — 여기서 추가
    // decodeURIComponent를 하면 안 되므로, 입력에 남은 '%'가 그대로 보존돼야 한다.
    expect(resolveRedirectTarget('/orders/A%2F1')).toBe('/orders/A%2F1')
  })

  test('프로토콜 상대 URL("//")도 오픈 리다이렉트로 보고 막는다', () => {
    expect(resolveRedirectTarget('//evil.com')).toBe('/')
  })

  test('역슬래시 변형("/\\evil.com")도 오픈 리다이렉트로 보고 막는다', () => {
    // 브라우저 URL 파서가 특수 스킴에서 '\'를 '/'로 정규화해 "//evil.com"과 동일하게 해석될 수
    // 있다(round2 QA 권고 3, round3 재작업 지시 3).
    expect(resolveRedirectTarget('/\\evil.com')).toBe('/')
  })

  test('값이 없으면(null) 기본 "/"로 이동한다', () => {
    expect(resolveRedirectTarget(null)).toBe('/')
  })

  test('"/"로 시작하지 않는 상대경로도 오픈 리다이렉트로 보고 막는다', () => {
    expect(resolveRedirectTarget('evil.com')).toBe('/')
  })
})
