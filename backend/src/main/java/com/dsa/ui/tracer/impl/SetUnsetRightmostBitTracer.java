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

        // Three steps rather than one: the interesting part is *where* the carry (or borrow)
        // lands, and a single step asserting the answer never shows it. Each step stays on
        // the anchor that actually executed - the other branch's anchor is unreachable by
        // design, because `operation` selects between two different tricks.
        if (operation.equals("set")) {
            int target = rightmostUnsetBit(n);
            int result = n | (n + 1);
            emit.at("setRightmostUnset")
                    .say("N = %d is %s. Its rightmost 0 bit is at position %d - that is the bit to set.",
                            n, binary(n), target)
                    .var("N", n).var("N in binary", binary(n)).var("rightmost 0 at", target)
                    .bits(n, target).step();
            emit.at("setRightmostUnset")
                    .say("N + 1 = %d is %s. Adding 1 carries through the trailing 1s and lands a 1 exactly on position %d.",
                            n + 1, binary(n + 1), target)
                    .var("N", n).var("N + 1", n + 1).var("N + 1 in binary", binary(n + 1))
                    .bits(n + 1, target).step();
            emit.at("setRightmostUnset")
                    .say("OR-ing keeps every bit N already had and adopts that new 1: %d | %d = %d (%s).",
                            n, n + 1, result, binary(result))
                    .var("N", n).var("N + 1", n + 1).var("result", result)
                    .var("result in binary", binary(result))
                    .bits(result, target).step();
        } else {
            int target = rightmostSetBit(n);
            int result = n & (n - 1);
            emit.at("unsetRightmostSet")
                    .say("N = %d is %s. Its rightmost 1 bit is at position %d - that is the bit to clear.",
                            n, binary(n), target)
                    .var("N", n).var("N in binary", binary(n)).var("rightmost 1 at", target)
                    .bits(n, target).step();
            emit.at("unsetRightmostSet")
                    .say("N - 1 = %d is %s. Subtracting 1 borrows from position %d, flipping it to 0 and every bit below it to 1.",
                            n - 1, binary(n - 1), target)
                    .var("N", n).var("N - 1", n - 1).var("N - 1 in binary", binary(n - 1))
                    .bits(n - 1, target).step();
            emit.at("unsetRightmostSet")
                    .say("AND-ing keeps only the bits both share, so position %d is cleared: %d & %d = %d (%s).",
                            target, n, n - 1, result, binary(result))
                    .var("N", n).var("N - 1", n - 1).var("result", result)
                    .var("result in binary", binary(result))
                    .bits(result, target).step();
        }
    }

    /** Index of the lowest 0 bit of n. */
    private static int rightmostUnsetBit(int n) {
        int i = 0;
        while (((n >> i) & 1) == 1) {
            i++;
        }
        return i;
    }

    /** Index of the lowest 1 bit of n; -1 when n has none. */
    private static int rightmostSetBit(int n) {
        if (n == 0) {
            return -1;
        }
        int i = 0;
        while (((n >> i) & 1) == 0) {
            i++;
        }
        return i;
    }

    private static String binary(int n) {
        return "0b" + Integer.toBinaryString(n);
    }
}
