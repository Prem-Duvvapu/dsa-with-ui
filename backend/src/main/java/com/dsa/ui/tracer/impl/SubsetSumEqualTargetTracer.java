package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Subset Sum Equal to Target: does some subset of the array sum to exactly K?
 *
 * <p>{@code dp[i][s]} is true when some subset of the first {@code i} items sums to exactly
 * {@code s}. It is a reachability table, not an accumulation: a cell answers "can we land
 * here at all", never "what is the best value here" — the first genuinely boolean DP table
 * traced in this catalogue. Every cell has exactly two candidate predecessors, "skip item i"
 * and "take item i", and — unlike {@link com.dsa.ui.tracer.impl.MaxSumNonAdjacentTracer}'s
 * max — the two combine with OR, so either one being true is enough.
 *
 * <p>{@link PartitionEqualSubsetSumTracer} reduces to exactly this recurrence with
 * {@code target = totalSum / 2}; the two share {@link SubsetSumDpTable} for the view but
 * each runs its own trace, since partition's odd-sum short-circuit has no analogue here.
 */
@Component
public class SubsetSumEqualTargetTracer implements AlgorithmTracer {

    private static final String FORMULA = "dp[i][s] = dp[i-1][s] OR dp[i-1][s-nums[i-1]]";

    private static String tf(boolean value) {
        return value ? "T" : "F";
    }

    @Override
    public String id() {
        return "subset-sum-equal-target";
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
                        .help("Non-negative values only — a subset either includes an item "
                                + "or it does not.")
                        .length(1, 10).values(0, 20)
                        .defaultValue(List.of(1, 2, 3, 7))
                        .build(),
                InputField.of("target", FieldType.INT)
                        .label("Target sum K")
                        .help("The exact sum some subset must reach.")
                        .range(0, 40)
                        .defaultValue(6)
                        .build());
    }

    /** A target no subset of even numbers can reach, so the "not found" branch runs. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(2, 4), "target", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean subsetSumEqualTarget(int[] nums, int target) {
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
        int target = in.getInt("target");
        int n = nums.length;

        boolean[][] dp = new boolean[n + 1][target + 1];
        boolean[][] settled = new boolean[n + 1][target + 1];

        emit.at("init")
                .say("A table of %d items by sums 0..%d. dp[i][s] will be true exactly when "
                        + "some subset of the first %d values sums to exactly s.", n, target, n)
                .var("n", n).var("target", target)
                .dpTable(table(dp, settled, nums, null, "?", Set.of(), false)).step();

        dp[0][0] = true;
        for (int s = 0; s <= target; s++) {
            settled[0][s] = true;
        }
        emit.at("base")
                .say("With no items considered yet, the only reachable sum is 0 — the empty "
                        + "subset. dp[0][0] = true, and dp[0][s] = false for every s > 0, "
                        + "since there is nothing available to add.")
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

                com.dsa.ui.model.DpTable decideTable = table(dp, settled, nums,
                        new SubsetSumDpTable.Coord(i, s), chosen ? "T" : "F", reads, false);
                if (canTake) {
                    String substitution = String.format(
                            "dp[%d][%d] = dp[%d][%d] OR dp[%d][%d] = %s OR %s = %s",
                            i, s, i - 1, s, i - 1, s - value,
                            tf(skip), tf(take), tf(chosen));
                    decideTable = decideTable.withFormula(FORMULA, substitution);
                }

                emit.at("decide")
                        .say("dp[%d][%d]: %s. dp[%d][%d] = %s.", i, s, reasoning, i, s,
                                chosen ? "true" : "false")
                        .var("i", i).var("s", s)
                        .var("skip", skip).var("take", take)
                        .var("dp[i][s]", chosen)
                        .dpTable(decideTable).step();

                dp[i][s] = chosen;
                settled[i][s] = true;
            }
        }

        boolean answer = dp[n][target];
        emit.at("done")
                .say("dp[%d][%d] = %s: %s.", n, target, answer ? "true" : "false",
                        answer
                                ? "some subset of these " + n + " values sums to exactly "
                                        + target
                                : "no subset of these " + n + " values sums to exactly "
                                        + target)
                .var("answer", answer)
                .dpTable(table(dp, settled, nums, null, "?", Set.of(), true)).step();
    }

    private static com.dsa.ui.model.DpTable table(boolean[][] dp, boolean[][] settled,
                                                   int[] nums, SubsetSumDpTable.Coord probe,
                                                   String probeValue,
                                                   Set<SubsetSumDpTable.Coord> reads,
                                                   boolean done) {
        return SubsetSumDpTable.of(dp, settled, nums, probe, probeValue, reads, done);
    }
}
