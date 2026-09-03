package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coin Change II / LeetCode 518: the number of distinct COMBINATIONS (order does not matter)
 * of coins that sum to an amount, given unlimited supply of each denomination. Same
 * unbounded-reuse shape as {@link MinimumCoinsDpTracer} — "count" instead of "minimize" — so
 * "use denomination i again" still reads {@code dp[i][x - coin]} from the SAME row, not
 * {@code dp[i-1][x - coin]} from the row above.
 *
 * <p>The classic bug in this exact problem is iterating amount in the outer loop and
 * denominations in the inner loop of a 1-D array, which counts {1,2} and {2,1} as two
 * separate combinations (permutations, not combinations). Building a genuine 2-D table with
 * denominations as the OUTER loop and amount as the INNER loop — matching the recurrence
 * below — sidesteps that entirely: each denomination's presence/absence is decided once per
 * row, so no combination is ever double-counted regardless of coin order.
 */
@Component
public class CoinChange2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "coin-change-2";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("coins", FieldType.INT_ARRAY)
                        .label("Coin denominations")
                        .help("Unlimited supply of each — a coin may be used more than once.")
                        .length(1, 8).values(1, 25)
                        .defaultValue(List.of(1, 2, 5))
                        .build(),
                InputField.of("amount", FieldType.INT)
                        .label("Target amount")
                        .help("Count combinations (not permutations) of coins that sum to "
                                + "exactly this amount.")
                        .range(1, 40)
                        .defaultValue(5)
                        .build());
    }

    /** coins = [3], amount = 5: only multiples of 3 are reachable, so the count stays 0 —
     *  the branch the default (which always has combinations) never exercises. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("coins", List.of(3), "amount", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public int change(int[] coins, int amount) {
                   int n = coins.length;
                   // @a init
                   int[][] dp = new int[n + 1][amount + 1];
                   for (int i = 0; i <= n; i++) {
                       // @a base
                       dp[i][0] = 1;
                   }
                   for (int x = 1; x <= amount; x++) {
                       // @a unreachableBase
                       dp[0][x] = 0;
                   }
                   for (int i = 1; i <= n; i++) {
                       for (int x = 1; x <= amount; x++) {
                           int without = dp[i - 1][x];
                           if (x < coins[i - 1]) {
                               // @a tooBig
                               dp[i][x] = without;
                           } else {
                               int withAtLeastOne = dp[i][x - coins[i - 1]];
                               // @a combine
                               dp[i][x] = without + withAtLeastOne;
                           }
                       }
                   }
                   // @a done
                   return dp[n][amount];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] coins = in.getIntArray("coins");
        int amount = in.getInt("amount");
        int n = coins.length;

        int[][] dp = new int[n + 1][amount + 1];
        boolean[][] settled = new boolean[n + 1][amount + 1];
        List<String> rowLabels = CoinChangeDpTable.rowLabels(coins);
        List<String> colLabels = CoinChangeDpTable.colLabels(amount);

        emit.at("init")
                .say("dp[i][x] will hold the number of distinct COMBINATIONS of the first i "
                        + "denominations of %s that sum to x — order does not matter, so "
                        + "{1,2} and {2,1} count once. Denominations are the OUTER loop and "
                        + "amount the INNER loop; that ordering is what keeps every "
                        + "combination counted exactly once.", Arrays.toString(coins))
                .var("coins", Arrays.toString(coins)).var("amount", amount)
                .dpTable(table(dp, settled, rowLabels, colLabels, null, "?", Set.of(), false))
                .step();

        for (int i = 0; i <= n; i++) {
            dp[i][0] = 1;
            settled[i][0] = true;
        }
        emit.at("base")
                .say("There is exactly one way to make amount 0 with any set of "
                        + "denominations: use none of them. Column x=0 is 1 all the way "
                        + "down.")
                .var("phase", "base")
                .dpTable(table(dp, settled, rowLabels, colLabels, null, "?", Set.of(), false))
                .step();

        for (int x = 1; x <= amount; x++) {
            dp[0][x] = 0;
            settled[0][x] = true;
        }
        emit.at("unreachableBase")
                .say("With no denominations available yet, there is no way to make any "
                        + "positive amount: row \"0 coins\" is 0 everywhere except x=0.")
                .var("phase", "unreachableBase")
                .dpTable(table(dp, settled, rowLabels, colLabels, null, "?", Set.of(), false))
                .step();

        for (int i = 1; i <= n; i++) {
            int coin = coins[i - 1];
            for (int x = 1; x <= amount; x++) {
                CoinChangeDpTable.Coord here = new CoinChangeDpTable.Coord(i, x);
                int without = dp[i - 1][x];

                if (x < coin) {
                    dp[i][x] = without;
                    settled[i][x] = true;
                    emit.at("tooBig")
                            .say("Amount %d is smaller than coin %d, so no combination here "
                                    + "can use it. dp[%d][%d] simply carries down the count "
                                    + "of combinations that never touch this denomination: "
                                    + "%d.", x, coin, i, x, without)
                            .var("phase", "tooBig").var("row", i).var("col", x)
                            .var("dp[i][x]", without)
                            .dpTable(table(dp, settled, rowLabels, colLabels, here,
                                    String.valueOf(without),
                                    Set.of(new CoinChangeDpTable.Coord(i - 1, x)), false))
                            .step();
                    continue;
                }

                int withAtLeastOne = dp[i][x - coin];
                int total = without + withAtLeastOne;
                dp[i][x] = total;
                settled[i][x] = true;

                Set<CoinChangeDpTable.Coord> reads = Set.of(
                        new CoinChangeDpTable.Coord(i - 1, x),
                        new CoinChangeDpTable.Coord(i, x - coin));

                emit.at("combine")
                        .say("Cell (%d,%d): %d combination(s) never use coin %d at all "
                                + "(dp[%d][%d], the row above), plus %d combination(s) that "
                                + "use AT LEAST ONE more coin %d — read from dp[%d][%d] = %d "
                                + "on THIS SAME ROW, since the coin may be reused. Every "
                                + "combination is counted in exactly one of those two groups: "
                                + "%d + %d = %d.",
                                i, x, without, coin, i - 1, x, withAtLeastOne, coin, i,
                                x - coin, withAtLeastOne, without, withAtLeastOne, total)
                        .var("phase", "combine").var("row", i).var("col", x)
                        .var("without", without).var("withAtLeastOne", withAtLeastOne)
                        .var("dp[i][x]", total)
                        .dpTable(table(dp, settled, rowLabels, colLabels, here,
                                String.valueOf(total), reads, false))
                        .step();
            }
        }

        int answer = dp[n][amount];
        emit.at("done")
                .say("dp[%d][%d] = %d. %s", n, amount, answer,
                        answer == 0
                                ? "No combination of these denominations sums exactly to "
                                        + amount + "."
                                : "That count already treats every coin order as the same "
                                        + "combination, because denominations were the outer "
                                        + "loop.")
                .var("phase", "done")
                .var("answer", answer)
                .dpTable(table(dp, settled, rowLabels, colLabels, null, "?", Set.of(), true))
                .step();
    }

    private static com.dsa.ui.model.DpTable table(int[][] dp, boolean[][] settled,
                                                  List<String> rowLabels, List<String> colLabels,
                                                  CoinChangeDpTable.Coord probe,
                                                  String probeValue,
                                                  Set<CoinChangeDpTable.Coord> reads,
                                                  boolean done) {
        return CoinChangeDpTable.of(dp, settled, rowLabels, colLabels, probe, probeValue, reads,
                done, false);
    }
}
