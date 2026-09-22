// linked_func: FUNC-member-005, FUNC-member-006
// SR-232 로그인(005, 발급+하우스키핑) + 로그아웃/리프레시(006 — 조회·전체폐기·회전폐기 신규)
// spec: docs/00_FUNC/stories/STORY-FUNC-member-005.md, docs/00_FUNC/stories/STORY-FUNC-member-006.md
package com.sm.lab.shop.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * 리프레시 토큰 전용 DAO(SR-232, {@code MEMBER_REFRESH_TOKENS}) — 005(로그인)은 발급
 * ({@link #insert})과 자기 자신이 만드는 행의 하우스키핑({@link #purgeExpiredOrRevoked},
 * {@link #enforceActiveCap})만 했다. 006(로그아웃/리프레시)이 토큰 원문 기준 조회·전체 폐기·
 * 회전 폐기({@link #selectActiveByTokenHash}, {@link #revokeAllForMember}, {@link #revokeByTokenHash})를
 * add-only로 추가한다 — 기존 3개 메서드는 무변경(계획 "005 인터페이스 경계표" 근거).
 *
 * <p><b>006과의 인터페이스 가정</b>(계획 "계약" 절) — {@code tokenHash}는 클라이언트에 원문
 * 그대로 전달된 리프레시 토큰의 SHA-256 해시(소문자 hex)다. 원문 자체는 어디에도 저장하지
 * 않는다. FUNC-006은 클라이언트가 보낸 원문을 동일한 해시 방식으로 변환해 이 테이블을
 * 조회해야 한다.
 *
 * <p>round 9(재작업 지시 4, QA FAIL medium 2 — 무한 누적) — 로그인마다 신규 행이 하나씩
 * 쌓이기만 하고 지우는 주체가 없어 회원 1명 앞으로 60행까지 쌓인 실측(DB 확인)이 나왔다.
 * {@link com.sm.lab.shop.service.MemberLoginService}가 매 성공 로그인마다
 * {@link #purgeExpiredOrRevoked}로 만료·폐기 행을 지우고 {@link #enforceActiveCap}으로 회원당
 * 활성 토큰을 상한(5) 이내로 유지한다({@code MemberSignupRateLimitDao#purgeOldRows}와 같은
 * 하우스키핑 사고방식 — 다만 이건 스케줄러가 아니라 로그인 경로 자체에서 매번 실행된다).
 */
@Mapper
public interface MemberRefreshTokenDao {

    /** {@code revoked_at}은 항상 NULL로 INSERT한다(폐기는 FUNC-006 소관). */
    void insert(@Param("tokenHash") String tokenHash, @Param("memberId") String memberId,
                @Param("issuedAt") LocalDateTime issuedAt, @Param("expiresAt") LocalDateTime expiresAt);

    /**
     * round 9 — 이 회원의 만료됐거나({@code expires_at <= now}) 폐기된({@code revoked_at NOT NULL})
     * 행을 지운다. 발급 직후 호출되므로 방금 넣은 행은 만료·폐기 둘 다 아니라 절대 지워지지 않는다.
     */
    int purgeExpiredOrRevoked(@Param("memberId") String memberId, @Param("now") LocalDateTime now);

    /**
     * round 9 — {@link #purgeExpiredOrRevoked} 이후에도 이 회원의 남은 행이 {@code limit}보다
     * 많으면 {@code issued_at} 오래된 순으로 초과분을 지운다(가장 최근 {@code limit}개만 남긴다).
     * 정확한 상한 강제라기보다 하우스키핑이다 — 동시 로그인 레이스에서는 일시적으로 상한을
     * 넘나들 수 있지만 다음 로그인 때 다시 정리된다(회원 자격증명 보안 속성이 아니라 저장공간
     * 위생 목적).
     */
    int enforceActiveCap(@Param("memberId") String memberId, @Param("limit") int limit);

    /**
     * linked_func: FUNC-member-006 — refresh 자격 판정의 유일한 검문소(계획 "폴백·우회 경로의
     * 자격 판정" 절). {@code revoked_at IS NULL AND expires_at > now} 조건은 반드시 이 SQL의
     * WHERE 절에 있어야 한다(애플리케이션 레이어에서 재확인하지 않음, 005의 {@code del_yn} 필터
     * 관례와 동일). 토큰 미존재·만료·폐기 3경우 모두 결과 없음(null)으로 반환돼 호출부가 동일한
     * 401로 처리한다. 회원 탈퇴 여부는 이 조회의 책임이 아니다(반환된 {@code memberId}로 호출부가
     * {@code MemberDao#selectById}를 별도로 확인한다).
     */
    String selectActiveByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);

    /**
     * linked_func: FUNC-member-006 — 로그아웃 = 이 회원의 활성 리프레시 토큰 전체 폐기(개별
     * 아님, GATE-005 round2 경계표 근거 — apiKey가 회원당 1개뿐인 기존 설계와 일관). 이미
     * 폐기된 행은 이 UPDATE의 대상이 아니므로(WHERE revoked_at IS NULL) 재실행해도 안전하다.
     */
    int revokeAllForMember(@Param("memberId") String memberId, @Param("now") LocalDateTime now);

    /**
     * linked_func: FUNC-member-006 — refresh 성공 시 회전(rotation)으로 방금 사용된 구 토큰을
     * 개별 폐기한다(재사용 방지). {@link #revokeAllForMember}와 달리 이 회원의 다른 활성 토큰에는
     * 영향이 없다 — 로그인/리프레시는 회원당 apiKey 1개 모델과 달리 여러 리프레시 토큰 행이 공존할
     * 수 있다(005의 {@link #enforceActiveCap} 상한 정책 그대로).
     */
    int revokeByTokenHash(@Param("tokenHash") String tokenHash, @Param("now") LocalDateTime now);
}
