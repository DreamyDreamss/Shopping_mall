---
paths: ["modules/**/*.java"]
severity: must
checks:
  - id: no-printstacktrace
    kind: regex
    glob: "modules/**/src/main/**/*.java"
    pattern: "\\.printStackTrace\\("
    message: "예외는 삼키거나 콘솔에 찍지 않는다 — 로거로 남기거나 ApiExceptionHandler 계약(코드·메시지)으로 올린다"
---
# 예외를 콘솔에 찍지 않는다

`printStackTrace()`는 운영 로그 체계 밖으로 새고, 호출자에게는 아무 계약도 주지 않는다.
이 프로젝트의 오류 계약은 `web/*ExceptionHandler.java`(코드 `ORD-4001`·`MBR-4092` 같은 문자열 + message)다 —
새 예외는 그 핸들러에 매핑하고, 로그는 slf4j 로거로 남긴다.
