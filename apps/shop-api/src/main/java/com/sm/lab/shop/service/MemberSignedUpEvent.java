// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.service;

/**
 * 회원가입 완료 이벤트(SR-231, INF-MBR-002) — Spring {@code ApplicationEventPublisher}가 배포하는
 * 애플리케이션 이벤트. STORY "승인된 설계 확정"에 따라 이 FUNC은 이벤트 발행까지만 책임진다 —
 * 환영 쿠폰 발급 로직 자체는 별도 프로모션 SR(백로그)의 몫이며, 이 이벤트를 구독하는 리스너는
 * {@link MemberSignedUpEventLogger}(로그 1개) 하나뿐이다.
 */
public class MemberSignedUpEvent {
    private final String memberId;
    private final String target;

    public MemberSignedUpEvent(String memberId, String target) {
        this.memberId = memberId;
        this.target = target;
    }

    public String getMemberId() {
        return memberId;
    }

    public String getTarget() {
        return target;
    }
}
