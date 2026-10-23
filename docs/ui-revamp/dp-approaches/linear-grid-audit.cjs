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
  const scenarios=cases.reduce((sum,c)=>sum+6+(c.zero?1:0)+(c.negative?1:0)+Object.keys(c.memoExtra??{}).length+3*Object.keys(c.extra??{}).length,0);
  assert.equal(transport.rows.length,scenarios);
  const expectedKeys=cases.flatMap(c=>['recursion','memoization','canonical'].flatMap(form=>
    ['default','maximum',...(c.zero&&form==='memoization'?['zero']:[]),...(c.negative&&form==='memoization'?['negative']:[]),
      ...(form==='memoization'?Object.keys(c.memoExtra??{}):[]),...Object.keys(c.extra??{})].map(label=>`${c.id}/${form}/${label}`)));
  assert.deepEqual(transport.rows.map(row=>`${row.problemId}/${row.approachId}/${row.label}`).sort(),expectedKeys.sort());
  const packaged=fs.statSync(path.resolve(__dirname,'../../../backend/target/dsa-backend-1.0.0.jar')).mtimeMs;
  assert(Date.parse(transport.generated)>=packaged);
  const hashes=new Map();
  for(const row of transport.rows) {
    const candidate=cases.find(c=>c.id===row.problemId);assert(candidate);
    const expectedInput=row.label==='maximum'?maximum(row.problemId,row.approachId):row.label==='zero'?candidate.zero:row.label==='negative'?candidate.negative:candidate.memoExtra?.[row.label]??candidate.extra?.[row.label]??candidate.input;
    assert.deepEqual(row.input,expectedInput);
    assert.equal(row.answer,String(oracle(row.problemId,row.input)));
    if(row.calls!==null)assert.equal(row.rootCount,typeof candidate.roots==='function'?candidate.roots(row.input):candidate.roots??1);
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
  const names=[['chromium-standard-zoom1.json',cases.length*10],['firefox-standard-zoom1.json',cases.length*4],
    ['chromium-standard-zoom2.json',cases.length*4],['chromium-maximum-zoom1.json',cases.length*8],['firefox-maximum-zoom1.json',cases.length*8]];
  const documents=names.map(([name,count])=>{
    const doc=read(name);assert.equal(doc.rows.length,count);
    assert(Date.parse(doc.generated)>=packaged);
    return doc;
  });
  const comparison=read('comparison-results.json');assert.equal(comparison.rows.length,cases.length*4);
  assert(Date.parse(comparison.generated)>=packaged);
  const edges=['subset-family','capacity-family'].includes(family)?read('edge-results.json'):null;
  if(edges){
    assert(edges.realBackend&&edges.buildMode==='production-preview'&&Date.parse(edges.generated)>=packaged);
    const axes=cases.flatMap(c=>['recursion','memoization','canonical'].flatMap(form=>[...Object.keys(c.extra),...(form==='canonical'?['maximum']:[])].flatMap(label=>
      ['chromium','firefox'].map(engine=>`${c.id}/${label}/${engine}/${form}`))));
    assert.deepEqual(edges.rows.map(r=>`${r.problemId}/${r.label}/${r.engine}/${r.approachId}`).sort(),axes.sort());
    for(const row of edges.rows){
      const c=cases.find(c=>c.id===row.problemId);assert(c);assert.deepEqual(row.input,row.label==='maximum'?maximum(c.id,row.approachId):c.extra[row.label]);
      assert.equal(row.answer,String(oracle(row.problemId,row.input)));
      assert.equal(row.sourceHash,hashes.get(`${row.problemId}/${row.approachId}`));
      assert(row.passed&&(family==='capacity-family'?row.sharedStepHandledAndRestored:row.sharedStepClampedAndRestored)
        &&row.exactDraftRetained&&row.noUnsolicitedExecution&&row.noOverflowOrErrors);
      if(row.label==='odd'&&row.approachId!=='canonical')assert(row.oddNoFabricatedCalls);
      if(row.approachId!=='recursion'&&!(row.approachId==='memoization'&&row.label==='odd'))assert(row.actualTableValuesAndLabels);
      if(row.approachId==='memoization'&&row.label==='false-cache')assert(row.falseOrZeroCacheReuse&&row.jointRefusalPreservesRunDraftStepAndLink);
      if(family==='capacity-family'&&row.approachId==='memoization'&&row.label==='zero-cache'){
        assert(row.zeroCacheReuse&&row.jointRefusalPreservesRunDraftStepAndLink);
        assert.equal(row.refusedRuns,c.input.weights?3:1);
        if(c.input.weights)assert(row.mismatchedPairsBothDirections);
      }
      if(family==='capacity-family'){
        const executed=transport.rows.find(r=>r.problemId===row.problemId&&r.approachId===row.approachId&&r.label===row.label);assert(executed);
        assert.equal(row.requestedStep,row.label==='maximum'?4000:3);
        const outOfRange=row.requestedStep>executed.steps;
        assert.equal(row.restoredStep,outOfRange?1:row.requestedStep);
        assert.equal(row.stepWasOutOfRange,outOfRange);assert.equal(row.outOfRangeNoticeAndLinkReset,outOfRange);
      }
    }
  }
  const recovery=family==='circular-activity-obstacles'?read('input-recovery-results.json'):null;
  if(recovery){
    assert.equal(recovery.rows.length,8);assert(Date.parse(recovery.generated)>=packaged);
    const axes=['chromium','firefox'].flatMap(engine=>[320,1366].flatMap(width=>['light','dark'].map(theme=>`${engine}:${width}:${theme}`)));
    assert.deepEqual(recovery.rows.map(row=>`${row.engine}:${row.width}:${row.theme}`).sort(),axes.sort());
    for(const row of recovery.rows)assert(row.passed&&row.rejectedDraftPreserved&&row.defaultRunAndStepKept
      &&row.noAutomaticRepair&&row.explicitColumnRepair&&row.minimumColumnsEnforced&&row.successfulInputShared&&row.answer==='50');
  }
  const shapes=family==='falling-triangle'?read('shape-results.json'):null;
  if(shapes){
    assert(shapes.realBackend&&shapes.buildMode==='production-preview');
    assert(Date.parse(shapes.generated)>=packaged);
    const axes=['chromium','firefox'].flatMap(engine=>cases.flatMap(c=>Object.keys(c.extra??{}).flatMap(shape=>
      ['recursion','memoization','canonical'].map(form=>`${engine}:${c.id}:${shape}:${form}`))));
    assert.deepEqual(shapes.rows.map(row=>`${row.engine}:${row.problemId}:${row.shape}:${row.approachId}`).sort(),axes.sort());
    for(const row of shapes.rows){
      const candidate=cases.find(c=>c.id===row.problemId);assert(candidate);
      assert.deepEqual(row.input,candidate.extra[row.shape]);
      assert.equal(row.answer,String(oracle(row.problemId,row.input)));
      assert.equal(row.sourceHash,hashes.get(`${row.problemId}/${row.approachId}`));
      assert(row.width===320&&row.height===568&&row.theme==='dark'&&row.passed&&row.sharedStepRestored
        &&row.exactSubmittedGridRetained&&row.noUnsolicitedExecution&&row.noOverflowOrErrors);
      if(candidate.field==='triangle'&&row.approachId==='memoization')assert(row.memoDomainAndExcludedCells);
      if(row.approachId!=='recursion')assert(row.tableSummaryMatchesActualCells&&row.tableCellLabelsMatchActualCells);
    }
  }
  const compatibilityFamily=family==='circular-activity-obstacles'?'linear-grid':family==='falling-triangle'?'circular-activity-obstacles':family==='subset-family'?'falling-triangle':family==='capacity-family'?'subset-family':null;
  const compatibility=compatibilityFamily?read(`${compatibilityFamily==='circular-activity-obstacles'?'circular':compatibilityFamily}-compatibility/chromium-standard-zoom1.json`):null;
  if(compatibility){
    assert(compatibility.realBackend&&compatibility.buildMode==='production-preview'&&compatibility.engine==='chromium');
    assert(Date.parse(compatibility.generated)>=packaged);
    const expected=require(`./${compatibilityFamily}-cases.cjs`).cases.flatMap(c=>['light','dark'].map(theme=>`${c.id}:${theme}`));
    assert.deepEqual(compatibility.rows.map(r=>`${r.problemId}:${r.theme}`).sort(),expected.sort());
    for(const row of compatibility.rows){
      assert(row.width===320&&row.height===568&&row.nativeZoom===1&&row.noOverflowOrErrors
        &&row.allThreeExecuted&&row.inputAndStepRestored&&row.viewsDoNotExecute&&row.refusalPreservesRunAndDraft);
      const response=await fetch(`${api}/api/problems/${row.problemId}?approach=${row.shownApproach}`);assert.equal(response.status,200);
      assert.equal(row.sourceHash,createHash('sha256').update((await response.json()).javaCode).digest('hex'));
    }
  }
  const assets=documents[0].servedBuild.assets;
  for(const doc of [...documents,comparison,...(recovery?[recovery]:[]),...(compatibility?[compatibility]:[]),...(shapes?[shapes]:[]),...(edges?[edges]:[])])assert.deepEqual(doc.servedBuild.assets,assets);
  const html=fs.readFileSync(path.resolve(__dirname,'../../../frontend/dist/index.html'),'utf8');
  for(const asset of assets)assert(html.includes(asset));
  for(const doc of documents)for(const row of doc.rows) {
    assert.equal(row.sourceHash,hashes.get(`${row.problemId}/${row.shownApproach}`));
    assert(row.noOverflowOrErrors);
    if(row.allCallsRenderedAndRootVisible&&family!=='linear-grid'){
      const c=cases.find(c=>c.id===row.problemId);assert(c);
      assert.equal(row.rootCount,typeof c.roots==='function'?c.roots(maximum(c.id,row.initial)):c.roots??1);
      assert(row.allStateCoordinatesVisible);
    }
  }
  const rows=documents.flatMap(doc=>doc.rows);
  const result={generated:new Date().toISOString(),passed:true,sourceHashes:Object.fromEntries(hashes),
    packagedBackendAt:new Date(packaged).toISOString(),assets,apiScenarios:scenarios,apiRequests:scenarios*2,
    browserJourneys:rows.length,comparisonJourneys:comparison.rows.length,inputRecoveryJourneys:recovery?.rows.length??0,
    shapeJourneys:shapes?.rows.length??0,
    edgeJourneys:edges?.rows.length??0,
    legacyCompatibilityJourneys:compatibility?.rows.length??0,
    totalJourneys:rows.length+comparison.rows.length+(recovery?.rows.length??0)+(compatibility?.rows.length??0)+(shapes?.rows.length??0)+(edges?.rows.length??0),
    perProblem:cases.map(c=>({problemId:c.id,browserRows:rows.filter(r=>r.problemId===c.id).length,
      comparisonRows:comparison.rows.filter(r=>r.problemId===c.id).length})),
    maxFullResponseBytes:Math.max(...transport.rows.map(r=>r.fullBytes)),
    allSourceHashesMatchCurrentBackend:true,allManifestsPostdatePackagedBackend:true,allAssetsMatchProductionBuild:true,
    limits:`Bounded automated scenarios for these${cases.length} candidates only; not all56 DP candidates, public deployment, human/device/screen-reader/learner/performance/release acceptance.`};
  fs.writeFileSync(path.join(out,'manifest-audit.json'),JSON.stringify(result,null,2));
  console.log(JSON.stringify(result,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
