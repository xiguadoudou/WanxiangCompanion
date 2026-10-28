const {chromium}=require(process.env.WXQ_PLAYWRIGHT_MODULE||'playwright');
const fs=require('fs'),assert=require('node:assert/strict'),{pathToFileURL}=require('url');
(async()=>{
 const b=await chromium.launch({executablePath:process.env.WXQ_BROWSER_PATH,args:['--no-sandbox']});try{
 const p=await b.newPage({viewport:{width:390,height:844}});const errors=[];p.on('pageerror',e=>errors.push(e.message));
 const catalog=JSON.parse(fs.readFileSync('assets/catalog-snapshot.json'));const data=JSON.parse(fs.readFileSync('tests/fixtures/ui-fixture.json')).data;
 data.queryKey=JSON.stringify(['commander-r1',['河洛'],'avgPlacement',7,50]);
 await p.addInitScript(({catalog,data})=>{window.sent=[];window.Native={initialState:()=>JSON.stringify({type:'init',catalog,data,imageCacheStatus:'图片缓存准备中 12/188',prefs:{commanderId:'commander-r1',bans:['河洛'],minCount:50},updater:{state:'checking',installedVersion:'0.1.3',message:'正在检查软件版本…'}}),command:x=>sent.push(JSON.parse(x))};},{catalog,data});
 await p.goto(pathToFileURL(process.cwd()+'/assets/index.html').href);assert.equal(await p.locator('.card').count(),data.rows.length,'Native HTTP response must render');
 await p.evaluate(()=>App.receive({type:'queryError',queryKey:JSON.stringify(['commander-r1',['河洛'],'avgPlacement',7,50]),message:'阵容查询失败：服务端异常'}));assert.equal(await p.locator('.card').count(),data.rows.length,'Query failure must retain matching cached cards');
 await p.evaluate(()=>App.receive({type:'dataStatus',state:'failed',message:'资料检查失败，仍可使用缓存'}));assert.equal(await p.locator('.card').count(),data.rows.length,'Metadata failure cannot erase lineups');assert.match(await p.locator('#dataSyncStatus').innerText(),/资料检查失败/);
 await p.locator('#settingsButton').click();await p.locator('#downloadPage').click();assert.equal(await p.evaluate(()=>sent.at(-1).action),'downloadPage');assert.match(await p.locator('#cacheStatus').innerText(),/12\/188/);await p.evaluate(()=>App.receive({type:'cacheStatus',message:'图片缓存 188/188 · 3.0 MB · 刷新优先使用本地'}));assert.match(await p.locator('#cacheStatus').innerText(),/刷新优先使用本地/);assert.equal(await p.locator('#checkSoftware').isDisabled(),true);
 await p.evaluate(()=>App.receive({type:'softwareUpdate',state:'available',available:true,installedVersion:'0.1.3',latestVersion:'0.1.4',message:'发现新版本 0.1.4',notes:'连接修复'}));
 await p.locator('#updateLater').click();await p.locator('#downloadSoftware').click();assert.equal(await p.evaluate(()=>sent.at(-1).action),'downloadSoftwareUpdate');
 await p.evaluate(()=>App.receive({type:'softwareUpdate',state:'downloading',available:true,message:'正在下载更新 42%',progress:42}));assert.equal(await p.locator('#softwareProgress').evaluate(el=>String(el.value)),'42');assert.equal(await p.locator('#downloadSoftware').isDisabled(),true);
 await p.evaluate(()=>App.receive({type:'softwareUpdate',state:'ready',available:true,downloadReady:true,message:'下载并校验完成，可安装更新',progress:100}));
 assert.equal(await p.locator('#downloadSoftware').isVisible(),false);await p.locator('#installSoftware').click();assert.equal(await p.evaluate(()=>sent.at(-1).action),'installSoftwareUpdate');
 await p.evaluate(()=>App.receive({type:'softwareUpdate',state:'error',message:'软件更新检查失败：连接超时'}));assert.equal(await p.locator('#checkSoftware').isDisabled(),false);assert.match(await p.locator('#softwareStatus').innerText(),/超时/);
 await p.evaluate(()=>App.receive({type:'softwareUpdate',state:'current',installedVersion:'0.1.3',message:'已是最新版本 0.1.3'}));assert.equal(await p.locator('#downloadSoftware').isVisible(),false);
 await p.screenshot({path:'build/update-settings-0.1.3.png',fullPage:true});assert.ok(await p.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));assert.deepEqual(errors,[]);console.log('Native-result rendering, metadata-failure retention, update states, progress and all three native bridge actions passed.');
 }finally{await b.close();}
})().catch(e=>{console.error(e);process.exit(1)});
