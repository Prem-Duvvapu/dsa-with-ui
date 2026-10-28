// Actual packaged backend + production-preview browser proof; never intercept execution bodies.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {root,id,cases,oracle,sourceHash,packageSnapshot,assertDetail,assertIdentity,assertTrace}=require('./api.cjs');
const endpoint=response=>new URL(response.url()).pathname===`/api/problems/${id}/execute`;
const seek=page=>page.getByRole('slider',{name:'Seek to step',exact:true});
const input=page=>page.getByLabel('Digits',{exact:true});
async function echo(page,digits){
  const summary=page.locator('[data-testid="input-summary"]:visible').first();await summary.waitFor();
  assert.deepEqual(await summary.locator('span[title]').evaluateAll(es=>Object.fromEntries(es.map(el=>
    [el.previousElementSibling.textContent,el.getAttribute('title')]))),{digits});
}
async function source(page,run,index){
  await page.getByRole('tab',{name:'Code walkthrough',exact:true}).click();
  const button=page.getByRole('button',{name:'Source',exact:true});if(await button.isVisible())await button.click();
  const region=page.getByRole('region',{name:'Java source',exact:true});await region.waitFor();
  const shown=await region.locator(':scope > div').evaluateAll(es=>es.map(el=>el.lastElementChild.textContent).join('\n'));
  assert.equal(shown.replace(/\r\n/g,'\n'),run.code.replace(/\r\n/g,'\n'));
  const active=region.locator('[data-active-line="true"]');await active.waitFor();assert.equal(await active.count(),1);
  assert.equal(await active.evaluate(el=>el.children[1].textContent),String(run.steps[index].activeLine));
  assert(await page.getByTestId('recursion-tree-stage').count()<=1,'More than one primary recursion canvas');
  await noOverflow(page);return shown;
}
async function noOverflow(page){assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Page-wide horizontal overflow');}
async function stage(page,run,index,large){
  await page.getByRole('tab',{name:'Playground',exact:true}).click();
  assert.equal(await seek(page).inputValue(),String(index));
  const region=page.getByTestId('recursion-tree-stage');await region.waitFor();
  assert.equal(await region.count(),1);
  // Read the actual character cells; the API separately checks their numeric code points.
  const strip=region.locator('..').locator(':scope > div').nth(1),chars=run.steps[index].arrayState??[];
  if(chars.length){
    await strip.waitFor();assert.deepEqual(await strip.locator(':scope > div > div > span:first-child').allTextContents(),chars.map(c=>String(c.label??c.value)));
  }else assert.equal(await strip.count(),0);
  const derived=page.getByTestId('derived-recursion-tree');await derived.waitFor();
  if(large&&index===run.steps.length-1){
    await derived.getByText('Showing the first 220 calls. The run explores more than fits here.',{exact:true}).waitFor();
    assert.equal(await derived.locator('svg > g').count(),220);
    assert.equal(await derived.locator('svg').getAttribute('aria-label'),'Recursion tree, 220 calls explored');
    assert(!run.truncated,'The diagram cap must not be confused with an incomplete execution');
  }
  await noOverflow(page);
}
async function results(page,reference){
  await page.getByRole('tab',{name:'Analysis',exact:true}).click();
  const variables=page.getByRole('region',{name:'Variables',exact:true});await variables.waitFor();
  const row=name=>variables.locator('dl > div').filter({has:page.getByText(name,{exact:true})});
  assert.equal(await row('combinations').locator('dd > code').textContent(),String(reference.results.length));
  const value=row('results');assert.equal(await value.count(),1);
  if(reference.javaList.length>60){
    const disclosure=value.locator('details');assert.equal(await disclosure.count(),1);
    await disclosure.locator('summary').click();assert.equal(await disclosure.locator('pre').textContent(),reference.javaList);
  }else assert.equal(await value.locator('dd > code').textContent(),reference.javaList);
  assert(await page.getByTestId('recursion-tree-stage').count()<=1);await noOverflow(page);
}
async function main(){
  const engine=process.env.AUDIT_ENGINE||'chromium';assert(['chromium','firefox'].includes(engine));
  const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
  const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180',out=path.resolve(process.env.PHONE_EVIDENCE_DIR||__dirname);
  const pace=require('../pace-executions.cjs')(),localPackage=packageSnapshot();
  const servedBuild=await require('../served-build-identity.cjs')(base);
  const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  const detailResponse=await fetch(`${base}/api/problems/${id}`);assert.equal(detailResponse.status,200);
  const detail=await detailResponse.json();assertDetail(detail);fs.mkdirSync(out,{recursive:true});
  const browser=await types[engine].launch(),rows=[];
  async function newPage(width,theme,randomize=false){
    const page=await browser.newPage({viewport:{width,height:width===320?568:768},colorScheme:theme,reducedMotion:'reduce'});
    page.setDefaultTimeout(30000);await pace(page);
    await page.addInitScript(({theme,randomize})=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));
      if(randomize)Math.random=()=>0.99999;},{theme,randomize});
    const record={errors:[],executions:0};page.on('pageerror',error=>record.errors.push(error.message));
    page.on('request',request=>{if(new URL(request.url()).pathname===`/api/problems/${id}/execute`)record.executions++;});
    return {page,record};
  }
  async function decode(response,digits){
    assert.equal(response.status(),200);const raw=await response.json(),run={...raw,steps:decodeTrace(raw)};
    assertIdentity(run,detail,{digits});assertTrace(run,run.steps,{digits});return run;
  }
  try{
    const authored=['default','maximum-five','safe-six-late-four'].map(label=>cases.find(c=>c.label===label));
    for(const width of [320,1366])for(const theme of ['light','dark'])for(const c of authored){
      const {page,record}=await newPage(width,theme),reference=oracle(c.digits),large=reference.results.length>220;
      const pending=page.waitForResponse(r=>endpoint(r)&&r.request().method()==='POST');
      const shared=Buffer.from(JSON.stringify({digits:c.digits})).toString('base64url');
      await page.goto(`${base}/problem/${id}?input=${shared}&step=3`);const run=await decode(await pending,c.digits);
      await page.waitForFunction(()=>document.querySelector('input[aria-label="Seek to step"]')?.value==='2');await echo(page,c.digits);
      const originalExecutions=record.executions,finalIndex=run.steps.length-1;
      const captures=run.steps.map((s,i)=>s.activeLine===run.anchors.capture?i:-1).filter(i=>i>=0);
      const inspected=[...new Set([0,captures[0],captures.at(-1),finalIndex])];
      let shownSource;
      for(const index of inspected){
        await seek(page).fill(String(index));await stage(page,run,index,large);
        shownSource=await source(page,run,index);
        await page.getByRole('tab',{name:'Playground',exact:true}).click();assert.equal(await seek(page).inputValue(),String(index));
      }
      await results(page,reference);await stage(page,run,finalIndex,large);await echo(page,c.digits);
      assert.equal(record.executions,originalExecutions,'View or source inspection executed again');
      await page.waitForFunction(step=>new URL(location.href).searchParams.get('step')===String(step),finalIndex+1);
      await page.waitForFunction(()=>{const view=new URL(location.href).searchParams.get('view');return view===null||view==='playground';});
      const successful=page.url();await page.getByRole('button',{name:'Edit input',exact:true}).click();
      assert.equal(await input(page).inputValue(),c.digits);await input(page).fill('722222');
      const refused=page.waitForResponse(r=>endpoint(r)&&r.request().method()==='POST');await page.getByRole('button',{name:'Run input',exact:true}).click();
      const rejection=await refused;assert.equal(rejection.status(),400);const failure=await rejection.json();
      assert.equal(typeof failure.fieldErrors?.digits,'string');assert.equal(failure.steps,undefined);
      const alert=page.getByRole('alert').filter({hasText:'This input could not run'});await alert.waitFor();
      assert(await alert.evaluate(el=>el===document.activeElement),'400 did not focus the error summary');
      assert.equal(page.url(),successful);assert.equal(await seek(page).inputValue(),String(finalIndex));
      assert.equal(await input(page).inputValue(),'722222');await echo(page,c.digits);await stage(page,run,finalIndex,large);
      assert.equal(await source(page,run,finalIndex),shownSource);assert.equal(record.executions,originalExecutions+1);
      const restored=page.waitForResponse(r=>endpoint(r)&&r.request().method()==='POST');await page.goto(successful);
      const restoredRun=await decode(await restored,c.digits);
      await page.waitForFunction(index=>document.querySelector('input[aria-label="Seek to step"]')?.value===String(index),finalIndex);
      await echo(page,c.digits);await stage(page,restoredRun,finalIndex,large);await results(page,reference);
      await stage(page,restoredRun,finalIndex,large);
      await page.getByRole('button',{name:'Edit input',exact:true}).click();assert.equal(await input(page).inputValue(),c.digits);
      assert.deepEqual(record.errors,[]);await noOverflow(page);
      rows.push({engine,width,height:width===320?568:768,theme,label:c.label,digits:c.digits,combinations:reference.results.length,
        sourceHash:sourceHash(run.code),resultsHash:sourceHash(reference.javaList),steps:run.steps.length,inspectedSteps:inspected.map(i=>i+1),
        executions:record.executions,diagramCapChecked:large?220:null,wholeResultsRead:true,actualCharValuesAndActiveLines:true,
        viewChangesDoNotExecute:true,sharedInputThenStepRestored:true,rejectedDraftRunSourceInputStepAndLinkPreserved:true,
        errorSummaryFocused:true,noPageOverflowOrErrors:true,passed:true});
      await page.close();console.log(`PASS phone ${engine}/${width}/${theme}/${c.label}`);
    }
    assert.equal(rows.length,12);
    // One extra bounded phone row per engine. Determinism applies only to this page.
    const {page,record}=await newPage(320,'dark',true);
    const initial=page.waitForResponse(r=>endpoint(r)&&r.request().method()==='GET');await page.goto(`${base}/problem/${id}`);
    await decode(await initial,'23');await seek(page).waitFor();await page.getByRole('button',{name:'Edit input',exact:true}).click();
    const before=record.executions;await page.getByRole('button',{name:'Randomize',exact:true}).click();
    const generated=await input(page).inputValue();assert.equal(generated,'99999');assert.match(generated,/^[2-9]{1,5}$/);
    assert.equal(record.executions,before,'Randomize must remain draft-only');await echo(page,'23');
    const pending=page.waitForResponse(r=>endpoint(r)&&r.request().method()==='POST');await page.getByRole('button',{name:'Run input',exact:true}).click();
    const randomized=await decode(await pending,generated);await seek(page).fill(String(randomized.steps.length-1));
    await stage(page,randomized,randomized.steps.length-1,true);await results(page,oracle(generated));
    await stage(page,randomized,randomized.steps.length-1,true);await echo(page,generated);
    assert.deepEqual(record.errors,[]);await noOverflow(page);
    const randomize={engine,width:320,theme:'dark',digits:generated,steps:randomized.steps.length,executions:record.executions,
      declaredHintsRespected:true,draftOnlyUntilRun:true,realSuccessfulRun:true,wholeResultsRead:true,passed:true};await page.close();
    console.log(`PASS phone ${engine}/320/dark/deterministic-randomize`);
    assert.deepEqual(packageSnapshot(),localPackage,'Jar changed during the browser journey');
    fs.writeFileSync(path.join(out,`${engine}-results.json`),JSON.stringify({generated:new Date().toISOString(),realBackend:true,complete:true,
      buildMode:'production-preview',problemId:id,engine,browserVersion:browser.version(),servedBuild,localPackage,rows,randomize,
      limits:'Bounded owner journeys; diagram220 cap is not a run truncation. Source compilation is verified separately. Not all-input, device, native zoom, screen-reader, performance, public-deployment or whole-project acceptance.'},null,2));
  }finally{await browser.close();}
}
if(require.main===module)main().catch(error=>{console.error(error);process.exitCode=1;});
