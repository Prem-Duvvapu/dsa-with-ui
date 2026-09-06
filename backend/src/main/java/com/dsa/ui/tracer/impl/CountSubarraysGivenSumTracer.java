package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Count subarrays with sum equal to K using prefix-sum frequency counting.
 *
 * <p>If {@code prefixSum[i] - prefixSum[j] = K}, then subarray {@code [j+1..i]}
 * sums to K. The HashMap stores how many times each prefix sum has been seen,
 * so looking up {@code prefixSum - K} instantly tells us how many earlier
 * positions form a valid subarray ending at the current index.
 */
@Component
public class CountSubarraysGivenSumTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-subarrays-given-sum";
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
                        .help("May contain positive, negative, and zero values.")
                        .length(1, 30).values(-50, 50)
                        .defaultValue(List.of(1, 1, 1))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Target sum K")
                        .range(-200, 200)
                        .defaultValue(2)
                        .build());
    }

    /**
     * Longer array with negatives and more matches to exercise the
     * frequency-counting path (same prefix sum seen multiple times).
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "nums", List.of(1, 2, 3, -3, 1, 1, 1, 4, 2, -3),
                "k", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public int subarraySum(int[] nums, int k) {
                   // @a init
                   Map<Integer, Integer> map = new HashMap<>();
                   map.put(0, 1);
                   int preSum = 0, cnt = 0;
               
                   for (int i = 0; i < nums.length; i++) {
                       // @a addElement
                       preSum += nums[i];
                       int remove = preSum - k;
                       if (map.containsKey(remove)) {
                           // @a countMatch
                           cnt += map.getOrDefault(remove, 0);
                       }
                       // @a updateMap
                       map.put(preSum, map.getOrDefault(preSum, 0) + 1);
                   }
                   // @a done
                   return cnt;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        Map<Integer, Integer> map = new HashMap<>();
        map.put(0, 1);
        int preSum = 0, cnt = 0;

        emit.at("init")
                .say("Count subarrays with sum %d in %s. Map starts with {0: 1} (empty prefix sums to 0).",
                        k, java.util.Arrays.toString(nums))
                .var("k", k).var("preSum", 0).var("cnt", 0)
                .array(nums)
                .step();

        for (int i = 0; i < nums.length; i++) {
            preSum += nums[i];
            int remove = preSum - k;

            emit.at("addElement")
                    .say("i=%d: add %d → preSum = %d. Looking for (preSum - K) = %d in map.",
                            i, nums[i], preSum, remove)
                    .var("i", i).var("preSum", preSum).var("remove", remove)
                    .array(nums, i)
                    .step();

            if (map.containsKey(remove)) {
                int freq = map.get(remove);
                cnt += freq;
                emit.at("countMatch")
                        .say("Map contains %d with frequency %d → %d subarray(s) ending at index %d sum to %d. Total count = %d.",
                                remove, freq, freq, i, k, cnt)
                        .var("cnt", cnt).var("freq", freq)
                        .array(nums, i)
                        .step();
            }

            map.put(preSum, map.getOrDefault(preSum, 0) + 1);
            emit.at("updateMap")
                    .say("Record preSum %d in map (now seen %d time(s)).", preSum, map.get(preSum))
                    .var("map", map.toString()).var("preSum", preSum)
                    .array(nums, i)
                    .step();
        }

        emit.at("done")
                .say("Total subarrays with sum %d: %d.", k, cnt)
                .var("cnt", cnt)
                .array(nums)
                .step();
    }
}
