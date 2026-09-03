package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Partition Equal Subset Sum: can the array be split into two subsets with equal sum?
 *
 * <p>Not a new recurrence — a partition into two equal halves exists exactly when some
 * subset sums to {@code totalSum / 2}, so the interior of this tracer is the identical
 * reachability table {@link SubsetSumEqualTargetTracer} builds, just with the target
 * derived rather than caller-supplied. What is genuinely different is the branch in front
 * of it: an odd total sum makes an equal split arithmetically impossible before a single
 * DP cell is considered, and that short-circuit is traced as its own real step rather than
 * silently returning false.
 *
 * <p>The two tracers share {@link SubsetSumDpTable} for the view. They deliberately do not
 * share a call into one another — this one runs its own copy of the reachability loop, the
 * same way {@link GridUniquePathsTracer} and {@code UniquePaths2Tracer} each run their own
 * loop over a table built by the same shared view helper.
 */
@Component
public class PartitionEqualSubsetSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "partition-equal-subset-sum";
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
                        .help("Split into two subsets with equal sum, if possible.")
                        .length(1, 10).values(0, 20)
                        .defaultValue(List.of(1, 5, 11, 5))
                        .build());
    }

    /** An odd total sum, so the "no computation needed" branch runs instead. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean canPartition(int[] nums) {
                   int totalSum = 0;
                   for (int num : nums) totalSum += num;
                   if (totalSum % 2 != 0) {
                       // @a odd
                       return false;
                   }
                   int target = totalSum / 2;
                   int n = nums.length;
                   // @a init
                   boolean[][] dp = new boolean[n + 1][target + 1];
                   // @a base
                   dp[0][0] = true;
                   for (int i = 1; i <= n; i++) {
                       for (int s = 0; s <= target; s++) {
                           boolean skip = dp[i - 1][s];
                           boolean take = s >= nums[i - 1] && dp[i - 1][s - nums[i - 1]];
                           // @a decide
                           dp[i][s] = skip || take;
                       }
                   }
                   // @a done
                   return dp[n][target];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int totalSum = 0;
        for (int num : nums) {
            totalSum += num;
        }

        if (totalSum % 2 != 0) {
            emit.at("odd")
                    .say("Total sum is %d, which is odd. Two equal integer subsets would "
                            + "each need to sum to %.1f — not a whole number — so no equal "
                            + "partition can exist. No subset-sum table is needed at all.",
                            totalSum, totalSum / 2.0)
                    .var("totalSum", totalSum)
                    .var("answer", false)
                    .dpTable(oddSumTable(totalSum)).step();
            return;
        }

        int target = totalSum / 2;
        boolean[][] dp = new boolean[n + 1][target + 1];
        boolean[][] settled = new boolean[n + 1][target + 1];

        emit.at("init")
                .say("Total sum is %d, which is even, so look for one subset summing to "
                        + "exactly %d — whatever is left over sums to the other %d "
                        + "automatically.", totalSum, target, target)
                .var("totalSum", totalSum).var("target", target)
                .dpTable(table(dp, settled, nums, null, "?", Set.of(), false)).step();

        dp[0][0] = true;
        for (int s = 0; s <= target; s++) {
            settled[0][s] = true;
        }
        emit.at("base")
                .say("With no items considered yet, the only reachable sum is 0 — the empty "
                        + "subset. dp[0][0] = true, and dp[0][s] = false for every s > 0.")
                .var("dp[0][0]", true)
                .dpTable(table(dp, settled, nums, new SubsetSumDpTable.Coord(0, 0), "T",
                        Set.of(), false)).step();

        for (int i = 1; i <= n; i++) {
            int value = nums[i - 1];
            for (int s = 0; s <= target; s++) {
                boolean skip = dp[i - 1][s];
                boolean canTake = s >= value;
                boolean take = canTake && dp[i - 1][s - value];
                boolean chosen = skip || take;

                Set<SubsetSumDpTable.Coord> reads = canTake
                        ? Set.of(new SubsetSumDpTable.Coord(i - 1, s),
                                new SubsetSumDpTable.Coord(i - 1, s - value))
                        : Set.of(new SubsetSumDpTable.Coord(i - 1, s));

                String reasoning;
                if (skip && take) {
                    reasoning = ("reachable two ways — skip item %d and dp[%d][%d] was "
                            + "already true, or take it and dp[%d][%d] was true")
                                    .formatted(i, i - 1, s, i - 1, s - value);
                } else if (skip) {
                    reasoning = ("skipping item %d (worth %d) is enough, since dp[%d][%d] "
                            + "was already true").formatted(i, value, i - 1, s);
                } else if (take) {
                    reasoning = ("taking item %d (worth %d) reaches it: s - %d = %d, and "
                            + "dp[%d][%d] was true").formatted(i, value, value, s - value,
                            i - 1, s - value);
                } else if (canTake) {
                    reasoning = ("skipping leaves dp[%d][%d] false, and taking needs "
                            + "dp[%d][%d], also false").formatted(i - 1, s, i - 1, s - value);
                } else {
                    reasoning = ("item %d (worth %d) is bigger than %d on its own, so taking "
                            + "is impossible, and skipping leaves dp[%d][%d] false")
                                    .formatted(i, value, s, i - 1, s);
                }

                emit.at("decide")
                        .say("dp[%d][%d]: %s. dp[%d][%d] = %s.", i, s, reasoning, i, s,
                                chosen ? "true" : "false")
                        .var("i", i).var("s", s)
                        .var("skip", skip).var("take", take)
                        .var("dp[i][s]", chosen)
                        .dpTable(table(dp, settled, nums, new SubsetSumDpTable.Coord(i, s),
                                chosen ? "T" : "F", reads, false)).step();

                dp[i][s] = chosen;
                settled[i][s] = true;
            }
        }

        boolean answer = dp[n][target];
        emit.at("done")
                .say("dp[%d][%d] = %s: %s.", n, target, answer ? "true" : "false",
                        answer
                                ? "a subset summing to " + target + " exists, so the array "
                                        + "splits into two subsets of equal sum"
                                : "no subset sums to exactly " + target + ", so no equal "
                                        + "partition exists")
                .var("answer", answer)
                .dpTable(table(dp, settled, nums, null, "?", Set.of(), true)).step();
    }

    /** The one-cell table shown when the odd-sum short-circuit fires, before any dp work. */
    private static DpTable oddSumTable(int totalSum) {
        return new DpTable(
                List.of("parity check"),
                List.of("total sum"),
                List.of(List.of(new DpCell(String.valueOf(totalSum), "resolved"))));
    }

    private static DpTable table(boolean[][] dp, boolean[][] settled, int[] nums,
                                 SubsetSumDpTable.Coord probe, String probeValue,
                                 Set<SubsetSumDpTable.Coord> reads, boolean done) {
        return SubsetSumDpTable.of(dp, settled, nums, probe, probeValue, reads, done);
    }
}
