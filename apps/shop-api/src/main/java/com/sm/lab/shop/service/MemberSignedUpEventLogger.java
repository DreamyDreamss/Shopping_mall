// linked_func: FUNC-member-003
// spec: docs/00_FUNC/stories/STORY-FUNC-member-003.md
package com.sm.lab.shop.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * {@link MemberSignedUpEvent} 리스너 — 로그 1개만(STORY "승인된 설계 확정": "쿠폰 발급 로직
 * 자체는 이 FUNC 범위 아님 — 로그 리스너 1개만"). 개인정보(이메일/휴대폰번호) 원문은 로그에
 * 남기지 않는다(MemberSignupService.mask와 동일한 house 관례 — 앞 2자만 노출).
 */
@Component
public class MemberSignedUpEventLogger {
    private static final Logger log = LoggerFactory.getLogger(MemberSignedUpEventLogger.class);

    @EventListener
    public void onMemberSignedUp(MemberSignedUpEvent event) {
        log.info("회원가입 완료 — memberId={}, target={} (환영 쿠폰 발급은 별도 프로모션 SR 몫, "
                + "이 리스너는 이벤트 발행 확인용 로그만 남긴다)", event.getMemberId(), mask(event.getTarget()));
    }

    private String mask(String target) {
        if (target == null || target.length() <= 2) {
            return "**";
        }
        return target.substring(0, 2) + "*".repeat(target.length() - 2);
    }
}
