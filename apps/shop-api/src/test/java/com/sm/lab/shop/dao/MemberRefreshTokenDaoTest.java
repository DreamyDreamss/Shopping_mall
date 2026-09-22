// linked_func: FUNC-member-005, FUNC-member-006
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md, docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.MEMBER_REFRESH_TOKENS) 대상 통합 테스트 — 005는
 * {@link MemberRefreshTokenDao#insert}와 자기 하우스키핑({@link MemberRefreshTokenDao#purgeExpiredOrRevoked},
 * {@link MemberRefreshTokenDao#enforceActiveCap})만 가졌다. FUNC-member-006이 토큰 원문 기준
 * 조회·전체 폐기·회전 폐기({@link MemberRefreshTokenDao#selectActiveByTokenHash},
 * {@link MemberRefreshTokenDao#revokeAllForMember}, {@link MemberRefreshTokenDao#revokeByTokenHash})를
 * add-only로 추가한다 — {@link #FUNC_006_MEMBER_ID}/{@link #FUNC_006_TOKEN_HASH}로 005 테스트
 * 행과 겹치지 않게 격리한다(계획 "테스트 격리" 절). 원문이 아니라 해시만 저장되는지, revoked_at이
 * 항상 NULL로 들어가는지, round 9(재작업 지시 4) 하우스키핑 두 메서드가 실제로 지우는지를 실
 * SQL로 확인한다. 삭제 메서드가 없어 정리는 JdbcTemplate으로 직접 한다.
 */
@SpringBootTest
class MemberRefreshTokenDaoTest {

    private static final String TOKEN_HASH = "refresh-dao-test-hash";
    private static final String MEMBER_ID = "M-9002-TEST";
    private static final String FUNC_006_MEMBER_ID = "M-9006-RT-TEST";
    private static final String FUNC_006_TOKEN_HASH = "func-006-refresh-dao-test-hash";
    private static final String FUNC_006_TOKEN_HASH_2 = "func-006-refresh-dao-test-hash-2";

    @Autowired
    private MemberRefreshTokenDao dao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM MEMBER_REFRESH_TOKENS WHERE token_hash = ? OR member_id = ?",
                TOKEN_HASH, MEMBER_ID);
        jdbcTemplate.update("DELETE FROM MEMBER_REFRESH_TOKENS WHERE member_id = ?", FUNC_006_MEMBER_ID);
    }

    @Test
    void insert_storesHashOnly_revokedAtNull() {
        LocalDateTime issuedAt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        LocalDateTime expiresAt = issuedAt.plusDays(30);

        dao.insert(TOKEN_HASH, MEMBER_ID, issuedAt, expiresAt);

        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT member_id, issued_at, expires_at, revoked_at FROM MEMBER_REFRESH_TOKENS WHERE token_hash = ?",
                TOKEN_HASH);
        assertThat(row.get("member_id")).isEqualTo(MEMBER_ID);
        assertThat(row.get("revoked_at")).as("폐기는 FUNC-006 소관 — 이 FUNC은 항상 NULL로만 INSERT").isNull();
    }

    // linked_tc: TC-FUNC-member-005-13 — round 9(재작업 지시 4) 만료 행은 지우고 유효 행은 남긴다.
    @Test
    void purgeExpiredOrRevoked_deletesExpiredAndRevoked_keepsActive() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.insert("purge-expired", MEMBER_ID, now.minusDays(31), now.minusDays(1)); // 만료
        dao.insert("purge-active", MEMBER_ID, now, now.plusDays(30)); // 유효
        dao.insert("purge-revoked", MEMBER_ID, now, now.plusDays(30));
        jdbcTemplate.update("UPDATE MEMBER_REFRESH_TOKENS SET revoked_at = ? WHERE token_hash = ?",
                now, "purge-revoked");

        int deleted = dao.purgeExpiredOrRevoked(MEMBER_ID, now);

        assertThat(deleted).isEqualTo(2);
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_REFRESH_TOKENS WHERE member_id = ?", Integer.class, MEMBER_ID);
        assertThat(remaining).isEqualTo(1);
        Integer stillThere = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_REFRESH_TOKENS WHERE token_hash = ?", Integer.class, "purge-active");
        assertThat(stillThere).isEqualTo(1);
    }

    // linked_tc: TC-FUNC-member-005-14 — round 9(재작업 지시 4) 상한 초과분은 오래된 순으로 지운다.
    @Test
    void enforceActiveCap_keepsOnlyMostRecentByIssuedAt() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        for (int i = 0; i < 7; i++) {
            dao.insert("cap-token-" + i, MEMBER_ID, now.minusMinutes(7 - i), now.plusDays(30));
        }

        dao.enforceActiveCap(MEMBER_ID, 5);

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_REFRESH_TOKENS WHERE member_id = ?", Integer.class, MEMBER_ID);
        assertThat(remaining).isEqualTo(5);
        // 가장 오래된 2개(cap-token-0/1)만 지워지고 최근 5개(cap-token-2..6)는 남아야 한다.
        Integer oldestGone = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_REFRESH_TOKENS WHERE token_hash IN ('cap-token-0','cap-token-1')",
                Integer.class);
        assertThat(oldestGone).isEqualTo(0);
        Integer newestKept = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_REFRESH_TOKENS WHERE token_hash = 'cap-token-6'", Integer.class);
        assertThat(newestKept).isEqualTo(1);
    }

    // ===== FUNC-member-006 — selectActiveByTokenHash/revokeAllForMember/revokeByTokenHash 신규 =====

    // linked_tc: TC-FUNC-member-006-14 — 자격 판정의 유일한 검문소. 유효한(폐기 안 됨·만료 안 됨)
    // 토큰은 memberId를 반환한다.
    @Test
    void selectActiveByTokenHash_activeToken_returnsMemberId() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.insert(FUNC_006_TOKEN_HASH, FUNC_006_MEMBER_ID, now, now.plusDays(30));

        assertThat(dao.selectActiveByTokenHash(FUNC_006_TOKEN_HASH, now)).isEqualTo(FUNC_006_MEMBER_ID);
    }

    // linked_tc: TC-FUNC-member-006-15
    @Test
    void selectActiveByTokenHash_unknownHash_returnsNull() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        assertThat(dao.selectActiveByTokenHash("no-such-hash-anywhere", now)).isNull();
    }

    // linked_tc: TC-FUNC-member-006-16 — 만료된 토큰은 결과 없음(폐기·탈퇴와 refresh에서 동일한
    // 401로 취급되는 근거 — 계획 "폴백·우회 경로의 자격 판정" 절).
    @Test
    void selectActiveByTokenHash_expiredToken_returnsNull() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.insert(FUNC_006_TOKEN_HASH, FUNC_006_MEMBER_ID, now.minusDays(31), now.minusDays(1));

        assertThat(dao.selectActiveByTokenHash(FUNC_006_TOKEN_HASH, now)).isNull();
    }

    // linked_tc: TC-FUNC-member-006-17 — 폐기된 토큰도 결과 없음. 이 필터가 SQL WHERE에 없으면
    // 로그아웃된 세션이 refresh로 영구히 되살아난다(사례집 계열 재발 방지, 계획 참고).
    @Test
    void selectActiveByTokenHash_revokedToken_returnsNull() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.insert(FUNC_006_TOKEN_HASH, FUNC_006_MEMBER_ID, now, now.plusDays(30));
        dao.revokeByTokenHash(FUNC_006_TOKEN_HASH, now);

        assertThat(dao.selectActiveByTokenHash(FUNC_006_TOKEN_HASH, now)).isNull();
    }

    // linked_tc: TC-FUNC-member-006-18 — 로그아웃 = 이 회원의 활성 토큰 전체 폐기(개별 아님).
    @Test
    void revokeAllForMember_multipleActiveTokens_revokesAllOfThem() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.insert(FUNC_006_TOKEN_HASH, FUNC_006_MEMBER_ID, now, now.plusDays(30));
        dao.insert(FUNC_006_TOKEN_HASH_2, FUNC_006_MEMBER_ID, now, now.plusDays(30));

        int revoked = dao.revokeAllForMember(FUNC_006_MEMBER_ID, now.plusMinutes(1));

        assertThat(revoked).isEqualTo(2);
        assertThat(dao.selectActiveByTokenHash(FUNC_006_TOKEN_HASH, now.plusMinutes(1))).isNull();
        assertThat(dao.selectActiveByTokenHash(FUNC_006_TOKEN_HASH_2, now.plusMinutes(1))).isNull();
    }

    // linked_tc: TC-FUNC-member-006-19 — 이미 전부 폐기된 뒤 다시 호출해도 안전(idempotent, 0행).
    @Test
    void revokeAllForMember_calledTwice_secondCallAffectsNoRows() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.insert(FUNC_006_TOKEN_HASH, FUNC_006_MEMBER_ID, now, now.plusDays(30));
        dao.revokeAllForMember(FUNC_006_MEMBER_ID, now.plusMinutes(1));

        int secondCall = dao.revokeAllForMember(FUNC_006_MEMBER_ID, now.plusMinutes(2));

        assertThat(secondCall).isEqualTo(0);
    }

    // linked_tc: TC-FUNC-member-006-20 — refresh 회전: 구 토큰만 개별 폐기하고 이 회원의 다른
    // 활성 토큰에는 영향이 없다(revokeAllForMember와의 차이).
    @Test
    void revokeByTokenHash_onlyRevokesThatToken_leavesOtherActiveTokensForSameMember() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.insert(FUNC_006_TOKEN_HASH, FUNC_006_MEMBER_ID, now, now.plusDays(30));
        dao.insert(FUNC_006_TOKEN_HASH_2, FUNC_006_MEMBER_ID, now, now.plusDays(30));

        int revoked = dao.revokeByTokenHash(FUNC_006_TOKEN_HASH, now.plusMinutes(1));

        assertThat(revoked).isEqualTo(1);
        assertThat(dao.selectActiveByTokenHash(FUNC_006_TOKEN_HASH, now.plusMinutes(1)))
                .as("회전된 구 토큰은 더 이상 유효하지 않다").isNull();
        assertThat(dao.selectActiveByTokenHash(FUNC_006_TOKEN_HASH_2, now.plusMinutes(1)))
                .as("같은 회원의 다른 활성 토큰은 영향받지 않는다").isEqualTo(FUNC_006_MEMBER_ID);
    }
}
