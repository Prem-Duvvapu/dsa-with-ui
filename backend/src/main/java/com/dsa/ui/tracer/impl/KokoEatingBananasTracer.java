package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Binary search on the answer, like {@link AggressiveCowsTracer} - but this time the
 * search is minimizing rather than maximizing. Feasible here means "try a smaller speed",
 * the mirror image of aggressive-cows' "feasible means try a larger distance": the
 * candidate space still halves every iteration, just in the opposite direction.
 */
@Component
public class KokoEatingBananasTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "koko-eating-bananas";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("piles", FieldType.INT_ARRAY)
                        .label("Banana piles")
                        .length(1, 10).values(1, 1000)
                        .defaultValue(java.util.List.of(3, 6, 7, 11))
                        .build(),
                InputField.of("h", FieldType.INT)
                        .label("Hours available")
                        .range(1, 1000)
                        .defaultValue(8)
                        .build());
    }

    /** Larger piles and fewer hours: a much larger minimum speed is needed. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("piles", java.util.List.of(30, 11, 23, 4, 20), "h", 5);
    }

    @Override
    public String annotatedCode() {
        return """
               public int minEatingSpeed(int[] piles, int h) {
                   int max = 0;
                   for (int p : piles) max = Math.max(max, p);
                   // @a init
                   int low = 1, high = max, ans = high;

                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       long hours = 0;
                       for (int p : piles) {
                           // @a hoursTally
                           hours += (p + mid - 1) / mid;
                       }
                       if (hours <= h) {
                           // @a feasible
                           ans = mid;
                           high = mid - 1;
                       } else {
                           // @a infeasible
                           low = mid + 1;
                       }
                   }
                   // @a done
                   return ans;
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] piles = in.getIntArray("piles");
        int h = in.getInt("h");
        int max = 0;
        for (int p : piles) {
            max = Math.max(max, p);
        }
        int low = 1, high = max, ans = high;

        emit.at("init")
                .say("Binary search the answer itself: the slowest eating speed that still "
                        + "finishes in %d hours. Range starts at [1, %d] - 1 is the slowest "
                        + "possible speed, %d is fast enough to eat any single pile in one hour.",
                        h, max, max)
                .var("low", low).var("high", high).var("ans", ans)
                .array(piles).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Test whether eating at speed %d finishes within %d hours.", mid, h)
                    .var("low", low).var("high", high).var("mid", mid)
                    .array(piles).step();

            long hours = 0;
            for (int i = 0; i < piles.length; i++) {
                long pileHours = (piles[i] + mid - 1) / mid;
                hours += pileHours;
                emit.at("hoursTally")
                        .say("Pile %d has %d bananas - takes ceil(%d/%d)=%d hours at this speed. Running total: %d.",
                                i, piles[i], piles[i], mid, pileHours, hours)
                        .var("hours", hours)
                        .array(piles, i).step();
            }

            if (hours <= h) {
                ans = mid;
                emit.at("feasible")
                        .say("%d hours <= %d available - speed %d works. Record it and try slower.",
                                hours, h, mid)
                        .var("ans", ans).var("high", mid - 1)
                        .array(piles).step();
                high = mid - 1;
            } else {
                emit.at("infeasible")
                        .say("%d hours > %d available - speed %d is too slow. Try faster.",
                                hours, h, mid)
                        .var("low", mid + 1)
                        .array(piles).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("low passed high. The slowest speed that still finishes in time is %d.", ans)
                .var("answer", ans)
                .array(piles).step();
    }
}
