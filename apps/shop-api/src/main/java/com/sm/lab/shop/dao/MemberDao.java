package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.MemberCredential;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MemberDao {
    List<Member> selectMembers();
    Member selectById(@Param("memberId") String memberId);

    /**
     * linked_func: FUNC-member-003 — SR-231 가입 요청 API(INF-MBR-002) 회원 생성.
     * spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
     * 이메일/휴대폰 중복은 이 INSERT가 {@code uq_members_email}/{@code uq_members_phone_norm}
     * UNIQUE 인덱스 위반(DuplicateKeyException)으로 원자적으로 검출한다 — 호출측이 미리
     * select로 중복을 확인하지 않는다(선조회 금지, STORY "승인된 설계 확정" — 동시 요청
     * 레이스 방지). email/phone 중 채널에 해당하지 않는 쪽은 null로 저장한다.
     *
     * <p>round2(SR-231 round1 QA FAIL 필수2 재작업, 사람 결정) — {@code phoneNorm}(숫자만
     * 정규화된 휴대폰번호)을 별도 컬럼에 함께 저장한다. 기존 시드 회원의 {@code phone}이
     * 하이픈 포함('010-1111-2222')이라 원문 {@code phone} 컬럼의 UNIQUE 인덱스로는 숫자만
     * 저장되는 신규 가입과의 중복을 잡지 못했다(V3__members_signup.sql round2 참고) —
     * {@code uq_members_phone_norm}이 유일한 판정 인덱스다.
     *
     * <p>기존 {@link #selectMembers()}/{@link #selectById(String)}(회원 조회, FUNC-order-004/017
     * 등 다른 FUNC 소유)는 이 메서드 추가로 전혀 바뀌지 않는다(회귀 없음).
     */
    int insertMember(@Param("memberId") String memberId, @Param("memberName") String memberName,
                      @Param("grade") String grade, @Param("email") String email,
                      @Param("phone") String phone, @Param("phoneNorm") String phoneNorm,
                      @Param("passwordHash") String passwordHash,
                      @Param("marketingOptIn") boolean marketingOptIn, @Param("createdAt") LocalDateTime createdAt);

    /**
     * linked_func: FUNC-member-003 — 테스트 데이터 정리 전용(가입 요청 API 테스트가 생성한
     * 행 원복, MemberSignupVerificationDao#deleteByChannelAndTarget과 동일한 house 관례).
     * 운영 경로에서는 호출하지 않는다(회원 삭제 API 자체는 이 SR 범위 밖).
     */
    int deleteById(@Param("memberId") String memberId);

    /**
     * linked_func: FUNC-member-003 — round3(SR-231 round2 QA FAIL 재작업 지시, 사람 결정 — 3단계
     * 흐름 지정) STEP 0 "사전 판정"(비트랜잭션 읽기, 안내용). {@code email} 또는 {@code phoneNorm}
     * (둘 중 요청 채널에 해당하지 않는 쪽은 항상 null로 전달됨 — SQL {@code = } 비교는 null과
     * 절대 매치되지 않으므로 안전)로 이미 존재하는 회원을 찾는다. 이 조회는 빠른 실패(안내)용일
     * 뿐이다 — 최종 중복 보장은 여전히 {@link #insertMember}의 UNIQUE 인덱스 위반
     * (DuplicateKeyException) 캐치다(동시 요청 레이스는 이 SELECT만으로 막을 수 없다). 탈퇴
     * 회원({@code del_yn='Y'})도 걸러내지 않는다 — 최종 UNIQUE 인덱스 역시 del_yn을 구분하지
     * 않으므로 사전 판정과 최종 판정의 결과가 어긋나지 않게 일부러 필터를 두지 않았다(탈퇴 회원의
     * email/phone_norm이 재가입을 막는 문제 자체는 round1 권고4/round2 권고5로 이월된 별도
     * 백로그 — 이 FUNC 범위 밖).
     */
    String selectMemberIdByEmailOrPhoneNorm(@Param("email") String email, @Param("phoneNorm") String phoneNorm);

    /**
     * linked_func: FUNC-member-003 — round2(SR-231 round1 QA FAIL 필수3 재작업, 사람 결정)
     * 회원 ID 원자 채번의 첫 단계. {@code ID_SEQUENCES}(V3__members_signup.sql round2) 행을
     * {@code INSERT ... ON DUPLICATE KEY UPDATE next_val = LAST_INSERT_ID(next_val + 1)}로
     * 원자 증가시키고, MySQL/MariaDB의 {@code LAST_INSERT_ID(expr)} 관용구로 이 커넥션의 세션
     * 값을 그 증가된 값으로 명시 설정한다. 반드시 같은 트랜잭션(=같은 JDBC 커넥션) 안에서
     * {@link #selectLastMemberIdSeq()}를 곧바로 뒤이어 호출해야 한다(다른 커넥션에서 읽으면
     * 값이 섞인다 — {@code LAST_INSERT_ID()}는 세션 스코프). 이전 in-memory
     * {@code AtomicInteger}(JVM 기동마다 리셋되어 PK 충돌을 냈던 방식, round1 QA FAIL 필수3)를
     * 완전히 대체한다.
     */
    int touchMemberIdSeq();

    /**
     * linked_func: FUNC-member-003 — 위 {@link #touchMemberIdSeq()} 직후, 같은 커넥션에서
     * 읽는 세션 {@code LAST_INSERT_ID()}. 회원 ID 포맷은 {@code "M-" + String.format("%04d", seq)}.
     */
    long selectLastMemberIdSeq();

    /**
     * linked_func: FUNC-member-005 — 로그인 API(SR-232, INF-MBR-003) 인증 전용 조회. 공개 응답에
     * 쓰이는 {@link Member}에는 {@code passwordHash}를 절대 추가하지 않고, 이 인증 전용 DTO
     * ({@link MemberCredential})로만 노출한다(직렬화 유출 위험 회피).
     *
     * <p>{@code del_yn} 필터를 WHERE에 걸지 않는다 — 탈퇴 회원도 조회해 서비스가 "회원 없음"과
     * 완전히 동일한 401 응답으로 처리하기 위함이다(STORY "순서·보안" 2 — 존재 판정 오라클 방지,
     * 세 경우(회원 없음/delYn='Y'/비밀번호 불일치)를 동일 코드·문구·카운터 의미로 통일).
     *
     * <p>기존 {@link #selectMembers()}/{@link #selectById(String)} 등 6개 메서드는 이 메서드
     * 추가로 전혀 바뀌지 않는다(회귀 없음, add-only).
     */
    MemberCredential selectAuthByEmail(@Param("email") String email);

    /**
     * linked_func: FUNC-member-009 — 비밀번호 재설정 확정(SR-234, INF-MBR-007) 전용 조회.
     * {@link #selectMemberIdByEmailOrPhoneNorm}(FUNC-member-003 소유, 가입 시 중복 판정 전용)을
     * 재사용하지 않는다 — 그 메서드는 {@code email = #{email}}(대소문자 구분, DB collation 의존)
     * 정확 일치로 충분했지만, 이 조회는 FUNC-008이 이미 정규화(소문자)해 저장한 target으로
     * {@code MEMBERS.email}(가입 시 원문 그대로 저장, 소문자화 안 됨 — {@link
     * com.sm.lab.shop.service.MemberRegistrationService} 실측)을 찾아야 해 {@code LOWER(email)}로
     * 명시 비교한다(DB collation 대소문자 구분 여부에 매칭을 맡기지 않는다). {@code phoneNorm}은
     * 원래도 숫자만 저장되므로 정확 일치 그대로. {@code email}/{@code phoneNorm} 중 요청 채널에
     * 해당하지 않는 쪽은 항상 null로 넘어와 그 조건은 매치되지 않는다(SQL {@code =} 비교는 null과
     * 절대 매치되지 않음, 기존 관례).
     *
     * <p>{@code del_yn = 'N'}을 상시 건다(사례집 SR-232 r2 — 회원 존재/자격 관련 조회는 항상
     * {@code MEMBERS} + {@code del_yn} 필터를 통과해야 한다 — 이 조회가 결과적으로 비밀번호
     * 변경·세션 폐기라는 자격 변경 행위로 이어지므로).
     */
    String selectMemberIdByResetTarget(@Param("email") String email, @Param("phoneNorm") String phoneNorm);

    /**
     * linked_func: FUNC-member-009 — 비밀번호 재설정 확정 시 비밀번호 반영 +
     * {@code updated_at}(V6 마이그레이션 신규 컬럼) 갱신. {@code WHERE ... AND del_yn = 'N'}을
     * 방어적으로 건다(위 조회와 이 UPDATE 사이에 이론상 벌어질 수 있는 회원 탈퇴 레이스에도 안전
     * — 0행이면 그 사이 탈퇴했다는 뜻이고 호출부는 이후 단계를 스킵한다).
     */
    int updatePasswordHash(@Param("memberId") String memberId, @Param("passwordHash") String passwordHash,
                            @Param("updatedAt") LocalDateTime updatedAt);
}
