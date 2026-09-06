package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binary Subarrays With Sum — count subarrays summing to exact goal using
 * sliding window: {@code numSubarraysWithSum(goal) = atMost(goal) - atMost(goal - 1)}.
 * For a non-negative array, atMost(K) is monotonically expandable and shrinkable in O(N).
 */
@Component
public class BinarySubarraysWithSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "binary-subarrays-with-sum";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Binary Array")
                        .help("Array of 0s and 1s.")
                        .length(1, 30).values(0, 1)
                        .defaultValue(List.of(1, 0, 1, 0, 1))
                        .build(),
                InputField.of("goal", FieldType.INT)
                        .label("Target Sum (Goal)")
                        .help("Exact sum of subarray elements.")
                        .range(0, 30)
                        .defaultValue(2)
                        .build());
    }

    /** Alternate input with all zeroes and goal=0. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(0, 0, 0, 0, 0), "goal", 0);
    }

    @Override
    public String annotatedCode() {
        return """
               public int numSubarraysWithSum(int[] nums, int goal) {
                   // @a combine
                   return atMost(nums, goal) - atMost(nums, goal - 1);
               }

               private int atMost(int[] nums, int goal) {
                   if (goal < 0) return 0;
                   int left = 0, sum = 0, total = 0;
                   for (int right = 0; right < nums.length; right++) {
                       // @a expand
                       sum += nums[right];
                       while (sum > goal && left <= right) {
                           // @a shrink
                           sum -= nums[left];
                           left++;
                       }
                       // @a countWindows
                       total += right - left + 1;
                   }
                   return total;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int goal = in.getInt("goal");

        int atMostGoal = atMost(nums, goal, emit, goal);
        int atMostGoalMinus1 = goal > 0 ? atMost(nums, goal - 1, emit, goal - 1) : 0;
        int exact = atMostGoal - atMostGoalMinus1;

        emit.at("combine")
                .say("Pass 1 (sum ≤ %d): %d subarrays. Pass 2 (sum ≤ %d): %d subarrays. "
                                + "Exact sum = %d: %d - %d = %d.",
                        goal, atMostGoal, goal - 1, atMostGoalMinus1, goal, atMostGoal, atMostGoalMinus1, exact)
                .var("atMostGoal", atMostGoal).var("atMostGoalMinus1", atMostGoalMinus1).var("answer", exact)
                .array(nums, -1)
                .step();
    }

    private int atMost(int[] nums, int limit, StepEmitter emit, int passLabel) {
        if (limit < 0) return 0;
        int left = 0, sum = 0, total = 0;

        for (int right = 0; right < nums.length; right++) {
            sum += nums[right];
            emit.at("expand")
                    .say("[Pass sum ≤ %d] Add nums[%d]=%d → window sum is %d.",
                            limit, right, nums[right], sum)
                    .var("pass", "atMost(" + passLabel + ")").var("left", left).var("right", right).var("sum", sum)
                    .arrayState(windowState(nums, left, right))
                    .step();

            while (sum > limit && left <= right) {
                sum -= nums[left];
                left++;
                emit.at("shrink")
                        .say("[Pass sum ≤ %d] Sum %d > %d → shrink from left past index %d. Window starts at %d, sum is %d.",
                                limit, sum + nums[left - 1], limit, left - 1, left, sum)
                        .var("pass", "atMost(" + passLabel + ")").var("left", left).var("right", right).var("sum", sum)
                        .arrayState(windowState(nums, left, right))
                        .step();
            }

            total += right - left + 1;
            emit.at("countWindows")
                    .say("[Pass sum ≤ %d] Window [%d,%d] adds %d subarrays ending at index %d. Running total: %d.",
                            limit, left, right, right - left + 1, right, total)
                    .var("pass", "atMost(" + passLabel + ")").var("added", right - left + 1).var("total", total)
                    .arrayState(windowState(nums, left, right))
                    .step();
        }

        return total;
    }

    private static List<ArrayElement> windowState(int[] nums, int left, int right) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String st = (i == right) ? "current"
                    : (i == left) ? "target"
                    : (i > left && i < right) ? "active"
                    : "default";
            state.add(new ArrayElement(i, nums[i], st));
        }
        return state;
    }
}
