package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binary search the day count: for a candidate day, walk the field once counting runs of
 * {@code k} adjacent already-bloomed flowers into bouquets. More days can only bloom more
 * flowers, never fewer, so feasibility is monotonic. When {@code m*k} exceeds the flower
 * count outright, no number of days can ever help — that check runs once, before any search.
 */
@Component
public class MinDaysBouquetsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "min-days-bouquets";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("bloomDay", FieldType.INT_ARRAY)
                        .label("Bloom day per flower")
                        .length(1, 30).values(1, 1000)
                        .defaultValue(List.of(1, 10, 3, 10, 2))
                        .build(),
                InputField.of("m", FieldType.INT)
                        .label("Bouquets needed (M)")
                        .range(1, 30)
                        .defaultValue(3)
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Adjacent flowers per bouquet (K)")
                        .range(1, 30)
                        .defaultValue(1)
                        .build());
    }

    /** m*k exceeds the flower count — impossible regardless of how many days pass. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("bloomDay", List.of(1, 10, 3, 10, 2), "m", 4, "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public int minDays(int[] bloomDay, int m, int k) {
                   long need = (long) m * k;
                   if (need > bloomDay.length) {
                       // @a impossible
                       return -1;
                   }
                   // @a init
                   int low = min(bloomDay), high = max(bloomDay), ans = -1;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       int bouquets = 0, run = 0;
                       for (int day : bloomDay) {
                           if (day <= mid) {
                               // @a extendRun
                               run++;
                               if (run == k) { bouquets++; run = 0; }
                           } else {
                               // @a breakRun
                               run = 0;
                           }
                       }
                       if (bouquets >= m) {
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

    private List<ArrayElement> state(int[] bloomDay, int upTo, int highlight) {
        List<ArrayElement> s = new ArrayList<>(bloomDay.length);
        for (int i = 0; i < bloomDay.length; i++) {
            String st = i == highlight ? "current" : bloomDay[i] <= upTo ? "target" : "default";
            s.add(new ArrayElement(i, bloomDay[i], st));
        }
        return s;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] bloomDay = in.getIntArray("bloomDay");
        int m = in.getInt("m");
        int k = in.getInt("k");
        long need = (long) m * k;

        if (need > bloomDay.length) {
            emit.at("impossible")
                    .say("%d bouquets of %d adjacent flowers need %d flowers total, but there are only %d — impossible.",
                            m, k, need, bloomDay.length)
                    .var("answer", -1).arrayState(state(bloomDay, -1, -1)).step();
            return;
        }

        int low = Integer.MAX_VALUE, high = Integer.MIN_VALUE;
        for (int day : bloomDay) {
            low = Math.min(low, day);
            high = Math.max(high, day);
        }
        int ans = -1;

        emit.at("init")
                .say("Binary search the day count. Nothing blooms before day %d, and everything has bloomed by day %d.",
                        low, high)
                .var("low", low).var("high", high)
                .arrayState(state(bloomDay, low - 1, -1)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Test day %d: which flowers have bloomed by then?", mid)
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(state(bloomDay, mid, -1)).step();

            int bouquets = 0, run = 0;
            for (int i = 0; i < bloomDay.length; i++) {
                if (bloomDay[i] <= mid) {
                    run++;
                    if (run == k) {
                        bouquets++;
                        run = 0;
                    }
                    emit.at("extendRun")
                            .say("Flower %d (day %d) has bloomed. Run = %d, bouquets so far = %d.",
                                    i, bloomDay[i], run, bouquets)
                            .var("run", run).var("bouquets", bouquets)
                            .arrayState(state(bloomDay, mid, i)).step();
                } else {
                    run = 0;
                    emit.at("breakRun")
                            .say("Flower %d (day %d) has not bloomed yet — the run of adjacent flowers breaks.",
                                    i, bloomDay[i])
                            .var("run", run)
                            .arrayState(state(bloomDay, mid, i)).step();
                }
            }

            if (bouquets >= m) {
                ans = mid;
                emit.at("feasible")
                        .say("%d bouquets made (needed %d) by day %d — feasible. Try an earlier day.", bouquets, m, mid)
                        .var("ans", ans).var("high", mid - 1)
                        .arrayState(state(bloomDay, mid, -1)).step();
                high = mid - 1;
            } else {
                emit.at("infeasible")
                        .say("Only %d bouquets made (needed %d) by day %d — too early. Try later.", bouquets, m, mid)
                        .var("low", mid + 1)
                        .arrayState(state(bloomDay, mid, -1)).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("low passed high. The earliest day with enough bouquets is %d.", ans)
                .var("answer", ans).arrayState(state(bloomDay, ans, -1)).step();
    }
}
