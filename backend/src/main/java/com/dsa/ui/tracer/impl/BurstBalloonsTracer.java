package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Interval DP again, like {@link MatrixChainMultiplicationTracer}, but built bottom-up by
 * increasing interval length instead of top-down memoized recursion - a deliberately
 * different implementation style for the same "choose where to split, then combine"
 * shape. The trick is choosing the LAST balloon burst in a range rather than the first:
 * once every other balloon in (left,right) is already gone, k's neighbors are guaranteed
 * to be whatever sits just outside the range, no matter what order the rest were burst in.
 */
@Component
public class BurstBalloonsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "burst-balloons";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Balloons")
                        .help("Bursting a balloon pays coins[left] * coins[i] * coins[right], the neighbors it had left.")
                        .length(1, 7).values(1, 100)
                        .defaultValue(List.of(3, 1, 5, 8))
                        .build());
    }

    /** Just two balloons: the order (which to burst last) genuinely changes the total. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxCoins(int[] nums) {
                   int n = nums.length;
                   int[] balloons = new int[n + 2];
                   balloons[0] = 1;
                   balloons[n + 1] = 1;
                   for (int i = 0; i < n; i++) balloons[i + 1] = nums[i];
                   int[][] dp = new int[n + 2][n + 2];
                   for (int length = 1; length <= n; length++) {
                       for (int left = 1; left <= n - length + 1; left++) {
                           int right = left + length - 1;
                           for (int k = left; k <= right; k++) {
                               int coins = balloons[left - 1] * balloons[k] * balloons[right + 1]
                                       + dp[left][k - 1] + dp[k + 1][right];
                               // @a tryK
                               if (coins > dp[left][right]) {
                                   // @a newBest
                                   dp[left][right] = coins;
                               }
                           }
                       }
                   }
                   // @a done
                   return dp[1][n];
               }""";
    }

    private DpTable table(int[][] dp, int size, int probeLeft, int probeRight, int k) {
        List<String> labels = new ArrayList<>(size);
        for (int i = 0; i < size; i++) labels.add(String.valueOf(i));

        List<List<DpCell>> rows = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            List<DpCell> row = new ArrayList<>(size);
            for (int j = 0; j < size; j++) {
                boolean invalid = i == 0 || j == 0 || i > j;
                String value = invalid ? "·" : String.valueOf(dp[i][j]);
                String state;
                if (invalid) {
                    state = "void";
                } else if (i == probeLeft && j == probeRight) {
                    state = "probe";
                } else if (i == probeLeft && j == k - 1) {
                    state = "read";
                } else if (i == k + 1 && j == probeRight) {
                    state = "read";
                } else if (dp[i][j] != 0) {
                    state = "known";
                } else {
                    state = "void";
                }
                row.add(new DpCell(value, state));
            }
            rows.add(row);
        }
        return new DpTable(labels, labels, rows);
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int[] balloons = new int[n + 2];
        balloons[0] = 1;
        balloons[n + 1] = 1;
        for (int i = 0; i < n; i++) balloons[i + 1] = nums[i];
        int[][] dp = new int[n + 2][n + 2];

        for (int length = 1; length <= n; length++) {
            for (int left = 1; left <= n - length + 1; left++) {
                int right = left + length - 1;
                for (int k = left; k <= right; k++) {
                    int coins = balloons[left - 1] * balloons[k] * balloons[right + 1]
                            + dp[left][k - 1] + dp[k + 1][right];
                    boolean better = coins > dp[left][right];
                    emit.at("tryK")
                            .say("Range [%d,%d]: if balloon at %d (value %d) bursts last, its "
                                    + "neighbors by then are %d and %d - %d*%d*%d + %d + %d = %d.%s",
                                    left, right, k, balloons[k], balloons[left - 1],
                                    balloons[right + 1], balloons[left - 1], balloons[k],
                                    balloons[right + 1], dp[left][k - 1], dp[k + 1][right], coins,
                                    better ? " New best for this range." : "")
                            .var("left", left).var("right", right).var("k", k).var("coins", coins)
                            .dpTable(table(dp, n + 2, left, right, k)).step();

                    if (better) {
                        dp[left][right] = coins;
                        emit.at("newBest")
                                .say("Recorded: dp[%d][%d] = %d.", left, right, coins)
                                .var("left", left).var("right", right).var("dp", coins)
                                .dpTable(table(dp, n + 2, left, right, k)).step();
                    }
                }
            }
        }

        emit.at("done")
                .say("Every range solved. Bursting all %d balloons optimally earns %d coins.",
                        n, dp[1][n])
                .var("answer", dp[1][n])
                .dpTable(table(dp, n + 2, -1, -1, -1)).step();
    }
}
