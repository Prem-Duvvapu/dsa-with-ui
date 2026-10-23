package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public final class CapacityApproachProvider implements SolutionApproachProvider {
    public List<SolutionApproach> approaches(TracerRegistry canonical,ProblemCatalog catalog){
        var result=new ArrayList<SolutionApproach>();
        for(var kind:CapacityRecursiveTracer.Kind.values())add(result,canonical,new CapacityRecursiveTracer(kind,false),new CapacityRecursiveTracer(kind,true),kind.reuse,false);
        add(result,canonical,new RodCapacityRecursiveTracer(false),new RodCapacityRecursiveTracer(true),true,true);
        return List.copyOf(result);
    }
    private static void add(List<SolutionApproach> result,TracerRegistry canonical,AlgorithmTracer recursion,AlgorithmTracer memo,boolean reuse,boolean rod){
        String state=rod?"best(n,l): maximum revenue within remaining length l using piece lengths 1..n; N=prices.length, initially n=l=N. Prices are nonnegative.":
                "best(n,w): maximum value within capacity w using the first n "+(reuse?"item types; each can repeat":"indexed items; each can be taken once")+". N=weights.length, C=capacity, m=minimum positive weight.";
        String base=rod?"best(0,l)=best(n,0)=0; no types or no length gives zero revenue, not proof of an exact fill.":"best(0,w)=best(n,0)=0; no items or no capacity gives zero value.";
        String take=rod?"prices[n-1]+best(n,l-n)":"values[n-1]+best("+(reuse?"n":"n-1")+",w-weights[n-1])";
        String recurrence="Evaluate skip=best(n-1,"+(rod?"l":"w")+") first; eligible take="+take+"; return max(skip,take). "+(reuse?"Taking keeps the current type available.":"Taking removes that item, so it cannot be reused.");
        String recursiveTime=rod?"O(2^(T + L))":reuse?"O(2^(N + C/m))":"O(2^N)";
        String recursiveSpace=rod?"O(T + L)":reuse?"O(N + C/m)":"O(N)";
        String table=rod?"O(N^2)":"O(N * (C + 1))";
        String bound=rod?"Conservative binary-tree bound: T piece types and L remaining length, initially T=L=N; each child decreases T or L.":reuse?
                "Conservative binary-tree bound: at most N skips and C/m takes on an active path (positive weights).":"At most a binary decision tree over N indexed items.";
        for(boolean cached:List.of(false,true)){
            var notes=new HashMap<String,String>();notes.put("base",base);notes.put("combine","Combine both returned choices with max; "+(reuse?"taking may reuse the same type.":"taking uses only preceding items."));
            if(cached)notes.put("hit","A complete state with known zero revenue/value is reusable; null alone means unknown.");
            var teaching=new ApproachTeaching(state,base,recurrence,
                    "Skip before take. "+(cached?"Cache the complete "+(rod?"(n,l)":"(n,w)")+" state; known zero is not unknown.":"Expand every real repeated call; no results are cached.")
                            +(rod?" Length1 is available and prices are nonnegative, so the final optimum can sell the whole rod; no-type substates need not fill their length.":" Paired arrays preserve their index order."),
                    cached?(rod?"(n,l) for this fixed price list":"(n,w) for these fixed paired items")+"; null means unknown, not zero":null,notes,Map.of());
            result.add(new SolutionApproach(recursion.id(),cached?"memoization":"recursion",cached?"Memoization":"Recursion",
                    cached?"Reuse complete capacity states, including known zero.":"Explore real skip/take choices "+(reuse?"with reusable types.":"without reusing an item."),false,
                    new ApproachComplexity(cached?table:recursiveTime,cached?"At most one evaluation per prefix/capacity state, at most two dependencies each.":bound,
                            cached?"Cached recurrence":"Skip/take choices",cached?table:recursiveSpace,
                            cached?"Nullable memo table plus the active recursive stack.":"Only the active recursive path; no result cache.",cached?"Memo and stack":"Call stack",
                            cached?table:recursiveSpace,cached?"Nullable memo table":"No result cache"),cached?memo:recursion).withTeaching(teaching));
        }
        result.add(new SolutionApproach(recursion.id(),"canonical","Tabulation","Keep the unchanged bottom-up capacity recurrence.",true,
                new ApproachComplexity(table,"Fill the full prefix/capacity table.","Bottom-up recurrence",table,"The full table, with no recursive stack.","DP table",table,"Computed states"),
                canonical.find(recursion.id()).orElseThrow()).withTeaching(new ApproachTeaching(state,base,recurrence,
                        "Seed zero-item and zero-capacity bases; fill increasing item prefix and capacity. Take reads the "+(reuse?"same":"preceding")+" row. "
                                +(rod?"Answer is dp[N][N]; prices are nonnegative and length1 is available.":"Answer is dp[N][C]; unused capacity is allowed."),null,Map.of(),Map.of())));
    }
}
