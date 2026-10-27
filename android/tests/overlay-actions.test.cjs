const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('node:assert/strict'),{pathToFileURL}=require('url');
(async()=>{
 const browser=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,args:['--no-sandbox']});
 try{
  const page=await browser.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));await page.route('https://**/*',r=>r.abort());
  const f=JSON.parse(fs.readFileSync('tests/fixtures/copy-ui-fixture.json'));
  await page.addInitScript(f=>{window.sent=[];window.Native={initialState:()=>JSON.stringify({type:'init',catalog:f.catalog,data:f.data,prefs:{...f.data.query}}),command:x=>sent.push(JSON.parse(x))};},f);
  await page.goto(pathToFileURL(process.cwd()+'/assets/overlay.html').href);await page.waitForSelector('.team-unit');
  for(const [width,height] of [[250,170],[280,240],[392,230],[560,300]]){
   await page.setViewportSize({width,height});await page.locator('#appearanceTrigger').click();
   const panel=page.locator('#appearance');assert.equal(await panel.isVisible(),true);
   for(const id of ['openApp','exitApp']){const box=await page.locator('#'+id).boundingBox();assert.ok(box.x>=0&&box.y>=0&&box.x+box.width<=width&&box.y+box.height<=height,`${id} reachable at ${width}x${height}`);assert.ok(box.height>=32);}
   await page.locator('#exitApp').click();assert.equal(await page.locator('#exitConfirm').isVisible(),true);assert.equal(await panel.isVisible(),false);
   const before=await page.evaluate(()=>sent.filter(x=>x.action==='exitApp').length);await page.locator('#cancelExit').click();assert.equal(await page.evaluate(()=>sent.filter(x=>x.action==='exitApp').length),before);
   await page.locator('#appearanceTrigger').click();await page.locator('#exitApp').click();await page.keyboard.press('Escape');assert.equal(await page.locator('#exitConfirm').isVisible(),false);
   await page.locator('#appearanceTrigger').click();await page.locator('#openApp').click();assert.equal(await page.evaluate(()=>sent.at(-1).action),'openApp');assert.equal(await panel.isVisible(),false);
   await page.locator('#appearanceTrigger').click();await page.locator('#exitApp').click();await page.locator('#confirmExit').click();assert.equal(await page.evaluate(()=>sent.filter(x=>x.action==='exitApp').length),before+1);
   assert.ok(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  }
  assert.deepEqual(errors,[]);console.log('Overlay return/exit actions, cancellation, Escape, single dispatch and button reachability at four sizes passed.');
 }finally{await browser.close();}
})().catch(e=>{console.error(e);process.exit(1)});
