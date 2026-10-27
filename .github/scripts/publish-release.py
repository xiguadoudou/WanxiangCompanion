"""Publish a pre-signed APK. No signing key or user token is stored in this repository."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

repo = os.environ['GITHUB_REPOSITORY']
assert repo == 'xiguadoudou/WanxiangCompanion', 'Wrong release repository'
m = json.loads(Path('updates/latest.json').read_text())
version = m['versionName']
assert re.fullmatch(r'\d+\.\d+\.\d+', version), 'Invalid version name'
assert m['packageName'] == 'com.doudou.wxq' and type(m['versionCode']) is int and m['versionCode'] > 0
name = f'WanxiangCompanion-{version}.apk'
apk = Path('updates') / name
blob = apk.read_bytes()
assert len(blob) == m['sizeBytes'], 'APK size mismatch'
assert hashlib.sha256(blob).hexdigest() == m['sha256'], 'APK SHA-256 mismatch'
assert m['apkUrl'] == f'https://github.com/{repo}/releases/download/v{version}/{name}'
assert m['apkFallbackUrls'] == [f'https://raw.githubusercontent.com/{repo}/main/updates/{name}']

def gh(*args, check=True):
    return subprocess.run(['gh', *args, '--repo', repo], check=check, text=True, capture_output=True)

# Re-runs leave an already-published version intact; mismatched assets fail loudly.
existing = gh('release', 'view', f'v{version}', '--json', 'isDraft,assets', check=False)
if existing.returncode == 0:
    info = json.loads(existing.stdout)
    if not info['isDraft']:
        assets = {a['name']: a for a in info['assets']}
        assert name in assets and 'latest.json' in assets, 'Published release is missing assets'
        assert assets[name]['size'] == len(blob), 'Published asset differs; bump the version'
        print(f'v{version} already published; leaving it intact')
        raise SystemExit(0)

with tempfile.TemporaryDirectory() as folder:
    notes = Path(folder) / 'notes.md'
    notes.write_text(m.get('notes', '') + '\n\nAndroid 8.0+，沿用原签名，可覆盖升级。\n')
    if existing.returncode != 0:
        gh('release', 'create', f'v{version}', '--target', os.environ['GITHUB_SHA'], '--title', f'万象弈伴 {version}', '--notes-file', str(notes), '--draft')
    gh('release', 'upload', f'v{version}', str(apk), 'updates/latest.json', '--clobber')
    gh('release', 'edit', f'v{version}', '--draft=false', '--latest')
print(f'Published https://github.com/{repo}/releases/tag/v{version}')
