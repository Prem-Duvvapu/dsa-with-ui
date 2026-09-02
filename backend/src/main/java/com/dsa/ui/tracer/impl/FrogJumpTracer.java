package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Frog jump: the same two-predecessor shape as climbing stairs, but minimising a cost
 * instead of counting paths — which is the step where a learner discovers that the
 * recurrence, not the loop, is what changes between DP problems.
 *
 * <p>Both candidate transitions are emitted on every step, including the losing one, so the
 * trace shows the comparison rather than only its outcome. A greedy 1-step-at-a-time frog
 * is wrong here, and seeing {@code jumpTwo} beat {@code jumpOne} is what proves it.
 */
@Component
public class FrogJumpTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "frog-jump";
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
                        .help("Energy for a jump is the height difference, so a tall stair "
                                + "next door can cost more than a short one two away.")
                        .length(2, 20).values(0, 999)
                        .defaultValue(List.of(10, 50, 40, 30))
                        .build());
    }

    /** Longer, and the far jump wins twice in a row before the near jump takes over. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("heights", List.of(30, 10, 60, 10, 20));
    }

    @Override
    public String annotatedCode() {
        return """
               public int frogJump(int n, int[] heights) {
                   // @a init
                   int[] energy = new int[n];
                   // @a base
                   energy[0] = 0;
                   for (int i = 1; i < n; i++) {
                       int jumpOne = energy[i - 1] + Math.abs(heights[i] - heights[i - 1]);
                       int jumpTwo = i > 1
                               ? energy[i - 2] + Math.abs(heights[i] - heights[i - 2])
                               : Integer.MAX_VALUE;
                       // @a evaluate
                       energy[i] = Math.min(jumpOne, jumpTwo);
                   }
                   // @a done
                   return energy[n - 1];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int[] heights = in.getIntArray("heights");
        int n = heights.length;
        int[] energy = new int[n];
        boolean[] settled = new boolean[n];

        emit.at("init")
                .say("The frog starts on stair 0 and must reach stair %d. energy[i] will hold "
                        + "the cheapest total cost of standing on stair i.", n - 1)
                .var("n", n)
                .dpTable(FrogEnergyTable.of(heights, energy, settled, -1, "?", Set.of(), false))
                .step();

        settled[0] = true;
        emit.at("base")
                .say("Stair 0 costs nothing — the frog is already there, having jumped nowhere.")
                .var("energy[0]", 0)
                .dpTable(FrogEnergyTable.of(heights, energy, settled, -1, "?", Set.of(), false))
                .step();

        for (int i = 1; i < n; i++) {
            int jumpOne = energy[i - 1] + Math.abs(heights[i] - heights[i - 1]);
            boolean farReachable = i > 1;
            int jumpTwo = farReachable
                    ? energy[i - 2] + Math.abs(heights[i] - heights[i - 2])
                    : Integer.MAX_VALUE;
            int chosen = Math.min(jumpOne, jumpTwo);

            String narration;
            if (!farReachable) {
                narration = ("Stair 1 has only one predecessor, so there is nothing to "
                        + "compare against: energy[0] + |%d - %d| = %d.")
                        .formatted(heights[1], heights[0], jumpOne);
            } else {
                String verdict;
                if (jumpTwo < jumpOne) {
                    verdict = "the 2-step wins. What decides it is the running total, not the "
                            + "height gap, so skipping a stair can beat hopping to the nearest";
                } else if (jumpOne < jumpTwo) {
                    verdict = "the 1-step wins";
                } else {
                    verdict = "the routes tie, so it makes no difference which the frog takes";
                }
                narration = ("Stair %d: a 1-step from %d costs %d + |%d - %d| = %d; a 2-step "
                        + "from %d costs %d + |%d - %d| = %d. energy[%d] = %d — %s.")
                        .formatted(i,
                                i - 1, energy[i - 1], heights[i], heights[i - 1], jumpOne,
                                i - 2, energy[i - 2], heights[i], heights[i - 2], jumpTwo,
                                i, chosen, verdict);
            }

            emit.at("evaluate")
                    .say(narration)
                    .var("i", i)
                    .var("jumpOne", jumpOne)
                    .var("jumpTwo", farReachable ? jumpTwo : "—")
                    .var("energy[i]", chosen)
                    .dpTable(FrogEnergyTable.of(heights, energy, settled, i,
                            String.valueOf(chosen),
                            farReachable ? Set.of(i - 1, i - 2) : Set.of(i - 1), false))
                    .step();

            energy[i] = chosen;
            settled[i] = true;
        }

        emit.at("done")
                .say("Cheapest way onto stair %d is %d energy. Note the frog never needed to "
                        + "know which route it took — only the cost of the best one.",
                        n - 1, energy[n - 1])
                .var("answer", energy[n - 1])
                .dpTable(FrogEnergyTable.of(heights, energy, settled, -1, "?", Set.of(), true))
                .step();
    }
}
