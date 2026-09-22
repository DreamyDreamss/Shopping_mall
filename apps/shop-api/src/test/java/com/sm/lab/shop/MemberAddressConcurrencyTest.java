// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop;

import com.sm.lab.shop.dao.MemberAddressDao;
import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.domain.MemberAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 회귀 — SR-235(FUNC-member-011) STORY "테스트·완료 조건" 절: (1) 9건 상태에서 2스레드
 * 동시 등록 → 정확히 1건만 성공(201)하고 나머지 1건은 409, 최종 10건(11건 아님). (2) 같은 회원의
 * 서로 다른 배송지 2개에 대해 동시 기본설정 → 종료 후 기본은 항상 정확히 1건.
 *
 * <p>{@code selectByMemberIdForUpdate}(락 지점)가 카운트-후-삽입/기본전환 경쟁을 막는지 실
 * DB(RANDOM_PORT)로 검증한다(사례집 SR-231 r3 — 락 순서를 한 곳에 고정). admin 키 +
 * {@code ?memberId=}로 임의 테스트 회원을 직접 지정한다({@code CartConcurrencyTest} 관례 —
 * admin 스코프는 {@code evaluateMemberScope}를 타지 않아 SCOPE_TO_SELF 강제와 무관하게 명시
 * memberId를 그대로 쓴다).
 *
 * <p>{@link CyclicBarrier}로 두 스레드가 실제로 동시에 출발하도록 강제한다
 * ({@code MemberRegistrationConcurrencyTest}와 동일 근거 — CountDownLatch만으로는 JVM 워밍업
 * 때문에 순차 처리로 겹치지 않을 위험이 있다). 이 클래스는 플레이키 여부 확인을 위해 3회 단독
 * 실행하는 것이 house 관례다(사례집 SR-231 r3).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberAddressConcurrencyTest {

    private static final String ADMIN_KEY = "lab-admin-key";
    private static final String CAPACITY_MEMBER_ID = "M-9011-CONC-CAP-TEST";
    private static final String DEFAULT_MEMBER_ID = "M-9011-CONC-DEF-TEST";
    // 재작업(round 2, QA CONCERNS 권고#1) — delete()의 승계 판정을 락 목록 기반으로 고친 것을
    // 실 DB 동시성으로 고정하는 두 테스트 전용 회원.
    private static final String DELETE_RACE_MEMBER_ID = "M-9011-DELRACE-TST";
    private static final String LAST_DEFAULT_RACE_MEMBER_ID = "M-9011-LASTDEF-TST";
    private static final int CONCURRENCY = 2;

    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private MemberAddressDao memberAddressDao;
    @Autowired
    private MemberDao memberDao;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpMembers() {
        LocalDateTime now = LocalDateTime.now();
        memberDao.insertMember(CAPACITY_MEMBER_ID, "동시성용량테스터", "BRONZE",
                "member-address-conc-cap-test@example.com", null, null, "unused-hash", false, now);
        memberDao.insertMember(DEFAULT_MEMBER_ID, "동시성기본테스터", "BRONZE",
                "member-address-conc-def-test@example.com", null, null, "unused-hash", false, now);
        memberDao.insertMember(DELETE_RACE_MEMBER_ID, "동시성삭제경합테스터", "BRONZE",
                "member-address-conc-delrace-test@example.com", null, null, "unused-hash", false, now);
        memberDao.insertMember(LAST_DEFAULT_RACE_MEMBER_ID, "동시성최후기본테스터", "BRONZE",
                "member-address-conc-lastdef-test@example.com", null, null, "unused-hash", false, now);
    }

    @AfterEach
    void cleanUp() {
        jdbcTemplate.update("DELETE FROM MEMBER_ADDRESSES WHERE member_id IN (?, ?, ?, ?)",
                CAPACITY_MEMBER_ID, DEFAULT_MEMBER_ID, DELETE_RACE_MEMBER_ID, LAST_DEFAULT_RACE_MEMBER_ID);
        memberDao.deleteById(CAPACITY_MEMBER_ID);
        memberDao.deleteById(DEFAULT_MEMBER_ID);
        memberDao.deleteById(DELETE_RACE_MEMBER_ID);
        memberDao.deleteById(LAST_DEFAULT_RACE_MEMBER_ID);
    }

    // linked_tc: TC-FUNC-member-011-C01
    @Test
    void concurrentRegister_atNineExisting_exactlyOneSucceedsFinalCountTen() throws InterruptedException {
        for (int i = 0; i < 9; i++) {
            insertAddress(CAPACITY_MEMBER_ID, i, "N");
        }

        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        for (int i = 0; i < CONCURRENCY; i++) {
            futures.add(pool.submit(callRegister(barrier, CAPACITY_MEMBER_ID)));
        }
        pool.shutdown();
        List<ResponseEntity<String>> results = collect(futures);

        long successCount = results.stream().filter(r -> r.getStatusCode().value() == 201).count();
        long conflictCount = results.stream().filter(r -> r.getStatusCode().value() == 409).count();

        assertThat(successCount)
                .as("동시 %d건 중 정확히 1건만 201이어야 함 — 응답들: %s", CONCURRENCY, results)
                .isEqualTo(1);
        assertThat(conflictCount)
                .as("나머지 %d건은 409(MBR-4201)여야 함 — 응답들: %s", CONCURRENCY - 1, results)
                .isEqualTo(CONCURRENCY - 1);

        Integer finalCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_ADDRESSES WHERE member_id = ? AND del_yn = 'N'",
                Integer.class, CAPACITY_MEMBER_ID);
        assertThat(finalCount).as("최종 10건이어야 함(11건 아님 — lost update 재현 확인)").isEqualTo(10);
    }

    // linked_tc: TC-FUNC-member-011-C02
    @Test
    void concurrentSetDefault_twoDifferentAddresses_exactlyOneEndsAsDefault() throws InterruptedException {
        Long addr1 = insertAddress(DEFAULT_MEMBER_ID, 1, "N");
        Long addr2 = insertAddress(DEFAULT_MEMBER_ID, 2, "N");

        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        futures.add(pool.submit(callSetDefault(barrier, DEFAULT_MEMBER_ID, addr1)));
        futures.add(pool.submit(callSetDefault(barrier, DEFAULT_MEMBER_ID, addr2)));
        pool.shutdown();
        List<ResponseEntity<String>> results = collect(futures);

        for (ResponseEntity<String> r : results) {
            assertThat(r.getStatusCode().is2xxSuccessful())
                    .as("500/기타 오류 없이 전부 2xx여야 함 — 응답들: %s", results)
                    .isTrue();
        }

        Integer defaultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_ADDRESSES WHERE member_id = ? AND is_default = 'Y' AND del_yn = 'N'",
                Integer.class, DEFAULT_MEMBER_ID);
        assertThat(defaultCount)
                .as("동시 기본설정 2건 후에도 기본은 정확히 1행이어야 함(락으로 직렬화)")
                .isEqualTo(1);
    }

    // linked_tc: TC-FUNC-member-011-C03 — 재작업 지시(round 2, QA CONCERNS 권고#1) 동시성 회귀(a):
    // 기본 배송지 delete()와 다른 배송지의 setDefault()가 동시에 들어와도 종료 후 기본은 정확히
    // 1행이어야 한다. delete()가 락(selectByMemberIdForUpdate) 이전 스냅샷으로 승계를 판정하던
    // 종전 결함은, 두 트랜잭션의 실행 순서에 따라 기본이 0건 또는 2건이 될 수 있었다(스냅샷이
    // 상대 트랜잭션의 커밋 결과를 못 보는 경우). 락 목록 기반 판정은 어느 순서로 인터리빙되든
    // 정확히 1행으로 수렴해야 한다.
    @Test
    void concurrentDeleteDefault_andSetDefaultOnAnotherAddress_exactlyOneDefaultRemains() throws InterruptedException {
        Long defaultAddr = insertAddress(DELETE_RACE_MEMBER_ID, 1, "Y");
        Long otherAddr = insertAddress(DELETE_RACE_MEMBER_ID, 2, "N");

        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        futures.add(pool.submit(callDelete(barrier, DELETE_RACE_MEMBER_ID, defaultAddr)));
        futures.add(pool.submit(callSetDefault(barrier, DELETE_RACE_MEMBER_ID, otherAddr)));
        pool.shutdown();
        List<ResponseEntity<String>> results = collect(futures);

        for (ResponseEntity<String> r : results) {
            assertThat(r.getStatusCode().is2xxSuccessful())
                    .as("500/기타 오류 없이 전부 2xx여야 함 — 응답들: %s", results)
                    .isTrue();
        }

        Integer defaultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_ADDRESSES WHERE member_id = ? AND is_default = 'Y' AND del_yn = 'N'",
                Integer.class, DELETE_RACE_MEMBER_ID);
        assertThat(defaultCount)
                .as("기본 배송지 삭제‖다른 배송지 기본전환 동시 실행 후에도 기본은 정확히 1행이어야 함")
                .isEqualTo(1);
    }

    // linked_tc: TC-FUNC-member-011-C04 — 재작업 지시(round 2) 동시성 회귀(b): 기본 배송지가 마지막
    // 1건 남은 상태에서 그 배송지의 delete()와 새 배송지의 register()가 동시에 들어와도 종료 후
    // 기본 배송지가 정확히 1건이어야 한다(0건이 되면 안 됨 — register()가 "기존 0건이면 무조건
    // 기본"으로 강제하는 경로와 delete()의 "마지막 남은 배송지는 승계 없음" 경로가 서로 다른
    // 순서로 겹쳐도 최종적으로 기본을 잃지 않아야 한다).
    @Test
    void concurrentDeleteLastDefault_andRegisterNewAddress_exactlyOneDefaultRemains() throws InterruptedException {
        Long onlyAddr = insertAddress(LAST_DEFAULT_RACE_MEMBER_ID, 1, "Y");

        CyclicBarrier barrier = new CyclicBarrier(CONCURRENCY);
        ExecutorService pool = Executors.newFixedThreadPool(CONCURRENCY);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();
        futures.add(pool.submit(callDelete(barrier, LAST_DEFAULT_RACE_MEMBER_ID, onlyAddr)));
        futures.add(pool.submit(callRegister(barrier, LAST_DEFAULT_RACE_MEMBER_ID)));
        pool.shutdown();
        List<ResponseEntity<String>> results = collect(futures);

        for (ResponseEntity<String> r : results) {
            assertThat(r.getStatusCode().is2xxSuccessful())
                    .as("500/기타 오류 없이 전부 2xx여야 함 — 응답들: %s", results)
                    .isTrue();
        }

        Integer defaultCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM MEMBER_ADDRESSES WHERE member_id = ? AND is_default = 'Y' AND del_yn = 'N'",
                Integer.class, LAST_DEFAULT_RACE_MEMBER_ID);
        assertThat(defaultCount)
                .as("마지막 기본 배송지 삭제‖새 배송지 등록 동시 실행 후에도 기본은 정확히 1건이어야 함(0건 아님)")
                .isEqualTo(1);
    }

    private Callable<ResponseEntity<String>> callRegister(CyclicBarrier barrier, String memberId) {
        return () -> {
            awaitBarrier(barrier);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-Api-Key", ADMIN_KEY);
            String body = "{\"recipient\":\"동시테스터\",\"phone\":\"01099998888\",\"zipcode\":\"12345\","
                    + "\"roadAddress\":\"서울시 동시로 1\",\"detailAddress\":\"1동 1호\"}";
            return restTemplate.postForEntity(
                    "/api/members/me/addresses?memberId=" + memberId,
                    new HttpEntity<>(body, headers), String.class);
        };
    }

    private Callable<ResponseEntity<String>> callSetDefault(CyclicBarrier barrier, String memberId, Long addressId) {
        return () -> {
            awaitBarrier(barrier);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", ADMIN_KEY);
            return restTemplate.exchange(
                    "/api/members/me/addresses/" + addressId + "/default?memberId=" + memberId,
                    HttpMethod.PUT, new HttpEntity<>(headers), String.class);
        };
    }

    private Callable<ResponseEntity<String>> callDelete(CyclicBarrier barrier, String memberId, Long addressId) {
        return () -> {
            awaitBarrier(barrier);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", ADMIN_KEY);
            return restTemplate.exchange(
                    "/api/members/me/addresses/" + addressId + "?memberId=" + memberId,
                    HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
        };
    }

    private static void awaitBarrier(CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
        } catch (BrokenBarrierException | TimeoutException | InterruptedException e) {
            throw new IllegalStateException("CyclicBarrier 대기 실패", e);
        }
    }

    private static List<ResponseEntity<String>> collect(List<Future<ResponseEntity<String>>> futures) {
        List<ResponseEntity<String>> results = new ArrayList<>();
        for (Future<ResponseEntity<String>> f : futures) {
            try {
                results.add(f.get(15, TimeUnit.SECONDS));
            } catch (ExecutionException | TimeoutException | InterruptedException e) {
                throw new AssertionError("동시 요청 실행 실패", e);
            }
        }
        return results;
    }

    private Long insertAddress(String memberId, int seq, String isDefault) {
        LocalDateTime base = LocalDateTime.now().minusMinutes(seq);
        MemberAddress address = new MemberAddress();
        address.setMemberId(memberId);
        address.setRecipient("배송지" + seq);
        address.setPhone("0109999" + String.format("%04d", seq));
        address.setPhoneNorm(address.getPhone());
        address.setZipcode("12345");
        address.setRoadAddress("서울시 동시로 " + seq);
        address.setDetailAddress(seq + "동 " + seq + "호");
        address.setIsDefault(isDefault);
        address.setLastUsedAt(base);
        address.setCreatedAt(base);
        address.setUpdatedAt(base);
        memberAddressDao.insertAddress(address);
        return address.getAddressId();
    }
}
