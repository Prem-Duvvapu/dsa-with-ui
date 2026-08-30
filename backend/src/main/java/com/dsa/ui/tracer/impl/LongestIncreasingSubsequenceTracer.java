package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.List;

/**
 * LIS as a genuine 2-D DP over (index, previous-taken-index):
 * dp[i][p] is the longest increasing subsequence obtainable from index i onward
 * when the most recent element taken has index p-1. Cell (i,p) decides between
 * skipping nums[i] and taking it when it beats nums[p-1].
 */
@Component
public class LongestIncreasingSubsequenceTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-increasing-subsequence";
    }

    @Override
    public DsType dsType() {
        return DsType.MATRIX;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("dp[i][p] fills bottom-up: best LIS from index i when the last taken index is p-1.")
                        .length(1, 16).values(-999, 999)
                        .defaultValue(List.of(10, 9, 2, 5, 3, 7, 101, 18))
                        .build());
    }

    /** Strictly decreasing, so every take is refused except the no-predecessor column. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, 4, 3, 2, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public int lengthOfLIS(int[] nums) {
                   int n = nums.length;
                   // @a init
                   int[][] dp = new int[n + 1][n + 1];   // dp[i][p]: best from index i, last taken = p-1
                   for (int i = n - 1; i >= 0; i--) {
                       for (int p = i; p >= 0; p--) {
                           // @a fill
                           boolean canTake = p == 0 || nums[i] > nums[p - 1];
                           int take = canTake ? 1 + dp[i + 1][i + 1] : Integer.MIN_VALUE;
                           dp[i][p] = Math.max(dp[i + 1][p], take);
                       }
                   }
                   // @a done
                   return dp[0][0];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int[][] dp = new int[n + 1][n + 1];

        emit.at("init")
                .say("Table is (%d+1)x(%d+1). Row i asks \"what is the best LIS starting at nums[%d]?\"; column p encodes the last element already taken (index p-1). Filling starts at the bottom-right.", n, n, n - 1)
                .var("rows", n + 1).var("cols", n + 1)
                .grid(zeroCopy(dp)).step();

        for (int i = n - 1; i >= 0; i--) {
            for (int p = i; p >= 0; p--) {
                int skip = dp[i + 1][p];
                int take;
                String verdict;
                if (p == 0) {
                    take = 1 + dp[i + 1][i + 1];
                    verdict = "column 0 means nothing taken yet, so nums[%d]=%d can always start a subsequence".formatted(i, nums[i]);
                } else if (nums[i] > nums[p - 1]) {
                    take = 1 + dp[i + 1][i + 1];
                    verdict = "nums[%d]=%d beats the last taken nums[%d]=%d, so taking it yields %d".formatted(i, nums[i], p - 1, nums[p - 1], take);
                } else {
                    take = Integer.MIN_VALUE;
                    verdict = "nums[%d]=%d does not beat the last taken nums[%d]=%d, so it cannot be appended".formatted(i, nums[i], p - 1, nums[p - 1]);
                }
                dp[i][p] = Math.max(skip, take);
                emit.at("fill")
                        .say("(%d,%d): skip keeps %d; %s. dp[%d][%d]=%d.",
                                i, p, skip, verdict, i, p, dp[i][p])
                        .var("i", i).var("p", p).var("skip", skip)
                        .var("take", take == Integer.MIN_VALUE ? "x" : take)
                        .var("value", dp[i][p])
                        .grid(zeroCopy(dp)).step();
            }
        }

        emit.at("done")
                .say("dp[0][0] is the best LIS from index 0 with nothing pre-taken: length %d.", dp[0][0])
                .var("answer", dp[0][0])
                .grid(zeroCopy(dp)).step();
    }

    private static int[][] zeroCopy(int[][] dp) {
        int[][] out = new int[dp.length][];
        for (int r = 0; r < dp.length; r++) {
            out[r] = dp[r].clone();
        }
        return out;
    }
}
