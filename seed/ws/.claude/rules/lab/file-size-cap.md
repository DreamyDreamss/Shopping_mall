---
paths: ["modules/**/*.java", "modules/shop-web/src/**/*.tsx"]
severity: should
checks:
  - id: java-file-cap
    kind: line_cap
    glob: "modules/shop-api/src/main/**/*.java"
    max: 450
    message: "자바 파일 450줄 초과 — 책임을 나눈다(필터·핸들러·서비스가 한 파일에 자라면 QA가 스코프를 못 가른다)"
  - id: tsx-file-cap
    kind: line_cap
    glob: "modules/shop-web/src/**/*.tsx"
    max: 300
    message: "화면 부품 300줄 초과 — 상태별 부품으로 나누고 각각 스토리를 둔다"
---
# 파일 크기 상한

AIDD는 파일 단위로 읽고 고친다. 파일이 크면 에이전트가 한 번에 못 읽어 부분만 보고 고치고(RUN7: 400줄 필터에서 세션 변수
순서 결함), QA도 diff 밖 코드를 못 본다. 상한은 차단이 아니라 나누라는 신호다(`should`).
