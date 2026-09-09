package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Whitespace and an optional sign are consumed once, iteratively, before the recursion ever
 * starts — they are a single decision each, not a repeated one. Only the digit-by-digit
 * accumulation actually recurses: each call folds one more digit into the running total and
 * hands the growing value forward, until a non-digit (or the string's end) stops it.
 */
@Component
public class AtoiRecursiveTracer implements AlgorithmTracer {

    private static final long INT_MAX = Integer.MAX_VALUE;
    private static final long INT_MIN = Integer.MIN_VALUE;

    @Override
    public String id() {
        return "atoi-recursive";
    }

    @Override
    public DsType dsType() {
        return DsType.STRING;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("s", FieldType.STRING)
                        .label("Input")
                        .help("May contain leading whitespace, an optional sign, then digits.")
                        .length(1, 20)
                        .defaultValue(" -042")
                        .build());
    }

    /** No leading whitespace or sign, and digits stop at the first non-digit character. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("s", "4193 with words");
    }

    @Override
    public String annotatedCode() {
        return """
               public int myAtoi(String s) {
                   int i = 0;
                   while (i < s.length() && s.charAt(i) == ' ') i++;
                   // @a skipWhitespace
                   int sign = 1;
                   if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
                       // @a readSign
                       sign = s.charAt(i) == '-' ? -1 : 1;
                       i++;
                   }
                   long value = accumulate(s, i, 0L);
                   // @a clamp
                   long signed = sign * value;
                   if (signed > Integer.MAX_VALUE) return Integer.MAX_VALUE;
                   if (signed < Integer.MIN_VALUE) return Integer.MIN_VALUE;
                   return (int) signed;
               }

               private long accumulate(String s, int i, long acc) {
                   if (i >= s.length() || !Character.isDigit(s.charAt(i))) {
                       // @a stop
                       return acc;
                   }
                   // @a digit
                   return accumulate(s, i + 1, acc * 10 + (s.charAt(i) - '0'));
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int i = 0;
        while (i < s.length() && s.charAt(i) == ' ') {
            i++;
        }
        emit.at("skipWhitespace")
                .say("Skip leading whitespace - digits start at index %d.", i)
                .var("i", i).chars(s, i < s.length() ? i : -1, -1).step();

        int sign = 1;
        if (i < s.length() && (s.charAt(i) == '+' || s.charAt(i) == '-')) {
            sign = s.charAt(i) == '-' ? -1 : 1;
            emit.at("readSign")
                    .say("'%c' at index %d sets sign = %d.", s.charAt(i), i, sign)
                    .var("sign", sign).chars(s, i, -1).step();
            i++;
        }

        long value = accumulate(s, i, 0L, emit);
        long signed = sign * value;
        int result = signed > INT_MAX ? Integer.MAX_VALUE : signed < INT_MIN ? Integer.MIN_VALUE : (int) signed;

        emit.at("clamp")
                .say("Apply sign: %d * %d = %d. %s Final result: %d.",
                        sign, value, signed,
                        signed != result ? "Clamped to the 32-bit int range." : "Within range, no clamping needed.",
                        result)
                .var("value", value).var("signed", signed).var("result", result)
                .chars(s).step();
    }

    private long accumulate(String s, int i, long acc, StepEmitter emit) {
        emit.push("accumulate(i=" + i + ",acc=" + acc + ")");

        if (i >= s.length() || !Character.isDigit(s.charAt(i))) {
            emit.at("stop")
                    .say(i >= s.length()
                            ? String.format("Reached the end of the string - accumulated value = %d.", acc)
                            : String.format("'%c' at index %d is not a digit - stop. Accumulated value = %d.",
                                    s.charAt(i), i, acc))
                    .var("i", i).var("acc", acc).chars(s, i < s.length() ? i : -1, -1).step();
            emit.pop();
            return acc;
        }

        long next = acc * 10 + (s.charAt(i) - '0');
        emit.at("digit")
                .say("'%c' at index %d folds in: acc = %d * 10 + %c = %d. Recurse into index %d.",
                        s.charAt(i), i, acc, s.charAt(i), next, i + 1)
                .var("i", i).var("acc", next).chars(s, i, -1).step();

        long result = accumulate(s, i + 1, next, emit);
        emit.pop();
        return result;
    }
}
