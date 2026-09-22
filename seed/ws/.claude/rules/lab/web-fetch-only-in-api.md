---
paths: ["modules/shop-web/src/**/*.tsx"]
severity: must
checks:
  - id: web-fetch-only-in-api
    kind: regex
    glob: "modules/shop-web/src/{components,pages}/**/*.tsx"
    pattern: "\\bfetch\\s*\\("
    message: "부품·페이지에서 직접 fetch 금지 — src/api.ts를 거친다(프록시가 X-Api-Key를 붙이고 {items} 봉투를 푼다)"
  - id: web-no-console
    kind: regex
    glob: "modules/shop-web/src/**/*.tsx"
    pattern: "console\\.(log|debug)\\("
    message: "console.log를 남기지 않는다 — 스토리북 test-runner가 콘솔 오류를 실패로 센다"
---
# API 호출은 `src/api.ts` 한 곳

shop-web은 `/api/*`를 vite 프록시로 shop-api에 넘기고, 프록시가 `X-Api-Key`를 붙인다. 부품에서 `fetch`를 직접 부르면
헤더·봉투(`{items}`)·상태 코드 매핑이 흩어져 SR-231 랩에서 겪은 SPA 계약 불일치가 다시 생긴다.
