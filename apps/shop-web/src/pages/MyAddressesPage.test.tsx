/** @jest-environment jsdom */
// SR-235 — 마이페이지 배송지 관리(/shop/mypage/addresses) 통합 테스트. `CartPage.test.tsx`/
// `OrderPage.test.tsx` 관례 그대로(jsdom, URL+메서드 분기 fetch mock, 테스트별 고유 memberId/apiKey/
// addressId, `useNavigate`만 스텁).
declare const jest: typeof import('@jest/globals').jest

import '@testing-library/jest-dom/jest-globals'
import { act, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, test } from '@jest/globals'
import MyAddressesPage from './MyAddressesPage'
import { saveSession } from '../session'
import type { MemberAddress } from '../types'

const mockNavigate = jest.fn()
jest.mock('react-router-dom', () => {
  const actual = jest.requireActual('react-router-dom') as Record<string, unknown>
  return { ...actual, useNavigate: () => mockNavigate }
})

function jsonResponse(status: number, body: unknown, ok = status >= 200 && status < 300) {
  return { ok, status, statusText: '', json: async () => body } as Response
}

function address(i: number, overrides: Partial<MemberAddress> = {}): MemberAddress {
  return {
    addressId: i, memberId: `m-addr-${i}`, recipient: `수령인${i}`, phone: `010-0000-${String(i).padStart(4, '0')}`,
    phoneNorm: `0100000${String(i).padStart(4, '0')}`, zipcode: '06236', roadAddress: `서울 강남구 테헤란로 ${i}`,
    detailAddress: `${i}동 ${i}호`, entranceMethod: null, deliveryMemo: null, isDefault: 'N',
    lastUsedAt: '2026-09-19T10:00:00', createdAt: '2026-09-19T10:00:00', updatedAt: '2026-09-19T10:00:00',
    ...overrides,
  }
}

function session(i: number) {
  return {
    memberId: `m-addr-${i}`, memberName: '홍길동', grade: 'NORMAL',
    apiKey: `key-addr-${i}`, refreshToken: `r-addr-${i}`, refreshTokenExpiresAt: '2099-01-01T00:00:00Z',
  }
}

describe('MyAddressesPage', () => {
  let fetchMock: import('@jest/globals').jest.Mock<typeof fetch>
  let listHeaders: Record<string, string>[]
  let registerCalls: unknown[]
  let updateCalls: { addressId: number; body: unknown }[]
  let deleteCalls: number[]
  let setDefaultCalls: number[]

  beforeEach(() => {
    fetchMock = jest.fn()
    listHeaders = []
    registerCalls = []
    updateCalls = []
    deleteCalls = []
    setDefaultCalls = []
    global.fetch = fetchMock as unknown as typeof fetch
    localStorage.clear()
    mockNavigate.mockClear()
  })

  afterEach(() => {
    localStorage.clear()
  })

  function routeFetch(handlers: {
    list?: () => Response | Promise<Response>
    register?: (body: Record<string, unknown>) => Response | Promise<Response>
    update?: (addressId: number, body: Record<string, unknown>) => Response | Promise<Response>
    remove?: (addressId: number) => Response | Promise<Response>
    setDefault?: (addressId: number) => Response | Promise<Response>
    zipcodes?: () => Response | Promise<Response>
  }) {
    fetchMock.mockImplementation(async (input: unknown, init?: RequestInit) => {
      const url = String(input)
      const method = (init?.method ?? 'GET').toUpperCase()
      if (url.startsWith('/api/members/me/addresses')) {
        const rest = url.slice('/api/members/me/addresses'.length)
        const headers = (init?.headers ?? {}) as Record<string, string>
        if (rest === '' && method === 'GET') {
          listHeaders.push(headers)
          return handlers.list ? handlers.list() : jsonResponse(200, { items: [] })
        }
        if (rest === '' && method === 'POST') {
          const body = init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : {}
          registerCalls.push(body)
          return handlers.register ? handlers.register(body) : jsonResponse(201, address(1))
        }
        const defaultMatch = rest.match(/^\/(\d+)\/default$/)
        if (defaultMatch && method === 'PUT') {
          const addressId = Number(defaultMatch[1])
          setDefaultCalls.push(addressId)
          return handlers.setDefault ? handlers.setDefault(addressId) : jsonResponse(200, address(addressId))
        }
        const idMatch = rest.match(/^\/(\d+)$/)
        if (idMatch && method === 'PUT') {
          const addressId = Number(idMatch[1])
          const body = init?.body ? (JSON.parse(String(init.body)) as Record<string, unknown>) : {}
          updateCalls.push({ addressId, body })
          return handlers.update ? handlers.update(addressId, body) : jsonResponse(200, address(addressId))
        }
        if (idMatch && method === 'DELETE') {
          const addressId = Number(idMatch[1])
          deleteCalls.push(addressId)
          return handlers.remove ? handlers.remove(addressId) : jsonResponse(204, null)
        }
      }
      if (url.startsWith('/api/cart')) {
        return jsonResponse(200, { items: [] })
      }
      if (url.startsWith('/api/zipcodes')) {
        return handlers.zipcodes
          ? handlers.zipcodes()
          : jsonResponse(200, { items: [{ zipcode: '06236', roadAddress: '서울 강남구 테헤란로 1', sido: '서울', sigungu: '강남구' }] })
      }
      throw new Error('unexpected url ' + url + ' ' + method)
    })
  }

  function renderPage() {
    return render(
      <MemoryRouter initialEntries={['/shop/mypage/addresses']}>
        <Routes>
          <Route path="/shop/mypage/addresses" element={<MyAddressesPage />} />
        </Routes>
      </MemoryRouter>,
    )
  }

  // linked_tc: TC-FUNC-member-010-001
  test('비로그인 진입 — 로그인 화면으로 리다이렉트되고 목록 조회는 호출되지 않는다', async () => {
    routeFetch({})
    renderPage()

    await waitFor(() => expect(mockNavigate).toHaveBeenCalledWith(
      '/login?redirect=' + encodeURIComponent('/shop/mypage/addresses'), { replace: true },
    ))
    expect(listHeaders).toHaveLength(0)
  })

  // linked_tc: TC-FUNC-member-010-002
  test('로그인 상태 — 목록 조회 시 X-Api-Key 헤더로 회원 자신의 apiKey를 보낸다', async () => {
    const s = session(1)
    saveSession(s)
    routeFetch({ list: () => jsonResponse(200, { items: [] }) })
    renderPage()

    await screen.findByText('등록된 배송지가 없습니다')
    expect(listHeaders).toHaveLength(1)
    expect(listHeaders[0]['X-Api-Key']).toBe(s.apiKey)
  })

  // linked_tc: TC-FUNC-member-010-003
  test('0건 — 안내 문구 + [추가] 버튼', async () => {
    saveSession(session(2))
    routeFetch({ list: () => jsonResponse(200, { items: [] }) })
    renderPage()

    await screen.findByText('등록된 배송지가 없습니다')
    expect(screen.getByRole('button', { name: '추가' })).not.toBeDisabled()
  })

  // linked_tc: TC-FUNC-member-010-004
  test('1건 — 기본 배지가 표시된다', async () => {
    saveSession(session(3))
    routeFetch({ list: () => jsonResponse(200, { items: [address(1, { isDefault: 'Y' })] }) })
    renderPage()

    await screen.findByText('수령인1 · 010-0000-0001')
    expect(screen.getByText('기본')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-005
  test('10건 도달 — [추가] 버튼 비활성 + "최대 10개" 안내', async () => {
    saveSession(session(4))
    const rows = Array.from({ length: 10 }, (_, i) => address(i + 1, { isDefault: i === 0 ? 'Y' : 'N' }))
    routeFetch({ list: () => jsonResponse(200, { items: rows }) })
    renderPage()

    await screen.findByText('배송지 관리 (10)')
    expect(screen.getByRole('button', { name: '추가' })).toBeDisabled()
    expect(screen.getByText(/최대 10개/)).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-006
  test('등록: 필수값 누락 — 인라인 오류 표시, POST는 호출되지 않는다', async () => {
    saveSession(session(5))
    routeFetch({ list: () => jsonResponse(200, { items: [] }) })
    renderPage()

    await screen.findByText('등록된 배송지가 없습니다')
    fireEvent.click(screen.getByRole('button', { name: '추가' }))
    fireEvent.click(screen.getByRole('button', { name: '저장' }))

    expect(await screen.findByText('수령인을 입력해 주세요')).toBeInTheDocument()
    expect(screen.getByText('연락처를 입력해 주세요')).toBeInTheDocument()
    expect(screen.getByText('우편번호 찾기로 주소를 선택해 주세요')).toBeInTheDocument()
    expect(screen.getByText('상세주소를 입력해 주세요')).toBeInTheDocument()
    expect(registerCalls).toHaveLength(0)
  })

  async function fillAndOpenZipcode() {
    fireEvent.change(screen.getByLabelText('수령인'), { target: { value: '홍길동' } })
    fireEvent.change(screen.getByLabelText('연락처'), { target: { value: '010-1111-2222' } })
    fireEvent.change(screen.getByLabelText('상세주소'), { target: { value: '101동 202호' } })
    fireEvent.click(screen.getByRole('button', { name: '우편번호 찾기' }))
    fireEvent.change(screen.getByLabelText('도로명·지번 검색어'), { target: { value: '테헤란로' } })
    fireEvent.click(screen.getByRole('button', { name: '검색' }))
    const resultButton = await screen.findByRole('button', { name: /06236/ })
    fireEvent.click(resultButton)
  }

  // linked_tc: TC-FUNC-member-010-007
  test('등록: 정상 입력 — POST 호출 후 목록에 반영된다', async () => {
    saveSession(session(6))
    routeFetch({
      list: () => jsonResponse(200, { items: [] }),
      register: (body) => jsonResponse(201, address(101, {
        recipient: body.recipient as string, phone: body.phone as string, isDefault: 'Y',
      })),
    })

    renderPage()
    await screen.findByText('등록된 배송지가 없습니다')
    fireEvent.click(screen.getByRole('button', { name: '추가' }))
    await fillAndOpenZipcode()
    fireEvent.click(screen.getByRole('button', { name: '저장' }))

    await waitFor(() => expect(registerCalls).toHaveLength(1))
    expect(registerCalls[0]).toMatchObject({ recipient: '홍길동', phone: '010-1111-2222', zipcode: '06236', detailAddress: '101동 202호' })
    await screen.findByText('홍길동 · 010-1111-2222')
    expect(screen.queryByLabelText('배송지 추가')).not.toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-008
  // QA FAIL 필수3 — 서버 selectList는 last_used_at DESC 정렬이고 신규 등록분이 맨 앞이 정본이므로,
  // 기존 행이 있는 상태에서 등록해도 새 행이 목록 첫 자리에 와야 한다(끝에 붙으면 회귀).
  test('등록: 정상 입력 — 기존 행이 있어도 새 배송지가 목록 맨 앞에 온다(서버 last_used_at DESC 정렬과 일치)', async () => {
    saveSession(session(16))
    const existing = address(16)
    routeFetch({
      list: () => jsonResponse(200, { items: [existing] }),
      register: (body) => jsonResponse(201, address(160, {
        recipient: body.recipient as string, phone: body.phone as string, isDefault: 'N',
      })),
    })

    renderPage()
    await screen.findByText('수령인16 · 010-0000-0016')
    fireEvent.click(screen.getByRole('button', { name: '추가' }))
    await fillAndOpenZipcode()
    fireEvent.click(screen.getByRole('button', { name: '저장' }))

    await waitFor(() => expect(registerCalls).toHaveLength(1))
    await screen.findByText('홍길동 · 010-1111-2222')
    const rows = within(screen.getByRole('list', { name: '배송지 목록' })).getAllByRole('listitem')
    expect(rows).toHaveLength(2)
    expect(within(rows[0]).getByText('홍길동 · 010-1111-2222')).toBeInTheDocument()
    expect(within(rows[1]).getByText('수령인16 · 010-0000-0016')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-009
  test('11번째 등록 — 락 조회 시점에 이미 10건이 된 경쟁 상황(서버 409) 문구 그대로', async () => {
    saveSession(session(8))
    const rows = Array.from({ length: 9 }, (_, i) => address(i + 1))
    routeFetch({
      list: () => jsonResponse(200, { items: rows }),
      register: () => jsonResponse(409, { code: 'MBR-4201', message: '배송지는 최대 10개까지 등록할 수 있습니다' }, false),
    })

    renderPage()
    await screen.findByText('배송지 관리 (9)')
    fireEvent.click(screen.getByRole('button', { name: '추가' }))
    await fillAndOpenZipcode()
    fireEvent.click(screen.getByRole('button', { name: '저장' }))

    expect(await screen.findByText('배송지는 최대 10개까지 등록할 수 있습니다')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-010
  test('수정: 기존 값이 prefill되고, 저장 후 목록의 해당 행이 갱신된다', async () => {
    saveSession(session(9))
    const existing = address(9, { recipient: '기존수령인' })
    routeFetch({
      list: () => jsonResponse(200, { items: [existing] }),
      update: (addressId, body) => jsonResponse(200, { ...existing, addressId, recipient: body.recipient as string }),
    })
    renderPage()

    await screen.findByText('기존수령인 · 010-0000-0009')
    fireEvent.click(screen.getByRole('button', { name: '기존수령인 수정' }))
    expect(screen.getByLabelText('수령인')).toHaveValue('기존수령인')
    // 수정 폼은 기본설정 체크박스를 숨긴다(INF-MBR-008 "수정은 isDefault를 다루지 않음").
    expect(screen.queryByLabelText('기본 배송지로 설정')).not.toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('수령인'), { target: { value: '새수령인' } })
    fireEvent.click(screen.getByRole('button', { name: '저장' }))

    await screen.findByText('새수령인 · 010-0000-0009')
    expect(updateCalls).toHaveLength(1)
    expect(updateCalls[0].addressId).toBe(9)
  })

  // linked_tc: TC-FUNC-member-010-011
  test('삭제 — 확인 다이얼로그 없이는 DELETE가 호출되지 않고, [삭제] 확인 클릭 후에만 그 행이 제거된다', async () => {
    saveSession(session(10))
    const rows = [address(10), address(1010)]
    routeFetch({ list: () => jsonResponse(200, { items: rows }) })
    renderPage()

    await screen.findByText('수령인10 · 010-0000-0010')
    fireEvent.click(screen.getByRole('button', { name: '수령인10 삭제' }))
    expect(deleteCalls).toHaveLength(0)

    fireEvent.click(within(screen.getByRole('dialog', { name: '배송지 삭제 확인' })).getByRole('button', { name: '삭제' }))
    await waitFor(() => expect(deleteCalls).toEqual([10]))
    await waitFor(() => expect(screen.queryByText('수령인10 · 010-0000-0010')).not.toBeInTheDocument())
    expect(screen.getByText('수령인1010 · 010-0000-1010')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-012
  test('기본 배송지 삭제 — 삭제 후 GET이 재호출된다(승계 재조회)', async () => {
    saveSession(session(11))
    const target = address(11, { isDefault: 'Y' })
    const succeeded = address(1111, { isDefault: 'Y' })
    let listCallCount = 0
    routeFetch({
      list: () => {
        listCallCount += 1
        return jsonResponse(200, { items: listCallCount === 1 ? [target] : [succeeded] })
      },
    })
    renderPage()

    await screen.findByText('수령인11 · 010-0000-0011')
    fireEvent.click(screen.getByRole('button', { name: '수령인11 삭제' }))
    fireEvent.click(within(screen.getByRole('dialog', { name: '배송지 삭제 확인' })).getByRole('button', { name: '삭제' }))

    await waitFor(() => expect(deleteCalls).toEqual([11]))
    await waitFor(() => expect(listCallCount).toBe(2))
    await screen.findByText('수령인1111 · 010-0000-1111')
  })

  // linked_tc: TC-FUNC-member-010-013
  test('기본 설정 전환 — PUT .../default 호출 후 배지가 그 행으로 옮겨간다', async () => {
    saveSession(session(12))
    const rows = [address(12, { isDefault: 'Y' }), address(1212, { isDefault: 'N' })]
    routeFetch({
      list: () => jsonResponse(200, { items: rows }),
      setDefault: (addressId) => jsonResponse(200, { ...address(1212, { isDefault: 'Y' }), addressId }),
    })
    renderPage()

    await screen.findByText('수령인1212 · 010-0000-1212')
    fireEvent.click(screen.getByRole('button', { name: '수령인1212 기본으로 설정' }))

    // 배지가 옮겨간 뒤에는 반대로 뒤집힌다 — 이제 기본이 아닌 수령인12에 버튼이 새로 생기고,
    // 새로 기본이 된 수령인1212의 버튼은 사라진다(컴포넌트가 isDefault==='Y' 행의 버튼을 숨긴다).
    // PUT 호출은 응답이 온 뒤에야 로컬 상태가 갱신되므로, 응답 반영까지 재시도하는 find*로 기다린다.
    await screen.findByRole('button', { name: '수령인12 기본으로 설정' })
    expect(setDefaultCalls).toEqual([1212])
    expect(screen.getByText('기본')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '수령인1212 기본으로 설정' })).not.toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-014
  // QA 권고3 — 이름이 "두 시나리오 모두"를 주장했지만 실제로는 삭제(DELETE) 한 경로만 발사했다. 이
  // 테스트는 삭제 경로로 범위를 명확히 하고, 바로 아래 테스트가 수정(PUT) 경로를 별도로 재현한다.
  test('404 MBR-4041 — 삭제 대상이 없거나 남의 배송지면 그 메시지를 그대로 인라인 표시한다', async () => {
    saveSession(session(13))
    const rows = [address(13)]
    routeFetch({
      list: () => jsonResponse(200, { items: rows }),
      remove: () => jsonResponse(404, { code: 'MBR-4041', message: '배송지를 찾을 수 없습니다' }, false),
    })
    renderPage()

    await screen.findByText('수령인13 · 010-0000-0013')
    fireEvent.click(screen.getByRole('button', { name: '수령인13 삭제' }))
    fireEvent.click(within(screen.getByRole('dialog', { name: '배송지 삭제 확인' })).getByRole('button', { name: '삭제' }))

    expect(await screen.findByText('배송지를 찾을 수 없습니다')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-015
  test('404 MBR-4041 — 수정 대상이 없거나 남의 배송지면 그 메시지를 그대로 인라인 표시한다', async () => {
    saveSession(session(17))
    const rows = [address(17)]
    routeFetch({
      list: () => jsonResponse(200, { items: rows }),
      update: () => jsonResponse(404, { code: 'MBR-4041', message: '배송지를 찾을 수 없습니다' }, false),
    })
    renderPage()

    await screen.findByText('수령인17 · 010-0000-0017')
    fireEvent.click(screen.getByRole('button', { name: '수령인17 수정' }))
    fireEvent.click(screen.getByRole('button', { name: '저장' }))

    expect(await screen.findByText('배송지를 찾을 수 없습니다')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-016
  // QA 권고2 — MBR-4202(검색어 2자 미만)는 배송지 CRUD와 달리 OrderHttpError 계열(zipcode 경로)을
  // 탄다. 필수1·필수2 수정(toDisplayMessage 통일)으로 400 서버 문구가 조용히 일반 문구로 바뀌지
  // 않는지 고정한다(400은 ALLOWED_MESSAGE_STATUSES 안이라 원문 유지가 맞다).
  test('우편번호 검색 — 400 MBR-4202(검색어 2자 미만) 서버 문구를 그대로 인라인 표시한다', async () => {
    saveSession(session(18))
    routeFetch({
      list: () => jsonResponse(200, { items: [] }),
      zipcodes: () => jsonResponse(400, { code: 'MBR-4202', message: '검색어는 2자 이상 입력해 주세요' }, false),
    })
    renderPage()

    await screen.findByText('등록된 배송지가 없습니다')
    fireEvent.click(screen.getByRole('button', { name: '추가' }))
    fireEvent.click(screen.getByRole('button', { name: '우편번호 찾기' }))
    fireEvent.change(screen.getByLabelText('도로명·지번 검색어'), { target: { value: 'a' } })
    fireEvent.click(screen.getByRole('button', { name: '검색' }))

    expect(await screen.findByText('검색어는 2자 이상 입력해 주세요')).toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-017
  // QA FAIL 필수1 — 우편번호 검색 실패가 공용 필터 toDisplayMessage를 우회하고 e.message 원문을 그대로
  // 렌더했다. searchZipcodes는 상태코드 불문 OrderHttpError(서버 {message})를 던지고
  // application.yml의 include-message:always와 겹쳐, 계약 밖 5xx의 원문 예외 메시지가 그대로 모달에
  // 노출될 수 있었다 — 이제는 일반 문구만 보여야 한다.
  test('우편번호 검색 — 500 응답의 서버 원문 메시지는 노출하지 않고 일반 문구를 보여준다', async () => {
    saveSession(session(19))
    routeFetch({
      list: () => jsonResponse(200, { items: [] }),
      zipcodes: () => jsonResponse(
        500,
        { message: 'Cannot invoke "com.sl.member.Zipcode.getRoadAddress()" because "x" is null' },
        false,
      ),
    })
    renderPage()

    await screen.findByText('등록된 배송지가 없습니다')
    fireEvent.click(screen.getByRole('button', { name: '추가' }))
    fireEvent.click(screen.getByRole('button', { name: '우편번호 찾기' }))
    fireEvent.change(screen.getByLabelText('도로명·지번 검색어'), { target: { value: '테헤란로' } })
    fireEvent.click(screen.getByRole('button', { name: '검색' }))

    expect(await screen.findByText('일시적 오류입니다. 다시 시도해 주세요')).toBeInTheDocument()
    expect(screen.queryByText(/Cannot invoke/)).not.toBeInTheDocument()
  })

  // linked_tc: TC-FUNC-member-010-018
  test('연타 방지 — 기본설정 버튼 2회 클릭에도 PUT 호출은 1회만 나간다', async () => {
    saveSession(session(14))
    const rows = [address(14, { isDefault: 'Y' }), address(1414, { isDefault: 'N' })]
    routeFetch({
      list: () => jsonResponse(200, { items: rows }),
      setDefault: (addressId) => jsonResponse(200, { ...address(1414, { isDefault: 'Y' }), addressId }),
    })
    renderPage()

    await screen.findByText('수령인1414 · 010-0000-1414')
    const setDefaultButton = screen.getByRole('button', { name: '수령인1414 기본으로 설정' })
    act(() => {
      setDefaultButton.click()
      setDefaultButton.click()
    })

    await waitFor(() => expect(setDefaultCalls).toEqual([1414]))
  })

  // linked_tc: TC-FUNC-member-010-019
  // QA 권고4 — 기본설정 연타 방지만 검증돼 있었고 삭제 쪽(같은 inFlightAddressIdRef 공유)은 없었다.
  test('삭제 연타 방지 — [삭제] 확인 버튼 2회 클릭에도 DELETE 호출은 1회만 나간다', async () => {
    saveSession(session(15))
    const rows = [address(15)]
    routeFetch({ list: () => jsonResponse(200, { items: rows }) })
    renderPage()

    await screen.findByText('수령인15 · 010-0000-0015')
    fireEvent.click(screen.getByRole('button', { name: '수령인15 삭제' }))
    const confirmButton = within(screen.getByRole('dialog', { name: '배송지 삭제 확인' })).getByRole('button', { name: '삭제' })
    act(() => {
      confirmButton.click()
      confirmButton.click()
    })

    await waitFor(() => expect(deleteCalls).toEqual([15]))
  })
})
