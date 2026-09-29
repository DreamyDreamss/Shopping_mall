package com.sm.lab.shop.service;

import com.sm.lab.shop.dao.MemberDao;
import com.sm.lab.shop.dao.ProductDao;
import com.sm.lab.shop.dao.RestockAlertDao;
import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.domain.RestockAlertView;
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
 * 재입고 알림 신청 등록(SR-364 #1, INF-ORD-048) — {@link #register} 한 메서드. STORY "구현 계획"
 * 절, 사람 확정.
 *
 * <p><b>등록은 read-then-write가 아니라 단일 원자 INSERT IGNORE</b>(사례집 SR-231 r2/SR-298 #1·#2와
 * 동일 계열 — 카운터가 아니라 존재 판정이지만 같은 이유로 읽고 비교한 뒤 쓰지 않는다). 동시에 두
 * 번 신청 요청이 와도 PK(member_id, sku) 충돌을 DB가 흡수해 500이 나지 않는다({@code
 * ProductWishService#register}와 동일 원리) — "이미 신청한 상품 재신청은 멱등 200" 요구를 코드
 * 경로 자체를 하나로 만들어 지킨다(신규/재신청 분기가 없다).
 *
 * <p><b>0행일 때만 원인 재조회</b>. MariaDB {@code IGNORE}는 PK 충돌뿐 아니라 FK 위반(예: 존재하지
 * 않는 {@code member_id})도 경고로 격하시켜 조용히 0행으로 끝낸다. admin 키는 {@code memberId}를
 * 임의로 넘길 수 있어(evaluateMemberScope를 타지 않음) 이 경로로 실재하지 않는 회원이 들어오면
 * "저장 안 됐는데 200"이 나갈 수 있었다. 원자 INSERT IGNORE는 그대로 두고, 반환 영향행수가 0일
 * 때만 {@code memberDao.selectById}로 원인을 가른다 — 회원이 없으면 404({@code RSA-4042}), 있으면
 * (=이미 신청한 상품) 예외 없이 정상 반환(CLAUDE.md 카운터 원칙 후반부 — "0행일 때만 원인
 * 재조회로 오류 코드를 가른다").
 *
 * <p><b>self-invocation 함정 회피</b> — {@link #register} 하나의 직선 메서드다. 같은 빈 내부에서
 * 다른 {@code @Transactional} 메서드를 재호출하지 않는다(STORY "프레임워크 실행 모델 함정" 절).
 */
@Service
public class RestockAlertService {

    private static final String CODE_VALIDATION = "RSA-4001";
    private static final String CODE_PRODUCT_NOT_FOUND = "PRODUCT_NOT_FOUND";
    private static final String CODE_NOT_SOLD_OUT = "NOT_SOLD_OUT";
    private static final String CODE_MEMBER_NOT_FOUND = "RSA-4042";

    private final RestockAlertDao dao;
    private final ProductDao productDao;
    private final MemberDao memberDao;
    private final Clock clock;

    @Autowired
    public RestockAlertService(RestockAlertDao dao, ProductDao productDao, MemberDao memberDao) {
        this(dao, productDao, memberDao, Clock.systemDefaultZone());
    }

    /** 테스트 시계 주입용(package-private) — ProductWishService와 동일 시임 패턴. */
    RestockAlertService(RestockAlertDao dao, ProductDao productDao, MemberDao memberDao, Clock clock) {
        this.dao = dao;
        this.productDao = productDao;
        this.memberDao = memberDao;
        this.clock = clock;
    }

    /**
     * 등록 — sku 공백(400) → 상품 존재(404) → 품절 여부(409, 아직 품절 아니면) → {@code INSERT
     * IGNORE}(신규/재신청 구분 없이 단일 시도). 상품 조회+INSERT(+0행 시 재조회) 전부 트랜잭션으로
     * 묶는다(STORY "데이터" 절).
     *
     * <p>INSERT IGNORE의 영향행수가 0이면("이미 신청함" 또는 "member_id FK 위반이 조용히 흡수됨"
     * 둘 중 하나) 그때만 {@code memberDao.selectById}로 원인을 가른다 — 회원이 없으면 404({@code
     * RSA-4042}), 있으면 이미 신청한 상태이므로 예외 없이 정상 반환(컨트롤러가 항상 같은 200
     * 응답으로 매핑).
     */
    @Transactional
    public void register(String memberId, String sku) {
        requireSku(sku);
        Product product = productDao.selectBySku(sku);
        if (product == null) {
            throw productNotFound();
        }
        if (product.getStockQty() > 0) {
            throw notSoldOut();
        }
        int affected = dao.insertAlert(memberId, sku, LocalDateTime.now(clock));
        if (affected == 0 && memberDao.selectById(memberId) == null) {
            throw memberNotFound();
        }
    }

    /**
     * 목록 — 이 회원이 신청한 재입고 알림을 최근 신청 순({@code created_at DESC})으로 반환한다
     * (SR-364 #2, INF-ORD-049). 조회 전용(트랜잭션 없음, {@code ProductWishService.list}·{@code
     * ProductViewHistoryService.list}와 동일 관례) — memberId는 이미 SCOPE_TO_SELF로 강제됨. 0건이면
     * 빈 배열({@code ProductRecentlyViewedController.list}와 동일 — 빈 값이 오류가 아님).
     */
    public Map<String, Object> list(String memberId) {
        List<RestockAlertView> views = dao.selectAlertList(memberId);

        List<Map<String, Object>> items = views.stream().map(v -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("sku", v.getSku());
            item.put("createdAt", v.getCreatedAt());
            return item;
        }).toList();

        return Map.of("items", items);
    }

    private static void requireSku(String sku) {
        if (sku == null || sku.isBlank()) {
            throw validationError("sku", "sku를 입력하세요");
        }
    }

    private static RestockAlertApiException validationError(String field, String message) {
        return new RestockAlertApiException(HttpStatus.BAD_REQUEST, CODE_VALIDATION, field + ": " + message);
    }

    private static RestockAlertApiException productNotFound() {
        return new RestockAlertApiException(HttpStatus.NOT_FOUND, CODE_PRODUCT_NOT_FOUND, "상품을 찾을 수 없습니다");
    }

    private static RestockAlertApiException notSoldOut() {
        return new RestockAlertApiException(HttpStatus.CONFLICT, CODE_NOT_SOLD_OUT, "품절 상품이 아닙니다");
    }

    private static RestockAlertApiException memberNotFound() {
        return new RestockAlertApiException(HttpStatus.NOT_FOUND, CODE_MEMBER_NOT_FOUND, "회원을 찾을 수 없습니다");
    }
}
