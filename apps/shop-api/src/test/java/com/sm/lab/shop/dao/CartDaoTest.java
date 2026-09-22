// linked_func: FUNC-order-011
// spec: docs/00_FUNC/stories/STORY-FUNC-order-011.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.CartItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.CART_ITEMS) 대상 통합 테스트.
 * 랩 데이터: M-0001 김실증, SKU-1001 스탠딩데스크(재고12), SKU-1002 기계식키보드(재고40).
 * CART_ITEMS는 순수 신규 데이터라 각 테스트 후 삽입분을 정리한다(다른 테스트·상태 오염 방지).
 */
@SpringBootTest
class CartDaoTest {

    private static final String MEMBER_ID = "M-0001";
    private static final String SKU = "SKU-1001";

    @Autowired
    private CartDao cartDao;

    @AfterEach
    void cleanUp() {
        cartDao.deleteItem(MEMBER_ID, SKU);
    }

    @Test
    void insertItem_then_selectItem_returnsJoinedProductInfo() {
        cartDao.insertItem(MEMBER_ID, SKU, 2);

        CartItem item = cartDao.selectItem(MEMBER_ID, SKU);

        assertThat(item).isNotNull();
        assertThat(item.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(item.getSku()).isEqualTo(SKU);
        assertThat(item.getProductName()).isEqualTo("스탠딩 데스크");
        assertThat(item.getPrice()).isEqualTo(390000L);
        assertThat(item.getQty()).isEqualTo(2);
        assertThat(item.getAddedAt()).isNotNull();
    }

    @Test
    void selectItem_notExisting_returnsNull() {
        CartItem item = cartDao.selectItem(MEMBER_ID, "SKU-9999");

        assertThat(item).isNull();
    }

    @Test
    void selectItems_returnsAllItemsForMemberJoinedWithProduct() {
        cartDao.insertItem(MEMBER_ID, SKU, 1);
        cartDao.insertItem(MEMBER_ID, "SKU-1002", 3);

        List<CartItem> items = cartDao.selectItems(MEMBER_ID);

        assertThat(items).extracting(CartItem::getSku).containsExactlyInAnyOrder(SKU, "SKU-1002");
        assertThat(items).allMatch(it -> MEMBER_ID.equals(it.getMemberId()));

        cartDao.deleteItem(MEMBER_ID, "SKU-1002");
    }

    // r2 신규(FUNC-order-012 재작업 필수1): 체크아웃 동시성 차단용 잠금 조회. 상품 조인 없이
    // memberId/sku/qty/addedAt만 채운다(재고 판정은 CartService 사전 스윕이 productDao로 별도 수행).
    @Test
    void selectItemsForUpdate_returnsAllItemsForMemberWithoutProductJoin() {
        cartDao.insertItem(MEMBER_ID, SKU, 1);
        cartDao.insertItem(MEMBER_ID, "SKU-1002", 3);

        List<CartItem> items = cartDao.selectItemsForUpdate(MEMBER_ID);

        assertThat(items).extracting(CartItem::getSku).containsExactlyInAnyOrder(SKU, "SKU-1002");
        assertThat(items).allMatch(it -> MEMBER_ID.equals(it.getMemberId()));
        assertThat(items).allMatch(it -> it.getQty() > 0);

        cartDao.deleteItem(MEMBER_ID, "SKU-1002");
    }

    @Test
    void updateQty_updatesExistingRow() {
        cartDao.insertItem(MEMBER_ID, SKU, 1);

        int affected = cartDao.updateQty(MEMBER_ID, SKU, 5);

        assertThat(affected).isEqualTo(1);
        assertThat(cartDao.selectItem(MEMBER_ID, SKU).getQty()).isEqualTo(5);
    }

    @Test
    void deleteItem_removesRow() {
        cartDao.insertItem(MEMBER_ID, SKU, 1);

        int affected = cartDao.deleteItem(MEMBER_ID, SKU);

        assertThat(affected).isEqualTo(1);
        assertThat(cartDao.selectItem(MEMBER_ID, SKU)).isNull();
    }

    // round2(QA FAIL round1 필수1 재작업): 담기 합산 원자 UPSERT — INSERT..ON DUPLICATE KEY UPDATE.
    @Test
    void upsertMergeQty_newItem_insertsWithGivenQty() {
        cartDao.upsertMergeQty(MEMBER_ID, SKU, 3);

        assertThat(cartDao.selectItem(MEMBER_ID, SKU).getQty()).isEqualTo(3);
    }

    @Test
    void upsertMergeQty_existingItem_mergesQtyAtomically() {
        cartDao.insertItem(MEMBER_ID, SKU, 2);

        cartDao.upsertMergeQty(MEMBER_ID, SKU, 5);

        assertThat(cartDao.selectItem(MEMBER_ID, SKU).getQty()).isEqualTo(7);
    }
}
