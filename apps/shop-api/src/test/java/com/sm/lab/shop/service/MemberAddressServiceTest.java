// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberAddressDao;
import com.sm.lab.shop.domain.MemberAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * SR-235(FUNC-member-011) — {@link MemberAddressService} 단위 테스트(Mockito, DAO 목업).
 * HTTP 계약은 {@code MemberAddressControllerTest}가, SQL 자체는 {@code MemberAddressDaoTest}가
 * 맡는다 — 이 테스트는 STORY "테스트" 절이 지정한 경계 조건(승계 로직·기본 강제·멱등)만 본다.
 */
class MemberAddressServiceTest {

    private static final String MEMBER_ID = "M-0001";
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-09-13T00:00:00Z"), ZoneOffset.UTC);

    private MemberAddressDao dao;
    private MemberAddressService service;

    @BeforeEach
    void setUp() {
        dao = mock(MemberAddressDao.class);
        service = new MemberAddressService(dao, FIXED_CLOCK);
    }

    private static MemberAddressService.AddressInput validRequest(Boolean isDefault) {
        return new MemberAddressService.AddressInput("홍길동", "01011112222", "12345", "서울시 강남대로 1",
                "101동 101호", null, null, isDefault);
    }

    private static MemberAddress lockRow(Long addressId, String isDefault) {
        return lockRow(addressId, isDefault, null, null);
    }

    // 재작업(round 2) — selectByMemberIdForUpdate가 실제로 반환하는 모양(address_id, is_default,
    // last_used_at, created_at)을 그대로 흉내낸다. delete()의 승계 후보 선정이 이 두 필드로
    // 정렬(last_used_at DESC, created_at DESC)하므로 단위 테스트에서도 값을 채워야 한다.
    private static MemberAddress lockRow(Long addressId, String isDefault, LocalDateTime lastUsedAt,
                                          LocalDateTime createdAt) {
        MemberAddress row = new MemberAddress();
        row.setAddressId(addressId);
        row.setIsDefault(isDefault);
        row.setLastUsedAt(lastUsedAt);
        row.setCreatedAt(createdAt);
        return row;
    }

    // linked_tc: TC-FUNC-member-011-S01 — 기존 0건이면 요청 isDefault 값과 무관하게 강제 Y.
    @Test
    void register_noExistingAddresses_forcesDefaultYRegardlessOfRequest() {
        when(dao.selectByMemberIdForUpdate(MEMBER_ID)).thenReturn(new ArrayList<>());

        MemberAddress created = service.register(MEMBER_ID, validRequest(false));

        assertThat(created.getIsDefault()).isEqualTo("Y");
        verify(dao, never()).clearDefaultForMember(MEMBER_ID);
        verify(dao).insertAddress(any(MemberAddress.class));
    }

    // linked_tc: TC-FUNC-member-011-S02 — 락으로 얻은 크기가 10이면 아무것도 쓰지 않고 409.
    @Test
    void register_atCapacity_throws409WithoutWriting() {
        List<MemberAddress> tenRows = new ArrayList<>();
        for (long i = 1; i <= 10; i++) {
            tenRows.add(lockRow(i, i == 1 ? "Y" : "N"));
        }
        when(dao.selectByMemberIdForUpdate(MEMBER_ID)).thenReturn(tenRows);

        assertThatThrownBy(() -> service.register(MEMBER_ID, validRequest(false)))
                .isInstanceOf(MemberAddressApiException.class)
                .hasFieldOrPropertyWithValue("code", "MBR-4201");

        verify(dao, never()).clearDefaultForMember(MEMBER_ID);
        verify(dao, never()).insertAddress(any(MemberAddress.class));
    }

    // linked_tc: TC-FUNC-member-011-S03 — 기존 존재 + 요청 isDefault=true → clear 후 insert(Y).
    @Test
    void register_existingAddressesAndRequestDefault_clearsThenInsertsDefault() {
        when(dao.selectByMemberIdForUpdate(MEMBER_ID)).thenReturn(List.of(lockRow(1L, "Y")));

        MemberAddress created = service.register(MEMBER_ID, validRequest(true));

        verify(dao, times(1)).clearDefaultForMember(MEMBER_ID);
        ArgumentCaptor<MemberAddress> captor = ArgumentCaptor.forClass(MemberAddress.class);
        verify(dao).insertAddress(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo("Y");
        assertThat(created.getIsDefault()).isEqualTo("Y");
        // 사람 수정(1) — last_used_at은 created_at과 동일 값(NULL 금지).
        assertThat(captor.getValue().getLastUsedAt()).isEqualTo(captor.getValue().getCreatedAt());
    }

    // linked_tc: TC-FUNC-member-011-S04 — selectOwned null → 404, updateAddress 호출 안 됨.
    @Test
    void update_notOwned_throws404WithoutUpdating() {
        when(dao.selectOwned(MEMBER_ID, 1L)).thenReturn(null);

        assertThatThrownBy(() -> service.update(MEMBER_ID, 1L, validRequest(null)))
                .isInstanceOf(MemberAddressApiException.class)
                .hasFieldOrPropertyWithValue("code", "MBR-4041");

        verify(dao, never()).updateAddress(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    // linked_tc: TC-FUNC-member-011-S05 — 기본 배송지 삭제 → 승계 후보는 selectByMemberIdForUpdate
    // (락)가 반환한 목록에서 last_used_at DESC, created_at DESC로 고른다(재작업 round 2). 비잠금
    // selectNextDefaultCandidate는 delete 경로에서 더 이상 호출되지 않는다.
    @Test
    void delete_defaultAddressWithCandidate_promotesCandidateFromLockedList() {
        LocalDateTime base = LocalDateTime.now();
        MemberAddress target = lockRow(1L, "Y", base.minusDays(5), base.minusDays(5));
        MemberAddress older = lockRow(2L, "N", base.minusDays(3), base.minusDays(3));
        MemberAddress newest = lockRow(3L, "N", base.minusDays(1), base.minusDays(1));
        when(dao.selectOwned(MEMBER_ID, 1L)).thenReturn(target);
        when(dao.selectByMemberIdForUpdate(MEMBER_ID)).thenReturn(List.of(target, older, newest));

        service.delete(MEMBER_ID, 1L);

        verify(dao).softDelete(eq(MEMBER_ID), eq(1L), any(LocalDateTime.class));
        verify(dao).setDefault(MEMBER_ID, 3L);
        verify(dao, never()).selectNextDefaultCandidate(any(), anyLong());
    }

    // 회귀(재작업 지시 1의 핵심) — selectOwned(락 이전 스냅샷)가 본 is_default와 selectByMemberIdForUpdate
    // (락, 최신 커밋본)가 본 is_default가 서로 다르면(동시 setDefault/register가 락 대기 중 끼어든 상황을
    // 흉내) 락 목록 쪽을 따라야 한다 — 스냅샷을 근거로 판정하던 종전 결함이 재발하지 않는지 단위
    // 테스트로 고정한다.
    @Test
    void delete_lockedListDisagreesWithPreLockSnapshot_usesLockedListDefault() {
        MemberAddress preLockSnapshot = lockRow(1L, "N"); // selectOwned가 본 값(락 이전, 이제 판정에 안 씀)
        MemberAddress lockedTarget = lockRow(1L, "Y");    // 락 획득 시점의 최신 커밋본 — 실제로는 기본
        MemberAddress candidate = lockRow(2L, "N", LocalDateTime.now(), LocalDateTime.now());
        when(dao.selectOwned(MEMBER_ID, 1L)).thenReturn(preLockSnapshot);
        when(dao.selectByMemberIdForUpdate(MEMBER_ID)).thenReturn(List.of(lockedTarget, candidate));

        service.delete(MEMBER_ID, 1L);

        verify(dao).setDefault(MEMBER_ID, 2L);
    }

    // linked_tc: TC-FUNC-member-011-S06 — 기본이 아닌 배송지 삭제 → 승계 조회 자체를 안 함.
    @Test
    void delete_nonDefaultAddress_doesNotQueryCandidate() {
        MemberAddress owned = lockRow(1L, "N");
        when(dao.selectOwned(MEMBER_ID, 1L)).thenReturn(owned);
        when(dao.selectByMemberIdForUpdate(MEMBER_ID)).thenReturn(List.of(owned));

        service.delete(MEMBER_ID, 1L);

        verify(dao).softDelete(eq(MEMBER_ID), eq(1L), any(LocalDateTime.class));
        verify(dao, never()).selectNextDefaultCandidate(any(), anyLong());
        verify(dao, never()).setDefault(any(), anyLong());
    }

    // linked_tc: TC-FUNC-member-011-S07 — 마지막 남은 배송지(락 목록에 대상 하나뿐, 승계 후보 없음)
    // → setDefault 없이 정상 종료(NPE 없음).
    @Test
    void delete_lastRemainingAddress_noCandidateNoNpe() {
        MemberAddress owned = lockRow(1L, "Y");
        when(dao.selectOwned(MEMBER_ID, 1L)).thenReturn(owned);
        when(dao.selectByMemberIdForUpdate(MEMBER_ID)).thenReturn(List.of(owned));

        service.delete(MEMBER_ID, 1L);

        verify(dao, never()).setDefault(any(), anyLong());
        verify(dao, never()).selectNextDefaultCandidate(any(), anyLong());
    }

    // linked_tc: TC-FUNC-member-011-S08 — 이미 기본인 대상 → clear/setDefault 호출 없이 no-op 성공.
    @Test
    void setDefault_alreadyDefault_isNoOp() {
        MemberAddress owned = lockRow(1L, "Y");
        when(dao.selectOwned(MEMBER_ID, 1L)).thenReturn(owned);

        MemberAddress result = service.setDefault(MEMBER_ID, 1L);

        assertThat(result.getIsDefault()).isEqualTo("Y");
        verify(dao, never()).clearDefaultForMember(any());
        verify(dao, never()).setDefault(any(), anyLong());
        verify(dao, never()).selectByMemberIdForUpdate(any());
    }

    // linked_tc: TC-FUNC-member-011-S09 — setDefault 대상이 없거나 남의 것 → 404.
    @Test
    void setDefault_notOwned_throws404() {
        when(dao.selectOwned(MEMBER_ID, 99L)).thenReturn(null);

        assertThatThrownBy(() -> service.setDefault(MEMBER_ID, 99L))
                .isInstanceOf(MemberAddressApiException.class)
                .hasFieldOrPropertyWithValue("code", "MBR-4041");
    }

    // linked_tc: TC-FUNC-member-011-S10 — 필수값 누락(recipient 공백) → 400, DAO 호출 전 차단.
    @Test
    void register_blankRecipient_throws400BeforeAnyDaoCall() {
        MemberAddressService.AddressInput req = new MemberAddressService.AddressInput(" ", "01011112222",
                "12345", "서울시 강남대로 1", "101동 101호", null, null, null);

        assertThatThrownBy(() -> service.register(MEMBER_ID, req))
                .isInstanceOf(MemberAddressApiException.class)
                .hasFieldOrPropertyWithValue("code", "MBR-4200");

        Mockito.verifyNoInteractions(dao);
    }

    // linked_tc: TC-FUNC-member-011-S12 — 원문 phone 길이가 DDL 상한(20)을 넘으면 정규화 결과
    // 패턴 통과 여부와 무관하게 400(재작업 지시 3 — 정규화 전에 검사해 500 MBR-5000을 막는다).
    @Test
    void register_phoneRawLengthOverTwenty_throws400() {
        String longPhone = "0".repeat(21);
        MemberAddressService.AddressInput req = new MemberAddressService.AddressInput("홍길동", longPhone,
                "12345", "서울시 강남대로 1", "101동 101호", null, null, null);

        assertThatThrownBy(() -> service.register(MEMBER_ID, req))
                .isInstanceOf(MemberAddressApiException.class)
                .hasFieldOrPropertyWithValue("code", "MBR-4200");

        Mockito.verifyNoInteractions(dao);
    }

    // linked_tc: TC-FUNC-member-011-S11 — 우편번호 형식 위반(5자리 숫자 아님) → 400.
    @Test
    void register_invalidZipcode_throws400() {
        MemberAddressService.AddressInput req = new MemberAddressService.AddressInput("홍길동", "01011112222",
                "1234", "서울시 강남대로 1", "101동 101호", null, null, null);

        assertThatThrownBy(() -> service.register(MEMBER_ID, req))
                .isInstanceOf(MemberAddressApiException.class)
                .hasFieldOrPropertyWithValue("code", "MBR-4200");
    }
}
