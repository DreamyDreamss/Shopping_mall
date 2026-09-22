---
paths: ["modules/shop-api/src/main/resources/mapper/*.xml"]
severity: must
checks:
  - id: no-select-star
    kind: regex
    glob: "modules/shop-api/src/main/resources/mapper/*.xml"
    pattern: "(?i)select\\s+\\*"
    message: "SELECT * 금지 — 컬럼을 적는다. 응답 값 스냅샷(축 D)이 컬럼 추가 하나에 통째로 깨진다"
---
# 매퍼 XML에서 SELECT * 를 쓰지 않는다

MyBatis 매퍼는 이 앱의 응답 계약이 시작되는 곳이다. `SELECT *`는 테이블에 컬럼이 하나 늘면 모든 응답이 같이 늘어
축 D(응답 값 대조)가 "무엇이 바뀌었나"를 못 가르고 통째로 diff를 낸다. 컬럼을 적으면 변경이 그 SQL 한 줄로 드러난다.
