package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Longest subarray with sum K using prefix-sum + HashMap.
 *
 * <p>Handles both positive and negative numbers. The key insight: if
 * {@code prefixSum[i] - prefixSum[j] = K}, then the subarray {@code [j+1..i]}
 * sums to K. The HashMap stores the FIRST occurrence of each prefix sum,
 * because we want the longest subarray (earliest start).
 */
@Component
public class LongestSubarraySumKTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-subarray-sum-k";
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
                        .help("May contain positive and negative integers.")
                        .length(1, 30).values(-50, 50)
                        .defaultValue(List.of(-1, 2, 3, -2, 1))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Target sum K")
                        .range(-200, 200)
                        .defaultValue(3)
                        .build());
    }

    /**
     * A longer array where the longest subarray spans nearly the whole input
     * and the sum == K case fires at the very start.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "nums", List.of(1, -1, 5, -2, 3),
                "k", 6);
    }

    @Override
    public String annotatedCode() {
        return """
               public int getLongestSubarray(int[] a, long k) {
                   // @a init
                   Map<Long, Integer> preSumMap = new HashMap<>();
                   long sum = 0;
                   int maxLen = 0;
                   for (int i = 0; i < a.length; i++) {
                       // @a addElement
                       sum += a[i];
                       if (sum == k) {
                           // @a wholePrefix
                           maxLen = Math.max(maxLen, i + 1);
                       }
                       long rem = sum - k;
                       if (preSumMap.containsKey(rem)) {
                           // @a foundMatch
                           int len = i - preSumMap.get(rem);
                           maxLen = Math.max(maxLen, len);
                       }
                       if (!preSumMap.containsKey(sum)) {
                           // @a storePrefix
                           preSumMap.put(sum, i);
                       }
                   }
                   // @a done
                   return maxLen;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] a = in.getIntArray("nums");
        long k = in.getInt("k");
        Map<Long, Integer> preSumMap = new HashMap<>();
        long sum = 0;
        int maxLen = 0;

        emit.at("init")
                .say("Find longest subarray with sum %d in %s. HashMap stores first index of each prefix sum.",
                        k, java.util.Arrays.toString(a))
                .var("k", k).var("sum", 0).var("maxLen", 0)
                .array(a)
                .step();

        for (int i = 0; i < a.length; i++) {
            sum += a[i];
            emit.at("addElement")
                    .say("i=%d: add a[%d]=%d, prefix sum is now %d.", i, i, a[i], sum)
                    .var("i", i).var("sum", sum).var("maxLen", maxLen)
                    .array(a, i)
                    .step();

            if (sum == k) {
                maxLen = Math.max(maxLen, i + 1);
                emit.at("wholePrefix")
                        .say("Prefix sum %d == K! The entire subarray a[0..%d] sums to %d. maxLen = %d.",
                                sum, i, k, maxLen)
                        .var("maxLen", maxLen).var("sum", sum)
                        .array(a, i)
                        .step();
            }

            long rem = sum - k;
            if (preSumMap.containsKey(rem)) {
                int prevIdx = preSumMap.get(rem);
                int len = i - prevIdx;
                maxLen = Math.max(maxLen, len);
                emit.at("foundMatch")
                        .say("sum - K = %d found in map at index %d. Subarray a[%d..%d] has sum %d, length %d.%s",
                                rem, prevIdx, prevIdx + 1, i, k, len,
                                len >= maxLen ? " New best!" : " Not longer than current best.")
                        .var("maxLen", maxLen).var("rem", rem).var("prevIdx", prevIdx)
                        .array(a, i, prevIdx)
                        .step();
            }

            if (!preSumMap.containsKey(sum)) {
                preSumMap.put(sum, i);
                emit.at("storePrefix")
                        .say("First time seeing prefix sum %d — store index %d in map.", sum, i)
                        .var("sum", sum).var("map", preSumMap.toString())
                        .array(a, i)
                        .step();
            }
        }

        emit.at("done")
                .say("Longest subarray with sum %d has length %d.", k, maxLen)
                .var("maxLen", maxLen)
                .array(a)
                .step();
    }
}
