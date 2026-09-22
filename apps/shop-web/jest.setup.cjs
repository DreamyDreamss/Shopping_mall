// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-006.md · INF-MBR-007.md
// jest-environment-jsdom(v30, jsdom 26)이 TextEncoder/TextDecoder를 전역에 노출하지 않아
// react-router(dist/development/index.js 모듈 로드 시점)가 즉시 ReferenceError로 죽는다
// (PasswordResetPage.test.tsx 실측). Node 내장 TextEncoder/TextDecoder를 전역에 채워 넣는다 —
// node testEnvironment(기존 *.unit.test.ts)에는 이미 있으므로 이 setupFiles는 그쪽엔 무해하다.
if (typeof global.TextEncoder === 'undefined') {
  const { TextEncoder, TextDecoder } = require('node:util')
  global.TextEncoder = TextEncoder
  global.TextDecoder = TextDecoder
}
