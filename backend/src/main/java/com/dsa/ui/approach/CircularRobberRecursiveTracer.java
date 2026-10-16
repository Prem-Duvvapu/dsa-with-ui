package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Two independently keyed linear ranges; the driver is not invented as a recursive root. */
final class CircularRobberRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    CircularRobberRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "house-robber-2"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id() + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("nums", FieldType.INT_ARRAY).label("House values")
                .length(2, memo ? 16 : 10).values(0, 999).defaultValue(List.of(2, 7, 9, 3, 1))
                .help("The first and last houses are adjacent. Evaluate two allowed ranges separately. "
                        + (memo ? "Cache keys include the range; zero is known." : "At most 10 houses keeps both call trees visible.")).build());
    }
    public Map<String,Object> alternateInput() { return Map.of("nums", List.of(1, 2, 1, 1)); }
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        var execution = new RecursiveDpExecution(emit, memo, 1, List.of("pass 0: skip last", "pass 1: skip first"),
                RecursiveDpExecution.labels("n=", nums.length), null, "best");
        var context = new RecursiveDpExecution.State(0, -1, -1, "maximum of both circular breaks");
        execution.start(context, "Two separate call trees, one excluding each end. The cache key includes the pass; do not share prefix results across ranges.");
        execution.note("passOne", context, "Pass 0 allows houses 0 through " + (nums.length - 2) + "; exclude the last house.");
        int first = best(0, nums.length - 1, nums, execution);
        execution.note("passTwo", context, "Pass 1 allows houses 1 through " + (nums.length - 1) + "; exclude the first house. Keep the first range's cache separate.");
        int second = best(1, nums.length - 1, nums, execution);
        execution.done(context, Math.max(first, second));
    }
    private int best(int pass, int n, int[] nums, RecursiveDpExecution execution) {
        var state = new RecursiveDpExecution.State(0, pass, n, "pass=" + pass + ",n=" + n);
        return execution.evaluate(state, n <= 1, "Skip the last allowed house, or take it and exclude its predecessor within this same range.", () -> {
            if (n <= 1) return n == 0 ? 0 : nums[pass];
            int skip = best(pass, n - 1, nums, execution);
            int take = nums[pass + n - 1] + best(pass, n - 2, nums, execution);
            return Math.max(skip, take);
        });
    }
}
