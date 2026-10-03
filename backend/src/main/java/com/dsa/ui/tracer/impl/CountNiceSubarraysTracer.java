package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Count Number of Nice Subarrays (LeetCode 1248), traced on the owner's own accepted submission. Counting windows with EXACTLY k is awkward;
 * counting windows with AT MOST k is a plain sliding window - for each right end, every start from
 * left to right works, which is right - left + 1 windows. So exactly(k) = atMost(k) - atMost(k - 1),
 * two O(n) passes. Replacing every number by num % 2 turns "k odd numbers" into "sum k", so this is Binary Subarrays With Sum again.
 */
@Component
public class CountNiceSubarraysTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-nice-subarrays";
    }

    @Override
    public DsType dsType() {
        return DsType.WINDOW;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Array of positive integers.")
                        .length(1, 30).values(1, 100)
                        .defaultValue(List.of(1, 1, 2, 1, 1))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K (exact odd count)")
                        .help("Exact number of odd integers required in each subarray.")
                        .range(1, 30)
                        .defaultValue(3)
                        .build());
    }

    /** Alternate input with separated odds and evens, k=2. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(2, 2, 2, 1, 2, 2, 1, 2, 2, 2), "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int numberOfSubarrays(int[] nums, int k) {
                       // @a init
                       int n=nums.length;
                       int[] arr=new int[n];

                       for (int i=0;i<n;i++)
                           arr[i]=nums[i]%2;

                       int res=solve(arr,k)-solve(arr,k-1);
                       // @a done
                       return res;
                   }

                   private int solve(int[] nums,int k) {
                       int left=0;
                       int right=0;
                       int resCnt=0;
                       int sum=0;

                       while (right < nums.length) {
                           // @a add
                           sum+=nums[right];

                           while (sum > k) {
                               // @a shrink
                               sum-=nums[left];
                               left++;
                           }

                           // @a count
                           resCnt+=(right-left+1);
                           right++;
                       }

                       // @a total
                       return resCnt;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        int n = nums.length;
        int[] arr = new int[n];
        for (int i = 0; i < n; i++) arr[i] = nums[i] % 2;
        emit.at("init").say("Odd numbers become 1 and even ones 0: arr = %s. Now a subarray with k = %d odd numbers is "
                        + "a subarray of arr summing to %d, and exactly(%d) = atMost(%d) - atMost(%d).",
                        Arrays.toString(arr), k, k, k, k, k - 1)
                .var("k", k).arrayState(WindowCells.of(arr, -1, -1)).step();
        int a = solve(arr, k, emit);
        int b = solve(arr, k - 1, emit);
        int res = a - b;
        emit.at("done").say("%d - %d = %d nice subarrays.", a, b, res)
                .var("res", res).var("answer", res).arrayState(WindowCells.of(arr, -1, -1)).step();
    }

    private static int solve(int[] nums, int k, StepEmitter emit) {
        emit.push("solve(arr, " + k + ")");
        int left = 0;
        int right = 0;
        int resCnt = 0;
        int sum = 0;
        while (right < nums.length) {
            sum += nums[right];
            emit.at("add").say("solve(%d): add arr[%d] = %d. %d odd number%s in the window.",
                            k, right, nums[right], sum, Narration.s(sum))
                    .var("k", k).var("left", left).var("right", right).var("sum", sum).var("resCnt", resCnt)
                    .arrayState(WindowCells.of(nums, left, right)).step();
            while (sum > k) {
                sum -= nums[left];
                left++;
                emit.at("shrink").say("More than %d odd: drop arr[%d]. sum = %d.", k, left - 1, sum)
                        .var("k", k).var("left", left).var("right", right).var("sum", sum).var("resCnt", resCnt)
                        .arrayState(WindowCells.of(nums, left, right)).step();
            }
            resCnt += right - left + 1;
            emit.at("count").say("%d subarray%s %s at %d with at most %d odd. resCnt = %d.",
                            right - left + 1, Narration.s(right - left + 1),
                            Narration.plural(right - left + 1, "ends", "end"), right, k, resCnt)
                    .var("k", k).var("left", left).var("right", right).var("sum", sum).var("resCnt", resCnt)
                    .arrayState(WindowCells.of(nums, left, right)).step();
            right++;
        }
        emit.at("total").say("solve(arr, %d) = %d.", k, resCnt)
                .var("k", k).var("resCnt", resCnt).arrayState(WindowCells.of(nums, -1, -1)).step();
        emit.pop();
        return resCnt;
    }
}
