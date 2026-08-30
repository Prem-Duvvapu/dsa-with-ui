package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Find the length of the longest subarray with sum equal to K in an array of positive integers
 * using the optimal 2-pointer sliding window approach.
 */
@Component
public class LongestSubarraySumKPositivesTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "longest-subarray-sum-k-positives";
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
                        .help("Array of positive integers.")
                        .length(1, 40).values(1, 999)
                        .defaultValue(List.of(1, 2, 3, 1, 1, 1, 1, 4, 2, 3))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Target sum K")
                        .help("Target subarray sum.")
                        .range(1, 100000)
                        .defaultValue(5)
                        .build());
    }

    /** Target k=3 on [4, 1, 1, 1, 2, 3, 5] triggering multiple multi-step shrinks, maxLen=3 ([1, 1, 1]). */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(4, 1, 1, 1, 2, 3, 5), "k", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public int getLongestSubarray(int[] a, long k) {
                   // @a init
                   int left = 0, right = 0;
                   long sum = a[0];
                   int maxLen = 0;
                   int n = a.length;
                   while (right < n) {
                       // @a shrink
                       while (left <= right && sum > k) {
                           sum -= a[left];
                           left++;
                       }
                       // @a match
                       if (sum == k) {
                           maxLen = Math.max(maxLen, right - left + 1);
                       }
                       // @a expand
                       right++;
                       if (right < n) sum += a[right];
                   }
                   // @a done
                   return maxLen;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] a = in.getIntArray("nums");
        long k = in.getInt("k");
        int n = a.length;

        int left = 0;
        int right = 0;
        long sum = a[0];
        int maxLen = 0;

        emit.at("init")
                .say("Initialize 2-pointer window: left=0, right=0, sum=%d, target K=%d.", sum, k)
                .var("left", left).var("right", right).var("sum", sum).var("maxLen", maxLen).var("k", k)
                .array(a, left, right).step();

        while (right < n) {
            while (left <= right && sum > k) {
                long dropped = a[left];
                sum -= dropped;
                left++;
                emit.at("shrink")
                        .say("Window sum (%d + %d = %d) > K (%d): shrink window from left (drop a[%d]=%d, left=%d, sum=%d).",
                                sum, dropped, sum + dropped, k, left - 1, dropped, left, sum)
                        .var("left", left).var("right", right).var("sum", sum).var("maxLen", maxLen).var("k", k)
                        .array(a, Math.min(left, right), right).step();
            }

            if (sum == k) {
                maxLen = Math.max(maxLen, right - left + 1);
                emit.at("match")
                        .say("Window sum == K (%d)! Subarray a[%d..%d] has length %d; maxLen = %d.",
                                k, left, right, right - left + 1, maxLen)
                        .var("left", left).var("right", right).var("sum", sum).var("maxLen", maxLen).var("k", k)
                        .array(a, left, right).step();
            }

            right++;
            if (right < n) {
                sum += a[right];
                emit.at("expand")
                        .say("Expand window right to index %d (add a[%d]=%d, sum=%d).", right, right, a[right], sum)
                        .var("left", left).var("right", right).var("sum", sum).var("maxLen", maxLen).var("k", k)
                        .array(a, Math.min(left, right), right).step();
            }
        }

        emit.at("done")
                .say("Sliding window search complete. Longest subarray length with sum %d is %d.", k, maxLen)
                .var("result", maxLen).var("maxLen", maxLen).array(a).step();
    }
}
