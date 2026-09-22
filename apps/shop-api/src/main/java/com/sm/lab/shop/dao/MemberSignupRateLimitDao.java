// linked_func: FUNC-member-002
// spec: docs/00_FUNC/stories/STORY-FUNC-member-002.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberSignupRateLimit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 회원가입 인증코드 레이트리밋 전용 DAO(SR-231 round4, 사람 결정 — "설계를 단순화해서
 * 데드락 원인을 없앤다"). 코드 테이블을 다루는 {@link MemberSignupVerificationDao}와
 * 완전히 분리된 테이블(MEMBER_SIGNUP_RATE_LIMITS)을 다뤄 락이 섞이지 않는다.
 */
@Mapper
public interface MemberSignupRateLimitDao {

    /**
     * 원자 판정 UPSERT(round4 STORY 재작업 지시(A)) — {@code INSERT ... ON DUPLICATE KEY
     * UPDATE} 한 문장(autocommit, 서비스 쪽에 트랜잭션 없음)으로 쿨다운({@code cooldownSeconds})과
     * 일일상한({@code dailyLimit})을 동시에 만족할 때만 {@code daily_count}/
     * {@code last_requested_at}/{@code last_token}을 갱신한다. 조건 미달이면 그 UPDATE는 IF의
     * else 분기로 기존 값을 그대로 대입하므로(last_token 포함) 실제 행 변경이 없다.
     *
     * <p>round5(사람 결정, STORY 재작업 지시(1)) — 판정 신호를 JDBC affected-rows에서
     * <b>요청 토큰</b>으로 바꿨다. round4는 반환값(신규 삽입=1/실제 값 변경=2/무변경=0)으로
     * 판정했는데, 그 의미는 datasource 전역 설정({@code useAffectedRows=true})에 의존했고
     * 그 설정이 이 FUNC 밖 {@code ProductDao.decreaseStock}(FUNC-order-002 소유)의 UPDATE
     * 반환값 시맨틱까지 바꿔 qty=0 주문 라인의 {@code POST /api/orders} 응답을 200→409로
     * 회귀시켰다(QA 실측). 이 메서드는 이제 반환값(affected rows)을 판정에 쓰지 않는다 — 매
     * 호출이 생성하는 고유 {@code token}(UUID)을 조건부로 {@code last_token}에 함께 기록하고,
     * 호출부는 뒤이은 {@link #selectRateLimit}로 그 값을 다시 읽어 "내 토큰이 저장돼 있으면
     * 허용"으로 판정한다({@link com.sm.lab.shop.service.MemberSignupService} 참고). datasource
     * 커넥션 속성에 더 이상 의존하지 않는다.
     *
     * <p>round6(사람 결정, round5 QA CONCERNS 재작업 지시 1) — 매퍼 SQL의 판정 로직에서
     * MySQL 세션 변수(대입/읽기 순서가 매뉴얼상 정의되지 않은 패턴)를 걷어내고, UPDATE SET
     * 목록의 좌→우 평가(문서화된 동작)만으로 결정적인 형태로 재작성했다 — 메서드 시그니처와
     * 호출부 계약은 round5와 동일, 바뀐 것은 매퍼 XML(memberSignupRateLimit.xml)의 SQL
     * 본문뿐이다.
     */
    void touchRateLimit(@Param("target") String target, @Param("dayKey") LocalDate dayKey,
                         @Param("now") LocalDateTime now, @Param("cooldownSeconds") int cooldownSeconds,
                         @Param("dailyLimit") int dailyLimit, @Param("token") String token);

    /** 거부 사유(쿨다운 vs 일일상한) 판별 + 테스트용 조회. 없으면 null. */
    MemberSignupRateLimit selectRateLimit(@Param("target") String target, @Param("dayKey") LocalDate dayKey);

    /** 정리용 삭제(테스트 데이터 원복) — 없어도 0 반환. */
    int deleteRateLimit(@Param("target") String target, @Param("dayKey") LocalDate dayKey);

    /**
     * round4(STORY 재작업 지시(B)) — 요청 경로에서 완전히 제거되고
     * {@code MemberSignupMaintenanceScheduler}(@Scheduled fixedDelay=10분)로만 호출된다.
     * 오늘보다 이전 날짜의 행만 삭제한다(오늘 카운터는 절대 건드리지 않음).
     */
    int purgeOldRows(@Param("beforeDay") LocalDate beforeDay);
}
