// linked_func: FUNC-member-009
// spec: docs/00_FUNC/stories/STORY-FUNC-member-009.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberApiKeyDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.MemberRefreshTokenDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SR-234(FUNC-member-009) — "비밀번호 반영 + 전 기기 로그아웃"의 트랜잭션 경계 전담 협력자
 * (STORY "구현 계획" 절, {@link MemberSignupCompletionWriter}와 동일한 이유로 별도 스프링 빈으로
 * 분리했다).
 *
 * <p><b>왜 별도 빈인가(self-invocation 방지)</b> — {@link MemberPasswordResetConfirmationService}가
 * 성공 경로에서 이 로직을 자기 클래스 안의 {@code @Transactional} 메서드로 두고 그 메서드를
 * 직접(this.) 호출했다면, Spring 프록시 self-invocation으로 트랜잭션 어드바이스가 적용되지
 * 않는다(Spring AOP의 잘 알려진 함정 — STORY "프레임워크 실행 모델 함정" 절). 이 클래스를 별도
 * 빈으로 두고 서비스가 생성자 주입받아 빈 경계 너머로 호출하면 프록시를 정상적으로 거친다.
 *
 * <p><b>이 메서드가 실패하면</b> — 코드는 이미 소비된 상태({@code consumed_at} 세팅, 이 메서드와
 * 다른 트랜잭션이라 롤백되지 않음)라 사용자는 코드를 재요청해야 한다. 보안(세션 폐기 누락 방지)을
 * 사용자 편의(코드 재사용)보다 우선한 설계 결정이다(STORY "데이터·트랜잭션 경계" 절).
 */
@Service
public class MemberPasswordResetConfirmationWriter {

    private final MemberDao memberDao;
    private final MemberRefreshTokenDao refreshTokenDao;
    private final MemberApiKeyDao apiKeyDao;

    @Autowired
    public MemberPasswordResetConfirmationWriter(MemberDao memberDao, MemberRefreshTokenDao refreshTokenDao,
                                                  MemberApiKeyDao apiKeyDao) {
        this.memberDao = memberDao;
        this.refreshTokenDao = refreshTokenDao;
        this.apiKeyDao = apiKeyDao;
    }

    /**
     * 세 문장(STORY "순서·보안" 9단계 그대로) — ① {@link MemberDao#updatePasswordHash}(0행이면
     * 그 사이 탈퇴했다는 뜻 — {@code updatePasswordHash} javadoc이 명시한 대로 호출부는 이후
     * 두 문장을 **건너뛴다**, round2 QA CONCERNS 권고 2 재작업) ② {@link
     * MemberRefreshTokenDao#revokeAllForMember}(리프레시 토큰 전체 폐기 — 전 기기 로그아웃,
     * SR 수용 기준) ③ {@link MemberApiKeyDao#revokeByMemberId}(기존 메서드 재사용 —
     * {@code MEMBER_API_KEYS.member_id} WHERE로 이미 회원 전용이라 admin 키(별도 static map,
     * 이 테이블에 아예 없음)와는 무관하다). 순서는 006 로그아웃과 동일(리프레시 먼저, apiKey
     * 나중) — 두 번째 문장이 실패해도 트랜잭션 롤백으로 첫 문장(비밀번호 반영)까지 함께 취소돼
     * "비밀번호는 바뀌었는데 세션은 살아있는" 위험한 중간 상태를 만들지 않는다.
     */
    @Transactional
    public void applyNewPassword(String memberId, String passwordHash, LocalDateTime now) {
        int updatedRows = memberDao.updatePasswordHash(memberId, passwordHash, now);
        if (updatedRows == 0) {
            // 그 사이 탈퇴 등으로 비밀번호 반영이 실효 없었다 — 폐기 대상 세션도 없다고 보고
            // 나머지 두 문장을 건너뛴다(계획 9단계, MemberDao#updatePasswordHash javadoc 그대로).
            return;
        }
        refreshTokenDao.revokeAllForMember(memberId, now);
        apiKeyDao.revokeByMemberId(memberId, now);
    }
}
