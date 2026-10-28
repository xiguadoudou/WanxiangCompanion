(function(){
 'use strict';
 const $=id=>document.getElementById(id),L=WxqLogic;
 const fallback='data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="64" height="64"%3E%3Crect width="64" height="64" rx="9" fill="%23283e4a"/%3E%3Ctext x="32" y="43" text-anchor="middle" font-size="30" fill="%239ccbbf"%3E◇%3C/text%3E%3C/svg%3E';
 const esc=x=>String(x??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
 const src=u=>/^https:\/\//.test(u||'')?esc(u):fallback;
 const cmd=(action,data={})=>{if(window.Native)Native.command(JSON.stringify({action,data}));};
 let ui={commanderId:'',bans:[],sort:'avgPlacement',days:7,minCount:10,opacity:88,width:392,height:230},catalog={commanders:[]},data=null,rows=[],selectedKey='',heroId='',booted=false,timer,toastTimer,lastStatus='正在检查更新',copySeq=0,pinnedRow=null;
 const pct=v=>((+v||0)*100).toFixed(1)+'%';
 function query(){const c=catalog.commanders.find(x=>String(x.id)===String(ui.commanderId));return {commanderId:ui.commanderId,commanderName:c?.name||'',bans:L.cleanBans(ui.bans),sort:ui.sort,days:ui.days,minCount:ui.minCount};}
 function save(){cmd('save',ui);}
 function toast(s){$('toast').textContent=s;$('toast').hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').hidden=true,2200);}
 function setStatus(text,mode=''){lastStatus=text;$('statusText').textContent=text;$('status').className='status '+mode;$('status').title=text;}
 function closeMenus(){for(const id of ['commanderMenu','heroMenu','appearance','message','exitConfirm'])$(id).hidden=true;$('commanderTrigger').setAttribute('aria-expanded','false');$('appearanceTrigger').setAttribute('aria-expanded','false');}
 function showMessage(title,text){closeMenus();$('messageTitle').textContent=title;$('messageText').textContent=text;$('message').hidden=false;}
 function renderFilters(){
  $('bans').innerHTML=L.FACTIONS.map(f=>`<button class="ban" data-ban="${esc(f)}" aria-pressed="${ui.bans.includes(f)}" aria-label="${esc(f)}，${ui.bans.includes(f)?'已禁用':'点击禁用'}">${esc(f)}</button>`).join('');
  $('bans').querySelectorAll('button').forEach(b=>b.onclick=()=>{const f=b.dataset.ban;ui.bans=ui.bans.includes(f)?ui.bans.filter(x=>x!==f):[...ui.bans,f];closeMenus();renderFilters();invalidate();save();request();});
  const c=catalog.commanders.find(x=>String(x.id)===String(ui.commanderId));
  $('commanderTrigger').innerHTML=`${c?'<img src="'+src(c.avatar)+'" alt="">':''}<span>${esc(c?.name||'选择棋手')}</span><b class="chevron">⌄</b>`;
  $('commanderMenu').innerHTML=catalog.commanders.map(c=>`<button role="option" aria-selected="${String(c.id)===String(ui.commanderId)}" class="commander-choice ${String(c.id)===String(ui.commanderId)?'active':''}" data-id="${esc(c.id)}"><img src="${src(c.avatar)}" alt=""><span>${esc(c.name)}</span></button>`).join('')||'<div class="empty-list">棋手资料未加载，请点击刷新。</div>';
  $('commanderMenu').querySelectorAll('button').forEach(b=>b.onclick=()=>{ui.commanderId=b.dataset.id;selectedKey='';heroId='';closeMenus();renderFilters();invalidate();save();request();});
  document.querySelectorAll('[data-sort]').forEach(b=>b.classList.toggle('active',b.dataset.sort===ui.sort));
  $('scope').hidden=ui.bans.length<2;
 }
 function renderAppearance(){document.documentElement.style.setProperty('--surface-alpha',ui.opacity/100);for(const k of ['opacity','width','height']){$(k).value=ui[k];$(k+'Value').textContent=ui[k]+(k==='opacity'?'%':' dp');}}
 function invalidate(){if(!data||data.queryKey!==L.queryKey(query())){rows=[];selectedKey='';heroId='';$('candidates').innerHTML='<div class="loader"></div><div class="loader"></div>';$('detail').innerHTML='<div class="empty">正在匹配本局条件…</div>';}}
 function request(){if(pinnedRow||!ui.commanderId)return;clearTimeout(timer);timer=setTimeout(()=>{setStatus('匹配中…','loading');cmd('query',query());},320);}
 function renderResult(){
  if(pinnedRow)return;
  if(!data||data.queryKey!==L.queryKey(query()))return;
  rows=L.rank(data.rows||[],ui.sort).filter(r=>L.allowed(r,ui.bans)).slice(0,24);
  if(!rows.length){selectedKey='';$('candidates').innerHTML='<div class="empty-list">暂无匹配</div>';$('detail').innerHTML='<div class="empty"><b>当前条件暂无匹配</b>减少禁用项后重试'+(data.limited?'<br>候选尚未全部检索':'')+'</div>';return;}
  if(!rows.some(r=>r.key===selectedKey)){selectedKey=rows[0].key;heroId='';}
  renderList();renderDetail();
 }
 function renderList(){
  const left=$('candidates').scrollLeft,top=$('candidates').scrollTop;
  $('candidates').innerHTML=rows.map((r,i)=>`<div class="candidate ${r.key===selectedKey?'selected':''}"><button class="pick" data-pick="${i}" aria-label="查看${esc(r.title)}" aria-pressed="${r.key===selectedKey}"><strong><span class="number">${i+1}</span>${esc(r.title.replace(/^(河洛|逐鹿|日落海|三分之地|大河流域)\s*·\s*/,''))}</strong><small>均 ${(+r.avgPlacement).toFixed(2)} · 前三 ${pct(r.top3Rate)}<span class="list-count">${(+r.count).toLocaleString()} 场</span></small></button></div>`).join('');
  $('candidates').scrollLeft=left;$('candidates').scrollTop=top;
  $('candidates').querySelectorAll('[data-pick]').forEach(b=>b.onclick=()=>{selectedKey=rows[+b.dataset.pick].key;heroId='';renderList();renderDetail();});
 }
 function renderDetail(){
  const r=rows.find(r=>r.key===selectedKey);if(!r)return;
  if(!r.heroes.some(h=>h.id===heroId))heroId=(r.heroes.find(h=>h.core)||r.heroes[0])?.id||'';
  const h=r.heroes.find(h=>h.id===heroId);
  $('detail').innerHTML=`<div class="detail-head"><div class="detail-title"><h2 title="${esc(r.title)}">${esc(r.title)}</h2></div><button class="strip-trigger" id="pinLineup" aria-label="将当前阵容固定为精简长条">长条</button><button class="copy-main" id="copySelected" aria-label="${r.lineupCode?'复制阵容码':'数据源暂无阵容码'}" title="${r.lineupCode?'复制阵容码':'数据源暂无阵容码'}" ${r.lineupCode?'':'disabled'}>${r.lineupCode?'复制':'无码'}</button></div><div class="metrics"><span>均名<b>${(+r.avgPlacement).toFixed(2)}</b></span><span>前三<b>${pct(r.top3Rate)}</b></span><span>登顶<b>${pct(r.firstRate)}</b></span></div><div class="roster" aria-label="英雄，点击查看装备">${r.heroes.map((h,i)=>`<button class="hero ${h.core?'core':''} ${h.id===heroId?'selected':''}" data-hero="${i}" aria-label="${esc(h.name)}的装备" aria-pressed="${h.id===heroId}"><img src="${src(h.image)}" alt=""><span>${esc(h.name)}</span></button>`).join('')}</div><div class="equipment"><div class="items"><button class="equipment-label hero-picker" id="heroPicker" aria-label="选择英雄查看装备">${esc(h?.name||'装备')}<b class="chevron">⌄</b></button>${h?.items?.length?h.items.map(x=>`<span class="item">${x.image?'<img src="'+src(x.image)+'" alt="">':''}${esc(x.name)}</span>`).join(''):'<span class="equipment-note">暂无推荐装备</span>'}</div></div><section class="expanded-details"><div class="composition-note"><b>${r.size} 人 · 组合 ${(+r.count).toLocaleString()} 场 · 当前变阵 ${r.rosterCount??'未知'} 场</b><br>指标可能汇总其他变阵，平均等级为样本统计。</div><h3 class="team-title">全队装备速览</h3><div class="team-equipment">${r.heroes.map(u=>`<div class="team-unit ${u.id===heroId?'selected':''}"><strong>${esc(u.name)}${u.core?' · 核心':''}${u.level?' · 均级 '+Math.round(u.level):''}</strong><span>${u.items?.length?u.items.map(i=>esc(i.name)).join(' · '):'暂无推荐装备'}</span></div>`).join('')}</div></section>`;
  $('copySelected').onclick=()=>copy(r);
  $('pinLineup').onclick=()=>enterStrip(r);
  $('heroPicker').onclick=()=>{const opening=$('heroMenu').hidden;closeMenus();if(!opening)return;$('heroMenu').innerHTML=r.heroes.map((u,i)=>`<button class="hero-choice ${u.id===heroId?'active':''}" role="option" aria-selected="${u.id===heroId}" data-unit="${i}"><img src="${src(u.image)}" alt=""><span>${esc(u.name)}</span></button>`).join('');$('heroMenu').hidden=false;$('heroMenu').querySelectorAll('[data-unit]').forEach(b=>b.onclick=()=>{heroId=r.heroes[+b.dataset.unit].id;closeMenus();renderDetail();});};
  $('detail').querySelectorAll('[data-hero]').forEach(b=>b.onclick=()=>{heroId=r.heroes[+b.dataset.hero].id;const y=$('detail').scrollTop;renderDetail();$('detail').scrollTop=y;});
 }
 function enterStrip(row){
  if(!row?.heroes?.length){toast('当前阵容暂无英雄资料');return;}
  pinnedRow=JSON.parse(JSON.stringify(row));clearTimeout(timer);closeMenus();
  $('lineupStrip').innerHTML=pinnedRow.heroes.map(h=>`<article class="strip-unit" aria-label="${esc(h.name)}的推荐装备"><div class="strip-hero"><img src="${src(h.image)}" alt=""><strong>${esc(h.name)}</strong></div><div class="strip-equipment">${h.items?.length?h.items.map(i=>`<div class="strip-item" title="${esc(i.name)}">${i.image?'<img src="'+src(i.image)+'" alt="">':''}<span>${esc(i.name)}</span></div>`).join(''):'<span class="strip-no-items">暂无装备</span>'}</div></article>`).join('');
  $('lineupStrip').scrollLeft=0;$('lineupStrip').hidden=false;document.body.classList.add('strip-mode');
  cmd('stripMode',{enabled:true,heroCount:pinnedRow.heroes.length});
 }
 function leaveStrip(notify=true){
  if(!pinnedRow)return;pinnedRow=null;document.body.classList.remove('strip-mode');$('lineupStrip').hidden=true;$('lineupStrip').innerHTML='';
  if(notify)cmd('stripMode',{enabled:false});renderResult();
 }
 function copy(r){if(!r?.lineupCode){toast('数据源暂未提供此阵容码');return;}cmd('copy',{text:r.lineupCode,token:'overlay-'+(++copySeq)});}
 function resetRoundUi(prefs){leaveStrip();clearTimeout(timer);Object.assign(ui,prefs||{});ui.commanderId=String(prefs?.commanderId||'');ui.bans=[];data=null;rows=[];selectedKey='';heroId='';closeMenus();renderFilters();renderAppearance();$('candidates').innerHTML='<div class="empty-list">新一局</div>';$('detail').innerHTML='<div class="empty"><b>重新选择棋手</b>点选本局禁用后自动匹配</div>';setStatus(ui.commanderId?'常小娥 · 匹配中…':'已重置本局');if(ui.commanderId)request();}
 function newRound(){resetRoundUi();cmd('resetRound');}
 window.App={receive(e){switch(e.type){
  case 'init':if(booted)return;booted=true;Object.assign(ui,e.prefs||{});catalog=e.catalog||catalog;L.configureCatalog(catalog);ui.bans=L.cleanBans(ui.bans);catalog.commanders??=[];data=e.data?.rows?e.data:null;renderFilters();renderAppearance();renderResult();if(data?.queryKey===L.queryKey(query()))setStatus('缓存 · '+(data.version||'')+' · 检查中','loading');if(ui.commanderId)request();break;
  case 'overlayMode':if(e.mode!=='strip')leaveStrip(false);break;
  case 'roundReset':resetRoundUi(e.prefs);break;
  case 'catalog':catalog=e.catalog;L.configureCatalog(catalog);ui.bans=L.cleanBans(ui.bans);renderFilters();if(ui.commanderId)request();break;
  case 'preferences':{const before=L.queryKey(query());Object.assign(ui,e.prefs||{});renderAppearance();if(before!==L.queryKey(query())){renderFilters();if(ui.commanderId){invalidate();renderResult();request();}else{data=null;rows=[];$('candidates').innerHTML='<div class="empty-list">等待选择</div>';$('detail').innerHTML='<div class="empty">请选择本局棋手</div>';}}break;}
  case 'dataStatus':if(!ui.commanderId)setStatus(e.message,e.state==='failed'?'error':e.state==='checking'?'loading':'');break;
  case 'queryProgress':if(e.queryKey===L.queryKey(query()))setStatus(e.message,'loading');break;
  case 'status':setStatus('检查更新中…','loading');break;
  case 'queryStart':if(e.queryKey===L.queryKey(query())){invalidate();setStatus('匹配中…','loading');}break;
  case 'result':if(e.queryKey!==L.queryKey(query()))return;data=e;renderResult();setStatus((e.cached?'缓存 · ':'')+(e.version||'')+' · '+rows.length+' 套'+(e.cached?' · 检查中':''),e.cached?'loading':'');break;
  case 'queryError':if(e.queryKey!==L.queryKey(query()))return;setStatus(e.message,'error');if(!data||data.queryKey!==e.queryKey){$('candidates').innerHTML='<div class="empty-list">查询未完成</div>';$('detail').innerHTML='<div class="empty"><b>连接暂不可用</b>点击右上角刷新重试</div>';}break;
  case 'bootError':setStatus(e.message,'error');if(!data&&ui.commanderId){$('candidates').innerHTML='<div class="empty-list">离线</div>';$('detail').innerHTML='<div class="empty">连接失败，点击刷新重试</div>';}break;
  case 'copyResult':toast(e.message||(e.ok?'阵容码已复制':'复制失败'));break;
 }},back(){if(pinnedRow){leaveStrip();return;}if(['commanderMenu','heroMenu','appearance','message','exitConfirm'].some(id=>!$(id).hidden))closeMenus();else cmd('minimize');}};
 $('commanderTrigger').onclick=()=>{const opening=$('commanderMenu').hidden;closeMenus();$('commanderMenu').hidden=!opening;$('commanderTrigger').setAttribute('aria-expanded',String(opening));};
 $('appearanceTrigger').onclick=()=>{const opening=$('appearance').hidden;closeMenus();$('appearance').hidden=!opening;$('appearanceTrigger').setAttribute('aria-expanded',String(opening));};
 $('closeAppearance').onclick=$('closeMessage').onclick=closeMenus;
 $('openApp').onclick=()=>{closeMenus();cmd('openApp');};
 $('exitApp').onclick=()=>{closeMenus();$('exitConfirm').hidden=false;};
 $('cancelExit').onclick=closeMenus;
 $('confirmExit').onclick=()=>{closeMenus();cmd('exitApp');};
 $('status').onclick=()=>{const r=rows.find(r=>r.key===selectedKey);showMessage('同步状态',lastStatus+(r?'。'+r.size+' 人，组合 '+r.count+' 场，当前变阵 '+(r.rosterCount??'未知')+' 场。指标可能包含其他变阵。':'')+(data?.limited?' 候选尚未全部检索。':''));};
 $('scope').onclick=()=>showMessage('多选排除模式','已排除含所选阵营的阵容。指标来自该棋手全部禁用环境，不是多阵营同时禁用的统计。');
 $('nextRound').onclick=newRound;$('refresh').onclick=()=>{closeMenus();setStatus('检查更新中…','loading');cmd('refresh');if(ui.commanderId)request();};
 document.querySelectorAll('[data-sort]').forEach(b=>b.onclick=()=>{ui.sort=b.dataset.sort;renderFilters();invalidate();save();request();});
 for(const k of ['opacity','width','height'])$(k).oninput=()=>{ui[k]=+$(k).value;renderAppearance();save();};
 document.addEventListener('error',e=>{if(e.target.tagName==='IMG'&&e.target.src!==fallback)e.target.src=fallback;},true);
 document.addEventListener('keydown',e=>{if(e.key==='Escape')App.back();});
 function keepSelectedVisible(container,target,horizontal){
  if(!container||!target)return;const box=container.getBoundingClientRect(),item=target.getBoundingClientRect();
  if(horizontal){if(item.left<box.left)container.scrollLeft+=item.left-box.left;else if(item.right>box.right)container.scrollLeft+=item.right-box.right;}
  else{if(item.top<box.top)container.scrollTop+=item.top-box.top;else if(item.bottom>box.bottom)container.scrollTop+=item.bottom-box.bottom;}
 }
 new ResizeObserver(()=>{keepSelectedVisible($('candidates'),$('candidates').querySelector('.selected'),innerWidth<340);if(innerHeight<=205)keepSelectedVisible($('detail').querySelector('.roster'),$('detail').querySelector('.hero.selected'),true);}).observe(document.querySelector('.workspace'));
 if(window.Native){try{App.receive(JSON.parse(Native.initialState()));}catch(e){setStatus('界面初始化失败，请重开','error');}}else{renderFilters();renderAppearance();setStatus('对局面板预览');}
})();
