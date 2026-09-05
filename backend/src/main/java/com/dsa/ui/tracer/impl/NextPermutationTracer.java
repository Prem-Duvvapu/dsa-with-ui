package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Next lexicographic permutation in three linear passes: find the rightmost ascent (the
 * "breakpoint"), swap it with the smallest suffix value that still beats it, then reverse
 * the suffix back to ascending order - or, if no ascent exists, the array is already the
 * highest permutation and wraps around to the lowest by reversing outright.
 */
@Component
public class NextPermutationTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "next-permutation";
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
                        .help("The permutation that comes right after this one in sorted order of all permutations.")
                        .length(1, 24).values(-999, 999)
                        .defaultValue(List.of(3, 2, 1))
                        .build());
    }

    /**
     * Has a real breakpoint (unlike the fully-descending default), so this exercises the
     * swap-and-reverse path instead of wrapping around.
     */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(2, 3, 1));
    }

    @Override
    public String annotatedCode() {
        return """
               public void nextPermutation(int[] nums) {
                   int n = nums.length, ind = -1;
                   for (int i = n - 2; i >= 0; i--) {
                       if (nums[i] < nums[i + 1]) {
                           // @a foundBreak
                           ind = i;
                           break;
                       } else {
                           // @a checkBreak
                           continue;
                       }
                   }
                   if (ind == -1) {
                       // @a noBreak
                       reverse(nums, 0, n - 1);
                       return;
                   }
                   for (int i = n - 1; i > ind; i--) {
                       if (nums[i] > nums[ind]) {
                           // @a foundSwap
                           swap(nums, i, ind);
                           break;
                       } else {
                           // @a checkSwap
                           continue;
                       }
                   }
                   // @a reverseSuffix
                   reverse(nums, ind + 1, n - 1);
                   // @a done
                   return;
               }""";
    }

    private List<ArrayElement> window(int[] nums, int primary, int secondary) {
        List<ArrayElement> state = new ArrayList<>(nums.length);
        for (int i = 0; i < nums.length; i++) {
            String s = i == primary ? "current" : i == secondary ? "target" : "default";
            state.add(new ArrayElement(i, nums[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int n = nums.length;
        int ind = -1;

        for (int i = n - 2; i >= 0; i--) {
            if (nums[i] < nums[i + 1]) {
                ind = i;
                emit.at("foundBreak")
                        .say("nums[%d]=%d < nums[%d]=%d - the first ascent from the right. "
                                + "Everything after index %d is non-increasing, so this is "
                                + "the last digit we can still increase.", i, nums[i], i + 1,
                                nums[i + 1], i)
                        .var("ind", ind)
                        .arrayState(window(nums, i, i + 1)).step();
                break;
            } else {
                emit.at("checkBreak")
                        .say("nums[%d]=%d >= nums[%d]=%d - still non-increasing here, keep "
                                + "scanning left.", i, nums[i], i + 1, nums[i + 1])
                        .var("i", i)
                        .arrayState(window(nums, i, i + 1)).step();
            }
        }

        if (ind == -1) {
            emit.at("noBreak")
                    .say("No ascent exists anywhere: the array is strictly non-increasing, "
                            + "so it is already the last permutation. Reverse the whole "
                            + "array to wrap around to the first.")
                    .arrayState(window(nums, -1, -1)).step();
            reverse(nums, 0, n - 1);
            emit.at("done")
                    .say("Wrapped to the first permutation: %s.", java.util.Arrays.toString(nums))
                    .var("result", java.util.Arrays.toString(nums))
                    .arrayState(window(nums, -1, -1)).step();
            return;
        }

        for (int i = n - 1; i > ind; i--) {
            if (nums[i] > nums[ind]) {
                emit.at("foundSwap")
                        .say("Scanning from the right, nums[%d]=%d is the first value that "
                                + "beats nums[%d]=%d - the smallest suffix value still "
                                + "greater than it. Swap them.", i, nums[i], ind, nums[ind])
                        .var("i", i).var("ind", ind)
                        .arrayState(window(nums, i, ind)).step();
                swap(nums, i, ind);
                break;
            } else {
                emit.at("checkSwap")
                        .say("nums[%d]=%d does not beat nums[%d]=%d - keep scanning left "
                                + "for a smaller candidate that still beats it.",
                                i, nums[i], ind, nums[ind])
                        .var("i", i).var("ind", ind)
                        .arrayState(window(nums, i, ind)).step();
            }
        }

        emit.at("reverseSuffix")
                .say("Reverse the suffix after index %d back to ascending order, so it is "
                        + "the smallest arrangement of those values.", ind)
                .var("from", ind + 1).var("to", n - 1)
                .arrayState(window(nums, ind, -1)).step();
        reverse(nums, ind + 1, n - 1);

        emit.at("done")
                .say("Next permutation: %s.", java.util.Arrays.toString(nums))
                .var("result", java.util.Arrays.toString(nums))
                .arrayState(window(nums, -1, -1)).step();
    }

    private static void swap(int[] nums, int i, int j) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    private static void reverse(int[] nums, int from, int to) {
        while (from < to) {
            swap(nums, from, to);
            from++;
            to--;
        }
    }
}
