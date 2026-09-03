package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Coin Change I / LeetCode 322: the fewest coins that sum to exactly an amount, given
 * unlimited supply of each denomination. This is the "unbounded knapsack" pattern, the
 * deliberate complement to the 0/1-item subset-sum family traced elsewhere in this repo:
 * because a coin may be reused, "take denomination i again" reads {@code dp[i][x - coin]}
 * from the SAME row rather than {@code dp[i-1][x - coin]} from the row above. That is the
 * one thing every step here exists to make visible — {@link CoinChangeDpTable} highlights
 * both the same-row reuse read and the row-above skip read on every comparison so a learner
 * can tell them apart.
 *
 * <p>An amount no combination of the considered denominations can reach is tracked with an
 * explicit sentinel ({@link CoinChangeDpTable#INFINITY}) and rendered as "∞", never as a raw
 * huge integer with no explanation; the final answer reports that as -1, matching LeetCode's
 * contract.
 */
@Component
public class MinimumCoinsDpTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "minimum-coins-dp";
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
                        .help("The exact amount to make with the fewest coins.")
                        .range(1, 40)
                        .defaultValue(11)
                        .build());
    }

    /** coins = [2], amount = 3: 2 never sums to an odd number, so the target is unreachable
     *  — the branch the default (which always has a solution) never exercises. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("coins", List.of(2), "amount", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public int coinChange(int[] coins, int amount) {
                   int n = coins.length;
                   final int INFINITY = 1_000_000;
                   // @a init
                   int[][] dp = new int[n + 1][amount + 1];
                   for (int i = 0; i <= n; i++) {
                       // @a base
                       dp[i][0] = 0;
                   }
                   for (int x = 1; x <= amount; x++) {
                       // @a unreachableBase
                       dp[0][x] = INFINITY;
                   }
                   for (int i = 1; i <= n; i++) {
                       for (int x = 1; x <= amount; x++) {
                           int skip = dp[i - 1][x];
                           if (x < coins[i - 1]) {
                               // @a tooBig
                               dp[i][x] = skip;
                           } else {
                               int reused = dp[i][x - coins[i - 1]];
                               int take = reused >= INFINITY ? INFINITY : reused + 1;
                               // @a compare
                               dp[i][x] = Math.min(skip, take);
                           }
                       }
                   }
                   int answer = dp[n][amount];
                   // @a done
                   return answer >= INFINITY ? -1 : answer;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] coins = in.getIntArray("coins");
        int amount = in.getInt("amount");
        int n = coins.length;
        int inf = CoinChangeDpTable.INFINITY;

        int[][] dp = new int[n + 1][amount + 1];
        boolean[][] settled = new boolean[n + 1][amount + 1];
        List<String> rowLabels = CoinChangeDpTable.rowLabels(coins);
        List<String> colLabels = CoinChangeDpTable.colLabels(amount);

        emit.at("init")
                .say("dp[i][x] will hold the fewest coins that make amount x using only the "
                        + "first i denominations of %s. Because a coin may be reused, taking "
                        + "denomination i again will look back along ROW i itself, not up to "
                        + "row i-1 the way a 0/1-item table would.", Arrays.toString(coins))
                .var("coins", Arrays.toString(coins)).var("amount", amount)
                .dpTable(table(dp, settled, rowLabels, colLabels, null, "?", Set.of(), false))
                .step();

        for (int i = 0; i <= n; i++) {
            dp[i][0] = 0;
            settled[i][0] = true;
        }
        emit.at("base")
                .say("Zero coins are needed to make amount 0, no matter which denominations "
                        + "are available yet — column x=0 is 0 all the way down.")
                .var("phase", "base")
                .dpTable(table(dp, settled, rowLabels, colLabels, null, "?", Set.of(), false))
                .step();

        for (int x = 1; x <= amount; x++) {
            dp[0][x] = inf;
            settled[0][x] = true;
        }
        emit.at("unreachableBase")
                .say("With no denominations available yet, no positive amount is reachable: "
                        + "row \"0 coins\" is infinity everywhere except x=0.")
                .var("phase", "unreachableBase")
                .dpTable(table(dp, settled, rowLabels, colLabels, null, "?", Set.of(), false))
                .step();

        for (int i = 1; i <= n; i++) {
            int coin = coins[i - 1];
            for (int x = 1; x <= amount; x++) {
                CoinChangeDpTable.Coord here = new CoinChangeDpTable.Coord(i, x);
                int skip = dp[i - 1][x];

                if (x < coin) {
                    dp[i][x] = skip;
                    settled[i][x] = true;
                    emit.at("tooBig")
                            .say("Amount %d is smaller than coin %d, so this denomination "
                                    + "cannot be used here at all. dp[%d][%d] simply carries "
                                    + "down the value from the row above: %s.",
                                    x, coin, i, x, render(skip))
                            .var("phase", "tooBig").var("row", i).var("col", x)
                            .var("dp[i][x]", render(skip))
                            .dpTable(table(dp, settled, rowLabels, colLabels, here,
                                    render(skip), Set.of(new CoinChangeDpTable.Coord(i - 1, x)),
                                    false))
                            .step();
                    continue;
                }

                int reused = dp[i][x - coin];
                int take = reused >= inf ? inf : reused + 1;
                int best = Math.min(skip, take);
                dp[i][x] = best;
                settled[i][x] = true;

                Set<CoinChangeDpTable.Coord> reads = Set.of(
                        new CoinChangeDpTable.Coord(i - 1, x),
                        new CoinChangeDpTable.Coord(i, x - coin));

                String verdict;
                if (take < skip) {
                    verdict = "Using one more coin %d wins: %s.".formatted(coin, render(take));
                } else if (skip < take) {
                    verdict = "Skipping coin %d wins: %s.".formatted(coin, render(skip));
                } else {
                    verdict = "Both options tie at %s.".formatted(render(best));
                }

                emit.at("compare")
                        .say("Cell (%d,%d): skip coin %d and carry down dp[%d][%d] = %s from "
                                + "the row above, or take ONE MORE coin %d by reading "
                                + "dp[%d][%d] = %s from THIS SAME ROW (a coin may be reused) "
                                + "and adding 1 = %s. %s",
                                i, x, coin, i - 1, x, render(skip), coin, i, x - coin,
                                render(reused), render(take), verdict)
                        .var("phase", "compare").var("row", i).var("col", x)
                        .var("skip", render(skip)).var("take", render(take))
                        .var("dp[i][x]", render(best))
                        .dpTable(table(dp, settled, rowLabels, colLabels, here, render(best),
                                reads, false))
                        .step();
            }
        }

        int answer = dp[n][amount];
        boolean impossible = answer >= inf;
        String message = impossible
                ? "dp[%d][%d] is still infinity: no combination of these coins sums exactly "
                        .formatted(n, amount)
                        + "to %d, so the answer is reported as -1.".formatted(amount)
                : "dp[%d][%d] = %d coins make exactly %d.".formatted(n, amount, answer, amount);

        emit.at("done")
                .say(message)
                .var("phase", "done")
                .var("answer", impossible ? -1 : answer)
                .dpTable(table(dp, settled, rowLabels, colLabels, null, "?", Set.of(), true))
                .step();
    }

    private static String render(int value) {
        return value >= CoinChangeDpTable.INFINITY ? "∞" : String.valueOf(value);
    }

    private static com.dsa.ui.model.DpTable table(int[][] dp, boolean[][] settled,
                                                  List<String> rowLabels, List<String> colLabels,
                                                  CoinChangeDpTable.Coord probe,
                                                  String probeValue,
                                                  Set<CoinChangeDpTable.Coord> reads,
                                                  boolean done) {
        return CoinChangeDpTable.of(dp, settled, rowLabels, colLabels, probe, probeValue, reads,
                done, true);
    }
}
