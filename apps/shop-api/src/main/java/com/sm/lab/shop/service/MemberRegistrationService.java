// linked_func: FUNC-member-003, FUNC-member-009, FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md, docs/00_FUNC/stories/STORY-FUNC-member-009.md,
// docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberSignupCompletionDao;
import com.sm.lab.shop.domain.MemberGrade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 가입 요청 — 인증 완료 강제 + 회원 생성(SR-231, INF-MBR-002). {@code POST /api/members/signup}의
 * 비즈니스 로직. 인증코드 발송(INF-MBR-001, FUNC-member-002)은 이 FUNC 범위 밖 — 이미 저장된
 * {@code MEMBER_SIGNUP_VERIFICATIONS} 행을 대조 대상으로만 재사용한다(그 테이블을 소유한 클래스는
 * 수정하지 않는다 — {@link MemberSignupCompletionDao} 참고).
 *
 * <p><b>round3(2026-09-12, SR-231 round2 QA FAIL 재작업 지시 — 마지막 라운드, 사람 결정이 3단계
 * 흐름·정확한 SQL·트랜잭션 경계를 직접 지정)</b> — round2 QA가 지적한 근본 원인은 "비트랜잭션이어야
 * 할 코드(대입 시도 카운터 증가)가 signUp() 전체를 감싼 {@code @Transactional} 안에서 호출돼,
 * 검증 실패로 던진 예외가 그 증가분까지 함께 롤백시킨 것"이었다 — 즉 시도 제한이 죽은 코드였다.
 * round3는 흐름을 트랜잭션 경계가 서로 다른 3단계로 완전히 분리한다:
 * <ol>
 *   <li><b>STEP 0 — 사전 판정</b>({@link #precheckDuplicate}, 비트랜잭션 읽기). {@code email}
 *       또는 {@code phone_norm}이 이미 존재하면 코드 검증조차 시도하지 않고 즉시 409
 *       {@code MBR-4092}(+{@code login_url}, 이메일) / {@code MBR-4094}(휴대폰)로 거부한다.
 *       이 조회는 안내(빠른 실패)용일 뿐이다 — 최종 중복 보장은 여전히 {@link #completeSignup}
 *       내부 INSERT의 UNIQUE 인덱스 위반(DuplicateKeyException) 캐치다(동시 요청 레이스는 이
 *       SELECT만으로 막지 못하므로). 이 단계가 round2 QA FAIL 필수2("재발송이 consumed_at/
 *       attempt_count를 리셋하지 않아 재가입 시도가 MBR-4091을 받는다")를 근본적으로 해소한다 —
 *       판정을 verification 테이블의 신선도가 아니라 MEMBERS 테이블의 실재 여부로 옮겼기
 *       때문에, FUNC-member-002 소유 {@code writeCode}(재발송)를 전혀 건드리지 않고도 정확한
 *       중복 코드를 낼 수 있다.</li>
 *   <li><b>STEP 1 — 코드 검증</b>({@link #verifyCode}, 비트랜잭션 — 이 메서드에는 어떤
 *       {@code @Transactional}도 없다. {@link MemberSignupCompletionDao}의 각 UPDATE 호출은
 *       활성 트랜잭션이 없으므로 autocommit으로 개별 커밋되고, 그 결과는 이후 어떤 예외로도
 *       롤백되지 않는다). 조건부 UPDATE(verified_at 세팅, {@code attempt_count < 5} 포함)로
 *       검증을 시도하고, 0행이면 {@link MemberSignupCompletionDao#incrementAttemptCount}를
 *       호출한다 — 이 UPDATE 자체가 {@code attempt_count < 5}를 조건으로 갖는 원자 증가라서
 *       (SR-298 — 이전에는 현재 값을 먼저 읽어 상한 도달 여부로 분기한 뒤에만 증가시켰는데,
 *       그 두 문장 사이 간극에서 동시 버스트가 상한을 넘겨 증가시킬 수 있었다), 영향행수 1은
 *       정상 증가를 뜻해 곧바로 409 {@code MBR-4091}을, 0이면
 *       {@link MemberSignupCompletionDao#selectAttemptCount}로 현재 값을 읽어 원인만
 *       구분한다 — 이미 상한(5) 도달이면 409 {@code MBR-4093}("재발송 필요"), 그 밖의 0행
 *       사유(이미 소비·이미 인증·매치 행 없음)는 기존과 동일하게 409 {@code MBR-4091}이다
 *       (이 읽기로 다시 증가시키지 않는다 — 증가는 이미 원자 UPDATE에서 끝났다).</li>
 *   <li><b>STEP 2 — 가입</b>({@link MemberSignupCompletionWriter#completeSignup}, 트랜잭션 —
 *       ID 채번 → MEMBERS INSERT → verification {@code consumed_at} UPDATE 이 세 문장만).
 *       클래스 단위 {@code @Transactional}을 금지하고 메서드마다 명시하라는 사람 결정에 따라,
 *       이 트랜잭션은 이 클래스가 아니라 별도 스프링 빈({@link MemberSignupCompletionWriter})에
 *       둔다 — 같은 클래스 안에서 이 메서드를 호출했다면 self-invocation으로 트랜잭션 어드바이스가
 *       적용되지 않아 round2와 동일한 함정이 재발했을 것이다(그 클래스 javadoc 참고). 미분류
 *       (PK) 충돌은 완전히 새로운 트랜잭션으로 1회만 재시도한다(round2와 동일 정책, 다만 이번엔
 *       재시도가 별도 트랜잭션이다 — 여전히 각 시도는 "세 문장만"을 지킨다).</li>
 * </ol>
 *
 * <p><b>테스트 맹점 재발 방지(round2 QA FAIL 3번)</b> — {@code MemberRegistrationServiceTest}
 * (Mockito)는 STEP1/STEP2의 각 분기 로직(어떤 DAO 메서드를 호출하는가)만 격리 검증한다. 실제
 * 트랜잭션 경계가 지켜지는지(비트랜잭션 통계가 정말 커밋되는지, 트랜잭션 실패가 정말 롤백되는지)는
 * Mockito로 증명할 수 없다 — 그래서 {@code MemberRegistrationCompletionFlowTest}가 실 서버+실
 * DB로 HTTP 레벨 회귀(오답 5회 → attempt_count=5 실측, 6회째 정답도 거부 등)를 별도로 맡는다.
 *
 * <p><b>round4(2026-09-12, SR-231 round3 QA CONCERNS 권고1 "존재 오라클" — 사람 결정, 순서만
 * 뒤집는다)</b> — round3는 STEP 0(사전 판정)을 STEP 1(코드 검증)보다 먼저 실행했다. 그 결과
 * 인증코드를 한 번도 요청한 적 없는 요청자도 임의 target을 넣기만 하면 409 {@code MBR-4092}/
 * {@code MBR-4094}(가입됨) vs {@code MBR-4091}(미가입 또는 코드 필요)의 응답 차이로 회원 존재
 * 여부를 열거할 수 있었다(무인증 화이트리스트 엔드포인트라 레이트리밋도 없어 휴대폰 번호 공간
 * 전수 열거가 현실적). 사람 결정은 이 문제를 STEP 0/STEP 1의 **순서를 뒤집는 것**으로 해소한다
 * (각 단계 내부 구현·SQL·트랜잭션 경계는 손대지 않는다 — "STEP 1 코드 검증을 먼저(비트랜잭션,
 * 그대로 유지)"):
 * <ul>
 *   <li>{@link #verifyCode}(STEP 1)를 먼저 호출한다. 실패하면(오답/미인증/시도상한 도달 등
 *       어떤 사유든) {@link #precheckDuplicate}(STEP 0)는 **전혀 호출되지 않는다** — 즉
 *       코드 검증에 실패한 요청은 이 target이 이미 가입돼 있든 아니든 정확히 같은 예외(같은
 *       코드·같은 문구, 그리고 존재 여부에 따른 추가 DB 조회가 없으므로 같은 응답 시간대)로
 *       끝난다. 코드를 못 받은 사람은 이 target의 가입 여부를 알아낼 방법이 없다.</li>
 *   <li>{@link #verifyCode}가 성공한 뒤에만 {@link #precheckDuplicate}를 실행한다 — 이미
 *       코드를 검증한 사람(=target 소유자로 간주 가능)에게만 "이미 가입됨" 안내(+
 *       {@code login_url})를 보여준다는 뜻이다.</li>
 * </ul>
 * <p>이 재정렬은 round2 QA FAIL 2번("재발송이 consumed_at/attempt_count를 리셋하지 않아 재가입
 * 시도가 MBR-4092 대신 MBR-4091을 받는다")을 다시 불러온다 — round3가 그 결함을 STEP 0을
 * 먼저 실행해 해소했었기 때문이다. 사람 결정은 이 회귀를 **의도적으로 받아들인다**(보안
 * 결함이 스펙 결함보다 우선) — 근본 해결(재발송 시 리셋)은 FUNC-member-002 소유 파일을
 * 건드려야 해서 이 FUNC 범위 밖이고, 후속 SR-295로 접수돼 있다(아래 "후속 추적" 참고).
 */
@Service
public class MemberRegistrationService {
    static final int MAX_VERIFY_ATTEMPTS = 5; // round2 QA FAIL 필수1 — 코드 대입 시도 상한
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 64;
    private static final int NAME_MAX_LENGTH = 50; // MEMBERS.member_name VARCHAR(50) 정합

    private static final String CHANNEL_EMAIL = "EMAIL";
    private static final String CHANNEL_SMS = "SMS";

    private static final String CODE_VERIFY_REQUIRED = "MBR-4091";
    private static final String CODE_EMAIL_DUPLICATE = "MBR-4092";
    private static final String CODE_PASSWORD_INVALID = "MBR-4001";
    private static final String CODE_PHONE_DUPLICATE = "MBR-4094"; // round2 QA FAIL 필수2 — 사람 결정
    // round3(사람 결정) — 시도 상한(5회) 도달 상태에서 들어온 요청 전용 코드. MBR-4091(단순
    // 오답/미인증)과 구분해, 그 target은 재발송 없이는 더 이상 진행할 수 없음을 알린다.
    private static final String CODE_VERIFY_LOCKED = "MBR-4093";
    // DuplicateKeyException 메시지에 실리는 제약 이름 — 원문 문자열 매칭으로 분기한다(사람 결정).
    private static final String CONSTRAINT_EMAIL = "uq_members_email";
    private static final String CONSTRAINT_PHONE_NORM = "uq_members_phone_norm";
    // STORY AC "가입 완료 시 login_url 필드" — 이 랩에는 별도 로그인 화면 라우트가 없어(RECON
    // 실측, grep 0건) 향후 로그인 화면 경로로 쓰일 자리표시 상대경로를 둔다(가정 — Dev 기록 참고,
    // round2에 이어 round3도 미해결로 이월 — SR/UIS 등록은 이 FUNC 범위 밖).
    private static final String LOGIN_URL = "/login";

    // MemberSignupService(FUNC-member-002)와 동일한 채널 판정 정규식. 그 클래스는 이 값을
    // private으로만 노출해 재사용할 수 없고, 그 파일을 이 FUNC이 수정할 수도 없어(파일 경계)
    // 여기 그대로 복제한다 — 동일 규칙이므로 채널 판정이 어긋나지 않는다(두 FUNC이 같은 target
    // 형식 정의를 공유해야 같은 MEMBER_SIGNUP_VERIFICATIONS 행을 찾을 수 있다).
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    // package-private(재작업 지시 4, FUNC-member-011) — MemberAddressService가 이 값을 그대로
    // 재사용한다(문자열 복제 제거). 둘 다 com.sm.lab.shop.service 패키지라 가시성만 열면 된다.
    static final Pattern PHONE_PATTERN = Pattern.compile("^01[016789][0-9]{7,8}$");
    // 8~64자·영문+숫자 포함(STORY 확정 규칙)
    private static final Pattern PASSWORD_RULE_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{" + PASSWORD_MIN_LENGTH + "," + PASSWORD_MAX_LENGTH + "}$");

    private final MemberDao memberDao;
    private final MemberSignupCompletionDao completionDao;
    private final MemberSignupCompletionWriter completionWriter;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Autowired
    public MemberRegistrationService(MemberDao memberDao, MemberSignupCompletionDao completionDao,
                                      MemberSignupCompletionWriter completionWriter,
                                      ApplicationEventPublisher eventPublisher) {
        this(memberDao, completionDao, completionWriter, eventPublisher, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — MemberSignupService의 동일 시임 패턴. */
    MemberRegistrationService(MemberDao memberDao, MemberSignupCompletionDao completionDao,
                               MemberSignupCompletionWriter completionWriter,
                               ApplicationEventPublisher eventPublisher, Clock clock) {
        this.memberDao = memberDao;
        this.completionDao = completionDao;
        this.completionWriter = completionWriter;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * 가입 요청 처리. 이 메서드 자체는 {@code @Transactional}이 아니다(사람 결정 — 클래스
     * javadoc의 3단계 절 참고) — STEP 0/1은 비트랜잭션, STEP 2만 {@link MemberSignupCompletionWriter}
     * 안에서 트랜잭션이다.
     * <p>400 {@code MBR-4001} — 이름이 비었거나 50자를 넘음, 또는 비밀번호가 8~64자·영문+숫자
     * 포함 규칙을 어김.
     * <p>409 {@code MBR-4092} — 이메일 중복(+ {@code login_url}). 409 {@code MBR-4094} — 휴대폰
     * 중복. 이 둘은 STEP 0(사전 판정, 이제 STEP 1 코드 검증에 **성공한 뒤에만** 실행됨) 또는
     * STEP 2(최종 UNIQUE 위반 캐치) 어느 쪽에서도 던져질 수 있다.
     * <p>409 {@code MBR-4091} — 코드가 틀렸거나 만료됐거나 이미 소비/인증됐음(시도 상한 미만).
     * STEP 0보다 먼저 평가되므로(round4) target의 가입 여부와 무관하게 항상 이 코드다.
     * <p>409 {@code MBR-4093} — 코드 대입 시도 상한(5회) 도달 — 재발송이 필요함. 이 역시 STEP 0
     * 이전에 평가된다.
     */
    public SignupResult signUp(String target, String code, String password, String name, boolean marketingOptIn) {
        String trimmedTarget = requireTarget(target);
        String channel = resolveChannel(trimmedTarget);
        requireValidName(name);
        requireValidPassword(password);

        String email = CHANNEL_EMAIL.equals(channel) ? trimmedTarget : null;
        String phone = CHANNEL_SMS.equals(channel) ? trimmedTarget : null;
        String phoneNorm = normalizePhone(phone);

        // STEP 1(사람 결정, round4 — 순서 최우선) — 코드 검증을 먼저 수행한다. 비트랜잭션
        // (어떤 트랜잭션에도 속하지 않는 autocommit 단일 문장씩, 구현은 round3와 완전히 동일).
        // 실패하면(오답/미인증/시도상한 어느 사유든) 여기서 예외로 끝난다 — STEP 0(존재 판정)은
        // 호출조차 되지 않으므로, 코드를 검증하지 못한 요청자는 이 target의 가입 여부를 전혀
        // 알아낼 수 없다(round3 QA CONCERNS 권고1 "존재 오라클" 해소 — 클래스 javadoc round4 참고).
        verifyCode(channel, trimmedTarget, code);

        // STEP 0(사람 결정, round4 — 순서만 뒤로 이동, 내부 구현은 round3와 동일) — 코드 검증을
        // 통과한 요청에 한해서만 사전 판정. 안내용 — 최종 보장은 여전히 STEP 2의 UNIQUE catch.
        precheckDuplicate(channel, email, phoneNorm);

        // 코드 검증(락 보유 가능성이 있는 조건부 UPDATE)이 끝난 뒤에만 BCrypt(수십~백 ms)를
        // 계산한다 — 무인증 엔드포인트에서 시도 상한(5회)에 걸리는 요청까지 매번 BCrypt를 돌릴
        // 필요는 없다(round2 필수6이 우려한 "락 보유 구간에서 BCrypt"는 STEP1 자체가 더 이상
        // 트랜잭션이 아니게 되어 구조적으로 해소됐다 — 이 순서는 순수 성능상의 선택이다).
        String passwordHash = passwordEncoder.encode(password);
        LocalDateTime now = LocalDateTime.now(clock);

        // STEP 2(사람 결정) — 가입, 짧은 트랜잭션(별도 빈 MemberSignupCompletionWriter). 미분류
        // (PK) 충돌은 완전히 새 트랜잭션으로 1회만 재시도한다(round2와 동일 정책).
        String memberId;
        try {
            memberId = completionWriter.completeSignup(channel, trimmedTarget, name, MemberGrade.BRONZE.getCode(),
                    email, phone, phoneNorm, passwordHash, marketingOptIn, now);
        } catch (DuplicateKeyException e) {
            String constraint = classifyDuplicate(e);
            if (CONSTRAINT_EMAIL.equals(constraint)) {
                throw new MemberRegistrationApiException(HttpStatus.CONFLICT, CODE_EMAIL_DUPLICATE,
                        "이미 가입된 이메일입니다").withLoginUrl(LOGIN_URL);
            }
            if (CONSTRAINT_PHONE_NORM.equals(constraint)) {
                throw new MemberRegistrationApiException(HttpStatus.CONFLICT, CODE_PHONE_DUPLICATE,
                        "이미 가입된 휴대폰번호입니다");
            }
            // 미분류(주로 PK) 충돌 — 1회만 재채번 재시도(완전히 새 트랜잭션). 재시도도 실패하면
            // 그대로 다시 던져(rethrow) 일반 DataAccessException 500 경로로 떨어뜨린다 — 업무
            // 409(이미 가입됨)로 오분류하지 않는다(round1 QA FAIL 필수3 재발 방지).
            memberId = completionWriter.completeSignup(channel, trimmedTarget, name, MemberGrade.BRONZE.getCode(),
                    email, phone, phoneNorm, passwordHash, marketingOptIn, now);
        }

        eventPublisher.publishEvent(new MemberSignedUpEvent(memberId, trimmedTarget));
        return new SignupResult(memberId, channel, trimmedTarget);
    }

    /**
     * STEP 0 — 사전 판정(사람 결정, 비트랜잭션 읽기). {@code email}/{@code phoneNorm} 중 하나로
     * 이미 존재하는 회원을 찾으면 즉시 거부한다 — verification 테이블의 {@code consumed_at}/
     * {@code attempt_count} 신선도와 무관하게 정확한 코드를 낸다(round2 QA FAIL 2번 해소 —
     * 재발송이 그 두 컬럼을 리셋하지 않아도 이 판정은 영향받지 않는다). channel로 이미 어느
     * 쪽(email/phoneNorm)이 채워졌는지 알고 있으므로, 조회 결과와 무관하게 요청 채널에 맞는
     * 코드를 선택한다(이메일 요청이면 4092, 휴대폰 요청이면 4094 — STEP 2 최종 catch와 동일
     * 코드 체계).
     *
     * <p><b>round4(사람 결정)</b> — 이 메서드는 이제 {@link #verifyCode}(STEP 1)가 **성공한
     * 뒤에만** 호출된다(호출부 {@link #signUp} 참고). "코드가 맞고 틀리고와 무관하게"이 아니라
     * "코드를 이미 검증한 요청에 한해서만" 판정한다 — 그래야 코드를 검증하지 못한 요청자가 이
     * 메서드의 존재/부재 신호(4092/4094 vs 4091)로 target의 가입 여부를 알아낼 수 없다(round3
     * QA CONCERNS 권고1 "존재 오라클" 해소, 클래스 javadoc round4 절 참고).
     */
    private void precheckDuplicate(String channel, String email, String phoneNorm) {
        String existingMemberId = memberDao.selectMemberIdByEmailOrPhoneNorm(email, phoneNorm);
        if (existingMemberId == null) {
            return;
        }
        if (CHANNEL_EMAIL.equals(channel)) {
            throw new MemberRegistrationApiException(HttpStatus.CONFLICT, CODE_EMAIL_DUPLICATE,
                    "이미 가입된 이메일입니다").withLoginUrl(LOGIN_URL);
        }
        throw new MemberRegistrationApiException(HttpStatus.CONFLICT, CODE_PHONE_DUPLICATE,
                "이미 가입된 휴대폰번호입니다");
    }

    /**
     * STEP 1 — 코드 검증(사람 결정, 비트랜잭션 — 이 메서드는 {@code @Transactional}이 아니고,
     * 아래 DAO 호출은 각각 자기 커넥션에서 autocommit으로 실행돼 어떤 예외로도 롤백되지 않는다).
     * 조건부 UPDATE({@code attempt_count < 5} 포함)로 검증을 시도하고, 0행이면
     * {@link MemberSignupCompletionDao#incrementAttemptCount}를 호출한다.
     *
     * <p><b>SR-298(사람 수정, 2026-09-16)</b> — 이 증가 UPDATE 자체가
     * {@code attempt_count < maxAttempts}를 조건으로 갖는 단일 원자 UPDATE다(이전에는 이 메서드가
     * 먼저 {@code selectAttemptCount}로 현재 값을 읽어 상한 도달 여부를 판정한 뒤에만 증가시켰는데,
     * 그 두 문장 사이 간극에서 동시에 도착한 다수 요청이 모두 "아직 상한 미만"으로 읽어 상한을
     * 넘겨 증가시킬 수 있었다 — 병렬 버스트 우회). 그 영향행수가 1이면 정상 증가된 것이므로
     * 곧바로 409 {@code MBR-4091}을 던진다. 0이면 원인이 "상한 도달" 하나만이 아니다 — 이미
     * 소비·이미 인증된 행이거나 매치되는 행 자체가 없는 경우도 0행이다 — 그래서 0일 때만
     * {@link MemberSignupCompletionDao#selectAttemptCount}(기존 메서드 재사용)로 현재 값을 다시
     * 읽어 오류 코드만 분기한다(이 읽기로 다시 증가시키지 않는다 — 증가는 이미 원자 UPDATE에서
     * 끝났으므로 "경합 판정"이 아니라 순수 "오류 코드 분기"이며 house rule의 select→분기→update
     * 금지 대상이 아니다): 상한(5) 도달이면 409 {@code MBR-4093}("재발송 필요"), 그 밖의 모든
     * 0행 사유는 기존과 동일하게 409 {@code MBR-4091}이다.
     *
     * <p><b>round4(사람 결정)</b> — 호출 순서는 그대로다 — {@link #signUp}에서
     * {@link #precheckDuplicate}(STEP 0)보다 먼저 호출되므로, 이 메서드가 던지는 예외(4091/
     * 4093)는 target의 가입 여부를 전혀 조회하지 않은 상태에서 나간다 — 존재 여부와 무관하게
     * 항상 같은 코드·같은 문구·같은 정도의 DB 조회량(=비슷한 응답 시간대)이다.
     */
    private void verifyCode(String channel, String target, String code) {
        int verified = completionDao.markVerifiedIfCodeMatches(channel, target, code, MAX_VERIFY_ATTEMPTS);
        if (verified > 0) {
            return;
        }
        int incremented = completionDao.incrementAttemptCount(channel, target, MAX_VERIFY_ATTEMPTS);
        if (incremented > 0) {
            throw new MemberRegistrationApiException(HttpStatus.CONFLICT, CODE_VERIFY_REQUIRED, "인증이 필요합니다");
        }
        Integer attemptCount = completionDao.selectAttemptCount(channel, target);
        if (attemptCount != null && attemptCount >= MAX_VERIFY_ATTEMPTS) {
            throw new MemberRegistrationApiException(HttpStatus.CONFLICT, CODE_VERIFY_LOCKED, "재발송 필요");
        }
        throw new MemberRegistrationApiException(HttpStatus.CONFLICT, CODE_VERIFY_REQUIRED, "인증이 필요합니다");
    }

    /** DuplicateKeyException 메시지에 실린 제약 이름으로 분기(사람 결정) — 못 찾으면 null(미분류/PK). */
    private static String classifyDuplicate(DuplicateKeyException e) {
        String message = e.getMessage();
        if (message == null) {
            return null;
        }
        if (message.contains(CONSTRAINT_EMAIL)) {
            return CONSTRAINT_EMAIL;
        }
        if (message.contains(CONSTRAINT_PHONE_NORM)) {
            return CONSTRAINT_PHONE_NORM;
        }
        return null;
    }

    /**
     * linked_func: FUNC-member-011 — 배송지 CRUD API(SR-235)의 {@code phone_norm} 계산이 이
     * 메서드를 재사용한다(복제 금지, STORY "사람 수정" (2) — 가시성만 {@code private}→
     * package-private로 열었고 본문·정규식은 문자 하나도 바꾸지 않았다).
     */
    static String normalizePhone(String phone) {
        return phone == null ? null : phone.replaceAll("[-\\s]", "");
    }

    private String requireTarget(String target) {
        if (target == null || target.isBlank()) {
            // 형식이 애초에 성립하지 않으면 어떤 인증 기록도 존재할 수 없다 — STORY가 별도
            // 코드를 정의하지 않아 "인증 필요"(MBR-4091)로 수렴시킨다(위 클래스 javadoc 참고).
            throw new MemberRegistrationApiException(HttpStatus.CONFLICT, CODE_VERIFY_REQUIRED, "인증이 필요합니다");
        }
        return target.trim();
    }

    private String resolveChannel(String target) {
        if (EMAIL_PATTERN.matcher(target).matches()) {
            return CHANNEL_EMAIL;
        }
        if (PHONE_PATTERN.matcher(target).matches()) {
            return CHANNEL_SMS;
        }
        throw new MemberRegistrationApiException(HttpStatus.CONFLICT, CODE_VERIFY_REQUIRED, "인증이 필요합니다");
    }

    /** round2 권고 — name 미검증으로 500이 나던 것을 400으로 옮긴다(사람 코멘트 (4)). */
    private void requireValidName(String name) {
        if (name == null || name.isBlank() || name.length() > NAME_MAX_LENGTH) {
            throw new MemberRegistrationApiException(HttpStatus.BAD_REQUEST, CODE_PASSWORD_INVALID,
                    "이름을 " + NAME_MAX_LENGTH + "자 이내로 입력하세요");
        }
    }

    /**
     * FUNC-member-009(확정 API) 재사용(package-private static, 가시성만 넓힘 — 본문·정규식
     * ({@code PASSWORD_RULE_PATTERN})·던지는 예외 타입·상수({@code CODE_PASSWORD_INVALID}=
     * "MBR-4001")는 문자 하나도 바꾸지 않는다. 인스턴스 필드를 쓰지 않아 static화가 안전하다).
     */
    static void requireValidPassword(String password) {
        if (password == null || !PASSWORD_RULE_PATTERN.matcher(password).matches()) {
            throw new MemberRegistrationApiException(HttpStatus.BAD_REQUEST, CODE_PASSWORD_INVALID,
                    "비밀번호는 8~64자, 영문과 숫자를 포함해야 합니다");
        }
    }

    /** 가입 성공 응답 — target은 요청자 본인이 보낸 값의 에코(누출 아님). */
    public record SignupResult(String memberId, String channel, String target) { }
}
