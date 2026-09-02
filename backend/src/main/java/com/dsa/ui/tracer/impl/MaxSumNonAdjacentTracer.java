package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Maximum sum of non-adjacent elements — House Robber I in Striver's ordering, and the first
 * recurrence here where a cell chooses between two options rather than combining them.
 *
 * <p>Climbing stairs adds its two predecessors and frog jump minimises over them. This one
 * takes the maximum of "use this element and jump back two" against "skip it and inherit the
 * neighbour", so both candidates are emitted on every step. The losing candidate is the
 * whole reason a greedy largest-first pick fails, and it has to be visible to make that
 * point.
 */
@Component
public class MaxSumNonAdjacentTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "max-sum-non-adjacent";
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
                        .help("Adjacent picks are forbidden, so a large value can be worth "
                                + "less than the two smaller ones it blocks.")
                        .length(1, 20).values(0, 999)
                        .defaultValue(List.of(2, 1, 4, 9, 4, 1, 8))
                        .build());
    }

    /** One dominant value early, so skipping wins three times in a row afterwards. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(5, 5, 10, 100, 10, 5));
    }

    @Override
    public String annotatedCode() {
        return """
               public int maxNonAdjacentSum(int[] nums) {
                   int n = nums.length;
                   // @a init
                   int[] best = new int[n];
                   // @a base
                   best[0] = nums[0];
                   for (int i = 1; i < n; i++) {
                       int take = nums[i] + (i > 1 ? best[i - 2] : 0);
                       int skip = best[i - 1];
                       // @a decide
                       best[i] = Math.max(take, skip);
                   }
                   // @a done
                   return best[n - 1];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int[] best = new int[n];
        boolean[] settled = new boolean[n];

        emit.at("init")
                .say("best[i] will hold the largest non-adjacent sum obtainable from the first "
                        + "%d values. It is a running answer, not a running total — best[i] "
                        + "need not use nums[i] at all.", n)
                .var("n", n)
                .dpTable(table(nums, best, settled, -1, "?", Set.of(), false)).step();

        best[0] = nums[0];
        settled[0] = true;
        emit.at("base")
                .say("With only the first value available the best you can do is take it: %d.",
                        nums[0])
                .var("best[0]", best[0])
                .dpTable(table(nums, best, settled, -1, "?", Set.of(), false)).step();

        for (int i = 1; i < n; i++) {
            boolean hasGap = i > 1;
            int carried = hasGap ? best[i - 2] : 0;
            int take = nums[i] + carried;
            int skip = best[i - 1];
            int chosen = Math.max(take, skip);

            String takeClause = hasGap
                    ? "take it and add best[%d]: %d + %d = %d".formatted(i - 2, nums[i],
                            carried, take)
                    : "take it, with no earlier cell to add, for %d".formatted(take);
            String verdict;
            if (take > skip) {
                verdict = "taking wins";
            } else if (skip > take) {
                verdict = ("skipping wins: nums[%d]=%d is not worth losing what best[%d] "
                        + "already earned").formatted(i, nums[i], i - 1);
            } else {
                verdict = "the two tie and nums[%d] is optional here".formatted(i);
            }

            emit.at("decide")
                    .say("Value %d at index %d: %s, or skip it and inherit best[%d] = %d. "
                            + "best[%d] = %d, so %s.",
                            nums[i], i, takeClause, i - 1, skip, i, chosen, verdict)
                    .var("i", i)
                    .var("take", take)
                    .var("skip", skip)
                    .var("best[i]", chosen)
                    .dpTable(table(nums, best, settled, i, String.valueOf(chosen),
                            hasGap ? Set.of(i - 1, i - 2) : Set.of(i - 1), false)).step();

            best[i] = chosen;
            settled[i] = true;
        }

        emit.at("done")
                .say("best[%d] = %d is the answer. Note it is monotonic: a later cell can never "
                        + "be worse than an earlier one, because skipping is always available.",
                        n - 1, best[n - 1])
                .var("answer", best[n - 1])
                .dpTable(table(nums, best, settled, -1, "?", Set.of(), true)).step();
    }

    private static com.dsa.ui.model.DpTable table(int[] nums, int[] best, boolean[] settled,
                                                  int probe, String probeValue,
                                                  Set<Integer> reads, boolean done) {
        return SeriesDpTable.of("value", nums, "best sum", best, settled,
                probe, probeValue, reads, done);
    }
}
