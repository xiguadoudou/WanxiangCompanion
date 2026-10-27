const vm=require('node:vm'),fs=require('node:fs'),assert=require('node:assert/strict');
const code=fs.readFileSync('docs/download.js','utf8'),repo='https://github.com/xiguadoudou/WanxiangCompanion',raw='https://raw.githubusercontent.com/xiguadoudou/WanxiangCompanion/main/updates/',proxy='https://ghfast.top/';
const stable=repo+'/releases/latest/download/WanxiangCompanion.apk';
async function page(responses){
 const elements=Object.fromEntries(['version','notesVersion','downloadText','download','notes','releaseLink','backup','downloadNote','sourceLink','rawBackup'].map(id=>[id,{textContent:'',href:''}]));
 elements.download.href=proxy+stable;elements.backup.href=stable;const requested=[];
 vm.runInNewContext(code,{document:{getElementById:id=>elements[id]},setTimeout,clearTimeout,AbortController,fetch:async url=>{const key=url.split('?')[0];requested.push(key);const r=responses[key];if(!r||r instanceof Error)throw Error('offline');return {ok:true,json:async()=>r};}});
 await new Promise(r=>setTimeout(r,20));return {elements,requested};
}
(async()=>{
 const m={packageName:'com.doudou.wxq',versionName:'0.1.12',versionCode:13,apkUrl:repo+'/releases/download/v0.1.12/WanxiangCompanion-0.1.12.apk',apkFallbackUrls:[raw+'WanxiangCompanion-0.1.12.apk'],sha256:'a'.repeat(64),sizeBytes:90000,notes:'1. 悬浮窗菜单\n2. <img src=x onerror=alert(1)>'};
 let {elements:e,requested}=await page({'latest.json':m});assert.deepEqual(requested,['latest.json']);assert.equal(e.download.href,proxy+m.apkUrl);assert.equal(e.backup.href,m.apkUrl);assert.equal(e.rawBackup.href,proxy+m.apkFallbackUrls[0]);assert.equal(e.notes.textContent,m.notes);assert.equal(e.version.textContent,'v0.1.12');assert.equal(e.sourceLink.href,proxy+repo+'/releases/download/v0.1.12/WanxiangCompanion-0.1.12-source.zip');
 const future={...m,versionName:'0.1.13',versionCode:14,apkUrl:repo+'/releases/download/v0.1.13/WanxiangCompanion-0.1.13.apk',apkFallbackUrls:[raw+'WanxiangCompanion-0.1.13.apk']};
 ({elements:e,requested}=await page({[proxy+raw+'latest.json']:future}));assert.equal(e.version.textContent,'v0.1.13');assert.deepEqual(requested,['latest.json',proxy+raw+'latest.json']);
 const r={tag_name:'v0.1.13',draft:false,prerelease:false,body:'published release',assets:[{name:'WanxiangCompanion-0.1.13.apk',browser_download_url:future.apkUrl,size:90000}]};
 ({elements:e}=await page({'https://api.github.com/repos/xiguadoudou/WanxiangCompanion/releases/latest':r}));assert.equal(e.version.textContent,'v0.1.13');
 for(const bad of [{...m,apkUrl:'https://evil.example/other.apk'},{...m,apkUrl:'javascript:alert(1)'},{...m,sizeBytes:-1},{...m,packageName:'other'},'<html>proxy error</html>']){
  ({elements:e,requested}=await page({'latest.json':bad,[proxy+raw+'latest.json']:bad,[raw+'latest.json']:m}));assert.equal(e.download.href,proxy+m.apkUrl);assert.equal(requested.length,4);
 }
 ({elements:e,requested}=await page({}));assert.equal(requested.length,4);assert.equal(e.download.href,proxy+stable);assert.equal(e.backup.href,stable);assert.match(e.downloadNote.textContent,/版本信息暂未读取/);
 console.log('Download page: same-origin, accelerated/official fallback, future versions, safe text, URL validation and usable offline links passed.');
})().catch(e=>{console.error(e);process.exit(1)});
