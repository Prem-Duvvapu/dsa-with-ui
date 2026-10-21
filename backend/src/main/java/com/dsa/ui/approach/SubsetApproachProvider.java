package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public final class SubsetApproachProvider implements SolutionApproachProvider {
    public List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog) {
        var result = new ArrayList<SolutionApproach>();
        for (var kind : SubsetFeasibilityRecursiveTracer.Kind.values())
            add(result, canonical, new SubsetFeasibilityRecursiveTracer(kind, false), new SubsetFeasibilityRecursiveTracer(kind, true), false,
                    kind == SubsetFeasibilityRecursiveTracer.Kind.EQUAL_PARTITION);
        add(result, canonical, new SubsetCountRecursiveTracer(false), new SubsetCountRecursiveTracer(true), true, false);
        return List.copyOf(result);
    }
    private static void add(List<SolutionApproach> result, TracerRegistry canonical, AlgorithmTracer recursion, AlgorithmTracer memo,
                            boolean counting, boolean partition) {
        String function = counting ? "count" : "possible";
        String state = function + "(n,s): " + (counting ? "number of indexed subsets of the first n items reaching exactly sum s" : "whether any subset of the first n items reaches exactly sum s")
                + ". N=nums.length; S=" + (partition ? "total sum/2 after the parity check" : counting ? "k" : "target");
        String base = counting ? "count(0,0)=1; count(0,s>0)=0 (only the empty subset exists)" : "possible(0,s)=(s==0)";
        String recurrence = "skip=" + function + "(n-1,s); take=" + function + "(n-1,s-nums[n-1]) only if the item fits; combine with " + (counting ? "addition" : "boolean OR");
        String reduction = partition ? "First sum all values. An odd sum returns false without a call or memo table; otherwise target=sum/2. " : "";
        for (boolean cached : List.of(false, true)) {
            var notes = new HashMap<String, String>();
            notes.put("base", counting ? "One empty subset reaches zero, otherwise no subset exists." : "Return the actual boolean for zero items.");
            notes.put("combine", counting ? "Add counts from the two indexed choices." : "OR the two actual boolean choices after both eligible children return.");
            if (cached) notes.put("hit", counting ? "A known zero is reusable, not unknown." : "A known false is reusable, not unknown.");
            if (partition) notes.put("odd", "An odd total cannot be split equally; no recursive call is invented.");
            var teaching = new ApproachTeaching(state, base, recurrence, reduction
                    + "Evaluate skip then take when eligible. Keep duplicate indices separate. "
                    + (cached ? "Reuse the complete (n,s) state, including " + (counting ? "zero counts." : "false results.") : "Repeated states are separate real calls."),
                    cached ? "(n,s) for this fixed indexed input; null means unknown, not " + (counting ? "zero" : "false") : null, notes, Map.of());
            result.add(new SolutionApproach(recursion.id(), cached ? "memoization" : "recursion", cached ? "Memoization" : "Recursion",
                    cached ? "Reuse complete subset states, including " + (counting ? "zero counts." : "false results.") : "Explore the real skip/take choices for each indexed item.", false,
                    new ApproachComplexity(cached ? "O(N * (S + 1))" : "O(2^N)",
                            (cached ? "At most (N+1)*(S+1) states, at most two dependencies each." : "A binary decision tree over N items.")
                                    + (partition ? " The parity preflight takes O(N); an odd total requires no subset states." : ""),
                            cached ? "Cached recurrence" : "Subset choices", cached ? "O(N * (S + 1))" : "O(N)",
                            cached ? "The actual nullable memo table plus the active recursive stack." : "Only the active path; no result cache.",
                            cached ? "Memo and stack" : "Call stack", cached ? "O(N * (S + 1))" : "O(N)", cached ? "Nullable memo table" : "No result cache"),
                    cached ? memo : recursion).withTeaching(teaching));
        }
        result.add(new SolutionApproach(recursion.id(), "canonical", "Tabulation", "Keep the unchanged bottom-up subset recurrence.", true,
                new ApproachComplexity("O(N * (S + 1))", "Fill all prefix/sum cells in the unchanged canonical table."
                        + (partition ? " The parity preflight takes O(N); an odd total requires no subset states." : ""), "Bottom-up recurrence",
                        "O(N * (S + 1))", "The actual full prefix/sum table, including column zero; no recursive stack.", "DP table", "O(N * (S + 1))", "Computed states"),
                canonical.find(recursion.id()).orElseThrow()).withTeaching(new ApproachTeaching(state, base, recurrence,
                        reduction + "Seed the zero-item row, then fill increasing prefix length and sums 0..S. Read only the preceding row; answer is dp[N][S].", null, Map.of(), Map.of())));
    }
}
