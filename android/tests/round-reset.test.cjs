const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('node:assert/strict'),{pathToFileURL}=require('url');
(async()=>{
 const f=JSON.parse(fs.readFileSync('tests/fixtures/copy-ui-fixture.json'));
 const b=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,args:['--no-sandbox']});
 try{
 for(const overlay of [false,true]){
  const p=await b.newPage({viewport:overlay?{width:390,height:228}:{width:390,height:844}});const errors=[];p.on('pageerror',e=>errors.push(e.message));await p.route('https://**/*',r=>r.abort());
  await p.addInitScript(({f,overlay})=>{window.sent=[];window.Native={initialState:()=>JSON.stringify({type:'init',overlay,catalog:f.catalog,data:f.data,prefs:{...f.data.query,opacity:61,width:420,height:310},updater:{state:'current',available:false,installedVersion:'0.1.8'}}),command:x=>sent.push(JSON.parse(x))};},{f,overlay});
  await p.goto(pathToFileURL(process.cwd()+'/assets/'+(overlay?'overlay.html':'index.html')).href);await p.waitForSelector(overlay?'.candidate':'.card');
  const before=await p.evaluate(()=>sent.filter(x=>x.action==='query').length);await p.evaluate(()=>App.receive({type:'roundReset',prefs:{commanderId:'',bans:[],minCount:100,sort:'avgPlacement',days:7,opacity:61,width:420,height:310}}));
  assert.equal(await p.locator('.ban[aria-pressed=true]').count(),0);assert.equal(await p.locator(overlay?'.candidate':'.card').count(),0);assert.equal(await p.locator(overlay?'#copySelected':'.copy-card').count(),0);
  assert.match(await p.locator(overlay?'#commanderTrigger':'#commanderButton').innerText(),/选择棋手|选择你的棋手/);
  await p.waitForTimeout(450);assert.equal(await p.evaluate(()=>sent.filter(x=>x.action==='query').length),before,'Reset cancels pending query timers');
  await p.evaluate(data=>App.receive({...data,type:'result'}),f.data);assert.equal(await p.locator(overlay?'.candidate':'.card').count(),0,'An old result cannot restore the previous round');
  assert.equal(await p.evaluate(()=>getComputedStyle(document.documentElement).getPropertyValue('--surface-alpha').trim()),'0.61');
  if(!overlay){await p.locator('#settingsButton').click();assert.equal(await p.locator('[data-min="100"]').getAttribute('class'),'pill active');assert.equal(await p.locator('#width').inputValue(),'420');assert.equal(await p.locator('#height').inputValue(),'310');await p.locator('#back').click();}
  await p.locator(overlay?'#nextRound':'#next').click({force:true});assert.equal(await p.evaluate(()=>sent.at(-1).action),'resetRound','Manual new round uses the same native cancellation/reset');
  const catalog=JSON.parse(fs.readFileSync('assets/catalog-snapshot.json'));await p.evaluate(c=>App.receive({type:'catalog',catalog:c}),catalog);
  const afterClear=await p.evaluate(()=>sent.filter(x=>x.action==='query').length);
  await p.evaluate(()=>App.receive({type:'roundReset',prefs:{commanderId:'commander-r11',bans:[],minCount:100,sort:'avgPlacement',days:7,opacity:61,width:420,height:310}}));
  assert.match(await p.locator(overlay?'#commanderTrigger':'#commanderButton').innerText(),/常小娥/);assert.equal(await p.locator('.ban[aria-pressed=true]').count(),0);
  await p.waitForTimeout(450);assert.equal(await p.evaluate(()=>sent.filter(x=>x.action==='query').length),afterClear+1,'Returning to main automatically queries the default player once');
  const query=await p.evaluate(()=>sent.filter(x=>x.action==='query').at(-1).data);assert.equal(query.commanderId,'commander-r11');assert.equal(query.commanderName,'常小娥');assert.equal(query.minCount,100);assert.deepEqual(query.bans,[]);
  await p.evaluate(data=>App.receive({...data,type:'result'}),f.data);assert.equal(await p.locator(overlay?'.candidate':'.card').count(),0,'Prior different-player result is ignored after default reset');
  assert.deepEqual(errors,[]);await p.close();
 }
 console.log('Main entry defaults to 常小娥 and queries once; main and overlay reset chess player, bans, candidates and stale results; retain display/sample settings; pending queries cancelled.');
 }finally{await b.close();}
})().catch(e=>{console.error(e);process.exit(1)});
