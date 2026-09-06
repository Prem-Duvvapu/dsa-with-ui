package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Max Consecutive Ones III — sliding window with at most K zero flips.
 * Expand the right pointer. When the count of zeroes in the window exceeds K,
 * shrink the left pointer until zero count is at most K again.
 */
@Component
public class MaxConsecutiveOnes3Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "max-consecutive-ones-3";
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
                        .defaultValue(List.of(1, 1, 1, 0, 0, 0, 1, 1, 1, 1, 0))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("K (max zeroes to flip)")
                        .help("Maximum number of consecutive zeroes you can flip to 1.")
                        .range(0, 30)
                        .defaultValue(2)
                        .build());
    }

    /** Alternate input with different distribution and k=3. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "nums", List.of(0, 0, 1, 1, 0, 0, 1, 1, 1, 0, 1, 1, 0, 0, 0, 1, 1, 1, 1),
                "k", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public int longestOnes(int[] nums, int k) {
                   int left = 0, zeroes = 0, maxLen = 0;
                   for (int right = 0; right < nums.length; right++) {
                       // @a expand
                       if (nums[right] == 0) zeroes++;
                       while (zeroes > k) {
                           // @a shrink
                           if (nums[left] == 0) zeroes--;
                           left++;
                       }
                       // @a windowComplete
                       maxLen = Math.max(maxLen, right - left + 1);
                   }
                   // @a done
                   return maxLen;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        int left = 0, zeroes = 0, maxLen = 0;

        for (int right = 0; right < nums.length; right++) {
            if (nums[right] == 0) zeroes++;
            emit.at("expand")
                    .say("Expand right to %d (value %d). Zeroes in window: %d (budget K=%d).",
                            right, nums[right], zeroes, k)
                    .var("left", left).var("right", right).var("zeroes", zeroes).var("k", k)
                    .arrayState(windowState(nums, left, right))
                    .step();

            while (zeroes > k) {
                boolean droppedZero = (nums[left] == 0);
                if (droppedZero) zeroes--;
                left++;
                emit.at("shrink")
                        .say("Zeroes (%d) exceeded budget %d → shrink left past %d (value %d). Left is now %d.",
                                zeroes + (droppedZero ? 1 : 0), k, left - 1, droppedZero ? 0 : 1, left)
                        .var("left", left).var("right", right).var("zeroes", zeroes)
                        .arrayState(windowState(nums, left, right))
                        .step();
            }

            maxLen = Math.max(maxLen, right - left + 1);
            emit.at("windowComplete")
                    .say("Window [%d,%d] is valid with %d zeroes. Current length: %d (max: %d).",
                            left, right, zeroes, right - left + 1, maxLen)
                    .var("left", left).var("right", right).var("windowLen", right - left + 1).var("maxLen", maxLen)
                    .arrayState(windowState(nums, left, right))
                    .step();
        }

        emit.at("done")
                .say("Scan complete. Longest contiguous subarray of 1s after flipping at most %d zeroes: %d.", k, maxLen)
                .var("answer", maxLen)
                .arrayState(windowState(nums, -1, -1))
                .step();
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
