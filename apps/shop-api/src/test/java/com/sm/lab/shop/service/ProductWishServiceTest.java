package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductWishDao;
import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SR-356(#2) — {@link ProductWishService} 단위 테스트(Mockito, DAO 목업). HTTP 계약은
 * {@code ProductWishControllerTest}가, SQL 자체는 {@code ProductWishDaoTest}가 맡는다 — 이
 * 테스트는 STORY "테스트" 절이 지정한 경계 조건(존재 판정·멱등·page 파싱·offset 계산)만 본다.
 */
class ProductWishServiceTest {

    private static final String MEMBER_ID = "M-0001";
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-27T00:00:00Z"), ZoneOffset.UTC);

    private ProductWishDao dao;
    private ProductDao productDao;
    private MemberDao memberDao;
    private ProductWishService service;

    @BeforeEach
    void setUp() {
        dao = mock(ProductWishDao.class);
        productDao = mock(ProductDao.class);
        memberDao = mock(MemberDao.class);
        service = new ProductWishService(dao, productDao, memberDao, FIXED_CLOCK);
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

    // linked_tc: 등록 — sku 공백은 DAO 접근 전 400으로 차단한다.
    @Test
    void register_skuBlank_throws400BeforeAnyDaoCall() {
        assertThatThrownBy(() -> service.register(MEMBER_ID, " "))
                .isInstanceOf(ProductWishApiException.class)
                .hasFieldOrPropertyWithValue("code", "WSH-4001");

        verifyNoInteractions(productDao, dao);
    }

    // linked_tc: 등록 — 상품 미존재는 404, insertWish 호출 안 됨.
    @Test
    void register_productNotFound_throws404WithoutInsert() {
        when(productDao.selectBySku("SKU-9999")).thenReturn(null);

        assertThatThrownBy(() -> service.register(MEMBER_ID, "SKU-9999"))
                .isInstanceOf(ProductWishApiException.class)
                .hasFieldOrPropertyWithValue("code", "WSH-4041");

        verify(dao, never()).insertWish(any(), any(), any());
    }

    // linked_tc: 등록 성공 — 고정 시계 now()로 insertWish 호출(1행 — 신규 삽입이라 회원 재조회 없음).
    @Test
    void register_success_insertsWishWithFixedClockNow() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001"));
        when(dao.insertWish(any(), any(), any())).thenReturn(1);

        service.register(MEMBER_ID, "SKU-1001");

        verify(dao).insertWish(MEMBER_ID, "SKU-1001", LocalDateTime.now(FIXED_CLOCK));
        verify(memberDao, never()).selectById(any());
    }

    // linked_tc: 재등록(이미 찜한 상품 다시 찜) — DAO가 0행(IGNORE)을 반환하면 회원 존재를
    // 재조회하고(존재 확인), 회원이 실재하므로 예외를 던지지 않는다(멱등 200 요구, 컨트롤러가
    // 항상 같은 응답으로 매핑). round2(QA CONCERNS 권고 1) — 0행 원인 재조회 경로.
    @Test
    void register_alreadyWished_daoReturnsZeroRows_memberExists_stillSucceeds() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001"));
        when(dao.insertWish(any(), any(), any())).thenReturn(0);
        when(memberDao.selectById(MEMBER_ID)).thenReturn(member(MEMBER_ID));

        service.register(MEMBER_ID, "SKU-1001");

        verify(dao).insertWish(eq(MEMBER_ID), eq("SKU-1001"), any(LocalDateTime.class));
        verify(memberDao).selectById(MEMBER_ID);
    }

    // linked_tc: round2(QA CONCERNS 권고 1, 재현 테스트) — admin 키가 실재하지 않는 memberId로
    // 등록을 시도하면 INSERT IGNORE가 member_id FK 위반을 조용히 흡수해 0행을 반환한다(MariaDB
    // IGNORE 의미). 0행이고 회원도 존재하지 않으면 "저장 안 됐는데 200"이 아니라 404
    // WSH-4042(신규 코드)를 던진다.
    @Test
    void register_memberDoesNotExist_daoReturnsZeroRows_throws404WithMemberNotFoundCode() {
        when(productDao.selectBySku("SKU-1001")).thenReturn(product("SKU-1001"));
        when(dao.insertWish(any(), any(), any())).thenReturn(0);
        when(memberDao.selectById("M-9999")).thenReturn(null);

        assertThatThrownBy(() -> service.register("M-9999", "SKU-1001"))
                .isInstanceOf(ProductWishApiException.class)
                .hasFieldOrPropertyWithValue("code", "WSH-4042");
    }

    // linked_tc: 해제 — DAO가 0행(찜 안 한 상품)을 반환해도 예외 없음(항상 성공).
    @Test
    void unregister_daoReturnsZero_doesNotThrow() {
        when(dao.deleteWish(MEMBER_ID, "SKU-1001")).thenReturn(0);

        service.unregister(MEMBER_ID, "SKU-1001");

        verify(dao).deleteWish(MEMBER_ID, "SKU-1001");
    }

    // linked_tc: 해제 — DAO가 1행(실제 존재)을 반환해도 동일하게 예외 없음.
    @Test
    void unregister_daoReturnsOne_doesNotThrow() {
        when(dao.deleteWish(MEMBER_ID, "SKU-1001")).thenReturn(1);

        service.unregister(MEMBER_ID, "SKU-1001");

        verify(dao).deleteWish(MEMBER_ID, "SKU-1001");
    }

    // linked_tc: page 경계 — 0/음수/비숫자는 400.
    @Test
    void list_pageZero_throws400() {
        assertThatThrownBy(() -> service.list(MEMBER_ID, "0"))
                .isInstanceOf(ProductWishApiException.class)
                .hasFieldOrPropertyWithValue("code", "WSH-4001");
    }

    @Test
    void list_pageNegative_throws400() {
        assertThatThrownBy(() -> service.list(MEMBER_ID, "-1"))
                .isInstanceOf(ProductWishApiException.class)
                .hasFieldOrPropertyWithValue("code", "WSH-4001");
    }

    @Test
    void list_pageNonNumeric_throws400() {
        assertThatThrownBy(() -> service.list(MEMBER_ID, "abc"))
                .isInstanceOf(ProductWishApiException.class)
                .hasFieldOrPropertyWithValue("code", "WSH-4001");
    }

    // linked_tc: page=1 → offset=0, 응답에 page/size 포함.
    @Test
    void list_pageOne_offsetZero() {
        when(dao.selectWishList(eq(MEMBER_ID), eq(0L), eq(20))).thenReturn(List.of());

        Map<String, Object> body = service.list(MEMBER_ID, "1");

        assertThat(body.get("page")).isEqualTo(1);
        assertThat(body.get("size")).isEqualTo(20);
        verify(dao).selectWishList(MEMBER_ID, 0L, 20);
    }

    // linked_tc: page=2 → offset=20.
    @Test
    void list_pageTwo_offsetTwenty() {
        when(dao.selectWishList(eq(MEMBER_ID), eq(20L), eq(20))).thenReturn(List.of());

        service.list(MEMBER_ID, "2");

        verify(dao).selectWishList(MEMBER_ID, 20L, 20);
    }

    // 사례집 SR-351 #1 round1→round2 재발 방지 — int 곱셈 오버플로 경계값(Integer.MAX_VALUE).
    // (long) 캐스팅이 곱셈보다 먼저이면 이 값에서도 offset이 음수로 랩어라운드되지 않는다.
    @Test
    void list_pageIntegerMaxValue_offsetDoesNotOverflowNegative() {
        long expectedOffset = (long) (Integer.MAX_VALUE - 1) * 20; // (page-1)*20
        when(dao.selectWishList(eq(MEMBER_ID), eq(expectedOffset), eq(20))).thenReturn(List.of());

        service.list(MEMBER_ID, String.valueOf(Integer.MAX_VALUE));

        verify(dao).selectWishList(MEMBER_ID, expectedOffset, 20);
        assertThat(expectedOffset).isNotNegative();
    }
}
