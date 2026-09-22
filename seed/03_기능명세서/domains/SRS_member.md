# SRS — member 도메인

> 기능(FUNC) 1개 = SRS-F 1건. AIDD가 끝낸 기능은 `spec_resync_check`가 STORY(제목·수용 기준)에서 골격을 추가한다 — 업무 흐름·규칙 종합은 보강 대상.

---

## SRS-F-001: 신규 UIS-MBR-001 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-001](../../00_FUNC/FUNC_MAP.md) · SR: SR-231 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-001.md`

### 기능 요약

신규 UIS-MBR-001 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)

### 수용 기준(STORY에서)

- 기능이 설명대로 동작(연결 INF 없음 — 수용기준 수동 작성 권장)
- SR 정본 계약 충족 — `docs/변경관리/SR-231/02_변경명세.md` · inputs/_decisions.md의 D-결정 의 요구·계약 조항을 AC로 구체화해 사람 승인 전 보강할 것(OBS-020 관례)
- 컨트롤러/핸들러 — MemberSignupViewController (GET /member/signup, 정적 뼈대만 렌더)
- 서비스/비즈니스 로직 — 없음(이 FUNC 범위 밖. 회원가입 업무 로직은 이미 구현된
- 데이터 접근 레이어 — 없음(신규 테이블/DAO 없음, 화면 전용 FUNC)
- 단위 테스트 — MemberSignupViewControllerTest 2건 + ApiKeyAuthIntegrationTest 화이트리스트

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-002: 신규 INF-MBR-001 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-002](../../00_FUNC/FUNC_MAP.md) · SR: SR-231 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-002.md`

### 기능 요약

신규 INF-MBR-001 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

### 수용 기준(STORY에서)

- INF-MBR-001: 요청/응답 계약 충족 — round6에서 확정 체크(사람 결정). 근거: QA Gate round5 재게이트(2026-09-12) Layer1이 "통과"로 판정 — 오류계약(`MEMBER_TARGET_INVALID` 400 / `MEMBER_VERIFY_COOLDOWN`·`MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED` 429 / `MBR-5000` 500)이 실 응답과 일치함을 QA가 직접 재실행(`mvnw test` 256건 실패 0)·JDBC 직결 프로브(8스레드×150라운드, 1,200요청, 예외 0)·실 HTTP 8-way 동시요청/순차 20연타로 3,000회 이상 독립 재현해 확인. round6도 이 계약 자체는 바꾸지 않고 판정 SQL만 결정적으로 재작성했으며 `mvnw test` 259건(신규 3건 포함) 실패 0으로 재확인했다. **test-agent 재확인(2026-09-12)**: 매핑된 TC 66건(MemberSignupControllerTest 2 + MemberSignupServiceTest 9 + MemberSignupVerificationDaoTest 4 + MemberSignupRateLimitDaoTest 6 + MemberSignupRateLimitConcurrencyTest 1 + MemberSignupRateLimitTest 1 + MemberSignupSchedulingConfigTest 3 + ApiKeyAuthIntegrationTest 39 + OrderCreateQtyZeroRegressionTest 1) 모두 통과(259/259 전체 스위트 통과).
- SR 정본 계약 충족 — `docs/변경관리/SR-231/02_변경명세.md` · inputs/_decisions.md의 D-결정의 요구·계약 조항을 AC로 구체화했으며(OBS-020 관례) round6에서 확정 체크(사람 결정). 근거: QA Gate round5 재게이트 표 (3) "이행 — 조항별 대조"에서 `02_변경명세.md` §FUNC-member-002 전 조항(purge 배치 전환·`MEMBER_SIGNUP_RATE_LIMITS` 스키마·deprecated 컬럼·오류계약·쿨다운/일일상한·알려진 한계)을 실 DB 스키마·실 코드와 QA가 직접 대조해 stale 문제가 완전히 닫혔음을 확인(round1~round4를 관통하던 정본 stale 문제의 최종 해소). **test-agent 재확인(2026-09-12)**: 명세 조항별 TC 매핑 — 채널 판별(MemberSignupServiceTest#targetDetection_email_sms 2건), 6자리 코드 생성(MemberSignupServiceTest#generatedCode_format_length 1건), 5분 만료(MemberSignupServiceTest#codeExpiry_5minutes 1건), 60초 쿨다운(MemberSignupRateLimitDaoTest 경계값 + MemberSignupRateLimitConcurrencyTest 동시성 검증), 1일 5회 상한(MemberSignupRateLimitTest#dailyLimit_5requests_reject_6th 1건 + MemberSignupRateLimitDaoTest 경계값 6건), 배치 스케줄러(MemberSignupSchedulingConfigTest 3건), 무인증 화이트리스트(ApiKeyAuthIntegrationTest 2건 회귀), 오류 응답 봉투(MemberSignupControllerTest + MemberSignupExceptionHandler 검증 1건). 모든 조항 ≥1개 실행 테스트로 매핑 확인됨.
- 컨트롤러/핸들러 — MemberSignupController (POST /api/members/signup/verification-codes)
- 서비스/비즈니스 로직 — MemberSignupService (채널 판별·코드 생성·5분 만료)
- 데이터 접근 레이어 — MemberSignupVerificationDao + MEMBER_SIGNUP_VERIFICATIONS(신규 테이블)
- 단위 테스트 — Service 6건 + Controller 2건 + Dao(실DB) 3건 + 인증필터 회귀 2건
- STORY 본문 라벨 잔여 불일치 — 이 STORY(002) 제목·AC·Dev Notes가 여전히
- DDL 주석의 DB호환성 오서술 — `member_signup_verifications.sql`의

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-003: 가입 요청 API 구현(INF-MBR-002 역할 · 7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-003](../../00_FUNC/FUNC_MAP.md) · SR: SR-231 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-003.md`

### 기능 요약

가입 요청 API 구현(INF-MBR-002 역할 · 7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

### 수용 기준(STORY에서)

- 가입 성공: 인증 완료(아래 인증 판정) + 중복 없음 + 비밀번호 규칙 충족 시 MEMBERS insert, 201 응답
- 미인증 거부: 코드 검증(만료 전 · attempt_count<5)에 실패하면 **가입 여부와 무관하게 항상** 409 `MBR-4091` "인증이 필요합니다"(존재 오라클 방지 — round4 사람 결정). attempt_count>=5는 409 `MBR-4093` "재발송 필요".
- 중복 이메일/휴대폰 거부: **인증코드 검증을 통과한 뒤에만** 판정 — MEMBERS insert 시 UNIQUE 위반(DuplicateKeyException 캐치, 선조회는 안내용일 뿐 최종 보장 아님) → 409 `MBR-4092`(이메일) / `MBR-4094`(휴대폰) "이미 가입된 이메일" + `login_url` 필드. **검증 전에는** 가입 여부와 무관하게 `MBR-4091`(round4 CONCERNS 수용 결정, 2026-09-12).
- 비밀번호 규칙 위반(8~64자·영문+숫자 포함 아님) → 400 `MBR-4001`
- 가입 완료 시 `MemberSignedUpEvent{memberId, target}` ApplicationEvent 발행(쿠폰 발급 로직 자체는 이 FUNC 범위 아님 — 로그 리스너 1개만)
- 회원 INSERT + verification 행 consumed_at 갱신은 하나의 `@Transactional` 안에서 처리(레이트리밋 테이블은 건드리지 않음)
- 기존 회원 조회 API 응답 스키마·값 불변(회귀) — 기존 컬럼/기존 엔드포인트 무변경
- `/api/members/signup`을 인증 필터 화이트리스트(정확 일치)에 추가, 기존 화이트리스트 판정 불변

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-004: 로그인 화면 · 신규 UIS-MBR-002 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-004](../../00_FUNC/FUNC_MAP.md) · SR: SR-232 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-004.md`

### 기능 요약

로그인 화면 · 신규 UIS-MBR-002 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)

### 수용 기준(STORY에서)

- 기능이 설명대로 동작(연결 INF 없음 — 수용기준 수동 작성 권장)
- SR 정본 계약 충족 — `docs/변경관리/SR-232/02_변경명세.md` · inputs/_decisions.md의 D-결정 의 요구·계약 조항을 AC로 구체화해 사람 승인 전 보강할 것(OBS-020 관례)
- 컨트롤러/핸들러
- 서비스/비즈니스 로직
- 데이터 접근 레이어
- 단위 테스트

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-005: 로그인 API · 신규 INF-MBR-003 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-005](../../00_FUNC/FUNC_MAP.md) · SR: SR-232 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-005.md`

### 기능 요약

로그인 API · 신규 INF-MBR-003 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

### 수용 기준(STORY에서)

- **AC1 정상 로그인**: 유효한 이메일/비밀번호 → 200, `{memberId, memberName, grade, apiKey, refreshToken, refreshTokenExpiresAt}` 응답
- **AC2 자격증명 실패**: 미가입/탈퇴/비번오류 → 401 `MBR-4011`, 사유 비노출 통합 메시지 `"이메일 또는 비밀번호가 올바르지 않습니다 (n/5)"`
- **AC3 잠금 상태**: 5회 실패 후 → 429 `MBR-4291`, `retryAfterSeconds: 600` (10분)
- **AC4 비밀번호 미일치 시 카운트**: 매 실패마다 `MEMBER_LOGIN_ATTEMPTS.fail_count` 증가, 카운트 표시(2/5, 3/5, ...)
- **AC5 5번째 실패 즉시 잠금**: 5번째 실패가 401(5/5)이 아니라 곧바로 429(`MBR-4291`) 응답
- **AC6 로그인 경로 무인증**: `POST /api/members/login` 화이트리스트 (X-Api-Key 헤더 불필요)
- **AC7 리프레시 토큰 발급**: 성공 시 30일 유효 refresh token 응답(원문만, 해시는 DB에)
- **AC8 기존 정적 API 키 회귀**: admin(lab.api-keys 맵)·M-0001 기존 키 200 무변경

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-006: 로그아웃·자동 로그인 토큰 API · 신규 INF-MBR-004 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-006](../../00_FUNC/FUNC_MAP.md) · SR: SR-232 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-006.md`

### 기능 요약

로그아웃·자동 로그인 토큰 API · 신규 INF-MBR-004 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

### 수용 기준(STORY에서)

- INF-MBR-004: 요청/응답 계약 충족(비즈룰 스펙 미상 — 보강 필요)
- SR 정본 계약 충족 — `docs/변경관리/SR-232/02_변경명세.md` · inputs/_decisions.md의 D-결정 의 요구·계약 조항을 AC로 구체화해 사람 승인 전 보강할 것(OBS-020 관례)
- 컨트롤러/핸들러
- 서비스/비즈니스 로직
- 데이터 접근 레이어
- 단위 테스트

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-007: 비밀번호 재설정 화면 · 신규 UIS-MBR-003 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-007](../../00_FUNC/FUNC_MAP.md) · SR: SR-234 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-007.md`

### 기능 요약

비밀번호 재설정 화면 · 신규 UIS-MBR-003 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-uis가 역생성)

### 수용 기준(STORY에서)

- AC1 3단계 화면(요청→코드 확인→새 비밀번호)과 현재 단계 표시
- AC2 휴대폰은 하이픈·공백을 제거해 숫자만 전송, 이메일은 그대로
- AC3 요청 202면 코드 단계로 — '존재하지 않는 계정' 상태는 없다
- AC4 남은 유효시간 카운트다운은 응답 `expiresInSeconds` 기준, 재전송 버튼은 60초 쿨다운 비활성+남은 초 표시
- AC5 확정 응답 매핑: 204→완료 화면('모든 기기에서 로그아웃됨') · 410 MBR-4101→'만료됨'+다시 요청 버튼(1단계로) · 409 MBR-4102→코드 오류 인라인(입력 유지) · 409 MBR-4103→시도 초과+다시 요청 · 400 MBR-4001→비밀번호 규칙 오류 인라인 · 400 MBR-4100→형식 오류 인라인
- AC6 로그인 화면에 '비밀번호를 잊으셨나요' 링크(기존 로그인 동작 무변경)
- AC7 부품·상태별 스토리(요청/코드/새 비밀번호/만료/시도초과/완료 — 규칙 `story-per-component`)
- AC8 테스트: jest+testing-library로 단계 전이·쿨다운 타이머·오류 매핑, 반드시 `npm test`(타입검사+jest)에 걸리게(RUN8 004 r2 재발 방지)

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-008: 비밀번호 재설정 코드 요청 API · 신규 INF-MBR-006 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-008](../../00_FUNC/FUNC_MAP.md) · SR: SR-234 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-008.md`

### 기능 요약

비밀번호 재설정 코드 요청 API · 신규 INF-MBR-006 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

### 수용 기준(STORY에서)

- INF-MBR-006: 요청/응답 계약 충족 — TC-FUNC-member-008-01~08 (컨트롤러 200, 요청/응답 계약, 형식·DB 오류, 무인증, 정규화 echo)
- SR 정본 계약 충족 — TC-FUNC-member-008-09~24 (서비스 정규화·형식·채널·쿨다운·발송로그, DAO 원자성·동시성)
- 컨트롤러/핸들러
- 서비스/비즈니스 로직
- 데이터 접근 레이어
- 단위 테스트

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-009: 비밀번호 재설정 확정 API · 신규 INF-MBR-007 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-009](../../00_FUNC/FUNC_MAP.md) · SR: SR-234 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-009.md`

### 기능 요약

비밀번호 재설정 확정 API · 신규 INF-MBR-007 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

### 수용 기준(STORY에서)

- INF-MBR-007: 요청/응답 계약 충족
- SR 정본 계약 충족 — `docs/변경관리/SR-234/02_변경명세.md` 모든 요구사항
- 컨트롤러/핸들러
- 서비스/비즈니스 로직
- 데이터 접근 레이어
- 단위 테스트

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)

## SRS-F-010: 배송지 CRUD API · 신규 INF-MBR-008 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)  〈사용자 업무 명세〉

> FUNC-ID: [FUNC-member-011](../../00_FUNC/FUNC_MAP.md) · SR: SR-235 · 출처: STORY `docs/00_FUNC/stories/STORY-FUNC-member-011.md`

### 기능 요약

배송지 CRUD API · 신규 INF-MBR-008 구현(7-N 예약분 · 스펙 본문은 구현 후 /sl-recon-inf가 역생성)

### 수용 기준(STORY에서)

- INF-MBR-008: 요청/응답 계약 충족(5개 엔드포인트 · 상태코드 201/200/204/200/200 · 오류코드
- SR 정본 계약 충족 — `docs/변경관리/SR-235/02_변경명세.md` · `_decisions.md` D-결정의 요구·계약
- 컨트롤러/핸들러
- 서비스/비즈니스 로직
- 데이터 접근 레이어
- 단위 테스트

### 업무 흐름 · 비즈니스 규칙

[TBD] — 보강 대상(srs-agent)
