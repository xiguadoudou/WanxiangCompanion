/* A left swipe on ordinary page content opens app navigation. */
(()=>{'use strict';
 const excluded='input,textarea,select,[contenteditable="true"],[role="slider"],[draggable="true"],dialog,.el-dialog,.el-select,.el-color-picker,.wxq-plan-menu,.dropdown,.simulator-map,.simple-simulator-map,.hero-item,.equip-table,.simulator-player-library-list,.simulator-talent-library-list,.simulator-mobile-selection-grid,.roster,.bans,.sorts,.h5-db-tabs,.h5-chips,[data-navigation-gesture="ignore"]';
 function blocked(target){
  if(!(target instanceof Element)||target.closest(excluded))return true;
  for(let e=target;e&&e!==document.body;e=e.parentElement){const s=getComputedStyle(e);if(/auto|scroll/.test(s.overflowX)&&e.scrollWidth>e.clientWidth+2)return true;}
  return false;
 }
 function install({open}){
  if(document.documentElement.dataset.wxqNavigationGesture)return;
  document.documentElement.dataset.wxqNavigationGesture='left';let start=null,suppressUntil=0;
  function move(e){
   if(!start)return;if(e.touches.length!==1){start=null;return;}const t=e.touches[0];if(t.identifier!==start.id){start=null;return;}
   start.dx=t.clientX-start.x;start.dy=t.clientY-start.y;
   if(start.dx>16||Math.abs(start.dy)>12&&Math.abs(start.dy)*1.5>Math.abs(start.dx)){start=null;return;}
   if(start.dx<-16&&Math.abs(start.dx)>Math.abs(start.dy)*1.7&&e.cancelable)e.preventDefault();
  }
  function unwatch(){document.removeEventListener('touchmove',move,true);}
  document.addEventListener('touchstart',e=>{unwatch();suppressUntil=0;if(e.touches.length!==1||blocked(e.target)){start=null;return;}const t=e.touches[0];start={id:t.identifier,x:t.clientX,y:t.clientY,dx:0,dy:0,time:Date.now()};document.addEventListener('touchmove',move,{capture:true,passive:false});},{capture:true,passive:true});
  document.addEventListener('touchend',e=>{
   const s=start;start=null;unwatch();if(!s||e.touches.length)return;
   if(s.dx<=-56&&Math.abs(s.dx)>Math.abs(s.dy)*1.7&&Date.now()-s.time<1500){if(e.cancelable)e.preventDefault();suppressUntil=Date.now()+350;setTimeout(open,0);}
  },{capture:true,passive:false});
  document.addEventListener('touchcancel',()=>{start=null;unwatch();},{capture:true,passive:true});
  document.addEventListener('click',e=>{if(Date.now()<suppressUntil){e.preventDefault();e.stopImmediatePropagation();}},true);
 }
 window.WxqNavigationGesture={install};
})();
