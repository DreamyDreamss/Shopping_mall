// linked_func: FUNC-member-004 — 라우트 정의를 App.tsx로 이동(로그인 라우트 추가를 위해)
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import App, { applyShopBootRedirect } from './App'
import './styles/tokens.css'

// SR-302 재작업(round 2 QA FAIL 필수 수정 1) — 라우터(HashRouter)가 초기 해시를 읽기 전, 부팅 시
// 정확히 한 번만 "/shop 서버 경로 + 해시 없음"을 쇼핑 홈으로 보낸다(App.tsx 주석 참조). 컴포넌트
// 렌더 안이 아니라 여기(모듈 최상위, render 호출 이전)에서 호출해야 "매 렌더 재평가"가 아니라
// "부팅 1회성 사이드이펙트"가 된다.
applyShopBootRedirect()

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
