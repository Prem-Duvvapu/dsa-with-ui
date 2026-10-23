// Real browser checks of reuse, zero caches, paired editors and refused draft retention.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {createHash}=require('node:crypto');
const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const {cases,maximum,oracle}=require('./capacity-family-cases.cjs');
const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180';
const pace=require('../evidence/pace-executions.cjs')();
async function draft(page,c,input,write=false){
  for(const [key,label] of Object.entries(c.arrayLabels)){
    const group=page.getByRole('group',{name:label,exact:true}),positions=group.getByRole('spinbutton',{name:/^Position \d+ value$/});
    if(write){
      while(await positions.count()<input[key].length)await group.getByRole('button',{name:'Add',exact:true}).click();
      while(await positions.count()>input[key].length)await group.getByRole('button',{name:`Remove position ${await positions.count()}`,exact:true}).click();
      for(let i=0;i<input[key].length;i++)await group.getByLabel(`Position ${i+1} value`,{exact:true}).fill(String(input[key][i]));
    }
    assert.deepEqual(await positions.evaluateAll(es=>es.map(e=>Number(e.value))),input[key]);
  }
  for(const [key,label] of Object.entries(c.scalarLabels??{})){
    const scalar=page.getByLabel(label,{exact:true});if(write)await scalar.fill(String(input[key]));
    assert.equal(await scalar.inputValue(),String(input[key]));
  }
}
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
      const requestedStep=label==='maximum'?4000:3;
      page.on('pageerror',e=>errors.push(e.message));
      page.on('request',r=>{if(new URL(r.url()).pathname.endsWith('/execute'))requests++;});
      const pending=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname.endsWith('/execute'));
      await page.goto(`${base}/problem/${c.id}?approach=${form}&input=${Buffer.from(JSON.stringify(input)).toString('base64url')}&step=${requestedStep}`);
      const response=await pending;assert.equal(response.status(),200);
      const raw=await response.json(),steps=decodeTrace(raw),final=steps.at(-1),seek=page.getByRole('slider',{name:'Seek to step',exact:true});
      assert(!raw.truncated);assert.deepEqual(raw.resolvedInput,input);assert.equal(final.variables.answer,String(oracle(c.id,input)));
      const outOfRange=requestedStep>steps.length;
      await page.waitForFunction(index=>document.querySelector('input[aria-label="Seek to step"]')?.value===String(index),outOfRange?0:requestedStep-1);
      if(outOfRange){
        await page.getByText(`This link pointed to step ${requestedStep}, but this run has ${steps.length} steps. Showing step 1.`,{exact:true}).waitFor();
        await page.waitForFunction(()=>!new URL(location.href).searchParams.has('step'));
      }
      const restoredStep=Number(await seek.inputValue())+1;
      const before=requests;
      await seek.fill(String(steps.length-1));
      if(final.dpTable){
        if(form==='memoization')await page.getByText('Memo table',{exact:true}).click();
        const table=page.locator('table.dp-table');await table.waitFor();
        const cells=final.dpTable.cells.flat();
        assert.deepEqual(await table.locator('td .dp-value').allTextContents(),cells.map(c=>c.value));
        const labels=final.dpTable.cells.flatMap((row,r)=>row.map((cell,col)=>`row ${final.dpTable.rowLabels[r]}, column ${final.dpTable.colLabels[col]}: ${cell.value} (${cell.state==='void'&&cell.value==='·'?'unknown':cell.state})`));
        assert.deepEqual(await table.locator('td').evaluateAll(es=>es.map(e=>e.getAttribute('aria-label'))),labels);
        if(form==='memoization'&&label==='zero-cache'){
          assert(steps.some(s=>s.variables.event==='cache-hit'&&s.variables.value==='0'));
          assert(cells.some(cell=>cell.value==='0'&&cell.state!=='void'));
          assert(cells.some(cell=>cell.value==='·'&&cell.state==='void'));
        }
      }
      for(const view of ['Code walkthrough','Analysis','Playground'])await page.getByRole('tab',{name:view,exact:true}).click();
      assert.equal(await seek.inputValue(),String(steps.length-1));
      await page.getByRole('button',{name:'Edit input',exact:true}).click();
      await draft(page,c,input);assert.equal(requests,before,'Inspection re-executed');
      const jointRefusal=form==='memoization'&&label==='zero-cache';let refusedRuns=0;
      if(jointRefusal){
        const committed=page.url();
        const selector=page.getByRole('combobox',{name:'Solution approach',exact:true});
        // Rod needs the wider canonical editor to author an11-price draft; preparing
        // a narrower approach must preserve it exactly, never clip it to the new cap.
        if(input.prices)await selector.selectOption('canonical');
        const rejected=input.prices?{prices:Array(11).fill(0)}:{weights:Array(8).fill(1),values:Array(8).fill(1),capacity:8};
        await draft(page,c,rejected,true);
        if(input.prices)await selector.selectOption('memoization');
        const refusals=[[rejected,input.prices?'prices':'weights'],...(input.weights?[
          [{weights:[1,2],values:[3],capacity:4},'values'],[{weights:[1],values:[3,4],capacity:4},'values']
        ]:[])];
        for(const [rejectedInput,field] of refusals){
          await draft(page,c,rejectedInput,true);
          const failed=page.waitForResponse(r=>r.request().method()==='POST'&&new URL(r.url()).pathname.endsWith('/execute'));
          await page.getByRole('button',{name:'Run input',exact:true}).click();
          const response=await failed;assert.equal(response.status(),400);const failure=await response.json();
          assert(failure.fieldErrors[field]);assert.equal(failure.steps,undefined);
          const alert=page.getByRole('alert').filter({hasText:'This input could not run'});await alert.waitFor();
          assert(await alert.evaluate(el=>el===document.activeElement));
          assert.equal(page.url(),committed);assert.equal(await seek.inputValue(),String(steps.length-1));
          await draft(page,c,rejectedInput);refusedRuns++;
          assert.equal(requests,before+refusedRuns);
        }
      }
      assert.deepEqual(errors,[]);assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
      rows.push({problemId:c.id,label,approachId:form,engine,width:320,height:568,theme:'dark',input,answer:final.variables.answer,
        sourceHash:createHash('sha256').update(raw.code).digest('hex'),passed:true,sharedStepHandledAndRestored:true,
        requestedStep,restoredStep,stepWasOutOfRange:outOfRange,outOfRangeNoticeAndLinkReset:outOfRange,
        exactDraftRetained:true,noUnsolicitedExecution:true,noOverflowOrErrors:true,actualTableValuesAndLabels:Boolean(final.dpTable),
        zeroCacheReuse:form==='memoization'&&label==='zero-cache',jointRefusalPreservesRunDraftStepAndLink:jointRefusal,
        mismatchedPairsBothDirections:jointRefusal&&Boolean(input.weights),refusedRuns});
      await page.close();console.log(`PASS capacity edge ${engine}/${c.id}/${label}/${form}`);
    }}finally{await browser.close();}
  }
  fs.mkdirSync(path.join(__dirname,'capacity-family'),{recursive:true});
  fs.writeFileSync(path.join(__dirname,'capacity-family','edge-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,buildMode:'production-preview',servedBuild,rows},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
