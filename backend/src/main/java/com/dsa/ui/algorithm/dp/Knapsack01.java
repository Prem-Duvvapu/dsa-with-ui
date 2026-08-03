package com.dsa.ui.algorithm.dp;

import com.dsa.ui.trace.SnapshotUtil;
import com.dsa.ui.trace.TraceEvent;
import com.dsa.ui.trace.TraceRecorder;

import java.util.List;
import java.util.Map;

/**
 * Problem: 0/1 Knapsack Problem (2D Dynamic Programming)
 *
 * Given N items with weights and values, find maximum value in knapsack of capacity W.
 *
 * Approach: 2D Table DP where dp[i][w] represents max value considering first i items
 * with maximum capacity w.
 *
 * Time Complexity:  O(N * W) - Fills (N+1) x (W+1) table.
 * Space Complexity: O(N * W) - 2D DP table.
 */
public class Knapsack01 {

    public int solve(int[] wt, int[] val, int W, TraceRecorder recorder) {
        int n = wt.length;
        int[][] dp = new int[n + 1][W + 1];

        recorder.record(new TraceEvent(
            "start", 20,
            String.format("0/1 Knapsack DP: %d items, capacity W = %d. Initialize (%d x %d) DP table with 0s.", n, W, n + 1, W + 1),
            Map.of("N", String.valueOf(n), "W", String.valueOf(W)),
            "Matrix", SnapshotUtil.clone2DGrid(dp)
        ));

        for (int i = 1; i <= n; i++) {
            int currentWt = wt[i - 1];
            int currentVal = val[i - 1];

            for (int w = 0; w <= W; w++) {
                if (currentWt <= w) {
                    int exclude = dp[i - 1][w];
                    int include = currentVal + dp[i - 1][w - currentWt];
                    dp[i][w] = Math.max(include, exclude);

                    recorder.record(new TraceEvent(
                        "fill_cell", 30,
                        String.format("Item %d (wt=%d, val=%d), Cap w=%d: Include (val %d + dp[%d][%d]=%d) = %d vs Exclude (dp[%d][%d]=%d) -> dp[%d][%d] = %d",
                            i, currentWt, currentVal, w, currentVal, i - 1, w - currentWt, dp[i - 1][w - currentWt], include, i - 1, w, exclude, i, w, dp[i][w]),
                        Map.of("i", String.valueOf(i), "w", String.valueOf(w), "include", String.valueOf(include), "exclude", String.valueOf(exclude), "dp[i][w]", String.valueOf(dp[i][w])),
                        "Matrix", SnapshotUtil.clone2DGrid(dp)
                    ));
                } else {
                    dp[i][w] = dp[i - 1][w];

                    recorder.record(new TraceEvent(
                        "fill_cell", 34,
                        String.format("Item %d (wt=%d > cap %d): Cannot include. Copy dp[%d][%d] = %d -> dp[%d][%d] = %d",
                            i, currentWt, w, i - 1, w, dp[i - 1][w], i, w, dp[i][w]),
                        Map.of("i", String.valueOf(i), "w", String.valueOf(w), "dp[i][w]", String.valueOf(dp[i][w])),
                        "Matrix", SnapshotUtil.clone2DGrid(dp)
                    ));
                }
            }
        }

        recorder.record(new TraceEvent(
            "complete", 40,
            String.format("0/1 Knapsack DP Complete! Maximum total value achievable with capacity W=%d is %d.", W, dp[n][W]),
            Map.of("Max Value", String.valueOf(dp[n][W])),
            "Matrix", SnapshotUtil.clone2DGrid(dp)
        ));

        return dp[n][W];
    }
}
