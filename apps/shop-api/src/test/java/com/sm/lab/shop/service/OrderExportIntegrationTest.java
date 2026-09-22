// linked_func: FUNC-order-013
// spec: docs/00_FUNC/stories/STORY-FUNC-order-013.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 실 DB(MariaDB, sl_lab.ORDERS) 대상 통합 테스트 — LAB-104(FUNC-order-013).
 * 랩 시드 데이터(실측): 20260817-0001(M-0003,PLACED) · 20260817-0002(M-0002,CANCELED) ·
 * 20260816-0002(M-0001,PARTIAL_SHIPPED) · 20260816-0001(M-0002,PAID) · 20260815-0001(M-0001,DONE),
 * 전부 del_yn='N'. OrderDao.selectOrders를 그대로 재사용하므로 목록 API와 동일 필터·상시필터를
 * DB 레벨에서 실측한다(새 조회 규칙을 도입하지 않았음을 증거).
 * r2(D11, QA r1 FAIL 정정): 시드 건수 정확결합(hasSize) 단언을 불변식 단언으로 완화(권고5/필수4) —
 * 이 랩 DB는 SR-202/203 라운드에서 이미 주문이 추가 생성된 이력이 있어 정확 건수는 깨지기 쉽다.
 */
// SR-204 R-5: 인증 필터 도입 후 admin 키 기본 주입(AdminApiKeyTestConfig) — mockMvc.perform 호출이
// 401로 깨지지 않게 한다. 인증/IDOR 자체 검증은 ApiKeyAuthIntegrationTest에서 별도 수행.
@SpringBootTest
@AutoConfigureMockMvc
@Import(AdminApiKeyTestConfig.class)
class OrderExportIntegrationTest {

    private static final String TEST_ORDER_NO = "20260823-9901";
    private static final String MEMBER_ID = "M-0001";

    @Autowired
    private OrderService orderService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    void cleanUp() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("DELETE FROM ORDERS WHERE order_no = '" + TEST_ORDER_NO + "'");
        }
    }

    private static String csvText(byte[] csv) {
        return new String(csv, 3, csv.length - 3, StandardCharsets.UTF_8); // BOM 3바이트 스킵
    }

    private static List<String> dataLines(byte[] csv) {
        List<String> lines = new ArrayList<>(List.of(csvText(csv).split("\r\n")));
        return lines.subList(1, lines.size()); // 헤더 제외
    }

    // linked_tc: TC-FUNC-order-013-03
    // r2(D11, QA r1 필수4): hasSize(2) 정확결합 → 불변식(모든 행이 PLACED) + 알려진 시드 1건 포함으로 완화.
    @Test
    void exportCsv_statusFilter_returnsOnlyMatchingSeedOrder() {
        byte[] csv = orderService.exportCsv(null, "PLACED");
        List<String> lines = dataLines(csv);

        assertThat(lines).isNotEmpty();
        assertThat(lines).allSatisfy(line -> assertThat(line.split(",", -1)[2]).isEqualTo("PLACED"));
        assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("20260817-0001,M-0003,PLACED,"));
    }

    // linked_tc: TC-FUNC-order-013-04
    // r2(D11, QA r1 필수4): hasSize(3) 정확결합 → 불변식(모든 행이 해당 회원) + 알려진 시드 2건 포함으로 완화.
    @Test
    void exportCsv_memberIdFilter_returnsOnlyThatMembersSeedOrders() {
        byte[] csv = orderService.exportCsv("M-0002", null);
        List<String> lines = dataLines(csv);

        assertThat(lines).isNotEmpty();
        assertThat(lines).allSatisfy(line -> assertThat(line.split(",", -1)[1]).isEqualTo("M-0002"));
        assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("20260817-0002,M-0002,CANCELED,"));
        assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("20260816-0001,M-0002,PAID,"));
    }

    // linked_tc: TC-FUNC-order-013-01 (HTTP 레벨 — orderState 파라미터 이름 축 회귀, QA r1 FAIL 필수1)
    // 서비스 레이어 pass-through 테스트는 컨트롤러의 파라미터 이름 매핑 결함을 구조적으로 못 잡는다
    // (컨트롤러가 무엇을 읽든 서비스 목만 보면 통과). 실제 HTTP 요청 + 실 DB로 orderState=PLACED가
    // 정말로 필터링되는지 실측한다.
    @Test
    void export_httpLevel_orderStateParam_filtersToMatchingRowsOnly() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/orders/export").param("orderState", "PLACED"))
                .andExpect(status().isOk())
                .andReturn();

        List<String> lines = dataLines(result.getResponse().getContentAsByteArray());

        assertThat(lines).isNotEmpty();
        assertThat(lines).allSatisfy(line -> assertThat(line.split(",", -1)[2]).isEqualTo("PLACED"));
        assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("20260817-0001,M-0003,PLACED,"));
    }

    // linked_tc: TC-FUNC-order-013-02 (삭제 주문 제외 — 상시필터 del_yn='N' DB 레벨 실측)
    @Test
    void exportCsv_excludesLogicallyDeletedOrder() throws Exception {
        try (Connection conn = dataSource.getConnection(); Statement st = conn.createStatement()) {
            st.executeUpdate("INSERT INTO ORDERS (order_no, member_id, order_state, total_amount, del_yn) "
                    + "VALUES ('" + TEST_ORDER_NO + "', '" + MEMBER_ID + "', 'PLACED', 1000, 'Y')");
        }

        byte[] csv = orderService.exportCsv(MEMBER_ID, null);

        assertThat(csvText(csv)).doesNotContain(TEST_ORDER_NO);
    }
}
