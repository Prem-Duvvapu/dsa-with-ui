package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpTable;
import com.dsa.ui.tracer.FieldType;
import com.dsa.ui.tracer.InputField;
import com.dsa.ui.tracer.InputSpec;
import com.dsa.ui.tracer.Inputs;
import com.dsa.ui.tracer.StepEmitter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class LisRemainingTracer extends RemainingDpTracer {
    protected abstract List<Integer> defaults();
    @Override public InputSpec inputSpec() {
        return InputSpec.of(DpTraceSupport.intArray("nums", "Numbers", defaults(), 1, 15, 1, 100));
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("nums", List.of(1, 3, 5, 4, 7)); }

    static DpTable rows(long[][] dp, boolean[][] known, DpTraceSupport.Coord probe,
                        Set<DpTraceSupport.Coord> reads, boolean done, List<String> names,
                        String formula, String substitution) {
        return DpTraceSupport.table(dp, known, probe, reads, names,
                DpTraceSupport.labels("i", dp[0].length), done, formula, substitution);
    }
}

@Component
class LongestStringChainRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "longest-string-chain"; }
    @Override public String annotatedCode() { return DpTraceSupport.CODE_WITH_RECONSTRUCTION; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("words", FieldType.STRING).label("Comma-separated words")
                .length(1, 100).constraint("pattern", "[a-z]+(,[a-z]+)*")
                .constraint("patternHint", "Lowercase words separated by commas.")
                .defaultValue("a,b,ba,bca,bda,bdca").build());
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("words", "xbc,pcxbcf,xb,cxbc,pcxbc"); }
    @Override public void run(Inputs in, StepEmitter emit) {
        String[] words = in.getString("words").split(","); Arrays.sort(words, Comparator.comparingInt(String::length).thenComparing(s -> s));
        long[][] state = new long[2][words.length]; boolean[][] known = new boolean[2][words.length];
        Arrays.fill(state[0], 1); Arrays.fill(state[1], -1); Arrays.fill(known[0], true); Arrays.fill(known[1], true);
        emit.at("init").say("Sort by length; chain[i] starts at 1 and parent[i] starts unset.")
                .dpTable(LisRemainingTracer.rows(state, known, null, Set.of(), false, List.of("length", "parent"),
                        "chain[i] = 1 + max(chain[j]) for predecessors", "all lengths = 1")).step();
        int best = 0, end = 0;
        for (int i = 0; i < words.length; i++) {
            for (int j = 0; j < i; j++) {
                boolean predecessor = predecessor(words[j], words[i]);
                if (predecessor && state[0][j] + 1 > state[0][i]) { state[0][i] = state[0][j] + 1; state[1][i] = j; }
                emit.at("fill").say("Compare \"%s\" -> \"%s\": %s; chain length at %d is %d.", words[j], words[i],
                                predecessor ? "valid predecessor" : "not a predecessor", i, state[0][i])
                        .var("word", words[i]).var("length", state[0][i])
                        .dpTable(LisRemainingTracer.rows(state, known, new DpTraceSupport.Coord(0, i),
                                Set.of(new DpTraceSupport.Coord(0, j)), false, List.of("length", "parent"),
                                "predecessor ? max(current, 1 + prior)", "length = " + state[0][i])).step();
            }
            if (state[0][i] > best) { best = (int) state[0][i]; end = i; }
        }
        List<String> chain = new ArrayList<>(); for (int at = end; at >= 0; at = (int) state[1][at]) { chain.add(words[at]); if (state[1][at] < 0) break; }
        Collections.reverse(chain);
        emit.at("reconstruct").say("Follow parent links to reconstruct %s.", chain).var("chain", chain)
                .dpTable(LisRemainingTracer.rows(state, known, null, Set.of(), false, List.of("length", "parent"), "follow parent", chain.toString())).step();
        emit.at("done").say("Longest string-chain length is %d: %s.", best, chain).var("answer", best).var("chain", chain)
                .dpTable(LisRemainingTracer.rows(state, known, null, Set.of(), true, List.of("length", "parent"), "maximum chain length", "answer = " + best)).step();
    }
    private static boolean predecessor(String small, String large) {
        if (large.length() != small.length() + 1) return false; int i = 0, j = 0;
        while (i < small.length() && j < large.length()) { if (small.charAt(i) == large.charAt(j)) i++; j++; }
        return i == small.length();
    }
}

@Component
class LongestBitonicSubsequenceRemainingTracer extends LisRemainingTracer {
    @Override public String id() { return "longest-bitonic-subsequence"; }
    @Override protected List<Integer> defaults() { return List.of(1, 11, 2, 10, 4, 5, 2, 1); }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums"); int n = nums.length; long[][] dp = new long[2][n]; boolean[][] known = new boolean[2][n];
        Arrays.fill(dp[0], 1); Arrays.fill(dp[1], 1); Arrays.fill(known[0], true); Arrays.fill(known[1], true);
        emit.at("init").say("Every element alone has increasing and decreasing length 1.")
                .dpTable(rows(dp, known, null, Set.of(), false, List.of("LIS→", "LDS←"), "bitonic[i] = lis[i] + lds[i] - 1", "all = 1")).step();
        for (int i = 0; i < n; i++) for (int j = 0; j < i; j++) {
            if (nums[j] < nums[i]) dp[0][i] = Math.max(dp[0][i], 1 + dp[0][j]);
            emit.at("fill").say("Forward pair (%d,%d): increasing length at %d is %d.", j, i, i, dp[0][i])
                    .dpTable(rows(dp, known, new DpTraceSupport.Coord(0, i), Set.of(new DpTraceSupport.Coord(0, j)), false,
                            List.of("LIS→", "LDS←"), "nums[j] < nums[i] ? 1 + lis[j]", "lis = " + dp[0][i])).step();
        }
        for (int i = n - 1; i >= 0; i--) for (int j = n - 1; j > i; j--) {
            if (nums[j] < nums[i]) dp[1][i] = Math.max(dp[1][i], 1 + dp[1][j]);
            emit.at("fill").say("Reverse pair (%d,%d): decreasing length at %d is %d.", i, j, i, dp[1][i])
                    .dpTable(rows(dp, known, new DpTraceSupport.Coord(1, i), Set.of(new DpTraceSupport.Coord(1, j)), false,
                            List.of("LIS→", "LDS←"), "nums[j] < nums[i] ? 1 + lds[j]", "lds = " + dp[1][i])).step();
        }
        long best = 1; for (int i = 0; i < n; i++) best = Math.max(best, dp[0][i] + dp[1][i] - 1);
        emit.at("done").say("Longest bitonic subsequence length: %d.", best).var("answer", best)
                .dpTable(rows(dp, known, null, Set.of(), true, List.of("LIS→", "LDS←"), "max(lis + lds - 1)", "answer = " + best)).step();
    }
}

@Component
class NumberOfLisRemainingTracer extends LisRemainingTracer {
    @Override public String id() { return "number-of-lis"; }
    @Override protected List<Integer> defaults() { return List.of(1, 3, 5, 4, 7); }
    @Override public Map<String, Object> alternateInput() { return Map.of("nums", List.of(2, 2, 2, 2, 2)); }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums"); int n = nums.length; long[][] dp = new long[2][n]; boolean[][] known = new boolean[2][n];
        Arrays.fill(dp[0], 1); Arrays.fill(dp[1], 1); Arrays.fill(known[0], true); Arrays.fill(known[1], true);
        emit.at("init").say("Each index starts with LIS length 1 and one such subsequence.")
                .dpTable(rows(dp, known, null, Set.of(), false, List.of("length", "count"), "extend shorter; add counts on ties", "length=count=1")).step();
        for (int i = 0; i < n; i++) for (int j = 0; j < i; j++) {
            if (nums[j] < nums[i]) {
                if (dp[0][j] + 1 > dp[0][i]) { dp[0][i] = dp[0][j] + 1; dp[1][i] = dp[1][j]; }
                else if (dp[0][j] + 1 == dp[0][i]) dp[1][i] += dp[1][j];
            }
            emit.at("fill").say("Process predecessor %d for index %d: length=%d, count=%d.", j, i, dp[0][i], dp[1][i])
                    .var("i", i).var("length", dp[0][i]).var("count", dp[1][i])
                    .dpTable(rows(dp, known, new DpTraceSupport.Coord(0, i), Set.of(new DpTraceSupport.Coord(0, j), new DpTraceSupport.Coord(1, j)), false,
                            List.of("length", "count"), "extend or merge equal-length counts", "length=" + dp[0][i] + ", count=" + dp[1][i])).step();
        }
        long longest = Arrays.stream(dp[0]).max().orElse(1), count = 0; for (int i = 0; i < n; i++) if (dp[0][i] == longest) count += dp[1][i];
        emit.at("done").say("There are %d longest increasing subsequences of length %d.", count, longest).var("answer", count).var("length", longest)
                .dpTable(rows(dp, known, null, Set.of(), true, List.of("length", "count"), "sum counts where length is maximum", "answer = " + count)).step();
    }
}

@Component
class LargestDivisibleSubsetRemainingTracer extends LisRemainingTracer {
    @Override public String id() { return "largest-divisible-subset"; }
    @Override public String annotatedCode() { return DpTraceSupport.CODE_WITH_RECONSTRUCTION; }
    @Override protected List<Integer> defaults() { return List.of(1, 2, 4, 8); }
    @Override public Map<String, Object> alternateInput() { return Map.of("nums", List.of(1, 2, 3)); }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums"); Arrays.sort(nums); int n = nums.length;
        long[][] dp = new long[2][n]; boolean[][] known = new boolean[2][n]; Arrays.fill(dp[0], 1); Arrays.fill(dp[1], -1); Arrays.fill(known[0], true); Arrays.fill(known[1], true);
        emit.at("init").say("Sort values; every value begins a divisible subset of length 1.")
                .dpTable(rows(dp, known, null, Set.of(), false, List.of("length", "parent"), "divisible ? 1 + length[j]", "all lengths = 1")).step();
        int end = 0;
        for (int i = 0; i < n; i++) { for (int j = 0; j < i; j++) {
            if (nums[i] % nums[j] == 0 && dp[0][j] + 1 > dp[0][i]) { dp[0][i] = dp[0][j] + 1; dp[1][i] = j; }
            emit.at("fill").say("Compare %d and %d: subset length at %d is %d.", nums[j], nums[i], i, dp[0][i])
                    .dpTable(rows(dp, known, new DpTraceSupport.Coord(0, i), Set.of(new DpTraceSupport.Coord(0, j)), false,
                            List.of("length", "parent"), "if divisible, extend predecessor", "length = " + dp[0][i])).step();
        } if (dp[0][i] > dp[0][end]) end = i; }
        List<Integer> subset = new ArrayList<>(); for (int at = end; at >= 0; at = (int) dp[1][at]) { subset.add(nums[at]); if (dp[1][at] < 0) break; }
        Collections.reverse(subset);
        emit.at("reconstruct").say("Follow parent links to reconstruct %s.", subset).var("subset", subset)
                .dpTable(rows(dp, known, null, Set.of(), false, List.of("length", "parent"), "follow parent links", subset.toString())).step();
        emit.at("done").say("Largest divisible subset: %s (length %d).", subset, subset.size()).var("answer", subset.size()).var("subset", subset)
                .dpTable(rows(dp, known, null, Set.of(), true, List.of("length", "parent"), "maximum length", "answer = " + subset.size())).step();
    }
}
