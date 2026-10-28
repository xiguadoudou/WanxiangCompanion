"""Package the public Android tree, never the private build workspace."""
import hashlib
import json
from pathlib import Path
import re
import xml.etree.ElementTree as ET
import zipfile

ROOT_FILES = ('AndroidManifest.xml', 'build.sh', 'README.md')
SOURCE_DIRS = ('src', 'assets', 'res', 'tests', 'tools')
ANDROID_NS = '{http://schemas.android.com/apk/res/android}'
FORBIDDEN_SUFFIXES = {'.jks', '.keystore', '.p12', '.pfx', '.pem', '.key', '.apk', '.zip', '.pyc'}


def source_files(root):
    root = Path(root)
    files = [root / name for name in ROOT_FILES]
    for folder in SOURCE_DIRS:
        directory = root / folder
        if directory.is_symlink():
            raise ValueError('Symlink in public source: ' + folder)
        for file in sorted(directory.rglob('*')):
            if file.is_symlink():
                raise ValueError('Symlink in public source: ' + str(file))
            if file.is_file():
                files.append(file)
    for file in files:
        rel = file.relative_to(root)
        if not file.is_file() or file.is_symlink():
            raise ValueError('Missing public source file: ' + str(rel))
        is_workshop = rel.as_posix() == 'assets/workshop/site.zip'
        if is_workshop:
            with zipfile.ZipFile(file) as bundle:
                names = bundle.namelist()
                if len(names) != len(set(names)) or len(names) > 700 or sum(i.file_size for i in bundle.infolist()) > 40000000:
                    raise ValueError('Invalid local module archive')
                for name in names:
                    path = Path(name)
                    if path.is_absolute() or '\\' in name or any(p in {'..','.git','signing','node_modules'} for p in path.parts) or path.name.startswith('.env') or path.suffix.lower() not in {'.js','.css','.json','.html','.png','.jpg','.webp','.svg','.woff2','.txt','.md'}:
                        raise ValueError('Private/generated file inside module archive: ' + name)
                    data = bundle.read(name)
                    if re.search(rb'-----BEGIN (?:[A-Z]+ )?PRIVATE KEY-----|github_pat_[A-Za-z0-9_]{20,}|gh[pousr]_[A-Za-z0-9]{20,}',data):
                        raise ValueError('Credential-like content inside module archive: ' + name)
        if any(p in {'signing', 'build', 'dist', 'node_modules', '__pycache__', '.git'} for p in rel.parts) or file.name.startswith('.env') or (file.suffix.lower() in FORBIDDEN_SUFFIXES and not is_workshop):
            raise ValueError('Private/generated file in public source: ' + str(rel))
        content = file.read_bytes()
        if re.search(rb'-----BEGIN (?:[A-Z]+ )?PRIVATE KEY-----|github_pat_[A-Za-z0-9_]{20,}|gh[pousr]_[A-Za-z0-9]{20,}', content):
            raise ValueError('Credential-like content in public source: ' + str(rel))
    return sorted(files, key=lambda p: p.relative_to(root).as_posix())


def validate_source(root, manifest, apk):
    root, apk = Path(root), Path(apk)
    files = source_files(root)
    attrs = ET.parse(root / 'AndroidManifest.xml').getroot().attrib
    assert attrs['package'] == manifest['packageName'], 'Source package differs from update manifest'
    assert attrs[ANDROID_NS + 'versionName'] == manifest['versionName'], 'Source versionName differs from APK release'
    assert int(attrs[ANDROID_NS + 'versionCode']) == manifest['versionCode'], 'Source versionCode differs from APK release'
    info = (root / 'src/com/doudou/wxq/BuildInfo.java').read_text()
    assert re.search(r'VERSION_NAME\s*=\s*"' + re.escape(manifest['versionName']) + r'"', info), 'BuildInfo versionName differs'
    assert re.search(r'VERSION_CODE\s*=\s*' + str(manifest['versionCode']) + r'\s*;', info), 'BuildInfo versionCode differs'
    assert (root / 'src/com/doudou/wxq/MainActivity.java').is_file(), 'Missing Android implementation'
    with zipfile.ZipFile(apk) as archive:
        packaged = {n for n in archive.namelist() if n.startswith('assets/') and not n.endswith('/')}
        supplied = {p.relative_to(root).as_posix() for p in files if p.is_relative_to(root / 'assets')}
        assert supplied == packaged, 'Source assets list differs from APK'
        for name in sorted(packaged):
            assert archive.read(name) == (root / name).read_bytes(), 'Source asset differs from APK: ' + name
    return files


def package_source(root, manifest, apk, output):
    root, output = Path(root), Path(output)
    files = validate_source(root, manifest, apk)
    hashes = {p.relative_to(root).as_posix(): hashlib.sha256(p.read_bytes()).hexdigest() for p in files}
    receipt = {'packageName': manifest['packageName'], 'versionName': manifest['versionName'], 'versionCode': manifest['versionCode'], 'apkSha256': manifest['sha256'], 'files': hashes}
    prefix = 'WanxiangCompanion-' + manifest['versionName'] + '/'
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, 'w', compression=zipfile.ZIP_DEFLATED, compresslevel=9) as archive:
        def write(name, data):
            info = zipfile.ZipInfo(prefix + name, date_time=(2020, 1, 1, 0, 0, 0))
            info.create_system = 3
            info.external_attr = (0o100755 if name == 'build.sh' else 0o100644) << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, data, compresslevel=9)
        for path in files:
            write(path.relative_to(root).as_posix(), path.read_bytes())
        write('SOURCE.json', (json.dumps(receipt, ensure_ascii=False, sort_keys=True, indent=2) + '\n').encode())
    return output
