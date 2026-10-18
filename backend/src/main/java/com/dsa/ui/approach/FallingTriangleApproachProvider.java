package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public final class FallingTriangleApproachProvider implements SolutionApproachProvider {
    private record Lesson(String state,String base,String recurrence,String order,String tableOrder,String key,
                          String recursionTime,String stack,String memoTime,String memoSpace) {}
    public List<SolutionApproach> approaches(TracerRegistry canonical,ProblemCatalog catalog) {
        var result=new ArrayList<SolutionApproach>();
        add(result,canonical,new FallingPathRecursiveTracer(false),new FallingPathRecursiveTracer(true),new Lesson(
                "cost(row,col): minimum path cost from any top-row start to this cell",
                "cost(0,col)=matrix[0][col]; missing predecessor columns do not generate calls",
                "cost(row,col)=matrix[row][col]+min(cost(row-1,col), valid upper-left, valid upper-right); answer=min over bottom columns",
                "Evaluate bottom columns left-to-right as separate real roots. Within each call evaluate directly above, upper-left, upper-right when valid. Negative and zero costs are ordinary results.",
                "Seed every top-row cell, then fill increasing row/column from already-known predecessors. Reduce over the entire bottom row, not one corner.",
                "(row,col) for this fixed matrix; null is unknown, never -1 or zero","O(C * 3^R)","O(R)","O(R * C)","O(R * C)"));
        add(result,canonical,new TrianglePathRecursiveTracer(false),new TrianglePathRecursiveTracer(true),new Lesson(
                "cost(row,col): minimum remaining cost from this triangle cell to the effective base",
                "N=min(input rows,input columns); cost(N-1,col)=triangle[N-1][col]",
                "cost(row,col)=triangle[row][col]+min(cost(row+1,col),cost(row+1,col+1)); answer=cost(0,0)",
                "Evaluate below then below-right. Preserve the full submitted rectangular input while ignoring entries outside the same effective triangle as canonical. # cells are excluded, not unknown or computed.",
                "Seed the effective base, then fill rows upward. Both child cells are already settled; only col<=row is a valid state. Answer is the apex.",
                "(row,col) for this fixed input and effective depth; null is unknown, including when the true cost is -1 or zero","O(2^N)","O(N)","O(N^2)","O(N^2)"));
        return List.copyOf(result);
    }
    private static void add(List<SolutionApproach> result,TracerRegistry canonical,AlgorithmTracer recursion,AlgorithmTracer memo,Lesson lesson) {
        for(boolean cached:List.of(false,true)){
            var teaching=new ApproachTeaching(lesson.state,lesson.base,lesson.recurrence,lesson.order+(cached?" Check/store the complete state, including negative/zero results.":" Repeated states are separate calls."),cached?lesson.key:null,
                    cached?Map.of("base","Store the actual signed base value.","cache-hit","Return this known result with no children.","store","Store the minimum after the real dependencies return.")
                            :Map.of("base","Return the real cell cost without children.","combine","Add this cell to the cheapest returned dependency."),Map.of());
            result.add(new SolutionApproach(recursion.id(),cached?"memoization":"recursion",cached?"Memoization":"Recursion",
                    cached?"Reuse signed path costs, with unknown separate from zero and -1.":"Explore actual path choices and repeated states.",false,
                    new ApproachComplexity(cached?lesson.memoTime:lesson.recursionTime,cached?"Compute each valid state once from at most three dependencies.":"A branching upper bound for the real root calls and their recursive paths.",
                            cached?"Cached recurrence":"Branching paths",cached?lesson.memoSpace:lesson.stack,
                            cached?"Actual nullable table plus the active recursive stack.":"Only the active recursive path; no result cache.",cached?"Memo and stack":"Call stack",cached?lesson.memoSpace:lesson.stack,cached?"Nullable memo table":"No result cache"),cached?memo:recursion).withTeaching(teaching));
        }
        result.add(new SolutionApproach(recursion.id(),"canonical","Tabulation","Keep the unchanged bottom-up dependency order.",true,
                new ApproachComplexity(lesson.memoTime,"Compute each valid state once from already-known neighbours.","Bottom-up recurrence",lesson.memoSpace,"The actual full table; no recursive stack.","DP table",lesson.memoSpace,"Computed states"),
                canonical.find(recursion.id()).orElseThrow()).withTeaching(new ApproachTeaching(lesson.state,lesson.base,lesson.recurrence,lesson.tableOrder,null,Map.of(),Map.of())));
    }
}
