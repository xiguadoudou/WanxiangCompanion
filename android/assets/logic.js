(function(root){
  'use strict';
  const DEFAULT_FACTIONS=['河洛','逐鹿','日落海','三分之地','大河流域'],FACTIONS=[...DEFAULT_FACTIONS];
  const BUNDLED={河洛:'heluo',逐鹿:'zhulu',日落海:'riluohai',三分之地:'sanfen-zhidi',大河流域:'dahe-liuyu'};let factionImages={};
  function configureCatalog(catalog){
    const records=(catalog?.factions||[]).filter(x=>typeof x?.name==='string'&&x.name.length>0&&x.name.length<=40&&x.name!=='无阵营');factionImages=Object.fromEntries(records.map(x=>[x.name,x.image||'']));
    if(!records.length){FACTIONS.splice(0,FACTIONS.length,...DEFAULT_FACTIONS);return;}const names=new Set(records.map(x=>x.name));const ordered=DEFAULT_FACTIONS.filter(x=>names.has(x));ordered.push(...[...names].filter(x=>!ordered.includes(x)).sort());FACTIONS.splice(0,FACTIONS.length,...ordered);
  }
  function bundledFactionImage(name){return Object.prototype.hasOwnProperty.call(BUNDLED,name)?'factions/'+BUNDLED[name]+'.png':'';}
  function factionImage(name){const remote=factionImages[name],slug=Object.prototype.hasOwnProperty.call(BUNDLED,name)?BUNDLED[name]:'';return /^https:\/\//.test(remote||'')&&remote!=='https://static.datatft.com/wxq/wxq-database/factions/'+slug+'.png'?remote:bundledFactionImage(name);}

  function cleanBans(bans){return FACTIONS.filter(x=>(bans||[]).includes(x));}
  function makePayload(q,version,page=1){
    const bans=cleanBans(q.bans), filters=[];
    if(!q.commanderId) throw Error('请先选择开局棋手');
    filters.push({type:'commander',id:q.commanderId,commanderName:q.commanderName||'',switchVal:true});
    if(bans.length===1) filters.push({type:'faction',id:bans[0],banned:true,switchVal:true});
    return {version,gameType:'2',time:[1,2,3,5,7].includes(+q.days)?+q.days:7,operator:'AND',advancedMode:false,filters,exclusions:[],page,pageSize:12,minimumCount:Math.max(1,+q.minCount||10),sortBy:['appearanceRate','avgPlacement','firstRate','top3Rate'].includes(q.sort)?q.sort:'avgPlacement'};
  }
  function allowed(lineup,bans){
    const blocked=new Set(cleanBans(bans));
    if(!blocked.size) return true;
    // Unknown affiliations must not silently pass an exclusion filter.
    return (lineup.heroes||[]).length>0 && lineup.heroes.every(h=>h.faction && !blocked.has(h.faction));
  }
  function rank(rows,sort){
    return [...rows].sort((a,b)=>{
      let d=sort==='firstRate'?b.firstRate-a.firstRate:sort==='top3Rate'?b.top3Rate-a.top3Rate:sort==='appearanceRate'?b.appearanceRate-a.appearanceRate:a.avgPlacement-b.avgPlacement;
      return d || b.count-a.count;
    });
  }
  function queryKey(q){return JSON.stringify([String(q.commanderId||''),cleanBans(q.bans),q.sort||'avgPlacement',+q.days||7,+q.minCount||10]);}
  const api={FACTIONS,configureCatalog,factionImage,bundledFactionImage,cleanBans,makePayload,allowed,rank,queryKey};
  if(typeof module!=='undefined'&&module.exports) module.exports=api;
  root.WxqLogic=api;
})(typeof globalThis!=='undefined'?globalThis:this);
