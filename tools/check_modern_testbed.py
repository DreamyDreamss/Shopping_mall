import os
from pathlib import Path
import subprocess
import sys
import json
import shutil

plugin = Path('D:/speclinker-main')
ws = Path('D:/sl-shop')
out = Path('D:/sl-shop-runtime/setup')
env = os.environ.copy()
env['PYTHONUTF8'] = '1'
env['JAVA_HOME'] = 'C:/Program Files/Java/jdk-17'
env['PATH'] = env['JAVA_HOME'] + '/bin;D:/Shopping_mall/.runtime/apache-maven-3.9.9/bin;' + env['PATH']
commands = [
    ('package', ['D:/Shopping_mall/.runtime/apache-maven-3.9.9/bin/mvn.cmd', '-B', '-DskipTests', 'package'],
     Path('D:/Shopping_mall-modern/apps/shop-api')),
    ('tests', [sys.executable, '-X', 'utf8', str(plugin / 'scripts/run_tests.py'), str(ws), '--json'], ws),
]
for name, args, cwd in commands:
    with (out / (name + '.log')).open('wb') as log:
        r = subprocess.run(args, cwd=cwd, env=env, stdout=log, stderr=subprocess.STDOUT)
    print(name, r.returncode, flush=True)
    if r.returncode:
        raise SystemExit(r.returncode)

summary = json.loads((out / 'tests.log').read_text(encoding='utf-8'))
assert summary['status'] == 'passed' and len(summary['results']) == 2
sys.path.insert(0, str(plugin / 'scripts'))
import test_baseline_ws
previous = ws / '.speclinker/test_baseline.json'
if previous.exists() and not (out / 'baseline-before.json').exists():
    shutil.copy2(previous, out / 'baseline-before.json')
data = test_baseline_ws.record(str(ws), summary, force=True,
    reason='Independent current-source regression fixture; previous baseline archived. Backend review fixture isolation and native member authentication fixes included.',
    by='testbed-setup')
(out / 'baseline-current.json').write_text(json.dumps(data, ensure_ascii=False, indent=2), encoding='utf-8')
print('Current measured baseline recorded; no approval or QA gate changed.')
