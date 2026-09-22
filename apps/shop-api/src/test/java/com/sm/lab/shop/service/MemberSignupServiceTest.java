// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberSignupRateLimitDao;
import com.sm.lab.shop.dao.MemberSignupVerificationDao;
import com.sm.lab.shop.domain.MemberSignupRateLimit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * round4(SR-231, 사람 결정 — "설계를 단순화해서 데드락 원인을 없앤다") — 레이트리밋 카운터가
 * 전용 테이블({@link MemberSignupRateLimitDao})로 옮겨졌다. {@link MemberSignupVerificationDao}
 * 는 이제 코드 기록({@code writeCode})만 책임진다.
 *
 * <p>round5(사람 결정, round4 QA FAIL 재작업 지시(1)) — 판정 신호가 {@code touchRateLimit}의
 * affected-rows 반환값(datasource 전역 {@code useAffectedRows=true}에 의존, FUNC-order-002
 * qty=0 주문 라인 회귀를 낸 원인)에서 <b>요청 토큰</b>으로 바뀌었다: 서비스가 만든 UUID
 * 토큰을 {@code touchRateLimit}에 넘기고, 뒤이은 {@code selectRateLimit} 재조회 결과의
 * {@code lastToken}이 그 토큰과 같으면 허용이다. house rule 3(select→분기→update 금지) 준수는
 * {@link com.sm.lab.shop.dao.MemberSignupRateLimitDaoTest}에서 실 DB로 별도 검증.
 */
@ExtendWith(MockitoExtension.class)
class MemberSignupServiceTest {

    @Mock
    private MemberSignupVerificationDao verificationDao;
    @Mock
    private MemberSignupRateLimitDao rateLimitDao;

    private MemberSignupService service() {
        return new MemberSignupService(verificationDao, rateLimitDao);
    }

    // round5 — 서비스가 매 호출마다 새 UUID 토큰을 만들어 touchRateLimit에 넘기므로, 미리 정해진
    // 값으로 stub할 수 없다. touchRateLimit 호출 시 넘어온 토큰을 captor로 붙잡아, 뒤이은
    // selectRateLimit 응답에 "그 토큰이 그대로 저장된 행"을 동적으로 만들어 돌려준다(허용 시나리오).
    private void stubAdmitted(int dailyCountAfter) {
        ArgumentCaptor<String> tokenCaptor = ArgumentCaptor.forClass(String.class);
        doNothing().when(rateLimitDao).touchRateLimit(any(), any(), any(), anyInt(), anyInt(), tokenCaptor.capture());
        when(rateLimitDao.selectRateLimit(any(), any())).thenAnswer(invocation -> {
            MemberSignupRateLimit row = new MemberSignupRateLimit();
            row.setDailyCount(dailyCountAfter);
            row.setLastToken(tokenCaptor.getValue());
            return row;
        });
    }

    // round5 — 거부 시나리오: last_token이 이번 요청의 토큰과 다른 값(다른 요청이 저장했거나
    // 애초에 아무도 갱신하지 못한 값)으로 남아 있음을 고정된 문자열로 표현한다.
    private void stubRejected(int dailyCount) {
        doNothing().when(rateLimitDao).touchRateLimit(any(), any(), any(), anyInt(), anyInt(), anyString());
        MemberSignupRateLimit row = new MemberSignupRateLimit();
        row.setDailyCount(dailyCount);
        row.setLastToken("existing-token-not-mine");
        when(rateLimitDao.selectRateLimit(any(), any())).thenReturn(row);
    }

    // linked_tc: TC-FUNC-member-002-01
    @Test
    void requestVerificationCode_emailTarget_resolvesEmailChannelAndWritesCode() {
        stubAdmitted(1); // 신규 삽입 — 허용

        MemberSignupService.VerificationCodeResult result =
                service().requestVerificationCode("user@example.com");

        assertThat(result.channel()).isEqualTo("EMAIL");
        assertThat(result.target()).isEqualTo("user@example.com");
        assertThat(result.expiresInSeconds()).isEqualTo(MemberSignupService.EXPIRES_IN_SECONDS);

        verify(rateLimitDao).touchRateLimit(eq("user@example.com"), any(LocalDate.class), any(LocalDateTime.class),
                eq(MemberSignupService.COOLDOWN_SECONDS), eq(MemberSignupService.DAILY_REQUEST_LIMIT), anyString());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> expiresCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(verificationDao).writeCode(eq("EMAIL"), eq("user@example.com"),
                codeCaptor.capture(), expiresCaptor.capture(), any(LocalDateTime.class));

        assertThat(codeCaptor.getValue()).hasSize(MemberSignupService.CODE_LENGTH);
        assertThat(codeCaptor.getValue()).matches("\\d{6}");
        assertThat(expiresCaptor.getValue()).isAfter(LocalDateTime.now());
    }

    // linked_tc: TC-FUNC-member-002-02
    @Test
    void requestVerificationCode_phoneTarget_resolvesSmsChannel() {
        stubAdmitted(1);

        MemberSignupService.VerificationCodeResult result =
                service().requestVerificationCode("01012345678");

        assertThat(result.channel()).isEqualTo("SMS");
        verify(verificationDao).writeCode(eq("SMS"), eq("01012345678"), any(), any(), any());
    }

    // linked_tc: TC-FUNC-member-002-03 — AC: 인증코드 미확인 시 가입 불가로 이어지려면, 애초에
    // 형식이 틀린 target으로는 코드 자체가 발급되지 않아야 한다(400, DAO 미호출).
    @Test
    void requestVerificationCode_invalidFormat_throws400WithTargetInvalidCode() {
        assertThatThrownBy(() -> service().requestVerificationCode("not-an-email-or-phone"))
                .isInstanceOf(MemberSignupApiException.class)
                .extracting(ex -> ((MemberSignupApiException) ex).getHttpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(rateLimitDao, never()).touchRateLimit(any(), any(), any(), anyInt(), anyInt(), anyString());
        verify(verificationDao, never()).writeCode(any(), any(), any(), any(), any());
    }

    @Test
    void requestVerificationCode_blankTarget_throws400WithTargetInvalidCode() {
        assertThatThrownBy(() -> service().requestVerificationCode("  "))
                .isInstanceOf(MemberSignupApiException.class)
                .extracting(ex -> ((MemberSignupApiException) ex).getCode())
                .isEqualTo("MEMBER_TARGET_INVALID");
    }

    @Test
    void requestVerificationCode_nullTarget_throws400WithTargetInvalidCode() {
        assertThatThrownBy(() -> service().requestVerificationCode(null))
                .isInstanceOf(MemberSignupApiException.class)
                .extracting(ex -> ((MemberSignupApiException) ex).getCode())
                .isEqualTo("MEMBER_TARGET_INVALID");
    }

    // round2(round1 QA FAIL 필수3) — target(VARCHAR(100)) 컬럼 길이 초과는 400으로 거부해야
    // DB INSERT 시점의 Error 1406(500)로 새지 않는다.
    // linked_tc: TC-FUNC-member-002-07
    @Test
    void requestVerificationCode_targetOver100Chars_throws400WithTargetInvalidCode() {
        String tooLong = "a".repeat(89) + "@example.com"; // 101자, 이메일 형식은 유지
        assertThat(tooLong.length()).isEqualTo(101);

        assertThatThrownBy(() -> service().requestVerificationCode(tooLong))
                .isInstanceOf(MemberSignupApiException.class)
                .extracting(ex -> ((MemberSignupApiException) ex).getCode())
                .isEqualTo("MEMBER_TARGET_INVALID");

        verify(rateLimitDao, never()).touchRateLimit(any(), any(), any(), anyInt(), anyInt(), anyString());
        verify(verificationDao, never()).writeCode(any(), any(), any(), any(), any());
    }

    // round2(round1 QA FAIL 필수4) — 같은 target을 60초 안에 다시 요청하면 쿨다운(429)으로
    // 거부한다. round5 — 재조회한 행의 last_token이 내 토큰과 다르면 거부이고, dailyCount가
    // 상한 미달이면 쿨다운으로 판정한다.
    // linked_tc: TC-FUNC-member-002-08
    @Test
    void requestVerificationCode_rejectedByAtomicUpsert_belowDailyLimit_throws429CooldownCode() {
        stubRejected(2);

        assertThatThrownBy(() -> service().requestVerificationCode("user@example.com"))
                .isInstanceOf(MemberSignupApiException.class)
                .extracting(ex -> ((MemberSignupApiException) ex).getCode())
                .isEqualTo("MEMBER_VERIFY_COOLDOWN");

        // round4 — 거부된 요청은 코드를 기록하지 않는다(카운터 갱신 자체가 이미 일어나지
        // 않았음 — round3처럼 "증가는 되지만 되돌리지 않는다"가 아니라, 애초에 증가하지 않음).
        verify(verificationDao, never()).writeCode(any(), any(), any(), any(), any());
    }

    // round2(round1 QA FAIL 필수4) — 쿨다운을 지났지만 오늘 이미 상한(5회)에 도달했으면 1일
    // 상한(429)으로 거부한다. round5 — 재조회한 행의 last_token이 내 토큰과 다르고
    // dailyCount가 이미 DAILY_REQUEST_LIMIT에 도달해 있으면 일일상한으로 판정한다.
    // linked_tc: TC-FUNC-member-002-09
    @Test
    void requestVerificationCode_rejectedByAtomicUpsert_atDailyLimit_throws429DailyLimitCode() {
        stubRejected(MemberSignupService.DAILY_REQUEST_LIMIT);

        assertThatThrownBy(() -> service().requestVerificationCode("user@example.com"))
                .isInstanceOf(MemberSignupApiException.class)
                .extracting(ex -> ((MemberSignupApiException) ex).getCode())
                .isEqualTo("MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED");

        verify(verificationDao, never()).writeCode(any(), any(), any(), any(), any());
    }

    // 재요청은 매번 touchRateLimit(원자 판정) + writeCode(코드 갱신)로 처리된다 — house rule:
    // 원자 UPSERT로 재발급 처리(select→분기→update 아님). round5 — stubAdmitted가 매 호출마다
    // 실제로 넘어온 토큰을 그대로 되돌려주므로(동적 answer), 두 번째 호출도 자기 자신의 토큰과
    // 일치해 허용된다.
    @Test
    void requestVerificationCode_calledTwiceForSameTarget_writesCodeTwice() {
        stubAdmitted(2);

        MemberSignupService service = service();
        service.requestVerificationCode("user@example.com");
        service.requestVerificationCode("user@example.com");

        verify(rateLimitDao, times(2)).touchRateLimit(eq("user@example.com"), any(), any(), anyInt(), anyInt(), anyString());
        verify(verificationDao, times(2)).writeCode(eq("EMAIL"), eq("user@example.com"), any(), any(), any());
    }
}
