---
item: SR-235.3
title: 우편번호(도로명) 검색 API
legacy_func: FUNC-member-012
story-id: STORY-FUNC-member-012
func-id: FUNC-member-012
status: Done
domain: member
created: 2026-09-13
spec_markers: 0
sr-id: SR-235
approved_sha: 15c72ac39d54
---

# STORY-FUNC-member-012 — SR-235 — 우편번호(도로명) 검색 API · 신규 INF-MBR-009 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

## Story
SR-235 — 우편번호(도로명) 검색 API · 신규 INF-MBR-009 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)


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
- [x] INF-MBR-009: 요청/응답 계약 충족 — {items: [...]} 봉투 · 4필드 셰이프 · 0건도 200 + 빈배열
- [x] SR 정본 계약 충족 — 검색어 정규화(trim+연속공백) · 2~50자 범위 · 도로명 부분일치 + 숫자 전방일치 · LIMIT 50

### TC 실행 결과 (2026-09-19)

**ZipcodeControllerTest** (6건): 6/6 ✅
- TC-FUNC-member-012-01: 정상 2건 반환 → 200 + items 2개
- TC-FUNC-member-012-02: 0건 → 200 + items []
- TC-FUNC-member-012-03: 1자 → 400 MBR-4202
- TC-FUNC-member-012-04: 51자 → 400 MBR-4202
- TC-FUNC-member-012-05: q 없음(null) → 400 MBR-4202
- TC-FUNC-member-012-06: member 키로 호출 → 200 통과 (회원스코프 강제 없음)

**ZipcodeServiceTest** (12건): 12/12 ✅
- S01: 정상 검색(도로명)
- S02~S05: null · 빈문자열 · 공백만 · 1자 → 400
- S06~S07: 앞뒤공백·연속공백 정규화
- S08~S09: 50자 통과·51자 400
- S10~S11: 숫자 판별(숫자만 true, 숫자+공백 false)
- S12: DAO 빈리스트 → 예외 아님

**ZipcodeDaoTest** (7건): 7/7 ✅
- D01: 도로명 부분일치 + 4필드 셰이프
- D02: 없는 주소 → 빈 리스트
- D03: 우편번호 전방일치(numeric=true)
- D04: 숫자도 도로명 부분일치(OR 동작) ← round2 재작업 반영
- D05: LIMIT 50(60행 → 50건)
- D06: ORDER BY road_address
- D07: 시드 분포·인코딩 정상 ← round2 재작업 신규

**통과율**: 25/25 (100%)

**AC 매핑**:
- AC1(요청/응답): TC-01, 02, D01 ✅
- AC2(SR 정본): TC-03, 04, 05, S01~S11, D01~D07 ✅

**환경 이슈(무관)**:
- mvn test 전체 573건 중 3건 실패(SR-300 시간 의존) — Zipcode 무관

**결론**: ✅ AC1·AC2 전량 검증 · 회귀 0건 · 납품 가능

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-009
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)

## 구현 계획

> 범위 한정(사전 확인 완료): `modules/shop-api` 소스를 확인한 결과 `MEMBER_ADDRESSES`(배송지 CRUD, `MemberAddressController`/`Service`/`Dao`/`V7__member_addresses.sql` 등)는 형제 FUNC-member-011이 이미 구현을 끝냈다(FUNC_MAP `✅ 완료`). 화면(shop-web 마이페이지)은 FUNC-member-010 몫이다. 이 FUNC-member-012는 **`ZIPCODES` 우편번호(도로명) 검색 API 단독**으로 범위를 한정한다 — 배송지 CRUD·화면 쪽 파일은 만들거나 고치지 않는다.

- **파일**:
  - 신규
    - `modules/shop-api/src/main/resources/db/V8__zipcodes.sql` — `ZIPCODES` 테이블 DDL(`CREATE TABLE IF NOT EXISTS`, DROP 없음, 규칙 `ddl-idempotent`) + 시드 100건(`INSERT IGNORE`, 아래 "데이터" 절 — `V3~V7`과 동일 네이밍 관례, 마이그레이션 엔진 없음).
    - `modules/shop-api/src/main/java/com/sm/lab/shop/domain/Zipcode.java` — 도메인 클래스(매퍼 `resultType`용, `MemberAddress` 관례와 동일하게 resultMap 없이 필드명 매칭). 필드는 SELECT 컬럼과 정확히 일치(`zipcode`, `roadAddress`, `sido`, `sigungu`) — PK(`zipcodeId`)는 API 응답에 노출할 이유가 없으므로 이 클래스에 아예 두지 않는다(select 안 하는 필드를 클래스에 남기면 항상 null인 유령 필드가 응답에 섞인다).
    - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/ZipcodeDao.java` — MyBatis `@Mapper` 인터페이스, 메서드 1개(`search`).
    - `modules/shop-api/src/main/resources/mapper/zipcode.xml` — 매퍼 XML(네임스페이스 `com.sm.lab.shop.dao.ZipcodeDao`, 컬럼 명시 — `no-select-star`).
    - `modules/shop-api/src/main/java/com/sm/lab/shop/service/ZipcodeService.java` — 검색어 검증(2자 미만 400) + DAO 위임. 트랜잭션 불필요(단일 SELECT, `MemberAddressService.list()`와 동일 판단).
    - `modules/shop-api/src/main/java/com/sm/lab/shop/service/ZipcodeApiException.java` — `RuntimeException`(HttpStatus + code + message), `MemberAddressApiException`과 동일 house 패턴이나 컨트롤러 스코프 advice 원칙상(project-context.md Critical Rule 2) 재사용하지 않고 이 FUNC 전용으로 별도로 둔다.
    - `modules/shop-api/src/main/java/com/sm/lab/shop/controller/ZipcodeController.java` — `@RestController @RequestMapping("/api/zipcodes")`, `GET ""` 단일 엔드포인트, `@RequestParam(required=false) String q`.
    - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ZipcodeExceptionHandler.java` — `@RestControllerAdvice(assignableTypes = ZipcodeController.class)`(컨트롤러 스코프 한정), `ZipcodeApiException`→봉투, `DataAccessException`→500 `MBR-5000`(기존 코드 재사용, `MemberAddressExceptionHandler`와 동일 패턴 — 로그에 원본, 응답엔 정제 메시지만).
  - 수정
    - `modules/shop-api/src/main/resources/application.yml` — `spring.sql.init.schema-locations` 목록 맨 끝에 `classpath:db/V8__zipcodes.sql` **한 항목만 이어붙인다**(기존 7개 항목 순서·값은 그대로, `V3~V7` 추가 이력과 동일한 이어붙이기 관례 — 목록 위 주석 블록에도 `linked_func: FUNC-member-012` 한 줄 추가). **주의**: 이 목록은 명시적 나열(`classpath:db/*.sql` 와일드카드 아님)이라 여기 추가하지 않으면 새 DDL/시드가 기동 시 전혀 실행되지 않는다(사전 확인 완료 — `ApiKeyAuthFilter` 등 다른 파일은 건드리지 않는다, 아래 "순서·보안" 절).
    - `ApiKeyAuthFilter.java`는 **건드리지 않는다** — 아래 "순서·보안" 절에서 그 이유(기존 default-ALLOW 분기가 이미 이 경로를 통과시킴)를 코드 추적으로 확인했다.
  - 테스트(신규)
    - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/ZipcodeControllerTest.java`
    - `modules/shop-api/src/test/java/com/sm/lab/shop/service/ZipcodeServiceTest.java`
    - `modules/shop-api/src/test/java/com/sm/lab/shop/dao/ZipcodeDaoTest.java`

- **데이터**:
  - DDL(`V8__zipcodes.sql`, 멱등):
    ```sql
    CREATE TABLE IF NOT EXISTS ZIPCODES (
      zipcode_id    BIGINT AUTO_INCREMENT PRIMARY KEY,
      zipcode       VARCHAR(10) NOT NULL,
      road_address  VARCHAR(200) NOT NULL,
      sido          VARCHAR(50) NOT NULL,
      sigungu       VARCHAR(50) NOT NULL,
      UNIQUE KEY uq_zipcodes_zipcode (zipcode)
    ) COMMENT='우편번호(도로명) 검색 샘플(SR-235, FUNC-member-012) — 실 API 연동 없이 로컬 시드 100건';
    ```
  - 시드: 같은 파일에 `INSERT IGNORE INTO ZIPCODES (zipcode, road_address, sido, sigungu) VALUES (...), ...;` 100건. `INSERT IGNORE` + `UNIQUE KEY uq_zipcodes_zipcode`가 재기동마다 같은 파일이 다시 실행돼도(`spring.sql.init.mode=always`) 중복 삽입되지 않게 한다 — 이 프로젝트 최초의 시드-데이터 스크립트라 `ddl-idempotent` 규칙의 "DDL 멱등"을 DML(시드)에도 동일 원리로 확장 적용한 것(규칙 자체는 DDL만 언급하지만 재기동마다 재실행되는 조건은 동일하게 성립 — `IF NOT EXISTS` 상당의 안전장치가 없으면 100건씩 계속 쌓인다).
  - `del_yn`·`created_at`/`updated_at` 컬럼을 두지 않는다 — 이 테이블은 애플리케이션이 쓰기(생성/수정/삭제)하지 않는 정적 참조 데이터라 이 코드베이스의 "회원/주문 소유 데이터" 관례(상시필터 `del_yn='N'`)가 적용될 대상이 아니다. 실 우편번호 API 연동(후속 SR)에서 갱신 전략이 바뀌면 그때 재설계한다.
  - `member_id` 연관 없음 — 이 테이블은 어떤 회원에도 속하지 않는 전역 참조 테이블이다(회원 스코프 판정 자체가 적용되지 않음, 아래 "순서·보안" 절).
  - DAO 메서드: `search(q)` — `SELECT zipcode, road_address, sido, sigungu FROM ZIPCODES WHERE road_address LIKE CONCAT('%', #{q}, '%') ORDER BY road_address LIMIT 50`(컬럼 명시 — `no-select-star`). `LIMIT 50`은 SR에 없는 Dev 판단(안전장치) — 시드가 100건뿐이라 실사용에 영향 없고, 결과 과다 응답을 원천 차단한다.
  - 트랜잭션 경계: **없음.** 단일 `SELECT` 조회뿐이라 `@Transactional`을 붙이지 않는다(`MemberAddressService.list()`와 동일 판단 — "트랜잭션 불필요(단순 조회)"). 락/원자 UPDATE 대상 없음(쓰기 자체가 없음).

- **순서·보안**:
  - 인증은 `ApiKeyAuthFilter`가 컨트롤러 도달 전에 이미 끝낸다. **필터를 손대지 않는다** — `/api/zipcodes`는 `MEMBERS_LIST_PATH`·`ORDERS_EXPORT_PATH`·`MEMBER_ME_ADDRESSES_PATH`·`MEMBERS_ITEM_PATH`·`MEMBER_VIEW_PATH`·`ORDER_RESOURCE_PATH` 어느 정규식과도 매치하지 않고, 쿼리 파라미터 `memberId`도 JSON 본문도 없으므로 `evaluateMemberScope`의 (a) 일반 규칙이 `queryMemberIds==null && bodyMemberId==null`로 끝까지 통과해 `Decision.ALLOW`를 반환한다(코드 추적 완료, `ApiKeyAuthFilter.java:333-354`). 즉 유효한 `X-Api-Key`(admin 또는 member 스코프 아무거나)만 있으면 통과한다 — SR 확정 문답의 "새 엔드포인트 `/api/zipcodes?q=`"에 `/api/members/me/addresses`와 달리 "(회원 토큰 인증)" 단서가 **붙지 않은 것**과 일치한다(회원별 자원이 아니라 공개 참조데이터 검색이라는 뜻으로 해석). 새 우회 경로·새 인증 로직을 추가하지 않는다.
  - 요청 검증 순서: 인증(필터, 기존 계약 그대로) → 검색어 검증(400 `MBR-4202`, DB 접근 전, `q`가 `null`이거나 trim 후 2자 미만) → 조회(캡 50건) → 응답.
  - 이 엔드포인트에는 "존재/소유" 판정이 없다(회원 소유 자원이 아닌 전역 참조데이터 검색이므로 404 개념 자체가 없음 — 결과 0건은 200 `{items: []}`로 응답, 화면 쪽 "검색 결과가 없습니다" 표기는 UIS 몫이지 API 오류가 아니다).
  - **부수효과(로그·발송·이벤트·감사) 없음** — 검색어를 감사 로그로 남기지 않는다(개인정보 아니지만 SR에 요구 없음, 범위 최소화).
  - 레이트리밋 없음(SR 요구 없음) — 대신 `LIMIT 50`(응답 크기 캡)과 인증 필수(무인증 401)가 사실상의 남용 억제선이다. 별도 레이트리밋 도입은 이 FUNC 범위 밖.

- **계약**:
  - 오류 코드(신규): `MBR-4202`(400, 검색어 2자 미만) 1개. `MBR-5000`(500)은 기존 코드 재사용(신규 아님).
  - 응답 봉투: `{items: [...]}`(프로젝트 공통 목록 관례). 0건이어도 200 + 빈 배열(404 아님).
  - 상태 코드: 검색 성공 200(0건 포함) · 검색어 미달 400.
  - 응답 항목 셰이프: `{zipcode, roadAddress, sido, sigungu}` — 4필드만, 내부 PK(`zipcodeId`)는 노출하지 않는다.

- **테스트**:
  - `ZipcodeControllerTest`(`@WebMvcTest(ZipcodeController.class)` + `@Import(AdminApiKeyTestConfig.class)` + `@MockBean(ZipcodeService)` — `controller-has-test` must 충족):
    1. `GET /api/zipcodes?q=강남`(기본 admin 키, `AdminApiKeyTestConfig` 자동 주입) → 서비스가 2건 반환하도록 스텁 → 200 + `{items:[...]}` 정확히 2건, 필드 셰이프 확인.
    2. `GET /api/zipcodes?q=강남` → 서비스가 빈 리스트 반환 → 200 + `{items: []}`(404 아님 명시적으로 확인).
    3. `GET /api/zipcodes?q=강` → 서비스가 `ZipcodeApiException(400, MBR-4202)`를 던지도록 스텁 → 400 `MBR-4202`.
    4. `GET /api/zipcodes`(q 파라미터 자체 없음) → 서비스가 같은 예외를 던지도록 스텁하고 컨트롤러가 `null`을 그대로 서비스에 넘기는지 `verify(service).search(isNull())`로 확인 → 400 `MBR-4202`(Spring 기본 "필수 파라미터 누락" 오류 봉투가 아니라 이 프로젝트 `{code,message}` 봉투로 나가는지 확인 — `@RequestParam(required=false)`라서 가능).
    5. `GET /api/zipcodes?q=강남`을 **member 키**(`X-Api-Key: lab-member-0001-key`)로 호출 → 200(이 엔드포인트가 회원 스코프 강제 없이 그대로 통과함을 확인 — `MemberAddressControllerTest`의 admin 키 400 케이스와 대칭되는 회귀 확인).
    6. 무인증(키 없음) 전역 401은 기존 `ApiKeyAuthIntegrationTest`가 이미 커버 — 중복 작성하지 않는다(`MemberAddressControllerTest` #12와 동일 판단).
  - `ZipcodeServiceTest`(Mockito, `ZipcodeDao` 목업 — `service-has-test` should, 신규 서비스라 작성):
    1. `q="강남"` → `dao.search("강남")` 호출, 결과 그대로 반환.
    2. `q=null` → `ZipcodeApiException(400, MBR-4202)`, `dao.search` 호출 안 됨(verify no interaction).
    3. `q=""`(빈 문자열) 또는 공백만(`"  "`) → 동일 400, dao 미호출.
    4. `q="a"`(trim 후 1자) → 400.
    5. `q="  강남  "`(앞뒤 공백 포함 4자, trim 후 2자) → 성공 경로, `dao.search("강남")`이 **trim된 값**으로 호출되는지 확인(원문 그대로 넘기지 않음).
    6. `dao.search`가 빈 리스트 반환 → 서비스도 빈 리스트 그대로 반환(예외 아님).
  - `ZipcodeDaoTest`(`@SpringBootTest`, 실 DB `sl_lab.ZIPCODES` — 매퍼 SQL 자체를 검증. Mockito로는 `LIKE`/`LIMIT`/컬럼 매핑을 증명할 수 없다, `MemberAddressDaoTest`와 동일 판단):
    1. 이 테스트 전용 행을 `@BeforeEach`에서 `JdbcTemplate`으로 직접 INSERT(운영 시드 문구에 의존하지 않기 위해 — 아래 "테스트 격리" 절). 고유 `zipcode`(`UNIQUE KEY` 충돌 방지, 예: `"9" + 임의 4자리`)와 식별 가능한 `road_address`(예: `"서울특별시 테스트로 1-" + UUID`)로 삽입.
    2. `dao.search(<그 UUID 마커>)` → 정확히 그 1행만 반환, 필드(`zipcode`/`roadAddress`/`sido`/`sigungu`) 값이 삽입값과 일치.
    3. `dao.search(<존재하지 않는 문자열>)` → 빈 리스트(예외 아님).
    4. `LIMIT 50` 검증 — 같은 마커 접두사를 공유하는 행 60개를 삽입(`road_address`에 공통 부분 문자열 + 개별 인덱스) → `dao.search(<공통 마커>)` 결과가 정확히 50건(60건 전체가 아님).
    5. `@AfterEach`에서 이 테스트가 삽입한 모든 행(마커 기준 `DELETE ... WHERE road_address LIKE '%마커%'`)을 정리한다 — 운영 시드 100건은 건드리지 않는다.

- **테스트 격리**: `ZipcodeDaoTest`는 운영 시드(V8의 100건)의 정확한 문구·지역명에 의존하지 않는다 — 시드 내용이 나중에 바뀌어도(예: 실 API 연동 후속 SR에서 시드를 다른 데이터로 교체) 이 테스트가 깨지지 않도록, 매 테스트가 **자기 전용 UUID 마커**가 포함된 행을 직접 삽입하고 `@AfterEach`에서 그 마커로 정리한다(SR-232 r2와 동일 계열 — 다만 여기서는 "카운터 누적"이 아니라 "고정 시드 문구에 대한 암묵적 결합"이 위험 지점이라 대응 형태가 다르다: 운영 시드는 절대 삭제하지 않고, 테스트가 만든 행만 지운다). `UNIQUE KEY uq_zipcodes_zipcode` 충돌을 피하기 위해 테스트 zipcode는 운영 시드 범위와 겹치지 않는 접두("9"로 시작 등)를 쓴다. `ZipcodeControllerTest`는 서비스가 목업이라 실 DB에 쓰지 않으므로 격리 이슈 없음.

- **폴백·우회 경로의 자격 판정**: 이 FUNC은 새로운 인증·조회 우회 경로를 열지 않는다. `ApiKeyAuthFilter`의 기존 default-ALLOW 분기를 그대로 타므로 회원 존재·탈퇴(`del_yn`) 여부와 무관하게 유효한 API 키만 있으면 통과한다 — 이는 신규 취약점이 아니라 **원래 회원 자원이 아닌 공개 참조데이터**라는 설계 의도와 일치한다(SR 확정 문답에 "(회원 토큰 인증)" 단서가 이 엔드포인트에만 빠진 것이 그 근거). `ZIPCODES` 자체에도 회원 식별 정보가 없어 유출 위험이 없다.

- **프레임워크 실행 모델 함정**: 없음 — 단일 `@Mapper` SELECT + 무상태 서비스, `@Transactional` 없음(self-invocation 함정 자체가 성립할 여지 없음), 스케줄러·리스너 없음.

- **범위 밖**:
  - `MEMBER_ADDRESSES` 배송지 CRUD API — FUNC-member-011이 이미 구현 완료(형제 FUNC, 이 STORY에서 다시 만들거나 고치지 않는다).
  - 마이페이지 배송지 관리 화면(shop-web, 우편번호 검색 모달 포함) — FUNC-member-010 몫.
  - 배송지 등록/수정 시 `zipcode`/`road_address`가 `ZIPCODES`와 실제로 일치하는지 서버측 대조 — FUNC-member-011 STORY가 이미 범위 밖으로 명시(클라이언트가 보낸 값을 그대로 저장). 이 FUNC(검색 API)도 그 대조를 추가하지 않는다 — 검색과 등록은 별개 흐름.
  - 실 우편번호 API(공공데이터포털 등) 연동 — 후속 SR.
  - `ZIPCODES` 관리(등록/수정/삭제) API — 이번 SR은 읽기 전용 검색만 요구, 관리 API는 만들지 않는다.
  - 검색 결과 정렬 고도화(관련도순·즐겨찾기·최근 검색어) — SR에 없음.

- **실패 사례집 대조** (`harness/antipatterns.all.md`):
  - SR-231 r4("한 FUNC의 필요로 전역 계약을 바꾸지 않는다") — `ApiKeyAuthFilter`는 여러 FUNC이 공유하는 전역 필터라 조건은 성립하지만, 이번엔 코드 추적 결과 **아무것도 바꿀 필요가 없다**(기존 default-ALLOW가 이미 이 경로를 정확히 처리함) — 필터에 손대지 않는 것 자체가 이 교훈을 지키는 방법이다.
  - `ddl-idempotent`("`CREATE TABLE`을 `IF NOT EXISTS` 없이 넣었다가 재기동에 죽음") — 조건 성립(신규 DDL) → `IF NOT EXISTS` 적용. 추가로 이 FUNC이 이 프로젝트 최초로 시드 DML을 도입하므로, 같은 "재기동마다 재실행" 조건이 INSERT문에도 성립함을 인지하고 `INSERT IGNORE` + `UNIQUE KEY`로 대응한다(사례집에 아직 없는 새 변종이라 여기 별도로 명시).
  - `no-select-star`(매퍼 XML 규칙) — 조건 성립(신규 매퍼) → 컬럼 명시로 대응.
  - SR-232 r3("인증 필요 신규 컨트롤러의 `@WebMvcTest`에 `AdminApiKeyTestConfig` 미 import") — 조건 성립(신규 컨트롤러가 `ApiKeyAuthFilter` 아래 놓임) → `ZipcodeControllerTest`에 `@Import(AdminApiKeyTestConfig.class)`를 반드시 포함한다(이 엔드포인트는 회원 스코프 강제가 없어 기본 admin 키만으로도 대부분 케이스가 통과하지만, 그 config를 빼면 필터가 슬라이스 테스트에도 Filter 빈으로 딸려와 401로 막힐 수 있다는 사고 조건 자체는 동일하게 성립).
  - SR-231 r5 계열("존재 판정을 분리하면 오라클이 생김") — 이 FUNC은 존재/소유 판정이 아예 없는 검색 API라 조건이 성립하지 않는다(공개 참조데이터, 404 개념 없음) — 그대로 옮겨 적지 않는다.
  - SR-232 r2("카운터/개수 제약 테스트가 청소 안 되면 다음 실행에 샌다") — 이 FUNC엔 카운터가 없어 그 조건은 성립하지 않지만, **같은 계열의 위험**(테스트가 만든 행이 다음 실행에 남아 간섭)은 `LIMIT 50` 검증 테스트(60행 삽입)에서 성립하므로 위 "테스트 격리" 절의 `@AfterEach` 마커 삭제로 대응한다.

### 사람 수정 (구현 계획 확인 게이트 회신 — 2026-09-13)

> 결정: **계획대로 진행** — 아래는 계획을 구체화하는 사람 지시. dev-agent는 이 절을 계획 본문과
> 함께 최우선으로 반영한다.

- **[결정 요약]** 범위를 ZIPCODES 검색 API 단독으로 한정한 것, 필터 무변경(코드 추적 근거), 시드
  INSERT IGNORE + UNIQUE 멱등, PK 비노출, LIMIT 50 — 전부 원안 그대로 승인.
- **[구현 방식 — 못 박는 것 3가지]**
  1. 검색어는 trim 뒤 연속 공백을 한 칸으로 정규화하고, 정규화 후 길이가 2~50자 범위 밖이면
     400 `MBR-4202`(message에 미달/초과 이유를 담는다).
  2. `q`가 숫자로만 구성되면 `zipcode` 전방 일치(`LIKE 'q%'`)로도 검색한다(사용자가 우편번호
     일부만 아는 경우 대응). 그 외에는 원안대로 `road_address LIKE '%q%'`. 정렬은
     `road_address` 오름차순, `LIMIT 50` 유지.
  3. 시드 100건은 실제 시도·시군구 분포를 흉내 낸다 — 서울·경기·부산·대구·인천 등 **5개 시도
     이상**, **시군구 10개 이상**, 도로명은 `"○○로 N길"` 형식. FUNC-member-010 화면 스토리와
     검색 e2e가 이 시드를 소비하므로 밋밋한 더미 문자열 반복 금지.
- **[보안 순서]** 인증(기존 필터, 무변경) → 검색어 검증(정규화+길이) → 조회 → 200
  `{items:[...]}`. 부수효과 없음. 무인증 401은 필터 기존 동작 — 테스트에서 단언(신규 필터 로직
  아님, 기존 계약 확인용).
- **[테스트·완료 조건]**
  - 컨트롤러: 2자 미만/50자 초과 400(`MBR-4202`) · 0건 200 빈 배열 · 무인증 401.
  - 서비스: 공백 정규화 · 숫자 판별(전방 일치 분기) 단위 테스트.
  - DAO(실 DB): 도로명 부분 일치 · 우편번호 전방 일치(숫자 검색어) · `LIMIT 50` · 응답 4필드
    셰이프.
  - `mvn test` 전량 통과, 기준선 483/0 유지(악화 없음).
- **[후속 SR·이월]** 실 우편번호 API 연동·레이트리밋은 후속 SR. 화면 검색 모달은
  FUNC-member-010 몫(이 FUNC 범위 아님).

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
- 계획 확인: 계획대로 진행(사람 수정 포함) — 검색어 정규화·숫자 전방일치·시드 분포 지시, 완료조건(기준선 483/0) (2026-09-13)
- 구현 완료(2026-09-13). 계획+사람 수정 그대로 구현. 계획과 다르게 간 지점 1건 — 아래 "계획 대비 변경" 참고.

### 생성/수정 파일
- 신규
  - `modules/shop-api/src/main/resources/db/V8__zipcodes.sql` — `ZIPCODES` DDL(`CREATE TABLE IF NOT EXISTS`) + 시드 100건(`INSERT IGNORE`). 시드는 5개 시도(서울특별시·경기도·부산광역시·대구광역시·인천광역시) × 시군구 12개(강남구·서초구·마포구·종로구·수원시·성남시·고양시·해운대구·부산진구·수성구·연수구·남동구), 도로명 "○○로 N길" 형식 — 사람 수정 (3) 충족.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/domain/Zipcode.java` — 4필드(zipcode/roadAddress/sido/sigungu), PK 비노출.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/ZipcodeDao.java` — `search(q, numeric)`.
  - `modules/shop-api/src/main/resources/mapper/zipcode.xml` — `<choose>`로 numeric 분기(컬럼 명시, LIMIT 50).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/ZipcodeService.java` — trim+연속공백 정규화 → 2~50자 검증(400 MBR-4202) → 숫자 판별(전방일치 분기) → DAO 위임.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/ZipcodeApiException.java`
  - `modules/shop-api/src/main/java/com/sm/lab/shop/controller/ZipcodeController.java` — `GET /api/zipcodes?q=`.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ZipcodeExceptionHandler.java` — `assignableTypes = ZipcodeController.class`, `MBR-5000` 재사용.
  - 테스트: `ZipcodeControllerTest`(6) · `ZipcodeServiceTest`(12) · `ZipcodeDaoTest`(6, 실 DB) — 계 24건.
- 수정
  - `modules/shop-api/src/main/resources/application.yml` — `schema-locations` 목록 끝에 `classpath:db/V8__zipcodes.sql` 1건 이어붙임(기존 7개 순서 무변경). `ApiKeyAuthFilter.java`는 계획대로 손대지 않음.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` — 아래 "계획 대비 변경" 참고. 2건 추가(`zipcodesRoute_withoutApiKey_returns401Unauthorized`, `zipcodesRoute_withMemberApiKey_returns200`).

### 계획 대비 변경 — 무인증 401 테스트 위치
계획의 "테스트" 절은 무인증 401을 "기존 `ApiKeyAuthIntegrationTest`가 이미 커버 — 중복 작성하지 않는다"고 적었으나, 사람 수정의 "테스트·완료 조건"이 "컨트롤러: … · 무인증 401"을 명시적으로 다시 요구했다. 실제로 확인해보니 `ApiKeyAuthIntegrationTest`는 `/api/zipcodes`가 아니라 `/api/orders` 등 다른 엔드포인트를 대표 프로브로 쓸 뿐 이 경로를 직접 검증하지 않았다 — 그리고 `ZipcodeControllerTest`(`@WebMvcTest` + `AdminApiKeyTestConfig` import)는 구조적으로 "무키" 상태를 만들 수 없다(`MockMvcBuilderCustomizer.defaultRequest`가 모든 요청에 헤더를 병합, 개별 요청이 값은 덮어써도 "헤더 없음"은 만들 수 없음 — `MemberAddressControllerTest`·`CartControllerTest`와 동일 하우스 판단, 그 주석에 이미 명시돼 있었다). 그래서 이 FUNC이 `ApiKeyAuthIntegrationTest`(실 서버, `TestRestTemplate`)에 `/api/zipcodes` 전용 무인증 401·member키 통과 테스트 2건을 새로 추가했다 — "중복 작성 금지"와 "무인증 401 단언" 두 지시를 모두 만족시키는 유일한 방법이었다.

### 검증
- `mvn test -Dtest=ZipcodeServiceTest,ZipcodeControllerTest,ZipcodeDaoTest` — 24/24 통과.
- `mvn test -Dtest=ApiKeyAuthIntegrationTest` — 59/59 통과(신규 2건 포함).
- `mvn test`(전량) — 509 실행/0 실패/0 오류(기존 기준선 483 + 신규 26 = 509, 무회귀).

## round2 재작업 완료(2026-09-13)

QA round1 FAIL 필수 4건(1 mojibake 시드, 2 회귀 테스트 부재, 3 zipcode.xml 배타분기, 4 DaoTest
카운터 잔여행 위험) 전부 반영. 권고 2건(LIKE 이스케이프·시드 분포 편중) 중 이스케이프는 사람
코멘트가 요구하지 않았고 story "권고" 자체가 "고칠 때는 zipcode.xml 단독이 아니라 product.xml과
같은 변경에서"로 이 FUNC 범위 밖임을 명시해 이번 라운드에 반영하지 않았다(전역 패턴을 한 FUNC
편의로 갈라놓지 않는다 — SR-231 r4 계열, 재작업 지시 5번과 동일 판단).

### 근본 원인
`spring.sql.init.encoding`이 지정되지 않아, Java 17 + Windows(ko_KR) 환경에서 Spring Boot가
`db/*.sql` 초기화 스크립트를 JVM 플랫폼 기본 charset(MS949)으로 읽었다. V8이 이 프로젝트 최초로
한글 *데이터*를 기동 스크립트에 넣은 파일이라(V3~V7의 한글은 주석뿐) 잠재 결함이 데이터 손상으로
드러났다. `INSERT IGNORE` + `UNIQUE(zipcode)` 조합은 이미 깨진 행을 절대 덮어쓰지 않아, 인코딩만
고쳐도 기존 mojibake 100행은 그대로 남는 2차 문제가 있었다.

### 수정 파일
- `modules/shop-api/src/main/resources/application.yml` — `spring.sql.init.encoding: UTF-8`
  명시 추가(schema-locations 목록 전체에 적용, V3~V7은 ASCII/영문이라 영향 없음 확인). datasource
  URL의 `useUnicode=true&characterEncoding=utf8`는 기존에 이미 있었다(변경 없음).
- `modules/shop-api/src/main/resources/db/V8__zipcodes.sql` — 시드 INSERT를 `INSERT IGNORE`에서
  `INSERT ... ON DUPLICATE KEY UPDATE road_address=VALUES(road_address), sido=VALUES(sido),
  sigungu=VALUES(sigungu)`로 변경. 재기동 1회로 기존 mojibake 100행이 정본 값으로 수렴한다(멱등
  유지, DROP 없음 — `ddl-idempotent` 준수).
- `modules/shop-api/src/main/resources/mapper/zipcode.xml` — `<choose>` 배타 분기를 제거하고
  `WHERE road_address LIKE '%q%'`에 `<if test="numeric">OR zipcode LIKE 'q%'</if>`를 추가(포함
  관계로 정정, 사람 수정 (2) "전방일치로도 검색한다" = 추가이지 대체가 아님).
- `modules/shop-api/src/test/java/com/sm/lab/shop/dao/ZipcodeDaoTest.java`:
  - `search_numericTrue_doesNotMatchByRoadAddress`(D04, 배타 기대)를
    `search_numericTrue_alsoMatchesByRoadAddress`(OR 기대, road_address만 일치해도 잡힘)로 정정.
  - `@BeforeEach cleanStaleRowsFromPreviousRuns()` 추가 — `DELETE FROM ZIPCODES WHERE zipcode
    LIKE '9%'`로 매 테스트 시작 전 이 클래스 전용 영역을 선청소한다. 종전엔 static
    `AtomicInteger`가 JVM마다 0에서 재시작해, 이전 실행이 `@AfterEach` 전에 중단되면 잔여 "9%"
    행과 다음 실행의 같은 카운터 값이 `UNIQUE KEY` 충돌을 일으켜 plain INSERT가 하드 실패할
    위험이 있었다(SR-232 r2 계열) — 이 위험을 제거했다.
  - `seedData_hasExpectedDistributionAndIsReadableAsKorean`(D07, 신규) 추가 — 운영 시드(zipcode
    NOT LIKE '9%') 기준 행 수 ≥100·`sido` distinct ≥5·`sigungu` distinct ≥10·`road_address`가
    `'%로 %길%'` 패턴을 만족하는 행 존재를 단언하고, `dao.search("강남", false)`·
    `dao.search("테헤란로", false)`가 비어 있지 않으며 결과에 치환 문자(U+FFFD)가 없음을
    단언한다 — 이 인코딩 회귀를 잡는 유일한 테스트. 정확한 문구가 아닌 분포·왕복 수준으로만
    단언해 story "테스트 격리"(운영 시드 문구 비의존) 원칙과 충돌하지 않는다.

### 검증
- `mvn test -Dtest=ZipcodeServiceTest,ZipcodeControllerTest,ZipcodeDaoTest` — 25/25 통과
  (ZipcodeDaoTest 6건 → 7건, D07 추가).
- `mvn test`(전량) — **510 실행/0 실패/0 오류**(기존 기준선 483 + 신규 27 = 510, 무회귀 —
  round1 대비 신규 테스트 1건 증가는 D07 추가분).
- 앱 재기동(기존 프로세스 kill 후 `mvnw spring-boot:run` 재기동, 8087) 후 실측:
  - `GET /api/zipcodes?q=강남`(admin 키) → **HTTP 200**, `items` **26건** 반환(예:
    `{"zipcode":"10000","roadAddress":"서울특별시 강남구 봉은사로 33길 99","sido":"서울특별시",
    "sigungu":"강남구"}` 등, mojibake 없이 한글 그대로).
  - `GET /api/zipcodes?q=16`(숫자 검색어) → **HTTP 200**, `items` **4건**(`road_address`에 "16"이
    포함된 "일산로 16길"·"호수로 16길"·"강남대로 4길 16"·"테헤란로 16길" — zipcode 전방일치
    대상이 아닌 행도 road_address로 잡혀 OR 동작 확인).
  - `SELECT road_address FROM ZIPCODES WHERE zipcode='10000'`(DB 직접 조회, 재기동 전/후 대조) —
    재기동 전 `'�꽌�슱�듅蹂꾩떆 媛뺣궓援� …'`(mojibake) → 재기동 후 `'서울특별시 강남구
    봉은사로 33길 99'`(정상), `ON DUPLICATE KEY UPDATE`가 기존 100행을 정본 값으로 수렴시킴을
    실측 확인.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-13 — FAIL
- Layer1 스펙: **fail** — 계획·사람 수정(1)(2)(3) 중 (1) 정규화·길이(2~50, 이유 담긴 메시지)는 코드로 정확히 확인됐다. 그러나 **(3) 시드 100건이 실제 DB에서 깨진 문자로 저장돼 한글 검색이 전혀 되지 않는다**(실측: `SELECT COUNT(*) FROM ZIPCODES WHERE road_address LIKE '%강남%'` → **0**, `'%테헤란로%'` → **0**, 저장값은 `'�꽌�슱�듅蹂꾩떆 媛뺣궓援� 遊됱��궗濡� 33湲� 99'`). 원인은 `spring.sql.init.encoding` 미지정 — Java 17 + Windows(ko_KR)에서 JVM 기본 charset(MS949)으로 UTF-8 스크립트를 읽어 mojibake 문자열이 그대로 INSERT됐다. `information_schema` 대조로 확인: 기동 스크립트로 만든 `ZIPCODES`·`MEMBER_ADDRESSES`의 table_comment는 깨졌고, 스크립트 밖에서 만든 `PRODUCTS`는 정상('상품 마스터'). V8은 이 프로젝트 **최초로 한글 *데이터*를 기동 스크립트로 넣은 파일**이라(V3·V4의 INSERT 언급은 주석뿐) 종전까지는 주석만 깨져 무해했던 잠재 결함이 이번에 데이터 손상으로 드러났다. 사람 수정 (3)이 "FUNC-member-010 화면 스토리와 검색 e2e가 이 시드를 소비"라고 못 박은 만큼 시드 가용성은 이 FUNC의 산출물 계약이다. 또한 **(2) 숫자 검색어 분기가 배타(`<choose>`)로 구현**돼 사람 수정의 "zipcode 전방 일치로**도** 검색한다"와 어긋난다. 파일 13건 전부 Read 대조 — 나머지(응답 봉투 `{items:[...]}`·4필드 셰이프·PK 비노출·0건 200·`MBR-4202`·`MBR-5000` 재사용·`ORDER BY road_address`·`LIMIT 50`·`application.yml` 기존 7항목 무변경)는 계획대로다.
- Layer2 보안: **pass** — `ApiKeyAuthFilter` 무변경을 파일로 확인(FUNC-member-012 언급 없음). 필터 코드 추적 재검증 결과 `/api/zipcodes`는 `isOpenRoute` 화이트리스트 밖이라 무키 401(default-deny)이고, member 키는 `evaluateMemberScope` (a) 일반 규칙에서 `memberId` 토큰이 없어 ALLOW — 계획의 주장과 일치하며 새 우회 경로가 없다. 주입 없음(`#{}` 파라미터 바인딩), 예외 핸들러는 컨트롤러 스코프 한정·SQL/경로 비노출·로거 사용(`no-printstacktrace`/`no-sysout` 충족), 검색어 감사 로그 없음, 회원 식별 정보 없는 전역 참조 테이블이라 IDOR 표면 없음. `LIKE` 특수문자(`%`/`_`) 이스케이프가 없지만 `product.xml`과 동일한 기존 하우스 패턴이고 공개 데이터 + `LIMIT 50`이라 영향은 낮다(권고).
- Layer3 회귀: **concerns** — 기존 계약 변경 없음(추가만), `schema-locations` 기존 7항목·순서 무변경, `MEMBER_ADDRESSES`/shop-web 무변경으로 형제 FUNC 범위 침범 없음, `ApiKeyAuthIntegrationTest` 추가 2건은 기존 테스트를 고치지 않고 덧붙였다. 다만 **테스트 24+2건이 전부 통과하는데도 Layer1의 치명 결함을 아무도 잡지 못했다** — 운영 시드 내용을 단언하는 테스트가 하나도 없고(`ZipcodeDaoTest`는 자기 행만 삽입·검증, `zipcodesRoute_withMemberApiKey_returns200`은 상태코드만 단언) `mvn test` 509/0은 이 결함에 구조적으로 눈이 멀다. 추가로 `ZipcodeDaoTest`의 고정 카운터 zipcode(`90001`…, JVM마다 0에서 재시작 + plain `INSERT`)는 중단된 실행이 남긴 잔여 행과 `UNIQUE KEY` 충돌로 다음 실행을 깨뜨릴 수 있다(SR-232 r2 계열).
- 필수 수정(FAIL시):
  1. **시드 인코딩 고정** — `application.yml`에 `spring.sql.init.encoding: UTF-8`을 명시한다(플랫폼 기본 charset에 의존하지 않는다). V3~V7은 `IF NOT EXISTS`라 재실행돼도 구조 변화가 없어 안전하지만, 이 키가 초기화 스크립트 전체에 적용되는 설정임을 주석에 남긴다.
  2. **이미 깨진 100행 복구** — `INSERT IGNORE` + `UNIQUE(zipcode)` 조합은 zipcode 10000~10099가 이미 있으므로 인코딩만 고쳐도 **기존 mojibake 행을 절대 덮어쓰지 않는다**. 시드를 `INSERT … ON DUPLICATE KEY UPDATE road_address=VALUES(road_address), sido=VALUES(sido), sigungu=VALUES(sigungu)`로 바꿔 재기동마다 정본 값으로 수렴하게 한다(정적 참조 데이터라 이 형태가 `INSERT IGNORE`보다 옳다 — 멱등 유지, DROP 없음, `ddl-idempotent` 준수).
  3. **시드 계약 테스트 추가** — 실 DB 기준으로 ① 행 수 ≥100 ② `COUNT(DISTINCT sido)` ≥5 ③ `COUNT(DISTINCT sigungu)` ≥10 ④ `dao.search("강남", false)`가 비어 있지 않음(한글 왕복 검증) ⑤ `road_address`가 `'%로 %길%'` 형식을 만족을 단언한다. 계획의 "테스트 격리(운영 시드 문구 비의존)"와 충돌하지 않게 **정확한 문구가 아닌 분포·왕복 수준**으로만 단언하고, 이 클래스는 운영 시드를 삭제·수정하지 않는다.
  4. **숫자 검색어 분기를 포함(OR)으로 정정** — 사람 수정 (2)는 "zipcode 전방 일치로**도** 검색한다"(추가)이지 "road_address는 보지 않는다"(대체)가 아니다. 현재 `<choose>`는 배타라 `q=16`이면 `'테헤란로 16길 10'`이 있어도 결과가 0건이다(시드 zipcode가 전부 `100xx`). `WHERE (zipcode LIKE CONCAT(#{q},'%') OR road_address LIKE CONCAT('%',#{q},'%'))`로 바꾸고, 이 배타 동작을 못 박아 둔 `ZipcodeDaoTest.search_numericTrue_doesNotMatchByRoadAddress`(D04)를 함께 정정한다 — 서술이 아니라 사람 확정 지시가 우선이므로 코드가 아니라 테스트를 지시에 맞춘다(SR-232 r3 교훈). 해석이 반대라면(배타가 의도) 그 근거를 STORY에 명시해 다음 라운드에 다시 묻지 않게 한다.
- 권고(CONCERNS시):
  1. `ZipcodeDaoTest`가 잔여 행에 강건하도록 `@BeforeEach`에서 `DELETE FROM ZIPCODES WHERE zipcode LIKE '9%'`로 자기 영역을 선청소하거나 `INSERT IGNORE`를 쓴다(현재는 JVM마다 같은 zipcode를 재사용하는 plain INSERT라 중단된 실행 뒤 하드 실패).
  2. `LIKE` 검색어의 `%`·`_` 이스케이프(`ESCAPE` 절) — `q="%%"`(2자, 검증 통과)가 전체 행을 매치한다. 공개 데이터 + `LIMIT 50`이라 피해는 없고 `product.xml`도 동일 패턴이므로, 고칠 때는 이 FUNC 단독이 아니라 두 매퍼를 같은 변경에서 다룬다(한 FUNC의 필요로 전역 패턴을 갈라놓지 않는다 — SR-231 r4 계열).
  3. 시드 분포가 강남구에 편중돼 있다(100건 중 23건, 대부분 테헤란로). 사람 수정 (3)의 하한(시도 5·시군구 12)은 충족하므로 차단 사유는 아니다.

### QA Gate — 2026-09-13 — round 2 — CONCERNS
- Layer1 스펙: **pass** — round1 FAIL 4건을 보고가 아니라 실측으로 재확인했다. ① `application.yml:52` `encoding: UTF-8` 실재(주석에 적용 범위·V3~V7 무영향 근거 포함), ② `V8__zipcodes.sql:38,139-142`가 `INSERT … ON DUPLICATE KEY UPDATE road_address/sido/sigungu=VALUES(...)`로 전환, ③ **실 DB 대조** — `ZIPCODES` 100행, `road_address LIKE '%강남%'` → **26**(round1 0), `'%테헤란로%'` → **18**(round1 0), `COUNT(DISTINCT sido)`=**5**, `COUNT(DISTINCT sigungu)`=**12**, `'%로 %길%'`=**100**, 저장값 `'서울특별시 강남구 봉은사로 33길 99'`(정상 한글). ④ `zipcode.xml:18-21`이 `<choose>` 배타 → `WHERE road_address LIKE '%q%'` + `<if test="numeric">OR zipcode LIKE 'q%'</if>` 포함 관계로 전환(사람 수정 (2) "로도" 해석 일치). **라이브 API 실측**(앱 8087, admin 키): `q=강남` → 200 `items` 26건·4필드 셰이프·한글 정상, `q=16` → 200 **4건**(`일산로 16길 77`·`호수로 16길 76`·`강남대로 4길 16`·`테헤란로 16길 10` — zipcode가 전부 `100xx`라 전방일치 대상이 아닌데도 road_address로 잡힘 = OR 동작 증명, round1 0건 결함 해소), `q=a` → 400 `{"code":"MBR-4202","message":"검색어는 최소 2자 이상이어야 합니다"}`, 51자 → 400. 정규화(trim+연속공백)·숫자판별·PK 비노출·0건 200은 round1 판정 유지.
- Layer2 보안: **pass** — 인증 경로 무변경을 라이브로 재확인(키 없음 → **401**, member 키 → **200**). `#{}` 파라미터 바인딩 유지(주입 없음), 예외 핸들러 컨트롤러 스코프 한정·SQL/경로 비노출·slf4j 사용. round2가 건드린 것은 스크립트 인코딩·시드 upsert·매퍼 WHERE절뿐으로 인증·인가 표면에 손대지 않았다. `LIKE` 와일드카드 미이스케이프는 그대로 남아 있고 실측으로 재확인했다(`q=%%` → 200 **50건** = 전체 행) — 사람 코멘트가 "고칠 때는 `product.xml`과 같은 변경에서"로 이 FUNC 범위 밖임을 확정했으므로 차단하지 않는다(이월).
- Layer3 회귀: **concerns** — `rules_check.py` must 위반 **0**(잔여 should 1건은 이 FUNC이 건드리지 않은 `ApiKeyAuthFilter` 450줄 상한, 기존분). **Zipcode 3개 테스트 클래스 독립 재실행 25/25 통과**(Controller 6·Dao 7·Service 12), **전량 `mvn test` 510 실행/0 실패/0 오류**(Dev 보고와 일치, 무회귀). `ZipcodeDaoTest`의 잔여행 위험은 `@BeforeEach cleanStaleRowsFromPreviousRuns()`(`DELETE … WHERE zipcode LIKE '9%'`)로 해소됐고 운영 시드(`1` 접두)를 건드리지 않음을 코드로 확인. 시드 계약 테스트 D07은 `@SpringBootTest`가 같은 `application.yml`을 로드해 테스트 JVM에서도 `sql.init`이 V8을 재실행하므로 **자기충족적**이고, 인코딩이 되돌아가면 upsert가 행을 다시 깨뜨려 `dao.search("강남")` 단언이 즉시 실패한다 — 회귀 검출력 있음. 남은 우려는 전부 low: **① `ZIPCODES` table_comment가 여전히 mojibake**(`information_schema` 실측: `'�슦�렪踰덊샇(�룄濡쒕챸) 寃��깋 …'`) — `CREATE TABLE IF NOT EXISTS`는 이미 존재하는 테이블에 `COMMENT`를 재적용하지 않아 데이터는 수렴했지만 메타데이터는 round1 상태로 남았다. API·AC·테스트에 영향은 없으나 이 랩의 `/sl-recon-sch`가 information_schema 코멘트를 SCH 본문으로 읽으므로 파생 산출물에 깨진 문자가 실린다. ② D07의 `doesNotContain("�")`(U+FFFD)는 round1의 실제 손상(MS949 오독 → 유효한 한글 쓰레기)을 못 잡는다 — 실제로 작동하는 단언은 `search("강남"/"테헤란로") isNotEmpty` 쪽이다(무해하나 커버리지 착시). ③ `zipcode.xml`의 OR에 괄호가 없다 — 현재는 술어가 둘뿐이라 정확하지만, 뒤에 `AND` 한 줄이 추가되면 조용히 의미가 바뀐다. ④ `.speclinker/test_baseline.json`의 shop-api `executed`가 **449**(HEAD 0d8ddb6 기록)로 이 FUNC 완료조건이 말하는 483과도, 현재 510과도 어긋난다 — 이 FUNC이 만든 격차가 아니고(현재 수 > 기준선이라 게이트 위반도 아님) SR 종결 시 재기록 대상이다.
- 필수 수정(FAIL시): 없음 — round1 필수 4건 전부 실측 확인 완료(차단 이슈 0).
- 권고(CONCERNS시):
  1. `V8__zipcodes.sql` 끝에 `ALTER TABLE ZIPCODES COMMENT = '...';` 한 줄을 덧붙여 테이블 코멘트를 정본으로 수렴시킨다(`ALTER … COMMENT`는 같은 값을 재실행해도 무해 = 멱등, DROP 없음 → `ddl-idempotent` 준수). 형제 테이블(`MEMBER_ADDRESSES`·`MEMBER_PASSWORD_RESETS`·`MEMBER_REFRESH_TOKENS`)도 같은 잔여물을 갖고 있으나 **다른 FUNC 소유라 이 FUNC에서 고치지 않는다** — 별도 정리 SR로(한 FUNC의 필요로 남의 DDL을 손대지 않는다, SR-231 r4 계열).
  2. D07의 U+FFFD 단언은 유지해도 무해하나 그것을 인코딩 회귀의 방어선으로 오해하지 않도록 주석에 "실제 검출은 한글 왕복 검색 단언이 한다"를 한 줄 남긴다.
  3. `zipcode.xml`의 `WHERE`를 `WHERE (road_address LIKE … <if>OR zipcode LIKE …</if>)`로 괄호를 명시한다(현재 동작 불변, 향후 `AND` 추가 시의 조용한 의미 변화 차단).
  4. SR-235 종결 시 `test_baseline_ws.py record`로 shop-api 기준선을 510으로 재기록한다(이 FUNC 밖 후속 작업).
  5. `LIKE` 와일드카드 `ESCAPE` 절 — 사람 결정대로 `product.xml`과 같은 변경에서 다룬다(이월, 이 FUNC 범위 밖).

### QA Gate — 2026-09-19 — round 3 — CONCERNS
- Layer1 스펙: **pass** — 사람이 승인한 3건이 지시된 그대로, 그 이상도 이하도 아니게 반영됐다(보고가 아니라 파일·DB·라이브 실측으로 대조).
  ① `V8__zipcodes.sql:146` — `ALTER TABLE ZIPCODES COMMENT='우편번호(도로명) 샘플';`가 파일 끝에 실재하고 문자열이 사람 구현방식 (1)과 **정확히 일치**한다(근거 주석 4줄 동반). 멱등·`DROP` 없음 → `ddl-idempotent` 준수. **DB 실측**: `information_schema.TABLES.TABLE_COMMENT`(`sl_lab.zipcodes`) = `'우편번호(도로명) 샘플'` — round2의 mojibake(`'�슦�렪踰덊샇(…'`)가 해소됐다. 저장된 값이 `CREATE TABLE`의 긴 코멘트가 아니라 **ALTER의 짧은 값**이라는 사실 자체가 ALTER가 실제로 실행됐다는 증거다(권고1 목적 달성).
  ② `ZipcodeDaoTest.java:194-196` — U+FFFD 단언 블록 바로 위에 "실제 인코딩 손상 검출은 한글 왕복 검색(강남/테헤란로) 단언이 한다" 주석 3줄. git diff가 **+3/-0**이라 `doesNotContain`·`isNotEmpty` 단언은 한 글자도 바뀌지 않았음이 기계적으로 증명된다(권고2 = 동작 불변 요구 충족).
  ③ `zipcode.xml:18,22` — `WHERE (road_address LIKE … <if test="numeric">OR zipcode LIKE …</if> )`로 괄호 명시(+1/-1). 술어·분기 조건·`ORDER BY road_address ASC`·`LIMIT 50` 전부 불변이며, `numeric` 참/거짓 양쪽 모두 생성 SQL이 유효하고 의미가 동일하다(권고3).
- Layer2 보안: **pass** — 이번 라운드가 건드린 3개 파일에 인증·인가·예외 핸들러 코드가 전혀 없어 보안 표면 변화가 구조적으로 없다. 그래도 라이브로 재확인: 무키 → **401**, member 키 → **200**, admin 키 → **200**(round2와 동일). `#{}` 파라미터 바인딩 유지 — 추가된 괄호는 `<if>` 바깥의 정적 리터럴이라 주입 표면을 만들지 않는다. 부수효과·감사 로그 없음. `LIKE` 와일드카드 미이스케이프(`q=%%` → 200 **50건** = 전체 행)는 round2와 동일하게 잔존하나, 사람이 "`product.xml`과 같은 변경에서"로 이월을 확정한 항목이므로 차단하지 않는다.
- Layer3 회귀: **concerns** — 이 FUNC 자체의 회귀는 **0**이고, 남은 우려는 전부 이 FUNC 밖의 환경 항목이다.
  - **"동작 불변" 주장 실증**: Zipcode 3개 클래스 독립 재실행 **25/25 통과**(Controller 6 · Dao 7 · Service 12, round2와 동일 건수). 라이브 API 결과가 round2 수치와 **완전 일치** — `q=강남` **26건**, `q=테헤란로` **18건**, `q=16` **4건**(`일산로 16길 77`·`호수로 16길 76`·`강남대로 4길 16`·`테헤란로 16길 10` — zipcode 전방일치 대상이 아닌 행이 road_address로 잡힘 = OR 포함관계가 괄호 추가 후에도 보존됨), `q=서초` 6건, `q=a` → 400 `{"code":"MBR-4202"}`. DB 시드도 무변화(100행 · `sido` 5 · `sigungu` 12 · `'%로 %길%'` 100 · 테스트 잔여행 `zipcode LIKE '9%'` **0**). → ②③의 "동작 불변"은 검증됐다.
  - **스코프**: `modules/shop-api`(자체 git 저장소 — 워크스페이스 저장소에서 `modules/`는 `.gitignore` 대상이라 루트 `git diff`로는 보이지 않는다) `git status` 결과가 수정 **정확히 3개 파일 / +11 -1**. 사람이 범위 밖으로 못 박은 것(권고4 ESCAPE · 권고5 기준선 재기록 · 형제 테이블 COMMENT)은 전부 미변경. 참고: 형제 테이블(`member_addresses`·`member_password_resets`·`member_refresh_tokens`) 코멘트가 현재 DB에서 정상 한글인 것은 **dev가 손댄 결과가 아니라** 랩 스냅샷 복원으로 테이블이 UTF-8 수정 이후 새로 생성됐기 때문이다(해당 DDL 파일 diff 0) — 스코프 이탈 아님.
  - `rules_check.py` — must **0** · should **0**.
  - **전량 `mvn test` 독립 재실행: 573 실행 / 3 실패 / 0 오류** — dev 보고 수치와 정확히 일치(보고 신뢰 확인). 실패 3건이 이 FUNC과 무관한 SR-300 시간 의존 이슈임을 **코드+데이터 산술로 확정**했다: ① 실패 지점은 `OrderListEndToEndIntegrationTest:50,62`와 `ApiKeyAuthIntegrationTest:642`이고 세 테스트 모두 **날짜 파라미터 없이** 주문을 조회한다(`get("/order/list").param("memberId", …)`, `get("/api/orders")`). ② `OrderService.java:63`이 `LocalDate.now().minusDays(30)`을 기본 시작일로 쓴다. ③ DB 실측 — `ORDERS` 5행의 `ordered_at`이 `2026-08-15 ~ 2026-08-17`이고 `CURDATE()-INTERVAL 30 DAY` = **2026-08-20**이라 기본 창 안에 드는 주문이 **0건**이다(단언 대상 주문번호가 `20260815-0001`·`20260816-0002`·`20260817-0001`로 날짜를 그대로 담고 있다). ④ 따라서 최신 주문 2026-08-17 기준 **2026-09-17부터** 항상 실패한다 — round2 게이트(2026-09-13, 510/0 통과)와 round3(2026-09-19) 사이에 **달력이 이 절벽을 넘은 것**이지 이번 3줄 변경이 만든 회귀가 아니다. `CLAUDE.md:22`·`harness/antipatterns.all.md:30`에 `SR-300 이월`로 이미 문서화된 항목이며, `ApiKeyAuthIntegrationTest`는 67건 중 저 1건만 실패해 이 FUNC이 round1에 추가한 zipcode 2건(무키 401·member키 200)은 통과한다.
  - 다만 사람 완료조건 "`mvn test` 전량 통과"는 **문자 그대로는 미충족**이다(실패 3건). 원인이 이 FUNC 밖이고 dev-agent가 범위 안에서 고칠 수 없는 항목(주문 시드·타 FUNC 소유)이라 FAIL로 돌리지 않되, 이후 모든 SR의 기준선 게이트가 같은 실패로 멈추므로 사람 판단이 필요하다 — 아래 권고1.
- 필수 수정(FAIL시): 없음 — 사람이 승인한 3건 전부 실측 확인 완료, 차단 이슈 0.
- 권고(CONCERNS시):
  1. **[env·이 FUNC 밖] SR-300 시간 폭탄이 실제로 터졌다** — 주문 시드 3건이 기본 조회창 밖으로 밀려 통합테스트 3건이 **날마다 항상** 실패한다(2026-09-17부터). 시드를 상대 날짜(`CURDATE() - INTERVAL n DAY`)로 바꾸거나 해당 테스트의 시계를 고정하는 별도 작업을 SR-300 이월분으로 올린다. 방치하면 다음 SR의 기준선 게이트가 전부 이 3건으로 막힌다.
  2. **[env·이 FUNC 밖] `modules/shop-api/mvnw.cmd` 래퍼가 존재하지 않는 경로를 가리킨다** — 내용이 `call "{{PLUGIN_PATH}}\lab\.runtime\apache-maven-3.9.9\bin\mvn.cmd" %*`인데 실제 Maven 런타임은 `{{PLUGIN_PATH}}\lab\.runtime\apache-maven-3.9.9\`에 있다. 그대로 실행하면 "지정된 경로를 찾을 수 없습니다"로 즉사한다(이번 게이트도 실 런타임 절대경로로 우회해 검증했다). 오늘 커밋된 `22cfc7f lab 운영: Windows 앱 실행 계약(APP_RUN_CMD=mvnw.cmd)`이 `CLAUDE.md`가 문서화한 실행 계약을 깨 놓은 상태라, 다음 세션·에이전트가 테스트를 못 돌린다. 경로를 실 런타임으로 정정한다.
  3. **[low·후속 TODO] `ZIPCODES` 코멘트의 추적성 상실** — 같은 파일이 이제 같은 테이블에 두 코멘트를 쓴다: `CREATE TABLE`은 `'우편번호(도로명) 검색 샘플(SR-235, FUNC-member-012) — 실 API 연동 없이 로컬 시드 100건'`, 파일 끝 `ALTER`는 `'우편번호(도로명) 샘플'`. 신규 DB에서도 ALTER가 나중이라 **항상 짧은 값이 이긴다** → `CREATE` 쪽 문구는 사실상 죽은 텍스트가 되고, `/sl-recon-sch`가 SCH 본문으로 읽을 값에서 `SR-235, FUNC-member-012` 귀속이 사라진다(형제 테이블은 전부 `(SR-xxx, FUNC-xxx)`를 코멘트에 달고 있어 `ZIPCODES`만 예외가 된다). 원 목적(mojibake 해소·한글 가독)은 완전히 달성됐고 API·AC·테스트 영향이 0이라 차단하지 않는다. 문자열은 **사람 코멘트가 직접 지정한 값**이므로, 고칠지 여부는 사람이 정한다 — 고친다면 `ALTER` 문자열을 `CREATE` 리터럴과 동일하게 맞춰 파일 안 단일 정본으로 만든다.
  4. [이월 유지] 권고4(`LIKE` `ESCAPE`)·권고5(`test_baseline.json` 449 → 현재 573 재기록)는 사람 결정대로 손대지 않았다. 기준선 재기록 시 573이 기준이 되며, 권고1이 해소되지 않으면 그 573도 3 실패를 포함한 채로 굳는다.

## 재작업 지시
> round 2 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [low/regression] ZIPCODES table_comment가 여전히 mojibake(information_schema 실측 '�싦�렺뷄덼샣 …') — CREATE TABLE IF NOT EXISTS는 기존 테이블에 COMMENT를 재적용하지 않아 데이터만 수렴했다. API/AC/테스트 영향은 없으나 /sl-recon-sch가 이 코멘트를 SCH 본문으로 읽는다 → V8__zipcodes.sql 끝에 ALTER TABLE ZIPCODES COMMENT='...' 한 줄 추가(멱등, DROP 없음). 형제 테이블의 같은 잔여물은 다른 FUNC 소유라 별도 정리 SR로
2. [low/regression] ZipcodeDaoTest D07의 doesNotContain(U+FFFD) 단언은 round1의 실제 손상(MS949 오독 → 유효 한글 쓰레기)을 검출하지 못한다 — 실효 단언은 search("강남"/"테헤란로") isNotEmpty 쪽이다 → U+FFFD 단언은 유지해도 무해하나 '실제 검출은 한글 왕복 검색 단언이 한다'를 주석에 명시해 커버리지 착시를 막는다
3. [low/spec] zipcode.xml의 OR 절에 괄호가 없다 — 술어가 둘뿐인 현재는 정확하지만 뒤에 AND 한 줄이 붙으면 조용히 의미가 바뀐다 → WHERE (road_address LIKE ... <if>OR zipcode LIKE ...</if>)로 괄호 명시(현재 동작 불변)
4. [low/security] LIKE 와일드카드 미이스케이프가 그대로다 — 라이브 실측 q=%% → 200 50건(전체 행). 주입은 아니고(#{} 바인딩) 공개 참조데이터 + LIMIT 50이라 피해는 제한적 → 사람 결정대로 product.xml과 같은 변경에서 ESCAPE 절 도입(이월, 이 FUNC 범위 밖)
5. [low/regression] .speclinker/test_baseline.json의 shop-api executed=449(HEAD 0d8ddb6)가 완료조건 483·현재 510과 어긋난다 — 이 FUNC이 만든 격차는 아니고 현재 수 > 기준선이라 게이트 위반도 아니다 → SR-235 종결 시 test_baseline_ws.py record로 shop-api 기준선 510 재기록(이 FUNC 밖 후속)

사람 코멘트: [결정 요약] QA round2 CONCERNS 권고 5건 중 1·2·3만 이번 라운드에서 고친다. 권고4(LIKE ESCAPE)는 round1 결정대로 이월(product.xml과 같은 변경에서), 권고5(test_baseline 재기록)는 SR-235 종결 시. 형제 테이블 COMMENT 잔여물은 손대지 않는다.
[구현 방식]
(1) V8__zipcodes.sql 끝에 ALTER TABLE ZIPCODES COMMENT='우편번호(도로명) 샘플' 멱등 한 줄 추가(같은 값 재실행 무해, DROP 없음).
(2) ZipcodeDaoTest D07(seedData_hasExpectedDistributionAndIsReadableAsKorean)의 U+FFFD 부재 단언 옆에 "실제 인코딩 손상 검출은 한글 왕복 검색(강남/테헤란로) 단언이 한다"는 주석 한 줄 추가(동작 불변).
(3) zipcode.xml WHERE 절에 괄호 명시 — WHERE (road_address LIKE '%q%' <if>OR zipcode LIKE 'q%'</if>)로 정정(현재 동작 불변, 향후 AND 추가 시 의미 보존).
[보안 순서] 변경 없음.
[테스트·완료 조건] mvn test 전량 통과, 기준선(510) 유지. Zipcode 3개 테스트 클래스 재실행 통과.
[후속 SR·이월] 권고4(LIKE ESCAPE)는 product.xml과 같은 변경에서. 권고5(기준선 재기록)는 SR-235 종결 시. 형제 테이블 COMMENT는 별도 정리 SR.

## round3 재작업 완료(2026-09-19)

재작업 지시 1·2·3(사람 코멘트가 승인한 스코프)만 반영. 4(LIKE ESCAPE)·5(기준선 재기록)는 지시대로
이번 라운드에 손대지 않았다.

### 수정 파일 (전체 경로, 이 3개만)
- `modules/shop-api/src/main/resources/db/V8__zipcodes.sql` — 파일 끝에
  `ALTER TABLE ZIPCODES COMMENT='우편번호(도로명) 샘플';` 한 줄 추가(같은 값 재실행 무해 = 멱등,
  DROP 없음 — `ddl-idempotent` 준수). 위에 재작업 근거 주석 4줄.
- `modules/shop-api/src/test/java/com/sm/lab/shop/dao/ZipcodeDaoTest.java` — D07
  (`seedData_hasExpectedDistributionAndIsReadableAsKorean`)의 U+FFFD `doesNotContain` 단언 블록
  바로 위에 "실제 인코딩 손상 검출은 한글 왕복 검색(강남/테헤란로) 단언이 한다"는 주석 3줄 추가.
  단언(assert) 자체는 한 글자도 바꾸지 않았다.
- `modules/shop-api/src/main/resources/mapper/zipcode.xml` — `search` 쿼리의 `WHERE` 절을
  `WHERE (road_address LIKE ... <if test="numeric">OR zipcode LIKE ...</if> )`로 괄호 명시. SQL이
  생성하는 조건식은 이전과 동일(술어가 OR 두 개뿐이라 괄호 유무가 결과에 영향 없음) — 순수 표기
  변경.

### 검증
- `mvn test -Dtest=ZipcodeServiceTest,ZipcodeControllerTest,ZipcodeDaoTest` — **25/25 통과**
  (Controller 6 · Dao 7 · Service 12, round2와 건수 동일 — 이번 라운드는 신규 테스트를 추가하지
  않음).
- `mvn test`(전량) — **573 실행 / 3 실패 / 0 오류**. 실패 3건은 전부
  `com.sm.lab.shop.controller.OrderListEndToEndIntegrationTest`(2건)와
  `com.sm.lab.shop.web.ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders`
  (1건) — 이 3개 모두 Zipcode와 무관한 주문 목록 조회이고, 실패 사유는 CLAUDE.md에 이미 문서화된
  시간 의존 이슈(주문 조회 기본 창 = 오늘-30일, 시드는 고정 날짜라 시스템 날짜가 흐르면서 창 밖으로
  밀려남 — SR-300)다. `git diff`로 이 라운드의 변경이 위 3개 파일(zipcode 관련)로만 한정됨을 재확인
  — Order 코드·테스트는 이번 라운드 이전에 이미 커밋된 상태였고 내가 손대지 않았다. 실행 건수는
  기준선(직전 510) 대비 **감소하지 않았고**(573 > 510, 그 사이 SR-304/305가 커밋한 신규 테스트가
  포함된 자연 증가), 이 3건의 실패는 이번 3가지 변경이 유발한 회귀가 아니다.

## 후속 추적(TODO) — round3 CONCERNS 결정(2026-09-19)
> 사람 결정: "추적등록 후 진행". 아래는 이 항목 밖의 환경 이슈이며 **새로 등록하지 않는다** — 이미 접수된 추적으로 참조만 한다.

1. **SR-300 추적(신규 아님)** — `mvn test` 전량 573건 중 3건 실패(`OrderListEndToEndIntegrationTest` 2건, `ApiKeyAuthIntegrationTest.memberApiKey_ordersListWithoutMemberIdParam_returnsOnlyOwnOrders` 1건)는 이 FUNC(Zipcode) 변경과 무관하다. 원인: `OrderService.java:63`의 `LocalDate.now().minusDays(30)` 기본 조회창이 시스템 날짜(2026-09-19) 기준 `2026-08-20`으로 밀리면서 고정 시드 주문(2026-08-15~17)이 창 밖으로 나감 — CLAUDE.md에 이미 문서화된 SR-300 시간 의존 이슈다. 이 항목의 기준선 대조(STEP5)에서 이 3건으로 인한 악화는 `by: 사람, 사유: SR-300 추적 · 이 항목 변경과 무관`으로 waive한다.
2. **mvnw.cmd 경로** — 사람이 이 job 밖에서 이미 수정 완료(커밋 "lab 운영: Windows 앱 실행 계약(APP_RUN_CMD=mvnw.cmd)", 22cfc7f). `./mvnw.cmd -v` 재확인 결과 `{{PLUGIN_PATH}}\lab\.runtime\apache-maven-3.9.9`를 정상 호출함 — 재작업 불필요.
3. **ZIPCODES 테이블 코멘트** — 사람이 지정한 값(`'우편번호(도로명) 샘플'`) 그대로 유지한다. `/sl-recon-sch`가 SR-235/FUNC-member-012 귀속 문구를 잃는 부작용은 인지하고 수용(영향 0).
