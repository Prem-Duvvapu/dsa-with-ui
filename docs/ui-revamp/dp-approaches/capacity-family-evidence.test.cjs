const test=require('node:test');
const assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process');
const profile=()=>require('./capacity-family-cases.cjs');
test('capacity is an explicit evidence profile, never a fallback',()=>{
  const result=spawnSync(process.execPath,['-e',"require('./dp-audit-profile.cjs')"],{cwd:__dirname,env:{...process.env,DP_AUDIT_FAMILY:'capacity-family'}});
  assert.equal(result.status,0,result.stderr.toString());
});
test('independent references distinguish reuse, index pairs, unused capacity and zero prices',()=>{
  const {oracle}=profile(),input={weights:[2],values:[5],capacity:4};
  assert.equal(oracle('knapsack-01',input),5);assert.equal(oracle('unbounded-knapsack',input),10);
  assert.equal(oracle('knapsack-01',{weights:[2,3],values:[8,1],capacity:3}),8);
  assert.equal(oracle('knapsack-01',{weights:[3,2],values:[1,8],capacity:3}),8);
  assert.equal(oracle('unbounded-knapsack',{weights:[5],values:[10],capacity:4}),0);
  assert.equal(oracle('rod-cutting-problem',{prices:[0,0,0,0]}),0);
  assert.equal(oracle('rod-cutting-problem',{prices:[2,5,7,8,10]}),12);
  assert.throws(()=>oracle('invented',input));
});
test('each authored dirty editor changes the answer, preserving pair context',()=>{
  const {cases,oracle}=profile();assert.equal(cases.length,3);
  for(const c of cases){
    assert.equal(String(oracle(c.id,c.input)),c.answer);
    const edited=structuredClone(c.input);edited[c.draftField][0]=9;
    assert.notEqual(String(oracle(c.id,edited)),c.answer);
    if(c.input.weights){assert.equal(c.draftField,'values');assert.deepEqual(c.arrayLabels,{weights:'Weights',values:'Values'});}
    else assert.deepEqual(c.arrayLabels,{prices:'Prices by piece length'});
  }
});
test('maximum and refused drafts are explicitly authored for each input contract',()=>{
  const {cases,maximum,oracle}=profile();
  for(const c of cases){
    for(const form of ['recursion','memoization','canonical']){
      const input=maximum(c.id,form);
      if(input.weights){assert.equal(input.weights.length,input.values.length);assert(input.weights.every(v=>v>=1));}
      else assert.equal(input.prices.length,form==='recursion'?9:form==='memoization'?10:12);
      assert(Number.isFinite(oracle(c.id,input)));
    }
    assert(c.rejected);
    if(c.rejected.weights)assert.equal(c.rejected.weights.length,c.rejected.values.length);
  }
  assert.throws(()=>maximum('invented','recursion'));assert.throws(()=>maximum(cases[0].id,'invented'));
});
test('memo payload stress covers every legal item count at its largest joint-safe capacity',()=>{
  for(const c of profile().cases.filter(c=>c.input.weights)){
    assert.equal(Object.keys(c.memoExtra??{}).length,8);
    for(let n=1;n<=8;n++){
      const input=c.memoExtra[`memo-shape-${n}`];
      assert.deepEqual(input.weights,Array(n).fill(1));assert.deepEqual(input.values,Array(n).fill(100));
      assert.equal(input.capacity,Math.min(30,Math.floor(80/(n+1))-1));
      assert((n+1)*(input.capacity+1)<=80);
    }
  }
});
