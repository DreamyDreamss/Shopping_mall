---
paths: ["modules/**/*.java"]
severity: must
checks:
  - id: no-sysout
    kind: regex
    glob: "modules/**/*.java"
    pattern: "System\\.out\\.println"
    message: "System.out 대신 로거를 쓴다"
---
# 콘솔 출력 금지

운영 코드에서 `System.out.println`을 쓰지 않는다 — 로거(slf4j)를 쓴다.
