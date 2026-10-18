// Read-only, bounded deployment check for the ALREADY MERGED #229 batch.
// curl uses this environment's working HTTPS transport; direct Node fetch timed out.
// Never use this report to claim the local Falling/Triangle package is deployed.
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const {execFileSync}=require('node:child_process');
const {createHash}=require('node:crypto');
const {cases,oracle}=require('./circular-activity-obstacles-cases.cjs');
const site='https://dsa-with-ui.vercel.app';
const local=process.env.BACKEND_URL||'http://127.0.0.1:8923';
function publicJson(route,input){
  const args=['--fail','--silent','--show-error','--connect-timeout','10','--max-time','30'];
  if(input!==undefined)args.push('--request','POST','--header','Content-Type: application/json','--data-binary',JSON.stringify(input));
  args.push(site+route);
  return JSON.parse(execFileSync('curl',args,{encoding:'utf8',maxBuffer:3_000_000}));
}
(async()=>{
  const {decodeTrace}=await import(path.resolve(__dirname,'../../../frontend/src/trace/decodeTrace.js'));
  const stats=publicJson('/api/problems/stats');assert.equal(stats.catalogued,431);assert.equal(stats.traced,431);
  assert.equal(stats.untraced,0);assert.deepEqual(stats.duplicateIds,{});assert.deepEqual(stats.orphanedTracerIds,[]);
  const rows=[];
  for(const c of cases)for(const form of ['recursion','memoization','canonical']){
    const route=`/api/problems/${c.id}?approach=${form}`;
    const response=await fetch(local+route,{signal:AbortSignal.timeout(10000)});assert.equal(response.status,200);
    const known=await response.json(),served=publicJson(route);
    for(const key of ['approachId','javaCode','inputSpec','dsType','complexity','defaultApproachId'])assert.deepEqual(served[key],known[key],`${c.id}/${form}/${key}`);
    const runs=[];
    for(const encoding of ['full','delta']){
      await new Promise(resolve=>setTimeout(resolve,1100));
      const run=publicJson(`/api/problems/${c.id}/execute?approach=${form}&encoding=${encoding}`,c.input);
      assert.equal(run.problemId,c.id);assert.equal(run.approachId,form);assert(!run.truncated);
      assert.deepEqual(run.resolvedInput,c.input);assert.equal(run.code,known.javaCode);
      assert.equal(run.dsType,known.dsType);assert.deepEqual(run.complexity,known.complexity);
      const steps=decodeTrace(run);assert.equal(steps.at(-1).variables.answer,String(oracle(c.id,c.input)));
      runs.push(steps);
    }
    const normalized=runs[0].map((step,i)=>Object.fromEntries([...new Set([...Object.keys(step),...Object.keys(runs[1][i]??{})])]
      .map(key=>[key,step[key]===undefined?null:step[key]])));
    assert.deepEqual(normalized,runs[1]);
    rows.push({problemId:c.id,approachId:form,input:c.input,answer:String(oracle(c.id,c.input)),
      sourceHash:createHash('sha256').update(known.javaCode).digest('hex'),detailAndIdentityMatch:true,
      independentAnswerMatches:true,fullDeltaEqual:true,complete:true});
    console.log(`PASS PUBLIC merged baseline ${c.id}/${form}`);
  }
  const report={generated:new Date().toISOString(),site,mergedBaseline:'da63085 (#229)',stats,rows,
    executions:rows.length*2,transport:'curl over HTTPS; no substitute or fallback responses',
    limits:'Nine default-input approach scenarios for the three already merged candidates only; not all inputs, browser/device acceptance, rollback, or deployment of the local Falling/Triangle batch.'};
  fs.writeFileSync(path.join(__dirname,'falling-triangle/merged-baseline-public.json'),JSON.stringify(report,null,2));
})().catch(error=>{console.error(error);process.exitCode=1;});
