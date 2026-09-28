package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.ProductCompareDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductReviewDao;
import com.sm.lab.shop.domain.ProductCompareView;
import com.sm.lab.shop.domain.ProductReviewSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DeadlockLoserDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 상품 비교함 담기/빼기/목록(SR-360 #2, INF-ORD-045/046/047) — add/remove/list 3개 메서드. STORY
 * "구현 계획" 절, 사람 확정.
 *
 * <p><b>담기는 read-then-write가 아니라 단일 원자 상한부 INSERT IGNORE</b>(사례집 SR-231
 * r2/SR-356 #2와 동일 계열이나, 이 항목은 "상한(4개)"이 추가로 있어 {@code INSERT IGNORE} 단독으로는
 * 부족하다 — {@link ProductCompareDao#insertCompareIfUnderLimit}이 카운트 서브쿼리를 결합해
 * 확장한다). "이미 담긴 상품 재담기"와 "5번째 초과" 둘 다 영향행수 0으로 관측되므로, 0행일 때만
 * {@link ProductCompareDao#existsCompare}로 원인을 가른다 — 존재하면 멱등 성공, 존재하지 않으면
 * 진짜 상한 초과(409). 그 뒤 {@code memberDao.selectById}로 회원 실재까지 확인해(방어적
 * 엣지케이스 — admin 키가 존재하지 않는 memberId로 호출한 경우) 404 {@code CMP-4042}를 가른다
 * ({@code ProductWishService} round2 {@code WSH-4042}와 동일 이유).
 *
 * <p><b>동시성 위험은 가정이 아니라 실측</b> — AC가 "단일 조건부 INSERT"를 명시해 명시적 {@code
 * SELECT ... FOR UPDATE} 선락을 두지 않는다. 유효성은 {@code ProductCompareConcurrencyTest}가
 * 실측한다(STORY "데이터" 절 "동시성 위험 인지").
 *
 * <p><b>round2(재작업 지시 1, QA CONCERNS medium)</b> — 상한 판정({@code
 * insertCompareIfUnderLimit})은 잠금읽기(INSERT...SELECT, 항상 최신)인데 0행 원인 재조회
 * ({@code existsCompare})가 비잠금 일관읽기였던 탓에, 같은 sku 동시 담기(더블클릭)의 뒤 요청이
 * 방금 커밋된 앞 요청의 행을 옛 REPEATABLE READ 스냅샷에서 못 보고 "존재하지 않음"으로 판정해
 * 진짜 상한 초과가 아닌데도 409로 떨어졌다(QA 실측 — 사례집 SR-356 #2 round2와 같은 계열, 이번엔
 * 술어가 아니라 읽기 시점 불일치). {@code existsCompare}를 {@code FOR UPDATE} 잠금읽기로 바꿔
 * 같은 트랜잭션 안에서 두 읽기의 시점을 맞췄다.
 *
 * <p><b>round2 추가 실측 발견 — INSERT 자체의 동시 중복 삽입 데드락</b> — 위 수정을 신규 동시성
 * 케이스({@code concurrentAdd_sameSku_bothSucceedFinalCountOne})로 실측하는 과정에서, 같은 sku를
 * 두 트랜잭션이 동시에 {@code insertCompareIfUnderLimit}(카운트 서브쿼리가 같은 테이블을 읽는
 * INSERT...SELECT)로 삽입 시도하면 InnoDB가 "Deadlock found when trying to get lock"(에러 1213)을
 * 던지는 것을 확인했다(existsCompare 수정과는 별개 원인 — 두 트랜잭션이 같은 신규 키를 향해 경합할
 * 때의 InnoDB 문서화된 동작, MySQL 공식 권고는 "애플리케이션이 재시도할 것"). 이 항목은 AC가 명시한
 * "단일 조건부 INSERT"를 유지하면서(선락으로 바꾸지 않음) {@link DeadlockLoserDataAccessException}
 * 발생 시 새 트랜잭션으로 최대 {@value #MAX_DEADLOCK_ATTEMPTS}회까지 담기 전체를 재시도한다 —
 * 재시도 뒤에는 앞선 요청이 이미 커밋한 행을 {@code existsCompare}(FOR UPDATE, 항상 최신)가 보게
 * 되어 멱등 200으로 정상 수렴한다. {@code @Transactional} 선언형 대신 {@link TransactionOperations}
 * (프로덕션은 {@link TransactionTemplate})로 트랜잭션을 프로그래밍적으로 열어, 재시도마다 완전히
 * 새 트랜잭션(새 잠금 대기열 진입 순서)을 보장한다 — 선언형 AOP로는 같은 메서드 안에서 재시도할 때
 * self-invocation이 프록시를 우회해 새 트랜잭션이 열리지 않는다(아래 노트).
 *
 * <p><b>빼기는 상품 존재조차 확인하지 않는다</b> — {@code DELETE}는 "이 회원의 비교함 행"만 다루는
 * 연산이라 상품 존재 판정 자체가 무의미하다(STORY "데이터" 절). 존재/부재 무관 항상 성공.
 *
 * <p><b>self-invocation 함정 회피</b> — add/remove/list는 서로 다른 빈의 {@code @Transactional}
 * 메서드를 재호출하지 않는다. {@code add}의 데드락 재시도 재귀 호출({@code addWithRetry})은 같은
 * 메서드를 반복 호출하는 것이지만 {@code @Transactional} 애노테이션이 전혀 없다 — 트랜잭션은
 * {@link TransactionOperations#execute}가 호출될 때마다 명시적으로 새로 열리므로, AOP 프록시를
 * 거치지 않는 내부 재귀 호출이어도 매 시도가 독립된 트랜잭션이다(프록시 우회 문제 자체가
 * 발생하지 않는 설계 — STORY "프레임워크 실행 모델 함정" 절의 원 원칙을 유지하는 방식으로 확장).
 */
@Service
public class ProductCompareService {

    private static final String CODE_VALIDATION = "CMP-4001";
    private static final String CODE_PRODUCT_NOT_FOUND = "CMP-4041";
    private static final String CODE_LIMIT_EXCEEDED = "CMP-4091";
    private static final String CODE_MEMBER_NOT_FOUND = "CMP-4042";
    private static final String LIMIT_MESSAGE = "비교함은 최대 4개까지 담을 수 있습니다";
    private static final int MAX_DEADLOCK_ATTEMPTS = 3;

    private final ProductCompareDao dao;
    private final ProductDao productDao;
    private final MemberDao memberDao;
    private final ProductReviewDao productReviewDao;
    private final Clock clock;
    private final TransactionOperations transactionOperations;

    @Autowired
    public ProductCompareService(ProductCompareDao dao, ProductDao productDao, MemberDao memberDao,
                                  ProductReviewDao productReviewDao, PlatformTransactionManager transactionManager) {
        this(dao, productDao, memberDao, productReviewDao, Clock.systemDefaultZone(),
                new TransactionTemplate(transactionManager));
    }

    /** 테스트 시계·트랜잭션 주입용(package-private) — ProductWishService와 동일 시임 패턴. */
    ProductCompareService(ProductCompareDao dao, ProductDao productDao, MemberDao memberDao,
                           ProductReviewDao productReviewDao, Clock clock,
                           TransactionOperations transactionOperations) {
        this.dao = dao;
        this.productDao = productDao;
        this.memberDao = memberDao;
        this.productReviewDao = productReviewDao;
        this.clock = clock;
        this.transactionOperations = transactionOperations;
    }

    /**
     * 담기 — sku 공백(400) → 상품 존재(404, FK 위반이 500으로 새는 것을 사전 차단) → 상한부 {@code
     * INSERT IGNORE}(신규/재담기/상한초과 구분 없이 단일 시도) → 0행이면 원인 재조회
     * (existsCompare → memberDao 순). 상품 조회+INSERT(+0행 시 재조회) 전부 한 트랜잭션으로 묶는다
     * (STORY "데이터" 절) — {@link #addWithinTransaction}이 그 트랜잭션 본문이고, 이 메서드는
     * {@link DeadlockLoserDataAccessException}(같은 sku 동시 삽입 경합, round2 실측)이 나면 새
     * 트랜잭션으로 재시도한다.
     */
    public void add(String memberId, String sku) {
        requireSku(sku);
        addWithRetry(memberId, sku, MAX_DEADLOCK_ATTEMPTS);
    }

    private void addWithRetry(String memberId, String sku, int remainingAttempts) {
        try {
            transactionOperations.executeWithoutResult(status -> addWithinTransaction(memberId, sku));
        } catch (DeadlockLoserDataAccessException deadlock) {
            if (remainingAttempts <= 1) {
                throw deadlock;
            }
            addWithRetry(memberId, sku, remainingAttempts - 1);
        }
    }

    private void addWithinTransaction(String memberId, String sku) {
        if (productDao.selectBySku(sku) == null) {
            throw productNotFound();
        }
        int affected = dao.insertCompareIfUnderLimit(memberId, sku, LocalDateTime.now(clock));
        if (affected == 0) {
            if (dao.existsCompare(memberId, sku)) {
                return;
            }
            if (memberDao.selectById(memberId) == null) {
                throw memberNotFound();
            }
            throw limitExceeded();
        }
    }

    /**
     * 빼기 — 단일 DML 한 문장이라 명시적 트랜잭션 불필요(STORY "데이터" 절). 존재 여부·상품
     * 존재 여부 모두 확인하지 않는다 — 항상 성공.
     */
    public void remove(String memberId, String sku) {
        dao.deleteCompare(memberId, sku);
    }

    /**
     * 목록 — 조회 전용, 트랜잭션 없음. 최대 4건이라 페이지네이션 없음(page/size 필드를 응답에
     * 넣지 않는다). 각 행에 {@code productReviewDao.selectRatingSummary(sku)}를 호출해 {@code
     * avgRating}/{@code reviewCount}를 병합한다(최대 4회 — 새 집계 SQL을 작성하지 않고 기존
     * 단건 쿼리를 재사용, STORY "데이터" 절 — 집계 쿼리 중복 금지 원칙 유지).
     */
    public Map<String, Object> list(String memberId) {
        List<ProductCompareView> views = dao.selectCompareList(memberId);

        List<Map<String, Object>> items = views.stream().map(v -> {
            ProductReviewSummary summary = productReviewDao.selectRatingSummary(v.getSku());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sku", v.getSku());
            item.put("productName", v.getProductName());
            item.put("imageUrl", v.getImageUrl());
            item.put("price", v.getPrice());
            item.put("listPrice", v.getListPrice());
            item.put("avgRating", summary.getAvgRating());
            item.put("reviewCount", summary.getReviewCount());
            item.put("stockQty", v.getStockQty());
            item.put("soldOut", v.isSoldOut());
            item.put("comparedAt", v.getComparedAt());
            return item;
        }).toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        return body;
    }

    private static void requireSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw validationError("sku", "sku를 입력하세요");
        }
    }

    private static ProductCompareApiException validationError(String field, String message) {
        return new ProductCompareApiException(HttpStatus.BAD_REQUEST, CODE_VALIDATION, field + ": " + message);
    }

    private static ProductCompareApiException productNotFound() {
        return new ProductCompareApiException(HttpStatus.NOT_FOUND, CODE_PRODUCT_NOT_FOUND, "상품을 찾을 수 없습니다");
    }

    private static ProductCompareApiException memberNotFound() {
        return new ProductCompareApiException(HttpStatus.NOT_FOUND, CODE_MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다");
    }

    private static ProductCompareApiException limitExceeded() {
        return new ProductCompareApiException(HttpStatus.CONFLICT, CODE_LIMIT_EXCEEDED, LIMIT_MESSAGE);
    }
}
