package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Left rotate array by K places using the optimal 3-step reversal algorithm:
 * reverse(0, k-1), reverse(k, n-1), and reverse(0, n-1).
 */
@Component
public class LeftRotateKTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "left-rotate-k";
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
                        .help("The array to rotate.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(1, 2, 3, 4, 5, 6, 7))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Rotate steps")
                        .help("Number of positions to rotate left.")
                        .range(0, 1000)
                        .defaultValue(2)
                        .build());
    }

    /** Different array length (6 vs 7) and k=4 places split at index 4 (producing [5, 6, 1, 2, 3, 4]). */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(1, 2, 3, 4, 5, 6), "k", 4);
    }

    @Override
    public String annotatedCode() {
        return """
               public void rotate(int[] nums, int k) {
                   // @a init
                   int n = nums.length;
                   k = k % n;
                   // @a callFirst
                   reverse(nums, 0, k - 1);
                   // @a callRest
                   reverse(nums, k, n - 1);
                   // @a callAll
                   reverse(nums, 0, n - 1);
                   // @a done
               }
               
               private void reverse(int[] nums, int start, int end) {
                   while (start < end) {
                       // @a swap
                       int temp = nums[start];
                       nums[start] = nums[end];
                       nums[end] = temp;
                       start++;
                       end--;
                   }
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int k = in.getInt("k");
        int n = nums.length;

        k = k % n;
        emit.at("init")
                .say("Array length n=%d; normalized k = %d %% %d = %d.", n, in.getInt("k"), n, k)
                .var("n", n).var("k", k).array(nums).step();

        emit.at("callFirst")
                .say("Phase 1: Reverse first k=%d elements (indices 0..%d).", k, k - 1)
                .var("k", k).array(nums).step();
        reverse(nums, 0, k - 1, emit);

        emit.at("callRest")
                .say("Phase 2: Reverse remaining n-k=%d elements (indices %d..%d).", n - k, k, n - 1)
                .var("k", k).array(nums).step();
        reverse(nums, k, n - 1, emit);

        emit.at("callAll")
                .say("Phase 3: Reverse entire array (indices 0..%d).", n - 1)
                .var("k", k).array(nums).step();
        reverse(nums, 0, n - 1, emit);

        // Every other tracer closes by stating its answer; this one stopped on the last
        // swap of phase 3, so the trace ended mid-reverse and never said what the rotation
        // produced. Three reversals is a trick, and a trick needs the punchline.
        emit.at("done")
                .say("Three reversals and no extra array: rotating left by %d gives %s.",
                        k, java.util.Arrays.toString(nums))
                .var("k", k).var("result", java.util.Arrays.toString(nums))
                .array(nums).step();
    }

    private void reverse(int[] nums, int start, int end, StepEmitter emit) {
        while (start < end) {
            int temp = nums[start];
            nums[start] = nums[end];
            nums[end] = temp;
            // The swap has already happened by the time this step is emitted, so the
            // sentence must describe where the values LANDED. It used to print both cells'
            // old contents - "Swap nums[0]=1 and nums[1]=2" - beside a picture already
            // showing 2 at index 0, so the words and the cells disagreed on the same step.
            emit.at("swap")
                    .say("Swap indices %d and %d: %d moves to index %d, %d to index %d.",
                            start, end, nums[start], start, nums[end], end)
                    .var("start", start).var("end", end).array(nums, start, end).step();
            start++;
            end--;
        }
    }
}
