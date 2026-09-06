package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * XOR of every integer in {@code [L, R]}, computed in O(1) via the closed form
 * {@code f(n) = XOR(0..n)}, which only depends on {@code n % 4}:
 * <pre>
 * n%4==0 -> n
 * n%4==1 -> 1
 * n%4==2 -> n+1
 * n%4==3 -> 0
 * </pre>
 * XOR(L..R) is then {@code f(R) ^ f(L - 1)}. L is constrained to be at least 1 so
 * {@code L - 1} always names a valid prefix.
 */
@Component
public class XorNumbersInRangeTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "xor-numbers-in-range";
    }

    @Override
    public DsType dsType() {
        return DsType.BITS;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("l", FieldType.INT)
                        .label("L")
                        .help("Left end of the range (inclusive).")
                        .range(1, 1_000_000)
                        .defaultValue(3)
                        .build(),
                InputField.of("r", FieldType.INT)
                        .label("R")
                        .help("Right end of the range (inclusive), R >= L.")
                        .range(1, 1_000_000)
                        .defaultValue(9)
                        .build());
    }

    /** L == R collapses the range to a single value, and reaches the two residues (0 and 3) the default never does. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("l", 8, "r", 8);
    }

    @Override
    public String annotatedCode() {
        return """
               public int xorInRange(int l, int r) {
                   // @a upToR
                   int upToR = xorUpTo(r);
                   // @a upToL
                   int upToLMinus1 = xorUpTo(l - 1);
                   // @a combine
                   int result = upToR ^ upToLMinus1;
                   // @a done
                   return result;
               }

               private static int xorUpTo(int n) {
                   int rem = n % 4;
                   if (rem == 0) {
                       // @a mod0
                       return n;
                   } else if (rem == 1) {
                       // @a mod1
                       return 1;
                   } else if (rem == 2) {
                       // @a mod2
                       return n + 1;
                   } else {
                       // @a mod3
                       return 0;
                   }
               }""";
    }

    private int xorUpTo(int n, String label, StepEmitter emit) {
        int rem = n % 4;
        int value;
        String anchor;
        String reason;
        switch (rem) {
            case 0 -> { value = n; anchor = "mod0"; reason = "n % 4 == 0, so XOR(0.." + n + ") collapses to n itself"; }
            case 1 -> { value = 1; anchor = "mod1"; reason = "n % 4 == 1, so XOR(0.." + n + ") is always 1"; }
            case 2 -> { value = n + 1; anchor = "mod2"; reason = "n % 4 == 2, so XOR(0.." + n + ") is n + 1"; }
            default -> { value = 0; anchor = "mod3"; reason = "n % 4 == 3, so XOR(0.." + n + ") cancels to 0"; }
        }
        emit.at(anchor)
                .say("%s(%d) — %s: f(%d) = %d.", label, n, reason, n, value)
                .var(label + "(n)", value).bits(n).step();
        return value;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int l = in.getInt("l");
        int r = in.getInt("r");

        emit.at("upToR")
                .say("Compute f(R) = XOR(0..%d) using the pattern-of-4 trick, one residue class at a time.", r)
                .var("L", l).var("R", r).bits(r).step();
        int upToR = xorUpTo(r, "f", emit);

        emit.at("upToL")
                .say("Compute f(L-1) = XOR(0..%d) the same way, to subtract off everything below L.", l - 1)
                .var("L-1", l - 1).bits(l - 1).step();
        int upToLMinus1 = xorUpTo(l - 1, "f", emit);

        int result = upToR ^ upToLMinus1;
        emit.at("combine")
                .say("XOR(%d..%d) = f(%d) ^ f(%d) = %d ^ %d = %d.", l, r, r, l - 1, upToR, upToLMinus1, result)
                .var("result", result).bits(result).step();

        emit.at("done")
                .say("XOR of every integer from %d to %d is %d.", l, r, result)
                .var("XOR(L..R)", result).bits(result).step();
    }
}
