package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The catalogue title names two different tricks under one id — this tracer disambiguates
 * with an explicit {@code operation} choice rather than picking one silently:
 * {@code N | (N + 1)} sets the rightmost UNSET bit ({@code N + 1} carries a 1 into it),
 * {@code N & (N - 1)} unsets the rightmost SET bit ({@code N - 1} borrows down to it). The
 * second is the same identity {@link CountSetBitsTracer} loops with and
 * {@link CheckPowerOf2Tracer} tests once, but here it is the whole point of a single call
 * rather than a subroutine, so its trace is one operation long, not a loop.
 */
@Component
public class SetUnsetRightmostBitTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "set-unset-rightmost-bit";
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
                        .help("Non-negative integer.")
                        .range(0, 1_000_000_000)
                        .defaultValue(12)
                        .build(),
                InputField.of("operation", FieldType.STRING)
                        .label("Operation")
                        .help("\"set\" turns the rightmost 0 into a 1; \"unset\" turns the rightmost 1 into a 0.")
                        .constraint("pattern", "set|unset")
                        .constraint("patternHint", "Must be \"set\" or \"unset\".")
                        .defaultValue("set")
                        .build());
    }

    /** The complementary operation on the same N, so both branches get exercised. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 12, "operation", "unset");
    }

    @Override
    public String annotatedCode() {
        return """
               public int setOrUnsetRightmostBit(int n, String operation) {
                   if (operation.equals("set")) {
                       // @a setRightmostUnset
                       return n | (n + 1);
                   }
                   // @a unsetRightmostSet
                   return n & (n - 1);
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        String operation = in.getString("operation");

        if (operation.equals("set")) {
            int result = n | (n + 1);
            emit.at("setRightmostUnset")
                    .say("N + 1 = %d + 1 = %d carries a 1 into the rightmost 0 bit. N | (N + 1) = %d | %d = %d.",
                            n, n + 1, n, n + 1, result)
                    .var("N", n).var("N + 1", n + 1).var("result", result).bits(result).step();
        } else {
            int result = n & (n - 1);
            emit.at("unsetRightmostSet")
                    .say("N - 1 = %d - 1 = %d borrows down to the rightmost 1 bit. N & (N - 1) = %d & %d = %d.",
                            n, n - 1, n, n - 1, result)
                    .var("N", n).var("N - 1", n - 1).var("result", result).bits(result).step();
        }
    }
}
