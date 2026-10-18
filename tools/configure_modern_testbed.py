"""Local, backed-up migration of the accumulated shop testbed to current SpecLens.

Run once with explicit paths; never deletes an existing workspace or database.
Generated runtime contains local credentials and must not be committed.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import time


def manifest(root):
    return {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest()
            for p in root.rglob('*') if p.is_file() and '.git' not in p.parts}


def main():
    ap = argparse.ArgumentParser()
    for key in ('plugin', 'source', 'legacy', 'workspace', 'runtime', 'mariadb'):
        ap.add_argument('--' + key, type=Path, required=True)
    a = ap.parse_args()
    a = argparse.Namespace(**{k: v.resolve() for k, v in vars(a).items()})
    if not (a.plugin / 'scripts/request_workflow.py').is_file():
        raise SystemExit('Current plugin required')
    if not (a.legacy / 'project.env').is_file() or a.workspace == a.legacy:
        raise SystemExit('Separate accumulated workspace required')
    if a.runtime == a.workspace or a.runtime.is_relative_to(a.workspace):
        raise SystemExit('Runtime/backup must be outside destination')
    if a.workspace.parent != Path('D:/').resolve() or a.workspace.name != 'sl-shop':
        raise SystemExit('This local migration only accepts D:/sl-shop as destination')
    a.runtime.mkdir(exist_ok=True, parents=True)
    proof = a.runtime / 'setup'
    proof.mkdir(exist_ok=True)
    if (proof / 'workspace-ready.json').exists():
        raise SystemExit('Already configured; use refresh-derived.ps1 to refresh, not reset')
    backup = a.runtime / ('sl-shop-before-' + time.strftime('%Y%m%d-%H%M%S'))
    before = manifest(a.workspace) if a.workspace.exists() else {}
    # Native rename, resolved exact target verified above; no recursive deletion.
    if a.workspace.exists():
        a.workspace.rename(backup)
        assert manifest(backup) == before
    ignore = shutil.ignore_patterns('.git', 'node_modules', '_tmp', '_sb-check',
                                   '.sr-worktrees', '__pycache__', 'viewer2', '*.pid', 'bridge.json')
    shutil.copytree(a.legacy, a.workspace, ignore=ignore)
    ws, src, plugin = a.workspace.as_posix(), a.source.as_posix(), a.plugin.as_posix()
    maven = 'D:/Shopping_mall/.runtime/apache-maven-3.9.9/bin/mvn.cmd'
    java = 'C:/Program Files/Java/jdk-17/bin/java.exe'
    dburl = 'jdbc:mariadb://127.0.0.1:13328/sl_shop_modern?useUnicode=true&characterEncoding=utf8'
    env = f'''# Current local testbed; historical records remain historical, not new approvals.
PROJECT_NAME=sl-shop
NETWORK=open
PLUGIN_PATH={plugin}
SOURCE_COUNT=2
SOURCE_1_LABEL=shop-api
SOURCE_1_PATH={src}/apps/shop-api
SOURCE_2_LABEL=shop-web
SOURCE_2_PATH={src}/apps/shop-web
PREVIEW_BASE_URL=http://127.0.0.1:18985/shop
MCP_DB_MARIADB=true
MCP_DB_mariadb=true
E2E_BASE_URL_local=http://127.0.0.1:18985
API_TRY_BASE=http://127.0.0.1:18985
APP_RUN_CMD="{maven}" -q spring-boot:run -Dspring-boot.run.arguments="--server.address=127.0.0.1 --server.port={{PORT}} --spring.datasource.url={dburl} --spring.datasource.username=root --spring.datasource.password= --spring.sql.init.mode=never"
APP_HEALTH_PATH=/shop/
APP_READY_TIMEOUT=240
STORYBOOK_SOURCE=shop-web
STORYBOOK_RUN_CMD=npm run storybook -- -p {{PORT}}
STORYBOOK_STATIC=storybook-static
STORYBOOK_BUILD_CMD=npm run build-storybook
STORYBOOK_TEST_CMD=npm run test-storybook:ci
SNAP_BASE_URL=http://127.0.0.1:18985
SNAP_AUTH_HEADER=X-Api-Key: lab-admin-key
IDE_KIND=vscode
IDE_PATH={ws}
TEST_ENV_shop-api=SPRING_DATASOURCE_URL={dburl.replace('/sl_shop_modern?', '/sl_shop_modern_test?')}
E2E_ENV=local
E2E_WRITE_local=allow
E2E_LOGIN_MODE=storage
E2E_CDP_PORT=19225
E2E_STORAGE_STATE=.speclinker/e2e_storage.json
'''
    (a.workspace / 'project.env').write_text(env, encoding='utf-8')
    # Remove active execution overrides only in the new copy, retaining originals in backup.
    for name in ('project.local.env',):
        p = a.workspace / name
        if p.exists():
            p.rename(p.with_suffix(p.suffix + '.historical'))
    config = {'mcpServers': {
        'mariadb': {'command': sys.executable, 'args': [plugin + '/mcp-servers/mariadb_schema_server.py'],
                    'env': {'MDB_HOST': '127.0.0.1', 'MDB_PORT': '13328', 'MDB_DATABASE': 'sl_shop_modern',
                            'MDB_USER': 'speclens_modern_ro', 'MDB_PASSWORD': ''}},
        'mcp-atlassian': {'command': sys.executable, 'args': [src + '/mocks/jira-mcp/server.py'],
                         'env': {'LAB_JIRA_STORE': ws + '/_lab/jira_issues.json'}}}}
    (a.workspace / '.mcp.json').write_text(json.dumps(config, ensure_ascii=False, indent=2), encoding='utf-8')
    data = a.runtime / 'mariadb-data'
    if data.exists():
        raise SystemExit('Refusing to overwrite existing database directory')
    with (proof / 'db-init.log').open('wb') as log:
        subprocess.run([str(a.mariadb / 'bin/mariadb-install-db.exe'), '--datadir=' + str(data),
                        '--port=13328'], stdout=log, stderr=subprocess.STDOUT, check=True)
    ini = data / 'my.ini'
    ini.write_text('[mysqld]\nbasedir=' + a.mariadb.as_posix() + '\ndatadir=' + data.as_posix() +
                   '\nport=13328\nbind-address=127.0.0.1\n[client]\nport=13328\n', encoding='utf-8')
    with (proof / 'db-server.log').open('ab') as log:
        proc = subprocess.Popen([str(a.mariadb / 'bin/mariadbd.exe'), '--defaults-file=' + str(ini), '--console'],
                                stdout=log, stderr=subprocess.STDOUT,
                                creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
    (proof / 'workspace-ready.json').write_text(json.dumps({
        'workspace': ws, 'source': src, 'plugin': plugin, 'backup': str(backup),
        'original_files': len(before), 'original_hashes': before, 'db_pid': proc.pid,
        'database': 'sl_shop_modern', 'database_port': 13328,
        'historical_source': str(a.legacy), 'status': 'workspace copied; database import/derived data pending'
    }, ensure_ascii=False, indent=2), encoding='utf-8')
    print(json.dumps({'workspace': ws, 'backup': str(backup), 'preserved_files': len(before), 'db_pid': proc.pid}))


if __name__ == '__main__':
    main()
