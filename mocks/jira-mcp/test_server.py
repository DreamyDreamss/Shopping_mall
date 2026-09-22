#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""mocks/jira-mcp/server.py 회귀 — mcp-atlassian 호환 도구 4종의 동작·영속.

MCP 전송 계층(FastMCP stdio)은 SDK 소관이라 여기서는 도구 함수 자체를 검증한다
(저장소는 LAB_JIRA_STORE 환경변수로 격리)."""
import os
import json
import shutil
import tempfile
import importlib.util

HERE = os.path.dirname(os.path.abspath(__file__))
SERVER_PY = os.path.join(HERE, 'server.py')
ISSUES = os.path.join(HERE, 'issues.json')


def _load_server(store_path):
    os.environ['LAB_JIRA_STORE'] = store_path
    spec = importlib.util.spec_from_file_location('lab_jira_server', SERVER_PY)
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod


def test_search_get_transition_comment_roundtrip():
    tmp = tempfile.mkdtemp()
    try:
        store = os.path.join(tmp, 'issues.json')
        shutil.copy2(ISSUES, store)
        srv = _load_server(store)

        # search: status != Done → LAB-90(Done) 제외, labels = SR 필터 동작
        res = json.loads(srv.jira_search('project = LAB AND labels = SR AND status != Done', 10))
        keys = {i['key'] for i in res['issues']}
        assert 'LAB-101' in keys and 'LAB-102' in keys and 'LAB-90' not in keys, keys

        # get: 상세에 description·comments 포함
        issue = json.loads(srv.jira_get_issue('LAB-101'))
        assert '요구사항' in issue['description'] and isinstance(issue['comments'], list)

        # transition: 상태 변경 + 이력 + 영속
        out = json.loads(srv.jira_transition_issue('LAB-102', 'In Progress'))
        assert out['ok'] and out['to'] == 'In Progress'
        again = json.loads(srv.jira_get_issue('LAB-102'))
        assert again['status'] == 'In Progress'
        assert again['history'][-1]['from'] == 'To Do'

        # comment: 추가 + 영속(파일 재로드로 확인)
        srv.jira_add_comment('LAB-102', 'CIA 완료 — 영향 FUNC 2건')
        on_disk = json.load(open(store, encoding='utf-8'))
        it = next(i for i in on_disk['issues'] if i['key'] == 'LAB-102')
        assert it['comments'][-1]['body'].startswith('CIA 완료')

        # 미존재 키는 에러 JSON(예외 아님 — MCP 도구는 죽지 않는다)
        err = json.loads(srv.jira_get_issue('LAB-999'))
        assert 'error' in err
        print('PASS: test_search_get_transition_comment_roundtrip')
    finally:
        os.environ.pop('LAB_JIRA_STORE', None)
        shutil.rmtree(tmp, ignore_errors=True)


if __name__ == '__main__':
    test_search_get_transition_comment_roundtrip()
