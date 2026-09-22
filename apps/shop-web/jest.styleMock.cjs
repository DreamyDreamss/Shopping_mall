// SR-310 — jest.config.cjs moduleNameMapper가 `*.css` 임포트를 이 파일로 돌린다. jest(jsdom)는
// 스타일시트를 처리하지 않으므로(실제 스타일은 Vite/Storybook 몫), 컴포넌트 테스트가 `import
// './Button.css'` 같은 구문을 만나도 빈 모듈로 통과시킨다.
module.exports = {}
