package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Unlike {@link CountSubsequencesSumKTracer}, this only needs one witness — so the moment the
 * running sum equals K, the whole recursion returns true immediately, without deciding the
 * remaining indices at all. That check happens BEFORE the base case: the elements chosen so
 * far, with every later index simply excluded, already form a complete subsequence in their
 * own right.
 */
@Component
public class CheckSubsequenceSumKTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "check-subsequence-sum-k";
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
                        .defaultValue(List.of(1, 2, 3))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Target sum (K)")
                        .range(-100, 100)
                        .defaultValue(5)
                        .build());
    }

    /** No subset of the array can possibly reach this sum - every branch runs to exhaustion. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3), "k", 100);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean checkSubsequenceSum(int[] nums, int k) {
                   return backtrack(0, nums, k, 0);
               }

               private boolean backtrack(int idx, int[] nums, int k, int sum) {
                   if (sum == k) {
                       // @a earlySuccess
                       return true;
                   }
                   if (idx == nums.length) {
                       // @a exhausted
                       return false;
                   }
                   if (backtrack(idx + 1, nums, k, sum + nums[idx])) {
                       // @a include
                       return true;
                   }
                   // @a exclude
                   return backtrack(idx + 1, nums, k, sum);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        boolean found = backtrack(0, nums, k, 0, new ArrayList<>(), emit);

        emit.at(found ? "earlySuccess" : "exhausted")
                .say("Overall result: %s.", found)
                .var("result", found).stack(List.of()).step();
    }

    private boolean backtrack(int idx, int[] nums, int k, int sum, List<Integer> path, StepEmitter emit) {
        emit.push("backtrack(idx=" + idx + ",sum=" + sum + ")");

        if (sum == k) {
            emit.at("earlySuccess")
                    .say("Path %s already sums to %d - a witness found. Excluding every remaining index still "
                            + "gives a valid subsequence, so stop here without deciding index %d onward.",
                            path, sum, idx)
                    .var("sum", sum).stack(path).step();
            emit.pop();
            return true;
        }

        if (idx == nums.length) {
            emit.at("exhausted")
                    .say("Every index decided and sum %d never reached %d along this path.", sum, k)
                    .var("sum", sum).stack(path).step();
            emit.pop();
            return false;
        }

        path.add(nums[idx]);
        boolean includeResult = backtrack(idx + 1, nums, k, sum + nums[idx], path, emit);
        if (includeResult) {
            emit.at("include")
                    .say("Including nums[%d]=%d found a witness deeper in the tree - propagate true upward.",
                            idx, nums[idx])
                    .var("idx", idx).stack(path).step();
            path.remove(path.size() - 1);
            emit.pop();
            return true;
        }
        path.remove(path.size() - 1);

        emit.at("exclude")
                .say("Including nums[%d]=%d found no witness - try excluding it instead.", idx, nums[idx])
                .var("idx", idx).stack(path).step();
        boolean result = backtrack(idx + 1, nums, k, sum, path, emit);

        emit.pop();
        return result;
    }
}
