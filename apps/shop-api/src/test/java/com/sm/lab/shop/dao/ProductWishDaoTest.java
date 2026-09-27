package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.ProductWishView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.PRODUCT_WISHES) 대상 통합 테스트(SR-356 #2) — 매퍼 SQL 자체(INSERT IGNORE
 * 원자성·정렬·상품 조인·LIMIT/OFFSET)를 검증한다. Mockito로는 실 SQL 정합을 증명할 수 없다.
 *
 * <p>랩 데이터: M-0001 김실증, SKU-1001 스탠딩 데스크(가격 390000, 재고 12, 품절 아님), SKU-1002
 * 기계식 키보드(가격 129000, 재고 40, 품절 아님) — {@code CartDaoTest} 관례와 동일하게 기존 시드를
 * 재사용하고 신규 회원·상품을 만들지 않는다.
 *
 * <p>테스트 격리(사람 확인 코멘트) — 이 테스트가 삽입한 PRODUCT_WISHES 행(M-0001 + SKU-1001/
 * SKU-1002)만 {@code @AfterEach}에서 지운다. 다른 테스트·시드가 남긴 행은 건드리지 않는다 — 카운터·
 * 상한 테이블이 아니라 배송지처럼 전용 회원을 새로 만들 필요는 없고, PK 유니크(member_id, sku)
 * 자체가 각 테스트의 삽입/삭제를 서로 간섭하지 않게 막아 준다.
 */
@SpringBootTest
class ProductWishDaoTest {

    private static final String MEMBER_ID = "M-0001";
    // SR-356(#6) — countWishesBySku 다회원 케이스 전용 두 번째 회원(기존 시드, M-0001~M-0004).
    private static final String OTHER_MEMBER_ID = "M-0002";
    private static final String SKU_1 = "SKU-1001";
    private static final String SKU_2 = "SKU-1002";

    @Autowired
    private ProductWishDao dao;

    // round3(QA round2 권고 1, 사람 확정) — insertWish는 INSERT IGNORE(PK member_id+sku,
    // productWish.xml:9-12)라 이 4쌍(M-0001/M-0002 × SKU-1001/SKU-1002) 중 하나라도 잔류 행이
    // 있으면(예: 이전 실행이 @AfterEach를 못 돌고 중단된 경우) countWishesBySku 델타 단언 테스트의
    // insertWish가 0행으로 삼켜져 카운트가 baseline+2/baseline+1까지 오르지 않는 플레이크가 난다.
    // @AfterEach와 같은 4쌍만 선제 정리한다 — 정리 범위를 넓히지 않는다(다른 회원·다른 sku 행은
    // 건드리지 않는다).
    @BeforeEach
    void cleanUpBeforeEach() {
        dao.deleteWish(MEMBER_ID, SKU_1);
        dao.deleteWish(MEMBER_ID, SKU_2);
        dao.deleteWish(OTHER_MEMBER_ID, SKU_1);
        dao.deleteWish(OTHER_MEMBER_ID, SKU_2);
    }

    @AfterEach
    void cleanUp() {
        dao.deleteWish(MEMBER_ID, SKU_1);
        dao.deleteWish(MEMBER_ID, SKU_2);
        dao.deleteWish(OTHER_MEMBER_ID, SKU_1);
        dao.deleteWish(OTHER_MEMBER_ID, SKU_2);
    }

    // linked_tc: 등록 — INSERT IGNORE 원자성. 첫 호출은 1행, 중복 호출(PK 충돌)은 예외 없이 0행.
    @Test
    void insertWish_duplicateCall_noExceptionAndAffectsZeroRowsOnSecondCall() {
        int first = dao.insertWish(MEMBER_ID, SKU_1, LocalDateTime.now());
        int second = dao.insertWish(MEMBER_ID, SKU_1, LocalDateTime.now());

        assertThat(first).isEqualTo(1);
        assertThat(second).as("PK 충돌 — IGNORE가 흡수해 0행, 예외 없음").isEqualTo(0);
    }

    // linked_tc: 해제 — 존재하는 행은 실제로 삭제된다.
    @Test
    void deleteWish_existingRow_removesIt() {
        dao.insertWish(MEMBER_ID, SKU_1, LocalDateTime.now());

        int affected = dao.deleteWish(MEMBER_ID, SKU_1);

        assertThat(affected).isEqualTo(1);
        assertThat(dao.selectWishList(MEMBER_ID, 0, 20))
                .extracting(ProductWishView::getSku)
                .doesNotContain(SKU_1);
    }

    // linked_tc: 해제 — 존재하지 않는 행도 예외 없이 0행.
    @Test
    void deleteWish_nonExistingRow_noExceptionAffectsZeroRows() {
        int affected = dao.deleteWish(MEMBER_ID, "SKU-DOES-NOT-EXIST");

        assertThat(affected).isEqualTo(0);
    }

    // linked_tc: 목록 — created_at DESC 정렬 + 상품 조인 필드(productName/price/soldOut/wishedAt).
    // round2(QA CONCERNS 권고 3, 사람 확정) — M-0001 찜 목록 *전체*를 containsExactly로 단언하지
    // 않는다(다른 시드·동시 SR이 M-0001에 찜을 추가해도 이 항목이 넣은 두 SKU만 보면 깨지지 않게
    // filteredOn으로 좁힌다). LIMIT을 크게 잡아 이 테스트의 두 행이 페이지 밖으로 밀려나지 않게 한다.
    @Test
    void selectWishList_ordersByCreatedAtDescAndJoinsProductFields() {
        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.insertWish(MEMBER_ID, SKU_1, base.minusDays(1));
        dao.insertWish(MEMBER_ID, SKU_2, base);

        List<ProductWishView> items = dao.selectWishList(MEMBER_ID, 0, 1000);
        List<ProductWishView> mine = items.stream()
                .filter(v -> v.getSku().equals(SKU_1) || v.getSku().equals(SKU_2))
                .toList();

        assertThat(mine).extracting(ProductWishView::getSku)
                .as("created_at DESC — 최신 찜이 먼저(이 테스트가 넣은 두 SKU만 비교)")
                .containsExactly(SKU_2, SKU_1);

        ProductWishView latest = mine.get(0);
        assertThat(latest.getProductName()).isEqualTo("기계식 키보드");
        assertThat(latest.getPrice()).isEqualTo(129000L);
        assertThat(latest.isSoldOut()).isFalse();
        assertThat(latest.getWishedAt()).isNotNull();
    }

    // linked_tc: 목록 — LIMIT/OFFSET이 실제로 페이지를 가른다. round2(QA CONCERNS 권고 3) —
    // M-0001의 총 행 수(0/1번째)를 가정하지 않고, 이 테스트가 넣은 두 SKU의 실제 위치를 먼저
    // 찾은 뒤 그 위치에 LIMIT 1로 질의해 정확히 그 한 건만 나오는지 확인한다(다른 시드/동시 SR이
    // M-0001에 찜을 추가해도 깨지지 않는다).
    @Test
    void selectWishList_limitOffset_pagesResults() {
        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.insertWish(MEMBER_ID, SKU_1, base.minusDays(1));
        dao.insertWish(MEMBER_ID, SKU_2, base);

        List<String> skus = dao.selectWishList(MEMBER_ID, 0, 1000).stream()
                .map(ProductWishView::getSku).toList();
        int indexOfSku2 = skus.indexOf(SKU_2);
        int indexOfSku1 = skus.indexOf(SKU_1);
        assertThat(indexOfSku2).as("SKU_2(최신)가 SKU_1보다 먼저").isLessThan(indexOfSku1);

        List<ProductWishView> pageWithSku2 = dao.selectWishList(MEMBER_ID, indexOfSku2, 1);
        List<ProductWishView> pageWithSku1 = dao.selectWishList(MEMBER_ID, indexOfSku1, 1);

        assertThat(pageWithSku2).extracting(ProductWishView::getSku).containsExactly(SKU_2);
        assertThat(pageWithSku1).extracting(ProductWishView::getSku).containsExactly(SKU_1);
    }

    // linked_tc: round2(QA CONCERNS 권고 2, 재현 테스트) — created_at이 같은 초로 동률(tie)일 때
    // sku ASC tie-breaker(productWish.xml)가 순서를 결정적으로 만든다. SKU_1("SKU-1001") <
    // SKU_2("SKU-1002") 사전순이라 동일 created_at이면 SKU_1이 먼저 나와야 하고, LIMIT 1을 두 번
    // 호출해도 페이지 경계에서 같은 항목이 겹치거나 빠지지 않는다.
    @Test
    void selectWishList_sameCreatedAt_tieBreaksBySkuAscAndSplitsDeterministicallyAtPageBoundary() {
        LocalDateTime tie = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.insertWish(MEMBER_ID, SKU_1, tie);
        dao.insertWish(MEMBER_ID, SKU_2, tie);

        List<String> skus = dao.selectWishList(MEMBER_ID, 0, 1000).stream()
                .map(ProductWishView::getSku).toList();
        int indexOfSku1 = skus.indexOf(SKU_1);
        int indexOfSku2 = skus.indexOf(SKU_2);
        assertThat(indexOfSku1).as("동일 created_at — sku ASC tie-break로 SKU-1001이 먼저")
                .isLessThan(indexOfSku2);

        List<ProductWishView> firstPage = dao.selectWishList(MEMBER_ID, indexOfSku1, 1);
        List<ProductWishView> secondPage = dao.selectWishList(MEMBER_ID, indexOfSku2, 1);

        assertThat(firstPage).extracting(ProductWishView::getSku).containsExactly(SKU_1);
        assertThat(secondPage).extracting(ProductWishView::getSku).containsExactly(SKU_2);
    }

    // linked_tc: SR-356(#6, INF-ORD-009 wishCount) — 찜이 하나도 없는(존재하지 않는 sku 포함) sku는 0.
    @Test
    void countWishesBySku_noWishes_returnsZero() {
        assertThat(dao.countWishesBySku("SKU-DOES-NOT-EXIST")).isEqualTo(0);
    }

    // linked_tc: SR-356(#6) — 서로 다른 두 회원이 같은 sku를 찜하면 집계가 2 증가한다. 이 테스트는
    // 전용 두 번째 회원(M-0002)이 필요해 @AfterEach 정리 대상에 그 회원의 SKU_1 찜 행도 추가한다.
    // round2(QA CONCERNS 재작업 지시 1, 사람 확정) — countWishesBySku는 member_id 조건이 없는 sku
    // 전역 집계라 절대값(2) 단언은 다른 시드 회원(M-0003~M-0004)이 같은 sku를 찜한 잔류 행이 있으면
    // 영구 실패한다(사례집 SR-297 #1과 동일 실패 모드). sku FK가 PRODUCTS를 참조해 UUID sku로 바꿀
    // 수 없으므로, 삽입 전 기준값을 먼저 읽고 (기준값+2)로 비교하는 델타 단언으로 바꾼다 — 잔류 행이
    // 있어도 깨지지 않고, "두 회원이 같은 sku를 찜하면 2 증가"라는 검증력은 그대로 유지된다. 다른
    // 테스트가 남긴 행은 지우지 않는다(@AfterEach 정리 범위를 넓히지 않는다).
    @Test
    void countWishesBySku_twoMembersSameSku_returnsTwo() {
        int baseline = dao.countWishesBySku(SKU_1);

        dao.insertWish(MEMBER_ID, SKU_1, LocalDateTime.now());
        dao.insertWish(OTHER_MEMBER_ID, SKU_1, LocalDateTime.now());

        int count = dao.countWishesBySku(SKU_1);

        assertThat(count).as("삽입 전 기준값(잔류 행 포함) + 이 테스트가 넣은 두 건")
                .isEqualTo(baseline + 2);
    }

    // linked_tc: SR-356(#6) — 다른 sku의 찜 행은 이 sku의 집계에 섞이지 않는다(다른 sku로 시딩 후 구분 확인).
    // round2(QA CONCERNS 재작업 지시 1) — 위와 같은 이유로 절대값(1) 대신 기준값 + 1 델타 단언.
    @Test
    void countWishesBySku_differentSkuWishesDoNotLeakIntoCount() {
        int baselineSku1 = dao.countWishesBySku(SKU_1);
        int baselineSku2 = dao.countWishesBySku(SKU_2);

        dao.insertWish(MEMBER_ID, SKU_1, LocalDateTime.now());
        dao.insertWish(OTHER_MEMBER_ID, SKU_2, LocalDateTime.now());

        assertThat(dao.countWishesBySku(SKU_1)).as("SKU_1 기준값 + 1 — SKU_2 삽입은 섞이지 않음")
                .isEqualTo(baselineSku1 + 1);
        assertThat(dao.countWishesBySku(SKU_2)).as("SKU_2 기준값 + 1 — SKU_1 삽입은 섞이지 않음")
                .isEqualTo(baselineSku2 + 1);
    }
}
