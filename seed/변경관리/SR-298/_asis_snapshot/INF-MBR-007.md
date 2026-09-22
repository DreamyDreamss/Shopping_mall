---
inf-id: INF-MBR-007
name: 비밀번호 재설정 확정
layer: api
method: POST
path: /api/members/password-resets/confirmations
domain: member
domain-code: MBR
srs-f: [TBD]
screens: []
tables:
  - MEMBER_PASSWORD_RESETS
anchors:
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetConfirmationController.java:13-29
  - modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetConfirmationController.java:40-45
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationService.java:85-137
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationService.java:139-162
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationWriter.java:44-65
  - modules/shop-api/src/main/java/com/sm/lab/shop/service/MemberPasswordResetConfirmationApiException.java:16-34
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/MemberPasswordResetConfirmationExceptionHandler.java:28-49
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberPasswordResetDao.java:41-63
  - modules/shop-api/src/main/java/com/sm/lab/shop/dao/MemberDao.java:93-118
  - modules/shop-api/src/main/resources/mapper/memberPasswordReset.xml:35-57
  - modules/shop-api/src/main/resources/mapper/member.xml:72-90
  - modules/shop-api/src/main/resources/db/V6__member_password_reset_confirmation.sql:1-10
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:157-161
  - modules/shop-api/src/main/java/com/sm/lab/shop/web/ApiKeyAuthFilter.java:209-223
---

> [반영: FUNC-member-009] 2026-09-13

# INF-MBR-007: POST /api/members/password-resets/confirmations — 비밀번호 재설정 확정

> **개요:** 이메일/휴대폰으로 받은 6자리 코드와 새 비밀번호를 제출하면 코드를 확인하고, 맞으면 비밀번호를 반영한 뒤 그 회원의 모든 기기를 로그아웃시킨다(리프레시 토큰 + API 키 폐기). 코드 요청은 별도 API(FUNC-member-008, INF-MBR-006)가 담당한다.

> **근거 소스:** `modules/shop-api/src/main/java/com/sm/lab/shop/controller/MemberPasswordResetConfirmationController.java:41-45`

## 요청

- Method: POST
- Path: /api/members/password-resets/confirmations
- Content-Type: application/json
- 인증: 없음(무인증) — `ApiKeyAuthFilter`의 `isOpenRoute` 화이트리스트(`MEMBER_PASSWORD_RESET_CONFIRM_PATH`, 정확 일치)에 등록. 로그인 전(비밀번호를 잊은) 사용자가 호출하므로 아직 API 키가 없기 때문(INF-MBR-006과 동일한 이유). 자격 판정은 필터가 아니라 이 서비스(원자 확정 UPDATE + del_yn 필터 회원 조회)가 한다.

| 파라미터 | 위치 | 타입 | 필수 | 설명 |
|---------|------|------|------|------|
| target | Body | string | Y | 이메일 또는 휴대폰번호. INF-MBR-006이 저장한 것과 같은 정규화 규칙(이메일 trim+소문자, 휴대폰 숫자만)으로 재검증·정규화한다. 형식 오류 시 400 |
| code | Body | string | Y | 6자리 인증코드. SHA-256 해시로 저장값과 대조(원문은 저장되지 않음) |
| newPassword | Body | string | Y | 새 비밀번호. 8~64자, 영문+숫자 포함 규칙(가입 API와 동일 검증 재사용). 형식 오류 시 400 |

## 응답 (204 No Content)

본문 없음. 아래 어느 경로든(코드 확정 성공 후 회원을 찾은 경우 / 회원을 못 찾은 경우) 동일하게 204를 반환한다 — 회원 존재 여부를 응답으로 절대 구분하지 않는다(`MemberSessionController.logout()`과 동일 형태).

## 비즈니스 규칙

> 이 섹션은 abstract다. 정본은 앵커(소스)이며, 상세 분기는 file:line을 직접 읽는다.

- **존재 오라클 방지(이 API의 핵심 위험)**: 코드 확정(`confirmIfCodeMatches`)이 회원 조회보다 항상 먼저 실행되고, "행 없음"(요청한 적 없는 target)도 단순 코드 불일치와 완전히 동일한 409 `MBR-4102`로 수렴한다(`handleConfirmFailure`가 두 경우 모두 같은 `mismatchException()` 팩토리 호출). 회원 조회(코드 확정 성공 후 1회) 결과가 있든 없든 응답은 204로 동일 — 이 API의 어떤 응답도 회원 존재 여부를 드러내지 않는다.
- **del_yn='N' 이중 필터**: 회원 매칱 조회(`selectMemberIdByResetTarget`)와 비밀번호 반영 UPDATE(`updatePasswordHash`) 양쪽 모두에 `del_yn = 'N'`을 건다 — 탈퇴 회원의 비밀번호가 되살아나거나 탈퇴 회원 명의로 세션이 재발급되는 경로를 원천 차단한다.
- **코드 확정이 회원 조회보다 항상 먼저**: `confirmPasswordReset`은 target/newPassword 형식 검증 → 코드 확정(`confirmIfCodeMatches`) → (성공 시에만) BCrypt → 회원 조회 순서로 진행한다. 회원 조회 이전 어떤 단계도 `MEMBERS` 테이블을 참조하지 않는다.
- **BCrypt는 확정 성공 시에만, 발견 여부와 무관하게 항상 실행**: 코드 확정이 성공한 직후 새 비밀번호를 BCrypt로 해싱한다 — 이후 회원을 찾든 못 찾든 이 해싱 자체는 항상 실행됐던 상태이므로, 회원 발견 여부가 처리 시간(타이밍)으로 새지 않는다.
- **회원 미발견 시 조용히 204**: 코드 확정에 성공했지만 매칭되는 활성 회원이 없으면(가짜 target 등) 로그 없이 그대로 반환한다 — 완료 로그는 실제로 비밀번호가 반영된 경우에만 남긴다.
- **트랜잭션 경계(Writer 별도 빈)**: 이 서비스는 `@Transactional`을 쓰지 않는다 — 코드 확정/시도횟수 증가는 각각 autocommit 단일 UPDATE 문(실패 시도 카운터가 트랜잭션 롤백에 함께 사라지는 것을 방지). 비밀번호 반영+전 기기 로그아웃만 별도 스프링 빈(`MemberPasswordResetConfirmationWriter`)의 `@Transactional` 메서드로 묶는다 — 서비스 클래스 안에서 자기 메서드를 직접 호출(self-invocation)하면 Spring 프록시가 트랜잭션 어드바이스를 적용하지 못하는 함정을 피하기 위함이다.
- **0행이면 세션 폐기 스킵**: `updatePasswordHash`가 0행(그 사이 회원 탈퇴 등으로 반영 실효 없음)이면 리프레시 토큰·API 키 폐기 두 문장을 건너뛰고 즉시 반환한다.
- **전 기기 로그아웃(사이드이펙트)**: 비밀번호 반영에 성공하면 그 회원의 리프레시 토큰 전체와 API 키를 폐기한다(순서: 리프레시 먼저, apiKey 나중 — INF-MBR-005 로그아웃과 동일 순서). 이 트랜잭션이 중간에 실패하면(예: 세션 폐기 UPDATE 예외) 비밀번호 반영까지 함께 롤백된다 — "비밀번호는 바뀌었는데 세션은 살아있는" 위험한 중간 상태를 만들지 않는다.
- **1회용(consumed_at)**: `confirmIfCodeMatches`는 `consumed_at IS NULL`인 행에만 매치하므로, 같은 코드로 두 번째 확정을 시도하면 이미 소비된 것으로 판정되어 410 `MBR-4101`이 된다.

## 트랜잭션 순서

1. target 형식 검증·정규화, newPassword 형식 검증(재사용 — INF-MBR-006/가입 API와 동일 규칙). 실패 시 각각 400, 이후 단계 없음(`attempt_count` 미소모).
2. 코드 SHA-256 해싱.
3. `MEMBER_PASSWORD_RESETS` 원자 확정 UPDATE(`confirmIfCodeMatches`, 비트랜잭션 단일 문). 0행이면 재조회(`selectByTarget`)로 사유 판별(행 없음/만료·소비/시도초과/단순오답) 후 예외 — 아래 단계 진행 안 함.
4. (확정 성공 시에만) BCrypt로 새 비밀번호 해싱.
5. `MEMBERS.selectMemberIdByResetTarget` 조회(이번이 처음 — 이전 어떤 단계도 `MEMBERS`를 조회하지 않음). 못 찾으면 조용히 204(로그 없음).
6. (회원 발견 시) `MemberPasswordResetConfirmationWriter.applyNewPassword`(별도 트랜잭션 빈) 호출:
   1. `MEMBERS.updatePasswordHash`(0행이면 이후 두 문장 스킵).
   2. `MEMBER_REFRESH_TOKENS.revokeAllForMember`(전 기기 로그아웃).
   3. `MEMBER_API_KEYS.revokeByMemberId`.
7. Writer 트랜잭션 커밋 후에만 완료 로그 1줄(마스킹된 target). 컨트롤러는 204 반환.

## 사이드이펙트

- 비밀번호 반영과 함께 그 회원의 전 기기 로그아웃 처리: `MEMBER_REFRESH_TOKENS` 전체 폐기 + `MEMBER_API_KEYS` 폐기(같은 Writer 트랜잭션, 순서 고정 — 리프레시 먼저, apiKey 나중).
- `MEMBERS.updated_at`(V6 마이그레이션 신규 컬럼) 갱신 — 이 FUNC이 최초로 세팅.

## 오류 응답

| 코드 | HTTP | 사유 | 발생 조건 |
|------|------|------|---------|
| MBR-4100 | 400 | 이메일 또는 휴대폰번호 형식이 올바르지 않습니다 | target 형식 검증 실패(재사용 — INF-MBR-006과 동일 코드) |
| MBR-4001 | 400 | 비밀번호 형식이 올바르지 않습니다 | newPassword 형식 검증 실패(재사용 — 가입 API와 동일 코드) |
| MBR-4101 | 410 | 인증코드가 만료되었습니다. 다시 요청해 주세요 | 코드가 이미 소비됐거나(`consumed_at` 있음) 만료됨(`expires_at` 경과) — 신규 |
| MBR-4102 | 409 | 코드가 올바르지 않습니다 | 코드 불일치, 또는 target에 해당하는 행 자체가 없음(둘을 동일 코드로 통일 — 존재 오라클 방지, 신규) |
| MBR-4103 | 409 | 코드 확인 시도 횟수를 초과했습니다. 다시 요청해 주세요 | `attempt_count`가 상한(5회) 이상 — 신규 |
| MBR-5000 | 500 | 일시적인 오류입니다 | `DataAccessException` — 정제된 메시지만 응답, 원본은 서버 로그에만(`MemberPasswordResetConfirmationExceptionHandler`) |

## 참조 테이블

- `MEMBER_PASSWORD_RESETS`

## curl 예시

```bash
curl -X POST /api/members/password-resets/confirmations \
  -H "Content-Type: application/json" \
  -d '{"target": "user@example.com", "code": "123456", "newPassword": "newPass123"}'
```
