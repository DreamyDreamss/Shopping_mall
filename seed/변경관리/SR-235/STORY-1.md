---
item: SR-235.1
title: 배송지 관리 화면
legacy_func: FUNC-member-010
story-id: STORY-FUNC-member-010
func-id: FUNC-member-010
status: Done
domain: member
created: 2026-09-13
spec_markers: 0
sr-id: SR-235
approved_sha: 829b42f3b50c
---

# STORY-FUNC-member-010 — SR-235 — 배송지 관리 화면 · 신규 UIS-MBR-004 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)

## Story
SR-235 — 배송지 관리 화면 · 신규 UIS-MBR-004 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)


## 변경 컨텍스트 (SR-235)
> 이 story는 변경요청 **SR-235 — SR-235** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-235/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-235/02_변경명세.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 배송지 CRUD API(목록·등록·수정·삭제·기본 설정, 회원당 최대 10개·기본 1개·기본 삭제 시 최근 사용 순 다음 배송지 승계) · 우편번호(도로명) 검색 API(실 API 연동 없이 로컬 샘플 테이블 ZIPCODES 시드로 검색) · 마이페이지 배송지 관리 화면. 제외(이월): 주문서 인라인 추가/선택·주문서 기본 배송지 미리 선택은 SR-255(주문서)에서, 실 우편번호 API 연동은 후속 SR
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 회원가입·로그인·주문 생성 흐름과 주문 API의 배송지 입력 계약(주문 본문에 주소 문자열)은 그대로. MEMBERS·ORDERS 테이블 컬럼 변경 없음
- **기존 클라이언트와의 하위호환이 필요한가?** — 추가만 — 새 엔드포인트 /api/members/me/addresses(회원 토큰 인증), /api/zipcodes?q=. 기존 필드명·타입·의미 불변
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 기존 오류 봉투(code+message). 11번째 등록 409 MBR-4201 '배송지는 최대 10개', 없는 배송지 404 MBR-4041, 남의 배송지 접근도 404(존재 노출 금지), 필수값 누락 400 MBR-4200, 우편번호 검색어 2자 미만 400 MBR-4202
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — MEMBER_ADDRESSES 신설(address_id PK, member_id FK, recipient, phone_norm, zipcode, road_address, detail_address, entrance_method, delivery_memo, is_default, last_used_at, created_at, updated_at, del_yn) + ZIPCODES 샘플(zipcode, road_address, sido, sigungu — 시드 100건). 타 도메인 테이블·집계 파급 없음
- **기존 데이터 이관·백필이 필요한가?** — Flyway V7__member_addresses.sql(CREATE TABLE IF NOT EXISTS, 랩 규칙 ddl-idempotent) + ZIPCODES 시드 INSERT IGNORE. 기존 데이터 이관·백필 없음
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — shop-web 마이페이지 '배송지 관리' 화면 1개: 목록(최근 사용 순, 기본 배지) · 추가/수정 폼 · 우편번호 검색 모달 · 기본 배송지 설정 · 삭제 확인. 마이페이지 진입 링크 1줄. 주문서 화면은 이 SR 범위 밖
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 0건: '등록된 배송지가 없습니다' + [추가] · 10건 도달: [추가] 비활성 + '최대 10개' 안내 · 검색 0건: '검색 결과가 없습니다 — 도로명·건물명으로 다시 검색' · 저장/삭제 실패: 인라인 오류(code 메시지 그대로)
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 목록(0건·1건·10건 상한) · 폼(신규·수정·검증 오류) · 우편번호 검색 모달(결과 있음·0건) · 삭제 확인 · 기본 배송지 전환 — 부품마다 스토리(story-per-component)

### 구현 모듈(제약) — `shop-web` (`{{SRC_SHOP_WEB}}`)
이 FUNC의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약 폼에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
- [x] 새 라우트 `#/shop/mypage/addresses` 1개로 배송지 관리 화면 진입(마이페이지 허브 화면은 만들지 않는다 — 범위 밖)
- [x] 쇼핑 GNB에 로그인 상태일 때만 '마이페이지 › 배송지 관리' 링크 1줄 노출, 클릭 시 위 라우트로 이동
- [x] 비로그인 상태로 그 라우트 진입 시 기존 로그인 화면으로 리다이렉트, 로그인 성공 후 원래 경로(redirectTarget 규약 재사용)로 복귀
- [x] 목록: 최근 사용 순 정렬, 기본 배송지 배지 표시
- [x] 목록 0건: '등록된 배송지가 없습니다' 안내 + [추가] 버튼
- [x] 목록 10건 도달: [추가] 버튼 비활성 + '최대 10개' 안내(회원당 최대 10개 제약)
- [x] 추가/수정 폼: 수신인·전화번호·우편번호/도로명주소·상세주소·공동현관 출입 방법·배송 요청사항 입력, 필수값 누락 시 인라인 검증 오류
- [x] 우편번호 검색 모달: 도로명/건물명으로 로컬 ZIPCODES API 검색, 결과 있음/0건('검색 결과가 없습니다' — 공유 `ZipcodeSearchModal`의 기존 문구 그대로, 이 항목에서 문구를 바꾸지 않는다) 상태 모두 표시, 검색어 2자 미만이면 요청하지 않거나 인라인 오류(MBR-4202)로 표기
- [x] 기본 배송지 설정: 목록에서 다른 배송지를 기본으로 전환 가능
- [x] 기본 배송지 삭제: 삭제 확인 후 최근 사용 순 다음 배송지가 자동으로 기본 승계
- [x] 삭제: 삭제 전 확인 다이얼로그
- [x] API 오류는 code/message 그대로 인라인 표기 — 409 MBR-4201(배송지 최대 10개), 404 MBR-4041(없는 배송지/남의 배송지), 400 MBR-4200(필수값 누락), 400 MBR-4202(검색어 2자 미만)
- [x] API 호출은 기존 `api.ts`의 인증 헤더 규약을 그대로 사용(부품에서 직접 fetch 금지, 백엔드 INF-MBR-008/009는 이미 구현되어 있음 — 건드리지 않는다)
- [x] 화면설계서 §5 상태별 스토리(story-per-component, must): 목록(0건·1건·10건 상한) · 폼(신규·수정·검증 오류) · 검색 모달(결과 있음·0건) · 삭제 확인 · 기본 배송지 전환

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS** UIS-MBR-004
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)

## 구현 계획

> 백엔드(INF-MBR-008 배송지 CRUD, INF-MBR-009 우편번호 검색)는 **이미 구현·문서화 완료**(SR-235 이전 라운드,
> `MemberAddressController`/`ZipcodeController` 등 실측 확인). 이 항목은 **shop-web 프론트만** — 컨트롤러·서비스·DAO·
> DDL·`ApiKeyAuthFilter`는 건드리지 않는다(STORY "Dev 기록" 재승인 조건과 동일).

- **파일** (모두 `modules/shop-web` 안)
  - 신규 페이지(컨테이너, 스토리 대상 아님 — 규칙 `story-per-component`는 `src/pages/` 제외)
    - `src/pages/MyAddressesPage.tsx` — `/shop/mypage/addresses`. 세션 유무 판정(없으면 로그인으로 리다이렉트) +
      목록 조회 + 등록/수정 폼 열기·닫기 + 삭제확인 다이얼로그 + 기본설정 오케스트레이션. `CartPage.tsx`/`OrderPage.tsx`와
      동일한 컨테이너 관례(Gnb 배치, `loadSession`/`saveSession`/`clearSession`, `handleLogout`, `handleSearchSubmit`
      그대로 복사). 300줄(should 상한)에 근접하면 폼 상태(add/edit 공용) 훅 `useAddressForm`으로 분리하거나
      `AddressFormPanel`(폼+저장 오케스트레이션 래퍼, 이 파일도 `src/pages/`가 아니라 컨테이너 보조라 필요시 별도
      export 없이 같은 파일 내 하위 함수로 유지 — 새 스토리 대상 컴포넌트를 만들지 않는다)로 쪼갠다.
  - 신규 부품(각각 `.stories.tsx` 동반, 규칙 `story-per-component`) — `src/features/member/` (member 도메인,
    `PasswordResetXxxStep.tsx`와 같은 위치 관례)
    - `AddressList.tsx` — 헤더("배송지 관리" + 건수) + 목록(최근 사용 순, 이미 정렬된 배열을 그대로 받아 렌더 —
      정렬 자체는 서버 `selectList`가 함, 프론트 재정렬 없음) + 행마다 기본 배지("기본")·수령인/연락처/주소·
      [수정]·[삭제]·[기본으로 설정](이미 기본인 행은 숨김/비활성)·행별 pending(연타 방지 표시) + 0건 안내
      ("등록된 배송지가 없습니다" + [추가]) + 10건 도달 시 [추가] 비활성 + "최대 10개" 안내. 스토리 3종:
      `목록_0건`(addresses: []) · `목록_1건`(addresses 1개, 기본배지) · `목록_10건상한`(addresses 10개, add 버튼
      disabled). props: `addresses: MemberAddress[]`, `onAdd`, `onEdit(addressId)`, `onDelete(addressId)`,
      `onSetDefault(addressId)`, `pendingAddressId: number | null`(기본설정/삭제 진행 중인 행 — 이 값이 그 행의
      버튼들을 비활성화해 연타를 시각적으로도 막는다).
    - `AddressForm.tsx` — 수령인·연락처·우편번호/도로명(읽기전용 + [우편번호 찾기] 버튼, `DeliveryAddressForm`과
      동일한 읽기전용 관례이나 별도 컴포넌트 — 아래 "재사용" 참고)·상세주소·공동현관 출입방법(선택)·배송
      요청사항(선택)·기본 배송지로 설정(체크박스, 신규 등록이 회원 최초 배송지면 이 체크박스를 disabled+checked로
      표시하고 안내 문구 — 서버가 무조건 기본 강제하므로 사용자가 끌 수 없음을 미리 알림, `isFirstAddress: boolean`
      prop) + 필드별 인라인 오류 + [저장]/[취소]. 스토리 3종: `신규`(빈 값) · `수정`(기존 값 prefill, 체크박스
      숨김 — INF-MBR-008 "수정은 isDefault를 다루지 않음") · `검증오류`(recipient/phone/zipcode/detailAddress
      전부 오류, `tags: ['shows-error']` 불필요 — 콘솔 오류 없는 순수 인라인 텍스트).
    - `AddressDeleteConfirm.tsx` — `ZipcodeSearchModal`과 동일한 오버레이+패널(`role="dialog"`) 스타일로 새로
      만든다(기존에 재사용할 만한 confirm 다이얼로그 없음, 실측 확인). "이 배송지를 삭제할까요?" + 대상 요약
      (수령인·도로명주소) + 기본 배송지면 "삭제하면 다음 배송지가 자동으로 기본이 됩니다" 안내 + [취소]/[삭제].
      스토리 2종: `일반삭제확인` · `기본배송지삭제확인`(승계 안내 문구 포함).
    - **재사용, 새 컴포넌트 아님**: `ZipcodeSearchModal`(`src/features/shop/ZipcodeSearchModal.tsx`)을 그대로
      import해 쓴다 — props가 이미 도메인 비종속(`open/query/onQueryChange/onSearch/results/loading/error/onSelect/
      onClose`)이라 수정 불필요. 기존 스토리(`결과있음`·`결과없음`)가 이미 AC가 요구하는 두 상태를 커버하므로
      스토리 신규 추가 불필요(파일 수정 없음).
    - **기본설정 전환 상태**: 별도 컴포넌트를 만들지 않는다 — `AddressList`의 세 번째 스토리(`목록_10건상한`) 외에
      pending 상태를 보여주는 스토리를 하나 더 추가한다: `AddressList.stories.tsx`에 `기본배송지전환중`(어느 한
      행이 `pendingAddressId`로 잠긴 상태, 버튼 disabled 시각 확인) 4번째 스토리 — AC "기본 배송지 전환" 상태
      요구를 이 스토리가 충족한다.
  - 순수 로직 — **재사용, 새 파일 아님**: `src/features/shop/deliveryAddressValidation.ts`의
    `validateDeliveryAddress`/`DeliveryAddressErrors`/`DeliveryAddressInput`을 `AddressForm`이 그대로 import해
    쓴다(수령인/연락처/우편번호+도로명/상세주소 필수값 검증 로직이 완전히 동일 — 중복 정의 금지). 공동현관
    출입방법·배송요청사항·기본설정 체크박스는 선택 필드라 이 함수가 검증하지 않는 것이 맞다(추가 검증 불필요).
  - 수정
    - `src/api.ts` — 신규 함수 5개, 전부 `session.apiKey`를 `X-Api-Key`로 **명시** 헤더에 실어 보낸다(아래
      "순서·보안" 2 참고). 기존 `ApiError`/`parseErrorBody`(모듈 내부, `{code,message}` 봉투 전용)를 그대로
      재사용 — `OrderHttpError` 계열이 아니다(이 컨트롤러들은 `MemberAddressExceptionHandler`/
      `ZipcodeExceptionHandler`로 `{code,message}` 봉투를 낸다, `login`/`requestPasswordResetCode`와 동일 계열).
      - `fetchAddresses(apiKey: string): Promise<MemberAddress[]>` — `GET /api/members/me/addresses`,
        `{items:[...]}` 봉투를 풀어 반환.
      - `registerAddress(apiKey: string, input: MemberAddressInput): Promise<MemberAddress>` — `POST`, 201.
      - `updateAddress(apiKey: string, addressId: number, input: MemberAddressInput): Promise<MemberAddress>` —
        `PUT /api/members/me/addresses/{addressId}`, 200.
      - `deleteAddress(apiKey: string, addressId: number): Promise<void>` — `DELETE`, 204(본문 없음, `postVoid`와
        동일하게 바디를 읽지 않는다).
      - `setDefaultAddress(apiKey: string, addressId: number): Promise<MemberAddress>` —
        `PUT /api/members/me/addresses/{addressId}/default`, 200.
      - `memberId` 쿼리 파라미터는 **어느 함수에도 넣지 않는다** — `ApiKeyAuthFilter`의 `SCOPE_TO_SELF`가 회원
        키 기준으로 강제 주입한다(INF-MBR-008 "인증·스코프" 절, 클라이언트가 직접 넘기지 않는 계약).
      - `searchZipcodes(q)`는 기존 함수를 **수정 없이** 그대로 쓴다(admin 프록시 키로도 통과하는 전역 공개
        검색, INF-MBR-009 "인증" 절 — member 키를 강제할 이유 없음).
    - `src/types.ts` — `MemberAddress`(INF-MBR-008 응답 셰이프 그대로: `addressId, memberId, recipient, phone,
      phoneNorm, zipcode, roadAddress, detailAddress, entranceMethod, deliveryMemo, isDefault('Y'|'N' 문자열,
      `Product.saleYn`과 동일 관례 — boolean으로 바꾸지 않는다), lastUsedAt, createdAt, updatedAt`)와
      `MemberAddressInput`(요청 바디: `recipient, phone, zipcode, roadAddress, detailAddress, entranceMethod?,
      deliveryMemo?, isDefault?`) 신규 타입 추가.
    - `src/App.tsx` — `<Route path="/shop/mypage/addresses" element={<MyAddressesPage />} />` 1개 추가(SR-305
      두 라우트 추가와 동일한 관례, 기존 라우트 불변).
    - `src/features/shop/Gnb.tsx` — `session` 블록(로그인 상태) 안에 "마이페이지 › 배송지 관리" 링크
      (`<a href="#/shop/mypage/addresses">`) 1줄 추가. 비로그인 블록은 손대지 않는다(AC "로그인 상태일 때만
      노출" 그대로 — 기존 `session ? (...) : (...)` 삼항의 참 분기에만 추가). 기존 `Gnb.stories.tsx`의
      `로그인`/`장바구니담김` 스토리는 args 변경 없이 그대로 이 링크를 함께 렌더하게 된다(회귀 없음, 별도
      스토리 추가 불필요 — 새 상태가 아니라 기존 로그인 상태의 부속 요소).

- **데이터**: 신규 테이블·DDL 없음(`V7__member_addresses.sql`·`ZIPCODES` 시드 모두 이전 라운드에 이미 반영).
  트랜잭션 경계도 전부 백엔드에 이미 구현됨(`MemberAddressService` — 락 순서·존재+소유 단일 SQL·기본 승계
  전부 완료) — 이 항목에서 새로 열거나 잠그는 행 없음. 프론트는 순수 HTTP 클라이언트다.

- **순서·보안**
  1. 라우트 진입 → `loadSession()`으로 세션 확인. 없으면 `useEffect`에서 즉시
     `navigate('/login?redirect=' + encodeURIComponent('/shop/mypage/addresses'), { replace: true })` — 로그인
     성공 후 `LoginPage.handleSubmit`이 기존 `resolveRedirectTarget(params.get('redirect'))`로 이 경로에 그대로
     복귀한다(코드 변경 없음, 프론트 전용 관례 재사용).
  2. 세션이 있으면 목록 조회 — `fetchAddresses(session.apiKey)`. `session.apiKey`는 로그인 응답이 발급한
     **회원 자신의 키**이고, `X-Api-Key` 헤더에 명시적으로 실어 보낸다(`logout(apiKey)`와 동일 패턴).
     ⚠ vite 프록시(`vite.config.ts`)는 클라이언트가 이미 `X-Api-Key`를 보낸 요청은 admin 키로 덮어쓰지 않지만,
     헤더를 아예 빼먹으면(fetch 기본 호출) 프록시가 admin 키(`lab-admin-key`, `*` 스코프)로 채운다 → admin
     스코프는 `evaluateMemberScope` 자체를 안 타서 `SCOPE_TO_SELF` 강제가 안 일어나고 `memberId` 쿼리가 비어
     컨트롤러의 `requireMemberId`가 즉시 400 `MBR-4200`으로 막는다(INF-MBR-008 "인증·스코프" 절 실측 근거) —
     따라서 addresses 5개 함수는 반드시 `apiKey` 인자를 받아 헤더에 싣는다(빠뜨리면 개발 모드에서 목록조차
     못 뜬다, 바로 드러나는 실패라 은폐 위험은 낮지만 계획 단계에서 명시).
  3. 등록/수정 저장 클릭 → `validateDeliveryAddress`(재사용)로 클라이언트 필수값 검증 먼저(API 호출 없음),
     통과해야 `registerAddress`/`updateAddress` 호출. 서버 400(`MBR-4200`)은 클라이언트가 못 잡는 형식 오류
     (전화번호 패턴·길이 등)의 최종 방어선 — 응답 `message`를 그대로 인라인(일반 오류 영역, 필드 매핑 없음
     — 서버가 필드별로 나눠 주지 않고 문장 하나만 준다, INF-MBR-008 "요청" 절 실측).
  4. 저장 성공(등록 201/수정 200) → 응답으로 받은 배송지 객체로 로컬 목록 갱신(등록은 append, 수정은 해당
     `addressId` 치환) 후 폼 닫기. 재조회(`fetchAddresses`) 없이 응답값만으로 충분 — `MemberAddressController`
     응답이 갱신된 전체 셰이프를 이미 돌려준다.
  5. 삭제 클릭 → `AddressDeleteConfirm` 오픈(즉시 DELETE 금지, AC "삭제 전 확인 다이얼로그"). [삭제] 확인 시에만
     `deleteAddress` 호출, `inFlightAddressIdRef`(단일 ref, `CartPage.inFlightSkusRef`와 동일 원리)로 연타 방지.
     204 성공 시 로컬 목록에서 그 `addressId` 제거 — 응답 바디가 없어(No Content) 승계된 새 기본 배송지를
     서버가 알려주지 않으므로, 삭제가 이전 기본을 없앴다면(`isDefault==='Y'`였던 행을 지웠다면) 목록을
     `fetchAddresses`로 **재조회**한다(로컬에서 "다음 후보"를 프론트가 재계산하지 않는다 — 정렬·승계 판정
     로직을 서버와 이중 구현하면 어긋날 위험, 정본은 서버 재조회로 확인).
  6. 기본 설정 클릭 → `setDefaultAddress` 호출(같은 `inFlightAddressIdRef`로 연타 방지), 200 응답 배송지로
     그 행을 갱신 + 이전 기본이었던 행의 `isDefault`를 로컬에서 `'N'`으로 내린다(서버가 `clearDefaultForMember`
     로 함께 바꾼 값 — 재조회 없이 로컬에서 반영 가능, 응답이 새 기본 행 1개뿐이라 이전 기본 행 로컬 갱신은
     프론트가 계산). 이미 기본인 행은 버튼을 숨겨 애초에 호출 자체가 안 나가게 한다(no-op 호출 방지, 서버도
     no-op이지만 불필요 요청을 만들지 않는다).
  7. 부수효과(로그·발송·이벤트) 없음 — 이 항목은 순수 CRUD 프론트라 판정 순서 뒤에 오는 사이드이펙트가 없다.
  8. 로그아웃(Gnb): `CartPage.handleLogout`/`OrderPage.handleLogout`과 동일 패턴 그대로 복사
     (`logout(apiKey)` 시도 후 무조건 `clearSession`).

- **계약**: 신규 오류 코드 없음(기존 `MBR-4200`/`MBR-4041`/`MBR-4201`/`MBR-4202`/`MBR-5000` 그대로 소비).
  신규 응답 봉투 없음. 신규 API 없음(INF-MBR-008/009 둘 다 이미 구현·문서화 완료) — 프론트가 새로 여는 것은
  `api.ts` 클라이언트 함수 5개뿐, 백엔드 계약은 1바이트도 바뀌지 않는다.

- **테스트**
  - `AddressList.stories.tsx`(4종) · `AddressForm.stories.tsx`(3종) · `AddressDeleteConfirm.stories.tsx`(2종) —
    축E(`story_gate.py`) 대상, 전부 정적 props라 콘솔 오류 없음(`shows-error`/`renders-nothing` 태그 불필요,
    사례집 SR-306 #1 r3~r4 함정과 조건이 다름 — 실제 네트워크 호출이 없다).
  - `src/pages/MyAddressesPage.test.tsx`(신규, `CartPage.test.tsx`/`OrderPage.test.tsx` 관례 — jsdom, URL+메서드
    분기 `fetch` mock, `useNavigate`만 스텁, 테스트별 고유 `memberId`/`apiKey`) HTTP 레벨 단언:
    - 비로그인 진입 → `mockNavigate`가 `'/login?redirect=' + encodeURIComponent('/shop/mypage/addresses')`로
      1회 호출되고 `GET /api/members/me/addresses`는 아예 호출되지 않는다(`CartPage`의 "로그인 안 됨" 테스트와
      동일 원리 — `fetchMock`이 안 불려야 한다).
    - 로그인 상태 → 목록 fetch 호출 시 실제로 보낸 헤더가 `X-Api-Key: session.apiKey`인지 직접 단언(호출 인자
      검사, admin 키로 새는지 여기서 잡는다 — 사례집 SR-232 r3 "인증 필요 컨트롤러 테스트가 실제로 보낸 키를
      확인하지 않아 헤더 계약이 조용히 깨져도 못 잡은" 교훈과 동일한 이유로, mock 호출의 두 번째 인자
      `init.headers['X-Api-Key']`를 명시적으로 비교한다).
    - 0건/1건/10건 각각 렌더(안내 문구·추가 버튼 활성/비활성).
    - 등록: 필수값 누락 → 인라인 오류 표시, API 미호출. 정상 입력 → `POST` 호출 후 목록에 반영.
    - 11번째 등록 시도(서버가 409 `MBR-4201`을 돌려주는 케이스를 mock) → 그 문구 그대로 인라인.
    - 수정: 기존 값 prefill 확인, 저장 후 목록의 해당 행 갱신.
    - 삭제: 확인 다이얼로그 없이는 `DELETE` 미호출 → [삭제] 확인 클릭 후에만 호출 → 그 행 제거.
    - 기본이었던 배송지 삭제 → 삭제 후 `GET` 재호출(승계 재조회 검증) 확인.
    - 기본 설정 전환 → `PUT .../default` 호출 후 배지가 그 행으로 옮겨간다(이전 기본 배지 사라짐).
    - 404(`MBR-4041`, 남의 배송지/미존재) → 인라인 문구, 동일 코드로 두 시나리오(수정 대상 없음, 삭제 대상
      없음) 각각 재현해 "존재 오라클 없음"을 프론트도 구분하지 않는지 확인(단순히 message 그대로 표시하는지만
      보면 충분 — 프론트가 별도 분기를 만들지 않는 것이 계약).
    - 연타 방지: 삭제/기본설정 버튼 2회 연속 클릭 → API 호출 1회만(`act()` 한 스코프 안에서 묶어 발사,
      사례집 SR-302 #1 — 분리 `fireEvent.click()` 3회는 두 번째 클릭이 이미 사라진 버튼에 발사돼 거짓
      통과를 만든다는 교훈 그대로 재사용).
  - `api.ts` 신규 5개 함수는 별도 유닛테스트 파일을 만들지 않는다(기존 관례 — `login`/`refreshSession`도 전용
    유닛테스트가 없고 페이지 통합테스트의 mock fetch로 간접 검증된다).
  - `deliveryAddressValidation.ts`는 재사용만 — 기존 `deliveryAddressValidation.unit.test.ts` 커버리지가
    그대로 유효하다(신규 테스트 불필요).
  - 회귀 확인은 계획에 적힌 좁은 범위가 아니라 최종적으로 `npm test`(`tsc` + jest 전체) 전체 실행으로
    한다(사례집 SR-307 #1 — 좁힌 범위 지정이 기존 회귀를 놓친 교훈).

- **테스트 격리**: `MyAddressesPage.test.tsx`는 매 테스트 `localStorage.clear()`(`beforeEach`/`afterEach`,
  `CartPage.test.tsx` 그대로) + 테스트별 고유 `memberId-N`/`apiKey-N`/`addressId`(숫자, 테스트 인덱스 기반)로
  분리한다. `fetch` mock은 URL+메서드 분기 함수를 매 테스트 `beforeEach`에서 `jest.fn()`으로 새로 만들어
  이전 테스트의 호출 이력이 새지 않게 한다(`patchCalls`/`deleteCalls` 배열과 동일 패턴으로 `postCalls`/
  `putCalls`/`deleteCalls` 각각 수집). 스토리북은 전부 정적 props(네트워크 없음)라 상태 오염 대상 자체가
  없다 — 별도 격리 조치 불필요.

- **폴백·우회 경로의 자격 판정**: 새로 여는 인증·조회 경로 없음 — `session.apiKey`(로그인/리프레시가 발급한
  회원 자신의 키) 하나만 그대로 싣는다. admin 프록시 폴백을 경유하면(헤더 누락) 위 "순서·보안" 2가 설명한
  대로 400으로 **즉시** 막혀 탈퇴·폐기 회원 우회 같은 조용한 통과 경로 자체가 성립하지 않는다(INF-MBR-008
  이미 SCOPE_TO_SELF로 자기 자신만 강제, 존재/탈퇴 판정은 서비스 계층 몫 — 이 항목이 새로 판정을 추가하거나
  건너뛰지 않는다). RUN8 r1(API 키 DB 폴백이 탈퇴 회원을 통과시킨 사례)과 달리 이 SR은 새 조회 경로를 열지
  않으므로 조건이 성립하지 않는다 — 확인만 해 둔다.

- **프레임워크 실행 모델 함정**: React 19 StrictMode(dev)가 최초 목록 로드 `useEffect`를 두 번 실행할 수
  있다. `GET`은 멱등이라 정확성에는 영향 없지만, `CartPage`/`OrderPage`의 `inFlightKeyRef` 패턴(같은
  `memberId` 키의 두 번째 호출을 억제)을 그대로 재사용해 불필요한 중복 요청·깜빡임을 없앤다. 로그인 리다이렉트
  `useEffect`는 사례집 SR-302 #1("최초 진입 시만" 지시를 라우트 element `<Navigate>` 치환으로 구현해 상시
  리다이렉트가 된 사례)과 조건이 다르다는 점을 명시한다 — 여기서는 "세션이 없을 때마다"가 정확한 요구이므로
  매 렌더 재평가가 정답이고, `<Route>` element 자체를 바꾸지 않고 `useEffect` 안의 `navigate()` 호출(부수효과)
  로만 구현해 SR-302의 회귀(주문 목록 영구 도달 불가류)를 재현할 위험이 애초에 없다. 삭제/기본설정처럼 부작용
  있는 호출은 `inFlightAddressIdRef`(단일 ref, `addressId` 값 보관)로 이중 클릭·이중 effect를 동일하게 차단한다.

- **범위 밖**: 주문서 인라인 추가/선택·주문서 기본 배송지 미리 선택(SR-255) · 실 우편번호 API 연동(후속 SR,
  현재 로컬 ZIPCODES 시드 검색 그대로) · 마이페이지 허브 화면(이번엔 배송지 관리 라우트 1개만, 재승인 조건과
  동일) · 백엔드 INF-MBR-008/009 코드 수정(이미 구현 완료, 건드리지 않는다) · `DeliveryAddressForm.tsx`
  (SR-305 주문서 전용) 확장/재사용 — 필드 셰이프가 달라(`entranceMethod`/`deliveryMemo`/`isDefault` 없음, 읽기전용
  주소 정책 등) 별도 `AddressForm.tsx`로 새로 만들고 기존 파일은 손대지 않는다(주문서 회귀 위험 차단).

- **실패 사례집 대조**: `harness/antipatterns.all.md` 전체를 확인했다.
  - SR-302 #1("최초 진입 시만" → `<Navigate>` 치환 → 상시 리다이렉트 회귀): 위 "프레임워크 실행 모델 함정"에서
    조건이 다름을 확인·기록 — 여기서는 상시 재평가가 정답이라 같은 함정이 성립하지 않는다.
  - SR-302 #1(연타 방지를 분리 `fireEvent.click()` 3회로 검증해 거짓 통과): 위 "테스트" 절 연타 방지 항목에
    `act()` 한 스코프 묶음 발사로 반영.
  - SR-232 r3(인증 필요 컨트롤러 테스트가 실제 전송 헤더를 확인하지 않아 헤더 계약 이탈을 못 잡음): 자바
    `@WebMvcTest`가 아니라 프론트 `fetch` mock이지만 동일한 교훈 — `MyAddressesPage.test.tsx`가 mock 호출
    인자의 `X-Api-Key` 값을 직접 단언하도록 위 "테스트" 절에 명시.
  - SR-231 r5(존재 판정을 인증보다 먼저 노출해 존재 오라클): 이 항목은 존재+소유 판정을 서버(`selectOwned`
    단일 SQL, 이미 구현)에 전적으로 위임하고 프론트는 404 메시지를 그대로 보여줄 뿐 별도 분기를 만들지
    않는다 — 위 "테스트" 절에 두 시나리오(미존재/남의 것) 동일 처리 확인 항목으로 반영.
  - SR-234 FUNC-member-007 r1(미정의 오류코드를 `default` 분기로 묶어 계약에 없는 화면 전이 생성): 이 화면의
    오류 처리도 계약표(`MBR-4200/4041/4201/4202/5000`)에 있는 코드만 인라인 표시하고, 그 외(네트워크 오류 등
    `ApiError`가 아닌 예외)는 일반 문구로 대체할 뿐 새 상태 전이를 만들지 않는다.
  - SR-306 #1 r3~r4(콘솔 error=스토리 깨짐 판정과 의도된 오류 상태 스토리의 충돌): 이 항목의 신규 스토리는
    전부 정적 props(네트워크 호출 없음)이므로 해당 함정이 성립하지 않는다 — 위 "테스트" 절에서 확인 기록.
  - RUN8 r1(API 키 DB 폴백이 탈퇴 회원을 무기한 통과): 위 "폴백·우회 경로의 자격 판정" 절에서 이 항목은 새
    조회 경로를 열지 않아 조건이 성립하지 않음을 확인.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
- story 재승인(2026-09-19, 사람): AC를 구체 목록으로 보강 후 승인. 조건: 새 라우트 `#/shop/mypage/addresses` 1개만 추가(마이페이지 허브 미생성), GNB 로그인 상태에서 링크 1줄, 비로그인 진입은 기존 로그인 화면+redirectTarget 복귀 규약 재사용, 백엔드(INF-MBR-008/009)는 이미 구현되어 있으므로 건드리지 않고 기존 `api.ts` 인증 헤더 규약만 사용.
- 계획 확인: 계획대로 진행 (2026-09-19, 사람). 조건: 우편번호 검색 모달의 상태별 스토리(결과있음·결과없음)는 기존 `src/features/shop/ZipcodeSearchModal.stories.tsx`가 이미 보유 — 새 스토리 추가하지 않고 그대로 재사용(파일 수정 없음).
- AC 정정(2026-09-19, 사람, QA FAIL round1 권고 처리): 검색 0건 문구를 공유 `ZipcodeSearchModal`의 실제 문구('검색 결과가 없습니다')에 맞게 AC를 고쳤다. 공유 부품은 주문서(SR-305)와 함께 쓰므로 이 항목에서 문구를 바꾸지 않는다(모달 파일 수정 없음, 계획 그대로).
- dev-agent 구현(2026-09-19) — 계획대로 shop-web 프론트만 작업. 백엔드(INF-MBR-008/009, `MemberAddressController`/`ZipcodeController` 등)는 건드리지 않았다.
  - 신규 파일
    - `modules/shop-web/src/pages/MyAddressesPage.tsx` — `/shop/mypage/addresses` 컨테이너. 세션 판정→리다이렉트, 목록 조회, 등록/수정 폼 오케스트레이션, 삭제확인, 기본설정.
    - `modules/shop-web/src/pages/MyAddressesPage.test.tsx` — HTTP 레벨 통합 테스트(비로그인 리다이렉트, X-Api-Key 헤더 직접 단언, 0/1/10건 렌더, 등록 검증실패·정상·409, 수정 prefill·반영, 삭제 확인게이트·기본배송지 삭제 시 재조회, 기본설정 전환, 404 동일문구, 연타 방지).
    - `modules/shop-web/src/features/member/AddressList.tsx` + `.stories.tsx`(목록_0건·목록_1건·목록_10건상한·기본배송지전환중)
    - `modules/shop-web/src/features/member/AddressForm.tsx` + `.stories.tsx`(신규·수정·검증오류)
    - `modules/shop-web/src/features/member/AddressDeleteConfirm.tsx` + `.stories.tsx`(일반삭제확인·기본배송지삭제확인)
  - 수정 파일
    - `modules/shop-web/src/api.ts` — `fetchAddresses`/`registerAddress`/`updateAddress`/`deleteAddress`/`setDefaultAddress` 5개 추가(전부 `apiKey`를 `X-Api-Key`로 명시 헤더, `ApiError` 계열, `memberId` 쿼리 미전달 — `SCOPE_TO_SELF` 강제 위임). 공용 `getAuth`/`sendAuth` 헬퍼 신설.
    - `modules/shop-web/src/types.ts` — `MemberAddress`/`MemberAddressInput` 추가(INF-MBR-008 응답 셰이프 그대로, `isDefault`는 `'Y'|'N'` 문자열 유지).
    - `modules/shop-web/src/App.tsx` — `/shop/mypage/addresses` 라우트 1개 추가.
    - `modules/shop-web/src/features/shop/Gnb.tsx` — 로그인 상태 블록에 "마이페이지 › 배송지 관리" 링크 1줄 추가(비로그인 블록 무변경).
  - 재사용(수정 없음): `src/features/shop/ZipcodeSearchModal.tsx`(+기존 스토리), `src/features/shop/deliveryAddressValidation.ts`(`validateDeliveryAddress`/`DeliveryAddressErrors`) — `AddressForm`이 `recipient` 필드를 `recipientName` 키로 매핑해 그대로 소비.
  - 결정(계획 밖 보강, 계획과 상충 없음): ① GNB 장바구니 배지가 `cartItemCount` prop을 요구해 `ShopHomePage`와 동일하게 `fetchCartItemCount`(기존 API, 신규 아님)를 재사용해 채웠다. ② 등록 시 체크박스로 기본을 선택하거나 첫 배송지라 서버가 기본을 강제한 경우, 응답의 `isDefault==='Y'`를 보고 로컬의 이전 기본 행도 `'N'`으로 내려 "기본 배지 두 개"가 잠깐이라도 보이지 않게 했다(기본설정 핸들러와 동일 원리, 재조회 없이).
  - 검증: `npm test`(tsc 전체 + jest 197 tests) 전원 통과. `npx storybook build`로 신규 스토리 3개 포함 전체 빌드 성공 확인.
- dev-agent 재작업(2026-09-19, round1 QA FAIL carry-back — 필수 3건 전부 반영 + 권고 처리) — `modules/shop-web`만 수정. `modules/shop-api`의 `V8__zipcodes.sql`·`zipcode.xml`·`ZipcodeDaoTest.java`(형제 항목 SR-235.3 산출물)는 건드리지 않았다(shop-web은 별도 git 저장소라 애초에 같은 커밋 스코프에 섞이지 않는다).
  - 필수 수정
    1. `modules/shop-web/src/pages/MyAddressesPage.tsx`의 우편번호 검색 실패 처리(`handleZipcodeSearch`)가 공용 필터 `toDisplayMessage`를 우회하던 것을 고쳤다 — 해당 로직을 `modules/shop-web/src/pages/useZipcodeSearch.ts`(신규 훅, 아래 권고5와 함께 추출)로 옮기며 `setError(toDisplayMessage(e))`로 교체. 500 응답 mock에서 원문이 안 보이는 것을 `modules/shop-web/src/pages/MyAddressesPage.test.tsx`에 테스트로 고정(`우편번호 검색 — 500 응답의 서버 원문 메시지는 노출하지 않고 일반 문구를 보여준다`).
    2. 자체 정의했던 `toAddressErrorMessage`(`MyAddressesPage.tsx`)를 삭제했다. 새 필터를 또 만들지 않고 `modules/shop-web/src/features/shop/httpErrorMessage.ts`의 `toDisplayMessage`를 `ApiError`(`{code,message}` 계열)도 받도록 넓혀(`OrderHttpError`와 동일한 `ALLOWED_MESSAGE_STATUSES`+`looksLikeInternalDetail` 판정 공유) 배송지 CRUD 쪽도 그 함수 하나만 쓰게 했다. 단위 테스트 추가: `modules/shop-web/src/features/shop/httpErrorMessage.unit.test.ts`에 `ApiError` 케이스(409 원문 유지·500 대체·400이라도 내부상세면 대체) 3종.
    3. `MyAddressesPage.tsx`의 등록 성공 처리를 `[...rows, saved]`(맨 뒤)에서 `[saved, ...rows]`(맨 앞)로 고쳤다(서버 `selectList`의 `last_used_at DESC` 정렬과 일치). 기존 행이 있는 상태에서 등록해 새 행이 목록 1행에 오는지 확인하는 위치 단언 테스트 추가(`MyAddressesPage.test.tsx`: `등록: 정상 입력 — 기존 행이 있어도 새 배송지가 목록 맨 앞에 온다`).
  - 권고 처리
    - MBR-4202(검색어 2자 미만) 400 경로 테스트 추가(`우편번호 검색 — 400 MBR-4202… 서버 문구를 그대로 인라인 표시한다`) — 필수1·2 수정 후에도 400은 허용목록 안이라 원문이 유지되는지 고정.
    - 기존 "404 MBR-4041 — 미존재/남의 배송지 두 시나리오 모두…" 테스트는 실제로 삭제(DELETE) 한 경로만 발사하고 있어 이름을 그 범위로 좁히고(`… 삭제 대상이 없거나 남의 배송지면…`), 수정(PUT) 404 경로를 검증하는 테스트를 별도로 추가했다.
    - 삭제 연타 방지 테스트 추가(기존설정만 있었다) — `act()` 한 스코프에서 `[삭제]` 확인 버튼 2연타 → DELETE 1회만.
    - 우편번호 모달 상태(검색어·결과·오류)를 `MyAddressesPage.tsx`에서 신규 훅 `modules/shop-web/src/pages/useZipcodeSearch.ts`로 추출했다 — 모달을 닫을 때(`close()`) 네 상태를 전부 초기화해, 다음 배송지 등록에서 모달을 다시 열었을 때 이전 검색 결과·오류가 먼저 보이는 문제(권고6)도 함께 없앴다. 이 추출로 `MyAddressesPage.tsx`가 302줄 → 286줄로 내려가 `file-size-cap`(should, 300줄) 위반을 해소했다.
    - 검색 0건 문구(권고1)는 사람 코멘트가 "AC를 실제 모달 문구로 정정했으니 모달은 고치지 않는다"로 확정해 코드 변경 없음.
  - 수정 파일: `modules/shop-web/src/pages/MyAddressesPage.tsx`, `modules/shop-web/src/pages/MyAddressesPage.test.tsx`, `modules/shop-web/src/features/shop/httpErrorMessage.ts`, `modules/shop-web/src/features/shop/httpErrorMessage.unit.test.ts`.
  - 신규 파일: `modules/shop-web/src/pages/useZipcodeSearch.ts`(우편번호 검색 상태·요청 훅, 컴포넌트가 아니라 `story-per-component` 대상 아님).
  - 검증: `npm test`(tsc 전체 + jest, 203/203 tests, 18 suites) 전원 통과. `npx storybook build`로 전체 빌드 성공 확인(대상 컴포넌트 무변경 — 기존 스토리 그대로).

- test-agent 최종 검증 (2026-09-19, TC 앵커 주석 추가)
  - TC 앵커 주석 추가: `MyAddressesPage.test.tsx` 19개 테스트 함수 각각 위에 `// linked_tc: TC-FUNC-member-010-00X` 주석 추가(001~019).
  - AC↔TC 매핑 완성: SR-235.1 AC 14항이 TC-FUNC-member-010-001~019로 전부 1:1 또는 다대1로 커버됨.
    - AC-1(새 라우트 진입): TC-001, 002
    - AC-2(GNB 링크): Gnb.stories.tsx 기존 "로그인" 상태 재사용
    - AC-3(비로그인 리다이렉트): TC-001
    - AC-4(목록 정렬/배지): TC-003, 004, 005, 008
    - AC-5(0건 안내): TC-003
    - AC-6(10건 상한): TC-005
    - AC-7(폼 검증): TC-006, 010
    - AC-8(우편번호 모달): TC-007, 016, 017 + ZipcodeSearchModal.stories.tsx 기존 2가지 재사용
    - AC-9(기본 설정): TC-013
    - AC-10(기본 삭제 승계): TC-012
    - AC-11(삭제 확인): TC-011, 019
    - AC-12(API 오류): TC-009, 014, 015, 016
    - AC-13(X-Api-Key 헤더): TC-002
    - AC-14(상태별 스토리): AddressList 4가지 + AddressForm 3가지 + AddressDeleteConfirm 2가지 + ZipcodeSearchModal 기존 2가지
  - 색인 재구축: `scan_tc_anchors.py` 실행 → 304건 색인(조직 단위 기존 중복 제외, shop-web 신규 19건 포함).
  - 최종 검증: `npm test` 재실행 → **203/203 tests 통과** (tsc 전체, jest 18 suites).
  - 부품 스토리: AddressList.stories.tsx (4가지) · AddressForm.stories.tsx (3가지) · AddressDeleteConfirm.stories.tsx (2가지) — 정적 props, story-per-component 규칙 준수, 스토리 신규 추가·수정 없음.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-19 — FAIL
- Layer1 스펙: **concerns** — AC 14항 중 12항은 실측 확인(라우트 1개·GNB 로그인 링크·비로그인 리다이렉트+복귀[`resolveRedirectTarget('/shop/mypage/addresses')`가 화이트리스트를 통과하는지 `redirectTarget.ts:22`까지 직접 확인]·0건/10건 안내·폼 6필드+인라인 검증·기본설정·기본삭제 승계 재조회·삭제 확인 게이트·오류 코드 인라인·`api.ts` 단일 창구(부품 fetch 0건)·상태별 스토리 4+3+2종+ZipcodeSearchModal 기존 2종 재사용). 어긋난 2항: ① **등록 후 목록 정렬** — 서버 `selectList`는 `ORDER BY last_used_at DESC, created_at DESC`이고 신규 등록분은 `last_used_at=created_at=now`(INF-MBR-008 "응답" 절)라 맨 **앞**이 정본인데, `MyAddressesPage.tsx:161`이 `[...rows, saved]`로 맨 **뒤**에 붙인다 → 같은 데이터가 등록 직후와 새로고침 후 서로 다른 순서로 보인다(AC "목록: 최근 사용 순 정렬"). ② 검색 0건 문구가 확정 요건 문답·AC가 못 박은 `'검색 결과가 없습니다 — 도로명·건물명으로 다시 검색'`이 아니라 재사용 부품의 `'검색 결과가 없습니다'`다(사람이 "파일 수정 없음"으로 승인한 재사용의 부수 결과 — 사람 결정 필요).
- Layer2 보안: **fail** — `MyAddressesPage.tsx:233`의 우편번호 검색 실패 처리가 공용 필터 `toDisplayMessage`를 **우회**하고 `e.message`를 그대로 렌더한다. `searchZipcodes`는 상태코드 불문 `OrderHttpError(status, 서버 {message})`를 던지고 `application.yml:5`가 `server.error.include-message: always`이므로, `/api/zipcodes` 5xx(`ZipcodeExceptionHandler`가 처리하지 않는 예외 → Spring 기본 오류 봉투)의 **원문 예외 메시지가 모달에 그대로 노출**된다. 이 필터는 SR-305 round2 **QA FAIL 필수3**의 산출물이고 `httpErrorMessage.ts` 주석이 "표시 직전에 이 필터를 반드시 거친다 — 공용 헬퍼로 올려 5곳이 전부 같은 함수 하나만 쓴다(중복 정의 금지)"라고 못 박았다. 같은 API(`GET /api/zipcodes`)의 기존 소비처 `OrderPage.tsx:98`은 `toDisplayMessage(e)`를 쓴다 — 새 화면만 규율을 벗어났다(기존 유닛테스트 `httpErrorMessage.unit.test.ts:28`이 "500은 일반 문구"를 이미 고정해 둔 계약). 배송지 5개 함수 쪽 `toAddressErrorMessage`(같은 파일 :32)도 `ApiError`면 상태코드 무관하게 서버 문구를 그대로 쓰므로 계약표(400/404/409) 밖 상태에서 동일한 노출 경로가 열린다. 인증 쪽은 깨끗하다 — 5개 함수 전부 `X-Api-Key: session.apiKey` 명시, `memberId` 쿼리 미전달(`SCOPE_TO_SELF` 위임), 404 미존재/타인 소유를 프론트가 가르지 않음(존재 오라클 없음), 오픈 리다이렉트 없음(고정 리터럴), 콘솔 출력 0건.
- Layer3 회귀: **concerns** — 독립 재실행으로 `npm test`(tsc 전체 + jest **197/197, 18 suites**) 통과 확인. 백엔드 무변경 확인(shop-api 워킹트리 변경 3건 `V8__zipcodes.sql`·`zipcode.xml`·`ZipcodeDaoTest.java`는 mtime 13:55 = 형제 항목 FUNC-member-012 round2 잔여분, 이 항목 작업 시각 14:41~14:54와 분리 — 커밋 스코프 분리 필요). 공유 부품 `ZipcodeSearchModal`·`deliveryAddressValidation`·`DeliveryAddressForm` 무변경. `Gnb.tsx` 링크 1줄 추가는 `story_shots` baseline(46종)에 GNB 스토리가 없어 화면 기준선 충돌 없음. **회귀 성격의 지적은 Layer2의 공용 필터 우회 1건** — 새 화면이라 기존 화면 동작은 그대로지만, 이미 고친 결함 클래스를 새 화면에서 되살렸다. 연타 방지 가드는 **가드를 임시로 제거해 테스트가 실제로 깨지는지 직접 재현**(사례집 SR-302 #1 규율) → 실패 확인 후 원복, 재실행 14/14 통과 = 거짓 보증 아님.
- 필수 수정(FAIL시):
  1. `MyAddressesPage.tsx:233` — `setZipcodeError(e instanceof Error ? e.message : …)`를 `setZipcodeError(toDisplayMessage(e))`로 교체하고 `toDisplayMessage`를 import한다(`GENERIC_ERROR_MESSAGE` 직접 import는 불필요해지면 정리). 5xx 원문이 화면에 안 나오는 것을 테스트로 고정한다(`zipcodes: () => jsonResponse(500, {message:'Cannot invoke "…" because "x" is null'}, false)` → 모달에 일반 문구만).
  2. 같은 파일 `:31-33` `toAddressErrorMessage` — `ApiError` 여부만 보지 말고 **계약표 상태코드 허용목록**을 통과시킨다(`ALLOWED_MESSAGE_STATUSES`(400/404/409) + `looksLikeInternalDetail` 재사용, 새 필터를 또 정의하지 말고 `httpErrorMessage.ts`의 것을 쓴다 — `ApiError`도 받도록 헬퍼를 넓히거나 `status` 기반 공용 판정을 추출). 400 `MBR-4200`·404 `MBR-4041`·409 `MBR-4201`은 지금처럼 원문 그대로 유지되어야 한다(기존 테스트 3건이 그 계약을 잡고 있다).
  3. `MyAddressesPage.tsx:161` — 등록 성공 시 `[...rows, saved]` → `[saved, ...rows]`(서버 `last_used_at DESC` 정렬과 일치). 기존 "등록: 정상 입력" 테스트에 **위치 단언**을 더한다(목록 첫 행이 방금 등록한 수령인).
- 권고(CONCERNS시):
  1. (사람 결정 필요) 검색 0건 문구 — AC/확정 문답의 `'검색 결과가 없습니다 — 도로명·건물명으로 다시 검색'`을 살리려면 공유 `ZipcodeSearchModal`을 고쳐야 하고 그러면 SR-305 주문서 화면 문구도 함께 바뀐다. 사람이 (a) 공유 부품 문구 보강(주문서 동반 변경 수용) 또는 (b) AC 문구 정정 중 하나를 택해 달라. 구현은 승인된 계획("파일 수정 없음")대로다.
  2. AC의 `MBR-4202`(검색어 2자 미만) 경로에 테스트가 **없다** — 이 경로만 `ApiError`가 아닌 `OrderHttpError` 계열을 타므로 필수 수정 1·2를 적용한 뒤 400 허용목록에 걸려 서버 문구가 그대로 뜨는지 반드시 테스트로 고정한다(고치면서 조용히 일반 문구로 바뀌기 쉬운 자리다).
  3. `'404 MBR-4041 — 미존재/남의 배송지 두 시나리오 모두…'` 테스트(`MyAddressesPage.test.tsx:327`)는 이름이 두 시나리오를 주장하지만 실제로는 삭제 404 한 경로만 발사한다 — 수정(PUT) 404를 추가하거나 이름을 실제 범위로 줄인다(사례집 SR-306 #2 "거짓 보증" 클래스, 이름이 보증을 과장).
  4. 삭제 연타 방지 테스트 없음(기본설정만 있고 둘이 같은 `inFlightAddressIdRef`를 공유하긴 한다) — 다이얼로그 [삭제] 2연타 `act()` 묶음 1건 추가.
  5. `file-size-cap`(should) — `MyAddressesPage.tsx` **302줄 > 300**. 계획이 "300줄 근접 시 훅/하위 함수로 쪼갠다"고 미리 정해 뒀고, 프로덕션 페이지 중 첫 초과다(다음이 `OrderPage.tsx` 229줄). 우편번호 모달 상태 4개(`zipcodeQuery/Results/Loading/Error`)를 `useZipcodeSearch` 훅으로 묶으면 자연스럽게 내려간다. `MyAddressesPage.test.tsx` 361줄은 기존 관례 범위(`CartPage.test.tsx` 456줄).
  6. 모달을 닫아도 `zipcodeQuery`/`zipcodeResults`/`zipcodeError`가 남아, 두 번째 배송지 등록에서 모달을 열면 이전 검색 결과·오류가 먼저 보인다(`OrderPage`도 동일한 기존 관례라 이번 라운드 차단 사유는 아님 — 후속 TODO, 위 권고 5의 훅 추출과 같은 자리).
- 재동기화 입력(STEP 5.5): UIS-MBR-004 본문 미존재(예약분, `/sl-recon-uis` 역생성 예정) — 생성 시 **예약 ID `UIS-MBR-004` 그대로** 쓴다(사례집 SR-302 줄35·SR-304 #1 — `--fix`가 "다음 빈 번호"로 만드는 함정 2회 재발 이력). INF-MBR-008/009 본문은 이 항목이 건드리지 않았고 늦은 서술도 없다.

### QA Gate — 2026-09-19 — round 2 — PASS
- Layer1 스펙: **pass** — round1 필수 3건을 **전부 실측 확인**했다. ① 등록 후 정렬: `MyAddressesPage.tsx:155`이 `[saved, ...rows]`(prepend)로 고쳐졌고, 정본 근거를 백엔드에서 재확인했다 — `memberAddress.xml` `selectList`는 `ORDER BY last_used_at DESC, created_at DESC`(줄15)이고 `MemberAddressService:108,123-125`가 등록 시 `lastUsedAt=createdAt=updatedAt=now(clock)`을 넣으므로 신규분이 맨 앞이 정본이다. 위치 단언 테스트(`MyAddressesPage.test.tsx:230` — `rows[0]`이 방금 등록한 수령인, `rows[1]`이 기존 행)가 새로 붙었다. **같은 결함 클래스를 한 번에 닫았는지도 확인** — `updateAddress`·`setDefault`·`clearDefaultForMember` 매퍼는 `last_used_at`을 **건드리지 않으므로**(줄49-79) 수정·기본전환의 로컬 in-place 갱신은 서버 정렬과 어긋나지 않는다(형제 결함 없음). ② 검색 0건 문구: 사람이 AC를 공유 모달 실제 문구(`검색 결과가 없습니다`, `ZipcodeSearchModal.tsx:71`)로 정정했고 모달 파일은 무변경(git status 확인) — 이제 AC와 구현이 일치한다. 나머지 AC 12항은 round1에서 실측 확인한 그대로 유지(변경 없음). must 규칙 위반 0건(`rules_check.py` — must 0).
- Layer2 보안: **pass** — round1 차단 사유였던 **원문 예외 메시지 노출 우회가 완전히 사라졌다**. ① 우편번호 검색 실패는 `useZipcodeSearch.ts:36`이 `setError(toDisplayMessage(e))`로 공용 필터를 거친다. ② 자체 필터 `toAddressErrorMessage`는 **정의 자체가 삭제**됐고(전 소스 grep 0건), 배송지 CRUD 4개 표시 지점(`MyAddressesPage.tsx:80` 로드·`:161` 저장·`:191` 삭제·`:215` 기본설정)이 전부 같은 `toDisplayMessage` 하나만 쓴다 — 새 화면 파일 전체에 `e.message` 직접 렌더가 0건이다. ③ 필터는 새로 만들지 않고 `httpErrorMessage.ts`의 함수를 `ApiError`도 받도록 넓혀(`:46-56`) `ALLOWED_MESSAGE_STATUSES`(400/404/409)+`looksLikeInternalDetail`을 **공유**한다(중복 정의 금지 규율 준수). 허용목록이 스펙과 일치하는지 대조 — INF-MBR-008 오류표(줄217-220): 400 `MBR-4200`·404 `MBR-4041`·409 `MBR-4201`만 원문 노출, 500 `MBR-5000`은 일반 문구로 대체가 정본이며 구현이 정확히 그렇다. ④ **거짓 보증이 아닌지 가드 제거로 재현**(사례집 SR-302 #1 규율): `useZipcodeSearch.ts`의 `toDisplayMessage(e)`를 `e.message`로 일시 되돌리자 `우편번호 검색 — 500 응답의 서버 원문 메시지는 노출하지 않고 일반 문구를 보여준다` 테스트가 **실제로 실패**(1 failed/19)했고, 원복 후 27/27 통과 — 테스트가 회귀를 실제로 잡는다. ⑤ 인증은 round1 그대로 깨끗 — 5개 함수 전부 `X-Api-Key: session.apiKey` 명시(`api.ts:295-340` `getAuth`/`sendAuth`/`deleteAddress`), `memberId` 쿼리 미전달(`SCOPE_TO_SELF` 위임), 404 미존재/타인소유를 프론트가 가르지 않음(존재 오라클 없음, PUT·DELETE 두 경로 각각 테스트), 리다이렉트 경로는 고정 리터럴, 콘솔 출력·직접 `fetch` 0건.
- Layer3 회귀: **pass** — 독립 재실행으로 `npm test`(tsc 전체 + jest **203/203, 18 suites**) 통과 재현(round1 197 → +6건: 등록 위치, 400 MBR-4202, 500 비노출, 수정 404, 삭제 연타, `ApiError` 유닛). 공용 필터 확장의 파급을 직접 추적 — `toDisplayMessage`의 기존 소비처(`CartPage` 3곳·`OrderPage` 2곳·`OrderFailureNotice`)가 받는 예외는 `api.ts`에서 전부 `OrderHttpError` 계열이고(장바구니·주문·상품·우편번호 함수 모두), `ApiError`를 던지는 함수는 로그인/재설정/배송지 계열뿐이라 **기존 화면의 판정값은 바뀌지 않는다**(허용목록·거부목록도 불변, 유닛테스트 기존 5건 그대로 통과). 공유 부품 `ZipcodeSearchModal`·`deliveryAddressValidation`·`DeliveryAddressForm` 무변경(git status). 스코프도 깨끗 — shop-web 6 수정 + 9 신규만이고 `modules/shop-api` 워킹트리는 **변경 0건**(round1이 지적한 `V8__zipcodes.sql`·`zipcode.xml`·`ZipcodeDaoTest.java`는 형제 항목 SR-235.3 종결분으로 이미 커밋돼 커밋 스코프 혼입 위험이 해소됐다). `rules_check.py` must 0 · should 1(아래 후속 TODO).
- 권고(CONCERNS시): 없음 — round1 권고 6건은 전부 처리 확인(①0건 문구=사람 AC 정정으로 종결 ②MBR-4202 400 테스트 추가, 목 픽스처가 실물 계약과 일치하는지 `ZipcodeExceptionHandler`·`ZipcodeController`까지 대조해 확인 ③404 테스트를 삭제 경로로 이름 축소 + 수정(PUT) 404 테스트 신설 ④삭제 연타 `act()` 묶음 테스트 추가 ⑤`MyAddressesPage.tsx` 302→**286줄**로 `file-size-cap` 해소 ⑥모달 `close()`가 query/results/error 초기화).
- 후속 TODO(low, 이번 라운드 차단·재작업 사유 아님):
  1. `useZipcodeSearch.close()`가 `loading`은 초기화하지 않고 진행 중 요청도 취소·무효화하지 않는다 — 검색 요청이 떠 있는 동안 모달을 닫으면 응답이 뒤늦게 `setResults`/`setError`를 실행해, 다시 열었을 때 이전 검색의 결과·오류가 보일 수 있다(권고6이 고친 문제의 잔여 레이스). 요청 id 가드(페이지 `requestIdRef`와 동일 원리) 1줄로 닫힌다. 화면 표시만 영향(데이터·보안 무관).
  2. 같은 우편번호 검색 상태 로직이 `OrderPage.tsx:93-100`에 인라인으로 남아 있다(이번 훅과 동일 형태) — SR-305 회귀 위험 때문에 이번에 건드리지 않은 것이 맞다. 다음에 주문서를 만질 때 `useZipcodeSearch`로 합친다.
  3. `file-size-cap`(should) — `MyAddressesPage.test.tsx` 469줄 > 300. 테스트 파일이고 기존 관례 범위(`CartPage.test.tsx` 456줄)라 프로덕션 상한 취지 밖이다. 이 규칙을 테스트 파일에 적용할지는 하네스 차원 결정 사항.
- 재동기화 입력(STEP 5.5):
  1. UIS-MBR-004 본문 미존재(예약분, `/sl-recon-uis` 역생성 예정) — 생성 시 **예약 ID `UIS-MBR-004` 그대로** 쓴다(사례집 SR-302 줄35·SR-304 #1, `--fix`의 "다음 빈 번호" 함정 2회 재발 이력).
  2. **`INF-MBR-009`라는 문서는 존재하지 않는다**(round1 기록 정정). 우편번호 검색 API(`GET /api/zipcodes`)의 실제 스펙 문서는 `docs/05_설계서/order/INF/INF-ORD-016.md`이고, 그마저 "코드 기준 재동기화 골격 — 요청·응답·오류 계약은 보강 대상" 상태로 `MBR-4202`(검색어 2자 미만 400)·`MBR-5000` 계약이 본문에 없다. STORY 계획·`ZipcodeController` javadoc·`ZipcodeExceptionHandler`가 모두 `INF-MBR-009`를 참조한다 → ID를 실제 문서(`INF-ORD-016`, member 도메인으로 옮길지 포함)로 정리하고 오류 계약 본문을 보강한다. 구현은 실물 계약(400 `{code,message}`)과 일치하므로 Layer1 불일치가 아니라 **스펙 본문이 늦은 것**이다.
  3. INF-MBR-008 본문은 이 항목이 건드리지 않았고 늦은 서술도 없다(오류표·정렬·스코프 전부 구현과 일치).

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [high/security] MyAddressesPage.tsx:233 우편번호 검색 실패 처리가 공용 필터 toDisplayMessage를 우회하고 e.message를 그대로 렌더 — searchZipcodes는 상태코드 불문 OrderHttpError(서버 {message})를 던지고 application.yml:5가 include-message: always이므로 /api/zipcodes 5xx의 원문 예외 메시지가 모달에 노출된다. 같은 API의 기존 소비처 OrderPage.tsx:98은 toDisplayMessage를 쓰고, 이 필터는 SR-305 round2 QA FAIL 필수3의 산출물(httpErrorMessage.ts 주석: 표시 직전에 반드시 거친다, 중복 정의 금지)이다 → setZipcodeError(toDisplayMessage(e))로 교체하고, 500 응답 mock에서 모달에 일반 문구만 나오는 것을 테스트로 고정
2. [medium/security] MyAddressesPage.tsx:31-33 toAddressErrorMessage가 ApiError 여부만 보고 상태코드를 안 봐서, 계약표(400/404/409) 밖 상태(5xx·Spring 기본 오류 봉투)에서도 서버 문구를 그대로 인라인 노출한다 → httpErrorMessage.ts의 ALLOWED_MESSAGE_STATUSES + looksLikeInternalDetail을 재사용해 허용목록 통과분만 원문 표시(새 필터 중복 정의 금지). 400 MBR-4200 / 404 MBR-4041 / 409 MBR-4201 원문 유지는 기존 테스트 3건으로 확인
3. [medium/spec] MyAddressesPage.tsx:161 등록 성공 시 [...rows, saved]로 목록 맨 뒤에 붙인다 — 서버 selectList는 ORDER BY last_used_at DESC, created_at DESC이고 신규 등록분은 last_used_at=created_at=now라 맨 앞이 정본. 등록 직후와 새로고침 후 순서가 달라 AC '목록: 최근 사용 순 정렬'과 어긋난다 → [saved, ...rows]로 prepend(또는 등록 후 재조회) + 등록 테스트에 첫 행 위치 단언 추가
4. [medium/spec] 검색 0건 문구가 AC·확정 요건 문답의 '검색 결과가 없습니다 — 도로명·건물명으로 다시 검색'이 아니라 재사용 부품의 '검색 결과가 없습니다'다. 구현은 사람이 '파일 수정 없음'으로 승인한 재사용 계획대로이므로 사람 결정이 필요(공유 ZipcodeSearchModal 문구 보강 = SR-305 주문서 동반 변경 vs AC 문구 정정) → 사람이 (a) 공유 부품 문구 보강 또는 (b) AC 문구 정정 중 택일
5. [medium/spec] AC의 MBR-4202(검색어 2자 미만) 경로에 테스트가 없다 — 이 경로만 ApiError가 아닌 OrderHttpError 계열을 타므로, 위 필터 수정 과정에서 400 서버 문구가 조용히 일반 문구로 바뀔 수 있는 자리다 → 400 MBR-4202 mock으로 모달 인라인 문구가 서버 문구 그대로인지 고정하는 테스트 1건 추가
6. [low/regression] MyAddressesPage.test.tsx:327 '404 — 미존재/남의 배송지 두 시나리오 모두' 테스트가 이름과 달리 삭제 404 한 경로만 발사한다(수정 PUT 404 미검증). 삭제 연타 방지 테스트도 없다(기본설정만) → 수정 404 시나리오 추가 또는 테스트 이름을 실제 범위로 축소 + 다이얼로그 [삭제] 2연타 act() 묶음 테스트 1건
7. [low/regression] file-size-cap(should) 위반 — MyAddressesPage.tsx 302줄 > 300(프로덕션 페이지 중 첫 초과, 다음은 OrderPage 229줄). 계획이 '300줄 근접 시 분리'를 미리 정해 뒀다. 부수적으로 모달 닫힘 후 zipcodeQuery/Results/Error가 잔존해 재등록 시 이전 검색 결과·오류가 먼저 보인다(OrderPage도 동일 관례) → 우편번호 모달 상태 4개를 useZipcodeSearch 훅으로 추출 — 줄 수와 상태 잔존을 한 번에 정리

사람 코멘트: 필수 수정 3건: (1) MyAddressesPage.tsx:233 오류 표시를 공용 toDisplayMessage(e)로 교체 + 500 mock 테스트로 원문 비노출 고정(보안 차단 사유 — searchZipcodes의 OrderHttpError 원문 메시지가 include-message:always로 유출됨) (2) 자체 정의한 toAddressErrorMessage(:31-33) 삭제, 기존 ALLOWED_MESSAGE_STATUSES(400/404/409)·looksLikeInternalDetail 재사용(새 필터 중복 정의 금지) (3) 등록 후 목록 삽입을 [...rows, saved]에서 [saved, ...rows]로(서버 정렬 last_used_at DESC, 신규 등록분이 맨 앞이 정본) + 첫 행 위치 단언 테스트 추가. 같은 라운드에 권고도 함께 처리: MBR-4202(검색어 2자 미만) 경로 테스트 추가, 404 테스트를 미존재/타인소유 두 요청으로 각각 발사해 구분, 삭제 연타 방지 테스트 추가, 모달 닫을 때 상태 초기화, file-size-cap(should, 302줄) 가능하면 부품 분리로 해소(안되면 사유만 기록). shop-api 워킹트리의 V8__zipcodes.sql·zipcode.xml·ZipcodeDaoTest.java 변경은 형제 항목 SR-235.3(Done)의 산출물이니 건드리지 말고 커밋 스코프에서 제외만 한다. 검색 0건 문구는 AC를 실제 모달 문구('검색 결과가 없습니다')로 정정했으니 모달은 고치지 않는다.
