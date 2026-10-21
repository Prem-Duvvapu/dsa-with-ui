// Real production/browser checks of false, zero, indexed duplicate and odd-total states.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {createHash}=require('node:crypto');
const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const {cases,maximum,oracle}=require('./subset-family-cases.cjs');
const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180';
const pace=require('../evidence/pace-executions.cjs')();
(async()=>{
  const {decodeTrace}=await import(path.resolve(__dirname,'../../../frontend/src/trace/decodeTrace.js'));
  const servedBuild=await require('../evidence/served-build-identity.cjs')(base),rows=[];
  for(const engine of ['chromium','firefox']){
    const browser=await types[engine].launch();
    try{for(const c of cases)for(const form of ['recursion','memoization','canonical'])for(const [label,input] of Object.entries({
      ...c.extra,...(form==='canonical'?{maximum:maximum(c.id,form)}:{})
    })){
      const page=await browser.newPage({viewport:{width:320,height:568},colorScheme:'dark'});
      page.setDefaultTimeout(20000);await pace(page);
      await page.addInitScript(()=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify('dark'));});
      const errors=[];let requests=0;
      page.on('pageerror',e=>errors.push(e.message));
      page.on('request',r=>{if(new URL(r.url()).pathname.endsWith('/execute'))requests++;});
      const pending=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname.endsWith('/execute'));
      await page.goto(`${base}/problem/${c.id}?approach=${form}&input=${Buffer.from(JSON.stringify(input)).toString('base64url')}&step=3`);
      const response=await pending;assert.equal(response.status(),200);
      const raw=await response.json(),steps=decodeTrace(raw),final=steps.at(-1),seek=page.getByRole('slider',{name:'Seek to step',exact:true});
      assert(!raw.truncated);assert.deepEqual(raw.resolvedInput,input);assert.equal(final.variables.answer,String(oracle(c.id,input)));
      await page.waitForFunction(index=>document.querySelector('input[aria-label="Seek to step"]')?.value===String(index),Math.min(2,steps.length-1));
      const before=requests;
      await seek.fill(String(steps.length-1));
      if(final.dpTable){
        if(form==='memoization')await page.getByText('Memo table',{exact:true}).click();
        const table=page.locator('table.dp-table');await table.waitFor();
        const cells=final.dpTable.cells.flat();
        assert.deepEqual(await table.locator('td .dp-value').allTextContents(),cells.map(c=>c.value));
        const labels=final.dpTable.cells.flatMap((row,r)=>row.map((cell,col)=>`row ${final.dpTable.rowLabels[r]}, column ${final.dpTable.colLabels[col]}: ${cell.value} (${cell.state==='void'&&cell.value==='·'?'unknown':cell.state})`));
        assert.deepEqual(await table.locator('td').evaluateAll(es=>es.map(e=>e.getAttribute('aria-label'))),labels);
        if(form==='memoization'&&c.id!=='count-subsets-with-sum-k')await page.getByText(final.variables.memoDomain,{exact:true}).waitFor();
        if(form==='memoization'&&label==='false-cache'){
          const value=c.id==='count-subsets-with-sum-k'?'0':'false';
          assert(steps.some(s=>s.variables.event==='cache-hit'&&s.variables.value===value));
          assert(cells.some(cell=>cell.value===(value==='false'?'F':'0')&&cell.state!=='void'));
          assert(cells.some(cell=>cell.value==='·'&&cell.state==='void'));
        }
      }
      const odd=label==='odd'&&form!=='canonical';
      if(odd){
        assert.equal(steps.length,1);assert.equal(final.variables.calls,'0');assert.deepEqual(final.callStack,[]);
        assert.equal(await page.locator('[data-recursion-diagram]').count(),0);
        assert.equal(await page.getByText('Memo table',{exact:true}).count(),0);
        assert(!final.dpTable);assert.equal(final.variables.target,undefined);
      }
      for(const view of ['Code walkthrough','Analysis','Playground'])await page.getByRole('tab',{name:view,exact:true}).click();
      assert.equal(await seek.inputValue(),String(steps.length-1));
      await page.getByRole('button',{name:'Edit input',exact:true}).click();
      assert.deepEqual(await page.getByRole('spinbutton',{name:/^Position \d+ value$/}).evaluateAll(es=>es.map(e=>Number(e.value))),input.nums);
      for(const [key,name] of Object.entries(c.scalarLabels??{}))assert.equal(await page.getByLabel(name,{exact:true}).inputValue(),String(input[key]));
      assert.equal(requests,before,'Inspection re-executed');
      const jointRefusal=form==='memoization'&&label==='false-cache';
      if(jointRefusal){
        const rejected=c.id==='partition-equal-subset-sum'?{nums:[0,0,0,0,0,0,0,16]}:{nums:Array(8).fill(1),[c.id==='count-subsets-with-sum-k'?'k':'target']:8};
        const positions=page.getByRole('spinbutton',{name:/^Position \d+ value$/});
        while(await positions.count()<8)await page.getByRole('button',{name:'Add',exact:true}).click();
        for(let i=0;i<8;i++)await page.getByLabel(`Position ${i+1} value`,{exact:true}).fill(String(rejected.nums[i]));
        for(const [key,name] of Object.entries(c.scalarLabels??{}))await page.getByLabel(name,{exact:true}).fill(String(rejected[key]));
        const committed=page.url();
        const failed=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname.endsWith('/execute'));
        await page.getByRole('button',{name:'Run input',exact:true}).click();
        const response=await failed;assert.equal(response.status(),400);const failure=await response.json();
        assert(failure.fieldErrors.nums);assert.equal(failure.steps,undefined);
        const alert=page.getByRole('alert').filter({hasText:'This input could not run'});await alert.waitFor();
        assert(await alert.evaluate(el=>el===document.activeElement));
        assert.equal(page.url(),committed);assert.equal(await seek.inputValue(),String(steps.length-1));
        assert.deepEqual(await positions.evaluateAll(es=>es.map(e=>Number(e.value))),rejected.nums);
        for(const [key,name] of Object.entries(c.scalarLabels??{}))assert.equal(await page.getByLabel(name,{exact:true}).inputValue(),String(rejected[key]));
        assert.equal(requests,before+1);
      }
      assert.deepEqual(errors,[]);
      assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
      rows.push({problemId:c.id,label,approachId:form,engine,width:320,height:568,theme:'dark',input,answer:final.variables.answer,
        sourceHash:createHash('sha256').update(raw.code).digest('hex'),passed:true,sharedStepClampedAndRestored:true,
        exactDraftRetained:true,noUnsolicitedExecution:true,noOverflowOrErrors:true,oddNoFabricatedCalls:odd,
        actualTableValuesAndLabels:Boolean(final.dpTable),falseOrZeroCacheReuse:form==='memoization'&&label==='false-cache',
        jointRefusalPreservesRunDraftStepAndLink:jointRefusal});
      await page.close();console.log(`PASS edge ${engine}/${c.id}/${label}/${form}`);
    }}finally{await browser.close();}
  }
  fs.mkdirSync(path.join(__dirname,'subset-family'),{recursive:true});
  fs.writeFileSync(path.join(__dirname,'subset-family','edge-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,buildMode:'production-preview',servedBuild,rows},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
