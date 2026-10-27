const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
const code=fs.readFileSync('docs/download.js','utf8'),repo='https://github.com/xiguadoudou/WanxiangCompanion',raw='https://raw.githubusercontent.com/xiguadoudou/WanxiangCompanion/main/updates/';
async function page(manifest,fail=false,release=null){
 const elements=Object.fromEntries(['version','notesVersion','downloadText','download','notes','releaseLink','backup','downloadNote'].map(id=>[id,{textContent:'',href:''}]));
 elements.download.href=repo+'/releases/latest/download/WanxiangCompanion.apk';elements.backup.href=repo+'/releases/latest';
 let requested=[];vm.runInNewContext(code,{document:{getElementById:id=>elements[id]},setTimeout,clearTimeout,AbortController,fetch:async url=>{requested.push(url);if(fail)throw Error('offline');if(url.includes('api.github.com')){if(!release)throw Error('API unavailable');return {ok:true,json:async()=>release};}return {ok:true,json:async()=>manifest};}});
 await new Promise(r=>setTimeout(r,20));assert.ok(requested[0].startsWith('https://api.github.com/repos/xiguadoudou/WanxiangCompanion/releases/latest?t='));if(!release)assert.ok(requested[1].startsWith(raw+'latest.json?t='));return elements;
}
(async()=>{
 const m={packageName:'com.doudou.wxq',versionName:'0.1.9',versionCode:10,apkUrl:repo+'/releases/download/v0.1.9/WanxiangCompanion-0.1.9.apk',apkFallbackUrls:[raw+'WanxiangCompanion-0.1.9.apk'],sha256:'a'.repeat(64),sizeBytes:90000,notes:'1. 默认选择常小娥\n2. <img src=x onerror=alert(1)>'};
 let e=await page(m);assert.equal(e.download.href,m.apkUrl);assert.equal(e.backup.href,m.apkFallbackUrls[0]);assert.equal(e.notes.textContent,m.notes);assert.equal(e.version.textContent,'v0.1.9');
 const future={...m,versionName:'0.1.10',versionCode:11,apkUrl:repo+'/releases/download/v0.1.10/WanxiangCompanion-0.1.10.apk',apkFallbackUrls:[raw+'WanxiangCompanion-0.1.10.apk']};e=await page(future);assert.equal(e.version.textContent,'v0.1.10');assert.equal(e.download.href,future.apkUrl);
 for(const bad of [{...m,apkUrl:'https://example.com/other.apk'},{...m,apkUrl:'javascript:alert(1)'},{...m,sizeBytes:-1},{...m,packageName:'other'}]){e=await page(bad);assert.equal(e.download.href,repo+'/releases/latest/download/WanxiangCompanion.apk');assert.match(e.downloadNote.textContent,/暂未读取/);}
 const r={tag_name:'v0.1.10',draft:false,prerelease:false,body:'latest published release',assets:[{name:'WanxiangCompanion-0.1.10.apk',browser_download_url:future.apkUrl,size:90000}]};e=await page(m,false,r);assert.equal(e.version.textContent,'v0.1.10','Published release takes precedence over cached old manifest');assert.equal(e.notes.textContent,r.body);e=await page(m,false,{...r,draft:true});assert.equal(e.version.textContent,'v0.1.9','Invalid release falls back to manifest');
 e=await page(null,true);assert.equal(e.download.href,repo+'/releases/latest/download/WanxiangCompanion.apk');assert.equal(e.backup.href,repo+'/releases/latest');
 console.log('Download page: latest/future release, text-only notes, expected-repository URLs, invalid manifest and network fallback passed.');
})().catch(e=>{console.error(e);process.exit(1)});
