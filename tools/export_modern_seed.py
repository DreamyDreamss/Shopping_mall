"""Export reproducible local seed data without runtime sessions, keys or job handles."""
from pathlib import Path
import datetime
import hashlib
import json
import shutil
import subprocess

ws = Path('D:/sl-shop')
root = Path('D:/sl-shop-runtime')
out = root / ('seed-current-' + datetime.datetime.now().strftime('%Y%m%d-%H%M%S'))
out.mkdir()
ignore = shutil.ignore_patterns('viewer', 'viewer2', '*.pid', '*.log', '*.bak', 'node_modules', '__pycache__')
for name in ('docs', '.claude', 'harness', 'tests', '_lab'):
    shutil.copytree(ws / name, out / name, ignore=ignore)
for name in ('CLAUDE.md', 'TESTBED_GUIDE.md', 'package.json', 'package-lock.json', 'playwright.config.ts', '.gitignore', '.gitattributes'):
    shutil.copy2(ws / name, out / name)
state = out / '.speclinker'
state.mkdir()
# Operational data only; no auth.json, local.key, API keys, storageState, chat DB,
# SQLite projection, process records, paid jobs or resumable sessions.
for name in ('approved', 'asis', 'rules_proposals', 'snapshots', 'document_drafts',
             'stage_gates.yaml', 'profile.yaml', 'test_baseline.json', 'db_facts.json',
             'db_fk_snapshot.json', 'func_registry.json', 'table_registry.json',
             'inf_targets.json', 'bat_targets.json', 'epic_notes.json',
             'source_integrity.jsonl', 'source_integrity_backups'):
    p = ws / '.speclinker' / name
    if p.is_dir(): shutil.copytree(p, state / name, ignore=ignore)
    elif p.is_file(): shutil.copy2(p, state / name)
text = (ws / 'project.env').read_text(encoding='utf-8')
text = text.replace('D:/speclinker-main', '{{PLUGIN_PATH}}').replace('D:/Shopping_mall-modern', '{{SOURCE_ROOT}}').replace('D:/sl-shop', '{{WS}}')
(out / 'project.env.template').write_text(text, encoding='utf-8')
(out / 'db').mkdir()
exclude = ['MEMBER_API_KEYS', 'MEMBER_REFRESH_TOKENS', 'MEMBER_LOGIN_ATTEMPTS',
           'MEMBER_SIGNUP_VERIFICATIONS', 'MEMBER_PASSWORD_RESETS', 'MEMBER_SIGNUP_RATE_LIMITS',
           'MEMBER_PASSWORD_RESET_RATE_LIMITS']
args = ['D:/Shopping_mall/.runtime/mariadb-11.4.5-winx64/bin/mariadb-dump.exe', '-h127.0.0.1', '-P13328', '-uroot',
        '--single-transaction', '--skip-lock-tables', '--hex-blob', '--default-character-set=utf8mb4']
args += ['--ignore-table-data=sl_shop_modern.' + name for name in exclude]
args += ['sl_shop_modern']
with (out / 'db/dump.sql').open('wb') as f:
    subprocess.run(args, stdout=f, check=True)
files = {p.relative_to(out).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in out.rglob('*') if p.is_file()}
metadata = {'at': datetime.datetime.now().isoformat(timespec='seconds'), 'workspace': str(ws),
            'source_commit': subprocess.check_output(['git', '-C', 'D:/Shopping_mall-modern', 'rev-parse', 'HEAD'], text=True).strip(),
            'plugin_commit': subprocess.check_output(['git', '-C', 'D:/speclinker-main', 'rev-parse', 'HEAD'], text=True).strip(),
            'files': files, 'database': 'sl_shop_modern', 'database_port': 13328,
            'session_table_data_excluded': exclude, 'historical_approvals_not_reissued': True,
            'restore_notes': 'Canonical historical document bytes preserved. Expand project.env.template; regenerate derived data; create a read-only MCP user. Import SQL into a NEW schema only. Log in again; no stored authenticated session is exported.'}
(out / 'manifest.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding='utf-8')
(root / 'setup/seed-export.json').write_text(json.dumps({'path': str(out), 'files': len(files),
    'manifest_sha256': hashlib.sha256((out / 'manifest.json').read_bytes()).hexdigest(),
    'no_runtime_sessions_exported': True}, indent=2), encoding='utf-8')
print(json.dumps({'path': str(out), 'files': len(files)}))
