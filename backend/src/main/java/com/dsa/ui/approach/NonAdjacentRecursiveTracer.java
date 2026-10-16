package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** A prefix-length state makes the empty prefix explicit without invalid cache indices. */
final class NonAdjacentRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    NonAdjacentRecursiveTracer(boolean memo) { this.memo=memo; }
    public String id() { return "max-sum-non-adjacent"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/"+id()+"/"+(memo?"memoization":"recursion")+".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("nums",FieldType.INT_ARRAY).label("Values").length(1,memo?20:10).values(0,999)
                .defaultValue(List.of(2,1,4,9,4,1,8))
                .help(memo?"Cache best(count) for the complete prefix; zero is a computed value.":"Up to 10 values keeps all take/skip calls visible. Adjacent picks remain forbidden.").build());
    }
    public Map<String,Object> alternateInput() { return Map.of("nums",List.of(5,5,10,100,10,5)); }
    public void run(Inputs in,StepEmitter emit) {
        int[] nums=in.getIntArray("nums");
        var execution=new RecursiveDpExecution(emit,memo,1,List.of("best sum"),RecursiveDpExecution.labels("count=",nums.length+1),null,"best");
        var root=state(nums.length);
        execution.start(root,"best(count) is the maximum non-adjacent sum in the first count values; an empty prefix has sum 0.");
        execution.done(root,solve(nums.length,nums,execution));
    }
    private int solve(int count,int[] nums,RecursiveDpExecution execution) {
        return execution.evaluate(state(count),count<=1,"Skip the last value using best(count-1), or take it plus best(count-2), never its neighbour.",()->{
            if(count<=1) return count==0?0:nums[0];
            int skip=solve(count-1,nums,execution);
            int take=nums[count-1]+solve(count-2,nums,execution);
            return Math.max(skip,take);
        });
    }
    private static RecursiveDpExecution.State state(int count) { return new RecursiveDpExecution.State(0,0,count,"count="+count); }
}
