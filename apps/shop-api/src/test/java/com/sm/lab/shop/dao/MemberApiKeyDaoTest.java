// linked_func: FUNC-member-005, FUNC-member-006
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md, docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.MEMBER_API_KEYS) 대상 통합 테스트 — no-op UPSERT(issueIfAbsent)의
 * "이미 발급된 키는 절대 덮어쓰지 않는다"는 사람 결정을 실 SQL로 검증한다(동시 최초 로그인
 * 레이스에서 키가 2개 발급되지 않는 근거). {@code MemberApiKeyDao}는 삭제 메서드를 두지 않아
 * (계획 — selectByMemberId/issueIfAbsent만) 테스트 정리는 JdbcTemplate으로 직접 한다
 * ({@code CheckoutConcurrencyTest}와 동일 관례).
 *
 * <p>round 9(재작업 지시 2, QA FAIL 필수2) — {@code selectMemberIdByApiKey}가 {@code MEMBERS}
 * 조인 + {@code del_yn='N'}으로 좁혀졌으므로, 이 스위트도 실제 {@code MEMBERS} 행을 만들어야
 * 조회가 성립한다({@link #MEMBER_ID}는 이제 진짜 회원 행을 갖는다 — 없으면 조인이 항상 0행).
 *
 * <p>FUNC-member-006 — {@link #FUNC_006_MEMBER_ID}로 조건부 rotate(issueIfAbsent 확장)와
 * {@code revokeByMemberId} 신규 메서드를 검증한다. 이 두 메서드는 {@code MEMBERS} 조인이 없어
 * (직접 {@code MEMBER_API_KEYS} 조작) 별도 회원 행이 필요 없다 — 006 전용 상수로 기존 005
 * 테스트 행과 겹치지 않게 한다(계획 "테스트 격리" 절).
 */
@SpringBootTest
class MemberApiKeyDaoTest {

    private static final String MEMBER_ID = "M-9001-TEST";
    private static final String WITHDRAWN_MEMBER_ID = "M-9001-WD-TEST"; // member_id VARCHAR(20) 상한
    private static final String FUNC_006_MEMBER_ID = "M-9006-KEY-TEST";

    @Autowired
    private MemberApiKeyDao dao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpMember() {
        memberDao.insertMember(MEMBER_ID, "키테스터", "BRONZE", "member-api-key-dao-test@example.com",
                null, null, "unused-hash", false, LocalDateTime.now());
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM MEMBER_API_KEYS WHERE member_id IN (?, ?, ?)",
                MEMBER_ID, WITHDRAWN_MEMBER_ID, FUNC_006_MEMBER_ID);
        memberDao.deleteById(MEMBER_ID);
        jdbcTemplate.update("DELETE FROM MEMBERS WHERE member_id = ?", WITHDRAWN_MEMBER_ID);
    }

    @Test
    void issueIfAbsent_newMember_storesGivenKey() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);

        dao.issueIfAbsent(MEMBER_ID, "mk_first", now);

        assertThat(dao.selectByMemberId(MEMBER_ID)).isEqualTo("mk_first");
    }

    @Test
    void issueIfAbsent_alreadyIssued_keepsFirstKeyNoOp() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.issueIfAbsent(MEMBER_ID, "mk_first", now);

        dao.issueIfAbsent(MEMBER_ID, "mk_second", now.plusMinutes(1));

        assertThat(dao.selectByMemberId(MEMBER_ID))
                .as("이미 발급된 키가 있으면 no-op — 동시 최초 로그인 레이스에서 키가 2개 발급되지 않는다")
                .isEqualTo("mk_first");
    }

    @Test
    void selectByMemberId_noRow_returnsNull() {
        assertThat(dao.selectByMemberId("no-such-member")).isNull();
    }

    @Test
    void selectMemberIdByApiKey_returnsOwningMember() {
        dao.issueIfAbsent(MEMBER_ID, "mk_lookup", LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        assertThat(dao.selectMemberIdByApiKey("mk_lookup")).isEqualTo(MEMBER_ID);
    }

    @Test
    void selectMemberIdByApiKey_unknownKey_returnsNull() {
        assertThat(dao.selectMemberIdByApiKey("no-such-key")).isNull();
    }

    // linked_tc: TC-FUNC-member-005-15 — round 9(재작업 지시 2) MEMBERS 조인 del_yn 필터.
    @Test
    void selectMemberIdByApiKey_withdrawnMember_returnsNull() {
        // insertMember는 del_yn 기본값('N')으로만 넣으므로(member.xml — 컬럼 기본값 사용), 탈퇴는
        // 이 테스트가 UPDATE로 직접 재현한다(회원탈퇴 API 자체는 이 SR 범위 밖).
        memberDao.insertMember(WITHDRAWN_MEMBER_ID, "탈퇴테스터", "BRONZE",
                "member-api-key-dao-withdrawn-test@example.com", null, null, "unused-hash", false,
                LocalDateTime.now());
        jdbcTemplate.update("UPDATE MEMBERS SET del_yn = 'Y' WHERE member_id = ?", WITHDRAWN_MEMBER_ID);
        dao.issueIfAbsent(WITHDRAWN_MEMBER_ID, "mk_withdrawn", LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS));

        assertThat(dao.selectMemberIdByApiKey("mk_withdrawn"))
                .as("탈퇴 회원(del_yn='Y')의 키는 더 이상 통과시키지 않는다 — 로그인이 delYn='Y'를 401로 막는 것과 동일 규칙")
                .isNull();
    }

    // round 9(재작업 지시 2) — 폐기된(revoked_at NOT NULL) 키도 조회되지 않아야 한다.
    @Test
    void selectMemberIdByApiKey_revoked_returnsNull() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.issueIfAbsent(MEMBER_ID, "mk_revoked", now);
        jdbcTemplate.update("UPDATE MEMBER_API_KEYS SET revoked_at = ? WHERE api_key = ?", now, "mk_revoked");

        assertThat(dao.selectMemberIdByApiKey("mk_revoked")).isNull();
    }

    // ===== FUNC-member-006 — revokeByMemberId 신규 =====

    // linked_tc: TC-FUNC-member-006-10 — 로그아웃(revokeByMemberId)은 DELETE가 아니라
    // revoked_at 세팅이다(행은 남는다, selectByMemberId는 revoked_at을 필터하지 않아 여전히
    // 반환한다 — 필터는 selectMemberIdByApiKey 쪽 책임).
    @Test
    void revokeByMemberId_activeKey_setsRevokedAtButRowRemains() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.issueIfAbsent(FUNC_006_MEMBER_ID, "mk_006_before_revoke", now);

        int updated = dao.revokeByMemberId(FUNC_006_MEMBER_ID, now.plusMinutes(1));

        assertThat(updated).isEqualTo(1);
        assertThat(dao.selectByMemberId(FUNC_006_MEMBER_ID))
                .as("revokeByMemberId는 DELETE가 아니다 — 행은 남는다")
                .isEqualTo("mk_006_before_revoke");
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT revoked_at FROM MEMBER_API_KEYS WHERE member_id = ?", FUNC_006_MEMBER_ID);
        assertThat(row.get("revoked_at")).isNotNull();
    }

    // linked_tc: TC-FUNC-member-006-11 — 이미 폐기된 키에 다시 호출해도 안전(idempotent, 0행
    // 영향) — 중복 로그아웃 호출 시나리오.
    @Test
    void revokeByMemberId_calledTwice_isIdempotentSecondCallAffectsNoRows() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.issueIfAbsent(FUNC_006_MEMBER_ID, "mk_006_double_revoke", now);
        dao.revokeByMemberId(FUNC_006_MEMBER_ID, now.plusMinutes(1));

        int secondCallUpdated = dao.revokeByMemberId(FUNC_006_MEMBER_ID, now.plusMinutes(2));

        assertThat(secondCallUpdated).as("이미 폐기된 행은 WHERE revoked_at IS NULL에 매치되지 않는다")
                .isEqualTo(0);
    }

    // linked_tc: TC-FUNC-member-006-12 — 핵심 rotate 시맨틱: 폐기된 뒤 issueIfAbsent를 다시
    // 호출하면(refresh 성공 경로) 새 후보 키로 교체되고 revoked_at이 NULL로 되돌아간다. 폐기된
    // 키 문자열 자체는 재사용되지 않는다(계획 "005 인터페이스 경계표" 근거).
    //
    // QA round1 재작업 지시 3(사람 확정) — 이 테스트는 memberApiKey.xml의 issueIfAbsent
    // ON DUPLICATE KEY UPDATE 세 대입(api_key/issued_at/revoked_at) 순서 의존성을 실 SQL로
    // 잡는 회귀 테스트이기도 하다. revoked_at 대입이 마지막이 아니게(예: 맨 앞으로) 바뀌면
    // api_key/issued_at의 IF(revoked_at IS NOT NULL, ...)가 이미 갱신된 NULL을 보게 되어 rotate가
    // 반쪽만 일어난다 — 이 테스트의 "새 후보로 교체된다" 단언이 그 상태에서 실패한다(mapper 주석
    // 참고).
    @Test
    void issueIfAbsent_afterRevoke_rotatesToNewCandidateAndClearsRevokedAt() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.issueIfAbsent(FUNC_006_MEMBER_ID, "mk_006_original", now);
        dao.revokeByMemberId(FUNC_006_MEMBER_ID, now.plusMinutes(1));

        dao.issueIfAbsent(FUNC_006_MEMBER_ID, "mk_006_rotated", now.plusMinutes(2));

        assertThat(dao.selectByMemberId(FUNC_006_MEMBER_ID))
                .as("폐기된 뒤에는 새 후보로 교체된다(no-op이 아니다)")
                .isEqualTo("mk_006_rotated");
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT revoked_at FROM MEMBER_API_KEYS WHERE member_id = ?", FUNC_006_MEMBER_ID);
        assertThat(row.get("revoked_at")).as("rotate 후 revoked_at은 다시 NULL이어야 한다").isNull();
    }

    // linked_tc: TC-FUNC-member-006-13 — 회귀: 폐기되지 않은(활성) 키는 여전히 완전 no-op이다
    // (005의 기존 계약 그대로 — 조건부 rotate 확장이 활성 키 경로를 건드리지 않았는지 확인).
    @Test
    void issueIfAbsent_activeKeyNotRevoked_stillCompleteNoOp() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS);
        dao.issueIfAbsent(FUNC_006_MEMBER_ID, "mk_006_active_first", now);

        dao.issueIfAbsent(FUNC_006_MEMBER_ID, "mk_006_should_not_apply", now.plusMinutes(1));

        assertThat(dao.selectByMemberId(FUNC_006_MEMBER_ID)).isEqualTo("mk_006_active_first");
    }
}
