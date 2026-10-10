package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

final class LisRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    LisRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "longest-increasing-subsequence"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id()
            + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("nums", FieldType.INT_ARRAY).label("Array").length(1, memo ? 9 : 6)
                .values(-999, 999).defaultValue(memo ? List.of(10, 9, 2, 5, 3, 7, 101, 18) : List.of(10, 9, 2, 5, 3, 7))
                .help("State (i,p): suffix starting at i; previous taken index is p-1. "
                        + (memo ? "Cache that complete pair; unknown cells are not zero." : "At most 6 values keeps all take/skip calls visible.")).build());
    }
    public Map<String, Object> alternateInput() { return Map.of("nums", List.of(5, 4, 3, 2, 1)); }
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        var trace = new RecursiveDpExecution(emit, memo, 1, RecursiveDpExecution.labels("i=", nums.length + 1),
                RecursiveDpExecution.labels("p=", nums.length + 1));
        var root = state(0, 0);
        trace.start(root, "Find a strictly increasing subsequence. p=0 means no previous element; p>0 means index p-1.");
        trace.done(root, solve(0, 0, nums, trace));
    }
    private int solve(int i, int p, int[] nums, RecursiveDpExecution trace) {
        return trace.evaluate(state(i, p), i == nums.length, "Skip nums[" + i + "]; take it only if p=0 or it exceeds nums[p-1].", () -> {
            if (i == nums.length) return 0;
            int skip = solve(i + 1, p, nums, trace);
            int take = p == 0 || nums[i] > nums[p - 1] ? 1 + solve(i + 1, i + 1, nums, trace) : Integer.MIN_VALUE;
            return Math.max(skip, take);
        });
    }
    private static RecursiveDpExecution.State state(int i, int p) { return new RecursiveDpExecution.State(0, i, p, "i=" + i + ",p=" + p); }
}
