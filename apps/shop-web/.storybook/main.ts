import type { StorybookConfig } from '@storybook/react-vite'

// shop-web 스토리북 — 화면 부품의 상태를 앱 없이 세운다.
// Speclinker가 읽는 것은 빌드 산출물의 `index.json`(스토리 id·title·importPath·tags)이다 —
// 이 파일의 스키마가 아니라 그 계약을 본다(스토리북 버전이 올라가도 안 깨지게).
const config: StorybookConfig = {
  stories: ['../src/**/*.stories.@(ts|tsx)'],
  framework: { name: '@storybook/react-vite', options: {} },
  // SR-306.1 재작업(round3) — /images/products/*.svg 는 shop-api가 서빙하는 정적 자산이다
  // (정본: modules/shop-api/src/main/resources/static, SR-306 #2). 스토리북은 백엔드 없이
  // 단독 렌더되므로 사본을 두면 원본과 어긋날 수 있어(사람 지시) 사본 대신 staticDirs로 그
  // 원본 디렉터리를 그대로 가리킨다. 경로는 이 설정 파일(.storybook/main.ts) 위치 기준 상대경로다
  // (storybook 실행 cwd가 아니라 config 파일 디렉터리 기준 — 실측 확인: build-storybook 산출물에
  // 이미지가 실제로 복사되는지로 검증) — `.storybook` → `shop-web` → `modules` → `shop-api/...`
  // 2단계 상승. dev(`npm run storybook`)·build(`npm run build-storybook`) 둘 다 이 설정을 그대로 쓴다.
  // 이미지로드실패 상태(ProductCard.stories.tsx)는 round4부터 404 경로 대신 깨진 data URI를
  // 쓴다 — 축E(story_gate.py)가 콘솔 error 1건만 있어도 스토리를 깨진 것으로 판정해 404
  // 네트워크 요청을 남길 수 없기 때문(round3 QA 실측, round4 재작업 지시 1).
  // 전제: 이 경로는 shop-api 저장소가 shop-web과 형제 디렉터리(`modules/` 아래 나란히)에
  // 체크아웃돼 있어야 성립한다 — 없으면 storybook dev/build 둘 다 하드 실패한다(round3 QA
  // 필수수정 3, 별도 git 저장소 간 크로스 참조).
  staticDirs: ['../../shop-api/src/main/resources/static'],
}
export default config
