package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Binary exponentiation: repeatedly square the base and halve the exponent, folding
 * the base into the running result only on the exponent's odd (bit-set) steps — O(log N)
 * multiplications instead of O(N). Restricted to a non-negative integer exponent and a
 * positive integer base, since the tracer contract has no floating-point field type.
 */
@Component
public class PowXNMathTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "pow-x-n-math";
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
                        .help("Positive integer base.")
                        .range(1, 100)
                        .defaultValue(2)
                        .build(),
                InputField.of("n", FieldType.INT)
                        .label("Exponent (n)")
                        .help("Non-negative integer exponent.")
                        .range(0, 30)
                        .defaultValue(10)
                        .build());
    }

    /** x=3, n=5: odd exponent on the very first iteration, unlike the default's even 10. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("x", 3, "n", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public double myPow(double x, int n) {
                   // @a init
                   double result = 1.0, base = x;
                   int exp = n;
                   while (exp > 0) {
                       // @a odd
                       if (exp % 2 == 1) {
                           result *= base;
                       }
                       // @a square
                       base *= base;
                       // @a halve
                       exp /= 2;
                   }
                   // @a done
                   return result;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int x = in.getInt("x");
        int n = in.getInt("n");

        double result = 1.0;
        double base = x;
        int exp = n;

        emit.at("init")
                .say("Compute %d^%d by repeated squaring: result=1, base=%d, exp=%d.", x, n, x, n)
                .var("result", fmt(result)).var("base", fmt(base)).var("exp", exp).bits(exp).step();

        while (exp > 0) {
            boolean odd = exp % 2 == 1;
            if (odd) {
                result *= base;
                emit.at("odd")
                        .say("exp=%d is odd (its lowest bit is set), so fold base=%s into result -> %s.",
                                exp, fmt(base), fmt(result))
                        .var("result", fmt(result)).var("exp", exp).bits(exp, 0).step();
            } else {
                emit.at("odd")
                        .say("exp=%d is even (its lowest bit is 0), so result stays %s this round.",
                                exp, fmt(result))
                        .var("result", fmt(result)).var("exp", exp).bits(exp, 0).step();
            }

            double prevBase = base;
            base *= base;
            emit.at("square")
                    .say("Square the base: base = %s * %s = %s.", fmt(prevBase), fmt(prevBase), fmt(base))
                    .var("base", fmt(base)).bits(exp).step();

            exp /= 2;
            emit.at("halve")
                    .say("Shift the exponent right one bit: exp = %d.", exp)
                    .var("exp", exp).bits(exp).step();
        }

        emit.at("done")
                .say("exp reached 0. %d^%d = %s.", x, n, fmt(result))
                .var("result", fmt(result)).bits(0).step();
    }

    /** Whole numbers print without a trailing ".0" clutter-free; fractions keep their precision. */
    private static String fmt(double d) {
        return d == Math.floor(d) && !Double.isInfinite(d)
                ? String.valueOf((long) d)
                : String.valueOf(d);
    }
}
