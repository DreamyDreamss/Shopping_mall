---
paths: ["modules/shop-api/src/main/java/**/controller/*.java"]
severity: must
checks:
  - id: controller-has-test
    kind: pair
    for: "modules/shop-api/src/main/java/com/sm/lab/shop/controller/(*)Controller.java"
    require: "modules/shop-api/src/test/java/com/sm/lab/shop/controller/{1}ControllerTest.java"
    message: "컨트롤러마다 HTTP 레벨 테스트(MockMvc)가 같은 이름으로 있어야 한다 — 응답 코드·오류 계약은 여기서만 검증된다"
---
# 컨트롤러 = HTTP 테스트 한 벌

이 프로젝트의 계약은 HTTP 응답(상태 코드·`MBR-4092` 같은 오류 코드·JSON 형태)이다. 서비스 단위 테스트만 있으면
컨트롤러의 매핑·예외 핸들러·API 키 필터가 검증되지 않는다(RUN7: 역할이 뒤바뀐 두 API를 잡은 것은 HTTP 테스트였다).
`XxxController.java`를 만들면 `XxxControllerTest.java`를 같은 패키지 경로에 만든다.
