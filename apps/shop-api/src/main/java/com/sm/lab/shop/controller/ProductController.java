// linked_func: FUNC-order-007
// spec: docs/05_설계서/order/INF/INF-ORD-008.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.Product;
import com.sm.lab.shop.service.ProductService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * 판매중 상품 목록. keyword 미지정 시 기존과 동일(전체 판매중·sku 정렬).
     * keyword 지정 시 product_name 부분일치(LIKE) 검색을 sale_yn='Y'와 AND. (SR-201)
     * inStock=true 지정 시 stock_qty >= 1인 상품만 반환(품절 상품 제외). 미지정/false는 기존과 동일(하위호환). (SR-220)
     */
    @GetMapping
    public List<Product> list(@RequestParam(required = false) String keyword,
                               @RequestParam(required = false) Boolean inStock) {
        return productService.list(keyword, inStock);
    }

    /** 상품 단건 조회. */
    @GetMapping("/{sku}")
    public Product get(@PathVariable String sku) {
        return productService.get(sku);
    }
}
