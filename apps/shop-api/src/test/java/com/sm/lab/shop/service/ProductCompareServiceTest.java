package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.ProductCompareDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductReviewDao;
import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.domain.ProductCompareView;
import com.sm.lab.shop.domain.ProductReviewSummary;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.transaction.support.TransactionOperations;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SR-360(#2) — {@link ProductCompareService} 단위 테스트(Mockito, DAO 목업). HTTP 계약은
 * {@code ProductCompareControllerTest}가, SQL 자체는 {@code ProductCompareDaoTest}가 맡는다 — 이
 * 테스트는 STORY "테스트" 절이 지정한 경계 조건(존재 판정·상한·멱등·avgRating 병합)만 본다.
 */
class ProductCompareServiceTest {

    private static final String MEMBER_ID = "M-0001";
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneOffset.UTC);

    private ProductCompareDao dao;
    private ProductDao productDao;
    private MemberDao memberDao;
    private ProductReviewDao productReviewDao;
    private ProductCompareService service;

    @BeforeEach
    void setUp() {
        dao = mock(ProductCompareDao.class);
        productDao = mock(ProductDao.class);
        memberDao = mock(MemberDao.class);
        productReviewDao = mock(ProductReviewDao.class);
        // round2 — 실 DB 트랜잭션 없이 콜백만 즉시 실행하는 TransactionOperations(Spring 제공
        // 정적 팩토리, DAO mock 기반 단위 테스트에 real TransactionTemplate/PlatformTransactionManager가
        // 필요 없게 한다). 데드락 재시도 자체는 이 단위 테스트가 아니라 실 DB
        // ProductCompareConcurrencyTest가 검증한다.
        service = new ProductCompareService(dao, productDao, memberDao, productReviewDao, FIXED_CLOCK,
                TransactionOperations.withoutTransaction());
    }

    private static Product product(String sku) {
        Product p = new Product();
        p.setSku(sku);
        return p;
    }

    private static Member member(String memberId) {
        Member m = new Member();
        m.setMemberId(memberId);
        return m;
    }

    private static ProductCompareView compareView(String sku, String productName, long price, Long listPrice,
                                                    String imageUrl, int stockQty, boolean soldOut,
                                                    LocalDateTime comparedAt) {
        ProductCompareView v = new ProductCompareView();
        v.setSku(sku);
        v.setProductName(productName);
        v.setPrice(price);
        v.setListPrice(listPrice);
        v.setImageUrl(imageUrl);
        v.setStockQty(stockQty);
        v.setSoldOut(soldOut);
        v.setComparedAt(comparedAt);
        return v;
    }

    // linked_tc: 담기 — sku 공백은 DAO 접근 전 400으로 차단한다.
    @Test
    void add_skuBlank_throws400BeforeAnyDaoCall() {
        assertThatThrownBy(() -> service.add(MEMBER_ID, " "))
                .isInstanceOf(ProductCompareApiException.class)
                .hasFieldOrPropertyWithValue("code", "CMP-4001");

        verifyNoInteractions(productDao, dao);
    }

    // linked_tc: 담기 — 상품 미존재는 404, insertCompareIfUnderLimit 호출 안 됨.
    @Test
    void add_productNotFound_throws404WithoutInsert() {
        when(productDao.selectBySku("SKU-9999")).thenReturn(null);

        assertThatThrownBy(() -> service.add(MEMBER_ID, "SKU-9999"))
                .isInstanceOf(ProductCompareApiException.class)
                .hasFieldOrPropertyWithValue("code", "CMP-4041");

        verify(dao, never()).insertCompareIfUnderLimit(any(), any(), any());
    }

    // linked_tc: 담기 성공 — 고정 시계 now()로 insertCompareIfUnderLimit 호출(1행 — 신규 담기라
    // existsCompare 재조회 없음).
    @Test
    void add_success_insertsCompareWithFixedClockNow() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001"));
        when(dao.insertCompareIfUnderLimit(any(), any(), any())).thenReturn(1);

        service.add(MEMBER_ID, "SKU-1001");

        verify(dao).insertCompareIfUnderLimit(MEMBER_ID, "SKU-1001", LocalDateTime.now(FIXED_CLOCK));
        verify(dao, never()).existsCompare(any(), any());
    }

    // linked_tc: 재담기(이미 담긴 상품 다시 담기) — DAO가 0행(IGNORE)을 반환하면 existsCompare로
    // 원인을 가르고, 존재하므로 예외를 던지지 않는다(멱등 200 요구, 컨트롤러가 항상 같은 응답으로
    // 매핑).
    @Test
    void add_alreadyCompared_daoReturnsZeroRows_existsTrue_stillSucceeds() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001"));
        when(dao.insertCompareIfUnderLimit(any(), any(), any())).thenReturn(0);
        when(dao.existsCompare(MEMBER_ID, "SKU-1001")).thenReturn(true);

        service.add(MEMBER_ID, "SKU-1001");

        verify(dao).insertCompareIfUnderLimit(eq(MEMBER_ID), eq("SKU-1001"), any(LocalDateTime.class));
        verify(dao).existsCompare(MEMBER_ID, "SKU-1001");
        verify(memberDao, never()).selectById(any());
    }

    // linked_tc: 상한 초과 — DAO가 0행을 반환하고 existsCompare가 false(=진짜 신규 시도)이며
    // 회원은 실재 → 409 CMP-4091.
    @Test
    void add_limitExceeded_daoReturnsZeroRows_existsFalse_memberExists_throws409() {
        when(productDao.selectBySku("SKU-1005")).thenReturn(product("SKU-1005"));
        when(dao.insertCompareIfUnderLimit(any(), any(), any())).thenReturn(0);
        when(dao.existsCompare(MEMBER_ID, "SKU-1005")).thenReturn(false);
        when(memberDao.selectById(MEMBER_ID)).thenReturn(member(MEMBER_ID));

        assertThatThrownBy(() -> service.add(MEMBER_ID, "SKU-1005"))
                .isInstanceOf(ProductCompareApiException.class)
                .hasFieldOrPropertyWithValue("code", "CMP-4091");
    }

    // linked_tc: 방어적 엣지케이스 — admin 키가 실재하지 않는 memberId로 담기를 시도하면
    // INSERT IGNORE가 member_id FK 위반을 조용히 흡수해 0행을 반환한다. 0행이고 회원도 존재하지
    // 않으면 404 CMP-4042(신규 코드)를 던진다.
    @Test
    void add_memberDoesNotExist_daoReturnsZeroRows_existsFalse_throws404WithMemberNotFoundCode() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001"));
        when(dao.insertCompareIfUnderLimit(any(), any(), any())).thenReturn(0);
        when(dao.existsCompare("M-9999", "SKU-1001")).thenReturn(false);
        when(memberDao.selectById("M-9999")).thenReturn(null);

        assertThatThrownBy(() -> service.add("M-9999", "SKU-1001"))
                .isInstanceOf(ProductCompareApiException.class)
                .hasFieldOrPropertyWithValue("code", "CMP-4042");
    }

    // linked_tc: 빼기 — DAO가 0행(담지 않은 상품)을 반환해도 예외 없음(항상 성공).
    @Test
    void remove_daoReturnsZero_doesNotThrow() {
        when(dao.deleteCompare(MEMBER_ID, "SKU-1001")).thenReturn(0);

        service.remove(MEMBER_ID, "SKU-1001");

        verify(dao).deleteCompare(MEMBER_ID, "SKU-1001");
    }

    // linked_tc: 빼기 — DAO가 1행(실제 존재)을 반환해도 동일하게 예외 없음.
    @Test
    void remove_daoReturnsOne_doesNotThrow() {
        when(dao.deleteCompare(MEMBER_ID, "SKU-1001")).thenReturn(1);

        service.remove(MEMBER_ID, "SKU-1001");

        verify(dao).deleteCompare(MEMBER_ID, "SKU-1001");
    }

    // linked_tc: 목록 — selectCompareList 결과에 sku별 avgRating/reviewCount를 병합하고, 정렬은
    // DAO 위임(서비스가 재정렬하지 않음). page/size 필드는 응답에 없다.
    @Test
    void list_mergesRatingSummaryPerSkuAndDoesNotReorderOrPaginate() {
        ProductCompareView v1 = compareView("SKU-1001", "스탠딩 데스크", 390000L, 450000L,
                "/images/products/sku-1001.svg", 12, false, LocalDateTime.now().minusDays(1));
        ProductCompareView v2 = compareView("SKU-1002", "기계식 키보드", 129000L, null,
                "/images/products/sku-1002.svg", 0, true, LocalDateTime.now());
        when(dao.selectCompareList(MEMBER_ID)).thenReturn(List.of(v1, v2));

        ProductReviewSummary summary1 = new ProductReviewSummary();
        summary1.setAvgRating(new BigDecimal("4.5"));
        summary1.setReviewCount(3);
        ProductReviewSummary summary2 = new ProductReviewSummary();
        summary2.setReviewCount(0);
        when(productReviewDao.selectRatingSummary("SKU-1001")).thenReturn(summary1);
        when(productReviewDao.selectRatingSummary("SKU-1002")).thenReturn(summary2);

        Map<String, Object> body = service.list(MEMBER_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0).get("sku")).isEqualTo("SKU-1001");
        assertThat(items.get(0).get("avgRating")).isEqualTo(new BigDecimal("4.5"));
        assertThat(items.get(0).get("reviewCount")).isEqualTo(3);
        assertThat(items.get(1).get("sku")).isEqualTo("SKU-1002");
        assertThat(items.get(1).get("avgRating")).isNull();
        assertThat(items.get(1).get("soldOut")).isEqualTo(true);
        assertThat(body).doesNotContainKey("page");
        assertThat(body).doesNotContainKey("size");
    }

    // linked_tc: 데드락 재시도 — DAO가 1회 DeadlockLoserDataAccessException을 던진 뒤 성공하면,
    // 정확히 2회 시도(1회 실패 + 1회 성공) 후 예외 없이 반환한다(round2 QA 권고 — 재시도 경로
    // 결정적 검증).
    @Test
    void add_deadlockOnce_retriesAndSucceeds() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001"));
        // 첫 시도: 데드락 에러, 두 번째 시도: 성공(1행)
        when(dao.insertCompareIfUnderLimit(MEMBER_ID, "SKU-1001", LocalDateTime.now(FIXED_CLOCK)))
                .thenThrow(new DeadlockLoserDataAccessException("Deadlock", new Exception()))
                .thenReturn(1);

        service.add(MEMBER_ID, "SKU-1001");

        verify(dao, times(2)).insertCompareIfUnderLimit(MEMBER_ID, "SKU-1001", LocalDateTime.now(FIXED_CLOCK));
        verify(dao, never()).existsCompare(any(), any());
    }

    // linked_tc: 데드락 재시도 소진 — DAO가 3회 연속 DeadlockLoserDataAccessException을 던지면,
    // 최대 재시도 횟수(3회)를 소진 후 그 예외가 그대로 전파된다(서비스가 예외를 삼키지 않음,
    // 컨트롤러가 ProductCompareExceptionHandler로 CMP-5000 500으로 매핑).
    @Test
    void add_deadlock3Times_exhaustsRetriesAndThrows() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001"));
        // 3회 연속 데드락
        when(dao.insertCompareIfUnderLimit(any(), any(), any()))
                .thenThrow(new DeadlockLoserDataAccessException("Deadlock", new Exception()))
                .thenThrow(new DeadlockLoserDataAccessException("Deadlock", new Exception()))
                .thenThrow(new DeadlockLoserDataAccessException("Deadlock", new Exception()));

        assertThatThrownBy(() -> service.add(MEMBER_ID, "SKU-1001"))
                .isInstanceOf(DeadlockLoserDataAccessException.class);
    }
}
