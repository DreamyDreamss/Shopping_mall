#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""세팅·기동 스크립트의 계약 회귀.

speclinker에서 테스트베드를 분리하며(2026-09-22) 함께 옮겨 온 회귀다 — 스크립트가 이 저장소로
왔으니 그 계약을 지키는 테스트도 여기 있어야 한다. 각 단언은 서버 실측에서 나온 것이고,
주석의 날짜가 그 사고 날짜다.
"""
import io
import os
import re

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def _read(name):
    # .ps1은 BOM으로 시작한다(PowerShell 5.1이 BOM 없는 UTF-8의 한글을 ANSI로 읽는다) — utf-8-sig로 읽는다
    return io.open(os.path.join(REPO, name), encoding='utf-8-sig').read()


def test_builds_spa_before_the_jar_that_embeds_it():
    """shop-api jar가 shop-web/dist를 /shop으로 싣는다. dist는 git 밖이라 SR 뒤 재빌드하지 않으면 옛 화면이 실린다
    (2026-09-19: 9/17 dist에 쇼핑 홈이 없어 첫 화면이 주문 목록으로 떨어졌다)."""
    for name, spa, jar in (('setup.ps1', 'npm run build --silent', 'mvn.cmd'),
                           ('setup-linux.sh', 'npm run build --silent', 'mvn -q -DskipTests package'),
                           ('run-app.ps1', 'npm run build --silent', 'mvn.cmd'),
                           ('run-app.sh', 'npm run build --silent', 'mvn -q -DskipTests package')):
        src = _read(name)
        assert spa in src, f'{name}: SPA를 빌드하지 않는다'
        # jar 빌드 명령의 마지막 등장(실제 package 호출)이 SPA 빌드 뒤여야 한다
        assert src.index(spa) < src.rindex(jar), f'{name}: jar를 SPA보다 먼저 만든다'


def test_pipes_are_sigpipe_safe():
    """2026-09-16 서버 실측 — `tar | head -1`의 SIGPIPE가 pipefail로 전파돼 설치가 **메시지 없이** 죽었다."""
    for name in ('setup-linux.sh', 'run-app.sh'):
        src = _read(name)
        if 'set -euo pipefail' not in src:
            continue
        for ln in src.splitlines():
            if re.search(r'\|\s*head\b', ln) and not ln.lstrip().startswith('#'):
                assert '|| true' in ln, f'{name}: SIGPIPE로 죽을 수 있는 파이프 — {ln.strip()[:80]}'


def test_db_setup_keeps_the_hard_won_mariadb_flags():
    """2026-09-16 Azure Ubuntu 24.04 실측 — skip-name-resolve가 없으면 TCP root 접속이 ERROR 1698로 막히고,
    lower_case_table_names=1이 없으면 Windows 덤프의 `members`와 앱 SQL의 `MEMBERS`가 다른 테이블이 된다."""
    setup = _read('setup-linux.sh')
    assert 'skip-name-resolve' in setup
    assert 'lower_case_table_names=1' in setup
    # DB 검증은 앱·MCP가 실제로 쓰는 경로(TCP)로 해야 한다 — sudo 소켓 접속만 보면 뒤 단계가 전부 헛돈다
    assert 'TCP(127.0.0.1:3307)' in setup


def test_setup_is_one_shot():
    """시연 준비 — 마지막 단계(앱·브리지 기동)가 수동 명령이면 "한 번에 세팅"이 아니다."""
    setup = _read('setup-linux.sh')
    assert '--start)   START=1' in setup or '--start) START=1' in setup
    assert 'run-app.sh' in setup and 'serve-speclens.sh' in setup


def test_no_absolute_paths_are_baked_in():
    """분리의 요지 — 두 저장소는 project.env로만 물린다. 경로가 박히면 다음 환경에서 깨진다."""
    bad = re.compile(r'[A-Za-z]:[\\/](?:gen-harness|sl-shop|sl-lab|speclinker)')
    for name in ('setup-linux.sh', 'setup.ps1', 'run-app.sh', 'run-app.ps1', 'README.md'):
        src = _read(name)
        for ln in src.splitlines():
            s = ln.strip()
            if s.startswith('#') or s.startswith('Write-Host'):
                continue          # 주석·사용법 예시는 사람이 읽는 것이라 예외
            assert not bad.search(ln), f'{name}: 절대 경로가 박혀 있다 — {s[:80]}'


def test_seed_uses_tokens_not_absolute_paths():
    """seed/ 는 토큰으로 담고 setup이 실제 경로로 치환한다. 절대 경로가 섞이면 다음 환경에서 어긋난다."""
    seed = os.path.join(REPO, 'seed')
    exts = {'.md', '.json', '.jsonl', '.txt', '.html', '.yaml', '.yml', '.csv', '.xml'}
    offenders = []
    for base, _dirs, files in os.walk(seed):
        for f in files:
            if os.path.splitext(f)[1].lower() not in exts:
                continue
            p = os.path.join(base, f)
            try:
                t = io.open(p, encoding='utf-8').read()
            except (UnicodeDecodeError, OSError):
                continue
            if re.search(r'[A-Za-z]:[\\/](?:gen-harness|sl-shop|sl-lab|speclinker)', t):
                offenders.append(os.path.relpath(p, REPO))
    assert not offenders, '절대 경로가 남은 seed 파일:\n  ' + '\n  '.join(offenders[:10])


def test_setup_substitutes_every_token_the_seed_uses():
    """치환 규칙이 빠지면 워크스페이스에 `{{...}}`가 그대로 남아 화면이 깨진 경로를 보여 준다."""
    seed = os.path.join(REPO, 'seed')
    used = set()
    exts = {'.md', '.json', '.jsonl', '.txt', '.html', '.yaml', '.yml', '.csv', '.xml'}
    for base, _dirs, files in os.walk(seed):
        for f in files:
            if os.path.splitext(f)[1].lower() not in exts:
                continue
            try:
                t = io.open(os.path.join(base, f), encoding='utf-8').read()
            except (UnicodeDecodeError, OSError):
                continue
            used.update(re.findall(r'\{\{[A-Z_]+\}\}', t))
    assert used, 'seed에 토큰이 하나도 없다 — 토큰화가 빠졌는가?'
    for script in ('setup-linux.sh', 'setup.ps1'):
        src = _read(script)
        for tok in sorted(used):
            assert tok in src, f'{script}: {tok} 를 치환하지 않는다'


def test_seed_carries_the_core_artifacts():
    """설계서가 seed에 없으면 SpecLens 화면이 **비어서 뜬다** — 테스트베드의 존재 이유가 사라진다.

    2026-09-22 실측: 분리 1차에서 `docs/05_설계서`·`00_FUNC`·테스트 산출물을 통째로 빠뜨렸고,
    서버에 올리고 나서야 `docs/05_설계서` 0건으로 드러났다. 건수까지 본다 — 디렉토리만 있고
    비어 있으면 같은 증상이기 때문이다."""
    seed = os.path.join(REPO, 'seed')
    need = {
        '05_설계서': 20,          # INF·SCH·UIS spec.md (common 3 · member 5 · order 12)
        '00_FUNC': 1,
        '09_납품': 21,
        '변경관리': 1,
        'ws': 1,
    }
    for name, least in need.items():
        d = os.path.join(seed, name)
        assert os.path.isdir(d), f'seed/{name} 이 없다'
        n = sum(len(fs) for _r, _d, fs in os.walk(d))
        assert n >= least, f'seed/{name} 파일 {n}건 — {least}건 이상이어야 한다'
    specs = [f for _r, _d, fs in os.walk(os.path.join(seed, '05_설계서')) for f in fs if f == 'spec.md']
    assert len(specs) >= 20, f'설계서 spec.md {len(specs)}건'


def test_setup_expands_seed_by_rule_not_by_a_name_list():
    """전개를 이름 목록으로 두면 seed에 담고도 빠뜨린다(실제로 그랬다) — 규칙으로 돈다."""
    for name in ('setup-linux.sh', 'setup.ps1'):
        s = _read(name)
        assert 'seed' in s
        # 이름을 하나씩 적은 복사문이 남아 있으면 안 된다
        assert '09_납품"' not in s.replace('docs/09_납품', ''), f'{name}: 이름 목록 복사가 남아 있다'
    sh = _read('setup-linux.sh')
    assert 'for src in' in sh and 'basename' in sh, 'setup-linux.sh 가 규칙으로 돌지 않는다'
    ps = _read('setup.ps1')
    assert 'Get-ChildItem' in ps and 'switch ($src.Name)' in ps, 'setup.ps1 이 규칙으로 돌지 않는다'


def test_setup_generates_the_derived_index():
    """화면 목록은 파생 색인(`docs/viewer/spec_index.json`)을 읽는다 — 안 만들면 설계서가
    워크스페이스에 다 있어도 SpecLens가 **빈 목록**으로 뜬다(2026-09-22 서버 실측).

    전개(seed)만 하고 색인을 안 만드는 것이 바로 그 증상이었다."""
    for name in ('setup-linux.sh', 'setup.ps1'):
        s = _read(name)
        assert 'gen_spec_index.py' in s, f'{name}: 파생 색인을 만들지 않는다 — 화면 목록이 빈다'
    sh = _read('setup-linux.sh')
    i, j = sh.index('4-a 파생물 생성'), sh.index('5/5 빌드')
    assert i < j, '파생물 생성이 빌드 뒤에 있으면 seed 전개 직후 상태가 반영되지 않는다'


def test_setup_builds_the_whole_derived_chain():
    """색인 하나만 만들면 재생성·RECON이 죽는다 — 연쇄 전체를 세운다.

    2026-09-22 서버 실측: `regen-spec`이 `_tmp/screen_inventory_static.json` 없음으로 exit 1.
    연쇄는 scan_source.js → source_index.json → build_router_inventory.py →
    screen_inventory_static.json → gen_spec_index.py 인데 마지막 하나만 부르고 있었다."""
    for name in ('setup-linux.sh', 'setup.ps1'):
        s = _read(name)
        for step in ('scan_source.js', 'build_router_inventory.py', 'gen_spec_index.py'):
            assert step in s, f'{name}: {step} 를 부르지 않는다'
    sh = _read('setup-linux.sh')
    assert sh.index('scan_source.js') < sh.index('build_router_inventory.py') < sh.index('gen_spec_index.py'), \
        '연쇄 순서가 틀렸다 — 뒤 단계가 앞 산출물을 읽는다'
