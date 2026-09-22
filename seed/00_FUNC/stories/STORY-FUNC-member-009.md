---
story-id: STORY-FUNC-member-009
func-id: FUNC-member-009
status: Done
domain: member
created: 2026-09-13
spec_markers: 0
sr-id: SR-234
approved_sha: 320b024639a8
---

# STORY-FUNC-member-009 — SR-234 — 비밀번호 재설정 확정 API · 신규 INF-MBR-007 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

## Story
SR-234 — 비밀번호 재설정 확정 API · 신규 INF-MBR-007 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)


## 변경 컨텍스트 (SR-234)
> 이 story는 변경요청 **SR-234 — SR-234** 에서 나왔다. 신규 구축이 아니라 **기존 동작의 변경**일 수 있으니, 바꾸지 않아야 할 것을 먼저 확인하라.
- 요구사항: `docs/변경관리/SR-234/00_요구사항.md`
- 변경명세(TO-BE): `docs/변경관리/SR-234/02_변경명세.md`

### 확정된 요건 문답 9건 — 이대로 구현한다(재해석 금지)
- **이번 SR에서 하는 것과 명시적으로 빼는 것은 무엇인가?** — 포함: [유형: 화면+API] 이메일/휴대폰 인증 후 새 비밀번호 설정. 링크·코드는 10분 유효, 1회용. UI/UX: 현재 단계·남은 유효시간 표시 · 재전송은 60초 쿨다운. 수용 기준: 만료 링크는 '만료됨 + 다시 요청' · 재설정 후 모든 기기 로그아웃 / 제외: 요구 본문에 적히지 않은 것 전부
- **반드시 유지돼야 하는 기존 동작(회귀 범위)은 무엇인가?** — 기존 로그인(SR-232)·가입(SR-231) 흐름과 MEMBER_LOGIN_ATTEMPTS·리프레시 토큰 동작은 그대로. 재설정 완료 시 그 회원의 리프레시 토큰 전부 폐기(모든 기기 로그아웃)
- **기존 클라이언트와의 하위호환이 필요한가?** — 기존 필드명·타입·의미는 그대로 둔다(추가만)
- **오류 응답 계약(코드·메시지)은 무엇인가?** — 기존 오류 계약(코드 MBR-4xxx + message) 그대로. 만료 링크/코드는 410 MBR-4101 '만료됨 — 다시 요청', 존재 여부는 새지 않게 요청 API는 항상 202
- **타 도메인 테이블·집계(정산·통계류)에 파급이 있는가?** — MEMBER_PASSWORD_RESETS 테이블 신설(대상 email/phone_norm · 코드 해시 · 만료 시각 · 소비 시각 · 시도 횟수 · 생성 시각) + MEMBERS.password_hash 갱신·updated_at. 기존 컬럼 의미 변경 없음
- **기존 데이터 이관·백필이 필요한가?** — Flyway V4__member_password_resets.sql — CREATE TABLE IF NOT EXISTS(랩 규칙 ddl-idempotent). 기존 행 이행 없음
- **변경 대상 화면이 전부 나열됐는가? (누락 화면 없음 확인)** — shop-web에 비밀번호 재설정 화면 1개(요청→코드 확인→새 비밀번호 3단계, 현재 단계·남은 유효시간·재전송 60초 쿨다운 표시). 로그인 화면에 '비밀번호를 잊으셨나요' 링크만 추가
- **빈 값·오류 상태의 화면 표기는 무엇인가?** — 0건·오류·로딩 표시는 기존 규칙(UIS §5)을 그대로 따른다
- **이 변경으로 생기거나 바뀌는 화면 상태 중 스토리(상태별 실물)로 남길 것은 무엇인가?** — 바뀌거나 새로 생기는 상태마다 스토리를 남긴다(기존 스토리는 깨뜨리지 않는다)

### 구현 모듈(제약) — `shop-api` (`{{SRC_SHOP_API}}`)
이 FUNC의 소스는 위 모듈 안에 만든다. 다른 모듈에 만들면 스코프 위반이다(예약 폼에서 사람이 지정한 값).

## 수용 기준 (Acceptance Criteria)
- [x] INF-MBR-007: 요청/응답 계약 충족
  - ✅ POST /api/members/password-resets/confirmations 엔드포인트
  - ✅ Request: target(이메일/휴대폰), code(6자리), newPassword(8~64자 영문+숫자)
  - ✅ Success: 204 No Content
  - ✅ Error: 400(형식 오류 MBR-4100/MBR-4001), 410(만료 MBR-4101), 409(오답 MBR-4102·초과 MBR-4103), 500(DB예외 MBR-5000)
  - ✅ 존재 여부 비공개: 항상 204 또는 4xx(회원 미발견 시에도 204)
  - ✅ 전 기기 로그아웃: 리프레시 토큰 폐기 ✅, 회원 API 키 폐기 ✅
- [x] SR 정본 계약 충족 — `docs/변경관리/SR-234/02_변경명세.md` 모든 요구사항
  - ✅ 순서·보안: 형식 검증(1-2) → 코드 확정(5) → 비밀번호 반영·세션 폐기(9)
  - ✅ 원자성: 코드 확정은 autocommit, 비밀번호 반영+세션폐기는 @Transactional Writer
  - ✅ 존재 오라클 방지: 행 없음과 오답 동일 409 MBR-4102
  - ✅ 탈퇴 회원 방어: SELECT·UPDATE 양쪽 del_yn='N' 필터
  - ✅ BCrypt: 코드 확정 이후, 회원 발견 여부와 무관하게 항상 실행

## 컨텍스트 (Dev Notes — 자기완결)
> Dev가 다른 문서를 안 읽어도 구현 가능하도록 전 컨텍스트를 담는다.
- **INF** INF-MBR-007
- **SCH**: (연결 없음)
- **UIS**: (연결 없음)
- **프로젝트 패턴**: docs/project-context.md 참조(레이어·네이밍·프레임워크 관례)

## 구현 계획

> **개정(사람 수정 반영, 재작업)** — 사람이 확인한 골격(원자 확정 UPDATE → 그 뒤에만 회원 조회 →
> 별도 트랜잭션 빈에서 반영+전 기기 로그아웃, 204 동일 응답, del_yn 양쪽 필터, BCrypt는 확정 뒤)은
> 그대로 두고 4가지를 고쳤다: (1) "행 없음"을 409 `MBR-4102`(코드 불일치와 완전 동일 취급)로
> 통일 — 이전 초안의 "행 없음 → 400"은 같은 계획의 "테스트" 절이 이미 `MBR-4102`를 기대해
> **계획 내부 모순**이었다(둘 다 사람이 지적). attempt_count는 행이 없으므로 증가 대상이 아니다.
> (2) target/newPassword 형식 검증·정규화를 문자 단위로 복제하지 않고 `MemberPasswordResetService`/
> `MemberRegistrationService`의 기존 메서드를 가시성만 넓혀 재사용한다(이동·추출 금지 — 008 round2가
> 복제로 202/400 불일치를 낸 전례 재발 방지). (3) 완료 로그는 Writer 트랜잭션 커밋 **후** 한 줄만,
> 회원 미발견(조용한 204) 경로에는 로그를 남기지 않는다 — BCrypt는 발견 여부와 무관하게 코드 확정
> 성공 직후 항상 수행(원안 유지, 타이밍 오라클 방지). (4) apiKey 폐기는 `MemberApiKeyDao.revokeByMemberId`
> (기존 메서드, `MEMBER_API_KEYS.member_id` WHERE로 이미 회원 전용 — admin 키는 별도 static map이라
> 애초에 이 테이블에 없다)를 그대로 재사용해 관리자 키와 무관함을 구조적으로 보장한다.

- **역할 확인(사례집 SR-231 r1 재발 방지)** — 이 FUNC(009)은 **확정**(코드 확인 + 새 비밀번호 반영 + 전 기기 로그아웃) API다. FUNC-member-008/INF-MBR-006(요청 API, Done)과 짝을 이루며, `MEMBER_PASSWORD_RESETS.consumed_at`/`attempt_count`는 008이 남겨둔 대로 이 FUNC이 처음 갱신한다(V5 마이그레이션 주석에 이미 예고돼 있음). 화면(UIS-MBR-003, FUNC-member-007, shop-web)은 별도 FUNC이며 이 작업 범위 밖이다.

- **파일**:
  - `modules/shop-api/src/main/resources/db/V6__member_password_reset_confirmation.sql` (신규, FUNC-member-009 소유) — `MEMBERS.updated_at DATETIME(3) NULL`을 `ADD COLUMN IF NOT EXISTS`로 추가(SR db_ripple 답변 "MEMBERS.password_hash 갱신·updated_at" 근거). 기존 컬럼 불변.
  - `modules/shop-api/src/main/resources/application.yml` (수정) — `spring.sql.init.schema-locations` 목록 끝에 `V6__member_password_reset_confirmation.sql` 추가(기존 5개 순서 불변, add-only).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetDao.java` (수정, add-only) — `confirmIfCodeMatches(target, codeHash, now, maxAttempts)`(원자 조건부 UPDATE, `consumed_at` 세팅) · `incrementAttemptCount(target, now)`(가드된 UPDATE) 2개 신규 메서드 추가. 기존 `touchRequest`/`selectByTarget`/`deleteByTarget`은 무변경. 헤더 `linked_func` 주석에 FUNC-member-009 추가(누적, `ApiKeyAuthFilter` 관례).
  - `modules/shop-api/src/main/resources/mapper/memberPasswordReset.xml` (수정, add-only) — 위 2개 SQL 추가. 헤더에 FUNC-member-009 언급 추가.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberDao.java` (수정, add-only) — `selectMemberIdByResetTarget(email, phoneNorm)`(신규 전용 조회 — 아래 "데이터" 절 근거로 기존 `selectMemberIdByEmailOrPhoneNorm`을 재사용하지 않고 새로 만든다) · `updatePasswordHash(memberId, passwordHash, updatedAt)`(신규) 2개 추가. 기존 8개 메서드 무변경.
  - `modules/shop-api/src/main/resources/mapper/member.xml` (수정, add-only) — 위 2개 SQL 추가.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/domain/MemberPasswordReset.java` (무변경) — 기존 필드(`consumedAt`/`attemptCount`)를 그대로 재사용(FUNC-008이 이미 조회용으로 매핑해 둠).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetService.java` (수정, **접근제어자만** — 사람 수정 (2)) — `requireValidTarget(String)`·`resolveChannel(String)`을 `private String` → package-private `static String`으로, `normalize(String, String)`을 `private static` → package-private `static`으로 가시성만 넓힌다. 본문·정규식(`EMAIL_PATTERN`/`PHONE_PATTERN`)·던지는 예외 타입(`MemberPasswordResetApiException`)·상수(`MAX_TARGET_LENGTH`, `CODE_TARGET_INVALID`="MBR-4100")는 문자 하나도 바꾸지 않는다(이동·추출 금지). 세 메서드 모두 인스턴스 필드를 쓰지 않아 `static`화가 안전하다.
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberRegistrationService.java` (수정, **접근제어자만**) — `requireValidPassword(String)`을 `private void` → package-private `static void`로 가시성만 넓힌다(`PASSWORD_RULE_PATTERN`/`PASSWORD_MIN_LENGTH`/`PASSWORD_MAX_LENGTH`/`CODE_PASSWORD_INVALID`="MBR-4001" 불변, 인스턴스 필드 미사용이라 `static`화 안전).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationApiException.java` (신규) — `MemberPasswordResetApiException`과 동일 모양(httpStatus/code/message)의 별도 예외 클래스. FUNC-008의 예외 클래스를 재사용하지 않는다 — 이 코드베이스는 밀접하게 연관된 FUNC끼리도(예: 로그인 005 vs 세션 006) 각자 예외 클래스를 만드는 관례다(파일 소유 경계를 예외 타입에도 그대로 반영).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationWriter.java` (신규, `@Service`) — `@Transactional` 메서드 `applyNewPassword(memberId, passwordHash, now)` 하나만 가진 협력자 빈. `MemberSignupCompletionWriter`와 동일한 이유(자기호출 방지)로 별도 빈으로 분리 — 이 메서드를 호출하는 `MemberPasswordResetConfirmationService`가 같은 클래스 안에서 이 로직을 `@Transactional`로 두면 self-invocation으로 트랜잭션이 적용되지 않는다(사례집에 아직 없지만 STORY 템플릿이 명시적으로 경계하는 함정 — "프레임워크 실행 모델 함정" 절 참고).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationService.java` (신규) — 코드 확정 + 새 비밀번호 반영 오케스트레이션(아래 "순서·보안" 절).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetConfirmationController.java` (신규) — `POST /api/members/password-resets/confirmations`, 204 반환. `MemberPasswordResetController`(008)와 별도 클래스로 만든다(사례집 "ddd-api-agent INF path 리스트" 항목 — 1 INF = 1 엔드포인트 = 1 컨트롤러 관례 유지, INF-MBR-007이 이 컨트롤러 하나에 대응).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberPasswordResetConfirmationExceptionHandler.java` (신규) — `@RestControllerAdvice(assignableTypes = MemberPasswordResetConfirmationController.class)`, `MemberPasswordResetConfirmationApiException` → 그 상태코드, `DataAccessException` → 500 `MBR-5000`(다른 5개 핸들러와 동일하게 이 클래스도 자신만의 사본을 갖는다 — 코드베이스 전역 관례, 공유 유틸 없음).
  - `modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java` (수정, add-only) — `MEMBER_PASSWORD_RESET_CONFIRM_PATH = "/api/members/password-resets/confirmations"`를 `shouldNotFilter` 화이트리스트에 추가(로그인 전 사용자가 호출 — `MEMBER_PASSWORD_RESET_CODE_PATH`와 동일 근거). 기존 화이트리스트·`evaluateMemberScope`는 건드리지 않는다. 헤더 누적 주석에 FUNC-member-009 추가.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/controller/MemberPasswordResetConfirmationControllerTest.java` (신규, must — `controller-has-test`).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationServiceTest.java` (신규, should — `service-has-test`).
  - `modules/shop-api/src/test/java/com/sm/lab/shop/dao/MemberPasswordResetDaoTest.java` (수정, add-only) — 기존 파일(FUNC-008 소유)에 `confirmIfCodeMatches`/`incrementAttemptCount` 검증 메서드를 추가한다(같은 Mapper 인터페이스를 테스트하는 기존 파일 재사용 — 새 DAO 테스트 파일을 또 만들지 않는다, `ApiKeyAuthFilter`처럼 파일이 여러 FUNC에 걸쳐 누적되는 관례). 헤더에 FUNC-member-009 추가.
  - `modules/shop-api/src/test/java/com/sm/lab/shop/MemberPasswordResetConfirmationFlowTest.java` (신규, `@SpringBootTest`, 실 서버+실 DB) — `MemberRegistrationCompletionFlowTest`와 동일한 이유(Mockito로 증명 못 하는 트랜잭션 경계·실제 비밀번호 변경·세션 폐기의 HTTP 레벨 회귀)로 별도 흐름 테스트를 둔다.

- **데이터**:
  - 신규 테이블 없음 — `MEMBER_PASSWORD_RESETS`(FUNC-008 소유 테이블)의 `consumed_at`/`attempt_count`를 이 FUNC이 처음 갱신한다(V5 SQL 파일 주석이 이미 이 FUNC의 소유를 예고).
  - `MEMBERS.updated_at`(신규 컬럼, `NULL` 허용, 이 FUNC이 최초 사용)만 추가한다 — SR db_ripple 답변 근거. 기존 회원의 이 컬럼은 계속 `NULL`(백필 없음, SR "기존 데이터 이관·백필 불필요"와 일관).
  - **왜 `selectMemberIdByEmailOrPhoneNorm`(FUNC-member-003 소유)을 재사용하지 않는가** — 그 메서드는 "가입 시 중복 여부만" 판정하면 돼 `email = #{email}`(대소문자 구분, DB 컬럼 정렬 규칙에 의존) 정확 일치로 충분했다. 이 FUNC은 FUNC-008이 저장한 **정규화(소문자)** target으로 MEMBERS.email(가입 시 원문 그대로 저장 — `MemberRegistrationService`는 소문자화하지 않음, 실측)을 찾아야 하는데, DB collation(대소문자 구분 여부)에 의존해 매칭 여부를 가정하면 환경마다 다르게 동작할 위험이 있다(검증 없이 가정하지 않는다). 그래서 이 FUNC 전용 `selectMemberIdByResetTarget`을 새로 만들어 `LOWER(email) = #{email}`(호출측이 이미 소문자화한 값)로 명시 비교하고, `phone_norm = #{phoneNorm}`은 원래도 숫자만 저장되므로 정확 일치 그대로 둔다. **`del_yn = 'N'`을 이 SELECT의 WHERE에 상시 건다**(사례집 SR-232 r2 "회원 존재/자격 관련 조회는 항상 MEMBERS + del_yn 필터를 통과해야 한다" 직접 적용 — 이 조회가 결과적으로 비밀번호 변경·세션 폐기라는 자격 변경 행위로 이어지므로).
  - `updatePasswordHash`의 UPDATE 문에도 `WHERE member_id = #{memberId} AND del_yn = 'N'`을 건다(방어적 이중 확인 — 위 조회와 이 UPDATE 사이에 이론상 벌어질 수 있는 회원 탈퇴 레이스에도 안전).
  - **트랜잭션 경계**: `MemberPasswordResetConfirmationService`는 `@Transactional`을 전혀 쓰지 않는다. 코드 확정(`confirmIfCodeMatches`/`incrementAttemptCount`)은 단일 UPSERT/UPDATE 문 각각이 autocommit으로 즉시 커밋된다(사례집 SR-231 r2 — 실패로 끝나는 시도의 카운터가 트랜잭션 롤백에 함께 사라지면 안 되므로, `MemberRegistrationService` STEP1과 동일 사고방식). 코드 확정이 성공한 **뒤**의 "비밀번호 반영 + 전 기기 로그아웃"만 `MemberPasswordResetConfirmationWriter.applyNewPassword`(별도 빈, `@Transactional`)로 원자 처리한다 — 이 단계가 부분 실패하면(예: 세션 폐기 UPDATE가 DB 예외로 실패) 트랜잭션 전체가 롤백되어 "비밀번호는 바뀌었는데 예전 세션은 살아있는" 위험한 중간 상태를 만들지 않는다(`MemberSessionService.logout()`이 이 메서드 하나만 `@Transactional`로 둔 것과 동일 논거 — "함께 성공하거나 함께 실패해야 하는 단일 논리적 동작"). 이 설계의 대가: 만약 이 트랜잭션이 실패하면 코드는 이미 소비된 상태(`consumed_at` 세팅, 롤백 안 됨)라 사용자는 코드를 재요청해야 한다 — 보안(세션 폐기 누락 방지)을 사용자 편의(코드 재사용)보다 우선한 결정.
  - **락/원자 UPDATE**: `confirmIfCodeMatches`는 `UPDATE ... SET consumed_at = #{now} WHERE target = #{target} AND code_hash = #{codeHash} AND expires_at > #{now} AND consumed_at IS NULL AND attempt_count < #{maxAttempts}` 단일 문장 — PK(`target`)의 InnoDB 행 잠금 하나로 충분하다(별도 락 순서 규약 불필요, FUNC-008과 동일 이유). 0행이면(코드 불일치/만료/이미 소비/시도 초과 중 하나) 서비스가 재조회로 사유를 구분한다(아래 "순서·보안" 5).
  - `incrementAttemptCount`는 `WHERE target = #{target} AND consumed_at IS NULL AND expires_at > #{now}`로 가드해, 이미 만료·소비된 행의 카운터를 의미 없이 늘리지 않는다(`MemberSignupCompletionDao#incrementAttemptCount`의 `consumed_at IS NULL AND verified_at IS NULL` 가드와 동일 사고방식). 각 증가는 그 자체로 원자 UPDATE 문이라 동시 여러 오답 시도가 들어와도(InnoDB 행 잠금이 직렬화) 카운터가 유실되지 않는다 — round3 QA가 지적했던 "트랜잭션 롤백에 카운터가 사라지는" 함정과 조건 자체가 다르다(이 문장은 애초에 어떤 트랜잭션에도 속하지 않는다).

- **순서·보안**(개정 — 사람 수정 (1)(2)(3) 반영):
  1. target 형식 검증(**재사용**, 복제 아님) — `String trimmedTarget = MemberPasswordResetService.requireValidTarget(target)`, 이어서 `String channel = MemberPasswordResetService.resolveChannel(trimmedTarget)`을 그대로 호출한다. 두 메서드가 던지는 `MemberPasswordResetApiException e`를 이 서비스가 catch해 `new MemberPasswordResetConfirmationApiException(e.getHttpStatus(), e.getCode(), e.getMessage())`로 감싸 재던진다(예외 타입은 FUNC별 분리 관례 유지, 정규식·길이 상수·판정 로직은 공유) → 결과 코드는 그대로 400 `MBR-4100`(재사용, 새 코드 아님).
  2. newPassword 형식 검증(**재사용**) — `MemberRegistrationService.requireValidPassword(newPassword)`를 그대로 호출하고, `MemberRegistrationApiException e`를 동일한 방식으로 감싸 400 `MBR-4001`로 재던진다. 순서(target보다 뒤, 코드 확인보다 앞)는 원안 그대로 — 형식이 잘못된 요청은 `attempt_count`를 소모하지 않는다.
  3. target 정규화(**재사용**) — `MemberPasswordResetService.normalize(channel, trimmedTarget)`을 그대로 호출한다(복제 없음, 이메일 trim+소문자·휴대폰 숫자만 규칙이 FUNC-008과 항상 같은 코드 경로로 동일하게 유지됨이 구조적으로 보장된다 — 향후 008이 규칙을 바꿔도 이 FUNC이 따로 안 바꾸면 자동으로 어긋나는 대신, 애초에 같은 메서드라 어긋날 수 없다).
  4. 제출된 code를 SHA-256(소문자 hex)으로 해싱 — 이 해시 함수 자체는 재사용 대상이 **아니다**(사람 수정 (2)는 "형식 검증 정규식·정규화"만 지목했다). `MemberLoginService`/`MemberPasswordResetService`가 이미 각자 로컬 `sha256Hex`를 복제해 둔 코드베이스 전역 관례(공유 유틸 추출은 범위 밖 결정)를 그대로 따라 이 서비스도 로컬 구현을 둔다. `code`가 null이면 빈 문자열로 취급(해시가 저장값과 자연히 달라 "코드 불일치"로 귀결).
  5. **원자 확정 시도**(`confirmIfCodeMatches`, 비트랜잭션): 0행이면 `selectByTarget`으로 재조회해 다음 우선순위로 사유를 구분한다(모든 분기가 회원 존재 여부를 전혀 조회하지 않은 상태에서 갈리므로 존재 오라클이 없다):
     - **행 없음(사람 수정 (1) — 개정)** → 409 `MBR-4102`(아래 "그 외" 분기와 **완전히 동일한 코드·메시지·응답 바이트**). "요청한 적 없는 target"이 다른 신호를 내면 그 자체로 target 존재 여부를 열거하는 오라클이 된다. `attempt_count`는 증가시킬 행이 없으므로 `incrementAttemptCount`를 호출하지 않는다.
     - `consumed_at IS NOT NULL` 또는 `expires_at <= now` → 410 `MBR-4101` "인증코드가 만료되었습니다. 다시 요청해 주세요"(SR 확정 문답 "만료됨 + 다시 요청" 그대로 — 이미 소비된 코드도 같은 코드로 묶는다).
     - `attempt_count >= 5`(=`MAX_CONFIRM_ATTEMPTS`, 로그인/가입과 동일 상수값) → 409 `MBR-4103` "코드 확인 시도 횟수를 초과했습니다. 다시 요청해 주세요"(카운터를 더 늘리지 않는다).
     - 그 외(코드가 단순히 틀림) → `incrementAttemptCount` 호출 후 409 `MBR-4102` "코드가 올바르지 않습니다".
     - 즉 최종적으로 "행 없음"과 "오답"은 서비스 로직에서 완전히 같은 분기(같은 예외 생성 호출 한 곳)로 수렴시켜, 코드 레벨에서부터 두 케이스가 구분될 여지를 없앤다(단순히 같은 코드를 두 곳에서 던지는 것이 아니라 같은 코드 경로를 타게 만든다).
  6. 확정 성공(1행)에만 도달하는 다음 단계: newPassword를 BCrypt로 해싱(코드 확인 **이후**, 무인증 엔드포인트의 연산 낭비 방지 — 원안 유지). **이 단계는 다음 7~8단계의 회원 발견 여부와 무관하게 항상 실행된다**(사람 수정 (3) — 미발견 경로에서 BCrypt를 생략하면 그만큼 응답이 빨라져 "회원 없음"이 타이밍으로 새는 오라클이 생긴다).
  7. `selectMemberIdByResetTarget`으로 정규화된 target에 매칭되는 활성 회원(`del_yn='N'`)을 조회한다. **이 조회가 이번이 처음이다** — 그 전 어떤 단계도 `MEMBERS`를 조회하지 않는다.
  8. **못 찾으면(사람 수정 (3))** — 아무 것도 하지 않고 조용히 204로 반환한다. **로그를 남기지 않는다**(완료 로그는 실제로 반영이 일어난 9단계에서만 남긴다 — 로그는 클라이언트에 노출되지 않으므로 오라클은 아니지만, "실효 없는 시도는 기록하지 않는다"는 008 관례의 연장이자 불필요한 로그 잡음 방지).
  9. 찾으면 `MemberPasswordResetConfirmationWriter.applyNewPassword(memberId, passwordHash, now)`(트랜잭션)를 호출한다 — 내부에서 ① `updatePasswordHash`(0행이면, 즉 그 사이 탈퇴했으면 다음 단계 스킵) ② `refreshTokenDao.revokeAllForMember`(전 기기 로그아웃 — SR 수용 기준) ③ `MemberApiKeyDao.revokeByMemberId(memberId, now)`(**기존 메서드 재사용, 신규 DAO 메서드 아님** — 006 로그아웃의 "리프레시 먼저, apiKey 나중" 순서를 그대로 따른다). 이 메서드는 이미 `MEMBER_API_KEYS.member_id = #{memberId}`로 좁혀진 UPDATE라(사람 수정 (4)) admin 키(별도 `application.yml` static map, 이 테이블에 아예 없음)와는 무관함이 테이블 설계 자체로 보장된다 — 새 WHERE 조건을 추가할 필요가 없다(이미 있다는 사실을 확인·기록하는 것이 이번 수정의 실질).
  10. **완료 로그(사람 수정 (3))** — `MemberPasswordResetConfirmationWriter.applyNewPassword(...)` 호출이 예외 없이 반환한 **직후**(=트랜잭션 프록시가 커밋을 마친 뒤, Writer 빈이 아니라 Service에서) 마스킹된 target으로 한 줄만 남긴다(`MemberPasswordResetService.mask`와 동일 마스킹 로직 — 이 메서드도 `private static`이라 필요하면 같은 방식으로 package-private static을 열어 재사용하거나, 로그 목적의 단순 마스킹이므로 이 서비스에 짧게 로컬 구현해도 무방하다 — 로그 문자열 하나이므로 사람 수정 (2)가 지목한 "정규식·정규화"의 범주가 아니다). 예: `log.info("비밀번호 재설정 완료 — target={}", mask(normalizedTarget))`.
  11. 어느 경로든(회원 있음/없음, 세션 폐기 성공) 컨트롤러는 204를 반환한다. 예외는 위 1·2·5단계의 오류(400/410/409)와 DB 계층 예외(500 `MBR-5000`)뿐이다.

- **계약**:
  - `POST /api/members/password-resets/confirmations`, body `{"target": "...", "code": "123456", "newPassword": "..."}` → 204(본문 없음, `MemberSessionController.logout()`과 동일 형태 — 성공 시 노출할 정보가 없다).
  - 신규 오류 코드 `MBR-4101`(410, "인증코드가 만료되었습니다. 다시 요청해 주세요" — SR이 지정한 정확한 코드 번호), `MBR-4102`(409, "코드가 올바르지 않습니다" — **오답과 "요청한 적 없는 target"(행 없음) 두 경우 모두 이 코드로 통일한다, 사람 수정 (1)**. 응답 바디가 두 경우에서 바이트 단위로 동일함이 이 코드의 존재 이유다), `MBR-4103`(409, "코드 확인 시도 횟수를 초과했습니다. 다시 요청해 주세요"). 셋 다 INF-MBR-006이 이미 예약해 둔 410x 대역이며 기존 4011/4012/4091~4094/4100/4291과 겹치지 않는다(확인 완료). 상태 코드 409는 `MemberRegistrationService`의 코드 검증 실패(4091/4093, 둘 다 409)와 동일한 선례를 따른다 — "제출한 상태가 서버가 기대하는 상태와 충돌"이라는 의미가 같다.
  - `MBR-4100`(400, target 형식)·`MBR-4001`(400, 비밀번호 형식)은 **재사용**(신규 코드 아님, 기존과 의미 동일).
  - `ApiKeyAuthFilter` 화이트리스트에 경로 1개 추가(계약 변경 아님, add-only).
  - 이 API는 회원 존재 여부·탈퇴 여부·`updatePasswordHash`의 실제 반영 여부를 응답으로 절대 구분하지 않는다(204 고정) — 계약의 핵심 성질.

- **테스트**:
  - 컨트롤러(MockMvc, `@WebMvcTest(MemberPasswordResetConfirmationController.class)`, 서비스는 목, `AdminApiKeyTestConfig` **import 안 함** — 무인증 화이트리스트 경로라 헤더 없이 통과함을 양성 확인, 사례집 SR-232 r3의 반대 조건 적용):
    - 유효 target/code/newPassword → 204, 본문 없음.
    - target 형식 오류 → 400 `MBR-4100`(서비스가 그 예외를 던지도록 스텁).
    - newPassword 형식 오류 → 400 `MBR-4001`.
    - 서비스가 만료 예외 → 410 `MBR-4101`.
    - 서비스가 시도초과 예외 → 409 `MBR-4103`.
    - 서비스가 코드불일치 예외 → 409 `MBR-4102`.
    - 서비스가 `DataAccessException` → 500 `MBR-5000`.
  - 서비스(Mockito, DAO+writer 목):
    - target/password 형식 오류 시 `passwordResetDao.confirmIfCodeMatches`가 **호출되지 않음**(`verify(..., never())`) — STORY "순서·보안" 1~2 순서 검증. target 형식 오류는 `MemberPasswordResetService.requireValidTarget`/`resolveChannel`이 던진 예외가 그대로 `MBR-4100`으로 재포장됐는지, password 형식 오류는 `MemberRegistrationService.requireValidPassword`가 던진 예외가 `MBR-4001`로 재포장됐는지 각각 검증(재사용 경로 자체를 증명 — 사람 수정 (2) 핵심 증거).
    - `confirmIfCodeMatches`가 1을 반환 → `writer.applyNewPassword(memberId, passwordHash, now)` 호출 인자 검증(`ArgumentCaptor`로 `passwordHash`가 `BCryptPasswordEncoder().matches(newPassword, captured)`를 만족하는지 확인 — 원문 저장 아님).
    - **(사람 수정 (1), 개정)** `confirmIfCodeMatches`가 0 반환 + `selectByTarget`이 null(행 없음, 요청한 적 없는 target) → 예외 `MBR-4102`(409)이 던져짐, `incrementAttemptCount` **호출 안 됨**을 검증(증가시킬 행이 없으므로).
    - `selectByTarget`이 `consumedAt != null`인 행 반환 → `MBR-4101`, `incrementAttemptCount` 호출 안 됨.
    - `selectByTarget`이 `expiresAt`가 과거인 행 반환 → `MBR-4101`.
    - `selectByTarget`이 `attemptCount=5`인 행 반환 → `MBR-4103`, `incrementAttemptCount` 호출 안 됨(더 늘리지 않음 검증).
    - `selectByTarget`이 `attemptCount=2`(활성, 미만료)인 행 반환(=단순 오답) → `incrementAttemptCount` **정확히 1회** 호출, `MBR-4102`.
    - 위 "행 없음" 케이스와 "단순 오답" 케이스가 **동일한 예외 타입·동일한 code(`MBR-4102`)·동일한 message 문자열**을 던짐을 같은 테스트 클래스 안에서 나란히 대조(사람 수정 (1)의 핵심 증거 — 서비스 레벨에서부터 두 케이스가 구분 불가능함을 실증).
    - **(존재 오라클 방지, 회원 발견 여부)** `confirmIfCodeMatches`가 1 반환, `selectMemberIdByResetTarget`이 **null** 반환(가짜 target) → 예외 없이 정상 반환(void), `writer.applyNewPassword`가 **호출되지 않음**을 검증. 이 케이스와 "회원이 실재하는" 케이스가 서비스 리턴값·예외 타입 양쪽에서 완전히 구분 불가능함을 같은 테스트 클래스에서 대조.
    - `writer.applyNewPassword` 호출이 정상 반환한 뒤에만 완료 로그가 남고(로그 자체는 assert 대상 아님, 호출 순서만 검증 — `verify(writer).applyNewPassword(...)` 이후 위치), 회원 미발견 분기에서는 그 로그 경로가 아예 실행되지 않음을 코드 리딩으로 대조(사람 수정 (3)).
  - DAO(`MemberPasswordResetDaoTest`에 추가, `@SpringBootTest` 실 DB):
    - `confirmIfCodeMatches`: 정답 코드+미만료+미소비+시도미만 → 1행, `consumed_at` 세팅 확인. 오답 코드 → 0행(consumed_at 불변). 만료(`expires_at`가 `now` 이전) → 0행. 이미 소비(`consumed_at` NOT NULL) → 0행. `attempt_count = 5`(=max)로 시드 후 정답 코드로 호출 → **0행**(SQL 자체가 상한을 강제함을 실증 — 서비스 레벨 목만으로는 이 SQL 조건이 실제로 동작하는지 증명 못 함).
    - `incrementAttemptCount`: 활성 행 → `attempt_count` +1. 이미 만료·소비된 행 → 가드에 걸려 불변(0행 영향).
    - (should) 동시 5스레드가 같은 target에 오답 코드로 `incrementAttemptCount` 동시 호출 → 예외 없이 종료, 최종 `attempt_count == 5`(유실 없음 — PK 행 잠금이 직렬화함을 실증).
  - 통합 흐름(`MemberPasswordResetConfirmationFlowTest`, `@SpringBootTest`, 실 서버+실 DB, HTTP 레벨):
    - 실존 회원(이메일, 대소문자 섞어 가입 — 예: 가입 시 이메일을 `"Flow009@Example.com"`으로 등록) → FUNC-008 API로 코드 요청(또는 DAO로 직접 시드) → 소문자로 정규화된 target(`"flow009@example.com"`)과 코드로 확정 호출 → 204 → 그 회원이 새 비밀번호로 로그인 API 호출 시 200(비밀번호가 실제로 반영됐음을 흐름으로 증명, `MemberDao.updatePasswordHash`의 `LOWER(email)` 매칭이 실제로 대소문자 다른 원문과 맞아떨어짐을 실측).
    - 위 성공 이후 재설정 **이전**에 발급받은 리프레시 토큰으로 `/api/members/sessions/refresh` 호출 → 401 `MBR-4012`(전 기기 로그아웃 실측 — SR 수용 기준의 핵심 증거).
    - 탈퇴 회원(`del_yn='Y'`)의 원래 이메일로 코드 요청→확정 → 204(오라클 없음)이지만 DB 직접 조회로 `password_hash`가 변경되지 않았음을 확인(del_yn 필터가 실제로 걸렀음을 실측).
    - 한 번도 가입된 적 없는 가짜 이메일로 FUNC-008 요청 → 코드 확인(DB에서 codeHash 직접 조회 후 사용, 실 발송 없으므로) → 확정 호출 → 204(회원 없음이 절대 다른 응답을 만들지 않음을 실측).
    - 동일 코드로 확정을 두 번 호출(재전송/중복 클릭 시나리오) → 첫 번째 204, 두 번째 410 `MBR-4101`(1회용 실측).
    - **(신규, 사람 수정 (1)의 통합 레벨 증거)** `MEMBER_PASSWORD_RESETS`에 **행 자체가 없는**("요청한 적 없는") target으로 임의 code를 넣어 확정 호출 → 409 `MBR-4102`. 같은 target으로 실제 코드를 요청해 두고 일부러 틀린 code로 확정 호출한 케이스(오답)도 같은 테스트에서 실행해 **두 응답의 상태코드·바디(JSON 문자열)가 바이트 단위로 동일함**을 `assertEquals`로 직접 대조한다(HTTP 레벨에서 존재 오라클이 없음을 실측하는 이 STORY의 핵심 완료 조건).
  - **완료 조건**: `mvn test` 전량 통과, 기준선 **416/0**(실행/실패+오류) 유지(`.speclinker/test_baseline.json` 대조). 위 "요청한 적 없는 target" 바이트 동일 검증과 전 기기 로그아웃(리프레시 토큰·회원 API 키 폐기) 실측이 모두 통과해야 한다.

- **테스트 격리**: DAO 테스트는 파일 전용 상수 target(예: `"pwreset-confirm-dao-test@example.com"`) + 동시성 테스트는 `UUID` 접미를 쓰고 `@AfterEach`에서 `dao.deleteByTarget(target)`으로 정리한다(FUNC-008과 동일 관례). 통합 흐름 테스트는 매 테스트 이메일에 `UUID` 접미를 붙이고 `@AfterEach`에서 `memberDao.deleteById(memberId)` + `passwordResetDao.deleteByTarget(target)`을 호출한다 — 리프레시 토큰·apiKey 행은 회원 삭제와 별개 테이블이라 남을 수 있으므로, 기존 SR-232 로그인/세션 테스트가 쓰는 정리 방식을 그대로 따른다(별도 신규 정리 로직을 만들지 않는다 — 이미 있는 관례 재사용).

- **폴백·우회 경로의 자격 판정**: 이 FUNC은 FUNC-008이 예고한 접점을 그대로 마주친다 — "확정 시점에 target에 매칭되는 MEMBERS 행이 없으면 그때 가서도 존재 오라클을 새지 않는 응답을 내야 한다." 이 STORY의 순서·보안 5·8단계가 정확히 그 요건을 구현한다: 코드 확정(5단계, "행 없음"도 오답과 동일 코드로 수렴 — 사람 수정 (1))까지는 회원 조회가 전혀 없고, 회원 조회(7단계) 이후에도 결과가 있든 없든 응답은 204로 동일(8단계)하다. 추가로 `del_yn` 필터를 조회(`selectMemberIdByResetTarget`)와 UPDATE(`updatePasswordHash`) **양쪽 모두**에 걸어(사례집 SR-232 r2 "회원 존재/자격 관련 조회는 항상 del_yn 필터를 통과해야 한다") 탈퇴 회원의 비밀번호가 되살아나거나 탈퇴 회원 명의로 세션이 발급되는 경로를 원천 차단한다 — r2의 사고(API 키 DB 폴백이 del_yn을 안 봐 탈퇴 회원 키가 무기한 통과)와 같은 계열이지만, 여기서는 "폴백 조회"가 아니라 "재설정 확정 시 회원 매칭"이 그 위험한 지점이다.

- **프레임워크 실행 모델 함정**: `MemberPasswordResetConfirmationService.confirmPasswordReset(...)`이 성공 경로에서 `this.applyNewPassword(...)`처럼 **같은 클래스 안의 `@Transactional` 메서드를 직접 호출하면 Spring 프록시 self-invocation으로 트랜잭션이 적용되지 않는다**(`MemberSignupCompletionWriter`가 별도 빈으로 존재하는 이유와 동일). 이 함정을 피하려고 트랜잭션 로직을 처음부터 별도 빈(`MemberPasswordResetConfirmationWriter`)에 두고, 서비스는 그 빈을 생성자 주입받아 **빈 경계 너머로** 호출한다(프록시를 거치므로 정상 적용). shop-web(React StrictMode) 코드는 이 FUNC(shop-api)에 없다. `@Scheduled` 배치를 도입하지 않으므로 스케줄러 다중 인스턴스 문제도 해당 없음.

- **범위 밖**:
  - 화면(UIS-MBR-003, 3단계 폼) — FUNC-member-007(shop-web), 별도 FUNC.
  - 만료·소비된 `MEMBER_PASSWORD_RESETS` 행 정리 배치 — FUNC-008이 이미 범위 밖으로 이월한 것과 동일 사유(SR 미요구), 후속 SR 후보로만 남긴다.
  - 회원당 여러 target(이메일+휴대폰) 동시 재설정 요청 간 상호 무효화 — SR 요구에 없다.
  - 재설정 완료 알림 메일/문자 발송 — SR 요구에 없다(코드 발송 시뮬레이션 로그는 008이 이미 처리했고, 확정 완료 알림은 별도 요구가 없어 만들지 않는다).
  - 공유 SHA-256/BCrypt 유틸 추출 — 기존 서비스별 복제 관례(FUNC-008과 동일 판단)를 그대로 따른다. **이번에 연 것은 "가시성 확장으로 기존 메서드를 직접 호출"뿐이며(사람 수정 (2), 위 "파일"·"순서·보안" 절), 별도 공유 유틸 클래스·패키지를 새로 만들지 않는다** — 두 결정(정규식/정규화는 재사용하되 해시·BCrypt는 계속 복제)의 경계가 이 STORY의 판단이다.

- **실패 사례집 대조** (`harness/antipatterns.all.md`):
  - r1(SR-231 — ID만 보고 역할이 뒤바뀜) — STORY 제목·INF-MBR-007로 "확정"임을 재확인(위 "역할 확인" 참고). 조건 성립: ID만 봤다면 008과 뒤바뀔 위험이 있었다.
  - r2(SR-231 — 카운터를 트랜잭션 안에서 올렸다가 롤백에 같이 사라짐) — `incrementAttemptCount`/`confirmIfCodeMatches`는 어떤 `@Transactional` 블록에도 속하지 않는 개별 autocommit 문장이다(위 "데이터·트랜잭션 경계" 절). 조건이 실제로 성립할 뻔한 지점(코드 확정 로직을 성공 경로의 `@Transactional` 안에 무심코 합쳤다면 재발했을 것)이라 명시적으로 분리했다.
  - r3(SR-231 — `FOR UPDATE` 순서가 달라 데드락) — 단일 테이블·단일 문장·명시적 락 없음. 조건 불성립.
  - r4(SR-231 — `useAffectedRows=true` 전역 속성) — 이 FUNC은 UPDATE 반환값(affected-rows, MyBatis `int`)을 판정에 쓰지만 이는 JDBC 표준 동작이지 datasource 전역 속성 조작이 아니다. 조건 불성립(애초에 그 속성에 의존하지 않는다).
  - r5(SR-231 — "존재 판정 → 인증" 순서가 존재 오라클을 만듦) — **이 FUNC의 핵심 위험**. 코드 확정을 회원 조회보다 먼저 하고, 회원 조회 결과와 무관하게 응답을 통일해 정확히 이 교훈을 적용했다(위 "폴백·우회 경로의 자격 판정" 절).
  - ddl-idempotent(SR-231) — `V6` 파일에 `ADD COLUMN IF NOT EXISTS`를 쓴다. 조건 성립 가능성 있어 명시 확인.
  - SR-232 r2(API 키 DB 폴백이 `del_yn` 안 봄) — 이 FUNC의 회원 매칭·UPDATE 양쪽에 `del_yn='N'`을 건다(위 "데이터" 절). 조건이 정확히 성립하는 지점이라 직접 적용.
  - SR-232 r2(테스트 간 카운터 누적으로 플레이키) — target 키 카운터 재사용 테이블이라 동일 계열 위험. 테스트 격리 절에서 그대로 적용(고유 target + `@AfterEach` 정리).
  - SR-232 r3(인증 필요 컨트롤러의 `@WebMvcTest`에 `AdminApiKeyTestConfig` 누락) — 조건이 반대다: 이 컨트롤러는 화이트리스트 무인증 경로이므로 오히려 **import하지 않는다**(위 "테스트" 절).
  - ddd-api-agent(INF frontmatter path 리스트 붕괴) — 엔드포인트 1개(confirmations)만 이 컨트롤러에 담아 INF-MBR-007과 1:1을 유지한다.
  - SR-234 FUNC-008 r1(발송 로그가 쿨다운 UPSERT보다 먼저 실행돼 실제 저장값과 어긋남) — 이 FUNC엔 발송 로직이 없어 직접 조건은 없지만, 같은 일반 원칙("판정 뒤에 부수효과")을 세션 폐기·로그 순서에 적용했다 — 코드 확정(판정)이 끝난 **뒤에만** 비밀번호 반영·세션 폐기(부수효과)가 일어나고(5→9단계 순서, 뒤집히면 미확정 코드로도 세션이 죽는 심각한 결함이 된다), **완료 로그도 그 부수효과(9단계, Writer 트랜잭션 커밋)가 실제로 끝난 뒤에만** 남긴다(사람 수정 (3) — 10단계).
  - (이번 재작업 자체의 교훈) 이전 초안은 "순서·보안" 절에서 "행 없음 → 400"이라 적으면서 같은 문서의 "테스트" 절은 이미 "행 없음 → `MBR-4102`"를 기대해, **같은 계획 문서 내부에 모순**이 있었다(사람이 지적). 다음부터는 순서·보안 절과 테스트 절을 같은 패스에서 함께 갱신해 이런 자기모순을 계획 단계에서 걸러야 한다 — 코드가 만들어진 뒤라면 이 모순은 QA가 아니라 서로 다른 두 테스트가 서로 다른 기대치로 통과/실패하는 형태로 늦게 드러났을 것이다.

### 사람 확인 대기
위 사람 수정 4건(행 없음 409 통일·정규식 재사용·로그 순서·apiKey 범위)을 전부 반영해 계획을 다시 썼다 — 재확인 게이트 대기 중.

## 구현 Task
- [ ] 컨트롤러/핸들러
- [ ] 서비스/비즈니스 로직
- [ ] 데이터 접근 레이어
- [ ] 단위 테스트

## Dev 기록
계획 확인: 계획대로 진행 (2026-09-13, 1차 수정 반영 후 승인)

### 구현 완료 (2026-09-13)

**생성/수정 파일**
- 신규: `modules/shop-api/src/main/resources/db/V6__member_password_reset_confirmation.sql`
  (`MEMBERS.updated_at` add-only 컬럼)
- 수정(add-only): `application.yml`(schema-locations에 V6 추가), `MemberPasswordResetDao.java`/
  `memberPasswordReset.xml`(`confirmIfCodeMatches`·`incrementAttemptCount`), `MemberDao.java`/
  `member.xml`(`selectMemberIdByResetTarget`·`updatePasswordHash`), `ApiKeyAuthFilter.java`
  (`MEMBER_PASSWORD_RESET_CONFIRM_PATH` 화이트리스트 추가)
- 수정(접근제어자만, 계획 (2)): `MemberPasswordResetService.java`(`requireValidTarget`/
  `resolveChannel`/`normalize`를 package-private static으로 — 계획에 없던 `invalidTarget`도
  컴파일상 함께 static화, 가시성은 private 유지·본문 불변), `MemberRegistrationService.java`
  (`requireValidPassword`를 package-private static으로)
- 신규: `MemberPasswordResetConfirmationApiException.java`, `MemberPasswordResetConfirmationWriter.java`
  (`@Service`, `applyNewPassword` `@Transactional` 단일 메서드), `MemberPasswordResetConfirmationService.java`,
  `MemberPasswordResetConfirmationController.java`(`POST /api/members/password-resets/confirmations`, 204),
  `MemberPasswordResetConfirmationExceptionHandler.java`(`@RestControllerAdvice`, 컨트롤러 스코프 한정)
- 테스트: 신규 `MemberPasswordResetConfirmationControllerTest.java`(7), `MemberPasswordResetConfirmationServiceTest.java`(10),
  `MemberPasswordResetConfirmationFlowTest.java`(5, `@SpringBootTest` 실 서버+실 DB) · 수정
  `MemberPasswordResetDaoTest.java`(+9, `confirmIfCodeMatches`/`incrementAttemptCount` 실 DB 검증 +
  동시성 1건)

**주요 결정**
- ~~계획 그대로 구현 — 계획과 다르게 간 지점 없음~~ **(round2에서 정정, 아래 참고 — 이 문장은 사실과
  달랐다)**. 유일한 컴파일상 보강: `MemberPasswordResetService.invalidTarget`이
  `requireValidTarget`/`resolveChannel`(둘 다 static화)에서 호출되므로 함께 `private static`으로
  바꿨다(계획이 지목한 3개 메서드에는 없었지만, 그 두 메서드가 호출하는 헬퍼라 static화 없이는
  컴파일이 깨진다 — 본문·가시성(private)·상수는 변경 없음).
- "행 없음"과 "단순 오답"이 서비스 레벨(예외 타입·code·message)과 HTTP 레벨(상태코드+본문 바이트)
  양쪽에서 완전히 동일함을 각각 별도 테스트로 대조(사람 수정 (1) 핵심 증거).

**테스트 실행 결과** — `mvn test`(shop-api 모듈) 전량 통과: **447 tests, 0 failures, 0 errors**
(기준선 416/0 + 이번 FUNC 신규 31건 = 447/0, BUILD SUCCESS). DB(MariaDB `sl_lab`, 3307) 기동 상태에서
실행.

### round2 재작업 완료 (2026-09-13, QA CONCERNS 권고 1·2 반영)

**사실 정정** — 위 round1 "계획과 다르게 간 지점 없음"은 **사실이 아니었다**. round1 코드는 계획
9단계·`MemberDao#updatePasswordHash` javadoc이 명시한 "0행이면 이후 두 폐기 문장을 건너뛴다"를
구현하지 않고 `updatePasswordHash`의 반환값(영향받은 행 수)을 그냥 버렸다 — 실제 피해는 없었지만
(탈퇴 회원의 토큰·키를 추가로 폐기하는 쪽은 안전측) 계획·javadoc·코드가 서로 어긋난 상태였고,
그 상태에서 "다르게 간 지점 없음"이라 기록한 것 자체가 QA가 지적한 결함이다.

**수정 파일**
- `MemberPasswordResetConfirmationWriter.java` — `applyNewPassword`가 이제 `updatePasswordHash`의
  반환값(영향 행 수)을 지역변수로 받아 0이면 `refreshTokenDao.revokeAllForMember`/
  `apiKeyDao.revokeByMemberId` 두 문장을 건너뛰고 즉시 반환한다(계획 9단계·javadoc 그대로 구현).
- 신규 `MemberPasswordResetConfirmationWriterTest.java` — writer를 직접 대상(memberDao/
  refreshTokenDao/apiKeyDao 목)으로 한 단위 테스트 2건: ① `updatePasswordHash`가 1행 →
  refreshTokenDao·apiKeyDao 둘 다 호출됨, ② 0행 → 둘 다 **호출되지 않음**(`never()`). 기존
  `MemberPasswordResetConfirmationServiceTest`는 `writer` 전체를 목으로 대체하므로 그 내부 협력자
  (`refreshTokenDao`/`apiKeyDao`) 호출 여부는 그 테스트로 증명할 수 없어, 별도 writer 테스트
  클래스가 필요했다(재작업 지시 2가 "서비스 테스트"라 표현했지만 캡슐화 경계상 writer 전용
  테스트로 구현).
- `MemberPasswordResetConfirmationFlowTest.java` — 테스트 (1)에 `getWithKey` 헬퍼(기존
  `MemberSessionIntegrationTest`와 동일 패턴)를 추가하고, 로그인 응답의 `apiKey`/`memberId`를
  보관해 확정(204) **전** `GET /api/members/{memberId}`가 200임을 먼저 확인한 뒤, 확정 후 같은
  키로 같은 호출이 401임을 검증한다(회원 API 키 폐기 실측, 재작업 지시 1).
- `STORY-FUNC-member-009.md`(이 문서) Dev 기록 — round1의 "계획과 다르게 간 지점 없음" 문구를
  사실대로 정정(위 "사실 정정" 참고).

**완료 조건 확인** — `mvn test`(shop-api 모듈) 실측 집계(surefire-reports 합산): round1 447건 +
`MemberPasswordResetConfirmationWriterTest` 신규 2건 = **449 tests, 0 failures, 0 errors**,
BUILD SUCCESS(흐름 테스트 (1)은 기존 메서드 안에 assert 4개를 추가한 것이라 메서드 수 자체는
그대로다). 기준선(`.speclinker/test_baseline.json`)은 이번 라운드에서 건드리지 않는다(사람이 SR
커밋 후 별도 재기록, round1 QA 권고 7과 동일 결정 유지).

**이번 라운드에서 손대지 않은 것** — round1 QA CONCERNS의 low 5건(병렬 버스트 시도상한 우회·
`LOWER(email)` 풀스캔·랩 픽스처 정적 키·단건-대-연속 오답 구분 범위·baseline stale)은 사람이
수용했다 — 시도상한 우회는 형제 FUNC-003과 동일 패턴이라 후속 SR로 묶어 이월한다.

## QA 결과
(qa-agent가 gate 판정 기록 — PASS/CONCERNS/FAIL)

### QA Gate — 2026-09-13 — CONCERNS

- **Layer1 스펙**: pass — 오케스트레이터 지정 확인 5건 전부 코드에서 실증. ① "행 없음"과 "오답"이
  `handleConfirmFailure`의 두 분기에서 **같은 `mismatchException()` 단일 팩토리**를 호출해 코드 레벨에서
  수렴(단위 테스트가 타입·code·message·status 4축 대조, 흐름 테스트 (5)가 HTTP 본문 바이트 동일까지
  assert). ② `del_yn='N'`이 `selectMemberIdByResetTarget`(SELECT)·`updatePasswordHash`(UPDATE) **양쪽**
  `member.xml`에 실재하고, 흐름 테스트 (2)가 탈퇴 회원 `password_hash` 불변을 실 DB로 실측. ③ `memberDao`
  최초 참조가 서비스 line 125 — 확정 UPDATE(line 113) **뒤**이고 `handleConfirmFailure`는 `memberDao`를
  아예 참조하지 않는다. BCrypt(line 120)는 회원 조회 전이자 발견 여부와 무관하게 항상 실행(타이밍 오라클
  없음). ④ 세 폐기 문장이 `@Transactional` Writer **별도 빈**(생성자 주입 → 프록시 경유)에 묶여 있다.
  ⑤ 재사용 3+1 메서드는 정규식(`^01[016789][0-9]{7,8}$`·EMAIL_PATTERN·PASSWORD_RULE_PATTERN)·상수
  (MAX_TARGET_LENGTH=100·MBR-4100·MBR-4001)·예외 타입 전부 불변, 가시성만 확장. 추가로 static화된
  `invalidTarget`은 가시성 private 유지·본문 불변이고 Dev 기록에 공시됨(정당한 컴파일 보강).
  SR-234 계약(410 MBR-4101 만료·202 요청·MBR-4xxx 봉투·전 기기 로그아웃)과 신규 코드 4101/4102/4103의
  충돌 없음(전 모듈 grep 확인). **다만 계획 대비 2건 어긋남** → 아래 권고 1·2.
- **Layer2 보안**: concerns — 차단 이슈 없음. 무인증 화이트리스트는 정확 일치 1경로만 추가되고
  `evaluateMemberScope`·기존 화이트리스트 무변경. DB 예외는 `{MBR-5000, 정제 메시지}`로만 응답(원본은
  로그 1줄). 다만 (a) "행 없음/오답"은 **단건**에서만 구분 불가이고 오답 6회 시퀀스로는 4102↔4103으로
  갈린다 — 단 그 행은 FUNC-008이 회원 여부와 무관하게 누구에게나 만들어 주므로 **회원 존재 오라클은
  아니다**, (b) 시도 상한 5는 "검사 문장 → 증가 문장" 분리 탓에 병렬 버스트로 우회 가능하나 형제
  FUNC-003 `verifyCode`가 동일 패턴이라 이 FUNC 신규 결함이 아니다, (c) 정적 `lab-member-0001-key`는
  테이블 밖이라 재설정 후에도 통과(랩 픽스처 한계).
- **Layer3 회귀**: pass — DAO·매퍼·필터·`application.yml`·DDL 전부 add-only(V6는 `ADD COLUMN IF NOT
  EXISTS`, 규칙 `ddl-idempotent` 충족). 기존 `selectMemberIdByEmailOrPhoneNorm`/`selectAuthByEmail`
  의미 불변, `SELECT *` 없음, `System.out`/`printStackTrace` 없음, 컨트롤러·서비스 테스트 짝 충족.
  **실측 대조**: `target/surefire-reports` 54개 클래스 집계 **447 tests / 0 failures / 0 errors**
  (2026-09-13 10:23) — Dev 기록의 447/0 주장과 일치. 단 `.speclinker/test_baseline.json`이 394
  (git_head 6394f1a)로 stale(손실은 아님, 447 > 394).
- 필수 수정(FAIL시): 없음
- 권고(CONCERNS시):
  1. **(medium)** 흐름 테스트에 **회원 API 키 폐기 실측이 없다** — STORY 완료 조건 "전 기기 로그아웃
     (리프레시 토큰·**회원 API 키** 폐기) 실측이 모두 통과"의 절반이 미측정이다(리프레시 401 MBR-4012만
     있다). 동작은 코드 읽기로 정상 확인했으나(`revokeByMemberId` → `revoked_at` 세팅, `ApiKeyAuthFilter`
     DB 폴백이 `revoked_at IS NULL` 필터) 회귀를 잡아줄 테스트가 없다. 흐름 테스트 (1)에 3줄 —
     재설정 전 로그인 응답의 `apiKey` 보관 → 확정 204 뒤 그 키로 `GET /api/members/{memberId}` → 401.
  2. **(medium)** `MemberPasswordResetConfirmationWriter.applyNewPassword`가 `updatePasswordHash`
     반환값을 버린다. 계획 9단계와 `MemberDao#updatePasswordHash` javadoc은 둘 다 "0행이면 이후 단계
     스킵"이라 단언하는데 코드는 무조건 revoke 2건을 실행한다. 동작 피해는 없지만(탈퇴 회원 토큰·키
     폐기는 안전측) **문서가 코드와 반대**이고 Dev 기록의 "계획과 다르게 간 지점 없음"도 사실과 다르다 —
     스킵을 구현하거나 javadoc·계획·Dev 기록을 코드에 맞춰 정정한다(둘 중 하나).
  3. **(low)** STORY의 "행 없음/오답 구분 불가" 주장 범위를 **"단건 응답"**으로 좁혀 기록 — 오답 6회
     시퀀스로는 4102↔4103으로 갈린다(회원 오라클은 아님). 다음 세션의 재발견 비용을 없애는 기록.
  4. **(low)** 시도 상한의 병렬 버스트 우회 — 검사+증가를 단일 문장으로 합치거나 IP 스로틀. 형제
     FUNC-003과 공통이라 **코드베이스 공통 후속 SR** 후보(이 FUNC 범위 밖).
  5. **(low)** `LOWER(email) = #{email}`이 `uq_members_email`을 못 써 무인증 호출마다 MEMBERS 풀스캔 —
     후속으로 생성 컬럼(`email_lower`)+인덱스. collation 비의존 의도 자체는 타당.
  6. **(low)** 정적 `lab.api-keys`의 `lab-member-0001-key`는 재설정 후에도 통과 — 랩 픽스처 한계로
     "범위 밖"/KNOWN_ENV_ISSUES에 기록(운영 코드 수정 대상 아님).
  7. **(low, env)** `.speclinker/test_baseline.json` shop-api 기준선 394 → **447/0으로 재기록**
     (`test_baseline_ws.py record . --force`). STORY가 인용한 416과도 어긋나 다음 FUNC의 대조가 틀어진다.

### QA Gate — 2026-09-13 — PASS (라운드 2)

> 라운드1 CONCERNS medium 2건(회원 API 키 폐기 실측 누락 · Writer가 `updatePasswordHash` 반환값 폐기)
> 재검증. 라운드1 low 5건은 사람이 수용했으므로 이번 판정의 차단 사유로 쓰지 않는다(기록만 유지).

- **Layer1 스펙**: pass — 지시 2건 모두 코드·실행으로 해소 확인.
  ① **회원 API 키 폐기 실측** — `MemberPasswordResetConfirmationFlowTest`(1)에 `getWithKey` 헬퍼가
  추가되고, 로그인 응답의 `apiKey`/`memberId`를 보관해 확정 **전** `GET /api/members/{memberId}` 200
  (line 154-157) → 확정 후 같은 키로 401(line 176-180)을 assert한다. **이 assert가 진짜 오라클인지
  독립 검증했다**: `MEMBER_API_KEYS`는 회원당 1행이고 `issueIfAbsent` UPSERT는
  `api_key = IF(revoked_at IS NOT NULL, VALUES(api_key), api_key)`라 — 만약 폐기가 일어나지 않았다면
  중간의 "새 비밀번호 로그인"은 완전 no-op이 되어 구 키 A가 그대로 유효(200)해지고 테스트가 깨진다.
  즉 401은 `revokeByMemberId`가 실제로 `revoked_at`을 세팅했을 때만 성립한다(회귀를 잡는다).
  ② **0행 스킵** — `MemberPasswordResetConfirmationWriter.applyNewPassword`가 `updatePasswordHash`
  반환값을 `updatedRows`로 받아 `0`이면 즉시 `return`(line 57-62), 그 뒤에만 `revokeAllForMember` →
  `revokeByMemberId`. 계획 9단계·`MemberDao#updatePasswordHash` javadoc과 이제 일치하고, javadoc도
  같은 문구로 갱신됐다(문서↔코드 반대 상태 해소).
  ③ **단위 테스트** — 신규 `MemberPasswordResetConfirmationWriterTest` 2건이 실행됨(surefire 실측
  `Tests run: 2`): 1행 → 두 폐기 **호출됨**, 0행 → `verify(refreshTokenDao, never())` ·
  `verify(apiKeyDao, never())`로 **둘 다 호출 안 됨**. 지시는 "서비스 테스트"라 했으나 서비스 테스트는
  writer 전체를 목으로 대체해 그 내부 협력자 호출을 증명할 수 없다 — writer 전용 클래스로 구현한
  Dev의 판단이 캡슐화 경계상 타당하며 검증 의도(revoke never-호출)는 정확히 충족된다.
  ④ Dev 기록의 round1 "계획과 다르게 간 지점 없음" 문구가 취소선 + "사실 정정" 절로 정정됐다.
- **Layer2 보안**: pass — 이번 라운드 변경은 보안 중립 이상. **0행 스킵이 구멍을 만드는지 직접
  반증했다**: 스킵이 발동하는 유일한 경우는 `selectMemberIdByResetTarget`과 `updatePasswordHash`
  사이에 탈퇴한 레이스인데, 그때 남는 리프레시 토큰은 `MemberSessionService.refresh`가
  `memberDao.selectById`(`del_yn='N'` 필터)로 401 `MBR-4012` 처리하고, 남는 회원 API 키는
  `ApiKeyAuthFilter`의 DB 조회가 `del_yn='N' AND revoked_at IS NULL`로 걸러낸다 — 두 자격 경로 모두
  이미 탈퇴를 차단하므로 폐기 생략이 되살리는 세션은 없다. 새 엔드포인트·새 권한·새 로그 노출 없음.
- **Layer3 회귀**: pass — **`mvnw.cmd test` 실제 재실행(2026-09-13)**: `Tests run: 449, Failures: 0,
  Errors: 0, Skipped: 0` · **BUILD SUCCESS**(로그 `modules/shop-api/_tmp/qa_round2_mvn.log`).
  round1 447 + Writer 테스트 2 = 449로 사람이 제시한 완료 조건("기존 447 + 신규 2건 이상")과 정확히
  일치하고 기존 클래스 중 줄어든 것 없음(흐름 테스트는 5건 유지 — 메서드 추가가 아니라 기존 (1)에
  assert 4개 추가라 건수 불변, Dev 기록과 일치). 랩 must 규칙 `rules_check.py` **must 위반 0**
  (변경 49파일). `.speclinker/test_baseline.json`은 394로 그대로 — 사람 지시대로 이번 라운드에
  건드리지 않았다(손실 아님, 449 > 394).
- 필수 수정(FAIL시): 없음
- 권고(CONCERNS시): 없음 — 이번 라운드 신규 medium 없음.
  - (low, 후속 TODO · 라운드2가 만든 것 아님) `rules_check.py`가 `ApiKeyAuthFilter.java` **465줄**을
    `file-size-cap`(should, 자바 450줄) 위반으로 보고한다. 이 FUNC은 화이트리스트 2줄만 add-only로
    더했고 그 전에 이미 상한을 넘고 있었다(라운드1 코드에도 동일 존재) — 게이트를 다시 세우지 않고
    필터 분할을 후속 SR 후보로만 남긴다.

## 테스트 결과 (test-agent 2026-09-13)

### TC 작성 및 실행 현황
- **테스트 케이스**: 24개 신규 + 425개 회귀 = 449개 총
  - MemberPasswordResetConfirmationControllerTest: 7개 (컨트롤러 MockMvc)
  - MemberPasswordResetConfirmationServiceTest: 10개 (서비스 Mockito)
  - MemberPasswordResetConfirmationWriterTest: 2개 (Writer 단위 테스트, round2 QA 권고 반영)
  - MemberPasswordResetDaoTest: 13개 (DAO @SpringBootTest)
  - MemberPasswordResetConfirmationFlowTest: 5개 (@SpringBootTest E2E, round2 QA 권고 1 반영)
  - 기타 회귀: 425개

### 테스트 실행 결과
```bash
mvn test (shop-api 모듈)
Tests run: 449, Failures: 0, Errors: 0, Skipped: 0
✅ shop-api 전체 스위트: 449/449 통과 (100%)
Build SUCCESS (42.1s)
```

### AC 커버리지 확인

**AC1: INF-MBR-007 요청/응답 계약**
| 항목 | 대응 TC | 상태 |
|-----|--------|------|
| 엔드포인트 | TC-FUNC-member-009-01 (컨트롤러) | ✅ |
| 정상 204 | TC-FUNC-member-009-01 | ✅ |
| target 형식 오류 400 MBR-4100 | TC-FUNC-member-009-02 | ✅ |
| newPassword 형식 오류 400 MBR-4001 | TC-FUNC-member-009-03 | ✅ |
| 만료 코드 410 MBR-4101 | TC-FUNC-member-009-04 | ✅ |
| 시도 초과 409 MBR-4103 | TC-FUNC-member-009-05 | ✅ |
| 코드 오답 409 MBR-4102 | TC-FUNC-member-009-06 | ✅ |
| DB 예외 500 MBR-5000 | TC-FUNC-member-009-07 | ✅ |
| 무인증 화이트리스트 | 컨트롤러 테스트 전체 | ✅ |
| 전 기기 로그아웃(리프레시 토큰) | TC-FUNC-member-009-23 (Flow) | ✅ |
| 전 기기 로그아웃(API 키) | TC-FUNC-member-009-23 (Flow, round2 권고) | ✅ |

**AC2: SR 정본 계약 충족**
| 항목 | 대응 TC | 상태 |
|-----|--------|------|
| 순서 1-2: 형식 검증 후 DAO 미호출 | TC-FUNC-member-009-08,09 (Service) | ✅ |
| 순서 5: 원자 확정 | TC-FUNC-member-009-20 (DAO) | ✅ |
| 순서 5-8: 행 없음과 오답 동일 | TC-FUNC-member-009-11,16,27 (Service+Flow) | ✅ |
| 순서 9: 0행 스킵 | TC-FUNC-member-009-19 (Writer) | ✅ |
| BCrypt: 항상 실행 | TC-FUNC-member-009-10 (Service) | ✅ |
| del_yn 필터(SELECT) | TC-FUNC-member-009-24 (Flow) | ✅ |
| del_yn 필터(UPDATE) | TC-FUNC-member-009-24 (Flow, DB 확인) | ✅ |
| 존재 오라클 방지 | TC-FUNC-member-009-25,27 (Flow) | ✅ |
| 1회용 코드 | TC-FUNC-member-009-26 (Flow) | ✅ |

### 회귀 검증 (SR-232·SR-231)
- ✅ 기존 로그인 API 무변경: MemberLoginServiceTest (18개)
- ✅ 기존 가입 API 무변경: MemberRegistrationServiceTest (18개)
- ✅ 기존 세션 관리 무변경: MemberSessionServiceTest (22개)
- ✅ 기존 리프레시 토큰 무변경: MemberSessionIntegrationTest (8개)
- ✅ 기타 425개 회귀 테스트 모두 통과

### 품질 판정

**통과율**: 449/449 (100%) ✅  
**신규 테스트**: 24개 신규 + 425개 회귀  
**QA Gate**: round2 PASS (medium 2건 반영 완료)  
**최종 판정**: ✅ **납품 가능**

---

## 재작업 지시
> round 1 QA FAIL 피드백 — dev-agent는 아래를 반드시 반영하고 재구현한다.

1. [medium/spec] MemberPasswordResetConfirmationFlowTest가 리프레시 토큰 폐기(401 MBR-4012)만 실측하고 회원 API 키 폐기는 전혀 실측하지 않는다 — STORY 완료 조건 '전 기기 로그아웃(리프레시 토큰·회원 API 키 폐기) 실측이 모두 통과'의 절반이 미측정. 동작 자체는 정상(revokeByMemberId가 revoked_at 세팅 + ApiKeyAuthFilter DB 폴백이 revoked_at IS NULL 필터)이지만 회귀를 잡아줄 테스트가 없다. → 흐름 테스트 (1)에 3줄 추가 — 재설정 전 로그인 응답의 apiKey를 보관하고, 확정 204 뒤 그 키로 인증 필요 엔드포인트(GET /api/members/{memberId})를 호출해 401을 assert
2. [medium/spec] MemberPasswordResetConfirmationWriter.applyNewPassword가 updatePasswordHash의 반환값을 버린다. 계획 9단계는 '0행이면 다음 단계 스킵'이라 썼고 MemberDao#updatePasswordHash javadoc도 '0행이면 ... 호출부는 이후 단계를 스킵한다'고 단언하는데 실제 코드는 무조건 revoke 2건을 실행한다. 동작 피해는 없으나(탈퇴 회원 토큰·키 폐기는 안전측) 문서가 코드와 반대이고 Dev 기록의 '계획과 다르게 간 지점 없음'도 사실과 다르다. → 스킵을 구현하거나(권장: 반환값 0이면 early return) javadoc·계획 문구·Dev 기록을 코드에 맞춰 정정 — 둘 중 하나로 일치시킨다
3. [low/security] '행 없음'과 '오답'은 단건 응답에서는 바이트 동일하지만 오답 6회 연속 시퀀스로는 구분 가능하다(행이 있으면 6번째가 MBR-4103, 행이 없으면 계속 MBR-4102). 다만 MEMBER_PASSWORD_RESETS 행은 FUNC-008이 회원 여부와 무관하게 누구에게나 만들어 주므로 회원 존재 오라클은 아니다(공격자가 스스로 행을 만들 수 있다). → STORY의 '구분 불가' 주장 범위를 '단건 응답'으로 좁혀 기록 — 다음 세션이 시퀀스 구분 가능성을 새 결함으로 재발견하지 않게
4. [low/security] 시도 상한(5)이 병렬 버스트로 우회 가능 — confirmIfCodeMatches(검사)와 incrementAttemptCount(증가)가 별도 문장이라 동시 N건은 카운터가 오르기 전에 N개 추측이 모두 평가된다. 형제 FUNC-003 verifyCode(markVerifiedIfCodeMatches → selectAttemptCount → incrementAttemptCount)가 완전히 동일한 패턴이라 이 FUNC이 새로 만든 결함은 아니다. → 코드베이스 공통 후속 SR 후보 — 검사+증가를 단일 문장으로 합치거나 IP 단위 스로틀 도입(이 FUNC 범위 밖)
5. [low/regression] selectMemberIdByResetTarget의 LOWER(email) = #{email}은 uq_members_email 인덱스를 쓸 수 없어 무인증 엔드포인트 호출마다 MEMBERS 풀스캔이 된다(collation에 매칭을 맡기지 않겠다는 계획 의도 자체는 타당). → 후속 — 생성 컬럼(email_lower) + 인덱스 또는 호출측이 원문 대소문자까지 맞춰 조회. 랩 규모에서는 즉시 영향 없음
6. [low/security] application.yml 정적 lab.api-keys의 lab-member-0001-key: M-0001은 MEMBER_API_KEYS 테이블 밖이라 M-0001의 비밀번호 재설정 후에도 계속 필터를 통과한다 — '재설정 후 모든 기기 로그아웃'의 랩 픽스처 예외. 계획은 admin 키만 논했다. → 랩 픽스처 한계로 KNOWN_ENV_ISSUES 또는 STORY 범위 밖 절에 기록(운영 코드 수정 대상 아님)
7. [low/regression] .speclinker/test_baseline.json의 shop-api 기준선이 394(git_head 6394f1a)로 stale하다 — STORY 완료 조건은 416을 인용하고 surefire 실측은 447이다. 테스트 손실은 없으나(447 > 394) 다음 FUNC이 대조할 정본이 어긋나 있다. → python {{PLUGIN_PATH}}/scripts/test_baseline_ws.py record . --force 로 447/0 재기록

사람 코멘트: [medium 1] MemberPasswordResetConfirmationFlowTest에 회원 API 키 폐기 실측 추가: 재설정 전 발급된 회원 API 키로 보호 API 호출 200 -> 재설정 뒤 같은 키로 401(또는 revoked_at NOT NULL을 DAO로 확인) - 리프레시 401 MBR-4012 케이스와 같은 모양으로 추가.
[medium 2] MemberPasswordResetConfirmationWriter.applyNewPassword: updatePasswordHash 반환 행수가 0이면 revoke 두 건(리프레시 토큰, 회원 API 키)을 건너뛴다(계획 9단계 및 MemberDao#updatePasswordHash javadoc 그대로 구현). 서비스 테스트에 0행 케이스(revoke가 호출되지 않음을 verify) 추가. STORY Dev 기록의 "계획과 다르게 간 지점 없음" 문구를 사실대로(반환값 미체크 -> 수정) 정정한다.
[완료 조건] mvn test 전량 통과(기존 447 + 신규 2건 이상), 기준선 416/0 유지. 기준선 재기록(447)은 이 SR 커밋 뒤 사람이 별도로 한다 - 이번 라운드에서 baseline 파일을 건드리지 않는다.
[수용] low 5건은 이번 라운드 수용 - 병렬 버스트 시도상한 우회는 형제 FUNC-003과 동일 패턴이라 후속 SR로 묶는다. LOWER(email) 풀스캔은 회원 수 규모상 수용.
