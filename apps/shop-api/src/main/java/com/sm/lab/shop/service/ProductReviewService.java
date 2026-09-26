package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductReviewDao;
import com.sm.lab.shop.domain.ProductReview;
import com.sm.lab.shop.domain.ProductReviewSummary;
import com.sm.lab.shop.domain.ProductReviewView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 상품평 작성/삭제(SR-351(#1), INF-ORD-037/INF-ORD-039) — 구매 자격 판정 → 저장(작성) /
 * 소유권 조건부 UPDATE(삭제). STORY "구현 계획" + "사람 수정" 절, 사람 확정.
 *
 * <p><b>self-invocation 함정 회피</b> — {@link #create}/{@link #delete} 각각을 하나의
 * {@code @Transactional} public 메서드 안에 직선으로 적는다. 같은 빈 내부에서 별도
 * {@code @Transactional} 메서드를 다시 호출하지 않는다(STORY "프레임워크 실행 모델 함정" 절).
 * {@link DuplicateKeyException}은 unchecked라 이 프록시를 그대로 통과해 컨트롤러 어드바이스까지
 * 전파되고 트랜잭션은 자동 롤백된다 — 별도 처리 불필요.
 *
 * <p><b>존재 판정 순서(사례집 SR-231 r5와는 조건이 다름)</b> — 이 FUNC은 "남의 것=403"과
 * "없음=404"를 의도적으로 분리한다(SR 확정 문답이 명시). {@link com.sm.lab.shop.dao.MemberAddressDao#selectOwned}
 * 처럼 존재+소유를 한 SQL로 합치지 않는다 — 이 항목만 예외적으로 분리 유지(STORY "실패 사례집 대조" 절).
 */
@Service
public class ProductReviewService {

    private static final String CODE_VALIDATION = "RVW-4001";
    private static final String CODE_FORBIDDEN = "RVW-4031";
    private static final String CODE_NOT_FOUND = "RVW-4041";
    private static final String CODE_DUPLICATE = "RVW-4091";

    private static final int RATING_MIN = 1;
    private static final int RATING_MAX = 5;
    private static final int CONTENT_MIN_LENGTH = 10;
    private static final int CONTENT_MAX_LENGTH = 1000;

    // SR-351(#2) — 목록 API(INF-ORD-038) 전용 상수. 컨트롤러 파라미터가 아니라 서비스 상수로
    // 고정(STORY "데이터" 절) — 응답에는 size로 그대로 노출한다.
    private static final int PAGE_SIZE = 10;
    private static final String SORT_LATEST = "latest";
    private static final String SORT_RATING_DESC = "ratingDesc";
    private static final String SORT_RATING_ASC = "ratingAsc";
    private static final List<String> VALID_SORTS = List.of(SORT_LATEST, SORT_RATING_DESC, SORT_RATING_ASC);

    private final ProductReviewDao dao;
    private final ProductDao productDao;
    private final Clock clock;

    @Autowired
    public ProductReviewService(ProductReviewDao dao, ProductDao productDao) {
        this(dao, productDao, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — MemberAddressService와 동일 시임 패턴. */
    ProductReviewService(ProductReviewDao dao, ProductDao productDao, Clock clock) {
        this.dao = dao;
        this.productDao = productDao;
        this.clock = clock;
    }

    /**
     * 작성 — 검증(①rating②content③orderNo, 전부 DB 접근 전 400) → 상품 존재(④404) → 주문
     * 존재(⑤404) → 구매 자격(⑥403) → INSERT(⑦, UNIQUE 위반 시 409). 선조회 후 INSERT가 아니라
     * INSERT를 먼저 시도하고 예외를 캐치한다(STORY "데이터" 절 — 애플리케이션 레벨 락 대신 DB
     * 유일성 인덱스로 원자성을 보장).
     */
    @Transactional
    public ProductReview create(String memberId, String sku, ReviewInput input) {
        validateRating(input.rating());
        validateContent(input.content());
        validateOrderNo(input.orderNo());

        if (productDao.selectBySku(sku) == null) {
            throw notFound();
        }
        if (dao.existsOrder(input.orderNo()) == 0) {
            throw notFound();
        }
        if (dao.countEligiblePurchase(input.orderNo(), memberId, sku) == 0) {
            throw forbidden("이 상품을 구매한 회원만 작성할 수 있습니다");
        }

        LocalDateTime now = LocalDateTime.now(clock);
        ProductReview review = new ProductReview();
        review.setOrderNo(input.orderNo());
        review.setSku(sku);
        review.setMemberId(memberId);
        review.setRating(input.rating().intValueExact());
        review.setContent(input.content());
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        try {
            dao.insertReview(review);
        } catch (DuplicateKeyException e) {
            throw duplicate();
        }
        return review;
    }

    /**
     * 삭제 — 단일 조건부 UPDATE로 먼저 판정한다(선조회 후 UPDATE 금지, 사례집 SR-298 #1·#2와
     * 동일 원칙). 0행일 때만 {@link ProductReviewDao#selectById}로 원인을 가른다: 없음/이미
     * 삭제됨/path의 sku와 실제 리뷰의 sku 불일치 → 404(교차 상품 reviewId 추측 방지, 이 판정이
     * 소유자 불일치보다 우선), 그 외(review_id·sku는 맞지만 member_id가 다름) → 403.
     */
    @Transactional
    public void delete(String memberId, String sku, Long reviewId) {
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = dao.softDelete(reviewId, sku, memberId, now);
        if (updated == 1) {
            return;
        }

        ProductReview existing = dao.selectById(reviewId);
        if (existing == null || "Y".equals(existing.getDelYn()) || !sku.equals(existing.getSku())) {
            throw new ProductReviewApiException(HttpStatus.NOT_FOUND, CODE_NOT_FOUND, "상품평을 찾을 수 없습니다");
        }
        throw forbidden("이 상품평을 삭제할 권한이 없습니다");
    }

    /**
     * 공개 목록(SR-351(#2), INF-ORD-038) — 무인증(비로그인 포함 전원, memberId 파라미터 없음).
     * 순서: sort 화이트리스트(400) → page 파싱·범위(400) → 상품 존재(404) → 목록·집계 조회
     * (순서 무관, 트랜잭션 없음 — STORY "순서·보안" 절). {@code page}는 컨트롤러에서 String으로
     * 받아 여기서 직접 파싱한다 — Spring 기본 바인딩 실패로 떨어지면 이 컨트롤러의 {code,message}
     * 계약과 다른 바디가 나간다(STORY "순서·보안" 2 근거).
     */
    public Map<String, Object> list(String sku, String sortParam, String pageParam) {
        String sort = validateSort(sortParam);
        int page = parsePage(pageParam);

        if (productDao.selectBySku(sku) == null) {
            throw notFoundProduct();
        }

        // page가 Integer.MAX_VALUE에 가까우면 int 곱셈에서 오버플로로 음수 OFFSET이 될 수 있다
        // (사례집 SR-351 #1 round1→round2와 동일 원칙) — cast를 곱셈보다 먼저 한다.
        long offset = (long) (page - 1) * PAGE_SIZE;
        List<ProductReviewView> views = dao.selectReviews(sku, sort, offset, PAGE_SIZE);
        ProductReviewSummary summary = dao.selectRatingSummary(sku);

        List<Map<String, Object>> items = views.stream().map(v -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reviewId", v.getReviewId());
            item.put("rating", v.getRating());
            item.put("content", v.getContent());
            item.put("authorName", maskAuthorName(v.getMemberName()));
            item.put("createdAt", v.getCreatedAt());
            return item;
        }).toList();

        Map<String, Integer> distribution = new LinkedHashMap<>();
        distribution.put("1", summary.getRating1Count());
        distribution.put("2", summary.getRating2Count());
        distribution.put("3", summary.getRating3Count());
        distribution.put("4", summary.getRating4Count());
        distribution.put("5", summary.getRating5Count());

        // Map.of는 null 값(avgRating이 0건일 때 null)에서 NullPointerException을 던진다 —
        // LinkedHashMap으로 조립한다(STORY "데이터" 절).
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalCount", summary.getReviewCount());
        body.put("page", page);
        body.put("size", PAGE_SIZE);
        body.put("avgRating", summary.getAvgRating());
        body.put("reviewCount", summary.getReviewCount());
        body.put("distribution", distribution);
        body.put("items", items);
        return body;
    }

    private static String validateSort(String sort) {
        if (sort == null || !VALID_SORTS.contains(sort)) {
            throw validationError("sort", "정렬 값은 latest/ratingDesc/ratingAsc 중 하나여야 합니다");
        }
        return sort;
    }

    private static int parsePage(String pageParam) {
        int page;
        try {
            page = Integer.parseInt(pageParam);
        } catch (NumberFormatException | NullPointerException e) {
            throw validationError("page", "page는 1 이상의 정수여야 합니다");
        }
        if (page < 1) {
            throw validationError("page", "page는 1 이상의 정수여야 합니다");
        }
        return page;
    }

    /**
     * 확정 문답 pii_role·pii_format 그대로 구현: 1자 → {@code "*"}, 2자 → 첫글자+"*"(끝글자
     * 버림), 3자 이상 → 첫글자+"*"+끝글자(가운데 글자 수와 무관하게 별표는 항상 1개). 권한
     * (관리자·본인·타인) 구분 없이 전원 동일 마스킹.
     */
    private static String maskAuthorName(String name) {
        if (name == null || name.isEmpty()) {
            return "*";
        }
        int length = name.length();
        if (length == 1) {
            return "*";
        }
        if (length == 2) {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*" + name.charAt(length - 1);
    }

    private static void validateRating(BigDecimal rating) {
        if (rating == null) {
            throw validationError("rating", "별점을 입력하세요");
        }
        if (rating.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) != 0) {
            throw validationError("rating", "별점은 정수여야 합니다");
        }
        // 범위 판정은 BigDecimal.compareTo로 끝낸다(임의 정밀도, narrowing 없음) — intValue()로
        // 먼저 좁힌 뒤 비교하면 32비트 오버플로(예: 4294967297 -> intValue()=1)가 검증을 우회한다
        // (round 1 QA CONCERNS 재작업 지시 1). intValueExact()는 이 비교를 통과한 값에만 쓴다.
        if (rating.compareTo(BigDecimal.valueOf(RATING_MIN)) < 0 || rating.compareTo(BigDecimal.valueOf(RATING_MAX)) > 0) {
            throw validationError("rating", "별점은 " + RATING_MIN + "~" + RATING_MAX + " 사이여야 합니다");
        }
    }

    /** content는 trim하지 않는다 — 길이 검증과 저장 모두 원문 그대로(STORY "순서·보안" 절). */
    private static void validateContent(String content) {
        if (content == null || content.length() < CONTENT_MIN_LENGTH || content.length() > CONTENT_MAX_LENGTH) {
            throw validationError("content", "본문은 " + CONTENT_MIN_LENGTH + "~" + CONTENT_MAX_LENGTH + "자여야 합니다");
        }
    }

    private static void validateOrderNo(String orderNo) {
        if (orderNo == null || orderNo.isBlank()) {
            throw validationError("orderNo", "주문번호를 입력하세요");
        }
    }

    private static ProductReviewApiException validationError(String field, String message) {
        return new ProductReviewApiException(HttpStatus.BAD_REQUEST, CODE_VALIDATION, field + ": " + message);
    }

    private static ProductReviewApiException notFound() {
        return new ProductReviewApiException(HttpStatus.NOT_FOUND, CODE_NOT_FOUND, "상품 또는 주문을 찾을 수 없습니다");
    }

    /**
     * 목록 API(INF-ORD-038) 전용 404 — {@link #notFound()}(작성 API, 주문 개념 포함)를 재사용하지
     * 않는다. 목록 조회에는 주문 개념이 없어 그 문구를 그대로 쓰면 오해를 준다(round 1 QA CONCERNS
     * 재작업 지시 3, 코드 {@code RVW-4041}은 계약대로 그대로 유지).
     */
    private static ProductReviewApiException notFoundProduct() {
        return new ProductReviewApiException(HttpStatus.NOT_FOUND, CODE_NOT_FOUND, "상품을 찾을 수 없습니다");
    }

    private static ProductReviewApiException forbidden(String message) {
        return new ProductReviewApiException(HttpStatus.FORBIDDEN, CODE_FORBIDDEN, message);
    }

    private static ProductReviewApiException duplicate() {
        return new ProductReviewApiException(HttpStatus.CONFLICT, CODE_DUPLICATE, "이미 이 주문으로 작성한 상품평이 있습니다");
    }

    /**
     * 이 서비스 전용 요청 전송 record({@code MemberAddressService.AddressInput}과 동일 이유 —
     * 컨트롤러의 요청 record를 그대로 받지 않는다, 역방향 의존 회피). {@code rating}을
     * {@code BigDecimal}로 받는 이유는 "정수인지"를 서비스가 직접 판정해 400 RVW-4001로 응답하기
     * 위함이다 — {@code Integer}로 받으면 "4.5" 같은 입력이 서비스에 닿기 전에 Jackson
     * 역직렬화 단계에서 Spring 기본 400(이 계약과 다른 바디)으로 먼저 끊긴다.
     */
    public record ReviewInput(BigDecimal rating, String content, String orderNo) { }
}
