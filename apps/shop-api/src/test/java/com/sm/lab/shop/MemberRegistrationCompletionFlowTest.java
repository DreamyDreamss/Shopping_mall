// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberSignupCompletionDao;
import com.sm.lab.shop.dao.MemberSignupRateLimitDao;
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

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SR-231(FUNC-member-003) round3 재작업(2026-09-12, round2 QA FAIL 재작업 지시 3번, 사람 결정 —
 * 마지막 라운드) — round2 QA가 지적한 "테스트 맹점"을 API 레벨(실 서버+실 DB, TestRestTemplate)로
 * 메운다. round2까지의 필수1 증명은 {@code MemberSignupCompletionDaoTest}(DAO 직접 호출, 트랜잭션
 * 롤백 경로를 타지 않음)와 {@code MemberRegistrationServiceTest}(Mockito, DB·트랜잭션 없음)에만
 * 있어, 실제 HTTP 요청 → 컨트롤러 → (비트랜잭션) 서비스 → DB 순서를 통과하는 회귀가 없었다 —
 * 그래서 "시도 카운터 증가가 트랜잭션 롤백으로 사라지는" round2의 핵심 결함이 297건 초록 아래
 * 숨었다. 이 클래스는 그 실제 경로를 그대로 태운다.
 *
 * <p>사람 코멘트가 지정한 테스트 목록 중 (a)(b)(c)를 담당한다 — (d) 하이픈 번호 중복은
 * {@link MemberRegistrationPhoneNormalizationTest}, (e) 기존 회원 조회 불변은
 * {@code MemberQueryRegressionTest}가 이미 API 레벨로 담당한다(중복 작성하지 않음).
 *
 * <p><b>round4(2026-09-12, round3 QA CONCERNS 권고1 "존재 오라클" — 사람 결정, 마지막 라운드)</b>
 * — STEP 0(사전 판정)과 STEP 1(코드 검증)의 순서가 뒤집혔다(이제 코드 검증이 먼저, 사전 판정은
 * 그 검증이 성공한 뒤에만). {@link #reSignUpWithAlreadyRegisteredEmail_afterResend_returns409EmailDuplicateWithLoginUrl}
 * ((c))가 이 순서 변경의 직접 영향을 받는다 — round4 당시에는 재발송이 {@code consumed_at}/
 * {@code attempt_count}를 리셋하지 않아, 재발송받은 새 코드로 재시도해도 그 코드 검증
 * 자체가 실패해 409 {@code MBR-4091}이 나왔다(사람 결정이 명시적으로 받아들인 회귀 — "이렇게
 * 하면 코드를 못 받는 사람은 존재 여부를 알 수 없다"). 근본 해결(재발송 시 리셋)은
 * FUNC-member-002 소유 {@code writeCode} 몫이라 이 FUNC 범위 밖이었다.
 *
 * <p><b>SR-295(2026-09-15, 사람 결정)</b> — 위 한계를 해소했다. INF-MBR-001의 {@code writeCode}
 * (재발송 분기)가 이제 {@code attempt_count=0}/{@code consumed_at=NULL}도 함께 리셋한다.
 * STEP 0/STEP 1 순서 자체는 그대로이지만, 재발송받은 새 코드로 재시도하면 STEP 1이 이제
 * 성공하고 STEP 0에서 기존 회원과 충돌해 409 {@code MBR-4092}(+login_url)가 나온다(round4
 * 이전 동작으로 되돌아감 — 존재 오라클 재검토 결론: 코드값이 응답·로그 어디에도 노출되지 않아
 * 이 채널을 소유하지 않은 제3자는 여전히 코드를 알아낼 방법이 없으므로 오라클이 아니다).
 * {@link #reSignUpWithAlreadyRegisteredEmail_afterResend_returns409EmailDuplicateWithLoginUrl}가
 * 이 새 기대값을 검증한다. MBR-4093 잠금 회복 자체는
 * {@link #wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds}가 검증한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberRegistrationCompletionFlowTest {

    private static final String CHANNEL = "EMAIL";
    private static final int MAX_ATTEMPTS = 5;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private MemberSignupVerificationDao verificationDao;
    @Autowired
    private MemberSignupCompletionDao completionDao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private MemberSignupRateLimitDao rateLimitDao;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String cleanupTarget;
    private String cleanupMemberId;
    // SR-295 재작업(round2 QA CONCERNS 권고1, 사람 결정) — 이 클래스에서 처음으로 실제
    // 레이트리밋 엔드포인트(POST /api/members/signup/verification-codes)를 리터럴 target으로
    // 호출하는 케이스가 생겨, MEMBER_SIGNUP_RATE_LIMITS 행이 실행을 가로질러 남는 것을 막는다
    // (선례: MemberSignupRateLimitConcurrencyTest:71-74). 기존 테스트들은 모두 verificationDao
    // .writeCode를 직접 불러 레이트리밋을 우회하므로 이 필드를 쓰지 않는다(null로 남음).
    private String cleanupRateLimitTarget;

    @AfterEach
    void cleanUp() {
        if (cleanupTarget != null) {
            verificationDao.deleteByChannelAndTarget(CHANNEL, cleanupTarget);
        }
        if (cleanupMemberId != null) {
            memberDao.deleteById(cleanupMemberId);
        }
        if (cleanupRateLimitTarget != null) {
            rateLimitDao.deleteRateLimit(cleanupRateLimitTarget, LocalDate.now());
        }
    }

    private ResponseEntity<String> callSignUp(String target, String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"target\":\"" + target + "\",\"code\":\"" + code + "\","
                + "\"password\":\"abcd1234\",\"name\":\"완료흐름\"}";
        return restTemplate.postForEntity("/api/members/signup", new HttpEntity<>(body, headers), String.class);
    }

    private String readMemberId(String body) {
        try {
            JsonNode node = objectMapper.readTree(body);
            return node.get("memberId").asText();
        } catch (Exception e) {
            throw new AssertionError("응답에서 memberId를 읽을 수 없음: " + body, e);
        }
    }

    // (a) 사람 코멘트 — 틀린 코드 5회 → 6회째 정답 제출도 409 MBR-4093, DB attempt_count=5 실측.
    // round2는 이 시나리오에서 attempt_count가 8회 내내 0에 머물다 9번째 정답이 201로 성공했다
    // (실측 프로브, QA round2 필수1) — round3는 STEP1이 비트랜잭션이라 각 증가가 즉시 커밋된다.
    @Test
    void wrongCodeFiveTimesThenCorrect_locksOutAtFiveAndRejectsCorrectCode() {
        String target = "attempt-cap@example.com";
        cleanupTarget = target;
        LocalDateTime now = LocalDateTime.now();
        verificationDao.writeCode(CHANNEL, target, "111111", now.plusMinutes(5), now);

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            ResponseEntity<String> res = callSignUp(target, "000000");
            assertThat(res.getStatusCode().value()).as("오답 %d회차", i).isEqualTo(409);
            assertThat(res.getBody()).as("오답 %d회차 코드", i).contains("\"code\":\"MBR-4091\"");
        }
        assertThat(completionDao.selectAttemptCount(CHANNEL, target))
                .as("오답 %d회 후 attempt_count 실측", MAX_ATTEMPTS)
                .isEqualTo(MAX_ATTEMPTS);

        ResponseEntity<String> sixth = callSignUp(target, "111111"); // 정답 코드
        assertThat(sixth.getStatusCode().value()).isEqualTo(409);
        assertThat(sixth.getBody()).contains("\"code\":\"MBR-4093\"");
        assertThat(completionDao.selectAttemptCount(CHANNEL, target))
                .as("상한 도달 후에는 추가 요청이 attempt_count를 더 늘리지 않아야 함")
                .isEqualTo(MAX_ATTEMPTS);
    }

    // (SR-295) MBR-4093 잠금 회복 — 5회 오답으로 잠긴(attempt_count==5) target도, 실제 재발송
    // 엔드포인트(INF-MBR-001)를 다시 불러 새 코드를 받으면 그 새 코드로는 정상 가입이 된다.
    // 응답 자체에는 코드값이 없으므로(계약 불변 — {channel,target,expiresInSeconds}) DB에서
    // 새 코드를 읽어 가입을 완료한다(기존 (a)/(b)/(c) 테스트와 같은 관례).
    @Test
    void wrongCodeFiveTimesThenCorrect_thenResendRecovers_newCodeSucceeds() {
        String target = "attempt-cap-recovery@example.com";
        cleanupTarget = target;
        cleanupRateLimitTarget = target; // 실제 재발송 엔드포인트를 호출하는 유일한 케이스
        LocalDateTime now = LocalDateTime.now();
        verificationDao.writeCode(CHANNEL, target, "111111", now.plusMinutes(5), now);

        for (int i = 1; i <= MAX_ATTEMPTS; i++) {
            ResponseEntity<String> res = callSignUp(target, "000000");
            assertThat(res.getStatusCode().value()).as("오답 %d회차", i).isEqualTo(409);
        }
        ResponseEntity<String> locked = callSignUp(target, "111111"); // 정답이어도 잠김
        assertThat(locked.getStatusCode().value()).isEqualTo(409);
        assertThat(locked.getBody()).contains("\"code\":\"MBR-4093\"");
        assertThat(completionDao.selectAttemptCount(CHANNEL, target)).isEqualTo(MAX_ATTEMPTS);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String resendBody = "{\"target\":\"" + target + "\"}";
        ResponseEntity<String> resend = restTemplate.postForEntity(
                "/api/members/signup/verification-codes", new HttpEntity<>(resendBody, headers), String.class);
        assertThat(resend.getStatusCode().value()).isEqualTo(200);
        assertThat(resend.getBody()).contains("\"channel\":\"EMAIL\"");
        assertThat(resend.getBody()).contains("\"target\":\"" + target + "\"");
        assertThat(resend.getBody()).contains("\"expiresInSeconds\"");

        assertThat(completionDao.selectAttemptCount(CHANNEL, target))
                .as("재발송 직후 attempt_count는 0으로 리셋돼야 함")
                .isEqualTo(0);

        String newCode = verificationDao.selectByChannelAndTarget(CHANNEL, target).getCode();
        ResponseEntity<String> retry = callSignUp(target, newCode);
        assertThat(retry.getStatusCode().value())
                .as("재발송된 새 코드로는 MBR-4093 없이 가입이 성공해야 함")
                .isEqualTo(201);
        cleanupMemberId = readMemberId(retry.getBody());
    }

    // (b) 성공 후 같은 코드 재사용 거부. 이 시점엔 이메일이 이미 회원이지만, 소비된 코드로는
    // STEP 1(코드 검증) 자체가 실패해(consumed_at IS NOT NULL) STEP 0(사전 판정)이 호출되지
    // 않는다 — 그래서 관찰되는 코드는 MBR-4092가 아니라 MBR-4091이다(SR-298 사람 수정 — 이미
    // 소비된 코드로 재시도 시 기존 오류 코드가 그대로인지 HTTP 레벨로 회귀 확인. DAO 레벨의
    // 1회성 소비 자체는
    // MemberSignupCompletionDaoTest#markVerifiedIfCodeMatches_afterConsumed_rejectsSameCodeAgain가
    // 별도로 증명한다).
    @Test
    void signUpSucceeds_thenSameCodeReuse_isRejected() {
        String target = "reuse-code@example.com";
        cleanupTarget = target;
        LocalDateTime now = LocalDateTime.now();
        verificationDao.writeCode(CHANNEL, target, "222222", now.plusMinutes(5), now);

        ResponseEntity<String> first = callSignUp(target, "222222");
        assertThat(first.getStatusCode().value()).isEqualTo(201);
        cleanupMemberId = readMemberId(first.getBody());

        ResponseEntity<String> reuse = callSignUp(target, "222222");
        assertThat(reuse.getStatusCode().value()).isEqualTo(409);
        assertThat(reuse.getBody()).contains("\"code\":\"MBR-4091\"");
    }

    // SR-298(사람 수정 — 테스트 추가 (b)) — 만료된 코드로 가입을 시도해도 기존 오류 코드
    // (MBR-4091, 회귀)가 그대로인지 HTTP 레벨로 확인한다. incrementAttemptCount의 원자화는
    // markVerifiedIfCodeMatches의 만료 판정 자체를 건드리지 않으므로(구현 계획 "범위 밖" —
    // STEP1 자체의 매치 조건은 이 SR 대상이 아니다) 이 회귀는 그대로 통과해야 한다.
    @Test
    void expiredCode_isRejectedWithSameVerifyRequiredCode() {
        String target = "expired-code@example.com";
        cleanupTarget = target;
        LocalDateTime now = LocalDateTime.now();
        // expires_at이 이미 지난 코드를 시딩 — markVerifiedIfCodeMatches의 expires_at > NOW(3)
        // 조건에 걸려 0행이 되고, incrementAttemptCount 이후 attempt_count는 상한 미만이므로
        // selectAttemptCount 분기 결과는 기존과 동일하게 MBR-4091이다.
        verificationDao.writeCode(CHANNEL, target, "666666", now.minusMinutes(1), now.minusMinutes(6));

        ResponseEntity<String> res = callSignUp(target, "666666");

        assertThat(res.getStatusCode().value()).isEqualTo(409);
        assertThat(res.getBody()).contains("\"code\":\"MBR-4091\"");
        assertThat(res.getBody()).doesNotContain("login_url");
    }

    // (c) SR-295로 기대값이 다시 뒤바뀐 테스트(사람 결정, SR-231 round4 → SR-295로 재교체) —
    // 이미 가입된 이메일이 재발송받은 새 코드로 재시도하면 이제 409 MBR-4091이 아니라 다시
    // 409 MBR-4092(+login_url)이어야 한다. writeCode(재발송 분기)가 attempt_count/consumed_at을
    // 리셋하므로 STEP 1(코드 검증)이 이제 성공하고, 그 뒤 STEP 0(사전 판정)에서 기존 회원과
    // 충돌해 4092가 나온다. 존재 오라클 재검토: 코드값은 응답·로그 어디에도 노출되지 않으므로
    // 이 채널을 소유하지 않은 제3자는 재발송 API를 아무리 불러도 코드 자체를 알아낼 수 없다 —
    // 이 경로로 가입 여부를 알아낼 수 있는 것은 "실제로 그 채널의 코드를 받을 수 있는 사람"뿐이고,
    // 그 사람은 이미 자신의 가입 여부를 안다. 따라서 오라클 재발이 아니다(사람 결정, STORY
    // "구현 계획 — 순서·보안" 참고).
    @Test
    void reSignUpWithAlreadyRegisteredEmail_afterResend_returns409EmailDuplicateWithLoginUrl() {
        String target = "already-member@example.com";
        cleanupTarget = target;
        LocalDateTime now = LocalDateTime.now();
        verificationDao.writeCode(CHANNEL, target, "333333", now.plusMinutes(5), now);

        ResponseEntity<String> first = callSignUp(target, "333333");
        assertThat(first.getStatusCode().value()).isEqualTo(201);
        cleanupMemberId = readMemberId(first.getBody());

        // 재발송 — writeCode(FUNC-member-002, SR-295)는 이제 verified_at뿐 아니라
        // attempt_count=0/consumed_at=NULL도 함께 리셋한다. 그래서 STEP 1의 조건부 UPDATE
        // (consumed_at IS NULL 포함)가 새 코드에 대해 성공하고, 뒤이은 STEP 0에서 기존
        // 회원(email)과 충돌해 409 MBR-4092(+login_url)로 떨어진다.
        verificationDao.writeCode(CHANNEL, target, "444444", now.plusMinutes(10), now);

        ResponseEntity<String> retry = callSignUp(target, "444444");
        assertThat(retry.getStatusCode().value()).isEqualTo(409);
        assertThat(retry.getBody()).contains("\"code\":\"MBR-4092\"");
        assertThat(retry.getBody()).contains("login_url");
    }

    // (c') 사람 코멘트 테스트 지시 — "미검증(코드 요청도 안 한/틀린) 상태에서 기존 이메일로
    // 요청 → 4092가 아니라 4091 단언". 위 테스트는 "재발송받은 새 코드"로 재시도하는 경우를
    // 다루고, 이 테스트는 재발송조차 받지 않고 아무 코드나 넣는 더 단순한 경우를 다룬다 — 둘 다
    // STEP 1이 먼저 실패해 이 target이 MEMBERS에 있는지 전혀 조회되지 않는다는 같은 성질을
    // 증명한다.
    @Test
    void reSignUpWithAlreadyRegisteredEmail_withoutRequestingNewCode_returns409VerifyRequiredNotEmailDuplicate() {
        String target = "no-resend-member@example.com";
        cleanupTarget = target;
        LocalDateTime now = LocalDateTime.now();
        verificationDao.writeCode(CHANNEL, target, "555555", now.plusMinutes(5), now);

        ResponseEntity<String> first = callSignUp(target, "555555");
        assertThat(first.getStatusCode().value()).isEqualTo(201);
        cleanupMemberId = readMemberId(first.getBody());

        // 재발송을 아예 받지 않은 채(= 코드 요청도 안 한 상태와 동등 — consumed_at이 이미
        // non-null) 임의의 틀린 코드로 재시도.
        ResponseEntity<String> retry = callSignUp(target, "999999");
        assertThat(retry.getStatusCode().value()).isEqualTo(409);
        assertThat(retry.getBody()).contains("\"code\":\"MBR-4091\"");
        assertThat(retry.getBody()).doesNotContain("login_url");
    }
}
