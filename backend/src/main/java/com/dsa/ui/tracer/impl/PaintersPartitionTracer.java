package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The painter's time cap is minimized the same way {@link BookAllocationTracer} minimizes a
 * student's page cap: a greedy contiguous split decides how many painters a candidate cap
 * needs, and binary search shrinks the cap whenever that count still fits within the crew.
 */
@Component
public class PaintersPartitionTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "painters-partition";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("boards", FieldType.INT_ARRAY)
                        .label("Board lengths")
                        .help("Painted in this order — contiguous ranges only, one painter per range.")
                        .length(1, 20).values(1, 1000)
                        .defaultValue(List.of(10, 20, 30, 40))
                        .build(),
                InputField.of("painters", FieldType.INT)
                        .label("Painters")
                        .range(1, 20)
                        .defaultValue(2)
                        .build());
    }

    /** One painter per board: every split is trivially feasible, so the answer is just the largest board. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("boards", List.of(5, 5, 5, 5), "painters", 4);
    }

    @Override
    public String annotatedCode() {
        return """
               public int findLargestMinDistance(int[] boards, int painters) {
                   int low = max(boards), high = sum(boards), ans = high;
                   // @a init
                   while (low <= high) {
                       // @a mid
                       int mid = (low + high) / 2;
                       if (paintersNeeded(boards, mid) <= painters) {
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

               private int paintersNeeded(int[] boards, int cap) {
                   int painters = 1, time = 0;
                   for (int b : boards) {
                       if (time + b > cap) {
                           // @a newPainter
                           painters++;
                           time = b;
                       } else {
                           // @a addToCurrent
                           time += b;
                       }
                   }
                   return painters;
               }""";
    }

    private List<ArrayElement> state(int[] boards, int current, int splitStart) {
        List<ArrayElement> s = new ArrayList<>(boards.length);
        for (int i = 0; i < boards.length; i++) {
            String st = i == current ? "current" : i < splitStart ? "sorted" : "default";
            s.add(new ArrayElement(i, boards[i], st));
        }
        return s;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] boards = in.getIntArray("boards");
        int painters = in.getInt("painters");
        int max = 0, sum = 0;
        for (int b : boards) {
            max = Math.max(max, b);
            sum += b;
        }
        int low = max, high = sum, ans = high;

        emit.at("init")
                .say("Binary search the smallest possible cap on any one painter's total board length. "
                        + "Cannot be below the single longest board (%d) or above one painter doing everything (%d).",
                        low, high)
                .var("low", low).var("high", high)
                .arrayState(state(boards, -1, boards.length)).step();

        while (low <= high) {
            int mid = (low + high) / 2;
            emit.at("mid")
                    .say("Test whether a cap of %d needs at most %d painters.", mid, painters)
                    .var("low", low).var("high", high).var("mid", mid)
                    .arrayState(state(boards, -1, boards.length)).step();

            int used = 1, time = 0, splitStart = 0;
            for (int i = 0; i < boards.length; i++) {
                int b = boards[i];
                if (time + b > mid) {
                    used++;
                    time = b;
                    splitStart = i;
                    emit.at("newPainter")
                            .say("Board %d (%d) would exceed cap %d — painter #%d starts here.", i, b, mid, used)
                            .var("i", i).var("painters", used)
                            .arrayState(state(boards, i, splitStart)).step();
                } else {
                    time += b;
                    emit.at("addToCurrent")
                            .say("Board %d (%d) still fits this painter's queue (%d so far).", i, b, time)
                            .var("i", i).var("time", time)
                            .arrayState(state(boards, i, splitStart)).step();
                }
            }

            if (used <= painters) {
                ans = mid;
                emit.at("feasible")
                        .say("%d painters suffice (have %d) at cap %d. Try a smaller cap.", used, painters, mid)
                        .var("painters", used).var("ans", ans).var("high", mid - 1)
                        .arrayState(state(boards, -1, boards.length)).step();
                high = mid - 1;
            } else {
                emit.at("infeasible")
                        .say("%d painters are needed (have %d) at cap %d — too tight. Try larger.", used, painters, mid)
                        .var("painters", used).var("low", mid + 1)
                        .arrayState(state(boards, -1, boards.length)).step();
                low = mid + 1;
            }
        }

        emit.at("done")
                .say("low passed high. The smallest workable cap is %d.", ans)
                .var("answer", ans).arrayState(state(boards, -1, boards.length)).step();
    }
}
