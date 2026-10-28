const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const types=require(process.env.PLAYWRIGHT_MODULE||'playwright');
const {packageSnapshot}=require('./api.cjs');
const rgb=value=>value.match(/[\d.]+/g).slice(0,3).map(Number);
const luminance=values=>values.map(v=>{v/=255;return v<=0.04045?v/12.92:((v+0.055)/1.055)**2.4;}).reduce((n,v,i)=>n+v*[0.2126,0.7152,0.0722][i],0);
const ratio=(a,b)=>{const x=luminance(rgb(a)),y=luminance(rgb(b));return (Math.max(x,y)+0.05)/(Math.min(x,y)+0.05);};
(async()=>{
  const base=process.env.FRONTEND_URL||'http://127.0.0.1:5180',out=path.resolve(process.env.PHONE_EVIDENCE_DIR||__dirname);
  const localPackage=packageSnapshot(),servedBuild=await require('../served-build-identity.cjs')(base),rows=[];
  for(const engine of ['chromium','firefox']){
    const browser=await types[engine].launch();
    try{for(const theme of ['dark','light']){
      const page=await browser.newPage({viewport:{width:320,height:568},colorScheme:theme,reducedMotion:'reduce'});
      await require('../pace-executions.cjs')()(page);
      await page.addInitScript(theme=>{localStorage.setItem('dsa-ui:seenWelcome','true');localStorage.setItem('dsa-ui:theme',JSON.stringify(theme));},theme);
      const input=Buffer.from(JSON.stringify({digits:'77777'})).toString('base64url');
      await page.goto(`${base}/problem/letter-combinations-phone?input=${input}&step=6`);
      await page.waitForFunction(()=>document.querySelector('input[aria-label="Seek to step"]')?.value==='5');
      const stage=page.getByTestId('recursion-tree-stage'),strip=stage.locator('..').locator(':scope > div').nth(1);
      const text=strip.locator(':scope > div > div > span:first-child').first();assert.equal(await text.textContent(),'p');
      const colors=await text.evaluate(el=>{
        const foreground=getComputedStyle(el).color;let node=el;
        while(node){const style=getComputedStyle(node),bg=style.backgroundColor;
          if(style.backgroundImage!=='none')throw Error('Layered background needs pixel measurement');
          if(bg!=='rgba(0, 0, 0, 0)'&&bg!=='transparent')return {foreground,background:bg};
          node=node.parentElement;
        }
        throw Error('No opaque background found');
      });
      const measured=ratio(colors.foreground,colors.background);rows.push({engine,theme,width:320,...colors,ratio:measured,passed:measured>=4.5});
      fs.writeFileSync(path.join(out,'contrast-results.json'),JSON.stringify({generated:new Date().toISOString(),complete:rows.length===4,servedBuild,localPackage,rows},null,2)+'\n');
      assert(measured>=4.5,`Path text contrast ${engine}/${theme}: ${measured.toFixed(2)}:1`);
      await page.close();console.log(`PASS path contrast ${engine}/${theme}: ${measured.toFixed(2)}:1`);
    }}finally{await browser.close();}
  }
  assert.deepEqual(packageSnapshot(),localPackage);
})().catch(error=>{console.error(error);process.exitCode=1;});
