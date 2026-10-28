// Real HTTP proof. The oracle enumerates top-left/side positions, never the DP recurrence.
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const {createHash}=require('node:crypto');
const {pathToFileURL}=require('node:url');
const root=path.resolve(__dirname,'../../../..'),id='count-square-submatrices';
const cases=[
  {label:'default',grid:[[0,1,1,1],[1,1,1,1],[0,1,1,1]],answer:15},
  {label:'alternate',grid:[[1,0,1],[0,1,0],[1,0,1]],answer:5},
  {label:'two-by-two-ones',grid:[[1,1],[1,1]],answer:5},
  {label:'mixed',grid:[[1,0],[1,1]],answer:3},
  {label:'zero',grid:[[0,0],[0,0]],answer:0},
  {label:'maximum-ones',grid:Array.from({length:8},()=>Array(8).fill(1)),answer:204},
];
function oracle(grid){
  assert(Array.isArray(grid)&&grid.length&&grid[0].length);
  const rows=grid.length,cols=grid[0].length,squares=[],ending=Array.from({length:rows},()=>Array(cols).fill(0));
  assert(grid.every(row=>row.length===cols&&row.every(value=>value===0||value===1)));
  for(let r=0;r<rows;r++)for(let c=0;c<cols;c++)for(let side=1;r+side<=rows&&c+side<=cols;side++){
    let allOnes=true;
    for(let dr=0;dr<side;dr++)for(let dc=0;dc<side;dc++)if(grid[r+dr][c+dc]!==1)allOnes=false;
    if(allOnes){squares.push({top:r,left:c,side,bottom:r+side-1,right:c+side-1});ending[r+side-1][c+side-1]++;}
  }
  return {answer:squares.length,squares,ending};
}
function sourceHash(source){return createHash('sha256').update(source).digest('hex');}
function packageSnapshot(){
  const jar=path.join(root,'backend/target/dsa-backend-1.0.0.jar'),stat=fs.statSync(jar);
  return {packagedAt:new Date(stat.mtimeMs).toISOString(),sha256:sourceHash(fs.readFileSync(jar))};
}
function assertComplexity(complexity){
  assert.equal(complexity.timeComplexity,'O(R * C)');assert.equal(complexity.spaceComplexity,'O(1)');
  assert.match(complexity.spaceExplanation,/in place/i);assert.match(complexity.spaceExplanation,/input conversion/i);
  assert.match(complexity.spaceExplanation,/O\(R \* C\)/);assert.match(complexity.spaceExplanation,/animation snapshots are excluded/i);
  assert.match(complexity.spaceExplanation,/scalar output.*O\(1\)/i);
}
function assertDetail(detail){
  assert.equal(detail.dsType,'Matrix');assert.equal(detail.approachId,'canonical');assertComplexity(detail.complexity);
  const field=detail.inputSpec.fields.find(f=>f.name==='grid');assert(field);assert.deepEqual(field.defaultValue,cases[0].grid);
  assert.equal(field.constraints.maxRows,8);assert.equal(field.constraints.maxCols,8);
  assert.equal(field.constraints.minValue,0);assert.equal(field.constraints.maxValue,1);
  assert.equal(detail.inputSpec.maxSteps,5000);assert.equal(detail.inputSpec.maxBytes,2_000_000);
}
function assertIdentity(run,detail,input){
  assert.equal(run.problemId,id);assert.equal(run.approachId,'canonical');assert.equal(run.dsType,'Matrix');
  assert.equal(run.dsType,detail.dsType);assert.equal(run.code,detail.javaCode);assert.deepEqual(run.complexity,detail.complexity);
  assert.deepEqual(run.resolvedInput,input);assertComplexity(run.complexity);
}
function countNarration(r,c,contribution,total){
  return `(${r},${c}) contributes ${contribution} new all-ones square${contribution===1?'':'s'} ending here: one of each side length from 1 through ${contribution}. total=${total}.`;
}
function assertTrace(run,steps,{grid}){
  const reference=oracle(grid),anchors=run.anchors,working=grid.map(row=>[...row]);
  assert(!run.truncated,'Accepted input must finish');assert.equal(run.stepCount,steps.length);
  assert.deepEqual(Object.keys(anchors).sort(),['compute','count','done','init']);
  const lineCount=run.code.split('\n').length;
  for(const line of Object.values(anchors))assert(Number.isInteger(line)&&line>0&&line<=lineCount);
  for(const [index,step] of steps.entries()){
    assert.equal(step.stepNumber,index+1);assert(Object.values(anchors).includes(step.activeLine));
    assert.equal(step.dsType,'Matrix');
  }
  assert.equal(steps[0].activeLine,anchors.init);assert.deepEqual(steps[0].gridState,grid);assert.equal(Number(steps[0].variables.total),0);
  let index=1,total=0,countSteps=0,computeSteps=0;
  for(let r=0;r<grid.length;r++)for(let c=0;c<grid[0].length;c++){
    const contribution=reference.ending[r][c];
    if(grid[r][c]===1&&r>0&&c>0){
      const step=steps[index++];assert(step);computeSteps++;assert.equal(step.activeLine,anchors.compute);
      assert.equal(Number(step.variables.r),r);assert.equal(Number(step.variables.c),c);assert.equal(Number(step.variables.side),contribution);
      assert.deepEqual(step.gridState,working,'Compute snapshot must precede the current-cell rewrite');
    }
    working[r][c]=contribution;total+=contribution;
    if(contribution>0){
      const step=steps[index++];assert(step);countSteps++;assert.equal(step.activeLine,anchors.count);
      assert.equal(Number(step.variables.r),r);assert.equal(Number(step.variables.c),c);assert.equal(Number(step.variables.total),total);
      assert.deepEqual(step.gridState,working);assert.equal(step.description,countNarration(r,c,contribution,total));
      const sides=reference.squares.filter(s=>s.bottom===r&&s.right===c).map(s=>s.side).sort((a,b)=>a-b);
      assert.deepEqual(sides,Array.from({length:contribution},(_,i)=>i+1),'New squares must be one of each ending side length');
    }
  }
  assert.equal(index,steps.length-1);const final=steps[index];assert.equal(final.activeLine,anchors.done);
  assert.equal(Number(final.variables.total),reference.answer);assert.deepEqual(final.gridState,reference.ending);
  return {...reference,countSteps,computeSteps};
}
async function main(){
  const base=process.env.BACKEND_URL||'http://127.0.0.1:8923',out=path.resolve(process.env.SQUARES_EVIDENCE_DIR||__dirname);
  const localPackage=packageSnapshot();const {decodeTrace}=await import(pathToFileURL(path.join(root,'frontend/src/trace/decodeTrace.js')).href);
  let next=0;async function request(route,input,expected=200){
    if(route.includes('/execute')){const due=Math.max(Date.now(),next);next=due+1100;if(due>Date.now())await new Promise(resolve=>setTimeout(resolve,due-Date.now()));}
    const response=await fetch(base+route,input===undefined?undefined:{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(input)});
    const text=await response.text();assert.equal(response.status,expected,`${route}: ${text.slice(0,400)}`);
    return {body:JSON.parse(text),bytes:Buffer.byteLength(text)};
  }
  const detail=(await request(`/api/problems/${id}`)).body;assertDetail(detail);const rows=[];
  for(const c of cases){
    const input={grid:c.grid},requestInput=c.label==='default'?undefined:input;
    const full=await request(`/api/problems/${id}/execute?encoding=full`,requestInput);
    const delta=await request(`/api/problems/${id}/execute?encoding=delta`,requestInput);
    const fullSteps=decodeTrace(full.body),deltaSteps=decodeTrace(delta.body);
    const normalized=fullSteps.map((step,i)=>Object.fromEntries([...new Set([...Object.keys(step),...Object.keys(deltaSteps[i]??{})])]
      .map(key=>[key,step[key]===undefined?null:step[key]])));
    assert.deepEqual(normalized,deltaSteps,`${c.label}: full/delta decoded mismatch`);
    for(const run of [full.body,delta.body])assertIdentity(run,detail,input);
    const reference=assertTrace(full.body,fullSteps,input);assertTrace(delta.body,deltaSteps,input);assert.equal(reference.answer,c.answer);
    assert(full.bytes<2_000_000&&delta.bytes<2_000_000);
    rows.push({label:c.label,input,answer:reference.answer,steps:fullSteps.length,countSteps:reference.countSteps,computeSteps:reference.computeSteps,
      finalGrid:reference.ending,fullBytes:full.bytes,deltaBytes:delta.bytes,sourceHash:sourceHash(full.body.code),
      fullDeltaDecodedEqual:true,independentSquareEnumeration:true,everyNewSquareNarrationAndSnapshotChecked:true,identityAndMetadataMatch:true});
    console.log(`PASS API squares ${c.label}: ${reference.answer} squares, ${fullSteps.length} steps`);
  }
  const refusals=[];
  for(const encoding of ['full','delta']){
    const input={grid:[[1,2],[0,1]]},result=await request(`/api/problems/${id}/execute?encoding=${encoding}`,input,400);
    assert.equal(typeof result.body.fieldErrors?.grid,'string');assert.equal(result.body.steps,undefined);
    refusals.push({input,encoding,field:'grid',expected400:true,noSteps:true});
  }
  assert.deepEqual(packageSnapshot(),localPackage,'Packaged jar changed during API proof');fs.mkdirSync(out,{recursive:true});
  fs.writeFileSync(path.join(out,'api-results.json'),JSON.stringify({generated:new Date().toISOString(),realBackend:true,complete:true,problemId:id,
    localPackage,complexity:detail.complexity,sourceHash:sourceHash(detail.javaCode),scenarios:rows.length,executionRequests:rows.length*2+refusals.length,
    rows,refusals,limits:'Six authored inputs, independent top-left/side enumeration and original budgets. Not all-input, source compilation, device, screen-reader, performance, public deployment or whole-project certification.'},null,2));
}
module.exports={root,id,cases,oracle,sourceHash,packageSnapshot,assertComplexity,assertDetail,assertIdentity,assertTrace,countNarration};
if(require.main===module)main().catch(error=>{console.error(error);process.exitCode=1;});
