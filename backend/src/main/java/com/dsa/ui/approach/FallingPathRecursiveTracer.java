package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Genuine minimum-over-bottom-roots driver with valid predecessor calls only. */
final class FallingPathRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    FallingPathRecursiveTracer(boolean memo) { this.memo=memo; }
    public String id() { return "minimum-falling-path-sum"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/"+id()+"/"+(memo?"memoization":"recursion")+".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("matrix",FieldType.INT_GRID).label("Matrix")
                .constraint("minRows",1).constraint("maxRows",memo?8:4)
                .constraint("minCols",1).constraint("maxCols",memo?8:6).values(-100,100)
                .defaultValue(List.of(List.of(2,1,3),List.of(6,5,4),List.of(7,8,9)))
                .help("Start at any top-row column; move below or one column sideways. "
                        +(memo?"Cache signed costs by row and column; -1 and zero are known values.":"Up to4 rows and6 columns keeps all bottom-root calls within the220-call renderer; pan locally as needed.")).build());
    }
    public Map<String,Object> alternateInput() { return Map.of("matrix",List.of(List.of(100,100),List.of(1,100))); }
    public void run(Inputs in,StepEmitter emit) {
        int[][] matrix=in.getGrid("matrix");int rows=matrix.length,cols=matrix[0].length;
        var execution=new RecursiveDpExecution(emit,memo,1,RecursiveDpExecution.labels("row=",rows),RecursiveDpExecution.labels("col=",cols),null,"cost");
        var context=new RecursiveDpExecution.State(0,-1,-1,"minimum over all bottom-row endpoints");
        execution.start(context,"A path can start anywhere in row0. Evaluate one real root per bottom column; the driver itself is not a recursive call.");
        int answer=Integer.MAX_VALUE;
        for(int col=0;col<cols;col++){
            execution.note("reduce",context,"Evaluate the bottom-row root at column "+col+" and retain the minimum returned path cost.");
            answer=Math.min(answer,cost(rows-1,col,matrix,execution));
        }
        execution.done(context,answer);
    }
    private int cost(int row,int col,int[][] matrix,RecursiveDpExecution execution) {
        return execution.evaluate(new RecursiveDpExecution.State(0,row,col,"row="+row+",col="+col),row==0,
                "Read directly above, then upper-left/right only when they exist; add this cell to the cheapest returned predecessor.",()->{
                    if(row==0)return matrix[0][col];
                    int best=cost(row-1,col,matrix,execution);
                    if(col>0)best=Math.min(best,cost(row-1,col-1,matrix,execution));
                    if(col+1<matrix[0].length)best=Math.min(best,cost(row-1,col+1,matrix,execution));
                    return matrix[row][col]+best;
                });
    }
}
