package com.dsa.ui.service;

import com.dsa.ui.model.*;
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
            "climbing-stairs", "Climbing Stairs (1D DP)", "DP - 1D DP", "Dynamic Programming", "Easy",
            "You are climbing a staircase. It takes n steps to reach the top. Each time you can climb 1 or 2 steps. How many distinct ways can you climb to top?",
            """
            // Java Climbing Stairs - Space Optimized DP (LeetCode 70)
            public int climbStairs(int n) {
                if (n <= 1) return 1;
                int prev2 = 1, prev = 1;
                for (int i = 2; i <= n; i++) {
                    int curi = prev + prev2;
                    prev2 = prev;
                    prev = curi;
                }
                return prev;
            }
            """,
            null, null, null, createArrayState(new int[]{1, 1, 2, 3, 5, 8}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: 1D DP loop runs N-1 times.",
                "Why Fibonacci relation dp[i] = dp[i-1] + dp[i-2]? To reach step i, you can only jump from step i-1 (1 step) or step i-2 (2 steps).",
                "O(1)",
                "Space Complexity: Space optimized from O(N) array to O(1) space.",
                "Why O(1)? Tracks only previous two states `prev` and `prev2`.",
                "Auxiliary Space: O(1)",
                "Return Ways: O(1)"
            ),
            "Array"
        ));

        // 2. Frog Jump / Min Cost Climbing
        problems.put("frog-jump", new ProblemDetail(
            "frog-jump", "Frog Jump (Min Energy 1D DP)", "DP - 1D DP", "Dynamic Programming", "Easy",
            "Given heights array of N stairs, find minimum energy to reach top if frog can jump 1 or 2 stairs at cost |height[i] - height[j]|.",
            """
            // Java Frog Jump (Striver A2Z Sheet)
            public int frogJump(int n, int heights[]) {
                int prev = 0, prev2 = 0;
                for (int i = 1; i < n; i++) {
                    int jumpOne = prev + Math.abs(heights[i] - heights[i - 1]);
                    int jumpTwo = Integer.MAX_VALUE;
                    if (i > 1) jumpTwo = prev2 + Math.abs(heights[i] - heights[i - 2]);

                    int curi = Math.min(jumpOne, jumpTwo);
                    prev2 = prev;
                    prev = curi;
                }
                return prev;
            }
            """,
            null, null, null, createArrayState(new int[]{10, 20, 30, 10}, -1, -1), null, null, null,
            new ComplexityDetail(
                "O(N)",
                "Time Complexity: Iterates through N stairs.",
                "Why Min Energy DP works? Evaluates both 1-step and 2-step jumps from previous optimal states.",
                "O(1)",
                "Space Complexity: O(1) space optimization.",
                "Why O(1)? Retains only `prev` and `prev2` energy states.",
                "Auxiliary Space: O(1)",
                "Return Cost: O(1)"
            ),
            "Array"
        ));

        // 3. 0/1 Knapsack Problem
        problems.put("knapsack-01", new ProblemDetail(
            "knapsack-01", "0/1 Knapsack Problem", "DP - 2D Subsets", "Dynamic Programming", "Medium",
            "Given weights and values of N items, put items in a knapsack of capacity W to get maximum total value.",
            """
            // Java 0/1 Knapsack DP Matrix (Striver A2Z Sheet)
            public int knapsack(int[] wt, int[] val, int n, int W) {
                int[][] dp = new int[n][W + 1];
                for (int i = wt[0]; i <= W; i++) dp[0][i] = val[0];

                for (int ind = 1; ind < n; ind++) {
                    for (int cap = 0; cap <= W; cap++) {
                        int notTake = dp[ind - 1][cap];
                        int take = Integer.MIN_VALUE;
                        if (wt[ind] <= cap) {
                            take = val[ind] + dp[ind - 1][cap - wt[ind]];
                        }
                        dp[ind][cap] = Math.max(take, notTake);
                    }
                }
                return dp[n - 1][W];
            }
            """,
            null, null, null, null, null, null, createKnapsackGrid(),
            new ComplexityDetail(
                "O(N x W)",
                "Time Complexity: Two nested loops iterate over N items and W capacity units.",
                "Why Pick vs Non-Pick? At each item, we choose between skipping item (dp[ind-1][cap]) or including item (val[ind] + dp[ind-1][cap-wt[ind]]).",
                "O(N x W)",
                "Space Complexity: 2D DP matrix dp[N][W+1]. Can be space-optimized to O(W).",
                "Why O(N x W)? Stores subproblem answers for all item index and capacity combinations.",
                "Auxiliary Space: O(N x W)",
                "DP Matrix: O(N x W)"
            ),
            "Matrix"
        ));

        // 4. Longest Common Subsequence (LCS)
        problems.put("longest-common-subsequence", new ProblemDetail(
            "longest-common-subsequence", "Longest Common Subsequence (LCS)", "DP - Strings", "Dynamic Programming", "Medium",
            "Given two strings text1 and text2, return the length of their longest common subsequence.",
            """
            // Java Longest Common Subsequence DP (LeetCode 1143)
            public int longestCommonSubsequence(String text1, String text2) {
                int n = text1.length(), m = text2.length();
                int[][] dp = new int[n + 1][m + 1];

                for (int i = 1; i <= n; i++) {
                    for (int j = 1; j <= m; j++) {
                        if (text1.charAt(i - 1) == text2.charAt(j - 1)) {
                            dp[i][j] = 1 + dp[i - 1][j - 1]; // Character match!
                        } else {
                            dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]); // Character mismatch
                        }
                    }
                }
                return dp[n][m];
            }
            """,
            null, null, null, null, null, null, createLcsGrid(),
            new ComplexityDetail(
                "O(N x M)",
                "Time Complexity: Fills DP matrix of size (N+1) x (M+1).",
                "Why LCS recurrence works? If characters match: 1 + dp[i-1][j-1]. If mismatch: Math.max(dp[i-1][j], dp[i][j-1]).",
                "O(N x M)",
                "Space Complexity: 2D DP table of size O(N x M).",
                "Why O(N x M)? Stores LCS lengths for all prefixes of text1 and text2.",
                "Auxiliary Space: O(N x M)",
                "DP Table: O(N x M)"
            ),
            "Matrix"
        ));
    }

    // Dynamic Step Generators
    private List<ExecutionStep> generateClimbingStairsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int n = 5;
        int[] dp = new int[n + 1];
        dp[0] = 1; dp[1] = 1;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Base cases: dp[0] = 1 (1 way to stay at ground), dp[1] = 1 (1 way to take 1 step). Target: n = 5.",
            List.of(), Map.of(), List.of(), Map.of("dp[0]", "1", "dp[1]", "1"),
            "Array", null, createArrayState(new int[]{1, 1, 0, 0, 0, 0}, 0, 1), null, null
        ));

        for (int i = 2; i <= n; i++) {
            dp[i] = dp[i - 1] + dp[i - 2];
            steps.add(new ExecutionStep(
                stepNum++, 6,
                String.format("Step i = %d: dp[%d] = dp[%d] (%d) + dp[%d] (%d) = %d distinct ways.", i, i, i - 1, dp[i - 1], i - 2, dp[i - 2], dp[i]),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), String.format("dp[%d]", i), String.valueOf(dp[i])),
                "Array", null, createArrayState(dp, i - 2, i - 1), null, null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 9,
            String.format("Climbing Stairs Complete! Total distinct ways to climb %d stairs = %d.", n, dp[n]),
            List.of(), Map.of(), List.of(), Map.of("Total Ways", String.valueOf(dp[n])),
            "Array", null, createArrayState(dp, -1, n), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateFrogJumpSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[] heights = new int[]{10, 20, 30, 10};
        int n = heights.length;
        int prev = 0, prev2 = 0;
        int stepNum = 1;

        steps.add(new ExecutionStep(
            stepNum++, 4,
            "Heights: [10, 20, 30, 10] (N = 4). Base case: energy at stair 0 = 0.",
            List.of(), Map.of(), List.of(), Map.of("stair_0", "0 energy"),
            "Array", null, createArrayState(heights, 0, -1), null, null
        ));

        for (int i = 1; i < n; i++) {
            int jumpOne = prev + Math.abs(heights[i] - heights[i - 1]);
            int jumpTwo = Integer.MAX_VALUE;
            if (i > 1) jumpTwo = prev2 + Math.abs(heights[i] - heights[i - 2]);

            int curi = Math.min(jumpOne, jumpTwo);
            prev2 = prev;
            prev = curi;

            steps.add(new ExecutionStep(
                stepNum++, 8,
                String.format("Stair i = %d (height %d): 1-step energy = %d, 2-step energy = %d -> Min energy to stair %d = %d.", i, heights[i], jumpOne, jumpTwo == Integer.MAX_VALUE ? 0 : jumpTwo, i, curi),
                List.of(), Map.of(), List.of(), Map.of("i", String.valueOf(i), "minEnergy", String.valueOf(curi)),
                "Array", null, createArrayState(heights, i, -1), null, null
            ));
        }

        steps.add(new ExecutionStep(
            stepNum++, 12,
            String.format("Frog Jump Complete! Minimum energy to reach top stair = %d.", prev),
            List.of(), Map.of(), List.of(), Map.of("Min Energy", String.valueOf(prev)),
            "Array", null, createArrayState(heights, n - 1, -1), null, null
        ));

        return steps;
    }

    private List<ExecutionStep> generateKnapsackSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] dp = createKnapsackGrid();
        steps.add(new ExecutionStep(1, 4, "Initialize 2D DP Matrix for items [wt=[1,2,3], val=[10,15,40]] and capacity W = 5.", List.of(), Map.of(), List.of(), Map.of("W", "5"), "Matrix", dp));
        steps.add(new ExecutionStep(2, 12, "0/1 Knapsack DP completed! Maximum Profit = 55 (Item 1 + Item 2).", List.of(), Map.of(), List.of(), Map.of("Max Profit", "55"), "Matrix", dp));
        return steps;
    }

    private List<ExecutionStep> generateLcsSteps() {
        List<ExecutionStep> steps = new ArrayList<>();
        int[][] dp = createLcsGrid();
        steps.add(new ExecutionStep(1, 5, "LCS Matrix for text1=\"adebc\", text2=\"dcadb\".", List.of(), Map.of(), List.of(), Map.of("text1", "adebc", "text2", "dcadb"), "Matrix", dp));
        steps.add(new ExecutionStep(2, 13, "LCS DP Table filled! Longest Common Subsequence Length = 3 (\"adb\").", List.of(), Map.of(), List.of(), Map.of("LCS Length", "3"), "Matrix", dp));
        return steps;
    }

    // Helper builders
    private List<ArrayElement> createArrayState(int[] vals, int activeIdx1, int activeIdx2) {
        List<ArrayElement> list = new ArrayList<>();
        for (int i = 0; i < vals.length; i++) {
            String state = (i == activeIdx1 || i == activeIdx2) ? "active" : "default";
            list.add(new ArrayElement(i, vals[i], state));
        }
        return list;
    }

    private int[][] createKnapsackGrid() {
        return new int[][]{
            {0, 10, 10, 10, 10, 10},
            {0, 10, 15, 25, 25, 25},
            {0, 10, 15, 40, 50, 55}
        };
    }

    private int[][] createLcsGrid() {
        return new int[][]{
            {0, 0, 0, 0, 0, 0},
            {0, 0, 0, 1, 1, 1},
            {0, 1, 1, 1, 1, 1},
            {0, 1, 1, 1, 1, 1},
            {0, 1, 1, 1, 2, 2},
            {0, 1, 2, 2, 2, 3}
        };
    }
}
