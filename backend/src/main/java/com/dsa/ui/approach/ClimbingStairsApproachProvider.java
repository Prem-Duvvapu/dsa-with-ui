package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.TracerRegistry;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/** Actual recursive forms alongside the unchanged canonical tabulated executable. */
@Component
public final class ClimbingStairsApproachProvider implements SolutionApproachProvider {
    @Override public List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog) {
        String id = "climbing-stairs";
        return List.of(new SolutionApproach(id, "recursion", "Recursion",
                "Expand the two choices recursively, including repeated states.", false,
                new ApproachComplexity("O(2^N)", "Each non-base call branches twice; repeated states are recomputed.",
                        "Branching recursion", "O(N)", "At most N active recursive frames; no memo table.",
                        "Recursive call stack", "O(N)", "No cached results"), new ClimbingStairsRecursiveTracer(false))
                        .withTeaching(teaching(false, false)),
                new SolutionApproach(id, "memoization", "Memoization",
                        "Use the same recursive choices, returning cached results on repeat visits.", false,
                        new ApproachComplexity("O(N)", "At most N+1 states are computed, each from two children.",
                                "Cached recursion", "O(N)", "Memo has N+1 entries and the recursive stack has at most N frames.",
                                "Memo plus recursive stack", "O(N)", "Memo array"), new ClimbingStairsRecursiveTracer(true))
                                .withTeaching(teaching(true, false)),
                new SolutionApproach(id, "tabulation", "Tabulation",
                "Fill each stair's count from the two earlier stairs.", true,
                ApproachComplexity.from(catalog.find(id).orElseThrow().getProblem().getComplexity()),
                canonical.find(id).orElseThrow()).withTeaching(teaching(false, true)));
    }
    private static ApproachTeaching teaching(boolean memo, boolean table) {
        return new ApproachTeaching("ways(k): the number of ways to reach stair k", "ways(0) = ways(1) = 1",
                "ways(k) = ways(k-1) + ways(k-2)", table
                ? "Initialize stairs 0 and 1, then fill k = 2 through n. Both earlier cells are already known."
                : "Visit k-1 before k-2, then add their returned values." + (memo
                    ? " Check memo[k] before expanding; store a computed result for later visits."
                    : " Repeated states are evaluated again; each visit is a distinct call."),
                memo ? "k, the remaining stair index; an unknown entry is not zero" : null,
                table ? Map.of() : memo ? Map.of(
                    "base", "This base state has one way; record it in memo.",
                    "cache-hit", "Return the recorded memo value without expanding children.",
                    "store", "Both child results have returned; store their sum under this state key.") : Map.of(
                    "base", "No further branch is needed for this base state.",
                    "combine", "Both child calls returned; add their values without caching this result."),
                table ? Map.of("base", "Initialize the two base cells to one, not every cell to zero.",
                    "combine", "Read the two earlier cells; the current cell shows their candidate sum.",
                    "done", "All dependencies were filled in increasing stair order; extract the answer cell.") : Map.of());
    }
}
