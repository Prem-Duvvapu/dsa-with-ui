// Actual production-preview pairing journey. Randomize remains draft-only until Run.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {root,sourceHash,packageSnapshot,assertSpec,assertIdentity,assertTrace}=require('./api.cjs');
const id='knapsack-01',generated={weights:[1],values:[1],capacity:1};
const seek=page=>page.getByRole('slider',{name:'Seek to step',exact:true});
async function draft(page){
  const arrays={};for(const [field,label] of [['weights','Weights'],['values','Values']])
    arrays[field]=await page.getByRole('group',{name:label,exact:true}).getByRole('spinbutton',{name:/^Position \d+ value$/})
      .evaluateAll(es=>es.map(el=>Number(el.value)));
  return {...arrays,capacity:Number(await page.getByLabel('Capacity',{exact:true}).inputValue())};
}
async function echo(page,input){
  const summary=page.locator('[data-testid="input-summary"]:visible').first();await summary.waitFor();
  assert.deepEqual(await summary.locator('span[title]').evaluateAll(es=>Object.fromEntries(es.map(el=>
    [el.previousElementSibling.textContent,el.getAttribute('title')]))),
  {weights:`[${input.weights.join(', ')}]`,values:`[${input.values.join(', ')}]`,capacity:String(input.capacity)});
  if(input.capacity===1&&input.weights.length===1&&input.weights[0]===1&&input.values.length===1&&input.values[0]===1){
    const shortValues=await summary.locator('span[title]').evaluateAll(es=>es.map(el=>{
      const field=el.previousElementSibling.textContent,rect=el.getBoundingClientRect(),range=document.createRange();
      range.selectNodeContents(el);const textRect=range.getBoundingClientRect(),style=getComputedStyle(el),clippedBy=[];
      for(let ancestor=el.parentElement;ancestor;ancestor=ancestor.parentElement){
        const ancestorStyle=getComputedStyle(ancestor);
        if(/^(hidden|clip|auto|scroll)$/.test(ancestorStyle.overflowX)){
          const box=ancestor.getBoundingClientRect(),left=box.left+ancestor.clientLeft,right=left+ancestor.clientWidth;
          if(textRect.left<left-1||textRect.right>right+1)clippedBy.push({tag:ancestor.tagName,left,right});
        }
      }
      return {field,text:el.textContent,clientWidth:el.clientWidth,scrollWidth:el.scrollWidth,
        fullTextFits:el.clientWidth>0&&rect.height>0&&textRect.width>0&&el.scrollWidth<=el.clientWidth+1
          &&textRect.left>=rect.left-1&&textRect.right<=rect.right+1&&clippedBy.length===0
          &&style.visibility==='visible'&&style.display!=='none',clippedBy};
    }));
    assert.deepEqual(Object.fromEntries(shortValues.map(value=>[value.field,value.text])),{weights:'[1]',values:'[1]',capacity:'1'});
    assert.deepEqual(shortValues.filter(value=>!value.fullTextFits),[],'Short input values must actually fit visibly, not merely survive in title attributes');
  }
}
async function noOverflow(page){assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Page-wide horizontal overflow');}
async function randomizeZero(page){
  // Override RNG only for the actual Randomize button event, restoring it immediately.
  await page.evaluate(()=>{window.__pairedEvidenceRandom=Math.random;Math.random=()=>0;});
  try{await page.getByRole('button',{name:'Randomize',exact:true}).click();}
  finally{await page.evaluate(()=>{Math.random=window.__pairedEvidenceRandom;delete window.__pairedEvidenceRandom;});}
}
async function capture(page,target,file){
  await target.waitFor();await target.evaluate(el=>el.scrollIntoView({block:'center',inline:'nearest',behavior:'instant'}));
  await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  const metadata=await target.evaluate(el=>{const rect=el.getBoundingClientRect();return {scope:'viewport',scroll:{x:scrollX,y:scrollY},
    viewport:{width:innerWidth,height:innerHeight},targetRect:{top:rect.top,right:rect.right,bottom:rect.bottom,left:rect.left}};});
  await page.screenshot({path:file,fullPage:false});return metadata;
}
async function main(){
  const engine=process.env.AUDIT_ENGINE||'chromium';assert(['chromium','firefox'].includes(engine));
  const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
  const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180',out=path.resolve(process.env.PAIRED_EVIDENCE_DIR||__dirname);
  const localPackage=packageSnapshot(),servedBuild=await require('../served-build-identity.cjs')(base);
  const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  const detailResponse=await fetch(`${base}/api/problems/${id}`);assert.equal(detailResponse.status,200);
  const detail=await detailResponse.json();assertSpec(id,detail.inputSpec);
  const defaults=Object.fromEntries(detail.inputSpec.fields.map(field=>[field.name,field.defaultValue]));
  const pace=require('../pace-executions.cjs')(),browser=await types[engine].launch(),rows=[],screenshots=[];fs.mkdirSync(out,{recursive:true});
  const endpoint=response=>new URL(response.url()).pathname===`/api/problems/${id}/execute`;
  async function readRun(response,input){
    assert.equal(response.status(),200);const raw=await response.json(),run={...raw,steps:decodeTrace(raw)};
    assertIdentity(run,detail,input);assertTrace(run,run.steps,input);return run;
  }
  try{
    for(const width of [320,1366])for(const theme of ['light','dark']){
      const page=await browser.newPage({viewport:{width,height:width===320?568:768},colorScheme:theme,reducedMotion:'reduce'});
      page.setDefaultTimeout(25000);await pace(page);
      await page.addInitScript(theme=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));},theme);
      const errors=[];let executions=0;
      page.on('pageerror',error=>errors.push(error.message));page.on('request',request=>{if(new URL(request.url()).pathname===`/api/problems/${id}/execute`)executions++;});
      const initial=page.waitForResponse(response=>endpoint(response)&&response.request().method()==='GET');
      await page.goto(`${base}/problem/${id}`);const prior=await readRun(await initial,defaults);await seek(page).waitFor();await echo(page,defaults);
      await page.getByRole('button',{name:'Edit input',exact:true}).click();assert.deepEqual(await draft(page),defaults);
      const before=executions,beforeUrl=page.url(),beforeStep=await seek(page).inputValue();
      await randomizeZero(page);
      const changed=await draft(page);assert.deepEqual(changed,generated);assert.notDeepEqual(changed,defaults);
      assert.equal(changed.weights.length,changed.values.length);assert.equal(executions,before,'Randomize executed instead of only changing the draft');
      assert.equal(page.url(),beforeUrl);assert.equal(await seek(page).inputValue(),beforeStep);await echo(page,defaults);await noOverflow(page);
      const selected=width===320&&theme==='dark'||width===1366&&theme==='light',rowScreenshots=[];
      if(selected){
        const file=`${engine}-${width}-${theme}-paired-draft.png`;
        const metadata=await capture(page,page.getByRole('region',{name:'Try your own input',exact:true}),path.join(out,file));
        rowScreenshots.push(file);screenshots.push({file,engine,width,theme,phase:'randomized-draft',...metadata,
          expected:'One weight1 and its paired value1; capacity1; the prior run remains until Run.'});
      }
      const pending=page.waitForResponse(response=>endpoint(response)&&response.request().method()==='POST');
      await page.getByRole('button',{name:'Run input',exact:true}).click();const response=await pending;
      assert.deepEqual(response.request().postDataJSON(),generated);const run=await readRun(response,generated);
      await page.waitForFunction(()=>document.activeElement?.id==='stage-title');
      assert.equal(await page.getByRole('heading',{name:'DP table',exact:true}).evaluate(el=>el===document.activeElement),true);
      await echo(page,generated);assert.deepEqual(await draft(page),generated);
      await page.waitForFunction(()=>new URL(location.href).searchParams.get('input')!==null);
      const encoded=new URL(page.url()).searchParams.get('input');assert.deepEqual(JSON.parse(Buffer.from(encoded,'base64url').toString('utf8')),generated);
      assert.equal(executions,before+1);assert.equal(await seek(page).inputValue(),'0');
      await seek(page).fill(String(run.steps.length-1));
      if(selected){
        const file=`${engine}-${width}-${theme}-paired-run.png`;
        const metadata=await capture(page,page.locator('[data-testid="input-summary"]:visible').first(),path.join(out,file));
        rowScreenshots.push(file);screenshots.push({file,engine,width,theme,phase:'successful-run',...metadata,
          expected:'Updated input echo weights[1],values[1],capacity1; complete canonical answer1.'});
      }
      await page.getByRole('tab',{name:'Code walkthrough',exact:true}).click();
      const button=page.getByRole('button',{name:'Source',exact:true});if(await button.isVisible())await button.click();
      const source=page.getByRole('region',{name:'Java source',exact:true});await source.waitFor();
      assert.equal((await source.locator(':scope > div').evaluateAll(es=>es.map(el=>el.lastElementChild.textContent).join('\n'))).replace(/\r\n/g,'\n'),run.code.replace(/\r\n/g,'\n'));
      const active=source.locator('[data-active-line="true"]');await active.waitFor();assert.equal(await active.count(),1);
      assert.equal(await active.evaluate(el=>el.children[1].textContent),String(run.steps.at(-1).activeLine));await noOverflow(page);
      await page.getByRole('tab',{name:'Playground',exact:true}).click();assert.equal(await seek(page).inputValue(),String(run.steps.length-1));
      await echo(page,generated);assert.equal(executions,before+1);assert.deepEqual(errors,[]);await noOverflow(page);
      let mismatchRecovery=null;
      if(width===320&&theme==='dark'){
        await page.waitForFunction(step=>new URL(location.href).searchParams.get('step')===String(step),run.steps.length);
        await page.waitForFunction(()=>{const view=new URL(location.href).searchParams.get('view');return view===null||view==='playground';});
        const successfulUrl=page.url(),successfulStep=await seek(page).inputValue(),beforeMismatch=executions;
        await page.getByRole('button',{name:'Edit input',exact:true}).click();
        const weights=page.getByRole('group',{name:'Weights',exact:true});await weights.getByRole('button',{name:'Add',exact:true}).click();
        await weights.getByRole('spinbutton',{name:'Position 2 value',exact:true}).fill('1');
        const unequal={weights:[1,1],values:[1],capacity:1};assert.deepEqual(await draft(page),unequal);
        assert.equal(executions,beforeMismatch);assert.equal(page.url(),successfulUrl);
        const refused=page.waitForResponse(response=>endpoint(response)&&response.request().method()==='POST');
        await page.getByRole('button',{name:'Run input',exact:true}).click();const rejection=await refused;
        assert.deepEqual(rejection.request().postDataJSON(),unequal);assert.equal(rejection.status(),400);
        const failure=await rejection.json();assert.equal(typeof failure.fieldErrors?.values,'string');assert.equal(failure.steps,undefined);
        const alert=page.getByRole('alert').filter({hasText:'This input could not run'});await alert.waitFor();
        assert(await alert.evaluate(el=>el===document.activeElement),'Manual mismatch did not focus the error summary');
        assert.deepEqual(await draft(page),unequal);await echo(page,generated);
        assert.equal(await seek(page).inputValue(),successfulStep);assert.equal(page.url(),successfulUrl);assert.equal(executions,beforeMismatch+1);
        await page.getByRole('tab',{name:'Code walkthrough',exact:true}).click();if(await button.isVisible())await button.click();
        await source.waitFor();
        assert.equal((await source.locator(':scope > div').evaluateAll(es=>es.map(el=>el.lastElementChild.textContent).join('\n'))).replace(/\r\n/g,'\n'),run.code.replace(/\r\n/g,'\n'));
        await active.waitFor();assert.equal(await active.count(),1);assert.equal(await active.evaluate(el=>el.children[1].textContent),String(run.steps.at(-1).activeLine));
        await page.getByRole('tab',{name:'Playground',exact:true}).click();
        await page.waitForFunction(url=>location.href===url,successfulUrl);assert.deepEqual(await draft(page),unequal);
        assert.equal(await seek(page).inputValue(),successfulStep);await echo(page,generated);assert.equal(executions,beforeMismatch+1);
        await randomizeZero(page);assert.deepEqual(await draft(page),generated);
        assert.equal(executions,beforeMismatch+1,'Recovery Randomize must remain draft-only');assert.equal(page.url(),successfulUrl);
        assert.equal(await seek(page).inputValue(),successfulStep);await echo(page,generated);
        const recovering=page.waitForResponse(response=>endpoint(response)&&response.request().method()==='POST');
        await page.getByRole('button',{name:'Run input',exact:true}).click();const recoveredResponse=await recovering;
        assert.deepEqual(recoveredResponse.request().postDataJSON(),generated);const recovered=await readRun(recoveredResponse,generated);
        await page.waitForFunction(()=>document.activeElement?.id==='stage-title');
        assert.equal(await alert.count(),0,'Successful recovery left the rejected-input error visible');
        assert.deepEqual(await draft(page),generated);await echo(page,generated);assert.equal(await seek(page).inputValue(),'0');
        assert.equal(executions,beforeMismatch+2);
        const recoveredEncoded=new URL(page.url()).searchParams.get('input');
        assert.deepEqual(JSON.parse(Buffer.from(recoveredEncoded,'base64url').toString('utf8')),generated);
        mismatchRecovery={rejectedInput:unequal,status:rejection.status(),field:'values',fieldError:failure.fieldErrors.values,
          previousInput:generated,previousStep:Number(successfulStep)+1,previousSourceHash:sourceHash(run.code),previousUrl:successfulUrl,
          unequalDraftAndPriorRunSourceInputStepUrlPreserved:true,errorSummaryFocused:true,
          randomizeRepairsDraftWithoutPostOrUrlChange:true,recoveredInput:generated,recoveredStatus:recoveredResponse.status(),
          recoveredAnswer:Number(recovered.steps.at(-1).variables.answer),successfulRecoveryClearsError:true,stageHeadingFocused:true};
      }
      assert.deepEqual(errors,[]);await noOverflow(page);
      rows.push({engine,width,height:width===320?568:768,theme,problemId:id,input:generated,priorInput:defaults,
        priorSteps:prior.steps.length,steps:run.steps.length,answer:Number(run.steps.at(-1).variables.answer),executions,sourceHash:sourceHash(run.code),
        screenshots:rowScreenshots,mismatchRecovery,declaredPairing:true,actualRandomizeButton:true,changedPairedDraftOnlyUntilRun:true,
        priorRunInputStepAndUrlPreservedUntilRun:true,requestPreservesIndexOrder:true,realSuccessfulRun:true,
        stageHeadingFocused:true,updatedEchoAndShareInput:true,shortInputValuesActuallyVisible:true,actualSourceAndHighlight:true,viewSwitchDoesNotExecute:true,noPageOverflowOrErrors:true,passed:true});
      await page.close();console.log(`PASS paired ${engine}/${width}/${theme}`);
    }
    assert.equal(rows.length,4);assert.equal(screenshots.length,4);assert.deepEqual(packageSnapshot(),localPackage);
    fs.writeFileSync(path.join(out,`${engine}-results.json`),JSON.stringify({generated:new Date().toISOString(),realBackend:true,complete:true,
      buildMode:'production-preview',engine,browserVersion:browser.version(),servedBuild,localPackage,rows,screenshots,
      limits:'One declared canonical pair and deterministic RNG0, two widths and both themes. Draft change means actual edited fields; no badge claim. Existing joint budgets are not generated/solved. Screenshots need visual inspection. Not all-input, device, screen-reader, native zoom, performance, public-deployment or whole-project acceptance.'},null,2));
  }finally{await browser.close();}
}
if(require.main===module)main().catch(error=>{console.error(error);process.exitCode=1;});
