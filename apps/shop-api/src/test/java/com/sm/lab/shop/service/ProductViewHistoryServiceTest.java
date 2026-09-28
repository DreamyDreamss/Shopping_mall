package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductViewHistoryDao;
import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.domain.ProductViewHistoryView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link ProductViewHistoryService} 단위 테스트(Mockito, DAO 목업). HTTP 계약은 {@code
 * ProductRecentlyViewedControllerTest}가, SQL 자체는 {@code ProductViewHistoryDaoTest}가 맡는다 —
 * 이 테스트는 존재 판정 순서(sku검증→회원존재→상품존재→upsert→cap삭제)와 목록 매핑만 본다.
 */
class ProductViewHistoryServiceTest {

    private static final String MEMBER_ID = "M-0001";
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-28T00:00:00Z"), ZoneOffset.UTC);

    private ProductViewHistoryDao dao;
    private ProductDao productDao;
    private MemberDao memberDao;
    private ProductViewHistoryService service;

    @BeforeEach
    void setUp() {
        dao = mock(ProductViewHistoryDao.class);
        productDao = mock(ProductDao.class);
        memberDao = mock(MemberDao.class);
        service = new ProductViewHistoryService(dao, productDao, memberDao, FIXED_CLOCK);
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

    // linked_tc: 기록 — sku 공백은 DAO 접근 전 400으로 차단한다.
    @Test
    void record_skuBlank_throws400BeforeAnyDaoCall() {
        assertThatThrownBy(() -> service.record(MEMBER_ID, " "))
                .isInstanceOf(ProductViewHistoryApiException.class)
                .hasFieldOrPropertyWithValue("code", "RCV-4001");

        verifyNoInteractions(memberDao, productDao, dao);
    }

    // linked_tc: 기록 — sku 형식 오류(ApiKeyAuthFilter의 라우팅 정규식과 동일 정의)도 400.
    @Test
    void record_skuFormatInvalid_throws400BeforeAnyDaoCall() {
        assertThatThrownBy(() -> service.record(MEMBER_ID, "not-a-sku"))
                .isInstanceOf(ProductViewHistoryApiException.class)
                .hasFieldOrPropertyWithValue("code", "RCV-4001");

        verifyNoInteractions(memberDao, productDao, dao);
    }

    // linked_tc: 기록 — 회원 없음(admin 대리 호출 전용 방어)은 404, 상품 조회·UPSERT 모두 호출 안 됨.
    @Test
    void record_memberNotFound_throws404WithoutProductLookupOrUpsert() {
        when(memberDao.selectById(MEMBER_ID)).thenReturn(null);

        assertThatThrownBy(() -> service.record(MEMBER_ID, "SKU-1001"))
                .isInstanceOf(ProductViewHistoryApiException.class)
                .hasFieldOrPropertyWithValue("code", "RCV-4042");

        verifyNoInteractions(productDao, dao);
    }

    // linked_tc: 기록 — 상품 미존재는 404 PRODUCT_NOT_FOUND(AC 확정 문답 리터럴 코드), UPSERT 호출 안 됨.
    @Test
    void record_productNotFound_throws404WithLiteralCodeWithoutUpsert() {
        when(memberDao.selectById(MEMBER_ID)).thenReturn(member(MEMBER_ID));
        when(productDao.selectBySku("SKU-9999")).thenReturn(null);

        assertThatThrownBy(() -> service.record(MEMBER_ID, "SKU-9999"))
                .isInstanceOf(ProductViewHistoryApiException.class)
                .hasFieldOrPropertyWithValue("code", "PRODUCT_NOT_FOUND");

        verify(dao, never()).recordView(any(), any(), any());
        verify(dao, never()).enforceRecentCap(any(), anyInt());
    }

    // linked_tc: 기록 성공 — 판정 순서(sku검증은 이미 통과 → 회원존재 → 상품존재 → upsert →
    // cap삭제)가 실제 호출 순서와 일치하는지, 고정 시계 now()로 recordView가 호출되는지.
    @Test
    void record_success_callsMemberThenProductThenUpsertThenCapInOrder() {
        when(memberDao.selectById(MEMBER_ID)).thenReturn(member(MEMBER_ID));
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001"));

        service.record(MEMBER_ID, "SKU-1001");

        InOrder order = inOrder(memberDao, productDao, dao);
        order.verify(memberDao).selectById(MEMBER_ID);
        order.verify(productDao).selectBySku("SKU-1001");
        order.verify(dao).recordView(MEMBER_ID, "SKU-1001", LocalDateTime.now(FIXED_CLOCK));
        order.verify(dao).enforceRecentCap(MEMBER_ID, 20);
    }

    // linked_tc: 목록 — DAO 결과가 items 봉투로 그대로 매핑된다.
    @Test
    void list_mapsFieldsIntoItemsEnvelope() {
        ProductViewHistoryView view = new ProductViewHistoryView();
        view.setSku("SKU-1001");
        view.setProductName("스탠딩 데스크");
        view.setPrice(390000L);
        view.setImageUrl("/images/products/sku-1001.svg");
        view.setSoldOut(false);
        view.setViewedAt(LocalDateTime.now(FIXED_CLOCK));
        when(dao.selectRecentlyViewed(MEMBER_ID, 20)).thenReturn(List.of(view));

        Map<String, Object> body = service.list(MEMBER_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("sku")).isEqualTo("SKU-1001");
        assertThat(items.get(0).get("productName")).isEqualTo("스탠딩 데스크");
        assertThat(items.get(0).get("price")).isEqualTo(390000L);
        assertThat(items.get(0).get("soldOut")).isEqualTo(false);
        assertThat(items.get(0).get("viewedAt")).isNotNull();
    }

    // linked_tc: 목록 0건 — 예외 없이 빈 배열(404 아님, 서비스 레벨 대응).
    @Test
    void list_empty_returnsEmptyItemsNotError() {
        when(dao.selectRecentlyViewed(MEMBER_ID, 20)).thenReturn(List.of());

        Map<String, Object> body = service.list(MEMBER_ID);

        assertThat((List<?>) body.get("items")).isEmpty();
    }
}
