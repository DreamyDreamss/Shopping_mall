// linked_func: FUNC-member-004
// spec: docs/05_설계서/member/INF/INF-MBR-003.md · INF-MBR-005.md
import { useEffect, useState } from 'react'

export interface LoginCredentials {
  email: string
  password: string
}

export interface LoginFormError {
  code: string
  message: string
  /** 429(MBR-4291)일 때만 존재 — 이 값에서 시작하는 카운트다운, 0이 되면 재제출을 허용한다. */
  retryAfterSeconds?: number
}

export interface LoginFormProps {
  value: LoginCredentials
  onChange: (next: LoginCredentials) => void
  onSubmit: () => void
  busy?: boolean
  /** 서버가 낸 401(MBR-4011, "…(n/5)" 포함)·429(MBR-4291) 오류를 그대로 옮겨 보여준다. */
  error?: LoginFormError | null
}

const box: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, padding: '8px 10px', fontSize: 14, width: '100%',
}

/**
 * 로그인 폼 — 순수 프레젠테이션(fetch 없음, 규칙 `web-fetch-only-in-api`). `OrderFilters`와 같은
 * 제어 컴포넌트 패턴(값·onChange·onSubmit을 props로). 잠금·존재·비밀번호 대조는 서버가 이미
 * 판정했으므로 이 컴포넌트는 그 결과(코드/문구/retryAfterSeconds)를 그대로 표시만 한다 —
 * 존재 오라클을 프론트에서 재구현하지 않는다(STORY "순서·보안" 절).
 */
export function LoginForm({ value, onChange, onSubmit, busy, error }: LoginFormProps) {
  const [showPassword, setShowPassword] = useState(false)
  const locked = error?.retryAfterSeconds != null
  const [remaining, setRemaining] = useState(error?.retryAfterSeconds ?? 0)

  // 429 카운트다운은 컴포넌트 로컬 useState+useEffect뿐이다 — 모듈 전역 타이머·카운터가 없어
  // 스토리 실행끼리(또는 재로그인 시도끼리) tick이 새지 않는다(STORY "테스트 격리" 절,
  // SR-232 r2 "이메일 문자열이 PK인 카운터가 테스트를 가로질러 누적" 사례의 프론트 버전 예방).
  //
  // dep는 `error?.retryAfterSeconds`(숫자 값)가 아니라 `error`(객체 identity)다 — round1 QA 권고:
  // 연속으로 같은 초 값을 돌려주는 429(예: 재시도해도 여전히 같은 잠금 만료 시각)가 오면 값은
  // 안 바뀌어도 새 실패 이벤트이므로 카운트다운을 재시작해야 한다. `LoginPage`는 실패마다 새
  // 객체(`setError({...})`)를 만들므로 identity 비교가 정확히 "새 이벤트"를 잡아낸다. 0에 도달하면
  // interval을 즉시 정리해 언마운트까지 불필요하게 tick하지 않는다(round1 QA 저위험 권고).
  //
  // round2 QA 권고1 재작업 — `remaining`은 더 이상 문구로 노출하지 않는다(정확 초 표시가 서버의
  // "잠시 후 다시 시도해 주세요" 완곡화 요건과 같은 줄에서 상충했다). 이 state는 이제 오직
  // `blocked`(제출 버튼 비활성)를 0이 될 때까지 유지하는 내부 타이머로만 쓰인다 — 사용자에게는
  // 정확한 초 대신 서버가 이미 낸 완곡 문구(`error.message`)만 보인다.
  useEffect(() => {
    if (error?.retryAfterSeconds == null) return
    setRemaining(error.retryAfterSeconds)
    const id = setInterval(() => {
      setRemaining(r => {
        if (r <= 1) {
          clearInterval(id)
          return 0
        }
        return r - 1
      })
    }, 1000)
    return () => clearInterval(id)
  }, [error])

  const blocked = !!busy || (locked && remaining > 0)

  return (
    <form onSubmit={e => { e.preventDefault(); if (!blocked) onSubmit() }}
          style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 320 }}>
      <label style={{ fontSize: 12, color: '#555' }}>이메일
        <input style={box} type="email" value={value.email} autoComplete="email"
               onChange={e => onChange({ ...value, email: e.target.value })} />
      </label>
      <label style={{ fontSize: 12, color: '#555' }}>비밀번호
        <div style={{ display: 'flex', gap: 6 }}>
          <input style={box} type={showPassword ? 'text' : 'password'} value={value.password}
                 autoComplete="current-password"
                 onChange={e => onChange({ ...value, password: e.target.value })} />
          <button type="button" onClick={() => setShowPassword(s => !s)}
                  style={{ border: '1px solid #d5d8dd', borderRadius: 5, background: '#fff', fontSize: 12, padding: '0 10px' }}>
            {showPassword ? '숨기기' : '보기'}
          </button>
        </div>
      </label>
      {error && (
        <div role="alert" style={{ color: '#912d2b', fontSize: 12.5 }}>
          {error.message}
        </div>
      )}
      <button type="submit" disabled={blocked}
              style={{ border: 0, borderRadius: 5, padding: '10px 16px', fontSize: 14, fontWeight: 600,
                       background: blocked ? '#c8ccd2' : '#0b4ea2', color: '#fff',
                       cursor: blocked ? 'not-allowed' : 'pointer' }}>
        {busy ? '로그인 중…' : '로그인'}
      </button>
    </form>
  )
}
