// linked_func: FUNC-member-001
// spec: docs/00_FUNC/stories/STORY-FUNC-member-001.md
package com.sm.lab.shop.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 회원가입 화면(Thymeleaf, SR-231 · UIS-MBR-001) — 모바일 우선 단일 컬럼, 3단계 마법사
 * (STEP1 가입수단 입력+약관 동의, STEP2 인증코드 확인, STEP3 비밀번호·이름·마케팅 동의).
 *
 * <p>이 컨트롤러는 정적 뼈대만 렌더한다 — 단계 전환·필드 즉시 검증·API 호출은 전부 클라이언트
 * 스크립트({@code templates/member/signup.html} 인라인 JS)가 담당하고, 그 스크립트는 이미
 * 구현된 REST API 2개를 그대로 재호출한다: 인증코드 발송
 * {@code POST /api/members/signup/verification-codes}(INF-MBR-001, FUNC-member-002 소유) ·
 * 가입 요청·코드 검증 {@code POST /api/members/signup}(INF-MBR-002, FUNC-member-003 소유).
 * 이 두 API의 코드·스키마·오류계약은 이 FUNC(FUNC-member-001)에서 절대 건드리지 않는다 —
 * 화면은 그 계약을 있는 그대로 소비하는 클라이언트일 뿐이다.
 *
 * <p><b>변경명세 초안과 다르게 구현한 지점(Dev 판단, 근거는 STORY Dev 기록 참고)</b>: "STEP1에서
 * 이메일 중복을 즉시 검증한다"는 초안 문구는 실제 승인된 API 설계(INF-MBR-002 "존재 오라클
 * 방지" — 코드 검증 전에는 그 target의 가입 여부를 알 방법이 없어야 한다)와 충돌해 그대로
 * 구현할 수 없었다. 이 화면은 대신 최종 가입 제출(STEP3)의 409 응답을 STEP1로 되돌려 표시한다.
 * 비밀번호도 같은 이유로 실제 요청 바디 구성을 따라 STEP3에서만 입력받는다(초안은 STEP1에도
 * 비밀번호 규칙 위반 상태를 나열했으나, 이 화면 흐름에는 STEP1에 비밀번호 입력 필드 자체가 없다).
 *
 * <p>신규 가입자는 아직 API 키가 없어 이 화면 자체도 무인증으로 열어야 한다 —
 * {@link com.sm.lab.shop.web.ApiKeyAuthFilter} 화이트리스트(MEMBER_SIGNUP_SCREEN_PATH)에
 * 등록했다(MEMBER_GRADES_PATH·MEMBER_SIGNUP_VERIFICATION_CODE_PATH와 동일한 이유의 예외).
 * {@code /member/{memberId}}(MemberViewController, 마이페이지)는 경로변수 라우트지만, 리터럴
 * 세그먼트 {@code /member/signup}이 등록 순서와 무관하게 항상 먼저 매칭된다(project-context.md
 * 경로 우선순위 규칙 — {@code /product/list} vs {@code /product/{sku}}와 동일 패턴).
 *
 * <p>이 화면은 shop-api Thymeleaf 서버렌더 화면이다 — shop-web(React+Storybook)은 order
 * 도메인 전용 별도 SPA(주문 목록/상세)라 member 도메인 화면을 이 FUNC에서 새로 얹지 않았다.
 * 그래서 "화면 부품 스토리" 관례(story-per-component 규칙, {@code modules/shop-web/src/**\/*.tsx}
 * 한정)는 이 파일에 적용되지 않는다 — §5 표시 조건(9개 상태)은 이 템플릿의
 * {@code data-screen-state} 속성으로 식별 가능하게만 표시해 두었다(캡처는 QA/E2E 단계 몫).
 */
@Controller
public class MemberSignupViewController {

    /** 회원가입 화면 진입점 — 별도 모델 데이터 없음(전부 클라이언트 스크립트가 API로 채운다). */
    @GetMapping("/member/signup")
    public String signupForm() {
        return "member/signup";
    }
}
