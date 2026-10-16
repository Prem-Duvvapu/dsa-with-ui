package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** Authored recurrences, not an id-based generic executable or substituted trace. */
@Component
public final class LinearGridApproachProvider implements SolutionApproachProvider {
    public List<SolutionApproach> approaches(TracerRegistry canonical,ProblemCatalog catalog) {
        var result=new ArrayList<SolutionApproach>();
        add(result,canonical,new FrogJumpKRecursiveTracer(false),new FrogJumpKRecursiveTracer(true),
                "energy(i): minimum total energy to reach stair i", "energy(0)=0",
                "energy(i)=min over d=1..min(K,i) of energy(i-d)+|heights[i]-heights[i-d]|",
                "Evaluate legal predecessors from nearest to farthest; take the minimum total energy.",
                "Fill increasing i from 0 to N-1; each of its at most K predecessors is already settled.",
                "i for the fixed heights and K; a known zero is not unknown", "O(2^N)",
                "There are at most 2^(N-1) calls when every earlier stair is reachable; smaller K prunes branches.",
                "O(N * K)","Compute each stair once and inspect at most K predecessors.","O(N)");
        add(result,canonical,new NonAdjacentRecursiveTracer(false),new NonAdjacentRecursiveTracer(true),
                "best(count): maximum non-adjacent sum in the first count values", "best(0)=0; best(1)=nums[0]",
                "best(count)=max(best(count-1), nums[count-1]+best(count-2)) for count>=2",
                "Evaluate skip first, then take; taking the last value excludes its adjacent predecessor.",
                "The existing best[i] represents best(i+1). Fill increasing i; read i-1 and, when present, i-2.",
                "count for this fixed values input; unknown is null, not zero", "O(2^N)",
                "Take/skip recursion repeatedly evaluates overlapping prefixes; O(2^N) is an upper bound.",
                "O(N)","Each prefix is computed once from at most two smaller prefixes.","O(N)");
        add(result,canonical,new GridPathsRecursiveTracer(false),new GridPathsRecursiveTracer(true),
                "paths(row,col): number of right/down paths from (0,0) to this cell", "paths(0,col)=1; paths(row,0)=1",
                "paths(row,col)=paths(row-1,col)+paths(row,col-1) for row,col>0",
                "Evaluate the above cell then the left cell; edge cells need no predecessor calls.",
                "Fill rows top-to-bottom and columns left-to-right; above and left are already known.",
                "(row,col) for this fixed grid shape", "O(2^(R+C))",
                "At most two branches per level and at most R+C-1 active frames; edge bases prune branches.",
                "O(R * C)","Each reached grid coordinate is computed at most once from at most two neighbours.","O(R + C)");
        return List.copyOf(result);
    }
    private static void add(List<SolutionApproach> out,TracerRegistry canonical,AlgorithmTracer recursive,AlgorithmTracer memoized,
            String state,String base,String recurrence,String recursiveOrder,String tableOrder,String cacheKey,
            String recursionTime,String recursionReason,String cachedTime,String cachedReason,String stackSpace) {
        String id=recursive.id(); boolean grid=id.equals("grid-unique-paths");
        String tableSpace=grid?"O(R * C)":"O(N)";
        for(boolean memo:List.of(false,true)) {
            var teaching=new ApproachTeaching(state,base,recurrence,recursiveOrder+(memo?" Check the cache before expanding; store each computed result.":" Repeated states are distinct calls."),
                    memo?cacheKey:null,memo?Map.of("base","Store the known base value, including zero where applicable.",
                            "cache-hit","Reuse the exact recorded state without descending.","store","All dependencies returned; store the computed result.")
                            :Map.of("base","Return the declared base value without children.","combine","Combine returned dependencies using this recurrence."),Map.of());
            out.add(new SolutionApproach(id,memo?"memoization":"recursion",memo?"Memoization":"Recursion",
                    memo?"Reuse computed states in the same recursive recurrence.":"Expose the actual recursive choices and repeated states.",false,
                    new ApproachComplexity(memo?cachedTime:recursionTime,memo?cachedReason:recursionReason,
                            memo?"Cached recurrence":"Branching recursion",memo?tableSpace:stackSpace,
                            memo?"Memo entries plus the active recursive stack; unknown is null.":"Only the active recursive stack, without a result cache.",
                            memo?"Memo and recursive stack":"Recursive stack",memo?tableSpace:stackSpace,memo?"Result cache":"No result cache"),
                    memo?memoized:recursive).withTeaching(teaching));
        }
        out.add(new SolutionApproach(id,"canonical","Tabulation","Fill the unchanged table in dependency order.",true,
                new ApproachComplexity(cachedTime,cachedReason,"Bottom-up recurrence",tableSpace,"The actual complete table; no recursive stack.","DP table",tableSpace,"Computed states"),
                canonical.find(id).orElseThrow()).withTeaching(new ApproachTeaching(state,base,recurrence,tableOrder,null,Map.of(),Map.of())));
    }
}
