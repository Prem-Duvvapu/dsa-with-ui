package com.dsa.ui.service;

import com.dsa.ui.algorithm.dp.Knapsack01;
import com.dsa.ui.model.*;
import com.dsa.ui.trace.ListTraceRecorder;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DpService {

    private final Map<String, ProblemDetail> problems = new LinkedHashMap<>();

    public DpService() {
        initProblems();
    }

    public List<ProblemDetail> getAllProblems() {
        return new ArrayList<>(problems.values());
    }

    public ProblemDetail getProblemById(String id) {
        return problems.get(id);
    }

    public List<ExecutionStep> generateSteps(String problemId) {
        switch (problemId) {
            case "climbing-stairs": return generateClimbingStairsSteps();
            case "frog-jump": return generateFrogJumpSteps();
            case "knapsack-01": return generateKnapsackSteps();
            case "longest-common-subsequence": return generateLcsSteps();
            default: return generateClimbingStairsSteps();
        }
    }

    private void initProblems() {
        // 1. Climbing Stairs
        problems.put("climbing-stairs", new ProblemDetail(
            "climbing-stairs", "Climbing Stairs (1D DP)", "DP - Basic DP", "Dynamic Programming", "Easy",
            "Find total distinct ways to climb N stairs taking 1 or 2 steps.",
            """
            // Java Climbing Stairs (LeetCode 70)
            public int climbStairs(int n) {
                if (n <= 1) return 1;
                int prev2 = 1, prev = 1;
                for (int i = 2; i <= n; i++) {
                    int curi = prev + prev2;
                    prev2 = prev; prev = curi;
                }
                return prev;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 1, 2, 3, 5, 8}, -1, -1), null, null, null,
            new ComplexityDetail("O(N)", "Time Complexity: 1D DP loop runs N times.", "1D DP", "O(1)", "Space Complexity: Space-optimized space.", "O(1) Memory", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 2. Frog Jump
        problems.put("frog-jump", new ProblemDetail(
            "frog-jump", "Frog Jump (Min Energy 1D DP)", "DP - Basic DP", "Dynamic Programming", "Easy",
            "Find min energy for frog to reach stair N-1 jumping 1 or 2 stairs.",
            """
            // Java Frog Jump (Striver A2Z Sheet)
            public int frogJump(int n, int heights[]) {
                int prev = 0, prev2 = 0;
                for (int i = 1; i < n; i++) {
                    int jumpOne = prev + Math.abs(heights[i] - heights[i - 1]);
                    int jumpTwo = (i > 1) ? prev2 + Math.abs(heights[i] - heights[i - 2]) : Integer.MAX_VALUE;
                    int curi = Math.min(jumpOne, jumpTwo);
                    prev2 = prev; prev = curi;
                }
                return prev;
            }
            """,
            null, null, null, createArrayState(new int[]{10, 20, 30, 10}, -1, -1), null, null, null,
            new ComplexityDetail("O(N)", "Time Complexity: Single pass DP loop.", "1D DP", "O(1)", "Space Complexity: Constant memory.", "Memory", "Auxiliary Space: O(1)", "Memory"), "Array"
        ));

        // 3. 0/1 Knapsack
        problems.put("knapsack-01", new ProblemDetail(
            "knapsack-01", "0/1 Knapsack Problem", "DP - Subsequences", "Dynamic Programming", "Medium",
            "Maximize total value of items in knapsack of capacity W.",
            """
            // Java 0/1 Knapsack DP
            public int knapsack(int[] wt, int[] val, int n, int W) {
                int[] dp = new int[W + 1];
                for (int i = 0; i < n; i++) {
                    for (int w = W; w >= wt[i]; w--) {
                        dp[w] = Math.max(dp[w], val[i] + dp[w - wt[i]]);
                    }
                }
                return dp[W];
            }
            """,
            null, null, null, createArrayState(new int[]{0, 0, 10, 10, 15, 25}, -1, -1), null, null, null,
            new ComplexityDetail("O(N * W)", "Time Complexity: N items x Capacity W table.", "2D/1D DP", "O(W)", "Space Complexity: 1D array space-optimized DP table.", "DP Array", "Auxiliary Space: O(W)", "Memory"), "Array"
        ));

        // 4. Longest Common Subsequence
        problems.put("longest-common-subsequence", new ProblemDetail(
            "longest-common-subsequence", "Longest Common Subsequence", "DP - Strings", "Dynamic Programming", "Medium",
            "Find length of longest common subsequence between text1 and text2.",
            """
            // Java LCS DP (LeetCode 1143)
            public int longestCommonSubsequence(String text1, String text2) {
                int m = text1.length(), n = text2.length();
                int[][] dp = new int[m + 1][n + 1];
                for (int i = 1; i <= m; i++) {
                    for (int j = 1; j <= n; j++) {
                        if (text1.charAt(i - 1) == text2.charAt(j - 1)) dp[i][j] = 1 + dp[i - 1][j - 1];
                        else dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                    }
                }
                return dp[m][n];
            }
            """,
            null, null, null, null, null, null, createDefaultDpGrid(),
            new ComplexityDetail("O(M * N)", "Time Complexity: Table size M x N.", "2D String DP", "O(M * N)", "Space Complexity: 2D DP matrix table.", "Matrix", "Auxiliary Space: O(M * N)", "Memory"), "Matrix"
        ));

        // Bulk register remaining 50 DP problems
        populateRemainingDpProblems();
    }

    private void populateRemainingDpProblems() {
        String[][] list = new String[][]{
            {"frog-jump-k-distance", "Frog Jump with K Distances", "DP - Basic DP", "Medium", "Min energy for frog to jump at most K steps."},
            {"max-sum-non-adjacent", "Max Sum of Non-Adjacent Elements", "DP - Basic DP", "Medium", "Maximize sum of subsequence with no two adjacent elements picked."},
            {"house-robber-2", "House Robber II", "DP - Basic DP", "Medium", "Rob houses arranged in a circle (first & last adjacent)."},
            {"ninjas-training", "Ninja's Training", "DP - Grids", "Medium", "Maximize Ninja's merit points over N days with 3 activities."},
            {"grid-unique-paths", "Grid Unique Paths", "DP - Grids", "Medium", "Count unique paths from top-left (0,0) to bottom-right (m-1,n-1)."},
            {"unique-paths-2", "Unique Paths II", "DP - Grids", "Medium", "Grid unique paths with obstacles grid[i][j] == 1."},
            {"minimum-falling-path-sum", "Minimum Falling Path Sum", "DP - Grids", "Medium", "Find min falling path sum from top row to bottom row."},
            {"triangle-min-path-sum", "Triangle Minimum Path Sum", "DP - Grids", "Medium", "Find min path sum from top to bottom of triangle grid."},
            {"ninja-and-his-friends", "Ninja and His Friends (3D DP)", "DP - Grids", "Hard", "Maximize chocolates collected by 2 friends starting at (0,0) and (0,c-1)."},
            {"subset-sum-equal-target", "Subset Sum Equal to Target", "DP - Subsequences", "Medium", "Check if there exists a subset with sum equal to target K."},
            {"partition-equal-subset-sum", "Partition Equal Subset Sum", "DP - Subsequences", "Medium", "Partition array into 2 subsets of equal sum totalSum / 2."},
            {"partition-set-min-abs-diff", "Partition Set Min Absolute Sum Difference", "DP - Subsequences", "Hard", "Partition set into 2 subsets minimizing |sum1 - sum2|."},
            {"count-subsets-with-sum-k", "Count Subsets with Sum K", "DP - Subsequences", "Medium", "Count total subsets whose sum equals target K."},
            {"count-partitions-given-diff", "Count Partitions with Given Difference", "DP - Subsequences", "Medium", "Count partitions s1 - s2 = D using subset sum K = (total + D)/2."},
            {"assign-cookies-dp", "Assign Cookies DP", "DP - Subsequences", "Easy", "Maximize cookie assignment satisfaction using DP."},
            {"minimum-coins-dp", "Minimum Coins (Coin Change 1)", "DP - Subsequences", "Medium", "Min coins needed to make target amount X."},
            {"target-sum-dp", "Target Sum", "DP - Subsequences", "Medium", "Assign '+' and '-' signs to elements to reach target sum."},
            {"coin-change-2", "Coin Change 2 (Ways to Make Amount)", "DP - Subsequences", "Medium", "Count total combinations to make target sum using unlimited coins."},
            {"unbounded-knapsack", "Unbounded Knapsack", "DP - Subsequences", "Medium", "Maximize value in knapsack with infinite item reuse."},
            {"rod-cutting-problem", "Rod Cutting Problem", "DP - Subsequences", "Medium", "Maximize value by cutting rod of length N into pieces."},
            {"print-longest-common-subsequence", "Print Longest Common Subsequence", "DP - Strings", "Medium", "Reconstruct and print the actual LCS string using DP table backtrack."},
            {"longest-common-substring", "Longest Common Substring", "DP - Strings", "Medium", "Find length of longest contiguous common substring."},
            {"longest-palindromic-subsequence", "Longest Palindromic Subsequence", "DP - Strings", "Medium", "LCS of string s and its reverse reverse(s)."},
            {"min-insertions-palindrome", "Min Insertions to Make String Palindrome", "DP - Strings", "Medium", "Min insertions = length(s) - Longest Palindromic Subsequence."},
            {"min-insertions-deletions-a-b", "Min Insertions/Deletions Convert A to B", "DP - Strings", "Medium", "Convert A to B = (lenA - LCS) deletions + (lenB - LCS) insertions."},
            {"shortest-common-supersequence", "Shortest Common Supersequence", "DP - Strings", "Hard", "Find shortest supersequence string containing both A and B as subsequences."},
            {"distinct-subsequences", "Distinct Subsequences", "DP - Strings", "Hard", "Count distinct occurrences of string T as a subsequence of string S."},
            {"edit-distance", "Edit Distance", "DP - Strings", "Hard", "Min operations (insert, delete, replace) to convert word1 to word2."},
            {"wildcard-matching", "Wildcard Matching", "DP - Strings", "Hard", "Match string with pattern containing '?' and '*' wildcard characters."},
            {"best-time-stock-1", "Best Time to Buy & Sell Stock I", "DP - Stocks", "Easy", "Max profit buying once and selling once."},
            {"best-time-stock-2", "Best Time to Buy & Sell Stock II", "DP - Stocks", "Medium", "Max profit with unlimited buy & sell transactions."},
            {"best-time-stock-3", "Best Time to Buy & Sell Stock III", "DP - Stocks", "Hard", "Max profit with at most 2 buy & sell transactions."},
            {"best-time-stock-4", "Best Time to Buy & Sell Stock IV", "DP - Stocks", "Hard", "Max profit with at most K buy & sell transactions."},
            {"stock-cooldown", "Buy & Sell Stock with Cooldown", "DP - Stocks", "Medium", "Max profit with 1 day cooldown after selling."},
            {"stock-transaction-fee", "Buy & Sell Stock with Transaction Fee", "DP - Stocks", "Medium", "Max profit with transaction fee per completed trade."},
            {"longest-increasing-subsequence", "Longest Increasing Subsequence", "DP - LIS", "Medium", "Find length of longest strictly increasing subsequence in O(N log N)."},
            {"print-lis", "Print Longest Increasing Subsequence", "DP - LIS", "Medium", "Reconstruct and print the actual LIS sequence using hash parent array."},
            {"lis-binary-search", "LIS Using Binary Search", "DP - LIS", "Medium", "Find LIS length in O(N log N) time using Patience Sorting / lower_bound."},
            {"longest-string-chain", "Longest String Chain", "DP - LIS", "Medium", "Find longest chain of words where wordA is predecessor of wordB."},
            {"longest-bitonic-subsequence", "Longest Bitonic Subsequence", "DP - LIS", "Medium", "Longest subsequence increasing then decreasing LIS + LDS."},
            {"number-of-lis", "Number of Longest Increasing Subsequences", "DP - LIS", "Medium", "Count total number of subsequences attaining maximum LIS length."},
            {"largest-divisible-subset", "Largest Divisible Subset", "DP - LIS", "Medium", "Find largest subset where every pair satisfies arr[i] % arr[j] == 0."},
            {"matrix-chain-multiplication", "Matrix Chain Multiplication", "DP - MCM", "Hard", "Find min scalar multiplications to multiply N matrices."},
            {"mcm-cost-eval", "Mining Diamonds / MCM Cost Eval", "DP - MCM", "Hard", "Partition DP optimal matrix parenthesization cost."},
            {"burst-balloons", "Burst Balloons", "DP - MCM", "Hard", "Maximize coins from bursting balloons using interval DP."},
            {"evaluate-boolean-expression", "Evaluate Expression to True", "DP - MCM", "Hard", "Count ways to parenthesize boolean expression to evaluate to True."},
            {"palindrome-partitioning-2", "Palindrome Partitioning II", "DP - MCM", "Hard", "Find minimum cuts needed to partition string into palindromes."},
            {"partition-array-max-sum", "Partition Array for Maximum Sum", "DP - MCM", "Medium", "Partition array into subarrays of max length K maximizing total sum."},
            {"matrix-chain-multiplication-theory", "MCM Theory & Partition Pattern", "DP - MCM", "Easy", "Partition DP interval pattern theory f(i, j)."},
            {"max-rectangle-area-all-ones", "Max Rectangle Area with All 1s", "DP - Squares", "Hard", "Find max area 1s rectangle in 2D grid using Histogram DP."},
            {"count-square-submatrices", "Count Square Submatrices with All Ones", "DP - Squares", "Medium", "Count total square submatrices having all 1s in 2D binary grid."}
        };

        for (String[] p : list) {
            String id = p[0]; String title = p[1]; String cat = p[2]; String diff = p[3]; String desc = p[4];
            problems.put(id, new ProblemDetail(
                id, title, cat, "Dynamic Programming", diff, desc,
                String.format("// Java Implementation for %s\npublic int solve() {\n    // DP Striver A2Z Implementation\n    return 0;\n}", title),
                null, null, null, createArrayState(new int[]{1, 2, 3, 4}, -1, -1), null, null, null,
                new ComplexityDetail("O(N * K)", "Time Complexity: Optimal DP table state transitions.", "Dynamic Programming", "O(N)", "Space Complexity: 1D / 2D DP array space.", "Memory", "Auxiliary Space: O(N)", "Memory"), "Array"
            ));
        }
    }

    // Step Generators
    private List<ExecutionStep> generateClimbingStairsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] dp = new int[]{1, 1, 2, 3, 5, 8};
        steps.add(new ExecutionStep(1, 4, "Climbing Stairs N=5: Base cases dp[0] = 1, dp[1] = 1.", List.of(), Map.of(), List.of(), Map.of("dp[0]", "1", "dp[1]", "1"), "Array", null, createArrayState(dp, 0, 1), null, null));
        steps.add(new ExecutionStep(2, 48, "State Transition Loop: dp[i] = dp[i-1] + dp[i-2]. dp[5] = 8 ways.", List.of(), Map.of(), List.of(), Map.of("ways", "8"), "Array", null, createArrayState(dp, 5, -1), null, null));
        steps.add(new ExecutionStep(3, 52, "Climbing Stairs Complete! Total distinct ways to climb 5 stairs = 8.", List.of(), Map.of(), List.of(), Map.of("Result", "8"), "Array", null, createArrayState(dp, -1, -1), null, null));
        return steps;
    }

    private List<ExecutionStep> generateFrogJumpSteps() { return generateClimbingStairsSteps(); }
    private List<ExecutionStep> generateKnapsackSteps() {
        int[] weights = {1, 2, 3}; int[] values = {10, 15, 40};
        ListTraceRecorder recorder = new ListTraceRecorder();
        new Knapsack01().solve(weights, values, 6, recorder);
        return recorder.toExecutionSteps();
    }
    private List<ExecutionStep> generateLcsSteps() { return generateClimbingStairsSteps(); }

    private List<ArrayElement> createArrayState(int[] vals, int idx1, int idx2) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String st = (i == idx1 || i == idx2) ? "active" : "default";
            list.add(new ArrayElement(i, vals[i], st));
        }
        return list;
    }

    private int[][] createDefaultDpGrid() {
        return new int[][]{
            {0, 0, 0, 0},
            {0, 1, 1, 1},
            {0, 1, 2, 2},
            {0, 1, 2, 3}
        };
    }
}
