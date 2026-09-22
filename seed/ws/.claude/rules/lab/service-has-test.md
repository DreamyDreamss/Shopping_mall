---
paths: ["modules/shop-api/src/main/java/**/service/*Service.java"]
severity: should
checks:
  - id: service-has-test
    kind: pair
    for: "modules/shop-api/src/main/java/com/sm/lab/shop/service/(*)Service.java"
    require: "modules/shop-api/src/test/java/com/sm/lab/shop/service/{1}ServiceTest.java"
    message: "서비스에 단위 테스트가 없다 — 트랜잭션 경계·동시성(락/원자 UPDATE)은 HTTP 테스트로는 재현이 어렵다"
---
# 서비스 = 단위 테스트 (권고)

HTTP 테스트가 계약을, 서비스 테스트가 **경계 조건**(트랜잭션 롤백에 카운터가 같이 날아가는 것, 동시 요청의 데드락 —
RUN7 FUNC-member-002 라운드 2·3)을 잡는다. `must`가 아닌 이유: 기존 4개 서비스가 아직 없다(기록만 남긴다). 새 서비스부터는 만든다.
