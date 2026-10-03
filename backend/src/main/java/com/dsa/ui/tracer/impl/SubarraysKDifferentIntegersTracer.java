package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Subarrays with K Different Integers (LeetCode 992), traced on the owner's own accepted submission. Counting windows with EXACTLY k is awkward;
 * counting windows with AT MOST k is a plain sliding window - for each right end, every start from
 * left to right works, which is right - left + 1 windows. So exactly(k) = atMost(k) - atMost(k - 1),
 * two O(n) passes. The window keeps a HashMap of counts; it is too wide once the map holds more than k keys.
 */
@Component
public class SubarraysKDifferentIntegersTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "subarrays-k-different-integers";
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
                        .length(1, 20).values(1, 20)
                        .defaultValue(List.of(1, 2, 1, 2, 3))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K")
                        .help("Exact number of distinct integers required.")
                        .range(1, 20)
                        .defaultValue(2)
                        .build());
    }

    /** A different array and K - a different count, not the same answer under new labels. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 1, 3, 4), "k", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               class Solution {
                   public int subarraysWithKDistinct(int[] nums, int k) {
                       // @a init
                       int res=solve(nums,k)-solve(nums,k-1);
                       // @a done
                       return res;
                   }

                   private int solve(int[] nums,int k) {
                       int left=0;
                       int right=0;
                       int cnt=0;
                       Map<Integer,Integer> map=new HashMap<>();

                       while (right<nums.length) {
                           // @a add
                           map.put(nums[right],map.getOrDefault(nums[right],0)+1);

                           while (map.size()>k) {
                               // @a shrink
                               int val=map.get(nums[left]);
                               val--;
                               if (val==0)
                                   map.remove(nums[left]);
                               else
                                   map.put(nums[left],val);
                               left++;
                           }

                           // @a count
                           cnt+=(right-left+1);
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
        int k = in.getInt("k");
        emit.at("init").say("Subarrays with exactly %d distinct values = atMost(%d) - atMost(%d).", k, k, k - 1)
                .var("k", k).arrayState(WindowCells.of(nums, -1, -1)).step();
        int a = solve(nums, k, emit);
        int b = solve(nums, k - 1, emit);
        int res = a - b;
        emit.at("done").say("%d - %d = %d subarrays have exactly %d distinct values.", a, b, res, k)
                .var("res", res).var("answer", res).arrayState(WindowCells.of(nums, -1, -1)).step();
    }

    private static int solve(int[] nums, int k, StepEmitter emit) {
        emit.push("solve(nums, " + k + ")");
        int left = 0;
        int right = 0;
        int cnt = 0;
        Map<Integer, Integer> map = new HashMap<>();
        while (right < nums.length) {
            map.put(nums[right], map.getOrDefault(nums[right], 0) + 1);
            emit.at("add").say("solve(%d): add nums[%d] = %d. The window holds %d distinct value%s.",
                            k, right, nums[right], map.size(), Narration.s(map.size()))
                    .var("k", k).var("left", left).var("right", right).var("map", map.toString()).var("cnt", cnt)
                    .arrayState(WindowCells.of(nums, left, right)).step();
            while (map.size() > k) {
                int val = map.get(nums[left]) - 1;
                if (val == 0) map.remove(nums[left]);
                else map.put(nums[left], val);
                left++;
                emit.at("shrink").say("More than %d distinct: drop nums[%d]. %d distinct left.", k, left - 1, map.size())
                        .var("k", k).var("left", left).var("right", right).var("map", map.toString()).var("cnt", cnt)
                        .arrayState(WindowCells.of(nums, left, right)).step();
            }
            cnt += right - left + 1;
            emit.at("count").say("%d subarray%s %s at %d with at most %d distinct. cnt = %d.",
                            right - left + 1, Narration.s(right - left + 1),
                            Narration.plural(right - left + 1, "ends", "end"), right, k, cnt)
                    .var("k", k).var("left", left).var("right", right).var("map", map.toString()).var("cnt", cnt)
                    .arrayState(WindowCells.of(nums, left, right)).step();
            right++;
        }
        emit.at("total").say("solve(nums, %d) = %d.", k, cnt)
                .var("k", k).var("cnt", cnt).arrayState(WindowCells.of(nums, -1, -1)).step();
        emit.pop();
        return cnt;
    }
}
