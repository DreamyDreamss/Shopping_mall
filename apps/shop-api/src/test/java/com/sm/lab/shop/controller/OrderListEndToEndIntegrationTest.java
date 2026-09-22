// linked_func: FUNC-order-001
// spec: docs/00_FUNC/stories/STORY-FUNC-order-001.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.support.TestClocks;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * round4(재작업지시 3 — QA r13 CONCERNS 권고3): 계층별 mock 테스트(OrderDaoTest=실DB 단위,
 * OrderServiceTest=mock DAO, OrderViewControllerTest=mock 서비스)만으로는 컨트롤러→서비스→DAO→
 * 템플릿 전 구간의 배선 결함(모델 속성명 오타, 매퍼 미등록 등)이 잡히지 않는다. 이 테스트는
 * OrderService/OrderDao를 목으로 대체하지 않고 실제 스프링 컨텍스트 + 실 DB(MariaDB, sl_lab)로
 * GET /order/list를 끝까지 관통시켜, OrderViewController가 채운
 * {@code deliveryStatusByOrderNo} 모델 속성이 실제로 list.html 6번째 열("배송상태")까지
 * 렌더링되는지 확인한다. /order/** 는 ApiKeyAuthFilter 화이트리스트(SR-204)라 인증 설정 불필요.
 *
 * <p>랩 시드(실측, DB 직접 조회로 확인):
 * <ul>
 *   <li>회원 M-0001 — 주문 2건: 20260815-0001(배송 이력 1건, DELIVERED),
 *       20260816-0002(배송 이력 2건 — D-0816-1 SHIPPED/shipped_at 있음, D-0816-2 READY/shipped_at
 *       NULL). round4(재작업지시 2)로 "최신"을 배송 이력 생성 순서(delivery_no DESC)로 재정의했으므로
 *       더 나중에 채번된 D-0816-2(READY)가 최신 — 화면에는 READY가 표시되어야 한다.</li>
 *   <li>회원 M-0003 — 주문 1건: 20260817-0001(배송 이력 없음) → "-" 표시.</li>
 * </ul>
 * 페이지 크기(20)보다 각 회원의 주문 수가 적어 페이징 없이 1페이지에서 전량 확인 가능(DB 실측).
 *
 * <p>SR-300: 두 테스트 모두 startDate/endDate를 지정하지 않아 OrderService의 기본 조회창(오늘-30일
 * ~ 오늘)에 의존한다 — 시스템 시계를 그대로 쓰면 날짜가 흐르며 시드 주문일(2026-08-15~17)이 창
 * 밖으로 밀려 실패한다(SR-300 원인). 아래 {@link FixedClockTestConfig}가 '오늘'을 2026-08-20으로
 * 고정해 날짜와 무관하게 통과하게 한다 — 두 테스트 본문 자체는 바꾸지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OrderListEndToEndIntegrationTest {

    // SR-300 재작업(round2, QA CONCERNS 권고1 — 함정 경고): 주문 생성은 아직 시스템 시계다
    // (OrderService.create()는 이 SR 범위 밖 — raw LocalDate.now() 그대로). 고정 시계는 이
    // list() 기본 조회창에만 적용된다 — 이 컨텍스트 안에서 "주문 생성 → 날짜 없이 목록 조회"를
    // 하는 테스트를 새로 추가하면 ordered_at이 실제 오늘(고정창 밖, ~2026-08-20)이라 조용히
    // 0건이 나와 증상이 인가/필터 결함처럼 보일 수 있다(후속 SR에서 시계화 검토).
    @TestConfiguration
    static class FixedClockTestConfig {
        // 빈 이름을 ShopApiApplication의 "clock"과 다르게 둔다 — 같은 이름이면 스프링 부트 기본
        // 설정(빈 정의 오버라이딩 비허용)에서 BeanDefinitionOverrideException이 난다. @Primary로
        // 타입 기준 주입 시 이 빈이 선택되게 한다.
        @Bean
        @Primary
        Clock fixedTestClock() {
            return TestClocks.SEED_TODAY;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    // linked_tc: TC-FUNC-order-001-21
    @Test
    void orderList_realDbEndToEnd_rendersLatestDeliveryStateColumnForEachOrder() throws Exception {
        mockMvc.perform(get("/order/list").param("memberId", "M-0001"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("<th>배송상태</th>")))
                // 20260816-0002: 배송 이력 생성 순서상 최신은 D-0816-2(READY) — shipped_at DESC였다면
                // SHIPPED가 표시되어 이 단언이 실패했을 회귀 케이스(round4 재작업 핵심 검증 지점).
                .andExpect(content().string(containsString("20260816-0002")))
                .andExpect(content().string(containsString("READY")))
                // 20260815-0001: 배송 이력 1건뿐 — DELIVERED
                .andExpect(content().string(containsString("20260815-0001")))
                .andExpect(content().string(containsString("DELIVERED")));
    }

    // linked_tc: TC-FUNC-order-001-22
    @Test
    void orderList_realDbEndToEnd_showsDashWhenOrderHasNoDeliveryHistory() throws Exception {
        mockMvc.perform(get("/order/list").param("memberId", "M-0003"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("20260817-0001")))
                .andExpect(content().string(containsString("<td>-</td>")));
    }
}
