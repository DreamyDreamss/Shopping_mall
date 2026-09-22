// linked_func: FUNC-order-017
// spec: docs/00_FUNC/stories/STORY-FUNC-order-017.md
package com.sm.lab.shop.domain;

import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * 회원 등급 코드 정본(SR-217) — 등급 코드·이름·할인율(%)을 이 enum 한 곳에서만 정의한다.
 * SR-217 요구: "등급별 할인율이 코드에 흩어져 있어 화면마다 다른 값을 보여 준다"를 해소하기 위한
 * 단일 출처(single source of truth) — 새 등급이 필요하면 이 enum에 상수를 추가하고, 다른 클래스에
 * 등급·할인율을 별도로 재정의하지 않는다(중복 정의 금지, SR-217 확정 문답).
 * {@link Member#getGrade()}가 저장하는 코드값(BRONZE/SILVER/GOLD/VIP, 도메인 용어집 정본)과
 * 1:1 대응한다.
 *
 * <p>{@code @JsonFormat(shape = OBJECT)}로 이 enum을 REST 응답에 직접 반환하면(예:
 * {@link com.sm.lab.shop.controller.MemberController#grades()}) 기본 enum 직렬화(이름 문자열)
 * 대신 code/name/discountRate 3필드 객체로 직렬화된다 — 응답 표현을 위한 별도 DTO 클래스를
 * 만들지 않고 이 enum 하나로 정의·직렬화를 겸한다.
 *
 * <p><b>할인율 수치는 비즈룰 스펙 미상 — 잠정값</b>(SR-217 AC: "비즈룰 스펙 미상 — 보강 필요").
 * 코드 어디에도 등급별 할인율의 기존 정의가 없어(RECON 실측 — grep 결과 0건) 사업팀 확정치가
 * 아닌 잠정 스텝값을 넣었다. 실제 운영값이 정해지면 이 enum의 discountRate 인자만 바꾸면 된다
 * (호출부 변경 불필요 — 단일 출처 설계의 목적).
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MemberGrade {
    BRONZE("브론즈", 0),
    SILVER("실버", 3),
    GOLD("골드", 5),
    VIP("VIP", 10);

    private final String gradeName;
    private final int discountRate;

    MemberGrade(String gradeName, int discountRate) {
        this.gradeName = gradeName;
        this.discountRate = discountRate;
    }

    /** 등급 코드 — {@link Member#getGrade()}와 동일한 값(BRONZE/SILVER/GOLD/VIP). */
    public String getCode() {
        return name();
    }

    /** 등급 이름(한글 표시명). */
    public String getName() {
        return gradeName;
    }

    /** 할인율(%) — 정수(예: 5는 5%). */
    public int getDiscountRate() {
        return discountRate;
    }
}
