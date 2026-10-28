"""Repack already downloaded public assets after changing the local adapters."""
import hashlib,json,shutil,zipfile
from pathlib import Path
root=Path(__file__).resolve().parents[1];work=root/'build/workshop';m=json.loads((work/'port-manifest.json').read_text())
for name in ['index.html','port.css','port.js']:shutil.copyfile(root/'assets/workshop'/name,work/name)
(work/'index.html').write_text((work/'index.html').read_text().replace('__ENTRY__',m['entry']))
p=root/'assets/workshop/site.zip'
with zipfile.ZipFile(p,'w',zipfile.ZIP_DEFLATED,compresslevel=9) as z:
 for f in sorted(work.rglob('*')):
  if f.is_file():
   i=zipfile.ZipInfo(str(f.relative_to(work)),(2026,1,1,0,0,0));i.compress_type=zipfile.ZIP_DEFLATED;i.external_attr=0o100644<<16;z.writestr(i,f.read_bytes())
(root/'assets/workshop/bundle.json').write_text(json.dumps({'sha256':hashlib.sha256(p.read_bytes()).hexdigest(),'sizeBytes':p.stat().st_size,'entry':m['entry']},indent=2));print('Local bundle repacked',p.stat().st_size)
