// Production preview and actual backend only; never supply synthetic execution responses.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const {root,id,cases,oracle,packageSnapshot,sourceHash,assertIdentity,assertTrace}=require('./api.cjs');
const engine=process.env.AUDIT_ENGINE||'chromium';
assert(['chromium','firefox'].includes(engine));
const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180';
const out=path.resolve(process.env.GAS_EVIDENCE_DIR||__dirname);
const pace=require('../pace-executions.cjs')();
const authored=['default','maximum','half-cent'].map(label=>cases.find(c=>c.label===label));
const endpoint=response=>new URL(response.url()).pathname===`/api/problems/${id}/execute`;
const seek=page=>page.getByRole('slider',{name:'Seek to step',exact:true});
async function source(page,run,index){
  await page.getByRole('tab',{name:'Code walkthrough',exact:true}).click();
  const sourceButton=page.getByRole('button',{name:'Source',exact:true});
  if(await sourceButton.isVisible())await sourceButton.click();
  const region=page.getByRole('region',{name:'Java source',exact:true});await region.waitFor();
  assert(await page.getByTestId('search-space-canvas').count()<=1,'More than one primary range canvas is mounted');
  const displayed=await region.locator(':scope > div').evaluateAll(es=>es.map(e=>e.lastElementChild.textContent).join('\n'));
  assert.equal(displayed.replace(/\r\n/g,'\n'),run.code.replace(/\r\n/g,'\n'));
  const active=region.locator('[data-active-line="true"]');await active.waitFor();assert.equal(await active.count(),1);
  assert.equal(await active.evaluate(el=>el.children[1].textContent),String(run.steps[index].activeLine));
  return displayed;
}
async function range(page,step,initial){
  const low=Number(step.variables.low),high=Number(step.variables.high),width=high-low;
  const initialLow=Number(initial.variables.low),initialWidth=Number(initial.variables.high)-initialLow;
  const badge=page.getByTestId('search-range');
  await badge.filter({hasText:`continuous [${low}, ${high}]`}).waitFor();
  const text=await badge.textContent();assert(text.includes(`width ${width} of ${initialWidth}`));assert(!text.includes('left of'));
  assert(await badge.evaluate(el=>el.scrollWidth<=el.clientWidth+1),'Raw range text is clipped');
  const live=page.getByTestId('search-live');await live.waitFor();
  // CSSOM serializes percentages with browser-specific precision. Compare exact
  // normalized declarations, while the raw bracket and width above stay exact.
  const geometry=await live.evaluate((el,values)=>{
    const expected=document.createElement('div');
    expected.style.left=`${values.left}%`;expected.style.width=`${values.width}%`;
    return {actual:{left:el.style.left,width:el.style.width},
      expected:{left:expected.style.left,width:expected.style.width}};
  },{left:(low-initialLow)/initialWidth*100,width:width/initialWidth*100});
  assert.deepEqual(geometry.actual,geometry.expected);
  const input=page.getByTestId('search-input-row');await input.waitFor();
  assert.deepEqual(await input.locator('[data-state]').evaluateAll(es=>es.map(e=>e.firstElementChild.textContent)),
    step.arrayState.map(cell=>String(cell.label??cell.value)));
  assert.deepEqual(await input.locator('[data-state]').evaluateAll(es=>es.map(e=>e.getAttribute('data-state'))),step.arrayState.map(cell=>cell.state));
  assert.equal(await page.getByTestId('search-cells').count(),0);
  if(Object.hasOwn(step.variables,'mid'))assert.equal(await page.getByTestId('search-mid').textContent(),`probing ${Number(step.variables.mid)}`);
  else assert.equal(await page.getByTestId('search-mid').count(),0);
}
async function assertDraft(page,input){
  const group=page.getByRole('group',{name:'Existing station positions',exact:true});
  assert.deepEqual(await group.getByRole('spinbutton',{name:/^Position \d+ value$/}).evaluateAll(es=>es.map(e=>Number(e.value))),input.stations);
  assert.equal(await page.getByLabel('New stations to add (K)',{exact:true}).inputValue(),String(input.k));
}
async function assertEcho(page,input){
  const summary=page.locator('[data-testid="input-summary"]:visible').first();await summary.waitFor();
  const values=await summary.locator('span[title]').evaluateAll(es=>Object.fromEntries(es.map(el=>
    [el.previousElementSibling.textContent,el.getAttribute('title')])));
  assert.deepEqual(values,{stations:`[${input.stations.join(', ')}]`,k:String(input.k)});
}
(async()=>{
  const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  const localPackage=packageSnapshot(),servedBuild=await require('../served-build-identity.cjs')(base);
  const detailResponse=await fetch(`${base}/api/problems/${id}`);assert.equal(detailResponse.status,200);const detail=await detailResponse.json();
  fs.mkdirSync(out,{recursive:true});
  const browser=await types[engine].launch(),rows=[];
  try{
    for(const width of [320,1366])for(const theme of ['light','dark'])for(const c of authored){
      const page=await browser.newPage({viewport:{width,height:width===320?568:768},colorScheme:theme,reducedMotion:'reduce'});
      page.setDefaultTimeout(25000);await pace(page);
      await page.addInitScript(theme=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));},theme);
      const errors=[];let executions=0;
      page.on('pageerror',error=>errors.push(error.message));
      page.on('request',request=>{if(new URL(request.url()).pathname===`/api/problems/${id}/execute`)executions++;});
      const pending=page.waitForResponse(r=>endpoint(r)&&r.request().method()==='POST');
      const shared=Buffer.from(JSON.stringify(c.input)).toString('base64url');
      await page.goto(`${base}/problem/${id}?input=${shared}&step=3`);
      const response=await pending;assert.equal(response.status(),200);const raw=await response.json();
      const run={...raw,steps:decodeTrace(raw)};assertIdentity(run,detail,c.input);assertTrace(run,run.steps,c.input);
      await page.waitForFunction(()=>document.querySelector('input[aria-label="Seek to step"]')?.value==='2');
      await assertEcho(page,c.input);
      const originalExecutions=executions;
      const branch=run.steps.findIndex(s=>s.activeLine===run.anchors.feasible||s.activeLine===run.anchors.infeasible);
      const inspected=[...new Set([0,1,branch,Math.floor(run.steps.length/2),run.steps.length-1])];
      for(const index of inspected){
        await seek(page).fill(String(index));await range(page,run.steps[index],run.steps[0]);
        assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Page-wide overflow at actual step');
      }
      const finalIndex=run.steps.length-1,final=run.steps[finalIndex];
      assert.equal(await page.getByTestId('search-result').textContent(),`Result: ${Number(oracle(c.input).answer)}`);
      const shownSource=await source(page,run,finalIndex);
      await page.getByRole('tab',{name:'Analysis',exact:true}).click();
      const variables=page.getByRole('region',{name:'Variables',exact:true});await variables.waitFor();
      assert(await page.getByTestId('search-space-canvas').count()<=1,'More than one primary range canvas is mounted');
      for(const value of [final.variables.low,final.variables.high,final.variables.intervalWidth,'continuous'])assert((await variables.textContent()).includes(value));
      await page.getByRole('tab',{name:'Playground',exact:true}).click();
      await range(page,final,run.steps[0]);assert.equal(await seek(page).inputValue(),String(finalIndex));
      assert.equal(executions,originalExecutions,'Inspection or a view change executed again');
      await page.waitForFunction(()=>{const view=new URL(location.href).searchParams.get('view');return view===null||view==='playground';});
      await page.waitForFunction(step=>new URL(location.href).searchParams.get('step')===String(step),finalIndex+1);
      const successful=page.url();
      await page.getByRole('button',{name:'Edit input',exact:true}).click();await assertDraft(page,c.input);
      await page.getByLabel('New stations to add (K)',{exact:true}).fill('0');
      const refused=page.waitForResponse(r=>endpoint(r)&&r.request().method()==='POST');
      await page.getByRole('button',{name:'Run input',exact:true}).click();
      const rejection=await refused;assert.equal(rejection.status(),400);const failure=await rejection.json();
      assert.equal(typeof failure.fieldErrors?.k,'string');assert.equal(failure.steps,undefined);
      const alert=page.getByRole('alert').filter({hasText:'This input could not run'});await alert.waitFor();
      assert(await alert.evaluate(el=>el===document.activeElement),'Refusal did not focus error summary');
      assert.equal(page.url(),successful);assert.equal(await seek(page).inputValue(),String(finalIndex));
      await assertEcho(page,c.input);
      await assertDraft(page,{...c.input,k:0});
      assert.equal(await source(page,run,finalIndex),shownSource);assert.equal(executions,originalExecutions+1);
      const restored=page.waitForResponse(r=>endpoint(r)&&r.request().method()==='POST');
      await page.goto(successful);const restoredResponse=await restored;assert.equal(restoredResponse.status(),200);
      const restoredRaw=await restoredResponse.json(),restoredRun={...restoredRaw,steps:decodeTrace(restoredRaw)};
      assertIdentity(restoredRun,detail,c.input);assertTrace(restoredRun,restoredRun.steps,c.input);
      await page.waitForFunction(index=>document.querySelector('input[aria-label="Seek to step"]')?.value===String(index),finalIndex);
      await assertEcho(page,c.input);
      await range(page,restoredRun.steps[finalIndex],restoredRun.steps[0]);
      await page.getByRole('button',{name:'Edit input',exact:true}).click();await assertDraft(page,c.input);
      assert.deepEqual(errors,[]);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
      let screenshot=null;
      if(c.label==='maximum'&&(width===320&&theme==='dark'||width===1366&&theme==='light')){
        const stage=page.getByTestId('search-space-canvas');await stage.scrollIntoViewIfNeeded();
        screenshot=`${engine}-${width}-${theme}-final.png`;await page.screenshot({path:path.join(out,screenshot)});
      }
      rows.push({engine,width,height:width===320?568:768,theme,label:c.label,input:c.input,
        answer:oracle(c.input).answer,sourceHash:sourceHash(run.code),steps:run.steps.length,inspectedSteps:inspected.map(i=>i+1),
        finalLow:final.variables.low,finalHigh:final.variables.high,finalWidth:final.variables.intervalWidth,executions,screenshot,
        passed:true,actualContinuousRangesAndRuler:true,actualGapValuesAndHighlights:true,noStaleFinalProbe:true,
        actualSourceAndActiveLine:true,variablesTextAlternative:true,viewChangesDoNotExecute:true,
        sharedInputThenStepRestored:true,rejectedRunKeepsSourceInputStepLinkAndDraft:true,errorSummaryFocused:true,
        noPageOverflowOrErrors:true});
      await page.close();console.log(`PASS ${engine}/${width}/${theme}/${c.label}`);
    }
    assert.equal(rows.length,12);assert.deepEqual(packageSnapshot(),localPackage,'Jar changed during browser phase');
    fs.writeFileSync(path.join(out,`${engine}-results.json`),JSON.stringify({generated:new Date().toISOString(),realBackend:true,
      buildMode:'production-preview',complete:true,problemId:id,engine,browserVersion:browser.version(),servedBuild,localPackage,rows,
      limits:'Bounded automated owner-specific journeys. Not all-input, real-device, screen-reader, learner, performance, public-deployment or whole-project acceptance.'},null,2));
  }finally{await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
