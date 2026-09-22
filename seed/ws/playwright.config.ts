// 이 파일은 build_e2e_plan.py가 **없을 때만** 생성합니다(기존 설정은 건드리지 않음).
import fs from 'node:fs'
import { defineConfig } from '@playwright/test'

const STORAGE = process.env.E2E_STORAGE_STATE ?? '.speclinker/e2e_storage.json'

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list']],
  use: {
    baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:8087',
    // 로그인 세션: 사람이 CDP 브라우저에서 한 번 로그인한 뒤
    //   node <PLUGIN>/scripts/e2e_capture_state.js --ws .
    // 로 저장한 상태를 재사용한다(자동 로그인 우회를 하지 않기 위한 정식 경로).
    // 파일이 없으면 undefined → 비로그인으로 돌고, 인증이 필요한 화면은 로그인 벽에서 실패한다.
    storageState: fs.existsSync(STORAGE) ? STORAGE : undefined,
    headless: true,
    screenshot: 'only-on-failure',
    trace: 'retain-on-failure',
  },
})
