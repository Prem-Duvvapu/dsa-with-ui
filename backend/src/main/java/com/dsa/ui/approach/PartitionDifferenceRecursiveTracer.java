package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

final class PartitionDifferenceRecursiveTracer implements AlgorithmTracer {
    private final boolean memo;
    PartitionDifferenceRecursiveTracer(boolean memo) { this.memo = memo; }
    public String id() { return "count-partitions-given-diff"; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id()
            + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        return InputSpec.of(InputField.of("nums", FieldType.INT_ARRAY).label("Values").length(1, memo ? 10 : 6)
                .values(1, 15).defaultValue(List.of(1, 1, 2, 3))
                .help("Positive values, as in the existing visualizer. " + (memo
                        ? "Memo needs (n+1) × (target+1) ≤ 100 cells, where target=(sum+d)/2; reduce values or d if refused."
                        : "Up to 6 indexed items keeps every subset decision visible.")).build(),
                InputField.of("d", FieldType.INT).label("Difference D").range(0, 60).defaultValue(1).build());
    }
    public Map<String, Object> alternateInput() { return Map.of("nums", List.of(1, 2, 3), "d", 1); }
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int d = in.getInt("d"), sum = Arrays.stream(nums).sum();
        if (d > sum || (sum + d) % 2 != 0) {
            emit.at("zero").say("No partition: d exceeds the sum or sum+d is odd. No subset states were evaluated.")
                    .var("answer", 0).var("calls", 0).var("cacheHits", 0).var("computedStates", 0).var("stateEvaluations", 0).step();
            return;
        }
        int target = (sum + d) / 2;
        if (memo && (long) (nums.length + 1) * (target + 1) > 100)
            throw new InputValidationException(Map.of("nums", "Memo tracing needs (n+1) × (target+1) ≤ 100 cells; use fewer/smaller values or a smaller d."));
        var trace = new RecursiveDpExecution(emit, memo, 1, RecursiveDpExecution.labels("items=", nums.length + 1),
                RecursiveDpExecution.labels("sum=", target + 1));
        var root = state(nums.length, target);
        trace.start(root, "S1-S2=d and S1+S2=sum: count indexed subsets S1 with target=(sum+d)/2=" + target + ".");
        trace.done(root, solve(nums.length, target, nums, trace));
    }
    private int solve(int items, int sum, int[] nums, RecursiveDpExecution trace) {
        return trace.evaluate(state(items, sum), items == 0,
                "Skip item " + (items - 1) + "; take it only when its value fits the remaining sum.", () -> {
            if (items == 0) return sum == 0 ? 1 : 0;
            int skip = solve(items - 1, sum, nums, trace);
            int take = nums[items - 1] <= sum ? solve(items - 1, sum - nums[items - 1], nums, trace) : 0;
            return skip + take;
        });
    }
    private static RecursiveDpExecution.State state(int items, int sum) {
        return new RecursiveDpExecution.State(0, items, sum, "items=" + items + ",sum=" + sum);
    }
}
