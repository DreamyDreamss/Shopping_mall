// linked_func: FUNC-member-004
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md

/**
 * 로그인 성공 후 이동할 경로를 정한다(`LoginPage.tsx`).
 *
 * `useSearchParams().get('redirect')`는 이미 퍼센트 디코딩이 끝난 값을 돌려준다 — STORY "계약" 절이
 * 정한 `?redirect=<encodeURIComponent(path)>` 생산자와 합치면, 여기서 `decodeURIComponent`를 한 번
 * 더 하면 `%` 포함 경로가 깨지거나(`/search?q=50%25` → `/search?q=50%`) 디코딩 불가 시퀀스에서
 * `URIError`가 난다(round1 QA FAIL 근거, 재작업 지시 3) — 그래서 받은 값을 그대로 쓴다.
 *
 * `/`로 시작하고, 그 뒤 두 글자가 `/`나 `\`의 조합(`//`·`/\`·`\/`류)으로 시작하지 않는 **같은
 * 오리진 상대경로**만 허용한다. 그 외(절대 URL·프로토콜 상대 URL·역슬래시 변형)는 오픈 리다이렉트이므로
 * 기본 `/`로 돌려보낸다(재작업 지시 4 — 지금은 HashRouter라 당장 뚫리지 않지만 BrowserRouter 전환 시
 * 전형적 오픈 리다이렉트가 되는 시한폭탄을 미리 제거).
 *
 * round2 QA 권고 3(round3 재작업 지시 3) — 종전엔 `//evil.com`(프로토콜 상대)만 막고 `/\evil.com`
 * (역슬래시 변형)은 통과시켰다. 브라우저 URL 파서는 특수 스킴에서 `\`를 `/`로 정규화하므로 그 값이
 * `//evil.com`으로 해석될 수 있다 — 첫 두 글자가 `/`·`\` 조합이면 전부 막도록 넓혔다.
 */
export function resolveRedirectTarget(redirect: string | null): string {
  if (redirect && redirect.startsWith('/') && !/^[/\\]{2}/.test(redirect)) return redirect
  return '/'
}
