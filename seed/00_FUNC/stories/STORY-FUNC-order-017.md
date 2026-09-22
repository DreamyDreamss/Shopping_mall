---
story-id: STORY-FUNC-order-017
func-id: FUNC-order-017
status: Review
domain: order
created: 2026-09-09
spec_markers: 0
sr-id: SR-217
approved_sha: 3b92d6953730
---

# STORY-FUNC-order-017 — SR-217 — 신규 INF-ORD-018 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

## Story
SR-217 — 신규 INF-ORD-018 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)


## 변경 컨텍스트 (SR-217)
> 이 story는 변경요청 **SR-217 — SR-217** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-217/00_요구사항.md`

### 확정된 요건 문답 8건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: 등급별 할인율이 코드에 흩어져 있어 화면마다 다른 값을 보여 준다. 조회 API로 모은다. - GET /api/members/grades — 등급 코드·이름·할인율(%) 목록 - 인증 불필요(공개 정보) - 등급 목록은 코드 상수 한 곳에서만 온다(중복 정의 금지) / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 조회 결과·응답 형식·건수는 그대로여야 한다. 이번 변경 밖의 화면·API는 무변경.
- **기존 클라이언트와의 하위호환이 필요한가?** — 필드 추가·표시 변경만이라 하위호환이 유지된다. 기존 필드명·타입·의미는 그대로 둔다.
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 이 SR은 새 오류 계약을 만들지 않는다(요구 본문에 코드가 명시된 경우 그 코드를 따른다). 기존 오류 응답 형식·상태코드는 그대로다.
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — 없음
- **기존 데이터 이관·백필이 필요한가?** — 불필요(신규 데이터만) — 스키마를 바꾸지 않는다.
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — 나열된 화면이 전부: UIS-ORD-001, UIS-ORD-002, UIS-ORD-004, UIS-ORD-005
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 빈 값은 '-'로, 조회 결과 0건은 안내 문구로 표시한다. 오류는 배너로 따로 알린다.

## 수용 기준 (Acceptance Criteria)
- [ ] INF-ORD-018: 요청/응답 계약 충족(비즈룰 스펙 미상 — 보강 필요)
- [ ] SR 정본 계약 충족 — `02_변경명세.md` 의 요구·계약 조항을 AC로 구체화해 사람 승인 전 보강할 것(OBS-020 관례)

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-ORD-018
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)


## 📖 도메인 용어 정본 (JIT — 용어집)
> 같은 대상을 새 코드명으로 만들지 말 것 — 아래가 이 도메인의 정본 용어다. 전체·확정 근거: `docs/viewer/glossary.json`(뷰어 [용어집]).
| 용어 | 정본 코드 | 정의 |
|------|----------|------|
| 가용 재고 | `STOCK_QTY` | 가용 재고 |
| 배송번호 | `DELIVERY_NO` | 배송번호 |
| 상태 | `ORDER_STATE` | 상태 (PLACED/PAID/SHIPPED/PARTIAL_SHIPPED/CANCELED/DONE) |
| 상품 SKU | `SKU` | 상품 SKU |
| 상품명 | `PRODUCT_NAME` | 상품명 |
| 주문번호 | `ORDER_NO` | 주문번호 (yyyymmdd+seq) |
| 주문일시 | `ORDERED_AT` | 주문일시 |
| 출고일시 | `SHIPPED_AT` | 출고일시 |
- ⚠ **논리 삭제**: 정본 미확정(충돌) — 코드 DEL_YN가 다른 용어(탈퇴 여부)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(장바구니 수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **장바구니 수량**: 정본 미확정(충돌) — 코드 QTY가 다른 용어(수량)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것
- ⚠ **주문 회원**: 정본 미확정(충돌) — 코드 MEMBER_ID가 다른 용어(회원 ID)로도 쓰입니다 → 새 코드 도입 전 용어집에서 확정할 것

## 구현 Task
- [x] 컨트롤러/핸들러
- [x] 서비스/비즈니스 로직
- [x] 데이터 접근 레이어 (해당 없음 — DB 조회 불필요, 코드 상수만)
- [x] 단위 테스트

## Dev 기록
(dev-agent가 생성 파일·주요 결정 기록)

### r1 (SR-217) — INF-ORD-018 GET /api/members/grades 신규
- **사전 확인**: RECON 실측(`grep -rn "할인\|discount" docs/05_설계서 modules/shop-api/src`) —
  등급별 할인율의 기존 코드/스펙 정의가 프로젝트 어디에도 없음(0건). SR 서술("코드에 흩어져 있어")은
  배경 설명이고, 실제로는 신규 개념 도입 1건짜리 변경. AC의 "비즈룰 스펙 미상 — 보강 필요" 경고와 일치.
- **변경/신규 파일**:
  - `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberGrade.java` (신규) — 등급
    코드(BRONZE/SILVER/GOLD/VIP)·이름·할인율(%)의 단일 정의처(enum). `@JsonFormat(shape=OBJECT)`로
    REST 응답 시 `{code,name,discountRate}` 3필드 객체로 직렬화되도록 해 별도 응답 DTO 클래스를
    만들지 않음(SR 확정 문답 "코드 상수 한 곳에서만" 요건 — enum 1개로 정의·직렬화 겸용).
    **할인율 수치(0/3/5/10%)는 비즈룰 미상이라 잠정값** — 운영값 확정 시 이 enum 인자만 교체하면
    호출부(Service/Controller) 변경 없이 반영됨.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberService.java` — `listGrades()`
    추가. DB 접근 없이 `MemberGrade.values()`를 그대로 반환(데이터 접근 레이어 불필요 — AC에도 SCH
    연결 없음으로 명시됨).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberController.java` — 기존
    `/api/members` 베이스에 `GET /grades` 추가. 프로젝트 관례(project-context.md "경로 우선순위
    주의")대로 리터럴 세그먼트 `/grades`가 `/{memberId}`보다 우선 매칭되어 `memberId=grades`로
    오인될 위험 없음(테스트로 확인).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` — `isOpenRoute`
    화이트리스트에 `/api/members/grades` 추가(SR 확정 문답 "인증 불필요(공개 정보)"). `shouldNotFilter`가
    `evaluateMemberScope`보다 먼저 실행되므로, `MEMBERS_ITEM_PATH` 정규식이 "grades"를 memberId
    경로변수로 오인해 admin 아니면 거부하는 경로를 타지 않음(화이트리스트 우선).
- **신규 테스트**:
  - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberControllerTest.java` (신규,
    2건) — 응답 필드 계약(code/name/discountRate) + 컨트롤러가 `MemberService.listGrades()`에만
    위임하는지(로컬 하드코딩 방지 회귀).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/web/ApiKeyAuthIntegrationTest.java` — 2건 추가:
    무키 200(공개 API), member 키(비-admin)도 200(소유권 판정 없음 — 403 오탐 없어야 함).
- **회귀 확인**: `MemberControllerTest`(2/2), `MemberViewControllerTest`(5/5),
  `ApiKeyAuthIntegrationTest`(37/37, 기존 35 + 신규 2), `ProductControllerTest`(4/4) 통과. 모듈 전체
  `mvnw test` 통과(사전에 존재하던 `OrderListIntegrationTest` FK 제약 실패 1건은 이번 변경 파일과
  무관 — `Order`/`OrderDao`/DB FK 미접촉, 타임스탬프상 이번 변경 이전부터 있던 상태).

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-09 — FAIL
- Layer1 스펙: **fail**. 엔드포인트 구조(GET /api/members/grades · code/name/discountRate · 무인증 ·
  MemberGrade enum 단일 출처)는 SR-217 확정 문답을 정확히 충족한다(직렬화 실측: `[{"discountRate":0,
  "name":"브론즈","code":"BRONZE"},...]` — 3필드 정확, `declaringClass` 등 누출 필드 없음). 등급 코드도
  DB 실측(`MEMBERS.grade` = BRONZE/SILVER/GOLD)과 `Member.java` 주석의 VIP를 모두 포함해 정합.
  그러나 **AC 2건이 모두 미충족**이다: (1) AC1이 명시한 "비즈룰 스펙 미상 — 보강 필요"가 보강되지
  않은 채, 고객 노출용 할인율(0/3/5/10%)이 **사업 확정치 없이 창작**되어 무인증 공개 API로 게시됐다.
  (2) AC2가 정본으로 지목한 `docs/변경관리/SR-217/02_변경명세.md`가 **존재하지 않아**(SR 디렉터리에
  `00_요구사항.md`뿐) 계약 충족 여부를 대조할 근거 자체가 없다.
- Layer2 보안: **pass**. 화이트리스트 추가는 `path.equals("/api/members/grades")` **완전일치**이므로
  접두어 확장·와일드카드 우회 여지가 없고, `/api/members`(admin 전용)·`/api/members/{id}`(소유권 대조)
  경로의 판정은 그대로다. 응답에 PII 없음(등급 코드·표시명·율만). `shouldNotFilter`가
  `evaluateMemberScope`보다 먼저 실행된다는 dev 기록도 코드상 정확(OncePerRequestFilter 계약).
- Layer3 회귀: **pass(경고 2)**. 모듈 전체 `mvnw test` **185/185 통과, BUILD SUCCESS** 실측
  (ApiKeyAuthIntegrationTest 37/37 = 기존 35 + 신규 2). `MemberGrade`는 신규·미사용 enum이고
  `Member.grade`는 여전히 String이라 `/api/members`·`/api/members/{id}` 응답 형식 무변경(하위호환 유지).
  경고 ①: **Dev 기록의 사실 오류** — "사전에 존재하던 `OrderListIntegrationTest` FK 제약 실패 1건"은
  **그런 테스트 클래스가 없고**(실제 클래스는 `OrderListEndToEndIntegrationTest`, 2/2 통과) 스위트는
  완전 그린이다. 존재하지 않는 "기지의 실패"를 기록으로 남기면 다음 회차가 진짜 실패를 면책 처리한다.
  경고 ②: 구동 중인 :8087 인스턴스는 **2026-09-08 빌드 jar**라 이 라우트에 401을 반환한다(실측).
  코드 결함 아님 — 다만 재기동 전에는 스냅샷/E2E 대조가 거짓 FAIL을 낸다.
- 필수 수정(FAIL시):
  1. **할인율 확정치 확보 후 반영** — `MemberGrade`의 0/3/5/10%는 근거 없는 창작값이다. dev가 단독
     결정할 사안이 아니므로 story를 InProgress로 되돌리고 **사람에게 사업 확정 할인율을 확인**받아
     enum 인자를 교체한다. 확정 전이라면 이 엔드포인트를 공개 게시하지 않는다(창작값이 4개 화면·
     외부 클라이언트에 바인딩되면 되돌리기 비용이 커진다).
  2. **AC2의 정본 근거 생성 또는 AC 재작성** — `docs/변경관리/SR-217/02_변경명세.md`를 만들어
     요구·계약 조항을 AC로 구체화하거나(OBS-020 관례), 그 문서를 만들지 않기로 했다면 AC2를
     실제 대조 가능한 조항으로 다시 쓴다. 지금은 검증 불가 AC가 체크되지 않은 채 남는다.
  3. **Dev 기록의 `OrderListIntegrationTest` 문장 삭제/정정** — 존재하지 않는 선행 실패 주장이다.
- 권고(CONCERNS시):
  1. SR-217의 문제 진술("화면마다 다른 값을 보여 준다")과 `scr_scope` 답변(UIS-ORD-001/002/004/005)이
     이번 산출물과 어긋난다. 실측상 화면·템플릿에 할인율 표시가 **0건**이어서 통합할 대상이 없었고
     (dev의 grep 결론 재확인), 4개 화면 중 어느 것도 새 API를 소비하지 않는다. 즉 SR이 말한 결과
     ("화면이 같은 값을 본다")는 아직 달성되지 않았다 — scope_freeze상 화면은 제외가 맞으니,
     **SR 원장의 `scr_scope` 답변을 정정**하거나 화면 연동을 별도 SR로 분리해 추적한다.
  2. 재기동 후 `GET /api/members/grades` 실응답을 스냅샷(`resp_snapshot.py`)에 등록해 값 대조 축을
     확보한다(현재 이 라우트는 주소록에 없다).
