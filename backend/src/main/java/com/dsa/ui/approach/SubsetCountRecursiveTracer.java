package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

final class SubsetCountRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    SubsetCountRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "count-subsets-with-sum-k"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id() + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("nums", FieldType.INT_ARRAY).label("Values").length(1, memo ? 10 : 6)
                        .values(1, 15).defaultValue(List.of(1, 2, 2, 3)).help("Positive values, matching tabulation's existing domain; equal values at different indices are distinct items. "
                                + (memo ? "Memo needs (n+1) × (k+1) ≤ 80 cells." : "Up to 6 items keeps the complete recursive tree below 220 calls.")).build(),
                InputField.of("k", FieldType.INT).label("Target sum K").range(0, 60).defaultValue(3).build());
    }
    public Map<String, Object> alternateInput() { return Map.of("nums", List.of(5, 10), "k", 3); }
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums"); int target = in.getInt("k");
        if (memo && (long) (nums.length + 1) * (target + 1) > 80)
            throw new InputValidationException(Map.of("nums", "Memo tracing needs (n+1) × (k+1) ≤ 80 cells; use fewer numbers or a smaller k."));
        var execution = new RecursiveDpExecution(emit, memo, 1, RecursiveDpExecution.labels("n=", nums.length + 1), RecursiveDpExecution.labels("s=", target + 1), null, "count");
        var root = state(nums.length, target);
        execution.start(root, "Count indexed subsets reaching sum " + target + ". Duplicate values at different positions remain separate choices.");
        execution.done(root, count(nums.length, target, nums, execution));
    }
    private int count(int n, int sum, int[] nums, RecursiveDpExecution execution) {
        return execution.evaluate(state(n, sum), n == 0, "Skip this indexed item first; also take it if its value fits the remaining sum.", () -> {
            if (n == 0) return sum == 0 ? 1 : 0;
            int skip = count(n - 1, sum, nums, execution);
            int take = nums[n - 1] <= sum ? count(n - 1, sum - nums[n - 1], nums, execution) : 0;
            return skip + take;
        });
    }
    private static RecursiveDpExecution.State state(int n, int sum) { return new RecursiveDpExecution.State(0, n, sum, "n=" + n + ",s=" + sum); }
}
