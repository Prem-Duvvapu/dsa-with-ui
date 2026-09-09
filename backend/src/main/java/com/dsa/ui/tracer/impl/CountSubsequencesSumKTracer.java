package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Every index still gets both an include and an exclude call — no pruning, unlike
 * {@link SubsequencesPatternsTheoryTracer} — because a running sum below K might still reach
 * K later, and one above K can never come back down (values may be non-positive here, so
 * even that is not assumed). A leaf contributes 1 to the count only when its final sum is
 * exactly K; every other leaf contributes 0, silently.
 */
@Component
public class CountSubsequencesSumKTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-subsequences-sum-k";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .length(1, 6).values(-20, 20)
                        .defaultValue(List.of(1, 2, 1))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Target sum (K)")
                        .range(-100, 100)
                        .defaultValue(2)
                        .build());
    }

    /** Every element equal - many different subsequences land on the same sum. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 1, 1, 1), "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public int countSubsequences(int[] nums, int k) {
                   int total = backtrack(0, nums, k, 0);
                   // @a done
                   return total;
               }

               private int backtrack(int idx, int[] nums, int k, int sum) {
                   if (idx == nums.length) {
                       // @a leaf
                       return sum == k ? 1 : 0;
                   }
                   int withCurrent = backtrack(idx + 1, nums, k, sum + nums[idx]);
                   // @a include
                   int withoutCurrent = backtrack(idx + 1, nums, k, sum);
                   // @a exclude
                   return withCurrent + withoutCurrent;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        int total = backtrack(0, nums, k, 0, new ArrayList<>(), emit);

        emit.at("done")
                .say("Recursion fully unwound. %d subsequence%s of %s sum to %d.",
                        total, total == 1 ? "" : "s", java.util.Arrays.toString(nums), k)
                .var("count", total).stack(List.of()).step();
    }

    private int backtrack(int idx, int[] nums, int k, int sum, List<Integer> path, StepEmitter emit) {
        emit.push("backtrack(idx=" + idx + ",sum=" + sum + ")");

        if (idx == nums.length) {
            int contributes = sum == k ? 1 : 0;
            emit.at("leaf")
                    .say("Every index decided. Path %s sums to %d, %s %d - contributes %d.",
                            path, sum, sum == k ? "matches" : "does not match", k, contributes)
                    .var("sum", sum).var("contributes", contributes).stack(path).step();
            emit.pop();
            return contributes;
        }

        path.add(nums[idx]);
        emit.at("include")
                .say("Include nums[%d]=%d - sum becomes %d. Recurse into index %d.",
                        idx, nums[idx], sum + nums[idx], idx + 1)
                .var("idx", idx).var("sum", sum + nums[idx]).stack(path).step();
        int withCurrent = backtrack(idx + 1, nums, k, sum + nums[idx], path, emit);
        path.remove(path.size() - 1);

        emit.at("exclude")
                .say("Exclude nums[%d]=%d - sum stays %d. Recurse into index %d.", idx, nums[idx], sum, idx + 1)
                .var("idx", idx).var("sum", sum).stack(path).step();
        int withoutCurrent = backtrack(idx + 1, nums, k, sum, path, emit);

        int total = withCurrent + withoutCurrent;
        emit.pop();
        return total;
    }
}
