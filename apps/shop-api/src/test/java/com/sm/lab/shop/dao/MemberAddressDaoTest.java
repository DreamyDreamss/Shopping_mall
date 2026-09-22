// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.MEMBER_ADDRESSES) 대상 통합 테스트 — 매퍼 SQL 자체(정렬·제외·존재+소유
 * 판정)를 검증한다(STORY "테스트" 절). Mockito로는 실 SQL의 {@code ORDER BY}·{@code != }·조인
 * 정합을 증명할 수 없다.
 *
 * <p>테스트 격리(사례집 SR-232 r2와 동일 계열 — 개수 제약이 있는 테이블) — 이 클래스 전용
 * member_id({@link #MEMBER_ID}/{@link #OTHER_MEMBER_ID})를 쓰고, {@code @AfterEach}에서
 * MEMBER_ADDRESSES 행과 회원 행을 모두 정리해 "회원당 최대 10개" 카운트가 다음 실행으로
 * 새지 않게 한다.
 */
@SpringBootTest
class MemberAddressDaoTest {

    private static final String MEMBER_ID = "M-9011-ADDR-TEST"; // member_id VARCHAR(20) 상한
    private static final String OTHER_MEMBER_ID = "M-9011-ADDR-OTH"; // member_id VARCHAR(20) 상한

    @Autowired
    private MemberAddressDao dao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpMembers() {
        LocalDateTime now = LocalDateTime.now();
        memberDao.insertMember(MEMBER_ID, "배송지DAO테스터", "BRONZE",
                "member-address-dao-test@example.com", null, null, "unused-hash", false, now);
        memberDao.insertMember(OTHER_MEMBER_ID, "배송지DAO테스터2", "BRONZE",
                "member-address-dao-test-other@example.com", null, null, "unused-hash", false, now);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM MEMBER_ADDRESSES WHERE member_id IN (?, ?)", MEMBER_ID, OTHER_MEMBER_ID);
        memberDao.deleteById(MEMBER_ID);
        memberDao.deleteById(OTHER_MEMBER_ID);
    }

    // linked_tc: TC-FUNC-member-011-D01 — 정렬(last_used_at DESC, created_at DESC).
    @Test
    void selectList_ordersByLastUsedThenCreatedDesc() {
        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Long older = insertAddress(MEMBER_ID, base.minusDays(2), base.minusDays(2));
        Long newer = insertAddress(MEMBER_ID, base.minusDays(1), base.minusDays(1));

        List<MemberAddress> items = dao.selectList(MEMBER_ID);

        assertThat(items).extracting(MemberAddress::getAddressId)
                .as("last_used_at 내림차순 — 최근 사용이 먼저")
                .containsExactly(newer, older);
    }

    // linked_tc: TC-FUNC-member-011-D02 — 삭제 대상(excludeAddressId)을 실제로 제외.
    @Test
    void selectNextDefaultCandidate_excludesGivenAddress() {
        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Long candidate = insertAddress(MEMBER_ID, base.minusDays(1), base.minusDays(1));
        Long excluded = insertAddress(MEMBER_ID, base, base); // 더 최근이지만 제외 대상

        MemberAddress next = dao.selectNextDefaultCandidate(MEMBER_ID, excluded);

        assertThat(next).isNotNull();
        assertThat(next.getAddressId()).isEqualTo(candidate);
    }

    // linked_tc: TC-FUNC-member-011-D03 — 존재+소유 단일 SQL: 타 회원 소유 행은 null.
    @Test
    void selectOwned_otherMembersAddress_returnsNull() {
        Long addressId = insertAddress(OTHER_MEMBER_ID, LocalDateTime.now(), LocalDateTime.now());

        assertThat(dao.selectOwned(MEMBER_ID, addressId))
                .as("남의 배송지는 null이어야 한다(존재+소유 단일 SQL, 사례집 SR-231 r5)")
                .isNull();
    }

    // linked_tc: TC-FUNC-member-011-D04 — 재작업 지시(2, QA CONCERNS 권고#2): del_yn='N' 상시필터가
    // selectList·selectOwned·selectByMemberIdForUpdate 셋 다에 실제로 적용되는지 실 DB로 확인한다.
    // 서비스·컨트롤러 테스트는 DAO가 목업이라 softDelete 이후 세 조회 경로 어디에서도 이 필터를
    // 검증하지 못한다 — 이 테스트가 그 공백을 메운다.
    @Test
    void softDeletedRow_isExcludedFromSelectList_selectOwned_andSelectByMemberIdForUpdate() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        Long deletedId = insertAddress(MEMBER_ID, now, now);
        Long liveId = insertAddress(MEMBER_ID, now.minusDays(1), now.minusDays(1));

        jdbcTemplate.update("UPDATE MEMBER_ADDRESSES SET del_yn = 'Y' WHERE address_id = ?", deletedId);

        assertThat(dao.selectList(MEMBER_ID))
                .as("소프트 삭제된 행은 목록(selectList)에서 빠져야 한다")
                .extracting(MemberAddress::getAddressId)
                .containsExactly(liveId)
                .doesNotContain(deletedId);

        assertThat(dao.selectOwned(MEMBER_ID, deletedId))
                .as("소프트 삭제된 행은 selectOwned에서 null이어야 한다")
                .isNull();
        assertThat(dao.selectOwned(MEMBER_ID, liveId))
                .as("삭제되지 않은 행은 selectOwned로 여전히 조회돼야 한다")
                .isNotNull();

        assertThat(dao.selectByMemberIdForUpdate(MEMBER_ID))
                .as("소프트 삭제된 행은 락 조회(selectByMemberIdForUpdate)에서도 빠져야 한다")
                .extracting(MemberAddress::getAddressId)
                .containsExactly(liveId)
                .doesNotContain(deletedId);
    }

    private Long insertAddress(String memberId, LocalDateTime lastUsedAt, LocalDateTime createdAt) {
        MemberAddress address = new MemberAddress();
        address.setMemberId(memberId);
        address.setRecipient("테스트수령인");
        address.setPhone("01099998888");
        address.setPhoneNorm("01099998888");
        address.setZipcode("12345");
        address.setRoadAddress("서울시 테스트로 1");
        address.setDetailAddress("1동 1호");
        address.setIsDefault("N");
        address.setLastUsedAt(lastUsedAt);
        address.setCreatedAt(createdAt);
        address.setUpdatedAt(createdAt);
        dao.insertAddress(address);
        return address.getAddressId();
    }
}
