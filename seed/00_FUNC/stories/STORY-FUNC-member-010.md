---
story-id: STORY-FUNC-member-010
func-id: FUNC-member-010
status: Approved
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
- [ ] 기능이 설명대로 동작(연결 INF 없음 — 수용기준 수동 작성 권장)
- [ ] SR 정본 계약 충족 — `docs/변경관리/SR-235/02_변경명세.md` · inputs/_decisions.md의 D-결정 의 요구·계약 조항을 AC로 구체화해 사람 승인 전 보강할 것(OBS-020 관례)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF**: (연결 없음)
- **SCH**: (연결 없음)
- **UIS** UIS-MBR-004
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
(dev-agent가 생성 파일·주요 결정 기록)

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)
