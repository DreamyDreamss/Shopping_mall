// linked_func: FUNC-member-005
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberLoginAttemptDao;
import com.sm.lab.shop.dao.MemberRefreshTokenDao;
import com.sm.lab.shop.domain.MemberCredential;
import com.sm.lab.shop.domain.MemberLoginAttempt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 로그인 — 잠금 판정 → 인증 → 세션(리프레시 토큰 + API 키) 발급(SR-232, INF-MBR-003)의
 * 비즈니스 로직. {@code POST /api/members/login}.
 *
 * <p>이 클래스는 {@code @Transactional}을 전혀 쓰지 않는다(사람 결정 그대로) — 모든 DB
 * 문장을 개별 autocommit 단일 statement로 처리한다(사례집 SR-231 r2 "카운터 증가를 트랜잭션
 * 안에 넣었다가 롤백에 같이 사라짐" 재발 방지, {@code MemberRegistrationService}의 "STEP
 * 분리+비트랜잭션" 패턴과 동일한 사고방식).
 *
 * <p><b>순서·보안(계획 그대로, 사람 확인 반영)</b>:
 * <ol>
 *   <li>잠금 여부를 비밀번호 검증 <b>전에</b> 확인한다(BCrypt 비용 절약). 이 판정은 "회원
 *       존재"가 아니라 "이 email 문자열의 실패 이력"만 보고, 회원 존재 여부와 무관하게 동일한
 *       방식으로 쌓인다(사례집 r5 "존재 판정→인증 순서 오라클"과 다른 이유).</li>
 *   <li>회원 없음 / delYn='Y'(탈퇴) / 비밀번호 불일치 — 이 세 경우 전부 완전히 동일한 401
 *       코드·메시지·카운터 의미로 응답한다.</li>
 *   <li><b>사람 확인 1(메시지 문구)</b> — "이메일 또는 비밀번호가 올바르지 않습니다 (n/5)"로
 *       일반화한다. 사유(미가입/탈퇴/비번오류)는 절대 구분해 노출하지 않는다.</li>
 *   <li>회원이 없어도 더미 해시로 {@code passwordEncoder.matches}를 실행해 응답시간을
 *       비슷하게 만든다(should, 타이밍 사이드채널 완화).</li>
 *   <li><b>사람 확인 2(잠금 트리거)</b> — 5번째 실패 자체를 429로 응답한다(즉시 잠금 알림).
 *       4번째까지는 401(n/5), 5번째 실패는 401이 아니라 곧바로 429 {@code MBR-4291}이다.</li>
 *   <li>성공 시: 실패카운터 reset(단일 DELETE) → refresh token 발급(원문은 응답 1회만 노출,
 *       DB엔 SHA-256 해시만) → 만료·폐기·상한초과 리프레시 토큰 정리(round 9, 무한누적 방지) →
 *       apiKey 조회/신규발급(원자 UPSERT+재조회, 동시 최초 로그인 레이스에서도 1개만 발급).</li>
 * </ol>
 *
 * <p><b>사람 확인 3(ApiKeyAuthFilter DB 폴백)</b>은 이 클래스가 아니라
 * {@link com.sm.lab.shop.web.ApiKeyAuthFilter}에서 처리한다 — 이 서비스는 발급만 책임진다.
 */
@Service
public class MemberLoginService {

    static final int MAX_ATTEMPTS = 5; // 사람 확인 2 — 5회 실패
    static final int LOCK_MINUTES = 10;
    static final int REFRESH_TOKEN_VALID_DAYS = 30;
    // round 9(재작업 지시 3/4) — 회원당 활성 리프레시 토큰 상한. issueSession이 매 성공 로그인마다
    // 만료·폐기 행을 지우고 이 상한을 넘는 오래된 행도 함께 정리한다(무한 누적 방지).
    static final int MAX_ACTIVE_REFRESH_TOKENS = 5;

    private static final String DEL_YN_DELETED = "Y";
    private static final String CODE_INVALID_CREDENTIALS = "MBR-4011";
    private static final String CODE_LOCKED = "MBR-4291";
    // 사람 확인 1 — 사유(미가입/탈퇴/비번오류)를 절대 구분해 노출하지 않는 일반화 문구.
    private static final String MESSAGE_INVALID_CREDENTIALS_TEMPLATE = "이메일 또는 비밀번호가 올바르지 않습니다 (%d/%d)";
    private static final String MESSAGE_LOCKED = "로그인 시도 횟수를 초과했습니다. 잠시 후 다시 시도해 주세요";
    // 순서·보안 4(should) — 회원이 없을 때도 이 더미 해시로 matches를 실행해 응답시간을 비슷하게
    // 만든다(타이밍 사이드채널 완화). 실제 회원 비밀번호와 무관한 임의 시드.
    private static final String DUMMY_PASSWORD_SEED = "no-such-member-timing-mitigation-seed";
    private static final String API_KEY_PREFIX = "mk_";

    private final MemberDao memberDao;
    private final MemberLoginAttemptDao loginAttemptDao;
    private final MemberRefreshTokenDao refreshTokenDao;
    private final MemberApiKeyDao apiKeyDao;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD_SEED);

    @Autowired
    public MemberLoginService(MemberDao memberDao, MemberLoginAttemptDao loginAttemptDao,
                               MemberRefreshTokenDao refreshTokenDao, MemberApiKeyDao apiKeyDao) {
        this(memberDao, loginAttemptDao, refreshTokenDao, apiKeyDao, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — MemberRegistrationService와 동일 시임 패턴. */
    MemberLoginService(MemberDao memberDao, MemberLoginAttemptDao loginAttemptDao,
                        MemberRefreshTokenDao refreshTokenDao, MemberApiKeyDao apiKeyDao, Clock clock) {
        this.memberDao = memberDao;
        this.loginAttemptDao = loginAttemptDao;
        this.refreshTokenDao = refreshTokenDao;
        this.apiKeyDao = apiKeyDao;
        this.clock = clock;
    }

    public LoginResult login(String email, String password) {
        String normalizedEmail = (email == null) ? "" : email.trim();
        LocalDateTime now = LocalDateTime.now(clock);

        // 순서·보안 1 — 비밀번호 검증 전에 잠금 여부부터 확인한다(BCrypt 비용 절약).
        checkNotLocked(normalizedEmail, now);

        MemberCredential credential = memberDao.selectAuthByEmail(normalizedEmail);
        if (!isAuthenticated(credential, password)) {
            throw handleFailure(normalizedEmail, now);
        }

        loginAttemptDao.reset(normalizedEmail);
        return issueSession(credential, now);
    }

    /**
     * 순서·보안 2 — 회원 없음/delYn='Y'/비밀번호 불일치, 이 세 경우를 완전히 동일하게 취급한다.
     * 순서·보안 4(should) — 회원이 없거나 탈퇴한 경우에도 더미 해시로 matches를 돌려 응답시간을
     * 비슷하게 만든다(결과는 버리고 부작용만 취한다).
     */
    private boolean isAuthenticated(MemberCredential credential, String password) {
        String rawPassword = (password == null) ? "" : password;
        if (credential == null || DEL_YN_DELETED.equals(credential.getDelYn())) {
            passwordEncoder.matches(rawPassword, dummyPasswordHash);
            return false;
        }
        return passwordEncoder.matches(rawPassword, credential.getPasswordHash());
    }

    private void checkNotLocked(String email, LocalDateTime now) {
        MemberLoginAttempt attempt = loginAttemptDao.selectAttempt(email);
        if (isCurrentlyLocked(attempt, now)) {
            throw lockedException(attempt.getLockedUntil(), now);
        }
    }

    /**
     * 순서·보안 6 — 원자 UPSERT({@link MemberLoginAttemptDao#touchFailure}) 한 문장으로
     * fail_count를 늘리고, 이번 실패로 상한(5)에 도달했으면 같은 문장이 locked_until도 함께
     * 세팅한다(사람 확인 2 — 5번째 실패 자체가 429). 반환값이 아니라 항상 예외를 던지는
     * 계약이므로 호출부는 {@code throw handleFailure(...)}로 받는다(컴파일러가 도달 불가능
     * 경로를 알 수 있도록).
     */
    private MemberLoginApiException handleFailure(String email, LocalDateTime now) {
        loginAttemptDao.touchFailure(email, now, LOCK_MINUTES, MAX_ATTEMPTS);
        MemberLoginAttempt attempt = loginAttemptDao.selectAttempt(email);
        if (isCurrentlyLocked(attempt, now)) {
            return lockedException(attempt.getLockedUntil(), now);
        }
        int failCount = (attempt == null) ? 1 : attempt.getFailCount();
        return new MemberLoginApiException(HttpStatus.UNAUTHORIZED, CODE_INVALID_CREDENTIALS,
                String.format(MESSAGE_INVALID_CREDENTIALS_TEMPLATE, failCount, MAX_ATTEMPTS));
    }

    private static boolean isCurrentlyLocked(MemberLoginAttempt attempt, LocalDateTime now) {
        return attempt != null && attempt.getLockedUntil() != null && attempt.getLockedUntil().isAfter(now);
    }

    private MemberLoginApiException lockedException(LocalDateTime lockedUntil, LocalDateTime now) {
        long retryAfterSeconds = Math.max(1, Duration.between(now, lockedUntil).getSeconds());
        return new MemberLoginApiException(HttpStatus.TOO_MANY_REQUESTS, CODE_LOCKED, MESSAGE_LOCKED)
                .withRetryAfterSeconds(retryAfterSeconds);
    }

    /**
     * 순서·보안 6 — 성공 경로. 실패카운터는 이미 reset된 뒤 호출된다. refresh token은 원문을
     * 응답 1회만 노출하고 DB엔 SHA-256 해시(소문자 hex)만 저장한다(계약 — 006과의 인터페이스
     * 가정). apiKey는 최초 로그인이면 원자 UPSERT(no-op)+재조회로 정확히 1개만 발급된다
     * (동시 레이스에서도 {@link MemberApiKeyDao#issueIfAbsent} 자체가 흡수).
     *
     * <p>round 9(재작업 지시 3/4, QA FAIL medium 2) — refresh token 발급 직후 이 회원의 만료·
     * 폐기 행을 지우고({@link MemberRefreshTokenDao#purgeExpiredOrRevoked}) 활성 토큰 수를
     * {@link #MAX_ACTIVE_REFRESH_TOKENS}로 제한한다({@link MemberRefreshTokenDao#enforceActiveCap}) —
     * 로그인마다 행이 쌓이기만 해 회원 1명 앞으로 60행까지 쌓인 실측을 재발 방지한다.
     */
    private LoginResult issueSession(MemberCredential credential, LocalDateTime now) {
        String refreshTokenPlain = UUID.randomUUID().toString() + UUID.randomUUID();
        String tokenHash = sha256Hex(refreshTokenPlain);
        LocalDateTime expiresAt = now.plusDays(REFRESH_TOKEN_VALID_DAYS);
        refreshTokenDao.insert(tokenHash, credential.getMemberId(), now, expiresAt);
        refreshTokenDao.purgeExpiredOrRevoked(credential.getMemberId(), now);
        refreshTokenDao.enforceActiveCap(credential.getMemberId(), MAX_ACTIVE_REFRESH_TOKENS);

        String candidateApiKey = API_KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");
        apiKeyDao.issueIfAbsent(credential.getMemberId(), candidateApiKey, now);
        String apiKey = apiKeyDao.selectByMemberId(credential.getMemberId());

        return new LoginResult(credential.getMemberId(), credential.getMemberName(), credential.getGrade(),
                apiKey, refreshTokenPlain, expiresAt);
    }

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

    /** 로그인 성공 응답 — apiKey/refreshToken은 이 응답에서만 원문 그대로 노출된다. */
    public record LoginResult(String memberId, String memberName, String grade, String apiKey,
                               String refreshToken, LocalDateTime refreshTokenExpiresAt) { }
}
