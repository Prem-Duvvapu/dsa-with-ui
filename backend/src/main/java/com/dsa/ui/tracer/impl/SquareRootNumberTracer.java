package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Binary search over the answer itself, not the input: candidate x is feasible when
 * {@code x*x <= n}, and feasibility is monotonic (every smaller x is feasible too), which is
 * exactly what binary search needs. {@code long} arithmetic for the square keeps large n
 * from overflowing mid-search.
 */
@Component
public class SquareRootNumberTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "square-root-number";
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
                        .help("Find floor(sqrt(N)).")
                        .range(0, 2_000_000_000)
                        .defaultValue(28)
                        .build());
    }

    /** N = 1: low and high start equal, and the single candidate is immediately feasible. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public int floorSqrt(int n) {
                   // @a init
                   long low = 0, high = n, ans = 0;
                   while (low <= high) {
                       // @a mid
                       long mid = (low + high) / 2;
                       if (mid * mid <= n) {
                           // @a feasible
                           ans = mid;
                           low = mid + 1;
                       } else {
                           // @a infeasible
                           high = mid - 1;
                       }
                   }
                   // @a done
                   return (int) ans;
               }""";
    }

    /** The visible search window [low, mid, high] — the answer space itself, not an input array. */
    private int[] window(long low, long mid, long high) {
        return new int[]{(int) low, (int) mid, (int) high};
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        long low = 0, high = n, ans = 0;

        emit.at("init")
                .say("Binary search the answer: the largest x with x*x <= %d. Range starts at [0, %d].", n, n)
                .var("low", low).var("high", high).var("ans", ans)
                .array(window(low, low, high), 0, 2).step();

        while (low <= high) {
            long mid = (low + high) / 2;
            long sq = mid * mid;
            emit.at("mid")
                    .say("Test x = %d: %d*%d = %d.", mid, mid, mid, sq)
                    .var("low", low).var("high", high).var("mid", mid).var("mid*mid", sq)
                    .array(window(low, mid, high), 1, -1).step();

            if (sq <= n) {
                ans = mid;
                emit.at("feasible")
                        .say("%d <= %d — %d is a valid square root candidate. Try larger.", sq, n, mid)
                        .var("ans", ans).var("low", mid + 1)
                        .array(window(mid + 1, mid, high), 1, -1).step();
                low = mid + 1;
            } else {
                emit.at("infeasible")
                        .say("%d > %d — %d overshoots. Try smaller.", sq, n, mid)
                        .var("high", mid - 1)
                        .array(window(low, mid, mid - 1), 1, -1).step();
                high = mid - 1;
            }
        }

        emit.at("done")
                .say("low passed high. floor(sqrt(%d)) = %d.", n, ans)
                .var("answer", ans)
                .array(window(ans, ans, ans), 0, -1).step();
    }
}
