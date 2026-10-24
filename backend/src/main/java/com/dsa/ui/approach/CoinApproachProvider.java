package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public final class CoinApproachProvider implements SolutionApproachProvider {
    public List<SolutionApproach> approaches(TracerRegistry canonical,ProblemCatalog catalog){
        var result=new ArrayList<SolutionApproach>();
        add(result,canonical,new MinimumCoinsRecursiveTracer(false),new MinimumCoinsRecursiveTracer(true),true);
        add(result,canonical,new CoinCountRecursiveTracer(false),new CoinCountRecursiveTracer(true),false);
        return List.copyOf(result);
    }
    private static void add(List<SolutionApproach> result,TracerRegistry canonical,AlgorithmTracer recursion,AlgorithmTracer memo,boolean minimum){
        String state=(minimum?"fewest":"ways")+"(n,a): "+(minimum?"fewest coins":"number of denomination combinations")+" for exact amount a using the first n denominations; N=coins.length, A=amount, m=minimum positive coin.";
        String base=minimum?"a=0 returns 0; n=0 with positive a returns INFINITY (1000000). Only the final public answer maps unreachable to -1.":"a=0 returns 1 (the empty quantity vector); n=0 with positive a returns 0.";
        String recurrence=minimum?"skip=fewest(n-1,a); eligible take=1+fewest(n,a-coins[n-1]), preserving INFINITY for an unreachable dependency; return min(skip,take).":
                "skip=ways(n-1,a); eligible take=ways(n,a-coins[n-1]); return skip+take. Taking keeps n, so coins can repeat; the prefix prevents counting permutations.";
        for(boolean cached:List.of(false,true)){
            var notes=new HashMap<String,String>();notes.put("base",base);notes.put("combine",recurrence);
            if(cached)notes.put("hit",minimum?"Known zero and known INFINITY are reusable; null alone is uncomputed.":"A known zero combination count is reusable; null alone is uncomputed.");
            var teaching=new ApproachTeaching(state,base,recurrence,"Skip before take; input order is preserved. "+(cached?"Reuse the complete (n,a) state.":"Expand each real repeated call, with no cache."),
                    cached?"(n,a) for this fixed denomination array; null is uncomputed, "+(minimum?"INFINITY is known unreachable and zero is known cost":"zero is a known count"):null,notes,Map.of());
            result.add(new SolutionApproach(recursion.id(),cached?"memoization":"recursion",cached?"Memoization":"Recursion",
                    cached?"Reuse exact-amount states, including known "+(minimum?"unreachable results.":"zero counts."):"Explore real skip/reuse choices for "+(minimum?"the fewest coins.":"combination counts."),false,
                    new ApproachComplexity(cached?"O(N * (A + 1))":"O(2^(N + A/m))",cached?"Each complete prefix/amount state has at most two dependencies.":"Conservative binary-tree bound: at most N skips and A/m takes on an active path.",
                            cached?"Cached recurrence":"Skip/reuse choices",cached?"O(N * (A + 1))":"O(N + A/m)",cached?"Nullable memo table plus active recursive stack.":"Only the active path, without a result cache.",cached?"Memo and stack":"Call stack",
                            cached?"O(N * (A + 1))":"O(N + A/m)",cached?"Nullable memo table":"No result cache"),cached?memo:recursion).withTeaching(teaching));
        }
        result.add(new SolutionApproach(recursion.id(),"canonical","Tabulation","Keep the unchanged bottom-up exact-amount recurrence.",true,
                new ApproachComplexity("O(N * (A + 1))","Fill the full prefix/amount table.","Bottom-up recurrence","O(N * (A + 1))","Full table, without a recursive stack.","DP table","O(N * (A + 1))","Computed states"),
                canonical.find(recursion.id()).orElseThrow()).withTeaching(new ApproachTeaching(state,base,recurrence,"Seed zero-amount and zero-denomination bases; fill increasing prefix and amount. Take reads the same row.",null,Map.of(),Map.of())));
    }
}
