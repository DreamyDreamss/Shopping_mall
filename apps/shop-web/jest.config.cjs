// linked_func: FUNC-member-004
// linked_func: FUNC-member-007 — testMatch에 컴포넌트 테스트(.test.tsx) 패턴 추가
// spec: docs/05_설계서/member/INF/INF-MBR-005.md · INF-MBR-006.md · INF-MBR-007.md
// round1 QA FAIL 재작업 — "StrictMode 이중 마운트에서도 refresh 네트워크 요청 1회"·"redirect 검증"을
// test-storybook(브라우저 렌더)이 아니라 순수 로직 단위 테스트로 단언하기 위한 최소 jest 설정.
// jest/@swc/jest/@jest/globals는 @storybook/test-runner가 이미 끌어오는 기존 의존성이라 새 설치가
// 아니다(devDependencies에 명시만 추가) — package.json 참조.
/** @type {import('jest').Config} */
module.exports = {
  rootDir: __dirname,
  // 전역 기본은 'node'로 유지한다(기존 3개 *.unit.test.ts 회귀 방지) — jsdom이 필요한 컴포넌트
  // 테스트는 파일 상단 `/** @jest-environment jsdom */` 독블록으로 개별 지정한다(FUNC-member-007).
  testEnvironment: 'node',
  // Windows에서 <rootDir> 치환이 `D:/...\...` 처럼 구분자가 혼용돼 testMatch가 0건이 되는 사전
  // 존재 이슈가 있다(docs/KNOWN_ENV_ISSUES.md 참조, storybook test-runner에서도 동일 증상) —
  // <rootDir> 치환 없는 상대 glob으로 우회한다.
  testMatch: ['**/src/**/*.unit.test.ts', '**/src/**/*.test.tsx'],
  setupFiles: ['./jest.setup.cjs'],
  // SR-310 — 공통 컴포넌트가 처음으로 `.css` 파일을 직접 import한다(Button.css 등). jest(jsdom)는
  // 스타일시트 로더가 없어 그대로 두면 SyntaxError로 죽으므로 빈 목으로 돌린다(<rootDir> 치환은
  // moduleNameMapper 값 치환이라 testMatch의 글롭 구분자 혼용 이슈와는 무관하다).
  moduleNameMapper: {
    '\\.css$': '<rootDir>/jest.styleMock.cjs',
  },
  transform: {
    '^.+\\.tsx?$': ['@swc/jest', { jsc: { target: 'es2022', transform: { react: { runtime: 'automatic' } } } }],
  },
}
