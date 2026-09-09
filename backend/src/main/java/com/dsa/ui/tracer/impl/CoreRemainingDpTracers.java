package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.AlgorithmTracer;
import com.dsa.ui.tracer.FieldType;
import com.dsa.ui.tracer.InputField;
import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.Inputs;
import com.dsa.ui.tracer.StepEmitter;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class RemainingDpTracer implements AlgorithmTracer {
    @Override public DsType dsType() { return DsType.DP_TABLE; }
    @Override public String annotatedCode() { return DpTraceSupport.CODE; }

    static void bases(long[][] dp, boolean[][] known) {
        for (int r = 0; r < dp.length; r++) known[r][0] = true;
        for (int c = 0; c < dp[0].length; c++) known[0][c] = true;
    }
}

@Component
class LongestCommonSubsequenceRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "longest-common-subsequence"; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(DpTraceSupport.text("first", "First string", "abcde", 12),
                DpTraceSupport.text("second", "Second string", "ace", 12));
    }
    @Override public Map<String, Object> alternateInput() {
        return Map.of("first", "aggtab", "second", "gxtxayb");
    }
    @Override public void run(Inputs in, StepEmitter emit) {
        String a = in.getString("first"), b = in.getString("second");
        long[][] dp = new long[a.length() + 1][b.length() + 1];
        boolean[][] known = new boolean[dp.length][dp[0].length]; bases(dp, known);
        emit.at("init").say("dp[i][j] is the LCS length of the first i and j characters; empty prefixes have length 0.")
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), false,
                        "equal ? 1 + dp[i-1][j-1] : max(dp[i-1][j], dp[i][j-1])", "base = 0")).step();
        for (int i = 1; i < dp.length; i++) for (int j = 1; j < dp[0].length; j++) {
            boolean match = a.charAt(i - 1) == b.charAt(j - 1);
            dp[i][j] = match ? 1 + dp[i - 1][j - 1] : Math.max(dp[i - 1][j], dp[i][j - 1]);
            known[i][j] = true;
            Set<DpTraceSupport.Coord> reads = match
                    ? Set.of(new DpTraceSupport.Coord(i - 1, j - 1))
                    : Set.of(new DpTraceSupport.Coord(i - 1, j), new DpTraceSupport.Coord(i, j - 1));
            emit.at("fill").say("Compare '%c' and '%c': dp[%d][%d] = %d.", a.charAt(i - 1), b.charAt(j - 1), i, j, dp[i][j])
                    .var("i", i).var("j", j).var("value", dp[i][j])
                    .dpTable(DpTraceSupport.table(dp, known, new DpTraceSupport.Coord(i, j), reads, false,
                            "equal ? 1 + diagonal : max(up, left)", match ? "1 + diagonal = " + dp[i][j] : "max(up, left) = " + dp[i][j])).step();
        }
        emit.at("done").say("The longest common subsequence length is %d.", dp[a.length()][b.length()])
                .var("answer", dp[a.length()][b.length()]).dpTable(DpTraceSupport.table(dp, known, null, Set.of(), true,
                        "answer = dp[m][n]", "answer = " + dp[a.length()][b.length()])).step();
    }
}

@Component
class PartitionSetMinAbsDiffRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "partition-set-min-abs-diff"; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(DpTraceSupport.intArray("nums", "Numbers", DpTraceSupport.list(1, 2), 1, 12, 0, 30));
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("nums", List.of(3, 1, 4, 2, 2)); }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums"); int total = Arrays.stream(nums).sum();
        long[][] dp = new long[nums.length + 1][total + 1]; boolean[][] known = new boolean[dp.length][dp[0].length];
        for (int i = 0; i <= nums.length; i++) { dp[i][0] = 1; known[i][0] = true; }
        for (int s = 1; s <= total; s++) known[0][s] = true;
        emit.at("init").say("dp[i][s] records whether the first i values can form sum s; sum 0 is reachable.")
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), false, "reachable(i,s) = skip OR take", "dp[i][0] = true")).step();
        for (int i = 1; i <= nums.length; i++) for (int s = 1; s <= total; s++) {
            boolean reachable = dp[i - 1][s] == 1 || (s >= nums[i - 1] && dp[i - 1][s - nums[i - 1]] == 1);
            dp[i][s] = reachable ? 1 : 0; known[i][s] = true;
            Set<DpTraceSupport.Coord> reads = s >= nums[i - 1]
                    ? Set.of(new DpTraceSupport.Coord(i - 1, s), new DpTraceSupport.Coord(i - 1, s - nums[i - 1]))
                    : Set.of(new DpTraceSupport.Coord(i - 1, s));
            emit.at("fill").say("Using %d values, sum %d is %s.", i, s, reachable ? "reachable" : "unreachable")
                    .var("i", i).var("sum", s).dpTable(DpTraceSupport.table(dp, known,
                            new DpTraceSupport.Coord(i, s), reads, false, "skip OR take", reachable ? "1" : "0")).step();
        }
        int best = total;
        for (int s = 0; s <= total / 2; s++) if (dp[nums.length][s] == 1) best = Math.min(best, total - 2 * s);
        emit.at("done").say("The minimum partition difference is %d.", best).var("answer", best)
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), true, "min |total - 2*s|", "answer = " + best)).step();
    }
}

@Component
class AssignCookiesDpRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "assign-cookies-dp"; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(DpTraceSupport.intArray("greed", "Child greed", DpTraceSupport.list(1, 2, 3), 1, 10, 1, 20),
                DpTraceSupport.intArray("cookies", "Cookie sizes", DpTraceSupport.list(1, 1), 1, 10, 1, 20));
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("greed", List.of(1, 2), "cookies", List.of(1, 2, 3)); }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] greed = in.getIntArray("greed"), cookies = in.getIntArray("cookies"); Arrays.sort(greed); Arrays.sort(cookies);
        long[][] dp = new long[greed.length + 1][cookies.length + 1]; boolean[][] known = new boolean[dp.length][dp[0].length]; bases(dp, known);
        emit.at("init").say("After sorting, dp[i][j] is the most satisfied among the first i children using the first j cookies.")
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), false, "max(skip cookie, match if large enough)", "empty side = 0")).step();
        for (int i = 1; i <= greed.length; i++) for (int j = 1; j <= cookies.length; j++) {
            long skipCookie = dp[i][j - 1], skipChild = dp[i - 1][j];
            long match = cookies[j - 1] >= greed[i - 1] ? 1 + dp[i - 1][j - 1] : -1;
            dp[i][j] = Math.max(Math.max(skipCookie, skipChild), match); known[i][j] = true;
            emit.at("fill").say("Child greed %d, cookie %d: best satisfied count is %d.", greed[i - 1], cookies[j - 1], dp[i][j])
                    .var("i", i).var("j", j).var("value", dp[i][j])
                    .dpTable(DpTraceSupport.table(dp, known, new DpTraceSupport.Coord(i, j),
                            Set.of(new DpTraceSupport.Coord(i, j - 1), new DpTraceSupport.Coord(i - 1, j),
                                    new DpTraceSupport.Coord(i - 1, j - 1)), false,
                            "max(skip cookie, skip child, match)", "max(" + skipCookie + ", " + skipChild + ", " + match + ") = " + dp[i][j])).step();
        }
        emit.at("done").say("Maximum satisfied children: %d.", dp[greed.length][cookies.length])
                .var("answer", dp[greed.length][cookies.length]).dpTable(DpTraceSupport.table(dp, known, null, Set.of(), true,
                        "answer = dp[n][m]", "answer = " + dp[greed.length][cookies.length])).step();
    }
}

@Component
class TargetSumDpRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "target-sum-dp"; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(DpTraceSupport.intArray("nums", "Numbers", DpTraceSupport.list(1, 1), 1, 10, 0, 10),
                InputField.of("target", FieldType.INT).label("Target").range(-30, 30).defaultValue(0).build());
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("nums", List.of(1, 2, 1), "target", 0); }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums"); int target = in.getInt("target"), total = Arrays.stream(nums).sum(), width = 2 * total + 1;
        long[][] dp = new long[nums.length + 1][width]; boolean[][] known = new boolean[dp.length][width];
        dp[0][total] = 1; Arrays.fill(known[0], true);
        emit.at("init").say("dp[i][s] counts sign assignments for the first i numbers; the centre column represents sum 0.")
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), false, "ways(next) += ways(current) for ±value", "ways[0][0] = 1")).step();
        for (int i = 1; i <= nums.length; i++) {
            for (int col = 0; col < width; col++) {
                int plus = col - nums[i - 1], minus = col + nums[i - 1];
                dp[i][col] = (plus >= 0 ? dp[i - 1][plus] : 0) + (minus < width ? dp[i - 1][minus] : 0); known[i][col] = true;
                Set<DpTraceSupport.Coord> reads = plus >= 0 && minus < width
                        ? Set.of(new DpTraceSupport.Coord(i - 1, plus), new DpTraceSupport.Coord(i - 1, minus))
                        : plus >= 0 ? Set.of(new DpTraceSupport.Coord(i - 1, plus))
                        : minus < width ? Set.of(new DpTraceSupport.Coord(i - 1, minus)) : Set.of();
                emit.at("fill").say("After value %d, sum %d has %d assignments.", nums[i - 1], col - total, dp[i][col])
                        .var("i", i).var("sum", col - total).var("ways", dp[i][col])
                        .dpTable(DpTraceSupport.table(dp, known, new DpTraceSupport.Coord(i, col), reads, false,
                                "ways(s) = ways(s-v) + ways(s+v)", "ways = " + dp[i][col])).step();
            }
        }
        long answer = Math.abs(target) <= total ? dp[nums.length][target + total] : 0;
        emit.at("done").say("There are %d sign assignments reaching %d.", answer, target).var("answer", answer)
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), true, "answer = ways[n][target]", "answer = " + answer)).step();
    }
}

@Component
class RodCuttingRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "rod-cutting-problem"; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(DpTraceSupport.intArray("prices", "Prices by piece length", DpTraceSupport.list(2, 5, 7, 8, 10), 1, 12, 0, 100));
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("prices", List.of(3, 5, 8, 9)); }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] prices = in.getIntArray("prices");
        int n = prices.length;
        long[][] dp = new long[n + 1][n + 1]; boolean[][] known = new boolean[dp.length][dp[0].length]; bases(dp, known);
        emit.at("init").say("dp[i][length] is the best price using piece lengths 1 through i; pieces may repeat.")
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), false, "max(skip, price[i] + reuse)", "length 0 = 0")).step();
        for (int i = 1; i <= n; i++) for (int len = 1; len <= n; len++) {
            long skip = dp[i - 1][len], take = len >= i ? prices[i - 1] + dp[i][len - i] : Long.MIN_VALUE / 4;
            dp[i][len] = Math.max(skip, take); known[i][len] = true;
            Set<DpTraceSupport.Coord> reads = len >= i
                    ? Set.of(new DpTraceSupport.Coord(i - 1, len), new DpTraceSupport.Coord(i, len - i))
                    : Set.of(new DpTraceSupport.Coord(i - 1, len));
            emit.at("fill").say("Piece length %d at price %d, rod length %d: best value %d.", i, prices[i - 1], len, dp[i][len])
                    .var("piece", i).var("length", len).var("value", dp[i][len])
                    .dpTable(DpTraceSupport.table(dp, known, new DpTraceSupport.Coord(i, len), reads, false,
                            "max(skip, price + reuse)", "max(" + skip + ", " + (len >= i ? take : "n/a") + ") = " + dp[i][len])).step();
        }
        emit.at("done").say("Maximum revenue for rod length %d is %d.", n, dp[n][n]).var("answer", dp[n][n])
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), true, "answer = dp[n][n]", "answer = " + dp[n][n])).step();
    }
}
