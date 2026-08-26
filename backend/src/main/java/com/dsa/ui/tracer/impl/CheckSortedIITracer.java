package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Check if array is sorted and rotated: count the number of circular drops where
 * nums[i] > nums[(i+1)%n]. A sorted and rotated array has at most one drop.
 */
@Component
public class CheckSortedIITracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "check-sorted-ii";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Check if array was originally sorted in non-decreasing order and then rotated.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(3, 4, 5, 1, 2))
                        .build());
    }

    /** Two drops (2 > 1 and circular 4 > 2): returns false instead of true. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(2, 1, 3, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean check(int[] nums) {
                   // @a init
                   int count = 0;
                   int n = nums.length;
                   for (int i = 0; i < n; i++) {
                       // @a compare
                       if (nums[i] > nums[(i + 1) % n]) {
                           // @a drop
                           count++;
                       }
                   }
                   // @a done
                   return count <= 1;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int count = 0;
        int n = nums.length;

        emit.using("Array");
        emit.at("init")
                .say("Initialize drop count = 0 for array length n = %d.", n)
                .var("count", 0).var("n", n).array(nums).step();

        for (int i = 0; i < n; i++) {
            int next = (i + 1) % n;
            if (nums[i] > nums[next]) {
                emit.at("compare")
                        .say("i=%d: Circular pair nums[%d]=%d > nums[%d]=%d (drop detected).", i, i, nums[i], next, nums[next])
                        .var("i", i).var("next", next).var("count", count).array(nums, i, next).step();
                count++;
                emit.at("drop")
                        .say("Increment drop count to %d.", count)
                        .var("i", i).var("next", next).var("count", count).array(nums, i, next).step();
            } else {
                emit.at("compare")
                        .say("i=%d: Circular pair nums[%d]=%d <= nums[%d]=%d (no drop, count stays %d).", i, i, nums[i], next, nums[next], count)
                        .var("i", i).var("next", next).var("count", count).array(nums, i, next).step();
            }
        }

        boolean result = count <= 1;
        emit.at("done")
                .say("Scan complete: found %d drop(s) (<= 1 drop means sorted and rotated: %s).", count, result)
                .var("count", count).var("result", result).array(nums).step();
    }
}
