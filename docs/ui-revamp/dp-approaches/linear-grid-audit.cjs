// Reconcile completed manifests, actual current served source and the production bundle.
const fs=require('node:fs');
const path=require('node:path');
const assert=require('node:assert/strict');
const {createHash}=require('node:crypto');
const {cases}=require('./linear-grid-cases.cjs');
const out=path.join(__dirname,'linear-grid');
const api=process.env.BACKEND_URL||'http://127.0.0.1:8923';
const read=name=>JSON.parse(fs.readFileSync(path.join(out,name),'utf8'));
(async()=>{
  const transport=read('api-results.json');
  assert.equal(transport.rows.length,18);
  const packaged=fs.statSync(path.resolve(__dirname,'../../../backend/target/dsa-backend-1.0.0.jar')).mtimeMs;
  assert(Date.parse(transport.generated)>=packaged);
  const hashes=new Map();
  for(const row of transport.rows) {
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
  const assets=documents[0].servedBuild.assets;
  for(const doc of [...documents,comparison])assert.deepEqual(doc.servedBuild.assets,assets);
  const html=fs.readFileSync(path.resolve(__dirname,'../../../frontend/dist/index.html'),'utf8');
  for(const asset of assets)assert(html.includes(asset));
  for(const doc of documents)for(const row of doc.rows) {
    assert.equal(row.sourceHash,hashes.get(`${row.problemId}/${row.shownApproach}`));
    assert(row.noOverflowOrErrors);
  }
  const rows=documents.flatMap(doc=>doc.rows);
  const result={generated:new Date().toISOString(),passed:true,sourceHashes:Object.fromEntries(hashes),
    packagedBackendAt:new Date(packaged).toISOString(),assets,apiScenarios:18,apiRequests:36,
    browserJourneys:rows.length,comparisonJourneys:comparison.rows.length,totalJourneys:rows.length+comparison.rows.length,
    perProblem:cases.map(c=>({problemId:c.id,browserRows:rows.filter(r=>r.problemId===c.id).length,
      comparisonRows:comparison.rows.filter(r=>r.problemId===c.id).length})),
    maxFullResponseBytes:Math.max(...transport.rows.map(r=>r.fullBytes)),
    allSourceHashesMatchCurrentBackend:true,allManifestsPostdatePackagedBackend:true,allAssetsMatchProductionBuild:true,
    limits:'Bounded automated scenarios for these three candidates only; not all56 DP candidates, public deployment, human/device/screen-reader/learner/performance/release acceptance.'};
  fs.writeFileSync(path.join(out,'manifest-audit.json'),JSON.stringify(result,null,2));
  console.log(JSON.stringify(result,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
