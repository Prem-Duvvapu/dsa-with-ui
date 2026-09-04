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
 * Counts ways to split the array into two subsets S1, S2 with S1 - S2 = D (S1 &gt;= S2,
 * D &gt;= 0). This is not a new recurrence: since S1 + S2 = totalSum and S1 - S2 = D,
 * algebra gives S1 = (totalSum + D) / 2, so the problem reduces to counting subsets that
 * sum to exactly that target — the same table shape {@link CountSubsetsWithSumKTracer}
 * builds. Only the {@link SubsetCountDpTable} shape helper is shared between the two
 * tracers (mirroring how {@code FrogJumpTracer} and {@code FrogJumpKDistanceTracer} share
 * {@code SeriesDpTable}); this tracer runs and narrates its own loop rather than calling
 * into the other tracer's instance.
 *
 * <p>Two cases never reach a table at all: if D is larger than totalSum, S1 could never be
 * that far ahead of S2; if (totalSum + D) is odd, it cannot be split evenly into an integer
 * S1, so no partition can hit this exact difference either way. Both are answered honestly
 * as 0 without a single dp cell being computed — the narration says so explicitly rather
 * than silently returning an empty table.
 */
@Component
public class CountPartitionsGivenDiffTracer implements AlgorithmTracer {

    private static final String FORMULA = "dp[i][s] = dp[i-1][s] + dp[i-1][s-nums[i-1]]";

    @Override
    public String id() {
        return "count-partitions-given-diff";
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
                        .defaultValue(List.of(1, 1, 2, 3))
                        .build(),
                InputField.of("d", FieldType.INT)
                        .label("Difference D")
                        .help("How many ways to split the array so S1 - S2 = D, S1 >= S2.")
                        .range(0, 60)
                        .defaultValue(1)
                        .build());
    }

    /** totalSum=6, (6+1)=7 is odd — the "immediately 0, no table built" branch the
     * default (an even totalSum+D) never takes. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3), "d", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public int countPartitions(int[] nums, int d) {
                   // @a sumTotal
                   int totalSum = 0;
                   for (int x : nums) totalSum += x;
                   // @a parityCheck
                   if (d > totalSum || (totalSum + d) % 2 != 0) {
                       // @a zero
                       return 0;
                   }
                   // @a target
                   int target = (totalSum + d) / 2;
                   int n = nums.length;
                   // @a init
                   int[][] dp = new int[n + 1][target + 1];
                   // @a base
                   dp[0][0] = 1;
                   for (int i = 1; i <= n; i++) {
                       for (int s = 0; s <= target; s++) {
                           int skip = dp[i - 1][s];
                           int take = (s >= nums[i - 1]) ? dp[i - 1][s - nums[i - 1]] : 0;
                           // @a combine
                           dp[i][s] = skip + take;
                       }
                   }
                   // @a done
                   return dp[n][target];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int d = in.getInt("d");

        int totalSum = 0;
        for (int x : nums) {
            totalSum += x;
        }
        emit.at("sumTotal")
                .say("S1 - S2 = %d and S1 + S2 = totalSum = %d. Adding those, "
                        + "2*S1 = totalSum + D, so S1 = (totalSum + D) / 2 — this reduces "
                        + "to counting subsets that sum to that target.", d, totalSum)
                .var("totalSum", totalSum).var("d", d)
                .dpTable(checkpoint("totalSum", String.valueOf(totalSum)))
                .step();

        int sumPlusD = totalSum + d;
        boolean tooLarge = d > totalSum;
        boolean oddSplit = sumPlusD % 2 != 0;
        emit.at("parityCheck")
                .say(tooLarge
                        ? String.format("D=%d already exceeds totalSum=%d, so S1 could never "
                                + "be that far ahead of S2 — impossible before parity is "
                                + "even checked.", d, totalSum)
                        : String.format("totalSum + D = %d + %d = %d, which is %s, so an "
                                + "integer S1 %s.", totalSum, d, sumPlusD,
                                oddSplit ? "odd" : "even",
                                oddSplit ? "does not exist" : "exists"))
                .var("totalSum+D", sumPlusD)
                .dpTable(checkpoint("totalSum+D", String.valueOf(sumPlusD)))
                .step();

        if (tooLarge || oddSplit) {
            String reason = tooLarge
                    ? String.format(
                            "D=%d exceeds totalSum=%d, so S1 = S2 + D can never be reached.",
                            d, totalSum)
                    : String.format("(totalSum + D) = %d is odd, so it cannot be split "
                            + "evenly into S1 = (totalSum + D)/2 — no partition can achieve "
                            + "this exact difference.", sumPlusD);
            emit.at("zero")
                    .say("%s The answer is 0, without building a subset-count table at all.",
                            reason)
                    .var("answer", 0)
                    .dpTable(checkpoint("answer", "0"))
                    .step();
            return;
        }

        int target = sumPlusD / 2;
        int n = nums.length;
        int[][] dp = new int[n + 1][target + 1];
        boolean[][] settled = new boolean[n + 1][target + 1];
        List<String> rowLabels = SubsetCountDpTable.rowLabels(nums);
        List<String> colLabels = SubsetCountDpTable.colLabels(target);

        emit.at("target")
                .say("S1 = %d is achievable, so this reduces to counting subsets of the "
                        + "%d items that sum to exactly %d.", target, n, target)
                .var("target", target)
                .dpTable(checkpoint("target", String.valueOf(target)))
                .step();

        emit.at("init")
                .say("dp[i][s] will count the subsets of the first i items that sum to "
                        + "exactly s. Building a %d by %d table over %d items and sums "
                        + "0..%d.", n + 1, target + 1, n, target)
                .var("n", n)
                .dpTable(table(rowLabels, colLabels, dp, settled, null, "?", Set.of(), false))
                .step();

        dp[0][0] = 1;
        for (int s = 0; s <= target; s++) {
            settled[0][s] = true;
        }
        emit.at("base")
                .say("With zero items available, the only subset at all is the empty one, "
                        + "and it sums to 0 — exactly one way. dp[0][0] = 1; every dp[0][s] "
                        + "for s > 0 stays 0.")
                .var("dp[0][0]", 1)
                .dpTable(table(rowLabels, colLabels, dp, settled, null, "?", Set.of(), false))
                .step();

        for (int i = 1; i <= n; i++) {
            int val = nums[i - 1];
            for (int s = 0; s <= target; s++) {
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

                DpTable combineTable = table(rowLabels, colLabels, dp, settled, here,
                        String.valueOf(total), reads, false);
                if (canTake) {
                    String substitution = String.format(
                            "dp[%d][%d] = dp[%d][%d] + dp[%d][%d] = %d + %d = %d",
                            i, s, i - 1, s, i - 1, s - val, skip, take, total);
                    combineTable = combineTable.withFormula(FORMULA, substitution);
                }

                emit.at("combine")
                        .say(narration)
                        .var("i", i).var("s", s)
                        .var("skip", skip).var("take", take).var("dp[i][s]", total)
                        .dpTable(combineTable)
                        .step();

                dp[i][s] = total;
                settled[i][s] = true;
            }
        }

        emit.at("done")
                .say("dp[%d][%d] = %d: that many partitions split the array with "
                        + "S1 - S2 = %d.", n, target, dp[n][target], d)
                .var("answer", dp[n][target])
                .dpTable(table(rowLabels, colLabels, dp, settled, null, "?", Set.of(), true))
                .step();
    }

    /** A one-cell checkpoint table for the algebraic steps before (or instead of) the
     * real subset-count table exists — never a fabricated computed cell. */
    private static DpTable checkpoint(String label, String value) {
        return new DpTable(List.of("check"), List.of(label),
                List.of(List.of(new DpCell(value, "void"))));
    }

    private static DpTable table(List<String> rowLabels, List<String> colLabels, int[][] dp,
            boolean[][] settled, SubsetCountDpTable.Coord probe, String probeValue,
            Set<SubsetCountDpTable.Coord> reads, boolean done) {
        return SubsetCountDpTable.of(rowLabels, colLabels, dp, settled, probe, probeValue,
                reads, done);
    }
}
