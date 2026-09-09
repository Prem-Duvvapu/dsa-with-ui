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
import java.util.List;
import java.util.Map;
import java.util.Set;

abstract class MatrixChainRemainingTracer extends RemainingDpTracer {
    protected abstract List<Integer> defaults();
    @Override public InputSpec inputSpec() {
        return InputSpec.of(DpTraceSupport.intArray("dimensions", "Matrix dimensions", defaults(), 2, 9, 1, 100));
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("dimensions", List.of(10, 20, 30, 40)); }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] d = in.getIntArray("dimensions"); int n = d.length - 1;
        long[][] dp = new long[n][n]; boolean[][] known = new boolean[n][n];
        for (int i = 0; i < n; i++) known[i][i] = true;
        emit.at("init").say("dp[i][j] is the minimum cost to multiply matrices i through j; one matrix costs 0.")
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), false, "min(dp[i][k] + dp[k+1][j] + d[i]*d[k+1]*d[j+1])", "diagonal = 0")).step();
        for (int gap = 1; gap < n; gap++) for (int i = 0; i + gap < n; i++) {
            int j = i + gap; long best = Long.MAX_VALUE;
            for (int k = i; k < j; k++) {
                long candidate = dp[i][k] + dp[k + 1][j] + (long) d[i] * d[k + 1] * d[j + 1]; best = Math.min(best, candidate);
                emit.at("fill").say("Interval [%d,%d], split %d: candidate %d; running minimum %d.", i, j, k, candidate, best)
                        .var("i", i).var("j", j).var("split", k).var("candidate", candidate)
                        .dpTable(DpTraceSupport.table(dp, known, new DpTraceSupport.Coord(i, j),
                                Set.of(new DpTraceSupport.Coord(i, k), new DpTraceSupport.Coord(k + 1, j)), false,
                                "left + right + boundary product", candidate + " -> best " + best)).step();
            }
            dp[i][j] = best; known[i][j] = true;
        }
        long answer = dp[0][n - 1];
        emit.at("done").say("Minimum matrix-chain multiplication cost: %d.", answer).var("answer", answer)
                .dpTable(DpTraceSupport.table(dp, known, null, Set.of(), true, "answer = dp[0][n-1]", "answer = " + answer)).step();
    }
}

@Component
class McmCostEvalRemainingTracer extends MatrixChainRemainingTracer {
    @Override public String id() { return "mcm-cost-eval"; }
    @Override protected List<Integer> defaults() { return List.of(40, 20, 30, 10, 30); }
}

@Component
class MatrixChainTheoryRemainingTracer extends MatrixChainRemainingTracer {
    @Override public String id() { return "matrix-chain-multiplication-theory"; }
    @Override protected List<Integer> defaults() { return List.of(10, 30, 5, 60); }
    @Override public Map<String, Object> alternateInput() { return Map.of("dimensions", List.of(5, 10, 3, 12, 5)); }
}

@Component
class EvaluateBooleanExpressionRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "evaluate-boolean-expression"; }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("expression", FieldType.STRING).label("Boolean expression")
                .length(1, 15).constraint("pattern", "([TF])([&|^][TF])*")
                .constraint("patternHint", "Alternate T/F operands with &, |, or ^.")
                .defaultValue("T|F&T^T").build());
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("expression", "T^F|F"); }
    @Override public void run(Inputs in, StepEmitter emit) {
        String expression = in.getString("expression"); int n = (expression.length() + 1) / 2;
        long[][] state = new long[2 * n][n]; boolean[][] known = new boolean[2 * n][n];
        for (int i = 0; i < n; i++) { boolean value = expression.charAt(2 * i) == 'T'; state[i][i] = value ? 1 : 0; state[n + i][i] = value ? 0 : 1; known[i][i] = true; known[n + i][i] = true; }
        List<String> rows = new ArrayList<>(); for (int i = 0; i < n; i++) rows.add("T" + i); for (int i = 0; i < n; i++) rows.add("F" + i);
        emit.at("init").say("Each operand seeds one true/false interval count; larger intervals combine every operator split.")
                .dpTable(DpTraceSupport.table(state, known, null, Set.of(), rows, DpTraceSupport.labels("end", n), false,
                        "combine left/right true and false counts by operator", "single operands")).step();
        for (int gap = 1; gap < n; gap++) for (int i = 0; i + gap < n; i++) {
            int j = i + gap; long t = 0, f = 0;
            for (int k = i; k < j; k++) {
                long lt = state[i][k], lf = state[n + i][k], rt = state[k + 1][j], rf = state[n + k + 1][j]; char op = expression.charAt(2 * k + 1);
                if (op == '&') { t += lt * rt; f += lt * rf + lf * rt + lf * rf; }
                else if (op == '|') { t += lt * rt + lt * rf + lf * rt; f += lf * rf; }
                else { t += lt * rf + lf * rt; f += lt * rt + lf * rf; }
                emit.at("fill").say("Interval [%d,%d], operator '%c' at %d: true=%d, false=%d so far.", i, j, op, k, t, f)
                        .var("trueWays", t).var("falseWays", f)
                        .dpTable(DpTraceSupport.table(state, known, new DpTraceSupport.Coord(i, j), Set.of(), rows,
                                DpTraceSupport.labels("end", n), false, "operator truth-count combination", "T=" + t + ", F=" + f)).step();
            }
            state[i][j] = t; state[n + i][j] = f; known[i][j] = true; known[n + i][j] = true;
        }
        emit.at("done").say("The expression can evaluate to true in %d parenthesizations.", state[0][n - 1]).var("answer", state[0][n - 1])
                .dpTable(DpTraceSupport.table(state, known, null, Set.of(), rows, DpTraceSupport.labels("end", n), true,
                        "answer = true[0][n-1]", "answer = " + state[0][n - 1])).step();
    }
}

@Component
class PalindromePartitioningTwoRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "palindrome-partitioning-2"; }
    @Override public InputSpec inputSpec() { return InputSpec.of(DpTraceSupport.text("text", "Text", "aab", 14)); }
    @Override public Map<String, Object> alternateInput() { return Map.of("text", "abccbc"); }
    @Override public void run(Inputs in, StepEmitter emit) {
        String text = in.getString("text"); int n = text.length(); long[][] state = new long[n + 1][n]; boolean[][] known = new boolean[n + 1][n];
        List<String> rows = new ArrayList<>(); for (int i = 0; i < n; i++) rows.add("start" + i); rows.add("cuts");
        emit.at("init").say("First mark every palindromic interval, then compute minimum cuts for each prefix.")
                .dpTable(DpTraceSupport.table(state, known, null, Set.of(), rows, DpTraceSupport.labels("end", n), false,
                        "pal[i][j] = equal ends && inner palindrome", "unfilled")).step();
        for (int gap = 0; gap < n; gap++) for (int i = 0; i + gap < n; i++) {
            int j = i + gap; boolean palindrome = text.charAt(i) == text.charAt(j) && (gap < 2 || state[i + 1][j - 1] == 1);
            state[i][j] = palindrome ? 1 : 0; known[i][j] = true;
            emit.at("fill").say("Substring [%d,%d] \"%s\" is %sa palindrome.", i, j, text.substring(i, j + 1), palindrome ? "" : "not ")
                    .dpTable(DpTraceSupport.table(state, known, new DpTraceSupport.Coord(i, j), Set.of(), rows,
                            DpTraceSupport.labels("end", n), false, "equal ends && inner palindrome", palindrome ? "1" : "0")).step();
        }
        for (int end = 0; end < n; end++) {
            long best = end;
            if (state[0][end] == 1) best = 0;
            else for (int start = 1; start <= end; start++) if (state[start][end] == 1) best = Math.min(best, state[n][start - 1] + 1);
            state[n][end] = best; known[n][end] = true;
            emit.at("fill").say("Minimum cuts for prefix ending at %d: %d.", end, best).var("end", end).var("cuts", best)
                    .dpTable(DpTraceSupport.table(state, known, new DpTraceSupport.Coord(n, end), Set.of(), rows,
                            DpTraceSupport.labels("end", n), false, "min(previous cuts + 1 for palindromic suffix)", "cuts = " + best)).step();
        }
        emit.at("done").say("Minimum palindrome-partition cuts: %d.", state[n][n - 1]).var("answer", state[n][n - 1])
                .dpTable(DpTraceSupport.table(state, known, null, Set.of(), rows, DpTraceSupport.labels("end", n), true,
                        "answer = cuts[n-1]", "answer = " + state[n][n - 1])).step();
    }
}

@Component
class PartitionArrayMaxSumRemainingTracer extends RemainingDpTracer {
    @Override public String id() { return "partition-array-max-sum"; }
    @Override public InputSpec inputSpec() { return InputSpec.of(
            DpTraceSupport.intArray("nums", "Numbers", DpTraceSupport.list(1, 15, 7, 9, 2, 5, 10), 1, 15, 0, 100),
            InputField.of("k", FieldType.INT).label("Maximum partition length").range(1, 10).defaultValue(3).build()); }
    @Override public Map<String, Object> alternateInput() { return Map.of("nums", List.of(1, 4, 1, 5, 7, 3, 6, 1, 9, 9, 3), "k", 4); }
    @Override public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums"); int k = in.getInt("k"), n = nums.length;
        long[][] state = new long[2][n + 1]; boolean[][] known = new boolean[2][n + 1]; known[0][0] = true; known[1][0] = true;
        emit.at("init").say("best[i] is the maximum transformed sum for the first i values; choice[i] stores its final block length.")
                .dpTable(DpTraceSupport.table(state, known, null, Set.of(), List.of("best", "choice"), DpTraceSupport.labels("prefix", n + 1), false,
                        "best[i] = max(best[i-len] + len * blockMax)", "best[0] = 0")).step();
        for (int i = 1; i <= n; i++) {
            long best = 0; int choice = 0, blockMax = 0;
            for (int len = 1; len <= Math.min(k, i); len++) {
                blockMax = Math.max(blockMax, nums[i - len]); long candidate = state[0][i - len] + (long) len * blockMax;
                if (candidate > best) { best = candidate; choice = len; }
                state[0][i] = best; state[1][i] = choice;
                emit.at("fill").say("Prefix %d, final block length %d with max %d: candidate %d; best %d.", i, len, blockMax, candidate, best)
                        .var("i", i).var("length", len).var("best", best)
                        .dpTable(DpTraceSupport.table(state, known, new DpTraceSupport.Coord(0, i),
                                Set.of(new DpTraceSupport.Coord(0, i - len)), List.of("best", "choice"), DpTraceSupport.labels("prefix", n + 1), false,
                                "best[i-len] + len * blockMax", candidate + " -> best " + best)).step();
            }
            known[0][i] = true; known[1][i] = true;
        }
        emit.at("done").say("Maximum partition-array sum: %d.", state[0][n]).var("answer", state[0][n])
                .dpTable(DpTraceSupport.table(state, known, null, Set.of(), List.of("best", "choice"), DpTraceSupport.labels("prefix", n + 1), true,
                        "answer = best[n]", "answer = " + state[0][n])).step();
    }
}
