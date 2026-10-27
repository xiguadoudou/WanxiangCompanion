const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('node:fs'),assert=require('node:assert/strict'),{pathToFileURL}=require('node:url');
(async()=>{
 const fixture=JSON.parse(fs.readFileSync('tests/fixtures/copy-ui-fixture.json'));
 const browser=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,headless:true,args:['--no-sandbox','--disable-dev-shm-usage']});
 const summary=[];
 try{
  for(const [width,height] of [[390,228],[350,228],[310,228],[280,208],[430,178]]){
   const p=await browser.newPage({viewport:{width,height},deviceScaleFactor:2});const errors=[];p.on('pageerror',e=>errors.push(e.message));
   await p.addInitScript(f=>{
    window.sentCommands=[];window.Native={initialState:()=>JSON.stringify({type:'init',ready:true,catalog:f.catalog,data:f.data,prefs:{...f.data.query,opacity:88,width:392,height:230,compactOverlayVersion:3}}),command:s=>{const c=JSON.parse(s);sentCommands.push(c);if(c.action==='copy')setTimeout(()=>App.receive({type:'copyResult',ok:true,message:'阵容码已复制'}),0);}};
   },fixture);
   await p.goto(pathToFileURL(process.cwd()+'/assets/overlay.html').href,{waitUntil:'domcontentloaded'});await p.waitForSelector('.candidate');
   assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth&&document.documentElement.scrollHeight<=innerHeight));
   const workspace=await p.locator('.workspace').boundingBox();
   const visible=await p.locator('.candidate').evaluateAll((xs,bottom)=>xs.filter(x=>x.getBoundingClientRect().bottom<=bottom+.5).length,workspace.y+workspace.height);
   summary.push({width,height,fullyVisibleLineups:visible});
   if(width===390)assert.ok(visible>=5,'Default smaller panel must show at least five complete lineup choices');
   assert.equal(await p.locator('.copy-mini,.sample,.detail-title small,.note').count(),0,'Repeated sample, copy, and explanation rows are removed');
   const i=fixture.data.rows.findIndex(r=>r.lineupCode);await p.locator('[data-pick="'+i+'"]').click();
   const box=await p.locator('.detail').boundingBox();const eq=await p.locator('.equipment').boundingBox();
   if(width===390)assert.ok(eq.y+eq.height<=box.y+box.height+1,'Default view must show equipment without scrolling');
   await p.locator('#copySelected').click();await p.waitForFunction(()=>document.getElementById('toast').textContent==='阵容码已复制');
   assert.equal(await p.evaluate(()=>sentCommands.filter(x=>x.action==='copy').at(-1).data.text),fixture.data.rows[i].lineupCode);
   if(width===390){await p.evaluate(()=>{document.documentElement.style.background='#132431';document.getElementById('toast').hidden=true});await p.screenshot({path:'build/overlay-0.1.2-compact.png'});}
   await p.locator('#commanderTrigger').click();assert.equal(await p.getByRole('option').count(),21);assert.equal((await p.locator('.workspace').boundingBox()).height,workspace.height);await p.locator('#commanderTrigger').click();
   await p.locator('#appearanceTrigger').click();await p.locator('#opacity').evaluate(e=>{e.value=51;e.dispatchEvent(new Event('input',{bubbles:true}))});assert.equal(await p.locator('#opacityValue').innerText(),'51%');await p.locator('#closeAppearance').click();
   await p.getByRole('button',{name:'河洛，点击禁用',exact:true}).click();await p.getByRole('button',{name:'逐鹿，点击禁用',exact:true}).click();await p.waitForTimeout(350);
   assert.deepEqual(await p.evaluate(()=>sentCommands.filter(x=>x.action==='query').at(-1).data.bans),['河洛','逐鹿']);assert.equal(await p.locator('#copySelected').count(),0);
   await p.locator('#nextRound').click();assert.equal(await p.locator('.ban[aria-pressed=true]').count(),0);assert.deepEqual(errors,[]);await p.close();
  }
  console.log(JSON.stringify(summary));console.log('Compact density, equipment visibility, exact code copying and controls passed.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
