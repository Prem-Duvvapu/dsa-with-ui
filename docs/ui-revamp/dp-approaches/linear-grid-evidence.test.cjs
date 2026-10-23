const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = process.env.DP_INVENTORY_BASELINE === '1'
  ? require('node:child_process').execFileSync('git',['show','HEAD:docs/ui-revamp/dp-approaches/refresh-inventory.cjs'],{cwd:__dirname,encoding:'utf8'})
  : fs.readFileSync(path.join(__dirname,'refresh-inventory.cjs'),'utf8');
const script = source.split('(async () => {')[0];
function documents() {
  const docs={'comparison-results.json':{realBackend:true,servedBuild:{assets:['/assets/real.js','/assets/real.css']},
    rows:['chromium','firefox'].flatMap(engine=>[[390,'dark'],[1366,'light']].map(([width,theme])=>({
      problemId:'test-problem',engine,width,theme,passed:true,noUnsolicitedExecution:true,
      sameCommittedInput:true,nativeKeyboardActivation:true,independentSliders:true,mainDraftStepAndLinkRetained:true
    })))}};
  for(const [name,engine,zoom,sizes] of [
    ['chromium-standard-zoom1','chromium',1,[[320,568],[390,844],[768,1024],[1366,768],[1440,900]]],
    ['firefox-standard-zoom1','firefox',1,[[390,844],[1366,768]]],
    ['chromium-standard-zoom2','chromium',2,[[640,1136],[1366,768]]],
    ['chromium-maximum-zoom1','chromium',1,[[320,568],[1366,768]]],
    ['firefox-maximum-zoom1','firefox',1,[[320,568],[1366,768]]]
  ]) docs[name+'.json']={realBackend:true,buildMode:'production-preview',engine,
    servedBuild:{assets:['/assets/real.js','/assets/real.css']},
    rows:sizes.flatMap(([width,height])=>['light','dark'].flatMap(theme=>(name.includes('maximum')?['recursion','memoization']:['recursion']).map(initial=>({
      problemId:'test-problem',width,height,theme,initial,nativeZoom:zoom,noOverflowOrErrors:true,
      allCallsRenderedAndRootVisible:true,memoCellsRendered:true,allThreeExecuted:true,
      inputAndStepRestored:true,viewsDoNotExecute:true,refusalPreservesRunAndDraft:true
    }))))};
  return docs;
}
function verified(docs, folder='linear-grid', problemId='test-problem') {
  const exists=file=>path.dirname(file)===path.join(__dirname,folder)&&Object.hasOwn(docs,path.basename(file));
  const context={__dirname,process:{env:{}},require:name=>name==='fs'?{
    existsSync:exists,
    readFileSync:file=>{assert(exists(file),'Wrong evidence folder');return JSON.stringify(docs[path.basename(file)]);}
  }:require(name)};
  vm.runInNewContext(script+`\nresult = linearGridCandidateUiVerified(${JSON.stringify(problemId)}, ${JSON.stringify(folder)});`,context);
  return context.result;
}
test('only a complete per-problem same-build matrix can advance a DP candidate',()=>assert.equal(verified(documents()),true));
test('missing or incomplete manifests cannot certify a candidate',()=>{
  const missing=documents();delete missing['firefox-maximum-zoom1.json'];assert.equal(verified(missing),false);
  const partial=documents();partial['chromium-standard-zoom1.json'].rows.pop();assert.equal(verified(partial),false);
  const noComparison=documents();delete noComparison['comparison-results.json'];assert.equal(verified(noComparison),false);
});
test('circular/activity/obstacles use the same complete per-problem gate',()=>{
  const docs=documents();
  for(const doc of Object.values(docs))for(const row of doc.rows){row.problemId='house-robber-2';row.rootCount=2;row.allStateCoordinatesVisible=true;}
  const check=()=>verified(docs,'circular-activity-obstacles','house-robber-2');
  assert.equal(check(),true);
  docs['firefox-maximum-zoom1.json'].rows[0].rootCount=1;assert.equal(check(),false);
  docs['firefox-maximum-zoom1.json'].rows[0].rootCount=2;
  docs['firefox-maximum-zoom1.json'].rows[0].allStateCoordinatesVisible=false;assert.equal(check(),false);
  docs['firefox-maximum-zoom1.json'].rows[0].allStateCoordinatesVisible=true;
  docs['firefox-maximum-zoom1.json'].rows.pop();assert.equal(check(),false);
});
test('independent circular, schedule and obstacle references include zero and endpoint controls',()=>{
  const {cases,oracle}=require('./circular-activity-obstacles-cases.cjs');
  for(const c of cases){assert.equal(String(oracle(c.id,c.input)),c.answer);assert.equal(oracle(c.id,c.zero),0);}
  assert.equal(oracle('house-robber-2',{nums:[2,7]}),7);
  assert.equal(oracle('house-robber-2',{nums:[9,1,1,9]}),10);
  assert.equal(oracle('ninjas-training',{points:[[1,0,0],[1,0,0]]}),1);
  assert.equal(oracle('unique-paths-2',{grid:[[0]]}),1);
  assert.equal(oracle('unique-paths-2',{grid:[[0,0],[0,1]]}),0);
  assert.throws(()=>oracle('unwritten-problem',{}),/No independent reference/);
});
test('an unknown evidence profile is refused instead of borrowing another family',()=>{
  const result=require('node:child_process').spawnSync(process.execPath,['-e','require("./dp-audit-profile.cjs")'],
    {cwd:__dirname,env:{...process.env,DP_AUDIT_FAMILY:'unknown-family'},encoding:'utf8'});
  assert.notEqual(result.status,0);assert.match(result.stderr,/Unknown DP evidence profile/);
});
test('authored matrix comparison edits materially change the independent answer',()=>{
  for(const family of ['circular-activity-obstacles','falling-triangle']){
  const {cases,oracle}=require(`./${family}-cases.cjs`);
  for(const c of cases.filter(c=>c.input.points||c.input.grid||c.field)){
    const edited=structuredClone(c.input),key=c.field??(c.input.points?'points':'grid');
    const [row,col]=c.draftCell??[0,0];edited[key][row][col]++;
    assert.notEqual(String(oracle(c.id,edited)),c.answer);
  }
  }
});
test('signed path references preserve rectangular depth, negatives, zero and default answers',()=>{
  const {cases,oracle,maximum}=require('./falling-triangle-cases.cjs');
  for(const c of cases){assert.equal(String(oracle(c.id,c.input)),c.answer);assert.equal(oracle(c.id,c.zero),0);assert.equal(oracle(c.id,c.negative),-4);}
  assert.equal(oracle(cases[0].id,maximum(cases[0].id,'recursion')),-400);
  assert.equal(oracle(cases[1].id,maximum(cases[1].id,'recursion')),-700);
  for(const input of Object.values(cases[1].extra))assert.equal(oracle(cases[1].id,input),5);
  assert.throws(()=>oracle('unwritten',{}),/No independent reference/);
});
test('falling-path evidence requires every real endpoint root for each advertised size',()=>{
  const docs=documents();
  for(const doc of Object.values(docs))for(const row of doc.rows){row.problemId='minimum-falling-path-sum';row.rootCount=row.initial==='memoization'?8:6;row.allStateCoordinatesVisible=true;}
  const check=()=>verified(docs,'falling-triangle','minimum-falling-path-sum');
  assert.equal(check(),true);
  docs['firefox-maximum-zoom1.json'].rows[0].rootCount=1;assert.equal(check(),false);
});
test('another problem, duplicate axes and a different production build cannot be borrowed',()=>{
  for(const mutate of [
    doc=>{doc.rows[0].problemId='other-problem';},
    doc=>{doc.rows[0]={...doc.rows[1]};},
    doc=>{doc.servedBuild.assets=['/assets/other.js','/assets/other.css'];}
  ]) {const docs=documents();mutate(docs['firefox-maximum-zoom1.json']);assert.equal(verified(docs),false);}
});
test('subset evidence requires actual roots and visible state coordinates',()=>{
  const docs=documents();
  for(const doc of Object.values(docs))for(const row of doc.rows){row.problemId='subset-sum-equal-target';row.rootCount=1;row.allStateCoordinatesVisible=true;}
  const check=()=>verified(docs,'subset-family','subset-sum-equal-target');
  assert.equal(check(),true);
  docs['firefox-maximum-zoom1.json'].rows[0].rootCount=0;assert.equal(check(),false);
  docs['firefox-maximum-zoom1.json'].rows[0].rootCount=1;
  docs['firefox-maximum-zoom1.json'].rows[0].allStateCoordinatesVisible=false;assert.equal(check(),false);
});
test('capacity evidence cannot certify absent roots or truncated state labels',()=>{
  for(const id of ['knapsack-01','unbounded-knapsack','rod-cutting-problem']){
    const docs=documents();
    for(const doc of Object.values(docs))for(const row of doc.rows){row.problemId=id;row.rootCount=1;row.allStateCoordinatesVisible=true;}
    const check=()=>verified(docs,'capacity-family',id);
    assert.equal(check(),true);
    docs['firefox-maximum-zoom1.json'].rows[0].rootCount=0;assert.equal(check(),false);
    docs['firefox-maximum-zoom1.json'].rows[0].rootCount=1;
    docs['firefox-maximum-zoom1.json'].rows[0].allStateCoordinatesVisible=false;assert.equal(check(),false);
  }
});
test('incorrect zoom, missing proof and an incorrect engine are refused',()=>{
  for(const mutate of [
    doc=>{doc.rows[0].nativeZoom=1;},
    doc=>{doc.rows[0].refusalPreservesRunAndDraft=false;},
    doc=>{doc.engine='firefox';}
  ]) {const docs=documents();mutate(docs['chromium-standard-zoom2.json']);assert.equal(verified(docs),false);}
});
test('independent references match authored examples and zero/base/boundary controls',()=>{
  const {cases,oracle}=require('./linear-grid-cases.cjs');
  for(const c of cases)assert.equal(String(oracle(c.id,c.input)),c.answer);
  assert.equal(oracle('grid-unique-paths',{rows:1,cols:8}),1);
  assert.equal(oracle('grid-unique-paths',{rows:8,cols:8}),3432);
  assert.equal(oracle('max-sum-non-adjacent',{nums:[0,0,0]}),0);
  assert.equal(oracle('max-sum-non-adjacent',{nums:[5,1,5]}),10);
  assert.equal(oracle('frog-jump-k-distance',{heights:[0,999,0],k:2}),0);
  assert.equal(oracle('frog-jump-k-distance',{heights:[0,999,0],k:1}),1998);
});
