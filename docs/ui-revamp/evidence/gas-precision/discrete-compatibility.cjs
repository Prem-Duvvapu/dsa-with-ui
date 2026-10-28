// Bounded regression proof for the two legacy discrete SearchSpace shapes.
// Run against a packaged backend and production preview; no execution is fabricated.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {root,packageSnapshot,sourceHash}=require('./api.cjs');
const owners=[
  {id:'binary-search-1d',kind:'indices',input:{nums:[1,3,5,7,9,11,13],target:5},result:2},
  {id:'book-allocation',kind:'answers',input:{pages:[12,34,67,90],m:2},result:113},
];
const finite=value=>typeof value==='number'||typeof value==='string'&&value.trim()!==''
  ?Number.isFinite(Number(value))?Number(value):null:null;
function pairedBounds(variables={}){
  for(const [lo,hi] of [['low','high'],['lo','hi'],['left','right'],['l','r'],['start','end']]){
    const low=finite(variables?.[lo]),high=finite(variables?.[hi]);
    if(low!==null&&high!==null)return {low,high,mid:finite(variables.mid),
      domain:Object.hasOwn(variables,'searchDomain')?variables.searchDomain:'discrete'};
  }
  return null;
}
function rangeAt(steps,index){
  for(let i=index;i>=0;i--){const b=pairedBounds(steps[i]?.variables);if(b)return b;}
  throw new Error(`No paired search bounds at step ${index+1}`);
}
const seek=page=>page.getByRole('slider',{name:'Seek to step',exact:true});
async function echo(page,input){
  const summary=page.locator('[data-testid="input-summary"]:visible').first();await summary.waitFor();
  const shown=await summary.locator('span[title]').evaluateAll(es=>Object.fromEntries(es.map(el=>
    [el.previousElementSibling.textContent,el.getAttribute('title')])));
  assert.deepEqual(shown,Object.fromEntries(Object.entries(input).map(([key,value])=>
    [key,Array.isArray(value)?`[${value.join(', ')}]`:String(value)])));
}
async function assertRange(page,owner,steps,index){
  const initial=steps.map(s=>pairedBounds(s.variables)).find(Boolean),here=rangeAt(steps,index);
  assert.equal(here.domain,'discrete');assert(Number.isInteger(here.low)&&Number.isInteger(here.high));
  const remaining=Math.max(0,here.high-here.low+1),span=initial.high-initial.low+1;
  const expected=`${owner.kind} [${here.low}, ${here.high}] · ${remaining} left of ${span}`;
  const badge=page.getByTestId('search-range');
  await badge.filter({hasText:expected}).waitFor();assert.equal(await badge.textContent(),expected);
  assert(await badge.evaluate(el=>el.scrollWidth<=el.clientWidth+1),'Range text is clipped');
  assert.equal(await page.getByTestId('search-space-canvas').count(),1);
  assert.equal(await page.getByRole('status').filter({hasText:/unsupported search domain|invalid continuous|no search range/i}).count(),0);
  const canvasText=await page.getByTestId('search-space-canvas').textContent();
  assert(!/continuous|unsupported/i.test(canvasText),'Discrete run acquired continuous/unsupported labelling');
  const live=page.getByTestId('search-live');
  if(remaining>0){
    await live.waitFor();const geometry=await live.evaluate((el,values)=>{
      const expected=document.createElement('div');
      expected.style.left=`${values.left}%`;expected.style.width=`${values.width}%`;
      return {actual:{left:el.style.left,width:el.style.width},
        expected:{left:expected.style.left,width:expected.style.width}};
    },{left:(here.low-initial.low)/span*100,width:remaining/span*100});
    assert.deepEqual(geometry.actual,geometry.expected);
  }else assert.equal(await live.count(),0,'Exhausted integer range still has live candidates');
  if(here.mid!==null)assert.equal(await page.getByTestId('search-mid').textContent(),`probing ${here.mid}`);
  else assert.equal(await page.getByTestId('search-mid').count(),0);
  const step=steps[index],values=step.arrayState.map(cell=>String(cell.label??cell.value));
  if(owner.kind==='indices'){
    const cells=page.getByTestId('search-cells').locator(':scope > div');
    assert.deepEqual(await cells.evaluateAll(es=>es.map(el=>el.firstElementChild.textContent)),values);
    assert.deepEqual(await cells.evaluateAll(es=>es.map(el=>el.getAttribute('data-alive')==='true')),
      values.map((_,i)=>i>=here.low&&i<=here.high));
    assert.deepEqual(await cells.evaluateAll(es=>es.map(el=>el.getAttribute('data-mid')==='true')),
      values.map((_,i)=>i===here.mid));
    assert.equal(await page.getByTestId('search-input-row').count(),0);
  }else{
    const cells=page.getByTestId('search-input-row').locator('[data-state]');
    assert.deepEqual(await cells.evaluateAll(es=>es.map(el=>el.firstElementChild.textContent)),values);
    assert.deepEqual(await cells.evaluateAll(es=>es.map(el=>el.getAttribute('data-state'))),step.arrayState.map(cell=>cell.state));
    assert.equal(await page.getByTestId('search-cells').count(),0);
  }
  if(Object.hasOwn(step.variables,'answer'))assert.equal(await page.getByTestId('search-result').textContent(),`Result: ${Number(step.variables.answer)}`);
  else assert.equal(await page.getByTestId('search-result').count(),0,'Invented explicit answer');
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Page-wide overflow in Playground');
  return {step:index+1,low:here.low,high:here.high,mid:here.mid,remaining,originalCandidates:span};
}
async function assertSource(page,run,index){
  await page.getByRole('tab',{name:'Code walkthrough',exact:true}).click();
  const button=page.getByRole('button',{name:'Source',exact:true});if(await button.isVisible())await button.click();
  const region=page.getByRole('region',{name:'Java source',exact:true});await region.waitFor();
  const displayed=await region.locator(':scope > div').evaluateAll(es=>es.map(el=>el.lastElementChild.textContent).join('\n'));
  assert.equal(displayed.replace(/\r\n/g,'\n'),run.code.replace(/\r\n/g,'\n'));
  const active=region.locator('[data-active-line="true"]');await active.waitFor();assert.equal(await active.count(),1);
  assert.equal(await active.evaluate(el=>el.children[1].textContent),String(run.steps[index].activeLine));
  assert(await page.getByTestId('search-space-canvas').count()<=1,'More than one primary canvas');
  assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Page-wide overflow in Code');
  await page.getByRole('tab',{name:'Playground',exact:true}).click();
  assert.equal(await seek(page).inputValue(),String(index),'View switch lost the current step');
}
function assertRun(run,detail,owner){
  assert.equal(run.problemId,owner.id);assert.equal(run.approachId,'canonical');
  assert.equal(run.dsType,'SearchSpace');assert.equal(detail.dsType,run.dsType);
  assert.equal(run.code,detail.javaCode);assert.deepEqual(run.complexity,detail.complexity);
  assert.deepEqual(run.resolvedInput,owner.input);assert.equal(run.truncated,false);
  assert(run.steps.length>2);assert.equal(run.stepCount,run.steps.length);
  const defaults=Object.fromEntries(detail.inputSpec.fields.map(f=>[f.name,f.defaultValue]));
  assert.deepEqual(defaults,owner.input,'Authored case no longer matches the real input specification');
  const sourceLines=run.code.split('\n').length;
  for(const step of run.steps){
    assert(Number.isInteger(step.activeLine)&&step.activeLine>0&&step.activeLine<=sourceLines);
    assert(Array.isArray(step.arrayState)&&step.arrayState.length>0);
    assert.deepEqual(step.arrayState.map(cell=>cell.value),owner.input.nums??owner.input.pages);
    if(Object.hasOwn(step.variables,'searchDomain'))assert.equal(step.variables.searchDomain,'discrete');
    const b=pairedBounds(step.variables);if(b)assert(Number.isInteger(b.low)&&Number.isInteger(b.high));
  }
  const final=run.steps.at(-1),field=owner.kind==='indices'?'result':'answer';
  assert.equal(Number(final.variables[field]),owner.result);
  if(owner.kind==='answers'){
    const b=rangeAt(run.steps,run.steps.length-1);assert.equal(Math.max(0,b.high-b.low+1),0);
    assert.equal(b.mid,null,'Book Allocation final snapshot must not claim to be probing');
  }
}
async function main(){
  const engine=process.env.AUDIT_ENGINE||'chromium';assert(['chromium','firefox'].includes(engine));
  const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
  const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180';
  const out=path.resolve(process.env.GAS_EVIDENCE_DIR||__dirname);
  const pace=require('../pace-executions.cjs')();
  const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  const localPackage=packageSnapshot(),servedBuild=await require('../served-build-identity.cjs')(base);
  const details=new Map();
  for(const owner of owners){const response=await fetch(`${base}/api/problems/${owner.id}`);assert.equal(response.status,200);details.set(owner.id,await response.json());}
  fs.mkdirSync(out,{recursive:true});const browser=await types[engine].launch(),rows=[];
  try{
    for(const width of [320,1366])for(const theme of ['light','dark'])for(const owner of owners){
      const page=await browser.newPage({viewport:{width,height:width===320?568:768},colorScheme:theme,reducedMotion:'reduce'});
      page.setDefaultTimeout(25000);await pace(page);
      await page.addInitScript(theme=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));},theme);
      const errors=[];let executions=0;
      const endpoint=url=>new URL(url).pathname===`/api/problems/${owner.id}/execute`;
      page.on('pageerror',error=>errors.push(error.message));page.on('request',request=>{if(endpoint(request.url()))executions++;});
      const pending=page.waitForResponse(response=>endpoint(response.url())&&response.request().method()==='GET');
      await page.goto(`${base}/problem/${owner.id}`);const response=await pending;assert.equal(response.status(),200);
      const raw=await response.json(),run={...raw,steps:decodeTrace(raw)};assertRun(run,details.get(owner.id),owner);
      await seek(page).waitFor();await echo(page,owner.input);const originalExecutions=executions;
      const probe=run.steps.findIndex(s=>pairedBounds(s.variables)?.mid!==null&&pairedBounds(s.variables)?.mid!==undefined);
      assert(probe>0);const inspected=[...new Set([0,probe,run.steps.length-1])],ranges=[];
      for(const index of inspected){
        await seek(page).fill(String(index));ranges.push(await assertRange(page,owner,run.steps,index));
        await assertSource(page,run,index);await assertRange(page,owner,run.steps,index);
        await echo(page,owner.input);
      }
      assert.equal(executions,originalExecutions,'Inspection or switching views caused another execution');assert.deepEqual(errors,[]);
      rows.push({engine,width,height:width===320?568:768,theme,problemId:owner.id,searchKind:owner.kind,input:owner.input,
        sourceHash:sourceHash(run.code),encoding:raw.encoding??'full',steps:run.steps.length,ranges,executions,
        actualDefaultsAndSource:true,actualDecodedValuesAndHighlights:true,legacyInclusiveCountsAndRuler:true,
        noContinuousOrUnsupportedLabelling:true,viewChangesKeepStepAndDoNotExecute:true,noPageOverflowOrErrors:true,passed:true});
      await page.close();console.log(`PASS discrete ${engine}/${width}/${theme}/${owner.id}`);
    }
    assert.equal(rows.length,8);assert.deepEqual(packageSnapshot(),localPackage,'Jar changed during discrete compatibility phase');
    fs.writeFileSync(path.join(out,`${engine}-discrete-results.json`),JSON.stringify({generated:new Date().toISOString(),realBackend:true,
      buildMode:'production-preview',complete:true,engine,browserVersion:browser.version(),servedBuild,localPackage,rows,
      limits:'Two authored default discrete owners, three actual snapshots each, two widths and both themes. Not all-input, native zoom, device, screen-reader, public deployment or whole-project acceptance.'},null,2));
  }finally{await browser.close();}
}
module.exports={pairedBounds,rangeAt};
if(require.main===module)main().catch(error=>{console.error(error);process.exitCode=1;});
