package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.TracerRegistry;
import org.springframework.stereotype.Component;
import java.util.List;

/** Actual recursive forms alongside the unchanged canonical tabulated executable. */
@Component
public final class ClimbingStairsApproachProvider implements SolutionApproachProvider {
    @Override public List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog) {
        String id = "climbing-stairs";
        return List.of(new SolutionApproach(id, "recursion", "Recursion",
                "Expand the two choices recursively, including repeated states.", false,
                new ApproachComplexity("O(2^N)", "Each non-base call branches twice; repeated states are recomputed.",
                        "Branching recursion", "O(N)", "At most N active recursive frames; no memo table.",
                        "Recursive call stack", "O(N)", "No cached results"), new ClimbingStairsRecursiveTracer(false)),
                new SolutionApproach(id, "memoization", "Memoization",
                        "Use the same recursive choices, returning cached results on repeat visits.", false,
                        new ApproachComplexity("O(N)", "At most N+1 states are computed, each from two children.",
                                "Cached recursion", "O(N)", "Memo has N+1 entries and the recursive stack has at most N frames.",
                                "Memo plus recursive stack", "O(N)", "Memo array"), new ClimbingStairsRecursiveTracer(true)),
                new SolutionApproach(id, "tabulation", "Tabulation",
                "Fill each stair's count from the two earlier stairs.", true,
                ApproachComplexity.from(catalog.find(id).orElseThrow().getProblem().getComplexity()),
                canonical.find(id).orElseThrow()));
    }
}
