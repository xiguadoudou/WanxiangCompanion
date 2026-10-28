#!/usr/bin/env bash
set -euo pipefail
umask 077
cd "$(dirname "$0")"
WXQ_SDK="${WXQ_SDK:-${ANDROID_HOME:?Set ANDROID_HOME or WXQ_SDK to the Android SDK directory}}"
WXQ_BT="${WXQ_BT:-$WXQ_SDK/build-tools/35.0.0}"
WXQ_PLATFORM="${WXQ_PLATFORM:-$WXQ_SDK/platforms/android-35/android.jar}"
WXQ_VERSION="$(python3 - <<'PY'
import xml.etree.ElementTree as ET
print(ET.parse('AndroidManifest.xml').getroot().attrib['{http://schemas.android.com/apk/res/android}versionName'])
PY
)"
WXQ_MODE=release
if [ -z "${WXQ_KEYSTORE:-}" ]; then
 WXQ_MODE=debug
 WXQ_KEYSTORE="$PWD/signing/debug.jks"
 export WXQ_STORE_PASSWORD=android WXQ_KEY_PASSWORD=android
 WXQ_KEY_ALIAS=wxqdebug
 mkdir -p signing
 if [ ! -f "$WXQ_KEYSTORE" ]; then
  keytool -genkeypair -keystore "$WXQ_KEYSTORE" -storepass:env WXQ_STORE_PASSWORD -keypass:env WXQ_KEY_PASSWORD -alias "$WXQ_KEY_ALIAS" -keyalg RSA -keysize 2048 -validity 10000 -dname 'CN=Wanxiang Local Debug,O=Local,C=CN'
 fi
else
 : "${WXQ_KEY_ALIAS:?Set WXQ_KEY_ALIAS for the existing signing key}"
 : "${WXQ_STORE_PASSWORD:?Set WXQ_STORE_PASSWORD privately in your environment}"
 export WXQ_STORE_PASSWORD
 export WXQ_KEY_PASSWORD="${WXQ_KEY_PASSWORD:-$WXQ_STORE_PASSWORD}"
 test -f "$WXQ_KEYSTORE"
fi
python3 - <<'PY'
import shutil
for path in ['build/classes','build/dex','build/generated']:shutil.rmtree(path,ignore_errors=True)
PY
mkdir -p build/classes build/dex build/generated dist
python3 - <<'PY_WORKSHOP'
from pathlib import Path
import json,hashlib,zipfile
p=Path('assets/workshop/site.zip');manifest=json.loads(Path('assets/workshop/bundle.json').read_text())
assert p.stat().st_size==manifest['sizeBytes'] and hashlib.sha256(p.read_bytes()).hexdigest()==manifest['sha256'], 'Local workshop bundle is incomplete or changed; repack it before building'
with zipfile.ZipFile(p) as z:assert z.testzip() is None, 'Invalid local workshop archive'
PY_WORKSHOP
"$WXQ_BT/aapt2" compile --dir res -o build/resources.zip
"$WXQ_BT/aapt2" link -o build/resources.apk -I "$WXQ_PLATFORM" --manifest AndroidManifest.xml --java build/generated -A assets build/resources.zip
python3 - <<'PY'
from pathlib import Path
Path('build/sources.txt').write_text('\n'.join(str(p) for root in ['src','build/generated'] for p in sorted(Path(root).rglob('*.java')))+'\n')
PY
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 --release 8 -classpath "$WXQ_PLATFORM" -d build/classes @build/sources.txt
python3 - <<'PY'
from pathlib import Path
import zipfile
with zipfile.ZipFile('build/classes.jar','w',zipfile.ZIP_DEFLATED) as z:
 for f in sorted(Path('build/classes').rglob('*.class')):z.write(f,str(f.relative_to('build/classes')))
PY
java -cp "$WXQ_BT/lib/d8.jar" com.android.tools.r8.D8 --min-api 26 --lib "$WXQ_PLATFORM" --output build/dex build/classes.jar
python3 - <<'PY'
import zipfile,shutil
shutil.copyfile('build/resources.apk','build/unsigned.apk')
with zipfile.ZipFile('build/unsigned.apk','a',zipfile.ZIP_DEFLATED) as z:z.write('build/dex/classes.dex','classes.dex')
PY
"$WXQ_BT/zipalign" -f -p 4 build/unsigned.apk build/aligned.apk
WXQ_OUTPUT="dist/WanxiangCompanion-$WXQ_VERSION.apk"
if [ "$WXQ_MODE" = debug ]; then WXQ_OUTPUT="dist/WanxiangCompanion-$WXQ_VERSION-debug.apk"; fi
java -jar "$WXQ_BT/lib/apksigner.jar" sign --ks "$WXQ_KEYSTORE" --ks-key-alias "$WXQ_KEY_ALIAS" --ks-pass env:WXQ_STORE_PASSWORD --key-pass env:WXQ_KEY_PASSWORD --out "$WXQ_OUTPUT" build/aligned.apk
java -jar "$WXQ_BT/lib/apksigner.jar" verify --verbose "$WXQ_OUTPUT"
"$WXQ_BT/aapt2" dump badging "$WXQ_OUTPUT"

