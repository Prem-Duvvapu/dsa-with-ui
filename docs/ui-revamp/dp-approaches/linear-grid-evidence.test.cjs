const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const script = fs.readFileSync(path.join(__dirname,'refresh-inventory.cjs'),'utf8').split('(async () => {')[0];
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
function verified(docs) {
  const context={__dirname,process:{env:{}},require:name=>name==='fs'?{
    existsSync:file=>Object.hasOwn(docs,path.basename(file)),
    readFileSync:file=>JSON.stringify(docs[path.basename(file)])
  }:require(name)};
  vm.runInNewContext(script+'\nresult = linearGridCandidateUiVerified("test-problem");',context);
  return context.result;
}
test('only a complete per-problem same-build matrix can advance a DP candidate',()=>assert.equal(verified(documents()),true));
test('missing or incomplete manifests cannot certify a candidate',()=>{
  const missing=documents();delete missing['firefox-maximum-zoom1.json'];assert.equal(verified(missing),false);
  const partial=documents();partial['chromium-standard-zoom1.json'].rows.pop();assert.equal(verified(partial),false);
  const noComparison=documents();delete noComparison['comparison-results.json'];assert.equal(verified(noComparison),false);
});
test('another problem, duplicate axes and a different production build cannot be borrowed',()=>{
  for(const mutate of [
    doc=>{doc.rows[0].problemId='other-problem';},
    doc=>{doc.rows[0]={...doc.rows[1]};},
    doc=>{doc.servedBuild.assets=['/assets/other.js','/assets/other.css'];}
  ]) {const docs=documents();mutate(docs['firefox-maximum-zoom1.json']);assert.equal(verified(docs),false);}
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
