#!/usr/bin/env bash
# Runs the installed official CLI. Does not install dependencies, archive,
# mark tasks complete, modify SDK source, or equate status with implementation.
set -euo pipefail
ROOT="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
exec python3 - "$ROOT" <<'PY'
import datetime
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys

root = Path(sys.argv[1]).resolve()
if not (root / 'openspec/config.yaml').is_file():
    print('ERROR: openspec/config.yaml is missing.', file=sys.stderr)
    sys.exit(2)
cli = shutil.which('openspec')
if not cli:
    print('NOT_RUN: official OpenSpec CLI is not installed or is not on PATH.', file=sys.stderr)
    print('No official validation or status command was executed.', file=sys.stderr)
    sys.exit(2)

stamp = datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%S.%fZ')
out = root / 'verification/openspec/official' / stamp
out.mkdir(parents=True, exist_ok=False)
env = os.environ.copy()
env['DO_NOT_TRACK'] = '1'
env['OPENSPEC_NO_UPDATE_CHECK'] = '1'
changes = (
    'harden-hermes-transport',
    'complete-hermes-http-runtime',
    'enhance-hermes-cli-runtime',
    'add-hermes-acp-client',
)
commands = [('version', ['--version']),
            ('strict-validation', ['validate', '--all', '--strict', '--no-interactive', '--json'])]
commands += [('status-' + name, ['status', '--change', name, '--json']) for name in changes]
summary = {'scope': 'official-openspec-cli-commands',
           'planned_cli_version': '1.13.1', 'executable': cli,
           'sdk_tests_executed': False, 'commands': []}
result_code = 0
for label, args in commands:
    command = [cli] + args
    print('+ openspec ' + ' '.join(args), flush=True)
    with (out / (label + '.stdout.log')).open('wb') as stdout, \
         (out / (label + '.stderr.log')).open('wb') as stderr:
        try:
            completed = subprocess.run(command, cwd=root, env=env,
                                       stdin=subprocess.DEVNULL,
                                       stdout=stdout, stderr=stderr, timeout=180)
            code = completed.returncode
        except subprocess.TimeoutExpired:
            code = 124
            stderr.write(b'Wrapper timeout after 180 seconds.\n')
        except OSError as ex:
            code = 126
            stderr.write((str(ex) + '\n').encode('utf-8', errors='replace'))
    summary['commands'].append({'command': ['openspec'] + args,
                                'exit_code': code,
                                'stdout': label + '.stdout.log',
                                'stderr': label + '.stderr.log'})
    if code != 0:
        result_code = code if 0 < code < 256 else 1
        print('FAILED: see ' + str(out / (label + '.stderr.log')), file=sys.stderr)
        break
summary['all_commands_succeeded'] = len(summary['commands']) == len(commands) and result_code == 0
summary['note'] = 'Artifact status is not SDK implementation or test completion.'
(out / 'summary.json').write_text(json.dumps(summary, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print('Official command records: ' + str(out))
sys.exit(result_code)
PY