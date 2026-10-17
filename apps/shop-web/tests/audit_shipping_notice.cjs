#!/usr/bin/env node
/**
 * audit_shipping_notice.cjs — 상품 상세(UIS-ORD-010) 배송비 3,000원 표시를 실제 브라우저로
 * 재검증하는 독립 CLI(automation-audit 검증 전용).
 *
 * 흐름:
 *   1. origin이 loopback(http/https + localhost·127.0.0.1·::1)인지 먼저 검사한다. 아니면
 *      네트워크 요청을 보내지 않고 즉시 실패로 기록한다.
 *   2. `{origin}/api/products`에서 실제 양수 가격 SKU 1건을 확보한다.
 *   3. playwright-core Chromium headless로 `{origin}/shop#/shop/products/{sku}`를 연다.
 *      페이지의 실제 JS 런타임 오류(pageerror)와 실패한 네트워크 요청(requestfailed)을
 *      관측한다.
 *   4. 로딩 스켈레톤을 지나 `section[aria-label="상품 정보"] h1` 텍스트가 선택한 productName과,
 *      `[aria-label="배송비 안내"]` 텍스트가 정확히 "배송비 3,000원"과 일치하는지 단언한다.
 *      DOM 텍스트가 정상이어도 pageerror·requestfailed가 관측됐으면 실패로 판정한다.
 *   5. 결과(+스크린샷)를 출력 JSON에 기록한다. 통과만 exit 0 — 그 외 모든 경로는 exit 1.
 *      브라우저 단계 전에 실패하면 이전 실행의 스크린샷을 지우거나 screenshot을 null로 남긴다.
 *
 * 사용법: node audit_shipping_notice.cjs <앱 origin> [출력 JSON 경로]
 *   출력 경로 생략 시 기본값: 이 스크립트 기준 ../_tmp/shipping-audit.json
 *
 * 운영 코드(화면·API·DB·배송비 계산)는 건드리지 않는다 — 이 스크립트는 그 운영 동작을
 * 외부에서 관찰만 하는 감사 전용 스크립트다. `npm test`(tsc+jest)에는 엮지 않는다.
 *
 * 종료는 브라우저를 열었다면 `browser.close()`가 완료된 뒤에만 한다(try/finally의 close를
 * process.exit()로 건너뛰지 않는다) — main()이 종료코드를 반환하고, 호출부가 그 Promise가
 * resolve된 뒤에 process.exit한다.
 */
'use strict';
const path = require('path');
const fs = require('fs');
const crypto = require('crypto');
const { chromium } = require(path.join(__dirname, '..', 'node_modules', 'playwright-core'));

const EXPECTED_SHIPPING_TEXT = '배송비 3,000원';
const LOOPBACK_HOSTS = new Set(['localhost', '127.0.0.1', '::1']);

function nowIso() {
  return new Date().toISOString();
}

function scriptSha256() {
  return crypto.createHash('sha256').update(fs.readFileSync(__filename)).digest('hex');
}

function resolveOutputPath(argPath) {
  if (argPath) return path.resolve(process.cwd(), argPath);
  return path.resolve(__dirname, '..', '_tmp', 'shipping-audit.json');
}

function screenshotPathFor(outputPath) {
  const dir = path.dirname(outputPath);
  const base = path.basename(outputPath, path.extname(outputPath));
  return path.join(dir, base + '.png');
}

function writeJson(outputPath, data) {
  fs.mkdirSync(path.dirname(outputPath), { recursive: true });
  fs.writeFileSync(outputPath, JSON.stringify(data, null, 2), 'utf8');
}

// http/https + loopback 호스트(localhost·127.0.0.1·::1)만 허용한다. 그 밖은 네트워크
// 요청을 보내기 전에 거절한다.
function checkLoopbackOrigin(originArg) {
  let url;
  try {
    url = new URL(originArg);
  } catch (e) {
    return { ok: false, reason: 'origin 파싱 실패: ' + (e && e.message ? e.message : String(e)) };
  }
  if (url.protocol !== 'http:' && url.protocol !== 'https:') {
    return { ok: false, reason: `허용되지 않는 scheme: ${url.protocol}` };
  }
  const bareHost = url.hostname.toLowerCase().replace(/^\[|\]$/g, '');
  if (!LOOPBACK_HOSTS.has(bareHost)) {
    return { ok: false, reason: `loopback이 아닌 주소: ${bareHost}` };
  }
  return { ok: true };
}

async function main() {
  const origin = process.argv[2];
  const outputArg = process.argv[3];

  const outputPath = resolveOutputPath(outputArg);
  const screenshotPath = screenshotPathFor(outputPath);
  const startedAt = nowIso();
  const sha256 = scriptSha256();

  // 공통 베이스 — 단계마다 덮어써 나간다. screenshot은 실제로 캡처했을 때만 채운다.
  const base = {
    startedAt,
    scriptSha256: sha256,
    apiUrl: null,
    pageUrl: null,
    sku: null,
    productName: null,
    screenshot: null,
  };

  function clearStaleScreenshot() {
    try {
      fs.rmSync(screenshotPath, { force: true });
    } catch {
      /* 없으면 지울 것도 없다 */
    }
  }

  function fail(reason, detail, screenshotTaken) {
    const result = {
      ...base,
      status: 'failed',
      finishedAt: nowIso(),
      reason,
      screenshot: screenshotTaken ? screenshotPath : null,
    };
    if (detail != null) result.detail = String(detail).slice(0, 300);
    writeJson(outputPath, result);
    return result;
  }

  // 인자 누락 — 브라우저 단계 전이므로 이전 실행의 스크린샷을 지워 재사용을 막는다.
  if (!origin) {
    clearStaleScreenshot();
    fail('인자 누락(origin)', '사용법: node audit_shipping_notice.cjs <앱 origin> [출력 JSON 경로]');
    return 1;
  }

  // loopback이 아니면 네트워크 요청을 보내기 전에 즉시 거절한다.
  const originCheck = checkLoopbackOrigin(origin);
  if (!originCheck.ok) {
    clearStaleScreenshot();
    fail('loopback이 아닌 주소', originCheck.reason);
    return 1;
  }

  const normalizedOrigin = origin.replace(/\/$/, '');
  base.apiUrl = normalizedOrigin + '/api/products';

  // 1) 출력 JSON을 먼저 running으로 덮어써 이전 성공 파일을 무효화하고, 이전 스크린샷도
  //    같은 자리에서 지운다(브라우저 단계에서 다시 찍을 때까지는 screenshot:null 상태).
  clearStaleScreenshot();
  writeJson(outputPath, { ...base, status: 'running', args: { origin: normalizedOrigin, outputPath } });

  // 2) 실제 양수 가격 SKU 1건 확보.
  let product;
  try {
    const res = await fetch(base.apiUrl, { headers: { Accept: 'application/json' } });
    if (!res.ok) {
      fail('상품 목록 응답 실패', `HTTP ${res.status}`);
      return 1;
    }
    let body;
    try {
      body = await res.json();
    } catch (e) {
      fail('상품 목록 응답 실패', 'JSON 파싱 실패: ' + e);
      return 1;
    }
    const items = Array.isArray(body) ? body : (body && Array.isArray(body.items) ? body.items : []);
    product = items.find((it) => it && it.sku && it.productName && typeof it.price === 'number' && it.price > 0);
  } catch (e) {
    fail('상품 목록 응답 실패', e && e.message ? e.message : String(e));
    return 1;
  }

  if (!product) {
    fail('양수 가격 상품 없음');
    return 1;
  }

  base.sku = product.sku;
  base.productName = product.productName;
  base.pageUrl = normalizedOrigin + '/shop#/shop/products/' + encodeURIComponent(product.sku);

  // 3) Chromium headless 기동(실패 시 시스템 Chrome 폴백 — 이 저장소 기존 관례).
  let browser;
  try {
    browser = await chromium.launch({ headless: true });
  } catch {
    try {
      browser = await chromium.launch({ headless: true, channel: 'chrome' });
    } catch (e2) {
      fail('Chromium 실행 불가', e2 && e2.message ? e2.message : String(e2));
      return 1;
    }
  }

  // 브라우저가 열린 뒤부터는 반드시 finally에서 닫는다 — 이 함수가 반환하는 exitCode는
  // close()가 완료된 뒤에만 호출부에 전달된다(process.exit()로 finally를 건너뛰지 않는다).
  let exitCode = 1;
  try {
    const page = await browser.newPage();

    const pageErrors = [];
    const requestFailures = [];
    page.on('pageerror', (err) => {
      pageErrors.push(err && err.message ? err.message : String(err));
    });
    page.on('requestfailed', (req) => {
      const failure = req.failure();
      requestFailures.push(`${req.method()} ${req.url()} — ${failure ? failure.errorText : 'unknown'}`);
    });

    let navError = null;
    try {
      await page.goto(base.pageUrl, { waitUntil: 'domcontentloaded' });
    } catch (e) {
      navError = e;
    }
    if (navError) {
      let screenshotTaken = false;
      try {
        await page.screenshot({ path: screenshotPath });
        screenshotTaken = true;
      } catch {
        /* 최선노력 */
      }
      fail('상품 상세 페이지 로딩 실패', navError.message || String(navError), screenshotTaken);
      return exitCode;
    }

    let h1El;
    let selectorError = null;
    try {
      h1El = await page.waitForSelector('section[aria-label="상품 정보"] h1', { timeout: 15000 });
    } catch (e) {
      selectorError = e;
    }
    if (selectorError) {
      let screenshotTaken = false;
      try {
        await page.screenshot({ path: screenshotPath });
        screenshotTaken = true;
      } catch {
        /* 최선노력 */
      }
      fail('상품 상세 페이지 로딩 실패', selectorError.message || String(selectorError), screenshotTaken);
      return exitCode;
    }

    const h1Text = (await h1El.textContent() || '').trim();
    const shippingEl = await page.$('[aria-label="배송비 안내"]');
    const shippingText = shippingEl ? (await shippingEl.textContent() || '').trim() : null;

    // 판정 전에 화면 상태를 남긴다(성공·실패 공통).
    let screenshotTaken = false;
    try {
      await page.screenshot({ path: screenshotPath });
      screenshotTaken = true;
    } catch {
      /* 최선노력 */
    }

    if (h1Text !== product.productName) {
      fail('상품명 표시 불일치', `expected="${product.productName}" actual="${h1Text}"`, screenshotTaken);
      return exitCode;
    }

    if (shippingText !== EXPECTED_SHIPPING_TEXT) {
      fail('배송비 표시 불일치', `expected="${EXPECTED_SHIPPING_TEXT}" actual="${shippingText}"`, screenshotTaken);
      return exitCode;
    }

    // DOM 텍스트가 정상이어도 실제 페이지 런타임 오류·네트워크 요청 실패가 관측됐으면 실패다.
    if (pageErrors.length > 0) {
      fail('페이지 런타임 오류 발생', pageErrors.join(' | '), screenshotTaken);
      return exitCode;
    }
    if (requestFailures.length > 0) {
      fail('네트워크 요청 실패', requestFailures.join(' | '), screenshotTaken);
      return exitCode;
    }

    const result = {
      ...base,
      status: 'passed',
      finishedAt: nowIso(),
      screenshot: screenshotTaken ? screenshotPath : null,
    };
    writeJson(outputPath, result);
    exitCode = 0;
    return exitCode;
  } finally {
    await browser.close();
  }
}

main()
  .then((code) => {
    process.exit(typeof code === 'number' ? code : 0);
  })
  .catch((e) => {
    console.error('audit_shipping_notice 실행 중 예외: ' + (e && e.stack ? e.stack : e));
    process.exit(1);
  });
