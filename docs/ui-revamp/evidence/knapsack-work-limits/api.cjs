// Real served contracts + the actual frontend sampler. Oracles do not use its work-limit helper.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {pathToFileURL}=require('node:url');
const {sourceHash,packageSnapshot,root}=require('../paired-input/api.cjs');
const owners=['knapsack-01','unbounded-knapsack'],forms=['canonical','memoization','recursion'];
const high=1-Number.EPSILON;
function rng(profile){
  let state=617,draw=0;
  if(profile==='high')return ()=>high;
  if(profile==='low-weights')return ()=>{draw++;return draw>=2&&draw<=9?0:high;};
  return ()=>{state=(Math.imul(state,1664525)+1013904223)>>>0;return state/2**32;};
}
function calls(weights,capacity,reuse,stop=Infinity){
  let total=0;
  function visit(n,left){if(total>=stop)return;total++;if(n===0||left===0)return;
    visit(n-1,left);if(weights[n-1]<=left)visit(reuse?n:n-1,left-weights[n-1]);}
  visit(weights.length,capacity);return total;
}
function work(rule,input){
  if(rule.kind==='CAPACITY_TABLE_CELLS')return (input[rule.arrayField].length+1)*(input[rule.scalarField]+1);
  assert.equal(rule.kind,'SKIP_TAKE_CALLS');return calls(input[rule.arrayField],input[rule.scalarField],rule.reuse,rule.limit+1);
}
function oracle(id,{weights,values,capacity}){
  assert.equal(weights.length,values.length);let best=0;
  if(id==='knapsack-01'){
    for(let mask=0;mask<2**weights.length;mask++){
      let weight=0,value=0;for(let i=0;i<weights.length;i++)if(mask&(1<<i)){weight+=weights[i];value+=values[i];}
      if(weight<=capacity)best=Math.max(best,value);
    }
    return best;
  }
  assert.equal(id,'unbounded-knapsack');
  // Integer quantity vectors with a fractional upper bound; no prefix/capacity DP.
  const items=weights.map((weight,i)=>({weight,value:values[i]})).sort((a,b)=>b.value/b.weight-a.value/a.weight);
  if(!items.length)return 0;best=Math.floor(capacity/items[0].weight)*items[0].value;
  function enumerate(index,left,value){
    best=Math.max(best,value);if(index===items.length)return;
    const item=items[index];if(value+left*item.value/item.weight<=best+1e-10)return;
    for(let quantity=Math.floor(left/item.weight);quantity>=0;quantity--)
      enumerate(index+1,left-quantity*item.weight,value+quantity*item.value);
  }
  enumerate(0,capacity,0);return best;
}
function assertSpec(id,form,spec){
  assert.deepEqual(spec.equalLengthGroups,[['weights','values']]);assert.equal(spec.workLimits.length,1);
  const rule=spec.workLimits[0],recursive=form==='recursion';
  assert.equal(rule.kind,recursive?'SKIP_TAKE_CALLS':'CAPACITY_TABLE_CELLS');assert.equal(rule.arrayField,'weights');assert.equal(rule.scalarField,'capacity');
  assert.equal(rule.limit,recursive?220:form==='canonical'?225:80);assert.equal(rule.reuse,recursive&&id==='unbounded-knapsack');
  assert.equal(rule.errorField,form==='canonical'?'capacity':'weights');assert.equal(typeof rule.message,'string');assert(rule.message.trim());
  const capacity=spec.fields.find(field=>field.name===rule.scalarField);assert.equal(capacity.type,'INT');
  assert.equal(capacity.constraints.min,1);assert.equal(capacity.constraints.max,30);return rule;
}
function assertInput(spec,input){
  assert.deepEqual(Object.keys(input).sort(),spec.fields.map(field=>field.name).sort());
  for(const field of spec.fields){const value=input[field.name],c=field.constraints;
    if(field.type==='INT_ARRAY'){assert(Array.isArray(value));assert(value.length>=c.minLength&&value.length<=c.maxLength);
      assert(value.every(v=>Number.isInteger(v)&&v>=c.minValue&&v<=c.maxValue));}
    else{assert.equal(field.type,'INT');assert(Number.isInteger(value)&&value>=c.min&&value<=c.max);}}
  for(const names of spec.equalLengthGroups)assert(names.every(name=>input[name].length===input[names[0]].length));
  for(const rule of spec.workLimits)assert(work(rule,input)<=rule.limit,'Actual generated draft exceeds declared work');
}
function assertRun(run,steps,detail,input){
  assert.equal(run.problemId,detail.id);assert.equal(run.approachId,detail.approachId);assert.equal(run.dsType,detail.dsType);
  assert.equal(run.code,detail.javaCode);assert.deepEqual(run.complexity,detail.complexity);assert.deepEqual(run.resolvedInput,input);
  assert(!run.truncated);assert(steps.length>0);assert.equal(run.stepCount,steps.length);
  const anchors=Object.values(run.anchors),lineCount=run.code.split('\n').length;
  for(const [i,step] of steps.entries()){
    assert.equal(step.stepNumber,i+1);assert(anchors.includes(step.activeLine));assert(step.activeLine>0&&step.activeLine<=lineCount);assert.equal(step.dsType,run.dsType);
  }
  const expected=oracle(run.problemId,input),actual=Number(steps.at(-1).variables.answer);assert.equal(actual,expected);
  if(run.approachId==='recursion')assert.equal(Number(steps.at(-1).variables.calls),calls(input.weights,input.capacity,run.problemId==='unbounded-knapsack'));
  return {expected,actual};
}
function boundary(id,form,accepted){
  if(form==='canonical')return {weights:Array(8).fill(20),values:Array(8).fill(100),capacity:accepted?24:25};
  if(form==='memoization')return accepted?
    {weights:id==='knapsack-01'?[1,3,1,1,5,1,2]:Array(7).fill(1),values:Array(7).fill(100),capacity:9}:
    {weights:Array(8).fill(1),values:Array(8).fill(100),capacity:8};
  const weights=id==='knapsack-01'?(accepted?[3,1,15,5,3,19,7,17]:[17,10,5,5,11,17,12,2]):
    accepted?[17,2,4,15]:[3,6,11,13,1,5];
  return {weights,values:Array(weights.length).fill(100),capacity:id==='knapsack-01'?(accepted?29:30):(accepted?26:12)};
}
async function main(){
  const base=process.env.BACKEND_URL||'http://127.0.0.1:8923',out=path.resolve(process.env.KNAPSACK_EVIDENCE_DIR||__dirname);
  const localPackage=packageSnapshot();
  const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  const {randomizeInput}=await import(pathToFileURL(path.join(root,'frontend/src/input/randomizeInput.js')).href);
  const samplerFile=path.join(root,'frontend/src/input/randomizeInput.js'),samplerHash=sourceHash(fs.readFileSync(samplerFile));
  let next=0,executionRequests=0;async function request(route,input,expected=200){
    if(route.includes('/execute')){executionRequests++;const due=Math.max(Date.now(),next);next=due+1100;
      if(due>Date.now())await new Promise(resolve=>setTimeout(resolve,due-Date.now()));}
    const response=await fetch(base+route,input===undefined?undefined:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(input)});
    const text=await response.text();assert.equal(response.status,expected,`${route}: ${text.slice(0,400)}`);return {body:JSON.parse(text),bytes:Buffer.byteLength(text)};
  }
  const rows=[],refusals=[],specifications=[],frontendMetadataControls=[];
  for(const id of owners)for(const form of forms){
    const detail=(await request(`/api/problems/${id}?approach=${form}`)).body;
    const spec=(await request(`/api/problems/${id}/input-spec?approach=${form}`)).body;
    assert.equal(detail.approachId,form);assert.deepEqual(detail.inputSpec,spec);const rule=assertSpec(id,form,spec);
    specifications.push({id,form,equalLengthGroups:spec.equalLengthGroups,workLimits:spec.workLimits,sourceHash:sourceHash(detail.javaCode)});
    for(const [label,workLimits] of [['unknown-kind',[{...rule,kind:'UNKNOWN_WORK'}]],
      ['duplicate-scalar-reference',[rule,{...rule}]],['wrong-scalar-type',[{...rule,scalarField:'values'}]]]){
      let draws=0;assert.throws(()=>randomizeInput({...spec,workLimits},()=>{draws++;return 0.5;}));
      assert.equal(draws,0,'Malformed work declarations must be refused before sampling');
      frontendMetadataControls.push({id,form,label,rejectedBeforeRng:true});
    }
    const scenarios=['seed','high','low-weights'].map(profile=>({label:`actual-generator-${profile}`,input:randomizeInput(spec,rng(profile))}));
    scenarios.push({label:'accepted-boundary',input:boundary(id,form,true)});
    for(const c of scenarios){
      assertInput(spec,c.input);
      if(c.label==='actual-generator-high')assert.deepEqual(c.input,{weights:Array(8).fill(20),values:Array(8).fill(100),capacity:form==='canonical'?24:form==='memoization'?7:30});
      if(c.label==='actual-generator-low-weights'){assert.deepEqual(c.input.weights,Array(8).fill(1));assert.deepEqual(c.input.values,Array(8).fill(100));}
      if(c.label==='accepted-boundary')assert.equal(work(rule,c.input),rule.limit);
      const route=`/api/problems/${id}/execute?approach=${form}`;
      const full=await request(`${route}&encoding=full`,c.input),delta=await request(`${route}&encoding=delta`,c.input);
      const fullSteps=decodeTrace(full.body),deltaSteps=decodeTrace(delta.body);
      const normalized=fullSteps.map((step,i)=>Object.fromEntries([...new Set([...Object.keys(step),...Object.keys(deltaSteps[i]??{})])]
        .map(key=>[key,step[key]===undefined?null:step[key]])));assert.deepEqual(normalized,deltaSteps);
      const answer=assertRun(full.body,fullSteps,detail,c.input);assertRun(delta.body,deltaSteps,detail,c.input);
      assert(full.bytes<2_000_000&&delta.bytes<2_000_000);
      rows.push({id,form,label:c.label,input:c.input,work:work(rule,c.input),limit:rule.limit,...answer,steps:fullSteps.length,
        fullBytes:full.bytes,deltaBytes:delta.bytes,sourceHash:sourceHash(full.body.code),actualSampler:c.label.startsWith('actual-generator'),
        independentWorkAndAnswer:true,pairAndIndexOrderPreserved:true,fullDeltaDecodedEqual:true,complete:true});
      console.log(`PASS work API ${id}/${form}/${c.label}`);
    }
    const input=boundary(id,form,false),measured=work(rule,input);assert.equal(measured,form==='canonical'?234:rule.limit+1);
    for(const encoding of ['full','delta']){
      const failure=await request(`/api/problems/${id}/execute?approach=${form}&encoding=${encoding}`,input,400);
      assert.equal(typeof failure.body.fieldErrors?.[rule.errorField],'string');assert.equal(failure.body.steps,undefined);
      refusals.push({id,form,input,encoding,work:measured,limit:rule.limit,field:rule.errorField,expected400:true,noSteps:true});
    }
  }
  assert.equal(rows.length,24);assert.equal(refusals.length,12);assert.equal(executionRequests,60);assert.deepEqual(packageSnapshot(),localPackage);
  assert.equal(sourceHash(fs.readFileSync(samplerFile)),samplerHash,'Frontend sampler changed during API proof');
  fs.mkdirSync(out,{recursive:true});fs.writeFileSync(path.join(out,'api-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,
    complete:true,localPackage,samplerHash,specifications,executionRequests,rows,refusals,frontendMetadataControls,
    limits:'Six declared knapsack specifications, three actual sampler profiles and exact boundaries. Source compilation is a separate gate. Not all-input, other joint budgets, device, screen-reader, performance, public rollout or whole-project acceptance.'},null,2));
}
module.exports={root,owners,forms,high,rng,calls,work,oracle,assertSpec,assertInput,assertRun,boundary,sourceHash,packageSnapshot};
if(require.main===module)main().catch(error=>{console.error(error);process.exitCode=1;});
