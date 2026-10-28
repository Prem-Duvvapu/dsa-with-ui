const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const {root,id,oracle,packageSnapshot,assertDetail,assertIdentity,assertTrace,sourceHash}=require('./api.cjs');
(async()=>{
  const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180',out=path.resolve(process.env.PHONE_EVIDENCE_DIR||__dirname);
  const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  const packaged=packageSnapshot(),servedBuild=await require('../served-build-identity.cjs')(base),rows=[];
  const detail=await (await fetch(`${base}/api/problems/${id}`)).json();assertDetail(detail);
  for(const engine of ['chromium','firefox']){
    const browser=await types[engine].launch();
    try{for(const [width,theme]of [[320,'dark'],[1366,'light']]){
      const page=await browser.newPage({viewport:{width,height:width===320?568:768},colorScheme:theme,reducedMotion:'reduce'});
      await require('../pace-executions.cjs')()(page);
      await page.addInitScript(theme=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));},theme);
      const errors=[];page.on('pageerror',error=>errors.push(error.message));
      const response=page.waitForResponse(r=>new URL(r.url()).pathname===`/api/problems/${id}/execute`&&r.request().method()==='POST');
      const shared=Buffer.from(JSON.stringify({digits:'77777'})).toString('base64url');await page.goto(`${base}/problem/${id}?input=${shared}`);
      const actual=await response;assert.equal(actual.status(),200);const raw=await actual.json(),run={...raw,steps:decodeTrace(raw)};
      assertIdentity(run,detail,{digits:'77777'});assertTrace(run,run.steps,{digits:'77777'});
      const first=run.steps.findIndex(s=>s.activeLine===run.anchors.capture),seek=page.getByRole('slider',{name:'Seek to step',exact:true});
      await seek.fill(String(first));
      const stage=page.getByTestId('recursion-tree-stage');await stage.waitFor();await stage.scrollIntoViewIfNeeded();
      const strip=stage.locator('..').locator(':scope > div').nth(1);
      await page.waitForFunction(()=>document.querySelector('input[aria-label="Seek to step"]')?.value==='5');
      assert.deepEqual(await strip.locator(':scope > div > div > span:first-child').allTextContents(),['p','p','p','p','p']);
      assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
      const stageImage=`${engine}-${width}-${theme}-path.png`;await page.screenshot({path:path.join(out,stageImage)});
      await seek.fill(String(run.steps.length-1));await page.getByRole('tab',{name:'Analysis',exact:true}).click();
      const region=page.getByRole('region',{name:'Variables',exact:true}),result=region.locator('dl > div').filter({has:page.getByText('results',{exact:true})});
      await result.locator('summary').click();assert.equal(await result.locator('pre').textContent(),oracle('77777').javaList);
      await region.scrollIntoViewIfNeeded();assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));assert.deepEqual(errors,[]);
      const resultsImage=`${engine}-${width}-${theme}-results.png`;await page.screenshot({path:path.join(out,resultsImage)});
      rows.push({engine,width,theme,sourceHash:sourceHash(run.code),firstCaptureStep:first+1,actualPath:'ppppp',combinations:1024,wholeResultsRead:true,stageImage,resultsImage,noOverflowOrErrors:true,passed:true});
      await page.close();console.log(`PASS visual ${engine}/${width}/${theme}`);
    }}finally{await browser.close();}
  }
  assert.equal(rows.length,4);assert.deepEqual(packageSnapshot(),packaged);
  fs.writeFileSync(path.join(out,'visual-results.json'),JSON.stringify({generated:new Date().toISOString(),complete:true,realBackend:true,servedBuild,localPackage:packaged,rows,limits:'Four selected visual states, not all viewport/device/accessibility/performance acceptance.'},null,2)+'\n');
})().catch(error=>{console.error(error);process.exitCode=1;});
