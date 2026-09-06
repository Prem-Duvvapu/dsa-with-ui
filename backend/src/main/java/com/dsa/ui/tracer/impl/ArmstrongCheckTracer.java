package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Raises each digit to the power of the digit count and sums the results; an Armstrong
 * number is one where that sum equals the number itself.
 */
@Component
public class ArmstrongCheckTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "armstrong-check";
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
                        .defaultValue(153)
                        .build());
    }

    /** Same digit count, but the cubed-digit sum does not equal the number. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 123);
    }

    @Override
    public String annotatedCode() {
        return """
               public boolean isArmstrong(int n) {
                   // @a init
                   int original = n;
                   int sum = 0;
                   int digits = String.valueOf(n).length();
                   while (n > 0) {
                       int digit = n % 10;
                       // @a accumulate
                       sum += Math.pow(digit, digits);
                       n = n / 10;
                   }
                   // @a check
                   return sum == original;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int original = in.getInt("n");
        int[] digitArr = CountDigitsTracer.digitsOf(original);
        int digitsCount = digitArr.length;
        int n = original;
        int sum = 0;

        emit.at("init")
                .say("N = %d has %d digits. sum starts at 0.", original, digitsCount)
                .var("original", original).var("digits", digitsCount).var("sum", sum)
                .array(digitArr).step();

        int peeled = 0;
        while (n > 0) {
            int digit = n % 10;
            int powVal = (int) Math.pow(digit, digitsCount);
            sum += powVal;
            n = n / 10;
            peeled++;
            int highlight = digitArr.length - peeled;
            emit.at("accumulate")
                    .say("digit %d ^ %d = %d. Running sum = %d.", digit, digitsCount, powVal, sum)
                    .var("digit", digit).var("pow", powVal).var("sum", sum)
                    .array(digitArr, Math.max(highlight, 0)).step();
        }

        boolean result = sum == original;
        emit.at("check")
                .say("sum (%d) == original (%d) -> %b — %s Armstrong.", sum, original, result,
                        result ? "IS" : "is NOT")
                .var("result", result ? "ARMSTRONG" : "NOT ARMSTRONG")
                .array(digitArr).step();
    }
}
