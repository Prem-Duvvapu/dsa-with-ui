package com.dsa.ui.tracer.impl;

import com.dsa.ui.model.DpCell;
import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.DsType;
import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Climbing stairs as the first tabulated recurrence a learner meets: ways[i] is the number
 * of distinct ways to stand on stair i, and the only way to arrive is a 1-step from i-1 or
 * a 2-step from i-2.
 *
 * <p>Traced against the full O(N) table rather than the two rolling variables the
 * space-optimised version keeps, because the whole lesson is that a cell depends on exactly
 * two earlier cells — which is invisible once they collapse into {@code prev} and
 * {@code prev2}.
 */
@Component
public class ClimbingStairsTracer implements AlgorithmTracer {

    @Override
    public String id() {
        return "climbing-stairs";
    }

    @Override
    public DsType dsType() {
        return DsType.DP_TABLE;
    }

    @Override
    public InputSpec inputSpec() {
        return InputSpec.of(
                InputField.of("n", FieldType.INT)
                        .label("Stairs")
                        .help("ways[i] = ways[i-1] + ways[i-2] — Fibonacci, arrived at from first principles.")
                        .range(1, 30)
                        .defaultValue(5)
                        .build());
    }

    /** Long enough that the table outgrows the eye and the doubling becomes the point. */
    @Override
    public Map<String, Object> alternateInput() {
        return Map.of("n", 12);
    }

    @Override
    public String annotatedCode() {
        return """
               public int climbStairs(int n) {
                   // @a init
                   int[] ways = new int[n + 1];
                   // @a base
                   ways[0] = 1; ways[1] = 1;
                   for (int i = 2; i <= n; i++) {
                       // @a combine
                       ways[i] = ways[i - 1] + ways[i - 2];
                   }
                   // @a done
                   return ways[n];
               }""";
    }

    @Override
    public void run(Inputs in, StepEmitter emit) {
        int n = in.getInt("n");
        int[] ways = new int[n + 1];
        boolean[] settled = new boolean[n + 1];

        emit.at("init")
                .say("One cell per stair, 0 through %d. Each cell will hold the number of "
                        + "distinct ways to be standing on that stair.", n)
                .var("n", n)
                .dpTable(table(ways, settled, -1, "?", Set.of(), false, null, null)).step();

        ways[0] = 1;
        ways[1] = 1;
        settled[0] = true;
        settled[1] = true;
        emit.at("base")
                .say("Two base cases, and they are not arbitrary: there is exactly one way "
                        + "to be at the bottom (take no steps), and exactly one way to reach "
                        + "stair 1 (a single 1-step). Everything else is derived from these.")
                .var("ways[0]", ways[0]).var("ways[1]", ways[1])
                .dpTable(table(ways, settled, -1, "?", Set.of(), false, null, null)).step();

        for (int i = 2; i <= n; i++) {
            int fromOneStep = ways[i - 1];
            int fromTwoSteps = ways[i - 2];
            int combined = fromOneStep + fromTwoSteps;
            String substitution = String.format("ways[%d] = ways[%d] + ways[%d] = %d + %d = %d",
                    i, i - 1, i - 2, fromOneStep, fromTwoSteps, combined);
            emit.at("combine")
                    .say("To stand on stair %d you arrived either with a 1-step from stair %d "
                            + "(which holds %d) or a 2-step from stair %d (which holds %d). No "
                            + "path is counted twice, so the two add: %d + %d = %d.",
                            i, i - 1, fromOneStep, i - 2, fromTwoSteps,
                            fromOneStep, fromTwoSteps, combined)
                    .var("i", i)
                    .var("fromOneStep", fromOneStep)
                    .var("fromTwoSteps", fromTwoSteps)
                    .var("ways[i]", combined)
                    .dpTable(table(ways, settled, i, String.valueOf(combined),
                            Set.of(i - 1, i - 2), false, FORMULA, substitution)).step();

            ways[i] = combined;
            settled[i] = true;
        }

        emit.at("done")
                .say("ways[%d] = %d. No cell was ever computed twice, which is why %d stairs "
                        + "cost %d additions instead of the exponential recursion tree the "
                        + "naive definition unfolds into.",
                        n, ways[n], n, Math.max(0, n - 1))
                .var("answer", ways[n])
                .dpTable(table(ways, settled, -1, "?", Set.of(), true, null, null)).step();
    }

    private static final String FORMULA = "ways[i] = ways[i-1] + ways[i-2]";

    private static DpTable table(int[] ways, boolean[] settled, int probe, String probeValue,
                                 Set<Integer> reads, boolean done, String formula, String substitution) {
        List<String> colLabels = new ArrayList<>(ways.length);
        List<DpCell> row = new ArrayList<>(ways.length);
        for (int i = 0; i < ways.length; i++) {
            colLabels.add(String.valueOf(i));

            String state;
            String value;
            if (done) {
                state = "resolved";
                value = String.valueOf(ways[i]);
            } else if (i == probe) {
                state = "probe";
                value = probeValue;
            } else if (reads.contains(i)) {
                state = "read";
                value = String.valueOf(ways[i]);
            } else if (settled[i]) {
                state = "known";
                value = String.valueOf(ways[i]);
            } else {
                state = "void";
                value = "·";
            }
            row.add(new DpCell(value, state));
        }
        DpTable dpTable = new DpTable(List.of("ways to reach"), colLabels, List.of(row));
        return formula != null ? dpTable.withFormula(formula, substitution) : dpTable;
    }
}
