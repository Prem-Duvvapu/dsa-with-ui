package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Edge cells have exactly one path; interior calls combine their two actual neighbours. */
final class GridPathsRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    GridPathsRecursiveTracer(boolean memo) { this.memo=memo; }
    public String id() { return "grid-unique-paths"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/"+id()+"/"+(memo?"memoization":"recursion")+".java"); }
    public InputSpec inputSpec() {
        String help=memo?"Cache paths(row,col); unknown cells remain distinct from known counts.":"Up to 4 rows and 4 columns keeps every recursive call visible.";
        return InputSpec.of(InputField.of("rows",FieldType.INT).label("Rows").range(1,memo?8:4).defaultValue(3).help(help).build(),
                InputField.of("cols",FieldType.INT).label("Columns").range(1,memo?8:4).defaultValue(3).help(help).build());
    }
    public Map<String,Object> alternateInput() { return Map.of("rows",4,"cols",3); }
    public void run(Inputs in,StepEmitter emit) {
        int rows=in.getInt("rows"),cols=in.getInt("cols");
        var execution=new RecursiveDpExecution(emit,memo,1,RecursiveDpExecution.labels("row=",rows),RecursiveDpExecution.labels("col=",cols),null,"paths");
        var root=state(rows-1,cols-1);
        execution.start(root,"Count paths from (0,0), moving right or down; each edge cell has exactly one path.");
        execution.done(root,solve(rows-1,cols-1,execution));
    }
    private int solve(int row,int col,RecursiveDpExecution execution) {
        return execution.evaluate(state(row,col),row==0||col==0,"Paths into this cell come from above or left; these two disjoint arrival choices add.",()->{
            if(row==0||col==0) return 1;
            return solve(row-1,col,execution)+solve(row,col-1,execution);
        });
    }
    private static RecursiveDpExecution.State state(int row,int col) { return new RecursiveDpExecution.State(0,row,col,"row="+row+",col="+col); }
}
