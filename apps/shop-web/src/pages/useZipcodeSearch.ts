// SR-235 재작업(round2, QA FAIL 권고5·권고6) — 우편번호 검색 상태(query/results/loading/error) 4개를
// `MyAddressesPage`에서 분리해 파일 크기 상한(300줄, 규칙 `file-size-cap`)을 지킨다. 동시에 모달을
// 닫을 때 상태를 비워, 다음 배송지 등록에서 모달을 다시 열었을 때 이전 검색어·결과·오류가 먼저 보이던
// 문제(권고6, `OrderPage`도 같은 관례라 이번 라운드 차단 사유는 아니었지만 이 화면에서는 함께 정리)도
// 없앤다. 페이지 전용 훅이라 컴포넌트(`.tsx`)가 아니고 렌더하지 않으므로 규칙 `story-per-component`
// 대상이 아니다.
import { useState } from 'react'
import { searchZipcodes } from '../api'
import { toDisplayMessage } from '../features/shop/httpErrorMessage'
import type { ZipcodeResult } from '../types'

export function useZipcodeSearch() {
  const [open, setOpen] = useState(false)
  const [query, setQuery] = useState('')
  const [results, setResults] = useState<ZipcodeResult[]>([])
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const openModal = () => setOpen(true)

  /** 닫을 때 검색 상태를 비운다 — 다음에 열었을 때 이전 세션의 검색어·결과·오류가 남아 있지 않게. */
  const close = () => {
    setOpen(false)
    setQuery('')
    setResults([])
    setError(null)
  }

  /** `searchZipcodes`(INF-MBR-009)는 상태코드 불문 `OrderHttpError(서버 {message})`를 던진다 — 공용
   * 필터 `toDisplayMessage`를 반드시 거쳐 계약 밖 상태(5xx 등)의 원문이 새지 않게 한다(QA FAIL 필수1). */
  const search = () => {
    setLoading(true)
    setError(null)
    searchZipcodes(query.trim())
      .then(setResults)
      .catch(e => setError(toDisplayMessage(e)))
      .finally(() => setLoading(false))
  }

  return { open, query, setQuery, results, loading, error, openModal, close, search }
}
