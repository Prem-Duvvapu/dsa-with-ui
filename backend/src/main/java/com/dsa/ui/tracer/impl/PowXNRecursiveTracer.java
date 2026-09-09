package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Halving the exponent each call — {@code x^n = (x^(n/2))^2}, with one extra factor of x
 * folded in when n is odd — turns O(n) multiplications into O(log n) recursive calls. A
 * negative exponent is handled once, up front: compute the positive-exponent result, then
 * invert it, rather than teaching the recursion two different sign conventions.
 */
@Component
public class PowXNRecursiveTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "pow-x-n-recursive";
    }

    @Override
    public DsType dsType() {
        return DsType.BITS;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("x", FieldType.INT)
                        .label("Base (x)")
                        .range(-20, 20)
                        .defaultValue(2)
                        .build(),
                InputField.of("n", FieldType.INT)
                        .label("Exponent (n)")
                        .range(-20, 20)
                        .defaultValue(10)
                        .build());
    }

    /** A negative exponent — the recursion still halves a positive magnitude, then the result is inverted. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("x", 2, "n", -3);
    }

    @Override
    public String annotatedCode() {
        return """
               public double myPow(double x, int n) {
                   long magnitude = Math.abs((long) n);
                   double result = power(x, magnitude);
                   // @a invert
                   return n < 0 ? 1 / result : result;
               }

               private double power(double x, long e) {
                   if (e == 0) {
                       // @a base
                       return 1.0;
                   }
                   double half = power(x, e / 2);
                   double squared = half * half;
                   if (e % 2 == 1) {
                       // @a odd
                       squared *= x;
                   } else {
                       // @a even
                   }
                   return squared;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int x = in.getInt("x");
        int n = in.getInt("n");
        long magnitude = Math.abs((long) n);

        double result = power(x, magnitude, emit);
        double finalResult = n < 0 ? 1 / result : result;

        emit.at("invert")
                .say(n < 0
                        ? String.format("n = %d is negative: invert %.6g to get 1/%.6g = %.6g.", n, result, result, finalResult)
                        : String.format("n = %d is non-negative: %.6g is already the answer.", n, finalResult))
                .var("result", finalResult).bits((int) magnitude).step();
    }

    private double power(double x, long e, StepEmitter emit) {
        emit.push("power(e=" + e + ")");

        if (e == 0) {
            emit.at("base")
                    .say("e = 0 - any base to the zeroth power is 1.")
                    .var("e", e).var("returns", 1.0).bits(0).step();
            emit.pop();
            return 1.0;
        }

        double half = power(x, e / 2, emit);
        double squared = half * half;
        double result;
        if (e % 2 == 1) {
            result = squared * x;
            emit.at("odd")
                    .say("e = %d is odd: (x^%d)^2 * x = %.6g * %.6g = %.6g.", e, e / 2, squared, x, result)
                    .var("e", e).var("half", half).var("result", result).bits((int) e).step();
        } else {
            result = squared;
            emit.at("even")
                    .say("e = %d is even: (x^%d)^2 = %.6g.", e, e / 2, result)
                    .var("e", e).var("half", half).var("result", result).bits((int) e).step();
        }

        emit.pop();
        return result;
    }
}
