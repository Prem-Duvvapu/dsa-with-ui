package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

/**
 * Every element is the minimum of some range of subarrays - specifically, every subarray
 * that starts somewhere between the previous smaller element and this one, and ends
 * somewhere between this one and the next smaller-or-equal element. A monotonic stack
 * finds both boundaries for every element in one linear pass each; the strict-vs-equal
 * asymmetry between the two passes (previous strictly smaller, next smaller-or-equal) is
 * what stops a subarray whose minimum repeats from being counted under two elements at once.
 */
@Component
public class SumSubarrayMinimumsTracer implements AlgorithmTracer {

    private static final long MOD = 1_000_000_007L;

    @Override
    public String id() {
        return "sum-subarray-minimums";
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
                        .length(1, 10).values(0, 1000)
                        .defaultValue(java.util.List.of(71, 55, 82, 55))
                        .build());
    }

    /** No repeated values this time: every subarray's minimum is unique to one element. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", java.util.List.of(3, 1, 2, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public int sumSubarrayMins(int[] nums) {
                   int n = nums.length;
                   int[] left = new int[n], right = new int[n];
                   Deque<Integer> stack = new ArrayDeque<>();

                   for (int i = 0; i < n; i++) {
                       while (!stack.isEmpty() && nums[stack.peek()] > nums[i]) {
                           // @a popStrictlyGreater
                           stack.pop();
                       }
                       // @a pushLeftBoundary
                       left[i] = i - (stack.isEmpty() ? -1 : stack.peek());
                       stack.push(i);
                   }

                   stack.clear();
                   for (int i = n - 1; i >= 0; i--) {
                       while (!stack.isEmpty() && nums[stack.peek()] >= nums[i]) {
                           // @a popGreaterOrEqual
                           stack.pop();
                       }
                       // @a pushRightBoundary
                       right[i] = (stack.isEmpty() ? n : stack.peek()) - i;
                       stack.push(i);
                   }

                   long total = 0;
                   for (int i = 0; i < n; i++) {
                       // @a contribution
                       total = (total + (long) nums[i] * left[i] * right[i]) % 1_000_000_007L;
                   }
                   // @a done
                   return (int) total;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int[] left = new int[n];
        int[] right = new int[n];
        Deque<Integer> stack = new ArrayDeque<>();

        for (int i = 0; i < n; i++) {
            while (!stack.isEmpty() && nums[stack.peek()] > nums[i]) {
                int popped = stack.pop();
                emit.at("popStrictlyGreater")
                        .say("nums[%d]=%d is strictly greater than nums[%d]=%d - it can never "
                                + "be the previous-smaller boundary for anything past here.",
                                popped, nums[popped], i, nums[i])
                        .array(nums, i).stack(new java.util.ArrayList<>(stack)).step();
            }
            left[i] = stack.isEmpty() ? i + 1 : i - stack.peek();
            emit.at("pushLeftBoundary")
                    .say("nums[%d]=%d's previous strictly-smaller element is %s - left[%d]=%d.",
                            i, nums[i], stack.isEmpty() ? "none" : "at index " + stack.peek(), i, left[i])
                    .var("left", left[i])
                    .array(nums, i).step();
            stack.push(i);
        }

        stack.clear();
        for (int i = n - 1; i >= 0; i--) {
            while (!stack.isEmpty() && nums[stack.peek()] >= nums[i]) {
                int popped = stack.pop();
                emit.at("popGreaterOrEqual")
                        .say("nums[%d]=%d is greater than or equal to nums[%d]=%d - it can "
                                + "never be the next-smaller-or-equal boundary for anything "
                                + "before here.", popped, nums[popped], i, nums[i])
                        .array(nums, i).stack(new java.util.ArrayList<>(stack)).step();
            }
            right[i] = stack.isEmpty() ? n - i : stack.peek() - i;
            emit.at("pushRightBoundary")
                    .say("nums[%d]=%d's next smaller-or-equal element is %s - right[%d]=%d.",
                            i, nums[i], stack.isEmpty() ? "none" : "at index " + stack.peek(), i, right[i])
                    .var("right", right[i])
                    .array(nums, i).step();
            stack.push(i);
        }

        long total = 0;
        for (int i = 0; i < n; i++) {
            long contribution = (long) nums[i] * left[i] * right[i];
            total = (total + contribution) % MOD;
            emit.at("contribution")
                    .say("nums[%d]=%d is the minimum of %d*%d=%d subarrays, contributing %d*%d=%d. Running total: %d.",
                            i, nums[i], left[i], right[i], left[i] * right[i],
                            nums[i], left[i] * right[i], contribution, total)
                    .var("runningTotal", total)
                    .array(nums, i).step();
        }

        emit.at("done")
                .say("Every element's contribution summed (mod 1e9+7): %d.", total)
                .var("answer", total)
                .array(nums).step();
    }
}
