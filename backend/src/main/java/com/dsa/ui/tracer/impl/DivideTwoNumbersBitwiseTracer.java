package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LeetCode 29's bit-shift long division: work in absolute values, and for each shift from
 * the dividend's own bit length down to 0, subtract {@code divisor << shift} whenever the
 * remaining dividend can afford it, recording that shift in the quotient. Restarting the
 * shift at the dividend's bit length (not always 31) keeps the trace proportional to the
 * input's own size instead of always emitting 32 steps.
 *
 * <p>Sign is tracked separately via one XOR of the two sign bits and reapplied at the end,
 * so the shifting loop itself only ever sees non-negative magnitudes. The one input the
 * shifting technique cannot represent is a zero divisor - not a magnitude problem but an
 * undefined operation - so it is rejected before the loop runs, through the same structured
 * validation path malformed input already uses.
 */
@Component
public class DivideTwoNumbersBitwiseTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "divide-two-numbers-bitwise";
    }

    @Override
    public DsType dsType() {
        return DsType.BITS;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("dividend", FieldType.INT)
                        .label("Dividend")
                        .help("The number being divided.")
                        .range(-1_000_000_000, 1_000_000_000)
                        .defaultValue(22)
                        .build(),
                InputField.of("divisor", FieldType.INT)
                        .label("Divisor")
                        .help("Must not be zero.")
                        .range(-1_000_000_000, 1_000_000_000)
                        .defaultValue(3)
                        .build());
    }

    /** Opposite signs and a divisor that does not divide evenly - truncation toward zero matters here. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("dividend", -43, "divisor", 8);
    }

    @Override
    public String annotatedCode() {
        return """
               public int divide(int dividend, int divisor) {
                   // @a sign
                   boolean negative = (dividend < 0) ^ (divisor < 0);
                   long a = Math.abs((long) dividend), b = Math.abs((long) divisor);
                   long quotient = 0;
                   int startShift = a <= 1 ? 0 : 63 - Long.numberOfLeadingZeros(a);
                   for (int shift = startShift; shift >= 0; shift--) {
                       // @a shiftCheck
                       if ((a >> shift) >= b) {
                           // @a shiftApply
                           a -= b << shift;
                           quotient |= 1L << shift;
                       }
                   }
                   // @a done
                   long result = negative ? -quotient : quotient;
                   return (int) Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, result));
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int dividendIn = in.getInt("dividend");
        int divisorIn = in.getInt("divisor");

        if (divisorIn == 0) {
            throw new InputValidationException(Map.of("divisor", "Division by zero is undefined."));
        }

        boolean negative = (dividendIn < 0) ^ (divisorIn < 0);
        long a = Math.abs((long) dividendIn);
        long b = Math.abs((long) divisorIn);

        emit.at("sign")
                .say("dividend = %d, divisor = %d. Signs %s, so the result will be %s. Work in absolute "
                        + "values: |dividend| = %d, |divisor| = %d.",
                        dividendIn, divisorIn, negative ? "differ" : "match", negative ? "negative" : "non-negative", a, b)
                .var("dividend", dividendIn).var("divisor", divisorIn).var("negative", negative)
                .bits((int) a).step();

        long quotient = 0;
        int startShift = a <= 1 ? 0 : 63 - Long.numberOfLeadingZeros(a);

        for (int shift = startShift; shift >= 0; shift--) {
            long shifted = b << shift;
            emit.at("shiftCheck")
                    .say("shift = %d: is the remaining %d at least divisor << %d = %d?", shift, a, shift, shifted)
                    .var("shift", shift).var("remaining", a).var("divisor << shift", shifted)
                    .bits((int) a).step();

            if (a >= shifted) {
                a -= shifted;
                quotient |= 1L << shift;
                emit.at("shiftApply")
                        .say("Yes — subtract %d, leaving %d. Set bit %d of the quotient: quotient = %d.",
                                shifted, a, shift, quotient)
                        .var("remaining", a).var("quotient", quotient)
                        .bits((int) a).step();
            }
        }

        long signedResult = negative ? -quotient : quotient;
        long clamped = Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, signedResult));
        emit.at("done")
                .say("Every shift checked. Magnitude quotient = %d, sign %s: %d / %d = %d.",
                        quotient, negative ? "negative" : "positive", dividendIn, divisorIn, clamped)
                .var("answer", clamped)
                .bits((int) clamped).step();
    }
}
