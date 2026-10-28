// Authorized UI/components run locally. Server-only actions are replaced by app-owned plans.
(()=>{'use strict';try{if(!localStorage.getItem('wxq-theme-mode'))localStorage.setItem('wxq-theme-mode','dark');}catch(e){}
const hideServerControls=()=>document.querySelectorAll('#simulator-settings .setting-btn,#simulator-settings .map-btn').forEach(el=>{if(/分享阵容|制作一图流|保存阵容图|保存横向图/.test((el.textContent||'').trim()))el.classList.add('wxq-port-hidden');});
new MutationObserver(hideServerControls).observe(document.getElementById('app'),{childList:true,subtree:true});
document.addEventListener('error',e=>{const img=e.target;if(img.tagName!=='IMG')return;e.stopImmediatePropagation();if(img.dataset.wxqFallback)return;img.dataset.wxqFallback='1';const label=(img.alt||'弈').slice(0,2).replace(/[<>&"']/g,'');img.src='data:image/svg+xml,'+encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="72" height="72"><rect width="72" height="72" rx="12" fill="#253c43"/><text x="36" y="44" text-anchor="middle" font-size="24" fill="#b7d4c8">'+label+'</text></svg>');},true);
const pictures=new Map();function image(url){if(!url)return Promise.resolve(null);if(!pictures.has(url))pictures.set(url,new Promise(resolve=>{const i=new Image();i.crossOrigin='anonymous';i.onload=()=>resolve(i);i.onerror=()=>resolve(null);i.src=url;}));return pictures.get(url);}
async function exportImage(horizontal=false,transparent=false){
 const raw=new URL(location.href).searchParams.get('lineup');if(!raw)throw Error('先向棋盘添加英雄');
 const plan=JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(raw.replace(/-/g,'+').replace(/_/g,'/')),c=>c.charCodeAt(0))));
 const arrays=window.__WXQ_DATA__.arrays,heroes=new Map([...arrays.wanxiang_hero,...(arrays.specialHeroes||[])].map(h=>[String(h.id),h])),items=new Map([...arrays.wanxiang_equipment,...(arrays.diamond_equipment||[])].map(h=>[String(h.id),h]));
 const units=plan.b.map((u,i)=>u?{u,i}:null).filter(Boolean);if(!units.length)throw Error('棋盘上还没有英雄');
 const enemy=!horizontal&&units.some(x=>x.i>=28),canvas=document.createElement('canvas');canvas.width=horizontal?Math.max(360,units.length*116+32):1120;canvas.height=horizontal?206:enemy?1190:660;const ctx=canvas.getContext('2d');
 if(!transparent){ctx.fillStyle='#101a27';ctx.fillRect(0,0,canvas.width,canvas.height);}ctx.fillStyle='#e4ece7';ctx.font='bold 25px system-ui';ctx.textAlign='left';ctx.fillText('万象弈伴 · 阵容方案',22,38);ctx.font='13px system-ui';ctx.fillStyle='#9fb4b7';ctx.fillText('资料来源：万象棋大数据',22,60);
 async function unit(entry,x,y,size){const h=heroes.get(String(entry.u[0])),a=h?.assets||{},portrait=await image(a.avatar||a.thumbnail||a.image);ctx.fillStyle='#293b4a';ctx.fillRect(x,y,size,size);if(portrait)ctx.drawImage(portrait,x,y,size,size);ctx.fillStyle='#edf5ef';ctx.textAlign='center';ctx.font='bold 15px system-ui';ctx.fillText(h?.name||'特殊单位',x+size/2,y+size+19);const equipment=entry.u[1]||[];for(let j=0;j<equipment.length;j++){const item=items.get(String(equipment[j])),icon=await image(item?.assets?.thumbnail||item?.assets?.image);const ix=x+j*(size/3+1),iy=y+size+27;if(icon)ctx.drawImage(icon,ix,iy,size/3-2,size/3-2);}ctx.textAlign='left';}
 if(horizontal){for(let k=0;k<units.length;k++)await unit(units[k],20+k*116,79,72);}
 else{const shown=enemy?56:28;for(let k=0;k<shown;k++){const side=Math.floor(k/28),cell=k%28,row=Math.floor(cell/7),col=cell%7,x=76+col*134+(row%2)*54,y=91+side*530+row*126;ctx.fillStyle='#202e40';ctx.fillRect(x,y,112,112);const u=units.find(e=>e.i===k);if(u)await unit(u,x+20,y+4,72);}}
 const value=canvas.toDataURL('image/png');if(window.prompt('__WXQ_EXPORT_PNG__',value)!=='ok')throw Error('图片保存未完成');return value;
}
window.WxqLocalPort={exportImage:(horizontal,transparent)=>exportImage(horizontal,transparent).catch(e=>window.alert(e.message))};
})();
