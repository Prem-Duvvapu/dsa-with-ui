// Bounded real backend + production-preview proof. Run engines sequentially.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {root,id,oracle,sourceHash,packageSnapshot,assertDetail,assertIdentity,assertTrace,countNarration}=require('./api.cjs');
const seek=page=>page.getByRole('slider',{name:'Seek to step',exact:true});
async function noOverflow(page){assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'Page-wide horizontal overflow');}
async function viewportScreenshot(page,target,file){
  await target.waitFor();await target.evaluate(el=>el.scrollIntoView({block:'center',inline:'nearest',behavior:'instant'}));
  await page.evaluate(()=>new Promise(resolve=>requestAnimationFrame(()=>requestAnimationFrame(resolve))));
  const capture=await target.evaluate(el=>{const rect=el.getBoundingClientRect();return {scope:'viewport',
    scroll:{x:scrollX,y:scrollY},viewport:{width:innerWidth,height:innerHeight},
    targetRect:{top:rect.top,right:rect.right,bottom:rect.bottom,left:rect.left}};});
  await page.screenshot({path:file,fullPage:false});return capture;
}
async function checkGrid(page,expected){
  const stage=page.locator('[data-audit="stage"]');await stage.waitFor();assert.equal(await stage.count(),1);
  // GridCanvas exposes visible coordinate text, not gridcell roles/test IDs. Scope to
  // the stable stage and read each real coordinate's sibling value; no CSS coordinates.
  for(let r=0;r<expected.length;r++)for(let c=0;c<expected[0].length;c++){
    const coordinate=stage.getByText(`(${r},${c})`,{exact:true});await coordinate.waitFor();
    assert.equal(await coordinate.count(),1);
    assert.equal(await coordinate.evaluate(el=>el.previousElementSibling.textContent),String(expected[r][c]));
  }
  const alternative=stage.getByTestId('step-state-summary');await alternative.waitFor({state:'attached'});
  const text=await alternative.textContent();assert(text.includes('Grid, 2 rows by 2 columns.'));
  assert(text.includes('Row 0: 1, 1'));assert(text.includes('Row 1: 1, 2'));
  await noOverflow(page);
}
async function checkNarration(page){
  const expected=countNarration(1,1,2,5),narration=page.getByRole('status').filter({hasText:expected});
  await narration.waitFor();assert.equal(await narration.textContent(),expected);
  assert.match(await narration.textContent(),/contributes 2 new all-ones squares ending here/);
  assert.match(await narration.textContent(),/one of each side length from 1 through 2\. total=5\./);
}
async function checkSource(page,run,index){
  await page.getByRole('tab',{name:'Code walkthrough',exact:true}).click();
  const button=page.getByRole('button',{name:'Source',exact:true});if(await button.isVisible())await button.click();
  const region=page.getByRole('region',{name:'Java source',exact:true});await region.waitFor();
  const shown=await region.locator(':scope > div').evaluateAll(es=>es.map(el=>el.lastElementChild.textContent).join('\n'));
  assert.equal(shown.replace(/\r\n/g,'\n'),run.code.replace(/\r\n/g,'\n'));
  const active=region.locator('[data-active-line="true"]');await active.waitFor();assert.equal(await active.count(),1);
  assert.equal(await active.evaluate(el=>el.children[1].textContent),String(run.steps[index].activeLine));
  assert(await page.locator('[data-audit="stage"]').count()<=1);await checkNarration(page);await noOverflow(page);
}
async function main(){
  const engine=process.env.AUDIT_ENGINE||'chromium';assert(['chromium','firefox'].includes(engine));
  const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
  const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180',out=path.resolve(process.env.SQUARES_EVIDENCE_DIR||__dirname);
  const localPackage=packageSnapshot(),servedBuild=await require('../served-build-identity.cjs')(base);
  const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  const detailResponse=await fetch(`${base}/api/problems/${id}`);assert.equal(detailResponse.status,200);
  const detail=await detailResponse.json();assertDetail(detail);
  const pace=require('../pace-executions.cjs')(),browser=await types[engine].launch(),rows=[],screenshots=[];
  const input={grid:[[1,1],[1,1]]},reference=oracle(input.grid);assert.equal(reference.answer,5);fs.mkdirSync(out,{recursive:true});
  try{
    for(const width of [320,1366])for(const theme of ['light','dark']){
      const page=await browser.newPage({viewport:{width,height:width===320?568:768},colorScheme:theme,reducedMotion:'reduce'});
      page.setDefaultTimeout(25000);await pace(page);
      await page.addInitScript(theme=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));},theme);
      const errors=[];let executions=0;
      page.on('pageerror',error=>errors.push(error.message));
      page.on('request',request=>{if(new URL(request.url()).pathname===`/api/problems/${id}/execute`)executions++;});
      const pending=page.waitForResponse(response=>new URL(response.url()).pathname===`/api/problems/${id}/execute`&&response.request().method()==='POST');
      const shared=Buffer.from(JSON.stringify(input)).toString('base64url');await page.goto(`${base}/problem/${id}?input=${shared}`);
      const response=await pending;assert.equal(response.status(),200);const raw=await response.json(),run={...raw,steps:decodeTrace(raw)};
      assertIdentity(run,detail,input);assertTrace(run,run.steps,input);assert.equal(run.steps.length,7);
      const countIndex=run.steps.findIndex(step=>step.activeLine===run.anchors.count&&Number(step.variables.r)===1&&Number(step.variables.c)===1);
      assert.equal(countIndex,5);const countStep=run.steps[countIndex];assert.deepEqual(countStep.gridState,[[1,1],[1,2]]);
      assert.equal(Number(countStep.variables.total),5);await seek(page).waitFor();await seek(page).fill(String(countIndex));
      await checkNarration(page);await checkGrid(page,countStep.gridState);const originalExecutions=executions;
      const selected=width===320&&theme==='dark'||width===1366&&theme==='light',rowScreenshots=[];
      if(selected){
        const file=`${engine}-${width}-${theme}-new-squares.png`;
        const capture=await viewportScreenshot(page,page.getByRole('status').filter({hasText:countNarration(1,1,2,5)}),path.join(out,file));
        rowScreenshots.push(file);screenshots.push({file,engine,width,theme,view:'playground',step:countIndex+1,
          ...capture,
          expected:'Grid [[1,1],[1,2]]; two new ending squares with side lengths1 and2; total5.'});
      }
      await checkSource(page,run,countIndex);assert.equal(await seek(page).inputValue(),String(countIndex));
      await page.getByRole('tab',{name:'Analysis',exact:true}).click();
      const complexity=page.getByRole('region',{name:'Algorithm complexity',exact:true});await complexity.waitFor();
      await complexity.getByText('O(R * C)',{exact:true}).waitFor();await complexity.getByText('O(1)',{exact:true}).waitFor();
      const text=await complexity.textContent();assert(text.includes(detail.complexity.timeExplanation));assert(text.includes(detail.complexity.spaceExplanation));
      assert.match(text,/input conversion uses O\(R \* C\) storage/i);assert.match(text,/animation snapshots are excluded/i);
      assert.match(text,/scalar output uses O\(1\)/i);await checkNarration(page);await noOverflow(page);
      if(selected){
        const file=`${engine}-${width}-${theme}-complexity.png`;
        const capture=await viewportScreenshot(page,complexity.getByText(detail.complexity.spaceExplanation,{exact:true}),path.join(out,file));
        rowScreenshots.push(file);screenshots.push({file,engine,width,theme,view:'analysis',step:countIndex+1,
          ...capture,
          expected:'O(R * C) time; O(1) in-place auxiliary space; input-conversion storage and animation exclusions visible.'});
      }
      assert.equal(await seek(page).inputValue(),String(countIndex));await page.getByRole('tab',{name:'Playground',exact:true}).click();
      await checkNarration(page);await checkGrid(page,countStep.gridState);assert.equal(await seek(page).inputValue(),String(countIndex));
      assert.equal(executions,originalExecutions,'View changes re-executed the algorithm');assert.deepEqual(errors,[]);
      rows.push({engine,width,height:width===320?568:768,theme,input,answer:reference.answer,steps:run.steps.length,inspectedStep:countIndex+1,
        grid:countStep.gridState,narration:countStep.description,sourceHash:sourceHash(run.code),executions,screenshots:rowScreenshots,
        realInputAndDecodedGrid:true,twoNewSquaresSidesOneThroughTwo:true,accessibleGridAlternativeChecked:true,
        exactSourceAndHighlight:true,ownerComplexityAndExclusionsVisible:true,viewChangesKeepStepAndDoNotExecute:true,noPageOverflowOrErrors:true,passed:true});
      await page.close();console.log(`PASS squares ${engine}/${width}/${theme}`);
    }
    assert.equal(rows.length,4);assert.equal(screenshots.length,4);assert.deepEqual(packageSnapshot(),localPackage,'Jar changed during browser proof');
    fs.writeFileSync(path.join(out,`${engine}-results.json`),JSON.stringify({generated:new Date().toISOString(),realBackend:true,complete:true,
      buildMode:'production-preview',problemId:id,engine,browserVersion:browser.version(),servedBuild,localPackage,rows,screenshots,
      limits:'One authored input and exact last-cell contribution, two widths and both themes. Screenshots are saved for visual inspection, not automatically judged. Not all-input, device, native zoom, screen-reader, performance, public-deployment or whole-project certification.'},null,2));
  }finally{await browser.close();}
}
if(require.main===module)main().catch(error=>{console.error(error);process.exitCode=1;});
