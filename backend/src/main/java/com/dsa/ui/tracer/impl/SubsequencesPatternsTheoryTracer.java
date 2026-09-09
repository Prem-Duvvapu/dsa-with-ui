package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The include/exclude pattern every subsequence problem in this topic builds on, made
 * concrete with one addition beyond a plain enumeration: a running sum and a limit. The
 * "include" branch is only even attempted when adding the current element would not exceed
 * the limit — an early exit that prunes a whole subtree before it is ever explored, rather
 * than generating it and filtering afterward.
 */
@Component
public class SubsequencesPatternsTheoryTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "subsequences-patterns-theory";
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
                InputField.of("limit", FieldType.INT)
                        .label("Sum limit")
                        .range(0, 200)
                        .defaultValue(3)
                        .build());
    }

    /** A generous limit that never prunes - every one of the 2^N subsequences survives. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(2, 4), "limit", 10);
    }

    @Override
    public String annotatedCode() {
        return """
               public void explore(int[] nums, int limit) {
                   backtrack(0, nums, limit, new ArrayList<>(), 0);
               }

               private void backtrack(int idx, int[] nums, int limit, List<Integer> path, int sum) {
                   if (idx == nums.length) {
                       // @a capture
                       System.out.println(path + " sum=" + sum);
                       return;
                   }
                   if (sum + nums[idx] <= limit) {
                       path.add(nums[idx]);
                       // @a include
                       backtrack(idx + 1, nums, limit, path, sum + nums[idx]);
                       path.remove(path.size() - 1);
                   } else {
                       // @a earlyExit
                   }
                   // @a exclude
                   backtrack(idx + 1, nums, limit, path, sum);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int limit = in.getInt("limit");
        int[] count = {0};
        backtrack(0, nums, limit, new ArrayList<>(), 0, count, emit);
    }

    private void backtrack(int idx, int[] nums, int limit, List<Integer> path, int sum, int[] count,
                            StepEmitter emit) {
        emit.push("backtrack(idx=" + idx + ",sum=" + sum + ")");

        if (idx == nums.length) {
            count[0]++;
            emit.at("capture")
                    .say("Every index decided - subsequence %s (sum=%d) is #%d.", path, sum, count[0])
                    .var("subsequence", path.toString()).var("sum", sum).var("count", count[0]).stack(path).step();
            emit.pop();
            return;
        }

        if (sum + nums[idx] <= limit) {
            path.add(nums[idx]);
            emit.at("include")
                    .say("Including nums[%d]=%d keeps the sum at %d, within the limit of %d - recurse.",
                            idx, nums[idx], sum + nums[idx], limit)
                    .var("idx", idx).var("sum", sum + nums[idx]).stack(path).step();
            backtrack(idx + 1, nums, limit, path, sum + nums[idx], count, emit);
            path.remove(path.size() - 1);
        } else {
            emit.at("earlyExit")
                    .say("Including nums[%d]=%d would push the sum to %d, past the limit of %d - the include "
                            + "branch is skipped entirely rather than explored and discarded.",
                            idx, nums[idx], sum + nums[idx], limit)
                    .var("idx", idx).var("wouldBeSum", sum + nums[idx]).stack(path).step();
        }

        emit.at("exclude")
                .say("Leave nums[%d]=%d out and recurse with the sum unchanged at %d.", idx, nums[idx], sum)
                .var("idx", idx).var("sum", sum).stack(path).step();
        backtrack(idx + 1, nums, limit, path, sum, count, emit);

        emit.pop();
    }
}
