package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.Order;
import com.sm.lab.shop.domain.OrderItem;
import com.sm.lab.shop.domain.ProductReview;
import com.sm.lab.shop.domain.ProductReviewSummary;
import com.sm.lab.shop.domain.ProductReviewView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 실 DB(MariaDB, sl_lab.PRODUCT_REVIEWS) 대상 통합 테스트 — 매퍼 SQL 자체(구매 자격 판정·
 * active_key 유일성·조건부 UPDATE)를 검증한다(SR-351(#1) STORY "테스트" 절). Mockito로는 실
 * {@code active_key} 생성열의 유일성 재판정(삭제 후 재작성 허용)을 증명할 수 없다.
 *
 * <p>테스트 격리(사례집 SR-232 r2와 동일 계열) — 이 클래스 전용 member_id/order_no를 쓰고,
 * {@code @AfterEach}에서 PRODUCT_REVIEWS/ORDER_ITEMS/ORDERS/MEMBERS 행을 모두 정리한다.
 */
@SpringBootTest
class ProductReviewDaoTest {

    private static final String MEMBER_ID = "M-9011-RVW-DAO-TST"; // member_id VARCHAR(20) 상한
    private static final String OTHER_MEMBER_ID = "M-9011-RVW-DAO-OTH";
    private static final String ORDER_NO = "20260902-RVWDAOTST"; // order_no VARCHAR(20) 상한
    private static final String CANCELED_ORDER_NO = "20260902-RVWDAOCXL";
    private static final String OTHER_ORDER_NO = "20260902-RVWDAOOTH";
    private static final String SKU = "SKU-1001";
    private static final String OTHER_SKU = "SKU-1002";

    // 목록·집계는 전용 상품까지 생성한다. 시드 SKU를 공유하면 실제 리뷰가 정렬·페이지
    // 경계에 섞여 테스트가 실패한다. 기존 리뷰는 지우지 않고 테스트 소유 행만 정리한다.
    private static final String LIST_MEMBER_ID = "M-9012-RVWLISTTST";
    private static final String LIST_SKU = "RVW-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    private boolean listProductCreated;
    private static final String LIST_ORDER_PREFIX = "20260903-RVWLIST";

    @Autowired
    private ProductReviewDao dao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private OrderDao orderDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        memberDao.insertMember(MEMBER_ID, "상품평DAO테스터", "BRONZE",
                "product-review-dao-test@example.com", null, null, "unused-hash", false, now);
        memberDao.insertMember(OTHER_MEMBER_ID, "상품평DAO테스터2", "BRONZE",
                "product-review-dao-test-other@example.com", null, null, "unused-hash", false, now);

        insertOrder(ORDER_NO, MEMBER_ID, "DONE");
        insertOrderItem(ORDER_NO, SKU);
        insertOrder(CANCELED_ORDER_NO, MEMBER_ID, "CANCELED");
        insertOrderItem(CANCELED_ORDER_NO, SKU);
        insertOrder(OTHER_ORDER_NO, OTHER_MEMBER_ID, "DONE");
        insertOrderItem(OTHER_ORDER_NO, SKU);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM PRODUCT_REVIEWS WHERE order_no IN (?, ?, ?)",
                ORDER_NO, CANCELED_ORDER_NO, OTHER_ORDER_NO);
        jdbcTemplate.update("DELETE FROM ORDER_ITEMS WHERE order_no IN (?, ?, ?)",
                ORDER_NO, CANCELED_ORDER_NO, OTHER_ORDER_NO);
        jdbcTemplate.update("DELETE FROM ORDERS WHERE order_no IN (?, ?, ?)",
                ORDER_NO, CANCELED_ORDER_NO, OTHER_ORDER_NO);
        memberDao.deleteById(MEMBER_ID);
        memberDao.deleteById(OTHER_MEMBER_ID);
    }

    // SR-351(#2) — 위 cleanUp()과 별개(JUnit5는 @AfterEach 다건을 전부 실행)의 목록·집계 테스트
    // 전용 정리. sku·order 묶음(LIST_SKU/LIST_ORDER_PREFIX) 기준으로 이 스위트가 새로 만든 행만
    // 지운다 — 아무것도 안 만든 테스트에서는 no-op.
    //
    // round 1 QA CONCERNS(재작업 지시 1) — 예전 구현은 "DELETE FROM PRODUCT_REVIEWS WHERE sku = ?"
    // (LIST_SKU=시드 SKU-1001)로 **이 스위트 밖에서 만든 리뷰까지 전역 삭제**했다(주석은 "새로 만든
    // 행만"이라 적었으나 실제로는 그렇지 않았다) — 시드 SKU를 공유하는 다른 리뷰(수동 확인용 데이터,
    // #3 화면 작업 데모 데이터 등)가 있으면 매 실행마다 소리 없이 파괴됐다(QA 재실행 후 0행 실측,
    // 사례집 SR-297 #1·SR-232 r2와 동일 계열). PRODUCT_REVIEWS.order_no로 이 스위트가 실제로 만든
    // 행(LIST_ORDER_PREFIX 접두)만 좁혀 지운다 — sku 컬럼으로는 절대 삭제하지 않는다.
    @AfterEach
    void cleanUpListFixtures() {
        jdbcTemplate.update("DELETE FROM PRODUCT_REVIEWS WHERE order_no LIKE ?", LIST_ORDER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM ORDER_ITEMS WHERE order_no LIKE ?", LIST_ORDER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM ORDERS WHERE order_no LIKE ?", LIST_ORDER_PREFIX + "%");
        jdbcTemplate.update("DELETE FROM MEMBERS WHERE member_id = ?", LIST_MEMBER_ID);
        if (listProductCreated) {
            jdbcTemplate.update("DELETE FROM PRODUCTS WHERE sku = ?", LIST_SKU);
        }
    }

    @Test
    void existsOrder_knownOrder_returnsOne() {
        assertThat(dao.existsOrder(ORDER_NO)).isEqualTo(1);
    }

    @Test
    void existsOrder_unknownOrder_returnsZero() {
        assertThat(dao.existsOrder("20260902-NOPE")).isEqualTo(0);
    }

    @Test
    void countEligiblePurchase_ownedNotCanceledWithSku_returnsOne() {
        assertThat(dao.countEligiblePurchase(ORDER_NO, MEMBER_ID, SKU)).isEqualTo(1);
    }

    @Test
    void countEligiblePurchase_foreignOrder_returnsZero() {
        assertThat(dao.countEligiblePurchase(OTHER_ORDER_NO, MEMBER_ID, SKU)).isEqualTo(0);
    }

    @Test
    void countEligiblePurchase_canceledOrder_returnsZero() {
        assertThat(dao.countEligiblePurchase(CANCELED_ORDER_NO, MEMBER_ID, SKU)).isEqualTo(0);
    }

    @Test
    void countEligiblePurchase_skuNotInOrder_returnsZero() {
        assertThat(dao.countEligiblePurchase(ORDER_NO, MEMBER_ID, OTHER_SKU)).isEqualTo(0);
    }

    @Test
    void insertReview_duplicateActiveSameOrderAndSku_throwsDuplicateKeyException() {
        insertReview(ORDER_NO, SKU, MEMBER_ID, 5);

        assertThatThrownBy(() -> insertReview(ORDER_NO, SKU, MEMBER_ID, 4))
                .isInstanceOf(DuplicateKeyException.class);
    }

    @Test
    void softDelete_thenReinsertSameOrderAndSku_succeeds() {
        Long reviewId = insertReview(ORDER_NO, SKU, MEMBER_ID, 5);

        int updated = dao.softDelete(reviewId, SKU, MEMBER_ID, LocalDateTime.now());
        assertThat(updated).isEqualTo(1);

        // active_key가 NULL로 재계산돼 유니크 인덱스 밖 — 재작성이 예외 없이 성공해야 한다.
        Long secondReviewId = insertReview(ORDER_NO, SKU, MEMBER_ID, 3);
        assertThat(secondReviewId).isNotNull().isNotEqualTo(reviewId);
    }

    @Test
    void softDelete_wrongMember_zeroRowsAffected() {
        Long reviewId = insertReview(ORDER_NO, SKU, MEMBER_ID, 5);

        int updated = dao.softDelete(reviewId, SKU, OTHER_MEMBER_ID, LocalDateTime.now());

        assertThat(updated).isEqualTo(0);
    }

    @Test
    void softDelete_alreadyDeleted_zeroRowsAffected() {
        Long reviewId = insertReview(ORDER_NO, SKU, MEMBER_ID, 5);
        dao.softDelete(reviewId, SKU, MEMBER_ID, LocalDateTime.now());

        int updated = dao.softDelete(reviewId, SKU, MEMBER_ID, LocalDateTime.now());

        assertThat(updated).isEqualTo(0);
    }

    @Test
    void selectById_returnsFullRowIncludingDelYn() {
        Long reviewId = insertReview(ORDER_NO, SKU, MEMBER_ID, 5);

        ProductReview found = dao.selectById(reviewId);

        assertThat(found).isNotNull();
        assertThat(found.getSku()).isEqualTo(SKU);
        assertThat(found.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(found.getDelYn()).isEqualTo("N");
    }

    @Test
    void selectById_unknownReviewId_returnsNull() {
        assertThat(dao.selectById(-1L)).isNull();
    }

    // 반복 삭제-재작성 시나리오(사람 수정 확정) — 매퍼 호출만으로(INSERT/조건부 UPDATE/재조회)
    // 작성→삭제→재작성→재삭제→재작성 5단계를 직접 반복 실행해, active_key 생성열이 매 사이클
    // 독립적으로 유일성을 재판정하는지(2회차 이후에도 깨지지 않는지) SQL 레벨에서 확인한다.
    @Test
    void repeatedInsertSoftDeleteCycle_fiveSteps_activeKeyUniquenessReleasedEveryTime() {
        Long first = insertReview(ORDER_NO, SKU, MEMBER_ID, 5);
        assertThat(dao.softDelete(first, SKU, MEMBER_ID, LocalDateTime.now())).isEqualTo(1);

        Long second = insertReview(ORDER_NO, SKU, MEMBER_ID, 4);
        assertThat(second).isNotEqualTo(first);
        assertThat(dao.softDelete(second, SKU, MEMBER_ID, LocalDateTime.now())).isEqualTo(1);

        Long third = insertReview(ORDER_NO, SKU, MEMBER_ID, 3);
        assertThat(third).isNotEqualTo(second);

        // 마지막 사이클은 삭제하지 않고 남겨 둔다 — 활성 리뷰가 정확히 1건임을 확인.
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM PRODUCT_REVIEWS WHERE order_no = ? AND sku = ? AND del_yn = 'N'",
                Integer.class, ORDER_NO, SKU);
        assertThat(activeCount).isEqualTo(1);
    }

    // ==================== SR-351(#2) — selectRatingSummary / selectReviews ====================

    @Test
    void selectRatingSummary_noReviews_returnsNullAvgZeroCountAndZeroDistribution() {
        // 아무것도 시딩하지 않은 새 SKU — COALESCE 가드 확인(SUM(CASE...)의 공허집합 NULL 함정).
        ProductReviewSummary summary = dao.selectRatingSummary("SKU-9012-RVWLIST-EMPTY");

        assertThat(summary).isNotNull();
        assertThat(summary.getAvgRating()).isNull();
        assertThat(summary.getReviewCount()).isEqualTo(0);
        assertThat(summary.getRating1Count()).isEqualTo(0);
        assertThat(summary.getRating2Count()).isEqualTo(0);
        assertThat(summary.getRating3Count()).isEqualTo(0);
        assertThat(summary.getRating4Count()).isEqualTo(0);
        assertThat(summary.getRating5Count()).isEqualTo(0);
    }

    // round 1 QA CONCERNS(재작업 지시 1) — 예전 구현은 LIST_SKU(시드 SKU-1001)가 이 테스트 시작
    // 시점에 정확히 0건이라고 가정하고 reviewCount==3/avgRating==4.7을 절대값으로 단언했다. 그
    // 가정은 cleanUpListFixtures()의 전역 삭제가 매번 시드 SKU를 비워 준 덕에만 성립했다 — 그
    // 전역 삭제 자체가 이 스위트 밖 리뷰(수동 확인·#3 화면 데모 데이터 등)를 파괴하는 버그였다.
    // 지금은 전용 상품을 만들어 시드 리뷰와 분리한다. 시딩 전 baseline을 조회하는 상대 단언도
    // 유지한다. ORDER_ITEMS FK는 insertListMember()가 먼저 생성하는 전용 PRODUCTS 행으로 만족한다.
    @Test
    void selectRatingSummary_threeReviews_returnsRoundedAverageAndDistributionSumEqualsReviewCount() {
        long baselineSum = sumOfActiveRatings(LIST_SKU);
        ProductReviewSummary baseline = dao.selectRatingSummary(LIST_SKU);

        insertListMember();
        insertListOrderAndItem("01");
        insertListOrderAndItem("02");
        insertListOrderAndItem("03");
        insertListReview(LIST_ORDER_PREFIX + "01", 5, LocalDateTime.of(2026, 9, 3, 10, 0, 0));
        insertListReview(LIST_ORDER_PREFIX + "02", 5, LocalDateTime.of(2026, 9, 3, 10, 1, 0));
        insertListReview(LIST_ORDER_PREFIX + "03", 4, LocalDateTime.of(2026, 9, 3, 10, 2, 0));

        ProductReviewSummary summary = dao.selectRatingSummary(LIST_SKU);

        int expectedCount = baseline.getReviewCount() + 3;
        // baseline 합(정수, COALESCE로 0건도 0)에 이번에 더한 5+5+4=14를 더해 정확한 새 평균을
        // SQL ROUND(...,1)과 동일 규칙(HALF_UP)으로 재계산한다 — baseline.getAvgRating()(이미
        // 반올림된 값)을 역산하지 않는다(반올림 오차 누적 방지).
        BigDecimal expectedAvg = BigDecimal.valueOf(baselineSum + 14)
                .divide(BigDecimal.valueOf(expectedCount), 10, RoundingMode.HALF_UP)
                .setScale(1, RoundingMode.HALF_UP);

        assertThat(summary.getReviewCount()).isEqualTo(expectedCount);
        assertThat(summary.getAvgRating()).isEqualByComparingTo(expectedAvg);
        assertThat(summary.getRating4Count()).isEqualTo(baseline.getRating4Count() + 1);
        assertThat(summary.getRating5Count()).isEqualTo(baseline.getRating5Count() + 2);
        int distributionSum = summary.getRating1Count() + summary.getRating2Count() + summary.getRating3Count()
                + summary.getRating4Count() + summary.getRating5Count();
        assertThat(distributionSum).isEqualTo(summary.getReviewCount());
    }

    @Test
    void selectRatingSummary_excludesSoftDeletedReview() {
        long baselineSum = sumOfActiveRatings(LIST_SKU);
        ProductReviewSummary baseline = dao.selectRatingSummary(LIST_SKU);

        insertListMember();
        insertListOrderAndItem("04");
        insertListOrderAndItem("05");
        Long keep = insertListReview(LIST_ORDER_PREFIX + "04", 5, LocalDateTime.of(2026, 9, 3, 10, 0, 0));
        Long deleted = insertListReview(LIST_ORDER_PREFIX + "05", 1, LocalDateTime.of(2026, 9, 3, 10, 1, 0));
        dao.softDelete(deleted, LIST_SKU, LIST_MEMBER_ID, LocalDateTime.now());

        ProductReviewSummary summary = dao.selectRatingSummary(LIST_SKU);

        int expectedCount = baseline.getReviewCount() + 1;
        BigDecimal expectedAvg = BigDecimal.valueOf(baselineSum + 5)
                .divide(BigDecimal.valueOf(expectedCount), 10, RoundingMode.HALF_UP)
                .setScale(1, RoundingMode.HALF_UP);

        assertThat(summary.getReviewCount()).isEqualTo(expectedCount);
        assertThat(summary.getAvgRating()).isEqualByComparingTo(expectedAvg);
        assertThat(keep).isNotNull();
    }

    /** {@link #selectRatingSummary_threeReviews_returnsRoundedAverageAndDistributionSumEqualsReviewCount}
     * 등의 baseline 대비 상대 단언 전용 — 활성(del_yn='N') 리뷰 rating 합을 그대로 얻는다
     * (매핑 없이 원시값, `selectRatingSummary`의 avgRating 반올림값을 역산하지 않기 위함). */
    private long sumOfActiveRatings(String sku) {
        Long sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(rating), 0) FROM PRODUCT_REVIEWS WHERE sku = ? AND del_yn = 'N'",
                Long.class, sku);
        return sum == null ? 0L : sum;
    }

    @Test
    void selectReviews_sortLatest_tieBreaksBySameCreatedAtOnReviewIdDesc() {
        insertListMember();
        insertListOrderAndItem("06");
        insertListOrderAndItem("07");
        LocalDateTime sameInstant = LocalDateTime.of(2026, 9, 3, 11, 0, 0);
        Long first = insertListReview(LIST_ORDER_PREFIX + "06", 3, sameInstant);
        Long second = insertListReview(LIST_ORDER_PREFIX + "07", 4, sameInstant);

        List<ProductReviewView> views = dao.selectReviews(LIST_SKU, "latest", 0L, 10);

        assertThat(views).extracting(ProductReviewView::getReviewId).containsExactly(second, first);
    }

    @Test
    void selectReviews_sortRatingDesc_tieBreaksBySameRatingOnCreatedAtDesc() {
        insertListMember();
        insertListOrderAndItem("08");
        insertListOrderAndItem("09");
        Long earlier = insertListReview(LIST_ORDER_PREFIX + "08", 5, LocalDateTime.of(2026, 9, 3, 12, 0, 0));
        Long later = insertListReview(LIST_ORDER_PREFIX + "09", 5, LocalDateTime.of(2026, 9, 3, 12, 5, 0));

        List<ProductReviewView> views = dao.selectReviews(LIST_SKU, "ratingDesc", 0L, 10);

        assertThat(views).extracting(ProductReviewView::getReviewId).containsExactly(later, earlier);
    }

    @Test
    void selectReviews_sortRatingAsc_tieBreaksBySameRatingOnCreatedAtDesc() {
        insertListMember();
        insertListOrderAndItem("10");
        insertListOrderAndItem("11");
        Long earlier = insertListReview(LIST_ORDER_PREFIX + "10", 2, LocalDateTime.of(2026, 9, 3, 13, 0, 0));
        Long later = insertListReview(LIST_ORDER_PREFIX + "11", 2, LocalDateTime.of(2026, 9, 3, 13, 5, 0));

        List<ProductReviewView> views = dao.selectReviews(LIST_SKU, "ratingAsc", 0L, 10);

        assertThat(views).extracting(ProductReviewView::getReviewId).containsExactly(later, earlier);
    }

    // round 1 QA CONCERNS(재작업 지시 2) — ratingDesc/ratingAsc는 review_id tie-break이 없으면
    // rating·created_at이 모두 같은 행들의 순서가 비결정적이라 페이지 경계에서 중복/누락이 생길 수
    // 있었다. rating=4로 동률인 3건을 만들어 9·10·11번째 자리(1페이지 끝 2개 + 2페이지 1번째)에
    // 정확히 걸치게 하고, tie-break(review_id DESC — 나중에 쓴 리뷰가 먼저)로 항상 같은 3건이
    // 항상 같은 순서로 쪼개지는지 확인한다. rating=5인 필러 8건으로 1~8번째 자리를 채운다(필러는
    // 서로 다른 created_at이라 동률이 아님 — 이 테스트의 관심사는 필러 순서가 아니라 경계의 동률
    // 3건).
    @Test
    void selectReviews_sortRatingDesc_threeTiedReviewsSplitDeterministicallyAtPageBoundary() {
        insertListMember();
        for (int i = 0; i < 8; i++) {
            String suffix = String.format("%02d", i + 40); // 40~47, 다른 테스트 접미와 겹치지 않게
            insertListOrderAndItem(suffix);
            insertListReview(LIST_ORDER_PREFIX + suffix, 5, LocalDateTime.of(2026, 9, 3, 18, 0, 0).plusMinutes(i));
        }
        insertListOrderAndItem("48");
        insertListOrderAndItem("49");
        insertListOrderAndItem("50");
        LocalDateTime tiedInstant = LocalDateTime.of(2026, 9, 3, 19, 0, 0);
        Long tie1 = insertListReview(LIST_ORDER_PREFIX + "48", 4, tiedInstant);
        Long tie2 = insertListReview(LIST_ORDER_PREFIX + "49", 4, tiedInstant);
        Long tie3 = insertListReview(LIST_ORDER_PREFIX + "50", 4, tiedInstant);

        List<ProductReviewView> page1 = dao.selectReviews(LIST_SKU, "ratingDesc", 0L, 10);
        List<ProductReviewView> page2 = dao.selectReviews(LIST_SKU, "ratingDesc", 10L, 10);

        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(1);
        // 동률 3건은 review_id 내림차순(나중에 작성된 tie3부터) — tie3·tie2가 1페이지 9·10번째,
        // tie1이 2페이지 1번째로 경계를 건너간다. tie-break 없이는 이 순서가 비결정적이다.
        assertThat(page1.get(8).getReviewId()).isEqualTo(tie3);
        assertThat(page1.get(9).getReviewId()).isEqualTo(tie2);
        assertThat(page2.get(0).getReviewId()).isEqualTo(tie1);
    }

    @Test
    void selectReviews_excludesSoftDeletedReview() {
        insertListMember();
        insertListOrderAndItem("12");
        insertListOrderAndItem("13");
        Long active = insertListReview(LIST_ORDER_PREFIX + "12", 5, LocalDateTime.of(2026, 9, 3, 14, 0, 0));
        Long deleted = insertListReview(LIST_ORDER_PREFIX + "13", 1, LocalDateTime.of(2026, 9, 3, 14, 1, 0));
        dao.softDelete(deleted, LIST_SKU, LIST_MEMBER_ID, LocalDateTime.now());

        List<ProductReviewView> views = dao.selectReviews(LIST_SKU, "latest", 0L, 10);

        assertThat(views).extracting(ProductReviewView::getReviewId).containsExactly(active);
    }

    @Test
    void selectReviews_joinsMemberNameFromMembers() {
        insertListMember();
        insertListOrderAndItem("14");
        insertListReview(LIST_ORDER_PREFIX + "14", 5, LocalDateTime.of(2026, 9, 3, 15, 0, 0));

        List<ProductReviewView> views = dao.selectReviews(LIST_SKU, "latest", 0L, 10);

        assertThat(views).hasSize(1);
        assertThat(views.get(0).getMemberName()).isEqualTo("상품평목록DAO테스터");
    }

    // 페이지 경계(10/11건) — 11번째(가장 오래된) 리뷰가 2페이지 1번째로 나오는지.
    @Test
    void selectReviews_pageBoundary_eleventhOldestReviewAppearsOnSecondPage() {
        insertListMember();
        Long[] reviewIds = new Long[11];
        for (int i = 0; i < 11; i++) {
            String suffix = String.format("%02d", i + 20); // 20~30, 위 테스트 접미와 겹치지 않게
            insertListOrderAndItem(suffix);
            LocalDateTime createdAt = LocalDateTime.of(2026, 9, 3, 16, 0, 0).plusMinutes(i);
            reviewIds[i] = insertListReview(LIST_ORDER_PREFIX + suffix, 5, createdAt);
        }

        List<ProductReviewView> page1 = dao.selectReviews(LIST_SKU, "latest", 0L, 10);
        List<ProductReviewView> page2 = dao.selectReviews(LIST_SKU, "latest", 10L, 10);

        assertThat(page1).hasSize(10);
        assertThat(page2).hasSize(1);
        // 가장 오래된 리뷰(i=0, 09:16:00)가 latest DESC 정렬의 마지막 = 2페이지 1번째.
        assertThat(page2.get(0).getReviewId()).isEqualTo(reviewIds[0]);
    }

    private void insertListMember() {
        jdbcTemplate.update("INSERT INTO PRODUCTS (sku, product_name, price, stock_qty, sale_yn) "
                + "VALUES (?, ?, ?, ?, ?)", LIST_SKU, "상품평 목록 테스트 전용 상품", 10000, 100, "Y");
        listProductCreated = true;
        Integer existing = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBERS WHERE member_id = ?", Integer.class, LIST_MEMBER_ID);
        if (existing != null && existing > 0) {
            return;
        }
        memberDao.insertMember(LIST_MEMBER_ID, "상품평목록DAO테스터", "BRONZE",
                "product-review-list-dao-test@example.com", null, null, "unused-hash", false,
                LocalDateTime.now());
    }

    private void insertListOrderAndItem(String orderNoSuffix) {
        String orderNo = LIST_ORDER_PREFIX + orderNoSuffix;
        insertOrder(orderNo, LIST_MEMBER_ID, "DONE");
        insertOrderItem(orderNo, LIST_SKU);
    }

    private Long insertListReview(String orderNo, int rating, LocalDateTime createdAt) {
        ProductReview review = new ProductReview();
        review.setOrderNo(orderNo);
        review.setSku(LIST_SKU);
        review.setMemberId(LIST_MEMBER_ID);
        review.setRating(rating);
        review.setContent("목록 조회 DAO 테스트 전용 상품평 본문입니다.");
        review.setCreatedAt(createdAt);
        review.setUpdatedAt(createdAt);
        dao.insertReview(review);
        return review.getReviewId();
    }

    private Long insertReview(String orderNo, String sku, String memberId, int rating) {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        ProductReview review = new ProductReview();
        review.setOrderNo(orderNo);
        review.setSku(sku);
        review.setMemberId(memberId);
        review.setRating(rating);
        review.setContent("DAO 테스트 전용 상품평 본문입니다.");
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        dao.insertReview(review);
        return review.getReviewId();
    }

    private void insertOrder(String orderNo, String memberId, String state) {
        Order order = new Order();
        order.setOrderNo(orderNo);
        order.setMemberId(memberId);
        order.setOrderState(state);
        order.setTotalAmount(10000);
        orderDao.insertOrder(order);
    }

    private void insertOrderItem(String orderNo, String sku) {
        OrderItem item = new OrderItem();
        item.setOrderNo(orderNo);
        item.setLineNo(1);
        item.setSku(sku);
        item.setQty(1);
        item.setUnitPrice(10000);
        orderDao.insertItem(item);
    }
}
