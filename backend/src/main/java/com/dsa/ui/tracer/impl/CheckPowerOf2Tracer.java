package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * A power of 2 has exactly one set bit, so {@code n - 1} flips every bit below (and
 * including) that bit — {@code n & (n - 1)} is zero only when there was one bit to flip.
 */
@Component
public class CheckPowerOf2Tracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "check-power-of-2";
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
                        .help("Positive integer to test.")
                        .range(1, 1_000_000_000)
                        .defaultValue(16)
                        .build());
    }

    /** 18 has two set bits, so it exercises the "not a power of 2" branch instead. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 18);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isPowerOfTwo(int n) {
                   // @a mask
                   int mask = n - 1;
                   // @a check
                   boolean isPower = (n & mask) == 0;
                   // @a done
                   return isPower;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");

        int mask = n - 1;
        emit.at("mask")
                .say("N = %d. A power of 2 has exactly one set bit, so N - 1 = %d flips every "
                        + "bit at or below it.", n, mask)
                .var("N", n).var("N - 1", mask).bits(n).step();

        int result = n & mask;
        boolean isPower = result == 0;
        emit.at("check")
                .say("N & (N - 1) = %d & %d = %d.", n, mask, result)
                .var("N & (N-1)", result).bits(mask).step();

        emit.at("done")
                .say(isPower ? String.format("%d IS a power of 2.", n)
                        : String.format("%d is NOT a power of 2.", n))
                .var("isPowerOfTwo", isPower).bits(result).step();
    }
}
