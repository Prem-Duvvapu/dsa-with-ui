// Real production bundle and backend; no invented execution responses or raised limits.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const types = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const { cases, maximum, oracle } = require('./linear-grid-cases.cjs');
const engine = process.env.AUDIT_ENGINE || 'chromium';
const zoom = Number(process.env.AUDIT_NATIVE_ZOOM || 1);
const mode = process.env.AUDIT_MODE || 'standard';
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const out = path.join(__dirname, 'linear-grid');
const sizes = process.env.AUDIT_SIZES ? process.env.AUDIT_SIZES.split(',').map(s => s.split('x').map(Number))
  : [[320,568],[390,844],[768,1024],[1366,768],[1440,900]];
assert(['chromium','firefox'].includes(engine));
assert(['standard','maximum'].includes(mode));
assert(zoom === 1 || zoom === 2 && engine === 'chromium');
const pace = require('../evidence/pace-executions.cjs')();
const selector = page => page.getByRole('combobox', { name:'Solution approach', exact:true });
const seek = page => page.getByRole('slider', { name:'Seek to step', exact:true });
const share = input => Buffer.from(JSON.stringify(input)).toString('base64url');
let decodeTrace;
async function ready(page) {
  await selector(page).waitFor();
  await page.waitForFunction(() => !document.querySelector('select[id]')?.disabled);
}
async function capture(page, action, status = 200) {
  const pending = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/execute'));
  await action();
  const response = await pending;
  assert.equal(response.status(), status);
  const raw = await response.json();
  const run = status === 200 ? {...raw,steps:decodeTrace(raw)} : raw;
  await ready(page);
  if (status === 200) await page.waitForFunction(id => {
    const current = new URL(location.href).searchParams.get('approach');
    return id === 'canonical' ? current === null : current === id;
  },run.approachId);
  return run;
}
async function switchRun(page, form) {
  const previous = page.url();
  await selector(page).selectOption(form);
  assert.equal(page.url(),previous,'Preparation changed the shared run');
  return capture(page, () => page.getByRole('button',{name:/^Run (recursion|memoization|tabulation)/}).click());
}
async function safe(page,errors) {
  assert.deepEqual(errors,[]);
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth),'Page-wide overflow');
  assert(await page.locator('[data-recursion-diagram]').count() <= 1,'Duplicate primary canvas');
}
async function edit(page,id,input) {
  const open = page.getByRole('button',{name:'Edit input',exact:true});
  if(await open.isVisible()) await open.click();
  if(id === 'grid-unique-paths') {
    await page.getByLabel('Rows',{exact:true}).fill(String(input.rows));
    await page.getByLabel('Columns',{exact:true}).fill(String(input.cols));
    return;
  }
  const values=input.heights??input.nums;
  const positions=page.getByRole('spinbutton',{name:/^Position \d+ value$/});
  while(await positions.count() < values.length) await page.getByRole('button',{name:'Add',exact:true}).click();
  while(await positions.count() > values.length) await page.getByRole('button',{name:`Remove position ${await positions.count()}`,exact:true}).click();
  for(let i=0;i<values.length;i++) await page.getByLabel(`Position ${i+1} value`,{exact:true}).fill(String(values[i]));
  if(input.k !== undefined) await page.getByLabel('Max jump distance',{exact:true}).fill(String(input.k));
}
async function assertDraft(page,id,input) {
  if(id === 'grid-unique-paths') {
    assert.equal(await page.getByLabel('Rows',{exact:true}).inputValue(),String(input.rows));
    assert.equal(await page.getByLabel('Columns',{exact:true}).inputValue(),String(input.cols));
  }
  else assert.deepEqual(await page.getByRole('spinbutton',{name:/^Position \d+ value$/}).evaluateAll(es=>es.map(e=>Number(e.value))),input.heights??input.nums);
}
function assertAnswer(run,input) {
  assert(!run.truncated);
  assert.deepEqual(run.resolvedInput,input);
  assert.equal(run.steps.at(-1).variables.answer,String(oracle(run.problemId,input)));
}
(async()=>{
  ({decodeTrace}=await import(path.resolve(__dirname,'../../../frontend/src/trace/decodeTrace.js')));
  fs.mkdirSync(out,{recursive:true});
  const servedBuild=await require('../evidence/served-build-identity.cjs')(base);
  const browser=zoom===2?await require('../evidence/native-browser-zoom.cjs')(types.chromium):await types[engine].launch();
  const rows=[];
  try {
    for(const c of cases) for(const [width,height] of sizes) for(const theme of ['light','dark'])
      for(const form of mode==='maximum'?['recursion','memoization']:['recursion']) {
        const page=await browser.newPage({viewport:{width,height},colorScheme:theme});
        page.setDefaultTimeout(20000);
        await pace(page);
        await page.addInitScript(theme=>{
          localStorage.setItem('dsa-ui:seenWelcome','true');
          localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));
        },theme);
        const errors=[];let requests=0;
        page.on('pageerror',e=>errors.push(e.message));
        page.on('request',r=>{if(new URL(r.url()).pathname.endsWith('/execute'))requests++;});
        const input=mode==='maximum'?maximum(c.id,form):c.input;
        let run=await capture(page,()=>page.goto(`${base}/problem/${c.id}?approach=${form}&input=${share(input)}&step=3`));
        assertAnswer(run,input);
        await page.waitForFunction(()=>document.querySelector('input[aria-label="Seek to step"]')?.value==='2');
        if(zoom===2) await browser.setZoom(page,2);
        assert.equal(await page.evaluate(()=>innerWidth),Math.round(width/zoom));
        let maximumCalls=null;
        if(mode==='maximum') {
          maximumCalls=Number(run.steps.at(-1).variables.calls);
          assert(maximumCalls<=220);
          await seek(page).fill(String(run.steps.length-1));
          await page.getByRole('img',{name:`Recursion tree, ${maximumCalls} calls explored`,exact:true}).waitFor();
          const tree=page.locator('[data-recursion-diagram]');
          assert.equal(await tree.locator('g[data-state]').count(),maximumCalls);
          const root=tree.locator('[data-recursion-root]');
          assert(await root.evaluate(el=>{
            const r=el.getBoundingClientRect(),s=el.closest('[data-testid="recursion-tree-stage"]').getBoundingClientRect();
            return r.right>s.left&&r.left<s.right&&r.bottom>s.top&&r.top<s.bottom;
          }),'All calls exist but root is outside the local viewport');
          if(form==='memoization') {
            await page.getByText('Memo table',{exact:true}).click();
            const table=page.locator('table.dp-table');
            await table.waitFor();
            const cells=run.steps.at(-1).dpTable.cells;
            assert.equal(await table.locator('td').count(),cells.reduce((n,row)=>n+row.length,0));
          }
        } else {
          const beforeSelection=requests;
          const memo=await switchRun(page,'memoization');
          assert.equal(requests,beforeSelection+1,'Selection executed without explicit Run');
          assertAnswer(memo,input);
          assert(await page.locator('#stage-title').evaluate(e=>e===document.activeElement));
          await seek(page).fill('2');
          await page.waitForFunction(()=>new URL(location.href).searchParams.get('step')==='3');
          const beforeViews=requests;
          for(const name of ['Code walkthrough','Analysis','Playground']) {
            await page.getByRole('tab',{name,exact:true}).click();
            if(name==='Code walkthrough') {
              const sourceButton=page.getByRole('button',{name:'Source',exact:true});
              if(await sourceButton.isVisible())await sourceButton.click();
              const text=await page.getByRole('region',{name:'Java source',exact:true}).textContent();
              assert(text.includes('public class Solution')&&text.includes('memo'));
            }
            assert.equal(await seek(page).inputValue(),'2');
            await safe(page,errors);
          }
          assert.equal(requests,beforeViews);
          assertAnswer(await switchRun(page,'canonical'),input);
          run=await switchRun(page,'memoization');
          await seek(page).fill('2');
          await page.waitForFunction(()=>new URL(location.href).searchParams.get('step')==='3');
          const committed=page.url();
          // Build an over-recursion draft under the wider memo limits, then prepare recursion.
          const rejected=maximum(c.id,'recursion');
          if(c.id==='grid-unique-paths')rejected.rows++;
          else(rejected.heights??rejected.nums).push(0);
          await edit(page,c.id,rejected);
          const beforePrepare=requests;
          await selector(page).selectOption('recursion');
          assert.equal(requests,beforePrepare);
          await assertDraft(page,c.id,rejected);
          const failed=await capture(page,()=>page.getByRole('button',{name:'Run input',exact:true}).click(),400);
          assert(failed.fieldErrors);
          assert.equal(failed.steps,undefined);
          const alert=page.getByRole('alert').filter({hasText:'This input could not run'});
          await alert.waitFor();
          assert(await alert.evaluate(e=>e===document.activeElement));
          assert.equal(page.url(),committed);
          assert.equal(await seek(page).inputValue(),'2');
          await assertDraft(page,c.id,rejected);
          // Reload the *successful* memo link: input first, then step; rejected input is not shared.
          run=await capture(page,()=>page.goto(committed));
          assertAnswer(run,input);
          assert.equal(await selector(page).inputValue(),'memoization');
          await page.waitForFunction(()=>document.querySelector('input[aria-label="Seek to step"]')?.value==='2');
        }
        await safe(page,errors);
        let screenshot=null;
        if(mode==='maximum'&&engine==='chromium'&&(width===320&&theme==='dark'||width===1366&&theme==='light')) {
          const filename=`${c.id}-${form}-${width}-${theme}.png`;
          await page.evaluate(()=>{
            const stage=document.querySelector('[data-testid="recursion-tree-stage"]');
            const rail=document.querySelector('nav[aria-label="Learning views"]');
            window.scrollTo({top:Math.max(0,scrollY+stage.getBoundingClientRect().top-rail.getBoundingClientRect().height-12),behavior:'instant'});
          });
          assert(await page.locator('[data-recursion-root]').evaluate(el=>{
            const root=el.getBoundingClientRect(),rail=document.querySelector('nav[aria-label="Learning views"]').getBoundingClientRect();
            return root.top>=rail.bottom&&root.bottom<=innerHeight;
          }),'Screenshot must show the root below the sticky rail, not behind it');
          await page.screenshot({path:path.join(out,filename)});
          screenshot=filename;
        }
        rows.push({problemId:c.id,width,height,theme,nativeZoom:zoom,initial:form,maximumCalls,screenshot,requests,
          shownApproach:run.approachId,sourceHash:createHash('sha256').update(run.code).digest('hex'),
          noOverflowOrErrors:true,allThreeExecuted:mode==='standard',inputAndStepRestored:mode==='standard',
          viewsDoNotExecute:mode==='standard',refusalPreservesRunAndDraft:mode==='standard',
          allCallsRenderedAndRootVisible:mode==='maximum',memoCellsRendered:mode==='maximum'&&form==='memoization'});
        console.log(`PASS ${c.id} ${mode} ${width}x${height} ${theme} ${engine} zoom${zoom}`);
        if(browser.closePage)await browser.closePage(page);else await page.close();
      }
    fs.writeFileSync(path.join(out,`${engine}-${mode}-zoom${zoom}.json`),JSON.stringify({generated:new Date().toISOString(),
      realBackend:true,buildMode:'production-preview',engine,servedBuild,rows,
      limits:'Automated repeated learner tasks only; not human/screen-reader/real-device/performance or all-DP acceptance.'},null,2));
  } finally {await browser.close();}
})().catch(error=>{console.error(error);process.exitCode=1;});
