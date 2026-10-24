package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

final class CoinCountRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    CoinCountRecursiveTracer(boolean memo){this.memo=memo;}
    public String id(){return "coin-change-2";}
    public DsType dsType(){return DsType.RECURSION_TREE;}
    public String annotatedCode(){return SolutionSource.read("/solutions/dp-approaches/"+id()+"/"+(memo?"memoization":"recursion")+".java");}
    public InputSpec inputSpec(){return InputSpec.of(
            InputField.of("coins",FieldType.INT_ARRAY).label("Coin denominations").length(1,8).values(1,25).distinct().defaultValue(List.of(1,2,5))
                    .help("Distinct denominations with unlimited reuse. "+(memo?"Memo needs (n+1) × (amount+1) ≤ 80 cells.":"The complete tree must fit 220 calls; coin values and amount affect its size.")).build(),
            InputField.of("amount",FieldType.INT).label("Target amount").range(1,40).defaultValue(5).help("Count combinations, not permutations, summing to exactly this amount.").build());}
    public Map<String,Object> alternateInput(){return Map.of("coins",List.of(3),"amount",5);}
    public void run(Inputs in,StepEmitter emit){
        int[] coins=in.getIntArray("coins");int amount=in.getInt("amount");CoinTraceBounds.requireFits(coins,amount,memo);
        var execution=new RecursiveDpExecution(emit,memo,1,RecursiveDpExecution.labels("n=",coins.length+1),RecursiveDpExecution.labels("a=",amount+1),null,"ways");
        var root=state(coins.length,amount);
        execution.start(root,"Count denomination combinations for exact amount "+amount+"; taking a coin keeps its denomination available.");
        execution.done(root,ways(coins.length,amount,coins,execution));
    }
    private int ways(int n,int a,int[] coins,RecursiveDpExecution execution){
        return execution.evaluate(state(n,a),n==0||a==0,"Skip the current denomination first; if it fits, take one coin and reuse that denomination. Add the two combination counts.",()->{
            if(a==0)return 1;if(n==0)return 0;
            int skip=ways(n-1,a,coins,execution),take=coins[n-1]<=a?ways(n,a-coins[n-1],coins,execution):0;
            return skip+take;
        });
    }
    private static RecursiveDpExecution.State state(int n,int a){return new RecursiveDpExecution.State(0,n,a,"n="+n+",a="+a);}
}
