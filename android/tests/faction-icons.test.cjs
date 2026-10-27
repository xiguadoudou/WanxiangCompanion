const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('node:assert/strict'),{pathToFileURL}=require('url');
(async()=>{
 const b=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,args:['--no-sandbox']});try{
 const c=JSON.parse(fs.readFileSync('assets/catalog-snapshot.json')),fixture=JSON.parse(fs.readFileSync('tests/fixtures/ui-fixture.json'));
 const p=await b.newPage({viewport:{width:390,height:844},deviceScaleFactor:2});const errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.route('https://**/*',r=>r.abort());
 await p.addInitScript(c=>{window.sent=[];window.Native={initialState:()=>JSON.stringify({type:'init',catalog:c,prefs:{},updater:{state:'current',available:false,installedVersion:'0.1.7'}}),command:x=>sent.push(JSON.parse(x))};},c);
 await p.goto(pathToFileURL(process.cwd()+'/assets/index.html').href);await p.waitForFunction(()=>[...document.querySelectorAll('.faction-symbol img')].filter(i=>i.naturalWidth===37).length===5);
 const paths=['heluo','zhulu','riluohai','sanfen-zhidi','dahe-liuyu'];assert.deepEqual(await p.locator('.faction-symbol img').evaluateAll(xs=>xs.map(i=>i.getAttribute('src'))),paths.map(n=>'factions/'+n+'.png'));
 assert.equal(await p.locator('.faction-fallback:visible').count(),0,'No substitute glyphs when game icons are available');assert.equal(await p.locator('.ban').count(),5);await p.screenshot({path:'build/faction-home-v017.png',fullPage:true});
 await p.getByRole('button',{name:'河洛，点击禁用',exact:true}).click();assert.equal(await p.getByRole('button',{name:'河洛，已禁用，再次点击取消',exact:true}).getAttribute('aria-pressed'),'true');
 const next={...c,factions:[...c.factions,{name:'测试阵营',image:'https://example.com/factions/new.png'}]};await p.evaluate(c=>App.receive({type:'catalog',catalog:c}),next);assert.equal(await p.locator('.ban').count(),6,'New source factions appear without editing UI code');
 await p.getByRole('button',{name:'测试阵营，点击禁用',exact:true}).click();await p.locator('#clearBans').click();await p.locator('#commanderButton').click();await p.getByRole('option',{name:'小妲己',exact:true}).click();await p.waitForTimeout(420);
 const query=await p.evaluate(()=>sent.filter(x=>x.action==='query').at(-1).data);const key=JSON.stringify([query.commanderId,[],query.sort,query.days,query.minCount]);
 const row=fixture.data.rows[0];await p.evaluate(({row,key})=>App.receive({type:'result',queryKey:key,rows:[row],version:'v261022',fetchedAt:Date.now()}),{row,key});assert.equal(await p.locator('.card').count(),1);
 const extra={...row,key:'new-lineup-regression',title:'新增组合回归验证'};await p.evaluate(({row,extra,key})=>App.receive({type:'result',queryKey:key,rows:[row,extra],version:'v261022',fetchedAt:Date.now()}),{row,extra,key});assert.equal(await p.locator('.card').count(),2,'New server lineup rows render automatically');assert.match(await p.locator('#results').innerText(),/新增组合回归验证/);
 await p.evaluate(()=>App.receive({type:'roundReset',prefs:{commanderId:'',bans:[],minCount:10}}));assert.equal(await p.locator('.card').count(),0);assert.equal(await p.locator('.ban[aria-pressed=true]').count(),0);assert.match(await p.locator('#commanderButton').innerText(),/选择你的棋手/);
 assert.deepEqual(errors,[]);console.log('Five original faction icons load offline, ban toggles, new catalog factions and new lineup rows render, round reset passed.');
 }finally{await b.close();}
})().catch(e=>{console.error(e);process.exit(1)});
