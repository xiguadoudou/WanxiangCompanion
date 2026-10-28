const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('node:assert/strict'),{pathToFileURL}=require('url');
const swipe=require('./touch-gesture.cjs');
(async()=>{const b=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,args:['--no-sandbox']});try{
 const p=await b.newPage({hasTouch:true});const errors=[];p.on('pageerror',e=>errors.push(e.message));await p.route('https://**/*',r=>r.abort());const f=JSON.parse(fs.readFileSync('tests/fixtures/copy-ui-fixture.json'));
 await p.addInitScript(f=>{window.sent=[];window.Native={initialState:()=>JSON.stringify({type:'init',catalog:f.catalog,data:f.data,prefs:{...f.data.query}}),command:x=>sent.push(JSON.parse(x))};},f);
 for(const [width,height] of [[320,720],[390,844],[800,390]]){await p.setViewportSize({width,height});await p.goto(pathToFileURL(process.cwd()+'/assets/index.html').href);await p.waitForSelector('.card');const before=await p.locator('#results').innerText();await p.locator('#navigationButton').click();assert.equal(await p.evaluate(()=>sent.at(-1).action),'navigation');assert.equal(await p.locator('#results').innerText(),before,'Opening drawer must not clear recommendations');assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Header must fit small and landscape windows');await p.locator('#settingsButton').click();assert.equal(await p.locator('#settings').isVisible(),true);}
 assert.equal(await p.locator('#updateButton').count(),0,'Remove the header jump-to-update button');assert.equal(await p.locator('#settingsButton svg').count(),1,'Settings must use a gear vector');
 await p.setViewportSize({width:390,height:844});await p.goto(pathToFileURL(process.cwd()+'/assets/index.html').href);await p.waitForSelector('.card');
 const navCount=()=>p.evaluate(()=>sent.filter(x=>x.action==='navigation').length);
 const intro=await p.locator('.intro').boundingBox();await swipe(p,intro.x+intro.width*.85,intro.y+intro.height/2,intro.x+intro.width*.2,intro.y+intro.height/2);assert.equal(await navCount(),1,'Left swipe opens navigation away from either screen edge');
 await swipe(p,180,intro.y+10,180,intro.y+90);assert.equal(await navCount(),1,'Vertical page scrolling must not open the drawer');
 const roster=p.locator('.roster').first();await roster.scrollIntoViewIfNeeded();const rb=await roster.boundingBox();await swipe(p,rb.x+rb.width*.8,rb.y+rb.height/2,rb.x+rb.width*.2,rb.y+rb.height/2);assert.equal(await navCount(),1,'Roster gestures remain with the horizontal control');
 await p.locator('#settingsButton').click();const range=p.locator('#opacity');await range.scrollIntoViewIfNeeded();const rangeBox=await range.boundingBox();await swipe(p,rangeBox.x+rangeBox.width*.8,rangeBox.y+rangeBox.height/2,rangeBox.x+rangeBox.width*.2,rangeBox.y+rangeBox.height/2);assert.equal(await navCount(),1,'Settings sliders must not open navigation');
 assert.deepEqual(errors,[]);console.log('Native drawer action, recommendation preservation, gear settings, two header actions and header fit at three sizes passed.');
}finally{await b.close();}})().catch(e=>{console.error(e);process.exit(1)});
