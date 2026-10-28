package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Two explicit knapsack problems, distinguished by the real take dependency. */
final class CapacityRecursiveTracer implements AlgorithmTracer {
    enum Kind {
        SINGLE_USE("knapsack-01",false), UNBOUNDED("unbounded-knapsack",true);
        final String id;
        final boolean reuse;
        Kind(String id,boolean reuse){this.id=id;this.reuse=reuse;}
    }
    private final Kind kind;
    private final boolean memo;
    CapacityRecursiveTracer(Kind kind,boolean memo){this.kind=kind;this.memo=memo;}
    public String id(){return kind.id;}
    public DsType dsType(){return DsType.RECURSION_TREE;}
    public String annotatedCode(){return SolutionSource.read("/solutions/dp-approaches/"+id()+"/"+(memo?"memoization":"recursion")+".java");}
    public InputSpec inputSpec(){
        return InputSpec.of(
                InputField.of("weights",FieldType.INT_ARRAY).label("Weights").length(1,8).values(1,20)
                        .defaultValue(kind.reuse?List.of(2,4,6):List.of(1,2,4,5))
                        .help("Paired by index with values. "+(kind.reuse?"Item types can repeat. ":"Each indexed item can be taken once. ")
                                +(memo?"Memo needs (n+1) × (capacity+1) ≤ 80 cells.":"The complete tree must fit 220 calls; weights and capacity affect its size. Reduce items/capacity or increase weights if refused.")).build(),
                InputField.of("values",FieldType.INT_ARRAY).label("Values").length(1,8).values(1,100)
                        .defaultValue(kind.reuse?List.of(5,11,13):List.of(5,4,8,6)).build(),
                InputField.of("capacity",FieldType.INT).label("Capacity").range(1,30).defaultValue(kind.reuse?10:5).build())
                .withEqualLengthGroup("weights","values")
                .withWorkLimit(memo ? new InputWorkLimit(InputWorkLimit.Kind.CAPACITY_TABLE_CELLS,
                        "weights","capacity",80,false,"weights",
                        "Memo tracing needs (n+1) × (capacity+1) ≤ 80 cells; use fewer paired items or a smaller capacity.")
                        : new InputWorkLimit(InputWorkLimit.Kind.SKIP_TAKE_CALLS,
                        "weights","capacity",220,kind.reuse,"weights",
                        "This input needs more than 220 recursive calls. Use fewer paired items, a smaller capacity or larger weights, or select Memoization."));
    }
    public Map<String,Object> alternateInput(){return kind.reuse?Map.of("weights",List.of(3),"values",List.of(10),"capacity",8):Map.of("weights",List.of(2,3),"values",List.of(4,5),"capacity",4);}
    public void run(Inputs in,StepEmitter emit){
        int[] weights=in.getIntArray("weights"),values=in.getIntArray("values");int capacity=in.getInt("capacity");
        var execution=new RecursiveDpExecution(emit,memo,1,RecursiveDpExecution.labels("n=",weights.length+1),RecursiveDpExecution.labels("w=",capacity+1),null,"best");
        var root=state(weights.length,capacity);
        execution.start(root,"Maximize value within capacity "+capacity+"; "+(kind.reuse?"each type may repeat.":"each indexed item may be taken once."));
        execution.done(root,best(weights.length,capacity,weights,values,execution));
    }
    private int best(int n,int w,int[] weights,int[] values,RecursiveDpExecution execution){
        return execution.evaluate(state(n,w),n==0||w==0,"Skip the current item first; take it if it fits, "+(kind.reuse?"keeping its type available.":"removing that indexed item from further choices."),()->{
            if(n==0||w==0)return 0;
            int skip=best(n-1,w,weights,values,execution);
            int take=weights[n-1]<=w?values[n-1]+best(kind.reuse?n:n-1,w-weights[n-1],weights,values,execution):0;
            return Math.max(skip,take);
        });
    }
    private static RecursiveDpExecution.State state(int n,int w){return new RecursiveDpExecution.State(0,n,w,"n="+n+",w="+w);}
}
