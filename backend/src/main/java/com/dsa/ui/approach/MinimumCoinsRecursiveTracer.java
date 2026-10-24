package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

final class MinimumCoinsRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    MinimumCoinsRecursiveTracer(boolean memo){this.memo=memo;}
    public String id(){return "minimum-coins-dp";}
    public DsType dsType(){return DsType.RECURSION_TREE;}
    public String annotatedCode(){return SolutionSource.read("/solutions/dp-approaches/"+id()+"/"+(memo?"memoization":"recursion")+".java");}
    public InputSpec inputSpec(){return InputSpec.of(
            InputField.of("coins",FieldType.INT_ARRAY).label("Coin denominations").length(1,8).values(1,25).defaultValue(List.of(1,2,5))
                    .help("Unlimited reuse; duplicate denominations are allowed. "+(memo?"Memo needs (n+1) × (amount+1) ≤ 80 cells.":"The complete tree must fit 220 calls; coin values and amount affect its size.")).build(),
            InputField.of("amount",FieldType.INT).label("Target amount").range(1,40).defaultValue(11).help("The exact amount to make with the fewest coins; -1 means unreachable.").build());}
    public Map<String,Object> alternateInput(){return Map.of("coins",List.of(2),"amount",3);}
    public void run(Inputs in,StepEmitter emit){
        int[] coins=in.getIntArray("coins");int amount=in.getInt("amount");CoinTraceBounds.requireFits(coins,amount,memo);
        var execution=new RecursiveMinimumDpExecution(emit,memo,coins.length,amount);var root=state(coins.length,amount);
        execution.start(root,"Find the fewest coins for exact amount "+amount+"; denominations can be reused. ∞ means unreachable, not uncomputed.");
        execution.done(root,fewest(coins.length,amount,coins,execution));
    }
    private int fewest(int n,int a,int[] coins,RecursiveMinimumDpExecution execution){
        return execution.evaluate(state(n,a),n==0||a==0,"Skip the current denomination first; if it fits, take one coin and keep that denomination available.",()->{
            if(a==0)return 0;if(n==0)return RecursiveMinimumDpExecution.INFINITY;
            int skip=fewest(n-1,a,coins,execution),take=RecursiveMinimumDpExecution.INFINITY;
            if(coins[n-1]<=a){int reused=fewest(n,a-coins[n-1],coins,execution);if(reused<RecursiveMinimumDpExecution.INFINITY)take=1+reused;}
            return Math.min(skip,take);
        });
    }
    private static RecursiveMinimumDpExecution.State state(int n,int a){return new RecursiveMinimumDpExecution.State(n,a,"n="+n+",a="+a);}
}
