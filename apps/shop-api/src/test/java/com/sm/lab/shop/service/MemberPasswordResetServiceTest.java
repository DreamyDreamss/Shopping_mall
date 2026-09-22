// linked_func: FUNC-member-008
// spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md
package com.sm.lab.shop.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.sm.lab.shop.dao.MemberPasswordResetDao;
import com.sm.lab.shop.dao.MemberPasswordResetRateLimitDao;
import com.sm.lab.shop.domain.MemberPasswordReset;
import com.sm.lab.shop.domain.MemberPasswordResetRateLimit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SR-234(FUNC-member-008) — {@link MemberPasswordResetService} 단위 테스트(Mockito, DAO 목).
 * house rule 3(select→분기→update 금지)에 따른 원자 UPSERT의 실제 SQL 시맨틱은
 * {@link com.sm.lab.shop.dao.MemberPasswordResetDaoTest}(실 DB)에서 별도 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class MemberPasswordResetServiceTest {

    @Mock
    private MemberPasswordResetDao dao;

    @Mock
    private MemberPasswordResetRateLimitDao rateLimitDao;

    private ListAppender<ILoggingEvent> logAppender;

    private MemberPasswordResetService service() {
        return new MemberPasswordResetService(dao, rateLimitDao);
    }

    private MemberPasswordResetService service(Clock clock) {
        return new MemberPasswordResetService(dao, rateLimitDao, clock);
    }

    @BeforeEach
    void attachLogAppender() {
        logAppender = new ListAppender<>();
        logAppender.start();
        ((Logger) LoggerFactory.getLogger(MemberPasswordResetService.class)).addAppender(logAppender);
    }

    // SR-297 #2 — 기본 "허용" 스텁(가입 MemberSignupServiceTest#stubAdmitted와 동일 취지). 호출
    // 시 넘어온 token을 AtomicReference에 담아(이 파일의 stubDaoAdmitsWrite()와 동일 기법 —
    // ArgumentCaptor.capture()를 쓰지 않는 이유는 아래 참고) selectRateLimit이 그 토큰을 담은
    // 행을 돌려주게 동적 응답한다. lenient()가 필요한 이유: 형식 오류로 즉시 400을 던지는 기존
    // 테스트들은 rateLimitDao를 전혀 호출하지 않으므로 strict stubbing이면
    // UnnecessaryStubbingException이 난다.
    //
    // ArgumentCaptor가 아니라 AtomicReference를 쓰는 이유(실측 회귀) — stubDailyLimitRejected가
    // 이 기본 스텁을 override할 때 `when(rateLimitDao.selectRateLimit(...)).thenReturn(...)`가
    // 내부적으로 mock 메서드를 실제 호출해 "이미 걸려 있던" 이 기본 스텁의 thenAnswer를 먼저
    // 실행한다(재스텁 시 Mockito의 일반 동작) — 그 시점엔 아직 touchDailyLimit가 이번 테스트에서
    // 호출된 적이 없어 ArgumentCaptor.getValue()는 "No argument value was captured!"로 던진다.
    // AtomicReference는 값이 없으면 그냥 null을 돌려줄 뿐이라 이 재스텁 경로에서 안전하다.
    private final AtomicReference<String> lastDailyLimitToken = new AtomicReference<>();

    @BeforeEach
    void stubDailyLimitAdmitsByDefault() {
        lenient().doAnswer(invocation -> {
            lastDailyLimitToken.set(invocation.getArgument(5));
            return null;
        }).when(rateLimitDao).touchDailyLimit(any(), any(), any(), anyInt(), anyInt(), anyString());
        lenient().when(rateLimitDao.selectRateLimit(any(), any())).thenAnswer(invocation -> {
            MemberPasswordResetRateLimit row = new MemberPasswordResetRateLimit();
            row.setDailyCount(1);
            row.setLastToken(lastDailyLimitToken.get());
            return row;
        });
    }

    // SR-297 #2 — 거부 시나리오: last_token이 이번 요청의 토큰과 다른 값으로 남아 있음을 고정된
    // 문자열로 표현한다(가입 MemberSignupServiceTest#stubRejected와 동일 기법).
    private void stubDailyLimitRejected(int dailyCount) {
        doNothing().when(rateLimitDao).touchDailyLimit(any(), any(), any(), anyInt(), anyInt(), anyString());
        MemberPasswordResetRateLimit row = new MemberPasswordResetRateLimit();
        row.setDailyCount(dailyCount);
        row.setLastToken("existing-token-not-mine");
        when(rateLimitDao.selectRateLimit(any(), any())).thenReturn(row);
    }

    @AfterEach
    void detachLogAppender() {
        ((Logger) LoggerFactory.getLogger(MemberPasswordResetService.class)).detachAndStopAllAppenders();
    }

    @Test
    void requestPasswordResetCode_emailTarget_resolvesEmailChannel() {
        MemberPasswordResetService.VerificationCodeResult result =
                service().requestPasswordResetCode("user@example.com");

        assertThat(result.channel()).isEqualTo("EMAIL");
        assertThat(result.target()).isEqualTo("user@example.com");
        assertThat(result.expiresInSeconds()).isEqualTo(MemberPasswordResetService.EXPIRES_IN_SECONDS);
    }

    @Test
    void requestPasswordResetCode_phoneTarget_resolvesSmsChannel() {
        MemberPasswordResetService.VerificationCodeResult result =
                service().requestPasswordResetCode("01012345678");

        assertThat(result.channel()).isEqualTo("SMS");
    }

    @Test
    void requestPasswordResetCode_invalidFormat_throws400AndDaoNeverCalled() {
        assertThatThrownBy(() -> service().requestPasswordResetCode("not-an-email-or-phone"))
                .isInstanceOf(MemberPasswordResetApiException.class)
                .extracting(ex -> ((MemberPasswordResetApiException) ex).getHttpStatus())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(dao, never()).touchRequest(any(), any(), any(), any(), anyInt());
    }

    @Test
    void requestPasswordResetCode_blankTarget_throws400WithMbr4100() {
        assertThatThrownBy(() -> service().requestPasswordResetCode("  "))
                .isInstanceOf(MemberPasswordResetApiException.class)
                .extracting(ex -> ((MemberPasswordResetApiException) ex).getCode())
                .isEqualTo("MBR-4100");
    }

    @Test
    void requestPasswordResetCode_nullTarget_throws400WithMbr4100() {
        assertThatThrownBy(() -> service().requestPasswordResetCode(null))
                .isInstanceOf(MemberPasswordResetApiException.class)
                .extracting(ex -> ((MemberPasswordResetApiException) ex).getCode())
                .isEqualTo("MBR-4100");
    }

    @Test
    void requestPasswordResetCode_over100Chars_throws400WithMbr4100() {
        String tooLong = "a".repeat(89) + "@example.com"; // 101자, 이메일 형식은 유지
        assertThat(tooLong.length()).isEqualTo(101);

        assertThatThrownBy(() -> service().requestPasswordResetCode(tooLong))
                .isInstanceOf(MemberPasswordResetApiException.class)
                .extracting(ex -> ((MemberPasswordResetApiException) ex).getCode())
                .isEqualTo("MBR-4100");

        verify(dao, never()).touchRequest(any(), any(), any(), any(), anyInt());
    }

    // dao.touchRequest 호출 인자 — codeHash는 64자 hex, expiresAt = now + 600s, cooldownSeconds = 60.
    @Test
    void requestPasswordResetCode_touchRequestArgs_codeHashAndExpiryAndCooldown() {
        ArgumentCaptor<String> codeHashCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> expiresCaptor = ArgumentCaptor.forClass(LocalDateTime.class);

        service().requestPasswordResetCode("user@example.com");

        verify(dao).touchRequest(eq("user@example.com"), codeHashCaptor.capture(), expiresCaptor.capture(),
                nowCaptor.capture(), eq(MemberPasswordResetService.COOLDOWN_SECONDS));

        assertThat(codeHashCaptor.getValue()).hasSize(64);
        assertThat(codeHashCaptor.getValue()).matches("[0-9a-f]{64}");
        assertThat(expiresCaptor.getValue())
                .isEqualTo(nowCaptor.getValue().plusSeconds(MemberPasswordResetService.EXPIRES_IN_SECONDS));
    }

    // 생성자에 MemberDao가 없음(컴파일 타임 강제) — MemberPasswordResetService(MemberPasswordResetDao,
    // MemberPasswordResetRateLimitDao) 두 인자 생성자만 존재한다는 사실 자체가 회원 테이블 미조회를
    // 강제한다(SR-297 #2로 rateLimitDao가 추가됐어도 MemberDao는 여전히 없다). 별도 런타임 테스트 불필요.

    // round2(QA 권고, 재작업 지시 low(4)) — PHONE_PATTERN을 형제 가입 API(MemberRegistrationService)
    // 와 문자 단위로 동일하게 되돌려 하이픈 표기를 더 이상 형식 유효로 인정하지 않는다. round1은
    // 이 입력을 202로 받아들였는데, 그 결과 같은 입력이 가입 API에서는 400·재설정 API에서는
    // 202가 되는 불일치가 있었다 — 지금은 둘 다 400이다(양성 확인, 정규화 대상 자체가 아니게 됨).
    @Test
    void requestPasswordResetCode_hyphenatedPhone_rejectedLikeSiblingSignupApi() {
        assertThatThrownBy(() -> service().requestPasswordResetCode("010-1234-5678"))
                .isInstanceOf(MemberPasswordResetApiException.class)
                .extracting(ex -> ((MemberPasswordResetApiException) ex).getCode())
                .isEqualTo("MBR-4100");

        verify(dao, never()).touchRequest(any(), any(), any(), any(), anyInt());
    }

    // 사람 수정 (1), 정규화 회귀 — 정규화 자체는 여전히 유효하다(하이픈 없는 표기 그대로 저장).
    @Test
    void requestPasswordResetCode_plainDigitPhone_normalizesToSameTarget() {
        MemberPasswordResetService.VerificationCodeResult result =
                service().requestPasswordResetCode("01012345678");

        verify(dao).touchRequest(eq("01012345678"), any(), any(), any(), anyInt());
        assertThat(result.target()).isEqualTo("01012345678");
    }

    // 사람 수정 (1), 정규화 회귀 — 대소문자·앞뒤 공백과 무관하게 같은 이메일은 같은 정규화 값으로 저장된다.
    @Test
    void requestPasswordResetCode_emailCaseAndWhitespaceVariants_normalizeToSameTarget() {
        ArgumentCaptor<String> targetCaptor = ArgumentCaptor.forClass(String.class);

        MemberPasswordResetService.VerificationCodeResult upper =
                service().requestPasswordResetCode("Foo@Bar.COM");
        MemberPasswordResetService.VerificationCodeResult padded =
                service().requestPasswordResetCode(" foo@bar.com ");

        verify(dao, times(2)).touchRequest(targetCaptor.capture(), any(), any(), any(), anyInt());
        assertThat(targetCaptor.getAllValues()).containsExactly("foo@bar.com", "foo@bar.com");
        assertThat(upper.target()).isEqualTo("foo@bar.com");
        assertThat(padded.target()).isEqualTo("foo@bar.com");
    }

    // 사람 수정 (2), 로그 회귀 — channel·마스킹된 target·유효기간 문자열 포함, 생성된 6자리 코드
    // 원문은 로그 어디에도 나타나지 않음(codeHash로부터 브루트포스 역산해 실제로 대조한다).
    // round2(QA FAIL 필수1 재작업 이후) — 발송 로그는 이제 touchRequest 뒤 재조회한 code_hash가
    // 방금 만든 해시와 같을 때만 남으므로, 목 DAO의 selectByTarget이 touchRequest에 전달된
    // codeHash를 그대로 돌려주도록 스텁해 "쿨다운에 걸리지 않고 실제로 반영된" 상황을 재현한다.
    @Test
    void requestPasswordResetCode_logsChannelAndMaskedTarget_neverLogsPlainCode() {
        ArgumentCaptor<String> codeHashCaptor = ArgumentCaptor.forClass(String.class);
        stubDaoAdmitsWrite();

        service().requestPasswordResetCode("user@example.com");

        verify(dao).touchRequest(eq("user@example.com"), codeHashCaptor.capture(), any(), any(), anyInt());

        assertThat(logAppender.list).isNotEmpty();
        String combined = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);

        assertThat(combined).contains("EMAIL");
        String expectedMasked = "us" + "*".repeat("user@example.com".length() - 2);
        assertThat(combined).contains(expectedMasked);
        assertThat(combined).contains("유효기간");
        assertThat(combined).doesNotContain("user@example.com");

        String plainCode = bruteForceFindCode(codeHashCaptor.getValue());
        assertThat(combined).doesNotContain(plainCode);
    }

    // round2(QA FAIL 필수1 재작업) — 쿨다운으로 버려진 요청은 발송 로그를 남기지 않는다.
    // touchRequest 뒤 재조회한 code_hash가 이번 요청이 만든 해시와 다르면(=쿨다운이 이전 코드를
    // 그대로 유지) 로그를 남기지 않고, DB(목 DAO 기준)에 남은 해시도 바뀌지 않는다.
    @Test
    void requestPasswordResetCode_withinCooldown_noSendLogAndStoredHashUnchanged() {
        String target = "cooldown-test@example.com";
        String previousHash = "a".repeat(64); // 이전 요청이 저장해 둔 해시 — 쿨다운으로 이번 요청은 버려졌다고 가정
        MemberPasswordReset stillStored = new MemberPasswordReset();
        stillStored.setTarget(target);
        stillStored.setCodeHash(previousHash);
        // touchRequest는 정상적으로 호출되지만(쿨다운 판정 자체는 DAO SQL 소관 —
        // MemberPasswordResetDaoTest가 별도 실증), 재조회 결과는 항상 이전 해시를 돌려준다 —
        // 즉 방금 생성한 codeHash와는 절대 같을 수 없다(SecureRandom 6자리 → SHA-256, 우연 충돌
        // 확률은 무시 가능).
        when(dao.selectByTarget(eq(target))).thenReturn(stillStored);

        MemberPasswordResetService.VerificationCodeResult result =
                service().requestPasswordResetCode(target);

        // 응답은 어느 경우든 동일(202·상수 바디) — 쿨다운 여부를 드러내지 않는다(오라클 금지).
        assertThat(result.channel()).isEqualTo("EMAIL");
        assertThat(result.target()).isEqualTo(target);
        assertThat(result.expiresInSeconds()).isEqualTo(MemberPasswordResetService.EXPIRES_IN_SECONDS);

        // 발송 로그가 전혀 없어야 한다 — 쿨다운으로 버려진(=검증 불가능해질) 코드를 발송했다고
        // 알리면 안 된다(round1 QA FAIL 필수1 재발 방지).
        assertThat(logAppender.list).isEmpty();

        // DB(목 DAO 기준)에 남은 해시는 이전 값 그대로다 — 서비스가 재조회 결과와 다른 해시를
        // 임의로 "발송됐다"고 취급하지 않는다.
        assertThat(dao.selectByTarget(target).getCodeHash()).isEqualTo(previousHash);
    }

    // SR-297 #2, STORY "테스트" 절 결정5 — 일일 상한 초과 시 dao.touchRequest가 호출되지 않고
    // (발송·코드 갱신 생략), 응답은 그럼에도 기존 "정상" 케이스와 동일한 채널/target/
    // expiresInSeconds의 202다(상한 오라클 없음). 거부 시에도 서버 로그 한 줄은 남지만(결정1
    // 5단계) "발송" 로그는 아니다 — 이 테스트는 "발송" 문구가 없음만 확인한다.
    @Test
    void requestPasswordResetCode_dailyLimitExceeded_skipsTouchRequestAndSendLog_stillReturns202() {
        stubDailyLimitRejected(MemberPasswordResetService.DAILY_REQUEST_LIMIT);

        MemberPasswordResetService.VerificationCodeResult result =
                service().requestPasswordResetCode("daily-limit@example.com");

        assertThat(result.channel()).isEqualTo("EMAIL");
        assertThat(result.target()).isEqualTo("daily-limit@example.com");
        assertThat(result.expiresInSeconds()).isEqualTo(MemberPasswordResetService.EXPIRES_IN_SECONDS);

        verify(dao, never()).touchRequest(any(), any(), any(), any(), anyInt());

        String combined = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(combined)
                .as("상한 초과는 발송 로그를 남기지 않는다(결정1 4단계 — touchRequest 자체가 안 불림)")
                .doesNotContain("발송");
    }

    // round2(QA CONCERNS round1 재작업 지시 1) — admitted=false 거부 로그가 사유(쿨다운 vs 일일
    // 상한)를 실제로 가르는지 단언한다. dailyCount가 DAILY_REQUEST_LIMIT 이상이면 "일일 상한
    // 초과" 문구가 남아야 한다(형제 MemberSignupService#rejectionFor와 동일 판별 기준).
    @Test
    void requestPasswordResetCode_dailyLimitExceeded_logsDailyLimitExceededReason() {
        stubDailyLimitRejected(MemberPasswordResetService.DAILY_REQUEST_LIMIT);

        service().requestPasswordResetCode("daily-limit-reason@example.com");

        String combined = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(combined)
                .as("dailyCount(%d) >= DAILY_REQUEST_LIMIT(%d)이면 사유가 '일일 상한 초과'여야 한다",
                        MemberPasswordResetService.DAILY_REQUEST_LIMIT, MemberPasswordResetService.DAILY_REQUEST_LIMIT)
                .contains("일일 상한 초과");
        assertThat(combined).doesNotContain("거부(쿨다운)");
    }

    // round2(QA CONCERNS round1 재작업 지시 1) — dailyCount가 상한 미만인데도 admitted=false인
    // 경우(=코드 테이블 쿨다운이 아니라 카운터 자체의 쿨다운 판정에 걸린 경우)는 사유가
    // "쿨다운"이어야 한다. round1은 이 경로도 무조건 "일일 상한 초과"로 잘못 기록했었다.
    @Test
    void requestPasswordResetCode_cooldownRejected_logsCooldownReason() {
        stubDailyLimitRejected(1); // dailyCount=1 < DAILY_REQUEST_LIMIT(5) → 쿨다운 거부

        service().requestPasswordResetCode("cooldown-reason@example.com");

        String combined = logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "\n" + b);
        assertThat(combined)
                .as("dailyCount(1) < DAILY_REQUEST_LIMIT(%d)이면 사유가 '쿨다운'이어야 한다",
                        MemberPasswordResetService.DAILY_REQUEST_LIMIT)
                .contains("거부(쿨다운)");
        assertThat(combined).doesNotContain("일일 상한 초과");
    }

    // SR-297 #2, STORY "테스트" 절 결정5 — touchDailyLimit 인자(쿨다운·일일상한 상수) 배선 고정.
    @Test
    void requestPasswordResetCode_touchDailyLimitArgs_cooldownAndDailyLimitConstants() {
        service().requestPasswordResetCode("user@example.com");

        verify(rateLimitDao).touchDailyLimit(eq("user@example.com"), any(), any(),
                eq(MemberPasswordResetService.COOLDOWN_SECONDS), eq(MemberPasswordResetService.DAILY_REQUEST_LIMIT),
                anyString());
    }

    // SR-297 #2, STORY "테스트" 절 결정5 — 게이트 순서(결정1) 자체를 InOrder로 고정한다. 향후
    // 리팩터링이 이 순서를 조용히 뒤집는 회귀를 잡는다.
    @Test
    void requestPasswordResetCode_dailyLimitAdmitted_thenTouchRequestCalledAfterGate() {
        service().requestPasswordResetCode("order-test@example.com");

        InOrder inOrder = inOrder(rateLimitDao, dao);
        inOrder.verify(rateLimitDao).touchDailyLimit(any(), any(), any(), anyInt(), anyInt(), anyString());
        inOrder.verify(dao).touchRequest(any(), any(), any(), any(), anyInt());
    }

    // SR-297 #2, STEP 3-0 사람 확인 — 자정 경계(시계 주입) 시나리오: 카운터는 오늘 새 day_key
    // 행이라 admit(일일 카운트 소비)하지만, 코드 테이블 자체 쿨다운(어제 created_at 기준)에는
    // 여전히 걸려 code_hash가 갱신되지 않는다(round2 재조회 대조 로직 불변) — 그럼에도 응답은
    // 동일한 202. dao.touchRequest는 admit됐으므로 호출은 된다(코드 테이블 쿨다운 판정 자체는
    // SQL이 한다 — 이 서비스는 재조회 대조로만 로그 여부를 가른다).
    // round2(QA CONCERNS round1 재작업 지시 2) — round1은 기본 lenient 허용 스텁이 무조건
    // admit해 버려 Clock.fixed를 아무 값으로 바꿔도 통과하는 무력한 테스트였다(자정과 무관).
    // 이번 재작성은 touchDailyLimit에 실제로 전달된 dayKey/now 인자를 ArgumentCaptor로 캡처해
    // day_key가 주입 시계(자정 롤오버 이후)에서 파생됐음을 직접 단언한다 — 이 단언이 있으면
    // Clock을 자정 이전 값으로 바꿨을 때 dayKey 기대값이 어긋나 테스트가 실패하므로, 게이트
    // 로직(LocalDateTime.now(clock).toLocalDate())을 실제로 행사한다.
    @Test
    void requestPasswordResetCode_midnightBoundary_dailyGateAdmitsButCodeCooldownStillBlocks_returns202WithoutSendLog() {
        LocalDateTime midnightInstant = LocalDateTime.of(2026, 9, 17, 0, 0, 10);
        Clock midnightClock = Clock.fixed(
                midnightInstant.atZone(ZoneId.systemDefault()).toInstant(),
                ZoneId.systemDefault());
        String target = "midnight-boundary@example.com";
        String previousHash = "b".repeat(64); // 어제 저장된 code_hash — 코드 테이블 쿨다운에 걸려 그대로 유지
        MemberPasswordReset stillStored = new MemberPasswordReset();
        stillStored.setTarget(target);
        stillStored.setCodeHash(previousHash);
        when(dao.selectByTarget(eq(target))).thenReturn(stillStored);

        MemberPasswordResetService.VerificationCodeResult result =
                service(midnightClock).requestPasswordResetCode(target);

        // 응답은 언제나 동일 202 바디(오라클 없음) — 자정 경계 여부가 드러나지 않는다.
        assertThat(result.channel()).isEqualTo("EMAIL");
        assertThat(result.target()).isEqualTo(target);
        assertThat(result.expiresInSeconds()).isEqualTo(MemberPasswordResetService.EXPIRES_IN_SECONDS);

        // 재작업 지시 2 — dayKey=새 날짜(2026-09-17), now=주입 시각 그대로가 touchDailyLimit에
        // 실제로 전달됐음을 캡처해 단언한다(lenient 기본 스텁이 이 인자를 검사하지 않고도
        // 통과시키던 결함을 없앤다).
        ArgumentCaptor<LocalDate> dayKeyCaptor = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalDateTime> nowCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(rateLimitDao).touchDailyLimit(eq(target), dayKeyCaptor.capture(), nowCaptor.capture(),
                eq(MemberPasswordResetService.COOLDOWN_SECONDS), eq(MemberPasswordResetService.DAILY_REQUEST_LIMIT),
                anyString());
        assertThat(dayKeyCaptor.getValue()).isEqualTo(LocalDate.of(2026, 9, 17));
        assertThat(nowCaptor.getValue()).isEqualTo(midnightInstant);

        // 게이트는 admit했으므로(기본 lenient 허용 스텁) touchRequest는 호출된다.
        verify(dao).touchRequest(eq(target), any(), any(), any(), eq(MemberPasswordResetService.COOLDOWN_SECONDS));
        // 재조회 해시가 옛 값 그대로라 "발송" 로그는 남지 않는다(logAppender 자체가 완전히 빈다 —
        // 거부 로그도 없다, 게이트는 admit했으므로).
        assertThat(logAppender.list).isEmpty();
    }

    /** touchRequest에 전달된 codeHash를 그대로 selectByTarget 결과로 되돌려주는 목 DAO 스텁. */
    private void stubDaoAdmitsWrite() {
        AtomicReference<String> capturedCodeHash = new AtomicReference<>();
        doAnswer(invocation -> {
            capturedCodeHash.set(invocation.getArgument(1));
            return null;
        }).when(dao).touchRequest(any(), any(), any(), any(), anyInt());
        when(dao.selectByTarget(any())).thenAnswer(invocation -> {
            MemberPasswordReset stored = new MemberPasswordReset();
            stored.setTarget(invocation.getArgument(0));
            stored.setCodeHash(capturedCodeHash.get());
            return stored;
        });
    }

    private static String bruteForceFindCode(String targetHash) {
        for (int i = 0; i < 1_000_000; i++) {
            String candidate = String.format("%06d", i);
            if (sha256Hex(candidate).equals(targetHash)) {
                return candidate;
            }
        }
        throw new IllegalStateException("코드 해시에 대응하는 평문을 찾지 못함 — 테스트 전제 위반");
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
