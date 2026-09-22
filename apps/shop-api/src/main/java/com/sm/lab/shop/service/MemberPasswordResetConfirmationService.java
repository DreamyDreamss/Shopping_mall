// linked_func: FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberPasswordResetDao;
import com.sm.lab.shop.domain.MemberPasswordReset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;

/**
 * 비밀번호 재설정 — 확정(SR-234, INF-MBR-007). {@code POST
 * /api/members/password-resets/confirmations}의 비즈니스 로직. FUNC-member-008(요청 API)과 짝을
 * 이루며, {@code MEMBER_PASSWORD_RESETS.consumed_at}/{@code attempt_count}를 이 FUNC이 처음
 * 갱신한다(V5 마이그레이션 주석이 이미 이 FUNC의 소유를 예고).
 *
 * <p><b>target/newPassword 검증 재사용(사람 수정 (2))</b> — 형식 검증·정규화 로직을 문자 단위로
 * 복제하지 않고 {@link MemberPasswordResetService#requireValidTarget}/{@link
 * MemberPasswordResetService#resolveChannel}/{@link MemberPasswordResetService#normalize}·
 * {@link MemberRegistrationService#requireValidPassword}를 그대로 호출한다(가시성만 넓힌 기존
 * 메서드 — 이동·추출 아님). 두 형제 서비스가 던지는 예외를 이 FUNC 전용
 * {@link MemberPasswordResetConfirmationApiException}으로 감싸 재던진다(예외 타입은 FUNC별
 * 분리 관례 유지, 판정 로직만 공유).
 *
 * <p><b>존재 오라클 방지(사례집 SR-231 r5, 이 FUNC의 핵심 위험)</b> — 코드 확정
 * ({@link #confirmPasswordReset})은 회원 조회보다 항상 먼저 실행되고, "행 없음"조차 단순 오답과
 * 완전히 동일한 코드({@code MBR-4102})로 수렴한다(사람 수정 (1)). 회원 조회 이후에도 결과가
 * 있든 없든 응답은 204로 동일하다 — 이 클래스의 어떤 메서드도 회원 존재 여부를 드러내지 않는다.
 */
@Service
public class MemberPasswordResetConfirmationService {
    private static final Logger log = LoggerFactory.getLogger(MemberPasswordResetConfirmationService.class);

    static final int MAX_CONFIRM_ATTEMPTS = 5; // 로그인/가입과 동일 상수값(SR-234 STORY 확정)

    private static final String CHANNEL_EMAIL = "EMAIL";

    private static final String CODE_EXPIRED = "MBR-4101";
    private static final String CODE_MISMATCH = "MBR-4102";
    private static final String CODE_LOCKED = "MBR-4103";

    private static final String MESSAGE_EXPIRED = "인증코드가 만료되었습니다. 다시 요청해 주세요";
    private static final String MESSAGE_MISMATCH = "코드가 올바르지 않습니다";
    private static final String MESSAGE_LOCKED = "코드 확인 시도 횟수를 초과했습니다. 다시 요청해 주세요";

    private final MemberPasswordResetDao passwordResetDao;
    private final MemberDao memberDao;
    private final MemberPasswordResetConfirmationWriter writer;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public MemberPasswordResetConfirmationService(MemberPasswordResetDao passwordResetDao, MemberDao memberDao,
                                                    MemberPasswordResetConfirmationWriter writer) {
        this(passwordResetDao, memberDao, writer, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — 형제 서비스와 동일 시임 패턴. */
    MemberPasswordResetConfirmationService(MemberPasswordResetDao passwordResetDao, MemberDao memberDao,
                                            MemberPasswordResetConfirmationWriter writer, Clock clock) {
        this.passwordResetDao = passwordResetDao;
        this.memberDao = memberDao;
        this.writer = writer;
        this.clock = clock;
    }

    /**
     * 코드 확정 + 새 비밀번호 반영 + 전 기기 로그아웃(STORY "순서·보안" 절 1~11단계 그대로).
     * 성공/회원미발견 모두 예외 없이 정상 반환한다(컨트롤러가 204로 응답) — 회원 존재 여부를
     * 응답으로 절대 구분하지 않는다. 예외는 400(형식 오류)·410(만료)·409(불일치/시도초과)뿐이다.
     */
    public void confirmPasswordReset(String target, String code, String newPassword) {
        // 1. target 형식 검증(재사용, 복제 아님).
        String trimmedTarget;
        String channel;
        try {
            trimmedTarget = MemberPasswordResetService.requireValidTarget(target);
            channel = MemberPasswordResetService.resolveChannel(trimmedTarget);
        } catch (MemberPasswordResetApiException e) {
            throw new MemberPasswordResetConfirmationApiException(e.getHttpStatus(), e.getCode(), e.getMessage());
        }

        // 2. newPassword 형식 검증(재사용). target보다 뒤, 코드 확인보다 앞 — 형식이 잘못된
        // 요청은 attempt_count를 소모하지 않는다.
        try {
            MemberRegistrationService.requireValidPassword(newPassword);
        } catch (MemberRegistrationApiException e) {
            throw new MemberPasswordResetConfirmationApiException(e.getHttpStatus(), e.getCode(), e.getMessage());
        }

        // 3. target 정규화(재사용) — FUNC-008과 항상 같은 코드 경로로 동일하게 유지된다.
        String normalizedTarget = MemberPasswordResetService.normalize(channel, trimmedTarget);

        // 4. 코드 해싱(로컬 구현 — 이 해시 함수 자체는 재사용 대상이 아니다, 코드베이스 전역 관례).
        String codeHash = sha256Hex((code == null) ? "" : code);

        LocalDateTime now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MILLIS);

        // 5. 원자 확정 시도(비트랜잭션). 0행이면 재조회해 사유를 구분한다(항상 예외를 던짐).
        int confirmed = passwordResetDao.confirmIfCodeMatches(normalizedTarget, codeHash, now, MAX_CONFIRM_ATTEMPTS);
        if (confirmed == 0) {
            handleConfirmFailure(normalizedTarget, now);
            return;
        }

        // 6. BCrypt — 회원 발견 여부와 무관하게 항상 실행(타이밍 오라클 방지).
        String passwordHash = passwordEncoder.encode(newPassword);

        // 7. 회원 조회(이번이 처음 — 그 전 어떤 단계도 MEMBERS를 조회하지 않는다).
        String email = CHANNEL_EMAIL.equals(channel) ? normalizedTarget : null;
        String phoneNorm = CHANNEL_EMAIL.equals(channel) ? null : normalizedTarget;
        String memberId = memberDao.selectMemberIdByResetTarget(email, phoneNorm);

        // 8. 못 찾으면 조용히 204(로그 없음).
        if (memberId == null) {
            return;
        }

        // 9. 비밀번호 반영 + 전 기기 로그아웃(별도 빈, 트랜잭션).
        writer.applyNewPassword(memberId, passwordHash, now);

        // 10. 완료 로그 — Writer 트랜잭션이 커밋된 뒤에만 한 줄(회원 미발견 경로에는 로그 없음).
        log.info("비밀번호 재설정 완료 — target={}", mask(normalizedTarget));
    }

    /**
     * 5단계 실패 분기(SR-298 원자화 — 사람 수정 (1) 개정 "행 없음도 오답과 동일 코드"는 그대로
     * 유지). {@code incrementAttemptCount}를 가장 먼저·무조건 시도한다 — 이 UPDATE 자체가
     * {@code attempt_count < maxAttempts}를 조건으로 갖는 원자 증가이므로(select→분기→update
     * 금지 하우스룰), 1행이면 그 자체로 "미만료·미소비·상한 미만·단순 오답"이 확정된 것이라
     * {@link #selectByTarget}를 부르지 않고 바로 오답 예외를 던진다. 0행일 때만 재조회해
     * 원인만 분류한다(재증가 없음) — 상한 도달만이 0행의 유일한 사유가 아니므로(이미 소비·이미
     * 만료·행 없음도 0행), STORY-1(SR-298.1) 선례와 동일하게 4갈래로 나눈다. 모든 경로가
     * 예외를 던진다.
     */
    private void handleConfirmFailure(String normalizedTarget, LocalDateTime now) {
        int incremented = passwordResetDao.incrementAttemptCount(normalizedTarget, now, MAX_CONFIRM_ATTEMPTS);
        if (incremented == 1) {
            // 미만료·미소비·상한 미만인 행에서 증가에 성공했다 — 남은 사유는 단순 오답뿐이다.
            throw mismatchException();
        }

        // 0행 — 원인 판별용 재조회(경합 판정이 아니라 오류 코드 분기용, 증가는 이미 원자적으로 끝남).
        MemberPasswordReset stored = passwordResetDao.selectByTarget(normalizedTarget);
        if (stored == null) {
            // 행 없음 — "요청한 적 없는 target"이 다른 신호를 내면 그 자체로 존재 오라클이 된다.
            throw mismatchException();
        }
        if (stored.getConsumedAt() != null || !stored.getExpiresAt().isAfter(now)) {
            throw new MemberPasswordResetConfirmationApiException(HttpStatus.GONE, CODE_EXPIRED, MESSAGE_EXPIRED);
        }
        if (stored.getAttemptCount() >= MAX_CONFIRM_ATTEMPTS) {
            throw new MemberPasswordResetConfirmationApiException(HttpStatus.CONFLICT, CODE_LOCKED, MESSAGE_LOCKED);
        }
        // 방어용 폴백 — 통상은 위 세 조건 중 하나가 참이어야 0행을 설명하지만, 진단 재조회
        // (selectByTarget) 직전에 재요청(쿨다운 경과)으로 행이 리셋되는 등의 경합이 끼면 도달할
        // 수 있다. 그 경우도 결과는 옳다(409 MBR-4102) — 범위 밖(별도 테스트를 강제하지 않는다).
        throw mismatchException();
    }

    private static MemberPasswordResetConfirmationApiException mismatchException() {
        return new MemberPasswordResetConfirmationApiException(HttpStatus.CONFLICT, CODE_MISMATCH, MESSAGE_MISMATCH);
    }

    /** MemberPasswordResetService/MemberLoginService와 동일 로컬 구현(공유 유틸 추출은 범위 밖). */
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

    /** 로그 노출 최소화 — MemberPasswordResetService#mask와 동일 로직(로그 목적 로컬 구현). */
    private static String mask(String target) {
        if (target.length() <= 2) {
            return "**";
        }
        return target.substring(0, 2) + "*".repeat(target.length() - 2);
    }
}
