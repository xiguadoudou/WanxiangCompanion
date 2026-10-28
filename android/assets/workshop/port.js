// Authorized UI/components run locally. Server-only actions are replaced by app-owned plans.
(()=>{'use strict';try{localStorage.setItem('wxq-theme-mode','dark');}catch(e){}
const database=location.pathname==='/h5/database',modulePaths=database?['/h5/database']:['/team-builder','/simulator'];
function ownsUrl(value){try{const u=new URL(value,location.href);return u.origin===location.origin&&modulePaths.includes(u.pathname);}catch(e){return false;}}
let toastTimer;function navigationNote(){let t=document.getElementById('wxq-port-toast');if(!t){t=document.createElement('div');t.id='wxq-port-toast';t.className='wxq-port-toast';t.setAttribute('role','status');document.body.appendChild(t);}t.textContent='请从左上角侧边栏切换软件功能';t.hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(()=>{t.hidden=true;},2500);}
// Vue navigation does not cause a WebView page request. Guard its router as well
// as links/history, while preserving query updates for plans and library filters.
for(const method of ['pushState','replaceState']){const original=history[method];history[method]=function(state,title,url){if(url!=null&&!ownsUrl(url)){navigationNote();throw new DOMException('Use the app sidebar to change modules','SecurityError');}return original.apply(this,arguments);};}
document.addEventListener('click',e=>{const a=e.target.closest&&e.target.closest('a[href]');if(a&&!ownsUrl(a.href)){e.preventDefault();e.stopImmediatePropagation();navigationNote();}},true);
window.open=()=>{navigationNote();return null;};
const shellNavigation='.ep-header,.ep-bottom-nav,.ep-mobile-more,.ep-mobile-more__backdrop,.h5-header,.h5-bottom-nav,.h5-tabbar,.site-footer,.ep-footer,.app-footer';
const header=document.createElement('header');header.className='wxq-app-header';
header.innerHTML=`<div class="wxq-brand"><button class="wxq-app-mark" data-action="navigation" aria-label="打开功能侧边栏"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M4 6h16M4 12h16M4 18h16"/></svg></button><div><strong>万象弈伴</strong><small id="wxq-module-status">${database?'万象资料库':'阵容模拟'} · 本地可用</small></div></div><div class="wxq-header-tools">${database?'':'<button class="wxq-tool wxq-plan-trigger" aria-label="阵容方案" aria-expanded="false">方案</button>'}<button class="wxq-tool" data-action="refresh" aria-label="同步资料并刷新"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="M20 4v6h-6M20 10a8 8 0 1 0 0 5"/></svg></button><button class="wxq-tool" data-action="home" aria-label="返回阵容推荐"><svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path d="m15 18-6-6 6-6"/></svg></button></div>`;
const app=document.getElementById('app');app.before(header);document.body.dataset.module=database?'database':'simulator';
function appAction(action){window.prompt('__WXQ_APP_ACTION__',action);}
header.addEventListener('click',e=>{const b=e.target.closest('button[data-action]');if(b)appAction(b.dataset.action);});
if(!database){
 const trigger=header.querySelector('.wxq-plan-trigger'),menu=document.createElement('div');menu.className='wxq-plan-menu';menu.hidden=true;menu.setAttribute('role','menu');
 for(const [action,label] of [['save','保存当前方案'],['open','打开已保存方案'],['copy','复制软件方案码'],['import','导入软件方案码'],['export','保存阵容图'],['export-strip','保存横向图']]){const b=document.createElement('button');b.textContent=label;b.setAttribute('role','menuitem');b.onclick=()=>{menu.hidden=true;trigger.setAttribute('aria-expanded','false');if(action.startsWith('export'))exportImage(action==='export-strip');else appAction(action);};menu.appendChild(b);}
 header.appendChild(menu);trigger.onclick=()=>{menu.hidden=!menu.hidden;trigger.setAttribute('aria-expanded',String(!menu.hidden));};document.addEventListener('click',e=>{if(!header.contains(e.target)){menu.hidden=true;trigger.setAttribute('aria-expanded','false');}});
}
let guardedRouter;
function integrate(){
 document.querySelectorAll(shellNavigation).forEach(el=>{el.hidden=true;el.setAttribute('inert','');el.setAttribute('aria-hidden','true');});
 document.querySelectorAll('#simulator-settings .setting-btn,#simulator-settings .map-btn').forEach(el=>{if(/分享阵容|制作一图流|保存阵容图|保存横向图/.test((el.textContent||'').trim())){el.classList.add('wxq-port-hidden');el.setAttribute('inert','');}});
 document.querySelectorAll('.setting-left-line').forEach(el=>{if(el.children.length&&[...el.children].every(c=>c.classList.contains('wxq-port-hidden')))el.classList.add('wxq-port-hidden');});
 const main=document.querySelector(database?'.h5-main':'.simulator-view');if(main&&!main.querySelector('.wxq-workshop-intro')){const intro=document.createElement('section');intro.className='wxq-workshop-intro';intro.innerHTML=database?'<small>WANXIANG · LIBRARY</small><h1>万象资料，<em>随手可查。</em></h1><p>英雄、装备与天赋，按需查阅，离线也清晰。</p>':'<small>WANXIANG · YOUR LINEUP</small><h1>落子之前，<em>先有章法。</em></h1><p>搭配英雄与装备，把这一局的思路摆出来。</p>';main.insertBefore(intro,main.firstChild);const credit=document.createElement('div');credit.className='wxq-workshop-credit';credit.textContent='软件由 ChatGPT 构建';main.appendChild(credit);}
 const router=document.getElementById('app').__vue_app__?.config.globalProperties.$router;if(router&&router!==guardedRouter){guardedRouter=router;router.beforeEach(to=>{if(ownsUrl(to.fullPath))return true;navigationNote();return false;});}
}
new MutationObserver(integrate).observe(document.getElementById('app'),{childList:true,subtree:true});integrate();
document.addEventListener('error',e=>{const img=e.target;if(img.tagName!=='IMG')return;e.stopImmediatePropagation();if(img.dataset.wxqFallback)return;img.dataset.wxqFallback='1';const label=(img.alt||'弈').slice(0,2).replace(/[<>&"']/g,'');img.src='data:image/svg+xml,'+encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="72" height="72"><rect width="72" height="72" rx="12" fill="#253c43"/><text x="36" y="44" text-anchor="middle" font-size="24" fill="#b7d4c8">'+label+'</text></svg>');},true);
let exporting=false;
async function exportImage(horizontal=false,transparent=false){
 if(exporting)return;exporting=true;let stage;
 try{
  const raw=new URL(location.href).searchParams.get('lineup');if(!raw)throw Error('请先添加英雄、棋手或天赋牌');
  const manifest=await(await fetch('/port-manifest.json')).json();
  // Reuse the packaged capture library and the actual board DOM, including
  // special units, equipment, backups, players and talents. Never re-layout data.
  const {h:capture}=await import('/assets/'+manifest.desktop);
  const board=document.querySelector('.board-capture-shell');if(!board)throw Error('棋盘尚未就绪');
  let target=board;
  if(horizontal){
   target=board.querySelector('.simple-simulator-map-wrapper');
   if(!target){
    const units=[...board.querySelectorAll('.simulator-map .map-item-wrapper')].filter(e=>e.querySelector('.hero-icon__portrait'));
    if(!units.length)throw Error('请先向棋盘添加英雄');
    stage=document.createElement('div');stage.className='board-capture-shell main-wrapper wxq-horizontal-export';stage.style.cssText='position:absolute;left:-10000px;top:0;width:max-content;padding:5px';
    const row=document.createElement('div');row.className='simulator-map';row.style.cssText='display:flex;gap:4px;width:max-content;height:50px;padding:0;margin:0';
    for(const unit of units){const clone=unit.cloneNode(true);clone.style.cssText='position:relative;display:block;width:40px;height:50px;margin:0';row.appendChild(clone);}
    stage.appendChild(row);document.body.appendChild(stage);target=stage;
   }
  }
  await document.fonts.ready;
  const pending=[...target.querySelectorAll('img')].filter(i=>!i.complete);
  await Promise.all(pending.map(i=>new Promise(resolve=>{i.addEventListener('load',resolve,{once:true});i.addEventListener('error',resolve,{once:true});setTimeout(resolve,6000);})));
  const canvas=await capture(target,{useCORS:true,allowTaint:false,backgroundColor:transparent?null:'#f7f9fc',scale:2,logging:false,onclone:doc=>{
   const css=doc.createElement('style');css.textContent=`
    .board-capture-shell{--simulator-grid-bg:#edf2f7!important;--champion-background-color:#edf2f7!important;--simulator-cell-bg:#edf2f7!important;background:${transparent?'transparent':'#f7f9fc'}!important;color:#909399!important}
    .board-capture-shell *{animation:none!important;transition:none!important;backdrop-filter:none!important;-webkit-backdrop-filter:none!important}
    html.dark .wxq-horizontal-export{border:0!important;border-radius:0!important;box-shadow:none!important;background:${transparent?'transparent':'#f7f9fc'}!important}
    .board-capture-shell .simulator-map,.board-capture-shell .simulator-mobile-side-panels,.board-capture-shell .simulator-mobile-side-panel{background:transparent!important}
    .board-capture-shell .map-item-hero{background:transparent!important}
    .board-capture-shell .map-item{background:#edf2f7!important}
    .board-capture-shell .simulator-mobile-selection-grid{background:${transparent?'transparent':'#f7f9fc'}!important;border:1px solid #e4e7ed!important;box-shadow:none!important}
    .board-capture-shell .simulator-mobile-side-title,.board-capture-shell .simulator-selected-title{color:#909399!important}
    .board-capture-shell .simulator-player-item,.board-capture-shell .simulator-selected-talent-item{background:#eef2f6!important;border-color:#e4e7ed!important}
    .board-capture-shell .simulator-player-name,.board-capture-shell .simulator-selected-talent-name{color:#596773!important;overflow:visible!important;min-height:18px;line-height:18px}
   `;doc.head.appendChild(css);
  }});
  const value=canvas.toDataURL('image/png');if(window.prompt('__WXQ_EXPORT_PNG__',value)!=='ok')throw Error('图片保存未完成');return value;
 }catch(e){window.alert(e.message);}finally{if(stage)stage.remove();exporting=false;}
}
window.WxqLocalPort={exportImage};
})();
