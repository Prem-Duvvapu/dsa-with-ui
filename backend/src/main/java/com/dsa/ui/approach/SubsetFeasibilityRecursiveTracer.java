package com.dsa.ui.approach;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import java.util.*;

/** Two explicitly registered boolean lessons; partition owns its real parity reduction. */
final class SubsetFeasibilityRecursiveTracer implements AlgorithmTracer {
    enum Kind { SUBSET, EQUAL_PARTITION }
    private final Kind kind;
    private final boolean memo;
    SubsetFeasibilityRecursiveTracer(Kind kind, boolean memo) { this.kind = Objects.requireNonNull(kind); this.memo = memo; }
    public String id() { return switch (kind) { case SUBSET -> "subset-sum-equal-target"; case EQUAL_PARTITION -> "partition-equal-subset-sum"; }; }
    public DsType dsType() { return DsType.RECURSION_TREE; }
    public String annotatedCode() { return SolutionSource.read("/solutions/dp-approaches/" + id() + "/" + (memo ? "memoization" : "recursion") + ".java"); }
    public InputSpec inputSpec() {
        var nums = InputField.of("nums", FieldType.INT_ARRAY).label("Values").length(1, memo ? 10 : 6).values(0, 20)
                .defaultValue(kind == Kind.SUBSET ? List.of(1, 2, 3, 7) : List.of(1, 5, 11, 5))
                .help("Non-negative indexed values; zero is allowed. " + (memo
                        ? "Memo needs (n+1) × (target+1) ≤ 80 cells; partition derives target=sum/2 after its parity check."
                        : "Up to 6 items keeps the full recursive tree below 220 calls.")).build();
        return kind == Kind.EQUAL_PARTITION ? InputSpec.of(nums) : InputSpec.of(nums,
                InputField.of("target", FieldType.INT).label("Target sum K").range(0, 40).defaultValue(6).build());
    }
    public Map<String, Object> alternateInput() { return kind == Kind.SUBSET ? Map.of("nums", List.of(2, 4), "target", 3) : Map.of("nums", List.of(1, 2, 3, 5)); }
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int target;
        if (kind == Kind.EQUAL_PARTITION) {
            int sum = Arrays.stream(nums).sum();
            if (sum % 2 != 0) {
                emit.at("odd").say("Total sum %d is odd; equal integer subset sums are impossible. No recursive call or memo state was needed.", sum)
                        .var("event", "odd").var("totalSum", sum).var("answer", false).var("calls", 0).var("cacheHits", 0)
                        .var("computedStates", 0).var("stateEvaluations", 0).step();
                return;
            }
            target = sum / 2;
        } else target = in.getInt("target");
        if (memo && (long) (nums.length + 1) * (target + 1) > 80)
            throw new InputValidationException(Map.of("nums", "Memo tracing needs (n+1) × (target+1) ≤ 80 cells; use fewer/smaller values or a smaller target."));
        var execution = new RecursiveBooleanDpExecution(emit, memo, nums.length, target);
        var root = new RecursiveBooleanDpExecution.State(nums.length, target);
        execution.start(root, (kind == Kind.EQUAL_PARTITION ? "Even total sum: reduce equal partition to target=sum/2=" : "Find a subset with target=")
                + target + ". Preserve every submitted indexed item, including zeros.");
        execution.done(root, possible(nums.length, target, nums, execution));
    }
    private boolean possible(int n, int sum, int[] nums, RecursiveBooleanDpExecution execution) {
        return execution.evaluate(new RecursiveBooleanDpExecution.State(n, sum), () -> {
            if (n == 0) return sum == 0;
            boolean skip = possible(n - 1, sum, nums, execution);
            boolean take = nums[n - 1] <= sum && possible(n - 1, sum - nums[n - 1], nums, execution);
            return skip || take;
        });
    }
}
