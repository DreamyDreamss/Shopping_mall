---
paths: ["modules/shop-api/src/main/resources/db/*.sql"]
severity: must
checks:
  - id: ddl-create-if-not-exists
    kind: regex
    glob: "modules/shop-api/src/main/resources/db/*.sql"
    pattern: "(?i)^\\s*CREATE\\s+TABLE\\s+(?!IF\\s+NOT\\s+EXISTS)"
    message: "CREATE TABLE은 IF NOT EXISTS — 이 앱은 마이그레이션 엔진 없이 기동마다 schema-locations를 재실행한다"
  - id: ddl-drop-forbidden
    kind: regex
    glob: "modules/shop-api/src/main/resources/db/*.sql"
    pattern: "(?i)^\\s*DROP\\s+(TABLE|COLUMN|INDEX)"
    message: "기동 DDL에서 DROP 금지 — 운영 데이터가 있는 테이블은 SR의 04_ROLLBACK.md로만 되돌린다"
---
# 기동 DDL은 멱등해야 한다

Flyway/Liquibase가 없어(pom.xml) `spring.sql.init.mode=always`로 매 기동마다 `db/*.sql`이 다시 실행된다(RUN7 라운드 2에서
확정한 방식). `CREATE TABLE IF NOT EXISTS` · `ALTER TABLE … ADD COLUMN IF NOT EXISTS`만 쓴다. DROP은 기동 스크립트에 넣지 않는다.
