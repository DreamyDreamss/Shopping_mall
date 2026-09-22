// linked_func: FUNC-member-012
// spec: docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.ZipcodeDao;
import com.sm.lab.shop.domain.Zipcode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * SR-235(FUNC-member-012) — {@link ZipcodeService} 단위 테스트(Mockito, DAO 목업). HTTP 계약은
 * {@code ZipcodeControllerTest}가, 매퍼 SQL 자체는 {@code ZipcodeDaoTest}가 맡는다 — 이 테스트는
 * 검색어 정규화(trim + 연속 공백 한 칸)와 숫자 판별(전방일치 분기)만 본다(사람 수정 (1)(2)).
 */
class ZipcodeServiceTest {

    private ZipcodeDao dao;
    private ZipcodeService service;

    @BeforeEach
    void setUp() {
        dao = mock(ZipcodeDao.class);
        service = new ZipcodeService(dao);
    }

    // linked_tc: TC-FUNC-member-012-S01
    @Test
    void search_normalWord_delegatesToDaoWithNonNumericFlag() {
        Zipcode found = new Zipcode();
        when(dao.search("강남", false)).thenReturn(List.of(found));

        List<Zipcode> result = service.search("강남");

        assertThat(result).containsExactly(found);
        verify(dao).search("강남", false);
    }

    // linked_tc: TC-FUNC-member-012-S02
    @Test
    void search_null_throwsValidationErrorWithoutCallingDao() {
        assertThatThrownBy(() -> service.search(null))
                .isInstanceOf(ZipcodeApiException.class)
                .satisfies(ex -> {
                    ZipcodeApiException apiEx = (ZipcodeApiException) ex;
                    assertThat(apiEx.getCode()).isEqualTo("MBR-4202");
                });
        verifyNoInteractions(dao);
    }

    // linked_tc: TC-FUNC-member-012-S03
    @Test
    void search_emptyString_throwsValidationError() {
        assertThatThrownBy(() -> service.search(""))
                .isInstanceOf(ZipcodeApiException.class);
        verify(dao, never()).search(anyString(), anyBoolean());
    }

    // linked_tc: TC-FUNC-member-012-S04 — 공백만(trim 후 빈 문자열).
    @Test
    void search_blankOnly_throwsValidationError() {
        assertThatThrownBy(() -> service.search("   "))
                .isInstanceOf(ZipcodeApiException.class);
        verify(dao, never()).search(anyString(), anyBoolean());
    }

    // linked_tc: TC-FUNC-member-012-S05 — trim 후 1자.
    @Test
    void search_oneCharAfterTrim_throwsValidationError() {
        assertThatThrownBy(() -> service.search("a"))
                .isInstanceOf(ZipcodeApiException.class);
        verify(dao, never()).search(anyString(), anyBoolean());
    }

    // linked_tc: TC-FUNC-member-012-S06 — 앞뒤 공백 포함 4자, trim 후 2자 → 성공, trim된 값으로 호출.
    @Test
    void search_leadingTrailingWhitespace_trimsBeforeDelegating() {
        when(dao.search("강남", false)).thenReturn(List.of());

        service.search("  강남  ");

        verify(dao).search("강남", false);
    }

    // linked_tc: TC-FUNC-member-012-S07 — 연속 공백은 한 칸으로 정규화(사람 수정 (1)).
    @Test
    void search_consecutiveInternalWhitespace_collapsesToSingleSpace() {
        when(dao.search("강남 대로", false)).thenReturn(List.of());

        service.search("강남   대로");

        verify(dao).search("강남 대로", false);
    }

    // linked_tc: TC-FUNC-member-012-S08 — 정규화 후 정확히 50자 → 통과.
    @Test
    void search_exactlyMaxLength_succeeds() {
        String q = "가".repeat(50);
        when(dao.search(q, false)).thenReturn(List.of());

        service.search(q);

        verify(dao).search(q, false);
    }

    // linked_tc: TC-FUNC-member-012-S09 — 정규화 후 51자 → 400(사람 수정 (1)).
    @Test
    void search_exceedsMaxLength_throwsValidationError() {
        String q = "가".repeat(51);

        assertThatThrownBy(() -> service.search(q))
                .isInstanceOf(ZipcodeApiException.class)
                .satisfies(ex -> assertThat(((ZipcodeApiException) ex).getCode()).isEqualTo("MBR-4202"));
        verify(dao, never()).search(anyString(), anyBoolean());
    }

    // linked_tc: TC-FUNC-member-012-S10 — 숫자로만 구성 → numeric=true(사람 수정 (2)).
    @Test
    void search_numericOnly_delegatesToDaoWithNumericFlag() {
        when(dao.search("123", true)).thenReturn(List.of());

        service.search("123");

        verify(dao).search("123", true);
    }

    // linked_tc: TC-FUNC-member-012-S11 — 숫자+공백 혼합은 숫자 전용이 아니므로 road_address 분기.
    @Test
    void search_digitsWithInternalSpace_isNotTreatedAsNumeric() {
        when(dao.search("12 34", false)).thenReturn(List.of());

        service.search("12 34");

        verify(dao).search("12 34", false);
    }

    // linked_tc: TC-FUNC-member-012-S12 — DAO가 빈 리스트를 반환하면 서비스도 예외 없이 그대로 반환.
    @Test
    void search_daoReturnsEmptyList_returnsEmptyListWithoutException() {
        when(dao.search(eq("존재하지않음"), eq(false))).thenReturn(List.of());

        List<Zipcode> result = service.search("존재하지않음");

        assertThat(result).isEmpty();
    }
}
