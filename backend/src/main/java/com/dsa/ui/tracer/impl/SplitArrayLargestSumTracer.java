package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Reuses {@link KokoEatingBananasTracer}'s exact mechanism - minimize a candidate answer by
 * binary search, using a greedy pass over the array as the feasibility check - but the
 * feasibility check itself is harder: instead of a per-element division, it greedily grows
 * one subarray until adding the next element would exceed the candidate sum, then starts a
 * new one, and feasibility is "did that greedy split use at most m subarrays".
 */
@Component
public class SplitArrayLargestSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "split-array-largest-sum";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .length(1, 10).values(0, 1000)
                        .defaultValue(java.util.List.of(7, 2, 5, 10, 8))
                        .build(),
                InputField.of("m", FieldType.INT)
                        .label("Number of subarrays")
                        .range(1, 10)
                        .defaultValue(2)
                        .build());
    }

    /** Smaller, evenly-sized values with more subarrays allowed: a much smaller answer. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", java.util.List.of(1, 2, 3, 4, 5), "m", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public int splitArray(int[] nums, int m) {
                   int max = 0, sum = 0;
                   for (int x : nums) { max = Math.max(max, x); sum += x; }
                   // @a init
                   int low = max, high = sum, ans = high;

                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       int splits = 1, current = 0;
                       for (int x : nums) {
                           if (current + x > mid) {
                               // @a split
                               splits++;
                               current = x;
                           } else {
                               // @a extend
                               current += x;
                           }
                       }
                       if (splits <= m) {
                           // @a feasible
                           ans = mid;
                           high = mid - 1;
                       } else {
                           // @a infeasible
                           low = mid + 1;
                       }
                   }
                   // @a done
                   return ans;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int m = in.getInt("m");
        int max = 0, sum = 0;
        for (int x : nums) {
            max = Math.max(max, x);
            sum += x;
        }
        int low = max, high = sum, ans = high;

        emit.at("init")
                .say("Binary search the answer itself: the smallest possible largest-subarray-sum. "
                        + "Range starts at [%d, %d] - %d is one subarray per element, %d is "
                        + "everything in a single subarray.", low, high, low, high)
                .var("low", low).var("high", high).var("ans", ans)
                .array(nums).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Test whether nums can be greedily split into at most %d subarrays "
                            + "each summing to at most %d.", m, mid)
                    .var("low", low).var("high", high).var("mid", mid)
                    .array(nums).step();

            int splits = 1, current = 0;
            for (int i = 0; i < nums.length; i++) {
                if (current + nums[i] > mid) {
                    splits++;
                    current = nums[i];
                    emit.at("split")
                            .say("Adding nums[%d]=%d would exceed %d - start subarray #%d here.",
                                    i, nums[i], mid, splits)
                            .var("splits", splits).var("current", current)
                            .array(nums, i).step();
                } else {
                    current += nums[i];
                    emit.at("extend")
                            .say("nums[%d]=%d fits in the current subarray - running sum now %d.",
                                    i, nums[i], current)
                            .var("current", current)
                            .array(nums, i).step();
                }
            }

            if (splits <= m) {
                ans = mid;
                emit.at("feasible")
                        .say("%d subarrays needed (allowed %d) - %d works. Record it and try smaller.",
                                splits, m, mid)
                        .var("ans", ans).var("high", mid - 1)
                        .array(nums).step();
                high = mid - 1;
            } else {
                emit.at("infeasible")
                        .say("%d subarrays needed (allowed %d) - %d is too small. Try larger.",
                                splits, m, mid)
                        .var("low", mid + 1)
                        .array(nums).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("low passed high. The smallest achievable largest-subarray-sum is %d.", ans)
                .var("answer", ans)
                .array(nums).step();
    }
}
