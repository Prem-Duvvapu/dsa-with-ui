package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * An array of size n should hold every value in 1..n exactly once; here one value repeats
 * and another is missing. Summing the array and summing its squares gives two equations in
 * the two unknowns X (repeating) and Y (missing) against the sums a defect-free 1..n array
 * would have produced - solving them algebraically finds both in one O(N) pass, without ever
 * needing a frequency array.
 */
@Component
public class RepeatingMissingNumberTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "repeating-missing-number";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("a", FieldType.INT_ARRAY)
                        .label("Array (1..n, one repeated, one missing)")
                        .help("Should contain every value from 1 to n exactly once, except one "
                                + "value repeats and another is missing entirely. This is not "
                                + "checked - an array that does not fit that shape still runs, "
                                + "it just will not mean anything.")
                        .length(3, 12).values(1, 20)
                        .defaultValue(java.util.List.of(3, 1, 2, 5, 3))
                        .build());
    }

    /** Different length, different repeated/missing pair: 2 repeats, 3 is missing. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("a", java.util.List.of(1, 2, 2, 4));
    }

    @Override
    public String annotatedCode() {
        return """
               public int[] findMissingRepeatingNumber(int[] a) {
                   long n = a.length;
                   long SN = (n * (n + 1)) / 2;
                   long S2N = (n * (n + 1) * (2 * n + 1)) / 6;
                   long S = 0, S2 = 0;

                   for (int val : a) {
                       // @a sumPass
                       S += val;
                       S2 += (long) val * val;
                   }

                   // @a solveEquations
                   long val1 = S - SN;         // X - Y
                   long val2 = (S2 - S2N) / val1; // X + Y
                   long x = (val1 + val2) / 2; // repeating
                   long y = x - val1;          // missing

                   // @a done
                   return new int[]{(int) x, (int) y};
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] a = in.getIntArray("a");
        long n = a.length;
        long sn = (n * (n + 1)) / 2;
        long s2n = (n * (n + 1) * (2 * n + 1)) / 6;
        long s = 0, s2 = 0;

        for (int i = 0; i < a.length; i++) {
            s += a[i];
            s2 += (long) a[i] * a[i];
            emit.at("sumPass")
                    .say("Read a[%d]=%d. Running sum S=%d, running sum of squares S2=%d.",
                            i, a[i], s, s2)
                    .var("S", s).var("S2", s2)
                    .array(a, i).step();
        }

        long val1 = s - sn;
        long val2 = (s2 - s2n) / val1;
        long x = (val1 + val2) / 2;
        long y = x - val1;

        emit.at("solveEquations")
                .say("Expected sum SN=%d, expected sum of squares S2N=%d. "
                        + "X - Y = S - SN = %d. X + Y = (S2 - S2N) / (X - Y) = %d.",
                        sn, s2n, val1, val2)
                .var("SN", sn).var("S2N", s2n).var("X-Y", val1).var("X+Y", val2)
                .array(a).step();

        emit.at("done")
                .say("Solving the pair of equations: repeating X=%d, missing Y=%d.", x, y)
                .var("repeating", x).var("missing", y)
                .array(a).step();
    }
}
