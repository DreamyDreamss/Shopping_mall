// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회귀 — SR-231(FUNC-member-003) STORY 확정 문답(regression_keep): "기존 조회 결과 전부(데이터
 * 계약 불변)". V3__members_signup.sql이 MEMBERS에 email/password_hash/marketing_opt_in 컬럼을
 * 추가했지만, {@code MemberDao.selectById}(FUNC-order-004/017 등 다른 FUNC 소유, 이 FUNC은
 * 수정하지 않음)는 여전히 기존 5개 컬럼만 SELECT하므로 신규 컬럼이 조회 응답에 노출돼서는 안 된다
 * (특히 password_hash가 새어나가면 심각한 정보노출).
 *
 * <p>linked_tc: TC-FUNC-member-003-07
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberQueryRegressionTest {

    private static final String HEADER = "X-Api-Key";
    private static final String ADMIN_KEY = "lab-admin-key";

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void getMember_afterSignupMigration_returnsOnlyPreExistingFields() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER, ADMIN_KEY);

        ResponseEntity<String> res = restTemplate.exchange("/api/members/M-0001", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        String body = res.getBody();
        assertThat(body).contains("\"memberId\":\"M-0001\"");
        assertThat(body).contains("\"memberName\":\"김실증\"");
        assertThat(body).contains("\"grade\"");
        assertThat(body).contains("\"phone\"");
        assertThat(body).contains("\"createdAt\"");
        assertThat(body).contains("\"recentOrders\"");
        // 신규 컬럼(email/passwordHash/marketingOptIn)은 selectById가 SELECT하지 않으므로
        // 응답 JSON에 그 키 자체가 없어야 한다(특히 비밀번호 해시 노출은 심각한 결함).
        assertThat(body).doesNotContain("passwordHash").doesNotContain("password_hash");
        assertThat(body).doesNotContain("marketingOptIn").doesNotContain("marketing_opt_in");
        assertThat(body).doesNotContain("\"email\"");
        // round2(SR-231 round1 QA FAIL 재작업) 신규 컬럼도 동일하게 비노출이어야 한다.
        assertThat(body).doesNotContain("phoneNorm").doesNotContain("phone_norm");
    }

    @Test
    void listMembers_afterSignupMigration_returnsOkWithoutNewColumns() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HEADER, ADMIN_KEY);

        ResponseEntity<String> res = restTemplate.exchange("/api/members", HttpMethod.GET,
                new HttpEntity<>(headers), String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).doesNotContain("passwordHash").doesNotContain("\"email\"");
    }
}
