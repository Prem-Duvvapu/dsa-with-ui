// Real HTTP verification. The independent oracle expands products iteratively, not by backtracking.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {createHash}=require('node:crypto');
const {pathToFileURL}=require('node:url');
const root=path.resolve(__dirname,'../../../..'),id='letter-combinations-phone';
const cases=[{label:'default',digits:'23'},{label:'alternate',digits:'2'},
  {label:'maximum-five',digits:'77777'},{label:'safe-six',digits:'222222'},{label:'safe-six-late-four',digits:'222227'}];
const letters={2:'abc',3:'def',4:'ghi',5:'jkl',6:'mno',7:'pqrs',8:'tuv',9:'wxyz'};
function oracle(digits){
  assert.match(digits,/^[2-9]+$/);let results=[''];const prefixes=[];
  for(const digit of digits){results=results.flatMap(prefix=>Array.from(letters[digit],letter=>prefix+letter));prefixes.push(...results);}
  return {results,prefixes,stepCount:prefixes.length+results.length+1,javaList:`[${results.join(', ')}]`};
}
function sourceHash(source){return createHash('sha256').update(source).digest('hex');}
function packageSnapshot(){
  const jar=path.join(root,'backend/target/dsa-backend-1.0.0.jar'),stat=fs.statSync(jar);
  return {packagedAt:new Date(stat.mtimeMs).toISOString(),sha256:sourceHash(fs.readFileSync(jar))};
}
function assertComplexity(complexity){
  assert.equal(complexity.timeComplexity,'O(N * P)');assert.equal(complexity.spaceComplexity,'O(N)');
  assert.match(complexity.spaceExplanation,/O\(N \* P\)/);assert.match(complexity.spaceExplanation,/output/i);
}
function assertDetail(detail){
  assert.equal(detail.dsType,'RecursionTree');assert.equal(detail.approachId,'canonical');assertComplexity(detail.complexity);
  const field=detail.inputSpec.fields.find(f=>f.name==='digits');assert(field);
  assert.equal(field.label,'Digits');assert.equal(field.defaultValue,'23');
  assert.equal(field.constraints.minLength,1);assert.equal(field.constraints.maxLength,6);
  assert.equal(field.constraints.alphabet,'23456789');assert.equal(field.constraints.randomMaxLength,5);
  assert.equal(detail.inputSpec.maxSteps,5000);assert.equal(detail.inputSpec.maxBytes,2_000_000);
  assert.match(detail.javaCode,/class Solution\b/);assert.match(detail.javaCode,/StringBuilder/);
  assert.match(detail.javaCode,/KEYPAD/);assert.match(detail.javaCode,/path/);
}
function assertIdentity(run,detail,input){
  assert.equal(run.problemId,id);assert.equal(run.approachId,'canonical');assert.equal(run.dsType,'RecursionTree');
  assert.equal(run.dsType,detail.dsType);assert.equal(run.code,detail.javaCode);assert.deepEqual(run.complexity,detail.complexity);
  assert.deepEqual(run.resolvedInput,input);assertComplexity(run.complexity);
}
function assertTrace(run,steps,{digits}){
  const reference=oracle(digits),anchors=run.anchors;assert(!run.truncated,'Accepted input must finish under the existing budget');
  assert.equal(run.stepCount,steps.length);assert.equal(steps.length,reference.stepCount);
  assert.deepEqual(Object.keys(anchors).sort(),['capture','choose','done']);
  for(const line of Object.values(anchors))assert(Number.isInteger(line)&&line>0&&line<=run.code.split('\n').length);
  const captures=[],chosen=[];
  for(const [index,step] of steps.entries()){
    const vars=step.variables,chars=step.arrayState??[],pathValue=chars.map(c=>c.label).join('');
    assert.equal(step.stepNumber,index+1);assert(Object.values(anchors).includes(step.activeLine));
    assert.deepEqual(chars.map(c=>c.value),Array.from(pathValue,c=>c.codePointAt(0)));
    assert.deepEqual(chars.map(c=>c.index),Array.from({length:chars.length},(_,i)=>i));
    assert(chars.every(c=>c.state==='default'));
    const stack=step.callStack??[];
    assert.deepEqual(stack,Array.from({length:stack.length},(_,i)=>`backtrack(idx=${i})`));
    if(step.activeLine===anchors.choose){
      assert.deepEqual(Object.keys(vars).sort(),['digit','letter']);
      assert.equal(stack.length,pathValue.length);assert(pathValue.length>0&&pathValue.length<=digits.length);
      assert.equal(vars.digit,digits[pathValue.length-1]);assert.equal(vars.letter,pathValue.at(-1));
      for(let i=0;i<pathValue.length;i++)assert(letters[digits[i]].includes(pathValue[i]));
      chosen.push(pathValue);
    }else if(step.activeLine===anchors.capture){
      assert.deepEqual(Object.keys(vars).sort(),['combination','count']);captures.push(vars.combination);
      assert.equal(pathValue,vars.combination);assert.equal(Number(vars.count),captures.length);
      assert.equal(pathValue.length,digits.length);assert.equal(stack.length,digits.length+1);
    }else{
      assert.equal(index,steps.length-1);assert.deepEqual(Object.keys(vars).sort(),['combinations','results']);
      assert.equal(Number(vars.combinations),reference.results.length);assert.equal(vars.results,reference.javaList);
      assert.deepEqual(chars,[]);assert.deepEqual(stack,[]);
    }
  }
  assert.deepEqual(captures,reference.results,'Capture order/content differs from the iterative Cartesian product');
  assert.equal(chosen.length,reference.prefixes.length);assert.deepEqual([...chosen].sort(),[...reference.prefixes].sort());
  return reference;
}
async function main(){
  const base=process.env.BACKEND_URL||'http://127.0.0.1:8923',out=path.resolve(process.env.PHONE_EVIDENCE_DIR||__dirname);
  const localPackage=packageSnapshot();const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  let next=0;async function request(route,input,expected=200){
    if(route.includes('/execute')){const due=Math.max(Date.now(),next);next=due+1100;if(due>Date.now())await new Promise(resolve=>setTimeout(resolve,due-Date.now()));}
    const response=await fetch(base+route,input===undefined?undefined:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(input)});
    const text=await response.text();assert.equal(response.status,expected,`${route}: ${text.slice(0,500)}`);
    return {body:JSON.parse(text),bytes:Buffer.byteLength(text)};
  }
  const stats=(await request('/api/problems/stats')).body;assert.equal(stats.catalogued,431);assert.equal(stats.traced,431);assert.equal(stats.untraced,0);
  assert.deepEqual(stats.duplicateIds,{});assert.deepEqual(stats.orphanedTracerIds,[]);
  const detail=(await request(`/api/problems/${id}`)).body;assertDetail(detail);const rows=[];
  for(const c of cases){
    const input={digits:c.digits},requestInput=c.label==='default'?undefined:input;
    const full=await request(`/api/problems/${id}/execute?encoding=full`,requestInput);
    const delta=await request(`/api/problems/${id}/execute?encoding=delta`,requestInput);
    const fullSteps=decodeTrace(full.body),deltaSteps=decodeTrace(delta.body);
    const normalized=fullSteps.map((step,i)=>Object.fromEntries([...new Set([...Object.keys(step),...Object.keys(deltaSteps[i]??{})])]
      .map(key=>[key,step[key]===undefined?null:step[key]])));
    assert.deepEqual(normalized,deltaSteps,`${c.label}: full/delta decoded mismatch`);
    for(const run of [full.body,delta.body])assertIdentity(run,detail,input);
    const reference=assertTrace(full.body,fullSteps,input);assertTrace(delta.body,deltaSteps,input);
    assert(full.bytes<2_000_000&&delta.bytes<2_000_000);
    rows.push({label:c.label,input,combinations:reference.results.length,steps:fullSteps.length,
      fullBytes:full.bytes,deltaBytes:delta.bytes,sourceHash:sourceHash(full.body.code),resultsHash:sourceHash(reference.javaList),
      fullDeltaDecodedEqual:true,independentCartesianOracle:true,everyCaptureAndChoiceChecked:true,
      charsAndFramesDepthChecked:true,completeUnderOriginalBudgets:true,identityAndMetadataMatch:true});
    console.log(`PASS API phone ${c.label}: ${reference.results.length} combinations, ${fullSteps.length} steps`);
  }
  const refusals=[];
  for(const [label,input,field] of [['over-budget-first',{digits:'722222'},'digits'],['over-budget-maximum',{digits:'777777'},'digits'],
    ['invalid-digit',{digits:'201'},'digits'],['unknown-field',{digits:'23',unexpected:1},'unexpected']])for(const encoding of ['full','delta']){
    const result=await request(`/api/problems/${id}/execute?encoding=${encoding}`,input,400);
    assert.equal(typeof result.body.fieldErrors?.[field],'string');assert.equal(result.body.steps,undefined);
    refusals.push({label,input,field,encoding,expected400:true,noSteps:true});
  }
  assert.deepEqual(packageSnapshot(),localPackage,'Packaged jar changed during API proof');fs.mkdirSync(out,{recursive:true});
  fs.writeFileSync(path.join(out,'api-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,complete:true,
    problemId:id,localPackage,stats,complexity:detail.complexity,sourceHash:sourceHash(detail.javaCode),scenarios:rows.length,
    executionRequests:rows.length*2+refusals.length,rows,refusals,
    limits:'Bounded authored cases. Source compilation/parity is a separate backend-test gate. Not all-input, public rollout, device, screen-reader, performance or whole-project certification.'},null,2));
}
module.exports={root,id,cases,oracle,sourceHash,packageSnapshot,assertDetail,assertIdentity,assertTrace};
if(require.main===module)main().catch(error=>{console.error(error);process.exitCode=1;});
