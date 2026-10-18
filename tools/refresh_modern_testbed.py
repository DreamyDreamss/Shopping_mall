"""Rebuild actual derived data, without marking historical designs newly approved."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys

ap = argparse.ArgumentParser()
ap.add_argument('--workspace', type=Path, default=Path('D:/sl-shop'))
ap.add_argument('--plugin', type=Path, default=Path('D:/speclinker-main'))
ap.add_argument('--runtime', type=Path, default=Path('D:/sl-shop-runtime'))
a = ap.parse_args()
ws, plugin, runtime = a.workspace.resolve(), a.plugin.resolve(), a.runtime.resolve()
out = runtime / 'setup'
out.mkdir(exist_ok=True, parents=True)
env = os.environ.copy()
env['PYTHONUTF8'] = '1'
env['JAVA_HOME'] = 'C:/Program Files/Java/jdk-17'
env['PATH'] = env['JAVA_HOME'] + '/bin;D:/Shopping_mall/.runtime/apache-maven-3.9.9/bin;' + env['PATH']
commands = [
    ('migrate', [sys.executable, '-X', 'utf8', str(plugin / 'scripts/ws_migrate.py'), str(ws), '--apply', '--json']),
    ('source-scan', ['node', str(plugin / 'scripts/scan_source.js'), str(ws)]),
    ('router', [sys.executable, str(plugin / 'scripts/build_router_inventory.py'), str(ws)]),
    ('kg', [sys.executable, str(plugin / 'scripts/build_kg_summary.py'), str(ws)]),
    ('funcs', [sys.executable, str(plugin / 'scripts/build_funcs_index.py'), str(ws)]),
    ('table-registry', [sys.executable, str(plugin / 'scripts/build_table_registry.py'), str(ws)]),
    ('derived', [sys.executable, str(plugin / 'scripts/refresh_derived.py'), str(ws), '--apply', '--json']),
    ('component-map', [sys.executable, str(plugin / 'scripts/build_component_map.py'), str(ws)]),
    ('glossary', [sys.executable, str(plugin / 'scripts/build_glossary.py'), str(ws)]),
]
results = []
for name, command in commands:
    with (out / (name + '.log')).open('wb') as log:
        r = subprocess.run(command, cwd=ws, env=env, stdout=log, stderr=subprocess.STDOUT)
    results.append({'name': name, 'exit_code': r.returncode})
    (out / 'refresh.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
    print(name, r.returncode, flush=True)
    if r.returncode:
        raise SystemExit(r.returncode)
