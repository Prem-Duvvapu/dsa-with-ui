package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Peels the last digit off with {@code % 10} and shrinks the number with {@code / 10},
 * counting once per peel until nothing is left.
 */
@Component
public class CountDigitsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "count-digits";
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
                        .defaultValue(74295)
                        .build());
    }

    /** A single digit — the loop body runs exactly once instead of five times. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 7);
    }

    @Override
    public String annotatedCode() {
        return """
               public int countDigits(int n) {
                   // @a init
                   int count = 0;
                   int temp = n;
                   while (temp > 0) {
                       // @a extract
                       int lastDigit = temp % 10;
                       count++;
                       temp = temp / 10;
                   }
                   // @a done
                   return count;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        int[] digits = digitsOf(n);
        int count = 0;
        int temp = n;

        emit.at("init")
                .say("Start with count = 0 and temp = %d.", n)
                .var("count", count).var("temp", temp)
                .array(digits).step();

        int peeled = 0;
        while (temp > 0) {
            int lastDigit = temp % 10;
            count++;
            temp = temp / 10;
            peeled++;
            int highlight = digits.length - peeled;
            emit.at("extract")
                    .say("temp %% 10 peels off %d, count becomes %d, temp shrinks to %d.",
                            lastDigit, count, temp)
                    .var("lastDigit", lastDigit).var("count", count).var("temp", temp)
                    .array(digits, Math.max(highlight, 0)).step();
        }

        emit.at("done")
                .say("temp reached 0 — %d has %d digit%s.", n, count, count == 1 ? "" : "s")
                .var("count", count)
                .array(digits).step();
    }

    static int[] digitsOf(int n) {
        String s = String.valueOf(n);
        int[] out = new int[s.length()];
        for (int i = 0; i < s.length(); i++) {
            out[i] = s.charAt(i) - '0';
        }
        return out;
    }
}
