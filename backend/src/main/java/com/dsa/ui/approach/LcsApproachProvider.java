package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.TracerRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** The same prefix-length recurrence, evaluated recursively or bottom up. */
@Component
public final class LcsApproachProvider implements SolutionApproachProvider {
    private static final String ID = "longest-common-subsequence";
    private static final String STATE = "lcs(i,j): the longest common subsequence length of the first i "
            + "characters of first and the first j characters of second. M and N are the string lengths.";
    private static final String BASE = "An empty prefix has no common subsequence characters: "
            + "lcs(0,j)=lcs(i,0)=0.";
    private static final String RECURRENCE = "Equal final characters contribute 1+lcs(i-1,j-1). "
            + "Otherwise return max(lcs(i-1,j),lcs(i,j-1)); dropping one final character explores "
            + "the two alternatives. The result is a length, not a reconstructed sequence.";

    @Override
    public List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog) {
        var tabulation = new SolutionApproach(ID, "canonical", "Tabulation",
                "Fill prefix lengths in dependency order, preserving the existing solution.", true,
                ApproachComplexity.from(catalog.find(ID).orElseThrow().getProblem().getComplexity()),
                canonical.find(ID).orElseThrow())
                .withTeaching(new ApproachTeaching(STATE, BASE, RECURRENCE,
                        "Seed empty-prefix row and column with zero; increase i then j. "
                                + "The diagonal, previous row and previous column are already available.",
                        null, Map.of(), Map.of("init", BASE, "fill", RECURRENCE,
                                "done", "Read the length for both complete strings at dp[M][N].")));
        return List.of(alternative(false), alternative(true), tabulation);
    }

    private static SolutionApproach alternative(boolean memo) {
        var complexity = memo
                ? new ApproachComplexity("O((M + 1) * (N + 1))",
                    "Evaluate each reachable prefix pair once, with at most two dependencies per state.",
                    "Cached prefix states", "O((M + 1) * (N + 1))",
                    "The nullable prefix table plus an O(M + N) active stack; inputs and trace snapshots are excluded.",
                    "Memo table and stack", "O((M + 1) * (N + 1))", "Nullable memo table")
                : new ApproachComplexity("O(2^(M + N))",
                    "A conservative binary-tree bound; each child shortens at least one prefix. Matches take only the diagonal.",
                    "Repeated prefix calls", "O(M + N)",
                    "Only the active recursive path; no result cache. Inputs and trace snapshots are excluded.",
                    "Call stack", "O(M + N)", "No result cache");
        Map<String, String> events = memo
                ? Map.of("base", BASE, "branch", RECURRENCE,
                    "store", "Store the result for this exact prefix pair, including a known zero.",
                    "cache-hit", "Return a known prefix result without expanding children; null alone means uncomputed.")
                : Map.of("base", BASE, "branch", RECURRENCE,
                    "combine", "Return the actual diagonal or maximum of the returned child lengths; no result is cached.");
        Map<String, String> anchors = memo
                ? Map.of("base", BASE, "branch", RECURRENCE,
                    "combine", "Store and return the computed length for (i,j).",
                    "hit", "Reuse the cached result for (i,j), including zero.")
                : Map.of("base", BASE, "branch", RECURRENCE,
                    "combine", "Return the computed length for (i,j).");
        var teaching = new ApproachTeaching(STATE, BASE, RECURRENCE,
                "On a mismatch, shorten first before shortening second; a match uses only the diagonal. "
                        + (memo ? "Reuse the complete prefix-pair key." : "Expand every actual repeated call."),
                memo ? "(i,j) for these fixed strings; null is unknown and zero is a known length." : null,
                events, anchors);
        return new SolutionApproach(ID, memo ? "memoization" : "recursion",
                memo ? "Memoization" : "Recursion",
                memo ? "Reuse prefix-pair lengths, including known zero results."
                        : "Explore real prefix choices and see repeated subproblems.",
                false, complexity, new LcsRecursiveTracer(memo)).withTeaching(teaching);
    }
}
