// SR-235 — 마이페이지 배송지 관리(`/shop/mypage/addresses`) 컨테이너. 백엔드(INF-MBR-008/009)는 이미
// 구현 완료라 이 파일은 순수 HTTP 클라이언트 오케스트레이션만 한다 — 컨테이너라 스토리 대상이 아니다.
import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  deleteAddress, fetchAddresses, fetchCartItemCount, logout,
  registerAddress, setDefaultAddress, updateAddress,
} from '../api'
import { clearSession, loadSession } from '../session'
import type { MemberAddress, MemberAddressInput, SessionResult, ZipcodeResult } from '../types'
import { AppShell } from '../features/shop/AppShell'
import { ZipcodeSearchModal } from '../features/shop/ZipcodeSearchModal'
import { toDisplayMessage } from '../features/shop/httpErrorMessage'
import { AddressList } from '../features/member/AddressList'
import { AddressForm, type AddressFormValue } from '../features/member/AddressForm'
import { AddressDeleteConfirm } from '../features/member/AddressDeleteConfirm'
import { hasDeliveryAddressErrors, validateDeliveryAddress, type DeliveryAddressErrors } from '../features/shop/deliveryAddressValidation'
import { useZipcodeSearch } from './useZipcodeSearch'

const EMPTY_FORM_VALUE: AddressFormValue = {
  recipient: '', phone: '', zipcode: '', roadAddress: '', detailAddress: '',
  entranceMethod: '', deliveryMemo: '', isDefault: false,
}

type FormMode = { kind: 'add' } | { kind: 'edit'; addressId: number }

const alertBoxStyle: React.CSSProperties = {
  border: '1px solid #e0b4b4', background: '#fff6f6', color: '#912d2b', borderRadius: 6, padding: '10px 14px', fontSize: 12.5,
}

export default function MyAddressesPage() {
  const navigate = useNavigate()

  const [session, setSession] = useState<SessionResult | null>(() => loadSession())
  const [searchValue, setSearchValue] = useState('')
  const [cartItemCount, setCartItemCount] = useState(0)

  const [addresses, setAddresses] = useState<MemberAddress[]>([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

  const [formMode, setFormMode] = useState<FormMode | null>(null)
  const [formValue, setFormValue] = useState<AddressFormValue>(EMPTY_FORM_VALUE)
  const [formErrors, setFormErrors] = useState<DeliveryAddressErrors>({})
  const [saving, setSaving] = useState(false)
  const [saveError, setSaveError] = useState<string | null>(null)

  const [deleteTarget, setDeleteTarget] = useState<MemberAddress | null>(null)
  const [pendingAddressId, setPendingAddressId] = useState<number | null>(null)

  const zipcode = useZipcodeSearch()

  const inFlightKeyRef = useRef<string | null>(null)
  const requestIdRef = useRef(0)
  // 삭제/기본설정 연타 방지(`CartPage.inFlightSkusRef`와 동일 원리) — 동시 1건뿐이라 단일 ref로 충분.
  const inFlightAddressIdRef = useRef<number | null>(null)

  // 비로그인 진입 — 로그인 화면 + redirectTarget 복귀 규약 재사용. 세션이 없을 때마다 재평가하는 것이
  // 정답이라(사례집 SR-302 #1과 조건이 다름) `<Route>` element는 안 바꾸고 navigate() 호출로만 리다이렉트.
  useEffect(() => {
    if (!session?.memberId) {
      navigate('/login?redirect=' + encodeURIComponent('/shop/mypage/addresses'), { replace: true })
    }
  }, [session?.memberId, navigate])

  const load = async (memberId: string, apiKey: string) => {
    const key = 'addresses:' + memberId
    if (inFlightKeyRef.current === key) return
    inFlightKeyRef.current = key
    const id = ++requestIdRef.current
    setLoading(true)
    setLoadError(null)
    try {
      const rows = await fetchAddresses(apiKey)
      if (id !== requestIdRef.current) return
      setAddresses(rows)
    } catch (e) {
      if (id !== requestIdRef.current) return
      setLoadError(toDisplayMessage(e))
    } finally {
      if (inFlightKeyRef.current === key) inFlightKeyRef.current = null
      if (id === requestIdRef.current) setLoading(false)
    }
  }

  useEffect(() => {
    if (!session?.memberId) { setLoading(false); return }
    void load(session.memberId, session.apiKey)
  }, [session?.memberId])

  // GNB 장바구니 배지 — `ShopHomePage`와 동일하게 기존 `fetchCartItemCount` 재사용(신규 API 없음).
  useEffect(() => {
    if (!session?.memberId) { setCartItemCount(0); return }
    let cancelled = false
    fetchCartItemCount(session.memberId)
      .then(count => { if (!cancelled) setCartItemCount(count) })
      .catch(() => { if (!cancelled) setCartItemCount(0) })
    return () => { cancelled = true }
  }, [session?.memberId])

  const handleAdd = () => {
    setFormMode({ kind: 'add' })
    setFormValue(EMPTY_FORM_VALUE)
    setFormErrors({})
    setSaveError(null)
  }

  const handleEdit = (addressId: number) => {
    const addr = addresses.find(a => a.addressId === addressId)
    if (!addr) return
    setFormMode({ kind: 'edit', addressId })
    setFormValue({
      recipient: addr.recipient, phone: addr.phone, zipcode: addr.zipcode, roadAddress: addr.roadAddress,
      detailAddress: addr.detailAddress, entranceMethod: addr.entranceMethod ?? '',
      deliveryMemo: addr.deliveryMemo ?? '', isDefault: addr.isDefault === 'Y',
    })
    setFormErrors({})
    setSaveError(null)
  }

  const handleFormCancel = () => setFormMode(null)

  /** 클라이언트 필수값 검증 먼저, 통과해야 등록/수정 호출. */
  const handleFormSave = () => {
    if (!session || !formMode) return
    const errors = validateDeliveryAddress({
      recipientName: formValue.recipient, phone: formValue.phone, zipcode: formValue.zipcode,
      roadAddress: formValue.roadAddress, detailAddress: formValue.detailAddress,
    })
    if (hasDeliveryAddressErrors(errors)) { setFormErrors(errors); return }
    setFormErrors({})
    setSaveError(null)
    setSaving(true)

    const input: MemberAddressInput = {
      recipient: formValue.recipient, phone: formValue.phone, zipcode: formValue.zipcode,
      roadAddress: formValue.roadAddress, detailAddress: formValue.detailAddress,
      entranceMethod: formValue.entranceMethod || undefined, deliveryMemo: formValue.deliveryMemo || undefined,
      isDefault: formMode.kind === 'add' ? formValue.isDefault : undefined,
    }
    const request = formMode.kind === 'edit'
      ? updateAddress(session.apiKey, formMode.addressId, input)
      : registerAddress(session.apiKey, input)

    request
      .then(saved => {
        setAddresses(rows => {
          // 저장된 배송지가 기본이 됐으면(첫 등록 강제 또는 체크박스 선택) 이전 기본 행의 배지도
          // 로컬에서 내린다(기본설정 핸들러와 동일 이유) — 재조회 없이 "기본 배지 두 개"를 막는다.
          // 등록(add)은 맨 앞에 꽂는다 — 서버 `selectList`가 `ORDER BY last_used_at DESC, created_at
          // DESC`이고 신규 등록분은 last_used_at=created_at=now라 맨 앞이 정본(QA FAIL 필수3).
          const next = formMode.kind === 'edit'
            ? rows.map(r => (r.addressId === saved.addressId ? saved : r))
            : [saved, ...rows]
          if (saved.isDefault !== 'Y') return next
          return next.map(r => (r.addressId === saved.addressId ? r : { ...r, isDefault: 'N' as const }))
        })
        setFormMode(null)
      })
      .catch(e => setSaveError(toDisplayMessage(e)))
      .finally(() => setSaving(false))
  }

  const handleDeleteRequest = (addressId: number) => {
    const addr = addresses.find(a => a.addressId === addressId)
    if (addr) { setDeleteTarget(addr); setActionError(null) }
  }

  const handleDeleteCancel = () => setDeleteTarget(null)

  const handleDeleteConfirm = () => {
    if (!session || !deleteTarget) return
    const { addressId } = deleteTarget
    if (inFlightAddressIdRef.current === addressId) return
    const wasDefault = deleteTarget.isDefault === 'Y'
    inFlightAddressIdRef.current = addressId
    setPendingAddressId(addressId)

    deleteAddress(session.apiKey, addressId)
      .then(() => {
        setDeleteTarget(null)
        if (wasDefault) {
          // 승계된 새 기본을 서버가 204 본문으로 알려주지 않으므로, 기본을 지웠을 때만 재조회한다
          // (STORY "순서·보안" 5 — "다음 후보"를 프론트가 로컬 재계산하지 않는다).
          void load(session.memberId, session.apiKey)
        } else {
          setAddresses(rows => rows.filter(r => r.addressId !== addressId))
        }
      })
      .catch(e => { setDeleteTarget(null); setActionError(toDisplayMessage(e)) })
      .finally(() => {
        inFlightAddressIdRef.current = null
        setPendingAddressId(null)
      })
  }

  const handleSetDefault = (addressId: number) => {
    if (!session) return
    if (inFlightAddressIdRef.current === addressId) return
    const current = addresses.find(a => a.addressId === addressId)
    if (!current || current.isDefault === 'Y') return // 이미 기본 — no-op 호출 자체를 만들지 않는다
    inFlightAddressIdRef.current = addressId
    setPendingAddressId(addressId)
    setActionError(null)

    setDefaultAddress(session.apiKey, addressId)
      .then(updated => {
        setAddresses(rows => rows.map(r => {
          if (r.addressId === updated.addressId) return updated
          if (r.isDefault === 'Y') return { ...r, isDefault: 'N' as const }
          return r
        }))
      })
      .catch(e => setActionError(toDisplayMessage(e)))
      .finally(() => {
        inFlightAddressIdRef.current = null
        setPendingAddressId(null)
      })
  }

  const handleZipcodeSelect = (result: ZipcodeResult) => {
    setFormValue(v => ({ ...v, zipcode: result.zipcode, roadAddress: result.roadAddress }))
    zipcode.close()
    setFormErrors(errs => { const { zipcode: _drop, ...rest } = errs; return rest })
  }

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

  return (
    <AppShell session={session} cartItemCount={cartItemCount} searchValue={searchValue}
              onSearchChange={setSearchValue} onSearchSubmit={handleSearchSubmit} onLogout={handleLogout}>
      <div style={{ fontFamily: 'system-ui, sans-serif', color: '#222', padding: '16px 18px 40px' }}>
        {!session ? (
          <div role="status" style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>이동 중…</div>
        ) : loading ? (
          <div role="status" style={{ padding: '40px 16px', textAlign: 'center', color: '#666' }}>불러오는 중…</div>
        ) : loadError ? (
          <div role="alert" style={{ ...alertBoxStyle, padding: '16px 18px', display: 'flex', flexDirection: 'column', gap: 10, alignItems: 'flex-start' }}>
            <span>불러오지 못했습니다 — {loadError}</span>
            <button type="button" onClick={() => void load(session.memberId, session.apiKey)}
                    style={{ border: '1px solid #912d2b', borderRadius: 5, background: '#fff', color: '#912d2b',
                             fontSize: 12.5, padding: '5px 12px', cursor: 'pointer' }}>
              다시 시도
            </button>
          </div>
        ) : (
          <>
            {actionError && <div role="alert" style={{ ...alertBoxStyle, marginBottom: 14 }}>{actionError}</div>}
            <AddressList addresses={addresses} onAdd={handleAdd} onEdit={handleEdit}
                         onDelete={handleDeleteRequest} onSetDefault={handleSetDefault}
                         pendingAddressId={pendingAddressId} />
            {formMode && (
              <div style={{ marginTop: 20, border: '1px solid #eef0f2', borderRadius: 8, padding: 16 }}>
                <AddressForm mode={formMode.kind} value={formValue} onChange={setFormValue} errors={formErrors}
                             isFirstAddress={addresses.length === 0} onOpenZipcodeSearch={zipcode.openModal}
                             onSave={handleFormSave} onCancel={handleFormCancel} saving={saving} />
                {saveError && <div role="alert" style={{ fontSize: 12.5, color: '#912d2b', marginTop: 8 }}>{saveError}</div>}
              </div>
            )}
          </>
        )}
      </div>

      <ZipcodeSearchModal open={zipcode.open} query={zipcode.query} onQueryChange={zipcode.setQuery}
                          onSearch={zipcode.search} results={zipcode.results} loading={zipcode.loading}
                          error={zipcode.error} onSelect={handleZipcodeSelect} onClose={zipcode.close} />

      <AddressDeleteConfirm open={!!deleteTarget} recipient={deleteTarget?.recipient ?? ''}
                            roadAddress={deleteTarget?.roadAddress ?? ''} isDefault={deleteTarget?.isDefault === 'Y'}
                            onCancel={handleDeleteCancel} onConfirm={handleDeleteConfirm}
                            confirming={deleteTarget != null && pendingAddressId === deleteTarget.addressId} />
    </AppShell>
  )
}
