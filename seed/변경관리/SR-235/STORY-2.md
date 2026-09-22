---
item: SR-235.2
title: 배송지 CRUD API
legacy_func: FUNC-member-011
story-id: STORY-FUNC-member-011
func-id: FUNC-member-011
status: Done
domain: member
created: 2026-09-13
spec_markers: 0
sr-id: SR-235
approved_sha: 20f26ef1f2ac
---

# STORY-FUNC-member-011 — SR-235 — 배송지 CRUD API · 신규 INF-MBR-008 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

## Story
SR-235 — 배송지 CRUD API · 신규 INF-MBR-008 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)


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

### 구현 모듈(제약) — `shop-api` (`{{SRC_SHOP_API}}`)
이 FUNC의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약 폼에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
- [x] INF-MBR-008: 요청/응답 계약 충족(5개 엔드포인트 · 상태코드 201/200/204/200/200 · 오류코드
  MBR-4200/4041/4201/5000 · 봉투 `{items:[]}`/단건 raw · 사람 확정 검증 규칙 6종 — QA Layer1 pass)
- [x] SR 정본 계약 충족 — `docs/변경관리/SR-235/02_변경명세.md` · `_decisions.md` D-결정의 요구·계약
  조항을 AC로 구체화(락 순서 단일화 · 존재+소유 단일 SQL · 개수 제한 · 승계 · 기본 강제 ·
  `del_yn` 필터 · IDOR 강제축소 — round 2 재작업까지 반영해 전부 실측 확인)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-008
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)



## 구현 계획

- **파일**:
  - 신규
    - `modules/shop-api/src/main/resources/db/V7__member_addresses.sql` — `MEMBER_ADDRESSES` 테이블 DDL(신규, `CREATE TABLE IF NOT EXISTS`, DROP 없음). **주의**: SR 변경명세 `02_변경명세.md`는 "Flyway V7__member_addresses.sql"이라고 적었지만 이 프로젝트에는 마이그레이션 엔진이 없다(`CLAUDE.md`) — `V7__` 접두는 기존 `db/*.sql` 파일들(V3~V6)의 **네이밍 관례**일 뿐이고 실제 실행은 `spring.sql.init.mode=always`가 기동마다 `db/*.sql` 전체를 재실행한다. 그래서 멱등(`IF NOT EXISTS`)이 필수다(규칙 `ddl-idempotent`).
    - `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberAddress.java` — 도메인 클래스(매퍼 `resultType`용, MemberDao 관례와 동일하게 resultMap 없이 필드명 매칭).
    - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberAddressDao.java` — MyBatis `@Mapper` 인터페이스. 락/조회/변경 메서드는 아래 "데이터" 절 참고.
    - `modules/shop-api/src/main/resources/mapper/memberAddress.xml` — 매퍼 XML(네임스페이스 `com.sm.lab.shop.dao.MemberAddressDao`, 컬럼 명시 — `no-select-star`).
    - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressService.java` — 목록/등록/수정/삭제/기본설정 비즈니스 로직 + 트랜잭션 경계(아래 "데이터" 절).
    - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressApiException.java` — `RuntimeException`(HttpStatus + code + message), `MemberSessionApiException`과 동일 패턴.
    - `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberAddressController.java` — `@RestController @RequestMapping("/api/members/me/addresses")`. 요청/응답 DTO는 컨트롤러 내부 `record`(기존 `CartController` 관례).
      - `GET ""` 목록 · `POST ""` 등록 · `PUT "/{addressId}"` 수정 · `DELETE "/{addressId}"` 삭제 · `PUT "/{addressId}/default"` 기본 설정.
    - `modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberAddressExceptionHandler.java` — `@RestControllerAdvice(assignableTypes = MemberAddressController.class)`(컨트롤러 스코프 한정 — project-context.md Critical Rule 2), `MemberAddressApiException`→봉투, `DataAccessException`→500 `MBR-5000`(정제 메시지, 원본은 로그만).
  - 수정(기존 파일, 최소 변경)
    - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — `evaluateMemberScope`에 `/api/members/me/addresses`(및 하위 `/{id}`, `/{id}/default`) 패턴 매칭 분기 **한 개**를 추가해 `Decision.SCOPE_TO_SELF`를 반환한다(아래 "순서·보안" 절에서 왜 이 방식인지 설명). **주의**: 이 파일은 이미 465줄로 `file-size-cap`(should, 450줄)을 넘겨 있다 — 이번 추가로 상수 1개 + 분기 4~5줄만 더해 최소화하고, 파일 분리(리팩터링)는 이 SR 범위 밖이다(별도 SR 후보로만 남긴다).
  - 테스트(신규)
    - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberAddressControllerTest.java`
    - `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberAddressServiceTest.java`
    - `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberAddressDaoTest.java`
    - `modules/shop-api/src/test/java/com/sm/lab/shop/MemberAddressConcurrencyTest.java`(실 DB 대상 통합 테스트 — `CartConcurrencyTest`/`MemberRegistrationConcurrencyTest`와 동일 위치·명명 관례)

- **데이터**:
  - DDL(`V7__member_addresses.sql`, 멱등):
    ```sql
    CREATE TABLE IF NOT EXISTS MEMBER_ADDRESSES (
      address_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
      member_id        VARCHAR(20) NOT NULL,
      recipient        VARCHAR(50) NOT NULL,
      phone            VARCHAR(20) NOT NULL,
      phone_norm       VARCHAR(20) NOT NULL,
      zipcode          VARCHAR(10) NOT NULL,
      road_address     VARCHAR(200) NOT NULL,
      detail_address   VARCHAR(200) NOT NULL,
      entrance_method  VARCHAR(200) NULL,
      delivery_memo    VARCHAR(200) NULL,
      is_default       CHAR(1) NOT NULL DEFAULT 'N',
      last_used_at     DATETIME NULL,
      created_at       DATETIME NOT NULL,
      updated_at       DATETIME NOT NULL,
      del_yn           CHAR(1) NOT NULL DEFAULT 'N',
      CONSTRAINT fk_member_addresses_member FOREIGN KEY (member_id) REFERENCES MEMBERS(member_id)
    ) COMMENT='회원 배송지(SR-235, FUNC-member-011) — 회원당 최대 10개·기본 1개';
    ```
    `ZIPCODES` 테이블은 만들지 않는다(형제 FUNC-member-012 몫) — `zipcode`/`road_address`/`detail_address`는 클라이언트가 보낸 값을 그대로 저장할 뿐, 서버가 `ZIPCODES`와 대조·검증하지 않는다(이 FUNC 범위 밖).
  - `address_id`는 `AUTO_INCREMENT`(접두 포맷 아님 — `ID_SEQUENCES` 원자채번은 `MEMBER_ID` 같은 접두 PK 전용이라 여기서는 불필요, `V3__members_signup.sql` 주석 근거).
  - 조회는 항상 `WHERE member_id=? AND del_yn='N'`(상시필터, 프로젝트 관례).
  - DAO 메서드(고정 락 지점 — 아래 순서·보안 절과 연결):
    - `selectList(memberId)` — `ORDER BY last_used_at DESC, created_at DESC`, `del_yn='N'`. 읽기 전용, 락 없음.
    - `selectOwned(memberId, addressId)` — `WHERE member_id=? AND address_id=? AND del_yn='N'` 단일 조회(존재+소유를 한 쿼리로 합침 — 아래 순서·보안 절).
    - `selectByMemberIdForUpdate(memberId)` — `SELECT address_id, is_default, last_used_at FROM MEMBER_ADDRESSES WHERE member_id=? AND del_yn='N' FOR UPDATE` — **이 FUNC의 모든 쓰기 경로가 공유하는 유일한 락 획득 지점**(SR-231 r3: 락 순서를 한 곳에 고정).
    - `insertAddress(...)`, `updateAddress(...)`, `softDelete(memberId, addressId)`, `clearDefaultForMember(memberId)`, `setDefault(memberId, addressId)`, `selectNextDefaultCandidate(memberId, excludeAddressId)`(`ORDER BY last_used_at DESC, created_at DESC LIMIT 1`, 삭제 대상 제외).
  - 트랜잭션 경계(메서드 단위로 판단 — `MemberSessionService` 관례, 클래스 전체 `@Transactional` 금지):
    - `register`: `@Transactional` 시작 → `selectByMemberIdForUpdate`(락, 첫 문장) → 락으로 얻은 목록 크기 ≥10이면 여기서 예외 던짐(아직 아무것도 안 썼으므로 롤백해도 안전 — SR-231 r2와 무관한 상황) → `isDefault = 기존이 0건이면 무조건 true, 아니면 요청값` → `isDefault && 기존 존재`면 `clearDefaultForMember` → `insertAddress` → 커밋.
    - `update`(필드 수정, `is_default` 제외): `@Transactional` → `selectOwned`(존재+소유 동시 확인) → 없으면 예외 → `updateAddress` → 커밋. 여러 행을 건드리지 않으므로 `selectByMemberIdForUpdate` 락 불필요.
    - `delete`: `@Transactional` → `selectOwned`(먼저, 싸게 실패) → 없으면 예외 → `selectByMemberIdForUpdate`(락, 두 번째 문장 — register/setDefault와 같은 지점) → `softDelete` → 대상이 `is_default='Y'`였으면 `selectNextDefaultCandidate`(락으로 얻은 목록에서 조회, 제외 대상 반영) → 있으면 `setDefault`(승계) → 커밋. 남은 배송지가 0건이면 승계 없음(NPE 방지).
    - `setDefault`: `@Transactional` → `selectOwned` → 없으면 예외 → 이미 기본이면 no-op 커밋(멱등) → `selectByMemberIdForUpdate`(락) → `clearDefaultForMember` → `setDefault` → 커밋.
    - `list`: 트랜잭션 불필요(단순 조회).
  - 락 순서 고정 이유(SR-231 r3 재발 방지): register/delete/setDefault 세 경로 모두 "쓰기 문장 이전에 `selectByMemberIdForUpdate`를 먼저 호출"하는 동일 순서를 지킨다 — 두 커넥션이 다른 순서로 같은 회원의 행을 잠그면 데드락이 난다(SR-231 r3 실제 사고).

- **순서·보안**:
  - 인증은 `ApiKeyAuthFilter`가 컨트롤러 도달 전에 이미 끝낸다(기존 계약 재사용, 신규 인증 로직 추가 없음).
  - **존재 판정과 소유 판정을 분리하지 않는다** — `selectOwned(memberId, addressId)`가 `WHERE member_id=? AND address_id=?`를 **한 SQL**로 묶어 "없음"과 "남의 것"을 원천적으로 같은 결과(0행)로 만든다. "먼저 address_id로 존재를 찾고 그 다음 소유자를 비교"하는 2단계 코드는 만들지 않는다 — 그 두 갈래가 서로 다른 예외/로그/타이밍을 낼 수 있어 존재 오라클이 될 수 있다(SR-231 r5: "존재 판정 → 인증" 순서가 회원 존재 오라클을 만든 사고와 같은 계열의 함정).
  - 404 MBR-4041은 "없음"과 "남의 것"에 **동일 코드·동일 메시지**로 응답한다(요건 확정 문답 그대로).
  - `/api/members/me/addresses` 소유권 판정 방식(필터 변경 설계): 이 경로는 URL에 memberId가 없다("me"). 기존 `ApiKeyAuthFilter`는 이미 `GET /api/orders`(memberId 토큰이 없는 목록 조회)를 `Decision.SCOPE_TO_SELF`로 처리해 `ForcedMemberIdRequest`로 `getParameter("memberId")`를 호출자의 실제 memberId로 강제하는 기존 메커니즘을 갖고 있다 — 이번 FUNC은 **그 기존 메커니즘을 재사용**한다(`/api/members/me/addresses(?:/.*)?` 패턴에 동일하게 `SCOPE_TO_SELF`를 반환). 새 클래스·새 우회 경로를 만들지 않는다. 컨트롤러는 `@RequestParam(required=false) String memberId`로 그 강제된 값을 읽는다.
  - 이 패턴은 `MEMBERS_ITEM_PATH`(`^/api/members/([^/]+)$`, 세그먼트 1개)와 충돌하지 않는다 — `/api/members/me/addresses`는 세그먼트가 3개 이상이라 그 정규식에 애초에 매치되지 않는다(확인 완료).
  - **admin 키로 `/me` 호출 시** — admin 스코프는 `evaluateMemberScope` 자체를 타지 않고(기존 코드 최상단에서 즉시 `chain.doFilter`, 손대지 않음) `memberId` 파라미터가 강제되지 않는다. 이 SR은 "관리자가 특정 회원 대신 `/me`를 호출"하는 시나리오를 정의하지 않으므로, 컨트롤러가 `memberId`가 null이면 400 `MBR-4200`(필수값 누락)으로 명시적으로 막는다(정의되지 않은 동작을 조용히 통과시키지 않음) — 테스트로 고정한다.
  - 요청 검증 순서: 인증(필터) → 필수값 검증(400 `MBR-4200`, DB 접근 전) → 존재+소유(404 `MBR-4041`, `selectOwned`) → 개수 제한(409 `MBR-4201`, register만, 락 이후) → 쓰기.
  - **부수효과(로그·발송·이벤트·감사) 없음** — 이 FUNC은 로그인/알림류 부수효과가 없다(해당 없음).

- **계약**:
  - 오류 코드(신규): `MBR-4200`(400, 필수값 누락) · `MBR-4041`(404, 없는 배송지/남의 배송지 — 동일 코드) · `MBR-4201`(409, 최대 10개 초과) · `MBR-5000`(500, 기존 코드 재사용, 신규 아님).
  - 응답 봉투: 목록(`GET`)은 `{items: [...]}`(프로젝트 공통 관례). 단건 등록/수정/기본설정 응답은 배송지 객체 그대로(봉투 없음, 기존 `Cart`/`Member` 단건 응답과 동일 관례).
  - 상태 코드: 등록 201 · 수정 200 · 삭제 204(본문 없음) · 기본설정 200 · 목록 200.
  - `phone_norm`은 이 FUNC 안에서 숫자만 남기는 로컬 정규화 헬퍼를 `MemberAddressService`에 새로 둔다(기존 `MemberRegistrationService.normalizePhone`은 `private`이라 재사용하지 않는다 — 도메인 간 결합을 늘리지 않는다, 스코프 절제). 이 테이블에는 `phone_norm` 유니크 제약을 걸지 않는다(회원 계정 유일성과 무관, 배송지는 같은 연락처를 여러 개 가질 수 있다).

- **테스트**:
  - `MemberAddressControllerTest`(`@WebMvcTest` + `AdminApiKeyTestConfig` import + `@MockBean(MemberAddressService)`, 모든 요청은 `X-Api-Key: lab-member-0001-key`로 개별 오버라이드해 member 스코프를 태운다 — 기본 admin 헤더로는 `SCOPE_TO_SELF`가 걸리지 않아 이 엔드포인트의 실제 경로를 검증 못 한다):
    1. `GET` 목록 성공 200 + `{items:[...]}`
    2. `POST` 등록 성공 201(첫 배송지 → 응답 `isDefault=true`)
    3. `POST` 필수값 누락(예: recipient 공백) → 400 `MBR-4200`
    4. `POST` 서비스가 `MemberAddressApiException(409, MBR-4201)` 던짐 → 409 `MBR-4201`
    5. `PUT` 수정 성공 200
    6. `PUT` 수정, 서비스가 404 예외 던짐(없음/남의 것 구분 안 함) → 404 `MBR-4041`
    7. `DELETE` 성공 204
    8. `DELETE` 없음/남의 것 → 404 `MBR-4041`
    9. `PUT .../default` 성공 200
    10. `PUT .../default` 없음/남의 것 → 404 `MBR-4041`
    11. admin 키(`lab-admin-key`)로 `GET` 목록 호출, `memberId` 파라미터 미지정 → 400 `MBR-4200`(위 "순서·보안" 절 명시 동작 고정)
    12. 무인증(키 없음) 전역 401은 기존 `ApiKeyAuthIntegrationTest`가 이미 커버 — 이 컨트롤러 전용으로 중복 작성하지 않는다.
  - `MemberAddressServiceTest`(Mockito, DAO 목업):
    1. 등록: 기존 0건이면 요청의 `isDefault` 값과 무관하게 `is_default='Y'`로 강제.
    2. 등록: 락으로 얻은 크기가 10이면 `clearDefaultForMember`/`insertAddress` 호출 없이 409 `MBR-4201` 던짐(아무것도 안 씀 확인).
    3. 등록: 기존 존재 + 요청 `isDefault=true` → `clearDefaultForMember` 호출 후 `insertAddress(isDefault=true)`.
    4. 수정: `selectOwned`가 null → 404 `MBR-4041`, `updateAddress` 호출 안 됨.
    5. 삭제: 기본 배송지 삭제 → `selectNextDefaultCandidate` 조회 후 있으면 `setDefault` 호출.
    6. 삭제: 기본이 아닌 배송지 삭제 → `selectNextDefaultCandidate`/`setDefault` 호출 안 됨.
    7. 삭제: 마지막 남은(승계 후보 없음) → `setDefault` 호출 없이 정상 종료(NPE 없음).
    8. 기본설정: 이미 기본인 대상 → `clearDefaultForMember`/`setDefault` 호출 없이 no-op 성공.
  - `MemberAddressDaoTest`(실 DB, MyBatis 매퍼 SQL 자체 검증): `selectList` 정렬(`last_used_at DESC, created_at DESC`), `selectNextDefaultCandidate`가 제외 대상(`excludeAddressId`)을 실제로 빼는지, `selectOwned`가 타 회원 소유 행에 대해 null을 반환하는지.
  - `MemberAddressConcurrencyTest`(실 DB 통합, **3회 단독 실행** — SR-231 r3 관례):
    1. 같은 회원이 9개를 가진 상태에서 동시 등록 요청 2건 → 최종 행 수가 정확히 10(11이 되지 않음, `selectByMemberIdForUpdate` 락이 카운트-후-삽입 경쟁을 막는지).
    2. 같은 회원의 서로 다른 배송지 2개에 대해 동시 기본설정 요청 → 종료 후 `is_default='Y'`가 정확히 1행(두 기본이 동시에 남지 않음).

- **테스트 격리**: 모든 테스트가 만드는 회원/배송지 데이터는 테스트 전용 `member_id`(예: 기존 시드 `M-0001` 재사용 시 다른 테스트의 배송지 개수 상태와 간섭할 수 있으므로, DAO/서비스/동시성 테스트는 **매 테스트가 자기 UUID 접미 member_id를 새로 만들거나**, 컨트롤러 테스트처럼 서비스가 목업이라 실제 DB에 안 쓰는 경우는 문제 없음). 통합/동시성/DAO 테스트는 `@AfterEach`(또는 트랜잭션 롤백 `@Transactional` 테스트)로 자신이 만든 `MEMBER_ADDRESSES` 행을 정리해, "회원당 최대 10개" 카운트가 다음 테스트로 새지 않게 한다(SR-232 r2와 동일 계열 함정 — 카운터/개수 제약이 있는 테이블은 테스트마다 격리하거나 반드시 청소).

- **폴백·우회 경로의 자격 판정**: 이 FUNC은 새로운 회원 식별 경로를 열지 않는다. `/me`가 가리키는 `memberId`는 항상 `ApiKeyAuthFilter`가 이미 해석해 둔 `scope` 값(정적 맵 또는 `MemberApiKeyDao.selectMemberIdByApiKey` DB 폴백)에서 온다 — 그 DB 폴백 자체는 SR-232 r2에서 이미 `MEMBERS` 조인 + `del_yn` 필터를 통과하도록 고쳐졌으므로, 탈퇴 회원의 API 키는 애초에 필터 단계에서 이미 걸러진다. 이 FUNC의 `MemberAddressDao`는 그 memberId를 그대로 믿고 조회하지만, 그 값 자체가 이미 자격 검증을 거쳐 필터 경계를 통과한 값이라 새로운 우회 경로가 아니다.

- **프레임워크 실행 모델 함정**: `MemberAddressService`의 각 쓰기 메서드(`register`/`delete`/`setDefault`) 전체를 **하나의 `@Transactional` public 메서드**로 두고, 그 안에서 `this.내부헬퍼()` 형태로 별도 `@Transactional` 메서드를 다시 호출하지 않는다 — Spring AOP 프록시는 같은 빈 내부에서의 self-invocation에 트랜잭션 어드바이스를 적용하지 못해(프록시를 안 거침) 락/커밋 경계가 깨질 수 있다. 이 설계는 각 흐름을 처음부터 단일 메서드 안에 직선으로 적어 그 함정 자체를 피한다(분리할 필요가 생기면 별도 서비스 빈으로 분리하되 이번 SR 범위에서는 불필요).

- **범위 밖**: 우편번호 검색 API(`/api/zipcodes`, `ZIPCODES` 테이블) — FUNC-member-012 몫. 배송지 관리 화면(shop-web) — FUNC-member-010 몫. 주문서 인라인 추가/선택·기본 배송지 미리 선택 — SR-255(주문서) 이월. 실 우편번호 API 연동 — 후속 SR. `zipcode`/`road_address`/`detail_address`의 서버측 유효성 검증(`ZIPCODES` 대조) — 이 FUNC은 클라이언트가 보낸 값을 그대로 저장한다. `last_used_at`을 실제 주문 시 갱신하는 로직 — 주문 통합은 SR-255 몫이라 이번 FUNC은 생성 시점에만 `last_used_at = created_at`으로 채운다(정렬 기준을 미리 확보해 두는 용도).

- **실패 사례집 대조** (`harness/antipatterns.all.md`):
  - SR-231 r3("락 순서를 한 곳에 고정") — 이 FUNC도 락 대상(회원의 배송지 행들)이 여러 쓰기 경로(register/delete/setDefault)에서 공유된다는 조건이 동일하게 성립한다 → `selectByMemberIdForUpdate` 한 DAO 메서드만을 락 지점으로 고정해 피한다(위 "데이터" 절).
  - SR-231 r5("존재 판정 → 인증 순서가 오라클을 만듦") — 이 FUNC은 인증 자체 순서 문제는 없지만 **같은 계열의 함정**(존재/소유를 분리해서 판정하면 오라클이 생김)이 여기서도 그대로 성립한다 → `selectOwned`가 존재+소유를 한 SQL로 묶어 피한다(위 "순서·보안" 절).
  - SR-231 r4("한 FUNC의 필요로 전역 계약을 바꾸지 않는다") — `ApiKeyAuthFilter`는 여러 FUNC이 공유하는 전역 필터라 조건이 성립한다. 다만 이번엔 그 교훈이 "건드리지 마라"가 아니라 "기존에 이미 있는 범용 메커니즘(`SCOPE_TO_SELF`)을 그대로 재사용하고 새 전역 동작을 추가하지 않는다"는 형태로 적용된다 — 새 패턴 매칭 1개만 추가하고 기존 분기 순서·기존 경로의 판정 결과는 전혀 바꾸지 않는다(회귀 없음, 기존 `ApiKeyAuthIntegrationTest`로 확인).
  - SR-231 r2("카운터를 트랜잭션 안에서 올렸다가 롤백에 같이 사라짐") — 이 FUNC의 개수 제한(409) 조건은 **롤백해도 잃을 것이 없는 사전 판정**(아직 아무 것도 쓰기 전에 락으로 읽은 크기만 검사)이라 그 사고의 조건(실패해도 남아야 하는 카운터를 트랜잭션 안에 넣음)이 성립하지 않는다 — 그대로 옮기지 않는다.
  - SR-232 r2("카운터/개수 제약 테스트가 청소 안 되면 다음 실행에 샌다") — "최대 10개" 제약이 있는 테이블이라 조건이 동일하게 성립 → 위 "테스트 격리" 절.

### 사람 수정 (2026-09-13, plan_decision=계획대로 진행 + 확정 지시 4건)

- **결정 요약**: 계획 승인. 락 지점 단일화(`selectByMemberIdForUpdate` 선행) · 존재+소유 단일 SQL(404 동일 응답) · 기존 `SCOPE_TO_SELF` 메커니즘 재사용 · `ZIPCODES` 미검증(FUNC-member-012 몫) · `ApiKeyAuthFilter` 최소 변경 — 전부 그대로 진행한다.
- **(1) `last_used_at`**: 등록 시 `created_at`과 **같은 값으로 채운다(NULL 금지)** — 목록 정렬(`last_used_at DESC, created_at DESC`)이 기본 배송지 승계 후보 선정과 같은 정렬을 쓰므로 NULL이 섞이면 순서가 흔들린다. 주문에서의 갱신은 SR-255 몫(이번 FUNC 범위 아님).
- **(2) `phone_norm`**: `MemberRegistrationService.normalizePhone`을 **재사용**(복제 금지 — 가시성만 `private`→패키지/`protected` 등으로 열어 재사용). `phone` 원문은 그대로 저장.
- **(3) 검증 규칙(400 `MBR-4200`, 위반 필드를 message에 명시)**:
  - `recipient`: 1~50자, 필수
  - `phone`: 정규화 후 형제 API(회원가입)와 동일한 정규식으로 검증, 필수
  - `zipcode`: 숫자 5자리, 필수
  - `road_address`: 1~200자, 필수
  - `detail_address`: 1~200자, 필수
  - `entrance_method`, `delivery_memo`: 선택, 최대 200자
- **(4) 응답 계약**: 등록 `201` + 생성된 배송지 본문(`addressId`·`isDefault` 포함) · 수정/기본설정 `200` + 본문 · 삭제 `204`(본문 없음) · 목록 `200`(0건이면 빈 배열 `{items: []}` — 404 아님).
- **보안 순서(확정)**: 인증(필터) → `SCOPE_TO_SELF`로 `memberId` 강제 → `selectOwned`(존재+소유 한 SQL) → 락(`selectByMemberIdForUpdate`) → 쓰기 → 커밋. 남의 `addressId`는 404 `MBR-4041`로 "없음"과 **바이트 동일** 응답. 로그에는 `addressId`·`memberId`만 남긴다(주소 본문·연락처는 로그 금지).
- **개수 제한**: 11번째 등록은 락 안에서 세어(`selectByMemberIdForUpdate` 결과 크기) 409 `MBR-4201`.
- **테스트·완료 조건(확정)**:
  - 컨트롤러(MockMvc): 5개 엔드포인트 + 400/404/409 매핑 전부
  - 서비스(Mockito): 승계 로직·기본 강제(첫 등록)·`setDefault` 멱등
  - DAO(실 DB): `del_yn` 필터·정렬
  - 동시성(실 DB): 9건 상태에서 2스레드 동시 등록 → 정확히 1건만 성공하고 나머지 1건은 409, 최종 10건(11건 아님) / 동시 `setDefault` 2건 → 종료 후 기본은 항상 정확히 1건
  - `mvn test` 전량 통과, 기준선(현재 shop-api 449건/0 실패)을 악화시키지 않을 것(순증가만 허용).
- **후속 SR·이월(이번 범위 아님)**: `ApiKeyAuthFilter` 파일 분리(450줄 상한 초과 해소)는 별도 SR 후보 · 주문서에서의 `last_used_at` 갱신·기본 배송지 미리 선택 → SR-255 · `ZIPCODES` 대조 → FUNC-member-012.

## 후속 추적(TODO) — 최종 확인 게이트 기록(2026-09-13)

사람 결정: **완료**. 아래는 이번 FUNC 범위 밖으로 명시 이월된 항목 — SR-235의 형제 FUNC(FUNC-member-012 이후) 진행 판단 시 함께 검토:
- `setDefault()`의 멱등 no-op 분기가 락 이전 스냅샷 기준으로 판정(`delete()`는 이번 라운드에 락 목록 기준으로 고쳤으나 `setDefault`는 범위 밖으로 한정)
- `MemberAddress` 응답에 `phoneNorm`이 `@JsonIgnore` 없이 노출됨(요구사항에 은닉 명시는 없었으나 내부 정규화 값 노출)
- `update()`가 요청의 `isDefault`를 무시함(스펙에 사실대로 기록)
- `ApiKeyAuthFilter` 483줄 `file-size-cap`(should) 초과 — 후속 SR에서 파일 분리

## 구현 Task
- [x] 컨트롤러/핸들러
- [x] 서비스/비즈니스 로직
- [x] 데이터 접근 레이어
- [x] 단위 테스트

## Dev 기록
- 계획 확인: 계획대로 진행 + 확정 지시 4건 반영(2026-09-13, 사람 게이트)

### 구현 완료(2026-09-13)

**신규 파일**
- `modules/shop-api/src/main/resources/db/V7__member_addresses.sql` — MEMBER_ADDRESSES DDL(멱등, FK → MEMBERS)
- `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberAddress.java`
- `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberAddressDao.java`
- `modules/shop-api/src/main/resources/mapper/memberAddress.xml`
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressService.java`
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressApiException.java`
- `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberAddressController.java`
- `modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberAddressExceptionHandler.java`
- `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberAddressControllerTest.java`(11)
- `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberAddressServiceTest.java`(11)
- `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberAddressDaoTest.java`(3)
- `modules/shop-api/src/test/java/com/sm/lab/shop/MemberAddressConcurrencyTest.java`(2)

**수정 파일**
- `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — `MEMBER_ME_ADDRESSES_PATH`
  정규식 1개 + `evaluateMemberScope`에 SCOPE_TO_SELF 분기 1개 추가(465→483줄, `file-size-cap`은
  `should`이고 계획이 이미 이번 SR 범위 밖 리팩터로 이월함을 명시). 기존 분기 순서·판정 결과는
  전혀 바꾸지 않음(`ApiKeyAuthIntegrationTest` 55건 전부 통과로 회귀 없음 확인).
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberRegistrationService.java` —
  `normalizePhone`을 `private`→package-private로 가시성만 변경(본문 무변경, 사람 수정 (2)).
- `modules/shop-api/src/main/resources/application.yml` — `schema-locations`에
  `V7__member_addresses.sql` 이어붙임(기존 6개 순서 불변).

**계획과 다르게 간 지점(코드 작성 전 판단)**
- 계획 문구 "요청/응답 DTO는 컨트롤러 내부 record(CartController 관례)"를 그대로 따라
  `MemberAddressService`가 컨트롤러의 `AddressRequest`를 파라미터로 받게 하면, 이 코드베이스의
  어떤 `service` 클래스도 `controller` 패키지를 import하지 않는다는 기존 계층 방향(controller→
  service→dao)을 어기는 역방향 의존이 생긴다(`CartController`의 실제 관례는 record를 그대로
  넘기지 않고 필드를 풀어 넘기는 것 — `cartService.addItem(req.memberId(), req.sku(), req.qty())`).
  필드가 8개라 그대로 풀면 파라미터 목록이 과도해져, 서비스 전용 내부 record
  `MemberAddressService.AddressInput`을 두고 컨트롤러가 변환해 넘기게 했다 — 의존 방향은
  controller→service만 유지.
- `isDefault`(및 Y/N 플래그 전반)는 boolean이 아니라 house 관례(`Product.saleYn` 등)를 따라
  String("Y"/"N")으로 두고 JSON 응답에도 그대로 노출(`ProductControllerTest`의 `saleYn` 응답
  관례와 동일 근거 확인 후 결정).

**테스트 실행 결과**
- 신규 27건(컨트롤러 11 + 서비스 11 + DAO 3 + 동시성 2) 전부 통과.
- 동시성 테스트(`MemberAddressConcurrencyTest`)는 SR-231 r3 관례대로 단독 3회 실행 — 3회 전부
  통과(락 지점 `selectByMemberIdForUpdate` 단일화가 카운트-후-삽입/기본전환 경쟁을 정확히 막음).
- `MemberRegistrationService` 가시성 변경 회귀 확인: `MemberRegistrationServiceTest`(18)·
  `MemberRegistrationConcurrencyTest`(1)·`MemberRegistrationPhoneNormalizationTest`(1) 전부 통과.
- `ApiKeyAuthFilter` 변경 회귀 확인: `ApiKeyAuthIntegrationTest`(55) 전부 통과 — 기존 화이트리스트·
  소유권 판정 결과 불변.
- 전체 스위트(`mvn test`, 포그라운드): **476건/0 실패**(기준선 449건/0 실패 대비 +27, 순증가만
  — 회귀 없음).

### 재작업 완료(round 2, 2026-09-13) — QA CONCERNS(round 1) 권고 6건 반영

**수정 파일**
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberAddressService.java`
  — [재작업 1] `delete()`가 `selectByMemberIdForUpdate`(락, 최신 커밋본)의 반환 목록을 그대로
    써서 삭제 대상의 `is_default`와 승계 후보(대상 제외, `last_used_at DESC, created_at DESC`
    첫 행)를 그 안에서 고르도록 재작성. 락 이전 `selectOwned` 스냅샷은 존재+소유 확인에만 쓰고
    승계 판정에는 더 이상 쓰지 않는다. 비잠금 `selectNextDefaultCandidate` 호출은 `delete()`
    경로에서 제거(DAO 메서드 자체는 `MemberAddressDaoTest`가 계속 쓰므로 유지).
  — [재작업 3] `requireValidPhone`에 정규화 전 원문 길이 상한(20, DDL `VARCHAR(20)`과 동일)
    검증을 추가 — 위반 시 400 `MBR-4200`(패턴 검사보다 먼저 수행).
  — [재작업 4] 로컬 `PHONE_PATTERN` 상수를 제거하고 `MemberRegistrationService.PHONE_PATTERN`을
    그대로 참조(문자열 복제 제거).
- `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberRegistrationService.java`
  — [재작업 4] `PHONE_PATTERN`을 `private`→package-private로 가시성만 변경(본문·값 무변경).
- `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberAddressDao.java`,
  `modules/shop-api/src/main/resources/mapper/memberAddress.xml`
  — [재작업 1] `selectByMemberIdForUpdate`가 반환하는 컬럼에 `created_at`을 추가(승계 후보
    2차 정렬 키로 필요 — 그전까지는 `address_id, is_default, last_used_at`만 반환했다).
    Javadoc/주석으로 `delete()`가 이 메서드 하나만을 승계 판정의 근거로 쓴다는 것과, 왜
    `selectNextDefaultCandidate`(비잠금)를 더 이상 쓰지 않는지 명시.
- `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberAddressServiceTest.java`
  — [재작업 1] S05(`delete_defaultAddressWithCandidate_promotesCandidateFromLockedList`)를 락
    목록 기반 승계로 재작성(정렬 검증 포함) + S07 갱신(불필요한 스텁 제거) + 회귀 단위테스트
    1건 신설(`delete_lockedListDisagreesWithPreLockSnapshot_usesLockedListDefault` — `selectOwned`
    와 락 목록의 `is_default`가 다를 때 락 목록을 따르는지 직접 확인). [재작업 3] 원문 phone
    21자 → 400 단위테스트 1건 추가.
- `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberAddressDaoTest.java`
  — [재작업 2] `softDeletedRow_isExcludedFromSelectList_selectOwned_andSelectByMemberIdForUpdate`
    신설(D04) — 소프트 삭제된 행이 `selectList`·`selectOwned`·`selectByMemberIdForUpdate` 셋
    다에서 안 보이는지 실 DB로 단언.
- `modules/shop-api/src/test/java/com/sm/lab/shop/MemberAddressConcurrencyTest.java`
  — [사람 코멘트 동시성 2건] `concurrentDeleteDefault_andSetDefaultOnAnotherAddress_exactlyOneDefaultRemains`
    (C03: delete(기본)‖setDefault(다른 배송지) → 종료 후 기본 정확히 1행) ·
    `concurrentDeleteLastDefault_andRegisterNewAddress_exactlyOneDefaultRemains`(C04: delete(기본,
    마지막 1건)‖register(새 배송지) → 종료 후 기본 정확히 1건, 0건 아님) 신설. 기존 `callRegister`/
    `callSetDefault`를 memberId 파라미터화해 재사용.
- `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java`
  — [재작업 5] `memberAddressesRoute_foreignMemberIdQueryParam_forcedToSelf_excludesForeignData`
    (GET `?memberId=타인` → 강제 축소로 타인 배송지 미노출) ·
    `memberAddressesRoute_foreignMemberIdParamWithForeignAddressId_updateAndDelete_return404`
    (PUT/DELETE `?memberId=타인` + 타인 소유 addressId → 404 `MBR-4041`) 신설.
- `docs/00_FUNC/stories/STORY-FUNC-member-011.md` — [재작업 6] 수용 기준·구현 Task 체크박스 갱신.

**계획·범위 확인**: 락 순서(register/delete/setDefault 전부 쓰기 전 `selectByMemberIdForUpdate`
선행)·존재+소유 단일 SQL(`selectOwned`)·404 동일 응답·로그 규칙(주소 본문·연락처 미기록)은
이번 라운드에서 손대지 않았다(round 1 QA PASS 유지 대상).

**테스트 실행 결과**(실측, `mvn test` surefire 리포트 기준)
- 순증 7건 — `MemberAddressServiceTest` 11→13(+2: S05는 락 목록 기반으로 재작성해 건수 불변,
  회귀 단위테스트 `delete_lockedListDisagreesWithPreLockSnapshot_usesLockedListDefault` 1건 +
  phone 원문 길이 `register_phoneRawLengthOverTwenty_throws400` 1건이 순증) ·
  `MemberAddressDaoTest` 3→4(+1, D04 `del_yn` 필터) ·
  `MemberAddressConcurrencyTest` 2→4(+2, C03/C04) ·
  `ApiKeyAuthIntegrationTest` 55→57(+2, 강제축소 회귀 2건). 전부 통과.
- 동시성 신규 2건(`concurrentDeleteDefault_andSetDefaultOnAnotherAddress_exactlyOneDefaultRemains`,
  `concurrentDeleteLastDefault_andRegisterNewAddress_exactlyOneDefaultRemains`)이 포함된
  `MemberAddressConcurrencyTest` 클래스 전체(4건)를 단독 3회 실행 — 3회 전부 통과(락 목록 기반
  승계가 두 인터리빙 순서 모두에서 "기본 정확히 1건" 불변식을 지킴을 확인).
- `MemberRegistrationService` 가시성 변경(2회째, `PHONE_PATTERN`) 회귀 확인:
  `MemberRegistrationServiceTest`(18)·`MemberRegistrationConcurrencyTest`(1)·
  `MemberRegistrationPhoneNormalizationTest`(1) 전부 통과.
- `ApiKeyAuthFilter`는 이번 라운드에서 변경하지 않음(round 1과 동일 483줄) — `ApiKeyAuthIntegrationTest`
  57건(기존 55 + 신규 2) 전부 통과.
- 전체 스위트(`mvn test`, 포그라운드): **483건/0 실패**(기준선 449/0 대비 +34, round 1의 476건
  대비 이번 라운드 순증 +7) — 회귀 없음.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-13 — CONCERNS
- **Layer1 스펙**: pass. 5개 엔드포인트·상태코드(201/200/204/200/200)·오류코드(MBR-4200/4041/4201/5000)·
  봉투(`{items:[]}` / 단건 raw)·사람 확정 검증 규칙 6종·`last_used_at = created_at`(NULL 경로 없음 —
  `register`만 유일한 insert 경로이고 항상 `now` 주입, `updateAddress`는 그 컬럼을 건드리지 않음)·
  `normalizePhone` 재사용(복제 아님, 가시성만 `private`→package-private)·개수 제한(락 안에서 세기)·
  승계·`setDefault` 멱등·`ZIPCODES` 미생성(FUNC-member-012 몫) 전부 계획·사람 확정대로. DDL 멱등
  (`IF NOT EXISTS`, DROP 없음), `no-select-star`/`no-sysout`/`no-printstacktrace`/`controller-has-test`/
  `service-has-test` 규칙 위반 0. 계획과 다르게 간 2건(`AddressInput` 서비스 내부 record, `isDefault`
  String "Y"/"N")은 Dev 기록에 근거와 함께 선언됐고 계층 방향·house 관례상 타당 — 수용.
- **Layer2 보안**: pass. 존재+소유가 `selectOwned` **한 SQL**(2단계 코드 0건, `MemberAddressDaoTest.D03`이
  실 DB로 고정)이고 "없음"과 "남의 것"이 동일 `notFound()` 한 지점에서 나와 상태·코드·메시지가 **바이트
  동일**. `ForcedMemberIdRequest`가 `getParameter`/`getParameterValues`/`getParameterMap` **셋 다**
  오버라이드하므로 member 키가 `?memberId=타인`을 실어도 Spring `@RequestParam` 바인딩(=`getParameterValues`)
  까지 자기 자신으로 강제됨 — IDOR 없음. admin 키 + memberId 미지정은 400으로 명시 차단(사람 확정).
  로그는 예외 핸들러 1줄뿐이고 주소 본문·연락처를 싣지 않음. 500 응답은 정제 메시지.
- **Layer3 회귀**: concerns. `ApiKeyAuthFilter` 추가 분기는 신규 경로 정규식만 매치하고 admin-only DENY
  뒤·`MEMBERS_ITEM_PATH` 앞에 놓여 기존 판정 불변(`ApiKeyAuthIntegrationTest` 55건 통과로 실측).
  전체 스위트 476/0(기준선 449/0 대비 +27, surefire 리포트 실측) — 순증가만. **다만 `delete()`의
  기본 배송지 승계가 락 획득 전 스냅샷을 근거로 판정한다**(아래 권고 1).
- 권고(CONCERNS시):
  1. **[medium·회귀/동시성] `delete()` 승계 판정이 락 이전 스냅샷을 쓴다 — 계획 문구 위반.**
     계획(`## 구현 계획` "데이터" 절)은 *"대상이 `is_default='Y'`였으면 `selectNextDefaultCandidate`
     (**락으로 얻은 목록에서 조회**)"* 라고 못 박았는데, 구현은 `dao.selectByMemberIdForUpdate(memberId);
     // 반환값은 락 확보 목적으로만 쓴다`로 반환값을 버리고 ① 락 **이전**에 읽은 `owned.getIsDefault()`로
     승계 여부를 판정하고 ② 후보를 비잠금 `selectNextDefaultCandidate`로 다시 읽는다. MariaDB 기본
     REPEATABLE READ에서 트랜잭션 스냅샷은 **첫 일반 SELECT(`selectOwned`)** 시점에 고정되므로, 비잠금
     재조회는 락을 기다리는 동안 커밋된 타 트랜잭션의 결과를 보지 못한다. 결과적으로 "기본 1개" 불변식이
     깨질 수 있다(아래 실패 시나리오). 수정: 락 호출의 반환 목록(잠금 읽기 = 최신 커밋본)을 받아 그
     목록에서 `is_default` 재확인과 승계 후보 선정을 수행한다. 회귀 테스트로 delete‖setDefault 동시성
     케이스를 `MemberAddressConcurrencyTest`에 1건 추가.
  2. **[medium·테스트커버리지] 소프트 삭제가 실제로 조회에서 사라지는지 검증하는 테스트가 없다.**
     사람 확정 완료조건은 *"DAO(실 DB): `del_yn` 필터·정렬"* 인데 `MemberAddressDaoTest`는 정렬(D01)·
     제외(D02)·소유(D03) 3건뿐이고 `del_yn` 필터 케이스가 없다. 서비스·컨트롤러 테스트는 목업이라
     `softDelete` 후 `selectList`/`selectOwned`에서 빠지는지를 어느 층도 확인하지 않는다 — 수용 기준의
     "삭제"가 실 SQL 수준에서 미검증. `softDelete` → `selectList` 0건 + `selectOwned` null 테스트 1건 추가.
  3. [low] 원문 `phone` 길이를 검증하지 않는다 — `normalizePhone`이 `-`·공백만 제거하므로 하이픈을 길게
     섞은 입력은 정규화 후 통과하지만 원문이 `VARCHAR(20)`을 넘겨 400이 아닌 500 `MBR-5000`이 된다.
     `phone` 원문 길이 상한(20) 검증 1줄 추가 권고.
  4. [low] `PHONE_PATTERN`을 `MemberRegistrationService`에서 문자열 복제했다(현재 두 값은 동일).
     `normalizePhone`은 지시대로 재사용했으므로 실질 위반은 아니나, 한쪽만 바뀌면 형제 API와 검증이
     조용히 어긋난다 — 후속 SR에서 정규식도 같은 방식으로 열어 재사용.
  5. [low] member 키가 `?memberId=타인`을 실어 보낸 경우의 강제 축소를 고정하는 테스트가 없다
     (코드는 `ForcedMemberIdRequest` 3개 오버라이드로 안전하나 회귀 고정이 없음). `ApiKeyAuthIntegrationTest`에
     `/api/members/me/addresses` 케이스 1건 추가 권고.
  6. [low] STORY `## 수용 기준`·`## 구현 Task` 체크박스가 여전히 `- [ ]` — 구현 완료 표기 누락(추적용).
  7. [low·기수용] `ApiKeyAuthFilter` 483줄로 `file-size-cap`(should, 450) 초과 — 계획이 이미 별도 SR로
     이월 명시. 이번 라운드에서 다루지 않는다.

### QA Gate — 2026-09-13 — PASS (round 2 재게이트)
- **재작업 6건 전수 확인(코드 직접 열람, 전부 반영됨)**:
  1. ✅ `delete()` 승계 판정이 락 목록 기반 — `MemberAddressService:172` `List<MemberAddress> locked =
     dao.selectByMemberIdForUpdate(memberId)`의 반환값을 실제로 소비한다. `wasDefault`는 `locked`에서
     삭제 대상 행의 `is_default`를 다시 읽고(`:174-177`), 승계 후보도 `locked`에서 대상 제외 후
     `max(lastUsedAt → createdAt)`로 고른다(`:183-188`, SQL의 `ORDER BY ... DESC LIMIT 1`과 동치).
     비잠금 `selectNextDefaultCandidate` 호출은 **main 전체에서 0건**(grep 실측 — 잔존 참조는 DAO/
     서비스 javadoc과 매퍼 XML 정의뿐). 매퍼 `selectByMemberIdForUpdate`에 `created_at`이 추가돼
     2차 정렬 키가 실제로 공급된다.
  2. ✅ `del_yn` 필터 실DB 케이스 — `MemberAddressDaoTest.D04`
     (`softDeletedRow_isExcludedFromSelectList_selectOwned_andSelectByMemberIdForUpdate`)가
     세 조회 경로 전부에 단언(3→4건).
  3. ✅ phone 원문 길이 검증 — `requireValidPhone`(`:236`)이 **패턴 검사(`:239`)보다 먼저** 원문
     `phone.length() > 20`을 400 `MBR-4200`으로 막는다. (`normalizePhone` 호출 자체는 `:95`로 앞서
     있으나 순수·null-safe 함수이고 검증 대상이 **원문**이므로 지시 의도 — "정규화가 초과 길이를
     가리지 못하게" — 는 충족.) 길이 상한 전수 대조 결과 `recipient(50)`·`zipcode(10)`·
     `road/detail(200)`·`entrance/memo(200)` 모두 DDL 컬럼 폭 이내 — 잔여 500 경로 없음.
  4. ✅ `PHONE_PATTERN` 복제 제거 — `MemberAddressService`에 로컬 상수 없음, `:239`가
     `MemberRegistrationService.PHONE_PATTERN`을 직접 참조. 원본은 `private`→package-private
     가시성만 변경(값·본문 무변경, diff 실측).
  5. ✅ `?memberId=타인` 회귀 — `ApiKeyAuthIntegrationTest`에 2건 신설(GET 강제축소로 타인 배송지
     미노출 · PUT/DELETE 타인 addressId → 404 `MBR-4041`), 55→57건.
  6. ✅ STORY 체크박스 — `## 수용 기준` 2건 · `## 구현 Task` 4건 전부 `- [x]`.
- **신규 동시성 2건 — 실제 경쟁 맞음(순차 호출 아님)**: C03/C04 모두 `CyclicBarrier(2)` + 2스레드
  `ExecutorService`로 두 요청이 **같은 출발선에서 동시 출발**하도록 강제한다(`awaitBarrier`가 HTTP
  호출 직전). C04(delete(마지막 기본)‖register)는 종전 결함을 **실제로 판별**한다 — register가
  delete의 락 대기 중 커밋되는 인터리빙에서 구(舊) 비잠금 재조회는 새 행을 못 봐 기본 0건이 됐다.
  (단, 그 인터리빙은 실행마다 갈리므로 C04의 판별력은 확률적 — 결정적 고정은 단위테스트
  `delete_lockedListDisagreesWithPreLockSnapshot_usesLockedListDefault`가 맡아 조합으로 충분.)
- **Layer1 스펙**: pass. round 1 계약(5 엔드포인트·201/200/204/200/200·`MBR-4200/4041/4201/5000`·
  `{items:[]}`/단건 raw·검증 규칙 6종·`last_used_at=created_at`·개수 제한·승계·`setDefault` 멱등)
  불변. 이번 라운드가 계획 위반 1건(승계 판정 위치)을 계획 문구대로 되돌려 **스펙 정합이 올라갔다**.
  `rules_check` must 위반 0(should 1건은 기수용 `ApiKeyAuthFilter` 483줄, 계획이 후속 SR 이월 명시).
- **Layer2 보안**: pass. round 1 PASS 항목 전부 재확인 — 존재+소유 `selectOwned` **단일 SQL** 유지
  (2단계 판정 코드 0건), 404 `MBR-4041`이 `notFound()` 한 지점에서만 나와 "없음"과 "남의 것"이
  바이트 동일, `ForcedMemberIdRequest`의 `getParameter`/`getParameterValues`/`getParameterMap`
  **3종 오버라이드 모두 온전**(`ApiKeyAuthFilter:466-481`), 예외 핸들러 로그에 주소 본문·연락처
  미기록·500 정제 메시지 유지. 이번 라운드는 여기에 **회귀 고정 테스트 2건을 더했다**(방어 강화).
- **Layer3 회귀**: pass. `ApiKeyAuthFilter`는 이번 라운드 무변경(483줄 동일), `MemberRegistrationService`는
  가시성 1줄만 변경(diff 실측) — `MemberRegistrationServiceTest`(18)·`ConcurrencyTest`(1)·
  `PhoneNormalizationTest`(1) 전부 통과. **전체 스위트 QA 독립 실측: 483건 / 0 실패 / 0 오류 /
  0 스킵, BUILD SUCCESS**(59 클래스 surefire 집계) — 기준선 449/0 대비 **+34**, round 1의 476 대비
  **+7**(Service 11→13 · DAO 3→4 · Concurrency 2→4 · ApiKeyAuth 55→57) — Dev 기록 수치와 일치,
  순증가만. 동시성 클래스 **단독 3회 재실행 전부 4/4 통과**(house 관례, 플레이키 없음).
- 후속 TODO(이번 라운드 차단 아님 — 이전 라운드부터 있던 코드에서 새로 관찰, 라운드 규율에 따라 low):
  1. [low] `setDefault()`의 멱등 no-op 분기(`:199`)가 **락 이전** `selectOwned` 스냅샷의 `is_default`로
     판정한다 — 방금 고친 `delete()`와 같은 계열. "기본 정확히 1건" 불변식은 깨지 않으나(C02 통과),
     동시 `setDefault`가 겹치면 두 번째 요청이 실제로는 기본이 아니게 된 대상에 대해 no-op 200을
     돌려줄 수 있다. round 1에도 있던 코드이고 사람이 이번 수정 범위를 delete로 한정했으므로 후속 SR.
  2. [low] `MemberAddressService:63-66`의 "PHONE_PATTERN 재사용" 주석이 삭제된 상수 자리에 남아
     `ZIPCODE_PATTERN` 위에 붙어 있다(내용은 맞으나 위치가 오독을 부른다).
  3. [low] IDOR 회귀 GET 단언이 `doesNotContain("\"addressId\":" + id)` 문자열 대조 — 자기 배송지
     id가 타인 id의 접두일 때 **거짓 실패**가 날 수 있다(거짓 통과는 불가 — fail-closed). 심는 행의
     UUID `recipient`로 대조하면 견고해진다.
  4. [low·기수용] `ApiKeyAuthFilter` 483줄 `file-size-cap` 초과 — 계획대로 후속 SR.

## 테스트 결과 (Test Agent, STEP 5)

### 수용 기준(AC) ↔ 테스트 케이스(TC) 매핑

| AC | 대상 항목 | 커버 TC | 상태 |
|---|---|---|---|
| **AC-1: INF-MBR-008 요청/응답 계약 충족** | 5개 엔드포인트 | TC-01(목록), TC-02(등록-201), TC-05(수정-200), TC-07(삭제-204), TC-09(기본설정-200) | ✅ Pass |
| | 상태코드 201/200/204/200/200 | 상동 | ✅ Pass |
| | 오류코드 MBR-4200 | TC-03(필수값), TC-10(phone), TC-11(admin), TC-11(zipcode), S10, S11, S12 | ✅ Pass (7개) |
| | 오류코드 MBR-4041 | TC-06(수정), TC-08(삭제), TC-10(기본설정), S04, S09 | ✅ Pass (5개) |
| | 오류코드 MBR-4201 | TC-04(11번째 등록), S02 | ✅ Pass (2개) |
| | 봉투 {items:[]}/단건 | TC-01({items:[]}), TC-02~09(단건 raw) | ✅ Pass |
| | 검증 규칙 6종 | S10(recipient), S11(zipcode), S12(phone-원문길이), S12(phone-정규식), D01(정렬), D04(del_yn) | ✅ Pass (6개) |
| **AC-2: SR 정본 계약 충족** | 락 순서 단일화 | S01~S08(select-before-write), C01, C02 | ✅ Pass |
| | 존재+소유 단일 SQL | D03(selectOwned 단일), TC-06/08/10(404), S04/09(404) | ✅ Pass |
| | 개수 제한 | TC-04(409), S02(409 미기록), C01(10개 정확) | ✅ Pass |
| | 승계 | S05(락 목록 기반), S06(안 함), S07(후보 없음), C03, C04 | ✅ Pass |
| | 기본 강제 | S01(첫 등록 Y 강제), S03(clear+insert), S08(no-op) | ✅ Pass |
| | del_yn 필터 | D04(soft delete 후 안 보임), D01/D02/D03에 암시적 포함 | ✅ Pass |
| | IDOR 강제축소 | TC-11(admin key + memberId 미지정 차단), API 회귀(강제 축소) | ✅ Pass |

### 테스트 통계

| 계층 | 테스트 클래스 | 건수 | 상태 |
|---|---|---|---|
| **Controller** | MemberAddressControllerTest | 11 | ✅ Pass (11/11) |
| **Service** | MemberAddressServiceTest | 13 | ✅ Pass (13/13) |
| **DAO** | MemberAddressDaoTest | 4 | ✅ Pass (4/4) |
| **Concurrency** | MemberAddressConcurrencyTest | 4 | ✅ Pass (4/4, 단독 3회) |
| **Regression** | ApiKeyAuthIntegrationTest (+2회귀) | 2 | ✅ Pass (2/2) |
| **신규 합계** | — | **34** | ✅ Pass (34/34) |
| **기존 합계** | shop-api 기준선 | 449 | ✅ Pass (449/449) |
| **전체 통과율** | 483 / 483 | 100% | ✅ **BUILD SUCCESS** |

### 회귀 검증

SR-235의 회귀 범위(변경명세 절):
- 기존 회원가입·로그인·주문 생성 흐름 유지 → MemberRegistrationServiceTest 18건 + ConcurrencyTest 1건 + PhoneNormalizationTest 1건 **전부 통과**
- 주문 API의 배송지 입력 계약(주문 본문에 주소 문자열) 유지 → OrderCreateTest 기존 테스트 통과(인터페이스 변경 없음)
- MEMBERS·ORDERS 테이블 컬럼 변경 없음 → 기존 스키마 유지(확인)
- ApiKeyAuthFilter 기존 동작 불변 → ApiKeyAuthIntegrationTest 기존 55건 + 신규 2건 **57건 전부 통과**

**회귀 TC 파일 경로**: SR-235 폴더에 03_TC.md 미제공 — 회귀 검증은 기존 테스트 재실행으로 구성

### 품질 판정

✅ **PASS** — 수용 기준 2개 전수 충족 / AC↔TC 매핑 명시적 완료 / 통과율 100%(34/34) / 회귀 0건 / 기준선 유지(449/483 사업부+신규)

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/regression] delete()의 기본 배송지 승계가 락 이전 스냅샷을 근거로 판정한다 — 계획은 '락으로 얻은 목록에서 조회'라고 못 박았으나 구현은 selectByMemberIdForUpdate 반환값을 버리고(주석 '반환값은 락 확보 목적으로만 쓴다') 락 전에 읽은 owned.getIsDefault()로 승계 여부를 판정하고 후보를 비잠금 selectNextDefaultCandidate로 재조회한다. MariaDB 기본 REPEATABLE READ에서 트랜잭션 스냅샷은 첫 일반 SELECT(selectOwned) 시점에 고정되므로 락 대기 중 커밋된 타 트랜잭션 결과가 보이지 않아 '기본 1개' 불변식이 깨질 수 있다(동시 setDefault와 겹치면 기본 2건, 동시 register와 겹치면 기본 0건). → selectByMemberIdForUpdate(잠금 읽기 = 최신 커밋본) 반환 목록을 받아 그 목록에서 is_default 재확인과 승계 후보 선정을 수행하고, delete||setDefault 동시성 케이스를 MemberAddressConcurrencyTest에 1건 추가한다.
2. [medium/spec] 소프트 삭제가 실제 조회에서 사라지는지 검증하는 테스트가 없다. 사람 확정 완료조건 'DAO(실 DB): del_yn 필터·정렬' 중 del_yn 필터가 미이행 — MemberAddressDaoTest는 정렬(D01)·제외(D02)·소유(D03) 3건뿐이고, 서비스·컨트롤러 테스트는 DAO/서비스 목업이라 softDelete 후 selectList/selectOwned에서 빠지는지를 어느 층도 확인하지 않는다(수용 기준 '삭제'가 실 SQL 수준 미검증). → MemberAddressDaoTest에 softDelete 후 selectList 0건 + selectOwned null을 단언하는 테스트 1건 추가.
3. [low/spec] 원문 phone 길이를 검증하지 않는다 — normalizePhone이 '-'와 공백만 제거하므로 하이픈을 길게 섞은 입력은 정규화 후 패턴을 통과하지만 원문이 VARCHAR(20)을 넘겨 400 MBR-4200이 아니라 500 MBR-5000이 된다. → requireValidPhone에 원문 길이 상한(20) 검증을 추가.
4. [low/regression] PHONE_PATTERN을 MemberRegistrationService에서 문자열로 복제했다(현재 두 값은 ^01[016789][0-9]{7,8}$로 동일). normalizePhone은 사람 지시대로 재사용했으므로 실질 위반은 아니나, 한쪽만 바뀌면 형제 API(회원가입)와 검증이 조용히 어긋난다. → 후속 SR에서 정규식 상수도 normalizePhone과 같은 방식(가시성 개방)으로 재사용.
5. [low/security] member 키가 ?memberId=타인을 실어 보낸 경우의 SCOPE_TO_SELF 강제 축소를 고정하는 테스트가 없다. 코드는 ForcedMemberIdRequest가 getParameter/getParameterValues/getParameterMap 셋 다 오버라이드해 안전하나(IDOR 없음), 회귀를 막는 테스트가 없어 이후 누군가 오버라이드 하나를 지우면 조용히 뚫린다. → ApiKeyAuthIntegrationTest에 /api/members/me/addresses?memberId=타인 → 자기 자신으로 강제 케이스 1건 추가.
6. [low/spec] STORY의 ## 수용 기준·## 구현 Task 체크박스가 구현 완료 후에도 '- [ ]' 상태 그대로다(추적 표기 누락). → 구현 완료분 체크박스를 갱신.

사람 코멘트: [결정 요약] medium 2건 + low 4건 전부 이번 라운드에 고친다 — 전부 같은 서비스·DAO·테스트 파일이고, 1번은 계획(승계 후보는 락으로 얻은 목록에서)과 다르게 간 것이라 계획 준수 문제다.
[구현 방식]
(1) delete(): selectOwned → selectByMemberIdForUpdate(락)의 반환 목록을 그대로 쓴다 — 삭제 대상의 is_default와 승계 후보(대상 제외, last_used_at DESC, created_at DESC 첫 행)를 그 목록에서 고른다. 비잠금 selectNextDefaultCandidate 호출은 delete 경로에서 제거(메서드 자체는 남겨도 됨).
(2) DAO 실DB 테스트에 del_yn='N' 필터 케이스 추가: 소프트 삭제된 행이 selectList·selectOwned·selectByMemberIdForUpdate 셋 다에서 안 보임을 단언.
(3) phone 원문 길이 1~20 검증 → 400 MBR-4200(정규화 전에 검사).
(4) PHONE_PATTERN·normalizePhone은 MemberRegistrationService 것을 참조(가시성만 연다, 복제 삭제).
(5) 회귀 테스트: ?memberId=타인 으로 GET/PUT/DELETE 호출 시 강제 축소돼 본인 데이터만 보이고 남의 addressId는 404.
(6) STORY 수용 기준·구현 Task 체크박스 갱신.
[보안 순서] 락 순서는 계획 그대로(쓰기 전 selectByMemberIdForUpdate 선행) — delete 경로가 락 밖 스냅샷으로 판단하던 것을 락 안 목록으로 옮기는 것이 이번 수정의 전부다. 404 동일 응답·로그 규칙 불변.
[테스트·완료 조건] 동시성 테스트 2건 추가: delete(기본)‖setDefault(다른 행) 동시 → 최종 기본 배송지 정확히 1건 · delete(기본, 마지막 1건)‖register(새 행) 동시 → 최종 기본 1건(0건 아님). mvn test 전량 통과(476+), 기준선 449/0 유지.
[후속 SR·이월] ApiKeyAuthFilter 파일 분리는 후속 SR(계획대로). 주문서 last_used_at 갱신·기본 배송지 미리 선택은 SR-255.
