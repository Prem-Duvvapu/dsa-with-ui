package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.TracerRegistry;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

/** Preserve the canonical tabulation and its public id while adding actual energy recurrences. */
@Component
public final class FrogJumpApproachProvider implements SolutionApproachProvider {
    @Override public List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog) {
        String id = "frog-jump";
        return List.of(new SolutionApproach(id, "recursion", "Recursion",
                "Evaluate both legal predecessor costs recursively, including repeated states.", false,
                new ApproachComplexity("O(2^N)", "Two predecessor branches repeatedly evaluate the same indices.",
                        "Branching recursion", "O(N)", "At most N recursive frames, without a result cache.",
                        "Recursive call stack", "O(N)", "No cached energy values"), new FrogJumpRecursiveTracer(false))
                        .withTeaching(teaching(false, false)),
                new SolutionApproach(id, "memoization", "Memoization",
                        "Use the same recursive minimum-energy recurrence, reusing known costs including zero.", false,
                        new ApproachComplexity("O(N)", "Each of N energy states is computed once from at most two predecessors.",
                                "Cached recursion", "O(N)", "N memo entries plus at most N active recursive frames.",
                                "Memo plus recursive stack", "O(N)", "Memo energy array"), new FrogJumpRecursiveTracer(true))
                        .withTeaching(teaching(true, false)),
                new SolutionApproach(id, "canonical", "Tabulation",
                        "Fill the unchanged energy table from smaller stair indices.", true,
                        ApproachComplexity.from(catalog.find(id).orElseThrow().getProblem().getComplexity()),
                        canonical.find(id).orElseThrow()).withTeaching(teaching(false, true)));
    }
    private static ApproachTeaching teaching(boolean memo, boolean table) {
        return new ApproachTeaching("energy(i): minimum total energy to reach stair i",
                "energy(0) = 0; stair 1 has only predecessor 0, costing |h[1]-h[0]|",
                "energy(i) = min(energy(i-1) + |h[i]-h[i-1]|, energy(i-2) + |h[i]-h[i-2]|) for i >= 2",
                table ? "Start with energy[0]=0. Fill indices 1 through n-1 from already-computed predecessors."
                        : "Visit predecessor i-1 before i-2 (only when i>1); compare total costs, not just height gaps."
                            + (memo ? " Check memo before expanding; store a computed minimum for later calls." : " Repeated indices remain distinct calls."),
                memo ? "i for this fixed heights input; unknown is null, and zero is a valid cached cost" : null,
                table ? Map.of() : memo ? Map.of("base", "The start costs zero. Store this known zero in memo.",
                        "cache-hit", "Reuse the recorded minimum energy, including zero; do not expand children.",
                        "store", "Every reachable candidate cost has returned; cache the minimum for this index.")
                        : Map.of("base", "The start costs zero; no predecessor is called.",
                            "combine", "Choose the smaller reachable total cost; stair 1 has only one candidate."),
                table ? Map.of("base", "Only stair 0 is initialized as known zero; later energy cells remain unknown.",
                        "evaluate", "Compare the reachable predecessor totals; a two-step jump is absent at stair 1.",
                        "done", "All energy dependencies were evaluated in increasing index order; read the final cost.") : Map.of());
    }
}
