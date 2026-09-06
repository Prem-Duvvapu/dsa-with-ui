package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Bit Manipulation's own "single number" problem — a separate catalogue id from
 * {@link SingleNumberTracer}'s {@code single-number} (owned by {@code ArrayService}),
 * so the two must not share a default input or the two traces would fingerprint
 * identically and {@code noTwoTracersProduceIdenticalTraces} would refuse them both.
 */
@Component
public class SingleNumberBitTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "single-number-1";
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
                        .help("Every value appears exactly twice except one. XOR cancels the pairs.")
                        .length(1, 40).values(-999, 999)
                        .defaultValue(List.of(2, 2, 3, 3, 7))
                        .build());
    }

    /** Longer, 7-element array with a different unique value (42) and different parity of pairs. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("nums", List.of(8, 8, 15, 1, 1, 15, 42));
    }

    @Override
    public String annotatedCode() {
        return """
               public int singleNumber(int[] nums) {
                   // @a init
                   int xor = 0;
                   for (int i = 0; i < nums.length; i++) {
                       // @a xor
                       xor ^= nums[i];
                   }
                   // @a done
                   return xor;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] nums = in.getIntArray("nums");
        int xor = 0;

        emit.at("init")
                .say("Every value except one appears twice. XOR cancels a value with itself "
                        + "(a ^ a = 0), so a running XOR over the whole array leaves only the "
                        + "unpaired one behind. Start xor = 0.")
                .var("xor", xor).array(nums).step();

        for (int i = 0; i < nums.length; i++) {
            int prev = xor;
            xor ^= nums[i];
            emit.at("xor")
                    .say("i=%d: xor = %d ^ %d = %d.", i, prev, nums[i], xor)
                    .var("i", i).var("nums[i]", nums[i]).var("xor", xor)
                    .array(nums, i).step();
        }

        emit.at("done")
                .say("All paired values cancelled to 0. The single number is %d.", xor)
                .var("result", xor).array(nums).step();
    }
}
