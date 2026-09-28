package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductViewHistoryDao;
import com.sm.lab.shop.domain.ProductViewHistoryView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 최근 본 상품 기록/조회 — record/list 2메서드.
 *
 * <p><b>기록은 단일 원자 UPSERT</b>(카운터·존재판정을 read-then-write로 만들지 않는다는 이
 * 코드베이스 관례와 동일 계열) — {@code INSERT ... ON DUPLICATE KEY UPDATE viewed_at =
 * VALUES(viewed_at)}({@code CartDao#upsertMergeQty}와 동일 원리). {@code
 * ProductWishDao#insertWish}의 {@code INSERT IGNORE}와 다르다 — "이미 있으면 viewed_at만 갱신"이
 * 요구라 IGNORE가 아니라 UPSERT를 쓴다.
 *
 * <p><b>회원 존재 방어</b>({@code ProductWishService.register}의 사전/사후 방어 교훈 재사용) —
 * 이 UPSERT는 IGNORE가 아니라 {@code ON DUPLICATE KEY UPDATE}라 FK(member_id) 위반이 조용히
 * 흡수되지 않고 즉시 예외로 던져진다(UPSERT는 "0행 완료" 경로가 없어 사후 재조회 트릭을 못 쓴다).
 * 그래서 UPSERT 이전에 {@code memberDao.selectById}로 회원 존재를 먼저 확인한다 — admin 키가
 * 실재하지 않는 memberId로 대리 호출하는 경우 전용 방어다(정상 회원 키는 {@code
 * ApiKeyAuthFilter} SCOPE_TO_SELF가 이미 실재하는 자기 자신으로 강제하므로 이 분기를 타지 않는다).
 *
 * <p><b>상한 삭제는 기록 요청 트랜잭션 안에서 동기 실행</b>된다(별도 {@code @Scheduled} 배치를
 * 만들지 않는다). {@link #record}가 하나의 직선 {@code @Transactional} 메서드라 같은 빈 내부에서
 * 다른 트랜잭션 메서드를 재호출하는 self-invocation 함정도 없다.
 */
@Service
public class ProductViewHistoryService {

    private static final String CODE_VALIDATION = "RCV-4001";
    private static final String CODE_MEMBER_NOT_FOUND = "RCV-4042";
    private static final String CODE_PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";

    private static final int RECENT_CAP = 20;

    // ApiKeyAuthFilter의 상품 경로 라우팅 정규식(SKU- 접두 + 영숫자·하이픈)과 동일한 sku 형식
    // 정의를 재사용한다 — 이 서비스가 새 정의를 만들지 않는다.
    private static final Pattern SKU_FORMAT = Pattern.compile("^SKU-[A-Za-z0-9-]+$");

    private final ProductViewHistoryDao dao;
    private final ProductDao productDao;
    private final MemberDao memberDao;
    private final Clock clock;

    @Autowired
    public ProductViewHistoryService(ProductViewHistoryDao dao, ProductDao productDao, MemberDao memberDao) {
        this(dao, productDao, memberDao, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — ProductWishService와 동일 시임 패턴. */
    ProductViewHistoryService(ProductViewHistoryDao dao, ProductDao productDao, MemberDao memberDao, Clock clock) {
        this.dao = dao;
        this.productDao = productDao;
        this.memberDao = memberDao;
        this.clock = clock;
    }

    /**
     * 기록 — sku 공백/형식(400) → 회원 존재(404, admin 대리 호출 전용 방어) → 상품 존재(404
     * {@code PRODUCT_NOT_FOUND}) → UPSERT → 상한(20) 초과분 삭제. 전부 한 트랜잭션 — 삭제가
     * 실패하면 UPSERT도 함께 롤백돼 상한을 넘긴 채 남는 것을 방지한다.
     */
    @Transactional
    public void record(String memberId, String sku) {
        requireValidSku(sku);
        if (memberDao.selectById(memberId) == null) {
            throw memberNotFound();
        }
        if (productDao.selectBySku(sku) == null) {
            throw productNotFound();
        }
        dao.recordView(memberId, sku, LocalDateTime.now(clock));
        dao.enforceRecentCap(memberId, RECENT_CAP);
    }

    /** 목록 — 최근 본 순 최대 20개, 조회 전용(트랜잭션 없음). memberId는 이미 SCOPE_TO_SELF로 강제됨. */
    public Map<String, Object> list(String memberId) {
        List<ProductViewHistoryView> views = dao.selectRecentlyViewed(memberId, RECENT_CAP);

        List<Map<String, Object>> items = views.stream().map(v -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sku", v.getSku());
            item.put("productName", v.getProductName());
            item.put("price", v.getPrice());
            item.put("imageUrl", v.getImageUrl());
            item.put("soldOut", v.isSoldOut());
            item.put("viewedAt", v.getViewedAt());
            return item;
        }).toList();

        return Map.of("items", items);
    }

    private static void requireValidSku(String sku) {
        if (sku == null || sku.isBlank() || !SKU_FORMAT.matcher(sku).matches()) {
            throw validationError("sku", "sku 형식이 올바르지 않습니다");
        }
    }

    private static ProductViewHistoryApiException validationError(String field, String message) {
        return new ProductViewHistoryApiException(HttpStatus.BAD_REQUEST, CODE_VALIDATION, field + ": " + message);
    }

    private static ProductViewHistoryApiException memberNotFound() {
        return new ProductViewHistoryApiException(HttpStatus.NOT_FOUND, CODE_MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다");
    }

    private static ProductViewHistoryApiException productNotFound() {
        return new ProductViewHistoryApiException(HttpStatus.NOT_FOUND, CODE_PRODUCT_NOT_FOUND, "상품을 찾을 수 없습니다");
    }
}
