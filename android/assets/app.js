(function(){
 'use strict';
 const $=id=>document.getElementById(id), L=WxqLogic;
 let softwareState={},manualUpdateCheck=false;const promptedVersions=new Set();
 const fallbackAvatar='data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="80" height="80"%3E%3Crect width="80" height="80" fill="%23283e4a"/%3E%3Ctext x="40" y="52" text-anchor="middle" font-size="40" fill="%239ccbbf"%3E◇%3C/text%3E%3C/svg%3E';
 const esc=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const src=url=>/^https:\/\//.test(url||'')?esc(url):fallbackAvatar;
 const cmd=(action,data={})=>{if(window.Native)Native.command(JSON.stringify({action,data}));};
 let catalog={commanders:[]}, data=null, ui={commanderId:'',bans:[],sort:'avgPlacement',days:7,minCount:10,opacity:88,width:392,height:230},overlay=false,settings=false,queued=null,queryTimer=null,booted=false,busy=false;
 function query(){const c=(catalog.commanders||[]).find(c=>String(c.id)===String(ui.commanderId));return {commanderId:ui.commanderId,commanderName:c?.name||'',bans:L.cleanBans(ui.bans),sort:ui.sort,days:ui.days,minCount:ui.minCount};}
 function save(){cmd('save',ui);}
 function request(){if(!ui.commanderId)return;clearTimeout(queryTimer);queryTimer=setTimeout(()=>{busy=true;status('正在匹配本局条件…','loading');if(!data||data.queryKey!==L.queryKey(query()))$('results').innerHTML='<div class="skeleton"></div><div class="skeleton"></div>';cmd('query',query());},380);}
 function status(message,type=''){$('statusText').textContent=message;$('status').className='status '+type;}
 function fmtTime(n){if(!n)return '时间未知';const d=new Date(n);return `${d.getMonth()+1}/${d.getDate()} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;}
 function pct(v){return ((+v||0)*100).toFixed(1)+'%';}
 function toast(message){$('toast').textContent=message;$('toast').style.display='block';clearTimeout(window.__copyToast);window.__copyToast=setTimeout(()=>$('toast').style.display='none',2300);}
 function copy(r){if(!data||data.queryKey!==L.queryKey(query())||!data.rows?.includes(r)){toast('条件已变化，请等待新的阵容');return;}if(!r?.lineupCode){toast('数据源暂未提供此阵容码');return;}cmd('copy',{text:r.lineupCode,token:'main-'+Date.now()});}
 function renderBans(){
  $('bans').innerHTML=L.FACTIONS.map((f,i)=>`<button class="ban" data-ban="${esc(f)}" aria-pressed="${ui.bans.includes(f)}" aria-label="${esc(f)}，${ui.bans.includes(f)?'已禁用，再次点击取消':'点击禁用'}"><span class="faction-symbol">${L.factionImage(f)?'<img data-faction="'+esc(f)+'" src="'+esc(L.factionImage(f))+'" alt="">':''}<span class="faction-fallback" ${L.factionImage(f)?'hidden':''}>${esc(f.slice(0,1))}</span></span><span class="ban-label">${esc(f)}</span></button>`).join('');
  $('bans').querySelectorAll('button').forEach(b=>b.onclick=()=>{const f=b.dataset.ban;ui.bans=ui.bans.includes(f)?ui.bans.filter(x=>x!==f):[...ui.bans,f];renderBans();save();request();});
  $('banNote').textContent=ui.bans.length?`已排除 ${ui.bans.length} 个阵营 · 再次点击取消`:'点击禁用，再次点击取消 · 支持多选';
 }
 function renderCommander(){
  const list=catalog.commanders||[],c=list.find(x=>String(x.id)===String(ui.commanderId));
  $('commanderButton').innerHTML=c?`<img class="portrait" src="${src(c.avatar)}" alt=""><span><b>${esc(c.name)}</b><small>${esc(c.skillName||'已选择棋手')}</small></span><span class="chevron">⌄</span>`:`<span class="portrait" style="display:grid;place-items:center;color:#9cb7b8">♙</span><span><b>选择你的棋手</b><small>${list.length?'共 '+list.length+' 名棋手，点击展开':'正在同步棋手资料…'}</small></span><span class="chevron">⌄</span>`;
  $('skill').textContent=c?c.skillText||'':'';
  $('commanders').innerHTML=list.map(c=>`<button class="choice ${String(c.id)===String(ui.commanderId)?'active':''}" role="option" aria-selected="${String(c.id)===String(ui.commanderId)}" data-id="${esc(c.id)}"><img src="${src(c.avatar)}" alt=""><span>${esc(c.name)}</span></button>`).join('')||'<div class="helper" style="padding:12px">棋手资料尚未加载，请联网后点右上角刷新。</div>';
  $('commanders').querySelectorAll('button').forEach(b=>b.onclick=()=>{ui.commanderId=b.dataset.id;toggleDropdown(false);renderCommander();save();request();});
 }
 function toggleDropdown(open){$('commanders').classList.toggle('open',open);$('commanderButton').setAttribute('aria-expanded',String(open));}
 function renderStyle(){
  document.documentElement.style.setProperty('--surface-alpha',ui.opacity/100);
  for(const k of ['opacity','width','height']){$(k).value=ui[k];$(k+'Value').textContent=ui[k]+(k==='opacity'?'%':' dp');}
  document.querySelectorAll('[data-days]').forEach(x=>x.classList.toggle('active',+x.dataset.days===ui.days));
  document.querySelectorAll('[data-min]').forEach(x=>x.classList.toggle('active',+x.dataset.min===ui.minCount));
  document.querySelectorAll('[data-sort]').forEach(x=>x.classList.toggle('active',x.dataset.sort===ui.sort));
 }
 function renderResult(){
  if(!data||data.queryKey!==L.queryKey(query())){
   $('resultCount').textContent='';$('scope').style.display='none';
   if(!ui.commanderId)$('results').innerHTML='<div class="empty glass"><div class="empty-symbol">◇</div><b>选择本局棋手</b>选好棋手与禁用后自动匹配阵容。</div>';
   else $('results').innerHTML='<div class="skeleton"></div><div class="skeleton"></div>';
   return;
  }
  const rows=L.rank(data.rows||[],ui.sort).filter(r=>L.allowed(r,ui.bans));
  $('resultCount').textContent=rows.length?rows.length+' 套':'';
  $('scope').style.display=data.scope==='multi-exclude'?'block':'none';
  $('scope').textContent='多选排除模式：已过滤包含所选阵营的阵容。指标来自该棋手的全部禁用环境，不是多阵营同时被禁的对局统计。';
  if(!rows.length){$('results').innerHTML=`<div class="empty glass"><div class="empty-symbol">◇</div><b>当前条件下暂无匹配阵容</b>${data.limited?'已检查前 '+data.scanned+' 套候选，尚未覆盖全部；':'未检索到满足当前条件和最小样本的阵容；'}<br>可减少禁用项，或降低最小样本后重试。</div>`;return;}
  $('results').innerHTML=rows.slice(0,24).map((r,i)=>`<article class="card glass"><div class="card-head"><div><div class="rank">${String(i+1).padStart(2,'0')} / ${i===0?'优先参考':'备选阵容'}</div><h3>${esc(r.title)}</h3></div><span class="sample">${(+r.count).toLocaleString()} 场</span></div><div class="roster">${r.heroes.map(h=>`<div class="unit ${h.core?'core':''}"><img src="${src(h.image)}" alt="${esc(h.name)}"><small>${esc(h.name)}</small></div>`).join('')}</div><div class="metrics"><div class="metric"><small>平均名次</small><b>${(+r.avgPlacement).toFixed(2)}</b></div><div class="metric"><small>前三率</small><b>${pct(r.top3Rate)}</b></div><div class="metric"><small>登顶率</small><b>${pct(r.firstRate)}</b></div></div><div class="card-foot"><span>${r.size} 人 · ${r.rosterCount!==null&&r.rosterCount!==undefined?"本变阵 "+r.rosterCount+" 场":"登场率 "+pct(r.appearanceRate)}</span><div class="card-actions"><button class="copy-card" data-copy="${i}" ${r.lineupCode?'':'disabled'} title="${r.lineupCode?'复制此阵容的导入码':'数据源暂无此阵容码'}">${r.lineupCode?'复制阵容码':'暂无阵容码'}</button><button data-detail="${i}">装备与详情 ↗</button></div></div></article>`).join('')+(data.limited?'<div class="helper" style="text-align:center;margin-top:12px">展示网站排序靠前的候选，未覆盖全部阵容。</div>':'');
  $('results').querySelectorAll('[data-copy]').forEach(b=>b.onclick=()=>copy(rows[+b.dataset.copy]));
  $('results').querySelectorAll('[data-detail]').forEach(b=>b.onclick=()=>openDetail(rows[+b.dataset.detail]));
 }
 function openDetail(r){
  $('drawer').classList.add('open');
  const c=(catalog.commanders||[]).find(x=>String(x.id)===String(ui.commanderId));
  $('detailContent').innerHTML=`<div class="eyebrow">LINEUP NOTES</div><h2>${esc(r.title)}</h2><div class="detail-sub">${esc(c?.name||'')} · ${(+r.count).toLocaleString()} 场样本 · ${esc(data.version||'')}</div><div class="helper" style="margin-bottom:15px">金色标记为网站标注的核心英雄。指标按网站的核心组合统计，可能汇总多种变阵；当前成员配置样本 ${r.rosterCount??'未提供'} 场。平均等级不是成型的硬性要求。</div>${r.heroes.map(h=>`<div class="hero-detail"><img src="${src(h.image)}" alt=""><div><h4>${esc(h.name)}${h.core?' <span style="color:var(--gold);font-size:10px">核心</span>':''}</h4><small>${esc(h.faction||'阵营未知')}${h.level?' · 平均 '+Math.round(h.level)+' 级':''}</small><div class="items">${h.items?.length?h.items.map(x=>`<span class="item">${x.image?'<img src="'+src(x.image)+'" alt="">':''}${esc(x.name)}</span>`).join(''):'<span class="helper">网站未提供该英雄的推荐装备</span>'}</div></div></div>`).join('')}<div class="drawer-actions">${r.lineupCode?'<button class="primary" id="copyCode">复制阵容码</button>':''}<button class="secondary" id="detailSource">查看数据来源 ↗</button></div><p class="helper">数据时间 ${fmtTime(data.fetchedAt)}。此工具提供阵容资料，不读取游戏画面或替你操作。</p>`;
  if($('copyCode'))$('copyCode').onclick=()=>copy(r);
  $('detailSource').onclick=()=>cmd('source');
 }
 function resetRoundUi(prefs){
  clearTimeout(queryTimer);Object.assign(ui,prefs||{});ui.commanderId=String(prefs?.commanderId||'');ui.bans=[];data=null;busy=false;queued=null;
  toggleDropdown(false);$('drawer').classList.remove('open');renderBans();renderCommander();renderStyle();$('resultCount').textContent='';$('scope').style.display='none';
  $('results').innerHTML='<div class="empty glass"><div class="empty-symbol">◇</div><b>新的一局，重新出发</b>选择本局棋手和禁用阵营即可自动匹配。</div>';status(ui.commanderId?'已选择常小娥 · 正在匹配…':'已清空本局选择');if(ui.commanderId)request();
 }
 function newRound(){resetRoundUi();cmd('resetRound');}
 function closeUpdateDialog(){if($('updateDialog').open)$('updateDialog').close();}
 function renderUpdateDialog(e){
  $('updateCurrentVersion').textContent='当前 '+(e.installedVersion||'0.1.11');$('updateLatestVersion').textContent=e.latestVersion||'';
  const notes=String(e.notes||'').trim()||'本版本暂无更新说明。';if($('updateChangelog').textContent!==notes)$('updateChangelog').textContent=notes;
  const active=['checking','downloading'].includes(e.state),error=e.state==='error';
  $('updateDialogStatus').textContent=e.message||'';$('updateDialogStatus').hidden=!['downloading','ready','error','checking'].includes(e.state);$('updateDialogStatus').classList.toggle('error',error);
  $('updateDialogProgress').hidden=e.state!=='downloading';$('updateDialogProgress').value=e.progress||0;
  $('updateDownloadNow').disabled=active;$('updateDownloadNow').textContent=e.downloadReady?'安装更新':e.state==='downloading'?'下载中 '+(e.progress||0)+'%':e.state==='checking'?'检查中…':error?'重试下载':'立即下载';
  $('updateLater').textContent=e.state==='downloading'?'收起弹窗':'稍后再说';
 }
 function maybeShowUpdateDialog(e,force=false){
  if(overlay||!e.available||(e.state!=='available'&&!(force&&e.state==='ready')))return;
  const key=String(e.latestVersionCode||e.latestVersion||'');if(!key||(!force&&promptedVersions.has(key)))return;
  promptedVersions.add(key);renderUpdateDialog(e);
  if(!$('updateDialog').open){$('updateDialog').showModal();document.body.classList.add('has-update-dialog');$('updateChangelog').focus({preventScroll:true});}
 }
 function renderUpdater(event={}){
  const e={...softwareState,...event};if(e.state==='current'){e.available=false;e.downloadReady=false;}softwareState=e;
  const active=['checking','downloading'].includes(e.state);
  $('installedVersion').textContent='当前 '+(e.installedVersion||'0.1.11');$('softwareStatus').textContent=e.message||'打开应用自动检查软件版本';
  $('softwareNotes').textContent=e.notes||'';$('softwareNotes').hidden=!e.notes;
  $('softwareProgress').hidden=e.state!=='downloading';$('softwareProgress').value=e.progress||0;
  $('checkSoftware').disabled=active;$('downloadSoftware').hidden=!e.available||e.downloadReady;$('downloadSoftware').disabled=active;
  $('installSoftware').hidden=!e.downloadReady;$('updateButton').textContent=e.available?'↑ •':'↑';
  if($('updateDialog').open){if(e.state==='current')closeUpdateDialog();else renderUpdateDialog(e);}
  maybeShowUpdateDialog(e,manualUpdateCheck);
  if(['available','ready','current','error'].includes(e.state))manualUpdateCheck=false;
 }
 function setSettings(value){settings=value;$('settings').classList.toggle('open',value);$('recommendation').style.display=value?'none':'';window.scrollTo(0,0);}
 window.App={
  receive(e){
   switch(e.type){
    case 'init':
     if(booted)return;booted=true;overlay=!!e.overlay;document.body.classList.toggle('overlay',overlay);
     Object.assign(ui,e.prefs||{});catalog=e.catalog||catalog;L.configureCatalog(catalog);ui.bans=L.cleanBans(ui.bans);data=e.data?.rows?e.data:null;
     renderBans();renderCommander();renderStyle();renderResult();renderUpdater(e.updater);if(e.imageCacheStatus)$('cacheStatus').textContent=e.imageCacheStatus;
     if(catalog.version)$('version').textContent=catalog.version+' · 上次检查 '+fmtTime(catalog.checkedAt);
     if(data&&data.queryKey===L.queryKey(query()))status('缓存 · '+fmtTime(data.fetchedAt)+' · 正在检查最新数据…','loading');
     if(ui.commanderId)request();break;
    case 'catalog':
     catalog=e.catalog;L.configureCatalog(catalog);ui.bans=L.cleanBans(ui.bans);renderBans();renderCommander();$('version').textContent=catalog.version+' · 检查于 '+fmtTime(catalog.checkedAt);
     $('refresh').classList.remove('busy-refresh');
     if(ui.commanderId)request();break;
    case 'roundReset':resetRoundUi(e.prefs);break;
    case 'cacheStatus':$('cacheStatus').textContent=e.message;break;
    case 'softwareUpdate':renderUpdater(e);break;
    case 'dataStatus':$('dataSyncStatus').textContent=e.message;if(e.state!=='checking')$('refresh').classList.remove('busy-refresh');break;
    case 'queryProgress':if(e.queryKey===L.queryKey(query()))status(e.message+'…','loading');break;
    case 'status':status(e.message,'loading');break;
    case 'copyResult':toast(e.message||(e.ok?'阵容码已复制':'复制失败'));break;
    case 'preferences':Object.assign(ui,e.prefs||{});renderBans();renderCommander();renderStyle();renderResult();break;
    case 'queryStart':if(e.queryKey===L.queryKey(query())){busy=true;status('正在查询网站真实对局数据…','loading');if(!data||data.queryKey!==e.queryKey){$('resultCount').textContent='';$('results').innerHTML='<div class="skeleton"></div><div class="skeleton"></div>';}}break;
    case 'result':
     if(e.queryKey!==L.queryKey(query()))return;data=e;busy=false;renderResult();$('refresh').classList.remove('busy-refresh');
     status((e.cached?'缓存数据 · ':'已同步 · ')+e.version+' · '+fmtTime(e.fetchedAt)+(e.cached?' · 正在联网检查':''),e.cached?'loading':'');break;
    case 'queryError':
     if(e.queryKey!==L.queryKey(query()))return;busy=false;status(e.message,'error');$('refresh').classList.remove('busy-refresh');
     if(!data||data.queryKey!==L.queryKey(query()))$('results').innerHTML='<div class="empty glass"><div class="empty-symbol">◇</div><b>本次查询未完成</b>'+esc(e.message)+'</div>';break;
    case 'bootError':
     status(e.message,'error');$('refresh').classList.remove('busy-refresh');
     if(!catalog.commanders?.length)$('commanderButton').querySelector('small').textContent='资料加载失败，点击右上角刷新';
     if(!data&&ui.commanderId)$('results').innerHTML='<div class="empty glass"><b>暂时无法连接数据源</b>'+esc(e.message)+'</div>';break;
   }
  },
  back(){if($('updateDialog').open)closeUpdateDialog();else if($('drawer').classList.contains('open'))$('drawer').classList.remove('open');else if(settings)setSettings(false);else if(overlay)cmd('minimize');else cmd('background');}
 };
 $('commanderButton').onclick=()=>toggleDropdown(!$('commanders').classList.contains('open'));
 $('clearBans').onclick=()=>{ui.bans=[];renderBans();save();request();};
 $('settingsButton').onclick=()=>setSettings(!settings);$('back').onclick=()=>setSettings(false);
 for(const k of ['opacity','width','height'])$(k).oninput=()=>{ui[k]=+$(k).value;renderStyle();save();};
 document.querySelectorAll('[data-sort]').forEach(b=>b.onclick=()=>{ui.sort=b.dataset.sort;renderStyle();save();request();});
 document.querySelectorAll('[data-days]').forEach(b=>b.onclick=()=>{ui.days=+b.dataset.days;renderStyle();save();request();});
 document.querySelectorAll('[data-min]').forEach(b=>b.onclick=()=>{ui.minCount=+b.dataset.min;renderStyle();save();request();});
 $('refresh').onclick=()=>{$('refresh').classList.add('busy-refresh');cmd('refresh');if(ui.commanderId)request();};
 $('updateButton').onclick=()=>{setSettings(true);$('softwareCard').scrollIntoView({behavior:'smooth',block:'center'});};
 $('downloadPage').onclick=()=>cmd('downloadPage');
 $('checkSoftware').onclick=()=>{manualUpdateCheck=true;cmd('checkSoftwareUpdate');};$('downloadSoftware').onclick=()=>cmd('downloadSoftwareUpdate');$('installSoftware').onclick=()=>cmd('installSoftwareUpdate');
 $('updateLater').onclick=closeUpdateDialog;
 $('updateDialog').addEventListener('close',()=>document.body.classList.remove('has-update-dialog'));
 $('updateDialog').addEventListener('cancel',e=>{e.preventDefault();closeUpdateDialog();});
 $('updateDownloadNow').onclick=()=>{if(softwareState.downloadReady){closeUpdateDialog();cmd('installSoftwareUpdate');}else{renderUpdater({state:'downloading',message:'准备从 GitHub 下载…',progress:0});cmd('downloadSoftwareUpdate');}};
 $('start').onclick=$('settingsStart').onclick=()=>cmd('start');$('stop').onclick=()=>cmd('stop');
 $('source').onclick=()=>cmd('source');$('next').onclick=$('newRound').onclick=newRound;
 $('closeDetail').onclick=()=>$('drawer').classList.remove('open');
 document.addEventListener('error',e=>{if(e.target.tagName!=='IMG')return;if(e.target.dataset.faction){const local=L.bundledFactionImage(e.target.dataset.faction);if(local&&e.target.getAttribute('src')!==local)e.target.src=local;else{e.target.hidden=true;e.target.nextElementSibling.hidden=false;}}else if(e.target.src!==fallbackAvatar)e.target.src=fallbackAvatar;},true);
 if(window.Native){try{App.receive(JSON.parse(Native.initialState()));}catch(e){status('界面初始化失败，请重新打开应用','error');}}
 else{renderBans();renderCommander();renderStyle();status('界面预览 · 实际数据在安卓 App 中同步');}
})();
