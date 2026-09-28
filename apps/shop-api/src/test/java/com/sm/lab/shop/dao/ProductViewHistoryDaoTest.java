package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.ProductViewHistoryView;
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
 * 실 DB(MariaDB, PRODUCT_VIEW_HISTORY) 대상 통합 테스트 — 매퍼 SQL 자체(UPSERT 원자성·정렬·상품
 * 조인·상한 삭제)를 검증한다. Mockito로는 실 SQL 정합을 증명할 수 없다.
 *
 * <p>랩 데이터: M-0001 김실증, SKU-1001/SKU-1002/SKU-1003(기존 시드, 전부 판매중) — {@code
 * ProductWishDaoTest} 관례와 동일하게 기존 시드를 재사용하고 신규 회원·상품을 만들지 않는다.
 *
 * <p>이 DAO에는 삭제 메서드가 없다(운영 경로에 불필요 — 상한 삭제는 {@link
 * ProductViewHistoryDao#enforceRecentCap}이 전담). 정리는 {@code MemberRefreshTokenDaoTest}와
 * 동일 관례로 {@link JdbcTemplate}을 직접 사용한다.
 *
 * <p><b>정리 범위 — round2(QA CONCERNS 재작업 지시 1)</b>: {@code enforceRecentCap}의 실제 SQL
 * blast radius는 SKU 3개 한정이 아니라 <b>M-0001의 {@code PRODUCT_VIEW_HISTORY} 행 전체</b>(상위
 * limit개만 남기고 나머지 전부 DELETE)다. 정리를 {@code sku IN (SKU-1001, SKU-1002, SKU-1003)}
 * 3건으로 좁혀두면(이전 버전), 이 테이블에 M-0001의 다른 SKU 행이 하나라도 섞이는 순간
 * {@code @BeforeEach}/{@code @AfterEach}가 그 행을 조용히 못 지우고(범위 밖), 반대로
 * {@code enforceRecentCap} 호출은 그 행까지 삭제 대상으로 보므로 "정리 범위"와 "실제 삭제 범위"가
 * 어긋난다(사례집 SR-356 #6·SR-297 #1과 동일 계열). 그래서 이 테스트는 M-0001 전체를 정리 범위로
 * 삼는다({@code WHERE member_id = 'M-0001'}) — enforceRecentCap의 blast radius와 정리 범위를
 * 일치시킨다. 이 테스트 클래스는 M-0001을 전용으로 쓰고 다른 클래스는 이 회원의
 * {@code PRODUCT_VIEW_HISTORY} 행을 만들지 않는다(다른 시드·동시 SR 행은 이 테이블 자체를
 * 건드리지 않으므로 여전히 안전).
 */
@SpringBootTest
class ProductViewHistoryDaoTest {

    private static final String MEMBER_ID = "M-0001";
    private static final String SKU_1 = "SKU-1001";
    private static final String SKU_2 = "SKU-1002";
    private static final String SKU_3 = "SKU-1003";

    @Autowired
    private ProductViewHistoryDao dao;
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
        // round2(재작업 지시 1) — enforceRecentCap의 blast radius(M-0001 전체)와 일치시킨다.
        // sku IN (...) 3건 한정이던 이전 범위는 enforceRecentCap이 그 밖의 M-0001 행까지 지우는
        // 실제 동작과 어긋났다(위 클래스 Javadoc 참고).
        jdbcTemplate.update("DELETE FROM PRODUCT_VIEW_HISTORY WHERE member_id = ?", MEMBER_ID);
    }

    // linked_tc: 기록 — 같은 회원·sku 재기록은 행 추가가 아니라 viewed_at 갱신이고, 목록 맨 앞으로 온다.
    @Test
    void recordView_duplicateCall_updatesViewedAtInPlaceWithoutAddingRow() {
        LocalDateTime t1 = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.recordView(MEMBER_ID, SKU_1, t1);
        dao.recordView(MEMBER_ID, SKU_2, t1.plusSeconds(1));
        LocalDateTime t3 = t1.plusSeconds(2);
        dao.recordView(MEMBER_ID, SKU_1, t3);

        Integer sku1RowCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_VIEW_HISTORY WHERE member_id = ? AND sku = ?",
                Integer.class, MEMBER_ID, SKU_1);
        assertThat(sku1RowCount).as("재기록은 행 추가가 아니라 viewed_at 갱신").isEqualTo(1);

        List<String> order = dao.selectRecentlyViewed(MEMBER_ID, 1000).stream()
                .filter(v -> v.getSku().equals(SKU_1) || v.getSku().equals(SKU_2))
                .map(ProductViewHistoryView::getSku)
                .toList();
        assertThat(order).as("재기록된 SKU-1001이 최신 viewed_at으로 맨 앞으로").containsExactly(SKU_1, SKU_2);
    }

    // linked_tc: 목록 — viewed_at DESC 정렬 + 상품 조인 필드(productName/price/soldOut/viewedAt).
    // 다른 시드·동시 SR이 M-0001에 기록을 추가해도 이 항목이 넣은 두 SKU만 필터링해 비교한다.
    @Test
    void selectRecentlyViewed_ordersByViewedAtDescAndJoinsProductFields() {
        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.recordView(MEMBER_ID, SKU_1, base.minusMinutes(1));
        dao.recordView(MEMBER_ID, SKU_2, base);

        List<ProductViewHistoryView> mine = dao.selectRecentlyViewed(MEMBER_ID, 1000).stream()
                .filter(v -> v.getSku().equals(SKU_1) || v.getSku().equals(SKU_2))
                .toList();

        assertThat(mine).extracting(ProductViewHistoryView::getSku)
                .as("viewed_at DESC — 가장 최근에 본 상품이 먼저(이 테스트가 넣은 두 SKU만 비교)")
                .containsExactly(SKU_2, SKU_1);

        ProductViewHistoryView latest = mine.get(0);
        assertThat(latest.getProductName()).isEqualTo("기계식 키보드");
        assertThat(latest.getPrice()).isEqualTo(129000L);
        assertThat(latest.isSoldOut()).isFalse();
        assertThat(latest.getViewedAt()).isNotNull();
    }

    // linked_tc: 동일 초(viewed_at tie) 시 sku ASC로 목록 순서가 결정적인지.
    @Test
    void selectRecentlyViewed_sameViewedAt_tieBreaksBySkuAsc() {
        LocalDateTime tie = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.recordView(MEMBER_ID, SKU_1, tie);
        dao.recordView(MEMBER_ID, SKU_2, tie);

        List<String> mine = dao.selectRecentlyViewed(MEMBER_ID, 1000).stream()
                .filter(v -> v.getSku().equals(SKU_1) || v.getSku().equals(SKU_2))
                .map(ProductViewHistoryView::getSku)
                .toList();

        assertThat(mine).as("동일 viewed_at — sku ASC tie-break로 SKU-1001이 먼저")
                .containsExactly(SKU_1, SKU_2);
    }

    // linked_tc: 상한 초과분(viewed_at 오래된 순)만 삭제한다. 실 시드 상품이 20/21개가 안 돼
    // MemberRefreshTokenDaoTest.enforceActiveCap_...와 동일하게 작은 limit(2)으로 로직 자체를
    // 검증한다(실제 서비스 상수 20은 ProductViewHistoryService에 그대로 둔다).
    @Test
    void enforceRecentCap_keepsOnlyMostRecentByViewedAt() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.recordView(MEMBER_ID, SKU_1, now.minusMinutes(2));
        dao.recordView(MEMBER_ID, SKU_2, now.minusMinutes(1));
        dao.recordView(MEMBER_ID, SKU_3, now);

        dao.enforceRecentCap(MEMBER_ID, 2);

        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_VIEW_HISTORY WHERE member_id = ? AND sku IN (?, ?, ?)",
                Integer.class, MEMBER_ID, SKU_1, SKU_2, SKU_3);
        assertThat(remaining).as("가장 오래된 SKU-1001 1개만 지워지고 최근 2개(SKU-1002/1003)는 남는다")
                .isEqualTo(2);
        Integer oldestGone = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_VIEW_HISTORY WHERE member_id = ? AND sku = ?",
                Integer.class, MEMBER_ID, SKU_1);
        assertThat(oldestGone).isEqualTo(0);
        Integer newestKept = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_VIEW_HISTORY WHERE member_id = ? AND sku = ?",
                Integer.class, MEMBER_ID, SKU_3);
        assertThat(newestKept).isEqualTo(1);
    }

    // linked_tc: 동일 초(viewed_at tie) 시 상한 삭제 판정도 sku ASC로 결정적인지 — 목록 정렬과
    // 동일한 tie-breaker를 상한 삭제 SQL이 공유하는지 확인한다.
    @Test
    void enforceRecentCap_sameViewedAt_tieBreaksBySkuAscKeepsSmallestSkus() {
        LocalDateTime tie = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.recordView(MEMBER_ID, SKU_1, tie);
        dao.recordView(MEMBER_ID, SKU_2, tie);
        dao.recordView(MEMBER_ID, SKU_3, tie);

        dao.enforceRecentCap(MEMBER_ID, 2);

        Integer sku1Kept = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_VIEW_HISTORY WHERE member_id = ? AND sku = ?",
                Integer.class, MEMBER_ID, SKU_1);
        Integer sku2Kept = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_VIEW_HISTORY WHERE member_id = ? AND sku = ?",
                Integer.class, MEMBER_ID, SKU_2);
        Integer sku3Gone = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_VIEW_HISTORY WHERE member_id = ? AND sku = ?",
                Integer.class, MEMBER_ID, SKU_3);
        assertThat(sku1Kept).as("동일 viewed_at — sku ASC tie-break로 가장 작은 SKU-1001 유지").isEqualTo(1);
        assertThat(sku2Kept).as("동일 viewed_at — sku ASC tie-break로 두 번째로 작은 SKU-1002 유지").isEqualTo(1);
        assertThat(sku3Gone).as("동일 viewed_at — sku ASC tie-break로 가장 큰 SKU-1003 삭제").isEqualTo(0);
    }
}
