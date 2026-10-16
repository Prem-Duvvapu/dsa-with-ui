package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** The same predecessor-energy recurrence as tabulation, with up to K real children. */
final class FrogJumpKRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    FrogJumpKRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "frog-jump-k-distance"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id() + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("heights", FieldType.INT_ARRAY).label("Stair heights")
                .length(2, memo ? 16 : 8).values(0,999).defaultValue(List.of(10,30,40,20,50))
                .help(memo ? "Cache the minimum energy for each stair, including zero." : "Up to 8 heights keeps every call visible even when K reaches 8.").build(),
                InputField.of("k", FieldType.INT).label("Max jump distance").range(1,8).defaultValue(3)
                        .help("Only predecessors that exist and are within K stairs are evaluated.").build());
    }
    public Map<String,Object> alternateInput() { return Map.of("heights",List.of(50,20,60,30,10,40,20),"k",2); }
    public void run(Inputs in, StepEmitter emit) {
        int[] heights=in.getIntArray("heights"); int k=in.getInt("k");
        var execution=new RecursiveDpExecution(emit,memo,1,List.of("minimum energy"),RecursiveDpExecution.labels("i=",heights.length),null,"energy");
        var root=state(heights.length-1);
        execution.start(root,"Find minimum energy from stair 0; each call weighs all existing predecessors within K="+k+".");
        execution.done(root,solve(heights.length-1,heights,k,execution));
    }
    private int solve(int i,int[] heights,int k,RecursiveDpExecution execution) {
        return execution.evaluate(state(i),i==0,"Evaluate each legal predecessor i-d for 1 <= d <= min(K,i); add its jump cost before minimizing.",()->{
            if(i==0) return 0;
            int best=Integer.MAX_VALUE;
            for(int d=1;d<=k && d<=i;d++) best=Math.min(best,solve(i-d,heights,k,execution)+Math.abs(heights[i]-heights[i-d]));
            return best;
        });
    }
    private static RecursiveDpExecution.State state(int i) { return new RecursiveDpExecution.State(0,0,i,"i="+i); }
}
