// 타입 검사 = 이 모듈의 최소 회귀(유닛 러너 없음). run_tests.py가 붙이는 --passWithNoTests 같은 인자는 무시한다.
require('child_process').execSync('npx tsc --noEmit', { stdio: 'inherit' })
