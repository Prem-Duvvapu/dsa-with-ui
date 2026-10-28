// Real API proof for this continuous search. No tracing algorithm is copied into the oracle.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {createHash}=require('node:crypto');
const {pathToFileURL}=require('node:url');
const root=path.resolve(__dirname,'../../../..');
const id='minimize-max-distance-gas-station';
const cases=[
  {label:'default',input:{stations:[1,13,17,23],k:4}},
  {label:'alternate',input:{stations:[1,2,3,4,5],k:1}},
  {label:'wide-third',input:{stations:[0,1000],k:2}},
  {label:'integer-third',input:{stations:[0,999],k:2}},
  {label:'multiple-gaps',input:{stations:[0,100,500,1000],k:5}},
  {label:'half-cent',input:{stations:[0,1],k:39}},
  {label:'maximum',input:{stations:[...Array.from({length:14},(_,i)=>i),1000],k:50}},
  {label:'uniform-maximum',input:{stations:Array.from({length:15},(_,i)=>i),k:50}},
];

// Every optimum is one gap divided into p pieces, p<=k+1. Feasibility uses exact
// integer products/division, independent of the production floating binary search.
function oracle({stations,k}){
  const gaps=stations.slice(1).map((v,i)=>v-stations[i]);
  let numerator=Math.max(...gaps),denominator=1;
  for(const gap of gaps)for(let pieces=1;pieces<=k+1;pieces++){
    if(gap*denominator>=numerator*pieces)continue;
    const needed=gaps.reduce((sum,g)=>sum+Math.floor((g*pieces-1)/gap),0);
    if(needed<=k){numerator=gap;denominator=pieces;}
  }
  const rounded=Math.floor((2*numerator*100000+denominator)/(2*denominator));
  return {numerator,denominator,value:numerator/denominator,
    answer:`${Math.floor(rounded/100000)}.${String(rounded%100000).padStart(5,'0')}`};
}
function packageSnapshot(){
  const jar=path.join(root,'backend/target/dsa-backend-1.0.0.jar'),stat=fs.statSync(jar);
  return {packagedAt:new Date(stat.mtimeMs).toISOString(),sha256:createHash('sha256').update(fs.readFileSync(jar)).digest('hex')};
}
function sourceHash(code){return createHash('sha256').update(code).digest('hex');}
function assertComplexity(complexity){
  assert.equal(complexity.timeComplexity,'O(N * I)');
  assert.equal(complexity.spaceComplexity,'O(N)');
  assert.match(complexity.timeExplanation,/40/);
  assert.match(complexity.timeExplanation,/gap/i);
  assert.match(complexity.spaceExplanation,/gap/i);
}
function assertIdentity(run,detail,input){
  assert.equal(run.problemId,id);assert.equal(run.approachId,'canonical');
  assert.equal(run.dsType,detail.dsType);assert.equal(run.dsType,'SearchSpace');
  assert.equal(run.code,detail.javaCode);assert.deepEqual(run.complexity,detail.complexity);
  assert.deepEqual(run.resolvedInput,input);assertComplexity(run.complexity);
}
function assertTrace(run,steps,input){
  assert(!run.truncated,'Accepted bounded input must finish');
  assert.equal(run.stepCount,steps.length);
  assert.equal(steps.length,2+40*(input.stations.length+1));
  const reference=oracle(input),gaps=input.stations.slice(1).map((s,i)=>s-input.stations[i]);
  const lineCount=run.code.split('\n').length,anchors=run.anchors;
  assert(anchors&&Number.isInteger(anchors.mid)&&Number.isInteger(anchors.done));
  let probes=0,decisions=0,tallies=0,lastProbe=null;
  for(const step of steps){
    const vars=step.variables;
    assert.equal(vars.searchDomain,'continuous');
    for(const key of ['low','high','intervalWidth']){
      assert.equal(typeof vars[key],'string',`Missing raw ${key}`);
      assert(vars[key].trim()!==''&&Number.isFinite(Number(vars[key])),`Invalid ${key}`);
    }
    const low=Number(vars.low),high=Number(vars.high),width=Number(vars.intervalWidth);
    assert(low>=0&&high>=low);assert.equal(width,high-low);
    assert(Number.isInteger(step.activeLine)&&step.activeLine>0&&step.activeLine<=lineCount);
    assert.deepEqual(step.arrayState.map(cell=>cell.value),gaps);
    if(step.activeLine===anchors.mid){
      probes++;const mid=Number(vars.mid);assert(Number.isFinite(mid)&&mid>low&&mid<high);
      assert.equal(mid,(low+high)/2);lastProbe={low,high,mid};
    }
    if(step.activeLine===anchors.gapTally){
      assert(lastProbe);tallies++;
      assert.equal(Number(vars.mid),lastProbe.mid);
      const active=step.arrayState.map((cell,i)=>cell.state==='current'?i:-1).filter(i=>i>=0);
      assert.deepEqual(active,[(tallies-1)%gaps.length]);
    }
    if(step.activeLine===anchors.feasible||step.activeLine===anchors.infeasible){
      assert(lastProbe);decisions++;
      assert(!Object.hasOwn(vars,'mid'),'A completed decision is not an active midpoint probe');
      assert.equal(low,step.activeLine===anchors.feasible?lastProbe.low:lastProbe.mid);
      assert.equal(high,step.activeLine===anchors.feasible?lastProbe.mid:lastProbe.high);
      assert(width<lastProbe.high-lastProbe.low);
    }
  }
  assert.equal(probes,40);assert.equal(decisions,40);assert.equal(tallies,40*gaps.length);
  assert.equal(Number(steps[0].variables.low),0);assert.equal(Number(steps[0].variables.high),Math.max(...gaps));
  const final=steps.at(-1);assert.equal(final.activeLine,anchors.done);
  assert.equal(final.variables.answer,reference.answer);
  assert(!Object.hasOwn(final.variables,'mid'));
  assert(Number(final.variables.intervalWidth)<1e-9);
  assert(Math.abs(Number(final.variables.high)-reference.value)<1e-9);
  assert.deepEqual(final.callStack,[]);
  return {reference,final};
}
async function api(){
  const base=process.env.BACKEND_URL||'http://127.0.0.1:8923';
  const out=path.resolve(process.env.GAS_EVIDENCE_DIR||__dirname);
  const packaged=packageSnapshot();
  const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  async function request(route,input,expected=200){
    if(route.includes('/execute'))await new Promise(resolve=>setTimeout(resolve,1100));
    const response=await fetch(base+route,input===undefined?undefined:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(input)});
    const text=await response.text();assert.equal(response.status,expected,`${route}: ${text.slice(0,400)}`);
    return {body:JSON.parse(text),bytes:Buffer.byteLength(text)};
  }
  const stats=(await request('/api/problems/stats')).body;
  assert.equal(stats.catalogued,431);assert.equal(stats.traced,431);assert.equal(stats.untraced,0);
  assert.deepEqual(stats.duplicateIds,{});assert.deepEqual(stats.orphanedTracerIds,[]);
  const detail=(await request(`/api/problems/${id}`)).body;
  assert.equal(detail.approachId,'canonical');assertComplexity(detail.complexity);
  assert.deepEqual(detail.approaches.find(a=>a.id==='canonical').complexity,detail.complexity);
  assert.equal(detail.examples[0].output,'4.00000');
  const rows=[];
  for(const c of cases){
    const input=c.label==='default'?undefined:c.input;
    const full=await request(`/api/problems/${id}/execute?encoding=full`,input);
    const delta=await request(`/api/problems/${id}/execute?encoding=delta`,input);
    const fullSteps=decodeTrace(full.body),deltaSteps=decodeTrace(delta.body);
    const normalized=fullSteps.map((step,i)=>Object.fromEntries([...new Set([...Object.keys(step),...Object.keys(deltaSteps[i]??{})])]
      .map(key=>[key,step[key]===undefined?null:step[key]])));
    assert.deepEqual(normalized,deltaSteps,`${c.label}: full/delta mismatch`);
    for(const run of [full.body,delta.body])assertIdentity(run,detail,c.input);
    const {reference}=assertTrace(full.body,fullSteps,c.input);assertTrace(delta.body,deltaSteps,c.input);
    assert(full.bytes<2_000_000&&delta.bytes<2_000_000);
    rows.push({label:c.label,input:c.input,answer:reference.answer,exactRational:`${reference.numerator}/${reference.denominator}`,
      sourceHash:sourceHash(full.body.code),steps:fullSteps.length,fullBytes:full.bytes,deltaBytes:delta.bytes,
      finalLow:fullSteps.at(-1).variables.low,finalHigh:fullSteps.at(-1).variables.high,
      finalWidth:fullSteps.at(-1).variables.intervalWidth,independentAnswer:true,fullDeltaEqual:true,identityAndComplexityMatch:true,
      everySnapshotContinuousAndPaired:true,fortyActualHalvings:true});
    console.log(`PASS API ${c.label}: ${fullSteps.length} steps, ${full.bytes} full bytes`);
  }
  const refusals=[];
  for(const [label,input,field]of [['zero-k',{stations:[0,1000],k:0},'k'],['unsorted',{stations:[0,3,2],k:1},'stations']])
    for(const encoding of ['full','delta']){
      const result=await request(`/api/problems/${id}/execute?encoding=${encoding}`,input,400);
      assert.equal(typeof result.body.fieldErrors?.[field],'string');assert.equal(result.body.steps,undefined);
      refusals.push({label,input,field,encoding,expected400:true,noSteps:true});
    }
  assert.deepEqual(packageSnapshot(),packaged,'Packaged jar changed during the API sweep');
  fs.mkdirSync(out,{recursive:true});
  fs.writeFileSync(path.join(out,'api-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,
    complete:true,problemId:id,localPackage:packaged,stats,sourceHash:sourceHash(detail.javaCode),complexity:detail.complexity,
    scenarios:rows.length,executionRequests:rows.length*2+refusals.length,rows,refusals,
    limits:'Bounded authored inputs for this owner; not all-input, public rollout, human/device/screen-reader/performance or whole-project certification.'},null,2));
}
module.exports={root,id,cases,oracle,packageSnapshot,sourceHash,assertComplexity,assertIdentity,assertTrace};
if(require.main===module)api().catch(error=>{console.error(error);process.exitCode=1;});
