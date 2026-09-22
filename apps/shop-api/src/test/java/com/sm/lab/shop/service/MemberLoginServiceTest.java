// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberLoginAttemptDao;
import com.sm.lab.shop.dao.MemberRefreshTokenDao;
import com.sm.lab.shop.domain.MemberCredential;
import com.sm.lab.shop.domain.MemberLoginAttempt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SR-232(FUNC-member-005) — 잠금/미가입/탈퇴/비번오류/성공 각 분기의 DAO 호출을 격리 검증한다.
 * 트랜잭션·동시성은 Mockito로 증명할 수 없다({@code MemberRegistrationService} 클래스 javadoc과
 * 동일 한계) — {@code MemberLoginConcurrencyTest}(실 DB)가 보강한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberLoginServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-12T10:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private MemberDao memberDao;
    @Mock
    private MemberLoginAttemptDao loginAttemptDao;
    @Mock
    private MemberRefreshTokenDao refreshTokenDao;
    @Mock
    private MemberApiKeyDao apiKeyDao;

    private MemberLoginService service() {
        return new MemberLoginService(memberDao, loginAttemptDao, refreshTokenDao, apiKeyDao, FIXED_CLOCK);
    }

    private static MemberCredential credential(String memberId, String rawPassword, String delYn) {
        MemberCredential c = new MemberCredential();
        c.setMemberId(memberId);
        c.setMemberName("테스터");
        c.setGrade("BRONZE");
        c.setPasswordHash(new BCryptPasswordEncoder().encode(rawPassword));
        c.setDelYn(delYn);
        return c;
    }

    private static MemberLoginAttempt attemptWithFailCount(int failCount) {
        MemberLoginAttempt a = new MemberLoginAttempt();
        a.setFailCount(failCount);
        return a;
    }

    // linked_tc: TC-FUNC-member-005-05 — 순서·보안 1: 잠금 여부를 비밀번호 검증보다 먼저 본다.
    @Test
    void login_locked_throws429WithoutCallingMemberDao() {
        MemberLoginAttempt locked = new MemberLoginAttempt();
        locked.setEmail("locked@example.com");
        locked.setFailCount(5);
        locked.setLockedUntil(NOW.plusMinutes(7));
        when(loginAttemptDao.selectAttempt("locked@example.com")).thenReturn(locked);

        assertThatThrownBy(() -> service().login("locked@example.com", "whatever"))
                .isInstanceOf(MemberLoginApiException.class)
                .satisfies(ex -> {
                    MemberLoginApiException e = (MemberLoginApiException) ex;
                    assertThat(e.getCode()).isEqualTo("MBR-4291");
                    assertThat(e.getMessage()).isEqualTo("로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요");
                });

        verify(memberDao, never()).selectAuthByEmail(any());
    }

    // linked_tc: TC-FUNC-member-005-06 — 회원 미존재 = 비번 오류와 동일 코드·문구.
    @Test
    void login_memberNotFound_throws401WithGenericMessage() {
        when(loginAttemptDao.selectAttempt("nobody@example.com"))
                .thenReturn(null, attemptWithFailCount(1));
        when(memberDao.selectAuthByEmail("nobody@example.com")).thenReturn(null);

        assertThatThrownBy(() -> service().login("nobody@example.com", "whatever"))
                .isInstanceOf(MemberLoginApiException.class)
                .satisfies(ex -> {
                    MemberLoginApiException e = (MemberLoginApiException) ex;
                    assertThat(e.getHttpStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(e.getCode()).isEqualTo("MBR-4011");
                    assertThat(e.getMessage()).contains("1/5");
                });

        verify(loginAttemptDao).touchFailure(eq("nobody@example.com"), eq(NOW), anyInt(), anyInt());
    }

    // linked_tc: TC-FUNC-member-005-07 — 탈퇴 회원(delYn='Y') = 비번 오류와 동일 코드·문구.
    @Test
    void login_deletedMember_throws401WithGenericMessage() {
        when(loginAttemptDao.selectAttempt("withdrawn@example.com"))
                .thenReturn(null, attemptWithFailCount(1));
        when(memberDao.selectAuthByEmail("withdrawn@example.com"))
                .thenReturn(credential("M-0009", "abcd1234", "Y"));

        assertThatThrownBy(() -> service().login("withdrawn@example.com", "abcd1234"))
                .isInstanceOf(MemberLoginApiException.class)
                .extracting(ex -> ((MemberLoginApiException) ex).getCode())
                .isEqualTo("MBR-4011");
    }

    // linked_tc: TC-FUNC-member-005-08 — 비밀번호 불일치.
    @Test
    void login_wrongPassword_throws401AndIncrementsAttempt() {
        when(loginAttemptDao.selectAttempt("user@example.com"))
                .thenReturn(null, attemptWithFailCount(2));
        when(memberDao.selectAuthByEmail("user@example.com"))
                .thenReturn(credential("M-0005", "correct-pass", "N"));

        assertThatThrownBy(() -> service().login("user@example.com", "wrong-pass"))
                .isInstanceOf(MemberLoginApiException.class)
                .satisfies(ex -> {
                    MemberLoginApiException e = (MemberLoginApiException) ex;
                    assertThat(e.getCode()).isEqualTo("MBR-4011");
                    assertThat(e.getMessage()).contains("2/5");
                });

        verify(loginAttemptDao).touchFailure(eq("user@example.com"), eq(NOW), anyInt(), anyInt());
        verify(refreshTokenDao, never()).insert(any(), any(), any(), any());
    }

    // linked_tc: TC-FUNC-member-005-09 — 5번째 실패 자체가 429(사람 확인 2), 401(5/5)가 아니다.
    @Test
    void login_fifthFailure_throws429NotFourOhOne() {
        MemberLoginAttempt lockedAfterFifth = new MemberLoginAttempt();
        lockedAfterFifth.setFailCount(5);
        lockedAfterFifth.setLockedUntil(NOW.plusMinutes(10));
        when(loginAttemptDao.selectAttempt("user@example.com")).thenReturn(null, lockedAfterFifth);
        when(memberDao.selectAuthByEmail("user@example.com"))
                .thenReturn(credential("M-0005", "correct-pass", "N"));

        assertThatThrownBy(() -> service().login("user@example.com", "wrong-pass"))
                .isInstanceOf(MemberLoginApiException.class)
                .extracting(ex -> ((MemberLoginApiException) ex).getCode())
                .isEqualTo("MBR-4291");
    }

    // linked_tc: TC-FUNC-member-005-10 — 성공: reset → refresh token 발급(해시만 DB에) → apiKey 조회/발급.
    @Test
    void login_success_resetsCounterIssuesRefreshTokenAndApiKey() {
        when(loginAttemptDao.selectAttempt("user@example.com")).thenReturn(null);
        when(memberDao.selectAuthByEmail("user@example.com"))
                .thenReturn(credential("M-0005", "abcd1234", "N"));
        when(apiKeyDao.selectByMemberId("M-0005")).thenReturn("mk_existing");

        MemberLoginService.LoginResult result = service().login("user@example.com", "abcd1234");

        assertThat(result.memberId()).isEqualTo("M-0005");
        assertThat(result.apiKey()).isEqualTo("mk_existing");
        assertThat(result.refreshToken()).isNotBlank();
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(NOW.plusDays(30));

        verify(loginAttemptDao).reset("user@example.com");
        verify(refreshTokenDao).insert(any(), eq("M-0005"), eq(NOW), eq(NOW.plusDays(30)));
        // round 9(재작업 지시 3/4) — 발급 직후 만료·폐기 정리 + 상한(5) 강제가 호출됐는지.
        verify(refreshTokenDao).purgeExpiredOrRevoked("M-0005", NOW);
        verify(refreshTokenDao).enforceActiveCap("M-0005", MemberLoginService.MAX_ACTIVE_REFRESH_TOKENS);
        verify(apiKeyDao).issueIfAbsent(eq("M-0005"), any(), eq(NOW));
        verify(loginAttemptDao, never()).touchFailure(any(), any(), anyInt(), anyInt());
    }
}
