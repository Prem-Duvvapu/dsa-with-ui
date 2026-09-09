package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * The Mth root of N is another monotonic-feasibility search: {@code x^m} grows strictly
 * with x, so "is x^m <= N" flips exactly once across the candidate range. Unlike square
 * root, an exact match is possible — and often isn't, since N need not be a perfect power.
 */
@Component
public class NthRootNumberTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "nth-root-number";
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
                        .help("Value to find the root of.")
                        .range(1, 1_000_000_000)
                        .defaultValue(27)
                        .build(),
                InputField.of("m", FieldType.INT)
                        .label("Root degree (M)")
                        .range(1, 30)
                        .defaultValue(3)
                        .build());
    }

    /** 37 has no exact integer cube root, so the search ends with -1 instead of a match. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 37, "m", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public int nthRoot(int n, int m) {
                   // @a init
                   long low = 1, high = n, ans = -1;
                   while (low <= high) {
                       // @a mid
                       long mid = (low + high) / 2;
                       long val = power(mid, m, n);
                       if (val == n) {
                           // @a exact
                           ans = mid;
                           break;
                       } else if (val < n) {
                           // @a tooSmall
                           low = mid + 1;
                       } else {
                           // @a tooLarge
                           high = mid - 1;
                       }
                   }
                   // @a done
                   return (int) ans;
               }

               /** Multiplies up to m times, stopping early once the product exceeds cap. */
               private long power(long base, int m, long cap) {
                   long result = 1;
                   for (int i = 0; i < m && result <= cap; i++) result *= base;
                   return result;
               }""";
    }

    private long power(long base, int m, long cap) {
        long result = 1;
        for (int i = 0; i < m && result <= cap; i++) {
            result *= base;
        }
        return result;
    }

    private int[] window(long low, long mid, long high) {
        return new int[]{(int) low, (int) mid, (int) high};
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        int m = in.getInt("m");
        long low = 1, high = n, ans = -1;

        emit.at("init")
                .say("Binary search x such that x^%d = %d. Range starts at [1, %d].", m, n, n)
                .var("low", low).var("high", high).var("m", m)
                .array(window(low, low, high), 0, 2).step();

        while (low <= high) {
            long mid = (low + high) / 2;
            long val = power(mid, m, n);
            emit.at("mid")
                    .say("Test x = %d: x^%d = %d.", mid, m, val)
                    .var("low", low).var("high", high).var("mid", mid).var("mid^m", val)
                    .array(window(low, mid, high), 1, -1).step();

            if (val == n) {
                ans = mid;
                emit.at("exact")
                        .say("%d^%d = %d exactly — %d is the answer.", mid, m, val, mid)
                        .var("ans", ans)
                        .array(window(low, mid, high), 1, -1).step();
                break;
            } else if (val < n) {
                emit.at("tooSmall")
                        .say("%d < %d — %d is too small, try larger.", val, n, mid)
                        .var("low", mid + 1)
                        .array(window(mid + 1, mid, high), 1, -1).step();
                low = mid + 1;
            } else {
                emit.at("tooLarge")
                        .say("%d > %d — %d is too large, try smaller.", val, n, mid)
                        .var("high", mid - 1)
                        .array(window(low, mid, mid - 1), 1, -1).step();
                high = mid - 1;
            }
        }

        emit.at("done")
                .say(ans == -1
                        ? String.format("low passed high with no exact match — %d has no integer %d-th root.", n, m)
                        : String.format("The %d-th root of %d is %d.", m, n, ans))
                .var("answer", ans)
                .array(window(ans, ans, ans), 0, -1).step();
    }
}
