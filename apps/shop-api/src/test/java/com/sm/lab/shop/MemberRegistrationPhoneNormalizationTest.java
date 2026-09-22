// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop;

import com.sm.lab.shop.dao.MemberSignupVerificationDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회귀 — SR-231(FUNC-member-003) round1 QA FAIL 필수2(사람 결정, round2 재작업). 랩 시드
 * 회원 M-0001의 {@code phone}은 하이픈 포함('010-1111-2222', DB MCP 실측)이고, 가입 요청은
 * 숫자만('01011112222')으로 target을 받는다. round1은 원문 {@code phone} 컬럼의 UNIQUE
 * 인덱스만 있어 이 둘을 다른 값으로 취급해 재가입을 막지 못했다 — round2가 추가한
 * {@code phone_norm}(V3__members_signup.sql round2 백필 포함) 전용 UNIQUE 인덱스로 같은
 * 번호임을 판정한다.
 *
 * <p>linked_tc: TC-FUNC-member-003-09
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberRegistrationPhoneNormalizationTest {

    // M-0001의 실제 phone(하이픈 포함) 숫자만 정규화 값 — OrderExportIntegrationTest/
    // ApiKeyAuthIntegrationTest 등 다른 테스트가 참조하는 것과 동일한 랩 시드 실측(phone
    // 010-1111-2222)에서 유도했다.
    private static final String CHANNEL = "SMS";
    private static final String DUPLICATE_PHONE_TARGET = "01011112222";
    private static final String CODE = "555555";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MemberSignupVerificationDao verificationDao;

    @AfterEach
    void cleanUp() {
        verificationDao.deleteByChannelAndTarget(CHANNEL, DUPLICATE_PHONE_TARGET);
    }

    @Test
    void signUp_phoneMatchingExistingHyphenatedSeedMember_returns409PhoneDuplicate() {
        LocalDateTime now = LocalDateTime.now();
        verificationDao.writeCode(CHANNEL, DUPLICATE_PHONE_TARGET, CODE, now.plusMinutes(5), now);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"target\":\"" + DUPLICATE_PHONE_TARGET + "\",\"code\":\"" + CODE + "\","
                + "\"password\":\"abcd1234\",\"name\":\"중복휴대폰\"}";

        ResponseEntity<String> res = restTemplate.postForEntity(
                "/api/members/signup", new HttpEntity<>(body, headers), String.class);

        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody()).contains("\"code\":\"MBR-4094\"");
    }
}
