// linked_func: FUNC-order-009, FUNC-order-010
// spec: docs/00_FUNC/stories/STORY-FUNC-order-009.md, docs/00_FUNC/stories/STORY-FUNC-order-010.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.service.CartService;
import com.sm.lab.shop.service.MemberService;
import com.sm.lab.shop.service.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** 상품 목록·상세 화면(Thymeleaf) — SR-201(카탈로그 신설) + SR-202(상세 담기 폼, reopen r6). */
@Controller
public class ProductViewController {
    private static final Logger log = LoggerFactory.getLogger(ProductViewController.class);

    private final ProductService productService;
    private final MemberService memberService;
    private final CartService cartService;

    public ProductViewController(ProductService productService, MemberService memberService,
                                  CartService cartService) {
        this.productService = productService;
        this.memberService = memberService;
        this.cartService = cartService;
    }

    /**
     * 상품 목록 화면 — 상품명 키워드 검색(부분일치는 ProductService에 위임) + 정렬 셀렉트
     * (기본순 기본·가격 낮은순·가격 높은순, SR-214). "기본순"(sort=latest)은 ORDER BY p.sku ASC(기존
     * 등록순)이다 — PRODUCTS에 등록일시 컬럼이 없어 참 최신순은 구현하지 않는다(라벨을 실제 동작에
     * 맞춤, 재작업 지시 SR-214 QA r5). sort는 {@link ProductService#normalizeSort}로
     * 정규화한다 — 값이 화이트리스트 밖이면(오탈자·조작된 URL) 오류 화면 대신 조용히 "latest"로
     * 되돌리고(SR-214 확정요건), 모델의 sort도 정규화된 값을 써서 셀렉트가 정상 옵션을 가리키게 한다.
     * 선택값은 이 GET 폼(#searchForm)의 쿼리 파라미터로 유지되어 새로고침·뒤로가기에도 남는다.
     * linked_func: FUNC-order-009 (SR-201, SR-214)
     */
    @GetMapping("/product/list")
    public String productList(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) String sort, Model model) {
        String normalizedSort = ProductService.normalizeSort(sort);
        model.addAttribute("products", productService.listSorted(keyword, normalizedSort));
        model.addAttribute("keyword", keyword == null ? "" : keyword);
        model.addAttribute("sort", normalizedSort);
        return "product/list";
    }

    /**
     * 상품 상세 화면. 담기 폼(회원 셀렉트 + 수량, SR-202 reopen r6)을 포함한다. "/product/list"는
     * 리터럴 매핑이 우선하므로 이 경로 변수에 가려지지 않는다(Spring MVC는 등록 순서와 무관하게
     * 리터럴 세그먼트를 경로변수보다 우선 매칭 — 009 QA 권고 7). 미존재 SKU(404)는 예외 페이지 대신
     * 안내 문구 + 목록 링크를 보여준다(SR-201 AC5, 스택 트레이스 노출 금지). 404가 아닌 상태는 장애
     * 은폐를 막기 위해 그대로 rethrow한다(r1 QA CONCERNS 권고1).
     * linked_func: FUNC-order-010 (SR-201, SR-202)
     */
    @GetMapping("/product/{sku}")
    public String productDetail(@PathVariable String sku, Model model) {
        loadProduct(sku, model);
        return "product/detail";
    }

    /**
     * 담기 폼 제출 — {@link CartService#addItem}을 직접 호출한다(회원·수량·재고 검증은 서비스
     * 계약을 그대로 재사용하고 컨트롤러에서 중복 구현하지 않는다 — SR-202). 미존재 SKU는 담기를
     * 시도하지 않고 조기 반환한다(r6 QA 권고3). PRG(Post-Redirect-Get) 적용 — 성공/거부 모두
     * {@code redirect:/product/{sku}}로 리다이렉트하고, 성공 문구·거부 사유는
     * {@link RedirectAttributes} 플래시로 넘겨 다음 GET의 모델에 자동 편입시킨다(F5 재제출 시
     * addItem 중복 호출 방지 — r6 QA 권고2). 흡수 대상은 CartService가 던지는 4xx(400 수량<1/
     * 404 회원·상품/409 재고초과·품절)로 한정하고, 5xx는 같은 파일 {@link #loadProduct}와 동일
     * 규약으로 rethrow한다(장애 은폐 방지 — r6 QA 권고1, r1 권고1과 동일 클래스). CartController/
     * CartViewController·cart 화면은 건드리지 않는다.
     * linked_func: FUNC-order-010 (SR-202 reopen r7)
     */
    @PostMapping("/product/{sku}/cart")
    public String addToCart(@PathVariable String sku, @RequestParam String memberId,
                             @RequestParam(defaultValue = "1") int qty, Model model,
                             RedirectAttributes redirectAttributes) {
        Product product = loadProduct(sku, model);
        if (product == null) {
            // 미존재 SKU — addItem 호출 없이 기존 미존재 안내 화면 그대로 반환(r6 QA 권고3)
            return "product/detail";
        }
        try {
            cartService.addItem(memberId, sku, qty);
            redirectAttributes.addFlashAttribute("addToCartSuccess", true);
            redirectAttributes.addFlashAttribute("addedMemberId", memberId);
        } catch (ResponseStatusException e) {
            if (!e.getStatusCode().is4xxClientError()) {
                throw e;
            }
            // CartService가 확정한 4xx 사유를 플래시로 전달 — 재검증 로직 중복 금지
            log.debug("담기 거부(4xx) 흡수 — sku={}, reason={}", sku, e.getReason());
            redirectAttributes.addFlashAttribute("addToCartError", e.getReason());
        }
        return "redirect:/product/" + sku;
    }

    /**
     * 상품 조회(404는 화면단 흡수) + 담기 폼용 회원 목록을 모델에 채운다(상품이 있을 때만).
     * @return 조회된 상품, 미존재(404 흡수)면 {@code null}
     */
    private Product loadProduct(String sku, Model model) {
        Product product = null;
        try {
            product = productService.get(sku);
        } catch (ResponseStatusException e) {
            if (e.getStatusCode() != HttpStatus.NOT_FOUND) {
                throw e;
            }
            // 미존재 SKU — 404만 화면단에서 흡수, product=null로 안내 문구 렌더
            log.debug("상품 상세 조회 404 흡수 — sku={}", sku);
        }
        model.addAttribute("product", product);
        if (product != null) {
            model.addAttribute("members", memberService.list());
        }
        return product;
    }
}
