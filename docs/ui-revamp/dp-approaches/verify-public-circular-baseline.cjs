// Read-only, bounded deployment check for explicitly ALREADY MERGED baselines.
// Default remains #229; PUBLIC_BASELINE_FAMILY=subset-family checks #231.
// curl uses this environment's working HTTPS transport; direct Node fetch timed out.
// Never use either report to claim the local capacity package is deployed.
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const {execFileSync}=require('node:child_process');
const {createHash}=require('node:crypto');
const family=process.env.PUBLIC_BASELINE_FAMILY||'circular-activity-obstacles';
const baselines={
  'circular-activity-obstacles':{commit:'da63085 (#229)',output:'falling-triangle/merged-baseline-public.json'},
  'subset-family':{commit:'435c362 (#231)',output:'capacity-family/merged-subset-public.json'}
};
if(!Object.hasOwn(baselines,family))throw Error('Unknown public baseline profile');
const baseline=baselines[family];
const {cases,oracle}=require(`./${family}-cases.cjs`);
const site='https://dsa-with-ui.vercel.app';
const local=process.env.BACKEND_URL||'http://127.0.0.1:8923';
function publicJson(route,input,expectedStatus=200){
  const args=['--silent','--show-error','--connect-timeout','10','--max-time','30','--write-out','\n%{http_code}'];
  if(input!==undefined)args.push('--request','POST','--header','Content-Type: application/json','--data-binary',JSON.stringify(input));
  args.push(site+route);
  const result=execFileSync('curl',args,{encoding:'utf8',maxBuffer:3_000_000}),split=result.lastIndexOf('\n');
  assert.equal(Number(result.slice(split+1)),expectedStatus,route);
  return JSON.parse(result.slice(0,split));
}
(async()=>{
  const {decodeTrace}=await import(path.resolve(__dirname,'../../../frontend/src/trace/decodeTrace.js'));
  const stats=publicJson('/api/problems/stats');assert.equal(stats.catalogued,431);assert.equal(stats.traced,431);
  assert.equal(stats.untraced,0);assert.deepEqual(stats.duplicateIds,{});assert.deepEqual(stats.orphanedTracerIds,[]);
  const rows=[],details=new Map(),refusals=[],oddRows=[];
  for(const c of cases)for(const form of ['recursion','memoization','canonical']){
    const route=`/api/problems/${c.id}?approach=${form}`;
    const response=await fetch(local+route,{signal:AbortSignal.timeout(10000)});assert.equal(response.status,200);
    const known=await response.json(),served=publicJson(route);
    assert.deepEqual(c.input,Object.fromEntries(known.inputSpec.fields.map(field=>[field.name,field.defaultValue])),`${c.id}/${form}: probe must use actual defaults`);
    details.set(`${c.id}/${form}`,known);
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
  if(family==='subset-family'){
    for(const c of cases){
      const input=c.id==='partition-equal-subset-sum'?{nums:[0,0,0,0,0,0,0,16]}:
        {nums:Array(8).fill(1),[c.id==='count-subsets-with-sum-k'?'k':'target']:8};
      await new Promise(resolve=>setTimeout(resolve,1100));
      const body=publicJson(`/api/problems/${c.id}/execute?approach=memoization&encoding=delta`,input,400);
      assert.equal(typeof body.fieldErrors?.nums,'string');assert.equal(body.steps,undefined);
      refusals.push({problemId:c.id,approachId:'memoization',input,jointCells:81,status:400,field:'nums',noSteps:true});
      console.log(`PASS PUBLIC joint81 refusal ${c.id}`);
    }
    const c=cases.find(c=>c.id==='partition-equal-subset-sum'),input={nums:[1,2]};
    for(const form of ['recursion','memoization']){
      const known=details.get(`${c.id}/${form}`),runs=[];
      for(const encoding of ['full','delta']){
        await new Promise(resolve=>setTimeout(resolve,1100));
        const raw=publicJson(`/api/problems/${c.id}/execute?approach=${form}&encoding=${encoding}`,input);
        assert.equal(raw.problemId,c.id);assert.equal(raw.approachId,form);assert.equal(raw.truncated,false);
        assert.deepEqual(raw.resolvedInput,input);assert.equal(raw.code,known.javaCode);assert.equal(raw.dsType,known.dsType);
        assert.deepEqual(raw.complexity,known.complexity);
        const steps=decodeTrace(raw),final=steps.at(-1);assert.equal(steps.length,1);
        assert.equal(final.variables.answer,String(oracle(c.id,input)));assert.equal(final.variables.calls,'0');
        assert.equal(final.variables.target,undefined);assert.deepEqual(final.callStack,[]);assert(final.dpTable==null);
        runs.push(steps);
      }
      const normalized=runs[0].map((step,i)=>Object.fromEntries([...new Set([...Object.keys(step),...Object.keys(runs[1][i]??{})])]
        .map(key=>[key,step[key]===undefined?null:step[key]])));
      assert.deepEqual(normalized,runs[1]);
      oddRows.push({problemId:c.id,approachId:form,input,answer:'false',calls:0,steps:1,noFabricatedRootOrMemo:true,fullDeltaEqual:true});
      console.log(`PASS PUBLIC odd-total no-call ${c.id}/${form}`);
    }
  }
  const report={generated:new Date().toISOString(),site,mergedBaseline:baseline.commit,stats,rows,refusals,oddRows,
    defaultExecutions:rows.length*2,executions:(rows.length+oddRows.length)*2,
    totalExecutionRequests:(rows.length+oddRows.length)*2+refusals.length,
    transport:'curl over HTTPS with explicit status checks; no substitute or fallback responses',
    limits:'Nine actual-default approach scenarios for the three selected already merged candidates, plus any explicitly listed refusals/parity cases. Not all inputs, browser/device acceptance, rollback, exact public binary/commit identity, or deployment of the local capacity package.'};
  fs.writeFileSync(path.join(__dirname,baseline.output),JSON.stringify(report,null,2));
})().catch(error=>{console.error(error);process.exitCode=1;});
