// linked_func: FUNC-member-011
// spec: docs/00_FUNC/stories/STORY-FUNC-member-011.md
package com.sm.lab.shop.controller;

import com.sm.lab.shop.domain.MemberAddress;
import com.sm.lab.shop.service.MemberAddressApiException;
import com.sm.lab.shop.service.MemberAddressService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 배송지 CRUD API(SR-235, INF-MBR-008) — {@code /api/members/me/addresses}.
 *
 * <p><b>memberId 해석</b> — 이 경로는 URL에 memberId가 없다("me"). {@link
 * com.sm.lab.shop.web.ApiKeyAuthFilter}가 기존 {@code GET /api/orders} SCOPE_TO_SELF 메커니즘을
 * 재사용해 member 스코프 키의 {@code memberId} 파라미터를 호출자 자신으로 강제한다(새 우회 경로
 * 없음, STORY "순서·보안" 절). admin 키로 이 경로를 호출하면 그 강제가 일어나지 않으므로,
 * {@code memberId}가 없으면 이 컨트롤러가 400 {@code MBR-4200}으로 명시적으로 막는다(사람 확정 —
 * "관리자가 특정 회원 대신 /me를 호출"하는 시나리오는 정의하지 않는다).
 *
 * <p>요청/응답 DTO는 컨트롤러 내부 {@code record}로 둔다({@code CartController} 관례). 목록은
 * {@code {items: [...]}} 봉투(0건이면 빈 배열, 404 아님), 단건 응답은 배송지 객체 그대로(봉투
 * 없음).
 */
@RestController
@RequestMapping("/api/members/me/addresses")
public class MemberAddressController {

    private final MemberAddressService memberAddressService;

    public MemberAddressController(MemberAddressService memberAddressService) {
        this.memberAddressService = memberAddressService;
    }

    /** 목록 — 200, 0건이면 {@code {items: []}}(404 아님, 사람 확정 (4)). */
    @GetMapping
    public Map<String, Object> list(@RequestParam(required = false) String memberId) {
        requireMemberId(memberId);
        return Map.of("items", memberAddressService.list(memberId));
    }

    /** 등록 — 201 + 생성된 배송지 본문. 400(필수값)/409(최대 10개). */
    @PostMapping
    public ResponseEntity<MemberAddress> register(@RequestParam(required = false) String memberId,
                                                   @RequestBody AddressRequest req) {
        requireMemberId(memberId);
        MemberAddress created = memberAddressService.register(memberId, toInput(req));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** 수정 — 200 + 본문. 400(필수값)/404(없음·남의 것 동일 코드). */
    @PutMapping("/{addressId}")
    public MemberAddress update(@RequestParam(required = false) String memberId,
                                 @PathVariable Long addressId, @RequestBody AddressRequest req) {
        requireMemberId(memberId);
        return memberAddressService.update(memberId, addressId, toInput(req));
    }

    /** 삭제 — 204(본문 없음). 404(없음·남의 것 동일 코드). */
    @DeleteMapping("/{addressId}")
    public ResponseEntity<Void> delete(@RequestParam(required = false) String memberId,
                                        @PathVariable Long addressId) {
        requireMemberId(memberId);
        memberAddressService.delete(memberId, addressId);
        return ResponseEntity.noContent().build();
    }

    /** 기본 설정 — 200 + 본문(이미 기본이면 no-op 200). 404(없음·남의 것 동일 코드). */
    @PutMapping("/{addressId}/default")
    public MemberAddress setDefault(@RequestParam(required = false) String memberId,
                                     @PathVariable Long addressId) {
        requireMemberId(memberId);
        return memberAddressService.setDefault(memberId, addressId);
    }

    /**
     * admin 키로 {@code /me}를 호출하면 {@code ApiKeyAuthFilter}의 SCOPE_TO_SELF 강제가 일어나지
     * 않는다(그 스코프는 evaluateMemberScope 자체를 타지 않음). 이 SR은 "관리자가 특정 회원
     * 대신 /me를 호출"하는 시나리오를 정의하지 않으므로, 정의되지 않은 동작을 조용히 통과시키지
     * 않고 명시적으로 400으로 막는다(사람 확정).
     */
    private void requireMemberId(String memberId) {
        if (memberId == null || memberId.isBlank()) {
            throw new MemberAddressApiException(HttpStatus.BAD_REQUEST, "MBR-4200", "memberId가 필요합니다");
        }
    }

    /**
     * {@code CartService.addItem(req.memberId(), req.sku(), req.qty())} 관례와 동일한 이유로
     * 이 record를 서비스에 그대로 넘기지 않는다(서비스가 controller 패키지를 import하지 않게
     * — {@code MemberAddressService} javadoc "Dev 기록" 참고). 필드가 8개라 그대로 풀지 않고
     * 서비스 전용 {@link MemberAddressService.AddressInput}으로 변환한다.
     */
    private static MemberAddressService.AddressInput toInput(AddressRequest req) {
        return new MemberAddressService.AddressInput(req.recipient(), req.phone(), req.zipcode(),
                req.roadAddress(), req.detailAddress(), req.entranceMethod(), req.deliveryMemo(), req.isDefault());
    }

    /** {@code isDefault}는 등록 요청에서만 의미가 있다(선택, 없으면 false 취급) — 수정 요청도 같은 셰이프를 공유한다. */
    public record AddressRequest(String recipient, String phone, String zipcode, String roadAddress,
                                  String detailAddress, String entranceMethod, String deliveryMemo,
                                  Boolean isDefault) { }
}
