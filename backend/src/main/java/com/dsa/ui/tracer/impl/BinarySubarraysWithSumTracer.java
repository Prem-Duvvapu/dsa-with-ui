package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Binary Subarrays With Sum (LeetCode 930), traced on the owner's own accepted submission. Counting windows with EXACTLY k is awkward;
 * counting windows with AT MOST k is a plain sliding window - for each right end, every start from
 * left to right works, which is right - left + 1 windows. So exactly(k) = atMost(k) - atMost(k - 1),
 * two O(n) passes. Each pass is shown as its own call on the stack.
 */
@Component
public class BinarySubarraysWithSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "binary-subarrays-with-sum";
    }

    @Override
    public DsType dsType() {
        return DsType.WINDOW;
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
               class Solution {
                   public int numSubarraysWithSum(int[] nums, int goal) {
                       // @a init
                       int a=solve(nums,goal);
                       int b=solve(nums,goal-1);

                       // @a done
                       return (a-b);
                   }

                   public int solve(int[] nums,int k) {
                       if (k<0)
                           // @a negative
                           return 0;

                       int n=nums.length;
                       int left=0;
                       int right=0;
                       int currSum=0;
                       int cnt=0;

                       while (right<n) {
                           // @a add
                           currSum+=nums[right];

                           //greater than k
                           while (left<=right && currSum>k) {
                               // @a shrink
                               currSum-=nums[left];
                               left++;
                           }

                           //less than or equal to k
                           // @a count
                           int currCnt=(right-left)+1;
                           cnt+=currCnt;

                           right++;
                       }

                       // @a total
                       return cnt;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int goal = in.getInt("goal");
        emit.at("init").say("Subarrays summing to exactly %d = (subarrays summing to at most %d) - (at most %d). "
                        + "Count each with solve().", goal, goal, goal - 1)
                .var("goal", goal).arrayState(WindowCells.of(nums, -1, -1)).step();
        int a = solve(nums, goal, emit);
        int b = solve(nums, goal - 1, emit);
        emit.at("done").say("%d subarrays sum to at most %d and %d to at most %d, so %d - %d = %d sum to exactly %d.",
                        a, goal, b, goal - 1, a, b, a - b, goal)
                .var("a", a).var("b", b).var("answer", a - b).arrayState(WindowCells.of(nums, -1, -1)).step();
    }

    private static int solve(int[] nums, int k, StepEmitter emit) {
        emit.push("solve(nums, " + k + ")");
        if (k < 0) {
            emit.at("negative").say("solve(nums, %d): no subarray of 0s and 1s sums to at most a negative number. "
                            + "Return 0.", k)
                    .var("k", k).arrayState(WindowCells.of(nums, -1, -1)).step();
            emit.pop();
            return 0;
        }
        int n = nums.length;
        int left = 0;
        int right = 0;
        int currSum = 0;
        int cnt = 0;
        while (right < n) {
            currSum += nums[right];
            emit.at("add").say("solve(%d): add nums[%d] = %d. currSum = %d.", k, right, nums[right], currSum)
                    .var("k", k).var("left", left).var("right", right).var("currSum", currSum).var("cnt", cnt)
                    .arrayState(WindowCells.of(nums, left, right)).step();
            while (left <= right && currSum > k) {
                currSum -= nums[left];
                left++;
                emit.at("shrink").say("currSum was over %d: drop nums[%d]. currSum = %d.", k, left - 1, currSum)
                        .var("k", k).var("left", left).var("right", right).var("currSum", currSum).var("cnt", cnt)
                        .arrayState(WindowCells.of(nums, left, right)).step();
            }
            int currCnt = (right - left) + 1;
            cnt += currCnt;
            emit.at("count").say("Every subarray ending at %d and starting from %d to %d sums to at most %d: %d more. "
                            + "cnt = %d.", right, left, right, k, currCnt, cnt)
                    .var("k", k).var("left", left).var("right", right).var("currSum", currSum).var("cnt", cnt)
                    .arrayState(WindowCells.of(nums, left, right)).step();
            right++;
        }
        emit.at("total").say("solve(nums, %d) = %d.", k, cnt)
                .var("k", k).var("cnt", cnt).arrayState(WindowCells.of(nums, -1, -1)).step();
        emit.pop();
        return cnt;
    }
}
