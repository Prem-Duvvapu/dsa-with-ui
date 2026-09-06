package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * Sum of Subarray Ranges (LeetCode 2104). Range of a subarray is max - min.
 * Sum of ranges = sum of (max of every subarray) - sum of (min of every subarray).
 *
 * <p>Each part is computed with the same monotonic stack technique as Sum of Subarray
 * Minimums. For each element, find how many subarrays it is the minimum of
 * (using previous-smaller and next-smaller) and how many it is the maximum of
 * (using previous-greater and next-greater), then combine.
 */
@Component
public class SumSubarrayRangesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "sum-subarray-ranges";
    }

    @Override
    public DsType dsType() {
        return DsType.STACK;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Values")
                        .help("Sum of (max - min) over all contiguous subarrays.")
                        .length(1, 12).values(-100, 100)
                        .defaultValue(List.of(1, 2, 3))
                        .build());
    }

    /** All same values: every range is 0, sum is 0. Completely different from default. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(4, -2, 7, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public long subArrayRanges(int[] nums) {
                   int n = nums.length;
                   // @a init
                   long sumMax = 0, sumMin = 0;
                   // Compute sum of subarray minimums
                   Deque<Integer> stack = new ArrayDeque<>();
                   for (int i = 0; i <= n; i++) {
                       while (!stack.isEmpty() && (i == n || nums[stack.peek()] >= nums[i])) {
                           // @a popMin
                           int mid = stack.pop();
                           int left = stack.isEmpty() ? -1 : stack.peek();
                           sumMin += (long) nums[mid] * (mid - left) * (i - mid);
                       }
                       // @a pushMin
                       stack.push(i);
                   }
                   // Compute sum of subarray maximums
                   // @a resetMax
                   stack.clear();
                   for (int i = 0; i <= n; i++) {
                       while (!stack.isEmpty() && (i == n || nums[stack.peek()] <= nums[i])) {
                           // @a popMax
                           int mid = stack.pop();
                           int left = stack.isEmpty() ? -1 : stack.peek();
                           sumMax += (long) nums[mid] * (mid - left) * (i - mid);
                       }
                       // @a pushMax
                       stack.push(i);
                   }
                   // @a done
                   return sumMax - sumMin;
               }""";
    }

    private List<ArrayElement> state(int[] nums, int current, Deque<Integer> stack) {
        List<ArrayElement> s = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String st = i == current ? "current" : stack.contains(i) ? "sorted" : "default";
            s.add(new ArrayElement(i, nums[i], st));
        }
        return s;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        long sumMax = 0, sumMin = 0;
        Deque<Integer> stack = new ArrayDeque<>();

        emit.at("init")
                .say("Sum of subarray ranges for %s. Compute sum of mins and sum of maxes separately using monotonic stacks.",
                        Arrays.toString(nums))
                .var("n", n)
                .arrayState(state(nums, -1, stack)).stack(stack).step();

        // Sum of subarray minimums
        for (int i = 0; i <= n; i++) {
            while (!stack.isEmpty() && (i == n || nums[stack.peek()] >= nums[i])) {
                int mid = stack.pop();
                int left = stack.isEmpty() ? -1 : stack.peek();
                long contribution = (long) nums[mid] * (mid - left) * (i - mid);
                sumMin += contribution;
                emit.at("popMin")
                        .say("Min-phase: pop index %d (value %d). It is the minimum for %d subarrays, contributing %d. sumMin = %d.",
                                mid, nums[mid], (mid - left) * (i - mid), contribution, sumMin)
                        .var("mid", mid).var("contribution", contribution).var("sumMin", sumMin)
                        .arrayState(state(nums, i < n ? i : -1, stack)).stack(stack).step();
            }
            if (i < n) {
                stack.push(i);
                emit.at("pushMin")
                        .say("Min-phase: push index %d (value %d).", i, nums[i])
                        .var("i", i)
                        .arrayState(state(nums, i, stack)).stack(stack).step();
            }
        }

        // Sum of subarray maximums
        stack.clear();
        emit.at("resetMax")
                .say("Sum of minimums complete (sumMin = %d). Now compute sum of maximums with a monotonic decreasing stack.", sumMin)
                .var("sumMin", sumMin)
                .arrayState(state(nums, -1, stack)).stack(stack).step();

        for (int i = 0; i <= n; i++) {
            while (!stack.isEmpty() && (i == n || nums[stack.peek()] <= nums[i])) {
                int mid = stack.pop();
                int left = stack.isEmpty() ? -1 : stack.peek();
                long contribution = (long) nums[mid] * (mid - left) * (i - mid);
                sumMax += contribution;
                emit.at("popMax")
                        .say("Max-phase: pop index %d (value %d). It is the maximum for %d subarrays, contributing %d. sumMax = %d.",
                                mid, nums[mid], (mid - left) * (i - mid), contribution, sumMax)
                        .var("mid", mid).var("contribution", contribution).var("sumMax", sumMax)
                        .arrayState(state(nums, i < n ? i : -1, stack)).stack(stack).step();
            }
            if (i < n) {
                stack.push(i);
                emit.at("pushMax")
                        .say("Max-phase: push index %d (value %d).", i, nums[i])
                        .var("i", i)
                        .arrayState(state(nums, i, stack)).stack(stack).step();
            }
        }

        long answer = sumMax - sumMin;
        emit.at("done")
                .say("Sum of ranges = sumMax - sumMin = %d - %d = %d.", sumMax, sumMin, answer)
                .var("sumMax", sumMax).var("sumMin", sumMin).var("answer", answer)
                .arrayState(state(nums, -1, stack)).stack(stack).step();
    }
}
