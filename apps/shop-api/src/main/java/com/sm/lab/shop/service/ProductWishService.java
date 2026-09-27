package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.ProductWishDao;
import com.sm.lab.shop.domain.ProductWishView;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 상품 찜 등록/해제/목록(SR-356 #2, INF-ORD-040) — register/unregister/list 3개 메서드. STORY
 * "구현 계획" + "사람 확인 코멘트" 절, 사람 확정.
 *
 * <p><b>등록은 read-then-write가 아니라 단일 원자 INSERT IGNORE</b>(사례집 SR-231 r2/SR-298 #1·#2와
 * 동일 계열 — 카운터가 아니라 존재 판정이지만 같은 이유로 읽고 비교한 뒤 쓰지 않는다). 동시에 두
 * 번 찜 요청이 와도 PK(member_id, sku) 충돌을 DB가 흡수해 500이 나지 않는다({@code
 * CartDao#upsertMergeQty}와 동일 원리) — "다시 찜하면 멱등 200" 요구를 코드 경로 자체를 하나로
 * 만들어 지킨다(신규/재등록 분기가 없다).
 *
 * <p><b>round2(QA CONCERNS 권고 1, 사람 확정) — 0행일 때만 원인 재조회</b>. MariaDB {@code IGNORE}는
 * PK 충돌뿐 아니라 FK 위반(예: 존재하지 않는 {@code member_id})도 경고로 격하시켜 조용히 0행으로
 * 끝낸다. admin 키는 {@code memberId}를 임의로 넘길 수 있어(evaluateMemberScope를 타지 않음) 이
 * 경로로 실재하지 않는 회원이 들어오면 "저장 안 됐는데 200"이 나갈 수 있었다. 원자 INSERT IGNORE는
 * 그대로 두고, 반환 영향행수가 0일 때만 {@code memberDao.selectById}로 원인을 가른다 — 회원이
 * 없으면 404({@code WSH-4042}), 있으면(=이미 찜한 상품) 기존대로 멱등 200. "항상 먼저 읽고 판단"이
 * 아니라 "단일 원자문 시도 후 0행일 때만" 재조회하므로 read-then-write 경쟁을 다시 만들지 않는다
 * (CLAUDE.md 카운터 원칙 후반부 — "0행일 때만 원인 재조회로 오류 코드를 가른다").
 *
 * <p><b>해제는 상품 존재조차 확인하지 않는다</b> — {@code DELETE}는 "이 회원의 찜 행"만 다루는
 * 연산이라 상품 존재 판정 자체가 무의미하다(STORY "데이터" 절). 존재/부재 무관 항상 성공.
 *
 * <p><b>self-invocation 함정 회피</b> — register/unregister/list 각각 하나의 직선 메서드다. 같은
 * 빈 내부에서 다른 {@code @Transactional} 메서드를 재호출하지 않는다(STORY "프레임워크 실행 모델
 * 함정" 절).
 */
@Service
public class ProductWishService {

    private static final String CODE_VALIDATION = "WSH-4001";
    private static final String CODE_PRODUCT_NOT_FOUND = "WSH-4041";
    private static final String CODE_MEMBER_NOT_FOUND = "WSH-4042";

    private static final int PAGE_SIZE = 20;

    private final ProductWishDao dao;
    private final ProductDao productDao;
    private final MemberDao memberDao;
    private final Clock clock;

    @Autowired
    public ProductWishService(ProductWishDao dao, ProductDao productDao, MemberDao memberDao) {
        this(dao, productDao, memberDao, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — MemberAddressService/ProductReviewService와 동일 시임 패턴. */
    ProductWishService(ProductWishDao dao, ProductDao productDao, MemberDao memberDao, Clock clock) {
        this.dao = dao;
        this.productDao = productDao;
        this.memberDao = memberDao;
        this.clock = clock;
    }

    /**
     * 등록 — sku 공백(400) → 상품 존재(404, FK 위반이 500으로 새는 것을 사전 차단) → {@code
     * INSERT IGNORE}(신규/재등록 구분 없이 단일 시도). 상품 조회+INSERT(+0행 시 재조회) 전부
     * 트랜잭션으로 묶는다(STORY "데이터" 절).
     *
     * <p>round2 — INSERT IGNORE의 영향행수가 0이면(회원·상품 존재가 아니면 절대 0이 될 수 없는
     * 값이 아니라 "이미 찜함" 또는 "member_id FK 위반이 조용히 흡수됨" 둘 중 하나) 그때만
     * {@code memberDao.selectById}로 원인을 가른다 — 회원이 없으면 404({@code WSH-4042}), 있으면
     * 이미 찜한 상태이므로 예외 없이 정상 반환(컨트롤러가 항상 같은 200 응답으로 매핑).
     */
    @Transactional
    public void register(String memberId, String sku) {
        requireSku(sku);
        if (productDao.selectBySku(sku) == null) {
            throw productNotFound();
        }
        int affected = dao.insertWish(memberId, sku, LocalDateTime.now(clock));
        if (affected == 0 && memberDao.selectById(memberId) == null) {
            throw memberNotFound();
        }
    }

    /**
     * 해제 — 단일 DML 한 문장이라 명시적 트랜잭션 불필요(STORY "데이터" 절). 존재 여부·상품
     * 존재 여부 모두 확인하지 않는다 — 항상 성공.
     */
    public void unregister(String memberId, String sku) {
        dao.deleteWish(memberId, sku);
    }

    /**
     * 목록 — page 파싱({@code ProductReviewService.parsePage} 동일 로직, 1 미만·비숫자 400) →
     * 조회(조회 전용, 트랜잭션 없음). {@code offset}은 {@code (long)} 캐스팅을 곱셈 앞에 둔다
     * (사례집 SR-351 #1 round1→round2 — int 곱셈 오버플로로 음수 OFFSET이 되는 함정).
     */
    public Map<String, Object> list(String memberId, String pageParam) {
        int page = parsePage(pageParam);
        long offset = (long) (page - 1) * PAGE_SIZE;
        List<ProductWishView> views = dao.selectWishList(memberId, offset, PAGE_SIZE);

        List<Map<String, Object>> items = views.stream().map(v -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sku", v.getSku());
            item.put("productName", v.getProductName());
            item.put("price", v.getPrice());
            item.put("imageUrl", v.getImageUrl());
            item.put("soldOut", v.isSoldOut());
            item.put("wishedAt", v.getWishedAt());
            return item;
        }).toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", items);
        body.put("page", page);
        body.put("size", PAGE_SIZE);
        return body;
    }

    private static void requireSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw validationError("sku", "sku를 입력하세요");
        }
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

    private static ProductWishApiException validationError(String field, String message) {
        return new ProductWishApiException(HttpStatus.BAD_REQUEST, CODE_VALIDATION, field + ": " + message);
    }

    private static ProductWishApiException productNotFound() {
        return new ProductWishApiException(HttpStatus.NOT_FOUND, CODE_PRODUCT_NOT_FOUND, "상품을 찾을 수 없습니다");
    }

    private static ProductWishApiException memberNotFound() {
        return new ProductWishApiException(HttpStatus.NOT_FOUND, CODE_MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다");
    }
}
