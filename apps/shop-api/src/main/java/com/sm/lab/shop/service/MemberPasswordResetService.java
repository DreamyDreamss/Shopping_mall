// linked_func: FUNC-member-008, FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-008.md, docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberPasswordResetDao;
import com.sm.lab.shop.dao.MemberPasswordResetRateLimitDao;
import com.sm.lab.shop.domain.MemberPasswordReset;
import com.sm.lab.shop.domain.MemberPasswordResetRateLimit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * 비밀번호 재설정 — 코드 요청(SR-234, INF-MBR-006). {@code POST
 * /api/members/password-resets/codes}의 비즈니스 로직. target(이메일 또는 휴대폰번호) 형식으로
 * 채널을 자동 판별하고, 정규화한 뒤 6자리 코드를 생성해 10분 유효로 저장하고 "발송"한다.
 *
 * <p><b>스코프(이 FUNC-member-008 한정)</b>: 코드 확인 + 새 비밀번호 반영 + 전 기기 로그아웃은
 * 별도 FUNC(FUNC-member-009, INF-MBR-007)의 책임이다 — 이 서비스는 그 흐름을 구현하지 않는다.
 *
 * <p><b>회원 조회를 하지 않는다</b> — 이 클래스 생성자에 {@code MemberDao}를 주입하지 않는 것
 * 자체가 이 결정의 구조적 강제다. SR 확정 문답("존재 여부는 새지 않게 요청 API는 항상 202")에
 * 따라 회원 존재/탈퇴 여부와 무관하게 정확히 같은 처리 경로·같은 202 응답을 낸다(STORY "폴백·
 * 우회 경로의 자격 판정" 절 — 사례집 SR-231 r5 "존재 판정 오라클"의 일반화 적용).
 *
 * <p><b>target 정규화(사람 수정 (1))</b> — 이메일은 {@code trim().toLowerCase()}, 휴대폰은
 * {@link com.sm.lab.shop.service.MemberRegistrationService#normalizePhone}과 동일 규칙
 * ({@code phone.replaceAll("[-\\s]", "")})을 이 서비스 안에 그대로 복제한다(공유 유틸 추출은
 * 범위 밖 — 기존 서비스별 복제 관례). PK(target)가 정규화 값이므로 같은 대상의 다른 표기는 같은
 * 행을 공유해 같은 쿨다운이 걸린다.
 *
 * <p><b>휴대폰 형식(round2, 사람 수정)</b> — round1은 {@link #PHONE_PATTERN}을 하이픈 허용
 * 형태로 넓혔는데, 그 결과 같은 입력({@code "010-1234-5678"})이 이 API에서는 202, 형제
 * 가입 API({@link MemberRegistrationService})에서는 400이 되는 불일치가 생겼다(QA 권고,
 * round1 재작업 지시 low(4)). 이 값은 {@code MemberRegistrationService.PHONE_PATTERN}
 * ({@code "^01[016789][0-9]{7,8}$"}, 하이픈 불허)과 문자 단위로 동일하게 되돌린다 — 두 API가
 * 같은 target 형식 정의를 공유해야 같은 입력에 같은 판정을 낸다(그 클래스의 채널 판정 정규식
 * 복제 관례와 동일 이유). 이 결과로 {@link #normalize}의 휴대폰 하이픈·공백 제거 분기는
 * (형식 검증을 통과한 값에는 하이픈·공백이 없으므로) 사실상 도달하지 않는다 — 참조 구현
 * ({@code normalizePhone})과 문자 단위로 동일하게 유지하는 것이 목적이라 그대로 둔다(사람 결정,
 * round2 재작업 지시 low(6)과 동일 판단).
 *
 * <p><b>발송 시뮬레이션(사람 수정 (2))</b> — 이 랩에는 실제 발송 게이트웨이가 없다.
 * {@link MemberSignupService#requestVerificationCode}의 로그 한 줄과 동일한 모양·수준으로만
 * "발송"을 남긴다 — 코드 원문은 이 로그에도, 다른 어떤 로그·응답에도 남기지 않는다. 새 전달
 * 추상화·설정 키는 만들지 않는다.
 *
 * <p><b>발송 로그 순서(round2, QA FAIL 필수1 재작업)</b> — round1은 "발송" 로그를
 * {@code touchRequest}(쿨다운 UPSERT) 호출보다 먼저 무조건 남겼다. 그 결과 60초 안 재요청도
 * (a) 발송 이벤트를 또 남기고(SR "재전송은 60초 쿨다운"이 발송 경로에는 안 걸림), (b) 방금
 * 생성·발송한 코드는 UPSERT가 버리고 DB엔 이전 코드의 해시만 남아, 이 로그가 실제 게이트웨이로
 * 교체되는 순간 수신자가 절대 검증되지 않는 코드를 받는 결함이 있었다. round2는
 * {@code touchRequest} 직후 {@link MemberPasswordResetDao#selectByTarget}으로 다시 읽어, 저장된
 * {@code code_hash}가 이번 요청이 만든 해시와 같을 때만(=쿨다운에 걸리지 않고 실제로 반영됐을
 * 때만) 발송 로그를 남긴다(SR-231 round5 {@code last_token} 재조회 대조와 동일 기법). 응답은
 * 어느 경우든 무조건 202·상수 바디이므로 이 판정은 로그 여부에만 쓰이고 오라클을 만들지 않는다.
 *
 * <p><b>일일 요청 상한(SR-297 #2, INF-MBR-006)</b> — target별 하루 {@value #DAILY_REQUEST_LIMIT}회
 * 상한을 전용 카운터 테이블({@link MemberPasswordResetRateLimitDao}, SCH-MBR-009,
 * SR-297 #1이 이미 만든 테이블)에 대한 원자 UPSERT로 판정한다. 이 게이트는 코드 테이블
 * UPSERT({@code touchRequest})보다 먼저 실행되고, 거부되면 {@code touchRequest} 자체를 호출하지
 * 않는다(발송·코드 갱신 생략) — 그러나 응답은 이 경우에도 항상 동일한 202 바디다(상한 오라클도
 * 만들지 않는다). 판정 방식은 {@link MemberSignupService#requestVerificationCode}와 완전히
 * 같다(요청 토큰 재조회, round5/round6 원리).
 *
 * <p><b>알려진 동작 — 자정 경계(STEP 3-0 사람 확인)</b> — 자정 직전 요청 뒤 60초 이내에 날짜가
 * 바뀌면, 카운터 테이블은 {@code day_key}가 PK라 새 (target, 오늘) 행이 생겨 쿨다운 없이
 * admit(일일 카운트 소비)하지만, 코드 테이블의 쿨다운은 여전히 어제 {@code created_at} 기준
 * 60초 이내라 거부할 수 있다 — 이 조합이면 일일 카운트만 소비되고 코드는 갱신되지 않는다. 응답은
 * 이미 어느 경우든 202·동일 바디라 오라클은 생기지 않으므로 이 동작을 그대로 허용한다(교차-
 * day_key 조회로 이 조합을 막는 것은 범위 밖 — INF-MBR-006에도 동일 문장을 기록한다).
 */
@Service
public class MemberPasswordResetService {
    private static final Logger log = LoggerFactory.getLogger(MemberPasswordResetService.class);

    static final int CODE_LENGTH = 6;
    static final int EXPIRES_IN_SECONDS = 600; // 10분(SR-234 확정 요건)
    static final int MAX_TARGET_LENGTH = 100; // MEMBER_PASSWORD_RESETS.target VARCHAR(100) 정합
    static final int COOLDOWN_SECONDS = 60; // 동일 target 재요청 쿨다운(SR-234 확정 요건)
    static final int DAILY_REQUEST_LIMIT = 5; // 동일 target 1일 요청 상한(SR-297 #2 확정 요건)

    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String CHANNEL_SMS = "SMS";

    private static final String CODE_TARGET_INVALID = "MBR-4100";

    // MemberSignupService와 동일한 이메일 패턴(대소문자 무관 — 별도 소문자화는 정규화 단계에서).
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    // round2(QA 권고, 재작업 지시 low(4)) — 형제 가입 서비스 MemberRegistrationService.
    // PHONE_PATTERN(하이픈 불허, 숫자만)과 문자 단위로 동일하게 되돌린다. round1은 이 값을
    // 하이픈 허용으로 넓혔는데, 그 결과 같은 입력이 재설정에서는 202·가입에서는 400이 되는
    // 불일치가 생겼다 — 두 API가 같은 target 형식을 공유해야 화면(FUNC-007/009)이 어느 API에
    // 보내도 같은 판정을 받는다(클래스 상단 설명 참고).
    private static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789][0-9]{7,8}$");

    private final MemberPasswordResetDao dao;
    private final MemberPasswordResetRateLimitDao rateLimitDao;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Autowired
    public MemberPasswordResetService(MemberPasswordResetDao dao, MemberPasswordResetRateLimitDao rateLimitDao) {
        this(dao, rateLimitDao, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — MemberSignupService와 동일 시임 패턴. */
    MemberPasswordResetService(MemberPasswordResetDao dao, MemberPasswordResetRateLimitDao rateLimitDao,
                                Clock clock) {
        this.dao = dao;
        this.rateLimitDao = rateLimitDao;
        this.clock = clock;
    }

    /**
     * 비밀번호 재설정 코드 요청. 형식 오류(빈 값/100자 초과/이메일·휴대폰 형식 불일치)만
     * 400 {@code MBR-4100}으로 거부하고, 그 외 모든 경우(회원 존재 여부·쿨다운 위반·일일 상한
     * 초과 포함)는 항상 202를 반환한다 — 실제로 코드가 갱신됐는지 여부를 응답에 드러내지 않는다
     * (존재 오라클·쿨다운 오라클·상한 오라클을 아예 만들지 않는다, STORY "데이터" 절 참고).
     */
    public VerificationCodeResult requestPasswordResetCode(String target) {
        String trimmed = requireValidTarget(target);
        String channel = resolveChannel(trimmed);
        String normalizedTarget = normalize(channel, trimmed);

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);
        LocalDate dayKey = now.toLocalDate();

        // SR-297 #2, STORY "순서·보안" 결정1 — 일일 상한 게이트가 코드 테이블 UPSERT보다 먼저
        // 실행된다. MemberSignupService.requestVerificationCode와 동일한 요청-토큰 판정
        // (round5/round6)을 그대로 재사용 — 반환값(affected rows)이 아니라 재조회한 last_token
        // 으로 admit을 가른다.
        String myToken = UUID.randomUUID().toString();
        rateLimitDao.touchDailyLimit(normalizedTarget, dayKey, now, COOLDOWN_SECONDS, DAILY_REQUEST_LIMIT, myToken);
        MemberPasswordResetRateLimit rateLimit = rateLimitDao.selectRateLimit(normalizedTarget, dayKey);
        boolean admitted = rateLimit != null && myToken.equals(rateLimit.getLastToken());

        if (admitted) {
            LocalDateTime expiresAt = now.plusSeconds(EXPIRES_IN_SECONDS);
            String code = generateCode();
            String codeHash = sha256Hex(code);

            dao.touchRequest(normalizedTarget, codeHash, expiresAt, now, COOLDOWN_SECONDS);

            // round2(QA FAIL 필수1 재작업) — 쿨다운으로 버려진 요청(60초 이내 재요청)은 위
            // UPSERT가 이 codeHash를 저장하지 않는다. 재조회한 code_hash가 방금 만든 codeHash와
            // 같을 때만 "이번 요청이 실제로 반영됐다"는 뜻이므로 그때만 발송 로그를 남긴다
            // (SR-231 round5 last_token 재조회 대조와 동일 기법). 응답은 어느 경우든 무조건
            // 202·상수 바디이므로 이 판정은 로그 여부에만 쓰이고 오라클을 만들지 않는다
            // (클래스 상단 설명 참고).
            MemberPasswordReset stored = dao.selectByTarget(normalizedTarget);
            boolean writeReflected = stored != null && codeHash.equals(stored.getCodeHash());
            if (writeReflected) {
                log.info("비밀번호 재설정 코드 발송(시뮬레이션, 실 게이트웨이 미연동) — channel={}, target={}, "
                        + "유효기간={}초", channel, mask(normalizedTarget), EXPIRES_IN_SECONDS);
            }
        } else {
            // SR-297 #2, STORY "순서·보안" 결정1 5단계 — 상한 초과는 서버 로그로만 남긴다(새
            // 오류·오라클 없음, dao.touchRequest는 아예 호출하지 않는다 — 발송·코드 갱신 생략).
            // 원문 target·코드는 절대 담지 않는다(mask() 재사용, 사람 수정 (2)의 PII 최소화 관례).
            //
            // round2(QA CONCERNS round1 재작업 지시 1) — admitted=false는 쿨다운 거부와 일일상한
            // 거부 둘 다에서 발생한다(UPSERT 조건이 last_requested_at<=now-COOLDOWN AND
            // daily_count<DAILY_REQUEST_LIMIT 둘 다 만족해야 admit). round1은 사유를 가리지 않고
            // 무조건 "일일 상한 초과"라 적어, 재클릭(최빈 케이스, dailyCount 1)까지 그 문구가
            // 지배했다. 형제 MemberSignupService#rejectionFor와 동일하게 재조회한 dailyCount로
            // 사유를 가른다 — 응답은 어느 사유든 202 동일이라 오라클과 무관(로그 전용 판별).
            int dailyCount = rateLimit != null ? rateLimit.getDailyCount() : 0;
            String reason = dailyCount >= DAILY_REQUEST_LIMIT ? "일일 상한 초과" : "쿨다운";
            log.info("비밀번호 재설정 코드 요청 거부({}) — target={}, dailyCount={}",
                    reason, mask(normalizedTarget), dailyCount);
        }

        return new VerificationCodeResult(channel, normalizedTarget, EXPIRES_IN_SECONDS);
    }

    /**
     * FUNC-member-009(확정 API) 재사용(package-private static, 가시성만 넓힘 — 본문·정규식·
     * 예외 타입·상수는 문자 하나도 바꾸지 않는다. 인스턴스 필드를 쓰지 않아 static화가 안전하다).
     */
    static String requireValidTarget(String target) {
        if (target == null || target.isBlank()) {
            throw invalidTarget("이메일 또는 휴대폰번호를 입력하세요");
        }
        String trimmed = target.trim();
        if (trimmed.length() > MAX_TARGET_LENGTH) {
            throw invalidTarget("이메일 또는 휴대폰번호는 " + MAX_TARGET_LENGTH + "자를 넘을 수 없습니다");
        }
        return trimmed;
    }

    /** FUNC-member-009 재사용(package-private static, 가시성만 넓힘 — 위 requireValidTarget과 동일 근거). */
    static String resolveChannel(String target) {
        if (EMAIL_PATTERN.matcher(target).matches()) {
            return CHANNEL_EMAIL;
        }
        if (PHONE_PATTERN.matcher(target).matches()) {
            return CHANNEL_SMS;
        }
        throw invalidTarget("이메일 또는 휴대폰번호 형식이 올바르지 않습니다: " + target);
    }

    /**
     * 사람 수정 (1) — 이메일은 trim+소문자, 휴대폰은 하이픈·공백 제거(숫자만). 정규화 후
     * 재검증은 하지 않는다 — 두 변환 모두 위 패턴들의 매칭 결과를 바꾸지 않는 변환이다.
     * FUNC-member-009 재사용(package-private static, 가시성만 넓힘).
     */
    static String normalize(String channel, String trimmedTarget) {
        if (CHANNEL_EMAIL.equals(channel)) {
            return trimmedTarget.toLowerCase(Locale.ROOT);
        }
        return trimmedTarget.replaceAll("[-\\s]", "");
    }

    /**
     * static화(Dev 기록 — 계획은 requireValidTarget/resolveChannel/normalize 3개만 지목했지만,
     * 이 메서드는 그 둘이 호출하는 헬퍼라 static이 아니면 static 메서드에서 호출할 수 없어
     * 컴파일이 깨진다. 인스턴스 필드를 쓰지 않아(상수만 참조) static화가 안전 — 가시성은
     * private 그대로 유지, 본문·상수는 문자 하나도 바꾸지 않았다).
     */
    private static MemberPasswordResetApiException invalidTarget(String message) {
        return new MemberPasswordResetApiException(HttpStatus.BAD_REQUEST, CODE_TARGET_INVALID, message);
    }

    private String generateCode() {
        int bound = (int) Math.pow(10, CODE_LENGTH);
        return String.format("%0" + CODE_LENGTH + "d", random.nextInt(bound));
    }

    /**
     * MemberLoginService#sha256Hex와 동일 로컬 구현을 이 서비스 안에 그대로 복제한다(공유 유틸
     * 추출은 범위 밖 — STORY "범위 밖" 절).
     */
    private static String sha256Hex(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // JDK 표준 알고리즘 — 정상 구동 환경에서는 발생하지 않는다.
            throw new IllegalStateException("SHA-256 알고리즘을 사용할 수 없습니다", e);
        }
    }

    /** 로그 노출 최소화 — 개인정보(이메일/휴대폰번호) 원문을 로그에 그대로 남기지 않는다. */
    private static String mask(String target) {
        if (target.length() <= 2) {
            return "**";
        }
        return target.substring(0, 2) + "*".repeat(target.length() - 2);
    }

    /** 코드 요청 응답 — target은 정규화된 값(사람 수정 (1), 원문 echo가 아님), code는 담지 않는다. */
    public record VerificationCodeResult(String channel, String target, int expiresInSeconds) { }
}
