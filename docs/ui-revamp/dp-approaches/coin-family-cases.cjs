// Independent shortest-path / quantity-vector references, not the traced prefix recurrence.
const memoStress=minimum=>Object.fromEntries(Array.from({length:8},(_,i)=>{
  const n=i+1;return [`memo-shape-${n}`,{coins:minimum?Array(n).fill(1):Array.from({length:n},(_,i)=>n-i),amount:Math.floor(80/(n+1))-1}];
}));
const cases=[
  {id:'minimum-coins-dp',input:{coins:[1,2,5],amount:11},answer:'3',roots:1,
    arrayLabels:{coins:'Coin denominations'},scalarLabels:{amount:'Target amount'},draftField:'coins',memoExtra:memoStress(true),
    rejected:{coins:[1,2,3,4],amount:12},
    extra:{'impossible-cache':{coins:[2,2,2],amount:3},'reuse':{coins:[2],amount:4},'permuted':{coins:[5,2,1],amount:11},'deep-chain':{coins:[1],amount:39}}},
  {id:'coin-change-2',input:{coins:[1,2,5],amount:5},answer:'4',roots:1,
    arrayLabels:{coins:'Coin denominations'},scalarLabels:{amount:'Target amount'},draftField:'coins',memoExtra:memoStress(false),
    rejected:{coins:[1,2,3,4],amount:12},
    extra:{'impossible-cache':{coins:[2,4,6],amount:7},'reuse':{coins:[1,2],amount:3},'permuted':{coins:[5,2,1],amount:5},'deep-chain':{coins:[1],amount:39}}}
];
function maximum(id,form){
  if(!cases.some(c=>c.id===id))throw Error(`No authored maximum for ${id}`);
  if(!['recursion','memoization','canonical'].includes(form))throw Error(`Unknown form ${form}`);
  if(form==='recursion')return {coins:[23,6,7,15,3,11],amount:23};
  if(form==='memoization')return id==='minimum-coins-dp'?{coins:Array(7).fill(1),amount:9}:{coins:[4,3,2,1],amount:15};
  return {coins:[1,2,3,4,5,6,7,8],amount:24};
}
function oracle(id,{coins,amount}){
  if(id==='minimum-coins-dp'){
    const distance=Array(amount+1).fill(-1),queue=[0];distance[0]=0;
    for(let head=0;head<queue.length;head++)for(const coin of coins){const next=queue[head]+coin;
      if(next<=amount&&distance[next]<0){distance[next]=distance[queue[head]]+1;queue.push(next);}}
    return distance[amount];
  }
  if(id!=='coin-change-2')throw Error(`No independent reference for ${id}`);
  function quantities(index,left){if(index===coins.length)return left===0?1:0;let count=0;
    for(let quantity=0;quantity*coins[index]<=left;quantity++)count+=quantities(index+1,left-quantity*coins[index]);return count;}
  return quantities(0,amount);
}
module.exports={cases,maximum,oracle};
