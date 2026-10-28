import importlib.util
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch
import zipfile

REPO = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(REPO / '.github/scripts'))
from source_package import package_source, source_files

spec = importlib.util.spec_from_file_location('publisher', REPO / '.github/scripts/publish-release.py')
publisher = importlib.util.module_from_spec(spec)
spec.loader.exec_module(publisher)


class SourceReleaseTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.work = Path(self.tmp.name)
        shutil.copytree(REPO / 'android', self.work / 'android', ignore=shutil.ignore_patterns('build', 'dist', 'signing', 'node_modules', '__pycache__'))
        self.manifest = json.loads((REPO / 'updates/latest.json').read_text())
        self.name = 'WanxiangCompanion-' + self.manifest['versionName'] + '.apk'
        (self.work / 'updates').mkdir()
        (self.work / 'docs').mkdir()
        shutil.copyfile(REPO / 'updates' / self.name, self.work / 'updates' / self.name)
        shutil.copyfile(REPO / 'updates/latest.json', self.work / 'updates/latest.json')
        shutil.copyfile(REPO / 'docs/latest.json', self.work / 'docs/latest.json')
        self.old = Path.cwd()
        os.chdir(self.work)
        self.remote = {self.name: Path('updates', self.name).read_bytes(), 'WanxiangCompanion.apk': Path('updates', self.name).read_bytes(), 'latest.json': Path('updates/latest.json').read_bytes()}
        self.calls = []
        self.existing = True

    def tearDown(self):
        os.chdir(self.old)
        self.tmp.cleanup()

    def bundle(self, name='source.zip'):
        return package_source('android', self.manifest, Path('updates', self.name), self.work / name)

    def gh(self, cmd, **kwargs):
        self.calls.append(cmd)
        action = cmd[1:3]
        if action == ['release', 'view']:
            if not self.existing:
                return subprocess.CompletedProcess(cmd, 1, '', 'not found')
            info = {'isDraft': False, 'assets': [{'name': n, 'size': len(b)} for n, b in self.remote.items()]}
            return subprocess.CompletedProcess(cmd, 0, json.dumps(info), '')
        if action == ['release', 'download']:
            name = cmd[cmd.index('--pattern') + 1]
            destination = Path(cmd[cmd.index('--dir') + 1]) / name
            destination.write_bytes(self.remote[name])
        if action == ['release', 'upload']:
            for name in cmd[4:cmd.index('--repo')]:
                if name == '--clobber':
                    continue
                path = Path(name)
                self.remote[path.name] = path.read_bytes()
        return subprocess.CompletedProcess(cmd, 0, '', '')

    def publish(self):
        with patch.dict(os.environ, {'GITHUB_REPOSITORY': 'xiguadoudou/WanxiangCompanion', 'GITHUB_SHA': 'test-head'}), patch('subprocess.run', self.gh):
            publisher.main()

    def test_deterministic_bundle_and_secret_exclusion(self):
        Path('android/signing').mkdir()
        Path('android/signing/private.jks').write_bytes(b'private-test-data')
        Path('android/.env').write_text('PRIVATE=test')
        a = self.bundle('first.zip').read_bytes()
        b = self.bundle('second.zip').read_bytes()
        self.assertEqual(a, b)
        with zipfile.ZipFile(self.work / 'first.zip') as z:
            names = z.namelist()
            self.assertTrue(any(n.endswith('MainActivity.java') for n in names))
            self.assertFalse(any('/signing/' in n or n.endswith('.env') or n.endswith('.jks') for n in names))
            receipt = json.loads(z.read(next(n for n in names if n.endswith('/SOURCE.json'))))
            self.assertEqual(receipt['apkSha256'], self.manifest['sha256'])

    def test_local_bundle_rejects_private_and_escape_paths(self):
        target = Path('android/assets/workshop/site.zip')
        target.parent.mkdir(parents=True, exist_ok=True)
        for name in ['../outside.js', '.env.js', 'signing/password.txt', 'nested.zip', 'C:\\private.js']:
            with zipfile.ZipFile(target, 'w') as archive:
                archive.writestr(name, b'test-only')
            with self.assertRaisesRegex(ValueError, 'Private/generated'):
                source_files('android')

    def test_reject_stale_source_version_before_publication(self):
        p = Path('android/AndroidManifest.xml')
        p.write_text(p.read_text().replace('versionName="' + self.manifest['versionName'] + '"', 'versionName="0.0.0"'))
        with self.assertRaisesRegex(AssertionError, 'versionName'):
            self.publish()
        self.assertEqual(self.calls, [])

    def test_reject_asset_drift_and_embedded_signing_material(self):
        p = Path('android/assets/app.js')
        p.write_text(p.read_text() + '\n// unbuilt asset\n')
        with self.assertRaisesRegex(AssertionError, 'asset differs'):
            self.bundle()
        Path('android/tests/private.jks').write_bytes(b'test-only-key')
        with self.assertRaisesRegex(ValueError, 'Private/generated'):
            self.bundle()

    def test_backfill_existing_release_preserves_apks_and_is_idempotent(self):
        originals = self.remote.copy()
        self.publish()
        for name, blob in originals.items():
            self.assertEqual(self.remote[name], blob)
        uploads = [c for c in self.calls if c[1:3] == ['release', 'upload']]
        self.assertEqual(len(uploads), 2)
        self.assertTrue(all(c[4].endswith('-source.zip') for c in uploads))
        self.assertEqual(self.remote['WanxiangCompanion-source.zip'], self.remote['WanxiangCompanion-' + self.manifest['versionName'] + '-source.zip'])
        self.calls.clear()
        self.publish()
        self.assertFalse(any(c[1:3] == ['release', 'upload'] for c in self.calls))

    def test_published_source_is_immutable(self):
        self.publish()
        p = Path('android/src/com/doudou/wxq/MainActivity.java')
        p.write_text(p.read_text() + '\n// changed after release\n')
        self.calls.clear()
        with self.assertRaisesRegex(AssertionError, 'Published asset differs'):
            self.publish()
        self.assertFalse(any(c[1:3] == ['release', 'upload'] for c in self.calls))

    def test_pages_feed_must_match(self):
        Path('docs/latest.json').write_text('{}')
        with self.assertRaisesRegex(AssertionError, 'Pages update feed differs'):
            self.publish()
        self.assertEqual(self.calls, [])

    def test_new_release_requires_both_apk_and_source(self):
        self.existing = False
        self.remote.clear()
        self.publish()
        self.assertEqual(set(self.remote), {self.name, 'WanxiangCompanion.apk', 'latest.json', 'WanxiangCompanion-source.zip', 'WanxiangCompanion-' + self.manifest['versionName'] + '-source.zip'})
        self.assertTrue(any(c[1:3] == ['release', 'edit'] for c in self.calls))


if __name__ == '__main__':
    unittest.main()
