package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Count Number of Nice Subarrays — subarrays with exactly K odd numbers.
 * An array element is either odd (1) or even (0). Counting subarrays with K odds
 * reduces to {@code atMost(k) - atMost(k - 1)}, solved in O(N) via two-pointer sliding window.
 */
@Component
public class CountNiceSubarraysTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-nice-subarrays";
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
               public int numberOfSubarrays(int[] nums, int k) {
                   // @a combine
                   return atMost(nums, k) - atMost(nums, k - 1);
               }

               private int atMost(int[] nums, int k) {
                   if (k < 0) return 0;
                   int left = 0, odds = 0, total = 0;
                   for (int right = 0; right < nums.length; right++) {
                       // @a expand
                       if (nums[right] % 2 != 0) odds++;
                       while (odds > k && left <= right) {
                           // @a shrink
                           if (nums[left] % 2 != 0) odds--;
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
        int exact = atMostK - atMostKMinus1;

        emit.at("combine")
                .say("Pass 1 (odds ≤ %d): %d subarrays. Pass 2 (odds ≤ %d): %d subarrays. "
                                + "Exactly %d odds: %d - %d = %d nice subarrays.",
                        k, atMostK, k - 1, atMostKMinus1, k, atMostK, atMostKMinus1, exact)
                .var("atMostK", atMostK).var("atMostKMinus1", atMostKMinus1).var("answer", exact)
                .array(nums, -1)
                .step();
    }

    private int atMost(int[] nums, int limit, StepEmitter emit, int passLabel) {
        if (limit < 0) return 0;
        int left = 0, odds = 0, total = 0;

        for (int right = 0; right < nums.length; right++) {
            boolean isOdd = nums[right] % 2 != 0;
            if (isOdd) odds++;
            emit.at("expand")
                    .say("[Pass odds ≤ %d] Add nums[%d]=%d (%s) → odds in window: %d.",
                            limit, right, nums[right], isOdd ? "ODD" : "EVEN", odds)
                    .var("pass", "atMost(" + passLabel + ")").var("left", left).var("right", right).var("odds", odds)
                    .arrayState(windowState(nums, left, right))
                    .step();

            while (odds > limit && left <= right) {
                boolean leftWasOdd = nums[left] % 2 != 0;
                if (leftWasOdd) odds--;
                left++;
                emit.at("shrink")
                        .say("[Pass odds ≤ %d] Odds (%d) > %d → shrink left past index %d (%s). Left is now %d, odds in window: %d.",
                                limit, odds + (leftWasOdd ? 1 : 0), limit, left - 1, leftWasOdd ? "ODD" : "EVEN", left, odds)
                        .var("pass", "atMost(" + passLabel + ")").var("left", left).var("right", right).var("odds", odds)
                        .arrayState(windowState(nums, left, right))
                        .step();
            }

            total += right - left + 1;
            emit.at("countWindows")
                    .say("[Pass odds ≤ %d] Window [%d,%d] adds %d valid subarrays ending at index %d. Running total: %d.",
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
            state.add(new ArrayElement(i, nums[i], st, nums[i] % 2 != 0 ? "odd" : "even"));
        }
        return state;
    }
}
