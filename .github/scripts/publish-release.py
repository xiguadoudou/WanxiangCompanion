"""Publish matching pre-signed APK and public source; signing keys stay private."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile

from source_package import package_source


def main():
    repo = os.environ['GITHUB_REPOSITORY']
    assert repo == 'xiguadoudou/WanxiangCompanion', 'Wrong release repository'
    manifest_path = Path('updates/latest.json')
    m = json.loads(manifest_path.read_text())
    assert Path('docs/latest.json').read_bytes() == manifest_path.read_bytes(), 'Pages update feed differs from canonical manifest'
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

    with tempfile.TemporaryDirectory() as folder:
        folder = Path(folder)
        source = package_source('android', m, apk, folder / f'WanxiangCompanion-{version}-source.zip')
        stable_source = folder / 'WanxiangCompanion-source.zip'
        stable_source.write_bytes(source.read_bytes())
        stable = folder / 'WanxiangCompanion.apk'
        stable.write_bytes(blob)
        existing = gh('release', 'view', f'v{version}', '--json', 'isDraft,assets', check=False)
        if existing.returncode == 0:
            info = json.loads(existing.stdout)
            if not info['isDraft']:
                assets = {a['name']: a for a in info['assets']}

                def verify(path):
                    assert path.name in assets, 'Published release is missing ' + path.name
                    assert assets[path.name]['size'] == path.stat().st_size, 'Published asset differs; bump version: ' + path.name
                    download_dir = folder / 'existing'
                    download_dir.mkdir(exist_ok=True)
                    gh('release', 'download', f'v{version}', '--pattern', path.name, '--dir', str(download_dir), '--clobber')
                    assert (download_dir / path.name).read_bytes() == path.read_bytes(), 'Published asset differs; bump version: ' + path.name

                # Backfill source without replacing the released APKs or notes.
                for path in [apk, stable, manifest_path]:
                    verify(path)
                for path in [source, stable_source]:
                    if path.name in assets:
                        verify(path)
                    else:
                        gh('release', 'upload', f'v{version}', str(path))
                print(f'v{version}: original APKs preserved; matching source attachments available')
                return

        notes = folder / 'notes.md'
        notes.write_text(m.get('notes', '') + '\n\nAndroid 8.0+，沿用原签名，可覆盖升级。\n对应源码见本版本的 source.zip 附件；签名私钥不公开。\n')
        if existing.returncode != 0:
            gh('release', 'create', f'v{version}', '--target', os.environ['GITHUB_SHA'], '--title', f'万象弈伴 {version}', '--notes-file', str(notes), '--draft')
        gh('release', 'upload', f'v{version}', str(apk), str(stable), str(manifest_path), str(source), str(stable_source), '--clobber')
        gh('release', 'edit', f'v{version}', '--draft=false', '--latest')
    print(f'Published APK and source: https://github.com/{repo}/releases/tag/v{version}')


if __name__ == '__main__':
    main()
