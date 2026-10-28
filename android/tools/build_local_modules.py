"""Reproduce the authorized local port. Public modules only; no account/admin bundles.
Run in android/: python3 tools/build_local_modules.py --json-jar ../../toolchain/json.jar
"""
import argparse, concurrent.futures, hashlib, json, re, shutil, subprocess, urllib.request, zipfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ORIGIN='https://www.datawxq.com'
parser=argparse.ArgumentParser();parser.add_argument('--json-jar',required=True);args=parser.parse_args()
work=ROOT/'build'/'workshop';assets=work/'assets';assets.mkdir(parents=True,exist_ok=True)
for p in (ROOT.parent.parent/'research-site').glob('*.js'):
    if not (assets/p.name).exists():shutil.copyfile(p,assets/p.name)
def fetch(name):
    if not re.fullmatch(r'[A-Za-z0-9_.-]+\.(?:js|css)',name):raise ValueError(name)
    p=assets/name
    if not p.exists():
        with urllib.request.urlopen(ORIGIN+'/assets/'+name,timeout=35) as r:p.write_bytes(r.read())
    return p.read_text()
with urllib.request.urlopen(ORIGIN+'/',timeout=30) as r:home=r.read().decode()
entry=re.search(r'src="/assets/(index-[\w-]+\.js)"',home).group(1)
index=fetch(entry);desktop=re.search(r'desktop-main-[\w-]+\.js',index).group();mobile=re.search(r'h5-main-[\w-]+\.js',index).group()
# Static imports only. Dynamic account/admin pages are intentionally excluded.
queue=[entry,desktop,mobile];seen=set();original={}
while queue:
    batch=list(dict.fromkeys(n for n in queue if n not in seen));queue=[]
    if not batch:break
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:results=list(pool.map(fetch,batch))
    for name,source in zip(batch,results):
        seen.add(name);original[name]=hashlib.sha256(source.encode()).hexdigest()
        queue.extend(re.findall(r'(?:from|import)\s*["\']\./([^"\']+\.(?:js|css))["\']',source))
        # Vite declares CSS preload filenames in dependency maps, not static imports.
        queue.extend(re.findall(r'"assets/([^"\']+\.css)"',source))
    print('Bundled code/styles:',len(seen),flush=True)
seo=re.search(r'seo-[\w-]+\.js',(assets/desktop).read_text()).group()
if 'window.__WXQ_DATA__' in (assets/seo).read_text():
    (assets/seo).unlink();original[seo]=hashlib.sha256(fetch(seo).encode()).hexdigest()
# The entry's single automatic bootstrap chooses desktop/H5. Everything resolves locally.
# Replace large literal data arrays with locally supplied, refreshable JSON records.
classes=ROOT/'build'/'prepare-workshop';classes.mkdir(parents=True,exist_ok=True)
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','--release','8','-encoding','UTF-8','-cp',str(Path(args.json_jar).resolve()),'-d',str(classes),*[str(ROOT/'src/com/doudou/wxq'/n) for n in ['JsDataReader.java','WorkshopData.java']],str(ROOT/'tools/PrepareWorkshop.java')],check=True)
subprocess.run(['java','-cp',str(classes)+':'+str(Path(args.json_jar).resolve()),'com.doudou.wxq.PrepareWorkshop',str(assets/seo),desktop+'|'+seo],check=True)
manifest={'source':ORIGIN,'entry':entry,'desktop':desktop,'mobile':mobile,'dataSource':seo,'originalSha256':original,'authorization':'Ported under the authorization reported by the repository owner.','notes':'Local module code; remote HTML, login, admin and server sharing are not used.'}
(work/'port-manifest.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2))
# Public per-tier icons used by the original HeroIcon component.
level=assets/'level';level.mkdir(exist_ok=True)
for n in range(1,6):
    p=level/(str(n)+'.png')
    if not p.exists():
        with urllib.request.urlopen(ORIGIN+'/assets/level/'+p.name,timeout=25) as r:p.write_bytes(r.read())
# Capture core portraits/icons so a fresh install can use the simulator offline.
data=json.loads((work/'library-data.json').read_text())['arrays'];urls=set()
for key in ['wanxiang_hero','wanxiang_equipment','commanders']:
    for row in data[key]:
        a=row.get('assets',{})
        for field in ['avatar','thumbnail','listImage']:
            url=a.get(field)
            if url and url.startswith('https://static.datatft.com/'):urls.add(url)
images=work/'images';images.mkdir(exist_ok=True);mapping={}
def image(url):
    filename=hashlib.sha256(url.encode()).hexdigest()+('.jpg' if url.lower().endswith(('.jpg','.jpeg')) else '.png');p=images/filename
    if not p.exists():
        with urllib.request.urlopen(url,timeout=30) as r:b=r.read()
        if len(b)>2*1024*1024 or not (b[:8]==b'\x89PNG\r\n\x1a\n' or b[:3]==b'\xff\xd8\xff'):raise ValueError('Invalid image '+url)
        p.write_bytes(b)
    return url,filename
with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
    for i,(url,name) in enumerate(pool.map(image,sorted(urls)),1):
        mapping[url]='images/'+name
        if i%25==0:print('Bundled core images:',i,'/',len(urls),flush=True)
(work/'image-map.json').write_text(json.dumps(mapping))
from cache_special_images import cache_special_images
cache_special_images(work)
# The module HTML/CSS/adapters are maintained source files, not fetched website HTML.
for name in ['index.html','port.css','port.js']:
    shutil.copyfile(ROOT/'assets'/'workshop'/name,work/name)
(work/'index.html').write_text((work/'index.html').read_text().replace('__ENTRY__',entry))
shutil.copyfile(ROOT/'assets/navigation-gesture.js',work/'navigation-gesture.js')
path=ROOT/'assets/workshop/site.zip';pending=ROOT/'build/workshop-pending.zip'
with zipfile.ZipFile(pending,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
    for p in sorted(work.rglob('*')):
        if p.is_file():
            rel=str(p.relative_to(work));info=zipfile.ZipInfo(rel,(2026,1,1,0,0,0));info.compress_type=zipfile.ZIP_DEFLATED;info.external_attr=0o100644<<16;z.writestr(info,p.read_bytes())
with zipfile.ZipFile(pending) as z:assert z.testzip() is None
pending.replace(path)
(ROOT/'assets/workshop/bundle.json').write_text(json.dumps({'sha256':hashlib.sha256(path.read_bytes()).hexdigest(),'sizeBytes':path.stat().st_size,'entry':entry},indent=2))
print('Local module archive:',path.stat().st_size,'bytes',flush=True)
