// Independent index-set enumeration, never answers read from the implementation.
const cases=[
  {id:'subset-sum-equal-target',input:{nums:[1,2,3,7],target:6},answer:'true',roots:1,
    scalarLabels:{target:'Target sum K'},
    extra:{'false-cache':{nums:[0,0,0,0],target:1},'zero-target':{nums:[0,0],target:0}}},
  {id:'partition-equal-subset-sum',input:{nums:[1,5,11,5]},answer:'true',
    roots:input=>input.nums.reduce((a,b)=>a+b,0)%2?0:1,
    extra:{odd:{nums:[1,2]},'false-cache':{nums:[0,0,0,2]},'zero-target':{nums:[0,0]}}},
  {id:'count-subsets-with-sum-k',input:{nums:[1,2,2,3],k:3},answer:'3',roots:1,minValue:1,
    scalarLabels:{k:'Target sum K'},
    extra:{'false-cache':{nums:[1,1,1,1],k:5},'zero-target':{nums:[1,1],k:0},'indexed-duplicates':{nums:[1,1],k:1}}}
];
function maximum(id,form){
  if(!cases.some(c=>c.id===id))throw Error(`No authored maximum for ${id}`);
  if(!['recursion','memoization','canonical'].includes(form))throw Error(`Unknown form ${form}`);
  const counting=id==='count-subsets-with-sum-k',partition=id==='partition-equal-subset-sum';
  let nums,target;
  if(form==='recursion'){nums=Array(6).fill(counting?1:0);target=counting?6:0;}
  else if(form==='memoization'){
    nums=counting?[1,1,1,1,1,1,1,2,4]:partition?[0,0,0,0,0,7,1,2,4]:[0,0,0,0,0,0,1,2,4];target=7;
  } else {nums=partition?[4,4,4,4,4,4,4,4,4,2]:Array(10).fill(1);target=19;}
  return partition?{nums}:{nums,[counting?'k':'target']:target};
}
function oracle(id,input){
  if(!cases.some(c=>c.id===id))throw Error(`No independent reference for ${id}`);
  const total=input.nums.reduce((a,b)=>a+b,0),partition=id==='partition-equal-subset-sum';
  if(partition&&total%2)return false;
  const target=partition?total/2:input.target??input.k;
  let matches=0;
  for(let mask=0;mask<2**input.nums.length;mask++){
    let sum=0;input.nums.forEach((value,i)=>{if(mask&(1<<i))sum+=value;});
    if(sum===target)matches++;
  }
  return id==='count-subsets-with-sum-k'?matches:matches>0;
}
module.exports={cases,maximum,oracle};
