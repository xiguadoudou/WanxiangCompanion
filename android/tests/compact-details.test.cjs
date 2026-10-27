const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('node:assert/strict'),{pathToFileURL}=require('url');
(async()=>{
 const f=JSON.parse(fs.readFileSync('tests/fixtures/copy-ui-fixture.json'));
 const b=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,args:['--no-sandbox']});
 try{
 const p=await b.newPage({viewport:{width:390,height:228}});const errors=[];p.on('pageerror',e=>errors.push(e.message));await p.route('https://**/*',r=>r.abort());
 await p.addInitScript(f=>{window.Native={initialState:()=>JSON.stringify({type:'init',catalog:f.catalog,data:f.data,prefs:{...f.data.query}}),command:()=>{}};},f);
 await p.goto(pathToFileURL(process.cwd()+'/assets/overlay.html').href);await p.waitForSelector('.team-unit');
 const selected=await p.locator('.candidate.selected .pick').getAttribute('aria-label');
 for(const [width,height] of [[390,228],[280,240],[250,170],[560,178]]){
  await p.setViewportSize({width,height});await p.locator('#detail').evaluate(e=>{e.scrollTop=0});
  assert.equal(await p.locator('.expanded-details').isVisible(),true);assert.equal(await p.locator('.team-unit').count(),f.data.rows[0].heroes.length);
  const before=await p.locator('.team-title').boundingBox(),d=await p.locator('#detail').boundingBox();assert.ok(before.y>=d.y+d.height-30,'Team details remain below the initial compact content');
  await p.locator('#detail').evaluate(e=>e.scrollTop=e.scrollHeight);
  await p.locator('.team-unit').last().evaluate(el=>{const d=document.getElementById('detail');d.scrollTop+=el.getBoundingClientRect().top-d.getBoundingClientRect().top-Math.max(0,(d.clientHeight-el.getBoundingClientRect().height)/2);});const last=await p.locator('.team-unit').last().boundingBox();assert.ok(last.y>=d.y&&last.y+last.height<=d.y+d.height+1,'Last hero equipment must be reachable by scrolling');
  assert.equal(await p.locator('.candidate.selected .pick').getAttribute('aria-label'),selected);assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
  await p.screenshot({path:`build/compact-details-${width}x${height}.png`});
 }
 assert.deepEqual(errors,[]);
 const q=await b.newPage({viewport:{width:390,height:844}});await q.route('https://**/*',r=>r.abort());await q.goto(pathToFileURL(process.cwd()+'/assets/index.html').href);
 await q.locator('#settingsButton').click();assert.equal(await q.locator('[data-min="10"]').getAttribute('class'),'pill active');await q.locator('[data-min="100"]').click();assert.equal(await q.locator('[data-min="100"]').getAttribute('class'),'pill active');await q.locator('#back').click();
 assert.equal(await q.locator('main > .build-credit').innerText(),'软件由 ChatGPT 构建');assert.equal(await q.locator('main > :last-child').getAttribute('class'),'build-credit');
 await q.locator('.build-credit').scrollIntoViewIfNeeded();await q.screenshot({path:'build/build-credit-home.png',fullPage:true});
 await q.locator('#settingsButton').click();assert.equal(await q.locator('.build-credit').isVisible(),true);console.log('All compact layouts reveal full sample and team equipment details on scroll; main/settings credit footer passed.');
 }finally{await b.close();}
})().catch(e=>{console.error(e);process.exit(1)});
