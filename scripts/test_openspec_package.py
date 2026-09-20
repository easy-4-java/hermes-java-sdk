"""Regression tests for the limited offline package checker, not SDK tests."""
from pathlib import Path
import json
import shutil
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
CHECKER = ROOT / 'scripts/check-openspec-package.py'
CHANGE = 'harden-hermes-transport'

class OfflinePackageCheckerTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name) / 'package'
        shutil.copytree(ROOT, self.root, ignore=shutil.ignore_patterns('__pycache__', '*.pyc'))

    def run_checker(self):
        p = subprocess.run([sys.executable, str(CHECKER), '--root', str(self.root), '--json'], capture_output=True, text=True)
        try:
            payload = json.loads(p.stdout)
        except json.JSONDecodeError:
            payload = {'errors': [{'code': 'CHECKER_UNAVAILABLE', 'detail': p.stderr}]}
        return p.returncode, payload

    def assert_failure(self, code):
        rc, output = self.run_checker()
        self.assertEqual(rc, 1, output)
        self.assertIn(code, {e['code'] for e in output['errors']}, output)

    def test_valid_planning_package(self):
        rc, output = self.run_checker()
        self.assertEqual(rc, 0, output)
        self.assertEqual(output['scope'], 'limited-offline-document-check')
        self.assertEqual(output['counts']['requirements'], 53)
        self.assertEqual(output['counts']['scenarios'], 107)
        self.assertEqual(output['counts']['tasks'], 92)

    def test_missing_then_is_rejected(self):
        p = self.root / f'openspec/changes/{CHANGE}/specs/trusted-endpoints/spec.md'
        p.write_text(p.read_text().replace('- **THEN**', '- **RESULT**', 1))
        self.assert_failure('SCENARIO_SHAPE')

    def test_duplicate_requirement_id_is_rejected(self):
        p = self.root / f'openspec/changes/{CHANGE}/specs/trusted-endpoints/spec.md'
        p.write_text(p.read_text().replace('### Requirement: EP-002', '### Requirement: EP-001', 1))
        self.assert_failure('DUPLICATE_REQUIREMENT')

    def test_unowned_requirement_is_rejected(self):
        p = self.root / 'docs/openspec/traceability.json'
        doc = json.loads(p.read_text()); doc['requirements']['EP-001']['tasks'] = []
        p.write_text(json.dumps(doc, ensure_ascii=False))
        self.assert_failure('TRACE_TASK_MISSING')

    def test_checked_implementation_task_is_rejected_in_planning(self):
        p = self.root / f'openspec/changes/{CHANGE}/tasks.md'
        p.write_text(p.read_text().replace('- [ ]', '- [x]', 1))
        self.assert_failure('PREMATURE_COMPLETION')

    def test_dependency_cycle_is_rejected(self):
        p = self.root / 'docs/openspec/traceability.json'
        doc = json.loads(p.read_text()); doc['dependencies'][CHANGE] = ['add-hermes-acp-client']
        p.write_text(json.dumps(doc, ensure_ascii=False))
        self.assert_failure('DEPENDENCY_CYCLE')

    def test_broken_relative_link_is_rejected(self):
        p = self.root / 'docs/openspec/README.md'
        p.write_text(p.read_text()+'\n[invalid](does-not-exist.md)\n')
        self.assert_failure('BROKEN_LINK')

if __name__ == '__main__':
    unittest.main(verbosity=2)