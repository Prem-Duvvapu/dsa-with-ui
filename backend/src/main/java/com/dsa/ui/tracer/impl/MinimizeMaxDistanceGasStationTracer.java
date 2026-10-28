package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.ArrayElement;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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

    // Initial gap <=1000;40 halvings leave a bracket narrower than1e-9.
    private static final int ITERATIONS = 40;
    private static final String SOURCE = SolutionSource.read(
            "/solutions/core/minimize-max-distance-gas-station.java");

    @Override
    public String id() {
        return "minimize-max-distance-gas-station";
    }

    @Override
    public DsType dsType() {
        return DsType.SEARCH_SPACE;
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
        return SOURCE;
    }

    private List<ArrayElement> gapState(double[] gaps, int highlight) {
        List<ArrayElement> state = new ArrayList<>(gaps.length);
        for (int i = 0; i < gaps.length; i++) {
            state.add(new ArrayElement(i, (int) gaps[i], i == highlight ? "current" : "target"));
        }
        return state;
    }

    /** A continuous domain and its raw bounds always belong to one actual snapshot. */
    private StepEmitter.Step snapshot(StepEmitter emit, String anchor, double low, double high,
                                      double[] gaps, int highlight) {
        return emit.at(anchor).var("searchDomain", "continuous")
                .var("low", low).var("high", high).var("intervalWidth", high - low)
                .arrayState(gapState(gaps, highlight));
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
        snapshot(emit, "init", low, high, gaps, -1)
                .say("Binary search the max distance between adjacent stations after adding %d more. "
                        + "%d gaps, largest is %s — that is the worst case with zero additions.",
                        k, gaps.length, Double.toString(maxGap))
                .var("k", k).var("iterations", ITERATIONS).step();

        for (int iter = 0; iter < ITERATIONS; iter++) {
            double mid = (low + high) / 2;
            snapshot(emit, "mid", low, high, gaps, -1)
                    .say("Iteration %d of %d: test max distance %s.", iter + 1, ITERATIONS, Double.toString(mid))
                    .var("iteration", iter + 1).var("mid", mid).step();

            int needed = 0;
            for (int i = 0; i < gaps.length; i++) {
                double gap = gaps[i];
                int extra = (int) Math.ceil(gap / mid) - 1;
                needed += extra;
                snapshot(emit, "gapTally", low, high, gaps, i)
                        .say("Gap %d = %s needs %d extra station%s at this distance. Running total = %d.",
                                i, Double.toString(gap), extra, Narration.s(extra), needed)
                        .var("iteration", iter + 1).var("mid", mid).var("needed", needed).step();
            }

            if (needed <= k) {
                high = mid;
                snapshot(emit, "feasible", low, high, gaps, -1)
                        .say("%d <= %d — %s is achievable. Try a smaller max distance.", needed, k, Double.toString(mid))
                        .var("iteration", iter + 1).var("needed", needed).var("k", k).step();
            } else {
                low = mid;
                snapshot(emit, "infeasible", low, high, gaps, -1)
                        .say("%d > %d — %s is too tight. Try a larger max distance.", needed, k, Double.toString(mid))
                        .var("iteration", iter + 1).var("needed", needed).var("k", k).step();
            }
        }

        String answer = String.format(Locale.ROOT, "%.5f", high);
        snapshot(emit, "done", low, high, gaps, -1)
                .say("After %d halvings, interval width is %s (below 1e-9). Rounded to five decimals: %s.",
                        ITERATIONS, Double.toString(high - low), answer)
                .var("iteration", ITERATIONS).var("answer", answer).step();
    }
}
