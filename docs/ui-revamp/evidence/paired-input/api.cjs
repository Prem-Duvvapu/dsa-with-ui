// Real HTTP verification of declared index pairing; joint budgets remain separate controls.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {createHash}=require('node:crypto');
const {pathToFileURL}=require('node:url');
const root=path.resolve(__dirname,'../../../..');
const ids=['knapsack-01','unbounded-knapsack','fractional-knapsack'];
const fixtures=[{label:'distinct-index-order',input:{weights:[3,1,2],values:[4,2,7],capacity:4}},
  {label:'reuse-distinction',input:{weights:[2],values:[5],capacity:4}}];
function oracle(id,{weights,values,capacity}){
  assert.equal(weights.length,values.length);let best=0;
  if(id==='unbounded-knapsack'){
    let vectors=[{weight:0,value:0}];
    for(let i=0;i<weights.length;i++)vectors=vectors.flatMap(v=>Array.from({length:Math.floor((capacity-v.weight)/weights[i])+1},(_,q)=>
      ({weight:v.weight+q*weights[i],value:v.value+q*values[i]})));
    return Math.max(...vectors.map(v=>v.value));
  }
  // 0/1 subsets; fractional-polytope vertices have at most one non-integral item.
  for(let mask=0;mask<2**weights.length;mask++){
    let weight=0,value=0;
    for(let i=0;i<weights.length;i++)if(mask&(1<<i)){weight+=weights[i];value+=values[i];}
    if(weight>capacity)continue;best=Math.max(best,value);
    if(id==='fractional-knapsack')for(let i=0;i<weights.length;i++)if(!(mask&(1<<i)))
      best=Math.max(best,value+Math.min(1,(capacity-weight)/weights[i])*values[i]);
  }
  return best;
}
function sourceHash(value){return createHash('sha256').update(value).digest('hex');}
function packageSnapshot(){
  const file=path.join(root,'backend/target/dsa-backend-1.0.0.jar'),stat=fs.statSync(file);
  return {packagedAt:new Date(stat.mtimeMs).toISOString(),sha256:sourceHash(fs.readFileSync(file))};
}
function assertSpec(id,spec){
  const expected=id==='fractional-knapsack'?[['values','weights']]:[['weights','values']];
  assert.deepEqual(spec.equalLengthGroups,expected);
  for(const name of ['weights','values'])assert.equal(spec.fields.find(field=>field.name===name)?.type,'INT_ARRAY');
}
function assertIdentity(run,detail,input){
  assert.equal(run.problemId,detail.id);assert.equal(run.approachId,detail.approachId);
  assert.equal(run.code,detail.javaCode);assert.equal(run.dsType,detail.dsType);
  assert.deepEqual(run.complexity,detail.complexity);assert.deepEqual(run.resolvedInput,input);
}
function assertTrace(run,steps,input){
  assert(!run.truncated);assert(steps.length>0);assert.equal(run.stepCount,steps.length);
  const lines=run.code.split('\n').length,anchors=Object.values(run.anchors);assert(anchors.length>0);
  for(const [i,step] of steps.entries()){
    assert.equal(step.stepNumber,i+1);assert(Number.isInteger(step.activeLine)&&step.activeLine>0&&step.activeLine<=lines);
    assert(anchors.includes(step.activeLine));assert.equal(step.dsType,run.dsType);
    if(run.problemId==='fractional-knapsack')for(const cell of step.arrayState??[])
      assert.equal(cell.value,input.values[cell.index],'Sorted display must retain the actual original item index');
  }
  const final=steps.at(-1),actual=Number(final.variables[run.problemId==='fractional-knapsack'?'totalValue':'answer']);
  const expected=oracle(run.problemId,input);assert(Number.isFinite(actual));
  assert(Math.abs(actual-expected)<(run.problemId==='fractional-knapsack'?0.005000001:1e-9),`Expected ${expected}, received ${actual}`);
  if(run.dsType==='DpTable'){
    const table=final.dpTable;assert(table);assert.equal(table.cells.length,input.weights.length+1);
    for(let n=0;n<=input.weights.length;n++)for(let capacity=0;capacity<=input.capacity;capacity++){
      const expectedCell=oracle(run.problemId,{weights:input.weights.slice(0,n),values:input.values.slice(0,n),capacity});
      assert.equal(Number(table.cells[n][capacity].value),expectedCell);
    }
  }
  return {expected,actual};
}
async function main(){
  const base=process.env.BACKEND_URL||'http://127.0.0.1:8923',out=path.resolve(process.env.PAIRED_EVIDENCE_DIR||__dirname);
  const localPackage=packageSnapshot();const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  let next=0;async function request(route,input,expected=200){
    if(route.includes('/execute')){const due=Math.max(Date.now(),next);next=due+1100;if(due>Date.now())await new Promise(resolve=>setTimeout(resolve,due-Date.now()));}
    const response=await fetch(base+route,input===undefined?undefined:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(input)});
    const text=await response.text();assert.equal(response.status,expected,`${route}: ${text.slice(0,400)}`);return {body:JSON.parse(text),bytes:Buffer.byteLength(text)};
  }
  const specs=[];
  for(const id of ids){
    const canonical=(await request(`/api/problems/${id}`)).body;assert.equal(canonical.approachId,'canonical');assertSpec(id,canonical.inputSpec);
    specs.push({id,approach:'canonical',detail:canonical});
    if(id!=='fractional-knapsack')for(const approach of ['recursion','memoization']){
      const detail=(await request(`/api/problems/${id}?approach=${approach}`)).body;
      const spec=(await request(`/api/problems/${id}/input-spec?approach=${approach}`)).body;
      assert.equal(detail.approachId,approach);assertSpec(id,spec);assert.deepEqual(detail.inputSpec,spec);
      assert.deepEqual(canonical.approaches.find(a=>a.id===approach).inputSpec,spec);specs.push({id,approach,detail});
    }
  }
  assert.equal(specs.length,7);const rows=[],refusals=[];
  for(const {id,approach,detail} of specs){
    const route=`/api/problems/${id}/execute?approach=${approach}`;
    for(const c of fixtures){
      const full=await request(`${route}&encoding=full`,c.input),delta=await request(`${route}&encoding=delta`,c.input);
      const fullSteps=decodeTrace(full.body),deltaSteps=decodeTrace(delta.body);
      const normalized=fullSteps.map((step,i)=>Object.fromEntries([...new Set([...Object.keys(step),...Object.keys(deltaSteps[i]??{})])]
        .map(key=>[key,step[key]===undefined?null:step[key]])));
      assert.deepEqual(normalized,deltaSteps);
      for(const run of [full.body,delta.body])assertIdentity(run,detail,c.input);
      const answer=assertTrace(full.body,fullSteps,c.input);assertTrace(delta.body,deltaSteps,c.input);
      assert(full.bytes<2_000_000&&delta.bytes<2_000_000);
      rows.push({id,approach,label:c.label,input:c.input,...answer,steps:fullSteps.length,fullBytes:full.bytes,deltaBytes:delta.bytes,
        sourceHash:sourceHash(full.body.code),declaredPairing:true,indexOrderPreserved:true,independentCapacityOracle:true,fullDeltaDecodedEqual:true});
      console.log(`PASS paired API ${id}/${approach}/${c.label}`);
    }
    for(const [label,input,encoding] of [['short-values',{weights:[1,2],values:[3],capacity:4},'full'],
      ['long-values',{weights:[1],values:[3,4],capacity:4},'delta']]){
      const failure=await request(`${route}&encoding=${encoding}`,input,400),field=id==='fractional-knapsack'?'weights':'values';
      assert.equal(typeof failure.body.fieldErrors?.[field],'string');assert.equal(failure.body.steps,undefined);
      refusals.push({id,approach,label,input,encoding,field,expected400:true,noSteps:true});
    }
    if(id!=='fractional-knapsack'){
      const input=approach==='canonical'?{weights:Array(8).fill(1),values:Array(8).fill(1),capacity:25}:
        approach==='memoization'?{weights:Array(8).fill(1),values:Array(8).fill(1),capacity:8}:
        id==='knapsack-01'?{weights:[17,10,5,5,11,17,12,2],values:Array(8).fill(1),capacity:30}:
          {weights:[3,6,11,13,1,5],values:Array(6).fill(1),capacity:12};
      const failure=await request(`${route}&encoding=full`,input,400),field=approach==='canonical'?'capacity':'weights';
      assert.equal(typeof failure.body.fieldErrors?.[field],'string');assert.equal(failure.body.steps,undefined);
      refusals.push({id,approach,label:'existing-joint-budget-control',input,encoding:'full',field,expected400:true,noSteps:true});
    }
  }
  assert.equal(rows.length,14);assert.equal(refusals.length,20);assert.deepEqual(packageSnapshot(),localPackage);
  fs.mkdirSync(out,{recursive:true});fs.writeFileSync(path.join(out,'api-results.json'),JSON.stringify({generated:new Date().toISOString(),
    realBackend:true,complete:true,localPackage,specifications:specs.map(({id,approach,detail})=>({id,approach,equalLengthGroups:detail.inputSpec.equalLengthGroups,
      sourceHash:sourceHash(detail.javaCode)})),executionRequests:rows.length*2+refusals.length,rows,refusals,
    limits:'Seven paired specifications and two small inputs each. Existing joint-budget refusals are controls, not a new budget generator. Not all-input, source compilation, public rollout, device, screen-reader, performance or whole-project certification.'},null,2));
}
module.exports={root,ids,fixtures,oracle,sourceHash,packageSnapshot,assertSpec,assertIdentity,assertTrace};
if(require.main===module)main().catch(error=>{console.error(error);process.exitCode=1;});
