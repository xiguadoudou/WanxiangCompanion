(()=>{
 'use strict';
 const repo='https://github.com/xiguadoudou/WanxiangCompanion',raw='https://raw.githubusercontent.com/xiguadoudou/WanxiangCompanion/main/updates/';
 const $=id=>document.getElementById(id);
 async function get(url){const controller=new AbortController(),timer=setTimeout(()=>controller.abort(),6000);try{const r=await fetch(url,{cache:'no-store',signal:controller.signal});if(!r.ok)throw Error('Release unavailable');return await r.json();}finally{clearTimeout(timer);}}
 function release(r){
  const version=String(r.tag_name||'').replace(/^v/,'');if(r.draft||r.prerelease||!/^\d+\.\d+\.\d+$/.test(version))throw Error('Invalid release');
  const apkUrl=`${repo}/releases/download/v${version}/WanxiangCompanion-${version}.apk`,asset=r.assets?.find(a=>a.name===`WanxiangCompanion-${version}.apk`);
  if(!asset||asset.browser_download_url!==apkUrl||!Number.isSafeInteger(asset.size)||asset.size<=0||asset.size>100*1024*1024)throw Error('Invalid package');
  return {version,apkUrl,fallback:`${raw}WanxiangCompanion-${version}.apk`,notes:r.body,sizeBytes:asset.size};
 }
 function valid(m){
  const version=String(m.versionName||'');if(m.packageName!=='com.doudou.wxq'||!/^\d+\.\d+\.\d+$/.test(version)||!Number.isInteger(m.versionCode)||m.versionCode<1)throw Error('Invalid release');
  if(m.apkUrl!==`${repo}/releases/download/v${version}/WanxiangCompanion-${version}.apk`)throw Error('Unexpected download URL');
  if(!/^[a-f\d]{64}$/i.test(m.sha256||'')||!Number.isSafeInteger(m.sizeBytes)||m.sizeBytes<=0||m.sizeBytes>100*1024*1024)throw Error('Invalid package');
  const fallback=Array.isArray(m.apkFallbackUrls)?m.apkFallbackUrls.find(x=>x===`${raw}WanxiangCompanion-${version}.apk`):null;
  return {...m,version,fallback};
 }
 get('https://api.github.com/repos/xiguadoudou/WanxiangCompanion/releases/latest?t='+Date.now()).then(release).catch(()=>get(raw+'latest.json?t='+Date.now()).then(valid)).then(m=>{
  $('version').textContent='v'+m.version;$('notesVersion').textContent='v'+m.version;$('downloadText').textContent='下载 '+m.version+' 安装包';$('download').href=m.apkUrl;
  $('notes').textContent=String(m.notes||'本版本暂无更新说明。');$('releaseLink').href=repo+'/releases/tag/v'+m.version;
  if(m.fallback){$('backup').href=m.fallback;$('backup').textContent='备用地址下载 ↗';}
  $('downloadNote').textContent=(m.sizeBytes<1024*1024?Math.ceil(m.sizeBytes/1024)+' KB':(m.sizeBytes/1024/1024).toFixed(1)+' MB')+' · 原签名覆盖升级 · 下载受 GitHub 网络连接影响';
 }).catch(()=>{
  $('downloadNote').textContent='版本信息暂未读取，仍可点击下载获取 GitHub 最新安装包。';$('notes').textContent='更新内容暂时无法加载，请点击下方链接查看 GitHub 版本说明。';
 });
})();
