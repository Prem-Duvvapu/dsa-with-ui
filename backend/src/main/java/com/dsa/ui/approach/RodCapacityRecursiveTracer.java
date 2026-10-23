package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

final class RodCapacityRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    RodCapacityRecursiveTracer(boolean memo){this.memo=memo;}
    public String id(){return "rod-cutting-problem";}
    public DsType dsType(){return DsType.RECURSION_TREE;}
    public String annotatedCode(){return SolutionSource.read("/solutions/dp-approaches/"+id()+"/"+(memo?"memoization":"recursion")+".java");}
    public InputSpec inputSpec(){return InputSpec.of(InputField.of("prices",FieldType.INT_ARRAY).label("Prices by piece length")
            .length(1,memo?10:9).values(0,100).defaultValue(List.of(2,5,7,8,10))
            .help("Position i gives the price of length i+1; rod length is the number of prices. Nonnegative prices, including zero. "
                    +(memo?"Up to 10 prices bounds the copied memo table.":"Up to 9 prices keeps the complete tree below 220 calls.")).build());}
    public Map<String,Object> alternateInput(){return Map.of("prices",List.of(3,5,8,9));}
    public void run(Inputs in,StepEmitter emit){
        int[] prices=in.getIntArray("prices");int n=prices.length;
        var execution=new RecursiveLongDpExecution(emit,memo,RecursiveDpExecution.labels("n=",n+1),RecursiveDpExecution.labels("l=",n+1));
        var root=state(n,n);execution.start(root,"Maximize revenue within rod length "+n+" using piece lengths 1.."+n+"; pieces can repeat.");
        execution.done(root,best(n,n,prices,execution));
    }
    private long best(int n,int length,int[] prices,RecursiveLongDpExecution execution){
        return execution.evaluate(state(n,length),n==0||length==0,"Skip piece length "+n+" first, then take it if it fits; the same length remains available.",()->{
            if(n==0||length==0)return 0L;
            long skip=best(n-1,length,prices,execution);
            long take=n<=length?prices[n-1]+best(n,length-n,prices,execution):0L;
            return Math.max(skip,take);
        });
    }
    private static RecursiveLongDpExecution.State state(int n,int length){return new RecursiveLongDpExecution.State(n,length,"n="+n+",l="+length);}
}
