// linked_func: FUNC-member-004 — 프록시 헤더 주입을 "폴백"으로 변경(추가 경로만, 기존 동작 불변)
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md
import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// shop-web — 랩의 프론트 모듈(SM 실증용). shop-api(:8087)를 소비한다.
// 프록시로 /api를 백엔드에 넘겨 CORS 없이 개발한다.
const FALLBACK_API_KEY = process.env.ORDER_API_KEY || 'lab-admin-key'

// SR-301(INF-ORD-017) — shop-api가 빌드 산출물을 /shop/** 서브패스로 서빙한다. Vite 기본
// base('/')로 빌드하면 index.html이 자산을 도메인 루트 기준 절대경로(/assets/...)로 참조해
// /shop 밑에서 서빙되는 순간 브라우저가 루트에서 자산을 찾다 404가 난다 — Spring 쪽 설정으로는
// 고칠 수 없는 문제라 빌드 시점 base 설정으로 해소한다. command==='build'로만 조건화해
// npm run dev(:5273, command==='serve') 동작은 그대로 — 개발 서버 구성 변경 대상이 아니다.
export default defineConfig(({ command }) => ({
  base: command === 'build' ? '/shop/' : '/',
  plugins: [react()],
  server: {
    port: 5273,
    // dev 전용 — shop-api는 /api/** 에 X-Api-Key를 요구한다(SR-204). 브라우저 코드에 키를 두지 않고
    // 프록시가 붙인다.
    // FUNC-member-004(SR-232)부터: 종전 정적 `headers` 옵션은 클라이언트가 무엇을 보내든 admin 키로
    // 무조건 덮어썼다(실측: vite http-proxy-3 setupOutgoing이 options.headers를 나중에 병합해 항상
    // 이긴다). 로그인·리프레시가 발급한 회원별 apiKey를 향후 어떤 호출이 실어 보내도 dev 프록시가
    // 조용히 admin 키로 바꿔치기하지 않도록, 클라이언트가 이미 `X-Api-Key`를 보낸 요청은 그대로 두고
    // 안 보낸 요청만 admin 키로 폴백한다(추가 경로만 — 사람 결정 1). 기존 주문목록/상세 등은 전부
    // `X-Api-Key`를 직접 세팅하지 않으므로("안 보낸 요청") 동작이 그대로 유지된다(회귀 확인 완료,
    // STORY "폴백·우회 경로의 자격 판정" 절).
    proxy: {
      '/api': {
        target: process.env.ORDER_API_BASE || 'http://localhost:8087',
        changeOrigin: true,
        configure: proxy => {
          proxy.on('proxyReq', (proxyReq, req) => {
            if (!req.headers['x-api-key']) proxyReq.setHeader('X-Api-Key', FALLBACK_API_KEY)
          })
        },
      },
    },
  },
}))
