"""Bundle the actual special-unit portraits used by the simulator component."""
import concurrent.futures,hashlib,json,urllib.request
from pathlib import Path

def cache_special_images(work):
    work=Path(work);arrays=json.loads((work/'library-data.json').read_text())['arrays']
    heroes={str(x['id']):x for x in arrays['wanxiang_hero']}
    commanders={str(x.get('lordId')):x for x in arrays['commanders']}
    urls=set()
    for row in arrays['specialHeroes']:
        if row.get('previewCardId'):
            url='https://static.datatft.com/wxq/wxq-database/heroes/'+str(row['previewCardId'])+'/avatar.png'
        elif row.get('sourceLordId'):
            url=commanders.get(str(row['sourceLordId']),{}).get('assets',{}).get('skillIcon')
        else:
            url=heroes.get(str(row.get('sourceHeroId')),{}).get('assets',{}).get('skillIcon')
        if url and url.startswith('https://static.datatft.com/'):urls.add(url)
    mapping=json.loads((work/'image-map.json').read_text());images=work/'images';images.mkdir(exist_ok=True)
    def fetch(url):
        name='images/'+hashlib.sha256(url.encode()).hexdigest()+'.png';p=work/name
        if not p.exists():
            with urllib.request.urlopen(url,timeout=30) as r:b=r.read()
            if len(b)>2*1024*1024 or b[:8]!=b'\x89PNG\r\n\x1a\n':raise ValueError('Invalid special image')
            p.write_bytes(b)
        return url,name
    with concurrent.futures.ThreadPoolExecutor(max_workers=6) as pool:
        for url,name in pool.map(fetch,sorted(urls)):mapping[url]=name
    (work/'image-map.json').write_text(json.dumps(mapping))
    print('Bundled special-unit images:',len(urls),flush=True)

if __name__=='__main__':cache_special_images(Path(__file__).resolve().parents[1]/'build/workshop')
