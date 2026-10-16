// Real backend rejection followed by an explicit, user-owned repair. No draft substitution.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180';
const pace=require('../evidence/pace-executions.cjs')();
const input={points:[[10,20],[30,40]]};
const repaired={points:[[10,20,0],[30,40,0]]};
const draft=page=>page.getByRole('button',{name:/^Cell row/}).evaluateAll(es=>{
  const rows=[];for(const e of es){const [,r,c,v]=e.getAttribute('aria-label').match(/^Cell row (\d+), column (\d+), value (-?\d+)$/);(rows[Number(r)-1]??=[])[Number(c)-1]=Number(v);}return rows;
});
(async()=>{
  const {decodeTrace}=await import(path.resolve(__dirname,'../../../frontend/src/trace/decodeTrace.js'));
  const servedBuild=await require('../evidence/served-build-identity.cjs')(base);
  const rows=[];
  for(const engine of ['chromium','firefox']){
    const browser=await types[engine].launch();
    try{for(const width of [320,1366])for(const theme of ['light','dark']){
      const page=await browser.newPage({viewport:{width,height:844},colorScheme:theme});
      await pace(page);
      await page.addInitScript(theme=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));},theme);
      const errors=[];page.on('pageerror',e=>errors.push(e.message));
      const rejected=page.waitForResponse(r=>r.status()===400&&new URL(r.url()).pathname.endsWith('/execute'));
      await page.goto(`${base}/problem/ninjas-training?approach=memoization&input=${Buffer.from(JSON.stringify(input)).toString('base64url')}&step=3`);
      const rejection=await(await rejected).json();
      assert.equal(rejection.fieldErrors?.points,'Needs at least 3 columns.');assert.equal(rejection.steps,undefined);
      await page.getByText(/The input in this link could not be run: the server rejected it/).waitFor();
      await page.waitForFunction(()=>!new URL(location.href).searchParams.has('input')&&!new URL(location.href).searchParams.has('approach'));
      const seek=page.getByRole('slider',{name:'Seek to step',exact:true});assert.equal(await seek.inputValue(),'0');
      await page.getByRole('button',{name:'Edit input',exact:true}).click();
      assert.deepEqual(await draft(page),input.points);
      await page.getByRole('combobox',{name:'Solution approach',exact:true}).selectOption('memoization');
      assert.deepEqual(await draft(page),input.points);
      assert(await page.getByRole('button',{name:'Remove a column',exact:true}).isDisabled());
      assert(await page.getByRole('button',{name:'Add a column',exact:true}).isEnabled());
      await page.getByRole('button',{name:'Add a column',exact:true}).click();
      assert.deepEqual(await draft(page),repaired.points);
      assert(await page.getByRole('button',{name:'Remove a column',exact:true}).isDisabled());
      assert(await page.getByRole('button',{name:'Add a column',exact:true}).isDisabled());
      const success=page.waitForResponse(r=>r.status()===200&&r.request().method()==='POST'&&new URL(r.url()).pathname.endsWith('/execute'));
      await page.getByRole('button',{name:'Run input',exact:true}).click();
      const raw=await(await success).json();assert(!raw.truncated);assert.equal(raw.approachId,'memoization');
      assert.deepEqual(raw.resolvedInput,repaired);assert.equal(decodeTrace(raw).at(-1).variables.answer,'50');
      await page.waitForFunction(()=>new URL(location.href).searchParams.get('approach')==='memoization');
      assert.deepEqual(JSON.parse(Buffer.from(new URL(page.url()).searchParams.get('input'),'base64url').toString()),repaired);
      assert(await page.locator('#stage-title').evaluate(el=>el===document.activeElement));
      assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));assert.deepEqual(errors,[]);
      rows.push({engine,width,theme,passed:true,rejectedDraftPreserved:true,defaultRunAndStepKept:true,
        noAutomaticRepair:true,explicitColumnRepair:true,minimumColumnsEnforced:true,answer:'50',successfulInputShared:true});
      await page.close();console.log(`PASS input recovery ${engine} ${width} ${theme}`);
    }}finally{await browser.close();}
  }
  const out=path.join(__dirname,'circular-activity-obstacles');fs.mkdirSync(out,{recursive:true});
  fs.writeFileSync(path.join(out,'input-recovery-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,servedBuild,rows},null,2));
})().catch(error=>{console.error(error);process.exitCode=1;});
