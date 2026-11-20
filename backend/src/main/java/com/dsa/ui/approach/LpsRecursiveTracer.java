package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.Map;

/** The canonical prefix-LCS formulation, with this problem's fixed reversed string. */
final class LpsRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;

    LpsRecursiveTracer(boolean memo) { this.memo = memo; }

    @Override public String id() { return "longest-palindromic-subsequence"; }
    @Override public DsType dsType() { return DsType.RECURSION_TREE; }
    @Override public String annotatedCode() {
        return SolutionSource.read("/solutions/dp-approaches/" + id() + "/"
                + (memo ? "memoization" : "recursion") + ".java");
    }
    @Override public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("text", FieldType.STRING).label("Text")
                .length(1, memo ? 8 : 5)
                .constraint("pattern", "[a-z]+")
                .constraint("patternHint", "Lowercase letters a-z only.")
                .help(memo ? "Up to eight lowercase letters keeps the prefix memo table visible; unvisited cells stay unknown."
                        : "Up to five lowercase letters keeps every actual recursive call visible. Select Memoization for longer input.")
                .defaultValue("bbbab").build());
    }
    @Override public Map<String, Object> alternateInput() { return Map.of("text", "cbbd"); }

    @Override public void run(Inputs input, StepEmitter emit) {
        String text = input.getString("text");
        String reversed = new StringBuilder(text).reverse().toString();
        var execution = new RecursiveDpExecution(emit, memo, 1,
                RecursiveDpExecution.labels("i=", text.length() + 1),
                RecursiveDpExecution.labels("j=", reversed.length() + 1), null, "lps");
        var root = state(text.length(), reversed.length());
        execution.start(root, "Find the LCS length of text and its reversal; this equals the longest palindromic subsequence length. Coordinates are prefix lengths, not interval ends.");
        execution.done(root, lps(text, reversed, text.length(), reversed.length(), execution));
    }

    private int lps(String text, String reversed, int i, int j, RecursiveDpExecution execution) {
        boolean base = i == 0 || j == 0;
        boolean match = !base && text.charAt(i - 1) == reversed.charAt(j - 1);
        String choices = match ? "Equal final characters in the fixed text and reversed prefixes: add one to the single diagonal dependency."
                : "Different final characters: evaluate (i-1,j) before (i,j-1), then take the larger prefix LCS length.";
        return execution.evaluate(state(i, j), base, choices, () -> {
            if (i == 0 || j == 0) return 0;
            if (text.charAt(i - 1) == reversed.charAt(j - 1)) {
                return 1 + lps(text, reversed, i - 1, j - 1, execution);
            }
            int withoutText = lps(text, reversed, i - 1, j, execution);
            int withoutReversed = lps(text, reversed, i, j - 1, execution);
            return Math.max(withoutText, withoutReversed);
        });
    }

    private static RecursiveDpExecution.State state(int i, int j) {
        return new RecursiveDpExecution.State(0, i, j, "i=" + i + ",j=" + j);
    }
}
