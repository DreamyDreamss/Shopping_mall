// test-storybook-ci.cjs — 축 E(story_gate.py, STORYBOOK_TEST_CMD)가 부르는 CI 러너.
//
// 왜: story_gate.py는 STORYBOOK_TEST_CMD가 project.env에 있으면 그 명령을 실행해
// play function까지 검증한다(없으면 play가 안 도는 내장 렌더러로 폴백). 이 스크립트가
// 그 명령의 구현체다 — storybook-static을 스스로 정적 서빙하고, 그 위에서
// `test-storybook`(playwright 기반, play function 실행)을 돌린 뒤 종료 코드를 그대로 돌려준다.
//
// 이 스크립트는 빌드를 하지 않는다(storybook-static이 없으면 즉시 실패) — 빌드 시점·트리거는
// 호출자(story_gate.py의 ensure_fresh, 또는 사람이 직접 build-storybook)가 정한다. 이 스크립트가
// 빌드까지 겸하면 "낡은 빌드로 통과시키지 않는다"는 story_gate.py의 신선도 판정을 우회하게 된다.
//
// CLI 도구 스크립트라 진행 로그(console.log)가 이 스크립트의 목적 자체다 — 규칙 web-no-console은
// `modules/shop-web/src/**/*.tsx`(화면 부품)만 대상이고 이 파일은 `scripts/`의 `.cjs`라 해당하지 않는다.

'use strict';

const fs = require('fs');
const path = require('path');
const http = require('http');
const { spawn } = require('child_process');

const ROOT = path.resolve(__dirname, '..');
const STATIC_DIR = path.join(ROOT, 'storybook-static');
const PORT = 6106;
const HOST = '127.0.0.1';
const BASE_URL = `http://${HOST}:${PORT}`;
const POLL_TIMEOUT_MS = 30000;
const POLL_INTERVAL_MS = 250;

function fail(message) {
  console.error(`[test-storybook-ci] ${message}`);
  process.exit(1);
}

function waitForServer(url, timeoutMs) {
  const deadline = Date.now() + timeoutMs;
  return new Promise((resolve, reject) => {
    const attempt = () => {
      const req = http.get(url, (res) => {
        res.resume();
        resolve();
      });
      req.on('error', () => {
        if (Date.now() > deadline) {
          reject(new Error(`서버가 ${timeoutMs}ms 안에 응답하지 않았습니다: ${url}`));
          return;
        }
        setTimeout(attempt, POLL_INTERVAL_MS);
      });
    };
    attempt();
  });
}

async function main() {
  if (!fs.existsSync(STATIC_DIR) || !fs.existsSync(path.join(STATIC_DIR, 'index.html'))) {
    fail(
      `storybook-static이 없습니다(${STATIC_DIR}) — 이 스크립트는 빌드를 하지 않습니다. ` +
        '먼저 `npm run build-storybook`을 실행하세요.'
    );
  }

  console.log(`[test-storybook-ci] storybook-static 정적 서빙 시작 → ${BASE_URL} (root: ${STATIC_DIR})`);

  const httpServer = require('http-server');
  const server = httpServer.createServer({ root: STATIC_DIR, cache: -1, logFn: () => {} });

  await new Promise((resolve, reject) => {
    server.listen(PORT, HOST, (err) => (err ? reject(err) : resolve()));
  });

  let exitCode = 1;
  try {
    await waitForServer(`${BASE_URL}/index.html`, POLL_TIMEOUT_MS);
    console.log('[test-storybook-ci] 서버 응답 확인 — test-storybook 실행');

    // 반드시 비동기 spawn을 쓴다 — spawnSync는 부모 프로세스의 이벤트 루프를 통째로 멈춰,
    // 같은 프로세스 안에서 돌아가는 위 http-server가 자식(npx test-storybook)의 요청에
    // 응답하지 못하게 만든다(랩 실측: spawnSync로 시도했을 때 test-storybook이 "storybook
    // 인스턴스가 안 떠 있다"며 항상 실패했다 — 서버가 죽은 게 아니라 이벤트 루프가 막혀
    // 그 요청을 처리할 차례가 오지 않은 것이었다).
    exitCode = await new Promise((resolve, reject) => {
      const child = spawn(
        'npx',
        ['test-storybook', '--url', BASE_URL, '--maxWorkers=1'],
        { cwd: ROOT, stdio: 'inherit', shell: true }
      );
      child.on('error', reject);
      child.on('exit', (code) => resolve(code === null ? 1 : code));
    });
    console.log(`[test-storybook-ci] test-storybook 종료 코드: ${exitCode}`);
  } finally {
    await new Promise((resolve) => server.close(resolve));
    console.log('[test-storybook-ci] 정적 서버 종료');
  }

  process.exit(exitCode);
}

main().catch((err) => {
  fail(err && err.stack ? err.stack : String(err));
});
