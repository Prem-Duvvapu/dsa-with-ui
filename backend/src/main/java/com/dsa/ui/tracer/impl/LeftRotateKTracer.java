package com.dsa.ui.tracer.impl;

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

        emit.using("Array");
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
    }

    private void reverse(int[] nums, int start, int end, StepEmitter emit) {
        while (start < end) {
            int temp = nums[start];
            nums[start] = nums[end];
            nums[end] = temp;
            emit.at("swap")
                    .say("Swap nums[%d]=%d and nums[%d]=%d.", start, temp, end, nums[start])
                    .var("start", start).var("end", end).array(nums, start, end).step();
            start++;
            end--;
        }
    }
}
