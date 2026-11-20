package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.TracerRegistry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** All three forms preserve the existing LCS-with-reversal length formulation. */
@Component
public final class LpsApproachProvider implements SolutionApproachProvider {
    private static final String ID = "longest-palindromic-subsequence";
    private static final String STATE = "For fixed text and reversed=reverse(text), lps(i,j) is the LCS length "
            + "of their prefixes of lengths i and j. These are two prefix lengths, not interval endpoints. N is text.length.";
    private static final String BASE = "An empty prefix has common subsequence length zero: lps(0,j)=lps(i,0)=0.";
    private static final String RECURRENCE = "Equal final characters contribute 1+lps(i-1,j-1). Otherwise take "
            + "max(lps(i-1,j),lps(i,j-1)). At (N,N), LCS length against the reversal equals the longest "
            + "palindromic subsequence length. This returns a length, not a reconstructed sequence.";

    @Override public List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog) {
        var tabulation = new SolutionApproach(ID, "canonical", "Tabulation",
                "Fill the prefix LCS table against the reversed text, preserving the existing solution.", true,
                ApproachComplexity.from(catalog.find(ID).orElseThrow().getProblem().getComplexity()),
                canonical.find(ID).orElseThrow())
                .withTeaching(new ApproachTeaching(STATE, BASE, RECURRENCE,
                        "Reverse once. Seed empty-prefix row and column with zero, then increase i and j; "
                                + "the diagonal, previous row and previous column are already available.",
                        null, Map.of(), Map.of("init", BASE, "fill", RECURRENCE,
                                "done", "Read the length for both complete prefixes at dp[N][N].")));
        return List.of(alternative(false), alternative(true), tabulation);
    }

    private static SolutionApproach alternative(boolean memo) {
        var complexity = memo
                ? new ApproachComplexity("O((N + 1)^2)",
                    "Evaluate each reachable prefix pair once, with at most two dependencies per state.",
                    "Cached prefix states", "O((N + 1)^2)",
                    "The nullable square table plus O(N) active stack and reversed text; trace snapshots are excluded.",
                    "Memo table, stack and reversal", "O((N + 1)^2)", "Nullable memo table")
                : new ApproachComplexity("O(2^(2N))",
                    "A conservative binary-tree bound; each child shortens at least one of the two length-N prefixes. "
                            + "Matches take only the diagonal.",
                    "Repeated prefix calls", "O(N)",
                    "The active recursive path and a reversed copy of the text; no result cache. Trace snapshots are excluded.",
                    "Call stack and reversal", "O(N)", "No result cache");
        Map<String, String> events = memo
                ? Map.of("base", BASE, "branch", RECURRENCE,
                    "store", "Store the length for this exact pair of text/reversed prefixes, including known zero.",
                    "cache-hit", "Return a known prefix result without expanding children; null alone means uncomputed.")
                : Map.of("base", BASE, "branch", RECURRENCE,
                    "combine", "Return the actual diagonal or maximum of child lengths; no result is cached.");
        Map<String, String> anchors = memo
                ? Map.of("base", BASE, "branch", RECURRENCE,
                    "combine", "Store and return the computed prefix length for (i,j).",
                    "hit", "Reuse the cached prefix length for (i,j), including zero.")
                : Map.of("base", BASE, "branch", RECURRENCE,
                    "combine", "Return the computed prefix length for (i,j).");
        var teaching = new ApproachTeaching(STATE, BASE, RECURRENCE,
                "Reverse text once. On a mismatch shorten text before shortening reversed; a match uses only the diagonal. "
                        + (memo ? "Reuse the complete prefix-pair key." : "Expand every actual repeated call."),
                memo ? "(i,j) for this fixed text and reversal; null is unknown and zero is a known length." : null,
                events, anchors);
        return new SolutionApproach(ID, memo ? "memoization" : "recursion",
                memo ? "Memoization" : "Recursion",
                memo ? "Reuse lengths for the text and reversed prefixes, including known zero results."
                        : "Explore actual repeated prefix calls against the reversed text.",
                false, complexity, new LpsRecursiveTracer(memo)).withTeaching(teaching);
    }
}
