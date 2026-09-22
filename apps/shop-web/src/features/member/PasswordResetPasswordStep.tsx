// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-007.md
export interface PasswordResetPasswordError {
  /**
   * MBR-4001(비밀번호 규칙 오류)·MBR-4100(방어적 표시용 — 정상 흐름에서는 컨테이너가 1단계로
   * 보내므로 사실상 안 옴) 외에, 미정의/네트워크/5xx(예: MBR-5000) 오류도 여기로 온다(재작업 지시
   * 2 — default 분기를 MBR-4100과 분리해 3단계 유지 + 재시도 가능한 인라인 오류로 표시한다).
   */
  code: string
  message: string
}

export interface PasswordResetPasswordStepProps {
  value: string
  onChange: (next: string) => void
  onSubmit: () => void
  busy?: boolean
  error?: PasswordResetPasswordError | null
}

const box: React.CSSProperties = {
  border: '1px solid #d5d8dd', borderRadius: 5, padding: '8px 10px', fontSize: 14, width: '100%',
}

/**
 * 3단계 — 새 비밀번호 입력. 제출이 실제 confirm(INF-MBR-007) 호출을 트리거한다(코드 검증이 원자
 * 포함돼 있다 — STORY "순서·보안" 절). 이 컴포넌트는 순수 프레젠테이션이라 그 호출은 컨테이너가 한다.
 */
export function PasswordResetPasswordStep({ value, onChange, onSubmit, busy, error }: PasswordResetPasswordStepProps) {
  const blocked = !!busy

  return (
    <form onSubmit={e => { e.preventDefault(); if (!blocked) onSubmit() }}
          style={{ display: 'flex', flexDirection: 'column', gap: 12, maxWidth: 320 }}>
      <label style={{ fontSize: 12, color: '#555' }}>새 비밀번호
        <input style={box} type="password" value={value} autoComplete="new-password"
               onChange={e => onChange(e.target.value)} />
      </label>
      {error && (
        <div role="alert" style={{ color: '#912d2b', fontSize: 12.5 }}>{error.message}</div>
      )}
      <button type="submit" disabled={blocked}
              style={{ border: 0, borderRadius: 5, padding: '10px 16px', fontSize: 14, fontWeight: 600,
                       background: blocked ? '#c8ccd2' : '#0b4ea2', color: '#fff',
                       cursor: blocked ? 'not-allowed' : 'pointer' }}>
        {busy ? '변경 중…' : '비밀번호 변경'}
      </button>
    </form>
  )
}
