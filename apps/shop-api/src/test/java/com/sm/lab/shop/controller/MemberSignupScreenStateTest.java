// linked_func: FUNC-member-001
// spec: docs/00_FUNC/stories/STORY-FUNC-member-001.md
package com.sm.lab.shop.controller;

import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlButton;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.sm.lab.shop.service.MemberRegistrationApiException;
import com.sm.lab.shop.service.MemberRegistrationService;
import com.sm.lab.shop.service.MemberSignupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.htmlunit.MockMvcWebClientBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * round2 재작업(QA round1 CONCERNS 권고2), round3 재작업(QA round2 CONCERNS 권고1·2·3 —
 * STEP3 체류 중 타이머 미정지·MBR-4091 잠금 잔존 경로·MBR-4093/만료 상태 공유) — signup.html의
 * data-screen-state 10개 상태(§5, round3 — step2-locked 신설로 9→10개, STORY-FUNC-member-001.md
 * 상단 스크립트 주석 "상태 전이표" 참고) 각각을 실제 브라우저형 JS 실행 환경(HtmlUnit)에서
 * 1건씩 단언한다. 이 클래스가 이 프로젝트 최초의 HtmlUnit 사용이다 — {@code MockMvcWebClientBuilder}로
 * WebClient의 모든 요청을 MockMvc(같은 슬라이스에 로드한 FUNC-member-002/003 소유 컨트롤러 +
 * 모킹한 서비스)로 라우팅한다.
 *
 * <p><b>fetch() 폴리필이 필요한 이유</b>: 이 프로젝트가 관리하는 HtmlUnit 버전(2.70.0,
 * spring-boot-dependencies 3.3.5 관리값)의 JS 엔진은 native {@code fetch}가 없다
 * ("fetch is not defined" ReferenceError로 실측 확인). signup.html은 절대 수정하지 않고,
 * 이 테스트에서만 {@code window.fetch}를 XMLHttpRequest 기반으로 주입한다(HtmlUnit은
 * XHR은 지원 — {@link #FETCH_POLYFILL} 참고). 프로덕션 스크립트가 실제로 소비하는 fetch
 * 응답 형태({@code {ok, status, json()}})만 흉내 낸다.
 *
 * <p>FUNC-member-002(MemberSignupService)/FUNC-member-003(MemberRegistrationService) 소유
 * 파일은 이 테스트에서 읽기(모킹) 대상으로만 참조할 뿐 하나도 수정하지 않는다.
 */
@WebMvcTest(controllers = {
        MemberSignupViewController.class,
        MemberSignupController.class,
        MemberRegistrationController.class
})
class MemberSignupScreenStateTest {

    private static final String FETCH_POLYFILL =
            "window.fetch = function(url, options) {" +
            "  return new Promise(function(resolve, reject) {" +
            "    try {" +
            "      var xhr = new XMLHttpRequest();" +
            "      xhr.open((options && options.method) || 'GET', url, true);" +
            "      if (options && options.headers) {" +
            "        for (var k in options.headers) { xhr.setRequestHeader(k, options.headers[k]); }" +
            "      }" +
            "      xhr.onreadystatechange = function() {" +
            "        if (xhr.readyState === 4) {" +
            "          resolve({" +
            "            ok: xhr.status >= 200 && xhr.status < 300," +
            "            status: xhr.status," +
            "            json: function() { return Promise.resolve(JSON.parse(xhr.responseText || '{}')); }" +
            "          });" +
            "        }" +
            "      };" +
            "      xhr.onerror = function() { reject(new Error('network')); };" +
            "      xhr.send((options && options.body) || null);" +
            "    } catch (e) { reject(e); }" +
            "  });" +
            "};";

    private static final String VALID_EMAIL = "user@example.com";
    private static final String VALID_PASSWORD = "abcd1234";
    private static final String VALID_NAME = "홍길동";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MemberSignupService memberSignupService;

    @MockBean
    private MemberRegistrationService memberRegistrationService;

    private static class Page {
        final WebClient webClient;
        final HtmlPage page;

        Page(WebClient webClient, HtmlPage page) {
            this.webClient = webClient;
            this.page = page;
        }

        String state() {
            return page.getBody().getAttribute("data-screen-state");
        }

        <T extends com.gargoylesoftware.htmlunit.html.HtmlElement> T byId(String id) {
            return page.getHtmlElementById(id);
        }

        void waitJs() throws Exception {
            // STEP2 진입 후에는 setInterval(카운트다운)이 계속 살아있어 background-job 카운트가
            // 0으로 떨어지지 않으므로 waitForBackgroundJavaScript는 매번 지정 시간을 다 쓴다
            // (fetch 폴리필의 XHR→MockMvc 응답 자체는 사실상 즉시 온다) — 전체 스위트 시간을
            // 줄이기 위해 짧게 잡는다. 타이머 정지 여부를 실제로 검증하는 곳(state3 테스트)은
            // 별도로 더 긴 시간을 기다린다.
            webClient.waitForBackgroundJavaScript(400);
        }
    }

    private Page openSignupPage() throws Exception {
        WebClient webClient = MockMvcWebClientBuilder.mockMvcSetup(mockMvc).build();
        HtmlPage htmlPage = webClient.getPage("http://localhost/member/signup");
        htmlPage.executeJavaScript(FETCH_POLYFILL);
        return new Page(webClient, htmlPage);
    }

    /** STEP1을 채우고 인증코드 발송에 성공해 STEP2(step2-initial)까지 이동한다(유효시간 300초). */
    private Page toStep2() throws Exception {
        return toStep2(300);
    }

    /**
     * STEP1을 채우고 인증코드 발송에 성공해 STEP2(step2-initial)까지 이동한다.
     * round3 재작업(§5-6 step2-code-expired 단독 테스트용) — 유효시간을 짧게 지정해 실제
     * 카운트다운 만료를 실측할 수 있게 한다.
     */
    private Page toStep2(int expiresInSeconds) throws Exception {
        Page p = openSignupPage();
        when(memberSignupService.requestVerificationCode(VALID_EMAIL))
                .thenReturn(new MemberSignupService.VerificationCodeResult("EMAIL", VALID_EMAIL, expiresInSeconds));

        HtmlInput target = p.byId("target");
        target.type(VALID_EMAIL);
        HtmlCheckBoxInput terms = p.byId("agreeTerms");
        terms.click();
        HtmlCheckBoxInput privacy = p.byId("agreePrivacy");
        privacy.click();
        p.waitJs();

        HtmlButton btnRequestCode = p.byId("btnRequestCode");
        btnRequestCode.click();
        p.waitJs();
        return p;
    }

    /** STEP2에서 6자리 코드를 입력하고 "다음"을 눌러 STEP3(step3-input)까지 이동한다. */
    private Page toStep3() throws Exception {
        Page p = toStep2();
        HtmlInput code = p.byId("code");
        code.type("123456");
        p.waitJs();
        HtmlButton btnVerifyNext = p.byId("btnVerifyNext");
        btnVerifyNext.click();
        p.waitJs();
        return p;
    }

    /**
     * round4 재작업 지시(2) 테스트 헬퍼 — STEP3까지 이동해 서버가 지정한 {@code loginUrl}로
     * MBR-4092(중복 이메일)를 응답하게 만들고 STEP1로 복귀시킨다. {@code isSafeRelativePath}가
     * 안전하다고 판정한 경우에만 로그인 링크(&lt;a&gt;)가 렌더되므로, 그 존재 여부로 가드의
     * 판정을 관찰한다(내부 함수를 직접 호출하지 않고 signup.html이 실제로 소비하는 방식 그대로
     * DOM 결과를 단언 — state3 테스트와 같은 접근).
     */
    private Page duplicateErrorWithLoginUrl(String loginUrl) throws Exception {
        Page p = toStep3();
        HtmlInput password = p.byId("password");
        password.type(VALID_PASSWORD);
        HtmlInput name = p.byId("name");
        name.type(VALID_NAME);
        p.waitJs();

        when(memberRegistrationService.signUp(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4092", "이미 가입된 이메일입니다")
                        .withLoginUrl(loginUrl));

        HtmlButton btnSignup = p.byId("btnSignup");
        btnSignup.click();
        p.waitJs();
        return p;
    }

    // linked_tc: TC-FUNC-member-001-03
    // §5-1 STEP1 초기 상태 — 화면 진입 직후 기본값
    @Test
    void state1_step1Initial_onLoad() throws Exception {
        Page p = openSignupPage();
        assertThat(p.state()).isEqualTo("step1-initial");
    }

    // linked_tc: TC-FUNC-member-001-04
    // §5-2 STEP1 약관 미동의 상태 — 진입(형식 유효 + 약관 미동의) + 복귀(약관 모두 동의 시 해제)
    @Test
    void state2_step1TermsUnchecked_entryAndRecovery() throws Exception {
        Page p = openSignupPage();
        HtmlInput target = p.byId("target");
        target.type(VALID_EMAIL);
        p.waitJs();
        assertThat(p.state()).isEqualTo("step1-terms-unchecked");

        HtmlCheckBoxInput terms = p.byId("agreeTerms");
        terms.click();
        HtmlCheckBoxInput privacy = p.byId("agreePrivacy");
        privacy.click();
        p.waitJs();

        // round2 재작업 지시 2 — 약관 체크 후 미동의 상태가 해제(복귀)되는지 확인
        assertThat(p.state()).isEqualTo("step1-initial");
    }

    // linked_tc: TC-FUNC-member-001-05
    // §5-3 STEP1 이메일 중복 오류 상태(재구성: STEP3 제출 409 MBR-4092 → STEP1 복귀) —
    // round2 재작업 지시 3(카운트다운 정지)·4(innerHTML 제거) 회귀도 함께 확인
    @Test
    void state3_step1DuplicateError_stopsCountdownAndUsesDomLink() throws Exception {
        Page p = toStep3();
        HtmlInput password = p.byId("password");
        password.type(VALID_PASSWORD);
        HtmlInput name = p.byId("name");
        name.type(VALID_NAME);
        p.waitJs();

        when(memberRegistrationService.signUp(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4092", "이미 가입된 이메일입니다")
                        .withLoginUrl("/login"));

        HtmlButton btnSignup = p.byId("btnSignup");
        btnSignup.click();
        p.waitJs();

        assertThat(p.state()).isEqualTo("step1-duplicate-error");
        // round2 재작업 지시 4 — innerHTML 문자열 결합이 아니라 실제 <a> 엘리먼트로 조립됐는지 확인
        assertThat(p.page.getAnchors()).hasSize(1);
        assertThat(p.page.getAnchors().get(0).getHrefAttribute()).isEqualTo("/login");
        assertThat(p.page.getAnchors().get(0).asNormalizedText()).isEqualTo("로그인하러 가기");

        // round2 재작업 지시 3 — STEP1로 복귀하면 STEP2 카운트다운이 정지돼 있어야 한다.
        // 정지되지 않았다면 배경 타이머(1초 간격)가 몇 초 뒤 data-screen-state를
        // step2-code-expired로 덮어써 이 상태가 깨진다(round1 QA CONCERNS 권고3 재현조건).
        p.webClient.waitForBackgroundJavaScript(3000);
        assertThat(p.state()).isEqualTo("step1-duplicate-error");
    }

    // linked_tc: TC-FUNC-member-001-14
    // round4 재작업 지시(2) 단위 테스트 — isSafeRelativePath: 안전한 상대경로(login_url)는
    // 그대로 로그인 링크로 렌더된다.
    @Test
    void isSafeRelativePath_allowsPlainRelativePath() throws Exception {
        Page p = duplicateErrorWithLoginUrl("/mypage/welcome");
        assertThat(p.page.getAnchors()).hasSize(1);
        assertThat(p.page.getAnchors().get(0).getHrefAttribute()).isEqualTo("/mypage/welcome");
    }

    // linked_tc: TC-FUNC-member-001-15
    // round4 재작업 지시(2) 단위 테스트 — isSafeRelativePath: '//evil.com'(스킴 상대 URL,
    // 이중 슬래시)은 거부되어 로그인 링크 자체가 렌더되지 않는다.
    @Test
    void isSafeRelativePath_rejectsDoubleSlash() throws Exception {
        Page p = duplicateErrorWithLoginUrl("//evil.com");
        assertThat(p.page.getAnchors()).isEmpty();
    }

    // linked_tc: TC-FUNC-member-001-16
    // round4 재작업 지시(2) 단위 테스트 — isSafeRelativePath: '/\evil.com'(역슬래시 — 브라우저
    // URL 파서가 특수 스킴에서 '\'를 '/'로 정규화해 '//evil.com'과 동일하게 외부 오리진으로
    // 해석될 수 있다, round3 QA 재게이트 관찰)도 거부되어 로그인 링크가 렌더되지 않는다.
    @Test
    void isSafeRelativePath_rejectsBackslash() throws Exception {
        Page p = duplicateErrorWithLoginUrl("/\\evil.com");
        assertThat(p.page.getAnchors()).isEmpty();
    }

    // linked_tc: TC-FUNC-member-001-06
    // §5-4 STEP2 인증코드 입력 초기 상태
    @Test
    void state4_step2Initial_afterVerificationCodeSent() throws Exception {
        Page p = toStep2();
        assertThat(p.state()).isEqualTo("step2-initial");
    }

    // linked_tc: TC-FUNC-member-001-07
    // §5-5 STEP2 인증코드 오류 상태(STEP3 제출 409 MBR-4091) — round2 재작업 지시 1: 입력을
    // 잠그지 않고 재입력을 허용해야 한다(잠그는 것은 결함, round1 QA FAIL 권고1). round3
    // 재작업(A) — 잠금은 오직 step2-locked에서만 걸리므로 이 상태에서는 절대 잠기지 않는다.
    @Test
    void state5_step2CodeInvalid_allowsReentry() throws Exception {
        Page p = toStep3();
        HtmlInput password = p.byId("password");
        password.type(VALID_PASSWORD);
        HtmlInput name = p.byId("name");
        name.type(VALID_NAME);
        p.waitJs();

        when(memberRegistrationService.signUp(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4091", "인증코드가 올바르지 않습니다."));

        HtmlButton btnSignup = p.byId("btnSignup");
        btnSignup.click();
        p.waitJs();

        assertThat(p.state()).isEqualTo("step2-code-invalid");
        HtmlInput code = p.byId("code");
        assertThat(code.isDisabled()).as("MBR-4091은 잠그지 않고 재입력을 허용해야 한다").isFalse();
        HtmlButton btnVerifyNext = p.byId("btnVerifyNext");
        assertThat(btnVerifyNext.isDisabled()).as("코드가 이미 6자리이므로 다음 버튼은 활성 상태여야 한다").isFalse();

        // round2 재작업 지시 2 — 코드 재입력 시 오류 표시가 해제(복귀)되는지 확인
        code.type("0"); // 6자리를 초과해 마지막 자리가 잘리지만 input 이벤트는 발생한다
        p.waitJs();
        assertThat(p.state()).isEqualTo("step2-initial");
    }

    // linked_tc: TC-FUNC-member-001-08
    // §5-6 STEP2 인증코드 만료 상태(진짜 카운트다운 만료 — round3 재작업 B로 step2-locked와
    // 분리됨). 만료는 잠금이 아니다: 입력은 그대로 두고 재전송 버튼만 강조한다.
    @Test
    void state6_step2CodeExpired_onCountdownReachesZero() throws Exception {
        Page p = toStep2(1); // 유효시간 1초
        p.webClient.waitForBackgroundJavaScript(2000);

        assertThat(p.state()).isEqualTo("step2-code-expired");
        HtmlInput code = p.byId("code");
        assertThat(code.isDisabled())
                .as("만료는 잠금이 아니다 — 입력 잠금은 오직 step2-locked에서만 건다(round3 재작업 A)")
                .isFalse();
        HtmlButton btnResend = p.byId("btnResend");
        assertThat(btnResend.isDisplayed()).isTrue();
        assertThat(btnResend.getAttribute("class"))
                .as("만료는 재전송으로 실제 회복되므로 강조한다")
                .contains("emphasize");
    }

    // linked_tc: TC-FUNC-member-001-09
    // §5-7 STEP2 인증 시도 상한 초과(잠금) 상태 — MBR-4093, round3 재작업 B로 step2-code-expired
    // 에서 분리한 신규 10번째 상태. 재전송이 잠금을 풀지 않으므로(SR-295) 강조하지 않는다.
    @Test
    void state7_step2Locked_onAttemptCapReached() throws Exception {
        Page p = toStep3();
        HtmlInput password = p.byId("password");
        password.type(VALID_PASSWORD);
        HtmlInput name = p.byId("name");
        name.type(VALID_NAME);
        p.waitJs();

        when(memberRegistrationService.signUp(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4093", "인증 시도 횟수를 초과했습니다."));

        HtmlButton btnSignup = p.byId("btnSignup");
        btnSignup.click();
        p.waitJs();

        assertThat(p.state()).isEqualTo("step2-locked");
        HtmlInput code = p.byId("code");
        assertThat(code.isDisabled()).as("MBR-4093(실제 잠금)일 때만 입력을 잠근다").isTrue();
        HtmlButton btnResend = p.byId("btnResend");
        assertThat(btnResend.isDisplayed()).isTrue();
        assertThat(btnResend.getAttribute("class"))
                .as("잠금 상태는 재전송을 강조하지 않는다 — 강조하면 '재전송하면 풀린다'로 오독된다(round3 재작업 B)")
                .doesNotContain("emphasize");
    }

    // linked_tc: TC-FUNC-member-001-10
    // §5-8 STEP3 입력 상태
    @Test
    void state8_step3Input_onEnteringStep3() throws Exception {
        Page p = toStep3();
        assertThat(p.state()).isEqualTo("step3-input");
    }

    // linked_tc: TC-FUNC-member-001-11
    // §5-9(재구성: STEP3) 비밀번호 규칙 위반 상태 — 진입 + 복귀(유효해지면 step3-input)
    @Test
    void state9_step3PasswordInvalid_entryAndRecovery() throws Exception {
        Page p = toStep3();
        HtmlInput password = p.byId("password");
        password.type("short");
        p.waitJs();
        assertThat(p.state()).isEqualTo("step3-password-invalid");

        password.type("1234"); // "short1234" — 8자 이상 + 영문/숫자 혼합으로 유효해짐
        p.waitJs();

        // round2 재작업 지시 2 — 비밀번호가 유효해지면 §5-8 입력 상태로 복귀해야 한다
        // (round1 QA CONCERNS 권고2 재현조건: 복구 분기가 없으면 여기서 영구 고착된다)
        assertThat(p.state()).isEqualTo("step3-input");
    }

    // linked_tc: TC-FUNC-member-001-12
    // §5-10 가입 완료 상태(환영 쿠폰 안내 포함)
    @Test
    void state10_stepDone_onSuccessfulSignup() throws Exception {
        Page p = toStep3();
        HtmlInput password = p.byId("password");
        password.type(VALID_PASSWORD);
        HtmlInput name = p.byId("name");
        name.type(VALID_NAME);
        p.waitJs();

        when(memberRegistrationService.signUp(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenReturn(new MemberRegistrationService.SignupResult("M-0001", "EMAIL", VALID_EMAIL));

        HtmlButton btnSignup = p.byId("btnSignup");
        btnSignup.click();
        p.waitJs();

        assertThat(p.state()).isEqualTo("step-done");
    }

    // linked_tc: TC-FUNC-member-001-13
    // round3 재작업 지시(1)(2) 회귀 — STEP3 체류 중(유효시간 1초, 2초 대기) 코드가 만료돼도
    // goStep(3)이 타이머를 정지시켜 화면이 조용히 바뀌지 않아야 한다(round2 QA CONCERNS 권고1
    // 재현조건). 그 뒤 STEP2로 되돌아와도(MBR-4091) 입력이 잠기지 않아야 한다(권고2 재현조건).
    // 주의(round4에서 재확인): 이 시나리오는 goStep(3)이 타이머를 멈춘 뒤 곧바로 제출하므로
    // state.expiresAt이 실제로는 아직 지나지 않은 채(활성 배경 작업이 없어 waitForBackgroundJavaScript
    // 가 거의 즉시 반환됨) MBR-4091을 받는 "코드 오답(미만료)" 경로다 — 아래
    // regressionMbr4091AfterRealExpiry_keepsResendButtonVisible이 "실제로 만료된 뒤" 경로를
    // 별도로 다룬다.
    @Test
    void regressionStep3TimerStopped_thenMbr4091NeverLocksInput() throws Exception {
        Page p = toStep2(1);
        HtmlInput code = p.byId("code");
        code.type("999999");
        p.waitJs();
        HtmlButton btnVerifyNext = p.byId("btnVerifyNext");
        btnVerifyNext.click();
        p.waitJs();
        assertThat(p.state()).isEqualTo("step3-input");

        p.webClient.waitForBackgroundJavaScript(2000);
        assertThat(p.state())
                .as("STEP3에 머무는 동안 코드가 만료돼도 화면이 조용히 바뀌면 안 된다")
                .isEqualTo("step3-input");

        HtmlInput password = p.byId("password");
        password.type(VALID_PASSWORD);
        HtmlInput name = p.byId("name");
        name.type(VALID_NAME);
        p.waitJs();

        when(memberRegistrationService.signUp(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4091", "인증코드가 올바르지 않습니다."));

        HtmlButton btnSignup = p.byId("btnSignup");
        btnSignup.click();
        p.waitJs();

        assertThat(p.state()).isEqualTo("step2-code-invalid");
        assertThat(p.<HtmlInput>byId("code").isDisabled())
                .as("MBR-4091은 절대 입력을 잠그지 않는다")
                .isFalse();
        assertThat(p.<HtmlButton>byId("btnVerifyNext").isDisabled()).isFalse();
    }

    // linked_tc: TC-FUNC-member-001-14
    // round4 재작업 지시(1) — 인증코드가 실제로 만료된 뒤(STEP2에서 배경 타이머가 실제로
    // 0에 도달 — state6과 같은 방식으로 실측) STEP3를 거쳐 서버가 MBR-4091(만료도 서버는
    // 이 코드로 응답한다, INF-MBR-002 §비즈니스규칙)을 돌려주면, 만료 UI(재전송 노출·강조)가
    // 화면에 그대로 남아 있어야 한다 — 되돌리면 사용자가 새로고침 외에 새 코드를 받을 수단이
    // 없어진다(round3 QA CONCERNS 권고1 재현조건, 사람 코멘트: "새로고침 없이는 복구 못 하는
    // 케이스는 남기지 않는다").
    @Test
    void regressionMbr4091AfterRealExpiry_keepsResendButtonVisible() throws Exception {
        Page p = toStep2(1); // 유효시간 1초
        p.webClient.waitForBackgroundJavaScript(2000); // 배경 타이머가 실제로 0에 도달(state6과 동일 방식)
        assertThat(p.state())
                .as("사전조건 — 이 시점에 코드가 실제로 만료돼 있어야 한다")
                .isEqualTo("step2-code-expired");

        HtmlInput code = p.byId("code");
        code.type("999999");
        p.waitJs();
        HtmlButton btnVerifyNext = p.byId("btnVerifyNext");
        btnVerifyNext.click();
        p.waitJs();
        assertThat(p.state()).isEqualTo("step3-input");

        HtmlInput password = p.byId("password");
        password.type(VALID_PASSWORD);
        HtmlInput name = p.byId("name");
        name.type(VALID_NAME);
        p.waitJs();

        when(memberRegistrationService.signUp(anyString(), anyString(), anyString(), anyString(), anyBoolean()))
                .thenThrow(new MemberRegistrationApiException(HttpStatus.CONFLICT, "MBR-4091", "인증이 필요합니다"));

        HtmlButton btnSignup = p.byId("btnSignup");
        btnSignup.click();
        p.waitJs();

        // round4 핵심 단언 — 실제로 만료된 뒤 STEP2로 돌아왔을 때 재전송 버튼이 화면에 존재해야
        // 한다.
        HtmlButton btnResend = p.byId("btnResend");
        assertThat(btnResend.isDisplayed())
                .as("만료된 채로 돌아왔으면 재전송 버튼이 사라지면 안 된다 — 유일한 회복 수단이다")
                .isTrue();
        assertThat(btnResend.getAttribute("class")).contains("emphasize");
        assertThat(p.state()).isEqualTo("step2-code-expired");
        assertThat(p.<HtmlInput>byId("code").isDisabled())
                .as("만료는 잠금이 아니다 — 재입력도 계속 허용된다")
                .isFalse();
    }
}
