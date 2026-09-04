package com.dsa.ui.tracer;

import com.dsa.ui.model.DpTable;
import com.dsa.ui.model.ExecutionStep;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PROMPT-F-visual-fidelity.md design D3: a DP table above the values should show the
 * recurrence itself, symbolic then substituted with this step's numbers
 * ("dp[4] = dp[3] + dp[2] = 3 + 2 = 5"). 18 of the 19 DP_TABLE tracers have adopted it —
 * every one whose recurrence is a cell computed from other cells. The lone holdout,
 * lis-binary-search, runs patience sorting: its per-element step is a binary-search
 * placement into a sorted tails[] array, not a "dp[i] = f(dp[...])" recurrence, so there
 * is no substitution to show. The other DP_TABLE tracers are deliberately unchanged; this
 * is a per-tracer opt-in, not a wire-format requirement.
 */
@SpringBootTest
class DpRecurrenceTraceTest {

    @Autowired
    private TracerRegistry registry;

    @Autowired
    private TraceRunner runner;

    private static final Set<String> ADOPTED_D3 = Set.of(
            "climbing-stairs", "frog-jump", "grid-unique-paths",
            "subset-sum-equal-target", "count-subsets-with-sum-k", "minimum-coins-dp",
            "unique-paths-2", "minimum-falling-path-sum", "triangle-min-path-sum",
            "ninjas-training", "max-sum-non-adjacent", "house-robber-2",
            "frog-jump-k-distance", "partition-equal-subset-sum",
            "count-partitions-given-diff", "coin-change-2",
            "longest-increasing-subsequence", "print-lis");

    @ParameterizedTest(name = "{0} carries a formula and a live substitution on at least one step")
    @ValueSource(strings = {
            "climbing-stairs", "frog-jump", "grid-unique-paths",
            "subset-sum-equal-target", "count-subsets-with-sum-k", "minimum-coins-dp",
            "unique-paths-2", "minimum-falling-path-sum", "triangle-min-path-sum",
            "ninjas-training", "max-sum-non-adjacent", "house-robber-2",
            "frog-jump-k-distance", "partition-equal-subset-sum",
            "count-partitions-given-diff", "coin-change-2",
            "longest-increasing-subsequence", "print-lis"})
    void recurrenceReachesTheWire(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        List<ExecutionStep> steps = runner.runDefaults(tracer).getSteps();

        boolean anyCarriesRecurrence = steps.stream()
                .map(ExecutionStep::getDpTable)
                .filter(java.util.Objects::nonNull)
                .anyMatch(table -> table.formula() != null && table.substitution() != null);

        assertTrue(anyCarriesRecurrence,
                id + ": no step carried both formula and substitution — the recurrence "
                        + "line would never render");
    }

    @ParameterizedTest(name = "{0} never sends a substitution without its formula, or vice versa")
    @ValueSource(strings = {
            "climbing-stairs", "frog-jump", "grid-unique-paths",
            "subset-sum-equal-target", "count-subsets-with-sum-k", "minimum-coins-dp",
            "unique-paths-2", "minimum-falling-path-sum", "triangle-min-path-sum",
            "ninjas-training", "max-sum-non-adjacent", "house-robber-2",
            "frog-jump-k-distance", "partition-equal-subset-sum",
            "count-partitions-given-diff", "coin-change-2",
            "longest-increasing-subsequence", "print-lis"})
    void formulaAndSubstitutionArePaired(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        List<ExecutionStep> steps = runner.runDefaults(tracer).getSteps();

        for (ExecutionStep step : steps) {
            DpTable table = step.getDpTable();
            if (table == null) continue;
            boolean hasFormula = table.formula() != null;
            boolean hasSubstitution = table.substitution() != null;
            assertEquals(hasFormula, hasSubstitution,
                    id + " step " + step.getStepNumber()
                            + ": formula and substitution must arrive together, got formula="
                            + table.formula() + " substitution=" + table.substitution());
        }
    }

    @ParameterizedTest(name = "every OTHER DP_TABLE tracer still sends null (unchanged, not a regression)")
    @ValueSource(strings = {"lis-binary-search"})
    void otherDpTracersAreUntouched(String id) {
        AlgorithmTracer tracer = registry.find(id).orElseThrow();
        List<ExecutionStep> steps = runner.runDefaults(tracer).getSteps();

        boolean anyCarriesRecurrence = steps.stream()
                .map(ExecutionStep::getDpTable)
                .filter(java.util.Objects::nonNull)
                .anyMatch(table -> table.formula() != null);

        assertTrue(ADOPTED_D3.contains(id) || !anyCarriesRecurrence,
                id + " now carries a formula but is not in ADOPTED_D3 — update this test's "
                        + "bookkeeping, it is not a bug");
    }

    @org.junit.jupiter.api.Test
    @DisplayName("climbing-stairs' substitution matches the hand-derived Fibonacci sequence")
    void climbingStairsSubstitutionIsHandVerified() {
        AlgorithmTracer tracer = registry.find("climbing-stairs").orElseThrow();
        List<ExecutionStep> steps = runner.runDefaults(tracer).getSteps();

        List<String> substitutions = steps.stream()
                .map(ExecutionStep::getDpTable)
                .filter(java.util.Objects::nonNull)
                .map(DpTable::substitution)
                .filter(java.util.Objects::nonNull)
                .toList();

        // Default input is n=5. 1, 1, 2, 3, 5, 8 — verified by hand, not just self-consistent.
        assertEquals(List.of(
                "ways[2] = ways[1] + ways[0] = 1 + 1 = 2",
                "ways[3] = ways[2] + ways[1] = 2 + 1 = 3",
                "ways[4] = ways[3] + ways[2] = 3 + 2 = 5",
                "ways[5] = ways[4] + ways[3] = 5 + 3 = 8"
        ), substitutions);
    }
}
