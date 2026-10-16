package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public final class CircularActivityObstaclesProvider implements SolutionApproachProvider {
    private record Lesson(String state, String base, String recurrence, String recursiveOrder, String tableOrder,
                          String key, String recursionTime, String recursionReason, String memoTime, String memoReason,
                          String memoSpace, String stackSpace) {}
    public List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog) {
        var out = new ArrayList<SolutionApproach>();
        add(out, canonical, new CircularRobberRecursiveTracer(false), new CircularRobberRecursiveTracer(true), new Lesson(
                "best(pass,n): best non-adjacent sum in the first n houses of one allowed range",
                "best(pass,0)=0; best(pass,1)=nums[pass]",
                "best(pass,n)=max(best(pass,n-1), nums[pass+n-1]+best(pass,n-2)); answer=max(best(0,N-1),best(1,N-1))",
                "First exclude the last house (pass 0), then the first house (pass 1). Two independent root calls form two trees; the driver itself is not a recursive call.",
                "Keep the unchanged two rows: houses 0..N-2 and 1..N-1. For n>0 the memo prefix maps to canonical endpoint i=pass+n-1. Fill each range left-to-right, then take the better endpoint.",
                "(pass,n) for this fixed nums; n is the prefix count, and identical counts from different ranges are NOT the same subproblem",
                "O(2^N)", "Two take/skip recursion trees repeatedly evaluate their own prefixes; this is an upper bound.",
                "O(N)", "Each prefix of each of two ranges is computed at most once.", "O(N)", "O(N)"));
        add(out, canonical, new TrainingRecursiveTracer(false), new TrainingRecursiveTracer(true), new Lesson(
                "score(day,task): best merit total through day, ending on that activity",
                "score(0,task)=points[0][task]",
                "score(day,task)=points[day][task]+max(score(day-1,other) for other != task); answer=max over final tasks",
                "Evaluate ending tasks 0,1,2 as three separate roots. A call explores only the two different previous activities, in increasing task order.",
                "Seed day 0's three activities. Fill later days from the two different previous columns; reduce over all three last-day activities.",
                "(day,task); unknown is null and zero is a reusable score",
                "O(2^D)", "Three roots each have at most two predecessor calls per level across D days.",
                "O(D)", "Three states per day, with two predecessor comparisons per state.", "O(D)", "O(D)"));
        add(out, canonical, new ObstaclePathsRecursiveTracer(false), new ObstaclePathsRecursiveTracer(true), new Lesson(
                "paths(row,col): number of open right/down paths from (0,0) to this cell",
                "A blocked cell has 0 paths; an open origin has 1. Missing neighbours contribute 0 without calls.",
                "For any other open cell, paths(row,col)=paths(row-1,col) if row>0 plus paths(row,col-1) if col>0",
                "Check obstacles before the origin. Recurse above then left only when those coordinates exist; a blocked endpoint stops immediately.",
                "Fill rows then columns, exclude obstacles, seed the open origin and add already-known above/left values. Input obstacles are not computed counts.",
                "(row,col) for this fixed obstacle map; cache zero for blocked and unreachable states",
                "O(2^(R+C))", "At most two calls per depth; obstacles prune branches and depth is bounded by R+C.",
                "O(R * C)", "Each reached coordinate is computed at most once from at most two neighbours.", "O(R * C)", "O(R + C)"));
        return List.copyOf(out);
    }
    private static void add(List<SolutionApproach> out, TracerRegistry canonical, AlgorithmTracer recursion, AlgorithmTracer memo, Lesson lesson) {
        for (boolean cached : List.of(false,true)) {
            var teaching = new ApproachTeaching(lesson.state, lesson.base, lesson.recurrence, lesson.recursiveOrder
                    + (cached ? " Check the complete key before expanding and store each known result." : " Repeated states are distinct calls."), cached ? lesson.key : null,
                    cached ? Map.of("base","Store the true base value, including zero.","cache-hit","Return this exact cached state without children.","store","Dependencies returned; store the computed result.")
                            : Map.of("base","Return the declared base value without children.","combine","Combine only the actual returned dependencies."), Map.of());
            out.add(new SolutionApproach(recursion.id(),cached?"memoization":"recursion",cached?"Memoization":"Recursion",
                    cached?"Reuse complete recursive states, including known zero.":"Explore the actual recursive choices and repeated states.",false,
                    new ApproachComplexity(cached?lesson.memoTime:lesson.recursionTime,cached?lesson.memoReason:lesson.recursionReason,
                            cached?"Cached recurrence":"Branching recursion",cached?lesson.memoSpace:lesson.stackSpace,
                            cached?"Actual nullable memo storage plus the active recursive stack.":"Only the active recursive stack; no result cache.",
                            cached?"Memo and stack":"Call stack",cached?lesson.memoSpace:lesson.stackSpace,cached?"Nullable memo table":"No result cache"),
                    cached?memo:recursion).withTeaching(teaching));
        }
        out.add(new SolutionApproach(recursion.id(),"canonical","Tabulation","Fill the unchanged bottom-up recurrence in dependency order.",true,
                new ApproachComplexity(lesson.memoTime,lesson.memoReason,"Bottom-up recurrence",lesson.memoSpace,"Actual full DP table; no recursive call stack.","DP table",lesson.memoSpace,"Computed states"),
                canonical.find(recursion.id()).orElseThrow()).withTeaching(new ApproachTeaching(lesson.state,lesson.base,lesson.recurrence,lesson.tableOrder,null,Map.of(),Map.of())));
    }
}
