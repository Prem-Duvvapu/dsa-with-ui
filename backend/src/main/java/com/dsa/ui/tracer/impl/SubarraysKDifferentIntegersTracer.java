package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * "Exactly K distinct" has no direct sliding window of its own - shrinking to restore
 * validity could mean either too many or too few distinct values, and a window can't tell
 * which without external bookkeeping. "At most K" has no such ambiguity (only "too many"
 * ever needs fixing), so exactly(K) = atMost(K) - atMost(K-1): every window counted by
 * atMost(K-1) is also counted by atMost(K), and the difference is exactly the windows with
 * precisely K distinct values.
 */
@Component
public class SubarraysKDifferentIntegersTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "subarrays-k-different-integers";
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
               public int subarraysWithKDistinct(int[] nums, int k) {
                   // @a combine
                   return atMost(nums, k) - atMost(nums, k - 1);
               }

               private int atMost(int[] nums, int k) {
                   Map<Integer, Integer> count = new HashMap<>();
                   int left = 0, total = 0;

                   for (int right = 0; right < nums.length; right++) {
                       // @a expand
                       count.merge(nums[right], 1, Integer::sum);

                       while (count.size() > k) {
                           // @a shrinkToAtMost
                           int leftVal = nums[left];
                           int remaining = count.merge(leftVal, -1, Integer::sum);
                           if (remaining == 0) count.remove(leftVal);
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
        int k = in.getInt("k");

        int atMostK = atMost(nums, k, emit, k);
        int atMostKMinus1 = k > 0 ? atMost(nums, k - 1, emit, k - 1) : 0;
        int exactly = atMostK - atMostKMinus1;

        emit.at("combine")
                .say("Subarrays with at most %d distinct: %d. With at most %d distinct: %d. "
                                + "Exactly %d distinct: %d - %d = %d.",
                        k, atMostK, k - 1, atMostKMinus1, k, atMostK, atMostKMinus1, exactly)
                .var("atMostK", atMostK).var("atMostKMinus1", atMostKMinus1).var("answer", exactly)
                .array(nums, -1).step();
    }

    private int atMost(int[] nums, int limit, StepEmitter emit, int passLabel) {
        Map<Integer, Integer> count = new LinkedHashMap<>();
        int left = 0;
        int total = 0;

        if (limit < 0) {
            return 0;
        }

        for (int right = 0; right < nums.length; right++) {
            count.merge(nums[right], 1, Integer::sum);
            emit.at("expand")
                    .say("[at most %d distinct] Add nums[%d]=%d. Window now holds %d distinct value%s.",
                            limit, right, nums[right], count.size(), count.size() == 1 ? "" : "s")
                    .var("pass", "atMost(" + passLabel + ")").var("distinct", count.size())
                    .arrayState(windowState(nums, left, right)).step();

            while (count.size() > limit) {
                int leftVal = nums[left];
                int remaining = count.merge(leftVal, -1, Integer::sum);
                if (remaining == 0) count.remove(leftVal);
                left++;
                emit.at("shrinkToAtMost")
                        .say("[at most %d distinct] Too many distinct values - shrink past nums[%d]=%d. "
                                        + "Window now starts at %d.",
                                limit, left - 1, leftVal, left)
                        .var("pass", "atMost(" + passLabel + ")").var("left", left)
                        .arrayState(windowState(nums, left, right)).step();
            }

            total += right - left + 1;
            emit.at("countWindows")
                    .say("[at most %d distinct] %d new subarrays end at index %d (window [%d,%d]). "
                                    + "Running total: %d.",
                            limit, right - left + 1, right, left, right, total)
                    .var("pass", "atMost(" + passLabel + ")").var("runningTotal", total)
                    .arrayState(windowState(nums, left, right)).step();
        }

        return total;
    }

    private static List<ArrayElement> windowState(int[] nums, int left, int right) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s = i == right ? "current" : i == left ? "target" : (i > left && i < right) ? "active" : "default";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }
}
