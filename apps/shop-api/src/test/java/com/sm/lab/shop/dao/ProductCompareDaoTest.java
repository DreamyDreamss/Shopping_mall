package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.ProductCompareView;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, PRODUCT_COMPARES) 대상 통합 테스트(SR-360 #2) — 매퍼 SQL 자체(상한부 INSERT
 * IGNORE 원자성·정렬·상품 조인)를 검증한다. Mockito로는 실 SQL 정합을 증명할 수 없다.
 *
 * <p>랩 데이터: M-0001 김실증, SKU-1001~1004(스탠딩 데스크 외 3종, 실 카탈로그 전체) — {@code
 * ProductWishDaoTest} 관례와 동일하게 기존 시드를 재사용하고 신규 회원·상품을 만들지 않는다(캡
 * 경계 테스트만 예외 — 5번째 상품이 필요해 {@code @Sql} 픽스처를 쓴다).
 *
 * <p>테스트 격리(SR-356 #2 round3 재발 방지) — {@code @BeforeEach}/{@code @AfterEach} 양쪽에서
 * 4개 seed sku + 임시 5번째 sku를 선제/사후 정리한다. 이전 실행이 중단돼 남긴 잔류 행이 {@code
 * INSERT IGNORE}를 0행으로 삼켜 델타 단언이 플레이키해지는 것을 방지한다.
 */
@SpringBootTest
class ProductCompareDaoTest {

    private static final String MEMBER_ID = "M-0001";
    // SR-363.1 — 존재하지 않는 회원. selectCompareList가 MEMBERS를 조인·FK 참조하지 않아(확인함)
    // 이 값으로도 0행이 결정적으로 나온다. M-0001을 재사용하지 않는 이유: 같은 클래스의 다른
    // 테스트가 매번 M-0001의 4개 seed sku를 @BeforeEach/@AfterEach로 정리하므로, 신규 테스트가
    // M-0001을 같이 쓰면 실행 순서·타이밍에 따라 다른 테스트의 잔류/정리 상태에 의존하게 된다
    // (사례집 SR-356 #6과 같은 계열의 플레이키 위험). M-9999는 아무 테스트도 INSERT하지 않으므로
    // 정리 대상 자체가 없다.
    private static final String EMPTY_MEMBER_ID = "M-9999";
    private static final String SKU_1 = "SKU-1001";
    private static final String SKU_2 = "SKU-1002";
    private static final String SKU_3 = "SKU-1003";
    private static final String SKU_4 = "SKU-1004";
    private static final String SKU_5TH = "SKU-TEST-CMP-5TH";

    @Autowired
    private ProductCompareDao dao;

    @BeforeEach
    void cleanUpBeforeEach() {
        cleanUp();
    }

    @AfterEach
    void cleanUp() {
        dao.deleteCompare(MEMBER_ID, SKU_1);
        dao.deleteCompare(MEMBER_ID, SKU_2);
        dao.deleteCompare(MEMBER_ID, SKU_3);
        dao.deleteCompare(MEMBER_ID, SKU_4);
        dao.deleteCompare(MEMBER_ID, SKU_5TH);
    }

    // linked_tc: 담기 — INSERT IGNORE 원자성. 첫 호출은 1행, 중복 호출(PK 충돌)은 예외 없이 0행 +
    // existsCompare로 원인(존재) 구분.
    @Test
    void insertCompareIfUnderLimit_duplicateCall_noExceptionAndAffectsZeroRowsOnSecondCall() {
        int first = dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_1, LocalDateTime.now());
        int second = dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_1, LocalDateTime.now());

        assertThat(first).isEqualTo(1);
        assertThat(second).as("PK 충돌 — IGNORE가 흡수해 0행, 예외 없음").isEqualTo(0);
        assertThat(dao.existsCompare(MEMBER_ID, SKU_1)).isTrue();
    }

    // linked_tc: 빼기 — 존재하는 행은 실제로 삭제된다.
    @Test
    void deleteCompare_existingRow_removesIt() {
        dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_1, LocalDateTime.now());

        int affected = dao.deleteCompare(MEMBER_ID, SKU_1);

        assertThat(affected).isEqualTo(1);
        assertThat(dao.existsCompare(MEMBER_ID, SKU_1)).isFalse();
    }

    // linked_tc: 빼기 — 존재하지 않는 행도 예외 없이 0행.
    @Test
    void deleteCompare_nonExistingRow_noExceptionAffectsZeroRows() {
        int affected = dao.deleteCompare(MEMBER_ID, "SKU-DOES-NOT-EXIST");

        assertThat(affected).isEqualTo(0);
    }

    // linked_tc: 캡 경계 — 실 카탈로그 4개(SKU-1001~1004)를 순서대로 담아 4번째까지 전부 1행 →
    // 카탈로그에 없는 5번째 sku(픽스처)에 대한 시도는 0행 + existsCompare false(=진짜 상한 초과,
    // 사례집 SR-353 #1 — 판별력 없는 테스트 방지를 위해 실제 5번째 상품을 만들어 상한 로직을
    // 실제로 행사한다). @Transactional 자동 롤백(insert-out-of-stock.sql 관례).
    @Test
    @Transactional
    @Sql("classpath:sql/insert-fifth-product-for-compare-cap-test.sql")
    void insertCompareIfUnderLimit_capBoundary_fourthSucceedsFifthRejected() {
        assertThat(dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_1, LocalDateTime.now())).isEqualTo(1);
        assertThat(dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_2, LocalDateTime.now())).isEqualTo(1);
        assertThat(dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_3, LocalDateTime.now())).isEqualTo(1);
        assertThat(dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_4, LocalDateTime.now())).isEqualTo(1);

        int fifthAffected = dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_5TH, LocalDateTime.now());

        assertThat(fifthAffected).as("4개 상한 — 5번째는 0행").isEqualTo(0);
        assertThat(dao.existsCompare(MEMBER_ID, SKU_5TH)).as("0행 원인이 상한 초과 — 실제 삽입 안 됨").isFalse();
    }

    // linked_tc: 목록 — created_at ASC 정렬(담은 시각 오름차순) + 상품 조인 필드(productName/
    // price/soldOut/comparedAt).
    @Test
    void selectCompareList_ordersByCreatedAtAscThenSkuAscAndJoinsProductFields() {
        LocalDateTime base = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_1, base.minusDays(1));
        dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_2, base);

        List<ProductCompareView> items = dao.selectCompareList(MEMBER_ID);
        List<ProductCompareView> mine = items.stream()
                .filter(v -> v.getSku().equals(SKU_1) || v.getSku().equals(SKU_2))
                .toList();

        assertThat(mine).extracting(ProductCompareView::getSku)
                .as("created_at ASC — 먼저 담은 것이 먼저(이 테스트가 넣은 두 SKU만 비교)")
                .containsExactly(SKU_1, SKU_2);

        ProductCompareView first = mine.get(0);
        assertThat(first.getProductName()).isEqualTo("스탠딩 데스크");
        assertThat(first.getPrice()).isEqualTo(390000L);
        assertThat(first.isSoldOut()).isFalse();
        assertThat(first.getComparedAt()).isNotNull();
    }

    // SR-363.1 round2(QA CONCERNS medium 재작업) — round1 버전은 PRODUCT_COMPARES가 전역 0행이라
    // WHERE c.member_id 절을 통째로 지워도 통과하는 판별력 없는 테스트였다(사례집 SR-353 #1 —
    // 테이블이 애초에 비어 있는 것이 먼저 게이팅). 다른 회원(M-0001)의 행을 1건 먼저 심어 테이블을
    // 비지 않은 상태로 만든 뒤 대상 회원(M-9999, 존재하지 않는 회원)으로 조회해야 회원 필터가 실제로
    // 행사된다. 심은 SKU_1 행은 클래스의 @AfterEach(cleanUp)가 MEMBER_ID 기준으로 이미 정리한다.
    @Test
    void selectCompareList_noRowsForMember_returnsEmptyList() {
        dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_1, LocalDateTime.now());

        List<ProductCompareView> result = dao.selectCompareList(EMPTY_MEMBER_ID);

        assertThat(result).isEmpty();
    }

    // linked_tc: 동일 created_at 동률(tie)일 때 sku ASC tie-breaker(productCompare.xml)가 순서를
    // 결정적으로 만든다(ProductWishDaoTest round2 관례와 동일).
    @Test
    void selectCompareList_sameCreatedAt_tieBreaksBySkuAsc() {
        LocalDateTime tie = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_2, tie);
        dao.insertCompareIfUnderLimit(MEMBER_ID, SKU_1, tie);

        List<String> skus = dao.selectCompareList(MEMBER_ID).stream()
                .map(ProductCompareView::getSku).toList();
        int indexOfSku1 = skus.indexOf(SKU_1);
        int indexOfSku2 = skus.indexOf(SKU_2);

        assertThat(indexOfSku1).as("동일 created_at — sku ASC tie-break로 SKU-1001이 먼저")
                .isLessThan(indexOfSku2);
    }
}
