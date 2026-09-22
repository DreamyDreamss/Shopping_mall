/** @jest-environment jsdom */
// linked_func: FUNC-member-007
// spec: docs/05_설계서/member/INF/INF-MBR-006.md · INF-MBR-007.md
// AC1~AC5, AC2 실증 — 3단계 상태기계, target 정규화(휴대폰 하이픈·공백 제거/이메일 그대로),
// 응답 코드→화면 전이 매핑(AC5 표), 남은 유효시간·재전송 쿨다운 카운트다운(fake timer).
// 이 프로젝트 테스트는 전역 `expect`가 아니라 `@jest/globals`의 `expect`를 쓴다(redirectTarget.unit.test.ts 등과
// 동일 관례) — jest-dom 매처 타입도 `@jest/expect` 모듈을 증강하는 서브패스로 가져와야 tsc가 인식한다.
import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, jest, test } from '@jest/globals'
import PasswordResetPage from './PasswordResetPage'
import { loadSession, saveSession } from '../session'
import type { SessionResult } from '../types'

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

describe('PasswordResetPage', () => {
  let fetchMock: jest.Mock<typeof fetch>

  beforeEach(() => {
    jest.useFakeTimers()
    fetchMock = jest.fn()
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear() // 재작업 지시 4 테스트가 세션을 저장하므로, 다음 테스트로 상태가 새지 않게 한다.
  })

  afterEach(() => {
    jest.useRealTimers()
    localStorage.clear()
  })

  function renderPage() {
    return render(
      <MemoryRouter>
        <PasswordResetPage />
      </MemoryRouter>,
    )
  }

  /** 1단계를 이메일 target으로 통과시켜 2단계까지 진행한다(여러 테스트가 공유하는 준비 단계). */
  async function advanceToCodeStep(target = 'user@example.com') {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(202, { channel: 'EMAIL', target, expiresInSeconds: 600 }),
    )
    fireEvent.change(screen.getByLabelText('이메일 또는 휴대폰번호'), { target: { value: target } })
    fireEvent.click(screen.getByRole('button', { name: '코드 요청' }))
    await waitFor(() => expect(screen.getByLabelText('인증코드')).toBeInTheDocument())
  }

  /** 2단계에서 형식만 맞는 코드를 넣고 "다음"을 눌러 3단계(새 비밀번호)까지 진행한다. */
  async function advanceToPasswordStep() {
    await advanceToCodeStep()
    fireEvent.change(screen.getByLabelText('인증코드'), { target: { value: '123456' } })
    fireEvent.click(screen.getByRole('button', { name: '다음' }))
    await screen.findByLabelText('새 비밀번호')
  }

  test('초기 렌더는 1단계(요청)만 보인다(AC1)', () => {
    renderPage()
    expect(screen.getByRole('button', { name: '코드 요청' })).toBeInTheDocument()
    expect(screen.queryByLabelText('인증코드')).not.toBeInTheDocument()
    expect(screen.getByText('1/3 · 계정 확인')).toBeInTheDocument()
  })

  test('휴대폰은 하이픈·공백을 제거해 숫자만 전송한다(AC2)', async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(202, { channel: 'SMS', target: '01012345678', expiresInSeconds: 600 }),
    )
    renderPage()
    fireEvent.change(screen.getByLabelText('이메일 또는 휴대폰번호'), { target: { value: '010-1234-5678' } })
    fireEvent.click(screen.getByRole('button', { name: '코드 요청' }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(JSON.parse(init.body as string)).toEqual({ target: '01012345678' })
  })

  test('이메일은 입력 그대로 전송한다(정규화는 서버 책임, AC2)', async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(202, { channel: 'EMAIL', target: 'user@example.com', expiresInSeconds: 600 }),
    )
    renderPage()
    fireEvent.change(screen.getByLabelText('이메일 또는 휴대폰번호'), { target: { value: 'User@Example.com' } })
    fireEvent.click(screen.getByRole('button', { name: '코드 요청' }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(1))
    const [, init] = fetchMock.mock.calls[0] as [string, RequestInit]
    expect(JSON.parse(init.body as string)).toEqual({ target: 'User@Example.com' })
  })

  test('202 응답이면 코드 단계로 전이한다 — "존재하지 않는 계정" 상태는 없다(AC3)', async () => {
    renderPage()
    await advanceToCodeStep()
    expect(screen.getByText('2/3 · 코드 확인')).toBeInTheDocument()
    expect(screen.queryByText(/존재하지 않는/)).not.toBeInTheDocument()
  })

  test('1단계 400 MBR-4100은 1단계에 인라인 오류로 남는다', async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse(400, { code: 'MBR-4100', message: '이메일 또는 휴대폰번호 형식이 올바르지 않습니다' }, false),
    )
    renderPage()
    fireEvent.change(screen.getByLabelText('이메일 또는 휴대폰번호'), { target: { value: 'bad' } })
    fireEvent.click(screen.getByRole('button', { name: '코드 요청' }))
    expect(await screen.findByText('이메일 또는 휴대폰번호 형식이 올바르지 않습니다')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '코드 요청' })).toBeInTheDocument()
  })

  test('남은 유효시간 카운트다운은 응답 expiresInSeconds 기준으로 fake timer에 맞춰 감소한다(AC4)', async () => {
    renderPage()
    await advanceToCodeStep()
    expect(screen.getByText(/600초/)).toBeInTheDocument()
    act(() => { jest.advanceTimersByTime(3000) })
    expect(screen.getByText(/597초/)).toBeInTheDocument()
  })

  test('재전송 버튼은 60초 쿨다운 비활성+잔여초 표시 후 활성화되고, 클릭 시 fetch를 재호출한다(AC4)', async () => {
    renderPage()
    await advanceToCodeStep()

    const resendButton = screen.getByRole('button', { name: /재전송/ })
    expect(resendButton).toBeDisabled()
    expect(screen.getByText(/재전송 \(\d+초\)/)).toBeInTheDocument()

    act(() => { jest.advanceTimersByTime(60000) })
    expect(resendButton).not.toBeDisabled()
    expect(resendButton).toHaveTextContent('재전송')

    fetchMock.mockResolvedValueOnce(
      jsonResponse(202, { channel: 'EMAIL', target: 'user@example.com', expiresInSeconds: 600 }),
    )
    fireEvent.click(resendButton)
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(2))
  })

  test('6자리 코드 입력 후 "다음" 클릭은 서버 호출 없이 로컬 전이만 한다(코드 확인은 confirm에 원자 포함)', async () => {
    renderPage()
    await advanceToCodeStep()
    expect(fetchMock).toHaveBeenCalledTimes(1)

    fireEvent.change(screen.getByLabelText('인증코드'), { target: { value: '123456' } })
    fireEvent.click(screen.getByRole('button', { name: '다음' }))

    expect(await screen.findByLabelText('새 비밀번호')).toBeInTheDocument()
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })

  test('3단계 새 비밀번호 제출 → 204 → 완료 화면(AC5)', async () => {
    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockResolvedValueOnce(jsonResponse(204, undefined))
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    expect(await screen.findByText(/모든 기기에서 로그아웃/)).toBeInTheDocument()
  })

  test('confirm 410 MBR-4101 → 2단계로 "만료됨"+다시 요청(AC5)', async () => {
    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockResolvedValueOnce(
      jsonResponse(410, { code: 'MBR-4101', message: '인증코드가 만료되었습니다. 다시 요청해 주세요' }, false),
    )
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    expect(await screen.findByText('인증코드가 만료되었습니다. 다시 요청해 주세요')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다시 요청' })).toBeInTheDocument()
    expect(screen.queryByLabelText('인증코드')).not.toBeInTheDocument()
  })

  test('confirm 409 MBR-4103 → 2단계로 "시도초과"+다시 요청(AC5)', async () => {
    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockResolvedValueOnce(
      jsonResponse(409, { code: 'MBR-4103', message: '코드 확인 시도 횟수를 초과했습니다. 다시 요청해 주세요' }, false),
    )
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    expect(await screen.findByText('코드 확인 시도 횟수를 초과했습니다. 다시 요청해 주세요')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '다시 요청' })).toBeInTheDocument()
  })

  test('confirm 409 MBR-4102 → 2단계 복귀, 코드 인라인 오류, newPassword 값은 유지된다(AC5, 계약 메모)', async () => {
    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockResolvedValueOnce(
      jsonResponse(409, { code: 'MBR-4102', message: '코드가 올바르지 않습니다' }, false),
    )
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    expect(await screen.findByText('코드가 올바르지 않습니다')).toBeInTheDocument()
    // 코드를 고치고 "다음"으로 3단계에 재진입하면 새 비밀번호 값이 그대로 남아 있어야 한다.
    fireEvent.change(screen.getByLabelText('인증코드'), { target: { value: '654321' } })
    fireEvent.click(screen.getByRole('button', { name: '다음' }))
    expect(await screen.findByLabelText('새 비밀번호')).toHaveValue('newPass123')
  })

  test('confirm 400 MBR-4100(방어적) → 1단계로, target 인라인 오류(AC5, 정상흐름 미도달 방어 경로)', async () => {
    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockResolvedValueOnce(
      jsonResponse(400, { code: 'MBR-4100', message: '이메일 또는 휴대폰번호 형식이 올바르지 않습니다' }, false),
    )
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    expect(await screen.findByText('이메일 또는 휴대폰번호 형식이 올바르지 않습니다')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '코드 요청' })).toBeInTheDocument()
  })

  test('confirm 400 MBR-4001 → 3단계 유지, 비밀번호 필드 인라인 오류(AC5)', async () => {
    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockResolvedValueOnce(
      jsonResponse(400, { code: 'MBR-4001', message: '비밀번호 형식이 올바르지 않습니다' }, false),
    )
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'short' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    expect(await screen.findByText('비밀번호 형식이 올바르지 않습니다')).toBeInTheDocument()
    expect(screen.getByLabelText('새 비밀번호')).toBeInTheDocument()
  })

  // --- round2 재작업 지시(사람 확정) 실증 ---------------------------------------------------

  test('재작업 지시 1 — 3단계 체류 70초 뒤 2단계로 복귀하면 잔여시간이 70초 줄고 재전송이 열려 있다', async () => {
    renderPage()
    await advanceToCodeStep() // expiresAt = t0+600s, resendAvailableAt = t0+60s
    fireEvent.change(screen.getByLabelText('인증코드'), { target: { value: '123456' } })
    fireEvent.click(screen.getByRole('button', { name: '다음' })) // 3단계 진입, 서버 호출 없음

    // 3단계(새 비밀번호)에 머무는 동안 벽시계가 70초 흐른다 — 카운트다운 인터벌은 step과 무관하게 돈다.
    act(() => { jest.advanceTimersByTime(70000) })

    // confirm이 409 MBR-4102를 줘 2단계(코드)로 복귀한다(설계된 정규 경로).
    fetchMock.mockResolvedValueOnce(
      jsonResponse(409, { code: 'MBR-4102', message: '코드가 올바르지 않습니다' }, false),
    )
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    await screen.findByText('코드가 올바르지 않습니다')

    // 600초 - 70초 = 530초 남아 있어야 하고(2단계 체류시간 기준이었다면 600초로 보였을 것),
    // 재전송 쿨다운(60초)은 이미 지났으므로 재전송 버튼이 활성화돼 있어야 한다.
    expect(screen.getByText(/530초/)).toBeInTheDocument()
    expect(screen.getByRole('button', { name: '재전송' })).not.toBeDisabled()
  })

  test('재작업 지시 2 — confirm 500(미정의 코드)은 1단계로 보내지 않고 3단계 유지 + 재시도 인라인 오류를 보인다', async () => {
    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockResolvedValueOnce(
      jsonResponse(500, { code: 'MBR-5000', message: '일시적인 오류입니다. 다시 시도해 주세요' }, false),
    )
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    expect(await screen.findByText('일시적인 오류입니다. 다시 시도해 주세요')).toBeInTheDocument()
    // 3단계 유지 — 1단계(코드 요청 버튼)도 2단계(인증코드 입력)도 아니다.
    expect(screen.getByLabelText('새 비밀번호')).toHaveValue('newPass123')
    expect(screen.queryByRole('button', { name: '코드 요청' })).not.toBeInTheDocument()
    expect(screen.queryByLabelText('인증코드')).not.toBeInTheDocument()
  })

  test('재작업 지시 2 — confirm 네트워크 오류도 3단계를 유지한다(MBR-4100과 분리)', async () => {
    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockRejectedValueOnce(new Error('network down'))
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    expect(await screen.findByText('network down')).toBeInTheDocument()
    expect(screen.getByLabelText('새 비밀번호')).toHaveValue('newPass123')
    expect(screen.queryByRole('button', { name: '코드 요청' })).not.toBeInTheDocument()
  })

  test('재작업 지시 3 — 재전송 성공 시 기존 코드 입력·코드 오류가 지워진다', async () => {
    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockResolvedValueOnce(
      jsonResponse(409, { code: 'MBR-4102', message: '코드가 올바르지 않습니다' }, false),
    )
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    await screen.findByText('코드가 올바르지 않습니다')
    expect(screen.getByLabelText('인증코드')).toHaveValue('123456') // advanceToPasswordStep에서 입력한 값

    // 재전송 쿨다운(60초)이 끝난 뒤 재전송을 누른다.
    act(() => { jest.advanceTimersByTime(60000) })
    fetchMock.mockResolvedValueOnce(
      jsonResponse(202, { channel: 'EMAIL', target: 'user@example.com', expiresInSeconds: 600 }),
    )
    fireEvent.click(screen.getByRole('button', { name: '재전송' }))
    await waitFor(() => expect(fetchMock).toHaveBeenCalledTimes(3)) // 요청 1회 + confirm 1회 + 재전송 1회
    // fetch 호출 자체는 상태 갱신(setCode/setCodeError)보다 먼저 관측되므로, 지워질 때까지 기다린다.
    await waitFor(() => expect(screen.queryByText('코드가 올바르지 않습니다')).not.toBeInTheDocument())

    expect(screen.getByLabelText('인증코드')).toHaveValue('')
  })

  test('재작업 지시 4 — confirm 204 직후 이 기기의 로컬 세션(apiKey/refreshToken)을 지운다', async () => {
    const session: SessionResult = {
      memberId: 'm-1', memberName: '홍길동', grade: 'NORMAL',
      apiKey: 'k-1', refreshToken: 'r-1', refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
    }
    saveSession(session)
    expect(loadSession()).not.toBeNull() // 준비 확인

    renderPage()
    await advanceToPasswordStep()
    fetchMock.mockResolvedValueOnce(jsonResponse(204, undefined))
    fireEvent.change(screen.getByLabelText('새 비밀번호'), { target: { value: 'newPass123' } })
    fireEvent.click(screen.getByRole('button', { name: '비밀번호 변경' }))
    await screen.findByText(/모든 기기에서 로그아웃/)

    expect(loadSession()).toBeNull()
  })
})
