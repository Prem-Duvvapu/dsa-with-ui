const test=require('node:test');
const assert=require('node:assert/strict');
const {spawnSync}=require('node:child_process');
const profile=()=>require('./coin-family-cases.cjs');
test('coin evidence requires its explicit profile',()=>{
  const result=spawnSync(process.execPath,['-e',"require('./dp-audit-profile.cjs')"],{cwd:__dirname,env:{...process.env,DP_AUDIT_FAMILY:'coin-family'}});
  assert.equal(result.status,0,result.stderr.toString());
});
test('independent references distinguish unreachable costs, zero counts and combinations',()=>{
  const {oracle}=profile();
  assert.equal(oracle('minimum-coins-dp',{coins:[2,2,2],amount:3}),-1);
  assert.equal(oracle('minimum-coins-dp',{coins:[2],amount:4}),2);
  assert.equal(oracle('coin-change-2',{coins:[2,4,6],amount:7}),0);
  assert.equal(oracle('coin-change-2',{coins:[1,2],amount:3}),2);
  assert.equal(oracle('coin-change-2',{coins:[1,2],amount:5}),3);
  assert.equal(oracle('coin-change-2',{coins:[1,2,3,4,5,6,7,8],amount:24}),919);
  assert.throws(()=>oracle('invented',{coins:[1],amount:1}));
});
test('dirty drafts change real answers and remain valid distinct denomination inputs',()=>{
  const {cases,oracle}=profile();assert.equal(cases.length,2);
  for(const c of cases){
    assert.equal(String(oracle(c.id,c.input)),c.answer);
    const edited=structuredClone(c.input);edited[c.draftField][0]=9;
    assert.notEqual(String(oracle(c.id,edited)),c.answer);
    assert.equal(new Set(edited.coins).size,edited.coins.length);
    assert.deepEqual(c.arrayLabels,{coins:'Coin denominations'});assert.deepEqual(c.scalarLabels,{amount:'Target amount'});
  }
});
test('stress probes cover all dimensions and honor each duplicate policy',()=>{
  const {cases,maximum}=profile();
  function calls(coins,n,a){return n===0||a===0?1:1+calls(coins,n-1,a)+(coins[n-1]<=a?calls(coins,n,a-coins[n-1]):0);}
  for(const c of cases){
    const max=maximum(c.id,'recursion');assert.equal(calls(max.coins,max.coins.length,max.amount),220);
    assert(calls(c.rejected.coins,c.rejected.coins.length,c.rejected.amount)>220);
    assert((c.rejected.coins.length+1)*(c.rejected.amount+1)<=80);
    assert.equal(Object.keys(c.memoExtra).length,8);
    for(let n=1;n<=8;n++){
      const input=c.memoExtra[`memo-shape-${n}`];assert.equal(input.coins.length,n);
      assert.equal(input.amount,Math.floor(80/(n+1))-1);
      if(c.id==='coin-change-2')assert.equal(new Set(input.coins).size,n);
    }
  }
  assert.throws(()=>maximum('invented','recursion'));assert.throws(()=>maximum(cases[0].id,'invented'));
});
