// Real declared approach URLs, actual Randomize controls, production-preview assets.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {root,owners,forms,high,assertSpec,assertInput,assertRun,work,sourceHash,packageSnapshot}=require('./api.cjs');
const seek=page=>page.getByRole('slider',{name:'Seek to step',exact:true});
async function draft(page){
  const values={};for(const [name,label] of [['weights','Weights'],['values','Values']])
    values[name]=await page.getByRole('group',{name:label,exact:true}).getByRole('spinbutton',{name:/^Position \d+ value$/})
      .evaluateAll(es=>es.map(el=>Number(el.value)));
  values.capacity=Number(await page.getByLabel('Capacity',{exact:true}).inputValue());return values;
}
async function echo(page,input){
  const summary=page.locator('[data-testid="input-summary"]:visible').first();await summary.waitFor();
  const entries=summary.locator('span[title]');
  assert.deepEqual(await entries.evaluateAll(es=>Object.fromEntries(es.map(el=>[el.previousElementSibling.textContent,el.getAttribute('title')]))),
    {weights:`[${input.weights.join(', ')}]`,values:`[${input.values.join(', ')}]`,capacity:String(input.capacity)});
  const capacity=await entries.evaluateAll(es=>{
    const el=es.find(e=>e.previousElementSibling.textContent==='capacity');if(!el)return null;
    const rect=el.getBoundingClientRect(),range=document.createRange();range.selectNodeContents(el);const text=range.getBoundingClientRect();
    let unclipped=true;
    for(let parent=el.parentElement;parent;parent=parent.parentElement)if(/^(hidden|clip|auto|scroll)$/.test(getComputedStyle(parent).overflowX)){
      const box=parent.getBoundingClientRect(),left=box.left+parent.clientLeft;
      if(text.left<left-1||text.right>left+parent.clientWidth+1)unclipped=false;
    }
    return {text:el.textContent,fits:el.clientWidth>0&&rect.height>0&&el.scrollWidth<=el.clientWidth+1
      &&text.left>=rect.left-1&&text.right<=rect.right+1&&unclipped};
  });assert.deepEqual(capacity,{text:String(input.capacity),fits:true},'Short capacity echo must actually be readable');
}
async function noOverflow(page){assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Page-wide horizontal overflow');}
async function screenshot(page,target,file){
  await target.waitFor();await target.evaluate(el=>el.scrollIntoView({block:'center',inline:'nearest',behavior:'instant'}));
  await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  const metadata=await target.evaluate(el=>{const r=el.getBoundingClientRect();return {scope:'viewport',scroll:{x:scrollX,y:scrollY},
    viewport:{width:innerWidth,height:innerHeight},targetRect:{top:r.top,right:r.right,bottom:r.bottom,left:r.left}};});
  await page.screenshot({path:file,fullPage:false});return metadata;
}
async function main(){
  const engine=process.env.AUDIT_ENGINE||'chromium';assert(['chromium','firefox'].includes(engine));
  const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
  const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180',out=path.resolve(process.env.KNAPSACK_EVIDENCE_DIR||__dirname);
  const localPackage=packageSnapshot(),servedBuild=await require('../served-build-identity.cjs')(base);
  const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  const details=new Map();for(const id of owners)for(const form of forms){
    const response=await fetch(`${base}/api/problems/${id}?approach=${form}`);assert.equal(response.status,200);
    const detail=await response.json();assertSpec(id,form,detail.inputSpec);details.set(`${id}/${form}`,detail);
  }
  const pace=require('../pace-executions.cjs')(),browser=await types[engine].launch(),rows=[],screenshots=[];fs.mkdirSync(out,{recursive:true});
  try{
    for(const id of owners)for(const form of forms)for(const [width,theme] of [[320,'dark'],[1366,'light']]){
      const detail=details.get(`${id}/${form}`),spec=detail.inputSpec,rule=spec.workLimits[0];
      const expected={weights:Array(8).fill(20),values:Array(8).fill(100),capacity:form==='canonical'?24:form==='memoization'?7:30};
      const defaults=Object.fromEntries(spec.fields.map(field=>[field.name,field.defaultValue]));
      const page=await browser.newPage({viewport:{width,height:width===320?568:768},colorScheme:theme,reducedMotion:'reduce'});
      page.setDefaultTimeout(30000);await pace(page);
      await page.addInitScript(theme=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));},theme);
      const errors=[];let executions=0;const route=url=>new URL(url).pathname===`/api/problems/${id}/execute`;
      page.on('pageerror',error=>errors.push(error.message));page.on('request',request=>{if(route(request.url()))executions++;});
      const pendingInitial=page.waitForResponse(response=>route(response.url())&&response.request().method()==='POST'
        &&new URL(response.url()).searchParams.get('approach')===form);
      await page.goto(`${base}/problem/${id}?approach=${form}`);const initial=await pendingInitial;assert.equal(initial.status(),200);
      const initialRaw=await initial.json(),prior={...initialRaw,steps:decodeTrace(initialRaw)};assertRun(prior,prior.steps,detail,defaults);
      await seek(page).waitFor();await echo(page,defaults);await page.getByRole('button',{name:'Edit input',exact:true}).click();
      assert.deepEqual(await draft(page),defaults);const before=executions,beforeUrl=page.url(),beforeStep=await seek(page).inputValue();
      await page.evaluate(high=>{window.__knapsackEvidenceRandom=Math.random;Math.random=()=>high;},high);
      try{await page.getByRole('button',{name:'Randomize',exact:true}).click();}
      finally{await page.evaluate(()=>{Math.random=window.__knapsackEvidenceRandom;delete window.__knapsackEvidenceRandom;});}
      const generated=await draft(page);assert.deepEqual(generated,expected);assertInput(spec,generated);
      assert.notDeepEqual(generated,defaults);assert.equal(executions,before);assert.equal(page.url(),beforeUrl);
      assert.equal(await seek(page).inputValue(),beforeStep);await echo(page,defaults);await noOverflow(page);
      const selected=id==='knapsack-01'&&form==='canonical',rowScreenshots=[];
      if(selected){
        const file=`${engine}-${width}-${theme}-work-draft.png`;
        const metadata=await screenshot(page,page.getByRole('region',{name:'Try your own input',exact:true}),path.join(out,file));
        rowScreenshots.push(file);screenshots.push({file,id,form,width,theme,phase:'draft',...metadata,expected:'Eight weight20/value100 pairs; capacity24 within the225-cell limit.'});
      }
      const pending=page.waitForResponse(response=>route(response.url())&&response.request().method()==='POST');
      await page.getByRole('button',{name:'Run input',exact:true}).click();const response=await pending;assert.equal(response.status(),200);
      assert.deepEqual(response.request().postDataJSON(),generated);const raw=await response.json(),run={...raw,steps:decodeTrace(raw)};
      const answer=assertRun(run,run.steps,detail,generated);
      await page.waitForFunction(()=>document.activeElement?.id==='stage-title');assert.equal(await seek(page).inputValue(),'0');
      await echo(page,generated);assert.deepEqual(await draft(page),generated);
      await page.waitForFunction(()=>new URL(location.href).searchParams.get('input')!==null);
      const url=new URL(page.url());assert.deepEqual(JSON.parse(Buffer.from(url.searchParams.get('input'),'base64url').toString('utf8')),generated);
      assert.equal(url.searchParams.get('approach'),form==='canonical'?null:form);assert.equal(executions,before+1);
      await seek(page).fill(String(run.steps.length-1));
      if(selected){
        const file=`${engine}-${width}-${theme}-work-run.png`;
        const metadata=await screenshot(page,page.locator('[data-testid="input-summary"]:visible').first(),path.join(out,file));
        rowScreenshots.push(file);screenshots.push({file,id,form,width,theme,phase:'complete-run',...metadata,expected:'Successful original eight-pair input; readable capacity24; complete answer100.'});
      }
      await page.getByRole('tab',{name:'Code walkthrough',exact:true}).click();
      const sourceButton=page.getByRole('button',{name:'Source',exact:true});if(await sourceButton.isVisible())await sourceButton.click();
      const source=page.getByRole('region',{name:'Java source',exact:true});await source.waitFor();
      assert.equal((await source.locator(':scope > div').evaluateAll(es=>es.map(el=>el.lastElementChild.textContent).join('\n'))).replace(/\r\n/g,'\n'),run.code.replace(/\r\n/g,'\n'));
      const active=source.locator('[data-active-line="true"]');await active.waitFor();assert.equal(await active.count(),1);
      assert.equal(await active.evaluate(el=>el.children[1].textContent),String(run.steps.at(-1).activeLine));await noOverflow(page);
      await page.getByRole('tab',{name:'Analysis',exact:true}).click();
      const variables=page.getByRole('region',{name:'Variables',exact:true});await variables.waitFor();
      const answerRow=variables.locator('dl > div').filter({has:page.getByText('answer',{exact:true})});
      assert.equal(await answerRow.locator('dd > code').textContent(),String(answer.expected));await noOverflow(page);
      await page.getByRole('tab',{name:'Playground',exact:true}).click();assert.equal(await seek(page).inputValue(),String(run.steps.length-1));
      await echo(page,generated);assert.equal(executions,before+1);assert.deepEqual(errors,[]);await noOverflow(page);
      rows.push({engine,id,form,width,height:width===320?568:768,theme,input:generated,work:work(rule,generated),limit:rule.limit,
        steps:run.steps.length,...answer,executions,sourceHash:sourceHash(run.code),screenshots:rowScreenshots,
        actualApproachUrlAndRandomize:true,pairedDraftOnlyUntilRun:true,priorStepInputAndUrlPreserved:true,completeUntruncatedPost:true,
        stageHeadingFocused:true,actualSourceAndHighlight:true,updatedEchoAndShareInput:true,shortCapacityActuallyReadable:true,
        viewChangesPreserveStepWithoutExecution:true,noPageOverflowOrErrors:true,passed:true});
      await page.close();console.log(`PASS work browser ${engine}/${id}/${form}/${width}/${theme}`);
    }
    assert.equal(rows.length,12);assert.equal(screenshots.length,4);assert.deepEqual(packageSnapshot(),localPackage);
    fs.writeFileSync(path.join(out,`${engine}-results.json`),JSON.stringify({generated:new Date().toISOString(),realBackend:true,complete:true,
      buildMode:'production-preview',engine,browserVersion:browser.version(),servedBuild,localPackage,rows,screenshots,
      limits:'Six knapsack specifications with high RNG on320dark/1366light; cheap eight-item recursion retained. Four viewport captures require visual inspection. Not all-input, other joint budgets, device, screen-reader, native zoom, performance, public rollout or whole-project acceptance.'},null,2));
  }finally{await browser.close();}
}
if(require.main===module)main().catch(error=>{console.error(error);process.exitCode=1;});
