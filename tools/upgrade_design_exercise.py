"""Upgrade only an untouched, unapproved practice draft; retain its exact bytes."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import sys


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plugin',default='D:/speclinker-main')
    parser.add_argument('--workspace',default='D:/sl-shop')
    parser.add_argument('--seed',required=True,help='Immutable earlier seed used to verify no human edits')
    parser.add_argument('--sr',default='SR-902')
    args=parser.parse_args();ws=Path(args.workspace).resolve();plugin=Path(args.plugin).resolve();seed=Path(args.seed).resolve()
    sys.path.insert(0,str(plugin/'scripts'))
    import knowledge_evidence as E
    import speclens_server as S
    import sr_ledger as R
    import draft_history
    folder=E.dossier(str(ws),args.sr)
    row=(R.load_ledger(str(ws)).get('srs') or {}).get(args.sr) or {}
    if (row.get('status')!='ANALYZED' or '실습' not in row.get('title','')
            or any(h.get('to')=='APPROVED' for h in row.get('history') or [])):
        raise ValueError('Only the unapproved analyzed practice request may be upgraded')
    names=['00_요구사항.md','_impact.json','_clarify.json','02_변경명세.md','inputs/_types.json']
    for name in names:
        current=folder/name;old=seed/folder.relative_to(ws)/name
        if current.exists()!=old.exists() or current.exists() and current.read_bytes()!=old.read_bytes():
            print(json.dumps({'sr':args.sr,'changed':False,'status':'preserved-existing-edits'}));return
    path=folder/'02_변경명세.md';raw=path.read_bytes();base=raw.decode('utf-8-sig')
    proposal=(plugin/'docs/audit/sr-lifecycle-20261018/model-cli-6-draft.md').read_text(encoding='utf-8-sig')
    if not re.search(r'^#\s+'+re.escape(args.sr)+r'\b',proposal,re.M):
        raise ValueError('Recorded proposal belongs to a different request')
    metadata=re.search(r'<!-- source-evidence: (.*?) -->',proposal)
    if not metadata:raise ValueError('Recorded source provenance is required')
    files=json.loads(metadata.group(1))['files']
    roots={entry['label']:Path(entry['path']).resolve() for entry in E.sl_config.sources(str(ws))}
    for entry in files:
        if entry['source'] not in roots:raise ValueError('Source labels changed; preserve the existing draft')
        root=roots[entry['source']];source=(root/entry['path']).resolve()
        if not source.is_relative_to(root) or not source.is_file() or hashlib.sha256(source.read_bytes()).hexdigest()!=entry['sha256']:
            raise ValueError('Recorded draft no longer matches current source bytes')
    E.collect(str(ws),args.sr)
    result=S._build_tobe_save_action(str(ws),{'sr':args.sr,'base':base,'text':proposal,
        '_by':'testbed-fixture (검증된 CLI 생성 예시·사용자 승인 아님)'})
    assert result['ok'] and draft_history.history(str(ws),args.sr,result['previous_revision'])['text']==base
    evidence=E.capture(str(ws),args.sr)
    assert evidence['ok'] and R.load_ledger(str(ws))['srs'][args.sr]['status']=='ANALYZED'
    print(json.dumps({'sr':args.sr,'status':'ANALYZED','approved':False,'changed':True,'new_model_calls':0,
                      'recorded_model_run':'model-cli-6','verified_source_files':len(files),'previous_revision':result['previous_revision']}))


if __name__=='__main__':main()
