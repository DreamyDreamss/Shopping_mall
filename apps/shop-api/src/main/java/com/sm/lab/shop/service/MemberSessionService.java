// linked_func: FUNC-member-006
// spec: docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberRefreshTokenDao;
import com.sm.lab.shop.domain.Member;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * 로그아웃·자동 로그인(리프레시) — SR-232, INF-MBR-004. {@code POST /api/members/sessions/logout}·
 * {@code POST /api/members/sessions/refresh}의 비즈니스 로직(GATE-005 round2 인터페이스 경계표,
 * 사람 확정 — STORY "구현 계획" 절 그대로).
 *
 * <p>{@code refresh()}는 005와 동일 house 결정을 유지한다({@code @Transactional} 미사용 — 사례집
 * SR-231 r2 "카운터 증가를 트랜잭션 안에 넣었다가 롤백에 같이 사라짐" 재발 방지 원칙). 모든 DB
 * 문장은 개별 autocommit 단일 statement이고, 각 statement가 {@code WHERE ... IS NULL}로
 * idempotent해 중간 실패에도 재시도가 안전하다(계획 "데이터" 절).
 *
 * <p><b>{@code logout()}만은 예외적으로 {@code @Transactional}이다</b>(QA round1 CONCERNS
 * 재작업 지시 1, 사람 확정 — 클래스 전체가 아니라 이 메서드 한정). SR-231 r2의 교훈은 "실패
 * 경로에서 남아야 하는 카운터를 트랜잭션에 넣지 말라"는 것이었는데, 로그아웃의 두 폐기는 그런
 * 카운터가 아니라 "둘 다 함께 성공하거나 함께 실패해야 하는" 단일 논리적 동작이라 트랜잭션이
 * 맞다 — 리프레시 토큰 전체 폐기 후 apiKey 폐기가 중간에 끊기면(부분 실패), 롤백으로 어느 쪽도
 * 폐기되지 않은 이전 상태로 되돌아가 재시도가 그대로 성립한다(idempotent 재시도 논거를
 * 트랜잭션이 대체한다).
 *
 * <p><b>로그아웃</b> — ① {@code X-Api-Key} 헤더 값으로 {@link MemberApiKeyDao#selectMemberIdByApiKey}
 * (기존 메서드 재사용, del_yn·revoked 필터가 이미 있음)로 memberId 해석 → ② 못 찾으면(정적
 * admin 키·이미 폐기된 키 등) idempotent 204 no-op(에러 아님, 계획 "폴백·우회 경로" 절) →
 * ③ 찾으면 {@link MemberRefreshTokenDao#revokeAllForMember}(이 회원의 활성 리프레시 토큰 전체
 * 폐기 — 개별 아님) → ④ {@link MemberApiKeyDao#revokeByMemberId}. 순서를 이렇게(리프레시 먼저,
 * apiKey 나중) 둔 이유 — 트랜잭션이 있어도 순서는 "부분 실패가 방금 일어났다면 무엇이 안전한
 * 상태인가"를 결정한다: 두 번째 문장에서 예외가 나 트랜잭션이 롤백되면 둘 다 취소되어 문제가
 * 없지만, 혹시 이 메서드가 트랜잭션 경계 밖에서 재사용되거나 트랜잭션 매니저가 없는 테스트
 * 컨텍스트에서 개별 호출될 가능성에 대비해 "apiKey가 아직 안 죽었다면 재시도 가능"이 되도록
 * 리프레시 폐기를 먼저 한다(round1에서 지적된 "apiKey만 죽고 refreshToken이 살아 refresh로
 * 세션이 되살아나는" 방향과 반대로, 최악의 경우도 SR 수용 기준 "로그아웃 시 리프레시 토큰 폐기"
 * 쪽으로 안전하게 치우친다).
 *
 * <p><b>refresh</b> — ① 입력 원문을 005와 동일한 SHA-256(소문자 hex)으로 해시 → ②
 * {@link MemberRefreshTokenDao#selectActiveByTokenHash}(폐기·만료 필터가 SQL WHERE에 있음)로
 * memberId 조회 → ③ {@link MemberDao#selectById}(기존 메서드, del_yn='N' 필터 이미 있음)로 회원
 * 활성 여부 확인 — 토큰 미존재/만료/폐기/회원탈퇴 4가지 실패 경우 전부 완전히 동일한 401
 * {@code MBR-4012}(사유 비노출, 005의 "3경우 동일 401" 원칙을 refresh에도 그대로 적용) →
 * ④ 통과 시 새 refreshToken 발급(rotate) → 구토큰 개별 폐기 → 005의 하우스키핑 재사용 →
 * apiKey rotate(재발급) → 로그인과 동일 응답 셰이프로 200.
 *
 * <p>레이트리밋 없음(계획 "순서·보안" 근거 — refreshToken은 UUID×2라 무차별 대입 표면이 사실상
 * 없음, 과설계 금지) — 이 클래스에는 실패 카운터 자체가 없다.
 */
@Service
public class MemberSessionService {

    static final int REFRESH_TOKEN_VALID_DAYS = 30; // 005와 동일 정책값(파일 경계상 상수는 중복 선언)
    static final int MAX_ACTIVE_REFRESH_TOKENS = 5;
    private static final String API_KEY_PREFIX = "mk_";

    private static final String CODE_INVALID_SESSION = "MBR-4012";
    // 계획 "오류 코드 구분 노출 금지" — 토큰 없음/만료/폐기/회원탈퇴 4가지를 절대 구분하지 않는다.
    private static final String MESSAGE_INVALID_SESSION = "유효하지 않거나 만료된 로그인 정보입니다";

    private final MemberDao memberDao;
    private final MemberApiKeyDao apiKeyDao;
    private final MemberRefreshTokenDao refreshTokenDao;
    private final Clock clock;

    @Autowired
    public MemberSessionService(MemberDao memberDao, MemberApiKeyDao apiKeyDao,
                                 MemberRefreshTokenDao refreshTokenDao) {
        this(memberDao, apiKeyDao, refreshTokenDao, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — MemberLoginService와 동일 시임 패턴. */
    MemberSessionService(MemberDao memberDao, MemberApiKeyDao apiKeyDao,
                          MemberRefreshTokenDao refreshTokenDao, Clock clock) {
        this.memberDao = memberDao;
        this.apiKeyDao = apiKeyDao;
        this.refreshTokenDao = refreshTokenDao;
        this.clock = clock;
    }

    /**
     * 로그아웃 — idempotent + {@code @Transactional}(이 메서드 한정, QA round1 재작업 지시 1 —
     * 사람 확정). {@code apiKey}가 {@code MEMBER_API_KEYS}에 없으면(정적 admin 키, 이미 폐기된
     * 키 등) 두 DAO 호출 자체가 일어나지 않고 조용히 반환한다(계획 "폴백·우회 경로의 자격 판정"
     * 절 — "admin 키로 logout 호출"은 의도된 동작이며 에러가 아니다).
     *
     * <p>호출 순서는 리프레시 토큰 전체 폐기 → apiKey 폐기(round1 지적과 반대로 뒤집음). 두 번째
     * 문장이 실패하면 트랜잭션이 롤백돼 둘 다 취소되지만, 혹시라도 트랜잭션 경계 밖에서 부분
     * 적용이 남는 상황에서도 이 순서면 "apiKey는 아직 유효 → 재시도 가능"이 되어 안전한 쪽으로
     * 치우친다(반대 순서였던 round1 구현은 apiKey만 죽고 refreshToken이 살아남아 재로그인 없이
     * refresh로 세션이 되살아나는 결함이 있었다 — TC-006-27 계열이 실측).
     */
    @Transactional
    public void logout(String apiKey) {
        String memberId = apiKeyDao.selectMemberIdByApiKey(apiKey);
        if (memberId == null) {
            return; // idempotent no-op — 이미 로그아웃된 상태와 동일 취급, 오라클 없음
        }
        LocalDateTime now = LocalDateTime.now(clock);
        refreshTokenDao.revokeAllForMember(memberId, now);
        apiKeyDao.revokeByMemberId(memberId, now);
    }

    /**
     * refresh — 토큰 검증(자격 판정) → 회전 → apiKey 재발급. 실패 경로(토큰 미존재/만료/폐기/
     * 회원탈퇴)는 전부 동일한 {@link MemberSessionApiException}(401, MBR-4012, 동일 메시지)을
     * 던진다.
     */
    public SessionResult refresh(String refreshTokenPlain) {
        LocalDateTime now = LocalDateTime.now(clock);
        String tokenHash = sha256Hex((refreshTokenPlain == null) ? "" : refreshTokenPlain);

        String memberId = refreshTokenDao.selectActiveByTokenHash(tokenHash, now);
        Member member = (memberId == null) ? null : memberDao.selectById(memberId);
        if (member == null) {
            // 토큰 미존재/만료/폐기(위 SELECT가 이미 필터) 또는 회원탈퇴(selectById의 del_yn='N'
            // 필터) — 어느 경우든 동일한 401(계획 "순서·보안" — 오라클 방지 원칙 재적용).
            throw invalidSessionException();
        }

        String newRefreshTokenPlain = UUID.randomUUID().toString() + UUID.randomUUID();
        String newTokenHash = sha256Hex(newRefreshTokenPlain);
        LocalDateTime expiresAt = now.plusDays(REFRESH_TOKEN_VALID_DAYS);
        refreshTokenDao.insert(newTokenHash, memberId, now, expiresAt);
        refreshTokenDao.revokeByTokenHash(tokenHash, now); // 회전 — 방금 쓴 구토큰 재사용 방지
        refreshTokenDao.purgeExpiredOrRevoked(memberId, now); // 005 하우스키핑 재사용, 신규 로직 없음
        refreshTokenDao.enforceActiveCap(memberId, MAX_ACTIVE_REFRESH_TOKENS);

        String candidateApiKey = API_KEY_PREFIX + UUID.randomUUID().toString().replace("-", "");
        apiKeyDao.issueIfAbsent(memberId, candidateApiKey, now); // 조건부 rotate(폐기됐으면 교체)
        String apiKey = apiKeyDao.selectByMemberId(memberId);

        return new SessionResult(memberId, member.getMemberName(), member.getGrade(), apiKey,
                newRefreshTokenPlain, expiresAt);
    }

    private static MemberSessionApiException invalidSessionException() {
        return new MemberSessionApiException(HttpStatus.UNAUTHORIZED, CODE_INVALID_SESSION,
                MESSAGE_INVALID_SESSION);
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

    /** refresh 성공 응답 — 계약: 로그인 {@code LoginResult}와 동일 셰이프. */
    public record SessionResult(String memberId, String memberName, String grade, String apiKey,
                                 String refreshToken, LocalDateTime refreshTokenExpiresAt) { }
}
