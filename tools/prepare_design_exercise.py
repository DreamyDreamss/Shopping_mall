"""Prepare a visibly attributed design exercise; never approves it."""
import json
from pathlib import Path
import subprocess
import sys

sys.path.insert(0, 'D:/speclinker-main/scripts')
import build_clarify
import clarify_store
import sr_ledger
import stage_gates
import sl_paths
import draft_history

ws = 'D:/sl-shop'
by = 'testbed-fixture (실습 자료·사용자 승인 아님)'
build_clarify.write_decided(ws, 'SR-902', ['SCREEN_MODIFY'], by=by, basis='배송비 안내 문구·배치만 변경하는 실습')
build_clarify.build(ws, 'SR-902')
answers = {
    'scope_freeze': '상품 상세 UIS-ORD-010의 배송비 안내 문구와 배치만 검토한다. 계산 API·DB·배송비 정책·다른 화면은 제외한다.',
    'regression_keep': '기존 가격·수량·장바구니 담기·찜·비교·상품평 동작과 선택 상품이 있을 때 고정 배송비 3,000원을 유지한다.',
    'scr_scope': '수정 화면은 UIS-ORD-010 상품 상세 /shop#/shop/products/:sku 하나다. 장바구니와 주문 화면은 회귀 확인만 한다.',
    'scr_empty_state': '상품 없음은 기존 404 안내, 통신 오류는 기존 다시 시도, 품절은 기존 구매 버튼 비활성화를 유지한다.',
    'scr_states': '재고 있음·품절·존재하지 않는 상품·통신 오류와 390px 모바일 상태를 기존 Storybook 및 브라우저로 대조한다.',
    'scr_entry': '홈 상품 목록과 검색 결과에서 상품 상세로 들어온다. 직접 URL 진입도 유지하며 진입 메뉴는 바꾸지 않는다.',
    'scr_m_target': 'UIS-ORD-010 상품 상세. 현재 URL /shop#/shop/products/:sku. ProductDetailPage가 사용하는 상품 정보 부품이다.',
    'scr_m_change': '가격 아래 배송비 3,000원 안내를 유지하고 최종 합계는 장바구니에서 확인하라는 보조 문구를 읽기 쉽게 배치한다.',
    'scr_m_asis': '현재 상품 가격 아래 배송비 3,000원이 고정 표시된다. 선택 상품이 있으면 배송비 3,000원이며 무료배송 기준금액은 없다.',
}
for qid, text in answers.items():
    clarify_store.answer(ws, 'SR-902', qid, text, by=by)
subprocess.run([sys.executable, 'D:/speclinker-main/scripts/build_impact.py', 'SR-902', '--root', ws,
                '--uis', 'UIS-ORD-010', '--title', '[실습] 상품 배송비 안내 설계 편집', '--clarification', 'resolved'], check=True)
evaluation = stage_gates.evaluate(ws, 'SR-902', 'ANALYZED')
if evaluation['blocking']:
    raise SystemExit('Exercise remains RECEIVED: ' + json.dumps(evaluation['blocking'], ensure_ascii=False))
sr_ledger.upsert(ws, 'SR-902', status='ANALYZED', by=by,
                 note='샘플 문답과 범위가 채워진 설계 편집 연습. 설계 승인·사용자 인수 아님')
draft = Path(sl_paths.doc_root(ws, 'change', 'SR-902', '02_변경명세.md'))
draft_history.retain(ws, 'SR-902', draft.read_bytes())
assert sr_ledger.approval_state(ws, 'SR-902')['approved'] is not True
Path('D:/sl-shop-runtime/setup/design-exercise.json').write_text(json.dumps({
    'sr': 'SR-902', 'status': 'ANALYZED', 'approved': False, 'actor': by,
    'analysis_blocking': evaluation['blocking'], 'sample_answers': list(answers),
    'draft_history_initialized': True}, ensure_ascii=False, indent=2), encoding='utf-8')
print('SR-902: analyzed fixture, unapproved; native design editor and history ready.')
