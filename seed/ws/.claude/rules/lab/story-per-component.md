---
paths: ["modules/shop-web/src/**/*.tsx"]
severity: must
checks:
  - id: story-per-component
    kind: pair
    for: "modules/shop-web/src/(*)/(*).tsx"
    require: "modules/shop-web/src/{1}/{2}.stories.tsx"
    exclude: ["**/*.stories.tsx", "**/pages/**", "**/*.test.tsx", "**/*.spec.tsx"]
    message: "화면 부품에는 상태 스토리가 있어야 한다"
---
# 부품마다 상태 스토리

화면 부품(`.tsx`)을 만들거나 고쳤으면 그 상태를 보여 주는 스토리(`X.stories.tsx`)가 옆에 있어야 한다.
상태를 구현했는데 볼 방법이 없으면 다음 사람이 그 상태를 못 본다.

페이지 컨테이너(`src/pages/`)는 데이터를 부르는 쪽이라 스토리 대상이 아니다(SR-227 채택 시 제외 추가).
