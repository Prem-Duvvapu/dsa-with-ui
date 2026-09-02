package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Frog jump generalised from two fixed predecessors to any of the previous K stairs.
 *
 * <p>Every candidate gets its own step, and the probe cell shows the running minimum
 * tightening as candidates are weighed. That is the whole difference from
 * {@link FrogJumpTracer}: the inner loop replaces a hand-written comparison of two
 * transitions, and the cost goes from O(N) to O(N*K). A trace that only showed the winning
 * predecessor would hide exactly the work that K adds.
 */
@Component
public class FrogJumpKDistanceTracer implements AlgorithmTracer {

    private static final String UNDECIDED = "?";

    @Override
    public String id() {
        return "frog-jump-k-distance";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("heights", FieldType.INT_ARRAY)
                        .label("Stair heights")
                        .help("A dip in the middle rewards long jumps, which is where K earns "
                                + "its cost.")
                        .length(2, 16).values(0, 999)
                        .defaultValue(List.of(10, 30, 40, 20, 50))
                        .build(),
                InputField.of("k", FieldType.INT)
                        .label("Max jump distance")
                        .help("K = 2 reduces this to plain frog jump; raise it and each cell "
                                + "reads more predecessors.")
                        .range(1, 8)
                        .defaultValue(3)
                        .build());
    }

    /** A tighter K over a longer, more jagged staircase — a different shape of window. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of(
                "heights", List.of(50, 20, 60, 30, 10, 40, 20),
                "k", 2);
    }

    @Override
    public String annotatedCode() {
        return """
               public int frogJumpK(int n, int k, int[] heights) {
                   // @a init
                   int[] energy = new int[n];
                   // @a base
                   energy[0] = 0;
                   for (int i = 1; i < n; i++) {
                       int best = Integer.MAX_VALUE;
                       for (int j = i - 1; j >= Math.max(0, i - k); j--) {
                           // @a consider
                           int cost = energy[j] + Math.abs(heights[i] - heights[j]);
                           best = Math.min(best, cost);
                       }
                       // @a settle
                       energy[i] = best;
                   }
                   // @a done
                   return energy[n - 1];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] heights = in.getIntArray("heights");
        int k = in.getInt("k");
        int n = heights.length;
        int[] energy = new int[n];
        boolean[] settled = new boolean[n];

        emit.at("init")
                .say("Same staircase, but the frog may now jump up to %d stairs at once. Each "
                        + "energy cell will be decided by looking back at as many as %d "
                        + "already-settled cells instead of exactly two.", k, k)
                .var("phase", "init").var("n", n).var("k", k)
                .dpTable(FrogEnergyTable.of(heights, energy, settled, -1, UNDECIDED,
                        Set.of(), false))
                .step();

        settled[0] = true;
        emit.at("base")
                .say("Stair 0 still costs nothing, and it is still the only cell that needs no "
                        + "predecessor.")
                .var("phase", "base").var("energy[0]", 0)
                .dpTable(FrogEnergyTable.of(heights, energy, settled, -1, UNDECIDED,
                        Set.of(), false))
                .step();

        for (int i = 1; i < n; i++) {
            int best = Integer.MAX_VALUE;
            int bestFrom = -1;
            int lowest = Math.max(0, i - k);

            for (int j = i - 1; j >= lowest; j--) {
                int cost = energy[j] + Math.abs(heights[i] - heights[j]);
                boolean improves = cost < best;
                if (improves) {
                    best = cost;
                    bestFrom = j;
                }

                String verdict;
                if (improves) {
                    verdict = "Best so far, so the running minimum drops to %d.".formatted(best);
                } else if (cost == best) {
                    verdict = ("Ties the %d already found from stair %d, so the minimum is "
                            + "unchanged.").formatted(best, bestFrom);
                } else {
                    verdict = ("Worse than the %d already found from stair %d, so discard it.")
                            .formatted(best, bestFrom);
                }

                emit.at("consider")
                        .say("Stair %d from stair %d, a jump of %d: %d already spent plus "
                                + "|%d - %d| = %d. %s",
                                i, j, i - j, energy[j], heights[i], heights[j], cost, verdict)
                        .var("phase", "consider")
                        .var("i", i)
                        .var("from", j)
                        .var("distance", i - j)
                        .var("cost", cost)
                        .var("best", best)
                        .dpTable(FrogEnergyTable.of(heights, energy, settled, i,
                                String.valueOf(best), Set.of(j), false))
                        .step();
            }

            energy[i] = best;
            settled[i] = true;

            int weighed = i - lowest;
            emit.at("settle")
                    .say("%s of stair %d %s been weighed. The cheapest arrives from stair %d at "
                            + "%d energy, so energy[%d] is now fixed and later stairs may read "
                            + "it.",
                            weighed == 1 ? "The one reachable predecessor"
                                    : "All %d reachable predecessors".formatted(weighed),
                            i,
                            weighed == 1 ? "has" : "have",
                            bestFrom, best, i)
                    .var("phase", "settle")
                    .var("i", i)
                    .var("from", bestFrom)
                    .var("energy[i]", best)
                    .dpTable(FrogEnergyTable.of(heights, energy, settled, -1, UNDECIDED,
                            Set.of(bestFrom), false))
                    .step();
        }

        emit.at("done")
                .say("Cheapest way onto stair %d is %d energy. Raising K widens the window each "
                        + "cell reads, which is why this costs O(N * K) where the two-step "
                        + "version cost O(N).", n - 1, energy[n - 1])
                .var("phase", "done")
                .var("answer", energy[n - 1])
                .dpTable(FrogEnergyTable.of(heights, energy, settled, -1, UNDECIDED,
                        Set.of(), true))
                .step();
    }
}
