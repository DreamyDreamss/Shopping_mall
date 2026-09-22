// linked_func: FUNC-member-012
// spec: docs/00_FUNC/stories/STORY-FUNC-member-012.md
package com.sm.lab.shop.dao;

import com.sm.lab.shop.domain.Zipcode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실 DB(MariaDB, sl_lab.ZIPCODES) 대상 통합 테스트 — 매퍼 SQL 자체(도로명 부분일치·우편번호
 * 전방일치·정렬·LIMIT 50·컬럼 매핑)를 검증한다(STORY "테스트" 절). Mockito로는 실 SQL의
 * {@code LIKE} 분기·{@code LIMIT}을 증명할 수 없다.
 *
 * <p>테스트 격리(STORY "테스트 격리" 절) — 운영 시드(V8의 100건, zipcode 10000~10099)의 정확한
 * 문구·지역명에 의존하지 않는다. 매 테스트가 UUID 마커가 포함된 행을 {@link JdbcTemplate}으로
 * 직접 삽입하고({@link ZipcodeDao}에는 insert 메서드가 없다 — 이 테이블은 애플리케이션이 쓰지
 * 않는 정적 참조데이터), {@code @AfterEach}에서 이 클래스가 삽입한 행만 zipcode 목록 기준으로
 * 정리한다. 운영 시드 100건은 건드리지 않는다. 테스트 zipcode는 운영 시드 범위와 겹치지 않는
 * "9" 접두 + 단조증가 카운터(랜덤이 아님 — 60행을 한 테스트에서 삽입해도 서로 충돌하지 않게)를
 * 써서 {@code UNIQUE KEY uq_zipcodes_zipcode} 충돌을 피한다.
 */
@SpringBootTest
class ZipcodeDaoTest {

    private static final AtomicInteger ZIPCODE_COUNTER = new AtomicInteger(0);

    @Autowired
    private ZipcodeDao dao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> insertedZipcodes = new ArrayList<>();

    /**
     * round2 재작업(QA FAIL 필수4·권고1): {@code ZIPCODE_COUNTER}는 JVM마다 0에서 재시작하는
     * static {@link AtomicInteger}라, 이전 실행이 {@code @AfterEach} 전에 중단되면(예: 강제
     * 종료) 남은 "9%" 행이 다음 실행의 같은 카운터 값과 {@code UNIQUE KEY uq_zipcodes_zipcode}
     * 충돌을 일으켜 plain INSERT가 하드 실패한다(SR-232 r2 계열). 매 테스트 시작 전에 이 클래스
     * 영역("9" 접두)을 선청소해 이전 실행의 잔여 행을 절대 만나지 않게 한다.
     */
    @BeforeEach
    void cleanStaleRowsFromPreviousRuns() {
        jdbcTemplate.update("DELETE FROM ZIPCODES WHERE zipcode LIKE '9%'");
    }

    @AfterEach
    void cleanUp() {
        if (insertedZipcodes.isEmpty()) {
            return;
        }
        String placeholders = String.join(",", insertedZipcodes.stream().map(z -> "?").toList());
        jdbcTemplate.update("DELETE FROM ZIPCODES WHERE zipcode IN (" + placeholders + ")",
                insertedZipcodes.toArray());
        insertedZipcodes.clear();
    }

    /** 운영 시드는 10000~10099(전부 "1" 접두) — "9" 접두 + 단조증가 카운터로 절대 충돌하지 않는다. */
    private String uniqueZipcode() {
        int n = ZIPCODE_COUNTER.incrementAndGet() % 10000;
        String zipcode = "9" + String.format("%04d", n);
        insertedZipcodes.add(zipcode);
        return zipcode;
    }

    private void insertRow(String zipcode, String roadAddress, String sido, String sigungu) {
        jdbcTemplate.update("INSERT INTO ZIPCODES (zipcode, road_address, sido, sigungu) VALUES (?, ?, ?, ?)",
                zipcode, roadAddress, sido, sigungu);
    }

    // linked_tc: TC-FUNC-member-012-D01 — 도로명 부분일치 + 응답 4필드 셰이프.
    @Test
    void search_roadAddressPartialMatch_returnsMatchingRowWithCorrectFields() {
        String marker = "테스트로마커-" + UUID.randomUUID();
        String zipcode = uniqueZipcode();
        insertRow(zipcode, "서울특별시 강남구 " + marker + " 1", "서울특별시", "강남구");

        List<Zipcode> result = dao.search(marker, false);

        assertThat(result).hasSize(1);
        Zipcode found = result.get(0);
        assertThat(found.getZipcode()).isEqualTo(zipcode);
        assertThat(found.getRoadAddress()).isEqualTo("서울특별시 강남구 " + marker + " 1");
        assertThat(found.getSido()).isEqualTo("서울특별시");
        assertThat(found.getSigungu()).isEqualTo("강남구");
    }

    // linked_tc: TC-FUNC-member-012-D02 — 존재하지 않는 검색어는 예외 없이 빈 리스트.
    @Test
    void search_noMatch_returnsEmptyList() {
        List<Zipcode> result = dao.search("절대존재하지않는마커-" + UUID.randomUUID(), false);

        assertThat(result).isEmpty();
    }

    // linked_tc: TC-FUNC-member-012-D03 — 우편번호 전방일치(숫자 검색어, 사람 수정 (2)).
    @Test
    void search_numericTrue_matchesByZipcodePrefix() {
        String zipcode = uniqueZipcode(); // "9" + 4자리 카운터, 예: 90001
        insertRow(zipcode, "인천광역시 남동구 전방일치테스트로 1", "인천광역시", "남동구");
        String prefix = zipcode.substring(0, 3);

        List<Zipcode> result = dao.search(prefix, true);

        assertThat(result).extracting(Zipcode::getZipcode).contains(zipcode);
    }

    // linked_tc: TC-FUNC-member-012-D04 — round2 재작업(QA FAIL 필수4): numeric=true는 배타가
    // 아니라 OR이다(사람 수정 (2) "zipcode 전방일치로도 검색한다" = 추가, 대체가 아님). digits는
    // "1" 접두(운영 시드·테스트 zipcode의 "9" 접두 어느 쪽과도 겹치지 않음)라 이 행의 zipcode
    // ("9...")가 전방일치로 우연히 잡힐 수 없다 — 그런데도 road_address 부분일치로 잡혀야 한다.
    // 종전엔 이 테스트가 "doesNotContain"으로 배타 동작을 못 박아 뒀으나, <choose> 배타 구현이
    // 시드 zipcode가 전부 "100xx"인 상황에서 숫자 검색어로 road_address 매치를 놓치는 결함의
    // 원인이었다 — 서술이 아니라 사람 확정 지시가 우선이므로 코드가 아니라 이 테스트를 고친다.
    @Test
    void search_numericTrue_alsoMatchesByRoadAddress() {
        String digits = "12345";
        String zipcode = uniqueZipcode();
        insertRow(zipcode, "부산광역시 해운대구 " + digits + "로 1", "부산광역시", "해운대구");

        List<Zipcode> result = dao.search(digits, true);

        assertThat(result).extracting(Zipcode::getZipcode).contains(zipcode);
    }

    // linked_tc: TC-FUNC-member-012-D05 — LIMIT 50(SR에 없는 Dev 판단, 응답 크기 캡).
    @Test
    void search_moreThan50Matches_returnsExactly50() {
        String marker = "한도테스트마커-" + UUID.randomUUID();
        for (int i = 0; i < 60; i++) {
            insertRow(uniqueZipcode(), "경기도 성남시 " + marker + " " + i, "경기도", "성남시");
        }

        List<Zipcode> result = dao.search(marker, false);

        assertThat(result).hasSize(50);
    }

    // linked_tc: TC-FUNC-member-012-D06 — road_address 오름차순 정렬.
    @Test
    void search_ordersByRoadAddressAscending() {
        String marker = "정렬테스트마커-" + UUID.randomUUID();
        String zipcodeA = uniqueZipcode();
        String zipcodeB = uniqueZipcode();
        insertRow(zipcodeB, marker + " B동", "서울특별시", "종로구");
        insertRow(zipcodeA, marker + " A동", "서울특별시", "종로구");

        List<Zipcode> result = dao.search(marker, false);

        assertThat(result).extracting(Zipcode::getZipcode).containsExactly(zipcodeA, zipcodeB);
    }

    // linked_tc: TC-FUNC-member-012-D07 — round2 재작업(QA FAIL 필수3): 운영 시드(V8) 계약
    // 테스트. 이 프로젝트가 신규 테스트 26건 전량 통과에도 놓친 mojibake 회귀(spring.sql.init.
    // encoding 미지정)를 잡는 유일한 테스트다 — 정확한 문구가 아니라 분포·왕복 수준으로만
    // 단언해 "테스트 격리"(운영 시드 문구 비의존) 원칙과 충돌하지 않는다. 운영 시드를 삭제·
    // 수정하지 않고 조회만 한다("9%" 접두는 이 클래스의 테스트 전용 영역이라 제외).
    @Test
    void seedData_hasExpectedDistributionAndIsReadableAsKorean() {
        Integer totalRows = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ZIPCODES WHERE zipcode NOT LIKE '9%'", Integer.class);
        assertThat(totalRows).isGreaterThanOrEqualTo(100);

        Integer distinctSido = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT sido) FROM ZIPCODES WHERE zipcode NOT LIKE '9%'", Integer.class);
        assertThat(distinctSido).isGreaterThanOrEqualTo(5);

        Integer distinctSigungu = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT sigungu) FROM ZIPCODES WHERE zipcode NOT LIKE '9%'", Integer.class);
        assertThat(distinctSigungu).isGreaterThanOrEqualTo(10);

        Integer roadPatternCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM ZIPCODES WHERE zipcode NOT LIKE '9%' AND road_address LIKE '%로 %길%'",
                Integer.class);
        assertThat(roadPatternCount).isGreaterThan(0);

        // 한글 왕복 검증(인코딩 회귀의 핵심 단언) — mojibake 상태면 아래 두 검색이 0건이거나
        // 저장값에 U+FFFD(치환 문자)가 섞여 나온다.
        List<Zipcode> gangnam = dao.search("강남", false);
        assertThat(gangnam).isNotEmpty();
        List<Zipcode> teheran = dao.search("테헤란로", false);
        assertThat(teheran).isNotEmpty();

        // 실제 인코딩 손상 검출은 한글 왕복 검색(강남/테헤란로) 단언이 한다 — round1의 실제
        // 손상(MS949 오독 → 유효한 한글 쓰레기)은 U+FFFD를 남기지 않아 아래 doesNotContain
        // 단언만으로는 잡히지 않는다.
        for (Zipcode z : gangnam) {
            assertThat(z.getRoadAddress()).doesNotContain("�");
            assertThat(z.getSido()).doesNotContain("�");
            assertThat(z.getSigungu()).doesNotContain("�");
        }
    }
}
