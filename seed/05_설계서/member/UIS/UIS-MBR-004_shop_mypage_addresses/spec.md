---
uis-id: UIS-MBR-004
name: shop_mypage_addresses
domain: member
domain-code: MBR
layer: ui
route: /shop/mypage/addresses
screens_role: 주화면
api_hints:
  - "GET /api/members/me/addresses"
  - "POST /api/members/me/addresses"
  - "PUT /api/members/me/addresses/{addressId}"
  - "DELETE /api/members/me/addresses/{addressId}"
  - "PUT /api/members/me/addresses/{addressId}/default"
  - "GET /api/zipcodes"
access_control:
  - "화면 전체: 로그인 필수 — 세션 없으면 useEffect에서 /login?redirect=... 리다이렉트(로그인 성공 후 복귀). 별도 메뉴 버튼권한 게이팅 없음"
  - "모든 배송지 API 호출: X-Api-Key: session.apiKey(회원 자신의 키) 명시 헤더 — 누락 시 vite 프록시가 admin 키로 채워 SCOPE_TO_SELF 미적용, 400 MBR-4200으로 즉시 차단(설계상 우회 불가)"
anchors:
  - "modules/shop-web/src/App.tsx:113"
  - "modules/shop-web/src/pages/MyAddressesPage.tsx"
  - "modules/shop-web/src/pages/useZipcodeSearch.ts"
  - "modules/shop-web/src/features/member/AddressList.tsx"
  - "modules/shop-web/src/features/member/AddressForm.tsx"
  - "modules/shop-web/src/features/member/AddressDeleteConfirm.tsx"
  - "modules/shop-web/src/features/shop/ZipcodeSearchModal.tsx"
  - "modules/shop-web/src/features/shop/deliveryAddressValidation.ts"
  - "modules/shop-web/src/api.ts:278-339"
  - "modules/shop-web/src/types.ts:121-160"
  - "modules/shop-web/src/features/shop/Gnb.tsx:54-57"
revision_history:
  - "2026-09-19 골격 생성(spec_resync_check, zero-LLM)"
  - "2026-09-19 본문 보강(ddd-ui-agent, SR-235.1 소스 기준 — 화면개요/구성/검증규칙/호출API/표시조건)"
---

# UIS-MBR-004: shop_mypage_addresses

> [반영: SR-235.1] — 코드(라우트) 기준 재동기화 골격 + 소스 기준 본문 보강(ddd-ui-agent). 근거 소스(권위):
> `modules/shop-web/src/pages/MyAddressesPage.tsx` 외 `features/member/*`·`api.ts`·`types.ts`.
> 캡처/DOM 스냅샷 없음(Storybook 미기동) — **소스폴백 모드**로 작성, 0(화면 미리보기) 절 생략.

## 1. 화면 개요

- 라우트: `/shop/mypage/addresses` (spa-route, `modules/shop-web/src/App.tsx:113`)
- 목적: 회원이 자신의 배송지를 조회·등록·수정·삭제하고 기본 배송지를 지정하는 마이페이지 화면. 회원당
  최대 10개, 기본 배송지는 항상 1개(등록 시 최초 배송지는 서버가 기본으로 강제, 기본 배송지 삭제 시
  최근 사용 순 다음 배송지가 자동 승계). 백엔드(INF-MBR-008 배송지 CRUD, INF-MBR-009 우편번호 검색)는
  이번 SR 이전에 이미 구현되어 있고, 이 화면은 그 위에 얹는 순수 프론트(HTTP 클라이언트)다.
- 진입 경로: 쇼핑 GNB의 "마이페이지 › 배송지 관리" 링크(로그인 상태에서만 노출, `Gnb.tsx:54-57`) 또는
  URL 직접 접근. 마이페이지 허브 화면은 없다 — 이 라우트 1개가 유일한 진입점.
- 비로그인 접근: 세션 없으면 즉시 `/login?redirect=%2Fshop%2Fmypage%2Faddresses`로 리다이렉트하고,
  로그인 성공 후 기존 `redirectTarget` 규약으로 이 화면에 복귀한다(`MyAddressesPage.tsx:61-65`).
- 구현 근거: SR-235.1, `docs/변경관리/SR-235/STORY-1.md`(STORY-FUNC-member-010).

## 2. 화면 구성 (블록)

| 마커 | 블록 | 역할 | 주요 위젯 | 소스 근거 |
|---|------|------|----------|----------|
| ① | GNB(공통) | 로그인 상태 표시·검색·장바구니 배지·마이페이지 링크·로그아웃 | `Gnb` | `Gnb.tsx:54-57` |
| ② | 배송지 목록 | 조회 결과(최근 사용 순, 서버 정렬 그대로 렌더) + 등록 진입 | 헤더(건수)·[추가]·행(기본배지/수령인·연락처/주소/[기본으로 설정]/[수정]/[삭제]) | `AddressList.tsx` |
| ③ | 등록·수정 폼(목록 하단 인라인) | 배송지 입력/수정 | 수령인·연락처·우편번호(읽기전용+찾기버튼)·도로명(읽기전용)·상세주소·공동현관출입방법·배송요청사항·기본설정 체크박스(등록만)·저장/취소 | `AddressForm.tsx`, `MyAddressesPage.tsx:264-271` |
| ④ | 우편번호 검색 모달(오버레이) | 도로명/건물명으로 검색해 우편번호·도로명주소 선택 | 검색어 입력+검색, 결과 목록/0건 안내, 닫기 | `ZipcodeSearchModal.tsx`(기존 부품 재사용, 수정 없음) |
| ⑤ | 삭제 확인 다이얼로그(오버레이) | 삭제 전 확인 + 기본배송지 승계 안내 | 대상 요약(수령인·도로명주소)·[취소]·[삭제] | `AddressDeleteConfirm.tsx` |

목록·폼·모달·삭제확인 4개 블록은 전부 fetch를 직접 하지 않는 표시 전용 부품이며(규칙
`web-fetch-only-in-api`), 실제 API 호출은 전부 컨테이너 `MyAddressesPage.tsx`가 오케스트레이션한다.

## 3. 입력·검증 규칙

**등록/수정 폼(③) 필드**

| 필드 | 필수 | 입력 방식 | 비고 |
|---|---|---|---|
| 수령인(recipient) | O | 텍스트 입력 | 미입력 시 "수령인을 입력해 주세요" |
| 연락처(phone) | O | 텍스트 입력 | 미입력 시 "연락처를 입력해 주세요" |
| 우편번호(zipcode) + 도로명주소(roadAddress) | O(한 쌍) | **읽기전용** — [우편번호 찾기] 클릭 → 모달(④)에서 선택해야만 채워짐, 직접 타이핑 불가 | 둘 중 하나라도 비면 "우편번호 찾기로 주소를 선택해 주세요"(zipcode 필드 오류로 통합) |
| 상세주소(detailAddress) | O | 텍스트 입력 | 미입력 시 "상세주소를 입력해 주세요" |
| 공동현관 출입 방법(entranceMethod) | 선택 | 텍스트 입력 | 검증 없음 |
| 배송 요청사항(deliveryMemo) | 선택 | 텍스트 입력 | 검증 없음 |
| 기본 배송지로 설정(isDefault) | 선택, **등록 모드에서만 노출** | 체크박스 | 수정 모드는 렌더 자체를 하지 않음(기본 지정은 전용 API `setDefaultAddress`만 담당, `AddressForm.tsx:100-108`). 현재 목록이 0건(최초 배송지)이면 `disabled + checked` 강제 표시 + "첫 배송지는 자동으로 기본 배송지로 설정됩니다" 안내(서버가 어차피 강제하므로 사용자가 끌 수 없음을 미리 고지, `isFirstAddress` prop) |

- **클라이언트 우선 검증**: [저장] 클릭 시 `validateDeliveryAddress`(`deliveryAddressValidation.ts`, 주문서와
  공유하는 순수 함수)가 수령인·연락처·zipcode·상세주소를 검사하고, 하나라도 실패하면 API를 호출하지 않고
  필드별 인라인 오류만 표시한다(`MyAddressesPage.tsx:125-131`).
- **서버 최종 검증**(클라이언트가 못 잡는 형식 오류의 최종 방어선): 필수값 누락 `400 MBR-4200` — 서버는
  필드별로 나누지 않고 문장 하나만 주므로 폼 하단 일반 오류 영역에 그대로 표시(`saveError`, 필드 매핑 없음).
- **등록 상한**: 11번째 등록 시도 → `409 MBR-4201`("배송지는 최대 10개") 인라인 표시. 목록이 10건이면
  애초에 [추가] 버튼이 비활성화되어 폼 진입 자체가 막힌다(§5).
- **존재/소유 오류**: 수정·삭제 대상이 없거나 남의 배송지면 `404 MBR-4041` — 두 경우를 구분하지 않고
  서버 메시지를 그대로 표시(존재 오라클 방지, 서버 `selectOwned` 단일 SQL이 판정).
- **우편번호 검색어**: 2자 미만이면 `400 MBR-4202`("검색어" 관련 안내) — `searchZipcodes` 호출 결과로
  받아 모달 내 인라인 오류(`error` prop)로 표시.

## 4. 호출 API

> 인증: 아래 배송지(`/api/members/me/addresses*`) 5개 호출은 전부 `X-Api-Key: session.apiKey`(회원 자신의
> 키)를 명시 헤더로 싣는다. `memberId`는 쿼리로 넘기지 않는다 — `ApiKeyAuthFilter`의 `SCOPE_TO_SELF`가
> 헤더의 키로 회원을 강제한다(`api.ts:288-309`). 우편번호 검색은 인증 불요(공개 API).

| 트리거 | 메서드/엔드포인트(raw) | 함수(`api.ts`) | 결과 |
|---|---|---|---|
| 화면 진입(세션 확인 후 자동) | GET /api/members/me/addresses | `fetchAddresses` | `{items}` 봉투를 풀어 배송지 배열 반환(0건도 200, 최근 사용 순은 서버 정렬) |
| [저장](등록 모드) | POST /api/members/me/addresses | `registerAddress` | 201 + 생성된 배송지. 로컬 목록 맨 앞에 추가(재조회 없음), 기본이 됐으면 다른 행의 배지를 로컬에서 내림 |
| [저장](수정 모드) | PUT /api/members/me/addresses/{addressId} | `updateAddress` | 200 + 수정된 배송지. 로컬에서 해당 행 치환. `isDefault`는 서버가 무시 |
| [삭제] 확인 | DELETE /api/members/me/addresses/{addressId} | `deleteAddress` | 204(본문 없음). 삭제 대상이 **기본이 아니었으면** 로컬 목록에서 filter, **기본이었으면** 승계된 새 기본을 서버가 알려주지 않으므로 `fetchAddresses`로 **재조회** |
| [기본으로 설정] | PUT /api/members/me/addresses/{addressId}/default | `setDefaultAddress` | 200 + 갱신된 배송지. 응답 행으로 갱신 + 로컬에서 이전 기본 행의 `isDefault`를 `'N'`으로 내림(이미 기본인 행은 버튼 자체를 숨겨 호출을 만들지 않음) |
| [우편번호 찾기] → 검색 | GET /api/zipcodes?q={검색어} | `searchZipcodes` | `{items}` 봉투. 결과 있음/0건 모두 200, 검색어 2자 미만은 400 MBR-4202 |

연타 방지: 삭제·기본설정은 `inFlightAddressIdRef`(단일 ref, addressId 보관)로 같은 대상에 대한 중복 호출을
막는다(`MyAddressesPage.tsx:57,175,200`). 목록 로드는 `inFlightKeyRef` + `requestIdRef`로 StrictMode 이중
실행에 안전하다.

## 5. 표시 조건(상태)

> 스토리북 스토리가 있으면 `[이름](story:ID)`로 연결(`.speclinker/storybook_index.json` 기준). `ZipcodeSearchModal`은
> 기존 재사용 부품이라 스토리가 `UIS-ORD-011`(주문서)에 연결돼 있지만 이 화면도 수정 없이 그대로 쓴다.

| 요소 | 표시 조건 | 근거 | 스토리 |
|------|----------|------|--------|
| 로그인 게이트 | `session.memberId` 없음 → `/login?redirect=...`로 즉시 이동, 목록 조회조차 안 함 | `MyAddressesPage.tsx:61-65` | (컨테이너, 스토리 대상 아님) |
| 목록 0건 | `addresses.length === 0` → "등록된 배송지가 없습니다" + [추가] 활성 | `AddressList.tsx:56-59` | [목록 0건](story:마이페이지-배송지-목록--목록-0건) |
| 목록 1건 이상 | 각 행에 수령인·연락처·주소, `isDefault==='Y'`인 행만 "기본" 배지 | `AddressList.tsx:67-71` | [목록 1건](story:마이페이지-배송지-목록--목록-1건) |
| 목록 10건 상한 | `addresses.length >= 10` → [추가] disabled + "배송지는 최대 10개까지 등록할 수 있습니다" 안내 | `AddressList.tsx:38,44-54` | [목록 10건상한](story:마이페이지-배송지-목록--목록-10건상한) |
| 기본 배송지 전환 중 | 해당 행이 `pendingAddressId`와 일치 → "처리 중…" 표시 + 그 행 버튼 전부 disabled | `AddressList.tsx:63,74-89` | [기본배송지전환중](story:마이페이지-배송지-목록--기본배송지전환중) |
| 폼 — 신규 | `formMode.kind==='add'`, 빈 값에서 시작, isDefault 체크박스 노출 | `MyAddressesPage.tsx:102-107`, `AddressForm.tsx:101-113` | [신규](story:마이페이지-배송지-폼--신규) |
| 폼 — 수정 | `formMode.kind==='edit'`, 기존 값 prefill, isDefault 체크박스 숨김 | `MyAddressesPage.tsx:109-120`, `AddressForm.tsx:100` | [수정](story:마이페이지-배송지-폼--수정) |
| 폼 — 검증 오류 | 필수값 누락 필드마다 인라인 오류(수령인/연락처/우편번호/상세주소) | `deliveryAddressValidation.ts:18-25`, `AddressForm.tsx` errors 표시부 | [검증오류](story:마이페이지-배송지-폼--검증오류) |
| 우편번호 검색 — 결과 있음 | `results.length > 0` → 클릭 가능한 목록 | `ZipcodeSearchModal.tsx:56-69` | 결과있음(주문서 스토리 재사용, UIS-ORD-011) |
| 우편번호 검색 — 0건 | `results.length === 0` → "검색 결과가 없습니다" | `ZipcodeSearchModal.tsx:70-72` | 결과없음(주문서 스토리 재사용, UIS-ORD-011) |
| 삭제 확인 — 일반 | `deleteTarget != null`, `isDefault === 'N'` → 대상 요약만 | `AddressDeleteConfirm.tsx:33-37` | [일반삭제확인](story:마이페이지-배송지-삭제-확인--일반삭제확인) |
| 삭제 확인 — 기본 배송지 | `deleteTarget.isDefault === 'Y'` → "삭제하면 다음 배송지가 자동으로 기본이 됩니다" 안내 추가 | `AddressDeleteConfirm.tsx:38-42` | [기본배송지삭제확인](story:마이페이지-배송지-삭제-확인--기본배송지삭제확인) |
| API 오류(공통 인라인) | 목록 로드 실패(`loadError`)·행 액션 실패(`actionError`)·저장 실패(`saveError`) — 코드별 문구 그대로(MBR-4200/4041/4201/4202/5000) | `MyAddressesPage.tsx` 각 `.catch(toDisplayMessage)` | (정적 props 스토리 대상 아님 — 통합테스트로 커버) |

> 보강: `/sl-sync --apply --kind=uis` 또는 ddd-ui-agent

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-19 | SR-235 | #1 | 마이페이지 배송지 관리 화면 신규 구현(목록·등록/수정 폼·우편번호 검색 모달·삭제확인·기본배송지 전환) — 화면설계서 본문 코드 기준 보강 | shop-api@1c82e8c, shop-web@8e1e3e3 |
| 2026-09-19 | SR-235 | #2 | 본문 보강(ddd-ui-agent, 소스폴백 — 화면개요/구성/검증규칙/호출API/표시조건) | shop-api@1c82e8c, shop-web@8e1e3e3 |
