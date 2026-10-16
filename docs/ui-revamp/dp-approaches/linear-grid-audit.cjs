// Reconcile completed manifests, actual current served source and the production bundle.
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const {family,cases,maximum,oracle}=require('./dp-audit-profile.cjs');
const out=path.join(__dirname,family);
const api=process.env.BACKEND_URL||'http://127.0.0.1:8923';
const read=name=>JSON.parse(fs.readFileSync(path.join(out,name),'utf8'));
(async()=>{
  const transport=read('api-results.json');
  const scenarios=cases.reduce((sum,c)=>sum+6+(c.zero?1:0),0);
  assert.equal(transport.rows.length,scenarios);
  const expectedKeys=cases.flatMap(c=>['recursion','memoization','canonical'].flatMap(form=>
    ['default','maximum',...(c.zero&&form==='memoization'?['zero']:[])].map(label=>`${c.id}/${form}/${label}`)));
  assert.deepEqual(transport.rows.map(row=>`${row.problemId}/${row.approachId}/${row.label}`).sort(),expectedKeys.sort());
  const packaged=fs.statSync(path.resolve(__dirname,'../../../backend/target/dsa-backend-1.0.0.jar')).mtimeMs;
  assert(Date.parse(transport.generated)>=packaged);
  const hashes=new Map();
  for(const row of transport.rows) {
    const candidate=cases.find(c=>c.id===row.problemId);assert(candidate);
    assert.deepEqual(row.input,row.label==='maximum'?maximum(row.problemId,row.approachId):row.label==='zero'?candidate.zero:candidate.input);
    assert.equal(row.answer,String(oracle(row.problemId,row.input)));
    if(row.calls!==null)assert.equal(row.rootCount,candidate.roots??1);
    const key=`${row.problemId}/${row.approachId}`;
    if(!hashes.has(key)) {
      const response=await fetch(`${api}/api/problems/${row.problemId}?approach=${row.approachId}`);
      assert.equal(response.status,200);
      hashes.set(key,createHash('sha256').update((await response.json()).javaCode).digest('hex'));
    }
    assert.equal(row.sourceHash,hashes.get(key));
    assert(row.fullDeltaEqual&&row.independentAnswerMatches&&row.sourceAndIdentityMatch);
    assert(row.fullBytes<2_000_000&&row.deltaBytes<2_000_000);
  }
  const names=[['chromium-standard-zoom1.json',30],['firefox-standard-zoom1.json',12],
    ['chromium-standard-zoom2.json',12],['chromium-maximum-zoom1.json',24],['firefox-maximum-zoom1.json',24]];
  const documents=names.map(([name,count])=>{
    const doc=read(name);assert.equal(doc.rows.length,count);
    assert(Date.parse(doc.generated)>=packaged);
    return doc;
  });
  const comparison=read('comparison-results.json');assert.equal(comparison.rows.length,12);
  assert(Date.parse(comparison.generated)>=packaged);
  const recovery=family==='circular-activity-obstacles'?read('input-recovery-results.json'):null;
  if(recovery){
    assert.equal(recovery.rows.length,8);assert(Date.parse(recovery.generated)>=packaged);
    const axes=['chromium','firefox'].flatMap(engine=>[320,1366].flatMap(width=>['light','dark'].map(theme=>`${engine}:${width}:${theme}`)));
    assert.deepEqual(recovery.rows.map(row=>`${row.engine}:${row.width}:${row.theme}`).sort(),axes.sort());
    for(const row of recovery.rows)assert(row.passed&&row.rejectedDraftPreserved&&row.defaultRunAndStepKept
      &&row.noAutomaticRepair&&row.explicitColumnRepair&&row.minimumColumnsEnforced&&row.successfulInputShared&&row.answer==='50');
  }
  const compatibility=family==='circular-activity-obstacles'?read('linear-grid-compatibility/chromium-standard-zoom1.json'):null;
  if(compatibility){
    assert(compatibility.realBackend&&compatibility.buildMode==='production-preview'&&compatibility.engine==='chromium');
    assert(Date.parse(compatibility.generated)>=packaged);
    const expected=require('./linear-grid-cases.cjs').cases.flatMap(c=>['light','dark'].map(theme=>`${c.id}:${theme}`));
    assert.deepEqual(compatibility.rows.map(r=>`${r.problemId}:${r.theme}`).sort(),expected.sort());
    for(const row of compatibility.rows){
      assert(row.width===320&&row.height===568&&row.nativeZoom===1&&row.noOverflowOrErrors
        &&row.allThreeExecuted&&row.inputAndStepRestored&&row.viewsDoNotExecute&&row.refusalPreservesRunAndDraft);
      const response=await fetch(`${api}/api/problems/${row.problemId}?approach=${row.shownApproach}`);assert.equal(response.status,200);
      assert.equal(row.sourceHash,createHash('sha256').update((await response.json()).javaCode).digest('hex'));
    }
  }
  const assets=documents[0].servedBuild.assets;
  for(const doc of [...documents,comparison,...(recovery?[recovery]:[]),...(compatibility?[compatibility]:[])])assert.deepEqual(doc.servedBuild.assets,assets);
  const html=fs.readFileSync(path.resolve(__dirname,'../../../frontend/dist/index.html'),'utf8');
  for(const asset of assets)assert(html.includes(asset));
  for(const doc of documents)for(const row of doc.rows) {
    assert.equal(row.sourceHash,hashes.get(`${row.problemId}/${row.shownApproach}`));
    assert(row.noOverflowOrErrors);
    if(row.allCallsRenderedAndRootVisible&&family==='circular-activity-obstacles'){
      assert.equal(row.rootCount,cases.find(c=>c.id===row.problemId)?.roots??1);
      assert(row.allStateCoordinatesVisible);
    }
  }
  const rows=documents.flatMap(doc=>doc.rows);
  const result={generated:new Date().toISOString(),passed:true,sourceHashes:Object.fromEntries(hashes),
    packagedBackendAt:new Date(packaged).toISOString(),assets,apiScenarios:scenarios,apiRequests:scenarios*2,
    browserJourneys:rows.length,comparisonJourneys:comparison.rows.length,inputRecoveryJourneys:recovery?.rows.length??0,
    legacyCompatibilityJourneys:compatibility?.rows.length??0,
    totalJourneys:rows.length+comparison.rows.length+(recovery?.rows.length??0)+(compatibility?.rows.length??0),
    perProblem:cases.map(c=>({problemId:c.id,browserRows:rows.filter(r=>r.problemId===c.id).length,
      comparisonRows:comparison.rows.filter(r=>r.problemId===c.id).length})),
    maxFullResponseBytes:Math.max(...transport.rows.map(r=>r.fullBytes)),
    allSourceHashesMatchCurrentBackend:true,allManifestsPostdatePackagedBackend:true,allAssetsMatchProductionBuild:true,
    limits:'Bounded automated scenarios for these three candidates only; not all56 DP candidates, public deployment, human/device/screen-reader/learner/performance/release acceptance.'};
  fs.writeFileSync(path.join(out,'manifest-audit.json'),JSON.stringify(result,null,2));
  console.log(JSON.stringify(result,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
