// Independent mask / quantity-vector / cut-composition references, not prefix DP copies.
const memoStress=()=>Object.fromEntries(Array.from({length:8},(_,i)=>{
  const n=i+1;return [`memo-shape-${n}`,{weights:Array(n).fill(1),values:Array(n).fill(100),capacity:Math.min(30,Math.floor(80/(n+1))-1)}];
}));
const cases=[
  {id:'knapsack-01',input:{weights:[1,2,4,5],values:[5,4,8,6],capacity:5},answer:'13',roots:1,
    arrayLabels:{weights:'Weights',values:'Values'},scalarLabels:{capacity:'Capacity'},draftField:'values',memoExtra:memoStress(),
    rejected:{weights:Array(7).fill(1),values:Array(7).fill(1),capacity:7},
    extra:{'single-vs-reuse':{weights:[2],values:[5],capacity:4},'zero-cache':{weights:[5,5,5],values:[1,1,1],capacity:6},
      'paired-permutation':{weights:[5,4,2,1],values:[6,8,4,5],capacity:5}}},
  {id:'unbounded-knapsack',input:{weights:[2,4,6],values:[5,11,13],capacity:10},answer:'27',roots:1,
    arrayLabels:{weights:'Weights',values:'Values'},scalarLabels:{capacity:'Capacity'},draftField:'values',memoExtra:memoStress(),
    rejected:{weights:Array(4).fill(1),values:Array(4).fill(1),capacity:5},
    extra:{'single-vs-reuse':{weights:[2],values:[5],capacity:4},'zero-cache':{weights:[5,5,5],values:[1,1,1],capacity:6},
      'unused-capacity':{weights:[3],values:[10],capacity:8}}},
  {id:'rod-cutting-problem',input:{prices:[2,5,7,8,10]},answer:'12',roots:1,
    arrayLabels:{prices:'Prices by piece length'},draftField:'prices',
    rejected:{prices:Array(10).fill(1)},
    extra:{'zero-cache':{prices:[0,0,0,0,0]},'repeat-unit':{prices:[9,1,1,1]},'long-piece':{prices:[0,0,0,99]}}}
];
function maximum(id,form){
  if(!cases.some(c=>c.id===id))throw Error(`No authored maximum for ${id}`);
  if(!['recursion','memoization','canonical'].includes(form))throw Error(`Unknown form ${form}`);
  if(id==='rod-cutting-problem')return {prices:Array(form==='recursion'?9:form==='memoization'?10:12).fill(100)};
  if(form==='recursion')return id==='knapsack-01'
    ?{weights:[3,1,15,5,3,19,7,17],values:Array(8).fill(100),capacity:29}
    :{weights:[17,2,4,15],values:Array(4).fill(100),capacity:26};
  if(form==='memoization')return {weights:id==='knapsack-01'?[1,3,1,1,5,1,2]:Array(7).fill(1),values:Array(7).fill(100),capacity:9};
  return {weights:Array(8).fill(20),values:Array(8).fill(100),capacity:24};
}
function oracle(id,input){
  if(!cases.some(c=>c.id===id))throw Error(`No independent reference for ${id}`);
  if(id==='rod-cutting-problem'){
    function cuts(left){let best=0;for(let cut=1;cut<=left;cut++)best=Math.max(best,input.prices[cut-1]+cuts(left-cut));return best;}
    return cuts(input.prices.length);
  }
  if(id==='knapsack-01'){
    let best=0;for(let mask=0;mask<2**input.weights.length;mask++){
      let weight=0,value=0;input.weights.forEach((w,i)=>{if(mask&(1<<i)){weight+=w;value+=input.values[i];}});
      if(weight<=input.capacity)best=Math.max(best,value);
    }return best;
  }
  function quantities(item,left){
    if(item===input.weights.length)return 0;
    let best=0;for(let quantity=0;quantity*input.weights[item]<=left;quantity++)
      best=Math.max(best,quantity*input.values[item]+quantities(item+1,left-quantity*input.weights[item]));
    return best;
  }
  return quantities(0,input.capacity);
}
module.exports={cases,maximum,oracle};
