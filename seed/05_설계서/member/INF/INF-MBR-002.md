---
inf-id: INF-MBR-002
name: 가입 요청(인증코드 검증 포함)
layer: api
method: POST
path: /api/members/signup
domain: member
domain-code: MBR
srs-f: [TBD]
screens: []
tables:
  - MEMBERS
  - MEMBER_SIGNUP_VERIFICATIONS
  - ID_SEQUENCES
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberRegistrationController.java:25-43
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberRegistrationService.java:92-332
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSignupCompletionWriter.java:31-70
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberRegistrationApiException.java:14-43
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberRegistrationExceptionHandler.java:32-61
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberSignupCompletionDao.java:39-76
  - modules/shop-api/src/main/resources/mapper/memberSignupCompletion.xml:6-70
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberDao.java:15-77
  - modules/shop-api/src/main/resources/mapper/member.xml:21-63
  - modules/shop-api/src/main/resources/db/V3__members_signup.sql:1-107
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSignedUpEvent.java:11-27
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberSignedUpEventLogger.java:15-31
---

> [반영: FUNC-member-003] 2026-09-12

# INF-MBR-002: POST /api/members/signup — 가입 요청(인증코드 검증 포함)

> **개요:** 이메일 또는 휴대폰번호 + 인증코드(6자리) + 비밀번호·이름·마케팅 수신 동의를 받아 회원을 생성한다. 인증코드 "발송"은 별도 API(INF-MBR-001)의 책임이지만, 코드 **검증**(대조·소비)은 발송 쪽이 아니라 이 API 자신이 수행한다 — `MEMBER_SIGNUP_VERIFICATIONS` 행은 INF-MBR-001이 써 둔 것을 이 API가 조건부 UPDATE로 대조·소비할 뿐, 그 테이블을 소유한 파일은 건드리지 않는다. round1~4에 걸친 QA 재작업으로 "① 코드 검증(비트랜잭션) → ② 존재 판정(비트랜잭션, 안내용) → ③ 가입(트랜잭션, 별도 스프링 빈)"의 3단계 순서가 사람이 직접 지정한 것으로 고정됐다 — 이 순서 자체가 보안 요건(존재 오라클 방지)이므로 임의로 바꾸면 안 된다.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberRegistrationController.java:34-40`

## 요청

- Method: POST
- Path: /api/members/signup
- Content-Type: application/json
- 인증: 없음(무인증) — 신규 가입자는 아직 API 키가 없으므로 `ApiKeyAuthFilter` 화이트리스트(`MEMBER_SIGNUP_REQUEST_PATH`, 정확 일치)에 등록되어 있다. **레이트리밋 없음**(아래 오류 표·비즈니스 규칙 참고).

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| target | Body | string | Y | 인증코드를 받은 대상 — 이메일 주소 또는 휴대폰번호(하이픈 없이 숫자만). 값 자체로 채널(EMAIL/SMS)을 자동 판별(INF-MBR-001과 동일 정규식을 이 서비스에 복제해 사용) |
| code | Body | string | Y | INF-MBR-001이 발송한 6자리 인증코드. 이 API가 직접 대조·소비한다 |
| password | Body | string | Y | 8~64자, 영문+숫자 포함(정규식 위반 시 400) |
| name | Body | string | Y | 회원 이름, 최대 50자(빈 값·공백만·50자 초과 시 400 — `MEMBERS.member_name VARCHAR(50)` 정합) |
| marketingOptIn | Body | boolean | N | 마케팅 수신 동의. 생략(null) 시 `false`로 처리 |

**서버 내부 검증 순서(그대로 구현된 순서 — 응답 코드가 이 순서에 의존한다):**
1. `target` 공백/누락 → 409 `MBR-4091`(형식이 성립 않으면 인증 기록도 존재할 수 없다는 논리로 "인증 필요"에 수렴)
2. `target` 형식이 이메일·휴대폰 정규식 둘 다 불일치 → 409 `MBR-4091`(동일 논리)
3. `name` 공백/누락/50자 초과 → 400 `MBR-4001`
4. `password` 규칙 위반 → 400 `MBR-4001`
5. (STEP 1) 코드 검증 실패 → 409 `MBR-4091` 또는 `MBR-4093`
6. (STEP 0) 코드 검증 성공 후에만 중복 판정 → 409 `MBR-4092`(+`login_url`) 또는 `MBR-4094`
7. BCrypt 해시 계산 → (STEP 2) 가입 트랜잭션 → 성공 시 201

## 응답 (201 Created)

```json
{
  "memberId": "M-0053",
  "channel": "EMAIL",
  "target": "user@example.com"
}
```

- `memberId`: 신규 채번된 회원 ID, `"M-" + %04d` 포맷(`ID_SEQUENCES` 원자 채번, `MEMBER_ID` 시퀀스)
- `channel`: 판별된 채널 — `EMAIL` 또는 `SMS`
- `target`: 요청받은 값의 에코(trim 처리됨)
- 응답에 이메일·비밀번호 해시·마케팅 동의값 등은 포함하지 않는다

## 비즈니스 규칙

- **3단계 흐름과 트랜잭션 경계(사람이 직접 지정, round3~4 최종 확정 — 임의 변경 금지):**
  - **STEP 1 — 코드 검증**(`MemberRegistrationService#verifyCode`, 비트랜잭션): `markVerifiedIfCodeMatches` 조건부 UPDATE(`channel`+`target`+`code`+미만료+`consumed_at IS NULL`+`verified_at IS NULL`+`attempt_count < 5`)로 검증. 0행이면 `incrementAttemptCount`(SR-298 — `channel`+`target`+`consumed_at IS NULL`+`verified_at IS NULL`+`attempt_count < 5` 조건부 UPDATE로 증가와 상한판정을 한 문장에 원자화, read-modify-write 아님)를 시도한다. 이 원자 UPDATE가 1행이면 정상 증가 후 409 `MBR-4091`. **0행이면(사유가 상한 도달 하나가 아니다 — 이미 소비·이미 인증된 행이거나 매치 행 자체가 없어도 0행이다) 그 순간만 `selectAttemptCount`로 현재 값을 읽어 원인만 재분기**한다: `attempt_count >= 5`(상한 도달)면 409 `MBR-4093`, 그 밖의 모든 0행 사유는 409 `MBR-4091`(재증가하지 않는다 — 증가는 이미 원자 UPDATE에서 끝났다). 이 읽기는 경합 판정이 아니라 오류 코드 표시용일 뿐이며, "5회 도달 후 카운터가 더 늘지 않는다"는 성질은 이제 이 UPDATE의 `attempt_count < 5` 조건절 자체가 보장한다(select→분기→update 패턴 아님). 각 UPDATE는 autocommit 개별 문장 — 이후 어떤 예외로도 롤백되지 않는다.
  - **STEP 0 — 존재 판정**(`precheckDuplicate`, 비트랜잭션 읽기, **STEP 1 성공 뒤에만 호출**): `MEMBERS`를 `email`/`phone_norm`으로 직접 조회해 이미 있으면 이메일→409 `MBR-4092`(+`login_url`), 휴대폰→409 `MBR-4094`. 안내용일 뿐 최종 보장은 STEP 2의 UNIQUE 위반 캐치다.
  - **STEP 2 — 가입**(`MemberSignupCompletionWriter#completeSignup`, `@Transactional`, 이 메서드만 갖는 별도 스프링 빈): ①`ID_SEQUENCES` 원자 채번(`touchMemberIdSeq`+`selectLastMemberIdSeq`, 같은 커넥션 필수 — `LAST_INSERT_ID()` 세션 스코프) ②`MEMBERS` INSERT ③`MEMBER_SIGNUP_VERIFICATIONS.consumed_at` UPDATE. **이 세 문장만** — 별도 빈으로 분리한 이유는 self-invocation(같은 클래스 안 호출은 프록시를 거치지 않아 `@Transactional`이 적용되지 않는 Spring AOP 함정)을 원천 차단하기 위함(과거 라운드에서 이 함정으로 시도횟수 카운터가 롤백되는 결함이 있었다).
  - `MemberRegistrationService#signUp` 자체는 `@Transactional`이 전혀 없는 순수 오케스트레이션 메서드다.

- **존재 오라클 방지(사람이 명시 승인한 보안 우선 트레이드오프)**: STEP 1(코드 검증)이 STEP 0(존재 판정)보다 항상 먼저 실행된다. 코드 검증에 실패하면(오답·만료·미요청·이미 소비/인증됨·시도상한 도달 등 **어떤 사유든**) STEP 0은 호출조차 되지 않는다 — 즉 **코드 검증 실패는 그 target의 가입 여부와 무관하게 항상 동일한 409 `MBR-4091`**(같은 문구, 존재 여부에 따른 추가 조회가 없어 응답 시간대도 갈리지 않음)이다. 코드를 받지 못한 요청자는 이 target의 가입 여부를 알아낼 방법이 없다.

- **부작용(사람이 명시 수용, round4 — SR-295로 발생 조건 축소)**: "이미 가입 완료된 target"이 **재발송 없이** 예전 코드로 재시도하면(그 코드의 `consumed_at`이 이미 채워져 있으므로) STEP 1 자체가 실패해 409 `MBR-4092`가 아니라 **409 `MBR-4091`**을 받는다. 자가치유는 있다 — 코드 만료(5분) + 정리배치(`MemberSignupMaintenanceScheduler`, 10분 주기)가 행을 지우거나, **재발송을 받으면 즉시**(SR-295, `consumed_at`이 리셋되므로) 다음 시도에서 정상적으로 `MBR-4092`(+`login_url`)가 나온다. 실사용 영향은 "가입 직후 같은 날, 재발송 없이 재시도" 구간으로 한정된다.

- **코드값 의미**:
  - `attempt_count >= 5`: 해당 (channel,target) 코드는 잠금 상태 — 정답이어도 더 이상 매치되지 않음(409 `MBR-4093`). **회복 경로(SR-295)**: 재발송(`writeCode`, INF-MBR-001 소유)이 같은 행의 `attempt_count`를 0으로, `consumed_at`/`verified_at`을 `NULL`로 리셋하므로, 상한에 걸린 target도 재발송을 받으면 새 코드로 STEP 1 조건(`attempt_count < 5`)을 다시 만족해 정상 검증 가능해진다. 이 STEP(STEP 1 SQL·조건식·순서)은 손대지 않았고, 선행 상태만 바뀐다.
  - **병렬 버스트 우회 차단(SR-298)**: `incrementAttemptCount`의 `attempt_count < 5` 조건절이 단일 PK(`channel`,`target`) 행 락 위에서 재평가되므로, 동시에 도착한 오답 요청들도 이 한 문장의 짧은 수명 동안만 직렬화된다 — 상한(5)을 넘겨 증가할 수 없다(이전에는 "현재값 읽기 → 상한 미만이면 별도 증가" 순서라 두 문장 사이 레이스로 5를 초과해 증가할 수 있었다). 0행 반환 시의 `selectAttemptCount` 읽기는 이 보장과 무관하다 — 그 시점엔 증가 여부가 이미 원자적으로 확정된 뒤이며, 오류 코드(4091 vs 4093) 표시에만 쓰인다. 부가로, 이 읽기가 있을 때(verification 행 없음) 3문장 / 없을 때(행 있고 1행 매치) 2문장으로 문장 수가 갈리지만, 어느 쪽도 `MEMBERS` 테이블을 조회하지 않아 존재 오라클 방지에는 영향이 없다(관측 가능한 응답 시간대 차이는 verification 테이블 PK 조회 1회 수준 — 회원 존재 신호를 만들지 않는다).
  - `consumed_at IS NOT NULL`: 코드가 이미 가입에 사용됨 — 같은 코드로 재검증 시도는 항상 0행(재사용 차단).
  - `verified_at`: STEP 1이 세팅하는 검증 시각. STEP 0에서 예외가 나도(트랜잭션이 아니므로) 롤백되지 않고 커밋된 채 남는다.
- **login_url 필드**: 이 랩에는 실제 로그인 화면 라우트가 없다(grep 0건) — `"/login"`은 자리표시(placeholder) 값이며 실재하지 않는 경로다. MBR-4092 응답에만 채워진다.
- **비밀번호 해시 시점**: BCrypt 계산은 STEP 1(코드 검증) 이후, STEP 2(가입 트랜잭션) 진입 직전에 수행한다 — 무인증 엔드포인트에서 시도 상한에 걸리는 요청까지 매번 BCrypt(수십~백 ms)를 돌리지 않기 위함.
- **미분류(PK) 충돌 재시도**: STEP 2에서 `DuplicateKeyException` 발생 시 메시지에 실린 제약 이름으로 `uq_members_email`→4092, `uq_members_phone_norm`→4094로 분기하고, 둘 다 아니면(주로 PK 충돌) 완전히 새 트랜잭션으로 1회만 재채번 재시도한다 — 재시도도 실패하면 rethrow하여 일반 500 `MBR-5000`으로 떨어뜨린다(업무 409로 오분류하지 않음).
- **레거시/미해결 주의**: ① `MemberSignupCompletionDao#selectVerifiedAt`/`#selectConsumedAt`는 테스트/조회 전용이며 운영 경로에서 호출되지 않는 죽은 코드로 남아 있다. ② 탈퇴 회원(`del_yn='Y'`)의 email/phone_norm이 UNIQUE 인덱스를 계속 점유해 재가입이 영구 불가하다(사전 판정도 `del_yn`을 보지 않아 UNIQUE와 판정을 의도적으로 일치시킴) — 이 API 범위 밖, 백로그. ③ 이 엔드포인트에는 어떤 레이트리밋도 없다(존재 오라클은 막혔지만, 제3자가 "가입 진행 중인" target의 시도 5회를 대신 소진시켜 그 target의 가입을 일시 방해하는 경로는 남아 있다 — 일일 발송 상한 5회로 영향은 제한적).

## 트랜잭션 순서

1. (비트랜잭션, autocommit) STEP 1 — `MEMBER_SIGNUP_VERIFICATIONS` 조건부 UPDATE(`markVerifiedIfCodeMatches`). 실패 시 원자 조건부 UPDATE(`incrementAttemptCount`, `attempt_count < 5` 포함 — SR-298 원자화)로 증가+상한판정을 한 문장에서 시도. 그 UPDATE가 0행이면(사유 다양 — 상한 도달/이미 소비/이미 인증/행 없음) `selectAttemptCount` 읽기 1회로 오류 코드만 재분기(재증가 없음). 즉시 응답 종료(이후 단계 진행 안 함).
2. (비트랜잭션, 읽기) STEP 0 — `MEMBERS`를 `email`/`phone_norm`으로 조회(`selectMemberIdByEmailOrPhoneNorm`). 존재하면 즉시 응답 종료(이후 단계 진행 안 함).
3. (트랜잭션 시작, `MemberSignupCompletionWriter#completeSignup`) ① `ID_SEQUENCES` UPSERT + `SELECT LAST_INSERT_ID()`로 `member_id` 채번 ② `MEMBERS` INSERT ③ `MEMBER_SIGNUP_VERIFICATIONS.consumed_at` UPDATE — 세 문장 커밋.
4. (트랜잭션 밖, 커밋 후) `MemberSignedUpEvent{memberId, target}` 발행 — 로그 리스너 1개(`MemberSignedUpEventLogger`)만 구독, 개인정보는 앞 2자만 노출하고 마스킹. 쿠폰 발급 로직은 이 API 범위 밖(별도 프로모션 SR 몫).

## 오류 응답

응답 봉투는 모두 `{ "code": "...", "message": "..." }` 형태(`MBR-4092`만 `login_url` 필드 추가, `MemberRegistrationExceptionHandler`).

| 코드 | HTTP | 사유 | 발생 조건 |
|------|------|------|---------|
| MBR-4001 | 400 | 이름/비밀번호 규칙 위반 | `name`이 비었거나 50자 초과, **또는** `password`가 8~64자·영문+숫자 포함 규칙 위반 (두 경우 모두 같은 코드로 응답) |
| MBR-4091 | 409 | 인증이 필요합니다 | `target` 형식 오류, 또는 STEP 1 코드 검증 실패(오답·만료·미요청·이미 소비/인증됨) — **target의 가입 여부와 무관하게 항상 이 코드**(존재 오라클 방지) |
| MBR-4093 | 409 | 재발송 필요 | STEP 1 진입 시점에 `attempt_count >= 5`(이미 시도 상한 도달, 정답 코드를 내도 거부) |
| MBR-4092 | 409 | 이미 가입된 이메일입니다(+`login_url` 필드) | STEP 1 성공 후 STEP 0 또는 STEP 2에서 `email` 중복 확인(`uq_members_email`) |
| MBR-4094 | 409 | 이미 가입된 휴대폰번호입니다 | STEP 1 성공 후 STEP 0 또는 STEP 2에서 `phone_norm` 중복 확인(`uq_members_phone_norm`) |
| MBR-5000 | 500 | 일시적인 오류입니다 | `DataAccessException`(미분류 PK 충돌 재시도 실패 포함) — 정제된 메시지만 응답, 원본은 서버 로그에만 |

## 참조 테이블

- `MEMBERS`
- `MEMBER_SIGNUP_VERIFICATIONS`
- `ID_SEQUENCES`

## curl 예시

```bash
curl -X POST /api/members/signup \
  -H "Content-Type: application/json" \
  -d '{"target": "user@example.com", "code": "123456", "password": "abcd1234", "name": "홍길동", "marketingOptIn": true}'
```

## 변경 이력
<!-- spec-history: machine-managed -->
| 일자 | SR | 항목 | 변경 요약 | 커밋 |
|---|---|---|---|---|
| 2026-09-15 | SR-295 | #1 | STEP1/STEP0 로직·조건식은 무변경 — INF-MBR-001의 재발송 리셋으로 선행 데이터 상태가 바뀌어 MBR-4093 회복 경로가 생기고 재발송 후 재시도 시 MBR-4092(+login_url)로 귀결되는 부작용 문서화 | shop-api@45e21e8 |
| 2026-09-16 | SR-298 | #1 | incrementAttemptCount에 attempt_count<5 조건 추가해 증가+상한판정을 원자 UPDATE로 통합(병렬 버스트 우회 차단). 0행일 때만 selectAttemptCount로 오류코드(4091/4093)만 재분기, 재증가 없음 | shop-api@0ed4f7d |
