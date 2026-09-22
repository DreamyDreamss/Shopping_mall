// linked_func: FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberPasswordResetDao;
import com.sm.lab.shop.domain.MemberPasswordReset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SR-234(FUNC-member-009) — {@link MemberPasswordResetConfirmationService} 단위 테스트(Mockito,
 * DAO+writer 목). 실제 SQL 시맨틱({@code confirmIfCodeMatches}/{@code incrementAttemptCount}의
 * 원자 UPDATE·상한 강제)은 {@link com.sm.lab.shop.dao.MemberPasswordResetDaoTest}(실 DB)가
 * 별도 검증한다. target/password 검증은 형제 서비스의 실제 static 메서드를 그대로 호출한다
 * (재사용 경로 자체를 증명 — 사람 수정 (2) 핵심 증거).
 */
@ExtendWith(MockitoExtension.class)
class MemberPasswordResetConfirmationServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-13T10:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private MemberPasswordResetDao passwordResetDao;
    @Mock
    private MemberDao memberDao;
    @Mock
    private MemberPasswordResetConfirmationWriter writer;

    private ListAppender<ILoggingEvent> logAppender;

    private MemberPasswordResetConfirmationService service() {
        return new MemberPasswordResetConfirmationService(passwordResetDao, memberDao, writer, FIXED_CLOCK);
    }

    @BeforeEach
    void attachLogAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(MemberPasswordResetConfirmationService.class)).addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(MemberPasswordResetConfirmationService.class)).detachAndStopAllAppenders();
    }

    private static MemberPasswordReset resetRow(LocalDateTime consumedAt, LocalDateTime expiresAt, int attemptCount) {
        MemberPasswordReset row = new MemberPasswordReset();
        row.setTarget("user@example.com");
        row.setCodeHash("irrelevant-in-this-test");
        row.setConsumedAt(consumedAt);
        row.setExpiresAt(expiresAt);
        row.setAttemptCount(attemptCount);
        return row;
    }

    // ===== 순서·보안 1~2 — 형식 검증 실패 시 confirmIfCodeMatches 미호출 =====

    @Test
    void confirmPasswordReset_invalidTarget_throws400Mbr4100AndDaoNeverCalled() {
        assertThatThrownBy(() -> service().confirmPasswordReset("not-an-email-or-phone", "123456", "abcd1234"))
                .isInstanceOf(MemberPasswordResetConfirmationApiException.class)
                .satisfies(ex -> {
                    MemberPasswordResetConfirmationApiException e = (MemberPasswordResetConfirmationApiException) ex;
                    assertThat(e.getHttpStatus().value()).isEqualTo(400);
                    assertThat(e.getCode()).isEqualTo("MBR-4100");
                });

        verify(passwordResetDao, never()).confirmIfCodeMatches(any(), any(), any(), anyInt());
    }

    @Test
    void confirmPasswordReset_invalidPassword_throws400Mbr4001AndDaoNeverCalled() {
        assertThatThrownBy(() -> service().confirmPasswordReset("user@example.com", "123456", "short"))
                .isInstanceOf(MemberPasswordResetConfirmationApiException.class)
                .satisfies(ex -> {
                    MemberPasswordResetConfirmationApiException e = (MemberPasswordResetConfirmationApiException) ex;
                    assertThat(e.getHttpStatus().value()).isEqualTo(400);
                    assertThat(e.getCode()).isEqualTo("MBR-4001");
                });

        verify(passwordResetDao, never()).confirmIfCodeMatches(any(), any(), any(), anyInt());
    }

    // ===== 확정 성공 =====

    @Test
    void confirmPasswordReset_confirmed_callsWriterWithBcryptHashedPassword() {
        when(passwordResetDao.confirmIfCodeMatches(eq("user@example.com"), any(), eq(NOW), anyInt()))
                .thenReturn(1);
        when(memberDao.selectMemberIdByResetTarget(eq("user@example.com"), isNull())).thenReturn("M-0001");

        service().confirmPasswordReset("user@example.com", "123456", "abcd1234");

        ArgumentCaptor<String> passwordHashCaptor = ArgumentCaptor.forClass(String.class);
        verify(writer).applyNewPassword(eq("M-0001"), passwordHashCaptor.capture(), eq(NOW));
        assertThat(new BCryptPasswordEncoder().matches("abcd1234", passwordHashCaptor.getValue())).isTrue();

        assertThat(logAppender.list).isNotEmpty();
    }

    // ===== 5단계 실패 분기(SR-298 원자화 — incrementAttemptCount를 먼저·무조건 시도) =====

    // TO-BE 핵심 전환 — 증가 성공(1행)이면 그 자체로 "단순 오답"이 확정되므로 selectByTarget을
    // 아예 부르지 않는다(AS-IS는 항상 먼저 읽었다).
    @Test
    void confirmPasswordReset_simpleMismatchUnderCap_incrementsOnceAndThrows409Mbr4102WithoutSelectByTarget() {
        when(passwordResetDao.confirmIfCodeMatches(eq("user@example.com"), any(), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.incrementAttemptCount(eq("user@example.com"), eq(NOW), anyInt()))
                .thenReturn(1);

        assertThatThrownBy(() -> service().confirmPasswordReset("user@example.com", "123456", "abcd1234"))
                .isInstanceOf(MemberPasswordResetConfirmationApiException.class)
                .satisfies(ex -> {
                    MemberPasswordResetConfirmationApiException e = (MemberPasswordResetConfirmationApiException) ex;
                    assertThat(e.getHttpStatus().value()).isEqualTo(409);
                    assertThat(e.getCode()).isEqualTo("MBR-4102");
                });

        verify(passwordResetDao, times(1)).incrementAttemptCount("user@example.com", NOW, 5);
        verify(passwordResetDao, never()).selectByTarget(any());
    }

    // increment가 0을 반환하는 4갈래 중 "행 없음"(STORY-1 선례 — 0행 사유는 상한 하나만이 아니다).
    @Test
    void confirmPasswordReset_incrementZeroAndRowMissing_throws409Mbr4102() {
        when(passwordResetDao.confirmIfCodeMatches(eq("user@example.com"), any(), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.incrementAttemptCount(eq("user@example.com"), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.selectByTarget("user@example.com")).thenReturn(null);

        assertThatThrownBy(() -> service().confirmPasswordReset("user@example.com", "123456", "abcd1234"))
                .isInstanceOf(MemberPasswordResetConfirmationApiException.class)
                .satisfies(ex -> {
                    MemberPasswordResetConfirmationApiException e = (MemberPasswordResetConfirmationApiException) ex;
                    assertThat(e.getHttpStatus().value()).isEqualTo(409);
                    assertThat(e.getCode()).isEqualTo("MBR-4102");
                });

        verify(passwordResetDao, times(1)).incrementAttemptCount("user@example.com", NOW, 5);
        verify(passwordResetDao, times(1)).selectByTarget("user@example.com");
    }

    // increment 0 + 이미 소비됨/만료 — 두 서브케이스, 기존 resetRow 헬퍼 재사용.
    @Test
    void confirmPasswordReset_incrementZeroAndAlreadyConsumed_throws410Mbr4101() {
        when(passwordResetDao.confirmIfCodeMatches(eq("user@example.com"), any(), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.incrementAttemptCount(eq("user@example.com"), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.selectByTarget("user@example.com"))
                .thenReturn(resetRow(NOW.minusMinutes(1), NOW.plusMinutes(5), 1));

        assertThatThrownBy(() -> service().confirmPasswordReset("user@example.com", "123456", "abcd1234"))
                .isInstanceOf(MemberPasswordResetConfirmationApiException.class)
                .extracting(ex -> ((MemberPasswordResetConfirmationApiException) ex).getCode())
                .isEqualTo("MBR-4101");
    }

    @Test
    void confirmPasswordReset_incrementZeroAndExpired_throws410Mbr4101() {
        when(passwordResetDao.confirmIfCodeMatches(eq("user@example.com"), any(), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.incrementAttemptCount(eq("user@example.com"), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.selectByTarget("user@example.com"))
                .thenReturn(resetRow(null, NOW.minusSeconds(1), 1));

        assertThatThrownBy(() -> service().confirmPasswordReset("user@example.com", "123456", "abcd1234"))
                .isInstanceOf(MemberPasswordResetConfirmationApiException.class)
                .extracting(ex -> ((MemberPasswordResetConfirmationApiException) ex).getCode())
                .isEqualTo("MBR-4101");
    }

    @Test
    void confirmPasswordReset_incrementZeroAndAttemptCountAtCap_throws409Mbr4103() {
        when(passwordResetDao.confirmIfCodeMatches(eq("user@example.com"), any(), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.incrementAttemptCount(eq("user@example.com"), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.selectByTarget("user@example.com"))
                .thenReturn(resetRow(null, NOW.plusMinutes(5), 5));

        assertThatThrownBy(() -> service().confirmPasswordReset("user@example.com", "123456", "abcd1234"))
                .isInstanceOf(MemberPasswordResetConfirmationApiException.class)
                .extracting(ex -> ((MemberPasswordResetConfirmationApiException) ex).getCode())
                .isEqualTo("MBR-4103");
    }

    // 사람 수정 (1) 핵심 증거 — "행 없음"과 "단순 오답"이 동일한 예외 타입·code·message를 던진다.
    // TO-BE에서는 mock 배선이 갈린다: 행 없음 경로는 incrementAttemptCount=0+selectByTarget=null,
    // 단순 오답 경로는 incrementAttemptCount=1(selectByTarget 미호출).
    @Test
    void confirmPasswordReset_rowMissingAndSimpleMismatch_throwSameExceptionShape() {
        when(passwordResetDao.confirmIfCodeMatches(eq("user@example.com"), any(), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.incrementAttemptCount(eq("user@example.com"), eq(NOW), anyInt()))
                .thenReturn(0);
        when(passwordResetDao.selectByTarget("user@example.com")).thenReturn(null);

        MemberPasswordResetConfirmationApiException rowMissing =
                (MemberPasswordResetConfirmationApiException) catchThrowable(
                        () -> service().confirmPasswordReset("user@example.com", "123456", "abcd1234"));

        when(passwordResetDao.incrementAttemptCount(eq("user@example.com"), eq(NOW), anyInt()))
                .thenReturn(1);

        MemberPasswordResetConfirmationApiException simpleMismatch =
                (MemberPasswordResetConfirmationApiException) catchThrowable(
                        () -> service().confirmPasswordReset("user@example.com", "123456", "abcd1234"));

        assertThat(rowMissing.getClass()).isEqualTo(simpleMismatch.getClass());
        assertThat(rowMissing.getCode()).isEqualTo(simpleMismatch.getCode());
        assertThat(rowMissing.getMessage()).isEqualTo(simpleMismatch.getMessage());
        assertThat(rowMissing.getHttpStatus()).isEqualTo(simpleMismatch.getHttpStatus());
    }

    // ===== 존재 오라클 방지 — 회원 발견 여부 =====

    @Test
    void confirmPasswordReset_memberNotFound_returnsWithoutExceptionAndWriterNeverCalled() {
        when(passwordResetDao.confirmIfCodeMatches(eq("user@example.com"), any(), eq(NOW), anyInt()))
                .thenReturn(1);
        when(memberDao.selectMemberIdByResetTarget(eq("user@example.com"), isNull())).thenReturn(null);

        service().confirmPasswordReset("user@example.com", "123456", "abcd1234");

        verify(writer, never()).applyNewPassword(any(), any(), any());
        assertThat(logAppender.list).as("회원 미발견 경로는 완료 로그를 남기지 않는다").isEmpty();
    }

    private static Throwable catchThrowable(ThrowingRunnable runnable) {
        try {
            runnable.run();
            return null;
        } catch (Throwable t) {
            return t;
        }
    }

    private interface ThrowingRunnable {
        void run();
    }
}
