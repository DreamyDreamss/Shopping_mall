// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-006.md · INF-MBR-007.md
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ApiError, confirmPasswordReset, fetchCartItemCount, logout, requestPasswordResetCode } from '../api'
import { clearSession, loadSession } from '../session'
import type { SessionResult } from '../types'
import { AppShell } from '../features/shop/AppShell'
import { requestCodeOnce } from '../features/member/requestCodeOnce'
import { PasswordResetRequestStep, type PasswordResetRequestError } from '../features/member/PasswordResetRequestStep'
import { PasswordResetCodeStep, type PasswordResetCodeError } from '../features/member/PasswordResetCodeStep'
import { PasswordResetPasswordStep, type PasswordResetPasswordError } from '../features/member/PasswordResetPasswordStep'
import { PasswordResetDoneStep } from '../features/member/PasswordResetDoneStep'

type Step = 'request' | 'code' | 'password' | 'done'

const RESEND_COOLDOWN_SECONDS = 60

/** 휴대폰은 하이픈·공백을 제거해 숫자만, 이메일은 입력 그대로 보낸다(AC2, STORY 계약 메모). */
function normalizeTargetForSend(raw: string): string {
  const trimmed = raw.trim()
  if (trimmed.includes('@')) return trimmed
  return trimmed.replace(/[-\s]/g, '')
}

function toDisplayError(e: unknown): { code: string; message: string } {
  if (e instanceof ApiError) return { code: e.code, message: e.message }
  return { code: 'MBR-5000', message: e instanceof Error ? e.message : String(e) }
}

/**
 * 비밀번호 재설정 페이지 컨테이너(3단계 상태기계 — `pages/`라 스토리 대상이 아니다, 규칙
 * `story-per-component`). `PasswordResetXStep` 4종을 현재 단계에 따라 스위치 렌더한다.
 *
 * "코드 확인"은 별도 API 호출이 아니다 — INF-MBR-007이 코드 검증과 비밀번호 반영을 원자적으로
 * 한 번에 하므로, 2단계는 로컬 형식 검증만 하고 실제 코드 검증은 3단계 제출(confirm)이 트리거한다
 * (STORY "순서·보안" 절). confirm 실패(410/409-4102/409-4103)는 이 컨테이너가 응답 코드를 보고
 * 2단계 화면으로 되돌리며, `newPassword` 값은 상태에 남겨 재입력을 요구하지 않는다.
 */
export default function PasswordResetPage() {
  const navigate = useNavigate()
  const [step, setStep] = useState<Step>('request')

  // SR-311 — 앱 셸 적용("로그인 계열"도 범위, 확정문답 scr_scope). 다른 쇼핑 화면과 동일한
  // session/cartItemCount/searchValue 패턴을 그대로 복제한다(신규 신원 확인 경로 없음).
  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [cartItemCount, setCartItemCount] = useState(0)
  const [searchValue, setSearchValue] = useState('')

  // 1단계
  const [target, setTarget] = useState('')
  const [requestBusy, setRequestBusy] = useState(false)
  const [requestError, setRequestError] = useState<PasswordResetRequestError | null>(null)

  // 2단계 — 요청 응답으로 채워짐
  const [normalizedTarget, setNormalizedTarget] = useState('')
  const [channel, setChannel] = useState<'EMAIL' | 'SMS'>('EMAIL')
  // 재작업 지시 1(round2 QA CONCERNS 1) — 카운트다운은 "2단계 체류 초"가 아니라 벽시계(epoch ms)
  // 기준으로 계산한다. expiresAt/resendAvailableAt은 목표 시각(ms)만 들고, remainingSeconds/
  // resendRemainingSeconds는 `now`와의 차로 매 tick 파생한다 — 3단계에 머무는 동안에도 실제
  // 경과시간이 반영되고(409 MBR-4102로 2단계 복귀해도 과대 표시되지 않음), 인터벌은 현재 단계와
  // 무관하게(requestSeq가 있는 한) 계속 돈다.
  const [expiresAt, setExpiresAt] = useState<number | null>(null)
  const [resendAvailableAt, setResendAvailableAt] = useState<number | null>(null)
  const [now, setNow] = useState(() => Date.now())
  const [resendBusy, setResendBusy] = useState(false)
  const [requestSeq, setRequestSeq] = useState(0)
  const [code, setCode] = useState('')
  const [codeError, setCodeError] = useState<PasswordResetCodeError | null>(null)

  // 3단계
  const [newPassword, setNewPassword] = useState('')
  const [passwordBusy, setPasswordBusy] = useState(false)
  const [passwordError, setPasswordError] = useState<PasswordResetPasswordError | null>(null)

  // 남은 유효시간·재전송 쿨다운은 위 expiresAt/resendAvailableAt(epoch ms)로부터 매 렌더 파생한다 —
  // 실제 값을 들고 있는 것은 이 두 파생 변수가 아니라 목표 시각이라, 3단계에 머무는 동안에도 `now`가
  // 계속 흐르면 정확한 잔여시간이 나온다(재작업 지시 1).
  const remainingSeconds = expiresAt === null ? 0 : Math.max(0, Math.ceil((expiresAt - now) / 1000))
  const resendRemainingSeconds = resendAvailableAt === null ? 0 : Math.max(0, Math.ceil((resendAvailableAt - now) / 1000))

  // `now`를 매초 갱신하는 로컬 setInterval(cleanup 있음)이라 StrictMode dev의 effect 이중 마운트에도
  // 안전하다(`LoginForm.tsx`의 429 카운트다운과 동일 기법, STORY "프레임워크 실행 모델 함정" 절).
  // requestSeq가 바뀔 때마다(최초 요청·재전송) 새로 시작하고, **현재 단계(step)와 무관하게** 돈다 —
  // step 조건으로 껐던 것이 round2 QA CONCERNS 1의 원인이었다(3단계 체류 중 카운트다운 정지).
  useEffect(() => {
    if (requestSeq === 0) return
    setNow(Date.now())
    const id = setInterval(() => setNow(Date.now()), 1000)
    return () => clearInterval(id)
  }, [requestSeq])

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
    navigate(keyword ? `/shop/products?keyword=${encodeURIComponent(keyword)}` : '/shop/products')
  }

  const resetAll = () => {
    setNormalizedTarget('')
    setExpiresAt(null)
    setResendAvailableAt(null)
    setRequestSeq(0)
    setCode('')
    setCodeError(null)
    setNewPassword('')
    setPasswordError(null)
    setRequestError(null)
  }

  const handleRequestSubmit = async () => {
    setRequestBusy(true)
    setRequestError(null)
    const cleaned = normalizeTargetForSend(target)
    try {
      const res = await requestCodeOnce(cleaned, requestPasswordResetCode)
      const nowMs = Date.now()
      setNormalizedTarget(res.target)
      setChannel(res.channel)
      setExpiresAt(nowMs + res.expiresInSeconds * 1000)
      setResendAvailableAt(nowMs + RESEND_COOLDOWN_SECONDS * 1000)
      setNow(nowMs)
      setRequestSeq(s => s + 1)
      setCode('')
      setCodeError(null)
      setStep('code')
    } catch (e) {
      setRequestError(toDisplayError(e) as PasswordResetRequestError)
    } finally {
      setRequestBusy(false)
    }
  }

  const handleResend = async () => {
    if (resendRemainingSeconds > 0) return
    setResendBusy(true)
    try {
      const res = await requestCodeOnce(normalizedTarget, requestPasswordResetCode)
      const nowMs = Date.now()
      setExpiresAt(nowMs + res.expiresInSeconds * 1000)
      setResendAvailableAt(nowMs + RESEND_COOLDOWN_SECONDS * 1000)
      setNow(nowMs)
      setRequestSeq(s => s + 1)
      // 재작업 지시 3 — 재전송 성공 시 옛 코드 입력·오류 문구를 지운다. 그렇지 않으면 새 코드를
      // 보냈는데도 이전 409 MBR-4102 오류("코드가 올바르지 않습니다")와 옛 입력이 남는다.
      setCode('')
      setCodeError(null)
    } catch {
      // 재전송 실패는 조용히 무시한다 — 요청 API는 형식 오류가 아닌 한 항상 202이므로(계약,
      // INF-MBR-006), 여기서 나는 실패는 네트워크·500류뿐이다. 기존 카운트다운을 유지해 사용자가
      // 다시 누를 수 있게 둔다.
    } finally {
      setResendBusy(false)
    }
  }

  const handleCodeNext = () => setStep('password')

  const handleRestart = () => {
    resetAll()
    setStep('request')
  }

  const handlePasswordSubmit = async () => {
    setPasswordBusy(true)
    setPasswordError(null)
    try {
      await confirmPasswordReset({ target: normalizedTarget, code, newPassword })
      // 재작업 지시 4 — 완료 화면이 "모든 기기에서 로그아웃됨"이라고 알리는데 이 기기의 localStorage
      // 세션(apiKey/refreshToken)이 남아 있으면 표시와 실제가 어긋난다. 서버가 이미 리프레시 토큰을
      // 전부 폐기했으므로(다음 401로 자가치유되긴 하나), 기존 로그아웃 헬퍼(`clearSession`)를 그대로
      // 재사용해 즉시 지운다 — `LoginForm.tsx`는 건드리지 않는다. AppShell(Gnb)에 그대로 반영되도록
      // session 상태도 함께 비운다(SR-311 — 이 페이지에 헤더가 새로 생기며 필요해진 동기화).
      clearSession()
      setSession(null)
      setStep('done')
    } catch (e) {
      const err = toDisplayError(e)
      switch (err.code) {
        case 'MBR-4101':
        case 'MBR-4103':
          setCodeError(err as PasswordResetCodeError)
          setStep('code')
          break
        case 'MBR-4102':
          // newPassword는 지우지 않는다 — 코드만 고치면 3단계 재진입 시 다시 칠 필요 없음(계약 메모).
          setCodeError(err as PasswordResetCodeError)
          setStep('code')
          break
        case 'MBR-4001':
          setPasswordError(err as PasswordResetPasswordError)
          break
        case 'MBR-4100':
          // 방어적 처리 — 정상 흐름에서는 도달하지 않는다(target은 1단계 202 응답으로 이미 정규화됨).
          setRequestError(err as PasswordResetRequestError)
          setStep('request')
          break
        default:
          // 재작업 지시 2 — 미정의/네트워크/5xx 오류는 MBR-4100과 분리한다. 일시적 오류로 사용자를
          // 1단계로 되돌리면 이미 입력한 코드·새 비밀번호가 날아간다 — 3단계를 유지하고 재시도 가능한
          // 인라인 오류로만 표시한다(AC5 표는 MBR-4100에만 1단계 전이를 확정했다).
          setPasswordError(err)
          break
      }
    } finally {
      setPasswordBusy(false)
    }
  }

  const stepLabel: Record<Step, string> = {
    request: '1/3 · 계정 확인',
    code: '2/3 · 코드 확인',
    password: '3/3 · 새 비밀번호',
    done: '완료',
  }

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ maxWidth: 400, margin: '60px auto', padding: '20px 18px', fontFamily: 'system-ui, sans-serif' }}>
        <h1 style={{ fontSize: 18, marginBottom: 4 }}>비밀번호 재설정</h1>
        <p style={{ fontSize: 12, color: '#888', marginBottom: 18 }}>{stepLabel[step]}</p>
        {step === 'request' && (
          <PasswordResetRequestStep
            value={target} onChange={setTarget} onSubmit={() => void handleRequestSubmit()}
            busy={requestBusy} error={requestError}
          />
        )}
        {step === 'code' && (
          <PasswordResetCodeStep
            target={normalizedTarget} channel={channel} code={code} onCodeChange={setCode}
            onNext={handleCodeNext} remainingSeconds={remainingSeconds}
            resendRemainingSeconds={resendRemainingSeconds} onResend={() => void handleResend()}
            resendBusy={resendBusy} error={codeError} onRestart={handleRestart}
          />
        )}
        {step === 'password' && (
          <PasswordResetPasswordStep
            value={newPassword} onChange={setNewPassword} onSubmit={() => void handlePasswordSubmit()}
            busy={passwordBusy} error={passwordError}
          />
        )}
        {step === 'done' && <PasswordResetDoneStep />}
      </div>
    </AppShell>
  )
}
