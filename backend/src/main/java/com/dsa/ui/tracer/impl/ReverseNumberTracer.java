package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds the reversal digit by digit: each peeled digit shifts the accumulator left one
 * place and lands in the units column.
 */
@Component
public class ReverseNumberTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "reverse-number";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("N")
                        .help("A positive whole number.")
                        .range(1, 999_999_999)
                        .defaultValue(12345)
                        .build());
    }

    /** Trailing zeros vanish on reversal — 900 reverses to 9, not 009. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 900);
    }

    @Override
    public String annotatedCode() {
        return """
               public int reverseNumber(int n) {
                   // @a init
                   int rev = 0;
                   int temp = n;
                   while (temp > 0) {
                       int lastDigit = temp % 10;
                       // @a build
                       rev = (rev * 10) + lastDigit;
                       temp = temp / 10;
                   }
                   // @a done
                   return rev;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        int[] digits = CountDigitsTracer.digitsOf(n);
        int rev = 0;
        int temp = n;

        emit.at("init")
                .say("Start with rev = 0 and temp = %d.", n)
                .var("rev", rev).var("temp", temp)
                .array(digits).step();

        int peeled = 0;
        while (temp > 0) {
            int lastDigit = temp % 10;
            rev = (rev * 10) + lastDigit;
            temp = temp / 10;
            peeled++;
            int highlight = digits.length - peeled;
            emit.at("build")
                    .say("Peel %d off temp, then rev = rev * 10 + %d = %d. temp shrinks to %d.",
                            lastDigit, lastDigit, rev, temp)
                    .var("digit", lastDigit).var("rev", rev).var("temp", temp)
                    .array(digits, Math.max(highlight, 0)).step();
        }

        emit.at("done")
                .say("temp reached 0 — %d reversed is %d.", n, rev)
                .var("rev", rev)
                .array(digits).step();
    }
}
