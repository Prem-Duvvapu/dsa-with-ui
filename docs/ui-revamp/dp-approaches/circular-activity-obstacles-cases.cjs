const cases=[
  {id:'house-robber-2',input:{nums:[2,7,9,3,1]},answer:'11',roots:2,recursionLength:10,zero:{nums:Array(7).fill(0)}},
  {id:'ninjas-training',input:{points:[[10,40,70],[20,50,80],[30,10,20]]},answer:'150',draftCell:[0,2],roots:3,recursionRows:6,fixedCols:3,zero:{points:Array.from({length:5},()=>[0,0,0])}},
  {id:'unique-paths-2',input:{grid:[[0,0,0],[0,1,0],[0,0,0]]},answer:'2',roots:1,recursionRows:4,zero:{grid:[[1,0,0],[0,0,0],[0,0,0]]}}
];
function maximum(id,form){
  if(id==='house-robber-2')return{nums:Array(form==='recursion'?10:16).fill(999)};
  if(id==='ninjas-training')return{points:Array.from({length:form==='recursion'?6:10},()=>[100,100,100])};
  if(id==='unique-paths-2'){const n=form==='recursion'?4:8;return{grid:Array.from({length:n},()=>Array(n).fill(0))};}
  throw Error(`No authored case for ${id}`);
}
function oracle(id,input){
  if(id==='house-robber-2'){
    const nums=input.nums;let best=0;
    for(let mask=0;mask<2**nums.length;mask++){
      if((mask&(mask<<1))!==0||(mask&1)!==0&&(mask&(1<<(nums.length-1)))!==0)continue;
      let sum=0;for(let i=0;i<nums.length;i++)if(mask&(1<<i))sum+=nums[i];best=Math.max(best,sum);
    }return best;
  }
  if(id==='ninjas-training'){
    function schedule(day,previous,total){if(day===input.points.length)return total;let best=0;for(let t=0;t<3;t++)if(t!==previous)best=Math.max(best,schedule(day+1,t,total+input.points[day][t]));return best;}
    return schedule(0,-1,0);
  }
  if(id==='unique-paths-2'){
    const grid=input.grid;
    function walk(row,col){if(grid[row][col]===1)return 0;if(row===grid.length-1&&col===grid[0].length-1)return 1;return(row+1<grid.length?walk(row+1,col):0)+(col+1<grid[0].length?walk(row,col+1):0);}
    return walk(0,0);
  }
  throw Error(`No independent reference for ${id}`);
}
module.exports={cases,maximum,oracle};
