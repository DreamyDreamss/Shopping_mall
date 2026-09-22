// linked_func: FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberRefreshTokenDao;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SR-234(FUNC-member-009) round2 재작업(QA CONCERNS 권고 2) — {@link
 * MemberPasswordResetConfirmationWriter#applyNewPassword} 단위 테스트. {@link
 * MemberPasswordResetConfirmationServiceTest}는 이 writer 자체를 목으로 대체하므로
 * {@code refreshTokenDao}/{@code apiKeyDao} 호출 여부(writer 내부 협력자)는 그 테스트로
 * 증명할 수 없다 — writer를 직접 대상으로 한 이 테스트 클래스가 필요하다.
 */
@ExtendWith(MockitoExtension.class)
class MemberPasswordResetConfirmationWriterTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 13, 10, 0, 0);

    @Mock
    private MemberDao memberDao;
    @Mock
    private MemberRefreshTokenDao refreshTokenDao;
    @Mock
    private MemberApiKeyDao apiKeyDao;

    private MemberPasswordResetConfirmationWriter writer() {
        return new MemberPasswordResetConfirmationWriter(memberDao, refreshTokenDao, apiKeyDao);
    }

    @Test
    void applyNewPassword_updatedOneRow_revokesRefreshTokensAndApiKey() {
        when(memberDao.updatePasswordHash("M-0001", "hashed", NOW)).thenReturn(1);

        writer().applyNewPassword("M-0001", "hashed", NOW);

        verify(refreshTokenDao).revokeAllForMember("M-0001", NOW);
        verify(apiKeyDao).revokeByMemberId("M-0001", NOW);
    }

    // round2 QA CONCERNS 권고 2 — updatePasswordHash가 0행(그 사이 탈퇴 등)이면 이후 두 폐기
    // 문장을 건너뛴다(계획 9단계, MemberDao#updatePasswordHash javadoc 그대로).
    @Test
    void applyNewPassword_updatedZeroRows_skipsBothRevocations() {
        when(memberDao.updatePasswordHash("M-0001", "hashed", NOW)).thenReturn(0);

        writer().applyNewPassword("M-0001", "hashed", NOW);

        verify(refreshTokenDao, never()).revokeAllForMember(any(), any());
        verify(apiKeyDao, never()).revokeByMemberId(any(), any());
    }
}
