const cases=[
  {id:'minimum-falling-path-sum',field:'matrix',input:{matrix:[[2,1,3],[6,5,4],[7,8,9]]},answer:'13',
    roots:input=>input.matrix[0].length,draftCell:[0,1],recursionRows:4,minValue:-100,
    zero:{matrix:Array.from({length:4},()=>Array(4).fill(0))},negative:{matrix:Array.from({length:4},()=>Array(4).fill(-1))},
    extra:{'one-column':{matrix:[[2],[3]]}}},
  {id:'triangle-min-path-sum',field:'triangle',input:{triangle:[[2,0,0,0],[3,4,0,0],[6,5,7,0],[4,1,8,3]]},answer:'11',
    roots:1,recursionRows:7,minValue:-100,
    zero:{triangle:Array.from({length:4},()=>Array(4).fill(0))},negative:{triangle:Array.from({length:4},()=>Array(4).fill(-1))},
    extra:{'rectangular-tall':{triangle:[[2,99],[3,4],[-100,-100]]},'rectangular-wide':{triangle:[[2,-100,-100],[3,4,-100]]}}}
];
function maximum(id,form){
  if(id==='minimum-falling-path-sum')return{matrix:Array.from({length:form==='recursion'?4:8},()=>Array(form==='recursion'?6:8).fill(-100))};
  if(id==='triangle-min-path-sum'){const n=form==='recursion'?7:8;return{triangle:Array.from({length:n},()=>Array(n).fill(-100))};}
  throw Error(`No authored maximum for ${id}`);
}
function oracle(id,input){
  if(id==='minimum-falling-path-sum'){
    const matrix=input.matrix;
    function fall(row,col,sum){sum+=matrix[row][col];if(row===matrix.length-1)return sum;let best=Infinity;for(let next=Math.max(0,col-1);next<=Math.min(matrix[0].length-1,col+1);next++)best=Math.min(best,fall(row+1,next,sum));return best;}
    let best=Infinity;for(let start=0;start<matrix[0].length;start++)best=Math.min(best,fall(0,start,0));return best;
  }
  if(id==='triangle-min-path-sum'){
    const triangle=input.triangle,n=Math.min(triangle.length,triangle[0].length);
    function descend(row,col,sum){sum+=triangle[row][col];return row===n-1?sum:Math.min(descend(row+1,col,sum),descend(row+1,col+1,sum));}
    return descend(0,0,0);
  }
  throw Error(`No independent reference for ${id}`);
}
module.exports={cases,maximum,oracle};
