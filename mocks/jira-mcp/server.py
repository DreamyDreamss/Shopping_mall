#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""lab/jira-mcp/server.py — 로컬 지라 MCP (mcp-atlassian 도구 호환 서브셋).

실증 랩용: 실제 Atlassian 없이 sl-change/sl-viewer의 지라 플로우(인입 검색→이슈 조회→
상태 전이→댓글)를 로컬에서 실증한다. 도구 이름·응답 필드는 스킬이 참조하는 형태
(jira_search / jira_get_issue / jira_transition_issue / jira_add_comment)를 따른다.

저장소: LAB_JIRA_STORE 환경변수(json 파일). 없으면 서버 옆 issues.json을 읽기 전용으로 쓴다
— 전이·댓글을 영속하려면 워크스페이스 사본을 가리키게 하라(setup.ps1이 배선).
"""
import json
import os
import datetime

from mcp.server.fastmcp import FastMCP

HERE = os.path.dirname(os.path.abspath(__file__))
STORE = os.environ.get('LAB_JIRA_STORE') or os.path.join(HERE, 'issues.json')

mcp = FastMCP('lab-jira', instructions='로컬 지라(실증 랩) — SR 인입·전이·댓글을 파일 저장소로 재현')


def _load():
    with open(STORE, encoding='utf-8-sig') as f:
        return json.load(f)


def _save(data):
    tmp = STORE + '.tmp'
    with open(tmp, 'w', encoding='utf-8', newline='\n') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)
    os.replace(tmp, STORE)


def _now():
    return datetime.datetime.now().isoformat(timespec='seconds')


def _summary_view(issue):
    return {k: issue.get(k) for k in
            ('key', 'summary', 'status', 'priority', 'labels', 'assignee', 'updated')}


@mcp.tool()
def jira_search(jql: str = '', limit: int = 20) -> str:
    """JQL(부분 지원: status != Done / labels = SR / assignee 토큰의 소박한 매칭)로 이슈를 검색한다.
    반환: 이슈 요약 목록 JSON."""
    data = _load()
    issues = data.get('issues', [])
    q = (jql or '').lower()
    out = []
    for it in issues:
        if 'status != done' in q and (it.get('status') or '').lower() == 'done':
            continue
        if 'labels = sr' in q and 'SR' not in (it.get('labels') or []):
            continue
        out.append(_summary_view(it))
        if len(out) >= max(1, int(limit)):
            break
    return json.dumps({'total': len(out), 'issues': out}, ensure_ascii=False, indent=2)


@mcp.tool()
def jira_get_issue(issue_key: str) -> str:
    """이슈 상세 — summary·description·comment 전체·priority·assignee·labels·status."""
    data = _load()
    for it in data.get('issues', []):
        if it.get('key') == issue_key:
            return json.dumps(it, ensure_ascii=False, indent=2)
    return json.dumps({'error': f'이슈 없음: {issue_key}'}, ensure_ascii=False)


@mcp.tool()
def jira_transition_issue(issue_key: str, transition: str) -> str:
    """이슈 상태를 전이한다(랩: 전이명 = 목표 상태명으로 취급, 이력 기록)."""
    data = _load()
    for it in data.get('issues', []):
        if it.get('key') == issue_key:
            prev = it.get('status')
            it['status'] = transition
            it['updated'] = _now()
            it.setdefault('history', []).append(
                {'at': _now(), 'from': prev, 'to': transition, 'by': 'speclinker'})
            _save(data)
            return json.dumps({'ok': True, 'key': issue_key, 'from': prev, 'to': transition},
                              ensure_ascii=False)
    return json.dumps({'error': f'이슈 없음: {issue_key}'}, ensure_ascii=False)


@mcp.tool()
def jira_add_comment(issue_key: str, comment: str) -> str:
    """이슈에 댓글을 추가한다(영속)."""
    data = _load()
    for it in data.get('issues', []):
        if it.get('key') == issue_key:
            it.setdefault('comments', []).append(
                {'at': _now(), 'author': 'speclinker', 'body': comment})
            it['updated'] = _now()
            _save(data)
            return json.dumps({'ok': True, 'key': issue_key,
                               'comments': len(it['comments'])}, ensure_ascii=False)
    return json.dumps({'error': f'이슈 없음: {issue_key}'}, ensure_ascii=False)


if __name__ == '__main__':
    mcp.run()
