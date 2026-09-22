// linked_func: FUNC-member-006
// spec: docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberRefreshTokenDao;
import com.sm.lab.shop.domain.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SR-232(FUNC-member-006) — logout의 idempotent 분기, refresh의 단일화된 401 분기(4갈래), 성공
 * 경로의 DAO 호출 순서를 DAO mock으로 격리 검증한다. 트랜잭션·동시성은 Mockito로 증명할 수
 * 없다({@code MemberLoginServiceTest}와 동일 한계) — {@code MemberSessionIntegrationTest}(실 DB)가
 * 보강한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberSessionServiceTest {

    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-12T10:00:00Z"), ZoneId.of("UTC"));
    private static final LocalDateTime NOW = LocalDateTime.now(FIXED_CLOCK);

    @Mock
    private MemberDao memberDao;
    @Mock
    private MemberApiKeyDao apiKeyDao;
    @Mock
    private MemberRefreshTokenDao refreshTokenDao;

    private MemberSessionService service() {
        return new MemberSessionService(memberDao, apiKeyDao, refreshTokenDao, FIXED_CLOCK);
    }

    private static Member member(String memberId, String name, String grade) {
        Member m = new Member();
        m.setMemberId(memberId);
        m.setMemberName(name);
        m.setGrade(grade);
        return m;
    }

    // ===== logout =====

    // linked_tc: TC-FUNC-member-006-06 — memberId를 못 찾으면(admin 키·이미 폐기된 키 등) 두
    // DAO 호출 자체가 일어나지 않는다(idempotent no-op).
    @Test
    void logout_apiKeyNotFound_noopWithoutCallingRevokeDaos() {
        when(apiKeyDao.selectMemberIdByApiKey("lab-admin-key")).thenReturn(null);

        service().logout("lab-admin-key");

        verify(apiKeyDao, never()).revokeByMemberId(any(), any());
        verify(refreshTokenDao, never()).revokeAllForMember(any(), any());
    }

    // linked_tc: TC-FUNC-member-006-07 — QA round1 재작업 지시 1(사람 확정)로 순서가 뒤집혔다:
    // revokeAllForMember(리프레시 토큰 전체 폐기) → revokeByMemberId(apiKey 폐기) 순서로 정확히
    // 1회씩 호출한다. 종전(apiKey 먼저) 순서는 두 번째 문장이 실패하면 apiKey만 죽고
    // refreshToken이 살아남아 refresh로 세션이 되살아나는 결함이 있었다(TC-006-27이 실측).
    @Test
    void logout_apiKeyFound_revokesAllRefreshTokensThenApiKeyInOrder() {
        when(apiKeyDao.selectMemberIdByApiKey("mk_abc")).thenReturn("M-0005");

        service().logout("mk_abc");

        InOrder order = inOrder(apiKeyDao, refreshTokenDao);
        order.verify(refreshTokenDao).revokeAllForMember("M-0005", NOW);
        order.verify(apiKeyDao).revokeByMemberId("M-0005", NOW);
    }

    // ===== refresh — 실패 4갈래, 전부 동일한 401 MBR-4012 =====

    @Test
    void refresh_tokenNotFound_throwsUnifiedInvalidSessionException() {
        when(refreshTokenDao.selectActiveByTokenHash(anyString(), eq(NOW))).thenReturn(null);

        assertUnifiedInvalidSessionException(() -> service().refresh("no-such-token"));
    }

    // selectActiveByTokenHash의 WHERE expires_at > now 조건이 SQL에서 이미 걸러내므로, 이
    // 서비스 단위 테스트 관점에서는 "만료"도 "미존재"와 동일하게 null로 관측된다(005의 잠금
    // 판정과 마찬가지로 SQL 필터 자체는 DAO 통합 테스트가 검증한다 — MemberRefreshTokenDaoTest).
    @Test
    void refresh_tokenExpired_throwsUnifiedInvalidSessionException() {
        when(refreshTokenDao.selectActiveByTokenHash(anyString(), eq(NOW))).thenReturn(null);

        assertUnifiedInvalidSessionException(() -> service().refresh("expired-token"));
    }

    // selectActiveByTokenHash의 WHERE revoked_at IS NULL 조건이 SQL에서 이미 걸러낸다(위 주석과
    // 동일 한계).
    @Test
    void refresh_tokenRevoked_throwsUnifiedInvalidSessionException() {
        when(refreshTokenDao.selectActiveByTokenHash(anyString(), eq(NOW))).thenReturn(null);

        assertUnifiedInvalidSessionException(() -> service().refresh("revoked-token"));
    }

    // linked_tc: TC-FUNC-member-006-08 — 회원탈퇴(memberDao.selectById가 del_yn='N' 필터로 null
    // 반환)도 동일한 401. 사례집 "API 키 DB 폴백이 탈퇴 회원 키를 통과" 재발 방지 원칙을
    // refresh에도 그대로 적용한다.
    @Test
    void refresh_memberWithdrawn_throwsUnifiedInvalidSessionException() {
        when(refreshTokenDao.selectActiveByTokenHash(anyString(), eq(NOW))).thenReturn("M-0009");
        when(memberDao.selectById("M-0009")).thenReturn(null);

        assertUnifiedInvalidSessionException(() -> service().refresh("withdrawn-member-token"));

        verify(refreshTokenDao, never()).insert(any(), any(), any(), any());
        verify(apiKeyDao, never()).issueIfAbsent(any(), any(), any());
    }

    private void assertUnifiedInvalidSessionException(ThrowingCallable callable) {
        assertThatThrownBy(callable::call)
                .isInstanceOf(MemberSessionApiException.class)
                .satisfies(ex -> {
                    MemberSessionApiException e = (MemberSessionApiException) ex;
                    assertThat(e.getHttpStatus().value()).isEqualTo(401);
                    assertThat(e.getCode()).isEqualTo("MBR-4012");
                    assertThat(e.getMessage()).isEqualTo("유효하지 않거나 만료된 로그인 정보입니다");
                });
    }

    private interface ThrowingCallable {
        void call();
    }

    // ===== refresh — 성공 경로 =====

    // linked_tc: TC-FUNC-member-006-09 — insert→revokeByTokenHash(구토큰)→purgeExpiredOrRevoked→
    // enforceActiveCap→issueIfAbsent→selectByMemberId 순서.
    @Test
    void refresh_success_rotatesTokenAndApiKeyInOrder() {
        when(refreshTokenDao.selectActiveByTokenHash(anyString(), eq(NOW))).thenReturn("M-0005");
        when(memberDao.selectById("M-0005")).thenReturn(member("M-0005", "테스터", "BRONZE"));
        when(apiKeyDao.selectByMemberId("M-0005")).thenReturn("mk_rotated");

        MemberSessionService.SessionResult result = service().refresh("old-refresh-token-plain");

        assertThat(result.memberId()).isEqualTo("M-0005");
        assertThat(result.memberName()).isEqualTo("테스터");
        assertThat(result.grade()).isEqualTo("BRONZE");
        assertThat(result.apiKey()).isEqualTo("mk_rotated");
        assertThat(result.refreshToken()).isNotBlank().isNotEqualTo("old-refresh-token-plain");
        assertThat(result.refreshTokenExpiresAt()).isEqualTo(NOW.plusDays(MemberSessionService.REFRESH_TOKEN_VALID_DAYS));

        InOrder order = inOrder(refreshTokenDao, apiKeyDao);
        order.verify(refreshTokenDao).insert(any(), eq("M-0005"), eq(NOW),
                eq(NOW.plusDays(MemberSessionService.REFRESH_TOKEN_VALID_DAYS)));
        order.verify(refreshTokenDao).revokeByTokenHash(anyString(), eq(NOW));
        order.verify(refreshTokenDao).purgeExpiredOrRevoked("M-0005", NOW);
        order.verify(refreshTokenDao).enforceActiveCap("M-0005", MemberSessionService.MAX_ACTIVE_REFRESH_TOKENS);
        order.verify(apiKeyDao).issueIfAbsent(eq("M-0005"), any(), eq(NOW));
        order.verify(apiKeyDao).selectByMemberId("M-0005");
    }
}
