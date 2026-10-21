const test=require('node:test');
const assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process');
const profile=()=>require('./subset-family-cases.cjs');
test('subset family is an explicit profile, not a fallback',()=>{
  const result=spawnSync(process.execPath,['-e',"require('./dp-audit-profile.cjs')"],{cwd:__dirname,env:{...process.env,DP_AUDIT_FAMILY:'subset-family'}});
  assert.equal(result.status,0,result.stderr.toString());
  assert.equal(spawnSync(process.execPath,['-e',"require('./dp-audit-profile.cjs')"],{cwd:__dirname,env:{...process.env,DP_AUDIT_FAMILY:'invented'}}).status,1);
});
test('independent enumeration preserves booleans, indexed duplicates and empty subset',()=>{
  const {oracle}=profile();
  assert.equal(oracle('subset-sum-equal-target',{nums:[0,0],target:0}),true);
  assert.equal(oracle('subset-sum-equal-target',{nums:[2,4],target:3}),false);
  assert.equal(oracle('partition-equal-subset-sum',{nums:[1,2]}),false);
  assert.equal(oracle('partition-equal-subset-sum',{nums:[0,0]}),true);
  assert.equal(oracle('count-subsets-with-sum-k',{nums:[1,1],k:1}),2);
  assert.equal(oracle('count-subsets-with-sum-k',{nums:[1,1],k:0}),1);
  assert.throws(()=>oracle('invented',{nums:[1]}));
});
test('authored default answers and comparison edits are materially different',()=>{
  const {cases,oracle}=profile();assert.equal(cases.length,3);
  for(const c of cases){assert.equal(String(oracle(c.id,c.input)),c.answer);const edited=structuredClone(c.input);edited.nums[0]=9;assert.notEqual(String(oracle(c.id,edited)),c.answer);}
});
test('maximum witnesses obey form-specific dimensions and memo joint cap',()=>{
  const {cases,maximum}=profile();
  for(const c of cases)for(const form of ['recursion','memoization','canonical']){
    const input=maximum(c.id,form),sum=input.nums.reduce((a,b)=>a+b,0),target=input.target??input.k??sum/2;
    assert.equal(input.nums.length,form==='recursion'?6:form==='memoization'?9:10);
    assert((input.nums.length+1)*(target+1)<=(form==='canonical'?225:form==='memoization'?80:1000));
    if(c.id==='count-subsets-with-sum-k')assert(input.nums.every(v=>v>=1&&v<=15));
  }
  assert.throws(()=>maximum('invented','memoization'));
});
test('parity preflight records no fabricated root',()=>{
  const c=profile().cases.find(c=>c.id==='partition-equal-subset-sum');
  assert.equal(c.roots({nums:[1,2]}),0);assert.equal(c.roots(c.input),1);
});
