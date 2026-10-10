package com.dsa.ui.approach;

import com.dsa.ui.catalog.ProblemCatalog;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;
import java.util.*;

/** Owner-requested recurrence families; existing canonical executables and links remain intact. */
@Component
public final class RequestedDpApproachProvider implements SolutionApproachProvider {
    @Override public List<SolutionApproach> approaches(TracerRegistry canonical, ProblemCatalog catalog) {
        List<SolutionApproach> result = new ArrayList<>();
        add(result, canonical, catalog, new LisRecursiveTracer(false), new LisRecursiveTracer(true),
                "best(i,p): longest increasing subsequence in suffix i, with previous taken index p-1 (p=0 means none)",
                "best(n,p)=0: no elements remain",
                "best(i,p)=max(best(i+1,p), 1+best(i+1,i+1) when p=0 or nums[i]>nums[p-1])",
                "Skip first; take only a strictly larger value. Tabulation fills i from n-1 down to 0, with p≤i.",
                "(i,p) for this fixed array; both indices are required", "O(2^N)", "O(N²)", "O(N)", "O(N²)",
                "Tabulation", "Fill suffix/previous-index states bottom-up.");
        add(result, canonical, catalog, new StockFeeRecursiveTracer(false), new StockFeeRecursiveTracer(true),
                "profit(day,holding): maximum future profit; holding=0 allows buying, holding=1 allows selling",
                "profit(n,holding)=0: no trading days remain",
                "profit(day,0)=max(skip,-price+profit(day+1,1)); profit(day,1)=max(skip,price-fee+profit(day+1,0))",
                "Evaluate skip then trade; the fee is charged once on a sale. Tabulation fills days backward from the terminal row.",
                "(day,holding) for these fixed prices and fee", "O(2^N)", "O(N)", "O(N)", "O(N)",
                "Tabulation", "Fill both buy/sell states backward; charge the fee on each sale.");
        add(result, canonical, catalog, new PartitionDifferenceRecursiveTracer(false), new PartitionDifferenceRecursiveTracer(true),
                "ways(items,sum): number of indexed subsets of the first items whose sum is sum; target=(total+d)/2",
                "Reject impossible parity/difference with answer 0; ways(0,0)=1 and ways(0,sum>0)=0",
                "ways(items,sum)=ways(items-1,sum)+(value≤sum ? ways(items-1,sum-value) : 0)",
                "Skip then take. Each index is used at most once; equal values at different indices remain different choices. Tabulation fills increasing item count.",
                "(items,sum) for this fixed positive array and derived target", "O(2^N)", "O(N×T)", "O(N)", "O(N×T)",
                "Tabulation", "Count indexed subset choices in increasing item-count order; T=(total+d)/2.");
        int ninjaStart = result.size();
        add(result, canonical, catalog, new NinjaFriendsRecursiveTracer(false), new NinjaFriendsRecursiveTracer(true),
                "chocolates(row,col1,col2): best collection from both robot positions through the last row",
                "Last row returns its two cell values, counting a shared cell only once",
                "chocolates(r,c1,c2)=collected(r,c1,c2)+max(chocolates(r+1,c1+d1,c2+d2)) over legal d1,d2∈{-1,0,1}",
                "Try legal move pairs in d1/d2 order. Memo companion shows the current row slice only. Rolling tabulation fills every column pair from the last row upward, reading the completed row below.",
                "(row,col1,col2): all three coordinates; the visible slice does not discard cached other rows",
                "O(9^R)", "O(R×C²)", "O(R)", "O(R×C²)",
                "Rolling tabulation (existing)", "Evaluate the same 3D recurrence bottom-up, retaining only adjacent row slices (O(C²) table space).");
        var ninjaTeaching = result.get(ninjaStart).teaching();
        result.add(result.size() - 1, new SolutionApproach("ninja-and-his-friends", "tabulation", "Tabulation",
                "Fill the complete 3D table from memoization bottom-up; inspect one labelled row slice at a time.", false,
                complexity("O(R×C²)", "O(R×C²)"), new NinjaFriendsTabulationTracer()).withTeaching(new ApproachTeaching(
                ninjaTeaching.state(), ninjaTeaching.baseCases(), ninjaTeaching.recurrence(),
                "Initialize every last-row column pair. Fill rows upward; all nine legal dependencies are in the already-computed row below. All slices remain stored.",
                null, Map.of(), Map.of("base", "Count a shared cell once in the last row.", "rowStart", "The row below is complete; keep it and every other computed slice.",
                        "fill", "Evaluate every legal move pair, then write this 3D state. Reads are in the next-row slice, not the visible slice.",
                        "done", "Read dp[0][0][cols-1] in the full 3D table."))));

        String id = "min-insertions-palindrome";
        var recursion = new PalindromeInsertionTracer("recursion");
        var memo = new PalindromeInsertionTracer("memoization");
        var table = new PalindromeInsertionTracer("tabulation");
        String state = "insertions(left,right): minimum characters to insert to make this interval a palindrome";
        String bases = "left≥right needs 0 insertions, including empty and single-character intervals";
        String recurrence = "equal ends: insertions(left+1,right-1); unequal ends: 1+min(insertions(left+1,right),insertions(left,right-1))";
        String order = "Recurse into shorter intervals; memoize the same pair. Tabulation increases interval length so every dependency is already known.";
        result.add(alternative(recursion, false, state, bases, recurrence, order, "(left,right); memo column right+1 also represents empty intervals", "O(2^N)", "O(N)"));
        result.add(alternative(memo, true, state, bases, recurrence, order, "(left,right); memo column right+1 also represents empty intervals", "O(N²)", "O(N²)"));
        result.add(new SolutionApproach(id, "tabulation", "Tabulation", "Fill the same minimum-insertion interval recurrence by increasing length.",
                false, complexity("O(N²)", "O(N²)"), table).withTeaching(new ApproachTeaching(state, bases, recurrence, order,
                null, Map.of(), Map.of("base", "Single characters need no insertions; lower-triangle intervals are not table states.",
                        "fill", "Both shorter dependencies are already known. Equal ends use only the inner interval.",
                        "done", "Read the whole interval [0,n-1]."))));
        result.add(new SolutionApproach(id, "canonical", "LCS with reverse (existing)",
                "Preserve the existing solution: minimum insertions = length − longest palindromic subsequence (LCS with reverse).", true,
                ApproachComplexity.from(catalog.find(id).orElseThrow().getProblem().getComplexity()), canonical.find(id).orElseThrow())
                .withTeaching(new ApproachTeaching("lcs(i,j): LCS of text prefix i and reversed-text prefix j",
                        "An empty prefix has LCS length 0", "Equal characters: 1+diagonal; otherwise max(up,left); answer=n−lcs(n,n)",
                        "Fill increasing prefix lengths; this is an equivalent LCS formulation, not the direct interval table.", null,
                        Map.of(), Map.of("init", "Allocate the prefix LCS table against the reversed text.", "fill", "Compare the two prefix end characters.",
                        "done", "Subtract the longest palindromic subsequence length from text length."))));
        return List.copyOf(result);
    }

    private static void add(List<SolutionApproach> result, TracerRegistry canonical, ProblemCatalog catalog,
                            AlgorithmTracer recursion, AlgorithmTracer memo, String state, String bases, String recurrence,
                            String order, String key, String recursionTime, String memoTime, String recursionSpace, String memoSpace,
                            String canonicalLabel, String canonicalSummary) {
        result.add(alternative(recursion, false, state, bases, recurrence, order, key, recursionTime, recursionSpace));
        result.add(alternative(memo, true, state, bases, recurrence, order, key, memoTime, memoSpace));
        String id = recursion.id();
        result.add(new SolutionApproach(id, "canonical", canonicalLabel, canonicalSummary, true,
                ApproachComplexity.from(catalog.find(id).orElseThrow().getProblem().getComplexity()), canonical.find(id).orElseThrow())
                .withTeaching(new ApproachTeaching(state, bases, recurrence, order, null, Map.of(), Map.of())));
    }

    private static SolutionApproach alternative(AlgorithmTracer tracer, boolean memo, String state, String bases,
                                                String recurrence, String order, String key, String time, String space) {
        return new SolutionApproach(tracer.id(), memo ? "memoization" : "recursion", memo ? "Memoization" : "Recursion",
                memo ? "Execute the same recursive state, caching known answers including zero." : "Execute every legal recursive branch; repeated states remain distinct calls.",
                false, complexity(time, space), tracer).withTeaching(new ApproachTeaching(state, bases, recurrence, order,
                memo ? key : null, memo ? Map.of("base", "Evaluate and store the actual base value, including zero.",
                        "cache-hit", "Reuse this full state's stored answer; no recursive children are called.",
                        "store", "All dependencies returned; store this answer under the full state key.")
                        : Map.of("base", "The recurrence stops here with its actual base value.",
                        "combine", "Combine this call's actual returned dependencies; do not cache them."), Map.of()));
    }

    private static ApproachComplexity complexity(String time, String space) {
        return new ApproachComplexity(time, "Worst-case recurrence work; N is input length, R/C are grid dimensions, T is the subset target.",
                "Counts state/branch evaluations, not trace steps or animation speed.", space,
                "Includes the result cache/table, when present, and the active recursive stack; excludes stored visualization snapshots.",
                "Algorithm working memory", space, "Recursive result cache or bottom-up table where applicable");
    }
}
