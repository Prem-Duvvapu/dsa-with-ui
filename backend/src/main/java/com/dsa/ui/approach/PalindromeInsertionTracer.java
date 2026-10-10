package com.dsa.ui.approach;

import com.dsa.ui.model.*;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Direct interval recurrence, separate from the preserved canonical LCS-with-reverse solution. */
final class PalindromeInsertionTracer implements AlgorithmTracer {
    private final String form;
    PalindromeInsertionTracer(String form) { this.form = form; }
    public String id() { return "min-insertions-palindrome"; }
    public DsType dsType() { return form.equals("tabulation") ? DsType.DP_TABLE : DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id() + "/" + form + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("text", FieldType.STRING).label("Text")
                .length(1, form.equals("recursion") ? 7 : form.equals("memoization") ? 10 : 12)
                .constraint("pattern", "[a-z]+").constraint("patternHint", "Lowercase letters a-z only.")
                .defaultValue("mbadm").help("State (left,right): minimum insertions for this substring. "
                        + "Match equal ends; otherwise insert a mirror of one end. Unknown intervals remain blank.").build());
    }
    public Map<String, Object> alternateInput() { return Map.of("text", form.equals("recursion") ? "abcde" : "leetcode"); }
    public void run(Inputs in, StepEmitter emit) {
        String text = in.getString("text");
        if (form.equals("tabulation")) { tabulate(text, emit); return; }
        var trace = new RecursiveDpExecution(emit, form.equals("memoization"), 1,
                RecursiveDpExecution.labels("left=", text.length() + 1), RecursiveDpExecution.labels("right+1=", text.length() + 1));
        var root = state(0, text.length() - 1);
        trace.start(root, "Find minimum insertions directly on intervals. Cache columns encode right+1, including empty intervals.");
        trace.done(root, solve(0, text.length() - 1, text, trace));
    }
    private int solve(int left, int right, String text, RecursiveDpExecution trace) {
        return trace.evaluate(state(left, right), left >= right,
                "Equal ends use the inner interval; unequal ends choose 1 + the cheaper interval after removing one end.", () -> {
            if (left >= right) return 0;
            if (text.charAt(left) == text.charAt(right)) return solve(left + 1, right - 1, text, trace);
            int removeLeft = solve(left + 1, right, text, trace);
            int removeRight = solve(left, right - 1, text, trace);
            return 1 + Math.min(removeLeft, removeRight);
        });
    }
    private static RecursiveDpExecution.State state(int left, int right) {
        return new RecursiveDpExecution.State(0, left, right + 1, "left=" + left + ",right=" + right);
    }
    private void tabulate(String text, StepEmitter emit) {
        int n = text.length();
        Integer[][] dp = new Integer[n][n];
        for (int i = 0; i < n; i++) dp[i][i] = 0;
        emit.at("base").say("A one-character interval needs zero insertions; uncomputed intervals remain unknown.")
                .dpTable(table(dp, -1, -1, Set.of())).step();
        for (int length = 2; length <= n; length++) for (int left = 0; left + length <= n; left++) {
            int right = left + length - 1;
            boolean equal = text.charAt(left) == text.charAt(right);
            dp[left][right] = equal ? length == 2 ? 0 : dp[left + 1][right - 1]
                    : 1 + Math.min(dp[left + 1][right], dp[left][right - 1]);
            Set<String> reads = equal ? length == 2 ? Set.of() : Set.of((left + 1) + ":" + (right - 1))
                    : Set.of((left + 1) + ":" + right, left + ":" + (right - 1));
            emit.at("fill").say("Interval [%d,%d], ends '%c' and '%c': %s = %d insertions.", left, right,
                    text.charAt(left), text.charAt(right), equal ? "use inner interval" : "1 + min(remove left, remove right)", dp[left][right])
                    .var("left", left).var("right", right).var("value", dp[left][right])
                    .dpTable(table(dp, left, right, reads).withFormula("equal ends ? inner : 1 + min(remove left, remove right)",
                            "interval [" + left + "," + right + "] = " + dp[left][right])).step();
        }
        emit.at("done").say("The full interval needs %d insertions.", dp[0][n - 1]).var("answer", dp[0][n - 1])
                .dpTable(table(dp, 0, n - 1, Set.of())).step();
    }
    private static DpTable table(Integer[][] dp, int left, int right, Set<String> reads) {
        List<List<DpCell>> cells = new ArrayList<>();
        for (int r = 0; r < dp.length; r++) {
            List<DpCell> row = new ArrayList<>();
            for (int c = 0; c < dp.length; c++) row.add(new DpCell(r > c ? "—" : dp[r][c] == null ? "·" : String.valueOf(dp[r][c]),
                    r > c || dp[r][c] == null ? "void" : r == left && c == right ? "probe" : reads.contains(r + ":" + c) ? "read" : "known"));
            cells.add(row);
        }
        return new DpTable(RecursiveDpExecution.labels("left=", dp.length), RecursiveDpExecution.labels("right=", dp.length), cells);
    }
}
