// linked_func: FUNC-member-004
// linked_func: FUNC-member-007 — "비밀번호를 잊으셨나요" 링크 추가(AC6, LoginForm.tsx 자체는 무변경)
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md
import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { LoginForm, type LoginCredentials, type LoginFormError } from '../components/LoginForm'
import { ApiError, fetchCartItemCount, login, logout } from '../api'
import { resolveRedirectTarget } from '../redirectTarget'
import { clearSession, loadSession, saveSession } from '../session'
import type { SessionResult } from '../types'
import { AppShell } from '../features/shop/AppShell'

/**
 * 로그인 페이지 컨테이너 — `pages/`라 스토리 대상이 아니다(규칙 `story-per-component`). SR-311 전에는
 * Gnb/헤더가 전혀 없던 화면이다("로그인 계열"도 앱 셸 적용 범위, 확정문답 scr_scope) — 다른 쇼핑
 * 화면과 동일한 session/cartItemCount/searchValue 패턴을 그대로 복제한다(신규 신원 확인 경로 없음).
 */
export default function LoginPage() {
  const nav = useNavigate()
  const [params] = useSearchParams()
  const [value, setValue] = useState<LoginCredentials>({ email: '', password: '' })
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<LoginFormError | null>(null)

  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [cartItemCount, setCartItemCount] = useState(0)
  const [searchValue, setSearchValue] = useState('')

  // 다른 쇼핑 화면과 동일 규칙 — 세션이 있을 때만 장바구니 수량을 조회한다(신규 신원 확인 경로 아님).
  useEffect(() => {
    if (!session?.memberId) { setCartItemCount(0); return }
    let cancelled = false
    fetchCartItemCount(session.memberId)
      .then(count => { if (!cancelled) setCartItemCount(count) })
      .catch(() => { if (!cancelled) setCartItemCount(0) })
    return () => { cancelled = true }
  }, [session?.memberId])

  // 다른 쇼핑 화면과 동일 로직(복제, 훅 추출하지 않음 — 기존 화면들의 관례 일관성 유지).
  const handleLogout = () => {
    const apiKey = session?.apiKey
    const clear = () => { clearSession(); setSession(null) }
    if (!apiKey) { clear(); return }
    void logout(apiKey).catch(() => {}).finally(clear)
  }

  const handleSearchSubmit = () => {
    const keyword = searchValue.trim()
    nav(keyword ? `/shop/products?keyword=${encodeURIComponent(keyword)}` : '/shop/products')
  }

  const handleSubmit = async () => {
    setBusy(true)
    setError(null)
    try {
      const loggedInSession = await login(value.email, value.password)
      saveSession(loggedInSession)
      setSession(loggedInSession) // AppShell(Gnb)이 이 화면을 벗어나기 전에도 즉시 로그인 상태를 반영한다.
      // 프론트 전용 관례: /#/login?redirect=<encodeURIComponent(path)> — 없으면 기본 '/'로 이동
      // (서버와 무관한 라우팅 계약, STORY "계약" 절). useSearchParams().get()이 이미 퍼센트 디코딩을
      // 끝낸 값을 주므로 여기서 추가로 decodeURIComponent하지 않는다(이중 디코딩 방지 + 오픈
      // 리다이렉트 차단은 resolveRedirectTarget에 위임, round1 QA FAIL 재작업 지시 3·4).
      nav(resolveRedirectTarget(params.get('redirect')))
    } catch (e) {
      if (e instanceof ApiError) {
        setError({ code: e.code, message: e.message, retryAfterSeconds: e.retryAfterSeconds })
      } else {
        setError({ code: 'MBR-5000', message: e instanceof Error ? e.message : String(e) })
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ maxWidth: 400, margin: '60px auto', padding: '20px 18px', fontFamily: 'system-ui, sans-serif' }}>
        <h1 style={{ fontSize: 18, marginBottom: 18 }}>로그인</h1>
        <LoginForm value={value} onChange={setValue} onSubmit={() => void handleSubmit()} busy={busy} error={error} />
        <Link to="/password-reset" style={{ display: 'inline-block', marginTop: 14, fontSize: 12.5, color: '#0b4ea2' }}>
          비밀번호를 잊으셨나요?
        </Link>
      </div>
    </AppShell>
  )
}
