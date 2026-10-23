const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const path=require('node:path');
const vm=require('node:vm');
const {createRequire}=require('node:module');
const file=path.join(__dirname,'verify-public-circular-baseline.cjs');
function configuration(family){
  const context={require:createRequire(file),__dirname,process:{env:family?{PUBLIC_BASELINE_FAMILY:family}:{}}};
  const prefix=fs.readFileSync(file,'utf8').split('(async()=>{')[0];
  vm.runInNewContext(prefix+'\nresult={family,baseline,cases};',context);
  return context.result;
}
test('legacy public verifier keeps its explicit merged baseline and historical output by default',()=>{
  const result=configuration();
  assert.equal(result.family,'circular-activity-obstacles');assert.equal(result.baseline.commit,'da63085 (#229)');
  assert.equal(result.baseline.output,'falling-triangle/merged-baseline-public.json');
  assert.equal(result.cases.length,3);
});
test('subset public verification targets only the already merged subset batch',()=>{
  const result=configuration('subset-family');
  assert.equal(result.baseline.commit,'435c362 (#231)');
  assert.equal(result.baseline.output,'capacity-family/merged-subset-public.json');
  assert.deepEqual(Array.from(result.cases,c=>c.id),['subset-sum-equal-target','partition-equal-subset-sum','count-subsets-with-sum-k']);
});
test('unknown and prototype-like public profiles cannot borrow another baseline',()=>{
  for(const name of ['invented','__proto__','constructor'])assert.throws(()=>configuration(name),/Unknown public baseline profile/);
});
