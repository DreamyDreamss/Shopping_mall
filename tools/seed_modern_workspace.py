from pathlib import Path
import json
import shutil
import sys

plugin = Path('D:/speclinker-main')
ws = Path('D:/sl-shop')
out = Path('D:/sl-shop-runtime/setup')
sys.path.insert(0, str(plugin / 'scripts'))
import sl_paths
import sr_ledger
import build_clarify
import request_workflow
import quick_sr

for name in ('CLAUDE.md',):
    p = ws / name
    if not (out / (name + '.audit-copy')).exists():
        shutil.copy2(p, out / (name + '.audit-copy'))
    original = Path(json.loads((out / 'workspace-ready.json').read_text(encoding='utf-8'))['backup']) / name
    historic = original.read_text(encoding='utf-8')
    # Replace the old one-request audit restriction, retaining project rules below it.
    (ws / name).write_text('''# 로컬 쇼핑몰 테스트베드 — 현재 실행 계약

- 플러그인: D:/speclinker-main (main). 소스: project.env의 SOURCE_*_PATH, D:/Shopping_mall-modern.
- 워크스페이스: D:/sl-shop. DB: 127.0.0.1:13328/sl_shop_modern. 앱: http://127.0.0.1:18985/shop.
- 원본 Shopping_mall 및 감사용 _tmp 소스/DB는 이 테스트베드의 수정 대상이 아니다.
- 일반/빠른 요청 모두 7단계다. 빠른 요청은 명시적 범위·완료 조건·통합 승인 확인이 필요하다.
- [실습] 요청과 샘플 계정은 체험용이다. 사람 승인·실제 인수로 간주하지 않는다.
- 과거 설계서·승인·QA의 날짜·소스 지문은 보존한다. 현재 소스로 재검증하지 않고 PASS로 바꾸지 않는다.
- DB 구조 변경은 일반 요청으로 검토한다. 작업 공간 분리는 Git/포트 분리이며 DB·OS 격리가 아니다.
- Java 17, Maven은 D:/Shopping_mall/.runtime/apache-maven-3.9.9/bin. 기동 스크립트가 PATH를 설정한다.
- DDL 최신 번호는 현재 V18이다. 새 마이그레이션은 실제 소스를 확인한 뒤 다음 빈 번호를 쓴다.
- 상세 사용법·체험 계정·단계별 다음 행동은 TESTBED_GUIDE.md를 읽는다.

---
아래는 기존 프로젝트 규칙이다. 환경·최신 번호는 위 계약과 실제 소스가 우선한다.

''' + historic, encoding='utf-8')

guide = '''# 쇼핑몰 테스트베드 사용 안내

SpecLens: http://localhost:18772/docs/viewer2/index.html
쇼핑몰: http://127.0.0.1:18985/shop

## 바로 해 볼 일

| 목적 | 시작할 곳 | 다음 행동 |
|---|---|---|
| 일반 요청의 문답 | 요청 SR-901 | 미결 문답에 답하고 요건을 검토한다 |
| 설계서 표 편집·비교·복구 | 요청 SR-902 | 변경 설계 초안을 항목별 편집하고 저장 전 비교한다 |
| 빠른 요청 | 새 요청 → 빠른 요청 | 기존 설계서·완료 조건·민감 변경 여부를 확인한 뒤 접수한다 |
| DB 변경 검토 | 요청 SR-903 | 일반 절차로 영향·DDL·되돌리기를 검토한다 |
| 구현·QA·반영의 누적 기록 | 기존 요청 목록 | 실제 상태·날짜·게이트를 확인한다. 과거 완료는 새 승인과 다르다 |
| 설계 탐색 | 설계서 → 화면/API/DB | 도메인을 고르고 구조화 보기와 원문·근거를 대조한다 |
| 납품 | 납품 → 문서 선택 | 경고·미검증 문서를 확인하고 HTML/PDF/Word/Excel을 생성한다 |

## 쇼핑몰 계정과 상품

로컬 체험 전용이며 외부 서비스 계정이 아니다.

- 구매자: buyer@speclinker.local / Shop-demo-local-20261018!
- 다른 회원: other@speclinker.local / 같은 비밀번호. 다른 회원 주문·장바구니 접근 차단 확인용.
- 배송비: SKU-DEMO-SHIP (20,000원), 고가 상품: SKU-DEMO-FREE (60,000원).
- 품절: SKU-DEMO-SOLDOUT, 판매중지: SKU-DEMO-HIDDEN. 기존 상품·회원·주문·리뷰도 유지한다.
- 최초 장바구니는 비어 있다. 주문 폼 주소 검색 → 수량·합계 → 주문 결과를 확인한다.
- 실제 결제·외부 배송 전송은 없다. 과거 주문은 날짜 필터로 기간을 넓혀 찾는다.

## 실행·갱신

`powershell -ExecutionPolicy Bypass -File D:/Shopping_mall-modern/tools/start-modern-testbed.ps1`

`python D:/Shopping_mall-modern/tools/refresh_modern_testbed.py`는 색인만 다시 만든다.
기존 워크스페이스나 DB를 삭제하지 않는다. 설정·DB·로그는 D:/sl-shop-runtime에 둔다.
읽기 전용 DB MCP와 로컬 Jira 목업을 등록했다. 실계정 Jira/Wiki 연동은 별도이며 자동 완료가 아니다.
원본 백업 위치는 D:/sl-shop-runtime/setup/workspace-ready.json에 기록되어 있다.
'''
(ws / 'TESTBED_GUIDE.md').write_text(guide, encoding='utf-8')

definitions = [
    ('SR-901', '[실습] 주문 목록 상태 필터 추가', '주문 목록에서 처리 상태를 선택해 원하는 주문을 빠르게 찾는다.',
     '기존 목록·페이지 이동을 유지한다. 상태 변경 API와 DB 구조는 바꾸지 않는다.',
     ['상태 선택 시 목록이 해당 상태만 표시된다', '필터 해제 시 전체 목록으로 돌아온다', '빈 결과를 명확히 안내한다']),
    ('SR-902', '[실습] 상품 배송비 안내 설계 편집', '상품 상세에서 배송비와 주문 전 확인 지점을 쉽게 이해하게 한다.',
     '문구와 안내 배치만 검토한다. 배송비 정책·계산 API·DB는 바꾸지 않는다.',
     ['고정 배송비와 장바구니 합계 확인 위치를 안내한다', '모바일에서 안내 문구가 잘리지 않는다', '원래 설계와 변경안을 비교할 수 있다']),
    ('SR-903', '[실습] 주문 배송정보 보존기간 검토', '배송정보 보존·파기 기준과 DB 영향을 분석한다.',
     '기획·분석용 요청이다. 고객정보 삭제 SQL이나 자동 배치는 아직 실행하지 않는다.',
     ['보존 대상·기간·예외를 검토한다', 'DB·API·배치 영향과 복구 제약을 정리한다', '사람 승인 전에는 구현·파기를 시작하지 않는다']),
]
created = []
for sid, title, gist, scope, acceptance in definitions:
    if sid in (sr_ledger.load_ledger(str(ws)).get('srs') or {}):
        raise SystemExit('Refusing to overwrite ' + sid)
    dossier = Path(sl_paths.doc_root(str(ws), 'change', sid))
    dossier.mkdir(parents=True)
    text = '# ' + title + '\n\n> 체험 데이터. 사람의 검토·승인·인수 기록이 아니다.\n\n## 요구 요지\n\n- ' + gist
    text += '\n\n## 범위\n\n' + scope + '\n\n## 수용 기준\n\n' + '\n'.join('- [ ] ' + s for s in acceptance) + '\n'
    (dossier / '00_요구사항.md').write_text(text, encoding='utf-8')
    sr_ledger.upsert(str(ws), sid, title=title, status='RECEIVED', source='local',
                     by='testbed-fixture', note='실습용 초기 데이터; 사용자 승인 아님')
    request_workflow.record(str(ws), sid, 'full', 'testbed-fixture')
    build_clarify.build(str(ws), sid)
    created.append({'sr': sid, 'status': 'RECEIVED', 'sample': True})

dossier = Path(sl_paths.doc_root(str(ws), 'change', 'SR-902'))
(dossier / '02_변경명세.md').write_text('''# 상품 배송비 안내 변경 설계 초안

> 실습용 미승인 초안. 아래 내용은 실제 구현 완료를 뜻하지 않는다.

## 목적

상품 가격 바로 아래에서 고정 배송비와 최종 합계 확인 위치를 이해한다.

## 표시 규칙

| 조건 | 안내 문구 | 사용자의 다음 행동 |
|---|---|---|
| 선택 상품이 있음 | 배송비 3,000원 | 장바구니에서 최종 금액 확인 |
| 선택 상품이 없음 | 배송비 0원 | 구매할 상품 선택 |
| 상품 품절 | 현재 품절 | 재입고 안내 확인 |

## 검토할 사항

- [ ] 배송비 기준을 실제 정책·API와 대조한다.
- [ ] 390px 화면에서 가격·안내·구매 버튼을 함께 읽을 수 있다.
- [ ] 표 편집 → 미리보기 → 저장 전 비교 → 저장 → 판본 복구를 확인한다.

## 제외 범위

배송비 계산식, 결제, DB 구조 변경은 이 초안의 범위가 아니다.
''', encoding='utf-8')
(out / 'sample-requests.json').write_text(json.dumps(created, ensure_ascii=False, indent=2), encoding='utf-8')
print(json.dumps({'sample_requests': created, 'guide': str(ws / 'TESTBED_GUIDE.md')}, ensure_ascii=False))
