package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Counts subsets of the first i items that sum to exactly s — the counting sibling of
 * subset-sum reachability, not a new recurrence. The only difference from a yes/no
 * "can some subset hit sum K" table is that a cell here adds two counts instead of ORing
 * two booleans, and the base case seeds a count of 1 (the empty subset) rather than "true".
 *
 * <p>Cells with different index-sets but the same values still count separately: with
 * {@code nums=[1,2,2,3]} and {@code target=3}, the two 2s are distinguishable items, so
 * {"1, first 2"} and {"1, second 2"} are two different subsets even though they sum to the
 * same value — which is exactly why the answer is 3, not 2.
 *
 * <p>Zero-valued items are deliberately excluded from this tracer's input bounds (values
 * start at 1): a zero can be freely included or excluded from any subset without changing
 * its sum, which multiplies every count and would need its own worked-example treatment
 * rather than being an incidental default.
 */
@Component
public class CountSubsetsWithSumKTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-subsets-with-sum-k";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Values")
                        .help("Non-negative integers; kept away from zero here so every "
                                + "subset stays uniquely countable.")
                        .length(1, 10).values(1, 15)
                        .defaultValue(List.of(1, 2, 2, 3))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Target sum K")
                        .help("How many subsets sum to exactly this value.")
                        .range(0, 60)
                        .defaultValue(3)
                        .build());
    }

    /** No subset of a two-item array with both values above the target can ever reach it —
     * the "count stays zero the whole way through" branch the default never exercises. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, 10), "k", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public int countSubsetsWithSumK(int[] nums, int k) {
                   int n = nums.length;
                   // @a init
                   int[][] dp = new int[n + 1][k + 1];
                   // @a base
                   dp[0][0] = 1;
                   for (int i = 1; i <= n; i++) {
                       for (int s = 0; s <= k; s++) {
                           int skip = dp[i - 1][s];
                           int take = (s >= nums[i - 1]) ? dp[i - 1][s - nums[i - 1]] : 0;
                           // @a combine
                           dp[i][s] = skip + take;
                       }
                   }
                   // @a done
                   return dp[n][k];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        int n = nums.length;
        int[][] dp = new int[n + 1][k + 1];
        boolean[][] settled = new boolean[n + 1][k + 1];
        List<String> rowLabels = SubsetCountDpTable.rowLabels(nums);
        List<String> colLabels = SubsetCountDpTable.colLabels(k);

        emit.at("init")
                .say("dp[i][s] will count the subsets of the first i items that sum to "
                        + "exactly s. Building a %d by %d table over %d items and sums "
                        + "0..%d.", n + 1, k + 1, n, k)
                .var("n", n).var("k", k)
                .dpTable(table(rowLabels, colLabels, dp, settled, null, "?", Set.of(), false))
                .step();

        dp[0][0] = 1;
        for (int s = 0; s <= k; s++) {
            settled[0][s] = true;
        }
        emit.at("base")
                .say("With zero items available, the only subset at all is the empty one, "
                        + "and it sums to 0 — exactly one way. dp[0][0] = 1; every dp[0][s] "
                        + "for s > 0 stays 0, since no items means no way to reach a "
                        + "positive sum.")
                .var("dp[0][0]", 1)
                .dpTable(table(rowLabels, colLabels, dp, settled, null, "?", Set.of(), false))
                .step();

        for (int i = 1; i <= n; i++) {
            int val = nums[i - 1];
            for (int s = 0; s <= k; s++) {
                int skip = dp[i - 1][s];
                boolean canTake = s >= val;
                int take = canTake ? dp[i - 1][s - val] : 0;
                int total = skip + take;
                SubsetCountDpTable.Coord here = new SubsetCountDpTable.Coord(i, s);

                Set<SubsetCountDpTable.Coord> reads = canTake
                        ? Set.of(new SubsetCountDpTable.Coord(i - 1, s),
                                new SubsetCountDpTable.Coord(i - 1, s - val))
                        : Set.of(new SubsetCountDpTable.Coord(i - 1, s));

                String narration = canTake
                        ? ("Item %d (val=%d), sum %d: ways that skip it equal dp[%d][%d]=%d; "
                                + "ways that take it need the remaining sum %d from the "
                                + "earlier items, dp[%d][%d]=%d. dp[%d][%d] = %d + %d = %d.")
                                .formatted(i, val, s, i - 1, s, skip, s - val, i - 1, s - val,
                                        take, i, s, skip, take, total)
                        : ("Item %d (val=%d), sum %d: %d is larger than the remaining sum, "
                                + "so it cannot be part of any subset reaching s=%d here — "
                                + "only the skip option applies. dp[%d][%d] = dp[%d][%d] = %d.")
                                .formatted(i, val, s, val, s, i, s, i - 1, s, skip);

                emit.at("combine")
                        .say(narration)
                        .var("i", i).var("s", s)
                        .var("skip", skip).var("take", take).var("dp[i][s]", total)
                        .dpTable(table(rowLabels, colLabels, dp, settled, here,
                                String.valueOf(total), reads, false))
                        .step();

                dp[i][s] = total;
                settled[i][s] = true;
            }
        }

        emit.at("done")
                .say("dp[%d][%d] = %d: that many subsets of the given items sum to exactly "
                        + "%d.", n, k, dp[n][k], k)
                .var("answer", dp[n][k])
                .dpTable(table(rowLabels, colLabels, dp, settled, null, "?", Set.of(), true))
                .step();
    }

    private static com.dsa.ui.model.DpTable table(List<String> rowLabels,
            List<String> colLabels, int[][] dp, boolean[][] settled,
            SubsetCountDpTable.Coord probe, String probeValue,
            Set<SubsetCountDpTable.Coord> reads, boolean done) {
        return SubsetCountDpTable.of(rowLabels, colLabels, dp, settled, probe, probeValue,
                reads, done);
    }
}
