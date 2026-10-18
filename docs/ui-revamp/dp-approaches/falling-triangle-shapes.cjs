// Bounded real-browser checks for the shapes a square default cannot exercise.
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const {cases,oracle}=require('./falling-triangle-cases.cjs');
const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180';
const out=path.join(__dirname,'falling-triangle');
const pace=require('../evidence/pace-executions.cjs')();
async function assertTableSummary(page,step){
  if(!step.dpTable?.cells?.length)return;
  const cells=step.dpTable.cells.flat();
  const excluded=cells.filter(cell=>cell.state==='void'&&cell.value==='#').length;
  const unknown=cells.filter(cell=>cell.state==='void'&&cell.value!=='#').length;
  await page.waitForFunction(({excluded,unknown})=>{
    const text=document.querySelector('[data-testid="step-state-summary"]')?.textContent;
    return text?.includes('DP table')&&(text.match(/excluded \(#\)/g)??[]).length===excluded
      &&(text.match(/: unknown/g)??[]).length===unknown;
  },{excluded,unknown});
}
(async()=>{
  const {decodeTrace}=await import(path.resolve(__dirname,'../../../frontend/src/trace/decodeTrace.js'));
  const servedBuild=await require('../evidence/served-build-identity.cjs')(base);
  const rows=[];
  for(const engine of ['chromium','firefox']){
    const browser=await types[engine].launch();
    try{
      for(const c of cases)for(const [shape,input] of Object.entries(c.extra))for(const form of ['recursion','memoization','canonical']){
        const page=await browser.newPage({viewport:{width:320,height:568},colorScheme:'dark'});
        page.setDefaultTimeout(20000);
        await pace(page);
        await page.addInitScript(()=>{
          localStorage.setItem('dsa-ui:seenWelcome','true');
          localStorage.setItem('dsa-ui:theme',JSON.stringify('dark'));
        });
        const errors=[],requests=[];let executions=0;
        page.on('pageerror',error=>errors.push(error.message));
        page.on('request',request=>{if(new URL(request.url()).pathname.endsWith('/execute')){executions++;requests.push({method:request.method(),url:request.url()});}});
        const response=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname.endsWith('/execute'));
        const shared=Buffer.from(JSON.stringify(input)).toString('base64url');
        await page.goto(`${base}/problem/${c.id}?approach=${form}&input=${shared}&step=3`);
        const actual=await response;assert.equal(actual.status(),200);
        const run=await actual.json(),steps=decodeTrace(run);
        assert(!run.truncated);assert.deepEqual(run.resolvedInput,input);
        assert.equal(steps.at(-1).variables.answer,String(oracle(c.id,input)));
        const seek=page.getByRole('slider',{name:'Seek to step',exact:true});
        await page.waitForFunction(()=>document.querySelector('input[aria-label="Seek to step"]')?.value==='2');
        // Restoration first loads the canonical default, then submits the link input.
        // Count post-restoration interactions separately; do not mistake that GET for a rerun.
        const restoredExecutions=executions;
        assert.equal(requests.filter(request=>request.method==='POST').length,1,JSON.stringify(requests));
        const committed=page.url();
        await assertTableSummary(page,steps[2]);
        await seek.fill(String(steps.length-1));
        await assertTableSummary(page,steps.at(-1));
        if(form==='memoization'&&c.field==='triangle'){
          await page.getByText('Memo table',{exact:true}).click();
          const table=page.locator('table.dp-table');await table.waitFor();
          const depth=Math.min(input.triangle.length,input.triangle[0].length);
          assert.equal(await table.locator('td').count(),depth*depth);
          assert.equal(await table.locator('td').filter({hasText:'#'}).count(),depth*(depth-1)/2);
          await page.getByText('Memo table',{exact:true}).locator('..').getByText(steps.at(-1).variables.memoDomain,{exact:true}).waitFor();
        }
        if(form!=='recursion'){
          if(form==='memoization'&&c.field!=='triangle')await page.getByText('Memo table',{exact:true}).click();
          const table=steps.at(-1).dpTable;
          const labels=table.cells.flatMap((row,r)=>row.map((cell,col)=>{
            const meaning=cell.state==='void'&&cell.value==='#'?'excluded':cell.state==='void'&&cell.value==='·'?'unknown':cell.state;
            return `row ${table.rowLabels[r]}, column ${table.colLabels[col]}: ${cell.value} (${meaning})`;
          }));
          assert.deepEqual(await page.locator('table.dp-table td').evaluateAll(elements=>elements.map(element=>element.getAttribute('aria-label'))),labels);
        }
        // Inspect the real editor, not only the response or a summary string.
        await page.getByRole('button',{name:'Edit input',exact:true}).click();
        const draft=await page.getByRole('button',{name:/^Cell row/}).evaluateAll(elements=>{
          const values=[];
          for(const element of elements){
            const [,r,c,v]=element.getAttribute('aria-label').match(/^Cell row (\d+), column (\d+), value (-?\d+)$/);
            (values[Number(r)-1]??=[])[Number(c)-1]=Number(v);
          }
          return values;
        });
        assert.deepEqual(draft,input[c.field],'The editor silently changed ignored input entries or its rectangular shape');
        assert.equal(executions,restoredExecutions,'Opening the editor or inspecting the memo re-executed the run');
        assert.equal(new URL(page.url()).searchParams.get('input'),new URL(committed).searchParams.get('input'));
        assert.equal(new URL(page.url()).searchParams.get('approach'),form==='canonical'?null:form);
        assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
        assert.deepEqual(errors,[]);
        rows.push({problemId:c.id,shape,approachId:form,engine,width:320,height:568,theme:'dark',
          input,answer:steps.at(-1).variables.answer,sourceHash:createHash('sha256').update(run.code).digest('hex'),
          initialExecutions:restoredExecutions,sharedStepRestored:true,exactSubmittedGridRetained:true,noUnsolicitedExecution:true,
          tableSummaryMatchesActualCells:form!=='recursion',
          tableCellLabelsMatchActualCells:form!=='recursion',
          memoDomainAndExcludedCells:form==='memoization'&&c.field==='triangle',noOverflowOrErrors:true,passed:true});
        await page.close();console.log(`PASS ${engine}/${c.id}/${shape}/${form}`);
      }
    }finally{await browser.close();}
  }
  fs.mkdirSync(out,{recursive:true});
  fs.writeFileSync(path.join(out,'shape-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,
    buildMode:'production-preview',servedBuild,rows},null,2));
})().catch(error=>{console.error(error);process.exitCode=1;});
