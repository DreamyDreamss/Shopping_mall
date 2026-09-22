// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-007.md
export interface PasswordResetCodeError {
  code: 'MBR-4101' | 'MBR-4102' | 'MBR-4103'
  message: string
}

export interface PasswordResetCodeStepProps {
  target: string
  channel: 'EMAIL' | 'SMS'
  code: string
  onCodeChange: (next: string) => void
  /** 로컬 형식 검증(6자리 숫자)을 통과했을 때만 컨테이너가 3단계로 전이한다 — 서버 호출 없음. */
  onNext: () => void
  /** 컨테이너가 계산해 내려주는 남은 유효시간(초) — 타이머 자체는 여기 두지 않는다. */
  remainingSeconds: number
  /** 재전송 쿨다운 남은 초 — 0이면 재전송 버튼이 활성화된다. */
  resendRemainingSeconds: number
  onResend: () => void
  resendBusy?: boolean
  /** 만료(410 MBR-4101)·시도초과(409 MBR-4103)·코드불일치(409 MBR-4102) 세 변형만 표시한다. */
  error?: PasswordResetCodeError | null
  /** 만료·시도초과일 때 "다시 요청" 클릭 — 전체 상태를 초기화하고 1단계로 되돌린다(컨테이너 책임). */
  onRestart: () => void
}

const box: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, padding: '8px 10px', fontSize: 16, letterSpacing: 4,
  width: '100%', textAlign: 'center',
}

const CODE_FORMAT = /^\d{6}$/

/**
 * 2단계 — 인증코드 입력. 코드 검증은 이 단계에서 서버에 묻지 않는다(INF-MBR-007이 코드 검증과
 * 비밀번호 반영을 원자적으로 한 번에 처리하므로) — 여기서는 6자리 숫자 형식만 로컬 확인하고
 * "다음"으로 3단계(새 비밀번호)로 넘어간다(STORY "순서·보안" 절).
 *
 * 만료·시도초과는 이 단계로 "돌아와서" 표시된다 — 3단계 confirm 제출이 410/409(MBR-4103)를 받으면
 * 컨테이너가 이 단계를 다시 렌더한다(AC5 매핑표).
 */
export function PasswordResetCodeStep({
  target, channel, code, onCodeChange, onNext,
  remainingSeconds, resendRemainingSeconds, onResend, resendBusy,
  error, onRestart,
}: PasswordResetCodeStepProps) {
  const validFormat = CODE_FORMAT.test(code)
  const expired = error?.code === 'MBR-4101' || error?.code === 'MBR-4103'

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 320 }}>
      <p style={{ fontSize: 12.5, color: '#555' }}>
        {channel === 'EMAIL' ? '이메일' : '휴대폰'}({target})로 발송된 6자리 코드를 입력하세요.
      </p>
      {!expired && (
        <p style={{ fontSize: 12, color: '#888' }}>남은 유효시간 {remainingSeconds}초</p>
      )}
      {expired ? (
        <div role="alert" style={{ color: '#912d2b', fontSize: 12.5, display: 'flex', flexDirection: 'column', gap: 8 }}>
          <span>{error.message}</span>
          <button type="button" onClick={onRestart}
                  style={{ border: '1px solid #912d2b', borderRadius: 5, background: '#fff', color: '#912d2b',
                           fontSize: 13, padding: '8px 12px', cursor: 'pointer', alignSelf: 'flex-start' }}>
            다시 요청
          </button>
        </div>
      ) : (
        <>
          <label style={{ fontSize: 12, color: '#555' }}>인증코드
            <input style={box} value={code} inputMode="numeric" maxLength={6}
                   onChange={e => onCodeChange(e.target.value.replace(/[^0-9]/g, '').slice(0, 6))} />
          </label>
          {error?.code === 'MBR-4102' && (
            <div role="alert" style={{ color: '#912d2b', fontSize: 12.5 }}>{error.message}</div>
          )}
          <button type="button" onClick={onNext} disabled={!validFormat}
                  style={{ border: 0, borderRadius: 5, padding: '10px 16px', fontSize: 14, fontWeight: 600,
                           background: validFormat ? '#0b4ea2' : '#c8ccd2', color: '#fff',
                           cursor: validFormat ? 'pointer' : 'not-allowed' }}>
            다음
          </button>
          <button type="button" onClick={onResend} disabled={!!resendBusy || resendRemainingSeconds > 0}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', fontSize: 12.5,
                           padding: '8px 12px', color: '#555',
                           cursor: resendRemainingSeconds > 0 ? 'not-allowed' : 'pointer' }}>
            {resendRemainingSeconds > 0 ? `재전송 (${resendRemainingSeconds}초)` : '재전송'}
          </button>
        </>
      )}
    </div>
  )
}
