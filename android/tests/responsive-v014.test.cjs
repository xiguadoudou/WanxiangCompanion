const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('node:assert/strict'),{pathToFileURL}=require('url');
(async()=>{
 const f=JSON.parse(fs.readFileSync('tests/fixtures/copy-ui-fixture.json'));
 const b=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,args:['--no-sandbox']});
 try{
 const p=await b.newPage({viewport:{width:390,height:228},deviceScaleFactor:2});const errors=[];p.on('pageerror',e=>errors.push(e.message));
 await p.addInitScript(f=>{window.sent=[];window.Native={initialState:()=>JSON.stringify({type:'init',ready:true,catalog:f.catalog,data:f.data,prefs:{...f.data.query,opacity:88,width:392,height:230}}),command:x=>sent.push(JSON.parse(x))};},f);
 await p.goto(pathToFileURL(process.cwd()+'/assets/overlay.html').href);await p.waitForSelector('.candidate');
 // Mock only Android's native chrome for local screenshots; resizing is tested in native geometry tests.
 await p.evaluate(()=>{document.body.style.background='#152630';const icon=document.createElement('div');icon.id='nativeIconMock';icon.style='position:fixed;left:1px;top:1px;width:44px;height:44px;pointer-events:none;z-index:30;display:grid;place-items:center';icon.innerHTML='<svg width="40" height="40" viewBox="0 0 40 40"><circle cx="20" cy="20" r="18" fill="#1b303b" stroke="#9dcbbc" stroke-opacity=".45"/><path d="M11 15H19M11 24H29M15 11V29M24 11V29" fill="none" stroke="#9db8b3" stroke-width="1.25" stroke-linecap="round"/><circle cx="24" cy="15" r="4.3" fill="#a7e5cb" stroke="#1b303b" stroke-width="1.4"/></svg>';document.body.append(icon);const grip=document.createElement('div');grip.id='nativeResizeMock';grip.style='position:fixed;right:1px;bottom:1px;width:32px;height:32px;pointer-events:none;z-index:30';grip.innerHTML='<svg width="32" height="32"><path d="M11 25L25 11M17 25L25 17M23 25L25 23" stroke="#b3e9d6" stroke-opacity=".65" stroke-width="1.6" stroke-linecap="round"/></svg>';document.body.append(grip);});
 await p.locator('[data-pick="3"]').click();await p.locator('[data-hero="1"]').click();
 const selected=await p.locator('.candidate.selected .pick').getAttribute('aria-label'),hero=await p.locator('.hero.selected').getAttribute('aria-label');
 await p.waitForTimeout(400);const nativeQueryCount=await p.evaluate(()=>sent.filter(x=>x.action==='query').length);const summary=[];
 for(const [width,height] of [[390,228],[640,360],[280,240],[560,178],[310,440],[250,170],[780,430],[340,206]]){
  await p.setViewportSize({width,height});await p.waitForTimeout(60);
  assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth&&document.documentElement.scrollHeight<=innerHeight),'Window must never overflow');
  assert.equal(await p.locator('.candidate.selected .pick').getAttribute('aria-label'),selected,'Resize cannot reset selected lineup');assert.equal(await p.locator('.hero.selected').getAttribute('aria-label'),hero,'Resize cannot reset selected hero');
  const rail=await p.locator('.candidates').evaluate(e=>getComputedStyle(e).display==='flex');assert.equal(rail,width<340);
  if(rail){assert.ok(await p.locator('.candidates').evaluate(e=>e.scrollWidth>e.clientWidth));const cb=await p.locator('.candidates').boundingBox(),db=await p.locator('.detail').boundingBox();assert.ok(db.y>=cb.y+cb.height,'Narrow windows must stack rail above full-width detail');}
  const extended=await p.locator('.expanded-details').isVisible();assert.equal(extended,true,"All window sizes retain full team details");
  const order=await p.locator('.equipment').evaluate(e=>getComputedStyle(e).order);if(height<=205)assert.equal(order,'2','Short windows prioritize equipment before portrait rail');
  const commander=await p.locator('#commanderTrigger').boundingBox();assert.ok(commander.x>=45,'Native toggle has its own header hit area');
  await p.screenshot({path:`build/v014-${width}x${height}.png`});summary.push({width,height,layout:rail?'rail':'split',extended});
 }
 await p.setViewportSize({width:250,height:170});assert.equal(await p.locator('.roster').isVisible(),false,'Smallest layout must use the hero menu instead of clipped portraits');await p.locator('#heroPicker').click();assert.equal(await p.locator('#heroMenu [role=option]').count(),f.data.rows[3].heroes.length);await p.locator('#heroMenu [role=option]').first().click();assert.equal(await p.locator('#heroMenu').isVisible(),false);await p.setViewportSize({width:640,height:360});await p.locator('[data-hero="1"]').click();
 assert.equal(await p.evaluate(()=>sent.filter(x=>x.action==='query').length),nativeQueryCount,'Live resizing must not issue data queries');
 // Native preferences emitted after a drag must preserve the actual detail DOM and scroll state.
 await p.setViewportSize({width:640,height:360});await p.locator('.detail').evaluate(e=>{e.scrollTop=65;e.dataset.marker='kept'});
 await p.evaluate(()=>App.receive({type:'preferences',prefs:{width:642,height:362,opacity:71}}));assert.equal(await p.locator('.detail').getAttribute('data-marker'),'kept');assert.ok(await p.locator('.detail').evaluate(e=>e.scrollTop)>0);assert.equal(await p.locator('.hero.selected').getAttribute('aria-label'),hero);
 const i=f.data.rows.findIndex(r=>r.lineupCode);await p.locator('[data-pick="'+i+'"]').click();await p.locator('#copySelected').click();assert.equal(await p.evaluate(()=>sent.filter(x=>x.action==='copy').at(-1).data.text),f.data.rows[i].lineupCode);
 assert.deepEqual(errors,[]);console.log(JSON.stringify(summary));console.log('Responsive reflow, density tiers, native-header reservation, retained selection/scroll, zero resize queries and copy passed.');
 }finally{await b.close();}
})().catch(e=>{console.error(e);process.exit(1)});
