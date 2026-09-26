package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductReviewDao;
import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.domain.ProductReview;
import com.sm.lab.shop.domain.ProductReviewSummary;
import com.sm.lab.shop.domain.ProductReviewView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DuplicateKeyException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SR-351(#1) — {@link ProductReviewService} 단위 테스트(Mockito, DAO 목업). HTTP 계약은
 * {@code ProductReviewControllerTest}가, SQL 자체는 {@code ProductReviewDaoTest}가 맡는다 — 이
 * 테스트는 STORY "테스트" 절이 지정한 검증 순서·판정 경계만 본다.
 */
class ProductReviewServiceTest {

    private static final String MEMBER_ID = "M-0001";
    private static final String OTHER_MEMBER_ID = "M-0002";
    private static final String SKU = "SKU-1001";
    private static final String ORDER_NO = "20260815-0001";
    private static final String VALID_CONTENT = "정말 좋은 상품이었습니다. 다음에도 또 구매하겠습니다.";
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-26T00:00:00Z"), ZoneOffset.UTC);

    private ProductReviewDao dao;
    private ProductDao productDao;
    private ProductReviewService service;

    @BeforeEach
    void setUp() {
        dao = mock(ProductReviewDao.class);
        productDao = mock(ProductDao.class);
        service = new ProductReviewService(dao, productDao, FIXED_CLOCK);
        when(productDao.selectBySku(SKU)).thenReturn(new Product());
        when(dao.existsOrder(ORDER_NO)).thenReturn(1);
        when(dao.countEligiblePurchase(ORDER_NO, MEMBER_ID, SKU)).thenReturn(1);
    }

    private static ProductReviewService.ReviewInput input(BigDecimal rating, String content, String orderNo) {
        return new ProductReviewService.ReviewInput(rating, content, orderNo);
    }

    // ---- ①rating 검증 ----

    @Test
    void create_ratingZero_throws400BeforeAnyDaoCall() {
        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(BigDecimal.ZERO, VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");

        // setUp()이 이미 mock에 스텁을 걸어 두어 verifyNoInteractions는 쓸 수 없다(스텁 호출도
        // 상호작용으로 기록됨) — 대신 검증 실패 시 실제 DB 접근 메서드가 호출되지 않았는지 확인.
        verify(productDao, never()).selectBySku(any());
        verify(dao, never()).existsOrder(any());
        verify(dao, never()).insertReview(any(ProductReview.class));
    }

    @Test
    void create_ratingSix_throws400() {
        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(new BigDecimal("6"), VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    @Test
    void create_ratingDecimal_throws400() {
        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(new BigDecimal("4.5"), VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    // round 1 QA CONCERNS 1 — validateRating이 BigDecimal.intValue()로 먼저 좁힌 뒤 범위를 봐서
    // 32비트 오버플로 값이 검증을 우회했다(4294967297 -> intValue()=1 -> 1~5 통과 -> 201).
    // compareTo(BigDecimal)로 범위 판정을 끝낸 뒤에만 intValueExact()로 변환하도록 고쳤다 — 이
    // 값은 int 범위를 훌쩍 넘지만 BigDecimal.compareTo는 narrowing 없이 정확히 비교한다.
    @Test
    void create_ratingIntOverflow_throws400NotSilentlyAccepted() {
        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU,
                input(new BigDecimal("4294967297"), VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");

        verify(dao, never()).insertReview(any(ProductReview.class));
    }

    @Test
    void create_ratingNegativeOne_throws400() {
        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU,
                input(new BigDecimal("-1"), VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    @Test
    void create_ratingNull_throws400() {
        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(null, VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");

        verify(dao, never()).insertReview(any(ProductReview.class));
    }

    // ---- ②content 검증 ----

    @Test
    void create_contentNineChars_throws400() {
        String tooShort = "a".repeat(9);
        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(BigDecimal.valueOf(5), tooShort, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    @Test
    void create_contentOverThousandOneChars_throws400() {
        String tooLong = "a".repeat(1001);
        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(BigDecimal.valueOf(5), tooLong, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    // ---- ③orderNo 검증 ----

    @Test
    void create_orderNoBlank_throws400() {
        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(BigDecimal.valueOf(5), VALID_CONTENT, " ")))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    // ---- ④상품 존재 ----

    @Test
    void create_unknownSku_throws404() {
        when(productDao.selectBySku(SKU)).thenReturn(null);

        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(BigDecimal.valueOf(5), VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4041");

        verify(dao, never()).insertReview(any(ProductReview.class));
    }

    // ---- ⑤주문 존재 ----

    @Test
    void create_unknownOrderNo_throws404() {
        when(dao.existsOrder(ORDER_NO)).thenReturn(0);

        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(BigDecimal.valueOf(5), VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4041");

        verify(dao, never()).insertReview(any(ProductReview.class));
    }

    // ---- ⑥구매 자격(타인 소유/CANCELED/SKU 미포함 — 전부 countEligiblePurchase=0으로 표현) ----

    @Test
    void create_foreignOrderOrCanceledOrSkuNotInOrder_throws403() {
        when(dao.countEligiblePurchase(ORDER_NO, MEMBER_ID, SKU)).thenReturn(0);

        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(BigDecimal.valueOf(5), VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4031");

        verify(dao, never()).insertReview(any(ProductReview.class));
    }

    // ---- ⑦정상 작성 / 중복 ----

    @Test
    void create_valid_insertsAndReturnsReview() {
        ProductReview created = service.create(MEMBER_ID, SKU, input(BigDecimal.valueOf(5), VALID_CONTENT, ORDER_NO));

        assertThat(created.getSku()).isEqualTo(SKU);
        assertThat(created.getOrderNo()).isEqualTo(ORDER_NO);
        assertThat(created.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(created.getRating()).isEqualTo(5);
        assertThat(created.getContent()).isEqualTo(VALID_CONTENT);
        assertThat(created.getCreatedAt()).isEqualTo(created.getUpdatedAt());
        verify(dao).insertReview(any(ProductReview.class));
    }

    @Test
    void create_duplicateActiveReview_throws409() {
        Mockito.doThrow(new DuplicateKeyException("uq_product_reviews_active"))
                .when(dao).insertReview(any(ProductReview.class));

        assertThatThrownBy(() -> service.create(MEMBER_ID, SKU, input(BigDecimal.valueOf(5), VALID_CONTENT, ORDER_NO)))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4091");
    }

    // ---- delete() ----

    @Test
    void delete_success_softDeletesWithoutFurtherLookup() {
        when(dao.softDelete(eq(1L), eq(SKU), eq(MEMBER_ID), any())).thenReturn(1);

        service.delete(MEMBER_ID, SKU, 1L);

        verify(dao, never()).selectById(anyLong());
    }

    @Test
    void delete_foreignReview_throws403NotFound404Priority() {
        when(dao.softDelete(anyLong(), any(), any(), any())).thenReturn(0);
        ProductReview other = new ProductReview();
        other.setReviewId(1L);
        other.setSku(SKU);
        other.setMemberId(OTHER_MEMBER_ID);
        other.setDelYn("N");
        when(dao.selectById(1L)).thenReturn(other);

        assertThatThrownBy(() -> service.delete(MEMBER_ID, SKU, 1L))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4031");
    }

    @Test
    void delete_alreadyDeleted_throws404() {
        when(dao.softDelete(anyLong(), any(), any(), any())).thenReturn(0);
        ProductReview already = new ProductReview();
        already.setReviewId(1L);
        already.setSku(SKU);
        already.setMemberId(MEMBER_ID);
        already.setDelYn("Y");
        when(dao.selectById(1L)).thenReturn(already);

        assertThatThrownBy(() -> service.delete(MEMBER_ID, SKU, 1L))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4041");
    }

    @Test
    void delete_unknownReviewId_throws404() {
        when(dao.softDelete(anyLong(), any(), any(), any())).thenReturn(0);
        when(dao.selectById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.delete(MEMBER_ID, SKU, 99L))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4041");
    }

    @Test
    void delete_skuMismatch_throws404_crossProductGuessPrevention() {
        when(dao.softDelete(anyLong(), any(), any(), any())).thenReturn(0);
        ProductReview otherSku = new ProductReview();
        otherSku.setReviewId(1L);
        otherSku.setSku("SKU-9999");
        otherSku.setMemberId(MEMBER_ID);
        otherSku.setDelYn("N");
        when(dao.selectById(1L)).thenReturn(otherSku);

        assertThatThrownBy(() -> service.delete(MEMBER_ID, SKU, 1L))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4041");
    }

    // ---- 반복 삭제-재작성 시나리오(사람 수정 확정) — 작성→삭제→재작성→재삭제→재작성 5단계가
    // 서비스 자체에 숨은 상태 없이 매번 동일하게 동작하는지(DAO만 상태를 가짐을 목업으로 확인). ----

    // round 1 QA CONCERNS 4 — 이 테스트는 DAO가 mock이라 active_key 유일성 상태를 갖지 않는다:
    // insertReview는 매 사이클 무조건 성공하므로 흐름(호출 순서·반환값)만 검증할 뿐, "반복해도
    // 실제로 깨지지 않는지"의 실보증은 되지 못한다(active_key 생성열 + UNIQUE 인덱스를 지워도 이
    // 테스트는 그대로 통과한다). 사람 수정이 확정한 반복 유일성 보증의 실제 행사 주체는
    // ProductReviewDaoTest(실 DB, 동일 5단계를 매퍼 호출로 직접 반복)다 — 이 테스트는 서비스
    // 계층이 사이클마다 새 INSERT/조건부 UPDATE를 올바른 인자로 호출하는지만 확인한다.
    @Test
    void repeatedCreateDeleteCycle_fiveSteps_allSucceedEveryTime() {
        for (int cycle = 0; cycle < 3; cycle++) {
            ProductReview created = service.create(MEMBER_ID, SKU,
                    input(BigDecimal.valueOf(5), VALID_CONTENT, ORDER_NO));
            assertThat(created).isNotNull();

            when(dao.softDelete(anyLong(), any(), any(), any())).thenReturn(1);
            service.delete(MEMBER_ID, SKU, 100L + cycle);
        }

        verify(dao, times(3)).insertReview(any(ProductReview.class));
        verify(dao, times(3)).softDelete(anyLong(), any(), any(), any());
    }

    // ==================== SR-351(#2) — list() ====================

    private static ProductReviewView view(Long reviewId, int rating, String content, String memberName) {
        ProductReviewView v = new ProductReviewView();
        v.setReviewId(reviewId);
        v.setRating(rating);
        v.setContent(content);
        v.setMemberName(memberName);
        v.setCreatedAt(LocalDateTime.of(2026, 9, 26, 0, 0, 0));
        return v;
    }

    private static ProductReviewSummary summary(BigDecimal avgRating, int reviewCount, int r1, int r2, int r3,
                                                 int r4, int r5) {
        ProductReviewSummary s = new ProductReviewSummary();
        s.setAvgRating(avgRating);
        s.setReviewCount(reviewCount);
        s.setRating1Count(r1);
        s.setRating2Count(r2);
        s.setRating3Count(r3);
        s.setRating4Count(r4);
        s.setRating5Count(r5);
        return s;
    }

    @Test
    void list_invalidSort_throws400BeforeAnyDaoCall() {
        assertThatThrownBy(() -> service.list(SKU, "bogus", "1"))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");

        verify(productDao, never()).selectBySku(anyString());
        verify(dao, never()).selectReviews(any(), any(), anyLong(), anyInt());
    }

    @Test
    void list_sortCaseVariant_throws400() {
        // 대소문자 변형도 화이트리스트 밖 — 조용히 정규화하지 않는다(ProductService.normalizeSort와
        // 다른 정책, STORY "순서·보안" 1).
        assertThatThrownBy(() -> service.list(SKU, "LATEST", "1"))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    @Test
    void list_pageZero_throws400() {
        assertThatThrownBy(() -> service.list(SKU, "latest", "0"))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    @Test
    void list_pageNegative_throws400() {
        assertThatThrownBy(() -> service.list(SKU, "latest", "-1"))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    @Test
    void list_pageNonInteger_throws400() {
        assertThatThrownBy(() -> service.list(SKU, "latest", "abc"))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    @Test
    void list_pageBlankOrNull_throws400() {
        assertThatThrownBy(() -> service.list(SKU, "latest", " "))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
        assertThatThrownBy(() -> service.list(SKU, "latest", null))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4001");
    }

    @Test
    void list_unknownSku_throws404_daoNotCalled() {
        when(productDao.selectBySku(SKU)).thenReturn(null);

        // round 1 QA CONCERNS(재작업 지시 3) — 목록 404는 작성 API의 notFound()("상품 또는
        // 주문을 찾을 수 없습니다")를 재사용하지 않고 전용 메시지를 쓴다(목록 조회에는 주문
        // 개념이 없어 그 문구가 오해를 준다). 코드 RVW-4041은 계약대로 그대로.
        assertThatThrownBy(() -> service.list(SKU, "latest", "1"))
                .isInstanceOf(ProductReviewApiException.class)
                .hasFieldOrPropertyWithValue("code", "RVW-4041")
                .hasFieldOrPropertyWithValue("message", "상품을 찾을 수 없습니다");

        verify(dao, never()).selectReviews(any(), any(), anyLong(), anyInt());
        verify(dao, never()).selectRatingSummary(any());
    }

    @Test
    void list_valid_returnsMaskedNamesAndDistribution() {
        when(dao.selectReviews(eq(SKU), eq("latest"), eq(0L), eq(10))).thenReturn(List.of(
                view(3L, 5, "정말 좋아요", "홍길동"),
                view(2L, 4, "괜찮아요", "이몽룡")
        ));
        when(dao.selectRatingSummary(SKU)).thenReturn(summary(new BigDecimal("4.5"), 2, 0, 0, 0, 1, 1));

        Map<String, Object> body = service.list(SKU, "latest", "1");

        assertThat(body.get("totalCount")).isEqualTo(2);
        assertThat(body.get("page")).isEqualTo(1);
        assertThat(body.get("size")).isEqualTo(10);
        assertThat(body.get("avgRating")).isEqualTo(new BigDecimal("4.5"));
        assertThat(body.get("reviewCount")).isEqualTo(2);

        @SuppressWarnings("unchecked")
        Map<String, Integer> distribution = (Map<String, Integer>) body.get("distribution");
        assertThat(distribution).containsExactly(
                Map.entry("1", 0), Map.entry("2", 0), Map.entry("3", 0), Map.entry("4", 1), Map.entry("5", 1));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("authorName")).isEqualTo("홍*동");
        assertThat(items.get(1).get("authorName")).isEqualTo("이*룡");
    }

    @Test
    void list_noReviews_avgRatingIsNullNotZeroAndDistributionAllZero() {
        // Map.of(...)는 null 값에서 NPE를 던진다 — LinkedHashMap 조립 회귀를 직접 재현한다.
        when(dao.selectReviews(eq(SKU), eq("latest"), eq(0L), eq(10))).thenReturn(List.of());
        when(dao.selectRatingSummary(SKU)).thenReturn(summary(null, 0, 0, 0, 0, 0, 0));

        Map<String, Object> body = service.list(SKU, "latest", "1");

        assertThat(body).containsKey("avgRating");
        assertThat(body.get("avgRating")).isNull();
        assertThat(body.get("reviewCount")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        Map<String, Integer> distribution = (Map<String, Integer>) body.get("distribution");
        assertThat(distribution).hasSize(5).containsValues(0, 0, 0, 0, 0);
    }

    @Test
    void list_page2_passesCorrectOffsetToDao() {
        when(dao.selectReviews(eq(SKU), eq("latest"), eq(10L), eq(10))).thenReturn(List.of());
        when(dao.selectRatingSummary(SKU)).thenReturn(summary(null, 0, 0, 0, 0, 0, 0));

        Map<String, Object> body = service.list(SKU, "latest", "2");

        assertThat(body.get("page")).isEqualTo(2);
        verify(dao).selectReviews(SKU, "latest", 10L, 10);
    }

    // ---- maskAuthorName 경계(list() 결과로 간접 검증 — private static 헬퍼 직접 호출 불가) ----

    @Test
    void list_maskAuthorName_oneCharacter_returnsSingleAsterisk() {
        when(dao.selectReviews(eq(SKU), eq("latest"), eq(0L), eq(10)))
                .thenReturn(List.of(view(1L, 5, "본문", "홍")));
        when(dao.selectRatingSummary(SKU)).thenReturn(summary(BigDecimal.valueOf(5), 1, 0, 0, 0, 0, 1));

        Map<String, Object> body = service.list(SKU, "latest", "1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items.get(0).get("authorName")).isEqualTo("*");
    }

    @Test
    void list_maskAuthorName_twoCharacters_dropsLastChar() {
        when(dao.selectReviews(eq(SKU), eq("latest"), eq(0L), eq(10)))
                .thenReturn(List.of(view(1L, 5, "본문", "홍길")));
        when(dao.selectRatingSummary(SKU)).thenReturn(summary(BigDecimal.valueOf(5), 1, 0, 0, 0, 0, 1));

        Map<String, Object> body = service.list(SKU, "latest", "1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items.get(0).get("authorName")).isEqualTo("홍*");
    }

    @Test
    void list_maskAuthorName_fourOrMoreCharacters_stillSingleAsterisk() {
        when(dao.selectReviews(eq(SKU), eq("latest"), eq(0L), eq(10)))
                .thenReturn(List.of(view(1L, 5, "본문", "김철수영")));
        when(dao.selectRatingSummary(SKU)).thenReturn(summary(BigDecimal.valueOf(5), 1, 0, 0, 0, 0, 1));

        Map<String, Object> body = service.list(SKU, "latest", "1");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        // 4자 이상도 별표는 항상 1개(가운데 글자 수와 무관) — 오해 방지(STORY "테스트" 절).
        assertThat(items.get(0).get("authorName")).isEqualTo("김*영");
    }
}
