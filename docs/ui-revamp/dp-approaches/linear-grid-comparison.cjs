const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const {family,cases,oracle}=require('./dp-audit-profile.cjs');
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
        let requests=0;const errors=[],responses=[];
        page.on('request',r=>{if(new URL(r.url()).pathname.endsWith('/execute'))requests++;});
        page.on('pageerror',e=>errors.push(e.message));
        page.on('response',r=>{if(new URL(r.url()).pathname.endsWith('/execute'))responses.push({url:r.url(),status:r.status()});});
        const input=Buffer.from(JSON.stringify(c.input)).toString('base64url');
        await page.goto(`${base}/problem/${c.id}?approach=memoization&input=${input}&step=3`);
        const main=page.getByRole('slider',{name:'Seek to step',exact:true});
        await page.waitForFunction(()=>document.querySelector('input[aria-label="Seek to step"]')?.value==='2');
        await page.getByRole('button',{name:'Edit input',exact:true}).click();
        const gridKey=['points','grid','matrix','triangle'].find(key=>Array.isArray(c.input[key]));
        const matrix=Boolean(gridKey);
        const [draftRow,draftCol]=c.draftCell??[0,0];
        const draft=c.draftField?page.getByRole('group',{name:c.arrayLabels[c.draftField],exact:true}).getByLabel('Position 1 value',{exact:true})
          :matrix?page.getByRole('button',{name:new RegExp(`^Cell row ${draftRow+1}, column ${draftCol+1}, value `)})
          :c.id==='grid-unique-paths'?page.getByLabel('Rows',{exact:true}):page.getByLabel('Position 1 value',{exact:true});
        if(matrix)await draft.click();else await draft.fill(c.id==='grid-unique-paths'?'2':'9');
        const draftValue=matrix?await draft.getAttribute('aria-label'):await draft.inputValue();
        const edited=structuredClone(c.input);
        if(c.draftField)edited[c.draftField][0]=Number(draftValue);
        else if(matrix)edited[gridKey][draftRow][draftCol]=Number(draftValue.match(/value (-?\d+)$/)[1]);
        else if(c.id==='grid-unique-paths')edited.rows=Number(draftValue);
        else(edited.heights??edited.nums)[0]=Number(draftValue);
        assert.notEqual(String(oracle(c.id,edited)),c.answer,'Dirty draft must change the independent answer to detect a comparison snapshot leak');
        const committed=page.url(),before=requests;
        await page.getByRole('button',{name:'Compare approaches',exact:true}).click();
        assert.equal(requests,before,'Opening comparison executed');
        const panel=page.getByRole('region',{name:'Approach comparison',exact:true});
        await panel.getByLabel('Approach 1',{exact:true}).selectOption('memoization');
        await panel.getByLabel('Approach 2',{exact:true}).selectOption('recursion');
        const run=panel.getByRole('button',{name:'Run comparison',exact:true});
        await run.focus();await page.keyboard.press('Enter');
        try{await panel.getByText('Both complete runs recorded the same answer.',{exact:true}).waitFor();}
        catch(error){
          console.error(JSON.stringify({problemId:c.id,engine,width,theme,requests,responses,errors,url:page.url(),
            focus:await page.evaluate(()=>document.activeElement?.outerHTML),panel:await panel.textContent()},null,2));
          throw error;
        }
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
        assert.equal(matrix?await draft.getAttribute('aria-label'):await draft.inputValue(),draftValue);
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
  fs.mkdirSync(path.join(__dirname,family),{recursive:true});
  fs.writeFileSync(path.join(__dirname,family,'comparison-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,servedBuild,rows},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
