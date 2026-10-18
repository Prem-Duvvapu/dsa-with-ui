package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Preserves the canonical min(rows,columns) padded triangle and genuine child dependencies. */
final class TrianglePathRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    TrianglePathRecursiveTracer(boolean memo) { this.memo=memo; }
    public String id() { return "triangle-min-path-sum"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/"+id()+"/"+(memo?"memoization":"recursion")+".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("triangle",FieldType.INT_GRID).label("Triangle")
                .constraint("minRows",1).constraint("maxRows",memo?8:7)
                .constraint("minCols",1).constraint("maxCols",memo?8:7).values(-100,100)
                .defaultValue(List.of(List.of(2,0,0,0),List.of(3,4,0,0),List.of(6,5,7,0),List.of(4,1,8,3)))
                .help("Effective depth is min(rows,columns); only columns0..row of that depth are used. Other input entries remain ignored, not replaced. "
                        +(memo?"Cache real signed costs; # marks excluded padding, not an unknown state.":"Each dimension is capped at7 to keep the complete binary call tree below220 nodes.")).build());
    }
    public Map<String,Object> alternateInput() { return Map.of("triangle",List.of(List.of(-1,0,0),List.of(2,3,0),List.of(1,-1,-3))); }
    public void run(Inputs in,StepEmitter emit) {
        int[][] triangle=in.getGrid("triangle");int n=Math.min(triangle.length,triangle[0].length);
        var execution=new RecursiveDpExecution(emit,memo,1,RecursiveDpExecution.labels("row=",n),RecursiveDpExecution.labels("col=",n),null,"cost",
                (row,col)->col>row,"# marks excluded triangle padding (col > row); these cells are not unknown states. Effective depth n="+n+".");
        var root=new RecursiveDpExecution.State(0,0,0,"row=0,col=0");
        execution.start(root,"The submitted grid is preserved. Use effective depth min(rows,columns)="+n+"; unused padding never becomes a valid state.");
        execution.done(root,cost(0,0,triangle,n,execution));
    }
    private int cost(int row,int col,int[][] triangle,int n,RecursiveDpExecution execution) {
        return execution.evaluate(new RecursiveDpExecution.State(0,row,col,"row="+row+",col="+col),row==n-1,
                "Compare the two real children directly below and below-right; add this cell to the cheaper returned cost.",()->{
                    if(row==n-1)return triangle[row][col];
                    int left=cost(row+1,col,triangle,n,execution);
                    int right=cost(row+1,col+1,triangle,n,execution);
                    return triangle[row][col]+Math.min(left,right);
                });
    }
}
