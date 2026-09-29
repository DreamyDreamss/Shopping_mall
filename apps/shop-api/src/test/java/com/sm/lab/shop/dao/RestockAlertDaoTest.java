package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.RestockAlertView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.RESTOCK_ALERTS) 대상 통합 테스트(SR-364 #1/#2) — 매퍼 SQL 자체(INSERT
 * IGNORE 원자성·FK 위반 흡수, {@code selectAlertList} 정렬·회원별 격리)를 검증한다. Mockito로는 실
 * SQL 정합을 증명할 수 없다.
 *
 * <p>랩 데이터: M-0001 김실증(SKU-1001 스탠딩 데스크, SKU-1002 기계식 키보드), M-0002(다른 회원
 * 검증 전용, SKU-1003) — {@code ProductWishDaoTest} 관례와 동일하게 기존 시드를 재사용하고 신규
 * 회원·상품을 만들지 않는다.
 *
 * <p>취소(DELETE) API는 범위 밖(STORY "범위 밖" 절). 존재 확인·정리는 {@code
 * ProductViewHistoryDaoTest}와 동일 관례로 {@link JdbcTemplate}을 직접 사용한다.
 *
 * <p>테스트 격리(STORY "테스트 격리" 절) — list 테스트는 같은 회원(M-0001)의 sku 2건 이상을 쓰므로
 * 정리 조건을 {@code member_id} 전체로 넓힌다(파일 내 기존 register 테스트들도 같은 MEMBER_ID를
 * 쓰지만 전부 SKU-1001 하나뿐이라 범위 확대로 충돌 없음 — 사례집 SR-356 #6 "정리 범위를 앞뒤로
 * 두 번+동일 범위로" 관례). "다른 회원 데이터와 안 섞임" 테스트가 쓰는 타 회원 행(OTHER_MEMBER_ID,
 * 고유 sku)도 같은 {@code @BeforeEach}/{@code @AfterEach}에서 함께 정리한다.
 */
@SpringBootTest
class RestockAlertDaoTest {

    private static final String MEMBER_ID = "M-0001";
    private static final String SKU = "SKU-1001";
    private static final String SKU_2 = "SKU-1002";
    // SR-364(#2) — selectAlertList_onlyReturnsOwnMemberRows 전용 두 번째 회원(기존 시드), 고유 sku.
    private static final String OTHER_MEMBER_ID = "M-0002";
    private static final String OTHER_MEMBER_SKU = "SKU-1003";
    private static final String NON_EXISTENT_MEMBER_ID = "M-9999-DOES-NOT-EXIST";

    @Autowired
    private RestockAlertDao dao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanUpBeforeEach() {
        cleanUpRows();
    }

    @AfterEach
    void cleanUp() {
        cleanUpRows();
    }

    private void cleanUpRows() {
        jdbcTemplate.update("DELETE FROM RESTOCK_ALERTS WHERE member_id = ?", MEMBER_ID);
        jdbcTemplate.update("DELETE FROM RESTOCK_ALERTS WHERE member_id = ? AND sku = ?",
                OTHER_MEMBER_ID, OTHER_MEMBER_SKU);
    }

    private int countRow(String memberId, String sku) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM RESTOCK_ALERTS WHERE member_id = ? AND sku = ?",
                Integer.class, memberId, sku);
        return count == null ? 0 : count;
    }

    // linked_tc: 최초 INSERT — 영향행수 1, 행 존재를 재조회로 확인(절대값 대신 존재 재조회 —
    // 사례집 SR-297 #1 교훈).
    @Test
    void insertAlert_firstCall_affectsOneRowAndRowExists() {
        int affected = dao.insertAlert(MEMBER_ID, SKU, LocalDateTime.now());

        assertThat(affected).isEqualTo(1);
        assertThat(countRow(MEMBER_ID, SKU)).isEqualTo(1);
    }

    // linked_tc: 같은 (member_id, sku) 재삽입 — 영향행수 0(예외 없음, IGNORE 확인).
    @Test
    void insertAlert_duplicateCall_noExceptionAndAffectsZeroRowsOnSecondCall() {
        int first = dao.insertAlert(MEMBER_ID, SKU, LocalDateTime.now());
        int second = dao.insertAlert(MEMBER_ID, SKU, LocalDateTime.now());

        assertThat(first).isEqualTo(1);
        assertThat(second).as("PK 충돌 — IGNORE가 흡수해 0행, 예외 없음").isEqualTo(0);
        assertThat(countRow(MEMBER_ID, SKU)).as("중복 호출도 행은 하나만").isEqualTo(1);
    }

    // linked_tc: 존재하지 않는 member_id로 삽입 시도 — FK 위반이 IGNORE로 흡수돼 0행(예외로 새지
    // 않음을 확인) — 서비스 레벨 재조회 분기의 전제가 되는 DB 동작 자체를 실측한다.
    @Test
    void insertAlert_nonExistentMemberId_fkViolationAbsorbedByIgnoreAffectsZeroRows() {
        int affected = dao.insertAlert(NON_EXISTENT_MEMBER_ID, SKU, LocalDateTime.now());

        assertThat(affected).as("FK 위반이 예외로 새지 않고 IGNORE가 조용히 0행으로 흡수").isEqualTo(0);
        assertThat(countRow(NON_EXISTENT_MEMBER_ID, SKU)).isEqualTo(0);
    }

    // linked_tc: SR-364(#2) — 같은 회원, 서로 다른 sku 2건을 created_at을 다르게 지정해 삽입 후
    // items.get(0)이 더 최근 것인지 구체 단언(created_at DESC).
    @Test
    void selectAlertList_returnsRowsOrderedByCreatedAtDesc() {
        LocalDateTime older = LocalDateTime.now().minusDays(1);
        LocalDateTime newer = LocalDateTime.now();
        dao.insertAlert(MEMBER_ID, SKU, older);
        dao.insertAlert(MEMBER_ID, SKU_2, newer);

        List<RestockAlertView> items = dao.selectAlertList(MEMBER_ID);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).getSku()).as("더 최근 신청이 먼저").isEqualTo(SKU_2);
        assertThat(items.get(1).getSku()).isEqualTo(SKU);
    }

    // linked_tc: SR-353(#1) 판별력 원칙 — 다른 회원(OTHER_MEMBER_ID)의 행을 먼저 심어 두고, 그
    // 회원의 목록에 섞이지 않는지 확인한다. 대상 회원 데이터가 아예 없는 상태에서 빈 리스트만 보는
    // 테스트는 WHERE member_id 필터가 실제로 걸러내는지 증명하지 못한다.
    @Test
    void selectAlertList_onlyReturnsOwnMemberRows() {
        dao.insertAlert(OTHER_MEMBER_ID, OTHER_MEMBER_SKU, LocalDateTime.now());
        dao.insertAlert(MEMBER_ID, SKU, LocalDateTime.now());

        List<RestockAlertView> items = dao.selectAlertList(MEMBER_ID);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).getSku()).isEqualTo(SKU);
    }
}
