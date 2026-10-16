const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const {cases}=require('./linear-grid-cases.cjs');
const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180';
const pace=require('../evidence/pace-executions.cjs')();
(async()=>{
  const rows=[];
  const servedBuild=await require('../evidence/served-build-identity.cjs')(base);
  for(const engine of ['chromium','firefox']) {
    const browser=await types[engine].launch();
    try {
      for(const c of cases) for(const [width,theme] of [[390,'dark'],[1366,'light']]) {
        const page=await browser.newPage({viewport:{width,height:844},colorScheme:theme});
        await pace(page);
        await page.addInitScript(theme=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));},theme);
        let requests=0;const errors=[];
        page.on('request',r=>{if(new URL(r.url()).pathname.endsWith('/execute'))requests++;});
        page.on('pageerror',e=>errors.push(e.message));
        const input=Buffer.from(JSON.stringify(c.input)).toString('base64url');
        await page.goto(`${base}/problem/${c.id}?approach=memoization&input=${input}&step=3`);
        const main=page.getByRole('slider',{name:'Seek to step',exact:true});
        await page.waitForFunction(()=>document.querySelector('input[aria-label="Seek to step"]')?.value==='2');
        await page.getByRole('button',{name:'Edit input',exact:true}).click();
        const draft=c.id==='grid-unique-paths'?page.getByLabel('Rows',{exact:true}):page.getByLabel('Position 1 value',{exact:true});
        await draft.fill(c.id==='grid-unique-paths'?'2':'9');
        const committed=page.url(),before=requests;
        await page.getByRole('button',{name:'Compare approaches',exact:true}).click();
        assert.equal(requests,before,'Opening comparison executed');
        const panel=page.getByRole('region',{name:'Approach comparison',exact:true});
        await panel.getByLabel('Approach 1',{exact:true}).selectOption('memoization');
        await panel.getByLabel('Approach 2',{exact:true}).selectOption('recursion');
        const run=panel.getByRole('button',{name:'Run comparison',exact:true});
        await run.focus();await page.keyboard.press('Enter');
        await panel.getByText('Both complete runs recorded the same answer.',{exact:true}).waitFor();
        assert.equal(requests,before+2);
        for(const label of ['Memoization comparison','Recursion comparison']) {
          const side=panel.getByRole('region',{name:label,exact:true});
          // The answer comes from the committed input, not the different edited draft.
          assert.equal(await side.locator('dd').first().textContent(),c.answer);
        }
        const memo=panel.getByRole('slider',{name:'Memoization: step',exact:true});
        const recursion=panel.getByRole('slider',{name:'Recursion: step',exact:true});
        await memo.focus();await page.keyboard.press('ArrowRight');
        assert.equal(await memo.inputValue(),'2');assert.equal(await recursion.inputValue(),'1');
        await recursion.focus();await page.keyboard.press('ArrowRight');
        assert.equal(await recursion.inputValue(),'2');assert.equal(await memo.inputValue(),'2');
        assert.equal(await main.inputValue(),'2');assert.equal(page.url(),committed);
        assert.equal(await draft.inputValue(),c.id==='grid-unique-paths'?'2':'9');
        await page.getByRole('button',{name:'Hide approach comparison',exact:true}).click();
        assert.deepEqual(errors,[]);
        assert(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth));
        rows.push({problemId:c.id,engine,width,theme,passed:true,noUnsolicitedExecution:true,
          sameCommittedInput:true,nativeKeyboardActivation:true,independentSliders:true,mainDraftStepAndLinkRetained:true});
        await page.close();
        console.log(`PASS comparison ${c.id} ${engine} ${width}`);
      }
    } finally {await browser.close();}
  }
  fs.writeFileSync(path.join(__dirname,'linear-grid','comparison-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,servedBuild,rows},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
