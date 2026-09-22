// linked_func: FUNC-order-017 (grades 메서드) — SR-217
// spec: docs/00_FUNC/stories/STORY-FUNC-order-017.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.OrderDao;
import com.sm.lab.shop.domain.Member;
import com.sm.lab.shop.domain.MemberGrade;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class MemberService {
    private static final int RECENT_ORDER_LIMIT = 5;

    private final MemberDao memberDao;
    private final OrderDao orderDao;

    public MemberService(MemberDao memberDao, OrderDao orderDao) {
        this.memberDao = memberDao;
        this.orderDao = orderDao;
    }

    public List<Member> list() {
        return memberDao.selectMembers();
    }

    /**
     * 등급 코드·이름·할인율(%) 전체 목록(SR-217) — DB 조회 없이 {@link MemberGrade}(코드 정본
     * 한 곳) 상수를 그대로 반환한다. 인증 불필요(공개 정보) 엔드포인트에서 사용.
     * linked_func: FUNC-order-017
     */
    public List<MemberGrade> listGrades() {
        return List.of(MemberGrade.values());
    }

    /** 회원 상세 + 최근 주문 5건. linked_func: FUNC-order-004 (LAB-103) */
    public Member get(String memberId) {
        Member m = memberDao.selectById(memberId);
        if (m == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "회원 없음: " + memberId);
        }
        m.setRecentOrders(orderDao.selectRecentByMember(memberId, RECENT_ORDER_LIMIT));
        return m;
    }
}
