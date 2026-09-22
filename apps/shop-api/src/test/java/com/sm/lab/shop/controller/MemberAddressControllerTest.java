// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.MemberAddress;
import com.sm.lab.shop.service.MemberAddressApiException;
import com.sm.lab.shop.service.MemberAddressService;
import com.sm.lab.shop.support.AdminApiKeyTestConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SR-235(FUNC-member-011) — POST/GET/PUT/DELETE {@code /api/members/me/addresses} 웹 계층 단위
 * 테스트({@code controller-has-test} must 충족). 서비스는 mock — HTTP 계약(상태코드·코드·메시지·
 * memberId 강제)만 단언한다.
 *
 * <p>모든 요청은 {@code X-Api-Key: lab-member-0001-key}로 개별 오버라이드해 member 스코프를
 * 태운다 — 기본 admin 헤더({@link AdminApiKeyTestConfig})로는 {@code ApiKeyAuthFilter}의
 * SCOPE_TO_SELF가 걸리지 않아 이 엔드포인트의 실제 경로(memberId 강제)를 검증하지 못한다(STORY
 * "테스트" 절, 사람 확정). admin 키 검증(11번)만 예외로 기본 admin 헤더를 그대로 쓴다.
 */
@WebMvcTest(MemberAddressController.class)
@Import(AdminApiKeyTestConfig.class)
class MemberAddressControllerTest {

    private static final String MEMBER_KEY = "lab-member-0001-key";
    private static final String MEMBER_ID = "M-0001";
    private static final String BASE_URL = "/api/members/me/addresses";
    private static final String VALID_REQUEST_BODY = "{"
            + "\"recipient\":\"홍길동\",\"phone\":\"01011112222\",\"zipcode\":\"12345\","
            + "\"roadAddress\":\"서울시 강남대로 1\",\"detailAddress\":\"101동 101호\"}";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberAddressService memberAddressService;

    private static MemberAddress sampleAddress(Long addressId, String isDefault) {
        MemberAddress address = new MemberAddress();
        address.setAddressId(addressId);
        address.setMemberId(MEMBER_ID);
        address.setRecipient("홍길동");
        address.setPhone("01011112222");
        address.setPhoneNorm("01011112222");
        address.setZipcode("12345");
        address.setRoadAddress("서울시 강남대로 1");
        address.setDetailAddress("101동 101호");
        address.setIsDefault(isDefault);
        LocalDateTime now = LocalDateTime.now();
        address.setLastUsedAt(now);
        address.setCreatedAt(now);
        address.setUpdatedAt(now);
        return address;
    }

    // linked_tc: TC-FUNC-member-011-01
    @Test
    void list_returnsItemsEnvelope() throws Exception {
        when(memberAddressService.list(MEMBER_ID)).thenReturn(List.of(sampleAddress(1L, "Y")));

        mockMvc.perform(get(BASE_URL).header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].addressId").value(1))
                .andExpect(jsonPath("$.items[0].recipient").value("홍길동"));

        verify(memberAddressService).list(MEMBER_ID);
    }

    // linked_tc: TC-FUNC-member-011-02 — 첫 배송지 → 서비스가 강제한 isDefault="Y" 그대로 응답.
    @Test
    void register_firstAddress_returns201WithDefaultY() throws Exception {
        when(memberAddressService.register(eq(MEMBER_ID), any(MemberAddressService.AddressInput.class)))
                .thenReturn(sampleAddress(1L, "Y"));

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.addressId").value(1))
                .andExpect(jsonPath("$.isDefault").value("Y"));

        verify(memberAddressService).register(eq(MEMBER_ID), any(MemberAddressService.AddressInput.class));
    }

    // linked_tc: TC-FUNC-member-011-03
    @Test
    void register_serviceThrowsValidationError_returns400() throws Exception {
        when(memberAddressService.register(eq(MEMBER_ID), any(MemberAddressService.AddressInput.class)))
                .thenThrow(new MemberAddressApiException(HttpStatus.BAD_REQUEST, "MBR-4200",
                        "recipient: 받는사람 이름은 1~50자여야 합니다"));

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"recipient\":\" \",\"phone\":\"01011112222\",\"zipcode\":\"12345\","
                                + "\"roadAddress\":\"서울시 강남대로 1\",\"detailAddress\":\"101동 101호\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MBR-4200"));
    }

    // linked_tc: TC-FUNC-member-011-04
    @Test
    void register_serviceThrowsLimitExceeded_returns409() throws Exception {
        when(memberAddressService.register(eq(MEMBER_ID), any(MemberAddressService.AddressInput.class)))
                .thenThrow(new MemberAddressApiException(HttpStatus.CONFLICT, "MBR-4201",
                        "배송지는 최대 10개까지 등록할 수 있습니다"));

        mockMvc.perform(post(BASE_URL).header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("MBR-4201"));
    }

    // linked_tc: TC-FUNC-member-011-05
    @Test
    void update_success_returns200() throws Exception {
        when(memberAddressService.update(eq(MEMBER_ID), eq(1L), any(MemberAddressService.AddressInput.class)))
                .thenReturn(sampleAddress(1L, "N"));

        mockMvc.perform(put(BASE_URL + "/1").header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.addressId").value(1));
    }

    // linked_tc: TC-FUNC-member-011-06 — 없음/남의 것 구분 안 함(서비스가 이미 동일 예외로 통일).
    @Test
    void update_notFoundOrNotOwned_returns404() throws Exception {
        when(memberAddressService.update(eq(MEMBER_ID), eq(99L), any(MemberAddressService.AddressInput.class)))
                .thenThrow(new MemberAddressApiException(HttpStatus.NOT_FOUND, "MBR-4041", "배송지를 찾을 수 없습니다"));

        mockMvc.perform(put(BASE_URL + "/99").header("X-Api-Key", MEMBER_KEY)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_REQUEST_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MBR-4041"));
    }

    // linked_tc: TC-FUNC-member-011-07
    @Test
    void delete_success_returns204() throws Exception {
        mockMvc.perform(delete(BASE_URL + "/1").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isNoContent());

        verify(memberAddressService).delete(MEMBER_ID, 1L);
    }

    // linked_tc: TC-FUNC-member-011-08
    @Test
    void delete_notFoundOrNotOwned_returns404() throws Exception {
        org.mockito.Mockito.doThrow(new MemberAddressApiException(HttpStatus.NOT_FOUND, "MBR-4041", "배송지를 찾을 수 없습니다"))
                .when(memberAddressService).delete(MEMBER_ID, 99L);

        mockMvc.perform(delete(BASE_URL + "/99").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MBR-4041"));
    }

    // linked_tc: TC-FUNC-member-011-09
    @Test
    void setDefault_success_returns200() throws Exception {
        when(memberAddressService.setDefault(MEMBER_ID, 1L)).thenReturn(sampleAddress(1L, "Y"));

        mockMvc.perform(put(BASE_URL + "/1/default").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isDefault").value("Y"));
    }

    // linked_tc: TC-FUNC-member-011-10
    @Test
    void setDefault_notFoundOrNotOwned_returns404() throws Exception {
        when(memberAddressService.setDefault(MEMBER_ID, 99L))
                .thenThrow(new MemberAddressApiException(HttpStatus.NOT_FOUND, "MBR-4041", "배송지를 찾을 수 없습니다"));

        mockMvc.perform(put(BASE_URL + "/99/default").header("X-Api-Key", MEMBER_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MBR-4041"));
    }

    // linked_tc: TC-FUNC-member-011-11 — admin 키는 evaluateMemberScope를 타지 않아 memberId가
    // 강제되지 않는다. 이 컨트롤러가 명시적으로 400으로 막는다(STORY "순서·보안" 절, 사람 확정).
    // 기본 AdminApiKeyTestConfig 헤더를 그대로 사용(오버라이드 없음).
    @Test
    void list_adminKeyWithoutMemberIdParam_returns400() throws Exception {
        mockMvc.perform(get(BASE_URL))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MBR-4200"));
    }
}
