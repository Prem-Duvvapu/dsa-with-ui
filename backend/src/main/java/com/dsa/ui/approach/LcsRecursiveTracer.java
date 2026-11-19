package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.List;
import java.util.Map;

/** Prefix lengths are the complete state; matching characters have one real dependency. */
final class LcsRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    LcsRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "longest-common-subsequence"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() {
        return SolutionSource.read("/solutions/dp-approaches/" + id() + "/" + (memo ? "memoization" : "recursion") + ".java");
    }
    public InputSpec inputSpec() {
        return InputSpec.of(text("first", "First string", memo ? "abcde" : "abcd"),
                text("second", "Second string", memo ? "ace" : "acd"));
    }
    private InputField text(String name, String label, String value) {
        return InputField.of(name, FieldType.STRING).label(label).length(1, memo ? 8 : 4)
                .constraint("pattern", "[a-z]+").constraint("patternHint", "Lowercase letters a-z only.")
                .help(memo ? "Prefix memo states fit strings of up to eight lowercase letters; unvisited cells stay unknown."
                        : "Up to four lowercase letters keeps every recursive call visible. Select Memoization for longer inputs.")
                .defaultValue(value).build();
    }
    public Map<String, Object> alternateInput() {
        return memo ? Map.of("first", "aggtab", "second", "gxtxayb") : Map.of("first", "abc", "second", "xyz");
    }
    public void run(Inputs input, StepEmitter emit) {
        String first = input.getString("first"), second = input.getString("second");
        var execution = new RecursiveDpExecution(emit, memo, 1,
                RecursiveDpExecution.labels("i=", first.length() + 1),
                RecursiveDpExecution.labels("j=", second.length() + 1), null, "lcs");
        var root = state(first.length(), second.length());
        execution.start(root, "Find the common subsequence length of these prefixes; an empty prefix has length zero.");
        execution.done(root, lcs(first, second, first.length(), second.length(), execution));
    }
    private int lcs(String first, String second, int i, int j, RecursiveDpExecution execution) {
        boolean base = i == 0 || j == 0;
        boolean match = !base && first.charAt(i - 1) == second.charAt(j - 1);
        String choices = match ? "Equal final characters: add one to the single diagonal dependency."
                : "Different final characters: evaluate (i-1,j) before (i,j-1), then take the larger length.";
        return execution.evaluate(state(i,j), base, choices, () -> {
            if (i == 0 || j == 0) return 0;
            if (first.charAt(i - 1) == second.charAt(j - 1)) return 1 + lcs(first, second, i - 1, j - 1, execution);
            int withoutFirst = lcs(first, second, i - 1, j, execution);
            int withoutSecond = lcs(first, second, i, j - 1, execution);
            return Math.max(withoutFirst, withoutSecond);
        });
    }
    private static RecursiveDpExecution.State state(int i, int j) {
        return new RecursiveDpExecution.State(0, i, j, "i=" + i + ",j=" + j);
    }
}
