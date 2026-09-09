package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Same greedy contiguous-partition feasibility as {@link BookAllocationTracer} and
 * {@link PaintersPartitionTracer} — a capacity's daily-load walk, not a page or paint cap —
 * so binary search over the capacity behaves identically: a working capacity is tried
 * smaller, a failing one tried larger.
 */
@Component
public class ShipPackagesDDaysTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "ship-packages-d-days";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("weights", FieldType.INT_ARRAY)
                        .label("Package weights")
                        .help("Loaded onto the ship in this order — contiguous groups only.")
                        .length(1, 25).values(1, 500)
                        .defaultValue(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10))
                        .build(),
                InputField.of("days", FieldType.INT)
                        .label("Days available")
                        .range(1, 25)
                        .defaultValue(5)
                        .build());
    }

    /** Fewer, smaller packages and fewer days — a much smaller answer with a different partition shape. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("weights", List.of(3, 2, 2, 4, 1, 4), "days", 3);
    }

    @Override
    public String annotatedCode() {
        return """
               public int shipWithinDays(int[] weights, int days) {
                   int low = max(weights), high = sum(weights), ans = high;
                   // @a init
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (daysNeeded(weights, mid) <= days) {
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
               }

               private int daysNeeded(int[] weights, int capacity) {
                   int days = 1, load = 0;
                   for (int w : weights) {
                       if (load + w > capacity) {
                           // @a newDay
                           days++;
                           load = w;
                       } else {
                           // @a addToDay
                           load += w;
                       }
                   }
                   return days;
               }""";
    }

    private List<ArrayElement> state(int[] weights, int current, int splitStart) {
        List<ArrayElement> s = new ArrayList<>(weights.length);
        for (int i = 0; i < weights.length; i++) {
            String st = i == current ? "current" : i < splitStart ? "sorted" : "default";
            s.add(new ArrayElement(i, weights[i], st));
        }
        return s;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] weights = in.getIntArray("weights");
        int daysAllowed = in.getInt("days");
        int max = 0, sum = 0;
        for (int w : weights) {
            max = Math.max(max, w);
            sum += w;
        }
        int low = max, high = sum, ans = high;

        emit.at("init")
                .say("Binary search the daily capacity: cannot be below the heaviest single package "
                        + "(%d) or above shipping everything in one day (%d).", low, high)
                .var("low", low).var("high", high)
                .arrayState(state(weights, -1, weights.length)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Test capacity %d packages per day. Can it ship within %d days?", mid, daysAllowed)
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(state(weights, -1, weights.length)).step();

            int days = 1, load = 0, splitStart = 0;
            for (int i = 0; i < weights.length; i++) {
                int w = weights[i];
                if (load + w > mid) {
                    days++;
                    load = w;
                    splitStart = i;
                    emit.at("newDay")
                            .say("Package %d (%d) would overflow capacity %d — start day %d.", i, w, mid, days)
                            .var("i", i).var("days", days)
                            .arrayState(state(weights, i, splitStart)).step();
                } else {
                    load += w;
                    emit.at("addToDay")
                            .say("Package %d (%d) still fits today's load (%d so far).", i, w, load)
                            .var("i", i).var("load", load)
                            .arrayState(state(weights, i, splitStart)).step();
                }
            }

            if (days <= daysAllowed) {
                ans = mid;
                emit.at("feasible")
                        .say("%d days needed (allowed %d) — capacity %d works. Try smaller.", days, daysAllowed, mid)
                        .var("days", days).var("ans", ans).var("high", mid - 1)
                        .arrayState(state(weights, -1, weights.length)).step();
                high = mid - 1;
            } else {
                emit.at("infeasible")
                        .say("%d days needed (allowed %d) — capacity %d is too small. Try larger.", days, daysAllowed, mid)
                        .var("days", days).var("low", mid + 1)
                        .arrayState(state(weights, -1, weights.length)).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("low passed high. The smallest workable capacity is %d.", ans)
                .var("answer", ans).arrayState(state(weights, -1, weights.length)).step();
    }
}
