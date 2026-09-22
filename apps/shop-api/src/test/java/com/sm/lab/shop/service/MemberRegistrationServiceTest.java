// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberSignupCompletionDao;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SR-231(FUNC-member-003) — 가입 요청 API 서비스 단위 테스트. round3(2026-09-12, round2 QA
 * FAIL 재작업 지시 — 마지막 라운드) 이후 {@code signUp}은 3단계(사전 판정/코드 검증/가입)로
 * 나뉘고, "가입" 단계는 별도 빈 {@link MemberSignupCompletionWriter}로 위임된다(자기호출로
 * 인한 트랜잭션 어드바이스 우회를 원천 차단하기 위함 — 클래스 javadoc 참고). 이 테스트는
 * Mockito로 그 위임과 분기 로직(사전 판정 우선순위, 시도 상한 도달 판정, 중복 제약 분류,
 * 미분류 충돌 재시도)만 격리 검증한다 — 실제 트랜잭션 경계·비트랜잭션 커밋이 정말 지켜지는지는
 * Mockito로 증명할 수 없으므로 {@link com.sm.lab.shop.MemberRegistrationCompletionFlowTest}
 * (실 서버+실 DB, HTTP 레벨)가 별도로 맡는다(round2 QA FAIL 3번 "테스트 맹점" 재발 방지).
 *
 * <p><b>round4(2026-09-12, round3 QA CONCERNS 권고1 "존재 오라클" — 사람 결정)</b> — STEP 0
 * (사전 판정)과 STEP 1(코드 검증)의 호출 순서가 뒤집혔다 — 이제 코드 검증이 먼저이고, 사전
 * 판정은 코드 검증에 **성공한 뒤에만** 실행된다. 그래서 사전 판정 관련 테스트는 이제
 * {@code markVerifiedIfCodeMatches}가 성공(1)했다는 전제를 함께 스텁해야 하고, 반대로 코드
 * 검증 실패 테스트는 {@code memberDao}가 전혀 호출되지 않았음을 검증한다(존재 여부를 조회조차
 * 하지 않는다는 것이 이번 라운드의 핵심 성질).
 */
@ExtendWith(MockitoExtension.class)
class MemberRegistrationServiceTest {

    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2026, 9, 12, 10, 0, 0);
    private static final Clock FIXED_CLOCK =
            Clock.fixed(FIXED_NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    private static final String VALID_PASSWORD = "abcd1234"; // 8자, 영문+숫자

    @Mock
    private MemberDao memberDao;
    @Mock
    private MemberSignupCompletionDao completionDao;
    @Mock
    private MemberSignupCompletionWriter completionWriter;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    private MemberRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new MemberRegistrationService(memberDao, completionDao, completionWriter, eventPublisher, FIXED_CLOCK);
    }

    /** STEP 0 사전 판정을 통과시키는 기본 스텁 — 대부분의 테스트가 중복 없음을 전제로 한다. */
    private void stubNoExistingMember() {
        when(memberDao.selectMemberIdByEmailOrPhoneNorm(any(), any())).thenReturn(null);
    }

    // linked_tc: TC-FUNC-member-003-01 — 가입 성공: 인증 완료 + 중복 없음 + 비밀번호 규칙 충족.
    @Test
    void signUp_verifiedAndPasswordValid_insertsMemberAndPublishesEvent() {
        stubNoExistingMember();
        when(completionDao.markVerifiedIfCodeMatches(eq("EMAIL"), eq("user@example.com"), eq("123456"),
                eq(MemberRegistrationService.MAX_VERIFY_ATTEMPTS))).thenReturn(1);
        when(completionWriter.completeSignup(eq("EMAIL"), eq("user@example.com"), eq("홍길동"), eq("BRONZE"),
                eq("user@example.com"), isNull(), isNull(), any(), eq(true), eq(FIXED_NOW)))
                .thenReturn("M-1001");

        MemberRegistrationService.SignupResult result =
                service.signUp("user@example.com", "123456", VALID_PASSWORD, "홍길동", true);

        assertThat(result.channel()).isEqualTo("EMAIL");
        assertThat(result.target()).isEqualTo("user@example.com");
        assertThat(result.memberId()).isEqualTo("M-1001");

        verify(completionDao, never()).incrementAttemptCount(any(), any(), anyInt());
        verify(completionWriter, times(1)).completeSignup(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any());

        ArgumentCaptor<MemberSignedUpEvent> eventCaptor = ArgumentCaptor.forClass(MemberSignedUpEvent.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getTarget()).isEqualTo("user@example.com");
    }

    @Test
    void signUp_phoneTarget_storesNormalizedPhoneInBothColumns() {
        stubNoExistingMember();
        when(completionDao.markVerifiedIfCodeMatches(eq("SMS"), eq("01012345678"), eq("123456"), anyInt()))
                .thenReturn(1);
        when(completionWriter.completeSignup(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(),
                any())).thenReturn("M-1002");

        service.signUp("01012345678", "123456", VALID_PASSWORD, "홍길동", false);

        verify(completionWriter).completeSignup(eq("SMS"), eq("01012345678"), any(), any(), isNull(),
                eq("01012345678"), eq("01012345678"), any(), eq(false), any());
    }

    // linked_tc: TC-FUNC-member-003-02 — 미인증 거부(SR-298 갱신): markVerifiedIfCodeMatches가
    // 0행이면 원자 incrementAttemptCount(channel, target, maxAttempts)를 호출한다 — 그 UPDATE
    // 자체가 attempt_count < maxAttempts를 조건으로 가져 증가와 상한 판정을 한 문장으로 끝내므로,
    // 영향행수 1은 곧바로 409 MBR-4091을 뜻한다(별도 읽기로 상한 여부를 먼저 판정하지 않는다).
    // round4 — STEP 1이 STEP 0보다 먼저 평가되므로 memberDao(사전 판정)는 전혀 호출되지 않아야 한다.
    @Test
    void signUp_codeNotMatched_incrementsAttemptAndThrows409VerifyRequired() {
        when(completionDao.markVerifiedIfCodeMatches(any(), any(), any(), anyInt())).thenReturn(0);
        when(completionDao.incrementAttemptCount(any(), any(), anyInt())).thenReturn(1);

        assertThatThrownBy(() -> service.signUp("user@example.com", "999999", VALID_PASSWORD, "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .extracting(ex -> ((MemberRegistrationApiException) ex).getCode())
                .isEqualTo("MBR-4091");

        verify(completionDao).incrementAttemptCount(eq("EMAIL"), eq("user@example.com"),
                eq(MemberRegistrationService.MAX_VERIFY_ATTEMPTS));
        verify(completionDao, never()).selectAttemptCount(any(), any());
        verify(memberDao, never()).selectMemberIdByEmailOrPhoneNorm(any(), any());
        verify(completionWriter, never()).completeSignup(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    // round4(사람 결정, SR-231 round3 QA CONCERNS 권고1 "존재 오라클" 해소) — 핵심 신규 성질:
    // 이 target이 실제로 이미 가입된 회원이라도, 코드 검증에 실패하면 그 사실이 응답에 전혀
    // 드러나지 않는다(항상 409 MBR-4091, memberDao 미호출) — round3까지는 사전 판정이 먼저
    // 돌아 같은 상황에서 409 MBR-4092가 나갔다(그것이 "존재 오라클"이었다).
    // linked_tc: TC-FUNC-member-003-02 갱신분 — 미검증 상태에서 이미 가입된 target으로 요청해도
    // 4092가 아니라 4091이 나오는 것을 단언(사람 코멘트 테스트 지시).
    @Test
    void signUp_codeNotMatched_evenIfTargetAlreadyRegistered_stillThrows409VerifyRequiredWithoutRevealingExistence() {
        when(completionDao.markVerifiedIfCodeMatches(any(), any(), any(), anyInt())).thenReturn(0);
        when(completionDao.incrementAttemptCount(any(), any(), anyInt())).thenReturn(1);

        assertThatThrownBy(() -> service.signUp("dup@example.com", "999999", VALID_PASSWORD, "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .extracting(ex -> ((MemberRegistrationApiException) ex).getCode())
                .isEqualTo("MBR-4091");

        verify(memberDao, never()).selectMemberIdByEmailOrPhoneNorm(any(), any());
        verify(completionWriter, never()).completeSignup(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any());
    }

    // round3(사람 결정) — 이미 시도 상한(5회)에 도달한 상태에서 들어온 요청은(정답 코드라도)
    // 시도 횟수를 더 늘리지 않고 곧바로 409 MBR-4093("재발송 필요")으로 거부해야 한다.
    // round4 — 이 경로 역시 STEP 0보다 먼저 평가되므로 memberDao는 호출되지 않는다.
    // SR-298(의미 전환) — round4까지는 "상한 도달 시 incrementAttemptCount가 아예 호출되지
    // 않음"을 검증했다(호출부가 먼저 selectAttemptCount로 상한 여부를 읽어 분기했으므로). 이제는
    // incrementAttemptCount(channel, target, maxAttempts) 자체가 원자 조건부 UPDATE라서 상한
    // 도달 여부와 무관하게 항상 호출되고, 그 영향행수 0이 "상한 도달"을 뜻하는 신호로 바뀐다 —
    // 그래서 이 테스트는 이제 그 호출이 일어났음(never()가 아님)을 검증하고, 0 이후에만
    // selectAttemptCount로 원인을 읽어 4093으로 분기하는지를 확인한다.
    @Test
    void signUp_codeNotMatchedAndAttemptCountAtCap_throws409VerifyLockedWithoutIncrementing() {
        when(completionDao.markVerifiedIfCodeMatches(any(), any(), any(), anyInt())).thenReturn(0);
        when(completionDao.incrementAttemptCount(any(), any(), anyInt())).thenReturn(0);
        when(completionDao.selectAttemptCount(any(), any())).thenReturn(MemberRegistrationService.MAX_VERIFY_ATTEMPTS);

        assertThatThrownBy(() -> service.signUp("user@example.com", "123456", VALID_PASSWORD, "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .extracting(ex -> ((MemberRegistrationApiException) ex).getCode())
                .isEqualTo("MBR-4093");

        verify(completionDao).incrementAttemptCount(eq("EMAIL"), eq("user@example.com"),
                eq(MemberRegistrationService.MAX_VERIFY_ATTEMPTS));
        verify(memberDao, never()).selectMemberIdByEmailOrPhoneNorm(any(), any());
        verify(completionWriter, never()).completeSignup(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any());
    }

    // round4(사람 결정) — STEP 0 사전 판정은 이제 STEP 1(코드 검증)이 **성공한 뒤에만** 실행된다
    // (round3까지는 순서가 반대였다 — 그것이 "존재 오라클"이었다). 이메일 중복은 여전히 채널에
    // 맞는 코드(4092 + login_url)로 분기한다.
    @Test
    void signUp_emailAlreadyRegistered_throws409EmailDuplicateAfterVerifyingCode() {
        when(completionDao.markVerifiedIfCodeMatches(any(), any(), any(), anyInt())).thenReturn(1);
        when(memberDao.selectMemberIdByEmailOrPhoneNorm(eq("dup@example.com"), isNull())).thenReturn("M-0001");

        assertThatThrownBy(() -> service.signUp("dup@example.com", "123456", VALID_PASSWORD, "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .satisfies(ex -> {
                    MemberRegistrationApiException mre = (MemberRegistrationApiException) ex;
                    assertThat(mre.getCode()).isEqualTo("MBR-4092");
                    assertThat(mre.getLoginUrl()).isNotBlank();
                });

        verify(completionDao).markVerifiedIfCodeMatches(any(), any(), any(), anyInt());
        verify(completionWriter, never()).completeSignup(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any());
    }

    // round4 — 사전 판정의 휴대폰 중복도 코드 검증 성공 뒤에만 평가된다. 채널에 맞는 코드(4094,
    // login_url 없음)로 분기하는 성질 자체는 round3와 동일.
    @Test
    void signUp_phoneAlreadyRegistered_throws409PhoneDuplicateAfterVerifyingCode() {
        when(completionDao.markVerifiedIfCodeMatches(any(), any(), any(), anyInt())).thenReturn(1);
        when(memberDao.selectMemberIdByEmailOrPhoneNorm(isNull(), eq("01011112222"))).thenReturn("M-0001");

        assertThatThrownBy(() -> service.signUp("01011112222", "123456", VALID_PASSWORD, "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .satisfies(ex -> {
                    MemberRegistrationApiException mre = (MemberRegistrationApiException) ex;
                    assertThat(mre.getCode()).isEqualTo("MBR-4094");
                    assertThat(mre.getLoginUrl()).isNull();
                });

        verify(completionDao).markVerifiedIfCodeMatches(any(), any(), any(), anyInt());
        verify(completionWriter, never()).completeSignup(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any());
    }

    // linked_tc: TC-FUNC-member-003-03 — STEP 2 최종 UNIQUE catch(이메일) — 사전 판정을
    // 통과했더라도(레이스 등) INSERT 시점 위반은 여전히 409 MBR-4092 + login_url로 변환돼야 한다.
    @Test
    void signUp_duplicateEmailConstraintOnInsert_throws409EmailDuplicateWithLoginUrl() {
        stubNoExistingMember();
        when(completionDao.markVerifiedIfCodeMatches(any(), any(), any(), anyInt())).thenReturn(1);
        when(completionWriter.completeSignup(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(),
                any())).thenThrow(new DuplicateKeyException("Duplicate entry 'dup@example.com' for key 'uq_members_email'"));

        assertThatThrownBy(() -> service.signUp("dup@example.com", "123456", VALID_PASSWORD, "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .satisfies(ex -> {
                    MemberRegistrationApiException mre = (MemberRegistrationApiException) ex;
                    assertThat(mre.getCode()).isEqualTo("MBR-4092");
                    assertThat(mre.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(mre.getLoginUrl()).isNotBlank();
                });

        verify(eventPublisher, never()).publishEvent(any());
    }

    // round2 QA FAIL 필수2 재작업 — STEP 2 최종 UNIQUE catch(휴대폰)도 유지된다.
    @Test
    void signUp_duplicatePhoneNormConstraintOnInsert_throws409PhoneDuplicateWithoutLoginUrl() {
        stubNoExistingMember();
        when(completionDao.markVerifiedIfCodeMatches(any(), any(), any(), anyInt())).thenReturn(1);
        when(completionWriter.completeSignup(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(),
                any())).thenThrow(new DuplicateKeyException("Duplicate entry '01011112222' for key 'uq_members_phone_norm'"));

        assertThatThrownBy(() -> service.signUp("01011112222", "123456", VALID_PASSWORD, "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .satisfies(ex -> {
                    MemberRegistrationApiException mre = (MemberRegistrationApiException) ex;
                    assertThat(mre.getCode()).isEqualTo("MBR-4094");
                    assertThat(mre.getHttpStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(mre.getLoginUrl()).isNull();
                });
    }

    // round2 QA FAIL 필수3 재작업 — 미분류(PK) 충돌은 완전히 새 트랜잭션(별도 completeSignup
    // 호출)으로 1회만 재시도한다. 성공하면 정상 201로 이어져야 한다.
    @Test
    void signUp_unclassifiedDuplicateOnFirstAttempt_retriesOnceInNewTransactionAndSucceeds() {
        stubNoExistingMember();
        when(completionDao.markVerifiedIfCodeMatches(any(), any(), any(), anyInt())).thenReturn(1);
        when(completionWriter.completeSignup(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(),
                any()))
                .thenThrow(new DuplicateKeyException("Duplicate entry 'M-2000' for key 'PRIMARY'"))
                .thenReturn("M-2001");

        MemberRegistrationService.SignupResult result =
                service.signUp("user@example.com", "123456", VALID_PASSWORD, "홍길동", false);

        assertThat(result.memberId()).isEqualTo("M-2001");
        verify(completionWriter, times(2)).completeSignup(any(), any(), any(), any(), any(), any(), any(), any(),
                anyBoolean(), any());
    }

    // round2 QA FAIL 필수3 재작업 — 재시도도 실패하면 업무 409로 오분류하지 않고 그대로 다시
    // 던진다(일반 DataAccessException 500 경로로 떨어져야 함 — 핸들러 테스트는 컨트롤러
    // 슬라이스에서 확인).
    @Test
    void signUp_unclassifiedDuplicateOnRetryToo_propagatesDuplicateKeyException() {
        stubNoExistingMember();
        when(completionDao.markVerifiedIfCodeMatches(any(), any(), any(), anyInt())).thenReturn(1);
        when(completionWriter.completeSignup(any(), any(), any(), any(), any(), any(), any(), any(), anyBoolean(),
                any())).thenThrow(new DuplicateKeyException("Duplicate entry for key 'PRIMARY'"));

        assertThatThrownBy(() -> service.signUp("user@example.com", "123456", VALID_PASSWORD, "홍길동", false))
                .isInstanceOf(DuplicateKeyException.class);

        verify(eventPublisher, never()).publishEvent(any());
    }

    // linked_tc: TC-FUNC-member-003-04 — 비밀번호 규칙 위반(8~64자·영문+숫자 포함 아님) → 400 MBR-4001.
    // 400 검증은 STEP 0/1보다 먼저 실행돼 DB 호출 자체가 없어야 한다.
    @Test
    void signUp_passwordTooShort_throws400PasswordInvalidWithoutTouchingDb() {
        assertThatThrownBy(() -> service.signUp("user@example.com", "123456", "ab12", "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .satisfies(ex -> {
                    MemberRegistrationApiException mre = (MemberRegistrationApiException) ex;
                    assertThat(mre.getCode()).isEqualTo("MBR-4001");
                    assertThat(mre.getHttpStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                });

        verify(memberDao, never()).selectMemberIdByEmailOrPhoneNorm(any(), any());
    }

    @Test
    void signUp_passwordWithoutDigit_throws400PasswordInvalid() {
        assertThatThrownBy(() -> service.signUp("user@example.com", "123456", "abcdefgh", "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .extracting(ex -> ((MemberRegistrationApiException) ex).getCode())
                .isEqualTo("MBR-4001");
    }

    @Test
    void signUp_passwordWithoutLetter_throws400PasswordInvalid() {
        assertThatThrownBy(() -> service.signUp("user@example.com", "123456", "12345678", "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .extracting(ex -> ((MemberRegistrationApiException) ex).getCode())
                .isEqualTo("MBR-4001");
    }

    @Test
    void signUp_passwordTooLong_throws400PasswordInvalid() {
        String tooLong = "a1".repeat(33); // 66자

        assertThatThrownBy(() -> service.signUp("user@example.com", "123456", tooLong, "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .extracting(ex -> ((MemberRegistrationApiException) ex).getCode())
                .isEqualTo("MBR-4001");
    }

    // round2 권고 — name 미검증으로 500이 나던 것을 400으로 옮김(사람 코멘트 (4)).
    @Test
    void signUp_nameBlank_throws400() {
        assertThatThrownBy(() -> service.signUp("user@example.com", "123456", VALID_PASSWORD, "  ", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .extracting(ex -> ((MemberRegistrationApiException) ex).getCode())
                .isEqualTo("MBR-4001");

        verify(completionDao, never()).markVerifiedIfCodeMatches(any(), any(), any(), anyInt());
    }

    @Test
    void signUp_nameTooLong_throws400() {
        String tooLong = "김".repeat(51);

        assertThatThrownBy(() -> service.signUp("user@example.com", "123456", VALID_PASSWORD, tooLong, false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .extracting(ex -> ((MemberRegistrationApiException) ex).getCode())
                .isEqualTo("MBR-4001");
    }

    // 형식이 성립하지 않는 target은 애초에 어떤 인증 기록도 있을 수 없으므로 "인증 필요"로 수렴한다
    // (STORY가 별도 형식오류 코드를 정의하지 않음 — 클래스 javadoc 참고).
    @Test
    void signUp_invalidTargetFormat_throws409VerifyRequired() {
        assertThatThrownBy(() -> service.signUp("not-a-target", "123456", VALID_PASSWORD, "홍길동", false))
                .isInstanceOf(MemberRegistrationApiException.class)
                .extracting(ex -> ((MemberRegistrationApiException) ex).getCode())
                .isEqualTo("MBR-4091");

        verify(memberDao, never()).selectMemberIdByEmailOrPhoneNorm(any(), any());
        verify(completionDao, never()).markVerifiedIfCodeMatches(any(), any(), any(), anyInt());
    }
}
