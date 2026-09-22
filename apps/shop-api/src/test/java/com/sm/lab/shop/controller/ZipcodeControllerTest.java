// linked_func: FUNC-member-012
// spec: docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Zipcode;
import com.sm.lab.shop.service.ZipcodeApiException;
import com.sm.lab.shop.service.ZipcodeService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-235(FUNC-member-012) — GET {@code /api/zipcodes} 웹 계층 단위 테스트({@code
 * controller-has-test} must 충족). 서비스는 mock — HTTP 계약(상태코드·코드·메시지·응답 셰이프)만
 * 단언한다.
 *
 * <p><b>무인증 401은 이 클래스에서 다루지 않는다</b> — {@link AdminApiKeyTestConfig}를 이 슬라이스
 * 전역에 import하면 {@code MockMvcBuilderCustomizer}가 모든 요청에 기본 admin 헤더를
 * 병합하므로(개별 요청이 다른 값으로 덮어쓸 수는 있어도 "헤더 자체가 없는" 상태를 만들 수는
 * 없다) 무인증 401을 여기서 재현할 수 없다({@code MemberAddressControllerTest}·{@code
 * CartControllerTest}와 동일한 house 판단). 무인증 401은 실 서버(RANDOM_PORT) 기반 {@code
 * ApiKeyAuthIntegrationTest}(이 FUNC이 추가한 {@code zipcodesRoute_withoutApiKey_returns401Unauthorized})
 * 가 검증한다 — 계획과 다르게 간 지점(STORY "Dev 기록" 참고).
 */
@WebMvcTest(ZipcodeController.class)
@Import(AdminApiKeyTestConfig.class)
class ZipcodeControllerTest {

    private static final String BASE_URL = "/api/zipcodes";
    private static final String MEMBER_KEY = "lab-member-0001-key";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ZipcodeService zipcodeService;

    private static Zipcode sample(String zipcode, String roadAddress, String sido, String sigungu) {
        Zipcode z = new Zipcode();
        z.setZipcode(zipcode);
        z.setRoadAddress(roadAddress);
        z.setSido(sido);
        z.setSigungu(sigungu);
        return z;
    }

    // linked_tc: TC-FUNC-member-012-01
    @Test
    void search_returnsItemsEnvelopeWithShape() throws Exception {
        when(zipcodeService.search("강남")).thenReturn(List.of(
                sample("10001", "서울특별시 강남구 테헤란로 16길 10", "서울특별시", "강남구"),
                sample("10002", "서울특별시 강남구 테헤란로 2길 33", "서울특별시", "강남구")));

        mockMvc.perform(get(BASE_URL).param("q", "강남").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].zipcode").value("10001"))
                .andExpect(jsonPath("$.items[0].roadAddress").value("서울특별시 강남구 테헤란로 16길 10"))
                .andExpect(jsonPath("$.items[0].sido").value("서울특별시"))
                .andExpect(jsonPath("$.items[0].sigungu").value("강남구"));

        verify(zipcodeService).search("강남");
    }

    // linked_tc: TC-FUNC-member-012-02 — 결과 0건도 404가 아니라 200 빈 배열.
    @Test
    void search_noResults_returns200WithEmptyItems() throws Exception {
        when(zipcodeService.search("존재하지않는주소")).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL).param("q", "존재하지않는주소").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    // linked_tc: TC-FUNC-member-012-03 — 검색어 2자 미만.
    @Test
    void search_serviceThrowsTooShort_returns400() throws Exception {
        when(zipcodeService.search("강")).thenThrow(new ZipcodeApiException(HttpStatus.BAD_REQUEST,
                "MBR-4202", "검색어는 최소 2자 이상이어야 합니다"));

        mockMvc.perform(get(BASE_URL).param("q", "강").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MBR-4202"));
    }

    // linked_tc: TC-FUNC-member-012-04 — 검색어 50자 초과(사람 수정 (1)).
    @Test
    void search_serviceThrowsTooLong_returns400() throws Exception {
        String tooLong = "가".repeat(51);
        when(zipcodeService.search(tooLong)).thenThrow(new ZipcodeApiException(HttpStatus.BAD_REQUEST,
                "MBR-4202", "검색어는 최대 50자까지 입력할 수 있습니다"));

        mockMvc.perform(get(BASE_URL).param("q", tooLong).header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MBR-4202"));
    }

    // linked_tc: TC-FUNC-member-012-05 — q 파라미터 자체가 없어도 이 프로젝트 {code,message}
    // 봉투로 400이 나가는지 확인(Spring 기본 "필수 파라미터 누락" 오류가 아님 — @RequestParam(required=false)).
    @Test
    void search_missingQParam_passesNullToServiceAndReturns400() throws Exception {
        when(zipcodeService.search(isNull())).thenThrow(new ZipcodeApiException(HttpStatus.BAD_REQUEST,
                "MBR-4202", "검색어는 최소 2자 이상이어야 합니다"));

        mockMvc.perform(get(BASE_URL).header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MBR-4202"));

        verify(zipcodeService).search(isNull());
    }

    // linked_tc: TC-FUNC-member-012-06 — 회원 스코프 강제가 없는 엔드포인트임을 확인(member
    // 키로도 그대로 통과 — MemberAddressControllerTest의 admin 키 400 케이스와 대칭되는 회귀).
    @Test
    void search_memberApiKey_passesThroughWithout403() throws Exception {
        when(zipcodeService.search(eq("강남"))).thenReturn(List.of());

        mockMvc.perform(get(BASE_URL).param("q", "강남").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk());
    }
}
