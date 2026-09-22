// SR-310 — CSS 파일 임포트(Button.css 등)를 위한 최소 타입 선언. 실제 처리는 Vite/Storybook
// 번들러가 하고, 이 파일은 tsc(타입 검사, `npm test`)가 `import './X.css'`를 인식하게만 한다.
declare module '*.css'
