"""Prepare/apply evidence maintenance on the modern testbed; never issue approvals."""
import argparse
import hashlib
import importlib
import json
from pathlib import Path
import sys
import time


MISSING_FILES = {'MemberOAuthController.java', 'V11__member_social_accounts.sql',
                 'ProductCardV2.tsx', 'ProductCardV2.stories.tsx', 'ProductCardV2.test.tsx',
                 'productCardBadges.ts', 'productCardBadges.unit.test.ts'}


def inventory(integrity, ws):
    catalog = integrity._candidate_catalog(str(ws))
    result = {}
    complete = not catalog[1]
    for name in sorted(MISSING_FILES):
        candidates, cut = integrity._candidates(str(ws), name, catalog)
        complete = complete and not cut
        result[name] = []
        for candidate in candidates:
            path = Path(integrity.source_preview.resolve(str(ws), candidate))
            if path.stat().st_size > integrity.MAX_BYTES:
                complete = False
                continue
            result[name].append([candidate, hashlib.sha256(path.read_bytes()).hexdigest()])
    return result, complete


def text_hashes(ws):
    return {p.relative_to(ws).as_posix(): hashlib.sha256(p.read_text(encoding='utf-8-sig').replace('\r\n', '\n').replace('\r', '\n').encode()).hexdigest() for p in (ws/'docs').rglob('*.md')}


def run(plugin, workspace, apply=False, plan_path=None):
    started = time.monotonic()
    sys.path.insert(0, str(Path(plugin).resolve() / 'scripts'))
    integrity = importlib.import_module('source_integrity')
    paths = importlib.import_module('sl_paths')
    ws = Path(workspace).resolve()
    before = {p.relative_to(ws).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in (ws / 'docs').rglob('*.md')}
    current_inventory, inventory_complete = inventory(integrity, ws)
    current_texts = text_hashes(ws)
    cached = json.loads(Path(plan_path).read_text(encoding='utf-8')) if plan_path else None
    if cached and (cached.get('schema') != 'source-integrity-testbed-plan-v1' or not isinstance(cached.get('document_text_hashes'), dict)):
        raise ValueError('Unsupported preflight plan format')
    report = {'schema': 'source-integrity-testbed-plan-v1', 'apply': apply, 'repairs': [], 'tracking': [], 'approvals_issued': False}
    design = Path(paths.design_root(str(ws)))
    # This exact abbreviated anchor has a unique current class, whose /shop
    # mapping returns the SPA index resource. Do not replace V2 with v1.
    shop = design / 'order/UIS/UIS-ORD-008_shop/spec.md'
    doc = shop.relative_to(ws).as_posix()
    current = integrity.inspect_document(str(ws), doc)
    old = next((r for r in current['references'] if r['anchor'] == 'src/controller/ShopIndexController.java'), None)
    if old:
        candidates = [p for p in old['proposals'] if p.endswith('src/main/java/com/sm/lab/shop/controller/ShopIndexController.java')]
        if len(candidates) != 1:
            raise ValueError('The abbreviated controller no longer has one verified candidate')
        args = dict(doc=doc, revision=current['revision'], reference=old['id'], replacement=candidates[0],
                    note='현재 등록 소스의 동일 클래스와 /shop 매핑·SPA index 리소스 반환을 대조해 잘못 줄인 경로를 수정함. 설계 승인 기록이 아님.')
        preview = integrity.act(str(ws), 'preview', args, 'testbed-maintenance')
        report['repairs'].append({k: preview[k] for k in ('doc', 'replacement', 'diff', 'revision', 'source_sha256')})
        if apply:
            integrity.act(str(ws), 'apply', dict(args, token=preview['token']), 'testbed-maintenance')
    # Enumerate the exact known absent implementations in current documents.
    # Track the gap even when an inline spelling differs from frontmatter.
    reuse = bool(cached and inventory_complete and cached.get('known_source_inventory') == current_inventory)
    if reuse:
        names = {row['doc'] for row in cached['tracking']}
        names.update(doc for doc, sha in current_texts.items() if cached['document_text_hashes'].get(doc) != sha)
        summaries = [{'doc': name} for name in sorted(names) if (ws/name).is_file()]
    else:
        scan = integrity.inspect_workspace(str(ws))
        if not scan['ok'] or scan['truncated']:
            raise ValueError('Document scan is incomplete; inspect the workspace before applying')
        summaries = [s for s in scan['documents'] if s.get('counts', {}).get('unresolved')]
    for summary in summaries:
        current = integrity.inspect_document(str(ws), summary['doc'], candidates=False)
        for row in current['references']:
            parsed = integrity.source_preview.parse_anchor(row['anchor'])
            name = Path(parsed[0].replace('\\', '/')).name if parsed else ''
            if row['state'] != 'unresolved' or name not in MISSING_FILES:
                continue
            tracked = row['review'] == 'tracked' and row.get('last_review', {}).get('disposition') == 'implementation-gap'
            report['tracking'].append({'doc': summary['doc'], 'anchor': row['anchor'], 'already_tracked': tracked})
            if apply and not tracked:
                integrity.act(str(ws), 'track', dict(doc=summary['doc'], revision=current['revision'], reference=row['id'], basis=row['basis'], review_revision=row['review_revision'],
                    disposition='implementation-gap', note='문서에 구현 근거가 기재되어 있으나 현재 등록 소스에는 해당 파일이 없음. 구현·이관 누락 또는 과거 설계 여부를 확인해야 함. 다른 이름의 파일이나 이전 저장소로 대체하지 않았으며 승인·구현·검증 완료가 아님.'), 'testbed-maintenance')
    after = {p.relative_to(ws).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in (ws / 'docs').rglob('*.md')}
    report['changed_documents'] = [p for p in before if before[p] != after.get(p)]
    expected = {doc} if report['repairs'] and apply else set()
    if set(report['changed_documents']) != expected or before.keys() != after.keys():
        raise ValueError('Unexpected document changes detected')
    report['source_files_changed'] = False
    report.update(document_text_hashes=text_hashes(ws), known_source_inventory=current_inventory, preflight_reused=reuse, elapsed_seconds=round(time.monotonic()-started,3))
    return report


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plugin', required=True)
    parser.add_argument('--workspace', required=True)
    parser.add_argument('--apply', action='store_true')
    parser.add_argument('--output', required=True)
    parser.add_argument('--plan', help='Reuse a prior complete plan only when known source files and document text fingerprints still match')
    args = parser.parse_args()
    report = run(args.plugin, args.workspace, args.apply, args.plan)
    Path(args.output).write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({'apply': args.apply, 'repairs': len(report['repairs']), 'tracked_references': len(report['tracking']), 'changed_documents': report['changed_documents']}, ensure_ascii=False))


if __name__ == '__main__':
    main()
