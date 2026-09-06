package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Builds the full reversal (same digit-by-digit accumulator as {@code reverse-number}),
 * then compares it against the untouched original in one shot.
 */
@Component
public class PalindromeNumberTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "palindrome-number";
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
                        .help("A non-negative whole number.")
                        .range(0, 999_999_999)
                        .defaultValue(12321)
                        .build());
    }

    /** Not a palindrome — the reversed value differs from the original. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 1234);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isPalindrome(int n) {
                   // @a init
                   int original = n;
                   int rev = 0;
                   while (n > 0) {
                       int digit = n % 10;
                       // @a build
                       rev = (rev * 10) + digit;
                       n = n / 10;
                   }
                   // @a check
                   return original == rev;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int original = in.getInt("n");
        int[] digits = CountDigitsTracer.digitsOf(original);
        int n = original;
        int rev = 0;

        emit.at("init")
                .say("original = %d, rev starts at 0.", original)
                .var("original", original).var("rev", rev)
                .array(digits).step();

        int peeled = 0;
        while (n > 0) {
            int digit = n % 10;
            rev = (rev * 10) + digit;
            n = n / 10;
            peeled++;
            int highlight = digits.length - peeled;
            emit.at("build")
                    .say("Peel %d off, then rev = rev * 10 + %d = %d.", digit, digit, rev)
                    .var("digit", digit).var("rev", rev).var("n", n)
                    .array(digits, Math.max(highlight, 0)).step();
        }

        boolean result = original == rev;
        emit.at("check")
                .say("original (%d) == rev (%d) -> %b — %s a palindrome.",
                        original, rev, result, result ? "IS" : "is NOT")
                .var("result", result ? "PALINDROME" : "NOT PALINDROME")
                .array(digits).step();
    }
}
