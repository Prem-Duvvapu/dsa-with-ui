package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * A "good" digit string needs a prime digit (2, 3, 5, or 7 — 4 choices) at every even index
 * and an even digit (0, 2, 4, 6, or 8 — 5 choices) at every odd index, and every position's
 * choice is independent of every other — so the count is just {@code 4^evenPositions *
 * 5^oddPositions}, computed with the same halving recursion as {@link PowXNRecursiveTracer},
 * kept under a modulus throughout so the intermediate products never overflow.
 */
@Component
public class CountGoodNumbersTracer implements AlgorithmTracer {

    private static final long MOD = 1_000_000_007L;

    @Override
    public String id() {
        return "count-good-numbers";
    }

    @Override
    public DsType dsType() {
        return DsType.BITS;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("Length (N)")
                        .range(1, 60)
                        .defaultValue(4)
                        .build());
    }

    /** Length 1 — a single even-indexed position, so only the prime-digit factor applies. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public int countGoodNumbers(int n) {
                   long evenPositions = (n + 1) / 2;
                   long oddPositions = n / 2;
                   // @a split
                   long primeChoices = power(4, evenPositions);
                   long evenDigitChoices = power(5, oddPositions);
                   // @a combine
                   return (int) ((primeChoices * evenDigitChoices) % MOD);
               }

               private long power(long base, long exp) {
                   if (exp == 0) {
                       // @a base
                       return 1L;
                   }
                   long half = power(base, exp / 2);
                   long squared = (half * half) % MOD;
                   if (exp % 2 == 1) {
                       // @a odd
                       squared = (squared * base) % MOD;
                   } else {
                       // @a even
                   }
                   return squared;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        long evenPositions = (n + 1) / 2;
        long oddPositions = n / 2;

        emit.at("split")
                .say("A length-%d string has %d even-indexed position(s) (need a prime digit: 2,3,5,7) "
                        + "and %d odd-indexed position(s) (need an even digit: 0,2,4,6,8).",
                        n, evenPositions, oddPositions)
                .var("evenPositions", evenPositions).var("oddPositions", oddPositions).bits(n).step();

        long primeChoices = power(4, evenPositions, emit);
        long evenDigitChoices = power(5, oddPositions, emit);
        long answer = (primeChoices * evenDigitChoices) % MOD;

        emit.at("combine")
                .say("4^%d * 5^%d mod %d = %d * %d mod %d = %d.",
                        evenPositions, oddPositions, MOD, primeChoices, evenDigitChoices, MOD, answer)
                .var("answer", answer).bits(n).step();
    }

    private long power(long base, long exp, StepEmitter emit) {
        emit.push("power(base=" + base + ",exp=" + exp + ")");

        if (exp == 0) {
            emit.at("base")
                    .say("exp = 0 - any base to the zeroth power is 1.")
                    .var("exp", exp).var("returns", 1).bits(0).step();
            emit.pop();
            return 1L;
        }

        long half = power(base, exp / 2, emit);
        long squared = (half * half) % MOD;
        long result;
        if (exp % 2 == 1) {
            result = (squared * base) % MOD;
            emit.at("odd")
                    .say("exp = %d is odd: (%d^%d)^2 * %d mod %d = %d.", exp, base, exp / 2, base, MOD, result)
                    .var("exp", exp).var("half", half).var("result", result).bits((int) exp).step();
        } else {
            result = squared;
            emit.at("even")
                    .say("exp = %d is even: (%d^%d)^2 mod %d = %d.", exp, base, exp / 2, MOD, result)
                    .var("exp", exp).var("half", half).var("result", result).bits((int) exp).step();
        }

        emit.pop();
        return result;
    }
}
