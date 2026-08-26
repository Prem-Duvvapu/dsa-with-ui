package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Find the single number that appears once in an array where all other elements appear twice,
 * using bitwise XOR cancellation (a ^ a = 0).
 */
@Component
public class SingleNumberTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "single-number";
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("nums", FieldType.INT_ARRAY)
                        .label("Array")
                        .help("Array where one element appears once and all others twice.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(4, 1, 2, 1, 2))
                        .build());
    }

    /** Shorter 3-element array [7, 3, 7] where 7s cancel, isolating 3. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(7, 3, 7));
    }

    @Override
    public String annotatedCode() {
        return """
               public int singleNumber(int[] nums) {
                   // @a init
                   int xorSum = 0;
                   for (int num : nums) {
                       // @a xor
                       xorSum ^= num;
                   }
                   // @a done
                   return xorSum;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int xorSum = 0;

        emit.using("Array");
        emit.at("init")
                .say("Initialize xorSum = 0. Paired identical numbers will cancel out (a ^ a = 0).")
                .var("xorSum", 0).array(nums).step();

        for (int i = 0; i < nums.length; i++) {
            int prev = xorSum;
            xorSum ^= nums[i];
            emit.at("xor")
                    .say("i=%d: xorSum = %d ^ nums[%d]=%d -> %d.", i, prev, i, nums[i], xorSum)
                    .var("i", i).var("num", nums[i]).var("xorSum", xorSum)
                    .array(nums, i).step();
        }

        emit.at("done")
                .say("Bitwise XOR cancellation complete. Single non-duplicated element is %d.", xorSum)
                .var("result", xorSum).array(nums).step();
    }
}
