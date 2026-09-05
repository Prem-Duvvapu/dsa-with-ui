package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Binary search on the answer: instead of searching for a value in the array, search the
 * space of possible minimum distances, using a greedy placement as the feasibility check
 * for each candidate. The array being searched is never the stall positions themselves -
 * it is the range of distances from 1 up to the full span of the stalls.
 */
@Component
public class AggressiveCowsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "aggressive-cows";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("stalls", FieldType.INT_ARRAY)
                        .label("Stall positions")
                        .help("Sorted positions along the stable.")
                        .length(2, 12).values(0, 10000).sorted()
                        .defaultValue(List.of(1, 2, 4, 8, 9))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Cows")
                        .range(2, 12)
                        .defaultValue(3)
                        .build());
    }

    /** Fewer cows over evenly-spaced stalls: the answer sits at the full span, feasible on the first real test. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("stalls", List.of(1, 2, 3, 4, 5), "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public int aggressiveCows(int[] stalls, int k) {
                   int n = stalls.length;
                   // @a init
                   int low = 1, high = stalls[n - 1] - stalls[0], ans = 0;
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (canPlace(stalls, k, mid)) {
                           // @a feasible
                           ans = mid;
                           low = mid + 1;
                       } else {
                           // @a infeasible
                           high = mid - 1;
                       }
                   }
                   // @a done
                   return ans;
               }

               private boolean canPlace(int[] stalls, int k, int dist) {
                   int count = 1, last = stalls[0];
                   for (int i = 1; i < stalls.length; i++) {
                       if (stalls[i] - last >= dist) {
                           // @a place
                           count++;
                           last = stalls[i];
                       } else {
                           // @a skip
                           continue;
                       }
                   }
                   return count >= k;
               }""";
    }

    private List<ArrayElement> stallState(int[] stalls, Set<Integer> placed, int current) {
        List<ArrayElement> state = new ArrayList<>(stalls.length);
        for (int i = 0; i < stalls.length; i++) {
            String s = i == current ? "current" : placed.contains(i) ? "sorted" : "default";
            state.add(new ArrayElement(i, stalls[i], s));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] stalls = in.getIntArray("stalls");
        int k = in.getInt("k");
        int n = stalls.length;
        int low = 1;
        int high = stalls[n - 1] - stalls[0];
        int ans = 0;

        emit.at("init")
                .say("Binary search the answer itself: the largest minimum distance "
                        + "between any two cows. Range starts at [%d, %d] - 1 is the "
                        + "smallest useful gap, %d is the full span of the stalls.",
                        low, high, high)
                .var("low", low).var("high", high).var("ans", ans)
                .arrayState(stallState(stalls, Set.of(), -1)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Test whether %d cows can be placed with every pair at least %d apart.",
                            k, mid)
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(stallState(stalls, Set.of(), -1)).step();

            int count = 1;
            int last = stalls[0];
            Set<Integer> placed = new LinkedHashSet<>();
            placed.add(0);
            for (int i = 1; i < n; i++) {
                if (stalls[i] - last >= mid) {
                    count++;
                    placed.add(i);
                    emit.at("place")
                            .say("Stall %d is %d away from the last placed cow (>= %d) - "
                                    + "place cow #%d here.", stalls[i], stalls[i] - last, mid, count)
                            .var("i", i).var("count", count)
                            .arrayState(stallState(stalls, placed, i)).step();
                    last = stalls[i];
                } else {
                    emit.at("skip")
                            .say("Stall %d is only %d away from the last placed cow (< %d) "
                                    + "- too close, skip it.", stalls[i], stalls[i] - last, mid)
                            .var("i", i)
                            .arrayState(stallState(stalls, placed, i)).step();
                }
            }

            if (count >= k) {
                ans = mid;
                emit.at("feasible")
                        .say("Placed %d cows (needed %d) - distance %d works. Record it "
                                + "and try a larger distance.", count, k, mid)
                        .var("count", count).var("ans", ans).var("low", mid + 1)
                        .arrayState(stallState(stalls, placed, -1)).step();
                low = mid + 1;
            } else {
                emit.at("infeasible")
                        .say("Only placed %d cows (needed %d) - distance %d is too large. "
                                + "Try smaller.", count, k, mid)
                        .var("count", count).var("high", mid - 1)
                        .arrayState(stallState(stalls, placed, -1)).step();
                high = mid - 1;
            }
        }

        emit.at("done")
                .say("low passed high. The largest distance that still works is %d.", ans)
                .var("answer", ans)
                .arrayState(stallState(stalls, Set.of(), -1)).step();
    }
}
