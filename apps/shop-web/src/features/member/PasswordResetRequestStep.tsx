// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-006.md
export interface PasswordResetRequestError {
  code: 'MBR-4100' | 'MBR-5000'
  message: string
}

export interface PasswordResetRequestStepProps {
  value: string
  onChange: (next: string) => void
  onSubmit: () => void
  busy?: boolean
  error?: PasswordResetRequestError | null
}

const box: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, padding: '8px 10px', fontSize: 14, width: '100%',
}

/**
 * 1단계 — 이메일 또는 휴대폰번호 입력. 순수 프레젠테이션(fetch 없음, `LoginForm.tsx`와 동일한 제어
 * 컴포넌트 패턴) — target 정규화(휴대폰 하이픈·공백 제거)는 컨테이너(`PasswordResetPage.tsx`)의
 * 책임이다(AC2, 서버로 나가는 값의 형태는 이 컴포넌트가 알 필요 없다).
 *
 * 요청 API는 형식 오류(400 MBR-4100) 외엔 항상 202이므로(존재 오라클 방지, AC3), 이 컴포넌트는
 * "존재하지 않는 계정" 상태를 그리지 않는다 — `error`는 형식 오류 한 가지 모양만 표시한다.
 */
export function PasswordResetRequestStep({ value, onChange, onSubmit, busy, error }: PasswordResetRequestStepProps) {
  const blocked = !!busy

  return (
    <form onSubmit={e => { e.preventDefault(); if (!blocked) onSubmit() }}
          style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 320 }}>
      <label style={{ fontSize: 12, color: '#555' }}>이메일 또는 휴대폰번호
        <input style={box} value={value} autoComplete="username"
               onChange={e => onChange(e.target.value)} />
      </label>
      {error && (
        <div role="alert" style={{ color: '#912d2b', fontSize: 12.5 }}>{error.message}</div>
      )}
      <button type="submit" disabled={blocked}
              style={{ border: 0, borderRadius: 5, padding: '10px 16px', fontSize: 14, fontWeight: 600,
                       background: blocked ? '#c8ccd2' : '#0b4ea2', color: '#fff',
                       cursor: blocked ? 'not-allowed' : 'pointer' }}>
        {busy ? '요청 중…' : '코드 요청'}
      </button>
    </form>
  )
}
