// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberSignupRateLimitDao;
import com.sm.lab.shop.dao.MemberSignupVerificationDao;
import com.sm.lab.shop.domain.MemberSignupRateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 회원가입 — 인증코드 발송(SR-231, INF-MBR-001). target(이메일 또는 휴대폰번호) 형식으로 채널을
 * 자동 판별하고, 6자리 인증코드를 생성해 5분 유효로 저장한 뒤 "발송"한다.
 *
 * <p><b>스코프(이 FUNC-member-002 한정)</b>: 인증코드 확인(verify) + 비밀번호·이름·마케팅 동의를
 * 받아 실제 회원을 생성하는 가입완료 흐름은 별도 FUNC(FUNC-member-003, INF-MBR-002)의 책임이다.
 *
 * <p><b>발송 시뮬레이션</b>: 이 랩에는 실제 이메일/SMS 발송 게이트웨이가 없다. 코드를 API 응답에도,
 * 로그에도 담지 않는다.
 *
 * <p><b>레이트리밋 — round4(2026-09-12, 사람 결정 — "설계를 단순화해서 데드락 원인을 없앤다",
 * STORY-FUNC-member-002 재작업 지시)</b>: round3는 카운터 갱신·판정을 코드 테이블에 함께
 * 두고 요청 전체를 {@code @Transactional}로 감쌌는데, 같은 트랜잭션에서 도는
 * {@code purgeExpired}(무인덱스 풀스캔 DELETE)와 만나 락 범위가 테이블 전체로 확대되어
 * InnoDB 데드락(500)이 났다(round3 QA FAIL 필수1). round4는 이 요청 경로에서
 * {@code @Transactional}을 완전히 제거했다 — 레이트리밋 카운터는 전용 테이블
 * ({@link MemberSignupRateLimitDao}, MEMBER_SIGNUP_RATE_LIMITS)에 대한 단일 원자 UPSERT
 * 문(autocommit)으로만 갱신하고, 코드 기록({@link MemberSignupVerificationDao#writeCode})도
 * 별도의 단일 원자 UPSERT다 — 두 문장 사이에 걸치는 트랜잭션·락이 없다. 만료행 정리
 * (purge)는 이 요청 경로에서 완전히 빠졌고 {@link MemberSignupMaintenanceScheduler}
 * (@Scheduled)가 배치로 담당한다.
 *
 * <p><b>판정 방식 — round5(2026-09-12, 사람 결정, round4 QA FAIL 재작업 지시(1))</b>: round4는
 * 위 UPSERT의 반환값(JDBC affected-rows: 신규 삽입=1/실제 값 변경=2/무변경=0)으로 200/429를
 * 갈랐는데, 그 의미를 고정하려고 datasource URL에 {@code useAffectedRows=true}(전역
 * 커넥션 속성)를 켰다. 이 속성은 애플리케이션의 모든 UPDATE에 적용되므로 이 FUNC 밖
 * {@code ProductDao.decreaseStock}(FUNC-order-002 소유)의 반환값 시맨틱까지 바꿔, qty=0
 * 주문 라인의 {@code POST /api/orders} 응답이 종전 200에서 409("재고 부족")로 회귀했다(QA
 * 실측). round5는 이 전역 설정을 제거하고 <b>요청 토큰</b> 방식으로 바꿨다: 매 요청이 UUID
 * 토큰을 하나 만들어 {@code touchRateLimit}의 조건부 갱신 절에 {@code last_token}으로 함께
 * 기록하고(허용 조건 미달이면 last_token도 갱신되지 않음), 그 뒤 {@code selectRateLimit}로
 * 다시 읽은 {@code last_token}이 내 토큰과 같으면 허용(200), 다르면 {@code daily_count}로
 * 쿨다운/일일상한을 가른다. 판정이 datasource 커넥션 속성이 아니라 이 FUNC이 직접 쓰고 읽는
 * 값 하나에만 의존한다. {@code ProductDao}/{@code OrderService}는 이 재작업에서 건드리지
 * 않는다 — 그 계약이 다시 깨지지 않았음은 별도 회귀 테스트로 고정한다(qty=0 주문 생성 200).
 */
@Service
public class MemberSignupService {
    private static final Logger log = LoggerFactory.getLogger(MemberSignupService.class);

    static final int CODE_LENGTH = 6;
    static final int EXPIRES_IN_SECONDS = 300; // 5분(SR-231 확정 요건 — 인증코드 6자리, 5분 유효)
    static final int MAX_TARGET_LENGTH = 100; // DB 컬럼(target VARCHAR(100)) 정합(round1 QA FAIL 필수3)
    static final int COOLDOWN_SECONDS = 60; // 동일 target 재발송 쿨다운(round1 QA FAIL 필수4)
    static final int DAILY_REQUEST_LIMIT = 5; // 동일 target 1일 요청 상한(round1 QA FAIL 필수4)

    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String CHANNEL_SMS = "SMS";

    private static final String CODE_TARGET_INVALID = "MEMBER_TARGET_INVALID";
    private static final String CODE_COOLDOWN = "MEMBER_VERIFY_COOLDOWN";
    private static final String CODE_DAILY_LIMIT_EXCEEDED = "MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED";

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    // 국내 휴대폰 번호(하이픈 없이 숫자만 — 화면단에서 정규화해 전달한다고 가정): 01[016789] + 7~8자리
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789][0-9]{7,8}$");

    private final MemberSignupVerificationDao verificationDao;
    private final MemberSignupRateLimitDao rateLimitDao;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    // round3부터 — Spring이 두 생성자(운영용 1-arg / 테스트용 2-arg) 사이에서 어느 것을 주입
    // 대상으로 삼을지 모호해 "No default constructor found"로 기동에 실패했다(실측) —
    // @Autowired로 명시 지정해 해소한다.
    @Autowired
    public MemberSignupService(MemberSignupVerificationDao verificationDao,
                                MemberSignupRateLimitDao rateLimitDao) {
        this(verificationDao, rateLimitDao, Clock.systemDefaultZone());
    }

    /**
     * round3 — 시계 주입 테스트 시임(test seam). 운영 경로는 항상 위 2-arg 생성자(시스템 시계,
     * {@code @Autowired})를 쓴다. 회귀 테스트가 "5분 1초 간격 6회 요청" 같은 시나리오를 실제로
     * 기다리지 않고 결정적으로 검증하려면 {@code now()}를 제어할 수단이 필요하다(package-private
     * — 테스트 전용).
     */
    MemberSignupService(MemberSignupVerificationDao verificationDao,
                         MemberSignupRateLimitDao rateLimitDao, Clock clock) {
        this.verificationDao = verificationDao;
        this.rateLimitDao = rateLimitDao;
        this.clock = clock;
    }

    /**
     * 인증코드 발급/재발급.
     * <p>400 {@code MEMBER_TARGET_INVALID} — target이 비었거나, 100자를 넘거나, 이메일도
     * 휴대폰번호도 아닌 형식.
     * <p>429 {@code MEMBER_VERIFY_COOLDOWN} — 같은 target 재발송을 60초 안에 다시 요청.
     * <p>429 {@code MEMBER_VERIFY_DAILY_LIMIT_EXCEEDED} — 같은 target이 오늘 이미
     * {@value #DAILY_REQUEST_LIMIT}회 요청함.
     * <p>500 {@code MBR-5000} — DB 계층 예외(무인증 엔드포인트라 내부 정보는 절대 노출하지
     * 않는다 — {@link com.sm.lab.shop.web.MemberSignupExceptionHandler} 참고).
     */
    public VerificationCodeResult requestVerificationCode(String target) {
        String trimmed = requireValidTarget(target);
        String channel = resolveChannel(trimmed);
        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        LocalDate dayKey = now.toLocalDate();

        // round5(사람 결정, STORY 재작업 지시(1)) — 별도 테이블에 대한 단일 원자 UPSERT.
        // 이번 요청 고유 토큰을 조건부로 last_token에 기록한 뒤, 재조회한 값이 내 토큰과
        // 같은지로만 판정한다(affected-rows 반환값은 더 이상 쓰지 않는다 — datasource
        // useAffectedRows=true라는 전역 설정에 판정을 기대지 않기 위함, 클래스 javadoc 참고).
        String myToken = UUID.randomUUID().toString();
        rateLimitDao.touchRateLimit(trimmed, dayKey, now, COOLDOWN_SECONDS, DAILY_REQUEST_LIMIT, myToken);
        MemberSignupRateLimit rateLimit = rateLimitDao.selectRateLimit(trimmed, dayKey);
        boolean admitted = rateLimit != null && myToken.equals(rateLimit.getLastToken());
        if (!admitted) {
            throw rejectionFor(rateLimit);
        }

        String code = generateCode();
        LocalDateTime expiresAt = now.plusSeconds(EXPIRES_IN_SECONDS);
        verificationDao.writeCode(channel, trimmed, code, expiresAt, now);

        log.info("회원가입 인증코드 발송(시뮬레이션, 실 게이트웨이 미연동) — channel={}, target={}, "
                + "유효기간={}초", channel, mask(trimmed), EXPIRES_IN_SECONDS);

        return new VerificationCodeResult(channel, trimmed, EXPIRES_IN_SECONDS);
    }

    /**
     * round5 — 재조회한 행의 {@code last_token}이 내 토큰과 다르다(거부)는 뜻이므로, 같은 행의
     * {@code daily_count}만 보고 쿨다운인지 일일상한인지를 가른다(이 조회는 오직 "거부 사유"
     * 안내용 — 판정 자체는 이미 위에서 토큰 비교로 끝났다).
     */
    private MemberSignupApiException rejectionFor(MemberSignupRateLimit rateLimit) {
        int dailyCount = rateLimit != null ? rateLimit.getDailyCount() : 0;
        if (dailyCount >= DAILY_REQUEST_LIMIT) {
            return new MemberSignupApiException(HttpStatus.TOO_MANY_REQUESTS, CODE_DAILY_LIMIT_EXCEEDED,
                    "오늘 인증코드 요청 횟수를 초과했습니다(1일 " + DAILY_REQUEST_LIMIT + "회)");
        }
        return new MemberSignupApiException(HttpStatus.TOO_MANY_REQUESTS, CODE_COOLDOWN,
                "잠시 후 다시 시도해주세요(재발송은 " + COOLDOWN_SECONDS + "초 후 가능합니다)");
    }

    private String requireValidTarget(String target) {
        if (target == null || target.isBlank()) {
            throw invalidTarget("이메일 또는 휴대폰번호를 입력하세요");
        }
        String trimmed = target.trim();
        if (trimmed.length() > MAX_TARGET_LENGTH) {
            throw invalidTarget("이메일 또는 휴대폰번호는 " + MAX_TARGET_LENGTH + "자를 넘을 수 없습니다");
        }
        return trimmed;
    }

    private String resolveChannel(String target) {
        if (EMAIL_PATTERN.matcher(target).matches()) {
            return CHANNEL_EMAIL;
        }
        if (PHONE_PATTERN.matcher(target).matches()) {
            return CHANNEL_SMS;
        }
        throw invalidTarget("이메일 또는 휴대폰번호 형식이 올바르지 않습니다: " + target);
    }

    private MemberSignupApiException invalidTarget(String message) {
        return new MemberSignupApiException(HttpStatus.BAD_REQUEST, CODE_TARGET_INVALID, message);
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        return String.format("%0" + CODE_LENGTH + "d", random.nextInt(bound));
    }

    /** 로그 노출 최소화 — 개인정보(이메일/휴대폰번호) 원문을 로그에 그대로 남기지 않는다. */
    private String mask(String target) {
        if (target.length() <= 2) {
            return "**";
        }
        return target.substring(0, 2) + "*".repeat(target.length() - 2);
    }

    /** 인증코드 발송 응답 — channel/target은 사용자가 입력한 값의 에코(누출 아님), code는 담지 않는다. */
    public record VerificationCodeResult(String channel, String target, int expiresInSeconds) { }
}
