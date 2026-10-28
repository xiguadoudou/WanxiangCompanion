const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('node:assert/strict'),{pathToFileURL}=require('url');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,args:['--no-sandbox']});
 try{
  const page=await browser.newPage({viewport:{width:560,height:280},isMobile:true,hasTouch:true});const errors=[];page.on('pageerror',e=>errors.push(e.message));await page.route('https://**/*',r=>r.abort());
  const f=JSON.parse(fs.readFileSync('tests/fixtures/copy-ui-fixture.json'));
  await page.addInitScript(f=>{window.sent=[];window.Native={initialState:()=>JSON.stringify({type:'init',catalog:f.catalog,data:f.data,prefs:{...f.data.query}}),command:x=>sent.push(JSON.parse(x))};},f);
  await page.goto(pathToFileURL(process.cwd()+'/assets/overlay.html').href);await page.waitForSelector('#pinLineup');await page.addStyleTag({content:'body{background:#111d2b}'});
  // Choose a non-default lineup, then pin exactly that roster and equipment.
  await page.locator('[data-pick="1"]').click();const selected=await page.locator('.candidate.selected .pick').getAttribute('aria-label');
  const names=await page.locator('.roster .hero span').allTextContents();const allItems=await page.locator('.team-unit span').allTextContents();
  await page.locator('#pinLineup').click();assert.equal(await page.evaluate(()=>sent.at(-1).action),'stripMode');assert.equal(await page.evaluate(()=>sent.at(-1).data.enabled),true);
  for(const [width,height] of [[names.length*80+52,98],[250,98],[390,98],[780,98]]){
   await page.setViewportSize({width,height});assert.equal(await page.locator('#lineupStrip').isVisible(),true);
   for(const selector of ['.filterbar','.toolbar','.workspace','.popover'])assert.equal(await page.locator(selector).first().isVisible(),false);
   assert.deepEqual(await page.locator('.strip-hero strong').allTextContents(),names);
   const items=await page.locator('.strip-equipment').allTextContents();assert.equal(items.length,allItems.length);
   for(let n=0;n<items.length;n++)for(const expected of allItems[n].split(' · '))if(expected!=='暂无推荐装备')assert.ok(items[n].includes(expected));
   await page.screenshot({path:`build/lineup-strip-${width}x${height}.png`});
   const bounds=await page.locator('.strip-unit').first().boundingBox();assert.ok(bounds.height<=height-8);
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
   // Last hero can be brought into view without making the overlay taller.
   await page.locator('#lineupStrip').evaluate(e=>e.scrollLeft=e.scrollWidth);
   const last=await page.locator('.strip-unit').last().boundingBox();assert.ok(last.x+last.width<=width-20+1);assert.ok(last.y+last.height<=height);
  }
  // Real touch gestures must work when the finger starts on either hero or item rows.
  await page.setViewportSize({width:250,height:98});const touch=await page.context().newCDPSession(page);
  for(const y of [20,50]){
   await page.locator('#lineupStrip').evaluate(e=>e.scrollLeft=0);
   if(y===50)assert.equal(await page.evaluate(()=>!!document.elementFromPoint(175,50).closest('.strip-equipment')),true);
   await touch.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:175,y}]});
   for(let x=160;x>=40;x-=15)await touch.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x,y}]});
   await touch.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});
   await page.waitForFunction(()=>document.getElementById('lineupStrip').scrollLeft>40,{timeout:1000});
  }
  const pinned=await page.locator('#lineupStrip').innerText();
  const changed=JSON.parse(JSON.stringify(f.data));changed.rows=changed.rows.slice(0,1);changed.rows[0].heroes[0].name='后台新阵容';
  await page.evaluate(e=>App.receive({...e,type:'result'}),changed);assert.equal(await page.locator('#lineupStrip').innerText(),pinned,'Refresh must not replace pinned lineup');
  // Native restore and failed-window recovery return to the full layout.
  await page.evaluate(()=>App.receive({type:'overlayMode',mode:'panel'}));await page.setViewportSize({width:560,height:280});assert.equal(await page.locator('#lineupStrip').isVisible(),false);assert.equal(await page.locator('.workspace').isVisible(),true);
  await page.evaluate(e=>App.receive({...e,type:'result'}),f.data);await page.locator('[data-pick="1"]').click();await page.locator('#pinLineup').click();await page.keyboard.press('Escape');
  assert.equal(await page.evaluate(()=>sent.at(-1).data.enabled),false);assert.equal(await page.locator('.candidate.selected .pick').getAttribute('aria-label'),selected);
  await page.locator('#pinLineup').click();await page.evaluate(()=>App.receive({type:'roundReset',prefs:{commanderId:'',bans:[]}}));assert.equal(await page.locator('#lineupStrip').isVisible(),false);assert.equal(await page.locator('#pinLineup').count(),0);
  assert.deepEqual(errors,[]);console.log('Pinned selected roster/equipment, four strip sizes, horizontal access, refresh locking, native recovery, Escape restore and new-round reset passed.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
