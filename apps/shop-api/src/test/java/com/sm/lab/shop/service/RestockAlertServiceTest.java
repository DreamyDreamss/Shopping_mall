package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.RestockAlertDao;
import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.domain.RestockAlertView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SR-364(#1) — {@link RestockAlertService} 단위 테스트(Mockito, DAO 목업). HTTP 계약은
 * {@code RestockAlertControllerTest}가, SQL 자체는 {@code RestockAlertDaoTest}가 맡는다 — 이
 * 테스트는 STORY "테스트" 절이 지정한 경계 조건(존재 판정·품절 여부·멱등)만 본다.
 */
class RestockAlertServiceTest {

    private static final String MEMBER_ID = "M-0001";
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-29T00:00:00Z"), ZoneOffset.UTC);

    private RestockAlertDao dao;
    private ProductDao productDao;
    private MemberDao memberDao;
    private RestockAlertService service;

    @BeforeEach
    void setUp() {
        dao = mock(RestockAlertDao.class);
        productDao = mock(ProductDao.class);
        memberDao = mock(MemberDao.class);
        service = new RestockAlertService(dao, productDao, memberDao, FIXED_CLOCK);
    }

    private static Product product(String sku, int stockQty) {
        Product p = new Product();
        p.setSku(sku);
        p.setStockQty(stockQty);
        return p;
    }

    private static Member member(String memberId) {
        Member m = new Member();
        m.setMemberId(memberId);
        return m;
    }

    private static RestockAlertView view(String sku, LocalDateTime createdAt) {
        RestockAlertView v = new RestockAlertView();
        v.setSku(sku);
        v.setCreatedAt(createdAt);
        return v;
    }

    // linked_tc: sku 공백 — DAO 접근 전 400으로 차단한다.
    @Test
    void register_skuBlank_throws400BeforeAnyDaoCall() {
        assertThatThrownBy(() -> service.register(MEMBER_ID, " "))
                .isInstanceOf(RestockAlertApiException.class)
                .hasFieldOrPropertyWithValue("code", "RSA-4001");

        verifyNoInteractions(productDao, dao);
    }

    // linked_tc: 상품 미존재 — 404 PRODUCT_NOT_FOUND, insertAlert 호출 안 됨.
    @Test
    void register_productNotFound_throws404WithoutInsert() {
        when(productDao.selectBySku("SKU-9999")).thenReturn(null);

        assertThatThrownBy(() -> service.register(MEMBER_ID, "SKU-9999"))
                .isInstanceOf(RestockAlertApiException.class)
                .hasFieldOrPropertyWithValue("code", "PRODUCT_NOT_FOUND");

        verify(dao, never()).insertAlert(any(), any(), any());
    }

    // linked_tc: 품절이 아닌 상품(재고 있음) — 409 NOT_SOLD_OUT, insertAlert 호출 안 됨.
    @Test
    void register_stockQtyPositive_throws409NotSoldOutWithoutInsert() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 12));

        assertThatThrownBy(() -> service.register(MEMBER_ID, "SKU-1001"))
                .isInstanceOf(RestockAlertApiException.class)
                .hasFieldOrPropertyWithValue("code", "NOT_SOLD_OUT");

        verify(dao, never()).insertAlert(any(), any(), any());
    }

    // linked_tc: 최초 신청 — 품절 상품, insertAlert 1행 — 정상 반환(예외 없음), 회원 재조회 없음.
    @Test
    void register_soldOutProduct_firstRequest_insertsAlertWithFixedClockNow() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 0));
        when(dao.insertAlert(any(), any(), any())).thenReturn(1);

        assertThatCode(() -> service.register(MEMBER_ID, "SKU-1001")).doesNotThrowAnyException();

        verify(dao).insertAlert(MEMBER_ID, "SKU-1001", LocalDateTime.now(FIXED_CLOCK));
        verify(memberDao, never()).selectById(any());
    }

    // linked_tc: 재신청(이미 신청한 상품 다시 신청) — DAO가 0행(IGNORE)을 반환하면 회원 존재를
    // 재조회하고(존재 확인), 회원이 실재하므로 예외를 던지지 않는다(멱등 200 요구, 컨트롤러가
    // 항상 같은 응답으로 매핑).
    @Test
    void register_alreadyRequested_daoReturnsZeroRows_memberExists_stillSucceeds() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 0));
        when(dao.insertAlert(any(), any(), any())).thenReturn(0);
        when(memberDao.selectById(MEMBER_ID)).thenReturn(member(MEMBER_ID));

        assertThatCode(() -> service.register(MEMBER_ID, "SKU-1001")).doesNotThrowAnyException();

        verify(dao).insertAlert(MEMBER_ID, "SKU-1001", LocalDateTime.now(FIXED_CLOCK));
        verify(memberDao).selectById(MEMBER_ID);
    }

    // linked_tc: admin 키가 실재하지 않는 memberId로 신청을 시도하면 INSERT IGNORE가 member_id
    // FK 위반을 조용히 흡수해 0행을 반환한다(MariaDB IGNORE 의미). 0행이고 회원도 존재하지 않으면
    // "저장 안 됐는데 200"이 아니라 404 RSA-4042를 던진다.
    @Test
    void register_memberDoesNotExist_daoReturnsZeroRows_throws404WithMemberNotFoundCode() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001", 0));
        when(dao.insertAlert(any(), any(), any())).thenReturn(0);
        when(memberDao.selectById("M-9999")).thenReturn(null);

        assertThatThrownBy(() -> service.register("M-9999", "SKU-1001"))
                .isInstanceOf(RestockAlertApiException.class)
                .hasFieldOrPropertyWithValue("code", "RSA-4042");
    }

    // linked_tc: SR-364(#2) — dao가 반환한 순서 그대로 items에 매핑되는지, dao.selectAlertList
    // 인자 검증.
    @Test
    void list_returnsItemsMappedFromDaoInDaoOrder() {
        LocalDateTime newer = LocalDateTime.of(2026, 9, 29, 10, 0, 0);
        LocalDateTime older = LocalDateTime.of(2026, 9, 28, 10, 0, 0);
        when(dao.selectAlertList(MEMBER_ID)).thenReturn(List.of(view("SKU-1002", newer), view("SKU-1001", older)));

        Map<String, Object> body = service.list(MEMBER_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items).hasSize(2);
        assertThat(items.get(0)).containsEntry("sku", "SKU-1002").containsEntry("createdAt", newer);
        assertThat(items.get(1)).containsEntry("sku", "SKU-1001").containsEntry("createdAt", older);
        verify(dao).selectAlertList(MEMBER_ID);
    }

    // linked_tc: dao가 빈 목록을 반환하면 items도 빈 배열(0건이 오류가 아님).
    @Test
    void list_daoReturnsEmptyList_returnsEmptyItems() {
        when(dao.selectAlertList(MEMBER_ID)).thenReturn(List.of());

        Map<String, Object> body = service.list(MEMBER_ID);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        assertThat(items).isEmpty();
    }
}
