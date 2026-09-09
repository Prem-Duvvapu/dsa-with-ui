package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The answer here is a real number, not an integer, so "low &lt;= high" cannot be the loop
 * condition — there is no adjacent-integer termination to reach. Instead the search runs a
 * fixed number of halvings (enough to shrink the window well past display precision) and
 * reports whatever {@code high} converges to. For a candidate max distance D, the number of
 * extra stations a gap needs is {@code ceil(gap / D) - 1}; summing that across every gap and
 * comparing to k is the same feasibility idea as every other answer-space search here.
 */
@Component
public class MinimizeMaxDistanceGasStationTracer implements AlgorithmTracer {

    private static final int ITERATIONS = 15;

    @Override
    public String id() {
        return "minimize-max-distance-gas-station";
    }

    @Override
    public DsType dsType() {
        return DsType.ARRAY;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("stations", FieldType.INT_ARRAY)
                        .label("Existing station positions")
                        .help("Sorted, strictly increasing positions along the road.")
                        .length(2, 15).values(0, 1000).sorted().distinct()
                        .defaultValue(List.of(1, 13, 17, 23))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("New stations to add (K)")
                        .range(1, 50)
                        .defaultValue(4)
                        .build());
    }

    /** A much smaller road with only one gas station to add. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("stations", List.of(1, 2, 3, 4, 5), "k", 1);
    }

    @Override
    public String annotatedCode() {
        return """
               public double minimizeMaxDistance(int[] stations, int k) {
                   int n = stations.length;
                   double[] gaps = new double[n - 1];
                   for (int i = 0; i < n - 1; i++) gaps[i] = stations[i + 1] - stations[i];
                   // @a init
                   double low = 0, high = max(gaps);
                   for (int iter = 0; iter < 15; iter++) {
                       // @a mid
                       double mid = (low + high) / 2;
                       int needed = 0;
                       for (double gap : gaps) {
                           // @a gapTally
                           needed += (int) Math.ceil(gap / mid) - 1;
                       }
                       if (needed <= k) {
                           // @a feasible
                           high = mid;
                       } else {
                           // @a infeasible
                           low = mid;
                       }
                   }
                   // @a done
                   return high;
               }""";
    }

    private List<ArrayElement> gapState(double[] gaps, int highlight) {
        List<ArrayElement> state = new ArrayList<>(gaps.length);
        for (int i = 0; i < gaps.length; i++) {
            state.add(new ArrayElement(i, (int) gaps[i], i == highlight ? "current" : "target"));
        }
        return state;
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] stations = in.getIntArray("stations");
        int k = in.getInt("k");
        int n = stations.length;
        double[] gaps = new double[n - 1];
        double maxGap = 0;
        for (int i = 0; i < n - 1; i++) {
            gaps[i] = stations[i + 1] - stations[i];
            maxGap = Math.max(maxGap, gaps[i]);
        }

        double low = 0, high = maxGap;
        emit.at("init")
                .say("Binary search the max distance between adjacent stations after adding %d more. "
                        + "%d gaps, largest is %.2f — that is the worst case with zero additions.",
                        k, gaps.length, maxGap)
                .var("low", low).var("high", high).var("k", k)
                .arrayState(gapState(gaps, -1)).step();

        for (int iter = 0; iter < ITERATIONS; iter++) {
            double mid = (low + high) / 2;
            emit.at("mid")
                    .say("Iteration %d: test max distance %.5f.", iter + 1, mid)
                    .var("low", low).var("high", high).var("mid", String.format("%.5f", mid))
                    .arrayState(gapState(gaps, -1)).step();

            int needed = 0;
            for (int i = 0; i < gaps.length; i++) {
                double gap = gaps[i];
                int extra = (int) Math.ceil(gap / mid) - 1;
                needed += extra;
                emit.at("gapTally")
                        .say("Gap %d = %.2f needs %d extra station(s) at this distance. Running total = %d.",
                                i, gap, extra, needed)
                        .var("needed", needed)
                        .arrayState(gapState(gaps, i)).step();
            }

            if (needed <= k) {
                emit.at("feasible")
                        .say("%d <= %d — %.5f is achievable. Try a smaller max distance.", needed, k, mid)
                        .var("high", String.format("%.5f", mid))
                        .arrayState(gapState(gaps, -1)).step();
                high = mid;
            } else {
                emit.at("infeasible")
                        .say("%d > %d — %.5f is too tight. Try a larger max distance.", needed, k, mid)
                        .var("low", String.format("%.5f", mid))
                        .arrayState(gapState(gaps, -1)).step();
                low = mid;
            }
        }

        emit.at("done")
                .say("After %d halvings, the minimum achievable max distance converges to %.5f.", ITERATIONS, high)
                .var("answer", String.format("%.5f", high))
                .arrayState(gapState(gaps, -1)).step();
    }
}
