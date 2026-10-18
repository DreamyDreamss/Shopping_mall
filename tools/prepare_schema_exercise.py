"""Create an unapproved DB review exercise, preserving every existing request.

Reads current schema metadata only. Never executes forward/rollback SQL or
records a design approval, implementation, QA pass or customer acceptance.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sys


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plugin',default='D:/speclinker-main')
    parser.add_argument('--workspace',default='D:/sl-shop')
    parser.add_argument('--sr',default='SR-904')
    args=parser.parse_args()
    plugin=Path(args.plugin).resolve();ws=Path(args.workspace).resolve()
    sys.path.insert(0,str(plugin/'scripts'))
    import knowledge_evidence as E
    import schema_change as C
    import sl_paths
    import sr_ledger as R
    import request_workflow as W
    import build_clarify
    import speclens_server as S
    import item_plan
    E.enabled(str(ws),args.sr)
    target=Path(sl_paths.doc_root(str(ws),'change',args.sr))
    if target.exists():
        print(json.dumps({'sr':args.sr,'status':'exists-preserved','changed':False}));return
    if args.sr in (R.load_ledger(str(ws)).get('srs') or {}):raise ValueError('Existing ledger request is preserved')
    template=plugin/'docs/audit/schema-change-20261018/contract'
    manifest=E.read(template.parent/'validation.json')['files']
    for name in C.FILES:
        if hashlib.sha256((template/name).read_bytes()).hexdigest()!=manifest['contract/'+name]['sha256']:
            raise ValueError('Published rehearsal artifact hash mismatch')
    before=C.snapshot((template/'before.json').read_bytes())
    actual=E.live_database(str(ws),True,['products'])
    if C.normalized(before['catalog'],before['schema'])!=C.normalized(actual['catalog'],actual['schema']):
        raise ValueError('Exercise rehearsal does not match current products metadata; generate a new rehearsal first')
    specs=[sid for sid,table in item_plan.spec_links(str(ws))['sch_table'].items() if str(table).split('.')[-1].strip('`"').lower()=='products']
    if len(specs)!=1:raise ValueError('Exactly one current products table design is required for this exercise')
    change_root=Path(sl_paths.doc_root(str(ws),'change'))
    previous={p:hashlib.sha256(p.read_bytes()).hexdigest() for p in change_root.rglob('*') if p.is_file()}
    ledger_file=change_root/'_sr_ledger.jsonl'
    original_ledger=ledger_file.read_bytes() if ledger_file.exists() else b''
    actor='testbed-fixture (실습 자료·사용자 승인 아님)'
    result=S._build_sr_new_action(str(ws),{'sr':args.sr,'title':'[실습] DB 변경·복구 검토 카드','mode':'full','_by':actor,
        'gist':['DB 변경안의 컬럼·필수 여부·기본값·적용 조건과 이관·복구 SQL을 검토하는 실습이다.',
                'schema-change-exercise-v1: products.sr_review_note 문자열(80), 필수, 기본값 pending을 제안한다.'],
        'scope_in':[{'id':specs[0],'name':'상품 테이블의 검토 메모 컬럼 변경안'}],
        'scope_out':['체험 DB에 SQL 실행','상품 API·화면 구현','기존 요청·사용자 데이터 변경'],
        'acceptance':['현재 DB와 변경 전 자료가 같은지 확인한다.','별도 스키마 리허설·복구와 적용 후 데이터 조건을 검토한다.',
                      '문답·설계·사용자 승인·구현·QA가 완료되지 않은 실습 요청임을 구분한다.']})
    assert result['ok']
    package=target/C.DIRECTORY;package.mkdir(parents=True)
    for name in C.FILES:shutil.copyfile(template/name,package/name)
    contract=E.read(package/'contract.json');contract['sr']=args.sr;E.write(package/'contract.json',contract)
    build_clarify.write_decided(str(ws),args.sr,['DB_CHANGE'],by=actor,basis='DB 변경 검토용 미승인 실습')
    build_clarify.build(str(ws),args.sr)
    E.capture(str(ws),args.sr)
    projection=W.project(str(ws),args.sr)
    assert projection['schema_change'] and projection['evidence']['ok']
    row=(R.load_ledger(str(ws))['srs'][args.sr])
    assert row['status']=='RECEIVED' and not any(h.get('to')=='APPROVED' for h in row.get('history',[]))
    for path,sha in previous.items():
        if path==ledger_file:continue
        assert path.is_file() and hashlib.sha256(path.read_bytes()).hexdigest()==sha, 'An existing request file changed'
    assert ledger_file.read_bytes().startswith(original_ledger),'Existing ledger records were rewritten'
    print(json.dumps({'sr':args.sr,'status':row['status'],'approved':False,'model_calls':0,'schema_writes':0,
                      'existing_request_files_preserved':len(previous),'schema_review_available':True}))


if __name__=='__main__':main()
