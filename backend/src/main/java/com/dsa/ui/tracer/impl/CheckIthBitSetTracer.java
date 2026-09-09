package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Building the mask {@code 1 << i} and ANDing it against N isolates exactly bit i — every
 * other bit of N is zeroed by the mask, so the AND result is nonzero only when bit i itself
 * was set.
 */
@Component
public class CheckIthBitSetTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "check-ith-bit-set";
    }

    @Override
    public DsType dsType() {
        return DsType.BITS;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("N")
                        .help("Non-negative integer to inspect.")
                        .range(0, 1_000_000_000)
                        .defaultValue(13)
                        .build(),
                InputField.of("i", FieldType.INT)
                        .label("Bit index i")
                        .help("0 = least significant bit.")
                        .range(0, 31)
                        .defaultValue(2)
                        .build());
    }

    /** Bit 1 of 13 (1101) is 0 — the UNSET branch the default never reaches. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 13, "i", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isBitSet(int n, int i) {
                   // @a mask
                   int mask = 1 << i;
                   // @a check
                   int result = n & mask;
                   // @a done
                   return result != 0;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        int i = in.getInt("i");

        int mask = 1 << i;
        emit.at("mask")
                .say("N = %d, i = %d. Build mask = 1 << %d = %d — a single bit at position %d.", n, i, i, mask, i)
                .var("N", n).var("i", i).var("mask", mask).bits(n, i).step();

        int result = n & mask;
        boolean isSet = result != 0;
        emit.at("check")
                .say("N & mask = %d & %d = %d.", n, mask, result)
                .var("N & mask", result).bits(n, i).step();

        emit.at("done")
                .say(isSet ? String.format("Bit %d of %d IS set.", i, n)
                        : String.format("Bit %d of %d is NOT set.", i, n))
                .var("isSet", isSet).bits(n, i).step();
    }
}
