// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.MemberAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 회원 배송지 CRUD(SR-235, INF-MBR-008). {@link #selectByMemberIdForUpdate}가 이 FUNC의 모든
 * 쓰기 경로(register/delete/setDefault)가 공유하는 유일한 락 획득 지점이다(사례집 SR-231 r3
 * 재발 방지 — STORY "데이터" 절).
 */
@Mapper
public interface MemberAddressDao {

    /** 목록 — 최근 사용 순(last_used_at DESC, created_at DESC), del_yn='N'. 락 없음. */
    List<MemberAddress> selectList(@Param("memberId") String memberId);

    /**
     * 존재+소유 단일 조회 — {@code WHERE member_id=? AND address_id=?}를 한 SQL로 묶어 "없음"과
     * "남의 것"을 원천적으로 같은 결과(null)로 만든다(사례집 SR-231 r5 — 존재 오라클 방지,
     * STORY "순서·보안" 절).
     */
    MemberAddress selectOwned(@Param("memberId") String memberId, @Param("addressId") Long addressId);

    /**
     * 락 지점 — {@code SELECT address_id, is_default, last_used_at, created_at ... FOR UPDATE}.
     * register/delete/setDefault 전부 쓰기 문장 이전에 이 메서드를 먼저 호출해 같은 순서를 지킨다
     * (사례집 SR-231 r3 — 락 순서 불일치는 데드락).
     *
     * <p>재작업 지시(round 2) — {@code delete()}는 이 메서드가 반환한 목록(잠금 읽기 = 최신
     * 커밋본)을 그대로 써서 삭제 대상의 {@code is_default}와 승계 후보(대상 제외, {@code
     * last_used_at DESC, created_at DESC} 첫 행)를 그 안에서 고른다 — 락 이전 스냅샷이나 비잠금
     * 재조회({@link #selectNextDefaultCandidate})를 쓰지 않는다(MariaDB REPEATABLE READ에서
     * 트랜잭션 스냅샷은 첫 일반 SELECT 시점에 고정되므로, 락 대기 중 커밋된 변경을 비잠금
     * 재조회가 보지 못할 수 있다). {@code created_at}을 포함하는 이유는 그 정렬의 2차 키다.
     */
    List<MemberAddress> selectByMemberIdForUpdate(@Param("memberId") String memberId);

    /** 등록 — AUTO_INCREMENT 채번값을 {@code address.addressId}에 되채운다(useGeneratedKeys). */
    int insertAddress(MemberAddress address);

    int updateAddress(@Param("memberId") String memberId, @Param("addressId") Long addressId,
                       @Param("recipient") String recipient, @Param("phone") String phone,
                       @Param("phoneNorm") String phoneNorm, @Param("zipcode") String zipcode,
                       @Param("roadAddress") String roadAddress, @Param("detailAddress") String detailAddress,
                       @Param("entranceMethod") String entranceMethod, @Param("deliveryMemo") String deliveryMemo,
                       @Param("updatedAt") LocalDateTime updatedAt);

    int softDelete(@Param("memberId") String memberId, @Param("addressId") Long addressId,
                    @Param("updatedAt") LocalDateTime updatedAt);

    int clearDefaultForMember(@Param("memberId") String memberId);

    int setDefault(@Param("memberId") String memberId, @Param("addressId") Long addressId);

    /**
     * 기본 배송지 삭제 시 승계 후보 — {@code ORDER BY last_used_at DESC, created_at DESC LIMIT 1},
     * 삭제 대상({@code excludeAddressId})은 제외. 후보가 없으면(마지막 남은 배송지) null.
     *
     * <p>재작업 지시(round 2) — {@code delete()}는 더 이상 이 메서드를 호출하지 않는다(비잠금
     * 재조회가 락 대기 중 커밋된 변경을 못 볼 수 있어 계획 위반이었다 — {@link
     * #selectByMemberIdForUpdate} javadoc 참고). 메서드 자체는 매퍼 SQL 검증용
     * {@code MemberAddressDaoTest}가 계속 쓰므로 남겨둔다.
     */
    MemberAddress selectNextDefaultCandidate(@Param("memberId") String memberId,
                                              @Param("excludeAddressId") Long excludeAddressId);
}
