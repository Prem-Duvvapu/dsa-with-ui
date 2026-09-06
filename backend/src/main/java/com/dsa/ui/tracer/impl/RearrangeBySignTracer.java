package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Rearrange array elements by sign: positives at even indices, negatives
 * at odd indices, preserving relative order within each group.
 *
 * <p>The array must contain equal numbers of positive and negative integers.
 * Two pointers {@code posIndex} and {@code negIndex} walk the output array
 * at stride 2, placing each element where it belongs in a single pass.
 */
@Component
public class RearrangeBySignTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "rearrange-by-sign";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array (equal +/-)")
                        .help("Must contain equal numbers of positive and negative integers.")
                        .length(2, 30).values(-100, 100)
                        .defaultValue(List.of(3, 1, -2, -5, 2, -4))
                        .build());
    }

    /**
     * Alternating signs already, but NOT in the required pattern
     * (starts negative). Different length forces different step count.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(-1, 2, -3, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] rearrangeArray(int[] nums) {
                   int n = nums.length;
                   // @a init
                   int[] ans = new int[n];
                   int posIndex = 0, negIndex = 1;
               
                   for (int i = 0; i < n; i++) {
                       if (nums[i] > 0) {
                           // @a placePositive
                           ans[posIndex] = nums[i];
                           posIndex += 2;
                       } else {
                           // @a placeNegative
                           ans[negIndex] = nums[i];
                           negIndex += 2;
                       }
                   }
                   // @a done
                   return ans;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int[] ans = new int[n];
        int posIndex = 0, negIndex = 1;

        emit.at("init")
                .say("Input: %s. Build output array of size %d. posIndex starts at 0, negIndex at 1, both jump by 2.",
                        java.util.Arrays.toString(nums), n)
                .var("posIndex", 0).var("negIndex", 1)
                .array(nums)
                .step();

        for (int i = 0; i < n; i++) {
            if (nums[i] > 0) {
                int target = posIndex < n ? posIndex : Math.min(negIndex, n - 1);
                ans[target] = nums[i];
                List<ArrayElement> state = buildOutputState(ans, n, target);
                emit.at("placePositive")
                        .say("nums[%d]=%d is positive → place at ans[%d]. posIndex advances to %d.",
                                i, nums[i], target, posIndex + 2)
                        .var("i", i).var("posIndex", posIndex).var("negIndex", negIndex)
                        .arrayState(state)
                        .step();
                if (posIndex < n) posIndex += 2;
                else if (negIndex < n) negIndex += 2;
            } else {
                int target = negIndex < n ? negIndex : Math.min(posIndex, n - 1);
                ans[target] = nums[i];
                List<ArrayElement> state = buildOutputState(ans, n, target);
                emit.at("placeNegative")
                        .say("nums[%d]=%d is negative → place at ans[%d]. negIndex advances to %d.",
                                i, nums[i], target, negIndex + 2)
                        .var("i", i).var("posIndex", posIndex).var("negIndex", negIndex)
                        .arrayState(state)
                        .step();
                if (negIndex < n) negIndex += 2;
                else if (posIndex < n) posIndex += 2;
            }
        }

        List<ArrayElement> finalState = new ArrayList<>(n);
        for (int idx = 0; idx < n; idx++) {
            finalState.add(new ArrayElement(idx, ans[idx], "sorted"));
        }
        emit.at("done")
                .say("Rearranged: %s. Positives at even indices, negatives at odd indices.",
                        java.util.Arrays.toString(ans))
                .var("result", java.util.Arrays.toString(ans))
                .arrayState(finalState)
                .step();
    }

    private List<ArrayElement> buildOutputState(int[] ans, int n, int justPlaced) {
        List<ArrayElement> state = new ArrayList<>(n);
        for (int idx = 0; idx < n; idx++) {
            String s;
            if (idx == justPlaced) s = "current";
            else if (ans[idx] != 0) s = "visited";
            else s = "default";
            state.add(new ArrayElement(idx, ans[idx], s));
        }
        return state;
    }
}
