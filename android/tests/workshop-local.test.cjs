const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),path=require('path'),assert=require('node:assert/strict');
const base='https://appassets.androidplatform.net',root=path.resolve('build/workshop'),images=JSON.parse(fs.readFileSync(root+'/image-map.json')),data=JSON.parse(fs.readFileSync(root+'/library-data.json'));
async function checkAppIntegration(p){
 await p.locator('.wxq-workshop-intro').waitFor();
 const current=p.url();
 for(const nav of await p.locator('.ep-bottom-nav,.h5-bottom-nav,.ep-header,.h5-header').all()){assert.equal(await nav.isVisible(),false,'Website navigation must be absent');assert.equal(await nav.getAttribute('inert'),'','Hidden navigation must not accept focus');}
 await p.evaluate(async()=>{const router=document.getElementById('app').__vue_app__.config.globalProperties.$router;for(const route of ['/','/rank','/h5','/login',location.pathname==='/h5/database'?'/team-builder':'/h5/database'])await router.push(route);});
 assert.equal(p.url(),current,'Vue router must not escape the active native module');
 assert.equal(await p.evaluate(()=>{try{history.pushState({},'','/rank');return false;}catch(e){return e.name==='SecurityError';}}),true,'Direct history navigation must also be blocked');
 await p.evaluate(()=>{const a=document.createElement('a');a.href='https://www.datawxq.com/';a.id='navigation-probe';a.textContent='测试外链';document.body.appendChild(a);a.click();a.remove();});
 assert.equal(p.url(),current,'External links must not open a website page');
 assert.equal(await p.evaluate(()=>getComputedStyle(document.documentElement).getPropertyValue('--el-color-primary').trim()),'#a2e8d0');
 assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Module must fit the viewport');
}
(async()=>{const b=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,args:['--no-sandbox']});try{
 const p=await b.newPage({viewport:{width:1340,height:900}}),errors=[],missing=[];p.on('pageerror',e=>errors.push(e.message));
 const serve=async r=>{const u=new URL(r.request().url());if(u.origin===base){let f=u.pathname.slice(1);if(['/team-builder','/simulator','/h5/database'].includes(u.pathname))f='index.html';if(u.pathname==='/workshop-data.js')return r.fulfill({contentType:'application/javascript',body:'window.__WXQ_DATA__='+JSON.stringify(data)+';'});if(fs.existsSync(root+'/'+f)&&fs.statSync(root+'/'+f).isFile())return r.fulfill({path:root+'/'+f});missing.push(u.pathname);return r.fulfill({status:404,body:''});}const f=images[r.request().url()];if(f)return r.fulfill({path:root+'/'+f,headers:{'access-control-allow-origin':'*'}});return r.abort();};await p.route('**/*',serve);
 await p.goto(base+'/team-builder');await p.waitForSelector('#hero-library',{timeout:20000});
 await checkAppIntegration(p);
 assert.equal(await p.getByText('分享阵容',{exact:true}).isVisible().catch(()=>false),false);
 await p.locator('#hero-library .hero-item').filter({has:p.locator('img[alt="苏烈"]')}).first().click({timeout:5000});await p.waitForFunction(()=>new URL(location.href).searchParams.has('lineup'));
 const plan=await p.evaluate(()=>JSON.parse(atob(new URL(location.href).searchParams.get('lineup').replace(/-/g,'+').replace(/_/g,'/'))));assert.ok(plan.b.some(x=>x&&x[0]==='1941'));
 await p.screenshot({path:'build/local-simulator.png',fullPage:true});
 const cell0=p.locator('.map-item-hero[data-index="0"]'),cell8=p.locator('.map-item-hero[data-index="8"]');
 const a=await cell0.boundingBox(),z=await cell8.boundingBox();await p.mouse.move(a.x+a.width/2,a.y+a.height/2);await p.mouse.down();await p.mouse.move(z.x+z.width/2,z.y+z.height/2,{steps:15});await p.mouse.up();
 await p.waitForFunction(()=>{const a=JSON.parse(atob(new URL(location.href).searchParams.get('lineup')));return a.b[8]&&a.b[8][0]==='1941'});
 const equip=p.locator('#equip-library .equip-table-item').filter({has:p.locator('img[alt="风暴巨剑"]')}).first();await equip.scrollIntoViewIfNeeded();await cell8.scrollIntoViewIfNeeded();const eb=await equip.boundingBox(),hb=await cell8.boundingBox();await p.mouse.move(eb.x+eb.width/2,eb.y+eb.height/2);await p.mouse.down();await p.mouse.move(hb.x+hb.width/2,hb.y+hb.height/2,{steps:18});await p.mouse.up();
 await p.waitForFunction(()=>JSON.parse(atob(new URL(location.href).searchParams.get('lineup'))).b[8][1].length===1);
 const url=await p.url();await p.reload();await p.waitForSelector('#simulator-root');assert.equal(await p.url(),url);await p.waitForFunction(()=>JSON.parse(atob(new URL(location.href).searchParams.get('lineup'))).b[8][1].length===1);
 // Native image export is represented by a narrowly scoped PNG save prompt.
 let exported=false;p.on('dialog',async d=>{if(d.type()==='prompt'&&d.message()==='__WXQ_EXPORT_PNG__'){exported=d.defaultValue().startsWith('data:image/png;base64,');await d.accept('ok');}else await d.dismiss();});
 await p.locator('.map-item-hero[data-index="8"] img[alt="苏烈"]').waitFor();await p.evaluate(()=>WxqLocalPort.exportImage(false));for(let n=0;n<20&&!exported;n++)await p.waitForTimeout(300);assert.equal(exported,true,'Local canvas export must produce a PNG without remote pages');

 await p.goto(base+'/h5/database');await p.getByRole('button',{name:'查看苏烈详情',exact:true}).waitFor({timeout:20000});
 await checkAppIntegration(p);
 await p.getByRole('button',{name:'装备',exact:true}).click({timeout:5000});await p.getByRole('searchbox').fill('金色圣剑');await p.getByRole('button',{name:'查看金色圣剑详情',exact:true}).click();
 assert.ok((await p.locator('body').innerText()).includes('合成散件'));
 await p.screenshot({path:'build/local-database.png',fullPage:true});
 const mobile=await b.newPage({viewport:{width:390,height:844},isMobile:true,hasTouch:true});mobile.on('pageerror',e=>errors.push(e.message));await mobile.route('**/*',serve);await mobile.goto(base+'/team-builder');await mobile.waitForSelector('#hero-library');await checkAppIntegration(mobile);await mobile.locator('#hero-library .hero-item').filter({has:mobile.locator('img[alt="苏烈"]')}).first().tap();await mobile.waitForFunction(()=>new URL(location.href).searchParams.has('lineup'));
 await mobile.locator('.map-item-hero[data-index="0"] img[alt="苏烈"]').waitFor();await mobile.evaluate(()=>window.scrollTo(0,0));await mobile.locator('#simulator-map').scrollIntoViewIfNeeded();const source=await mobile.locator('.map-item-hero[data-index="0"]').boundingBox(),target=await mobile.locator('.map-item-hero[data-index="8"]').boundingBox(),touch=await mobile.context().newCDPSession(mobile);
 await touch.send('Input.dispatchTouchEvent',{type:'touchStart',touchPoints:[{x:source.x+source.width/2,y:source.y+source.height/2}]});await mobile.waitForTimeout(220);for(let n=1;n<=15;n++)await touch.send('Input.dispatchTouchEvent',{type:'touchMove',touchPoints:[{x:source.x+source.width/2+(target.x-source.x)*n/15,y:source.y+source.height/2+(target.y-source.y)*n/15}]});await touch.send('Input.dispatchTouchEvent',{type:'touchEnd',touchPoints:[]});await mobile.waitForFunction(()=>{const q=new URL(location.href).searchParams.get('lineup');return q&&JSON.parse(atob(q)).b[8]?.[0]==='1941'},null,{timeout:5000});await mobile.screenshot({path:'build/local-simulator-mobile.png',fullPage:true});
 await mobile.goto(base+'/h5/database');await mobile.getByRole('button',{name:'查看苏烈详情',exact:true}).waitFor();for(const [label,key] of [['棋手','commanders'],['天赋','wanxiang_talent'],['效果牌','wanxiang_effect'],['初始卡包','packs']]){await mobile.getByRole('button',{name:label,exact:true}).click();const expected=data.arrays[key][0].name;await mobile.waitForFunction(name=>document.body.innerText.includes(name),expected,{timeout:5000});}assert.ok(await mobile.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));await mobile.screenshot({path:'build/local-database-mobile.png',fullPage:true});
assert.ok(missing.every(x=>x==='/assets/community/qq-group.png'),'No core local resources may be missing');
 assert.equal(missing.filter(x=>/\.js$|\.css$/.test(x)).length,0,'All executable resources must be bundled');assert.equal(errors.length,0,'Local modules must initialize without page errors');
 for(const viewport of [{width:320,height:740},{width:844,height:390}]){await mobile.setViewportSize(viewport);for(const route of ['/team-builder','/h5/database']){await mobile.goto(base+route);await mobile.waitForSelector(route.includes('database')?'.h5-database-list':'#hero-library');await checkAppIntegration(mobile);}}
 console.log('Offline desktop/mobile startup, placement, touch moves, equipment, PNG export, database categories, shared theme, hidden website navigation and router isolation at 320/390/844px passed.');
}finally{await b.close();}})().catch(e=>{console.error(e);process.exit(1)});
